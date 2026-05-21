package com.lingxi.knowledge.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("kb_document_chunk")
public class KbDocumentChunk {

    @TableId(type = IdType.AUTO)
    private Long chunkId;        // 分块ID
    private Long docId;          // 关联文档ID
    private String chunkContent; // 分块内容
    private Integer chunkIndex;  // 分块序号
    private Integer embedStatus; // 0未向量化 1已向量化
    @TableField("milvus_id")
    private String vectorId;     // 向量存储ID

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
