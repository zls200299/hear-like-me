package com.szmirren.common;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.ListCollectionsIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.szmirren.entity.MongoFieldAttribute;
import com.szmirren.entity.TableContent;
import com.szmirren.models.*;
import com.szmirren.options.DatabaseConfig;
import freemarker.template.utility.StringUtil;
import org.bson.Document;
import org.sqlite.util.StringUtils;

/**
 * 数据库工具
 * 
 * @author Mirren
 *
 */
public class DBUtil {
	private static final int DB_CONNECTION_TIMEOUTS_SECONDS = 1;

	/**
	 * 物理连接缓存：key = 配置标识，value = 真实 JDBC 连接
	 * 用于在同一轮代码生成中复用连接，避免每张表每个方法都重新建连接（远程 MySQL 情况下每次 50-100ms 累加会非常慢）
	 */
	private static final Map<String, Connection> CONN_CACHE = new ConcurrentHashMap<>();

	/** Cache of MongoClient instances keyed by "host:port@user" to avoid creating a new client per call. */
	private static final ConcurrentHashMap<String, MongoClient> MONGO_CLIENT_CACHE = new ConcurrentHashMap<>();

	/**
	 * 关闭所有缓存的数据库连接并清空缓存。
	 * 应在代码生成、加载表列表等数据库操作完成后调用，释放 MySQL 连接，避免连接池打满。
	 */
	public static synchronized void closeAllConnections() {
		for (Map.Entry<String, Connection> entry : CONN_CACHE.entrySet()) {
			try {
				Connection conn = entry.getValue();
				if (conn != null && !conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				System.err.println("关闭连接失败: " + entry.getKey() + " - " + e.getMessage());
			}
		}
		CONN_CACHE.clear();
	}

	/**
	 * 关闭指定配置对应的 JDBC 连接并从缓存中移除（切换/删除数据库连接时调用）
	 */
	public static synchronized void closeConnection(DatabaseConfig config) {
		if (config == null) {
			return;
		}
		String key = buildConnKey(config);
		Connection conn = CONN_CACHE.remove(key);
		if (conn != null) {
			try {
				if (!conn.isClosed()) {
					conn.close();
				}
			} catch (SQLException e) {
				System.err.println("关闭连接失败: " + key + " - " + e.getMessage());
			}
		}
	}

	private static String buildConnKey(DatabaseConfig config) {
		return config.getDbType() + "|" + config.getConnURL() + ":" + config.getListenPort()
				+ "/" + config.getDbName() + "@" + config.getUserName();
	}

	/**
	 * 把真实连接包装成一个"忽略 close"的代理：业务侧 try-with-resources 调用 close() 时不会真正关闭底层物理连接，
	 * 这样即可在不改动所有调用方代码的前提下实现连接复用。
	 */
	private static Connection wrapNonClosing(final Connection delegate) {
		return (Connection) Proxy.newProxyInstance(
				DBUtil.class.getClassLoader(),
				new Class<?>[]{Connection.class},
				(proxy, method, args) -> {
					String name = method.getName();
					if ("close".equals(name)) {
						return null; // no-op，保留物理连接到缓存里
					}
					if ("isClosed".equals(name)) {
						return delegate.isClosed();
					}
					try {
						return method.invoke(delegate, args);
					} catch (InvocationTargetException e) {
						throw e.getCause();
					}
				});
	}

	/**
	 * 从缓存拿一个有效的真实连接；若没有或已失效则新建
	 */
	private static synchronized Connection obtainRealConnection(DatabaseConfig config) throws ClassNotFoundException, SQLException {
		String key = buildConnKey(config);
		Connection cached = CONN_CACHE.get(key);
		if (cached != null) {
			try {
				if (!cached.isClosed() && cached.isValid(1)) {
					return cached;
				}
			} catch (SQLException ignore) {
				// fallthrough：重新创建
			}
			// 失效的连接清理掉
			try { cached.close(); } catch (SQLException ignore) {}
			CONN_CACHE.remove(key);
		}

		DriverManager.setLoginTimeout(DB_CONNECTION_TIMEOUTS_SECONDS);
		DBType dbType = DBType.valueOf(config.getDbType());
		Class.forName(dbType.getDriverClass());
		String url = getConnectionURL(config);
		Connection real;
		if (dbType == DBType.Oracle) {
			try {
				real = DriverManager.getConnection(url, config.getUserName(), config.getUserPwd());
			} catch (Exception e) {
				String oracle = String.format(DBType.OracleServiceName.getConnectionUrlPattern(), config.getConnURL(), config.getListenPort(),
						config.getDbName());
				real = DriverManager.getConnection(oracle, config.getUserName(), config.getUserPwd());
			}
		} else {
			real = DriverManager.getConnection(url, config.getUserName(), config.getUserPwd());
		}
		CONN_CACHE.put(key, real);
		return real;
	}

	/**
	 * 获得数据库连接（返回不会真正关闭的代理连接，物理连接由缓存管理）
	 *
	 * @param config
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static Connection getConnection(DatabaseConfig config) throws ClassNotFoundException, SQLException {
		Connection real = obtainRealConnection(config);
		return wrapNonClosing(real);
	}

	/**
	 * 获得数据库连接URL
	 * 
	 * @param dbConfig
	 * @return
	 * @throws ClassNotFoundException
	 */
	public static String getConnectionURL(DatabaseConfig dbConfig) throws ClassNotFoundException {
		DBType dbType = DBType.valueOf(dbConfig.getDbType());
		String connectionRUL = String.format(dbType.getConnectionUrlPattern(), dbConfig.getConnURL(), dbConfig.getListenPort(),
				dbConfig.getDbName(), dbConfig.getEncoding());
		return connectionRUL;
	}

	/**
	 * 获得数据库的表名
	 * 
	 * @param config
	 * @return
	 * @throws Exception
	 */
	public static List<String> getTableNames(DatabaseConfig config) throws Exception {
		List<String> tables = new ArrayList<>();
		try (Connection conn = getConnection(config)) {
			if (config.getDbType().equalsIgnoreCase(Constant.SQL_SERVER)) {
				// 如果是sqlserver数据库通过查询获得所有表跟视图
				String sql = "select name from sysobjects  where xtype='u' or xtype='v' ";
				try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
					while (rs.next()) {
						tables.add(rs.getString("name"));
					}
				}
			} else {
				// 如果非sqlserver类型的数据库通过JDBC获得所有表跟视图
				DatabaseMetaData md = conn.getMetaData();
				String[] types = {"TABLE", "VIEW"};
				if (config.getDbType().equalsIgnoreCase(Constant.POSTGRE_SQL)) {
					try (ResultSet rs = md.getTables(null, null, null, types)) {
						while (rs.next()) {
							tables.add(rs.getString(3));
						}
					}
				} else {
					String catalog = conn.getCatalog() == null ? null : conn.getCatalog();
					try (ResultSet rs = md.getTables(catalog, config.getUserName().toUpperCase(), "%%", types)) {
						while (rs.next()) {
							tables.add(rs.getString(3));
						}
					}
				}
			}
		}
		return tables;
	}

