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
		String schema = dsInfo.getSchema().toUpperCase(); // Oracle schema are always capitalized!
		String jsonDir = req.getServletContext().getRealPath("/json");
		File jsonFile = new File(jsonDir, schema + "_diagram.json");

		Network network;
		if (!dsInfo.isForceRefresh() && jsonFile.exists()) { // if file exists use it
			network = new ObjectMapper().readValue(FileUtils.readFileToString(jsonFile), Network.class);
		} else {
			LOGGER.info("Started at {}", new Date());
			network = new Network();

			Connection conn = getOracleConnection(dsInfo.getUrl(), dsInfo.getUsername(), dsInfo.getPassword());
			DatabaseMetaData meta = conn.getMetaData();

			List<String> tableNameList = new ArrayList<>();
			ResultSet rs = meta.getTables(conn.getCatalog(), schema, null, null);
			/*
			 * ResultSetMetaData rsmd = rs.getMetaData(); for (int i = 1; i <=
			 * rsmd.getColumnCount(); i++) { String colName =
			 * rsmd.getColumnName(i); LOGGER.debug("Column Name: " + colName); }
			 */
			Map<String, Node> tableMap = new HashMap<>();
			LOGGER.debug("List of tables: ");
			while (rs.next()) {
				String tableName = rs.getString("TABLE_NAME");
				LOGGER.debug("TABLE_CAT: " + rs.getString("TABLE_CAT") + ", " + "TABLE_SCHEM: " + rs.getString("TABLE_SCHEM") + ", " + "TABLE_NAME: "
						+ tableName + ", " + "TABLE_TYPE: " + rs.getString("TABLE_TYPE") + ", " + "REMARKS: " + rs.getString("REMARKS"));

				Node node = new Node();
				final String name = schema + "." + tableName;
				node.setId(name);
				node.setLabel(tableName);
				tableMap.put(tableName, node);

				tableNameList.add(tableName);
			}
			rs.close();

			for (String tableName : tableNameList) {
				Node node = tableMap.get(tableName);
				StringBuilder sbTitle = new StringBuilder("<table border=1><tr><th colspan=4>");
				sbTitle.append(tableName);
				sbTitle.append("</th></tr><tr><th>Column Name</th><th>Type</th><th>Size</th><th>Nullable</th></tr>");

				LOGGER.debug("List of " + tableName + " columns: ");
				rs = meta.getColumns(conn.getCatalog(), schema, tableName, null);
				while (rs.next()) {
					LOGGER.debug("TABLE_CAT: " + rs.getString("TABLE_CAT") + ", " + "TABLE_SCHEM: " + rs.getString("TABLE_SCHEM") + ", " + "TABLE_NAME: "
							+ rs.getString("TABLE_NAME") + ", " + "COLUMN_NAME: " + rs.getString("COLUMN_NAME") + ", " + "DATA_TYPE: "
							+ rs.getString("DATA_TYPE") + ", " + "TYPE_NAME: " + rs.getString("TYPE_NAME") + ", " + "COLUMN_SIZE: "
							+ rs.getString("COLUMN_SIZE") + ", " + "IS_NULLABLE: " + rs.getString("IS_NULLABLE") + ", " + "REMARKS: "
							+ rs.getString("REMARKS"));

					sbTitle.append("<tr><td>");
					sbTitle.append(rs.getString("COLUMN_NAME"));
					sbTitle.append("</td><td>");
					sbTitle.append(rs.getString("TYPE_NAME"));
					sbTitle.append("</td><td>");
					sbTitle.append(rs.getString("COLUMN_SIZE"));
					sbTitle.append("</td><td>");
					sbTitle.append(rs.getString("IS_NULLABLE"));
					sbTitle.append("</td></tr>");
				}
				rs.close();
				
				sbTitle.append("</table>");

				node.setTitle(sbTitle.toString());
			}
			List<Edge> edgeList = new ArrayList<>();

			LOGGER.debug("List of tables foreign keys: ");
			for (String tableName : tableNameList) {
				rs = meta.getImportedKeys(conn.getCatalog(), schema, tableName);
				while (rs.next()) {
					LOGGER.debug("PKTABLE_CAT: " + rs.getString("PKTABLE_CAT") + ", " + "PKTABLE_SCHEM: " + rs.getString("PKTABLE_SCHEM") + ", "
							+ "PKTABLE_NAME: " + rs.getString("PKTABLE_NAME") + ", " + "PKCOLUMN_NAME: " + rs.getString("PKCOLUMN_NAME") + ", "
							+ "FKTABLE_CAT: " + rs.getString("FKTABLE_CAT") + ", " + "FKTABLE_SCHEM: " + rs.getString("FKTABLE_SCHEM") + ", " + "FKTABLE_NAME: "
							+ rs.getString("FKTABLE_NAME") + ", " + "FKCOLUMN_NAME: " + rs.getString("FKCOLUMN_NAME") + ", " + "KEY_SEQ: "
							+ rs.getString("KEY_SEQ") + ", " + "UPDATE_RULE: " + rs.getString("UPDATE_RULE") + ", " + "DELETE_RULE: "
							+ rs.getString("DELETE_RULE") + ", " + "FK_NAME: " + rs.getString("FK_NAME") + ", " + "PK_NAME: " + rs.getString("PK_NAME") + ", "
							+ "DEFERRABILITY: " + rs.getString("DEFERRABILITY"));

					Edge edge = new Edge();
					final String name = rs.getString("FK_NAME");
					edge.setId(rs.getString("FKTABLE_NAME") + "." + rs.getString("FKCOLUMN_NAME") + "-" + rs.getString("PKTABLE_NAME") + "."
							+ rs.getString("PKCOLUMN_NAME"));
					edge.setLabel(name);
					edge.setTitle(rs.getString("FKTABLE_NAME") + "." + rs.getString("FKCOLUMN_NAME") + " references " + rs.getString("PKTABLE_NAME") + "."
							+ rs.getString("PKCOLUMN_NAME"));
					edge.setFrom(schema + "." + rs.getString("FKTABLE_NAME"));
					edge.setTo(schema + "." + rs.getString("PKTABLE_NAME"));
					edgeList.add(edge);
				}

				rs.close();
			}

			network.setNodes(new ArrayList<Node>(tableMap.values()));
			network.setEdges(edgeList);

			FileUtils.writeStringToFile(jsonFile, new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(network));

			conn.close();
			
			LOGGER.info("Done at {}", new Date());
		}

		return network;
	}

	public static Connection getOracleConnection(String url, String username, String password) throws Exception {
		String driver = "oracle.jdbc.driver.OracleDriver";

		Class.forName(driver); // load Oracle driver
		Connection conn = DriverManager.getConnection(url, username, password);
		LOGGER.debug("Connection string: " + url + " with username: " + username);

		return conn;
	}
}
