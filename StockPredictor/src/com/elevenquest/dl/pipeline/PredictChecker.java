package com.elevenquest.dl.pipeline;

import java.util.List;

import org.deeplearning4j.eval.Evaluation;

import com.elevenquest.dl.pipeline.dao.DailyStockDao;
import com.elevenquest.dl.pipeline.post.PredictMetric;
import com.elevenquest.dl.pipeline.post.StockPrediction;

public class PredictChecker {
	public static void main(String[] args) throws Exception {
		List<String[]> targetCompanyIdAndNames = DailyStockDao.getTargetCompanies(50);
		String lastDate = DailyStockDao.getLastDay();
    	
		for(String[] stockIdAndName : targetCompanyIdAndNames) {
			try {
				String stockId = stockIdAndName[0];
				float dailyRoi = DailyStockDao.getRoi(stockId, lastDate);
				int result = StockMultiLayerPredictor.getResultintFromRoi(dailyRoi);
				List<StockPrediction> predictions = DailyStockDao.getStockPredictionWithNoResult(lastDate, stockId);
				StockPrediction currentStockValue = DailyStockDao.getCurrentStocInfo(lastDate, stockId);
				for(StockPrediction prediction : predictions) {
					prediction.setEndPrice(currentStockValue.getPrevEndPrice());
					prediction.setHighPrice(currentStockValue.getPrevHighPrice());
					prediction.setLowPrice(currentStockValue.getPrevLowPrice());
					prediction.setStartPrice(currentStockValue.getPrevStartPrice());
					prediction.setResult(result);
					DailyStockDao.updateStockPredict(prediction);
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
