package com.zhs.service.impl;


import com.zhs.model.ReadAloudItem;
import com.zhs.dao.ReadAloudItemDao;
import com.zhs.service.IReadAloudItemService;
import com.zhs.dto.ReadAloudItemDto;


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
public class ReadAloudItemServiceImpl extends ServiceImpl< ReadAloudItemDao, ReadAloudItem> implements IReadAloudItemService {

    @Resource
    private ReadAloudItemDao readAloudItemDao;

    @Override
    public R addOrUpdate(ReadAloudItemDto readAloudItemDto) {
        if (BeanUtil.isEmpty(readAloudItemDto)) throw new ServiceException("数据不能为空");
        ReadAloudItem readAloudItem = new ReadAloudItem();
        BeanUtil.copyProperties(readAloudItemDto,readAloudItem);
        if (readAloudItemDto.getId() == null){
            readAloudItemDao.insert(readAloudItem);
            return R.ok("数据插入成功");
        }else {
            readAloudItemDao.updateById(readAloudItem);
            return R.ok("数据更新成功");
        }
    }
}