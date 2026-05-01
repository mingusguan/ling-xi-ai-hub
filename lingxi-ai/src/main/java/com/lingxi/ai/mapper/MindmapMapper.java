package com.lingxi.ai.mapper;

import java.util.List;
import com.lingxi.ai.domain.Mindmap;

/**
 * 思维导图Mapper接口
 * 
 * @author cloud
 */
public interface MindmapMapper
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
     * 删除思维导图
     * 
     * @param mindmapId 思维导图主键
     * @return 结果
     */
    public int deleteMindmapById(Long mindmapId);

    /**
     * 批量删除思维导图
     * 
     * @param mindmapIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMindmapByIds(Long[] mindmapIds);
}
