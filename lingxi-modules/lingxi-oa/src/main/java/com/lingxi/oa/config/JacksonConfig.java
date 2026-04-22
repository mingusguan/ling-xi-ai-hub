package com.lingxi.oa.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Jackson 日期格式化配置
 * 支持多种日期格式反序列化(ISO 8601 和 yyyy-MM-dd HH:mm:ss)
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper(Jackson2ObjectMapperBuilder builder) {
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        
        // 设置默认时区
        objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        
        // 设置默认日期格式
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        objectMapper.setDateFormat(dateFormat);
        
        // 禁用将日期写为时间戳
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        
        // 注册 JavaTimeModule 支持 Java 8 时间 API
        objectMapper.registerModule(new JavaTimeModule());
        
        // 注册自定义日期反序列化器，支持多种格式
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Date.class, new FlexibleDateDeserializer());
        objectMapper.registerModule(module);
        
        return objectMapper;
    }
    
    /**
     * 灵活的日期反序列化器，支持多种日期格式
     */
    static class FlexibleDateDeserializer extends DateDeserializers.DateDeserializer {
        private static final long serialVersionUID = 1L;
        
        private static final String[] DATE_FORMATS = {
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",  // ISO 8601 with timezone
            "yyyy-MM-dd'T'HH:mm:ss.SSSZ",     // ISO 8601
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd"
        };
        
        @Override
        public Date deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String dateStr = p.getText();
            if (dateStr == null || dateStr.isEmpty()) {
                return null;
            }
            
            // 尝试多种格式
            for (String format : DATE_FORMATS) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(format);
                    sdf.setTimeZone(TimeZone.getTimeZone("GMT+8"));
                    return sdf.parse(dateStr);
                } catch (ParseException ignored) {
                    // 尝试下一个格式
                }
            }
            
            // 如果都失败，抛出原始异常
            return super.deserialize(p, ctxt);
        }
    }
}
