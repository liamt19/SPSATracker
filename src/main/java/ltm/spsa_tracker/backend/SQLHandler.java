package ltm.spsa_tracker.backend;

import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SQLHandler {

	private static final String SQL_CREATE_PARAM_INFO_TABLE_COLUMNS = " (id INTEGER PRIMARY KEY AUTOINCREMENT, " + "name NVARCHAR(255), " + "start REAL, "
			+ "min REAL, " + "max REAL, " + "step REAL, " + "step_end REAL, " + "learn_rate REAL, " + "learn_rate_end REAL)";

	private static final String SQL_CREATE_SINGLE_ITERATION_TABLE_COLUMNS = " (id INTEGER PRIMARY KEY AUTOINCREMENT, " + "iteration INTEGER, "
			+ "name NVARCHAR(255), " + "current REAL)";

	private static final String SQL_CREATE_IF_NOT_EXISTS = "CREATE TABLE IF NOT EXISTS ";

	private static final String SQL_INSERT_PARAM_LIST_VALUES = " (name, start, min, max, step, step_end, learn_rate, learn_rate_end) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

	private static final String SQL_INSERT_SINGLE_ITERATION = " (iteration, name, current) VALUES (?, ?, ?)";

	private static String sanitizeName(String url) {
		return url.replace("https://", "").replace("http://", "").replace("/", "").replace(":", "_").replace(".", "_");
	}

	private static String getDatabasePath(String instanceName) {
		String cwd = FileSystems.getDefault().getPath("").toAbsolutePath().toString();
		return Paths.get(cwd, ParameterHandler.SAVE_FOLDER, instanceName).toString();
	}

	private static String getDriverPath(String instanceName, String dbName) {
		return "jdbc:sqlite:" + getDatabasePath(instanceName) + dbName + ".db";
	}

	private static void createTableIfNotExists(String instanceName, String tableName, String columns) {
		String dbName = sanitizeName(instanceName);
		String dbPath = getDriverPath(instanceName, dbName);
		try (Connection conn = DriverManager.getConnection(dbPath); Statement stmt = conn.createStatement()) {
			String sql = SQL_CREATE_IF_NOT_EXISTS + tableName + columns;
			stmt.execute(sql);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void insertSingleParameterSetIteration(String instanceName, String tableName, ParameterSet paramSet) {
		String dbName = sanitizeName(instanceName);
		createTableIfNotExists(instanceName, tableName, SQL_CREATE_SINGLE_ITERATION_TABLE_COLUMNS);

		String dbPath = getDriverPath(instanceName, dbName);
		try {
			Connection conn = DriverManager.getConnection(dbPath);
			var params = paramSet.parameters();

			for (int i = 0; i < params.size(); i++) {
				Parameter param = params.get(i);
				PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + tableName + SQL_INSERT_SINGLE_ITERATION);
				{
					int dbIndex = 1;
					pstmt.setInt(dbIndex++, paramSet.iteration());
					pstmt.setString(dbIndex++, param.name());
					pstmt.setFloat(dbIndex++, param.current());

					pstmt.executeUpdate();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void createParameterListTable(String instanceName, String tableName, ParameterSet paramSet) {
		String dbName = sanitizeName(instanceName);
		tableName += "_parameters";
		createTableIfNotExists(instanceName, tableName, SQL_CREATE_PARAM_INFO_TABLE_COLUMNS);

		String dbPath = getDriverPath(instanceName, dbName);
		try {
			Connection conn = DriverManager.getConnection(dbPath);

			Statement existsStmt = conn.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			boolean hasResults = existsStmt.execute("SELECT name FROM sqlite_schema WHERE type = 'table' and name like '%_parameters'");
			System.out.println("existsStmt: " + hasResults);
			var set = existsStmt.getResultSet();
			if (set != null) {
				do {
					String paramTableName = set.getString("name");
					if (paramTableName.equals(tableName)) {
						return;
					}
				} while (existsStmt.getMoreResults());
			}

			var params = paramSet.parameters();
			for (int i = 0; i < params.size(); i++) {
				Parameter param = params.get(i);
				PreparedStatement pstmt = conn.prepareStatement("INSERT INTO " + tableName + SQL_INSERT_PARAM_LIST_VALUES);
				{
					int dbIndex = 1;
					pstmt.setString(dbIndex++, param.name());
					pstmt.setFloat(dbIndex++, param.start());
					pstmt.setFloat(dbIndex++, param.min());
					pstmt.setFloat(dbIndex++, param.max());
					pstmt.setFloat(dbIndex++, param.C());
					pstmt.setFloat(dbIndex++, param.C_end());
					pstmt.setFloat(dbIndex++, param.R());
					pstmt.setFloat(dbIndex++, param.R_end());

					pstmt.executeUpdate();
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
