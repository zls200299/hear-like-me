package com.zhs.service.impl;


import com.zhs.model.ReadAloudCategory;
import com.zhs.model.ReadAloudItem;
import com.zhs.dao.ReadAloudCategoryDao;
import com.zhs.dao.ReadAloudItemDao;
import com.zhs.service.IReadAloudCategoryService;
import com.zhs.dto.ReadAloudCategoryDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import jakarta.annotation.Resource;


/**
 *
 * @author 
 * @since 2026-08-28
 */
@Service
@Slf4j
public class ReadAloudCategoryServiceImpl extends ServiceImpl< ReadAloudCategoryDao, ReadAloudCategory> implements IReadAloudCategoryService {

    @Resource
    private ReadAloudCategoryDao readAloudCategoryDao;

    @Resource
    private ReadAloudItemDao readAloudItemDao;

    @Override
    public R addOrUpdate(ReadAloudCategoryDto readAloudCategoryDto) {
        if (BeanUtil.isEmpty(readAloudCategoryDto)) throw new ServiceException("数据不能为空");
        if (StringUtils.isBlank(readAloudCategoryDto.getCategoryCode())) {
            throw new ServiceException("分类编码不能为空");
        }
        if (StringUtils.isBlank(readAloudCategoryDto.getNameCn())) {
            throw new ServiceException("中文名不能为空");
        }
        ReadAloudCategory readAloudCategory = new ReadAloudCategory();
        BeanUtil.copyProperties(readAloudCategoryDto, readAloudCategory);
        if (readAloudCategory.getSortOrder() == null) {
            readAloudCategory.setSortOrder(0);
        }
        if (readAloudCategory.getEnabled() == null) {
            readAloudCategory.setEnabled(1);
        }
        if (readAloudCategoryDto.getId() == null){
            readAloudCategory.setIsDelete(0);
            readAloudCategoryDao.insert(readAloudCategory);
            return R.ok("数据插入成功");
        }else {
            readAloudCategoryDao.updateById(readAloudCategory);
            return R.ok("数据更新成功");
        }
    }

    @Override
    public R deleteByIdSafe(String id) {
        if (StringUtils.isBlank(id)) throw new ServiceException("id不能为空");
        LambdaQueryWrapper<ReadAloudCategory> query = new QueryWrapper<ReadAloudCategory>().lambda()
                .eq(ReadAloudCategory::getId, id)
                .eq(ReadAloudCategory::getIsDelete, 0);
        ReadAloudCategory readAloudCategory = readAloudCategoryDao.selectOne(query);
        if (ObjectUtils.isEmpty(readAloudCategory)) {
            throw new ServiceException("该数据不存在或者已经被删除");
        }
        Long itemCount = readAloudItemDao.selectCount(new QueryWrapper<ReadAloudItem>().lambda()
                .eq(ReadAloudItem::getCategoryId, readAloudCategory.getId())
                .eq(ReadAloudItem::getIsDelete, 0));
        if (itemCount != null && itemCount > 0) {
            throw new ServiceException("该分类下仍有点读卡片，请先删除或挪走后再删分类");
        }
        readAloudCategory.setIsDelete(1);
        readAloudCategoryDao.updateById(readAloudCategory);
        return R.ok("数据删除成功");
    }
}
