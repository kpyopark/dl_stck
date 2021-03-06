package com.elevenquest.dl.pipeline;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.AutoEncoder;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerMinMaxScaler;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.util.FeatureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.elevenquest.dl.pipeline.dao.DailyStockDao;

/**
 * @author 
 */
public class StockMultiLayerPredictor {

    private static Logger log = LoggerFactory.getLogger(StockMultiLayerPredictor.class);
    
    final static int[] LAYER_NODES = { 500, 100, 10, 100 };
    final int epochTime = 20;
    final int iterations = 100;
    final int maxDataSize = 1201;
    final int miniBatchSize = 400;
    final float learningRate = 0.001f;
    
    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");

    public void buildModel() {
        log.info("Build model....");
        int layer = 0;
        MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
            .seed(seed)
            .iterations(iterations)
            .activation(Activation.TANH)
            .weightInit(WeightInit.XAVIER)
            .updater(Updater.ADAGRAD)
            .learningRate(learningRate)
            .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
            .regularization(true).l2(1e-4)
            .list()
            .layer(layer, new DenseLayer.Builder().nIn(numInputs).nOut(LAYER_NODES[layer]).activation(Activation.RELU).dropOut(0.15f)
                .build())
            .layer(++layer, new DenseLayer.Builder().nIn(LAYER_NODES[layer-1]).nOut(LAYER_NODES[layer]).activation(Activation.RELU).dropOut(0.15f)
                .build())
            .layer(++layer, new DenseLayer.Builder().nIn(LAYER_NODES[layer-1]).nOut(LAYER_NODES[layer]).activation(Activation.RELU) //.dropOut(0.15f)
                    .build())
            .layer(++layer, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .activation(Activation.SOFTMAX)
                .nIn(LAYER_NODES[layer-1]).nOut(outputNum).build())
            .backprop(true)
            .pretrain(true)
            .build();

        //run the model
        model = new MultiLayerNetwork(conf);
        model.init();
    }

    public static void main(String[] args) throws  Exception {
    	predictDailyMetric();
    }
    
    public static void predictDailyMetric() throws Exception {
    	List<String> closedDays = DailyStockDao.getClosedDay();
    	String predictTargetDirectory = FileUtil.getDailyPredictTargetDirectory(DailyStockDao.getLastDay(), closedDays);
    	List<String> csvFiles = FileUtil.getDailyPredictTargetList(predictTargetDirectory);
    	for(String csvFileName : csvFiles) {
    		if( csvFileName.contains("csv") ) {
    			FileDelegator file = null;
    			try {
    				String fileFullPath = predictTargetDirectory + "/" + csvFileName;
    				file = new FileDelegator(fileFullPath);
    				log.info("Target CSV File is " + predictTargetDirectory + "/" + csvFileName);
    				if(file.getLength() > 1000) {
    					StockMultiLayerPredictor predictor = new StockMultiLayerPredictor(
    							FileUtil.DEFAULT_MODEL_PATH,
    							null,
    							true, false);
    					predictor.predict(fileFullPath);
    				} else {
    					log.info("This file doesn't have contents. Skipped");
    				}
    			} catch (Exception e) {
    				log.error(e.getMessage(),e);
    			} finally {
    				file.close();
    			}
    		}
    	}
    }
    
    String modelPath;
    String[] learningSources;
    
    FileDelegator file = null;
    boolean predictYn = false;
    boolean preferReusedModel = false;
    boolean modelExist = false;
    
    public StockMultiLayerPredictor(String modelPath, 
    		String[] learningSources,
    		boolean predictYn, 
    		boolean preferReusedModel) {
    	this.modelPath = modelPath;
    	this.learningSources = learningSources;
    	this.predictYn = predictYn;
    	this.preferReusedModel = preferReusedModel;
    	if(modelPath != null) {
    		try {
        		file = new FileDelegator(modelPath);
    			file.getLength();
    			modelExist = true;
    		} catch (Exception e) {
                log.warn("This model file doens't exist or can't be accessed.:" + this.modelPath);
    			modelExist = false;
    		}
    	}
    }
    
