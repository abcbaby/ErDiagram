package com.dragonzone.spring.mvc.ctrl;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.dragonzone.model.DataSourceInfo;
import com.dragonzone.network.Edge;
import com.dragonzone.network.Network;
import com.dragonzone.network.Node;
import com.fasterxml.jackson.databind.ObjectMapper;

@Controller
@RequestMapping("/rest")
public class JsonController {
	private static final Logger LOGGER = LoggerFactory.getLogger(JsonController.class);

	@RequestMapping(value = "/erDiagram", method = { RequestMethod.POST })
	public @ResponseBody Network generateErDiagram(final HttpServletRequest req, 
			@RequestBody final DataSourceInfo dsInfo) throws Exception {
		LOGGER.debug("In generateErDiagram");
		final String schema = dsInfo.getSchema().toUpperCase(); // Oracle schema are always capitalized!
		final String jsonDir = req.getServletContext().getRealPath("/json");
		final File jsonFile = new File(jsonDir, schema + "_diagram.json");

		Network network;
		if (!dsInfo.isForceRefresh() && jsonFile.exists()) { // if file exists use it
			network = new ObjectMapper().readValue(FileUtils.readFileToString(jsonFile), Network.class);
		} else {
			LOGGER.info("Started at {}", new Date());
			network = new Network();

			final Connection conn = getOracleConnection(dsInfo.getUrl(), dsInfo.getUsername(), dsInfo.getPassword());
			final DatabaseMetaData meta = conn.getMetaData();

			List<String> tableNameList = new ArrayList<>();
			final ResultSet tableResultSet = meta.getTables(conn.getCatalog(), schema, null, null);
			/*
			 * Leave this here to troubleshoot if later need help to pull all the names from resultSet
			 * ResultSetMetaData rsmd = rs.getMetaData(); for (int i = 1; i <=
			 * rsmd.getColumnCount(); i++) { String colName =
			 * rsmd.getColumnName(i); LOGGER.debug("Column Name: " + colName); }
			 */
			Map<String, Node> tableMap = new HashMap<>();
			LOGGER.debug("List of tables: ");
			while (tableResultSet.next()) {
				final String tableName = tableResultSet.getString("TABLE_NAME");
				LOGGER.debug("TABLE_CAT: " + tableResultSet.getString("TABLE_CAT") + ", " + "TABLE_SCHEM: " + tableResultSet.getString("TABLE_SCHEM") + ", " + "TABLE_NAME: "
						+ tableName + ", " + "TABLE_TYPE: " + tableResultSet.getString("TABLE_TYPE") + ", " + "REMARKS: " + tableResultSet.getString("REMARKS"));

				Node node = new Node();
				final String name = schema + "." + tableName;
				node.setId(name);
				node.setLabel(tableName);
				tableMap.put(tableName, node);

				tableNameList.add(tableName);
			}
			tableResultSet.close();

			List<Edge> edgeList = new ArrayList<>();

			LOGGER.debug("List of tables foreign keys: ");
			for (String tableName : tableNameList) {
				final List<String> foreignKeyList = new ArrayList<>();
				final ResultSet foreignKeyResultSet = meta.getImportedKeys(conn.getCatalog(), schema, tableName);
				while (foreignKeyResultSet.next()) {
					LOGGER.debug("PKTABLE_CAT: " + foreignKeyResultSet.getString("PKTABLE_CAT") + ", " + "PKTABLE_SCHEM: " + foreignKeyResultSet.getString("PKTABLE_SCHEM") + ", "
							+ "PKTABLE_NAME: " + foreignKeyResultSet.getString("PKTABLE_NAME") + ", " + "PKCOLUMN_NAME: " + foreignKeyResultSet.getString("PKCOLUMN_NAME") + ", "
							+ "FKTABLE_CAT: " + foreignKeyResultSet.getString("FKTABLE_CAT") + ", " + "FKTABLE_SCHEM: " + foreignKeyResultSet.getString("FKTABLE_SCHEM") + ", " + "FKTABLE_NAME: "
							+ foreignKeyResultSet.getString("FKTABLE_NAME") + ", " + "FKCOLUMN_NAME: " + foreignKeyResultSet.getString("FKCOLUMN_NAME") + ", " + "KEY_SEQ: "
							+ foreignKeyResultSet.getString("KEY_SEQ") + ", " + "UPDATE_RULE: " + foreignKeyResultSet.getString("UPDATE_RULE") + ", " + "DELETE_RULE: "
							+ foreignKeyResultSet.getString("DELETE_RULE") + ", " + "FK_NAME: " + foreignKeyResultSet.getString("FK_NAME") + ", " + "PK_NAME: " + foreignKeyResultSet.getString("PK_NAME") + ", "
							+ "DEFERRABILITY: " + foreignKeyResultSet.getString("DEFERRABILITY"));

					final Edge edge = new Edge();
					final String fkName = foreignKeyResultSet.getString("FK_NAME");
					final String fkColumnName = foreignKeyResultSet.getString("FKCOLUMN_NAME");
					foreignKeyList.add(fkColumnName);
					edge.setId(foreignKeyResultSet.getString("FKTABLE_NAME") + "." + fkColumnName + "-" + foreignKeyResultSet.getString("PKTABLE_NAME") + "."
							+ foreignKeyResultSet.getString("PKCOLUMN_NAME"));
					edge.setLabel(fkName);
					edge.setTitle(foreignKeyResultSet.getString("FKTABLE_NAME") + "." + fkColumnName + " references " + foreignKeyResultSet.getString("PKTABLE_NAME") + "."
							+ foreignKeyResultSet.getString("PKCOLUMN_NAME"));
					edge.setFrom(schema + "." + foreignKeyResultSet.getString("FKTABLE_NAME"));
					edge.setTo(schema + "." + foreignKeyResultSet.getString("PKTABLE_NAME"));
					edgeList.add(edge);
				}
				foreignKeyResultSet.close();
				
				final Node node = tableMap.get(tableName);
				node.setForeignKeyList(foreignKeyList);
			}

			for (String tableName : tableNameList) {
				final Node node = tableMap.get(tableName);
				final StringBuilder sbTitle = new StringBuilder("<table border=1><tr><th colspan=4>");
				sbTitle.append(tableName);
				sbTitle.append("</th></tr><tr><th>Column Name</th><th>Type</th><th>Size</th><th>Nullable</th></tr>");

				final List<String> primaryKeyList = new ArrayList<>();
				final ResultSet primaryKeyResultSet = meta.getPrimaryKeys(conn.getCatalog(), schema, tableName);
				while (primaryKeyResultSet.next()) {
					primaryKeyList.add(primaryKeyResultSet.getString("COLUMN_NAME"));
				}
				node.setPrimaryKeyList(primaryKeyList);
				
				LOGGER.debug("List of " + tableName + " columns: ");
				final ResultSet columnResultSet = meta.getColumns(conn.getCatalog(), schema, tableName, null);
				while (columnResultSet.next()) {
					LOGGER.debug("TABLE_CAT: " + columnResultSet.getString("TABLE_CAT") + ", " + "TABLE_SCHEM: " + columnResultSet.getString("TABLE_SCHEM") + ", " + "TABLE_NAME: "
							+ columnResultSet.getString("TABLE_NAME") + ", " + "COLUMN_NAME: " + columnResultSet.getString("COLUMN_NAME") + ", " + "DATA_TYPE: "
							+ columnResultSet.getString("DATA_TYPE") + ", " + "TYPE_NAME: " + columnResultSet.getString("TYPE_NAME") + ", " + "COLUMN_SIZE: "
							+ columnResultSet.getString("COLUMN_SIZE") + ", " + "IS_NULLABLE: " + columnResultSet.getString("IS_NULLABLE") + ", " + "REMARKS: "
							+ columnResultSet.getString("REMARKS"));

					sbTitle.append("<tr><td>");
					final String columnName = columnResultSet.getString("COLUMN_NAME");
					sbTitle.append(columnName);
					if (existsInList(node.getPrimaryKeyList(), columnName)) {
						sbTitle.append(" (PK)");
					} else if (existsInList(node.getForeignKeyList(), columnName)) {
						sbTitle.append(" (FK)");
					}
					sbTitle.append("</td><td>");
					sbTitle.append(columnResultSet.getString("TYPE_NAME"));
					sbTitle.append("</td><td>");
					sbTitle.append(columnResultSet.getString("COLUMN_SIZE"));
					sbTitle.append("</td><td>");
					sbTitle.append(columnResultSet.getString("IS_NULLABLE"));
					sbTitle.append("</td></tr>");
				}
				columnResultSet.close();
				
				sbTitle.append("</table>");

				node.setTitle(sbTitle.toString());
			}
						
			network.setNodes(new ArrayList<Node>(tableMap.values()));
			network.setEdges(edgeList);

			// write to file to be reused later instead of regenerating every time.
			FileUtils.writeStringToFile(jsonFile, new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(network));

			conn.close();
			
			LOGGER.info("Done at {}", new Date());
		}

		return network;
	}
	
	private boolean existsInList(final List<String> list, final String nameInList) {
		boolean exists = false;
		for (String val : list) {
			if (val.equals(nameInList)) {
				exists = true;
				break;
			}
		}
		return exists;
	}

	public static Connection getOracleConnection(String url, String username, String password) throws Exception {
		String driver = "oracle.jdbc.driver.OracleDriver";

		Class.forName(driver); // load Oracle driver
		Connection conn = DriverManager.getConnection(url, username, password);
		LOGGER.debug("Connection string: " + url + " with username: " + username);

		return conn;
	}
}
