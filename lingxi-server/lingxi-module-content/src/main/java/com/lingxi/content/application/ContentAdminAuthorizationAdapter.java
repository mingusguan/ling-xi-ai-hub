package com.lingxi.content.application;

/** 内容管理权限适配端口。未配置实现时，内容模块按失败关闭处理管理命令。 */
public interface ContentAdminAuthorizationAdapter {
  boolean allowed(long adminId, String permission);
}
