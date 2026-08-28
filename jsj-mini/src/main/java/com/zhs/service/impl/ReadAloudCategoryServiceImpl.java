package com.zhs.service.impl;


import com.zhs.model.ReadAloudCategory;
import com.zhs.dao.ReadAloudCategoryDao;
import com.zhs.service.IReadAloudCategoryService;
import com.zhs.dto.ReadAloudCategoryDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;
import java.util.Date;


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

    @Override
    public R addOrUpdate(ReadAloudCategoryDto readAloudCategoryDto) {
        if (BeanUtil.isEmpty(readAloudCategoryDto)) throw new ServiceException("数据不能为空");
        ReadAloudCategory readAloudCategory = new ReadAloudCategory();
        BeanUtil.copyProperties(readAloudCategoryDto,readAloudCategory);
        if (readAloudCategoryDto.getId() == null){
            readAloudCategoryDao.insert(readAloudCategory);
            return R.ok("数据插入成功");
        }else {
            readAloudCategoryDao.updateById(readAloudCategory);
            return R.ok("数据更新成功");
        }
    }
}