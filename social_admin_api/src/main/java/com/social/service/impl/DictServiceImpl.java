package com.social.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.fhs.trans.service.impl.DictionaryTransService;
import com.social.common.exception.ServerException;
import com.social.common.result.PageResult;
import com.social.mapper.DictConfigMapper;
import com.social.convert.DictConvert;
import com.social.dto.DictRequestDTO;
import com.social.entity.Dict;
import com.social.mapper.DictMapper;
import com.social.entity.DictConfig;
import com.social.query.DictQuery;
import com.social.service.DictService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author ycshang
 * @since 2023-11-13
 */
@Service
@AllArgsConstructor
public class DictServiceImpl extends ServiceImpl<DictMapper, Dict> implements DictService {
    private final DictConfigMapper dictConfigMapper;
    private final DictionaryTransService dictionaryTransService;

    @Override
    public PageResult<Dict> getPage(DictQuery query) {
        Page<Dict> page = new Page<>(query.getPage(), query.getLimit());
        List<Dict> result = baseMapper.getPage(page, query);
        return new PageResult<>(result, page.getTotal());
    }

    @Override
    public void refreshTransCache() {
//        异步不阻塞主线程，不会增加启动用时
        CompletableFuture.supplyAsync(() -> {
//           获取所有的字典项数据
            List<DictConfig> dataList = dictConfigMapper.selectList(null);

//            根据类型分组
            Map<String, List<DictConfig>> dictTypeDataMap = dataList.stream().collect(Collectors.groupingBy(DictConfig::getDictNumber));
            List<Dict> dicts = super.list();
            for (Dict dictTypeEntity : dicts) {
                if (dictTypeDataMap.containsKey(dictTypeEntity.getNumber())) {
                    dictionaryTransService.refreshCache(dictTypeEntity.getNumber(), dictTypeDataMap.get(dictTypeEntity.getNumber())
                            .stream().collect(Collectors.toMap(DictConfig::getValue, DictConfig::getTitle)));
                }
            }
            return null;
        });

    }

    @Override
    public void add(DictRequestDTO dto) {
        Dict dict = DictConvert.INSTANCE.convertToDict(dto);
        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dict::getTitle, dict.getTitle());
        if (baseMapper.exists(wrapper)) {
            throw new ServerException("字典名称已存在");
        }
        wrapper.clear();
        wrapper.eq(Dict::getNumber, dict.getNumber());
        if (baseMapper.exists(wrapper)) {
            throw new ServerException("字典编号已存在");
        }
        baseMapper.insert(dict);
        refreshTransCache();
    }

    @Override
    public void edit(DictRequestDTO dto) {
        Dict updateDict = DictConvert.INSTANCE.convertToDict(dto);
        Dict dict = baseMapper.selectById(updateDict.getPkId());
        if (dict == null) {
            throw new ServerException("字典不存在");
        }
        LambdaQueryWrapper<Dict> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Dict::getTitle, dict.getTitle()).ne(Dict::getPkId, dict.getPkId());
        if (baseMapper.exists(wrapper)) {
            throw new ServerException("字典名称已存在");
        }
        wrapper.clear();
        wrapper.eq(Dict::getNumber, dict.getNumber()).ne(Dict::getPkId, dict.getPkId());
        if (baseMapper.exists(wrapper)) {
            throw new ServerException("字典编号已存在");
        }

//        如果修改了字典编码,字典配置选项的编码都需要修改
        if (!dict.getNumber().equals(updateDict.getNumber())) {
            List<DictConfig> dictConfigs = dictConfigMapper.selectList(new LambdaQueryWrapper<DictConfig>().eq(DictConfig::getDictNumber, dict.getNumber()));
            dictConfigs.forEach(item -> {
                item.setDictNumber(updateDict.getNumber());
                dictConfigMapper.updateById(item);
            });
        }
        updateById(dict);
        refreshTransCache();

    }

    @Override
    public void remove(List<Integer> ids) {
        List<Dict> dicts = baseMapper.selectList(new LambdaQueryWrapper<Dict>().in(Dict::getPkId, ids));
        List<String> numbers = dicts.stream().map(Dict::getNumber).collect(Collectors.toList());
//       同步删除字典配置的子项
        dictConfigMapper.delete(new LambdaQueryWrapper<DictConfig>().in(DictConfig::getDictNumber, numbers));
        removeByIds(ids);

    }

    @Override
    public boolean existByNumber(String number) {
        LambdaQueryWrapper<Dict> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Dict::getNumber, number);
        return baseMapper.exists(queryWrapper);
    }
}
