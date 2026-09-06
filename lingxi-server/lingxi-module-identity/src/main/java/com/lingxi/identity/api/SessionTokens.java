package com.lingxi.identity.api;

import java.time.Instant;

/** 登录或刷新成功后返回的 Opaque Token。 */
public record SessionTokens(
    String accessToken,
    String refreshToken,
    String sessionFamilyId,
    Instant accessExpiresAt,
    Instant refreshExpiresAt,
    AgeBand ageBand,
    AccountStatus accountStatus,
    long authorizationVersion) {}
