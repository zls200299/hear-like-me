package com.szmirren.controller;

import com.szmirren.Main;
import com.szmirren.common.*;
import com.szmirren.models.TableAttributeBean;
import com.szmirren.models.TableAttributeBeanEditingCell;
import com.szmirren.options.DatabaseConfig;
import com.szmirren.options.BeanConfig;
import com.szmirren.view.AlertUtil;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TableView.TableViewSelectionModel;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

/**
 * 实体类属性控制器
 * 
 * @author <a href="http://szmirren.com">Mirren</a>
 *
 */
public class SetBeanAttributeController extends BaseController {
	private final Logger LOG = LogManager.getLogger(getClass());
	/** 首页的控制器 */
	private IndexController indexController;
	/** 存储信息table里面的所有属性 */
	private ObservableList<TableAttributeBean> tblPropertyValues;

	// =======================控件区域===========================
	@FXML
	private TableView<TableAttributeBean> tblProperty;
	/** 是否生成 */
	@FXML
	private TableColumn<TableAttributeBean, Boolean> tdIsCreate;
	/** 列名 */
	@FXML
	private TableColumn<TableAttributeBean, String> tdColumn;
	/** jdbc数据类型 */
	@FXML
	private TableColumn<TableAttributeBean, String> tdSqlType;
	/** java数据类型 */
	@FXML
	private TableColumn<TableAttributeBean, ComboBox<String>> tdJavaType;
	/** java属性名 */
	@FXML
	private TableColumn<TableAttributeBean, String> tdField;

	/** 表的别名 */
	@FXML
	private Label lblTableAlias;
	/** 主键的名称 */
	@FXML
	private Label lblPrimaryKey;
	/** 添加自定义字段 */
	@FXML
	private Label lblAddCustomProperty;
	/** 添加自定义字段类型 */
	@FXML
	private Label lblKey;
	/** 添加自定义字段名称 */
	@FXML
	private Label lblValue;
	/** 使用模板 */
	@FXML
	private Label lblTemplate;

	/** 表的别名输入框 */
	@FXML
	private TextField txtTableAlias;
	/** 主键名称输入框 */
	@FXML
	private TextField txtPrimaryKey;
	/** 自定义类型输入框 */
	@FXML
	private TextField txtKey;
	/** 自定义名称输入框 */
	@FXML
	private TextField txtValue;
	/** 添加自定义属性按钮 */
	@FXML
	private Button btnAddProperty;
	/** 保存配置文件 */
	@FXML
	private Button btnSaveConfig;
	/** 确定按钮 */
	@FXML
	private Button btnConfirm;
	/** 取消按钮 */
	@FXML
	private Button btnCancel;

	/** 如果文件存在就将其覆盖 */
	@FXML
	private CheckBox chkOverrideFile;
	/** 字段属性名使用驼峰命名 */
	@FXML
	private CheckBox chkFieldCamel;
	/** 使用模板 */
	@FXML
	private ComboBox<String> cboTemplate;

	/** 右边的pane */
	@FXML
	private AnchorPane apRightPane;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		tblProperty.setEditable(true);
		tblProperty.setStyle("-fx-font-size:14px");
		StringProperty property = Main.LANGUAGE.get(LanguageKey.SET_TBL_TIPS);
		String title = property == null ? "可以在右边自定义添加属性..." : property.get();
		tblProperty.setPlaceholder(new Label(title));
		tblPropertyValues = FXCollections.observableArrayList();
		// 设置列的大小自适应
		tblProperty.setColumnResizePolicy(resize -> {
			double width = resize.getTable().getWidth();
			tdIsCreate.setPrefWidth(width * 0.1);
			tdColumn.setPrefWidth(width * 0.2);
			tdSqlType.setPrefWidth(width * 0.2);
			tdJavaType.setPrefWidth(width * 0.25);
			tdField.setPrefWidth(width * 0.25);
			return true;
		});
		btnConfirm.widthProperty().addListener(w -> {
			double x = btnConfirm.getLayoutX() + btnConfirm.getWidth() + 10;
			btnCancel.setLayoutX(x);
		});
		// 设置输入包名自适应
		lblTableAlias.widthProperty().addListener(w -> {
			double x = lblTableAlias.getLayoutX() + lblTableAlias.getWidth() + 25;
			txtTableAlias.setLayoutY(x);
			txtTableAlias.setPrefWidth(apRightPane.getWidth() - x);
		});
		apRightPane.widthProperty().addListener(w -> {
			double x = lblTableAlias.getLayoutX() + lblTableAlias.getWidth() + 25;
			txtTableAlias.setLayoutY(x);
			txtTableAlias.setPrefWidth(apRightPane.getWidth() - x);
		});