    public Evaluation train() throws Exception {
    	if(this.predictYn)
    		throw new Exception("This predictor in on prediction mode. It can't be trained. You should make this class instance with false value in predictYn field.");
    	readData(learningSources);
    	makeLabels(thresholds);
    	if(preferReusedModel && modelExist) {
        	loadModel();
    	} else {
        	buildModel();
    	}
    	trainModel();
    	saveModel();
    	return evaluate();
    }
    
    public Evaluation trainWithPrevModel() throws Exception {
    	if(this.predictYn)
    		throw new Exception("This predictor in on prediction mode. It can't be trained. You should make this class instance with false value in predictYn field.");
    	readData(learningSources);
    	makeLabels(thresholds);
    	loadModel();
    	trainModel();
    	saveModel();
    	return evaluate();
    }
    
    public Evaluation predict(String csvFile) throws Exception {
    	loadModel();
    	readData(new String[] { csvFile });
    	makeLabels(thresholds);
    	return predict();
    }
    
    public static int getResultintFromRoi(float roi) {
    	int count = 0;
    	for(float baseRoi:thresholds) {
    		count += roi > baseRoi ? 1 : 0;
    	}
    	return count;
    }
    
    public static int getResultFromString(String result) {
    	return "Sell".equals(result) ? 0 :
			"N/A".equals(result) ? 1 : 2;
    }
    
    public static String getLabelStringFromResultInt(int result) {
    	return outputLabel.get(result);
    }
    
    static final int numInputs = 387;
    static final float[] thresholds = { 0.042f , -0.02f };
    static final int outputNum = thresholds.length + 1;
    static final List<String> outputLabel = Arrays.asList(new String[]{"Sell","N/A","Buy"}); 
    long seed = 6;
    
    DataSet orgData = null;
    DataSet allData = null;
    DataSet labeledData = null;
    DataSet targetMiniBatchData = null;
    DataSet trainingData = null;
    DataSet testData = null;
    
    MultiLayerNetwork model = null;
    
    Evaluation eval = null;
    
