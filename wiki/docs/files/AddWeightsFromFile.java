import weka.core.converters.ConverterUtils.DataSource;
import weka.core.converters.XRFFSaver;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

/**
 * Loads file "args[0]" (can be ARFF, CSV, C4.5, etc.), sets class if necessary
 * (in that case the last attribute), adds weights from "args[1]" (one weight
 * per line) and saves it as XRFF file under "args[2]". E.g.: <br/>
 *   AddWeightsFromFile anneal.arff weights.txt anneal.xrff.gz
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class AddWeightsFromFile {
  public static void main(String[] args) throws Exception {
    // load data
    DataSource source = new DataSource(args[0]);
    Instances data = source.getDataSet();
    if (data.classIndex() == -1)
      data.setClassIndex(data.numAttributes() - 1);

    // read and set weights
    BufferedReader reader = new BufferedReader(new FileReader(args[1]));
    for (int i = 0; i < data.numInstances(); i++) {
      String line = reader.readLine();
      double weight = Double.parseDouble(line);
      data.instance(i).setWeight(weight);
    }
    reader.close();

    // save data
    XRFFSaver saver = new XRFFSaver();
    saver.setFile(new File(args[2]));
    saver.setInstances(data);
    saver.writeBatch();
  }
}