	/**
	 * 获得指定表的属性
	 * 
	 * @param config
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public static TableContent getTableAttribute(DatabaseConfig config, String tableName) throws Exception {
		TableContent content = new TableContent();
		try (Connection conn = getConnection(config)) {
			DatabaseMetaData md = conn.getMetaData();
			String[] types = {"TABLE", "VIEW"};
			if (config.getDbType().equalsIgnoreCase(Constant.POSTGRE_SQL)) {
				try (ResultSet rs = md.getTables(null, null, tableName, types)) {
					if (rs.next()) {
						fillTableContentFromRs(content, rs);
					}
				}
			} else {
				String catalog = conn.getCatalog() == null ? null : conn.getCatalog();
				try (ResultSet rs = md.getTables(catalog, config.getUserName().toUpperCase(), tableName, types)) {
					if (rs.next()) {
						fillTableContentFromRs(content, rs);
					}
				}
			}
		}
		return content;
	}

	private static void fillTableContentFromRs(TableContent content, ResultSet rs) throws SQLException {
		content.setTableCat(rs.getString("TABLE_CAT"));
		content.setTableSchem(rs.getString("TABLE_SCHEM"));
		content.setTableName(rs.getString("TABLE_NAME"));
		content.setTableType(rs.getString("TABLE_TYPE"));
		content.setRemarks(rs.getString("REMARKS"));
		content.setTypeCat(rs.getString("TYPE_CAT"));
		content.setTypeSchem(rs.getString("TYPE_SCHEM"));
		content.setTypeName(rs.getString("TYPE_NAME"));
		content.setSelfReferencingColName(rs.getString("SELF_REFERENCING_COL_NAME"));
		content.setRefGeneration(rs.getString("REF_GENERATION"));
	}



	/**
	 * 获取表的列属性
	 * 
	 * @param config
	 *          数据库配置文件
	 * @param tableName
	 *          表名
	 * @return
	 * @throws Exception
	 */
	public static List<MongoTypeDto> getMongoColumns(DatabaseConfig config, String tableName) throws Exception {
		MongoDatabase mongoConnection = getMongoConnection(config);
		//获取全部的文档
		MongoCollection<Document> collection = mongoConnection.getCollection(tableName);
		Document document = collection.find().first();
		//获取全部的key
		List<String> list = new ArrayList<>(document.keySet());
		List<MongoTypeDto> attrs = new ArrayList<>();
		if (list != null || list.size() != 0){
			list.stream().forEach(param ->{
				String str = JavaType.mongoDbTypeToJavaType(document.get(param));
				MongoTypeDto mongoTypeDto = new MongoTypeDto();
				mongoTypeDto.setColumn(param);
				mongoTypeDto.setType(str);
				attrs.add(mongoTypeDto);
			});
			if (!list.contains("createTime")){
                MongoTypeDto mongoTypeDto = new MongoTypeDto();
                mongoTypeDto.setColumn("createTime");
                mongoTypeDto.setType("Date");
                attrs.add(mongoTypeDto);
            }
            if (!list.contains("updateTime")){
                MongoTypeDto mongoTypeDto = new MongoTypeDto();
                mongoTypeDto.setColumn("updateTime");
                mongoTypeDto.setType("Date");
                attrs.add(mongoTypeDto);
            }
            if (!list.contains("isDelete")){
                MongoTypeDto mongoTypeDto = new MongoTypeDto();
                mongoTypeDto.setColumn("isDelete");
                mongoTypeDto.setType("Integer");
                attrs.add(mongoTypeDto);
            }
		}
		return attrs;
	}

