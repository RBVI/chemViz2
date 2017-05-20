package edu.ucsf.rbvi.chemViz2.internal.model;

import java.util.ArrayList;
import java.util.List;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableUtil;

@SuppressWarnings("rawtypes")
public class TableUtils {
	static public String getName(CyNetwork network, CyIdentifiable id) {
		if (network == null || id == null)
			return null;
		CyRow row = network.getRow(id);
		if (row != null)
			return row.get(CyNetwork.NAME, String.class);
		return null;
	}

	static public String getLabelAttribute(CyNetwork network, 
	                                       CyIdentifiable id, 
	                                       String labelAttribute) {
		CyRow row = network.getRow(id);
		if (row == null) return null;

		Object rawValue = row.getRaw(labelAttribute);
		if (rawValue == null) return null;
		return rawValue.toString();
	}

	static public Class getColumnType(CyNetwork network,
	                                  CyIdentifiable id, 
	                                  String columnName) {
		CyRow row = network.getRow(id);
		CyTable table = row.getTable();
		if (!columnExists(table, columnName)) return null;
		return table.getColumn(columnName).getType();
	}

	static public Class getColumnType(CyTable table, String columnName) {
		if (!columnExists(table, columnName)) return null;
		return table.getColumn(columnName).getType();
	}

	static public <T> T getAttribute(CyNetwork network,
	                                 CyIdentifiable id, 
	                                 String columnName, Class<? extends T> type) {
		CyRow row = network.getRow(id);
		if (row == null) return null;

		if (!columnExists(row.getTable(), columnName))
			return null;

		return row.get(columnName, type);
	}

	static public <T> List<T> getListAttribute(CyNetwork network,
	                                           CyIdentifiable id, 
	                                           String columnName, Class type) {
		List<T>returnList = new ArrayList<T>();
		CyRow row = network.getRow(id);
		if (row == null) return returnList;
		CyColumn column = row.getTable().getColumn(columnName);
		if (column == null || column.getListElementType() != type) return returnList;
		return row.getList(columnName, type);
	}

	static public List<String> getColumnNames(CyNetwork network, Class<? extends CyIdentifiable> type) {
		CyTable table = null;
		String prefix = "";
		if (type.equals(CyNode.class)) {
			table = network.getDefaultNodeTable();
			prefix = "node.";
		} else if (type.equals(CyEdge.class)) {
			table = network.getDefaultEdgeTable();
			prefix = "edge.";
		} else if (type.equals(CyNetwork.class)) {
			table = network.getDefaultNetworkTable();
			prefix = "network.";
		}
		List<String> results = new ArrayList<String>();
		for (String columnName: CyTableUtil.getColumnNames(table)) {
			results.add(prefix+columnName);
		}
		return results;
	}

	static public boolean columnExists(CyTable table, String columnName) {
		return (table.getColumn(columnName) != null);
	}

}
