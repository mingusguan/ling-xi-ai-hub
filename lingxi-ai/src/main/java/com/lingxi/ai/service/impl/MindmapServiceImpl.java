package com.lingxi.ai.service.impl;

import java.util.List;
import java.util.UUID;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.lingxi.ai.agent.MindmapAgent;
import com.lingxi.common.security.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.lingxi.ai.mapper.MindmapMapper;
import com.lingxi.ai.domain.Mindmap;
import com.lingxi.ai.service.IMindmapService;

/**
 * 思维导图Service业务层处理
 * 
 * @author cloud
 */
@Service
public class MindmapServiceImpl implements IMindmapService
{
    private static final Logger log = LoggerFactory.getLogger(MindmapServiceImpl.class);

    @Autowired
    private MindmapMapper mindmapMapper;

    @Autowired
    private MindmapAgent mindmapAgent;

    /**
     * 查询思维导图
     * 
     * @param mindmapId 思维导图主键
     * @return 思维导图
     */
    @Override
    public Mindmap selectMindmapById(Long mindmapId)
    {
        return mindmapMapper.selectMindmapById(mindmapId);
    }

    /**
     * 查询思维导图列表
     * 
     * @param mindmap 思维导图
     * @return 思维导图
     */
    @Override
    public List<Mindmap> selectMindmapList(Mindmap mindmap)
    {
        return mindmapMapper.selectMindmapList(mindmap);
    }

    /**
     * 新增思维导图
     * 
     * @param mindmap 思维导图
     * @return 结果
     */
    @Override
    public int insertMindmap(Mindmap mindmap)
    {
        mindmap.setCreateBy(SecurityUtils.getUsername());
        return mindmapMapper.insertMindmap(mindmap);
    }

    /**
     * 修改思维导图
     * 
     * @param mindmap 思维导图
     * @return 结果
     */
    @Override
    public int updateMindmap(Mindmap mindmap)
    {
        mindmap.setUpdateBy(SecurityUtils.getUsername());
        return mindmapMapper.updateMindmap(mindmap);
    }

    /**
     * 批量删除思维导图
     * 
     * @param mindmapIds 需要删除的思维导图主键
     * @return 结果
     */
    @Override
    public int deleteMindmapByIds(Long[] mindmapIds)
    {
        return mindmapMapper.deleteMindmapByIds(mindmapIds);
    }

    /**
     * 删除思维导图信息
     * 
     * @param mindmapId 思维导图主键
     * @return 结果
     */
    @Override
    public int deleteMindmapById(Long mindmapId)
    {
        return mindmapMapper.deleteMindmapById(mindmapId);
    }

    /**
     * AI生成思维导图
     * 
     * @param prompt 用户提示词
     * @param layoutType 布局类型
     * @return 思维导图JSON数据
     */
    @Override
    public String generateMindmapByAI(String prompt, String layoutType)
    {
        try {
            log.info("开始调用AI生成思维导图，主题：{}，布局：{}", prompt, layoutType);
            
            // 调用LangChain4j Agent生成思维导图
            String aiResponse = mindmapAgent.generateMindmap(prompt, layoutType != null ? layoutType : "logical");
            
            log.info("AI返回原始响应：{}", aiResponse);
            
            // 验证并解析JSON
            JSONObject mindmapData = JSON.parseObject(aiResponse);
            
            // 确保必要字段存在
            if (!mindmapData.containsKey("id")) {
                mindmapData.put("id", UUID.randomUUID().toString());
            }
            if (!mindmapData.containsKey("topic")) {
                mindmapData.put("topic", prompt.length() > 20 ? prompt.substring(0, 20) : prompt);
            }
            if (!mindmapData.containsKey("layout")) {
                mindmapData.put("layout", layoutType != null ? layoutType : "logical");
            }
            
            log.info("思维导图生成成功");
            return mindmapData.toJSONString();
            
        } catch (Exception e) {
            log.error("AI生成思维导图失败，使用降级方案", e);
            // 降级方案：返回基础结构
            return generateFallbackMindmap(prompt, layoutType);
        }
    }

    /**
     * 降级方案：生成基础思维导图结构
     */
    private String generateFallbackMindmap(String prompt, String layoutType) {
        JSONObject root = new JSONObject();
        root.put("id", UUID.randomUUID().toString());
        root.put("topic", prompt.length() > 20 ? prompt.substring(0, 20) : prompt);
        root.put("layout", layoutType != null ? layoutType : "logical");
        
        // 示例子节点
        JSONObject[] children = new JSONObject[3];
        
        for (int i = 0; i < 3; i++) {
            children[i] = new JSONObject();
            children[i].put("id", UUID.randomUUID().toString());
            children[i].put("topic", "分支" + (i + 1));
            
            // 每个分支添加2个子节点
            JSONObject[] subChildren = new JSONObject[2];
            for (int j = 0; j < 2; j++) {
                subChildren[j] = new JSONObject();
                subChildren[j].put("id", UUID.randomUUID().toString());
                subChildren[j].put("topic", "子节点" + (j + 1));
            }
            children[i].put("children", subChildren);
        }
        
        root.put("children", children);
        
        return root.toJSONString();
    }
}
