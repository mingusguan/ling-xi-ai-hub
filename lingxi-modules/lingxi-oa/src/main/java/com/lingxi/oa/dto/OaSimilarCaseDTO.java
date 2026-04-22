package com.lingxi.oa.dto;

import java.io.Serializable;
import java.util.Date;

public class OaSimilarCaseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private String applicant;

    private String dept;

    private String type;

    private String value;

    private Date applyTime;

    private String approvalResult;

    private String approverName;

    private String approverOpinion;

    private String businessType;

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public String getDept() {
        return dept;
    }

    public void setDept(String dept) {
        this.dept = dept;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Date getApplyTime() {
        return applyTime;
    }

    public void setApplyTime(Date applyTime) {
        this.applyTime = applyTime;
    }

    public String getApprovalResult() {
        return approvalResult;
    }

    public void setApprovalResult(String approvalResult) {
        this.approvalResult = approvalResult;
    }

    public String getApproverName() {
        return approverName;
    }

    public void setApproverName(String approverName) {
        this.approverName = approverName;
    }

    public String getApproverOpinion() {
        return approverOpinion;
    }

    public void setApproverOpinion(String approverOpinion) {
        this.approverOpinion = approverOpinion;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
}
