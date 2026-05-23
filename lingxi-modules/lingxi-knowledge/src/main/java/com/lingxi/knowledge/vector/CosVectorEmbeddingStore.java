package com.lingxi.knowledge.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.core.utils.uuid.Seq;
import com.lingxi.knowledge.config.CosVectorConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.BasicSessionCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.auth.COSSessionCredentials;
import com.qcloud.cos.utils.UrlEncoderUtils;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 基于腾讯云 COS 向量桶的 LangChain4j 向量存储适配器。
 */
@Slf4j
public class CosVectorEmbeddingStore implements EmbeddingStore<TextSegment> {

    private static final MediaType JSON = MediaType.get("application/json");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String VECTOR_PATH_PUT = "/PutVectors";
    private static final String VECTOR_PATH_QUERY = "/QueryVectors";
    private static final String VECTOR_PATH_DELETE = "/DeleteVectors";
    private static final Pattern INDEX_NAME_PATTERN = Pattern.compile("^[a-z0-9]([a-z0-9\\-.]{1,61}[a-z0-9])?$");

    private final CosVectorConfig config;
    private final String indexName;
    private final OkHttpClient httpClient;
    private final COSCredentials credentials;

    public CosVectorEmbeddingStore(CosVectorConfig config) {
        this.config = config;
        this.indexName = normalizeIndexName(config.getIndexName());
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(Duration.ofSeconds(10))
                .readTimeout(Duration.ofSeconds(30))
                .writeTimeout(Duration.ofSeconds(30))
                .build();
        this.credentials = buildCredentials(config);
    }

    @Override
    public String add(Embedding embedding) {
        String id = nextVectorId();
        add(id, embedding);
        return id;
    }

    @Override
    public void add(String id, Embedding embedding) {
        addAll(List.of(id), List.of(embedding), null);
    }

    @Override
    public String add(Embedding embedding, TextSegment embedded) {
        String id = nextVectorId();
        addAll(List.of(id), List.of(embedding), List.of(embedded));
        return id;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings) {
        List<String> ids = nextVectorIds(embeddings.size());
        addAll(ids, embeddings, null);
        return ids;
    }

    @Override
    public List<String> addAll(List<Embedding> embeddings, List<TextSegment> embedded) {
        List<String> ids = nextVectorIds(embeddings.size());
        addAll(ids, embeddings, embedded);
        return ids;
    }

    @Override
    public void remove(String id) {
        removeAll(List.of(id));
    }

    @Override
    public void removeAll(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("indexName", indexName);
        body.put("vectorBucketName", config.getBucketName());
        body.put("keys", new ArrayList<>(ids));
        post(VECTOR_PATH_DELETE, body);
    }

    @Override
    public void removeAll(Filter filter) {
        throw new UnsupportedOperationException("COS Vector Bucket removeAll by filter is not supported yet");
    }

    @Override
    public void removeAll() {
        throw new UnsupportedOperationException("COS Vector Bucket removeAll is not supported yet");
    }

    @Override
    public EmbeddingSearchResult<TextSegment> search(EmbeddingSearchRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("indexName", indexName);
        body.put("vectorBucketName", config.getBucketName());
        body.put("queryVector", Map.of("float32", request.queryEmbedding().vectorAsList()));
        body.put("topK", request.maxResults());
        body.put("returnData", true);
        body.put("returnDistance", true);
        body.put("returnMetadata", true);

        JsonNode response = post(VECTOR_PATH_QUERY, body);
        List<EmbeddingMatch<TextSegment>> matches = parseMatches(response, request.minScore());
        return new EmbeddingSearchResult<>(matches);
    }

