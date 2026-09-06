package com.lingxi.identity.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lingxi.kernel.BusinessException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/** 对各模块导出结果执行最终字段白名单，防止实体新增内部字段后被意外带入导出包。 */
@Component
public class PrivacyExportSanitizer {
  private static final Map<String, Set<String>> ROOT_FIELDS =
      Map.of(
          "identity", Set.of("profile", "devices", "consents", "ageVerifications"),
          "goal", Set.of("goals", "plans", "actions", "milestones", "occurrences", "checkIns", "reviews"),
          "companion", Set.of("conversations", "messages", "runs", "runEvents", "proposals", "memories", "safetyEvents"),
          "engagement", Set.of("calendars", "calendarEvents", "preferences", "notifications", "deliveries", "inbox", "offlineCommands", "syncChanges"),
          "relationship", Set.of("guardianRelations", "guardianPermissions", "partnerRelations", "partnerGrants", "partnerInteractions", "shares", "blocks", "reports"),
          "content", Set.of("files", "references", "derivatives", "imports", "exports"),
          "commerce", Set.of("orders", "orderItems", "transactions", "refunds", "subscriptions", "entitlements", "entitlementLedger"),
          "operations", Set.of("supportTickets"));

  private static final Set<String> DATA_FIELDS =
      Set.of(
          "id", "publicId", "userId", "ownerUserId", "teenUserId", "guardianUserId",
          "inviterUserId", "inviteeUserId", "blockerUserId", "blockedUserId", "reporterUserId",
          "recipientUserId", "conversationId", "runId", "relationId", "goalId", "planVersionId",
          "actionId", "occurrenceId", "bindingId", "taskId", "fileId", "orderId", "productId",
          "priceId", "title", "name", "subject", "description", "scene", "purpose", "category",
          "priority", "status", "role", "contentText", "resultText", "safePayloadJson", "argumentsJson",
          "resultJson", "snapshotJson", "fieldsJson", "requestedPermissionsJson", "permission",
          "resourceType", "resourceId", "resourceKey", "sourceType", "sourceId", "sourceRef",
          "toolName", "riskType", "riskLevel", "disclosureScope", "decision", "sensitivity",
          "provider", "deleteCreatedEvents", "channel", "transactionId", "refundTransactionId",
          "channelSubscriptionId", "cancelMode", "currency", "amountMinor", "refundedMinor", "quantity",
          "delta", "balance", "balanceAfter", "visitLimit", "visitCount", "sequenceNo", "attemptNo",
          "eventType", "eventCursor", "commandType", "payloadJson", "dedupeKey", "deliveryChannel",
          "originalName", "sizeBytes", "mimeType", "derivativeType", "progress", "errorCode",
          "ageBand", "accountStatus", "adultTransitionDate", "timezone", "deviceId", "method", "result",
          "documentVersion", "granted", "verifiedBirthDate", "periodKey", "periodEnd", "frequency",
          "timezoneId", "scheduledAt", "effectiveAt", "expiresAt", "verifiedAt", "revokedAt",
          "firstSeenAt", "lastSeenAt", "recordedAt", "deliveredAt", "handledAt", "createdAt", "updatedAt",
          "modelVersion", "promptVersion", "aiGenerated", "reason", "revokeReason", "assigneeAdminId",
          "ticketNo", "type", "summary", "notes", "score", "targetValue", "currentValue", "unit");

  public JsonNode sanitize(String moduleName, JsonNode source) {
    Set<String> rootFields = ROOT_FIELDS.get(moduleName);
    if (rootFields == null || source == null || !source.isObject()) {
      throw new BusinessException("PRIVACY_EXPORT_SCHEMA_INVALID", "隐私导出模块或数据结构不受支持");
    }
    JsonNode copy = source.deepCopy();
    filterObject((ObjectNode) copy, rootFields, true);
    return copy;
  }

  private void filterObject(ObjectNode object, Set<String> allowed, boolean root) {
    Iterator<String> names = object.fieldNames();
    while (names.hasNext()) {
      String name = names.next();
      if (!allowed.contains(name)) {
        names.remove();
      }
    }
    object.elements().forEachRemaining(value -> filterChildren(value));
  }

  private void filterChildren(JsonNode node) {
    if (node instanceof ObjectNode object) {
      filterObject(object, DATA_FIELDS, false);
    } else if (node instanceof ArrayNode array) {
      array.elements().forEachRemaining(this::filterChildren);
    }
  }
}
