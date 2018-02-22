import weka.classifiers.Evaluation;
import weka.classifiers.functions.LibSVM
import weka.core.converters.ConverterUtils.DataSource

import java.util.Random

/** 
 * Determines the best class-weights for LibSVM from a list of weight strings. 
 * <p/>
 * Just runs LibSVM on a binary dataset and chooses the class-weights
 * with the best accuracy.
 * <p/>
 * Note: The Groovy and libsvm classes must be present in the classpath to run 
 *       this script.
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */

// dataset provided?
if (args.size() == 0) {
  println "Usage: LibsvmWeights.groovy <ARFF-file>"
  return;
}

// load data
data = DataSource.read(args[0])
if (data.classIndex() == -1) data.setClassIndex(data.numAttributes() - 1)
if (data.classAttribute().numValues() != 2) {
  println "Dataset needs a binary class!"
  return;
}

// evaluate weights
def weights = ["1.0 1.0", "1.0 0.7", "1.0 0.4"]
def evals = []
for (weight in weights) {
  println "\n\n--> Processing weights: " + weight
  cls = new LibSVM();
  cls.setWeights(weight)
  eval = new Evaluation(data);
  eval.crossValidateModel(cls, data, 10, new Random(1));
  evals.add(eval)
}

// find best accuracy
index = 0
best  = evals[index].pctCorrect()
for (i = 1; i < weights.size(); i++) {
  if (evals[i].pctCorrect() > best) {
    index = 0
    best  = evals[i].pctCorrect()
  }
}
println "\n\nBest accuracy was found for weights: " + weights[index]
