package com.lingxi.ai.agent.tools;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.lingxi.ai.config.SeniverseWeatherProperties;
import com.lingxi.common.core.utils.StringUtils;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

@Component
public class WeatherTool {

    private static final Logger log = LoggerFactory.getLogger(WeatherTool.class);

    private static final String DEFAULT_LOCATION = "ip";

    private final SeniverseWeatherProperties properties;

    private final HttpClient httpClient;

    public WeatherTool(SeniverseWeatherProperties properties) {
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.getRequestTimeout())
                .build();
    }

    @Tool("查询指定城市或当前位置的实时天气。location 可以是城市名、拼音、经纬度或 ip；用户未指定城市时传 ip。")
    public String queryCurrentWeather(String location) {
        if (!properties.isConfigured()) {
            return "天气服务未配置，请先配置心知天气 API Key。";
        }

        try {
            JSONObject result = firstResult(sendWeatherRequest("/v3/weather/now.json", location, null));
            JSONObject locationObject = result.getJSONObject("location");
            JSONObject now = result.getJSONObject("now");
            return new StringBuilder()
                    .append(locationObject.getString("name")).append("当前天气：")
                    .append(now.getString("text"))
                    .append("，气温 ").append(now.getString("temperature")).append("℃")
                    .append("，更新时间：").append(result.getString("last_update"))
                    .toString();
        } catch (Exception e) {
            log.warn("查询实时天气失败，location={}", location, e);
            return "查询实时天气失败，请稍后重试或确认城市名称是否正确。";
        }
    }

    @Tool("查询指定城市未来几天的天气预报。location 可以是城市名、拼音、经纬度或 ip；用户未指定城市时传 ip。")
    public String queryWeatherForecast(String location) {
        if (!properties.isConfigured()) {
            return "天气服务未配置，请先配置心知天气 API Key。";
        }

        try {
            JSONObject result = firstResult(sendWeatherRequest("/v3/weather/daily.json",
                    location,
                    String.valueOf(normalizedForecastDays())));
            JSONObject locationObject = result.getJSONObject("location");
            JSONArray daily = result.getJSONArray("daily");
            StringBuilder builder = new StringBuilder(locationObject.getString("name")).append("天气预报：\n");
            for (int i = 0; i < daily.size(); i++) {
                JSONObject day = daily.getJSONObject(i);
                builder.append("- ")
                        .append(day.getString("date"))
                        .append("：白天 ").append(day.getString("text_day"))
                        .append("，夜间 ").append(day.getString("text_night"))
                        .append("，").append(day.getString("low"))
                        .append("~").append(day.getString("high"))
                        .append("℃\n");
            }
            builder.append("更新时间：").append(result.getString("last_update"));
            return builder.toString();
        } catch (Exception e) {
            log.warn("查询天气预报失败，location={}", location, e);
            return "查询天气预报失败，请稍后重试或确认城市名称是否正确。";
        }
    }

    private JSONObject sendWeatherRequest(String path, String location, String days) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(buildRequestUrl(path, location, days)))
                .timeout(properties.getRequestTimeout())
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("心知天气接口响应异常，HTTP 状态码：" + response.statusCode());
        }
        return JSONObject.parseObject(response.body());
    }

    private String buildRequestUrl(String path, String location, String days) {
        StringBuilder builder = new StringBuilder(trimTrailingSlash(properties.getBaseUrl()))
                .append(path)
                .append("?key=").append(encode(properties.getApiKey()))
                .append("&location=").append(encode(defaultIfBlank(location, DEFAULT_LOCATION)))
                .append("&language=").append(encode(properties.getLanguage()))
                .append("&unit=").append(encode(properties.getUnit()));
        if (StringUtils.isNotBlank(days)) {
            builder.append("&days=").append(encode(days));
        }
        return builder.toString();
    }

    private JSONObject firstResult(JSONObject response) {
        JSONArray results = response.getJSONArray("results");
        if (results == null || results.isEmpty()) {
            throw new IllegalStateException("心知天气接口未返回有效结果。");
        }
        return results.getJSONObject(0);
    }

    private int normalizedForecastDays() {
        int days = properties.getForecastDays();
        if (days < 1) {
            return 1;
        }
        return Math.min(days, 15);
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value.trim();
    }

    private String trimTrailingSlash(String value) {
        if (StringUtils.isBlank(value)) {
            return "https://api.seniverse.com";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String encode(String value) {
        return URLEncoder.encode(defaultIfBlank(value, ""), StandardCharsets.UTF_8);
    }
}
