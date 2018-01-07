package com.elevenquest.dl.pipeline.post;

public class StockPrediction {
	String predictStandardDate;
	String stockId;
	String modelStandardDate;
	String modelLearningStockId;
	String modelTargetDate;
	String modelPredictStockId;
	int modelLearnCount;
	Double startPrice;
	Double lowPrice;
	Double highPrice;
	Double endPrice;
	double prevStartPrice;
	double prevLowPrice;
	double prevHighPrice;
	double prevEndPrice;

	Integer predictResult;
	float accuracy;
	float precisions;
	float recall;
	boolean predicted;

	Integer result;
	
	public double getPrevStartPrice() {
		return prevStartPrice;
	}

	public void setPrevStartPrice(double prevStartPrice) {
		this.prevStartPrice = prevStartPrice;
	}

	public double getPrevLowPrice() {
		return prevLowPrice;
	}

	public void setPrevLowPrice(double prevLowPrice) {
		this.prevLowPrice = prevLowPrice;
	}

	public double getPrevHighPrice() {
		return prevHighPrice;
	}

	public void setPrevHighPrice(double prevHighPrice) {
		this.prevHighPrice = prevHighPrice;
	}

	public double getPrevEndPrice() {
		return prevEndPrice;
	}

	public void setPrevEndPrice(double prevEndPrice) {
		this.prevEndPrice = prevEndPrice;
	}

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

	public Double getStartPrice() {
		return startPrice;
	}

	public void setStartPrice(Double startPrice) {
		this.startPrice = startPrice;
	}

	public Double getLowPrice() {
		return lowPrice;
	}

	public void setLowPrice(Double lowPrice) {
		this.lowPrice = lowPrice;
	}

	public Double getHighPrice() {
		return highPrice;
	}

	public void setHighPrice(Double highPrice) {
		this.highPrice = highPrice;
	}

	public Double getEndPrice() {
		return endPrice;
	}

	public void setEndPrice(Double endPrice) {
		this.endPrice = endPrice;
	}

	public Integer getPredictResult() {
		return predictResult;
	}

	public void setPredictResult(Integer predictResult) {
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

	public Integer getResult() {
		return result;
	}

	public void setResult(Integer result) {
		this.result = result;
	}
}
