package com.szmirren.entity;

import com.szmirren.common.StrUtil;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * 实体类的上下文
 * 
 * @author <a href="http://szmirren.com">Mirren</a>
 *
 */
@Getter
@Setter
@ToString
public class CreateDtoContent {
	/** 实体类的包名 */
	private String classPackage;
	/** 实体类的名字 */
	private String className;
	/** 实体类的名字,首字母小写 */
	private String classNameLower;
	/** 数据库表的名字 */
	private String tableName;
	/** 表的别名 */
	private String tableAlias;
	/** 数据库表的主键名字 */
	private String primaryKey;
	/** 数据库表的主键jdbc数据类型 */
	private String primaryKeyJdbcType;
	/** 数据库表的主键java数据类型 */
	private String primaryKeyJavaType;

	/** 实体类的属性信息 */
	private List<FieldAttribute> attrs;

	/** 主键属性 */
	private FieldAttribute primaryKeyAttr;
	/** 不能为空的属性 */
	private List<FieldAttribute> cantNullAttrs;
	/** 其他属性 */
	private List<FieldAttribute> otherAttrs;

	/**
	 * 初始化
	 */
	public CreateDtoContent() {
		super();
	}
	/**
	 * 初始化
	 *
	 * @param classPackage
	 *          包名
	 * @param className
	 *          类名
	 * @param tableName
	 *          表名
	 */
	public CreateDtoContent(String classPackage, String className, String tableName) {
		super();
		this.classPackage = classPackage;
		this.className = className;
		this.classNameLower = StrUtil.fristToLoCase(className);
		this.tableName = tableName;
	}
}
