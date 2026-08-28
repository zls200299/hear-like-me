package com.zhs.service.impl;


import com.zhs.model.ScenarioPreset;
import com.zhs.dao.ScenarioPresetDao;
import com.zhs.service.IScenarioPresetService;
import com.zhs.dto.ScenarioPresetDto;


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
public class ScenarioPresetServiceImpl extends ServiceImpl< ScenarioPresetDao, ScenarioPreset> implements IScenarioPresetService {

    @Resource
    private ScenarioPresetDao scenarioPresetDao;

    @Override
    public R addOrUpdate(ScenarioPresetDto scenarioPresetDto) {
        if (BeanUtil.isEmpty(scenarioPresetDto)) throw new ServiceException("数据不能为空");
        ScenarioPreset scenarioPreset = new ScenarioPreset();
        BeanUtil.copyProperties(scenarioPresetDto,scenarioPreset);
        if (scenarioPresetDto.getId() == null){
            scenarioPresetDao.insert(scenarioPreset);
            return R.ok("数据插入成功");
        }else {
            scenarioPresetDao.updateById(scenarioPreset);
            return R.ok("数据更新成功");
        }
    }
}