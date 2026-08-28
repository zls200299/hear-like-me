package com.szmirren.controller;

import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import com.szmirren.Main;
import com.szmirren.common.ConfigUtil;
import com.szmirren.common.DBUtil;
import com.szmirren.common.LanguageKey;
import com.szmirren.options.DatabaseConfig;
import com.szmirren.view.AlertUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.util.ResourceBundle;

public class MongodbConnectionController extends BaseController {
	private final Logger LOG = LogManager.getLogger(getClass());
	private MongodbController mongodbController;
	/** 连接名称 */
	@FXML
	private Label lblConnName;
	/** 连接名称 */
	@FXML
	private Label lblConnURL;
	/** 监听端口号 */
	@FXML
	private Label lblListenPort;
	/** 数据库类型 */
	@FXML
	private Label lblDBType;
	/** 数据库名字 */
	@FXML
	private Label lblDBName;
	/** 数据库用户名 */
	@FXML
	private Label lblUserName;
	/** 数据库密码 */
	@FXML
	private Label lblUserPwd;
	/** 数据库编码格式 */
	@FXML
	private Label lblDBCoding;
	/** 连接名称 */
	@FXML
	private TextField txtConnName;
	/** 连接URL */
	@FXML
	private TextField txtConnURL;
	/** 监听端口号 */
	@FXML
	private TextField mongoPort;
	/** 数据库名称 */
	@FXML
	private TextField txtDBName;
	/** 数据库用户名 */
	@FXML
	private TextField txtUserName;
	/** 数据库用户密码 */
	@FXML
	private TextField txtUserPwd;
	/** 数据库类型 */
	@FXML
	private TextField cboDBType;
	/** 数据库编码格式 */
	@FXML
	private ComboBox<String> cboDBCoding;
	/** 测试连接 */
	@FXML
	private Button btnTestConn;
	/** 取消 */
	@FXML
	private Button btnCancel;
	/** 保存 */
	@FXML
	private Button btnSave;

