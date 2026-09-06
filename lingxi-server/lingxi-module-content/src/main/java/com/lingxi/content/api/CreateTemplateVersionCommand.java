package com.lingxi.content.api;

public record CreateTemplateVersionCommand(
    String templateKey,
    String name,
    int versionNo,
    String ageScope,
    String contentSnapshot,
    long operatorUserId) {}
