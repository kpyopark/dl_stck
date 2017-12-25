package com.elevenquest.dl.pipeline;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;

import com.elevenquest.dl.pipeline.dao.DailyStockDao;

public class DbToCsvConverter {
	
	private static void printHeader(FileDelegator out, ResultSetMetaData rsmd) throws SQLException {
		StringBuffer sb = new StringBuffer();
		for(int column = 0; column < rsmd.getColumnCount(); column++ ) {
			sb.append(",").append(rsmd.getColumnName(column + 1));
		}
		out.println(sb.substring(1));
	}
	
	private static void printBody(FileDelegator out, ResultSet rs) throws SQLException {
		StringBuffer oneLine = null;
		ResultSetMetaData rsmd = rs.getMetaData();
		while(rs.next()) {
			oneLine = new StringBuffer();
			for(int column = 0 ; column < rsmd.getColumnCount() ; column++) {
				Object value = rs.getObject(column + 1);
				oneLine.append(",").append(value != null ? value : "");
			}
			out.println(oneLine.substring(1));
		}
	}
	
	public static void makeCsv(String stockId, String startDate, String filePath) {
		ResultSet rs = null;
		Connection conn = null;
		FileDelegator out = null;
		try {
			out = new FileDelegator(filePath);
			rs = DailyStockDao.getDailyStockLearningDataRs(stockId, startDate);
			conn = rs.getStatement().getConnection();
			ResultSetMetaData rsmd = rs.getMetaData();
			printHeader(out, rsmd);
			printBody(out,rs);
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		} finally {
			if(conn != null) try { conn.close(); } catch (Exception e) {}
			if(out != null) try {out.close(); } catch (Exception e) {}
		}
	}
	
	public static void main(String[] args) {
		List<String> targetCompanyIds = DailyStockDao.getTargetCompanies(50);
		String startDate = "20160101";
		String lastDate = DailyStockDao.getLastDay();
		System.out.println(lastDate);
		//System.out.println(FileUtil.getNextWorkday(lastDate, getClosedDay()));
		for(String stockId : targetCompanyIds) {
			makeCsv(stockId, startDate, 
					FileUtil.getDailyPredictTargetFilePath(lastDate, DailyStockDao.getClosedDay(), stockId, startDate));
		}
	}
	
}
