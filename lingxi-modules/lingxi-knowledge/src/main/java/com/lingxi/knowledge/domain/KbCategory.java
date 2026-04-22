package com.lingxi.knowledge.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("kb_category")
public class KbCategory {

    @TableId(type = IdType.AUTO)
    private Long categoryId;
    private String categoryName;
    private Long parentId;
    private Long deptId;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}
