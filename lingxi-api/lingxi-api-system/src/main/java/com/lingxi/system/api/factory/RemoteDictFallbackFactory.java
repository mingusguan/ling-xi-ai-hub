package com.lingxi.system.api.factory;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import com.lingxi.common.core.domain.R;
import com.lingxi.system.api.RemoteDictService;
import com.lingxi.system.api.domain.SysDictData;

/**
 * 字典服务降级处理
 * 
 * @author cloud
 */
@Component
public class RemoteDictFallbackFactory implements FallbackFactory<RemoteDictService>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteDictFallbackFactory.class);

    @Override
    public RemoteDictService create(Throwable throwable)
    {
        log.error("字典服务调用失败:{}", throwable.getMessage());
        return new RemoteDictService()
        {
            @Override
            public R<List<SysDictData>> getDictDataByType(String dictType, String source)
            {
                return R.fail("获取字典数据失败:" + throwable.getMessage());
            }
        };
    }
}
