package com.elevenquest.dl.pipeline;

import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;

import org.deeplearning4j.eval.ConfusionMatrix;
import org.deeplearning4j.eval.Evaluation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.AmazonWebServiceClient;
import com.amazonaws.services.logs.AWSLogsClient;
import com.amazonaws.services.s3.AmazonS3Client;
import com.elevenquest.dl.pipeline.dao.DailyStockDao;
import com.elevenquest.dl.pipeline.post.PredictMetric;

/**
 * 
 * @author user
 *
 * 1. List up valid csv files in BaseML directory. (this datum are extracted from 2012's)
 * 
 * 2. Make each stock Model
 * 
 * 3. Predict each stock by using base csv file.
 * 
 * 4. Make matrix using today's source 
 * 
 *  
 *
 */
public class MetricCreator {
	
	private static Logger log = LoggerFactory.getLogger(MetricCreator.class);
	
	public static PredictMetric getPredicMetricFromEvaluation(Evaluation eval) {
		PredictMetric metric = new PredictMetric();
		metric.setAccuracy((float)eval.accuracy());
		metric.setPrecisions((float)eval.precision());
		metric.setRecall((float)eval.recall());
		metric.setTotalCount(eval.getNumRowCounter());
		ConfusionMatrix<Integer> matrix = eval.getConfusionMatrix();
		metric.setResult00(matrix.getCount(0, 0));
		metric.setResult01(matrix.getCount(0, 1));
		metric.setResult02(matrix.getCount(0, 2));
		metric.setResult10(matrix.getCount(1, 0));
		metric.setResult11(matrix.getCount(1, 1));
		metric.setResult12(matrix.getCount(1, 2));
		metric.setResult20(matrix.getCount(2, 0));
		metric.setResult21(matrix.getCount(2, 1));
		metric.setResult22(matrix.getCount(2, 2));
		return metric;
	}
	
	public void makeModels() throws IOException {
		List<String> validSources = FileUtil.getValidBaseMLDataFileList();
		int count = 0;
		for(String mlsource : validSources) {
			count++;
			log.info("Target[" + count + "/" + validSources.size() + "] is " + mlsource);
			try {
				String stockId = FileUtil.getStockIdFromBaseDataCsvFilePath(mlsource);
				String baseDt = FileUtil.getStartDateFromBaseDataCsvFilePath(mlsource);
				StockMultiLayerPredictor modelMaker = new StockMultiLayerPredictor(
						FileUtil.getStockModelPath(baseDt, stockId),
						mlsource, 
						false /* predictYn */,
						true /* prefer to reuse exist model. */);
				Evaluation evaluation = modelMaker.train();
				PredictMetric metric = getPredicMetricFromEvaluation(evaluation);
				metric.setBaseStandardDate(baseDt);
				metric.setLearningStockId(stockId);
				metric.setPredictTargetDate(baseDt);
				metric.setPredictStockId(stockId);
				DailyStockDao.appendPredictMetric(metric);
				log.info(evaluation.toString());
			} catch (Exception e) {
				log.error("[" + mlsource + "] has errors." );
				e.printStackTrace();
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		AmazonWebServiceClient client = new AmazonS3Client();
		LogManager manager = LogManager.getLogManager();
		
		System.out.println("############## LOGMANAGER :" + manager);
		Enumeration<String> names = manager.getLoggerNames();
		while(names.hasMoreElements()) {
			System.out.println("logger:" + names.nextElement());
		}
		java.util.logging.Logger logger = LogManager.getLogManager().getLogger("com.amazonaws.request");
		if(logger != null) {
			logger.setLevel(Level.OFF);
		}
		MetricCreator creator = new MetricCreator();
		creator.makeModels();
	}
	
}