    private void addAll(List<String> ids, List<Embedding> embeddings, List<TextSegment> segments) {
        if (embeddings == null || embeddings.isEmpty()) {
            return;
        }
        if (segments != null && segments.size() != embeddings.size()) {
            throw new IllegalArgumentException("embeddings and embedded segments size must match");
        }

        List<Map<String, Object>> vectors = new ArrayList<>();
        for (int i = 0; i < embeddings.size(); i++) {
            TextSegment segment = segments == null ? null : segments.get(i);
            Map<String, Object> vector = new LinkedHashMap<>();
            vector.put("key", ids.get(i));
            vector.put("data", Map.of("float32", embeddings.get(i).vectorAsList()));
            if (segment != null) {
                vector.put("metadata", toMetadata(segment));
            }
            vectors.add(vector);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("indexName", indexName);
        body.put("vectorBucketName", config.getBucketName());
        body.put("vectors", vectors);
        post(VECTOR_PATH_PUT, body);
    }

    private String normalizeIndexName(String configuredIndexName) {
        if (StringUtils.isEmpty(configuredIndexName)) {
            throw new IllegalStateException("COS Vector indexName must not be empty");
        }
        // COS Vector 索引名只允许小写字母、数字、短横线和点号，兼容历史配置里的下划线。
        String normalized = configuredIndexName.trim().toLowerCase().replace('_', '-');
        if (!INDEX_NAME_PATTERN.matcher(normalized).matches()) {
            throw new IllegalStateException("COS Vector indexName invalid: " + configuredIndexName
                    + ", expected pattern ^[a-z0-9]([a-z0-9\\-.]{1,61}[a-z0-9])?$");
        }
        return normalized;
    }

    private Map<String, Object> toMetadata(TextSegment segment) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("text", segment.text());
        if (segment.metadata() != null) {
            metadata.putAll(segment.metadata().toMap());
        }
        return metadata;
    }

    private List<EmbeddingMatch<TextSegment>> parseMatches(JsonNode response, double minScore) {
        JsonNode vectors = findVectorsNode(response);
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        if (vectors == null || !vectors.isArray()) {
            return matches;
        }

        for (JsonNode vectorNode : vectors) {
            double score = resolveScore(vectorNode);
            if (score < minScore) {
                continue;
            }
            String id = textValue(vectorNode, "key", "id");
            Embedding embedding = parseEmbedding(vectorNode);
            TextSegment segment = parseSegment(vectorNode);
            matches.add(new EmbeddingMatch<>(score, id, embedding, segment));
        }
        return matches;
    }

    private JsonNode findVectorsNode(JsonNode response) {
        for (String field : List.of("vectors", "matches", "results")) {
            JsonNode node = response.get(field);
            if (node != null && node.isArray()) {
                return node;
            }
        }
        return null;
    }

    private double resolveScore(JsonNode vectorNode) {
        if (vectorNode.has("score")) {
            return vectorNode.get("score").asDouble();
        }
        if (vectorNode.has("distance")) {
            return 1D - vectorNode.get("distance").asDouble();
        }
        return 1D;
    }

    private Embedding parseEmbedding(JsonNode vectorNode) {
        JsonNode float32 = vectorNode.path("data").path("float32");
        if (!float32.isArray()) {
            return null;
        }
        float[] values = new float[float32.size()];
        for (int i = 0; i < float32.size(); i++) {
            values[i] = (float) float32.get(i).asDouble();
        }
        return Embedding.from(values);
    }

    private TextSegment parseSegment(JsonNode vectorNode) {
        JsonNode metadataNode = vectorNode.path("metadata");
        Metadata metadata = new Metadata();
        if (metadataNode.isObject()) {
            metadataNode.fields().forEachRemaining(entry -> {
                if (!"text".equals(entry.getKey())) {
                    metadata.put(entry.getKey(), entry.getValue().asText());
                }
            });
        }
        String text = metadataNode.path("text").asText("");
        return TextSegment.from(text, metadata);
    }