		// 设置输入主键名称自适应
		lblPrimaryKey.widthProperty().addListener(w -> {
			double x = lblPrimaryKey.getLayoutX() + lblPrimaryKey.getWidth() + 25;
			txtPrimaryKey.setLayoutY(x);
			txtPrimaryKey.setPrefWidth(apRightPane.getWidth() - x);
		});
		apRightPane.widthProperty().addListener(w -> {
			double x = lblPrimaryKey.getLayoutX() + lblPrimaryKey.getWidth() + 25;
			txtPrimaryKey.setLayoutY(x);
			txtPrimaryKey.setPrefWidth(apRightPane.getWidth() - x);
		});
		// 设置输入自定义属性类型自适应
		lblKey.widthProperty().addListener(w -> {
			double x = lblKey.getLayoutX() + lblKey.getWidth() + 25;
			txtKey.setLayoutY(x);
			txtKey.setPrefWidth(apRightPane.getWidth() - x);
		});
		apRightPane.widthProperty().addListener(w -> {
			double x = lblKey.getLayoutX() + lblKey.getWidth() + 25;
			txtKey.setLayoutY(x);
			txtKey.setPrefWidth(apRightPane.getWidth() - x);
		});

		// 设置输入自定义属性名称自适应
		lblValue.widthProperty().addListener(w -> {
			double x = lblValue.getLayoutX() + lblValue.getWidth() + 25;
			txtValue.setLayoutY(x);
			txtValue.setPrefWidth(apRightPane.getWidth() - x);
		});
		apRightPane.widthProperty().addListener(w -> {
			double x = lblValue.getLayoutX() + lblValue.getWidth() + 25;
			txtValue.setLayoutY(x);
			txtValue.setPrefWidth(apRightPane.getWidth() - x);
		});

	}

	/**
	 * 初始化
	 */
	public void init() {
		LOG.debug("初始化SetBeanAttribute...");
		LOG.debug("初始化SetBeanAttribute->初始化属性...");
		// 添加右键删除属性
		StringProperty property = Main.LANGUAGE.get(LanguageKey.SET_TBL_MENU_ITEM_DELETE);
		String delMenu = property.get() == null ? "删除该属性" : property.get();
		MenuItem item = new MenuItem(delMenu);
		item.setOnAction(event -> {
			TableViewSelectionModel<TableAttributeBean> model = tblProperty.selectionModelProperty().get();
			StringProperty delConfirmP = Main.LANGUAGE.get(LanguageKey.SET_TBL_MENU_ITEM_DELETE_CONFIRM);
			String delConfirm = delConfirmP.get() == null ? "确定删除该属性吗" : delConfirmP.get();
			boolean isDel = AlertUtil.showConfirmAlert(delConfirm);
			if (isDel) {
				tblPropertyValues.remove(model.getSelectedItem());
			}
		});
		ContextMenu menu = new ContextMenu(item);
		Property<ContextMenu> tblCM = new SimpleObjectProperty<ContextMenu>(menu);
		tblProperty.contextMenuProperty().bind(tblCM);
		// 添加列
		Callback<TableColumn<TableAttributeBean, String>, TableCell<TableAttributeBean, String>> cellFactory = (
				TableColumn<TableAttributeBean, String> p) -> new TableAttributeBeanEditingCell();
		tdIsCreate.setCellFactory(CheckBoxTableCell.forTableColumn(tdIsCreate));
		tdIsCreate.setCellValueFactory(new PropertyValueFactory<>("tdCreate"));

		tdColumn.setCellValueFactory(new PropertyValueFactory<>("tdColumnName"));
		tdColumn.setCellFactory(cellFactory);
		tdColumn.setOnEditCommit((CellEditEvent<TableAttributeBean, String> t) -> {
			((TableAttributeBean) t.getTableView().getItems().get(t.getTablePosition().getRow())).setTdColumnName(t.getNewValue());
		});

		tdSqlType.setCellValueFactory(new PropertyValueFactory<>("tdJdbcType"));
		tdSqlType.setCellFactory(cellFactory);
		tdSqlType.setOnEditCommit((CellEditEvent<TableAttributeBean, String> t) -> {
			((TableAttributeBean) t.getTableView().getItems().get(t.getTablePosition().getRow())).setTdJdbcType(t.getNewValue());
		});
		tdJavaType.setCellValueFactory(new PropertyValueFactory<>("tdJavaType"));
		tdField.setCellValueFactory(new PropertyValueFactory<>("tdField"));
		tdField.setCellFactory(cellFactory);
		tdField.setOnEditCommit((CellEditEvent<TableAttributeBean, String> t) -> {
			((TableAttributeBean) t.getTableView().getItems().get(t.getTablePosition().getRow())).setTdField(t.getNewValue());
		});

		tblProperty.setItems(tblPropertyValues);;

		LOG.debug("初始化SetBeanAttribute->初始化模板文件名选择...");
		cboTemplate.getItems().addAll(indexController.getTemplateNameItems());
		if (indexController.getTemplateNameItems().contains(Constant.TEMPLATE_NAME_ENTITY)) {
			cboTemplate.setValue(Constant.TEMPLATE_NAME_ENTITY);
		}
		LOG.debug("初始化SetBeanAttribute->初始化配置信息...");
		// 该属性用来判断是否需要重新加载表格数据与主键
		boolean isInitProperyAndKey = true;
		if (indexController.getHistoryConfig() != null) {
			if (indexController.getHistoryConfig().getBeanConfig() == null) {
				loadConfig(getConfig());
			} else {
				LOG.debug("加载数据...从上一次保存获取");
				loadConfig(indexController.getHistoryConfig().getBeanConfig());
				isInitProperyAndKey = false;
			}
		} else {
			String configName = indexController.getHistoryConfigName();
			if (StrUtil.isNullOrEmpty(configName)) {
				configName = Constant.DEFAULT;
			}
			loadConfig(getConfig(configName));
		}
		initLanguage();
		if (isInitProperyAndKey) {
			LOG.debug("加载数据...从数据库获取");
			initTablePrimaryKey(indexController.getSelectedDatabaseConfig(), indexController.getSelectedTableName());
			initTablePropery(indexController.getSelectedDatabaseConfig(), indexController.getSelectedTableName());
		}
		LOG.debug("初始化SetBeanAttribute-->成功!");
	}

	/**
	 * 初始化语言
	 */
	private void initLanguage() {
		tdIsCreate.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_TD_CREATE));
		tdColumn.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_TD_COLUMN));
		tdSqlType.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_TD_SQL_TYPE));
		tdJavaType.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_TD_JAVA_TYPE));
		tdField.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_TD_FIELD));
		lblAddCustomProperty.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_LBL_ADD_CUSTOM_PROPERTY));
		lblKey.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_LBL_KEY));
		lblValue.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_LBL_VALUE));
		btnSaveConfig.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_BTN_SAVE_CONFIG));
		btnAddProperty.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_BTN_ADD_PROPERTY));
		btnConfirm.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_BTN_CONFIRM));
		btnCancel.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_BTN_CANCEL));
		txtKey.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_TXT_KEY));
		txtKey.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_TXT_KEY));
		txtValue.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_TXT_VALUE));
		chkOverrideFile.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_CHK_OVERRIDE_FILE));
		lblTemplate.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_LBL_TEMPLATE));
		cboTemplate.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_CBO_TEMPLATE));
		lblTableAlias.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_LBL_TABLE_ALIAS));
		txtTableAlias.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_TXT_TABLE_ALIAS));
		lblPrimaryKey.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_LBL_PRIMARY_KEY));
		txtPrimaryKey.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_ENTITY_TXT_PRIMARY_KEY));
		chkFieldCamel.textProperty().bind(Main.LANGUAGE.get(LanguageKey.SET_CHK_FIELD_CAMEL));
	}
	/**
	 * 初始化表格属性
	 */
	public void initTablePropery(DatabaseConfig selectedDatabaseConfig, String selectedTableName) {
		try {
			LOG.debug("执行获取表的列数据...");
			List<TableAttributeBean> columns = DBUtil.getTableColumnsForBean(selectedDatabaseConfig, selectedTableName);
			loadTablePropertyValues(columns);
			LOG.debug("执行获取表的列数据-->成功!");
		} catch (Exception e) {
			AlertUtil.showErrorAlert("获取表的列数据!失败原因:\r\n" + e.getMessage());
			LOG.error("执行获取表的列数据-->失败:", e);
		}
	}

	/**
	 * 初始化主键
	 */
	public void initTablePrimaryKey(DatabaseConfig selectedDatabaseConfig, String selectedTableName) {
		try {
			LOG.debug("执行获取表的主键列...");
			String key = DBUtil.getTablePrimaryKey(selectedDatabaseConfig, selectedTableName);
			if (key != null) {
				txtPrimaryKey.setText(key);
				LOG.debug("获取表的主键列成功!");
			} else {
				LOG.debug("获取表的主键列失败,当前表不存在主键!");
			}
		} catch (Exception e) {
			AlertUtil.showErrorAlert("获得主键失败!失败原因:\r\n" + e.getMessage());
			LOG.error("获取表的主键列失败!!!" + e);
		}

	}
	/**
	 * 从数据库中获取配置文件,使用默认值获取
	 * 
	 * @return
	 */
	public BeanConfig getConfig() {
		return getConfig(Constant.DEFAULT);
	}

	/**
	 * 从数据库中获取配置文件
	 * 
	 * @param name
	 * @return
	 */
	public BeanConfig getConfig(String name) {
		LOG.debug("执行从数据库中获取配置文件...");
		try {
			BeanConfig config = ConfigUtil.getBeanConfig(name);
			LOG.debug("执行获取配置文件-->成功!");
			if (config != null) {
				return config;
			}
		} catch (Exception e) {
			LOG.error("执行从数据库中获取配置文件-->失败:", e);
			AlertUtil.showErrorAlert("执行获得配置文件-->失败:" + e);
		}
		return new BeanConfig();
	}

	/**
	 * 获取当前控制器配置文件
	 * 
	 * @return
	 */
	public BeanConfig getThisConfig() {
		LOG.debug("执行获取当前页面配置文件...");
		BeanConfig config = new BeanConfig();
		config.setFieldCamel(chkFieldCamel.isSelected());
		config.setOverrideFile(chkOverrideFile.isSelected());
		config.setTemplateName(cboTemplate.getValue());
		LOG.debug("执行获取当前页面配置文件-->成功!");
		return config;
	}
	/**
	 * 获取当前控制器配置文件,带有表格数据
	 * 
	 * @return
	 */
	public BeanConfig getThisConfigHasProperty() {
		LOG.debug("执行获取当前页面带有表格数据配置文件...");
		BeanConfig config = new BeanConfig();
		config.setFieldCamel(chkFieldCamel.isSelected());
		config.setOverrideFile(chkOverrideFile.isSelected());
		config.setTemplateName(cboTemplate.getValue());
		config.setTblPropertyValues(tblPropertyValues);
		config.setTableAlias(txtTableAlias.getText());
		config.setPrimaryKey(txtPrimaryKey.getText());
		LOG.debug("执行获取当前页面带有表格数据配置文件-->成功!");
		return config;
	}

	/**
	 * 加载配置文件到当前页面
	 * 
	 * @param config
	 */
	public void loadConfig(BeanConfig config) {
		LOG.debug("执行加载配置文件到当前页面...");
		chkFieldCamel.setSelected(config.isFieldCamel());
		chkOverrideFile.setSelected(config.isOverrideFile());
		cboTemplate.setValue(config.getTemplateName());
		if (config.getTableAlias() != null) {
			txtTableAlias.setText(config.getTableAlias());
		}
		if (config.getPrimaryKey() != null) {
			txtPrimaryKey.setText(config.getPrimaryKey());
		}
		if (config.getTblPropertyValues() != null) {
			tblPropertyValues.addAll(config.getTblPropertyValues());
		}
		LOG.debug("执行加载配置文件到当前页面->成功!");
	}

	/**
	 * 加载表数据
	 */
	public void loadTablePropertyValues(List<TableAttributeBean> attr) {
		tblPropertyValues.clear();
		for (TableAttributeBean entity : attr) {
			if (chkFieldCamel.isSelected()) {
				entity.setTdField(StrUtil.unlineToCamel(entity.getTdColumnName()));
			}
			tblPropertyValues.add(entity);
		}
	}

	// =======================事件=================================
	/**
	 * 保存配置
	 * 
	 * @param event
	 */
	public void onSaveConfig(ActionEvent event) {
		LOG.debug("执行将配置文件保存到数据库...");
		try {
			String configName = indexController.getHistoryConfigName();
			if (StrUtil.isNullOrEmpty(configName)) {
				configName = Constant.DEFAULT;
			}
			ConfigUtil.saveBeanConfig(getThisConfig(), configName);
			LOG.debug("执行将配置文件保存到数据库-->成功!");
			AlertUtil.showInfoAlert("保存配置信息成功!");
		} catch (Exception e) {
			LOG.error("执行将配置文件保存到数据库-->失败:", e);
			AlertUtil.showErrorAlert("执行获得配置文件-->失败:" + e);
		}
	}

	/**
	 * 添加自定义属性
	 * 
	 * @param event
	 */
	public void onAddPropertyToTable(ActionEvent event) {
		LOG.debug("执行添加自定义属性...");
		TableAttributeBean field = new TableAttributeBean(txtKey.getText(), txtValue.getText());
		tblPropertyValues.add(field);
		LOG.debug("添加自定义属性-->成功!");
	}
	/**
	 * 是否驼峰命名
	 * 
	 * @param event
	 */
	public void onCamel(ActionEvent event) {
		ArrayList<TableAttributeBean> list = new ArrayList<>(tblPropertyValues);
		tblPropertyValues.clear();
		if (chkFieldCamel.isSelected()) {
			for (TableAttributeBean entity : list) {
				entity.setTdField(StrUtil.unlineToCamel(entity.getTdField()));
				tblPropertyValues.add(entity);
			}
			tblProperty.setItems(tblPropertyValues);
		} else {
			for (TableAttributeBean entity : list) {
				if (!StrUtil.isNullOrEmpty(entity.getTdColumnName())) {
					entity.setTdField(entity.getTdColumnName());
				}
				tblPropertyValues.add(entity);
			}
			tblProperty.setItems(tblPropertyValues);
		}
	}

	/**
	 * 取消关闭该窗口
	 * 
	 * @param event
	 */
	public void onCancel(ActionEvent event) {
		StringProperty property = Main.LANGUAGE.get(LanguageKey.SET_BTN_CANCEL_TIPS);
		String tips = property == null ? "如果取消,全部的设置都将恢复到默认值,确定取消吗?" : property.get();
		boolean result = AlertUtil.showConfirmAlert(tips);
		if (result) {
			indexController.getHistoryConfig().setBeanConfig(null);
			getDialogStage().close();
		}
	}

	/**
	 * 确定事件
	 * 
	 * @param event
	 */
	public void onConfirm(ActionEvent event) {
		indexController.getHistoryConfig().setBeanConfig(getThisConfigHasProperty());
		getDialogStage().close();
	}

	// ==================get/set=======================
	/**
	 * 获得首页控制器
	 * 
	 * @return
	 */
	public IndexController getIndexController() {
		return indexController;
	}

	/**
	 * 设置首页控制器
	 * 
	 * @param indexController
	 */
	public void setIndexController(IndexController indexController) {
		this.indexController = indexController;
	}

}
