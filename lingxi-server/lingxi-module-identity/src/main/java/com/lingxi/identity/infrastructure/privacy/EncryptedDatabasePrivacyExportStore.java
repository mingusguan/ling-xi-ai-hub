package com.lingxi.identity.infrastructure.privacy;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.lingxi.identity.domain.PrivacyExportStore;
import com.lingxi.identity.infrastructure.persistence.*;
import com.lingxi.kernel.BusinessException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 使用 AES-256-GCM 保存短期隐私导出包，数据库不落敏感明文。 */
@Component
public class EncryptedDatabasePrivacyExportStore implements PrivacyExportStore {
  private static final Duration TTL = Duration.ofDays(7);
  private final PrivacyExportMapper mapper;
  private final byte[] key;
  private final SecureRandom random = new SecureRandom();

  public EncryptedDatabasePrivacyExportStore(
      PrivacyExportMapper mapper,
      @Value("${lingxi.identity.privacy-export-key}") String encodedKey) {
    this.mapper = mapper;
    try {
      key = Base64.getDecoder().decode(encodedKey);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException("隐私导出密钥必须为 Base64", e);
    }
    if (key.length != 32) {
      throw new IllegalStateException("隐私导出密钥必须为 32 字节");
    }
  }

  @Override
  public String store(long requestId, long userId, String json) {
    try {
      byte[] nonce = new byte[12];
      random.nextBytes(nonce);
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, nonce));
      cipher.updateAAD((requestId + ":" + userId).getBytes(StandardCharsets.UTF_8));
      PrivacyExportEntity e = new PrivacyExportEntity();
      e.setRequestId(requestId);
      e.setUserId(userId);
      e.setNonce(nonce);
      e.setCipherText(cipher.doFinal(json.getBytes(StandardCharsets.UTF_8)));
      e.setCreatedAt(now());
      e.setExpiresAt(now().plus(TTL));
      if (mapper.upsert(e) <= 0) {
        throw new IllegalStateException("隐私导出包保存失败");
      }
      return "/api/v1/privacy/exports/" + requestId;
    } catch (Exception e) {
      throw new IllegalStateException("隐私导出加密失败", e);
    }
  }

  @Override
  public String load(long requestId, long userId) {
    PrivacyExportEntity e =
        mapper.selectOne(
            Wrappers.<PrivacyExportEntity>lambdaQuery()
                .eq(PrivacyExportEntity::getRequestId, requestId)
                .eq(PrivacyExportEntity::getUserId, userId)
                .last("LIMIT 1"));
    if (e == null || !e.getExpiresAt().isAfter(now())) {
      throw new BusinessException("PRIVACY_EXPORT_EXPIRED", "隐私导出包不存在或已过期");
    }
    try {
      Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
      cipher.init(
          Cipher.DECRYPT_MODE,
          new SecretKeySpec(key, "AES"),
          new GCMParameterSpec(128, e.getNonce()));
      cipher.updateAAD((requestId + ":" + userId).getBytes(StandardCharsets.UTF_8));
      return new String(cipher.doFinal(e.getCipherText()), StandardCharsets.UTF_8);
    } catch (Exception ex) {
      throw new IllegalStateException("隐私导出解密失败", ex);
    }
  }

  private LocalDateTime now() {
    return LocalDateTime.now(ZoneOffset.UTC);
  }
}
