package com.lingxi.knowledge.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("kb_document")
public class KbDocument {

    @TableId(type = IdType.AUTO)
    private Long docId;          // 文档ID
    private String docName;      // 文档名称
    private String docType;      // 文档类型（文件扩展名，如 docx, pdf, xlsx）
    private String fileUrl;      // MinIO 存储地址
    private Long fileSize;       // 文件大小
    private String visibleDeptIds; // 可见部门ID列表，逗号分隔
    private Integer status;      // 0 待解析 1 解析中 2 已入库 3 失败
    private Long categoryId; // 所属分类 ID
    private String docSummary;   // 文档摘要
    private Long createUser;     // 创建人

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDelete;
}
