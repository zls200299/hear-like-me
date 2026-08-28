package com.szmirren.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;

/**
 * 生成文件的上下文
 * 
 * @author <a href="http://szmirren.com">Mirren</a>
 *
 */
@Getter
@Setter
@ToString
public class GeneratorContent {

	/** 数据库配置文件 */
	private DatabaseContent database;
	/** 实体类配置信息 */
	private EntityContent entity;
	/** Bean实体类配置信息 */
	private BeanContent bean;
	/** 数据库表的属性 */
	private TableContent table;
	/** 实体类配置信息 */
	private ServiceContent service;
	/** 实体类配置信息 */
	private ServiceImplContent serviceImpl;
	/** 实体类配置信息 */
	private DaoContent dao;
	private CreateDtoContent createDto;
	/** 入参：新增 */
	private CreateDtoContent addReq;
	/** 入参：修改 */
	private CreateDtoContent updateReq;
	/** 入参：查询 */
	private CreateDtoContent queryReq;
	/** 出参：列表/通用响应 */
	private CreateDtoContent resp;
	/** 出参：详情 */
	private CreateDtoContent detailResp;
	/** 实体类配置信息 */
	private MapperContent mapper;
	/** 实体类配置信息 */
	private ControllerContent controller;
	/** 实体类配置信息 */
	private UnitTestContent unitTest;
	/** 实体类配置信息 */
	private SqlAssistContent assist;
	/** 实体类配置信息 */
	private CustomContent custom;
	/** 实体类配置信息 */
	private CustomPropertyContent customProperty;
	/** 代码作者 */
	private String author = "";
	/** 当前时间 */
	private LocalDate now = LocalDate.now();
	/** 文档 */
	private SwaggerContent swagger;
	private MetaContent handler;
	private MongoDtoContent mongoDto;
	private MongoListenContent mongoListen;
	private MongoControllerContent mongoController;
}
