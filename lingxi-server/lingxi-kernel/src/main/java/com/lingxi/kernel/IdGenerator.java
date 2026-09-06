package com.lingxi.kernel;

/** 应用生成趋势递增数据库标识与不可枚举公开标识的端口。 */
public interface IdGenerator {

  long nextId();

  String nextPublicId();

  String nextEventId();
}
