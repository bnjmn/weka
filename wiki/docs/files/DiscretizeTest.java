import java.io.*;

import weka.core.*;
import weka.filters.Filter;
import weka.filters.supervised.attribute.Discretize;

/**
 * Shows how to generate compatible train/test sets using the Discretize
 * filter.
 *
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class DiscretizeTest {

  /**
   * loads the given ARFF file and sets the class attribute as the last
   * attribute.
   *
   * @param filename    the file to load
   * @throws Exception  if somethings goes wrong
   */
  protected static Instances load(String filename) throws Exception {
    Instances       result;
    BufferedReader  reader;

    reader = new BufferedReader(new FileReader(filename));
    result = new Instances(reader);
    result.setClassIndex(result.numAttributes() - 1);
    reader.close();

    return result;
  }

  /**
   * saves the data to the specified file
   *
   * @param data        the data to save to a file
   * @param filename    the file to save the data to
   * @throws Exception  if something goes wrong
   */
  protected static void save(Instances data, String filename) throws Exception {
    BufferedWriter  writer;

    writer = new BufferedWriter(new FileWriter(filename));
    writer.write(data.toString());
    writer.newLine();
    writer.flush();
    writer.close();
  }
  
  /**
   * Takes four arguments:
   * <ol>
   *   <li>input train file</li>
   *   <li>input test file</li>
   *   <li>output train file</li>
   *   <li>output test file</li>
   * </ol>
   *
   * @param args        the commandline arguments
   * @throws Exception  if something goes wrong
   */
  public static void main(String[] args) throws Exception {
    Instances     inputTrain;
    Instances     inputTest;
    Instances     outputTrain;
    Instances     outputTest;
    Discretize    filter;
    
    // load data (class attribute is assumed to be last attribute)
    inputTrain = load(args[0]);
    inputTest  = load(args[1]);

    // setup filter
    filter = new Discretize();
    filter.setInputFormat(inputTrain);

    // apply filter
    outputTrain = Filter.useFilter(inputTrain, filter);
    outputTest  = Filter.useFilter(inputTest,  filter);

    // save output
    save(outputTrain, args[2]);
    save(outputTest,  args[3]);
  }
}