	/** 数据树列表 */
	@FXML
	private TreeView<String> mongoTvDataBase;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		LOG.debug("初始化mongodb连接窗口....");
		// 初始化下拉列表
		cboDBType.setEditable(false);
		cboDBType.setText("mongodb连接");
		cboDBCoding.setEditable(true);
		cboDBCoding.getItems().addAll("utf8", "utf16", "utf32", "utf8mb4", "gb2312", "gbk", "ascii");
		cboDBCoding.setValue("utf8");
		btnTestConn.widthProperty().addListener(w -> {
			double cw = btnTestConn.getLayoutX() + btnTestConn.getWidth() + 20;
			btnSave.setLayoutX(cw);
		});
		btnSave.widthProperty().addListener(w -> {
			double cw = btnSave.getLayoutX() + btnSave.getWidth() + 20;
			btnCancel.setLayoutX(cw);
		});
		initLanguage();
		LOG.debug("初始化mongodb连接成功!");
	}

	/**
	 * 初始化语言
	 */
	private void initLanguage() {
		lblConnName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_LBL_CONN_NAME));
		lblConnURL.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_LBL_CONN_URL));
		lblListenPort.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_LBL_LISTEN_PORT));
		lblDBType.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_LBL_DB_TYPE));
		lblDBName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_LBL_DB_NAME));
		lblUserName.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_LBL_USER_NAME));
		lblUserPwd.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_LBL_USER_PWD));
		lblDBCoding.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_LBL_DB_CODING));
		txtConnName.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_TXT_CONN_NAME));
		txtConnURL.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_TXT_CONN_URL));
		mongoPort.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_MONGO_PORT));
		cboDBType.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_CBO_DB_TYPE));
		txtDBName.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_TXT_DB_NAME));
		txtUserName.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_TXT_USER_NAME));
		txtUserPwd.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_TXT_USER_PWD));
		cboDBCoding.promptTextProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_LBL_DB_CODING));
		btnTestConn.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_BTN_TEST_CONN));
		btnSave.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_BTN_SAVE));
		btnCancel.textProperty().bind(Main.LANGUAGE.get(LanguageKey.CONN_BTN_CANCEL));
	}


	/**
	 * 保存连接
	 * 
	 * @param event
	 */
	public void saveMongoConnection(ActionEvent event) {
		LOG.debug("执行保存数据库连接...");
		DatabaseConfig config = getMongoDatabaseConfig();
		if (config == null) {
			LOG.debug("连接数据库的数据为null,取消保存操作!!!");
			return;
		}
		try {
			ConfigUtil.saveDatabaseConfig(config.getConnName(), config,2);
			getDialogStage().close();
			//保存数据之后加载左侧栏页面
			mongodbController.loadTVDataBase();
			LOG.debug("保存数据库连接成功!");
		} catch (Exception e) {
			AlertUtil.showErrorAlert(e.getMessage());
			e.printStackTrace();
			LOG.error("保存数据库连接失败!!!" + e);
		}

	}

	/**
	 * 关闭当前窗口
	 * 
	 * @param event
	 */
	public void onCancel(ActionEvent event) {
		closeDialogStage();
	}

	/**
	 * 测试连接
	 * 
	 * @param event
	 */
	public void testConnection(ActionEvent event) {
		LOG.debug("执行测试数据库连接...");
		DatabaseConfig config = getMongoDatabaseConfig();
		if (config == null) {
			LOG.debug("连接数据库的数据为null,取消测试操作!!!");
			return;
		}
		try {
			DBUtil.getMongoConnection(config);
			AlertUtil.showInfoAlert("连接成功!");
			LOG.debug("数据库测试连接成功!");
		} catch (Exception e) {
			e.printStackTrace();
			AlertUtil.showErrorAlert("连接失败" + e.getMessage());
			LOG.error("数据库连接测试失败!!!" + e);
		}
	}

	/**
	 * 获得连接的所有字段
	 * 
	 * @return
	 */
	public DatabaseConfig getMongoDatabaseConfig() {
		String connName = txtConnName.getText().trim();
		String connURL = txtConnURL.getText().trim();
		String listenPort = mongoPort.getText().trim();
		String dbName = txtDBName.getText().trim();
		String userName = txtUserName.getText().trim();
		String userPwd = txtUserPwd.getText().trim();
		String dbType = cboDBType.getText().trim();
		String encoding = cboDBCoding.getValue();
		boolean isEmpty = validata(connName, connURL, listenPort, dbName, dbType, encoding);
		if (isEmpty) {
			DatabaseConfig config = new DatabaseConfig(connName, connURL, listenPort, dbName, userName, userPwd, dbType, encoding);
			return config;
		} else {
			AlertUtil.showWarnAlert("除了用户和密码以外所有属性都为必需填与选择");
			return null;
		}

	}

	/**
	 * 验证所有属性是否已经填写
	 * 
	 * @param str
	 * @return
	 */
	public boolean validata(String... str) {
		for (String string : str) {
			if (string == null || "".equals(string)) {
				return false;
			}
		}
		return true;
	}

	// ----------------------get/set----------------------------
	public MongodbController getMongodbController() {
		return mongodbController;
	}

	public void setMongodbController(MongodbController mongodbController) {
		this.mongodbController = mongodbController;
	}

	public TextField getTxtConnName() {
		return txtConnName;
	}

	public void setTxtConnName(TextField txtConnName) {
		this.txtConnName = txtConnName;
	}

	public TextField getTxtConnURL() {
		return txtConnURL;
	}

	public void setTxtConnURL(TextField txtConnURL) {
		this.txtConnURL = txtConnURL;
	}

	public TextField getmongoPort() {
		return mongoPort;
	}

	public void setmongoPort(TextField mongoPort) {
		this.mongoPort = mongoPort;
	}

	public TextField getTxtDBName() {
		return txtDBName;
	}

	public void setTxtDBName(TextField txtDBName) {
		this.txtDBName = txtDBName;
	}

	public TextField getTxtUserName() {
		return txtUserName;
	}

	public void setTxtUserName(TextField txtUserName) {
		this.txtUserName = txtUserName;
	}

	public TextField getTxtUserPwd() {
		return txtUserPwd;
	}

	public void setTxtUserPwd(TextField txtUserPwd) {
		this.txtUserPwd = txtUserPwd;
	}

	public TextField getCboDBType() {
		return cboDBType;
	}

	public void setCboDBType(TextField cboDBType) {
		this.cboDBType = cboDBType;
	}

	public ComboBox<String> getCboDBCoding() {
		return cboDBCoding;
	}

	public void setCboDBCoding(ComboBox<String> cboDBCoding) {
		this.cboDBCoding = cboDBCoding;
	}

	public Button getBtnTestConn() {
		return btnTestConn;
	}

	public void setBtnTestConn(Button btnTestConn) {
		this.btnTestConn = btnTestConn;
	}

	public Button getBtnCancel() {
		return btnCancel;
	}

	public void setBtnCancel(Button btnCancel) {
		this.btnCancel = btnCancel;
	}

	public Button getBtnSave() {
		return btnSave;
	}

	public void setBtnSave(Button btnSave) {
		this.btnSave = btnSave;
	}

}
