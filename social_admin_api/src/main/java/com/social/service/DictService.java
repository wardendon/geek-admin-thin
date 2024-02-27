package com.social.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.social.common.result.PageResult;
import com.social.dto.DictRequestDTO;
import com.social.entity.Dict;
import com.social.query.DictQuery;

import java.util.List;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author ycshang
 * @since 2023-11-13
 */
public interface DictService extends IService<Dict> {
    /**
     * 字典列表 -分页
     *
     * @param query
     * @return
     */
    PageResult<Dict> getPage(DictQuery query);

    void refreshTransCache();

    /**
     * 新增字典
     *
     * @param dto
     */
    void add(DictRequestDTO dto);

    /**
     * 修改字典
     *
     * @param dto
     */
    void edit(DictRequestDTO dto);

    /**
     * 删除字典
     *
     * @param ids
     */
    void remove(List<Integer> ids);

    /**
     * 根据字典编号查询
     * @param number
     * @return
     */
    boolean existByNumber(String number);

}
