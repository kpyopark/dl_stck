package com.elevenquest.dl.pipeline;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elevenquest.dl.pipeline.dao.DailyStockDao;
import com.elevenquest.dl.pipeline.post.PredictMetric;

/**
 * For a specialized stock,
 * Model will be trained repeatedly.
 * 
 * @author user
 *
 */
public class SpecificStockML {
	
	private static Logger log = LoggerFactory.getLogger(SpecificStockML.class);

	private final static String TARGET_MODEL = "A002320";
	
	private final static float RENEWAL_THRESHOLD = 0.4f;
	private final static float SKIP_THRESHOLD = 0.8f;
	
	private final static String[] LEARNING_DATA_LIST = {
		TARGET_MODEL, 
		//"A039030", 
		//"A000480"
	};
	
	public static void main(String[] args) throws IOException {
		// checkModelExistAndTopCompanies();
		// regressionMatrix();
		// trainMultipleStocks();
		String[] targets = {
//				"A161390",
//				"A139480",
//				"A096770",
//				"A066570",
//				"A128940",
//				"A086790",
//				"A055550",
//				"A033780",
//				"A003550",
//				"A069500",
//				"A005490",
//				"A000660",
//				"A035250",
//				"A012330",
//				"A000270",
//				"A005930",
//				"A004020",
//				"A036570",
				// "A010130",	-- Out of Memory
				"A105560",
				"A010950",
				"A017670",
				"A018260",
				"A021240",
				"A051900",
				"A034730",
				"A051910",
				"A024110",
				"A011170",
				"A034220",
				"A006400",
				"A000810",
				"A005935",
				"A009150",
				"A005380",
				"A015760"
		};
		int count = 0;
		for(String targetStockId : targets) {
			count++;
			log.info("################### [" + count + "/" + targets.length + "] - " + targetStockId + " #################");
			trainSpecificStocks(targetStockId);
		}
	}
	
	public static void regressionMatrix() throws IOException {
		List<String> validStockIds = checkModelExistAndTopCompanies();
		String startDate = FileUtil.ML_BASE_DATE;
		String endDate = "20171201";
		for(String stockA: validStockIds) {
			for(String stockB : validStockIds) {
				try {
					float roiCorr = DailyStockDao.getCorrelation(startDate,endDate, stockA, stockB);
					DailyStockDao.insertCorrValue(startDate, endDate, stockA, stockB, roiCorr);
					log.info(String.format("[%s][%s] Corr value : %f", stockA, stockB, roiCorr));
				} catch (SQLException sqle) {
					log.warn("The corr value between " + stockA + " and " + stockB + " can't be estimated. because of " + sqle.getMessage());
				}
			}
		}
	}
	
	public static void trainSpecificStocks(String stockId) throws IOException {
		MetricCreator creator = new MetricCreator();
		String modelPath = FileUtil.getStockModelPath(FileUtil.ML_BASE_DATE, stockId);
		trainWithCorrelatedAndUncorrelatedStocks(creator, stockId, modelPath);
	}
	
	public static void trainMultipleStocks() throws IOException {
		List<String> validSources = FileUtil.getValidBaseMLDataFileList();
		MetricCreator creator = new MetricCreator();
		List<String> targetStocks = checkModelExistAndTopCompanies();
		for(int count = 0; count < targetStocks.size(); count++) {
			String stockId = targetStocks.get(count);
			String modelPath = FileUtil.getStockModelPath(FileUtil.ML_BASE_DATE, stockId);
			PredictMetric lastMetric = trainWithCorrelatedAndUncorrelatedStocks(creator, stockId, modelPath);
			/*
			if(lastMetric != null && lastMetric.getAccuracy() > 0.75f) {
				creator.calculateOneMatrix(modelPath, validSources);
			} else {
				log.warn(
						String.format("The stock[%s] has not been learned fully. It's accuracy is %f", 
								stockId, 
								lastMetric == null ? "null" : lastMetric.getAccuracy()
										)
						);
			}
			*/

		}
	}
	
	private static PredictMetric trainWithOtherStocks(MetricCreator creator, String stockId, String modelPath,
			List<String> mostCorrelateds, List<String> mostUncorrelateds, boolean needRenew,
			int times) {
		PredictMetric lastMetric = null;
		String[] relatedStocks = new String[2];
		if(mostCorrelateds == null || mostCorrelateds.size() < 1) {
			mostCorrelateds = new ArrayList<String>();
			mostCorrelateds.add(stockId);
		}
		if(mostUncorrelateds == null || mostUncorrelateds.size() < 1) {
			mostUncorrelateds = new ArrayList<String>();
			mostUncorrelateds.add(stockId);
		}
		relatedStocks[1] = stockId;
		// Many Input make the OOM. So cut thie input files just two files.
		// For 1 time, Make model and training.
		if(needRenew) {
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), needRenew);
			// For 1 times. Training with uncorrelated ones.
			// relatedStocks[0] = mostUncorrelateds.get((int)Math.floor(mostUncorrelateds.size() * Math.random()));
			// lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			// For 3 times, Training with correlated ones.
			relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			//relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			//lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			//relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			// For 1 times, Training with original ones.
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), true);
		} else {
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), needRenew);
			/*
			// For 4 times, Training with correlated ones.
			relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			// For 1 times, Training with correlated ones.
			relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			*/
			relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			//lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), true);
			//lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), true);
		}
		return lastMetric;
	}
	
	private static PredictMetric trainWithCorrelatedAndUncorrelatedStocks(MetricCreator creator, String stockId, String modelPath) {
		PredictMetric lastMetric = null;
		try {
			List<String> mostCorrelateds = DailyStockDao.getTopNCorrelatedStocks(stockId, true);
			List<String> mostUncorrelateds = DailyStockDao.getTopNCorrelatedStocks(stockId, false);
			log.debug("Learning target is " + String.join(",", mostCorrelateds));
			log.debug("Learning target is " + String.join(",", mostUncorrelateds));
			float accuracy = DailyStockDao.getLastPredictMetric(stockId).getAccuracy();
			log.debug("Accuracy is " + accuracy);
			if(accuracy > SKIP_THRESHOLD)
				return lastMetric;
			boolean needModelRenew = accuracy < RENEWAL_THRESHOLD;
			lastMetric = trainWithOtherStocks(creator, stockId, modelPath, mostCorrelateds, mostUncorrelateds, needModelRenew, 3);
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}
		return lastMetric;
	}
	
	public static List<String> checkModelExistAndTopCompanies() throws IOException {
		List<String[]> idAndNames = DailyStockDao.getTargetCompanies(1000);
		List<String> validModels = FileUtil.getValidModelFileList();
		List<String> validModelIds = new ArrayList<String>();
		List<String> validStockIds = new ArrayList<String>();
		for(String modelPath : validModels) {
			String stockId = FileUtil.getStockIdFromBaseDataCsvFilePath(modelPath);
			validModelIds.add(stockId);
		}
		for(String[] idAndName : idAndNames) {
			if(validModelIds.contains(idAndName[0])) {
				log.info("This company [" + idAndName[0] + ":" + idAndName[1] + "]has valid ");
				validStockIds.add(idAndName[0]);
			}
		}
		return validStockIds;
	}
	
}
