package com.elevenquest.dl.pipeline;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.eval.Evaluation;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration.ListBuilder;
import org.deeplearning4j.nn.conf.Updater;
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
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;
import org.nd4j.linalg.indexing.NDArrayIndex;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.nd4j.linalg.util.FeatureUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ModelBuilder {
	
	
    private static Logger log = LoggerFactory.getLogger(ModelBuilder.class);
    
    public static SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
    
    public static void main(String[] args) throws  Exception {
    	FileDelegator modelPath = new FileDelegator("s3://mldata-base/model/A008560_20120101.zip");
    	MultiLayerNetwork model = ModelSerializer.restoreMultiLayerNetwork(modelPath.getTempFile());
    	log.info(model.toString());
    }
    
    String[] learningSources;
    
    FileDelegator file = null;
    
    final int numOfEncoderLayers = 3;
    final int[] numOfNodesOnEncoderLayers = new int[numOfEncoderLayers];
    final float[] dropOutOnEncoderLayers = new float[numOfEncoderLayers];
    
    final int[] minNodeOnEncoderLayers = { 150 , 100, 50 };
    final int[] maxNodeOnEncoderLayers = { 1000, 800, 500 };
    
    final int numOfCategorizeLayers = 3;
    final int[] numOfNodesOnCategorizeLayers = new int[numOfCategorizeLayers];
    final float[] dropOutOnCategorizeLayers = new float[numOfCategorizeLayers];
    
    static final int numInputs = 387;
    static final float[] thresholds = { 0.042f , -0.02f };
    static final int outputNum = thresholds.length + 1;
    static final List<String> outputLabel = Arrays.asList(new String[]{"Sell","N/A","Buy"}); 
    final int iterations = 100;
    final int maxDataSize = 1201;
    final int miniBatchSize = 120;
    final float learningRate = 0.01f;

    final int epochTime = 40;
    long seed = 6;
    
    DataSet orgData = null;
    DataSet allData = null;
    DataSet labeledData = null;
    DataSet targetMiniBatchData = null;
    DataSet trainingData = null;
    DataSet testData = null;
    
    MultiLayerConfiguration conf = null;
    ListBuilder builder = null;
    MultiLayerNetwork model = null;
    
    Evaluation eval = null;

    public ModelBuilder(String[] learningSources) {
    	this.learningSources = learningSources;
    }
    
    private void initConf() {
        builder = new NeuralNetConfiguration.Builder()
                .seed(seed)
                .iterations(iterations)
                .activation(Activation.RELU)
                .weightInit(WeightInit.XAVIER)
                .updater(Updater.ADAGRAD)
                .learningRate(learningRate)
                .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                .regularization(true).l2(1e-4)
                .list();
    }
    
    private int configureEncoderLayer(int layerPosition) {
        int numOfInputNodes = numInputs;
        int numOfOutNodes = numInputs;
        
        for(int currentPosition = 0; currentPosition < layerPosition ; currentPosition++ ) {
        	numOfOutNodes = numOfNodesOnEncoderLayers[currentPosition];
        	builder.layer(currentPosition, new DenseLayer.Builder()
        			.nIn(numOfInputNodes)
        			.nOut(numOfOutNodes)
        			.dropOut(dropOutOnEncoderLayers[currentPosition])
        			.build());
        	numOfInputNodes = numOfOutNodes;
        }
        return numOfOutNodes;
    }
    
    private int configureCategorizeLayer(int layerPosition, int numOfInput) {
        int numOfInputNodes = numOfInput;
        int numOfOutNodes = numOfInput;
        int absLayerPosition;
        for(int currentPosition = 0; currentPosition < layerPosition ; currentPosition++ ) {
        	numOfOutNodes = numOfNodesOnCategorizeLayers[currentPosition];
        	absLayerPosition = numOfEncoderLayers + currentPosition;
        	builder.layer(
        			absLayerPosition, new DenseLayer.Builder()
        			.nIn(numOfInputNodes)
        			.nOut(numOfOutNodes)
        			.dropOut(dropOutOnCategorizeLayers[currentPosition])
        			.build());
        	numOfInputNodes = numOfOutNodes;
        }
        return numOfOutNodes;
    }

    private void buildEncoderModel(int layerPosition, int numOfNodes, float dropOutRatio) {
        log.info("Build Encoder Model....[" + layerPosition + "]");
        initConf();
        int numOfInputNodes = configureEncoderLayer(layerPosition);
        builder.layer(layerPosition, new DenseLayer.Builder()
    			.nIn(numOfInputNodes)
    			.nOut(numOfNodes)
    			.dropOut(dropOutRatio)
    			.build());
        conf = builder.build();
    }
    
    private void buildDecoderModel(int decodeLayerPosition, int numOfNodes, float dropOutRatio) {
        log.info("Build Decoder Model....[" + decodeLayerPosition + "]");
        initConf();
        int numOfInputNodes = configureEncoderLayer(numOfEncoderLayers);
        numOfInputNodes = configureCategorizeLayer(decodeLayerPosition, numOfInputNodes);
        int absLayerPosition = numOfEncoderLayers + decodeLayerPosition;
        builder.layer(
    			absLayerPosition, new DenseLayer.Builder()
    			.nIn(numOfInputNodes)
    			.nOut(numOfNodes)
    			.dropOut(dropOutRatio)
    			.build());
        builder.layer(absLayerPosition + 1, new OutputLayer.Builder(LossFunctions.LossFunction.NEGATIVELOGLIKELIHOOD)
                .activation(Activation.SOFTMAX)
                .nIn(100).nOut(outputNum).build())
            .backprop(true)
            .pretrain(true);
        
        conf = builder.build();
    }

    private void buildModel() {
        //run the model
        model = new MultiLayerNetwork(conf);
        model.init();
    }
    
    private float checkModelScore() {
    	// TODO :
    	return 0.0f;
    }

    private void buildEncoderLayer(int layerPosition) {
    	float minScore = Float.MAX_VALUE;
    	int finalNumOfNode = minNodeOnEncoderLayers[layerPosition];
    	float curScore = 0.0f;
    	for(int numOfNode = minNodeOnEncoderLayers[layerPosition] ; 
    			numOfNode < maxNodeOnEncoderLayers[layerPosition] ; numOfNode += 50) {
    		buildEncoderModel(layerPosition, numOfNode, 0.05f);
    		buildModel();
    		curScore = checkModelScore();
    		if(curScore < minScore) {
    			finalNumOfNode = numOfNode;
    			minScore = curScore;
    		}
    	}
    	log.info("Encoded Layer[" + layerPosition + "] : Recommended Node is " + finalNumOfNode);
    	numOfNodesOnEncoderLayers[layerPosition] = finalNumOfNode;
    }
    
    public Evaluation buildLayers() throws Exception {
    	readData(learningSources);
    	makeLabels(thresholds);
    	for(int layerCount = 0 ; layerCount < numOfEncoderLayers ; layerCount++) {
    		buildEncoderLayer(layerCount);
    	}
    	return evaluate();
    }
    
    private void readData(String[] dataFilePaths) throws Exception {
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
    }
    
    private void nextDataSet() {
    	labeledData.shuffle();
    	targetMiniBatchData = labeledData.sample(miniBatchSize);
    }
    
    private void normalize() {
        
        SplitTestAndTrain testAndTrain = targetMiniBatchData.splitTestAndTrain(0.65);  //Use 65% of data for training
        trainingData = testAndTrain.getTrain();
        testData = testAndTrain.getTest();

        DataNormalization normalizer = new NormalizerStandardize();
        normalizer.fit(trainingData);           //Collect the statistics (mean/stdev) from the training data. This does not modify the input data
        normalizer.transform(trainingData);     //Apply normalization to the training data
        normalizer.transform(testData);         //Apply normalization to the test data. This is using statistics calculated from the *training* set
    }
    
    final static int[] LAYER_NODES = { 760, 380 , 150, 15, 100 };
    
    
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
    
    private double getModelScore() {
    	return model.score();
    }
    
    public void saveModel() throws Exception {
    	ModelSerializer.writeModel(model, 
    			file.getTempFile(), 
    			true);
        file.flush();
    	
    }
    
}
