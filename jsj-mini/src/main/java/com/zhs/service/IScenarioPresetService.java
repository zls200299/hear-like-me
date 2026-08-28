package com.zhs.service;


import com.zhs.model.ScenarioPreset;
import com.zhs.dto.ScenarioPresetDto;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhs.util.R;


/**
 *
 * @author 
 * @since 2026-08-28
 */

public interface IScenarioPresetService extends IService<ScenarioPreset> {
    R addOrUpdate(ScenarioPresetDto scenarioPresetDto);
}
