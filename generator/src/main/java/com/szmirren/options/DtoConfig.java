package com.szmirren.options;

import com.alibaba.fastjson2.JSONObject;
import com.szmirren.common.Constant;
import com.szmirren.models.TableAttributeDto;
import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;

/**
 * 实体类的配置文件
 * 
 * @author Mirren
 *
 */
@Getter
@Setter
public class DtoConfig {
	/** 生成模板的名字 */
	private String templateName = Constant.TEMPLATE_NAME_DTO;
	/** 字段使用驼峰命名 */
	private boolean fieldCamel = true;
	/** 是否覆盖原文件 */
	private boolean overrideFile = true;

	// -----------------不在保存配置范围的属性-----------------------
	/** 存储信息table里面的所有属性 */
	private ObservableList<TableAttributeDto> tblPropertyValues;
	/** 表的别名 */
	private String tableAlias;
	/** 主键名称 */
	private String primaryKey;

	/**
	 * 实例化
	 */
	public DtoConfig() {
		super();
	}
	/**
	 * 实例化
	 *
	 * @param obj
	 */
	public DtoConfig(JSONObject obj) {
		super();
		this.templateName = obj.getString("templateName");
		this.fieldCamel = obj.getBoolean("fieldCamel");
		this.overrideFile = obj.getBoolean("overrideFile");
	}

	/**
	 * 将对象转换为JSONObject
	 * 
	 * @return
	 */
	public JSONObject toJson() {
		JSONObject result = new JSONObject();
		result.put("templateName", templateName);
		result.put("fieldCamel", fieldCamel);
		result.put("overrideFile", overrideFile);
		return result;
	}
	/**
	 * 将当前对象转换为Json字符串
	 * 
	 * @return
	 */
	public String toJsonString() {
		return toJson().toJSONString();
	}
}
