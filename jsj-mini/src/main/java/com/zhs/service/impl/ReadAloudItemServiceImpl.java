package com.zhs.service.impl;


import com.zhs.model.ReadAloudAudio;
import com.zhs.model.ReadAloudItem;
import com.zhs.dao.ReadAloudItemDao;
import com.zhs.service.IReadAloudItemService;
import com.zhs.service.ReadAloudAudioService;
import com.zhs.dto.ReadAloudItemDto;


import com.zhs.exception.ServiceException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhs.util.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import jakarta.annotation.Resource;


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

    @Resource
    private ReadAloudAudioService readAloudAudioService;

    @Override
    public R addOrUpdate(ReadAloudItemDto readAloudItemDto) {
        if (BeanUtil.isEmpty(readAloudItemDto)) throw new ServiceException("数据不能为空");
        if (readAloudItemDto.getCategoryId() == null) {
            throw new ServiceException("所属分类不能为空");
        }
        if (StringUtils.isBlank(readAloudItemDto.getItemCode())) {
            throw new ServiceException("内容编码不能为空");
        }
        if (StringUtils.isBlank(readAloudItemDto.getTitleCn())) {
            throw new ServiceException("中文标题不能为空");
        }

        ReadAloudItem readAloudItem = new ReadAloudItem();
        BeanUtil.copyProperties(readAloudItemDto, readAloudItem);

        // 小程序 v1 只播模拟声
        readAloudItem.setPlayMode("PROCESSED");
        if (StringUtils.isBlank(readAloudItem.getStatus())) {
            readAloudItem.setStatus("DRAFT");
        }
        if (readAloudItem.getSortOrder() == null) {
            readAloudItem.setSortOrder(0);
        }

        // 从点读音频库选择时，同步模拟声文件
        if (readAloudItem.getAudioBankId() != null) {
            ReadAloudAudio bank = readAloudAudioService.getById(readAloudItem.getAudioBankId());
            if (bank == null || Integer.valueOf(1).equals(bank.getIsDelete())) {
                throw new ServiceException("点读音频库记录不存在");
            }
            if (!"READY".equals(bank.getStatus()) || bank.getOutputAssetId() == null) {
                throw new ServiceException("请选择已生成完成的点读音频");
            }
            readAloudItem.setProcessedAudioAssetId(bank.getOutputAssetId());
        }

        if ("PUBLISHED".equals(readAloudItem.getStatus()) && readAloudItem.getProcessedAudioAssetId() == null) {
            throw new ServiceException("发布前必须配置模拟音频（从点读音频库选择或本地上传）");
        }

        if (readAloudItemDto.getId() == null){
            readAloudItem.setIsDelete(0);
            readAloudItemDao.insert(readAloudItem);
            return R.ok("数据插入成功");
        }else {
            readAloudItemDao.updateById(readAloudItem);
            return R.ok("数据更新成功");
        }
    }
}
