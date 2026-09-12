package com.lingxi.platform.web;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 把 64 位整型统一序列化为 JSON 字符串。
 *
 * <p>服务端使用趋势递增的 64 位标识，取值超出 JavaScript 与 ArkTS 的安全整数范围
 * （Number.MAX_SAFE_INTEGER = 9007199254740991）。若按数字输出，客户端解析会静默丢失精度，
 * 导致按标识寻址失败（目标详情、打卡、成就查询和同步游标都会受影响）。
 *
 * <p>因此响应中的 long/Long 统一输出为字符串；请求体仍同时接受数字与字符串，现有调用方无需改动。
 */
@Configuration
public class JacksonLongAsStringConfiguration {

  @Bean
  Jackson2ObjectMapperBuilderCustomizer longAsStringCustomizer() {
    return builder -> {
      builder.serializerByType(Long.class, ToStringSerializer.instance);
      builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
    };
  }
}