	/**
	 * 获取表的列属性
	 *
	 * @param config
	 *          数据库配置文件
	 * @param tableName
	 *          表名
	 * @return
	 * @throws Exception
	 */
	public static List<TableAttributeEntity> getTableColumns(DatabaseConfig config, String tableName) throws Exception {
		try (Connection conn = getConnection(config)) {
			DatabaseMetaData md = conn.getMetaData();
			ResultSet rs;
			if (config.getDbType().equalsIgnoreCase(Constant.MYSQL)) {
				rs = md.getColumns(conn.getCatalog(), "%%", tableName, "%%");
			} else {
				rs = md.getColumns(null, null, tableName, null);
			}
			try (ResultSet rsAuto = rs) {
				Map<String, TableAttributeEntity> columnMap = new HashMap<>();
				while (rsAuto.next()) {
					TableAttributeEntity attr = new TableAttributeEntity();
					attr.setTdColumnName(rsAuto.getString("COLUMN_NAME"));
					attr.setTdJdbcType(rsAuto.getString("TYPE_NAME"));
					attr.setTdJavaType(JavaType.jdbcTypeToJavaType(rsAuto.getString("TYPE_NAME")));

					attr.setColumnDef(rsAuto.getString("COLUMN_DEF"));
					attr.setRemarks(rsAuto.getString("REMARKS"));
					attr.setColumnSize(rsAuto.getInt("COLUMN_SIZE"));
					attr.setDecimalDigits(rsAuto.getInt("DECIMAL_DIGITS"));
					attr.setOrdinalPosition(rsAuto.getInt("ORDINAL_POSITION"));
					attr.setNullable(rsAuto.getInt("NULLABLE") == 1 ? true : false);
					columnMap.put(rsAuto.getString("COLUMN_NAME"), attr);
				}
				if (columnMap.size() == 0) {
					throw new NullPointerException("从表中获取字段失败!获取不到任何字段!");
				}
				ArrayList<TableAttributeEntity> result = new ArrayList<>(columnMap.values());
				Collections.sort(result);
				return result;
			}
		}
	}

