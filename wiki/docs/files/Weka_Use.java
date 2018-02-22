package main;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.*;
//import weka.classifiers.trees.J48;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;

/**
 * <b>Weka_Use is the class to use methods provide by the librairy Weka.</b>
 * <p>
 * Weka_Use uses different methods of Weka_ManageInstances to filter the extracted data.
 * </p>
 */
public class Weka_Use
{	
	/*
	public final static void main(String[] args) throws Exception
	{
		test();
	}
	*/
	
	/**
	 * Method to use Weka_Use.
	 * 
	 * @throws Exception
	 */	
	public final static void test () throws Exception
	{	
		DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		Calendar cal = Calendar.getInstance();
		System.out.println("\t["+ dateFormat.format(cal.getTime()) +"]");
		
			/* 1. Load Data */
		String filename   = "1mn.arff";
		Instances allData = new DataSource(filename).getDataSet();
		
			/* 2. Build Instances */
		double percent = 0.8;
		String attributes_filter = "1-30,28-60,63-68,62,70";

		Instances testInstances		= buildInstancesP(allData, attributes_filter, true, percent, 1);		
		Instances learningInstances = buildInstancesP(allData, attributes_filter, true, 0, percent);
		
			/* 3. Evaluation */
		Classifier classifier 	= learning (learningInstances);
		Evaluation eval			= evaluation(learningInstances, testInstances, classifier, true);
		
		System.out.println(eval.errorRate());
		
		dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
		cal = Calendar.getInstance();
		System.out.println("\t["+ dateFormat.format(cal.getTime()) +"]");
	}
	
	/**
	 * Used to build an Instances from some Percents of an other one.
	 * It's possible to add a filter on the attributes and choose if the new Instances got or not
	 * only different following rows.
	 * 
	 * @param data
	 * 				The Instances to extract the new Instances. 
	 * @param attributes_filter
	 * 				String which represents the attributes to remove.
	 * 				<i>"1-4"</i>  | <i>"28"</i>  | <i>"1-70,45,68-72"</i> | <i>""</i> | <i>...</i>
	 * @param differentNext
	 * 				Only different following rows or not?
	 * @param percent_start
	 * 				Percent indicating the first line of the selection.
	 * @param percent_end
	 * 				Percent indicating the last line of the selection.
	 * 				
	 * @return The new extracted and filtered Instances.
	 * 
	 * @throws Exception
	 */
	public static Instances buildInstancesP (Instances data, String attributes_filter, boolean differentNext, double percent_start, double percent_end) throws Exception
	{		
			/* Security (on percent_start and percent_end) */
		percent_end 	= Math.max(percent_end, 0);
		percent_start 	= Math.max(percent_start, 0);
		percent_end 	= Math.min(percent_end, 1);
		percent_start	= Math.min(percent_start, 1);
		if (percent_end < percent_start){
			double temp = percent_start;
			percent_start = percent_end;
			percent_end = temp;
		}
			/* Rows selection */
		Instances instances = Weka_ManageInstances.percentSelection(data, percent_start, percent_end);
			/* Attributes Filter */
		if (attributes_filter.length()>0) instances = Weka_ManageInstances.attributSelection(instances, attributes_filter);
			/* Set class attributes. */
		if (instances.classIndex() == -1)
			instances.setClassIndex(instances.numAttributes() - 1);
			/* Delete rows whose the next is same. */
		if(differentNext) instances = Weka_ManageInstances.differentNextSelection(instances);
		
		return(instances);
	}
	
	/**
	 * Used to build an Instances extracted between two row Numbers of an other one.
	 * It's possible to add a filter on the attributes and choose if the new Instances got or not
	 * only different following rows.
	 * 
	 * @param data
	 * 				The Instances to extract the new Instances.
	 * @param attributes_filter
	 * 				String which represents the attributes to remove.
	 * 				<i>"1-4"</i>  | <i>"28"</i>  | <i>"1-70,45,68-72"</i> | <i>""</i> | <i>...</i>
	 * @param differentNext
	 * 				Only different following rows or not?
	 * @param num_start
	 * 				Line number indicating the first line of the selection.
	 * @param num_end
	 * 				Line number indicating the last line of the selection.
	 * 				
	 * @return The new extracted and filtered Instances.
	 * 
	 * @throws Exception
	 */
	public static Instances buildInstancesN (Instances data, String attributes_filter, boolean differentNext, int num_start, int num_end) throws Exception
	{		
			/* Security (on num_end and num_start) */
		num_end 	= Math.max(num_end, 0);
		num_start 	= Math.max(num_start, 0);
		num_end		= Math.min(num_end, data.numInstances());
		num_start	= Math.min(num_start, data.numInstances());
		if (num_end < num_start){
			int temp = num_start;
			num_start = num_end;
			num_end = temp;
		}
			/* Rows selection */
		Instances learningInstances = Weka_ManageInstances.rowNumberSelection(data, num_start, num_end);
			/* Attributes Filter */
		if (attributes_filter.length()>0) learningInstances = Weka_ManageInstances.attributSelection(learningInstances, attributes_filter);
			/* Set class attributes */
		if (learningInstances.classIndex() == -1)
			learningInstances.setClassIndex(learningInstances.numAttributes() - 1);
			/* Delete rows whose the next is same. */
		if(differentNext) learningInstances = Weka_ManageInstances.differentNextSelection(learningInstances);
		
		return(learningInstances);
	}
	
	/**
	 * Used to train a Classifier.
	 * (In comments, the some other methods to learn).
	 * 
	 * @param learningInstances
	 * 				Instances used to build the Classifier.
	 * @return
	 * 			The trained Classifier.
	 * 
	 * @throws Exception
	 */
	public static Classifier learning (Instances learningInstances) throws Exception
	{		
			/* Methods to learn */
		Classifier classifier = new MultilayerPerceptron();
		//Classifier classifier = new J48();
		// Classifier classifier = new PaceRegression();
		// Classifier classifier = new GaussianProcesses();
		// Classifier classifier = new IsotonicRegression();
		// Classifier classifier = new RBFNetwork();
		// Classifier classifier = new SimpleLinearRegression();
		// Classifier classifier = new LinearRegression();
		// Classifier classifier = new SMOreg();
		// Classifier classifier = new LeastMedSq();

		classifier.buildClassifier(learningInstances);

		return (classifier);
	}
	
	/**
	 * Used to evaluate an Instances of test and display it.
	 * 
	 * @param learningInstances
	 * 				The Instances used to build the Classifier.
	 * @param testInstances
	 * 				The Instances of test data.
	 * @param classifier
	 * 				The Classifier trained by the learning Instances.
	 * 
	 * @return The Evaluation of the test data.
	 * 
	 * @throws Exception
	 */
	public static Evaluation evaluation (Instances learningInstances, Instances testInstances, Classifier classifier, boolean displayResults) throws Exception
	{
		for (int i = 0; i < testInstances.numInstances() && displayResults; i++)
		{
			Instance instance = testInstances.instance(i);
			System.out.println(instance + " => "
			+ classifier.classifyInstance(instance));
		}
				
		Evaluation eval = new Evaluation(learningInstances);
		eval.evaluateModel(classifier, testInstances);
		
		if (displayResults) System.out.println(eval.toSummaryString("\nResults\n======\n", true));
		
		return(eval);
	}
}