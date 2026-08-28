package com.zhs.service.impl;


import com.zhs.model.FileAsset;
import com.zhs.dao.FileAssetDao;
import com.zhs.service.IFileAssetService;
import com.zhs.dto.FileAssetDto;


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
public class FileAssetServiceImpl extends ServiceImpl< FileAssetDao, FileAsset> implements IFileAssetService {

    @Resource
    private FileAssetDao fileAssetDao;

    @Override
    public R addOrUpdate(FileAssetDto fileAssetDto) {
        if (BeanUtil.isEmpty(fileAssetDto)) throw new ServiceException("数据不能为空");
        FileAsset fileAsset = new FileAsset();
        BeanUtil.copyProperties(fileAssetDto,fileAsset);
        if (fileAssetDto.getId() == null){
            fileAssetDao.insert(fileAsset);
            return R.ok("数据插入成功");
        }else {
            fileAssetDao.updateById(fileAsset);
            return R.ok("数据更新成功");
        }
    }
}