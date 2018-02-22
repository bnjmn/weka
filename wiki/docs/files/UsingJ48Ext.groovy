import weka.classifiers.Evaluation
import weka.classifiers.trees.J48
import weka.core.converters.ConverterUtils.DataSource
import weka.core.Instances
import weka.core.Range

/** 
 * An example of using J48 from within Groovy.
 * <p/>
 * First parameter is the dataset to be processed by J48. The last attribute
 * is used as class attribute.
 *
 * @author: FracPete (fracpete at waikato dot ac dot nz)
 */

if (args.size() == 0) {
  println "Usage: UsingJ48Ext.groovy <ARFF-file>"
  System.exit(0)
}

// load data and set class index
print "Loading data..."
data = DataSource.read(args[0])
data.setClassIndex(data.numAttributes() - 1)

// create the model
evaluation = new Evaluation(data)
buffer = new StringBuffer()  // buffer for predictions
attRange = new Range()  // no additional attributes to output
outputDistribution = false  // we don't want distribution
j48 = new J48()
j48.buildClassifier(data)
evaluation.evaluateModel(j48, data, buffer, attRange, outputDistribution)

// print out the built model
println "--> Generated model:\n"
println j48

println "--> Evaluation:\n"
println evaluation.toSummaryString()

println "--> Predictions:\n"
println buffer

