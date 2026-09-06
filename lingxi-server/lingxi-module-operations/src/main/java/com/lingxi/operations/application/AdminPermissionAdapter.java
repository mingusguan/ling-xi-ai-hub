package com.lingxi.operations.application;

public interface AdminPermissionAdapter {
  boolean allowed(long adminId, String permission);
}
