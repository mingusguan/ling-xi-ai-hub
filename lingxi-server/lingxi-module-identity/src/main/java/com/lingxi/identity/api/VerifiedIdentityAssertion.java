package com.lingxi.identity.api;

import java.time.Instant;
import java.time.LocalDate;

/** 经受信适配器签名的身份断言。 */
public record VerifiedIdentityAssertion(
    String channel,
    String subjectHash,
    LocalDate verifiedBirthDate,
    String verificationMethod,
    String evidenceReference,
    Instant issuedAt) {}
