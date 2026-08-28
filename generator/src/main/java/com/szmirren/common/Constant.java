package com.szmirren.common;

import com.szmirren.Main;

/**
 * 工具需要用到的常量词
 * 
 * @author <a href="http://szmirren.com">Mirren</a>
 *
 */
public interface Constant {
	// 数据库名字
	/** MySQL */
	String MYSQL = "MySQL";
	/** PostgreSQL */
	String POSTGRE_SQL = "PostgreSQL";
	/** SqlServer */
	String SQL_SERVER = "SqlServer";
	/** Oracle */
	String ORACLE = "Oracle";

	/** java的后缀名.java */
	String JAVA_SUFFIX = ".java";

	/** default */
	String DEFAULT = "default";
	/** language */
	String LANGUAGE = "language";
	/** 模板的文件夹名称 */
	String TEMPLATE_DIR_NAME = "template";

	/** 实体类模板的默认名字 */
//	String TEMPLATE_NAME_ENTITY = "Entity.ftl";
	String TEMPLATE_NAME_ENTITY = "zhsEntity.ftl";


	/** dto模板名称 */
	String TEMPLATE_NAME_DTO = "zhsDto.ftl";

	/** Request/Response 分层模板（字段与 DTO 配置一致，包名与类名由首页配置） */
	String TEMPLATE_NAME_ADD_REQ = "zhsAddReq.ftl";
	String TEMPLATE_NAME_UPDATE_REQ = "zhsUpdateReq.ftl";
	String TEMPLATE_NAME_QUERY_REQ = "zhsQueryReq.ftl";
	String TEMPLATE_NAME_RESP = "zhsResp.ftl";
	String TEMPLATE_NAME_DETAIL_RESP = "zhsDetailResp.ftl";

	/** 统一响应包装类 R（与 Controller/ServiceImpl 模板中的 com.zhs.util.R 一致，默认不覆盖已存在文件） */
	String TEMPLATE_NAME_R = "zhsR.ftl";

	/** Bean实体类模板的默认名字 */
	String TEMPLATE_NAME_BEAN = "ScBean.ftl";
	/** Service模板的默认名字 */
	String TEMPLATE_NAME_SERVICE = "zhsService.ftl";
	/** Swagger文档 */
	String TEMPLATE_NAME_SWAGGER = "zhsSwagger.ftl";
	/** ServiceImpl模板的默认名字 */
	String TEMPLATE_NAME_SERVICE_IMPL = "zhsServiceImpl.ftl";
	/** Controller模板的默认名字 */
	String TEMPLATE_NAME_ROUTER = "zhsController.ftl";
	/** Dao模板的默认名字 */
	String TEMPLATE_NAME_DAO = "zhsDao.ftl";
	/** Mapper模板的默认名字 */
	String TEMPLATE_NAME_MAPPER = Main.LANGUAGE.get(LanguageKey.SET_ABSTRACT_AUTOMATIC).get();
	/** Mapper模板的默认名字 */
	String TEMPLATE_NAME_MAPPER_SUFFIX = "Mapper.ftl";
	/** SqlAssist模板的默认名字 */
	String TEMPLATE_NAME_SQL_ASSIST = "SqlAssist.ftl";
	/** 单元测试模板的默认名字 */
	String TEMPLATE_NAME_UNIT_TEST = "UnitTest.ftl";

	/** 时间类 */
	String TEMPLATE_NAME_TIME = "zhsMeta.ftl";

	//==================mongo模板==================
	String TEMPLATE_NAME_MONGO_DTO_ENTITY = "zhsMongoDto.ftl";
	String TEMPLATE_NAME_MONGO_LISTEN = "zhsMongoConfig.ftl";
	String TEMPLATE_NAME_MONGO_CONTROLLER = "zhsMongoController.ftl";
}
