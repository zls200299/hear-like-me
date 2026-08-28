package com.szmirren.entity;

import com.szmirren.models.TableAttributeBean;
import com.szmirren.models.TableAttributeDto;
import com.szmirren.models.TableAttributeEntity;

/**
 * 实体类的属性
 * 
 * @author <a href="http://szmirren.com">Mirren</a>
 *
 */
public class MongoFieldAttribute extends TableColumnsAttribute {

	/** 字段 */
	private String field;
	/** 字段的设置类型的方法名,比如字符串setString */
	private String fsetType;

	/**
	 * 初始化
	 */
	public MongoFieldAttribute() {
		super();
	}

	/**
	 * 初始化
	 */
	public MongoFieldAttribute(TableAttributeEntity entity) {
		super();
		super.setJavaType(entity.getTdJavaType().getValue());
		super.setNullable(entity.isNullable());
		super.setColumnName(entity.getTdColumnName());
		super.setColumnDef(entity.getColumnDef());
		super.setRemarks(entity.getRemarks());
		super.setColumnSize(entity.getColumnSize());
		super.setTypeName(entity.getTdJdbcType());
		super.setDecimalDigits(entity.getDecimalDigits());
		super.setOrdinalPosition(entity.getOrdinalPosition());
	}

	public MongoFieldAttribute(TableAttributeBean entity) {
		super();
		super.setJavaType(entity.getTdJavaType().getValue());
		super.setNullable(entity.isNullable());
		super.setColumnName(entity.getTdColumnName());
		super.setColumnDef(entity.getColumnDef());
		super.setRemarks(entity.getRemarks());
		super.setColumnSize(entity.getColumnSize());
		super.setTypeName(entity.getTdJdbcType());
		super.setDecimalDigits(entity.getDecimalDigits());
		super.setOrdinalPosition(entity.getOrdinalPosition());
	}

	/**
	 * 初始化
	 */
	public MongoFieldAttribute(TableAttributeDto createDto) {
		super();
		super.setJavaType(createDto.getTdJavaType().getValue());
		super.setNullable(createDto.isNullable());
		super.setColumnName(createDto.getTdColumnName());
		super.setColumnDef(createDto.getColumnDef());
		super.setRemarks(createDto.getRemarks());
		super.setColumnSize(createDto.getColumnSize());
		super.setTypeName(createDto.getTdJdbcType());
		super.setDecimalDigits(createDto.getDecimalDigits());
		super.setOrdinalPosition(createDto.getOrdinalPosition());
	}


	public String getField() {
		return field;
	}

	public void setField(String field) {
		this.field = field;
	}


	public String getFsetType() {
		return fsetType;
	}

	public void setFsetType(String fsetType) {
		this.fsetType = fsetType;
	}

	@Override
	public String toString() {
		return super.toString() + "\nFieldAttribute [ field=" + field + ", fsetType=" + fsetType + "]";
	}

}
