package com.szmirren.options;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * 配置信息
 * 
 * @author Mirren
 *
 */
@Getter
@Setter
@ToString
public class HistoryConfig {
	/** 配置信息的名字 */
	private String historyConfigName;
	/** 生产路径 */
	private String projectPath;
	/** 实体类的包名 */
	private String entityPackage;
	/** 实体类的类名 */
	private String entityName;
	/** Bean实体类的包名 */
	private String beanPackage;
	/** Bean实体类的类名 */
	private String beanName;
	/** Dto实体类的包名 */
	private String createDtoPackage;
	/** Dto实体类的类名 */
	private String createDtoName;
	/** service包名 */
	private String servicePackage;
	/** service类名 */
	private String serviceName;
	/** service实现类包名 */
	private String serviceImplPackage;
	/** service实现类名 */
	private String serviceImplName;
	/** Controller类包名 */
	private String controllerPackage;
	/** Controller类名 */
	private String controllerName;
	/** Dao类的包 */
	private String daoPackage;
	/** Dao类名 */
	private String daoName;
	/** Mapper类的包 */
	private String mapperPackage;
	/** Mapper名称 */
	private String mapperName;
	/** swagger类的包 */
	private String swaggerPackage;
	/** swagger名称 */
	private String swaggerName;
	/** Meta类的包 */
	private String metaPackage;
	/** Meta名称 */
	private String metaName;
	/** MongodbDto类的包 */
	private String mongoDtoPackage;
	/** MongodbDto名称 */
	private String mongoDtoName;
	/** MongodbListen类的包 */
	private String mongoListenPackage;
	/** MongodbListen名称 */
	private String mongoListenName;
	/** MongodbController类的包 */
	private String mongoControllerPackage;
	/** MongodbController名称 */
	private String mongoControllerName;
	/** sqlAssist包名 */
	private String assistPackage;
	/** 单元测试包名 */
	private String unitTestPackage;
	/** 单元测试类名 */
	private String unitTestName;
	/** 字符编码格式 */
	private String codeFormat;

	/** Request 包名（旧配置无此字段时由界面填充默认） */
	private String requestPackage;
	/** Response 包名 */
	private String responsePackage;
	/** AddReq 类名模板，如 Add{c}Req */
	private String addReqClassName;
	/** UpdateReq 类名模板 */
	private String updateReqClassName;
	/** QueryReq 类名模板 */
	private String queryReqClassName;
	/** 列表响应类名模板，如 {c}Resp */
	private String respClassName;
	/** 详情响应类名模板，如 {c}DetailResp */
	private String detailRespClassName;

	/** 数据库配置文件 */
	private DatabaseConfig dbConfig;
	/** 实体类配置文件 */
	private EntityConfig entityConfig;
	/** Bean实体类配置文件 */
	private BeanConfig beanConfig;
	/** Dto类配置文件 */
	private DtoConfig dtoConfig;
	/** Service配置文件 */
	private ServiceConfig serviceConfig;
	/** Service实现类的配置文件 */
	private ServiceImplConfig serviceImplConfig;
	/** Controller的配置文件 */
	private ControllerConfig controllerConfig;
	/** DAO的配置文件 */
	private DaoConfig daoConfig;
	/** Mapper的配置文件 */
	private MapperConfig mapperConfig;
	/** SqlAssist的配置文件 */
	private SqlAssistConfig assistConfig;
	/** 单元测试配置文件 */
	private UnitTestConfig unitTestConfig;
	/** 自定义包类的配置文件 */
	private CustomConfig customConfig;
	/** 自定义属性的配置文件 */
	private CustomPropertyConfig customPropertyConfig;

	private SwaggerConfig swaggerConfig;
	private MetaConfig metaConfig;
	private MongoDtoConfig mongoDtoConfig;
	private MongoControllerConfig mongoControllerConfig;


	/**
	 * 初始化
	 */
	public HistoryConfig() {
		super();
	}

	public HistoryConfig(String projectPath, String entityPackage, String entityName, String createDtoPackage, String createDtoName, String servicePackage, String serviceName,
						 String serviceImplPackage, String serviceImplName, String controllerPackage, String controllerName, String daoPackage, String daoName,
						 String mapperName,String mapperPackage,String codeFormat,String mongoDtoPackage,String mongoDtoName, String mongoControllerPackage,String mongoControllerName ) {
		super();
		this.projectPath = projectPath;
		this.entityPackage = entityPackage;
		this.entityName = entityName;
		this.createDtoPackage = createDtoPackage;
		this.createDtoName = createDtoName;
		this.servicePackage = servicePackage;
		this.serviceName = serviceName;
		this.serviceImplPackage = serviceImplPackage;
		this.serviceImplName = serviceImplName;
		this.controllerPackage = controllerPackage;
		this.controllerName = controllerName;
		this.daoPackage = daoPackage;
		this.daoName = daoName;
		this.mapperName = mapperName;
		this.mapperPackage = mapperPackage;
		this.codeFormat = codeFormat;
		this.swaggerPackage = "com.zhs.common";
		this.swaggerName = "Swagger";
		this.metaPackage = "com.zhs.handler";
		this.metaName = "ZhsMetaObjectHandler";
		this.mongoDtoPackage = mongoDtoPackage;
		this.mongoDtoName = mongoDtoName + "MongoDto";
		this.mongoListenPackage="com.zhs.configuration";
		this.mongoListenName="ApplicationReadyListener";
		this.mongoListenPackage = mongoControllerPackage;
		this.mongoControllerName = mongoControllerName;
	}
}
