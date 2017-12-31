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
	
	private final static String[] LEARNING_DATA_LIST = {
		TARGET_MODEL, 
		//"A039030", 
		//"A000480"
	};
	
	public static void main(String[] args) throws IOException {
		// checkModelExistAndTopCompanies();
		// regressionMatrix();
		trainMultipleStocks();
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
	
	private static PredictMetric trainWithCorrelatedAndUncorrelatedStocks(MetricCreator creator, String stockId, String modelPath) {
		PredictMetric lastMetric = null;
		try {
			List<String> mostCorrelateds = DailyStockDao.getTopNCorrelatedStocks(stockId, true);
			List<String> mostUncorrelateds = DailyStockDao.getTopNCorrelatedStocks(stockId, false);
			log.debug("Learning target is " + String.join(",", mostCorrelateds));
			log.debug("Learning target is " + String.join(",", mostUncorrelateds));
			String[] relatedStocks = new String[2];
			relatedStocks[1] = stockId;
			// Many Input make the OOM. So cut thie input files just two files.
			// For 1 time, Make model and training.
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), false);
			// For 3 times. Training with uncorrelated ones.
			for(int learnCount = 0; learnCount < 3; learnCount++) {
				relatedStocks[0] = mostUncorrelateds.get((int)Math.floor(mostUncorrelateds.size() * Math.random()));
				lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			}
			// For 1 times, Training with correlated ones.
			relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			// For 2 times, Training with original ones.
			for(int learnCount = 0; learnCount < 2; learnCount++) {
				lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), true);
			}
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), true);
			// For 3 times. Training with uncorrelated ones.
			for(int learnCount = 0; learnCount < 3; learnCount++) {
				relatedStocks[0] = mostUncorrelateds.get((int)Math.floor(mostUncorrelateds.size() * Math.random()));
				lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			}
			// For 1 times, Training with correlated ones.
			relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			// For 2 times, Training with original ones.
			for(int learnCount = 0; learnCount < 2; learnCount++) {
				lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), true);
			}
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
