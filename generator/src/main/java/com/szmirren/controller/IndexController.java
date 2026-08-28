package com.szmirren.controller;

import com.szmirren.Main;
import com.szmirren.common.*;
import com.szmirren.entity.*;
import com.szmirren.models.TableAttributeDto;
import com.szmirren.models.TableAttributeEntity;
import com.szmirren.models.TableAttributeKeyValueTemplate;
import com.szmirren.options.*;
import com.szmirren.view.AlertUtil;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 首页的控制器
 * 
 * @author <a href="http://szmirren.com">Mirren</a>
 *
 */
public class IndexController extends BaseController {
	private final Logger LOG = LogManager.getLogger(getClass());
	/** 配置信息的名字 */
	private String historyConfigName;
	/** 程序的配置信息 */
	private HistoryConfig historyConfig;
	/** 模板文件夹中模板现有模板名字 */
	private List<String> templateNameItems;

	/** 存储数据库指定数据库,修改属性时用 */
	private DatabaseConfig selectedDatabaseConfig;
	private DatabaseConfig updateOfDatabaseConfig;

	/** 记录存储的表名,修改属性时用 */
	private String selectedTableName;

	/** 实体类名默认的占位符 */
	private String entityNamePlace;
	/** Bean实体类名默认的占位符 */
//	private String beanNamePlace;
	/** createDto默认占位符 */
	private String createDtoNamePlace;
	/** updateDto默认占位符 */
	private String updateDtoNamePlace;
	/** respDto默认占位符 */
	private String respDtoNamePlace;
	/** queryDto默认占位符 */
	private String queryDtoNamePlace;
	/** Service默认占位符 */
	private String serviceNamePlace;
	/** ServiceImpl默认占位符 */
	private String serviceImplNamePlace;
	/** Controller默认占位符 */
	private String routerNamePlace;
	/** Dao默认占位符 */
	private String daoNamePlace;
	/** Mapper默认占位符 */
	private String mapperNamePlace;
	/** AddReq 类名模板占位 */
	private String addReqClassNamePlace;
	/** UpdateReq 类名模板占位 */
	private String updateReqClassNamePlace;
	/** QueryReq 类名模板占位 */
	private String queryReqClassNamePlace;
	/** Resp 类名模板占位 */
	private String respClassNamePlace;
	/** DetailResp 类名模板占位 */
	private String detailRespClassNamePlace;
	/** 单元测试默认占位符 */
	private String unitTestPlace;

	private static final String DEF_REQUEST_PACKAGE = "com.zhs.request";
	private static final String DEF_RESPONSE_PACKAGE = "com.zhs.response";
	private static final String DEF_ADD_REQ_CLASS = "Add{c}Req";
	private static final String DEF_UPDATE_REQ_CLASS = "Update{c}Req";
	private static final String DEF_QUERY_REQ_CLASS = "Query{c}Req";
	private static final String DEF_RESP_CLASS = "{c}Resp";
	private static final String DEF_DETAIL_RESP_CLASS = "{c}DetailResp";

	// ========================fxml控件============================
	/** 数据库连接 */
	@FXML
	private Label lblConnection;
	/** 配置信息 */
	@FXML
	private Label lblConfig;
	/** 使用说明 */
	@FXML
	private Label lblInstructions;
	/** 设置 */
	@FXML
	private Label lblSetting;
	@FXML
	private Button buChangeMongodb;
	/** 存放目录 */
	@FXML
	private Label lblProjectPath;
	/** 数据库表名 */
	@FXML
	private Label lblTableName;
	/** 实体类包名 */
	@FXML
	private Label lblEntityPackage;
	/** createDto实体类包名 */
	@FXML
	private Label lblCreateDtoPackage;
	/** updateDto实体类包名 */
	@FXML
	private Label lblUpdateDtoPackage;
	/** respDto实体类包名 */
	@FXML
	private Label lblRespDtoPackage;
	/** queryDto实体类包名 */
	@FXML
	private Label lblQueryDtoPackage;
	/**
	 * 入参包名
	 */
	@FXML
	private Label lblBeanPackage;
	/** Service包名 */
	@FXML
	private Label lblServicePackage;
	/** ServiceImpl包名 */
	@FXML
	private Label lblServiceImplPackage;
	/** router包名 */
	@FXML
	private Label lblRouterPackage;
	/** SQL包名 */
	@FXML
	private Label lblSqlPackage;
	/** Assist包名 */
	@FXML
	private Label lblAssistPackage;
	/** Mapper包名 */
	@FXML
	private Label lblMapperPackage;
	/** 单元测试的包名 */
	@FXML
	private Label lblUnitTestPackage;

	/** 实体类类名 */
	@FXML
	private Label lblEntityName;

	/** createDto实体类类名 */
	@FXML
	private Label lblCreateDtoName;

	/** createDto实体类类名 */
	@FXML
	private Label lblUpdateDtoName;

	/** respDto实体类类名 */
	@FXML
	private Label lblRespDtoName;

	/** queryDto实体类类名 */
	@FXML
	private Label lblQueryDtoName;

	/** 入参类名 */
//	@FXML
//	private Label lblBeanName;

	/** Service类名 */
	@FXML
	private Label lblServiceName;
	/** ServiceImpl类名 */
	@FXML
	private Label lblServiceImplName;
	/** router类名 */
	@FXML
	private Label lblRouterName;
	/** SQL类名 */
	@FXML
	private Label lblSqlName;
	/** Assist类名 */
	@FXML
	private Label lblAssistName;
	/** Mapper的名字 */
	@FXML
	private Label lblMapperName;
	/** 单元测试的类名 */
	@FXML
	private Label lblUnitTestName;

	/** 自定义包名与类 */
	@FXML
	private Label lblSetCustom;
	/** 自定义属性 */
	@FXML
	private Label lblSetCustomProperty;
	/** Request 包名 */
	@FXML
	private Label lblRequestPackage;
	/** Response 包名 */
	@FXML
	private Label lblResponsePackage;
	/** AddReq 类名模板 */
	@FXML
	private Label lblAddReqName;
	/** UpdateReq 类名模板 */
	@FXML
	private Label lblUpdateReqName;
	/** QueryReq 类名模板 */
	@FXML
	private Label lblQueryReqName;
	/** Resp 类名模板 */
	@FXML
	private Label lblRespName;
	/** DetailResp 类名模板 */
	@FXML
	private Label lblDetailRespName;
	/** 生成文件的编码格式 */
	@FXML
	private Label lblCodeFormat;

	/** 提示文字进度条 */
	@FXML
	private Label lblRunCreateAllTips;
	/** 提示文字的默认文字 */
	private String runCreateTipsText = "正在生成";

	/** 数据树列表 */
	@FXML
	private TreeView<String> tvDataBase;
	/** 存放目录 */
	@FXML
	private TextField txtProjectPath;
	/** 数据库表名 */
	@FXML
	private TextField txtTableName;
	/** 实体类包名 */
	@FXML
	private TextField txtEntityPackage;
//	/**
//	 * Bean实体类包名
//	 */
//	@FXML
//	private TextField txtBeanPackage;
	/** CreateDto包名 */
	@FXML
	private TextField txtCreateDtoPackage;
	/** UpdateDto包名 */
	@FXML
	private TextField txtUpdateDtoPackage;
	/** RespDto包名 */
	@FXML
	private TextField txtRespDtoPackage;
	/** QueryDto包名 */
	@FXML
	private TextField txtQueryDtoPackage;
	/** Service包名 */
	@FXML
	private TextField txtServicePackage;
	/** ServiceImpl包名 */
	@FXML
	private TextField txtServiceImplPackage;
	/** router包名 */
	@FXML
	private TextField txtRouterPackage;
	/** SQL包名 */
	@FXML
	private TextField txtSqlPackage;
	/** Assist包名 */
	@FXML
	private TextField txtAssistPackage;
	/** Mapper包名 */
	@FXML
	private TextField txtMapperPackage;
	/** Request 包名 */
	@FXML
	private TextField txtRequestPackage;
	/** Response 包名 */
	@FXML
	private TextField txtResponsePackage;
	/** 单元测试的包名 */
	@FXML
	private TextField txtUnitTestPackage;

	/** 实体类类名 */
	@FXML
	private TextField txtEntityName;
	/** Bean实体类类名 */
//	@FXML
//	private TextField txtBeanName;
	/** CreateDto实体类类名 */
	@FXML
	private TextField txtCreateDtoName;
	/** UpdateDto类名 */
	@FXML
	private TextField txtUpdateDtoName;
	/** RespDto类名 */
	@FXML
	private TextField txtRespDtoName;
	/** QueryDto类名 */
	@FXML
	private TextField txtQueryDtoName;
	/** Service类名 */
	@FXML
	private TextField txtServiceName;
	/** ServiceImpl类名 */
	@FXML
	private TextField txtServiceImplName;
	/** router类名 */
	@FXML
	private TextField txtRouterName;
	/** SQL类名 */
	@FXML
	private TextField txtSqlName;
	/** Assist类名 */
	@FXML
	private TextField txtAssistName;
	/** Mapper类名 */
	@FXML
	private TextField txtMapperName;
	/** AddReq 类名模板 */
	@FXML
	private TextField txtAddReqClassName;
	/** UpdateReq 类名模板 */
	@FXML
	private TextField txtUpdateReqClassName;
	/** QueryReq 类名模板 */
	@FXML
	private TextField txtQueryReqClassName;
	/** Resp 类名模板 */
	@FXML
	private TextField txtRespClassName;
	/** DetailResp 类名模板 */
	@FXML
	private TextField txtDetailRespClassName;
	/** 单元测试类名 */
	@FXML
	private TextField txtUnitTestName;

