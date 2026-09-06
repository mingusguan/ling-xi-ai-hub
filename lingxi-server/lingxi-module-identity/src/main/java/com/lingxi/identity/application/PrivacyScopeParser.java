package com.lingxi.identity.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lingxi.identity.api.PrivacyRequestType;
import com.lingxi.identity.api.PrivacyScope;
import com.lingxi.kernel.BusinessException;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.stereotype.Component;

/** 校验并规范化隐私请求模块范围，避免各模块自行解释任意 JSON。 */
@Component
public class PrivacyScopeParser {
  private static final Set<String> SUPPORTED_MODULES =
      Set.of(
          "identity", "goal", "companion", "engagement",
          "relationship", "content", "commerce", "operations");

  private final ObjectMapper objectMapper;

  public PrivacyScopeParser(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** 解析并校验范围；账号注销强制为全模块。 */
  public PrivacyScope parse(String scopeJson, PrivacyRequestType type) {
    JsonNode root = readObject(scopeJson);
    rejectUnknownFields(root);
    JsonNode modulesNode = root.get("modules");
    if (modulesNode == null || modulesNode.isNull()) {
      return new PrivacyScope(Set.of());
    }
    if (!modulesNode.isArray()) {
      throw invalid("modules 必须是字符串数组");
    }
    LinkedHashSet<String> modules = new LinkedHashSet<>();
    for (JsonNode moduleNode : modulesNode) {
      if (!moduleNode.isTextual() || moduleNode.asText().isBlank()) {
        throw invalid("modules 只能包含非空模块名");
      }
      String moduleName = moduleNode.asText();
      if (!SUPPORTED_MODULES.contains(moduleName)) {
        throw invalid("不支持的隐私模块：" + moduleName);
      }
      if (!modules.add(moduleName)) {
        throw invalid("隐私模块不能重复：" + moduleName);
      }
    }
    if (modules.isEmpty()) {
      throw invalid("modules 不能为空数组；使用空对象表示全部模块");
    }
    if (type == PrivacyRequestType.CLOSE_ACCOUNT && !modules.isEmpty()) {
      throw invalid("账号注销必须覆盖全部模块");
    }
    return new PrivacyScope(modules);
  }

  /** 生成稳定 JSON，保证同一语义范围具有相同请求摘要。 */
  public String canonicalJson(PrivacyScope scope) {
    ObjectNode root = objectMapper.createObjectNode();
    if (!scope.isAllModules()) {
      ArrayNode modules = root.putArray("modules");
      new TreeSet<>(scope.modules()).forEach(modules::add);
    }
    try {
      return objectMapper.writeValueAsString(root);
    } catch (JsonProcessingException exception) {
      throw new IllegalStateException("隐私范围规范化失败", exception);
    }
  }

  public Set<String> supportedModules() {
    return SUPPORTED_MODULES;
  }

  private JsonNode readObject(String scopeJson) {
    try {
      JsonNode root = objectMapper.readTree(scopeJson);
      if (root == null || !root.isObject()) {
        throw invalid("隐私请求范围必须是 JSON 对象");
      }
      return root;
    } catch (JsonProcessingException exception) {
      throw invalid("隐私请求范围不是合法 JSON");
    }
  }

  private void rejectUnknownFields(JsonNode root) {
    Iterator<String> fields = root.fieldNames();
    while (fields.hasNext()) {
      String field = fields.next();
      if (!"modules".equals(field)) {
        throw invalid("不支持的隐私范围字段：" + field);
      }
    }
  }

  private BusinessException invalid(String message) {
    return new BusinessException("PRIVACY_INVALID_SCOPE", message);
  }
}
