package com.szmirren.options;

import java.util.ArrayList;
import java.util.List;

import com.szmirren.common.Constant;
import com.szmirren.models.TableAttributeKeyValue;

import javafx.collections.ObservableList;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Service属性的配置文件
 * 
 * @author <a href="http://szmirren.com">Mirren</a>
 *
 */
@Getter
@Setter
@ToString
public class ServiceConfig {
	/** 设置的tableItem */
	private List<TableAttributeKeyValue> tableItem = new ArrayList<>();
	/** 生成模板的名字 */
	private String templateName = Constant.TEMPLATE_NAME_SERVICE;
	/** 是否覆盖原文件 */
	private boolean overrideFile = true;

	/**
	 * 初始化
	 */
	public ServiceConfig() {
		super();
	}

	/**
	 * 通过 ObservableList<TableAttributeKeyValue>初始化
	 * 
	 */
	public ServiceConfig(ObservableList<TableAttributeKeyValue> item) {
		super();
		if (item != null && !item.isEmpty()) {
			tableItem.addAll(item);
		}
	}

	/**
	 * 通过 ObservableList<TableAttributeKeyValue>初始化
	 * 
	 */
	public ServiceConfig(ObservableList<TableAttributeKeyValue> item, String templateName, boolean overrideFile) {
		super();
		if (item != null && !item.isEmpty()) {
			tableItem.addAll(item);
		}
		this.templateName = templateName;
		this.overrideFile = overrideFile;
	}

	/**
	 * 初始化默认数据
	 */
	public ServiceConfig initDefaultValue() {
		tableItem.add(new TableAttributeKeyValue("index", "query", "查询所有数据"));
		tableItem.add(new TableAttributeKeyValue("show", "show", "通过id查询数据"));
		tableItem.add(new TableAttributeKeyValue("create", "create", "插入不为空的数据"));
		tableItem.add(new TableAttributeKeyValue("update", "update", "更新不为空的数据"));
		tableItem.add(new TableAttributeKeyValue("delete", "delete", "通过ids删除数据"));
		tableItem.add(new TableAttributeKeyValue("export", "export", "导出数据"));
		return this;
	}
}
