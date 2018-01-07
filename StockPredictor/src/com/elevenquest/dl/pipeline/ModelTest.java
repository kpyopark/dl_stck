package com.elevenquest.dl.pipeline;

import java.util.List;

import com.elevenquest.dl.pipeline.dao.DailyStockDao;

public class ModelTest {
    final static String[] TRAIN_CSV_PATHS = { 
    		"E:/Document/A005930_20130101_org.csv", 
    		"E:/Document/A008560_20130101_org.csv",
    		"E:/Document/A114800_20160501_org.csv" };

    public static void main(String[] args) throws Exception {
		String stockId = "A008560";
		String lastDate = DailyStockDao.getLastDay();
		//trainFirst(stockId);
		//DbToCsvConverter.makeCsv(stockId, FileUtil.ML_BASE_DATE,FileUtil.getDailyPredictTargetFilePath(lastDate, DailyStockDao.getClosedDay(), stockId, FileUtil.ML_BASE_DATE));
		List<String> closedDays = DailyStockDao.getClosedDay();
		String nextWorkDay = FileUtil.getNextWorkday(lastDate, closedDays);
		PredictNextDay.predictOneStock(stockId, lastDate, nextWorkDay, closedDays, FileUtil.ML_BASE_DATE);
		
	}
    
    private static void trainFirst(String stockId) throws Exception {
    	String modelPath = FileUtil.getStockModelPath(FileUtil.ML_BASE_DATE, stockId);
    	StockMultiLayerPredictor trainer = new StockMultiLayerPredictor(modelPath, TRAIN_CSV_PATHS, false, false);
    	trainer.train();
    }
}
