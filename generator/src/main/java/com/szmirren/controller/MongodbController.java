package com.szmirren.controller;

import com.szmirren.Main;
import com.szmirren.common.*;
import com.szmirren.entity.*;
import com.szmirren.models.MongoTypeDto;
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
public class MongodbController extends BaseController {
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
	/** 单元测试默认占位符 */
	private String unitTestPlace;

	// ========================fxml控件============================
	/** 数据库连接 */
	@FXML
	private Label mongodbConnection;

	@FXML
	private Button buChangeMysql;
	/** 存放目录 */
	@FXML
	private Label lblProjectPath;
	/** 数据库表名 */
	@FXML
	private Label lblTableName;
	/** 实体类包名 */
	@FXML
	private Label lblMongoDtoPackage;
	/** Service包名 */
	@FXML
	private Label lblServicePackage;
	/** ServiceImpl包名 */
	@FXML
	private Label lblServiceImplPackage;
	/** router包名 */
	@FXML
	private Label lblRouterPackage;
	/** 实体类类名 */
	@FXML
	private Label lblMongoDtoName;

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

	/** 自定义包名与类 */
	@FXML
	private Label lblSetCustom;
	/** 自定义属性 */
	@FXML
	private Label lblSetCustomProperty;
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
	private TreeView<String> mongoTvDataBase;
	/** 存放目录 */
	@FXML
	private TextField txtProjectPath;
	/** 数据库表名 */
	@FXML
	private TextField txtTableName;
	/** 实体类包名 */
	@FXML
	private TextField txtEntityPackage;
	/** Service包名 */
	@FXML
	private TextField txtServicePackage;
	/** ServiceImpl包名 */
	@FXML
	private TextField txtServiceImplPackage;
	/** router包名 */
	@FXML
	private TextField txtRouterPackage;

	/** 实体类类名 */
	@FXML
	private TextField txtEntityName;
	/** Bean实体类类名 */

