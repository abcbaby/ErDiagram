package com.dragonzone.model;

public class DataSourceInfo {
	private String driver;
	private String url;
	private String username;
	private String password;
	private String schema;
	private boolean forceRefresh;
	
	public String getDriver() {
		return driver;
	}
	public void setDriver(String driver) {
		this.driver = driver;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getUsername() {
		return username;
	}
	public void setUsername(String username) {
		this.username = username;
	}
	public String getPassword() {
		return password;
	}
	public void setPassword(String password) {
		this.password = password;
	}
	public String getSchema() {
		return schema;
	}
	public void setSchema(String schema) {
		this.schema = schema;
	}
	public boolean isForceRefresh() {
		return forceRefresh;
	}
	public void setForceRefresh(boolean forceRefresh) {
		this.forceRefresh = forceRefresh;
	}	
}
