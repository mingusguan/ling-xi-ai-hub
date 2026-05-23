package com.lingxi.knowledge.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识库问答请求。
 */
@Data
public class KbQaChatRequest {

    /** 用户问题。 */
    @NotBlank(message = "问题不能为空")
    private String question;

    /** 用户ID，前端可不传，优先使用登录用户。 */
    private Long userId;

    /** 部门ID，前端可不传，优先使用登录用户部门。 */
    private Long deptId;

    /** 会话ID，空值表示自动创建会话。 */
    private Long sessionId;
}