    private String textValue(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull()) {
                return value.asText();
            }
        }
        return null;
    }

    private JsonNode post(String path, Map<String, Object> body) {
        try {
            String json = OBJECT_MAPPER.writeValueAsString(body);
            byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
            String host = domain();
            String date = DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC));
            String contentLength = String.valueOf(jsonBytes.length);
            Request.Builder builder = new Request.Builder()
                    .url(endpoint(host, path))
                    .post(RequestBody.create(jsonBytes, JSON))
                    .header("Host", host)
                    .header("Date", date)
                    .header("Content-Type", JSON.toString())
                    .header("Content-Length", contentLength)
                    .header("Content-MD5", contentMd5(jsonBytes))
                    .header("Authorization", authorization(path, host, date, contentLength));
            appendSessionToken(builder);
            try (Response response = httpClient.newCall(builder.build()).execute()) {
                ResponseBody responseBody = response.body();
                String responseText = responseBody == null ? "" : responseBody.string();
                if (!response.isSuccessful()) {
                    throw new IllegalStateException("COS Vector request failed, code=" + response.code() + ", body=" + responseText);
                }
                if (StringUtils.isEmpty(responseText)) {
                    return OBJECT_MAPPER.createObjectNode();
                }
                return OBJECT_MAPPER.readTree(responseText);
            }
        } catch (IOException e) {
            throw new IllegalStateException("COS Vector request failed", e);
        }
    }

    private String authorization(String path, String host, String date, String contentLength) {
        long startTime = System.currentTimeMillis() / 1000;
        long endTime = startTime + Duration.ofMinutes(10).toSeconds();
        String keyTime = startTime + ";" + endTime;

        Map<String, String> headers = signedHeaders(host, date, contentLength);
        String headerList = String.join(";", headers.keySet());
        String httpHeaders = formatMapToSign(headers);
        String httpString = "post\n" + path + "\n\n" + httpHeaders + "\n";
        String stringToSign = "sha1\n" + keyTime + "\n" + sha1Hex(httpString) + "\n";
        String signKey = hmacSha1Hex(credentials.getCOSSecretKey(), keyTime);
        String signature = hmacSha1Hex(signKey, stringToSign);

        return "q-sign-algorithm=sha1"
                + "&q-ak=" + credentials.getCOSAccessKeyId()
                + "&q-sign-time=" + keyTime
                + "&q-key-time=" + keyTime
                + "&q-header-list=" + headerList
                + "&q-url-param-list="
                + "&q-signature=" + signature;
    }

    private Map<String, String> signedHeaders(String host, String date, String contentLength) {
        Map<String, String> headers = new TreeMap<>();
        headers.put("content-length", contentLength);
        headers.put("date", date);
        headers.put("host", host);
        return headers;
    }

    private String formatMapToSign(Map<String, String> values) {
        StringJoiner joiner = new StringJoiner("&");
        values.forEach((key, value) -> joiner.add(UrlEncoderUtils.encode(key.toLowerCase()) + "="
                + UrlEncoderUtils.encode(value == null ? "" : value)));
        return joiner.toString();
    }

    private String sha1Hex(String value) {
        return digestHex("SHA-1", value.getBytes(StandardCharsets.UTF_8));
    }

    private String contentMd5(byte[] body) {
        try {
            return Base64.getEncoder().encodeToString(MessageDigest.getInstance("MD5").digest(body));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm is not available", e);
        }
    }

    private String hmacSha1Hex(String key, String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
            return hex(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("HmacSHA1 algorithm is not available", e);
        }
    }

    private String digestHex(String algorithm, byte[] value) {
        try {
            return hex(MessageDigest.getInstance(algorithm).digest(value));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(algorithm + " algorithm is not available", e);
        }
    }

    private String hex(byte[] value) {
        StringBuilder builder = new StringBuilder(value.length * 2);
        for (byte item : value) {
            builder.append(String.format("%02x", item & 0xff));
        }
        return builder.toString();
    }

    private void appendSessionToken(Request.Builder builder) {
        if (credentials instanceof COSSessionCredentials sessionCredentials) {
            builder.header("x-cos-security-token", sessionCredentials.getSessionToken());
        }
    }

    private String endpoint(String host, String path) {
        return "https://" + host + path;
    }

    private String domain() {
        if (StringUtils.isNotEmpty(config.getDomain())) {
            return removeScheme(trimSlash(config.getDomain()));
        }
        return "vectors." + config.getRegion() + ".coslake.com";
    }

    private String trimSlash(String value) {
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        return value;
    }

    private String removeScheme(String value) {
        if (value.startsWith("https://")) {
            return value.substring("https://".length());
        }
        if (value.startsWith("http://")) {
            return value.substring("http://".length());
        }
        return value;
    }

    private COSCredentials buildCredentials(CosVectorConfig config) {
        if (StringUtils.isNotEmpty(config.getSessionToken())) {
            return new BasicSessionCredentials(config.getSecretId(), config.getSecretKey(), config.getSessionToken());
        }
        return new BasicCOSCredentials(config.getSecretId(), config.getSecretKey());
    }

    private List<String> nextVectorIds(int size) {
        List<String> ids = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ids.add(nextVectorId());
        }
        return ids;
    }

    private String nextVectorId() {
        return "kb_" + Seq.getId();
    }
}
