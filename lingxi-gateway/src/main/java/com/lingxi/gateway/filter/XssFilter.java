package com.lingxi.gateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.common.core.utils.html.EscapeUtil;
import com.lingxi.gateway.config.properties.XssProperties;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 璺ㄧ珯鑴氭湰杩囨护鍣? *
 * @author cloud
 */
@Component
@ConditionalOnProperty(value = "security.xss.enabled", havingValue = "true")
public class XssFilter implements GlobalFilter, Ordered
{
    // 璺ㄧ珯鑴氭湰鐨?xss 閰嶇疆锛宯acos鑷娣诲姞
    @Autowired
    private XssProperties xss;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain)
    {
        ServerHttpRequest request = exchange.getRequest();
        // xss寮€鍏虫湭寮€鍚?鎴?閫氳繃nacos鍏抽棴锛屼笉杩囨护
        if (!xss.getEnabled())
        {
            return chain.filter(exchange);
        }
        // GET DELETE 涓嶈繃婊?
        HttpMethod method = request.getMethod();
        if (method == null || method == HttpMethod.GET || method == HttpMethod.DELETE)
        {
            return chain.filter(exchange);
        }
        // 闈瀓son绫诲瀷锛屼笉杩囨护
        if (!isJsonRequest(exchange))
        {
            return chain.filter(exchange);
        }
        // excludeUrls 涓嶈繃婊?
        String url = request.getURI().getPath();
        if (StringUtils.matches(url, xss.getExcludeUrls()))
        {
            return chain.filter(exchange);
        }
        // BPMN 模板保存请求包含 XML 文本，HTML 清洗会破坏流程定义（如 sequenceFlow 的 sourceRef/targetRef）。
        if (skipBpmnTemplateBodyXss(url, method))
        {
            return chain.filter(exchange);
        }
        ServerHttpRequestDecorator httpRequestDecorator = requestDecorator(exchange);
        return chain.filter(exchange.mutate().request(httpRequestDecorator).build());

    }

    private boolean skipBpmnTemplateBodyXss(String url, HttpMethod method)
    {
        if (method == null || method != HttpMethod.POST)
        {
            return false;
        }
        // 仅豁免模板保存接口，兼容网关前缀路径（如 /prod-api/oa/workflow/template）。
        return StringUtils.isNotBlank(url) && ("/oa/workflow/template".equals(url) || url.endsWith("/oa/workflow/template"));
    }

    private ServerHttpRequestDecorator requestDecorator(ServerWebExchange exchange)
    {
        ServerHttpRequestDecorator serverHttpRequestDecorator = new ServerHttpRequestDecorator(exchange.getRequest())
        {
            @Override
            public Flux<DataBuffer> getBody()
            {
                Flux<DataBuffer> body = super.getBody();
                return body.buffer().map(dataBuffers -> {
                    DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                    DataBuffer join = dataBufferFactory.join(dataBuffers);
                    byte[] content = new byte[join.readableByteCount()];
                    join.read(content);
                    DataBufferUtils.release(join);
                    String bodyStr = new String(content, StandardCharsets.UTF_8);
                    // 闃瞲ss鏀诲嚮杩囨护
                    bodyStr = EscapeUtil.clean(bodyStr);
                    // 杞垚瀛楄妭
                    byte[] bytes = bodyStr.getBytes(StandardCharsets.UTF_8);
                    NettyDataBufferFactory nettyDataBufferFactory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
                    DataBuffer buffer = nettyDataBufferFactory.allocateBuffer(bytes.length);
                    buffer.write(bytes);
                    return buffer;
                });
            }

            @Override
            public HttpHeaders getHeaders()
            {
                HttpHeaders httpHeaders = new HttpHeaders();
                httpHeaders.putAll(super.getHeaders());
                // 鐢变簬淇敼浜嗚姹備綋鐨刡ody锛屽鑷碿ontent-length闀垮害涓嶇‘瀹氾紝鍥犳闇€瑕佸垹闄ゅ師鍏堢殑content-length
                httpHeaders.remove(HttpHeaders.CONTENT_LENGTH);
                httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                return httpHeaders;
            }

        };
        return serverHttpRequestDecorator;
    }

    /**
     * 鏄惁鏄疛son璇锋眰
     * 
     * @param exchange HTTP璇锋眰
     */
    public boolean isJsonRequest(ServerWebExchange exchange)
    {
        String header = exchange.getRequest().getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        return StringUtils.startsWithIgnoreCase(header, MediaType.APPLICATION_JSON_VALUE);
    }

    @Override
    public int getOrder()
    {
        return -100;
    }
}
