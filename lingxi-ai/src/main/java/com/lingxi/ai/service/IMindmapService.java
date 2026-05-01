package com.lingxi.ai.service;

import java.util.List;
import com.lingxi.ai.domain.Mindmap;

/**
 * 思维导图Service接口
 * 
 * @author cloud
 */
public interface IMindmapService
{
    /**
     * 查询思维导图
     * 
     * @param mindmapId 思维导图主键
     * @return 思维导图
     */
    public Mindmap selectMindmapById(Long mindmapId);

    /**
     * 查询思维导图列表
     * 
     * @param mindmap 思维导图
     * @return 思维导图集合
     */
    public List<Mindmap> selectMindmapList(Mindmap mindmap);

    /**
     * 新增思维导图
     * 
     * @param mindmap 思维导图
     * @return 结果
     */
    public int insertMindmap(Mindmap mindmap);

    /**
     * 修改思维导图
     * 
     * @param mindmap 思维导图
     * @return 结果
     */
    public int updateMindmap(Mindmap mindmap);

    /**
     * 批量删除思维导图
     * 
     * @param mindmapIds 需要删除的思维导图主键集合
     * @return 结果
     */
    public int deleteMindmapByIds(Long[] mindmapIds);

    /**
     * 删除思维导图信息
     * 
     * @param mindmapId 思维导图主键
     * @return 结果
     */
    public int deleteMindmapById(Long mindmapId);

    /**
     * AI生成思维导图
     * 
     * @param prompt 用户提示词
     * @param layoutType 布局类型
     * @return 思维导图JSON数据
     */
    public String generateMindmapByAI(String prompt, String layoutType);
}
