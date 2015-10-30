package com.dragonzone.network;

import java.util.List;
import java.util.Map;

public class Node {
	private String id;
	private String label;
	private String title;
	private List<String> primaryKeyList;
	private List<String> foreignKeyList;
	private Map<String, String> propertyMap;
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public Map<String, String> getPropertyMap() {
		return propertyMap;
	}
	public void setPropertyMap(Map<String, String> propertyMap) {
		this.propertyMap = propertyMap;
	}
	public List<String> getPrimaryKeyList() {
		return primaryKeyList;
	}
	public void setPrimaryKeyList(List<String> primaryKeyList) {
		this.primaryKeyList = primaryKeyList;
	}
	public List<String> getForeignKeyList() {
		return foreignKeyList;
	}
	public void setForeignKeyList(List<String> foreignKeyList) {
		this.foreignKeyList = foreignKeyList;
	}
	
}
