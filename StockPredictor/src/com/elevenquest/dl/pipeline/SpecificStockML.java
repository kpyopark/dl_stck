package com.elevenquest.dl.pipeline;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

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

	private final static String TARGET_MODEL = "A008560";
	
	private final static float RENEWAL_THRESHOLD = 0.25f;
	private final static float SKIP_THRESHOLD = 0.75f;
	
	private final static String[] LEARNING_DATA_LIST = {
		TARGET_MODEL, 
		//"A039030", 
		//"A000480"
	};
	
	private static void retrainValidModels() throws IOException, SQLException {
		List<String[]> targetCompanyIdAndNames = DailyStockDao.getTargetCompanies(50);
		List<String> targets = new ArrayList<String>();
		for(String[] idAndName : targetCompanyIdAndNames) targets.add(idAndName[0]);
		int count = 0;
		for(String targetStockId : targets) {
			count++;
			log.info("################### [" + count + "/" + targets.size() + "] - " + targetStockId + " #################");
			trainSpecificStocks(targetStockId);
		}
	}
	
	private static void trainSpecificStockWithMultipleDataSets(String[] args) throws IOException, SQLException {
		
	}
	
	public static void main(String[] args) throws IOException, SQLException {
		// checkModelExistAndTopCompanies();
		// regressionMatrix();
		// trainMultipleStocks();
		if(args.length > 0)
			trainSpecificStocks(args[0]);
		else
			retrainValidModels();
		
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
		trainWithCorrelatedAndUncorrelatedStocks(creator, stockId, modelPath, true);
	}
	
	public static void trainMultipleStocks() throws IOException {
		List<String> validSources = FileUtil.getValidBaseMLDataFileList();
		MetricCreator creator = new MetricCreator();
		List<String> targetStocks = checkModelExistAndTopCompanies();
		for(int count = 0; count < targetStocks.size(); count++) {
			String stockId = targetStocks.get(count);
			String modelPath = FileUtil.getStockModelPath(FileUtil.ML_BASE_DATE, stockId);
			PredictMetric lastMetric = trainWithCorrelatedAndUncorrelatedStocks(creator, stockId, modelPath, false);
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
			log.debug("New Model Created.");
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), false);
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
			log.debug("Old Model reused.");
			// For 4 times, Training with correlated ones.
			relatedStocks[0] = mostCorrelateds.get((int)Math.floor(mostCorrelateds.size() * Math.random()));
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, relatedStocks), true);
			lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), true);
			//lastMetric = creator.trainOneModel(modelPath, FileUtil.getBaseDataCsvFilePaths(FileUtil.ML_BASE_DATE, new String[]{stockId}), true);
		}
		return lastMetric;
	}
	
	private static PredictMetric trainWithCorrelatedAndUncorrelatedStocks(MetricCreator creator, String stockId, String modelPath, boolean needSkip) {
		PredictMetric lastMetric = null;
		try {
			List<String> mostCorrelateds = DailyStockDao.getRelatedPredictMatric(stockId, true)
					.stream()
					.map(predict -> predict.getPredictStockId())
					.collect(Collectors.toList());
			List<String> mostUncorrelateds = DailyStockDao.getRelatedPredictMatric(stockId, false)
					.stream()
					.map(predict -> predict.getPredictStockId())
					.collect(Collectors.toList());
			log.debug("Correlated learning targets are " + String.join(",", mostCorrelateds));
			log.debug("Uncorrelated learning targets are " + String.join(",", mostUncorrelateds));
			float accuracy = DailyStockDao.getLastPredictMetric(stockId).getAccuracy();
			log.debug("Accuracy on Database is " + accuracy);
			if(accuracy > SKIP_THRESHOLD && !needSkip) {
				log.debug("This item need not more learning cause of high accuracy.");
				return lastMetric;
			}
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
