package com.elevenquest.dl.pipeline.post;

public class PredictMetric {
	public String getBaseStandardDate() {
		return baseStandardDate;
	}
	public void setBaseStandardDate(String baseStandardDate) {
		this.baseStandardDate = baseStandardDate;
	}
	public String getLearningStockId() {
		return learningStockId;
	}
	public void setLearningStockId(String learningStockId) {
		this.learningStockId = learningStockId;
	}
	public String getPredictTargetDate() {
		return predictTargetDate;
	}
	public void setPredictTargetDate(String predictTargetDate) {
		this.predictTargetDate = predictTargetDate;
	}
	public String getPredictStockId() {
		return predictStockId;
	}
	public void setPredictStockId(String predictStockId) {
		this.predictStockId = predictStockId;
	}
	public int getTotalCount() {
		return totalCount;
	}
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
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
	public int getResult00() {
		return result00;
	}
	public void setResult00(int result00) {
		this.result00 = result00;
	}
	public int getResult01() {
		return result01;
	}
	public void setResult01(int result01) {
		this.result01 = result01;
	}
	public int getResult02() {
		return result02;
	}
	public void setResult02(int result02) {
		this.result02 = result02;
	}
	public int getResult10() {
		return result10;
	}
	public void setResult10(int result10) {
		this.result10 = result10;
	}
	public int getResult11() {
		return result11;
	}
	public void setResult11(int result11) {
		this.result11 = result11;
	}
	public int getResult12() {
		return result12;
	}
	public void setResult12(int result12) {
		this.result12 = result12;
	}
	public int getResult20() {
		return result20;
	}
	public void setResult20(int result20) {
		this.result20 = result20;
	}
	private String baseStandardDate;
	private String learningStockId;
	private String predictTargetDate;
	private String predictStockId;
	private int totalCount;
	private float accuracy;
	private float precisions;
	private float recall;
	private int result00;
	private int result01;
	private int result02;
	private int result10;
	private int result11;
	private int result12;
	private int result20;
	private int result21;
	private int result22;
	private int learnCount;
	private float score;
	public float getScore() {
		return score;
	}
	public void setScore(float score) {
		this.score = score;
	}
	public int getLearnCount() {
		return learnCount;
	}
	public void setLearnCount(int learnCount) {
		this.learnCount = learnCount;
	}
	public int getResult21() {
		return result21;
	}
	public void setResult21(int result21) {
		this.result21 = result21;
	}
	public int getResult22() {
		return result22;
	}
	public void setResult22(int result22) {
		this.result22 = result22;
	}
	
}