    public void readData(String[] dataFilePaths) throws Exception {
        int numLinesToSkip = 1;
        String delimiter = ",";
        List<DataSet> mergedTarget = new ArrayList<DataSet>();
        for(String trainFile : dataFilePaths) {
            RecordReader recordReader = new CSVRecordReader(numLinesToSkip,delimiter);
            FileDelegator fileDelegator = null;
            try {
                fileDelegator = new FileDelegator(trainFile);
                File tempFile = fileDelegator.getTempFile();
                log.debug("Train File[" + trainFile + "] TEMPFILE[" + tempFile.getAbsolutePath() + "] : Size[" + tempFile.length() + "]");
                recordReader.initialize(new FileSplit(tempFile));
                DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader,maxDataSize);
                mergedTarget.add(iterator.next());
            } finally {
            	if(fileDelegator != null) try {fileDelegator.close();} catch (Exception e) {}
            }
        }
        orgData = DataSet.merge(mergedTarget);
    }
    
    public void makeLabels(final float[] thresholds) {
        INDArray labelRawData = orgData.getFeatures().getColumn(numInputs);
        INDArray labels = Nd4j.zeros(labelRawData.rows(), 1);
        for(float threshold : thresholds) {
        	labels = labels.add(labelRawData.gt(threshold));
        }
        // System.out.println(labels);
        allData = new DataSet(
        		orgData.getFeatures().get(NDArrayIndex.all(), NDArrayIndex.interval(0, numInputs)), 
        		FeatureUtil.toOutcomeMatrix(labels.data().asInt(), outputNum), 
        		null, null);
        allData.setLabelNames(outputLabel);
        
        int minNumExample = Integer.MAX_VALUE;
        List<DataSet> labelDataSets = new ArrayList<DataSet>();
        int[] labelFilter = new int[1];
        for(int inx = 0 ; inx < outputNum; inx++) {
        	labelFilter[0] = inx;
        	DataSet labelDataSet = allData.filterBy(labelFilter);
        	labelDataSet.shuffle();
        	labelDataSets.add(labelDataSet);
        	if(minNumExample > labelDataSet.numExamples())
        		minNumExample = labelDataSet.numExamples();
        }
        if(minNumExample == Integer.MAX_VALUE)
        	minNumExample = 1;
        List<DataSet> tempMergeList = new ArrayList<DataSet>();
        for(int inx = 0 ; inx < outputNum; inx++) {
        	tempMergeList.add(labelDataSets.get(inx).sample(minNumExample));
        }
        
        labeledData = DataSet.merge(tempMergeList);
        labeledData.shuffle();
        // normalize();
    }
    
    private void nextDataSet() {
    	labeledData.shuffle();
    	targetMiniBatchData = labeledData.sample(miniBatchSize);
    }
    
    private void normalize() {
        
        SplitTestAndTrain testAndTrain = targetMiniBatchData.splitTestAndTrain(0.65);  //Use 65% of data for training
        trainingData = testAndTrain.getTrain();
        testData = testAndTrain.getTest();

        //DataNormalization normalizer = new NormalizerMinMaxScaler(-1, 1);
        //normalizer.
        //normalizer.fit(trainingData);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
        //normalizer.transform(trainingData);     //Apply normalization to the training data
        //normalizer.transform(testData);         //Apply normalization to the test data. This is using statistics calculated from the *training* set
    }
    
    private void trainModel() {
    	for(int cnt = 0 ; cnt < epochTime; cnt++) {
    		nextDataSet();
    		normalize();
            model.setListeners(new ScoreIterationListener(100));
            model.fit(trainingData);
    	}
    }
    
    public Evaluation evaluate() {
        eval = new Evaluation(outputNum);
        DataSet testTarget = null;
        log.info("original test data set # :" + testData.numExamples());
        log.info("original test data set input # :" + testData.numInputs());
        log.info("original test data set output # :" + testData.numOutcomes());
        if(testData.numExamples() > 400) {
        	testTarget = testData.splitTestAndTrain(0.5).getTest();
        } else {
        	testTarget = testData;
        }
        log.info("test data set # :" + testTarget.numExamples());
        INDArray featureMatrix = testTarget.getFeatureMatrix();
        log.info("Feature Matrix Shapes:" + featureMatrix.shapeInfoToString());
        INDArray output = model.output(featureMatrix);
        eval.eval(testTarget.getLabels(), output);
        log.info(eval.stats());
        return eval;
    }
    
    public double getModelScore() {
    	return model.score();
    }
    
    public void saveModel() throws Exception {
    	ModelSerializer.writeModel(model, 
    			file.getTempFile(), 
    			true);
        file.flush();
    	
    }
    
    public void loadModel() throws Exception {
    	this.model = ModelSerializer.restoreMultiLayerNetwork(file.getTempFile());
    	this.model.printConfiguration();
    	for(Entry<String, Double> entry : this.model.conf().getLearningRateByParam().entrySet()) {
    		log.debug(entry.getKey() + ":" + entry.getValue());
    	}
    }
    
    private Evaluation predict() {
    	DataSet copySet = allData.copy();
        //DataNormalization normalizer = new NormalizerMinMaxScaler(-0.155, 0.114);
        //normalizer.fit(copySet);
        //normalizer.transform(copySet);
        Evaluation eval = new Evaluation(outputNum);
        INDArray output = model.output(copySet.getFeatureMatrix());
        eval.eval(copySet.getLabels(), output);
        //log.info(eval.stats());
        List<String> predicted = model.predict(copySet);
    	//System.out.println(predicted);
        log.info("accuracy:" + eval.accuracy());
        log.info("precision:" + eval.precision());
        log.info("recall:" + eval.recall());
        log.info("Total Precited Value List:" + predicted);
        log.info("Tommorow Predicted Value:" + predicted.get(predicted.size()-1));
        return eval;
    }
    
    public List<String> getPredictionResult() {
    	DataSet copySet = allData.copy();
        DataNormalization normalizer = new NormalizerStandardize();
        normalizer.fit(copySet);
        normalizer.transform(copySet);
        List<String> predicted = model.predict(copySet);
        return predicted;
    }
    
}