	/** 选择根目录按钮 */
	@FXML
	private Button btnSelectFile;
	/** 执行创建 */
	@FXML
	private Button btnRunCreate;
	/** 保存配置文件 */
	@FXML
	private Button btnSaveConfig;
	/** 实体类配置按钮 */
	@FXML
	private Button btnSetEntity;
	/** createDto类配置按钮 */
	@FXML
	private Button btnSetCreateDto;
	/** updateDto类配置按钮 */
	@FXML
	private Button btnSetUpdateDto;
	/** respDto类配置按钮 */
	@FXML
	private Button btnSetRespDto;
	/** queryDto类配置按钮 */
	@FXML
	private Button btnSetQueryDto;
	/** Bean实体类配置按钮 */
//	@FXML
//	private Button btnSetBean;
	/** 到设置按钮 */
	@FXML
	private Button btnSetService;
	/** biz设置按钮 */
	@FXML
	private Button btnSetServiceImpl;
	/** router设置按钮 */
	@FXML
	private Button btnSetRouter;
	/** SQL设置按钮 */
	@FXML
	private Button btnSetSql;
	/** Assist的设置按钮 */
	@FXML
	private Button btnSetAssist;
	/** SqlAndParams的设置按钮 */
	@FXML
	private Button btnSetMapper;
	/** 单元测试的设置按钮 */
	@FXML
	private Button btnSetUnitTest;
	/** 自定义包名类的设置按钮 */
	@FXML
	private Button btnSetCustom;
	/** 自定义包名类属性的设置按钮 */
	@FXML
	private Button btnSetCustomProperty;
	/** 字符编码格式 */
	@FXML
	private ComboBox<String> cboCodeFormat;
	/** 生成进度条 */
	@FXML
	private ProgressBar probCreateAll;
	@FXML
	private TextField searchField;

	@FXML
	private Button searchUp;

	@FXML
	private Button searchDown;

	private Map<Integer, TreeItem<String>> globalSearchMap = new HashMap<>();

