package com.lingxi.identity.api;

/** 记录不可覆盖的协议同意或撤回证据。 */
public record RecordConsentCommand(
    long userId,
    ConsentPurpose purpose,
    String documentVersion,
    boolean granted,
    String evidenceReference) {}
