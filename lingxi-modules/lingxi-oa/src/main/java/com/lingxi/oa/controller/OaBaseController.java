package com.lingxi.oa.controller;

import com.lingxi.common.core.constant.HttpStatus;
import com.lingxi.common.core.web.controller.BaseController;
import com.lingxi.common.core.web.page.TableDataInfo;

import java.util.Collections;
import java.util.List;

/**
 * OA模块基础控制器
 * 提供通用的分页构建方法
 *
 * @author lingxi
 */
public class OaBaseController extends BaseController {

    /**
     * 构建分页数据
     * 对内存中的列表数据进行分页处理
     *
     * @param list 原始数据列表
     * @param pageNum 页码，从1开始
     * @param pageSize 每页大小
     * @param <T> 数据类型
     * @return 分页数据对象
     */
    protected <T> TableDataInfo buildPage(List<T> list, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : pageSize;
        int fromIndex = Math.min((currentPage - 1) * currentSize, list.size());
        int toIndex = Math.min(fromIndex + currentSize, list.size());

        TableDataInfo tableDataInfo = new TableDataInfo();
        tableDataInfo.setCode(HttpStatus.SUCCESS);
        tableDataInfo.setMsg("查询成功");
        tableDataInfo.setRows(fromIndex >= toIndex ? Collections.emptyList() : list.subList(fromIndex, toIndex));
        tableDataInfo.setTotal(list.size());
        return tableDataInfo;
    }
}