    /**
     * 获取表的列属性
     *
     * @param config
     *          数据库配置文件
     * @param tableName
     *          表名
     * @return
     * @throws Exception
     */
    public static List<TableAttributeBean> getTableColumnsForBean(DatabaseConfig config, String tableName) throws Exception {
        try (Connection conn = getConnection(config)) {
            DatabaseMetaData md = conn.getMetaData();
            try (ResultSet rs = md.getColumns(conn.getCatalog(), "%%", tableName, "%%")) {
                Map<String, TableAttributeBean> columnMap = new HashMap<>();
                while (rs.next()) {
                    TableAttributeBean attr = new TableAttributeBean();
                    attr.setTdColumnName(rs.getString("COLUMN_NAME"));
                    attr.setTdJdbcType(rs.getString("TYPE_NAME"));
                    attr.setTdJavaType(JavaType.jdbcTypeToJavaType(rs.getString("TYPE_NAME")));

                    attr.setColumnDef(rs.getString("COLUMN_DEF"));
                    attr.setRemarks(rs.getString("REMARKS"));
                    attr.setColumnSize(rs.getInt("COLUMN_SIZE"));
                    attr.setDecimalDigits(rs.getInt("DECIMAL_DIGITS"));
                    attr.setOrdinalPosition(rs.getInt("ORDINAL_POSITION"));
                    attr.setNullable(rs.getInt("NULLABLE") == 1 ? true : false);
                    columnMap.put(rs.getString("COLUMN_NAME"), attr);
                }
                if (columnMap.size() == 0) {
                    throw new NullPointerException("从表中获取字段失败!获取不到任何字段!");
                }
                ArrayList<TableAttributeBean> result = new ArrayList<>(columnMap.values());
                Collections.sort(result);
                return result;
            }
        }
    }

	/**
	 * 获取表的列属性
	 *
	 * @param config
	 *          数据库配置文件
	 * @param tableName
	 *          表名
	 * @return
	 * @throws Exception
	 */
	public static List<TableAttributeDto> getTableColumnsForCreateDto(DatabaseConfig config, String tableName) throws Exception {
		try (Connection conn = getConnection(config)) {
			DatabaseMetaData md = conn.getMetaData();
			ResultSet rs;
			if (config.getDbType().equalsIgnoreCase(Constant.MYSQL)) {
				rs = md.getColumns(conn.getCatalog(), "%%", tableName, "%%");
			} else {
				rs = md.getColumns(null, null, tableName, null);
			}
			try (ResultSet rsAuto = rs) {
				Map<String, TableAttributeDto> columnMap = new HashMap<>();
				while (rsAuto.next()) {
					TableAttributeDto attr = new TableAttributeDto();
					attr.setTdColumnName(rsAuto.getString("COLUMN_NAME"));
					attr.setTdJdbcType(rsAuto.getString("TYPE_NAME"));
					attr.setTdJavaType(JavaType.jdbcTypeToJavaType(rsAuto.getString("TYPE_NAME")));

					attr.setColumnDef(rsAuto.getString("COLUMN_DEF"));
					attr.setRemarks(rsAuto.getString("REMARKS"));
					attr.setColumnSize(rsAuto.getInt("COLUMN_SIZE"));
					attr.setDecimalDigits(rsAuto.getInt("DECIMAL_DIGITS"));
					attr.setOrdinalPosition(rsAuto.getInt("ORDINAL_POSITION"));
					attr.setNullable(rsAuto.getInt("NULLABLE") == 1 ? true : false);
					columnMap.put(rsAuto.getString("COLUMN_NAME"), attr);
				}
				if (columnMap.size() == 0) {
					throw new NullPointerException("从表中获取字段失败!获取不到任何字段!");
				}
				ArrayList<TableAttributeDto> result = new ArrayList<>(columnMap.values());
				Collections.sort(result);
				return result;
			}
		}
	}

