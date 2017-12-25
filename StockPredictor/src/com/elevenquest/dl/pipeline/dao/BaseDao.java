package com.elevenquest.dl.pipeline.dao;

import java.sql.Connection;
import java.sql.DriverManager;

public class BaseDao {
	
	protected static Connection getConnection() {
		Connection conn = null;
		String url = "jdbc:postgresql://localhost:5433/stock?user=postgres&password=aaaa1111";
		try {
			conn = DriverManager.getConnection(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return conn;
	}
}