	/** Service类名 */
	@FXML
	private TextField txtServiceName;
	/** ServiceImpl类名 */
	@FXML
	private TextField txtServiceImplName;
	/** router类名 */
	@FXML
	private TextField txtRouterName;
	/** 选择根目录按钮 */
	@FXML
	private Button btnSelectFile;
	/** 执行创建 */
	@FXML
	private Button btnRunCreate;
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
		mongodbConnection.setGraphic(lblConnImage);
		mongodbConnection.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_MONGODB_CONNECTION));
		//点击事件
		mongodbConnection.setOnMouseClicked(this::mongodbConnection);
		mongodbConnection.widthProperty().addListener(event -> buChangeMysql.setLayoutX(ml + mongodbConnection.getLayoutX() + mongodbConnection.getWidth()));

		//初始化数据源切换
		ImageView changeDatasourceConnImage = new ImageView("image/computer.png");
		changeDatasourceConnImage.setFitHeight(40);
		changeDatasourceConnImage.setFitWidth(40);
		buChangeMysql.setGraphic(changeDatasourceConnImage);
		buChangeMysql.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_CHANGE_DATASOURCE));
		buChangeMysql.setOnMouseClicked(event -> toChangeMysqlDatasource(event));

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
		txtRouterPackage.setText(config.getMongoControllerPackage());
		if (txtRouterName.getText().contains("{c}")) {
			txtRouterName.setText(config.getMongoControllerPackage());
		}
		cboCodeFormat.setValue(config.getCodeFormat());
	}

	/**
	 * 初始化语言
	 */
	private void initLanguage() {
		lblProjectPath.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_PROJECT_PATH));
		txtProjectPath.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_TXT_PROJECT_PATH));
		lblTableName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_TABLE_NAME));
		txtTableName.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_TXT_MONGO_COllECTION_NAME));
		lblMongoDtoPackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_MONGODB_DTO_PACKAGE_NAME));
		lblMongoDtoName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_MONGO_DTO_NAME));

		lblServicePackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SERVICE_PACKAGE));
		lblServiceName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SERVICE_NAME));
		lblServiceImplPackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SERVICE_IMPL_PACKAGE));
		lblServiceImplName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SERVICE_IMPL_NAME));
		lblRouterPackage.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_ROUTER_PACKAGE));
		lblRouterName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_ROUTER_NAME));
		lblSetCustom.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SET_CUSTOM));
		lblSetCustomProperty.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_SET_CUSTOM_PROPERTY));
		lblCodeFormat.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_LBL_CODE_FORMAT));
		btnSelectFile.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_BTN_SELECT_FILE));
		btnRunCreate.textProperty().bind(Main.LANGUAGE.get(LanguageKey.INDEX_BTN_RUN_CREATE));
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
		String servicePackage = txtServicePackage.getText();
		String serviceName = txtServiceName.getText();
		String serviceImplPackage = txtServiceImplPackage.getText();
		String serviceImplName = txtServiceImplName.getText();
		String controllerPackage = txtRouterPackage.getText();
		String controllerName = txtRouterName.getText();
		String codeFormat = cboCodeFormat.getValue();
		HistoryConfig config = new HistoryConfig(projectPath, entityPackage, entityName, null, null, servicePackage, serviceName, serviceImplPackage,
				serviceImplName, controllerPackage, controllerName, null, null,null, null, codeFormat,entityPackage,entityName,controllerPackage,controllerName);
		config.setDbConfig(selectedDatabaseConfig);
		config.setMongoDtoConfig(historyConfig.getMongoDtoConfig());
		config.setEntityConfig(historyConfig.getEntityConfig());
		config.setMongoControllerConfig(historyConfig.getMongoControllerConfig());
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
		if (historyConfig != null) {
			config.setRequestPackage(historyConfig.getRequestPackage());
			config.setResponsePackage(historyConfig.getResponsePackage());
			config.setAddReqClassName(historyConfig.getAddReqClassName());
			config.setUpdateReqClassName(historyConfig.getUpdateReqClassName());
			config.setQueryReqClassName(historyConfig.getQueryReqClassName());
			config.setRespClassName(historyConfig.getRespClassName());
			config.setDetailRespClassName(historyConfig.getDetailRespClassName());
		}
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
			if (config.getMongoDtoConfig() == null) {
				MongoDtoConfig mongoDtoConfig = new MongoDtoConfig();
				//获取集合中的字段以及对应的字段类型
				config.setMongoDtoConfig(mongoDtoConfig);
			}
			if (config.getMongoControllerConfig() == null){
                MongoControllerConfig mongoControllerConfig = new MongoControllerConfig();
                config.setMongoControllerConfig(mongoControllerConfig);
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
		serviceNamePlace = txtServiceName.getText();
		serviceImplNamePlace = txtServiceImplName.getText();
		routerNamePlace = txtRouterName.getText();
	}

	/**
	 * 右边数据库树与事件
	 */
	@SuppressWarnings("unchecked")
	public void initTVDataBase() {
		LOG.debug("加载左侧数据库树与事件....");
		mongoTvDataBase.setShowRoot(false);
		mongoTvDataBase.setRoot(new TreeItem<>());
		Callback<TreeView<String>, TreeCell<String>> defaultCellFactory = TextFieldTreeCell.forTreeView();
		mongoTvDataBase.setCellFactory((TreeView<String> tv) -> {
			TreeCell<String> cell = defaultCellFactory.call(tv);
			cell.addEventHandler(MouseEvent.MOUSE_CLICKED, event -> {
				int level = mongoTvDataBase.getTreeItemLevel(cell.getTreeItem());
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
							controller.setIndexController(new IndexController());
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
						LOG.debug("加载所有集合....");
						DatabaseConfig selectedConfig = (DatabaseConfig) treeItem.getGraphic().getUserData();
						try {
							List<String> tables = DBUtil.getMongoDbAllCollectionName(selectedConfig);
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
							LOG.debug("加载所有集合成功!");

							initSearchMap(searchField.getText().trim());
						} catch (Exception e) {
							e.printStackTrace();
							AlertUtil.showErrorAlert(e.getMessage());
							LOG.error("加载所有集合失败!!!" + e);
						}
					} else if (level == 2) {
						LOG.debug("将表的数据加载到数据面板...");
						String tableName = treeCell.getTreeItem().getValue();
						selectedDatabaseConfig = (DatabaseConfig) treeItem.getParent().getGraphic().getUserData();
						selectedTableName = tableName;
						txtTableName.setText(tableName);
						String pascalTableName = StrUtil.unlineToPascal(tableName);
						txtEntityName.setText(entityNamePlace.replace("{c}", pascalTableName));
						txtServiceName.setText(serviceNamePlace.replace("{c}", pascalTableName));
						txtServiceImplName.setText(serviceImplNamePlace.replace("{c}", pascalTableName));
						txtRouterName.setText(routerNamePlace.replace("{c}", pascalTableName));

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
			List<Integer> indexes = globalSearchMap.values().stream().map(v -> mongoTvDataBase.getRow(v)).collect(Collectors.toList());
			int treeItemLevel = mongoTvDataBase.getTreeItemLevel(mongoTvDataBase.getSelectionModel().getSelectedItem());
			if (treeItemLevel != 1) {
				for (int i = indexes.size() - 1; i >= 0; i--) {
					if (indexes.get(i) < mongoTvDataBase.getSelectionModel().getSelectedIndex()) {
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
			List<Integer> indexes = globalSearchMap.values().stream().map(v -> mongoTvDataBase.getRow(v)).collect(Collectors.toList());
			int treeItemLevel = mongoTvDataBase.getTreeItemLevel(mongoTvDataBase.getSelectionModel().getSelectedItem());
			if (treeItemLevel != 1) {
				for (int i = 0; i < indexes.size(); i++) {
					if (indexes.get(i) > mongoTvDataBase.getSelectionModel().getSelectedIndex()) {
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
			mongoTvDataBase.getSelectionModel().select(treeItem);
			int selectedIndex = mongoTvDataBase.getSelectionModel().getSelectedIndex();
			int index = selectedIndex > 10 ? selectedIndex - 10 : 0;
			mongoTvDataBase.scrollTo(index);
			mongoTvDataBase.getFocusModel().focus(selectedIndex);
			mongoTvDataBase.requestFocus();
		}
	}

	private void initSearchMap(String trimed) {
		globalSearchCount = 0;
		globalSearchMap.clear();
		if (trimed.length() > 0) {
			// 获取所有的库节点
			TreeItem<String> root = mongoTvDataBase.getRoot();
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
		TreeItem<String> rootTreeItem = mongoTvDataBase.getRoot();
		rootTreeItem.getChildren().clear();
		List<DatabaseConfig> item = null;
		item = ConfigUtil.getDatabaseConfig();
		for (DatabaseConfig dbConfig : item) {
			if (!dbConfig.getConnName().contains("mongo_")) continue;
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
		String className = tableName != null ? entityNamePlace.replace("{c}", StrUtil.unlineToPascal(tableName)) : txtEntityName.getText();

		//设置mongoDto属性
		//设置列属性，让mongodb的列和java数据类型对应
		List<MongoTypeDto> mongoList = DBUtil.getMongoColumns(databaseConfig, tableName);
		//创建mongo上下文
		MongoDtoContent mongoDtoContent = new MongoDtoContent(txtEntityPackage.getText(), className , mongoList,tableName);
		content.setMongoDto(mongoDtoContent);

        //设置mongoListen属性
        MongoListenContent mongoListenContent = new MongoListenContent("com.zhs.configuration","ApplicationReadyListener");
        content.setMongoListen(mongoListenContent);

        //设置swagger属性
        // swagger 属性
        SwaggerContent swaggerContent = new SwaggerContent("com.zhs.common","Swagger","${server.port}");
        content.setSwagger(swaggerContent);

		//设置mongoController属性
		String routerName = tableName != null ? routerNamePlace.replace("{c}", StrUtil.unlineToPascal(tableName)) : txtRouterName.getText();
        MongoControllerContent routerContent = new MongoControllerContent(txtRouterPackage.getText(), routerName,tableName);
		content.setMongoController(routerContent);

		return content;
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
					for (int i = 0; i < tables.size(); i++) {
						updateProgress(progIndex * (i + 1), 1.0);
						updateMessage(runCreateTipsText + " {t} ...".replace("{t}", tables.get(i)));
						createAllRun(databaseConfig, tables.get(i));
						loadIndexConfigInfo(historyConfigName == null ? "default" : historyConfigName);
					}
					updateMessage("创建成功!");
					LOG.debug("执行全库生成-->成功");
					return null;
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

	private void ensureUtilResponseR(GeneratorContent content, String projectPath, String codeFormat) {
		try {
			CreateFileUtil.createFile(content, Constant.TEMPLATE_NAME_R, projectPath, "com.zhs.util", "R" + Constant.JAVA_SUFFIX, codeFormat, false);
			LOG.debug("执行生成或跳过 R 统一响应类-->完成");
		} catch (Exception e) {
			LOG.error("执行生成 R-->失败:", e);
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
					updateProgress(1, 15);
					// 项目生成的路径
					String projectPath = txtProjectPath.getText();
					String codeFormat = cboCodeFormat.getValue();
					//初始化配置文件
					HistoryConfig historyConfig = getThisHistoryConfigAndInit(selectedDatabaseConfig, txtTableName.getText());
					// todo
					GeneratorContent content = getGeneratorContent(selectedDatabaseConfig, selectedTableName);
					// 生成实体类
					try {
						MongoDtoConfig config = historyConfig.getMongoDtoConfig();
						if (!StrUtil.isNullOrEmpty(config.getTemplateName())) {
							updateMessage(runCreateTipsText + " {t} ...".replace("{t}", txtEntityName.getText() + ""));
							//创建实体类文件
							CreateFileUtil.createFile(content, config.getTemplateName(), projectPath, txtEntityPackage.getText(),
									txtEntityName.getText()  + Constant.JAVA_SUFFIX, codeFormat, config.isOverrideFile());
						}
						LOG.debug("执行生成实体类-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成实体类:" + txtEntityName.getText() + "失败:" + e);
						LOG.error("执行生成实体类-->失败:", e);
					}

					updateProgress(2, 15);

					// 生成mongoListen文件
					try {
						MongoListenConfig mongoListenConfig = new MongoListenConfig();
						if (!StrUtil.isNullOrEmpty(mongoListenConfig.getTemplateName())) {
							String templateName = mongoListenConfig.getTemplateName();
							if (templateName.equals(Constant.TEMPLATE_NAME_MONGO_LISTEN)) {
								templateName = Constant.TEMPLATE_NAME_MONGO_LISTEN;
							}
							CreateFileUtil.createFile(content, templateName, projectPath, "com.zhs.configuration", "ApplicationReadyListener" + Constant.JAVA_SUFFIX, codeFormat,
									mongoListenConfig.isOverrideFile());
						}
						LOG.debug("执行生成mongoListen-->成功!");
					} catch (Exception e) {
						updateMessage("执行生成mongoListen:失败:" + e);
						LOG.error("执行生成mongoListen-->失败:", e);
					}
					updateProgress(3, 15);

					ensureUtilResponseR(content, projectPath, codeFormat);

                    // 生成mongoController文件
                    try {
                        MongoControllerConfig mongoControllerConfig = new MongoControllerConfig();
                        if (!StrUtil.isNullOrEmpty(mongoControllerConfig.getTemplateName())) {
                            String templateName = mongoControllerConfig.getTemplateName();
                            if (templateName.equals(Constant.TEMPLATE_NAME_MONGO_CONTROLLER)) {
                                templateName = Constant.TEMPLATE_NAME_MONGO_CONTROLLER;
                            }
                            CreateFileUtil.createFile(content, templateName, projectPath,  txtRouterPackage.getText(),
                                    txtRouterName.getText() + Constant.JAVA_SUFFIX, codeFormat,
                                    mongoControllerConfig.isOverrideFile());
                        }
                        LOG.debug("执行生成mongoListen-->成功!");
                    } catch (Exception e) {
                        updateMessage("执行生成mongoListen:失败:" + e);
                        LOG.error("执行生成mongoListen-->失败:", e);
                    }
                    updateProgress(4, 15);

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
                    updateProgress(5, 15);

					updateMessage("创建成功!");
					LOG.debug("执行创建-->成功!");
					return null;
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
	 * mongodb数据库连接
	 * @param event
	 */
	public void mongodbConnection(MouseEvent event) {
		//加载标题
		StringProperty property = Main.LANGUAGE.get(LanguageKey.PAGE_CREATE_MONGODB_CONNECTION);
		String title = property == null ? "新建mogodb连接" : property.get();
		//加载fxml页面数据
		MongodbConnectionController controller = (MongodbConnectionController) loadFXMLPage(title, FXMLPage.MONGODB_CONNECTION, false);
		controller.setMongodbController(this);
		controller.showDialogStage();
	}


	/**
	 * 点击按钮，替换成mongodb模板，
	 * @param event
	 */
	public void toChangeMysqlDatasource(Event event) {
		//获取当前的舞台
		URL url = Thread.currentThread().getContextClassLoader().getResource("FXML/Index.fxml");
		FXMLLoader fxmlLoader = new FXMLLoader(url);
		Parent root = null;
		try {
			root = fxmlLoader.load();
			LOG.debug("=======================" + root);
		} catch (IOException e) {
			e.printStackTrace();
		}
		// todo 换按钮名称
		Stage stage = (Stage) buChangeMysql.getScene().getWindow();
		Scene scene = new Scene(root);
		stage.setScene(scene);
		stage.show();
		IndexController controller = fxmlLoader.getController();
		controller.setPrimaryStage(stage);
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
	 * 打开设置SetCustom
	 *
	 * @param event
	 */
	public void onSetCustom(ActionEvent event) {
		SetCustomController controller = (SetCustomController) loadFXMLPage("SetCustom Setting", FXMLPage.SET_CUSTOM, false);
		controller.setIndexController(new IndexController());
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
		controller.setIndexController(new IndexController());
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