	private Integer globalSearchCount = 0;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOG.debug("初始化首页...");
		final int ml = 20;// 左外边距
		// 初始化图标连接与配置信息
		ImageView lblConnImage = new ImageView("image/computer.png");
		lblConnImage.setFitHeight(40);
		lblConnImage.setFitWidth(40);
		lblConnection.setGraphic(lblConnImage);
		lblConnection.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_CONNECTION));
		lblConnection.setOnMouseClicked(this::onConnection);
		lblConnection.widthProperty().addListener(event -> lblConfig.setLayoutX(ml + lblConnection.getLayoutX() + lblConnection.getWidth()));

		ImageView lblConfImage = new ImageView("image/config.png");
		lblConfImage.setFitHeight(40);
		lblConfImage.setFitWidth(40);
		lblConfig.setGraphic(lblConfImage);
		lblConfig.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_CONFIG));
		lblConfig.setOnMouseClicked(this::onConfig);
		lblConfig.widthProperty().addListener(event -> lblInstructions.setLayoutX(ml + lblConfig.getLayoutX() + lblConfig.getWidth()));

		ImageView lblInstructionsImage = new ImageView("image/instructions.png");
		lblInstructionsImage.setFitHeight(40);
		lblInstructionsImage.setFitWidth(40);
		lblInstructions.setGraphic(lblInstructionsImage);
		lblInstructions.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_INSTRUCTIONS));
		lblInstructions.setOnMouseClicked(this::onInstructions);
		lblInstructions.widthProperty()
				.addListener(event -> lblSetting.setLayoutX(ml + lblInstructions.getLayoutX() + lblInstructions.getWidth()));

		ImageView lblSettingImage = new ImageView("image/setting.png");
		lblSettingImage.setFitHeight(40);
		lblSettingImage.setFitWidth(40);
		lblSetting.setGraphic(lblSettingImage);
		lblSetting.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SETTING));
		lblSetting.setOnMouseClicked(this::onSetting);

		//初始化数据源切换
		ImageView changeDatasourceConnImage = new ImageView("image/computer.png");
		changeDatasourceConnImage.setFitHeight(40);
		changeDatasourceConnImage.setFitWidth(40);
		buChangeMongodb.setGraphic(changeDatasourceConnImage);
		buChangeMongodb.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_CHANGE_DATASOURCE));
		buChangeMongodb.setOnMouseClicked(event -> toChangeMongodbDatasource(event));

		cboCodeFormat.setEditable(true);
		cboCodeFormat.getItems().addAll("UTF-8", "GBK", "UTF-16", "UTF-32", "GB2312", "GB18030", "ISO-8859-1");
		cboCodeFormat.setValue("UTF-8");
		initLanguage();
		LOG.debug("初始化首页成功!");
		try {
			// 加载左边数据库树
			initTVDataBase();
			loadTVDataBase();
			LOG.debug("加载所有数据库到左侧树集成功!");
		} catch (Exception e1) {
			AlertUtil.showErrorAlert(e1.getMessage());
			LOG.error("加载所有数据库到左侧树集失败!!!" + e1);
		}
		try {
			// 加载首页配置信息
			LOG.debug("执行查询默认配置信息并加载到首页...");
			loadIndexConfigInfo("default");// 查询使用有默认的配置,如果有就加载
			loadPlace();// 设置默认的占位符名字
			loadTemplate();// 获取模板文件夹中所有模板的名字
			LOG.debug("加载配置信息到首页成功!");
		} catch (Exception e) {
			AlertUtil.showErrorAlert("加载配置失败!失败原因:\r\n" + e.getMessage());
			e.printStackTrace();
			LOG.error("加载配置信息失败!!!" + e);
		}
	}

	// ======================方法区域================================
	/**
	 * 加载首页配置文件
	 *
	 * @param name
	 * @throws Exception
	 */
	public void loadIndexConfigInfo(String name) throws Exception {
		HistoryConfig config = ConfigUtil.getHistoryConfigByName(name);
		if (config == null) {
			historyConfig = new HistoryConfig();
			return;
		} else {
			historyConfig = config;
		}
		historyConfigName = config.getHistoryConfigName();
		txtProjectPath.setText(config.getProjectPath());
		txtEntityPackage.setText(config.getEntityPackage());
		if (txtEntityName.getText().contains("{c}")) {
			txtEntityName.setText(config.getEntityName());
		}

		txtServicePackage.setText(config.getServicePackage());
		if (txtServiceName.getText().contains("{c}")) {
			txtServiceName.setText(config.getServiceName());
		}
		txtServiceImplPackage.setText(config.getServiceImplPackage());
		if (txtServiceImplName.getText().contains("{c}")) {
			txtServiceImplName.setText(config.getServiceImplName());
		}
		txtRouterPackage.setText(config.getControllerPackage());
		if (txtRouterName.getText().contains("{c}")) {
			txtRouterName.setText(config.getControllerName());
		}
		txtMapperName.setText(config.getMapperPackage());
		if (txtMapperName.getText().contains("{c}")) {
			txtMapperName.setText(config.getControllerName());
		}
		txtSqlPackage.setText(config.getDaoPackage());
		if (txtSqlName.getText().contains("{c}")) {
			txtSqlName.setText(config.getMapperName());
		}
		txtRequestPackage.setText(StrUtil.isNullOrEmpty(config.getRequestPackage()) ? DEF_REQUEST_PACKAGE : config.getRequestPackage());
		txtResponsePackage.setText(StrUtil.isNullOrEmpty(config.getResponsePackage()) ? DEF_RESPONSE_PACKAGE : config.getResponsePackage());
		txtAddReqClassName.setText(StrUtil.isNullOrEmpty(config.getAddReqClassName()) ? DEF_ADD_REQ_CLASS : config.getAddReqClassName());
		txtUpdateReqClassName.setText(StrUtil.isNullOrEmpty(config.getUpdateReqClassName()) ? DEF_UPDATE_REQ_CLASS : config.getUpdateReqClassName());
		txtQueryReqClassName.setText(StrUtil.isNullOrEmpty(config.getQueryReqClassName()) ? DEF_QUERY_REQ_CLASS : config.getQueryReqClassName());
		txtRespClassName.setText(StrUtil.isNullOrEmpty(config.getRespClassName()) ? DEF_RESP_CLASS : config.getRespClassName());
		txtDetailRespClassName.setText(StrUtil.isNullOrEmpty(config.getDetailRespClassName()) ? DEF_DETAIL_RESP_CLASS : config.getDetailRespClassName());
		cboCodeFormat.setValue(config.getCodeFormat());
	}

	/**
	 * 初始化语言
	 */
	private void initLanguage() {
		lblProjectPath.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_PROJECT_PATH));
		txtProjectPath.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_TXT_PROJECT_PATH));
		lblTableName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_TABLE_NAME));
		txtTableName.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_TXT_TABLE_NAME));
		lblEntityPackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_ENTITY_PACKAGE));
		lblEntityName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_ENTITY_NAME));
		lblCreateDtoName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_CREATE_DTO_NAME));
		lblCreateDtoPackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_CREATE_DTO_PACKAGE));
		lblRequestPackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_REQUEST_PACKAGE));
		lblResponsePackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_RESPONSE_PACKAGE));
		lblAddReqName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_ADD_REQ_NAME));
		lblUpdateReqName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_UPDATE_REQ_NAME));
		lblQueryReqName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_QUERY_REQ_NAME));
		lblRespName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_RESP_NAME));
		lblDetailRespName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_DETAIL_RESP_NAME));

		lblServicePackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SERVICE_PACKAGE));
		lblServiceName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SERVICE_NAME));
		lblServiceImplPackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SERVICE_IMPL_PACKAGE));
		lblServiceImplName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SERVICE_IMPL_NAME));
		lblRouterPackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_ROUTER_PACKAGE));
		lblRouterName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_ROUTER_NAME));
		lblSqlPackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SQL_PACKAGE));
		lblSqlName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SQL_NAME));
		lblSetCustom.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SET_CUSTOM));
		lblSetCustomProperty.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SET_CUSTOM_PROPERTY));
		lblCodeFormat.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_CODE_FORMAT));
		btnSelectFile.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_BTN_SELECT_FILE));
		btnSetEntity.textProperty().bind(Main.LANGUAGE.get(LanguageKey.COMMON_BTN_SET));


		btnSetService.textProperty().bind(Main.LANGUAGE.get(LanguageKey.COMMON_BTN_SET));
		btnSetServiceImpl.textProperty().bind(Main.LANGUAGE.get(LanguageKey.COMMON_BTN_SET));
		btnSetRouter.textProperty().bind(Main.LANGUAGE.get(LanguageKey.COMMON_BTN_SET));
		btnSetSql.textProperty().bind(Main.LANGUAGE.get(LanguageKey.COMMON_BTN_SET));
		btnSetCustom.textProperty().bind(Main.LANGUAGE.get(LanguageKey.COMMON_BTN_SET));
		btnSetCustomProperty.textProperty().bind(Main.LANGUAGE.get(LanguageKey.COMMON_BTN_SET));
		btnRunCreate.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_BTN_RUN_CREATE));
		btnSaveConfig.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_BTN_SAVE_CONFIG));
	}

	/**
	 * 获得当前页面的信息并实例化为配置信息对象,
	 *
	 * @return
	 */
	private HistoryConfig getThisHistoryConfig() {
		String projectPath = txtProjectPath.getText();
		String entityPackage = txtEntityPackage.getText();
		String entityName = txtEntityName.getText();
		String createDtoPackage = txtCreateDtoPackage.getText();
		String createDtoName = txtCreateDtoName.getText();
		String servicePackage = txtServicePackage.getText();
		String serviceName = txtServiceName.getText();
		String serviceImplPackage = txtServiceImplPackage.getText();
		String serviceImplName = txtServiceImplName.getText();
		String controllerPackage = txtRouterPackage.getText();
		String controllerName = txtRouterName.getText();
		String daoPackage = txtSqlPackage.getText();
		String daoName = txtSqlName.getText();
		String codeFormat = cboCodeFormat.getValue();
		String mapperPackage = txtMapperPackage.getText();
		String mapperName = txtMapperName.getText();
		HistoryConfig config = new HistoryConfig(projectPath, entityPackage, entityName, createDtoPackage, createDtoName, servicePackage, serviceName, serviceImplPackage,
				serviceImplName, controllerPackage, controllerName, daoPackage, daoName,mapperName, mapperPackage, codeFormat,null,null,null,null);
		config.setDbConfig(selectedDatabaseConfig);
		config.setEntityConfig(historyConfig.getEntityConfig());
		config.setDtoConfig(historyConfig.getDtoConfig());
		config.setServiceConfig(historyConfig.getServiceConfig());
		config.setServiceImplConfig(historyConfig.getServiceImplConfig());
		config.setControllerConfig(historyConfig.getControllerConfig());
		config.setDaoConfig(historyConfig.getDaoConfig());
		config.setMapperConfig(historyConfig.getMapperConfig());
		config.setMapperConfig(historyConfig.getMapperConfig());
		config.setAssistConfig(historyConfig.getAssistConfig());
		config.setUnitTestConfig(historyConfig.getUnitTestConfig());
		config.setCustomConfig(historyConfig.getCustomConfig());
		config.setCustomPropertyConfig(historyConfig.getCustomPropertyConfig());
		config.setRequestPackage(txtRequestPackage.getText());
		config.setResponsePackage(txtResponsePackage.getText());
		config.setAddReqClassName(txtAddReqClassName.getText());
		config.setUpdateReqClassName(txtUpdateReqClassName.getText());
		config.setQueryReqClassName(txtQueryReqClassName.getText());
		config.setRespClassName(txtRespClassName.getText());
		config.setDetailRespClassName(txtDetailRespClassName.getText());
		return config;
	}

	/**
	 * 获得当前页面的配置信息,如果某个配置信息没有初始化就实例化并初始化基本属性,
	 *
	 * @return
	 * @throws Exception
	 */
	private HistoryConfig getThisHistoryConfigAndInit(DatabaseConfig databaseConfig, String selectedTableName) {
		try {
			HistoryConfig config = getThisHistoryConfig();
			if (config.getEntityConfig() == null) {
				EntityConfig entityConfig = Optional.ofNullable(ConfigUtil.getEntityConfig(Constant.DEFAULT)).orElse(new EntityConfig());
				List<TableAttributeEntity> columns = DBUtil.getTableColumns(databaseConfig, selectedTableName);
				if (entityConfig.isFieldCamel()) {
					for (TableAttributeEntity attr : columns) {
						attr.setTdField(StrUtil.unlineToCamel(attr.getTdColumnName()));
					}
				} else {
					for (TableAttributeEntity attr : columns) {
						attr.setTdField(attr.getTdColumnName());
					}
				}
				entityConfig.setTblPropertyValues(FXCollections.observableArrayList(columns));
				String primaryKey = DBUtil.getTablePrimaryKey(databaseConfig, selectedTableName);
				entityConfig.setPrimaryKey(primaryKey);
				config.setEntityConfig(entityConfig);
			}


			if (config.getDtoConfig() == null) {
				DtoConfig createDtoConfig = Optional.ofNullable(ConfigUtil.getDtoConfig(Constant.DEFAULT)).orElse(new DtoConfig());
				List<TableAttributeDto> columns = DBUtil.getTableColumnsForCreateDto(databaseConfig, selectedTableName);
				if (createDtoConfig.isFieldCamel()) {
					for (TableAttributeDto attr : columns) {
						attr.setTdField(StrUtil.unlineToCamel(attr.getTdColumnName()));
					}
				} else {
					for (TableAttributeDto attr : columns) {
						attr.setTdField(attr.getTdColumnName());
					}
				}
				createDtoConfig.setTblPropertyValues(FXCollections.observableArrayList(columns));
				String primaryKey = DBUtil.getTablePrimaryKey(databaseConfig, selectedTableName);
				createDtoConfig.setPrimaryKey(primaryKey);
				config.setDtoConfig(createDtoConfig);
			}

			if (config.getServiceConfig() == null) {
				config.setServiceConfig(
						Optional.ofNullable(ConfigUtil.getServiceConfig(Constant.DEFAULT)).orElse(new ServiceConfig().initDefaultValue()));
			}
			if (config.getServiceImplConfig() == null) {
				config.setServiceImplConfig(
						Optional.ofNullable(ConfigUtil.getServiceImplConfig(Constant.DEFAULT)).orElse(new ServiceImplConfig().initDefaultValue()));
			}
			if (config.getDaoConfig() == null) {
				config.setDaoConfig(Optional.ofNullable(ConfigUtil.getSQLConfig(Constant.DEFAULT)).orElse(new DaoConfig().initDefaultValue()));
			}
			if (config.getMapperConfig() == null) {
				config.setMapperConfig(
						Optional.ofNullable(ConfigUtil.getSqlAndParamsConfig(Constant.DEFAULT)).orElse(new MapperConfig().initDefaultValue()));
			}
			if (config.getControllerConfig() == null) {
				config.setControllerConfig(
						Optional.ofNullable(ConfigUtil.getRouterConfig(Constant.DEFAULT)).orElse(new ControllerConfig().initDefaultValue()));
			}
			if (config.getUnitTestConfig() == null) {
				config.setUnitTestConfig(
						Optional.ofNullable(ConfigUtil.getUnitTestConfig(Constant.DEFAULT)).orElse(new UnitTestConfig().initDefaultValue()));
			}
			if (config.getAssistConfig() == null) {
				config.setAssistConfig(
						Optional.ofNullable(ConfigUtil.getSqlAssistConfig(Constant.DEFAULT)).orElse(new SqlAssistConfig().initDefaultValue()));
			}
			if (config.getCustomConfig() == null) {
				config.setCustomConfig(
						Optional.ofNullable(ConfigUtil.getCustomConfig(Constant.DEFAULT)).orElse(new CustomConfig().initDefaultValue()));
			}
			if (config.getCustomPropertyConfig() == null) {
				config.setCustomPropertyConfig(Optional.ofNullable(ConfigUtil.getCustomPropertyConfig(Constant.DEFAULT))
						.orElse(new CustomPropertyConfig().initDefaultValue()));
			}
			return config;
		} catch (Exception e) {
			LOG.debug("执行初始化配置信息-->失败:", e);
		}
		return null;
	}

	/**
	 * 加载默认名字
	 */
	private void loadPlace() {
		entityNamePlace = txtEntityName.getText();
		createDtoNamePlace = txtCreateDtoName.getText();
		serviceNamePlace = txtServiceName.getText();
		serviceImplNamePlace = txtServiceImplName.getText();
		routerNamePlace = txtRouterName.getText();
		daoNamePlace = txtSqlName.getText();
		mapperNamePlace = txtMapperName.getText();
		addReqClassNamePlace = txtAddReqClassName.getText();
		updateReqClassNamePlace = txtUpdateReqClassName.getText();
		queryReqClassNamePlace = txtQueryReqClassName.getText();
		respClassNamePlace = txtRespClassName.getText();
		detailRespClassNamePlace = txtDetailRespClassName.getText();
	}

	/**
	 * 右边数据库树与事件
	 */
	@SuppressWarnings("unchecked")
	public void initTVDataBase() {
		LOG.debug("加载左侧数据库树与事件....");
		tvDataBase.setShowRoot(false);
		tvDataBase.setRoot(new TreeItem<>());
		Callback<TreeView<String>, TreeCell<String>> defaultCellFactory = TextFieldTreeCell.forTreeView();
		tvDataBase.setCellFactory((TreeView<String> tv) -> {
			TreeCell<String> cell = defaultCellFactory.call(tv);
			cell.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
				int level = tvDataBase.getTreeItemLevel(cell.getTreeItem());
				TreeCell<String> treeCell = (TreeCell<String>) event.getSource();
				TreeItem<String> treeItem = treeCell.getTreeItem();
				if (level == 1) {
					final ContextMenu contextMenu = new ContextMenu();
					MenuItem item0 = new MenuItem("打开连接");
					item0.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_TVMI_OPEN_CONNECT));
					item0.setOnAction(event1 -> {
						LOG.debug("执行打开数据库连接....");
						DatabaseConfig selectedConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();
						try {
							List<String> tables = DBUtil.getTableNames(selectedConfig);
							if (tables != null && tables.size() > 0) {
								ObservableList<TreeItem<String>> children = cell.getTreeItem().getChildren();
								children.clear();
								for (String tableName : tables) {
									TreeItem<String> newTreeItem = new TreeItem<>();
									ImageView imageView = new ImageView("image/table.png");
									imageView.setFitHeight(16);
									imageView.setFitWidth(16);
									newTreeItem.setGraphic(imageView);
									newTreeItem.setValue(tableName);
									children.add(newTreeItem);
								}
							}
						} catch (Exception e) {
							AlertUtil.showErrorAlert(e.getMessage());
							LOG.error("打开连接失败!!!" + e);
						}
					});
					MenuItem item1 = new MenuItem("关闭连接");
					item1.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_TVMI_CLOSE_CONNECT));
					item1.setOnAction(event1 -> {
						treeItem.getChildren().clear();
						initSearchMap(searchField.getText().trim());
					});
					MenuItem item3 = new MenuItem("修改连接");
					item3.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_TVMI_UPDATE_CONNECT));
					item3.setOnAction(event1 -> {
						updateOfDatabaseConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();
						if (updateOfDatabaseConfig != null) {
							LOG.debug("打开修改数据库连接窗口...");
							StringProperty property = Main.LANGUAGE.get(LanguageKey.PAGE_UPDATE_CONNECTION);
							String title = property == null ? "修改数据库连接" : property.get();
							UpdateConnection controller = (UpdateConnection) loadFXMLPage(title, FXMLPage.UPDATE_CONNECTION, false);
							controller.setIndexController(this);
							controller.init();
							controller.showDialogStage();
						}
					});
					MenuItem item2 = new MenuItem("删除连接");
					item2.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_TVMI_DELETE_CONNECT));
					item2.setOnAction(event1 -> {
						if (!AlertUtil.showConfirmAlert("确定删除该连接吗")) {
							return;
						}
						LOG.debug("执行删除数据库链接...");
						DatabaseConfig selectedConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();
						try {
							ConfigUtil.deleteDatabaseConfig(selectedConfig.getConnName());
							DBUtil.closeConnection(selectedConfig);
							DBUtil.closeMongoClient(selectedConfig);
							this.loadTVDataBase();
						} catch (Exception e) {
							AlertUtil.showErrorAlert("删除数据库连接失败: " + e.getMessage());
							LOG.error("删除数据库连接失败!!!" + e);
						}
					});

					MenuItem itemCreateAll = new MenuItem("全库生成");
					itemCreateAll.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_TVMI_CREATE_FULL_DB));
					itemCreateAll.setOnAction(event1 -> {
						if (StrUtil.isNullOrEmpty(txtProjectPath.getText())) {
							StringProperty property = Main.LANGUAGE.get(LanguageKey.TIPS_PATH_CANT_EMPTY);
							String title = property == null ? "生成的路径不能为空" : property.get();
							AlertUtil.showWarnAlert(title);
							return;
						}
						if (!AlertUtil.showConfirmAlert("确定当前数据库里面所有的表都生成吗?")) {
							return;
						}
						LOG.debug("执行全库生成...");
						DatabaseConfig selectedConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();

						createAllTable(selectedConfig);
					});
					contextMenu.getItems().addAll(itemCreateAll, item0, item1, item3, item2);
					cell.setContextMenu(contextMenu);
				}
				// 加载所有表
				if (event.getClickCount() == 2) {
					if (treeItem == null) {
						return;
					}
					treeItem.setExpanded(true);
					if (level == 1) {
						LOG.debug("加载所有表....");
						DatabaseConfig selectedConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();
						try {
							List<String> tables = DBUtil.getTableNames(selectedConfig);
							if (tables != null && tables.size() > 0) {
								ObservableList<TreeItem<String>> children = cell.getTreeItem().getChildren();
								children.clear();
								// 获得树节点
								for (String tableName : tables) {
									TreeItem<String> newTreeItem = new TreeItem<>();
									ImageView imageView = new ImageView("image/table.png");
									imageView.setFitHeight(18);
									imageView.setFitWidth(18);
									newTreeItem.setGraphic(imageView);
									newTreeItem.setValue(tableName);
									children.add(newTreeItem);
								}
							}
							LOG.debug("加载所有表成功!");

							initSearchMap(searchField.getText().trim());
						} catch (Exception e) {
							AlertUtil.showErrorAlert(e.getMessage());
							LOG.error("加载所有表失败!!!" + e);
						} finally {
							DBUtil.closeAllConnections();
						}
					} else if (level == 2) {
						LOG.debug("将表的数据加载到数据面板...");
						String tableName = treeCell.getTreeItem().getValue();
						selectedDatabaseConfig = (DatabaseConfig) treeItem.getParent().getGraphic().getUserData();
						selectedTableName = tableName;
						txtTableName.setText(tableName);
						String pascalTableName = StrUtil.unlineToPascal(tableName);
						txtEntityName.setText(entityNamePlace.replace("{c}", pascalTableName));
//						txtBeanName.setText(beanNamePlace.replace("{c}", pascalTableName));
						txtCreateDtoName.setText(createDtoNamePlace.replace("{c}", pascalTableName));
						txtServiceName.setText(serviceNamePlace.replace("{c}", pascalTableName));
						txtServiceImplName.setText(serviceImplNamePlace.replace("{c}", pascalTableName));
						txtRouterName.setText(routerNamePlace.replace("{c}", pascalTableName));
						txtMapperName.setText(mapperNamePlace.replace("{c}",pascalTableName));
						txtSqlName.setText(daoNamePlace.replace("{c}", pascalTableName));
						txtAddReqClassName.setText(addReqClassNamePlace.replace("{c}", pascalTableName));
						txtUpdateReqClassName.setText(updateReqClassNamePlace.replace("{c}", pascalTableName));
						txtQueryReqClassName.setText(queryReqClassNamePlace.replace("{c}", pascalTableName));
						txtRespClassName.setText(respClassNamePlace.replace("{c}", pascalTableName));
						txtDetailRespClassName.setText(detailRespClassNamePlace.replace("{c}", pascalTableName));

						LOG.debug("将表的数据加载到数据面板成功!");
					}
				}
			});
			return cell;
		});

		searchField.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue) {
				initSearchMap(searchField.getText());
			}
		});

		searchUp.setOnAction(btn -> {
			boolean searched = false;
			if (globalSearchMap.size() == 0) {
				return;
			}
			if (globalSearchCount == 0) {
				globalSearchCount = globalSearchMap.size();
			}
			List<Integer> indexes = globalSearchMap.values().stream().map(v -> tvDataBase.getRow(v)).collect(Collectors.toList());
			int treeItemLevel = tvDataBase.getTreeItemLevel(tvDataBase.getSelectionModel().getSelectedItem());
			if (treeItemLevel != 1) {
				for (int i = indexes.size() - 1; i >= 0; i--) {
					if (indexes.get(i) < tvDataBase.getSelectionModel().getSelectedIndex()) {
						globalSearchCount = i;
						searched = true;
						break;
					}
				}
				if (!searched) {
					globalSearchCount = globalSearchMap.size() - 1;
				}
			}
			focusTarget();
			globalSearchCount = Math.max(globalSearchCount - 1, 0);
		});

		searchDown.setOnAction(btn -> {
			boolean searched = false;
			if (globalSearchMap.size() == 0) {
				return;
			}
			if (globalSearchCount == globalSearchMap.size()) {
				globalSearchCount = 0;
			}
			List<Integer> indexes = globalSearchMap.values().stream().map(v -> tvDataBase.getRow(v)).collect(Collectors.toList());
			int treeItemLevel = tvDataBase.getTreeItemLevel(tvDataBase.getSelectionModel().getSelectedItem());
			if (treeItemLevel != 1) {
				for (int i = 0; i < indexes.size(); i++) {
					if (indexes.get(i) > tvDataBase.getSelectionModel().getSelectedIndex()) {
						globalSearchCount = i;
						searched = true;
						break;
					}
				}
				if (!searched) {
					globalSearchCount = 0;
				}
			}
			focusTarget();
			globalSearchCount++;
		});
	}

	/**
	 * 聚焦树形结构
	 */
	private void focusTarget() {
		TreeItem<String> treeItem = globalSearchMap.get(globalSearchCount);
		if (treeItem != null) {
			tvDataBase.getSelectionModel().select(treeItem);
			int selectedIndex = tvDataBase.getSelectionModel().getSelectedIndex();
			int index = selectedIndex > 10 ? selectedIndex - 10 : 0;
			tvDataBase.scrollTo(index);
			tvDataBase.getFocusModel().focus(selectedIndex);
			tvDataBase.requestFocus();
		}
	}

	private void initSearchMap(String trimed) {
		globalSearchCount = 0;
		globalSearchMap.clear();
		if (trimed.length() > 0) {
			// 获取所有的库节点
			TreeItem<String> root = tvDataBase.getRoot();
			ObservableList<TreeItem<String>> level1Children = root.getChildren();
			if (CollectionUtils.isEmpty(level1Children)) {
				return;
			}
			Integer count = 0;
			for (TreeItem<String> c : level1Children) {
				ObservableList<TreeItem<String>> level2Children = c.getChildren();
				if (CollectionUtils.isEmpty(level2Children)) {
					continue;
				}
				for (TreeItem<String> l : level2Children) {
					if (l.getValue().contains(trimed)) {
						globalSearchMap.put(count, l);
						count++;
					}
				}
			}
		}
	}


	/**
	 * 加载数据库到树集
	 *
	 * @throws Exception
	 */
	public void loadTVDataBase() throws Exception {
		TreeItem<String> rootTreeItem = tvDataBase.getRoot();
		rootTreeItem.getChildren().clear();
		List<DatabaseConfig> item = null;
		item = ConfigUtil.getDatabaseConfig();
		for (DatabaseConfig dbConfig : item) {
			if (dbConfig.getConnName().contains("mongo_")) continue;
			TreeItem<String> treeItem = new TreeItem<String>();
			treeItem.setValue(dbConfig.getConnName());
			ImageView dbImage = new ImageView("image/database.png");
			dbImage.setFitHeight(20);
			dbImage.setFitWidth(20);
			dbImage.setUserData(dbConfig);
			treeItem.setGraphic(dbImage);
			rootTreeItem.getChildren().add(treeItem);
		}
	}

	/**
	 * 加载模板文件夹里面所有模板的名字
	 *
	 * @throws IOException
	 */
	public void loadTemplate() {
		LOG.debug("执行加载模板文件夹里面所有模板的名字...");
		try {
			this.templateNameItems = Files.list(Paths.get(Constant.TEMPLATE_DIR_NAME)).filter(f -> f.getFileName().toString().endsWith(".ftl"))
					.map(p -> p.getFileName().toString()).collect(Collectors.toList());
			if (this.templateNameItems == null) {
				this.templateNameItems = new ArrayList<>();
			}
			LOG.debug("执行加载模板文件夹里面所有模板的名字-->成功!");
		} catch (IOException e) {
			LOG.error("执行加载模板文件夹里面所有模板的名字-->失败:", e);
			AlertUtil.showErrorAlert(e.toString());
		}
	}

	/**
	 * 获得模板需要的上下文
	 *
	 * @param databaseConfig
	 *          数据库配置文件
	 * @param tableName
	 *          表的名字,如果表名不为空,将类名设置为默认值占位表名,如果直接使用版面数据输入null
	 * @return
	 * @throws Exception
	 */
	public GeneratorContent getGeneratorContent(DatabaseConfig databaseConfig, String tableName) throws Exception {
		GeneratorContent content = new GeneratorContent();
		HistoryConfig history = getThisHistoryConfigAndInit(databaseConfig, tableName != null ? tableName : selectedTableName);
		// 数据库属性
		DatabaseContent databaseContent = new DatabaseContent();
		ConverterUtil.databaseConfigToContent(databaseConfig, databaseContent);
		content.setDatabase(databaseContent);

		// 数据库表属性
		TableContent tableContent = DBUtil.getTableAttribute(databaseConfig, tableName);
		content.setTable(tableContent);
		// 实体类属性
		EntityConfig ec = getThisHistoryConfigAndInit(databaseConfig, tableName != null ? tableName : selectedTableName).getEntityConfig();
		String className = tableName != null ? entityNamePlace.replace("{c}", StrUtil.unlineToPascal(tableName)) : txtEntityName.getText();
		String entityName = tableName != null ? entityNamePlace.replace("{c}", StrUtil.unlineToPascal(tableName)) : txtEntityName.getText();
		if (tableName == null) {
			tableName = selectedTableName;
		}
		EntityContent entityContent = new EntityContent(txtEntityPackage.getText(), entityName, tableName);
		ConverterUtil.entityConfigToContent(ec, entityContent);
		content.setEntity(entityContent);

		// CreateDto属性
		DtoConfig dc = history.getDtoConfig();
		String createDtoName = tableName != null ? createDtoNamePlace.replace("{c}", StrUtil.unlineToPascal(tableName)) : txtCreateDtoName.getText();
		CreateDtoContent createDtoContent = new CreateDtoContent(txtCreateDtoPackage.getText(), createDtoName, tableName);
		ConverterUtil.createDtoConfigToContent(dc, createDtoContent);
		content.setCreateDto(createDtoContent);
		attachApiLayerContents(content, dc, tableName);
		// Service属性
		ServiceConfig sc = history.getServiceConfig();
		String serviceName = tableName != null ? serviceNamePlace.replace("{c}", StrUtil.unlineToPascal(tableName)) : txtServiceName.getText();
		ServiceContent serviceContent = new ServiceContent(txtServicePackage.getText(), serviceName);
		ConverterUtil.serviceConfigToContent(sc, serviceContent, className);
		content.setService(serviceContent);
		// ServiceImpl属性
		ServiceImplConfig sci = history.getServiceImplConfig();
		String serviceNameImplName = tableName != null ? serviceImplNamePlace.replace("{c}", StrUtil.unlineToPascal(tableName)) : txtServiceImplName.getText();
		ServiceImplContent serviceImplContent = new ServiceImplContent(txtServiceImplPackage.getText(), serviceNameImplName);
		ConverterUtil.serviceImplConfigToContent(sci, serviceImplContent, className);
		content.setServiceImpl(serviceImplContent);
		// dao属性
		String sqlName = tableName != null ? daoNamePlace.replace("{c}", StrUtil.unlineToPascal(tableName)) : txtSqlName.getText();
		DaoConfig sql = history.getDaoConfig();
		DaoContent sqlContent = new DaoContent(txtSqlPackage.getText(), sqlName);
		ConverterUtil.SqlConfigToContent(sql, sqlContent, className);
		content.setDao(sqlContent);
		//mapper属性
		MapperConfig mapperConfig = history.getMapperConfig();
		String mapperName = tableName != null ? mapperNamePlace.replace("{c}", StrUtil.unlineToPascal(tableName)) : txtMapperName.getText();
		MapperContent mapperContent = new MapperContent(txtMapperPackage.getText(),mapperName);
		ConverterUtil.mapperConfigToContent(mapperConfig,mapperContent , mapperName);
		content.setMapper(mapperContent);
		// Controller属性
		String routerName = tableName != null ? routerNamePlace.replace("{c}", StrUtil.unlineToPascal(tableName)) : txtRouterName.getText();
		ControllerConfig router = history.getControllerConfig();
		ControllerContent routerContent = new ControllerContent(txtRouterPackage.getText(), routerName);
		ConverterUtil.routerConfigToContent(router, routerContent, className);
		content.setController(routerContent);
		// swagger 属性
		SwaggerContent swaggerContent = new SwaggerContent("com.zhs.common","Swagger","${server.port}");
		content.setSwagger(swaggerContent);
		//设置meta时间属性
		MetaContent metaContent = new MetaContent("com.zhs.handler","ZhsMetaObjectHandler");
		content.setHandler(metaContent);
		// 自定义包类属性
		CustomConfig customConfig = history.getCustomConfig();
		CustomContent customContent = new CustomContent();
		ConverterUtil.customConfigToContent(customConfig, customContent, className);
		content.setCustom(customContent);
		// 自定义属性
		CustomPropertyConfig propertyConfig = history.getCustomPropertyConfig();
		CustomPropertyContent propertyContent = new CustomPropertyContent();
		ConverterUtil.customPropertyConfigToContent(propertyConfig, propertyContent, className);
		content.setCustomProperty(propertyContent);
		return content;
	}

	/**
	 * 填充 Request/Response 分层上下文（字段与 DTO 配置一致）
	 */
	private void attachApiLayerContents(GeneratorContent content, DtoConfig dc, String tableName) {
		if (StrUtil.isNullOrEmpty(tableName)) {
			tableName = selectedTableName;
		}
		String pascal = tableName != null ? StrUtil.unlineToPascal(tableName) : "";
		String addName = tableName != null ? addReqClassNamePlace.replace("{c}", pascal) : txtAddReqClassName.getText();
		String updateName = tableName != null ? updateReqClassNamePlace.replace("{c}", pascal) : txtUpdateReqClassName.getText();
		String queryName = tableName != null ? queryReqClassNamePlace.replace("{c}", pascal) : txtQueryReqClassName.getText();
		String respName = tableName != null ? respClassNamePlace.replace("{c}", pascal) : txtRespClassName.getText();
		String detailName = tableName != null ? detailRespClassNamePlace.replace("{c}", pascal) : txtDetailRespClassName.getText();
		CreateDtoContent addReq = new CreateDtoContent(txtRequestPackage.getText(), addName, tableName);
		ConverterUtil.createDtoConfigToContent(dc, addReq);
		content.setAddReq(addReq);
		CreateDtoContent updateReq = new CreateDtoContent(txtRequestPackage.getText(), updateName, tableName);
		ConverterUtil.createDtoConfigToContent(dc, updateReq);
		content.setUpdateReq(updateReq);
		CreateDtoContent queryReq = new CreateDtoContent(txtRequestPackage.getText(), queryName, tableName);
		ConverterUtil.createDtoConfigToContent(dc, queryReq);
		content.setQueryReq(queryReq);
		CreateDtoContent resp = new CreateDtoContent(txtResponsePackage.getText(), respName, tableName);
		ConverterUtil.createDtoConfigToContent(dc, resp);
		content.setResp(resp);
		CreateDtoContent detailResp = new CreateDtoContent(txtResponsePackage.getText(), detailName, tableName);
		ConverterUtil.createDtoConfigToContent(dc, detailResp);
		content.setDetailResp(detailResp);
	}

	/**
	 * 生成 AddReq / UpdateReq / QueryReq / Resp / DetailResp（与 DTO 同开关：配置了 DTO 模板名才生成）
	 *
	 * @param addSimpleName 不含 .java 的类名，与当前生成模式一致（全库用占位符展开，单表用界面输入）
	 */
	private void generateApiLayerFiles(HistoryConfig historyConfig, GeneratorContent content, String projectPath, String codeFormat, String addSimpleName,
			String updateSimpleName, String querySimpleName, String respSimpleName, String detailSimpleName) {
		DtoConfig dtoConfig = historyConfig.getDtoConfig();
		if (StrUtil.isNullOrEmpty(dtoConfig.getTemplateName())) {
			return;
		}
		boolean override = dtoConfig.isOverrideFile();
		try {
			CreateFileUtil.createFile(content, Constant.TEMPLATE_NAME_ADD_REQ, projectPath, txtRequestPackage.getText(), addSimpleName + Constant.JAVA_SUFFIX,
					codeFormat, override);
			LOG.debug("执行生成 AddReq-->成功!");
		} catch (Exception e) {
			LOG.error("执行生成 AddReq-->失败:", e);
		}
		try {
			CreateFileUtil.createFile(content, Constant.TEMPLATE_NAME_UPDATE_REQ, projectPath, txtRequestPackage.getText(),
					updateSimpleName + Constant.JAVA_SUFFIX, codeFormat, override);
			LOG.debug("执行生成 UpdateReq-->成功!");
		} catch (Exception e) {
			LOG.error("执行生成 UpdateReq-->失败:", e);
		}
		try {
			CreateFileUtil.createFile(content, Constant.TEMPLATE_NAME_QUERY_REQ, projectPath, txtRequestPackage.getText(),
					querySimpleName + Constant.JAVA_SUFFIX, codeFormat, override);
			LOG.debug("执行生成 QueryReq-->成功!");
		} catch (Exception e) {
			LOG.error("执行生成 QueryReq-->失败:", e);
		}
		try {
			CreateFileUtil.createFile(content, Constant.TEMPLATE_NAME_RESP, projectPath, txtResponsePackage.getText(), respSimpleName + Constant.JAVA_SUFFIX,
					codeFormat, override);
			LOG.debug("执行生成 Resp-->成功!");
		} catch (Exception e) {
			LOG.error("执行生成 Resp-->失败:", e);
		}
		try {
			CreateFileUtil.createFile(content, Constant.TEMPLATE_NAME_DETAIL_RESP, projectPath, txtResponsePackage.getText(),
					detailSimpleName + Constant.JAVA_SUFFIX, codeFormat, override);
			LOG.debug("执行生成 DetailResp-->成功!");
		} catch (Exception e) {
			LOG.error("执行生成 DetailResp-->失败:", e);
		}
	}

	/**
	 * 生成 {@code com.zhs.util.R}：与 Controller、ServiceImpl 模板 import 一致；已存在则不覆盖（override=false）。
	 */
	private void ensureUtilResponseR(GeneratorContent content, String projectPath, String codeFormat) {
		try {
			CreateFileUtil.createFile(content, Constant.TEMPLATE_NAME_R, projectPath, "com.zhs.util", "R" + Constant.JAVA_SUFFIX, codeFormat, false);
			LOG.debug("执行生成或跳过 R 统一响应类-->完成");
		} catch (Exception e) {
			LOG.error("执行生成 R-->失败:", e);
		}
	}

	/**
	 * 将数据库中所有的表创建
	 *
	 * @param databaseConfig
	 */
	public void createAllTable(DatabaseConfig databaseConfig) {
		try {
			List<String> tables = DBUtil.getTableNames(databaseConfig);
			if (tables.size() == 0) {
				AlertUtil.showWarnAlert("当前数据库不存在表");
				return;
			}
			double progIndex = 1.0 / tables.size();
			probCreateAll.setVisible(true);
			Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					try {
						for (int i = 0; i < tables.size(); i++) {
							updateProgress(progIndex * (i + 1), 1.0);
							updateMessage(runCreateTipsText + " {t} ...".replace("{t}", tables.get(i)));
							createAllRun(databaseConfig, tables.get(i));
							loadIndexConfigInfo(historyConfigName == null ? "default" : historyConfigName);
						}
						updateMessage("创建成功!");
						LOG.debug("执行全库生成-->成功");
						return null;
					} finally {
						DBUtil.closeAllConnections();
					}
				}
			};
			probCreateAll.progressProperty().bind(task.progressProperty());
			lblRunCreateAllTips.textProperty().bind(task.messageProperty());
			new Thread(task).start();
		} catch (Exception e) {
			AlertUtil.showErrorAlert("创建文件失败:" + e);
			LOG.error("执行创建文件-->失败:" + e);
		}
	}
	/**
	 * 全库生成的执行方法
	 *
	 * @param databaseConfig
	 *          数据库连接信息
	 * @param tableName
	 *          表的名字
	 * @throws Exception
	 */
	public void createAllRun(DatabaseConfig databaseConfig, String tableName) throws Exception {
		HistoryConfig historyConfig = getThisHistoryConfigAndInit(databaseConfig, tableName);
		GeneratorContent content = getGeneratorContent(databaseConfig, tableName);
		// 项目生成的路径
		String projectPath = txtProjectPath.getText();
		String codeFormat = cboCodeFormat.getValue();
		// 实体类的名字
		String entityName = StrUtil.unlineToPascal(tableName);
		// 生成实体类
		try {
			EntityConfig config = historyConfig.getEntityConfig();
			if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
				CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtEntityPackage.getText(),
						entityNamePlace.replace("{c}", entityName) + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
			}
			LOG.debug("执行将" + tableName + "生成实体类-->成功!");
		} catch (Exception e) {
			LOG.error("执行将" + tableName + "生成实体类-->失败:", e);
		}

		// 生成 Service接口（与单表 onCreate 一致，全库此前漏掉导致只有 impl）
		try {
			ServiceConfig svcConfig = historyConfig.getServiceConfig();
			if (!StrUtil.isNullOrEmpty(svcConfig.getTemplateName())) {
				CreateFileUtil.createFile(content, svcConfig.getTemplateName(), projectPath, txtServicePackage.getText(),
						serviceNamePlace.replace("{c}", entityName) + Constant.JAVA_SUFFIX, codeFormat, svcConfig.isOverrideFile());
			}
			LOG.debug("执行将" + tableName + "生成Service-->成功!");
		} catch (Exception e) {
			LOG.error("执行将" + tableName + "生成Service-->失败:", e);
		}

		ensureUtilResponseR(content, projectPath, codeFormat);

		// 生成ServiceImpl
		try {
			ServiceImplConfig config = historyConfig.getServiceImplConfig();
			if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
				CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtServiceImplPackage.getText(),
						serviceImplNamePlace.replace("{c}", entityName) + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
			}
			LOG.debug("执行将" + tableName + "生成ServiceImpl-->成功!");
		} catch (Exception e) {
			LOG.error("执行将" + tableName + "生成ServiceImpl-->失败:", e);
		}
		// 生成dao（Mapper 接口）
		try {
			DaoConfig config = historyConfig.getDaoConfig();
			if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
				CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtSqlPackage.getText(),
						daoNamePlace.replace("{c}", entityName) + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
			}
			LOG.debug("执行将" + tableName + "生成DAO-->成功!");
		} catch (Exception e) {
			LOG.error("执行将" + tableName + "生成DAO-->失败:", e);
		}

		// 生成Router
		try {
			ControllerConfig config = historyConfig.getControllerConfig();
			if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
				CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtRouterPackage.getText(),
						routerNamePlace.replace("{c}", entityName) + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
			}
			LOG.debug("执行将" + tableName + "生成Router-->成功!");
		} catch (Exception e) {
			LOG.error("执行将" + tableName + "生成Router-->失败:", e);
		}

		// 生成 DTO（与单表一致）
		try {
			DtoConfig dtoConfig = historyConfig.getDtoConfig();
			if (!StrUtil.isNullOrEmpty(dtoConfig.getTemplateName())) {
				CreateFileUtil.createFile(content, dtoConfig.getTemplateName(), projectPath, txtCreateDtoPackage.getText(),
						createDtoNamePlace.replace("{c}", entityName) + Constant.JAVA_SUFFIX, codeFormat, dtoConfig.isOverrideFile());
			}
			LOG.debug("执行将" + tableName + "生成DTO-->成功!");
		} catch (Exception e) {
			LOG.error("执行将" + tableName + "生成DTO-->失败:", e);
		}

		generateApiLayerFiles(historyConfig, content, projectPath, codeFormat, addReqClassNamePlace.replace("{c}", entityName),
				updateReqClassNamePlace.replace("{c}", entityName), queryReqClassNamePlace.replace("{c}", entityName),
				respClassNamePlace.replace("{c}", entityName), detailRespClassNamePlace.replace("{c}", entityName));

		// 生成 mapper.xml（与单表一致）
		try {
			MapperConfig mapperConfig = historyConfig.getMapperConfig();
			if (!StrUtil.isNullOrEmpty(mapperConfig.getTemplateName())) {
				String templateName = mapperConfig.getTemplateName();
				if (templateName.equals(Constant.TEMPLATE_NAME_MAPPER)) {
					templateName = databaseConfig.getDbType() + Constant.TEMPLATE_NAME_MAPPER_SUFFIX;
				}
				CreateFileUtil.createFile(content, templateName, projectPath, txtMapperPackage.getText(),
						mapperNamePlace.replace("{c}", entityName), codeFormat, mapperConfig.isOverrideFile());
			}
			LOG.debug("执行将" + tableName + "生成Mapper XML-->成功!");
		} catch (Exception e) {
			LOG.error("执行将" + tableName + "生成Mapper XML-->失败:", e);
		}

		// Swagger、Meta：与单表相同，每表循环会覆盖同一固定路径文件，结果与多次单表生成一致
		try {
			SwaggerConfig swaggerConfig = new SwaggerConfig();
			if (!StrUtil.isNullOrEmpty(swaggerConfig.getTemplateName())) {
				String templateName = swaggerConfig.getTemplateName();
				if (templateName.equals(Constant.TEMPLATE_NAME_SWAGGER)) {
					templateName = Constant.TEMPLATE_NAME_SWAGGER;
				}
				CreateFileUtil.createFile(content, templateName, projectPath, "com.zhs.common", "Swagger" + Constant.JAVA_SUFFIX, codeFormat,
						swaggerConfig.isOverrideFile());
			}
			LOG.debug("执行将" + tableName + "生成Swagger-->成功!");
		} catch (Exception e) {
			LOG.error("执行将" + tableName + "生成Swagger-->失败:", e);
		}
		try {
			MetaConfig metaConfig = new MetaConfig();
			if (!StrUtil.isNullOrEmpty(metaConfig.getTemplateName())) {
				String templateName = metaConfig.getTemplateName();
				if (templateName.equals(Constant.TEMPLATE_NAME_SWAGGER)) {
					templateName = Constant.TEMPLATE_NAME_SWAGGER;
				}
				CreateFileUtil.createFile(content, templateName, projectPath, "com.zhs.handler", "ZhsMetaObjectHandler" + Constant.JAVA_SUFFIX, codeFormat,
						metaConfig.isOverrideFile());
			}
			LOG.debug("执行将" + tableName + "生成MetaHandler-->成功!");
		} catch (Exception e) {
			LOG.error("执行将" + tableName + "生成MetaHandler-->失败:", e);
		}

		CustomConfig config = historyConfig.getCustomConfig();
		if (config.getTableItem() != null) {
			for (TableAttributeKeyValueTemplate custom : config.getTableItem()) {
				if (!StrUtil.isNullOrEmpty(custom.getTemplateValue())) {
					try {
						String loCase = StrUtil.fristToLoCase(entityName);
						String cpackage = custom.getPackageName().replace("{C}", entityName).replace("{c}", loCase);
						String name = custom.getClassName().replace("{C}", entityName).replace("{c}", loCase);
						CreateFileUtil.createFile(content, custom.getTemplateValue(), projectPath, cpackage, name + custom.getSuffix(), codeFormat,
								config.isOverrideFile());
					} catch (Exception e) {
						LOG.error("执行生成自定义生成包类-->失败:", e);
					}
				}
			}
		}

	}

	// ============================事件区域=================================
	/**
	 * 执行创建
	 *
	 * @param event
	 */
	public void onCreate(ActionEvent event) {
		LOG.debug("执行创建...");
		try {
			if (StrUtil.isNullOrEmpty(txtProjectPath.getText())) {
				StringProperty property = Main.LANGUAGE.get(LanguageKey.TIPS_PATH_CANT_EMPTY);
				String tips = property == null ? "生成的路径不能为空" : property.get();
				AlertUtil.showWarnAlert(tips);
				return;
			}
			if (StrUtil.isNullOrEmpty(txtTableName.getText())) {
				StringProperty property = Main.LANGUAGE.get(LanguageKey.INDEX_TIPS_CREATE_TABLE);
				String tips = property == null ? "请双击左侧数据选择想要生成的表,或者在左侧右键全库生成!" : property.get();
				AlertUtil.showWarnAlert(tips);
				return;
			}
			probCreateAll.setVisible(true);
			Task<Void> task = new Task<Void>() {
				@Override
				protected Void call() throws Exception {
					try {
					updateProgress(1, 15);
					// 项目生成的路径
					String projectPath = txtProjectPath.getText();
					String codeFormat = cboCodeFormat.getValue();
					HistoryConfig historyConfig = getThisHistoryConfigAndInit(selectedDatabaseConfig, txtTableName.getText());
					GeneratorContent content = getGeneratorContent(selectedDatabaseConfig, selectedTableName);
					// 生成实体类
					try {
						EntityConfig config = historyConfig.getEntityConfig();
						if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
							updateMessage(runCreateTipsText + " {t} ...".replace("{t}", txtEntityName.getText() + ""));
							CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtEntityPackage.getText(),
									txtEntityName.getText() + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
						}
						LOG.debug("执行生成实体类-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成实体类:" + txtEntityName.getText() + "失败:" + e);
						LOG.error("执行生成实体类-->失败:", e);
					}
					// 生成Service
					updateProgress(2, 15);
					try {
						ServiceConfig config = historyConfig.getServiceConfig();
						if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
							updateMessage(runCreateTipsText + " {t} ...".replace("{t}", txtServiceName.getText() + ""));
							CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtServicePackage.getText(),
									txtServiceName.getText() + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
						}
						LOG.debug("执行生成Service-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成Service:" + txtServiceName.getText() + "失败:" + e);
						LOG.error("执行生成Service-->失败:", e);
					}
					ensureUtilResponseR(content, projectPath, codeFormat);
					// 生成ServiceImpl
					updateProgress(3, 15);
					try {
						ServiceImplConfig config = historyConfig.getServiceImplConfig();
						if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
							updateMessage(runCreateTipsText + " {t} ...".replace("{t}", txtServiceImplName.getText() + ""));
							CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtServiceImplPackage.getText(),
									txtServiceImplName.getText() + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
						}
						LOG.debug("执行生成ServiceImpl-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成ServiceImpl:" + txtServiceImplName.getText() + "失败:" + e);
						LOG.error("执行生成ServiceImpl-->失败:", e);
					}
					// 生成SQL
					updateProgress(4, 15);
					try {
						DaoConfig config = historyConfig.getDaoConfig();
						if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
							updateMessage(runCreateTipsText + " {t} ...".replace("{t}", txtSqlName.getText() + ""));
							CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtSqlPackage.getText(),
									txtSqlName.getText() + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
						}
						LOG.debug("执行生成DAO-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成DAO:" + txtSqlName.getText() + "失败:" + e);
						LOG.error("执行生成DAO-->失败:", e);
					}
					// 生成Router
					updateProgress(5, 15);
					try {
						ControllerConfig config = historyConfig.getControllerConfig();
						if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
							updateMessage(runCreateTipsText + " {t} ...".replace("{t}", txtRouterName.getText() + ""));
							CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtRouterPackage.getText(),
									txtRouterName.getText() + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
						}
						LOG.debug("执行生成Controller-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成Controller:" + txtRouterName.getText() + "失败:" + e);
						LOG.error("执行生成Controller-->失败:", e);
					}
					updateProgress(6, 15);
					// 生成createDto实体类
					try {
						DtoConfig config = historyConfig.getDtoConfig();
						if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
							updateMessage(runCreateTipsText + " {t} ...".replace("{t}", txtCreateDtoName.getText() + ""));
							CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtCreateDtoPackage.getText(),
									txtCreateDtoName.getText() + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
						}
						LOG.debug("执行生成createDto实体类-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成createDto实体类:" + txtCreateDtoName.getText() + "失败:" + e);
						LOG.error("执行生成createDto实体类-->失败:", e);
					}

					updateProgress(7, 15);
					try {
						generateApiLayerFiles(historyConfig, content, projectPath, codeFormat, txtAddReqClassName.getText(), txtUpdateReqClassName.getText(),
								txtQueryReqClassName.getText(), txtRespClassName.getText(), txtDetailRespClassName.getText());
						LOG.debug("执行生成 Request/Response 分层-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成 Request/Response 分层失败:" + e);
						LOG.error("执行生成 Request/Response 分层-->失败:", e);
					}

					// 生成mapper.xml文件
					try {
						MapperConfig config = historyConfig.getMapperConfig();
						if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
							updateMessage(runCreateTipsText + " {t} ...".replace("{t}", txtMapperPackage.getText() + ""));
							String templateName = config.getTemplateName();
							if (templateName.equals(Constant.TEMPLATE_NAME_MAPPER)) {
								templateName = selectedDatabaseConfig.getDbType() + Constant.TEMPLATE_NAME_MAPPER_SUFFIX;
							}
							CreateFileUtil.createFile(content, templateName, projectPath, txtMapperPackage.getText(), txtMapperName.getText(), codeFormat,
									config.isOverrideFile());
						}
						LOG.debug("执行生成Mapper-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成Mapper:" + txtMapperName.getText() + "失败:" + e);
						LOG.error("执行生成Mapper-->失败:", e);
					}
					updateProgress(8, 15);
                    // 生成Swagger文件
                    try {
						SwaggerConfig swaggerConfig = new SwaggerConfig();
						if (!StrUtil.isNullOrEmpty(swaggerConfig.getTemplateName())) {
                            String templateName = swaggerConfig.getTemplateName();
                            if (templateName.equals(Constant.TEMPLATE_NAME_SWAGGER)) {
                                templateName = Constant.TEMPLATE_NAME_SWAGGER;
                            }
                            CreateFileUtil.createFile(content, templateName, projectPath, "com.zhs.common", "Swagger" + Constant.JAVA_SUFFIX, codeFormat,
									swaggerConfig.isOverrideFile());
                        }
                        LOG.debug("执行生成swagger-->成功!");
                    } catch (Exception e) {
                        updateMessage("执行生成swagger:失败:" + e);
                        LOG.error("执行生成swagger-->失败:", e);
                    }

					updateProgress(9, 15);

					//生成时间模板
					try {
						MetaConfig metaConfig = new MetaConfig();
						if (!StrUtil.isNullOrEmpty(metaConfig.getTemplateName())) {
							String templateName = metaConfig.getTemplateName();
							if (templateName.equals(Constant.TEMPLATE_NAME_SWAGGER)) {
								templateName = Constant.TEMPLATE_NAME_SWAGGER;
							}
							CreateFileUtil.createFile(content, templateName, projectPath, "com.zhs.handler", "ZhsMetaObjectHandler" + Constant.JAVA_SUFFIX, codeFormat,
									metaConfig.isOverrideFile());
						}
						LOG.debug("执行生成swagger-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成swagger:失败:" + e);
						LOG.error("执行生成swagger-->失败:", e);
					}

					updateProgress(10, 15);

					CustomConfig config = historyConfig.getCustomConfig();
					if (config.getTableItem() != null) {
						for (TableAttributeKeyValueTemplate custom : config.getTableItem()) {
							if (!StrUtil.isNullOrEmpty(custom.getTemplateValue())) {
								try {
									String loCase = StrUtil.fristToLoCase(txtEntityName.getText());
									String cpackage = custom.getPackageName().replace("{C}", txtEntityName.getText()).replace("{c}", loCase);
									String name = custom.getClassName().replace("{C}", txtEntityName.getText()).replace("{c}", loCase);
									updateMessage(runCreateTipsText + " {t} ...".replace("{t}", custom.getKey() + ""));
									CreateFileUtil.createFile(content, custom.getTemplateValue(), projectPath, cpackage, name + custom.getSuffix(),
											codeFormat, config.isOverrideFile());
								} catch (Exception e) {
									updateMessage("执行生成自定义生成包类:" + custom.getKey() + "失败:" + e);
									LOG.error("执行生成自定义生成包类-->失败:", e);
								}
							}
						}
					}
					updateProgress(15, 15);
					loadIndexConfigInfo(historyConfigName == null ? "default" : historyConfigName);
					updateMessage("创建成功!");
					LOG.debug("执行创建-->成功!");
					return null;
					} finally {
						DBUtil.closeAllConnections();
					}
				}
			};
			probCreateAll.progressProperty().bind(task.progressProperty());
			lblRunCreateAllTips.textProperty().bind(task.messageProperty());
			new Thread(task).start();
		} catch (Exception e) {
			AlertUtil.showErrorAlert("创建文件失败:" + e);
			LOG.error("执行创建-->失败:", e);
		}
	}

	/**
	 * 保存配置文件
	 * 
	 * @param event
	 */
	public void onSaveConfig(ActionEvent event) {
		LOG.debug("执行保存配置文件...");
		TextInputDialog dialog = new TextInputDialog("");
		dialog.setTitle("保存当前配置");
		StringProperty property = Main.LANGUAGE.get(LanguageKey.INDEX_SAVE_CONFIG_TIPS);
		String title = property == null ? "请输入配置名称:\\r\\n(表名不在保存范围内必须通过数据库加载!!!)" : property.get();
		dialog.setContentText(title);
		Optional<String> result = dialog.showAndWait();
		if (result.isPresent()) {
			String name = result.map(n -> n).orElse("null");
			try {
				HistoryConfig config = getThisHistoryConfig();
				config.setHistoryConfigName(name);
				ConfigUtil.saveHistoryConfig(config);
				AlertUtil.showInfoAlert("保存配置成功!");
				LOG.debug("保存配置成功!");
			} catch (Exception e) {
				AlertUtil.showErrorAlert("保存配置失败!失败原因:\r\n" + e.getMessage());
				LOG.error("保存配置失败!!!" + e);
			}
		}
	}

	/**
	 * 数据库连接
	 * 
	 * @param event
	 */
	public void onConnection(MouseEvent event) {
		StringProperty property = Main.LANGUAGE.get(LanguageKey.PAGE_CREATE_CONNECTION);
		String title = property == null ? "新建数据库连接" : property.get();
		ConnectionController controller = (ConnectionController) loadFXMLPage(title, FXMLPage.CONNECTION, false);
		controller.setIndexController(this);
		controller.showDialogStage();
	}

	/**
	 * 点击按钮，替换成mongodb模板
	 * @param event
	 */
	public void toChangeMongodbDatasource(Event event) {
		//获取当前舞台
		URL url = Thread.currentThread().getContextClassLoader().getResource("FXML/mongodb.fxml");
		FXMLLoader fxmlLoader = new FXMLLoader(url);
		Parent root = null;
		try {
			root = fxmlLoader.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		LOG.info("数据源切换成功");
		Stage stage = (Stage) buChangeMongodb.getScene().getWindow();
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
		MongodbController controller = fxmlLoader.getController();
		controller.setPrimaryStage(stage);
		controller.showDialogStage();
	}

	/**
	 * 配置信息
	 * 
	 * @param event
	 */
	public void onConfig(MouseEvent event) {
		HistoryConfigController controller = (HistoryConfigController) loadFXMLPage("配置信息管理", FXMLPage.HISTORY_CONFIG, false);
		controller.setIndexController(this);
		controller.showDialogStage();

	}

	/**
	 * 使用说明
	 * 
	 * @param event
	 */
	public void onInstructions(MouseEvent event) {
		AboutController controller = (AboutController) loadFXMLPage("使用说明", FXMLPage.ABOUT, false, false);
		controller.showDialogStage();
	}

	/**
	 * 打开设置的事件
	 * 
	 * @param event
	 */
	public void onSetting(MouseEvent event) {
		SettingController controller = (SettingController) loadFXMLPage("设置", FXMLPage.SETTING, false, false);
		controller.showDialogStage();
	}

	/**
	 * 选择项目文件
	 * 
	 * @param event
	 */
	public void onSelectProjectPath(ActionEvent event) {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		File file = directoryChooser.showDialog(super.getPrimaryStage());
		if (file != null) {
			txtProjectPath.setText(file.getPath());
			LOG.debug("选择文件项目目录:" + file.getPath());
		}
	}

	/**
	 * 打开设置实体类
	 * 
	 * @param event
	 */
	public void onSetEntity(ActionEvent event) {
		if (selectedTableName == null) {
			StringProperty property = Main.LANGUAGE.get(LanguageKey.INDEX_TIPS_SELECT_TABLE_NAME);
			String tips = property == null ? "请先选择数据库表!打开左侧数据库双击表名便可加载..." : property.get();
			AlertUtil.showWarnAlert(tips);
			return;
		}
		SetEntityAttributeController controller = (SetEntityAttributeController) loadFXMLPage("Entity Attribute Setting",
				FXMLPage.SET_ENTITY_ATTRIBUTE, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}

	/**
	 * 打开设置Bean实体类
	 *
	 * @param event
	 */
	public void onSetBean(ActionEvent event) {
		if (selectedTableName == null) {
			StringProperty property = Main.LANGUAGE.get(LanguageKey.INDEX_TIPS_SELECT_TABLE_NAME);
			String tips = property == null ? "请先选择数据库表!打开左侧数据库双击表名便可加载..." : property.get();
			AlertUtil.showWarnAlert(tips);
			return;
		}
		SetEntityAttributeController controller = (SetEntityAttributeController) loadFXMLPage("Entity Attribute Setting",
				FXMLPage.SET_BEAN_ATTRIBUTE, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}

	/**
	 * 打开设置实体类
	 *
	 * @param event
	 */
	public void onSetCreateDto(ActionEvent event) {
		if (selectedTableName == null) {
			StringProperty property = Main.LANGUAGE.get(LanguageKey.INDEX_TIPS_SELECT_TABLE_NAME);
			String tips = property == null ? "请先选择数据库表!打开左侧数据库双击表名便可加载..." : property.get();
			AlertUtil.showWarnAlert(tips);
			return;
		}
		SetDtoController controller = (SetDtoController) loadFXMLPage("Entity Attribute Setting",
				FXMLPage.SET_DTO_ATTRIBUTE, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}



	/**
	 * 打开设置Service
	 * 
	 * @param event
	 */
	public void onSetService(ActionEvent event) {
		SetServiceController controller = (SetServiceController) loadFXMLPage("Service Setting", FXMLPage.SET_ROUTER_SERVICE, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}

	/**
	 * 打开设置Service
	 * 
	 * @param event
	 */
	public void onSetServiceImpl(ActionEvent event) {
		SetServiceImplController controller = (SetServiceImplController) loadFXMLPage("Service implement Setting",
				FXMLPage.SET_ROUTER_SERVICE_IMPL, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}

	/**
	 * 打开设置Router
	 * 
	 * @param event
	 */
	public void onSetRouter(ActionEvent event) {
		SetRouterController controller = (SetRouterController) loadFXMLPage("Controller Setting", FXMLPage.SET_ROUTER, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}

	/**
	 * 打开设置SetSQL
	 * 
	 * @param event
	 */
	public void onSetSQL(ActionEvent event) {
		SetSqlController controller = (SetSqlController) loadFXMLPage("DAO Setting", FXMLPage.SET_SQL, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}

	/**
	 * 打开设置SetSqlAssist
	 * 
	 * @param event
	 */
	public void onSetSqlAssist(ActionEvent event) {
		SetSqlAssistController controller = (SetSqlAssistController) loadFXMLPage("Assist Setting", FXMLPage.SET_ASSIST, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}

	/**
	 * 打开设置SqlAndParams
	 * 
	 * @param event
	 */
	public void onSetMapper(ActionEvent event) {
		SetMapperController controller = (SetMapperController) loadFXMLPage("Mapper Setting", FXMLPage.SET_MAPPER, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}

	/**
	 * 打开设置单元测试
	 * 
	 * @param event
	 */
	public void onSetUnitTest(ActionEvent event) {
		SetUnitTestController controller = (SetUnitTestController) loadFXMLPage("UnitTest Setting", FXMLPage.SET_UNIT_TEST, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}

	/**
	 * 打开设置SetCustom
	 * 
	 * @param event
	 */
	public void onSetCustom(ActionEvent event) {
		SetCustomController controller = (SetCustomController) loadFXMLPage("SetCustom Setting", FXMLPage.SET_CUSTOM, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}

	/**
	 * 打开设置CustomProperty
	 * 
	 * @param event
	 */
	public void onSetCustomProperty(ActionEvent event) {
		SetCustomPropertyController controller = (SetCustomPropertyController) loadFXMLPage("SetCustomProperty Setting",
				FXMLPage.SET_CUSTOM_PROPERTY, false);
		controller.setIndexController(this);
		controller.showDialogStage();
		controller.init();
	}
	// ======================get/set============================
	/**
	 * 获得当前选择数据库的信息
	 * 
	 * @return
	 */
	public DatabaseConfig getSelectedDatabaseConfig() {
		return selectedDatabaseConfig;
	}

	/**
	 * 设置当前选择数据库的信息
	 * 
	 * @param selectedDatabaseConfig
	 */
	public void setSelectedDatabaseConfig(DatabaseConfig selectedDatabaseConfig) {
		this.selectedDatabaseConfig = selectedDatabaseConfig;
	}

	/**
	 * 获得更新数据库选择的配置文件
	 * 
	 * @return
	 */
	public DatabaseConfig getUpdateOfDatabaseConfig() {
		return updateOfDatabaseConfig;
	}

	/**
	 * 设置更新数据库选择的配置文件
	 * 
	 * @param updateOfDatabaseConfig
	 */
	public void setUpdateOfDatabaseConfig(DatabaseConfig updateOfDatabaseConfig) {
		this.updateOfDatabaseConfig = updateOfDatabaseConfig;
	}

	/**
	 * 获得配置信息的名字
	 * 
	 * @return
	 */
	public String getHistoryConfigName() {
		return historyConfigName;
	}

	/**
	 * 设置配置信息的名字
	 * 
	 * @param historyConfigName
	 */
	public void setHistoryConfigName(String historyConfigName) {
		this.historyConfigName = historyConfigName;
	}

	/**
	 * 获得配置信息
	 * 
	 * @return
	 */
	public HistoryConfig getHistoryConfig() {
		return historyConfig;
	}

	/**
	 * 设置配置信息
	 * 
	 * @param historyConfig
	 */
	public void setHistoryConfig(HistoryConfig historyConfig) {
		this.historyConfig = historyConfig;
	}

	/**
	 * 获得当前数据库选择表的名字
	 * 
	 * @return
	 */
	public String getSelectedTableName() {
		return selectedTableName;
	}

	/**
	 * 设置当前数据库选择表的名字
	 * 
	 * @param selectedTableName
	 */
	public void setSelectedTableName(String selectedTableName) {
		this.selectedTableName = selectedTableName;
	}

	/**
	 * 获得模板文件夹现有模板名字
	 * 
	 * @return
	 */
	public List<String> getTemplateNameItems() {
		return templateNameItems;
	}



	/**
	 * 模板文件夹现有模板名字
	 * 
	 * @param templateNameItems
	 */
	public void setTemplateNameItems(List<String> templateNameItems) {
		this.templateNameItems = templateNameItems;
	}

}
