package com.lingxi.system.api;

import java.util.List;

import com.lingxi.common.core.constant.ServiceNameConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import com.lingxi.common.core.constant.SecurityConstants;
import com.lingxi.common.core.domain.R;
import com.lingxi.system.api.domain.SysDictData;
import com.lingxi.system.api.factory.RemoteDictFallbackFactory;

/**
 * 字典服务
 * 
 * @author cloud
 */
@FeignClient(contextId = "remoteDictService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteDictFallbackFactory.class)
public interface RemoteDictService
{
    /**
     * 根据字典类型查询字典数据信息
     *
     * @param dictType 字典类型
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/dict/data/inner/type/{dictType}")
    public R<List<SysDictData>> getDictDataByType(@PathVariable("dictType") String dictType, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
