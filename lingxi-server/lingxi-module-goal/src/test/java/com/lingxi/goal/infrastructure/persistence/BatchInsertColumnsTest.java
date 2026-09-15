package com.lingxi.goal.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * 批量写入语句的列清单完整性校验。
 *
 * <p>回归背景：这两个批量写入都显式列出列名。行动添加新字段时若只改实体与迁移、
 * 忘了同步列清单，写入会静默丢字段，直到读回时才以业务错误的形式暴露；
 * 单元测试用 mock 仓储，看不到这类问题。因此这里做静态一致性校验，
 * 实体新增字段而未同步列清单时直接失败。
 *
 * <p>实例表还额外依赖 {@code upsertBatch} 的 upsert 语义：它参与逻辑删除，
 * 唯一键是 (action_id, scheduled_at)，用 INSERT IGNORE 会让规则变更后无法重建实例。
 */
class BatchInsertColumnsTest {
  /** 逻辑删除标记由建表默认值补齐，不参与批量插入的列清单。 */
  private static final Set<String> COLUMNS_NOT_WRITTEN = Set.of("deleted");

  @Test
  void actionInsertBatchCoversEveryPersistedField() throws Exception {
    assertColumnsCoverEntity("mapper/ActionMapper.xml", "goal_action", ActionEntity.class);
  }

  @Test
  void occurrenceUpsertBatchCoversEveryPersistedField() throws Exception {
    assertColumnsCoverEntity(
        "mapper/OccurrenceMapper.xml", "goal_action_occurrence", OccurrenceEntity.class);
  }

  /** 实例重建必须使用 upsert：INSERT IGNORE 会被逻辑删除残留行挡住，导致规则改了却生成不出实例。 */
  @Test
  void occurrenceRebuildMustReviveLogicallyDeletedRows() throws Exception {
    String xml = read("mapper/OccurrenceMapper.xml");
    assertThat(xml).contains("ON DUPLICATE KEY UPDATE");
    assertThat(xml).contains("deleted = IF(status = 'SCHEDULED', 0, deleted)");
    assertThat(xml).doesNotContain("INSERT IGNORE");
  }

  /** 插入列清单不应残留已经不存在的实体字段，避免 INSERT 报未知列。 */
  @Test
  void insertColumnsDoNotReferenceUnknownFields() throws Exception {
    assertNoUnknownColumns("mapper/ActionMapper.xml", "goal_action", ActionEntity.class);
    assertNoUnknownColumns(
        "mapper/OccurrenceMapper.xml", "goal_action_occurrence", OccurrenceEntity.class);
  }

  private void assertColumnsCoverEntity(
      String resource, String table, Class<?> entityType) throws Exception {
    Set<String> columns = insertColumns(resource, table);
    List<String> missing = new ArrayList<>();
    for (Field field : entityType.getDeclaredFields()) {
      if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      String column = toSnakeCase(field.getName());
      if (COLUMNS_NOT_WRITTEN.contains(column)) {
        continue;
      }
      if (!columns.contains(column)) {
        missing.add(field.getName() + " -> " + column);
      }
    }
    assertThat(missing)
        .as("%s 的字段未出现在 %s 插入列清单中，写入会静默丢字段", entityType.getSimpleName(), table)
        .isEmpty();
  }

  private void assertNoUnknownColumns(String resource, String table, Class<?> entityType)
      throws Exception {
    Set<String> entityColumns = new LinkedHashSet<>();
    for (Field field : entityType.getDeclaredFields()) {
      if (field.isSynthetic() || java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
        continue;
      }
      entityColumns.add(toSnakeCase(field.getName()));
    }
    List<String> unknown =
        insertColumns(resource, table).stream().filter(c -> !entityColumns.contains(c)).toList();
    assertThat(unknown).as("%s 的插入列清单引用了实体上不存在的字段", table).isEmpty();
  }

  private Set<String> insertColumns(String resource, String table) throws Exception {
    Matcher matcher =
        Pattern.compile(
                "INSERT\\s+(?:IGNORE\\s+)?INTO\\s+" + table + "\\s*\\(([^)]*)\\)",
                Pattern.CASE_INSENSITIVE)
            .matcher(read(resource));
    assertThat(matcher.find()).as("未找到 %s 的插入列清单", table).isTrue();
    Set<String> columns = new LinkedHashSet<>();
    for (String raw : matcher.group(1).split(",")) {
      String column = raw.trim().toLowerCase();
      if (!column.isEmpty()) {
        columns.add(column);
      }
    }
    return columns;
  }

  /**
   * 读取 mapper 文件并剥离 XML 注释。
   *
   * <p>必须剥离：注释里会引用语句名与列名做说明，直接匹配整份文件会把注释当语句，
   * 既可能误报（例如注释里提到 INSERT IGNORE），也可能漏报。
   */
  private String read(String resource) throws Exception {
    try (InputStream in = getClass().getClassLoader().getResourceAsStream(resource)) {
      assertThat(in).as("%s 必须在 classpath 上", resource).isNotNull();
      String raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      return raw.replaceAll("(?s)<!--.*?-->", "");
    }
  }

  private static String toSnakeCase(String fieldName) {
    StringBuilder sb = new StringBuilder();
    for (char c : fieldName.toCharArray()) {
      if (Character.isUpperCase(c)) {
        sb.append('_').append(Character.toLowerCase(c));
      } else {
        sb.append(c);
      }
    }
    return sb.toString();
  }
}
