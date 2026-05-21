package com.lingxi.system.api.local;

import com.lingxi.common.core.domain.R;
import com.lingxi.common.core.utils.StringUtils;
import com.lingxi.system.api.RemoteDictService;
import com.lingxi.system.api.domain.SysDictData;
import com.lingxi.system.service.ISysDictTypeService;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 单体模式下的本地字典服务调用。
 */
@Service
public class LocalRemoteDictService implements RemoteDictService {

    private final ISysDictTypeService dictTypeService;

    public LocalRemoteDictService(ISysDictTypeService dictTypeService) {
        this.dictTypeService = dictTypeService;
    }

    @Override
    public R<List<SysDictData>> getDictDataByType(String dictType, String source) {
        List<SysDictData> data = dictTypeService.selectDictDataByType(dictType);
        if (StringUtils.isNull(data)) {
            data = new ArrayList<>();
        }
        return R.ok(data);
    }
}
