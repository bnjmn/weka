import weka.classifiers.trees.J48
import weka.core.converters.ConverterUtils.DataSource
import weka.core.Instances

/** 
 * An example of using J48 from within Groovy.
 * <p/>
 * First parameter is the dataset to be processed by J48. The last attribute
 * is used as class attribute.
 *
 * @author: FracPete (fracpete at waikato dot ac dot nz)
 */

if (args.size() == 0) {
  println "Usage: UsingJ48.groovy <ARFF-file>"
  System.exit(0)
}

// load data and set class index
data = DataSource.read(args[0])
data.setClassIndex(data.numAttributes() - 1)

// create the model
j48 = new J48()
j48.buildClassifier(data)

// print out the built model
println j48

