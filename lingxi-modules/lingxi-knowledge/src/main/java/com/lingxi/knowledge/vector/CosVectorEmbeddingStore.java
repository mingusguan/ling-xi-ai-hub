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
import com.qcloud.cos.auth.COSSigner;
import com.qcloud.cos.http.HttpMethodName;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于腾讯云 COS 向量桶的 LangChain4j 向量存储适配器。
 */
@Slf4j
public class CosVectorEmbeddingStore implements EmbeddingStore<TextSegment> {

    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String VECTOR_PATH_PUT = "/PutVectors";
    private static final String VECTOR_PATH_QUERY = "/QueryVectors";
    private static final String VECTOR_PATH_DELETE = "/DeleteVectors";

    private final CosVectorConfig config;
    private final OkHttpClient httpClient;
    private final COSCredentials credentials;

    public CosVectorEmbeddingStore(CosVectorConfig config) {
        this.config = config;
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
        body.put("indexName", config.getIndexName());
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
        body.put("indexName", config.getIndexName());
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
        body.put("indexName", config.getIndexName());
        body.put("vectorBucketName", config.getBucketName());
        body.put("vectors", vectors);
        post(VECTOR_PATH_PUT, body);
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
            String host = domain();
            Request.Builder builder = new Request.Builder()
                    .url(endpoint(host, path))
                    .post(RequestBody.create(json, JSON))
                    .header("Host", host)
                    .header("Content-Type", "application/json")
                    .header("Authorization", authorization(path, host));
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

    private String authorization(String path, String host) {
        Date expiredTime = new Date(System.currentTimeMillis() + Duration.ofMinutes(10).toMillis());
        return new COSSigner().buildAuthorizationStr(HttpMethodName.POST, path, signedHeaders(host), Map.of(), credentials, expiredTime);
    }

    private Map<String, String> signedHeaders(String host) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", host);
        headers.put("content-type", "application/json");
        return headers;
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
