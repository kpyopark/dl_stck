package com.elevenquest.dl.pipeline;

import java.util.List;

import org.deeplearning4j.eval.Evaluation;

import com.elevenquest.dl.pipeline.dao.DailyStockDao;
import com.elevenquest.dl.pipeline.post.PredictMetric;
import com.elevenquest.dl.pipeline.post.StockPrediction;

public class PredictNextDay {
	public static void main(String[] args) throws Exception {
		List<String[]> targetCompanyIdAndNames = DailyStockDao.getTargetCompanies(50);
		String startDate = "20160101";
		String lastDate = DailyStockDao.getLastDay();
		List<String> closedDays = DailyStockDao.getClosedDay();
		String nextWorkDay = FileUtil.getNextWorkday(lastDate, closedDays);
    	
		for(String[] stockIdAndName : targetCompanyIdAndNames) {
			predictOneStock(stockIdAndName[0], lastDate, nextWorkDay, closedDays, startDate);
		}
	}
	
	public static void predictOneStock(String stockId, String lastDate, String nextWorkDay, List<String> closedDays, String baseDate) {
		try {
			String predictTargetFilePath = 
					FileUtil.getDailyPredictTargetFilePath(lastDate, closedDays, stockId, baseDate); 
			String modelPath = FileUtil.getStockModelPath(FileUtil.ML_BASE_DATE, stockId);
			PredictMetric lastMetric = null;
			lastMetric = DailyStockDao.getLastPredictMetric(stockId);
			StockMultiLayerPredictor predictor = new StockMultiLayerPredictor(
					modelPath,
					null,
					true, false);
			Evaluation eval = predictor.predict(predictTargetFilePath);
			List<String> predictions = predictor.getPredictionResult();
			String pedictionResult = predictions.get(predictions.size() -1);
			StockPrediction prediction = DailyStockDao.getCurrentStocInfo(lastDate, stockId);
			int result = StockMultiLayerPredictor.getResultFromString(pedictionResult);
			prediction.setAccuracy((float)eval.accuracy());
			//prediction.setEndPrice(endPrice);
			prediction.setModelLearnCount(lastMetric.getLearnCount());
			prediction.setModelLearningStockId(lastMetric.getLearningStockId());
			prediction.setModelPredictStockId(lastMetric.getPredictStockId());
			prediction.setModelStandardDate(lastMetric.getBaseStandardDate());
			prediction.setModelTargetDate(lastMetric.getPredictTargetDate());
			prediction.setPrecisions((float)eval.precision());
			prediction.setPredict(true);
			prediction.setPredictResult(result);
			prediction.setPredictStandardDate(nextWorkDay);
			prediction.setRecall((float)eval.recall());
			prediction.setStockId(stockId);
			DailyStockDao.insertPredictStock(prediction);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
}
