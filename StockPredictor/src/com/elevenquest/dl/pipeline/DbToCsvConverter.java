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
	
	private static void printHeader(FileDelegator out, ResultSetMetaData rsmd) throws SQLException, IOException {
		StringBuffer sb = new StringBuffer();
		for(int column = 0; column < rsmd.getColumnCount(); column++ ) {
			sb.append(",").append(rsmd.getColumnName(column + 1));
		}
		out.println(sb.substring(1));
	}
	
	private static void printBody(FileDelegator out, ResultSet rs) throws SQLException, IOException {
		StringBuffer oneLine = null;
		ResultSetMetaData rsmd = rs.getMetaData();
		boolean hasNullValue = false;
		while(rs.next()) {
			hasNullValue = false;
			oneLine = new StringBuffer();
			for(int column = 0 ; !hasNullValue && column < rsmd.getColumnCount() ; column++) {
				Object value = rs.getObject(column + 1);
				if (value == null) {
					hasNullValue = true; 
					continue;
				}
				oneLine.append(",").append(value);
			}
			if(!hasNullValue)
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
		String stockId = null;
		if(args.length > 0) {
			stockId = args[0];
		}
		String startDate = "20160101";
		String lastDate = DailyStockDao.getLastDay();
		System.out.println("Last Day of Tx : " + lastDate);
		//System.out.println("Predict Target Day : " + FileUtil.getNextWorkday(lastDate, getClosedDay()));
		if(stockId != null) {
			makeCsv(stockId, startDate, 
					FileUtil.getDailyPredictTargetFilePath(lastDate, DailyStockDao.getClosedDay(), stockId, startDate));
		} else {
			List<String[]> targetCompanyIdAndNames = DailyStockDao.getTargetCompanies(50);
			for(String[] stockIdAndName : targetCompanyIdAndNames) {
				stockId = stockIdAndName[0];
				System.out.println("target:" + stockId);
				// if(stockId.equals("A005380"))
				//	continue;
				makeCsv(stockIdAndName[0], startDate, 
						FileUtil.getDailyPredictTargetFilePath(lastDate, DailyStockDao.getClosedDay(),stockId , startDate));
			}
		}
	}
	
}
