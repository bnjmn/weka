JAVAC = javac

SOURCES = AbstractDataSource.java AbstractDataSourceBeanInfo.java \
	AbstractEvaluator.java AbstractTestSetProducer.java \
	AbstractTestSetProducerBeanInfo.java \
	AbstractTrainAndTestSetProducer.java \
	AbstractTrainAndTestSetProducerBeanInfo.java \
	AbstractTrainingSetProducer.java \
	AbstractTrainingSetProducerBeanInfo.java \
	BatchClassifierEvent.java BatchClassifierListener.java \
	BeanCommon.java BeanConnection.java BeanInstance.java BeanVisual.java \
	ChartEvent.java ChartListener.java ClassAssigner.java \
	ClassAssignerBeanInfo.java ClassAssignerCustomizer.java \
	Classifier.java ClassifierBeanInfo.java ClassifierCustomizer.java \
	ClassifierPerformanceEvaluator.java \
	ClassifierPerformanceEvaluatorBeanInfo.java \
	CrossValidationFoldMaker.java CrossValidationFoldMakerBeanInfo.java \
	CrossValidationFoldMakerCustomizer.java DataSetEvent.java \
	DataSource.java DataSourceListener.java DataVisualizer.java \
	DataVisualizerBeanInfo.java EventConstraints.java Filter.java \
	FilterBeanInfo.java FilterCustomizer.java GraphEvent.java \
	GraphListener.java GraphViewer.java GraphViewerBeanInfo.java \
	IncrementalClassifierEvaluator.java \
	IncrementalClassifierEvaluatorBeanInfo.java \
	IncrementalClassifierEvent.java IncrementalClassifierListener.java \
	InstanceEvent.java InstanceListener.java KnowledgeFlow.java \
	Loader.java LoaderBeanInfo.java LoaderCustomizer.java \
	StripChart.java StripChartBeanInfo.java StripChartCustomizer.java \
	TestSetEvent.java TestSetListener.java TestSetMaker.java \
	TestSetMakerBeanInfo.java TestSetProducer.java TextEvent.java \
	TextListener.java TextViewer.java TextViewerBeanInfo.java \
	TrainTestSplitMaker.java TrainTestSplitMakerBeanInfo.java \
	TrainTestSplitMakerCustomizer.java TrainingSetEvent.java \
	TrainingSetListener.java TrainingSetMaker.java \
	TrainingSetMakerBeanInfo.java TrainingSetProducer.java \
	UserRequestAcceptor.java Visible.java WekaWrapper.java \
	AttributeSummarizer.java AttributeSummarizerBeanInfo.java \
	ScatterPlotMatrix.java ScatterPlotMatrixBeanInfo.java

CLASSFILES = $(SOURCES:.java=.class)

%.class : %.java
	$(JAVAC) $< 

.PHONY: all debug optimized clean

#----------------------------------------------------------------------------#

all :	$(CLASSFILES)

debug : clean
	$(JAVAC) -g $(SOURCES)

optimized : clean
	$(JAVAC) -O $(SOURCES)

clean : 
	$(RM) *.class *~
