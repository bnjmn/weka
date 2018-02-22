import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.instance.RemoveMisclassified;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * Runs the RemoveMisclassified filter over a given ARFF file.
 * First parameter is the input file, the second the classifier
 * to use and the third one is the output file. <p/>
 *
 * Usage: RemoveMisclassifiedTest input.arff classname output.arff
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class RemoveMisclassifiedTest {
  public static void main(String[] args) throws Exception {
    if (args.length != 3) {
      System.out.println("\nUsage: RemoveMisclassifiedTest <input.arff> <classname> <output.arff>\n");
      System.exit(1);
    }

    // get data 
    Instances input = new Instances(
        new BufferedReader(new FileReader(args[0])));
    input.setClassIndex(input.numAttributes() - 1);

    // get classifier
    Classifier c = Classifier.forName(args[1], new String[0]);

    // setup and run filter
    RemoveMisclassified filter = new RemoveMisclassified();
    filter.setClassifier(c);
    filter.setClassIndex(-1);
    filter.setNumFolds(0);
    filter.setThreshold(0.1);
    filter.setMaxIterations(0);
    filter.setInputFormat(input);
    Instances output = Filter.useFilter(input, filter);
    
    // output file
    BufferedWriter writer = new BufferedWriter(new FileWriter(args[2]));
    writer.write(output.toString());
    writer.newLine();
    writer.flush();
    writer.close();
  }
}
