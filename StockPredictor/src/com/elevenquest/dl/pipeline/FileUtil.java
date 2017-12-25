package com.elevenquest.dl.pipeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.elevenquest.dl.pipeline.dao.DailyStockDao;


/**
 * 
 * /mldata-base
 *              /model - includes model.zip file list
 *              /BaseML - base learning data rows from 2012/01/01
 *              /yyyyMMdd - predict target data.
 * 
 * @author user
 *
 */
public class FileUtil {
	private static Logger log = LoggerFactory.getLogger(MetricCreator.class);
	
	private final static AmazonS3 s3client = new AmazonS3Client(new ProfileCredentialsProvider());
	
	private final static String FILE_SEPARATOR = "/";
	public final static String CSV_FILE_FOLDER_PATH_S3 = "s3://mldata-base";
	public final static String CSV_FILE_FOLDER_PATH_FS = "E:/Document/04.AI";
    public final static String MODEL_DIR_S3 = "S3://mldata-base/model";
    public final static String MODEL_DIR_FS = "E:/Document/04.AI/Model";
    public final static String BASE_DATA_FOLDER = "BaseML";
    public final static String DEFAULT_MODEL_PATH = MODEL_DIR_FS + FILE_SEPARATOR + "Model.zip";
    public final static String ML_BASE_DATE = "20120101";
    
    public final static boolean LOCAL_FILE_SYSTEM = "true".equals(System.getProperty("S3_ENABLED"));
    /*
    final static String[] TRAIN_CSV_PATHS = { 
    		"E:/Document/A005930_20130101_org.csv", 
    		"E:/Document/A008560_20130101_org.csv",
    		"E:/Document/A114800_20160501_org.csv" };
    */
    // final static String[] PREDICT_CSV_PATHS = { "E:/Document/04.AI/A042670_20160101.csv" };
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
    	if(LOCAL_FILE_SYSTEM)
    		return CSV_FILE_FOLDER_PATH_FS + File.separator + getNextWorkday(currentWorkDay, closeDays);
    	else
    		return CSV_FILE_FOLDER_PATH_S3 + FILE_SEPARATOR + getNextWorkday(currentWorkDay, closeDays);
    }
    
    public static String getDailyPredictTargetFilePath(String currentworkDay, List<String> closedDays, String stockId, String startDate) {
    	return FileUtil.getDailyPrdictTargetDirectory(currentworkDay, 
				DailyStockDao.getClosedDay()) + File.separator + stockId + "_" + startDate + ".csv";
    }
    
    public static String getLearningBasePath() {
    	if(LOCAL_FILE_SYSTEM)
    		return CSV_FILE_FOLDER_PATH_FS + File.separator + BASE_DATA_FOLDER;
    	else
    		return CSV_FILE_FOLDER_PATH_S3 + FILE_SEPARATOR + BASE_DATA_FOLDER;
    }
    
	public static String getBaseDataCsvFilePath(String startDate, String stockId) {
		return FileUtil.getLearningBasePath() + File.separator + stockId + "_" + startDate + ".csv";
	}
	
	public static String getStockIdFromBaseDataCsvFilePath(String filePath) {
		String fileName = filePath;
		StringTokenizer folderFileDelimeter = new StringTokenizer(filePath, "/\\");
		while(folderFileDelimeter.hasMoreTokens()) {
			fileName = folderFileDelimeter.nextToken();
		}
		StringTokenizer st = new StringTokenizer(fileName, "_.");
		return st.nextToken();
	}

	public static String getStartDateFromBaseDataCsvFilePath(String filePath) {
		String fileName = filePath;
		StringTokenizer folderFileDelimeter = new StringTokenizer(filePath, "/\\");
		while(folderFileDelimeter.hasMoreTokens()) {
			fileName = folderFileDelimeter.nextToken();
		}
		StringTokenizer st = new StringTokenizer(fileName, "_.");
		st.nextToken();
		return st.nextToken();
	}

    public static String getStockModelPath(String startDate, String stockId) {
    	if(LOCAL_FILE_SYSTEM)
    		return MODEL_DIR_FS + File.separator + stockId + "_" + startDate + ".zip";
    	else
    		return MODEL_DIR_S3 + FILE_SEPARATOR + stockId + "_" + startDate + ".zip";
    }
    
    public static String[] getBucketAndKey(String path) {
		StringTokenizer st = new StringTokenizer(path.substring("s3://".length()), "/");
		String bucketName = st.nextToken();
		String key = path.substring("s3://".length() + bucketName.length() + "/".length());
		return new String[]{ bucketName, key };
    }
    
    public static void copyFileToS3(String source, String s3target) {
    	File sourceFile = new File(source);
    	String[] bucketAndKey = getBucketAndKey(s3target);
    	String bucket = bucketAndKey[0];
    	String key = bucketAndKey[1];
    	s3client.putObject(bucket, key, sourceFile);
    }
    
    public static List<String> getFileList(String path) throws IOException {
    	List<String> rtn = new ArrayList<String>();
    	if(path.indexOf("s3") == 0) {
    		// s3 file system.
    		ListObjectsRequest request = new ListObjectsRequest();
    		Object[] bucketAndPrefix = getBucketAndKey(path);
    		String prefix = (String)bucketAndPrefix[1];
    		request.setBucketName((String)bucketAndPrefix[0]);
    		request.setPrefix(prefix);
    		ObjectListing objects = s3client.listObjects(request);
    		for(S3ObjectSummary objectSummary : objects.getObjectSummaries()) {
    			log.debug(objectSummary.toString());
    			if(objectSummary.getSize() > 0)
    				rtn.add(objectSummary.getKey().substring(prefix.length()));
    		}
    	} else {
    		// normal file system.
    		File mldataDir = new File(path);
    		File[] files = mldataDir.listFiles();
    		for(File file : files) rtn.add(file.getName());
    	}
    	return rtn;
    }
    
    public static void main(String[] args) throws IOException {
    	for(String name : getFileList("s3://mldata-base/model/")) {
    		log.info("filename : [" + name + "]");
    	}
    }
    
}