	/**
	 * 获得主键名称
	 * 
	 * @param config
	 * @param tableName
	 * @return
	 * @throws Exception
	 */
	public static String getTablePrimaryKey(DatabaseConfig config, String tableName) throws Exception {
		try (Connection conn = getConnection(config)) {
			DatabaseMetaData md = conn.getMetaData();
			ResultSet rs;
			if (config.getDbType().equalsIgnoreCase(Constant.MYSQL)) {
				rs = md.getPrimaryKeys(conn.getCatalog(), conn.getSchema(), tableName);
			} else {
				rs = md.getPrimaryKeys(null, null, tableName);
			}
			try (ResultSet rsAuto = rs) {
				while (rsAuto.next()) {
					return rsAuto.getString("COLUMN_NAME");
				}
			}
		}
		return null;
	}

	/**
	 * 获得Mongodb数据库连接，有密码和无密码
	 *
	 * @param config
	 * @return
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public static MongoDatabase getMongoConnection(DatabaseConfig config){
		//获取页面属性
        MongoClient mongoClient = getCurrentMongoClient(config);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(config.getDbName());
		return mongoDatabase;
	}

	/**
	 * 获得Mongodb的全部集合
	 *
	 * @param config
	 * @return
	 * @throws Exception
	 */
	public static List<String> getMongoDbAllCollectionName(DatabaseConfig config)  {
		List<String> list = new ArrayList<>();
        MongoClient mongoClient = getCurrentMongoClient(config);
        MongoDatabase mongoDatabase = mongoClient.getDatabase(config.getDbName());
        //获取所有集合
		ListCollectionsIterable<Document> documents = mongoDatabase.listCollections();
		MongoCursor<Document> iterator = documents.iterator();
		while (iterator.hasNext()){
			String collectionName = iterator.next().getString("name");
			if (collectionName.contains("system.")) continue;
			//获取全部的集合名称
			list.add(collectionName);
		}
		return list;
	}

	//获取mongodbClient（MongoDB Java Driver 4.x）— 按配置缓存，避免每次调用都新建连接池
    public static MongoClient getCurrentMongoClient(DatabaseConfig config){
        String cacheKey = config.getConnURL() + ":" + config.getListenPort() + "@" + config.getUserName();
        return MONGO_CLIENT_CACHE.computeIfAbsent(cacheKey, k -> createMongoClient(config));
    }

    private static MongoClient createMongoClient(DatabaseConfig config) {
        int port = Integer.parseInt(config.getListenPort());
        String host = config.getConnURL();
        if ("".equals(config.getUserName()) && "".equals(config.getUserPwd())) {
            return MongoClients.create("mongodb://" + host + ":" + port);
        }
        List<ServerAddress> hosts = Collections.singletonList(new ServerAddress(host, port));
        MongoCredential credential = MongoCredential.createScramSha1Credential(
                config.getUserName(), config.getDbName(), config.getUserPwd().toCharArray());
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyToClusterSettings(builder -> builder.hosts(hosts))
                .credential(credential)
                .build();
        return MongoClients.create(settings);
    }

    /**
     * 关闭指定配置对应的 MongoClient 并从缓存中移除（切换数据库连接时调用）
     */
    public static void closeMongoClient(DatabaseConfig config) {
        String cacheKey = config.getConnURL() + ":" + config.getListenPort() + "@" + config.getUserName();
        MongoClient client = MONGO_CLIENT_CACHE.remove(cacheKey);
        if (client != null) {
            client.close();
        }
    }

    /**
     * 关闭所有缓存的 MongoClient（应用退出时调用）
     */
    public static void closeAllMongoClients() {
        MONGO_CLIENT_CACHE.values().forEach(MongoClient::close);
        MONGO_CLIENT_CACHE.clear();
    }


}
