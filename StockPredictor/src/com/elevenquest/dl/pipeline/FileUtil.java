package com.elevenquest.dl.pipeline;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import com.elevenquest.dl.pipeline.dao.DailyStockDao;



public class FileUtil {

	public final static String CSV_FILE_FOLDER_PATH = "E:/Document/04.AI";
    public final static String MODEL_DIR = "E:/Document/04.AI/Model";
    public final static String BASE_DATA_FOLDER = "BaseML";
    public final static String DEFAULT_MODEL_PATH = "E:/Document/Model.zip";
    public final static String ML_BASE_DATE = "20120101";
    final static String[] TRAIN_CSV_PATHS = { 
    		"E:/Document/A005930_20130101_org.csv", 
    		"E:/Document/A008560_20130101_org.csv",
    		"E:/Document/A114800_20160501_org.csv" };
    final static String[] PREDICT_CSV_PATHS = { "E:/Document/04.AI/A042670_20160101.csv" };
    	// { "E:/Document/A005930_20160101_org.csv", "E:/Document/A008560_20160101_org.csv" };

    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    
    public static String getNextWorkday(String currentWorkDay, List<String> closeDays) {
    	String rtn = currentWorkDay.substring(0,6) + (Integer.parseInt(currentWorkDay.substring(6,8)) + 1);
    	try {
    		Date nextDay = DATE_FORMAT.parse(currentWorkDay);
    		Calendar c = Calendar.getInstance();
    		int dayofweek = c.get(Calendar.DAY_OF_WEEK);
    		do {
    			c.setTime(nextDay);
    			c.add(Calendar.DATE, 1);
    			rtn = DATE_FORMAT.format(c.getTime());
    		} while (dayofweek == Calendar.SATURDAY || dayofweek == Calendar.SUNDAY 
    				|| closeDays.contains(rtn));
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return rtn;
    }
    
    public static String getDailyPrdictTargetDirectory(String currentWorkDay, List<String> closeDays) {
    	return CSV_FILE_FOLDER_PATH + File.separator + getNextWorkday(currentWorkDay, closeDays);
    }
    
    public static String getDailyPredictTargetFilePath(String currentworkDay, List<String> closedDays, String stockId, String startDate) {
    	return FileUtil.getDailyPrdictTargetDirectory(currentworkDay, 
				DailyStockDao.getClosedDay()) + File.separator + stockId + "_" + startDate + ".csv";
    }
    
    public static String getLearningBasePath() {
    	return CSV_FILE_FOLDER_PATH + File.separator + BASE_DATA_FOLDER;
    }
    
	public static String getBaseDataCsvFilePath(String startDate, String stockId) {
		return FileUtil.getLearningBasePath() + File.separator + stockId + "_" + startDate + ".csv";
	}
	
	public static String getStockIdFromBaseDataCsvFilePath(String filePath) {
		File file = new File(filePath);
		String fileName = file.getName();
		StringTokenizer st = new StringTokenizer(fileName, "_.");
		return st.nextToken();
	}

	public static String getStartDateFromBaseDataCsvFilePath(String filePath) {
		File file = new File(filePath);
		String fileName = file.getName();
		StringTokenizer st = new StringTokenizer(fileName, "_.");
		st.nextToken();
		return st.nextToken();
	}

    public static String getStockModelPath(String startDate, String stockId) {
    	return MODEL_DIR + File.separator + stockId + "_" + startDate + ".zip";
    }
    
}
