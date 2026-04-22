package com.lingxi.system.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.lingxi.common.core.annotation.Excel;
import com.lingxi.common.core.web.domain.BaseEntity;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.util.Date;

/**
 * 系统消息实体
 *
 * @author lingxi
 */
public class SysMessage extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 消息ID */
    private Long messageId;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 用户名称 */
    @Excel(name = "用户名称")
    private String userName;

    /** 消息来源（oa:OA系统,system:系统通知,knowledge:知识库,ai:AI助手） */
    @Excel(name = "消息来源")
    private String sourceType;

    /** 消息类型（timeout:超时,approval:待审批,complete:流程完成,cc:抄送,notice:通知等） */
    @Excel(name = "消息类型")
    private String messageType;

    /** 消息标题 */
    @Excel(name = "消息标题")
    private String title;

    /** 消息内容 */
    private String content;

    /** 业务类型 */
    private String businessType;

    /** 业务ID */
    private String businessId;

    /** 流程实例ID */
    private String processInstanceId;

    /** 任务ID */
    private String taskId;

    /** 优先级（1:低,2:中,3:高） */
    private Integer priority;

    /** 渠道（system:系统,wechat:微信,sms:短信） */
    private String channel;

    /** 状态（0:未读,1:已读） */
    private String status;

    /** 发送时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "发送时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    /** 阅读时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date readTime;

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public String getBusinessId() {
        return businessId;
    }

    public void setProcessInstanceId(String processInstanceId) {
        this.processInstanceId = processInstanceId;
    }

    public String getProcessInstanceId() {
        return processInstanceId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getChannel() {
        return channel;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setReadTime(Date readTime) {
        this.readTime = readTime;
    }

    public Date getReadTime() {
        return readTime;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("messageId", getMessageId())
                .append("userId", getUserId())
                .append("userName", getUserName())
                .append("sourceType", getSourceType())
                .append("messageType", getMessageType())
                .append("title", getTitle())
                .append("content", getContent())
                .append("businessType", getBusinessType())
                .append("businessId", getBusinessId())
                .append("processInstanceId", getProcessInstanceId())
                .append("taskId", getTaskId())
                .append("priority", getPriority())
                .append("channel", getChannel())
                .append("status", getStatus())
                .append("sendTime", getSendTime())
                .append("readTime", getReadTime())
                .toString();
    }
}
