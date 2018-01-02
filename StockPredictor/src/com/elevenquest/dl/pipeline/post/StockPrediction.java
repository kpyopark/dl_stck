package com.elevenquest.dl.pipeline.post;

public class StockPrediction {
	String predictStandardDate;
	String stockId;
	String modelStandardDate;
	String modelLearningStockId;
	String modelTargetDate;
	String modelPredictStockId;
	int modelLearnCount;
	float startPrice;
	float lowPrice;
	float highPrice;
	float endPrice;
	int predictResult;
	float accuracy;
	float precisions;
	float recall;
	boolean predicted;

	public String getPredictStandardDate() {
		return predictStandardDate;
	}

	public void setPredictStandardDate(String predictStandardDate) {
		this.predictStandardDate = predictStandardDate;
	}

	public String getStockId() {
		return stockId;
	}

	public void setStockId(String stockId) {
		this.stockId = stockId;
	}

	public String getModelStandardDate() {
		return modelStandardDate;
	}

	public void setModelStandardDate(String modelStandardDate) {
		this.modelStandardDate = modelStandardDate;
	}

	public String getModelLearningStockId() {
		return modelLearningStockId;
	}

	public void setModelLearningStockId(String modelLearningStockId) {
		this.modelLearningStockId = modelLearningStockId;
	}

	public String getModelTargetDate() {
		return modelTargetDate;
	}

	public void setModelTargetDate(String modelTargetDate) {
		this.modelTargetDate = modelTargetDate;
	}

	public String getModelPredictStockId() {
		return modelPredictStockId;
	}

	public void setModelPredictStockId(String modelPredictStockId) {
		this.modelPredictStockId = modelPredictStockId;
	}

	public int getModelLearnCount() {
		return modelLearnCount;
	}

	public void setModelLearnCount(int modelLearnCount) {
		this.modelLearnCount = modelLearnCount;
	}

	public float getStartPrice() {
		return startPrice;
	}

	public void setStartPrice(float startPrice) {
		this.startPrice = startPrice;
	}

	public float getLowPrice() {
		return lowPrice;
	}

	public void setLowPrice(float lowPrice) {
		this.lowPrice = lowPrice;
	}

	public float getHighPrice() {
		return highPrice;
	}

	public void setHighPrice(float highPrice) {
		this.highPrice = highPrice;
	}

	public float getEndPrice() {
		return endPrice;
	}

	public void setEndPrice(float endPrice) {
		this.endPrice = endPrice;
	}

	public int getPredictResult() {
		return predictResult;
	}

	public void setPredictResult(int predictResult) {
		this.predictResult = predictResult;
	}

	public float getAccuracy() {
		return accuracy;
	}

	public void setAccuracy(float accuracy) {
		this.accuracy = accuracy;
	}

	public float getPrecisions() {
		return precisions;
	}

	public void setPrecisions(float precisions) {
		this.precisions = precisions;
	}

	public float getRecall() {
		return recall;
	}

	public void setRecall(float recall) {
		this.recall = recall;
	}

	public boolean isPredict() {
		return predicted;
	}

	public void setPredict(boolean isPredict) {
		this.predicted = isPredict;
	}
}
