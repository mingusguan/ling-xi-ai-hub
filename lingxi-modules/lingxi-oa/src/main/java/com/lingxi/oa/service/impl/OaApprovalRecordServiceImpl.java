package com.lingxi.oa.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.oa.domain.OaApprovalRecord;
import com.lingxi.oa.mapper.OaApprovalRecordMapper;
import com.lingxi.oa.service.IOaApprovalRecordService;
import com.lingxi.system.api.RemoteUserService;
import com.lingxi.system.api.domain.SysUser;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OaApprovalRecordServiceImpl implements IOaApprovalRecordService {

    private static final Logger log = LoggerFactory.getLogger(OaApprovalRecordServiceImpl.class);

    private final OaApprovalRecordMapper approvalRecordMapper;
    private final RemoteUserService remoteUserService;

    @Override
    public void saveRecord(OaApprovalRecord record) {
        record.setCreateTime(new Date());
        approvalRecordMapper.insert(record);
    }

    @Override
    public List<OaApprovalRecord> listByBusinessKey(String businessKey) {
        List<OaApprovalRecord> records = approvalRecordMapper.selectList(new LambdaQueryWrapper<OaApprovalRecord>()
                .eq(StringUtils.isNotBlank(businessKey), OaApprovalRecord::getBusinessKey, businessKey)
                .orderByAsc(OaApprovalRecord::getCreateTime));
        // 填充审批人姓名
        fillApproverName(records);
        return records;
    }

    @Override
    public List<OaApprovalRecord> listByProcessInstanceId(String processInstanceId) {
        List<OaApprovalRecord> records = approvalRecordMapper.selectList(new LambdaQueryWrapper<OaApprovalRecord>()
                .eq(StringUtils.isNotBlank(processInstanceId), OaApprovalRecord::getProcessInstanceId, processInstanceId)
                .orderByAsc(OaApprovalRecord::getCreateTime));
        // 填充审批人姓名
        fillApproverName(records);
        return records;
    }

    @Override
    public List<OaApprovalRecord> listByBusinessType(String businessType, int limit) {
        List<OaApprovalRecord> records = approvalRecordMapper.selectList(new LambdaQueryWrapper<OaApprovalRecord>()
                .eq(StringUtils.isNotBlank(businessType), OaApprovalRecord::getBusinessType, businessType)
                .eq(OaApprovalRecord::getActionType, "approve")
                .orderByDesc(OaApprovalRecord::getCreateTime)
                .last("LIMIT " + limit));
        // 填充审批人姓名
        fillApproverName(records);
        return records;
    }

    /**
     * 填充审批人姓名
     */
    private void fillApproverName(List<OaApprovalRecord> records) {
        if (records == null || records.isEmpty()) {
            return;
        }
        
        // 收集所有不重复的审批人ID
        List<Long> approverIds = records.stream()
                .map(OaApprovalRecord::getApproverId)
                .filter(id -> id != null)
                .distinct()
                .collect(Collectors.toList());
        
        if (approverIds.isEmpty()) {
            return;
        }
        
        // 批量查询用户信息
        Map<Long, String> userNameMap = approverIds.stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> {
                            try {
                                R<SysUser> result = remoteUserService.getUserById(id, SecurityConstants.INNER);
                                if (result != null && result.getData() != null) {
                                    return result.getData().getNickName();
                                }
                            } catch (Exception e) {
                                log.warn("查询用户信息失败, userId: {}", id, e);
                            }
                            return "未知用户";
                        }
                ));
        
        // 填充审批人姓名
        records.forEach(record -> {
            if (record.getApproverId() != null) {
                record.setApproverName(userNameMap.getOrDefault(record.getApproverId(), "未知用户"));
            }
        });
    }
}
