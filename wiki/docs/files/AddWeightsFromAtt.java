import weka.core.converters.ConverterUtils.DataSource;
import weka.core.converters.XRFFSaver;
import weka.core.Instances; 

import java.io.File;

/**
 * Loads file "args[0]", Adds weight given in attribute with
 * index "args[1]" - 1, deletes this attribute.
 * sets class if necessary (in that case the last 
 * attribute) and saves it as XRFF file
 * under "args[2]". E.g.: <br/>
 *   AddWeightsFromAtt file.arff 2 file.xrff.gz
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @author gabi (gs23 at waikato dot ac dot nz)
 */
public class AddWeightsFromAtt {
  public static void main(String[] args) throws Exception {
    // load data
    DataSource source = new DataSource(args[0]);
    Instances data = source.getDataSet(); 

    // get weight index
    int wIndex = Integer.parseInt(args[1]) - 1;
    
    // set weights
    for (int i = 0; i < data.numInstances(); i++) {
      double weight = data.instance(i).value(wIndex);
      data.instance(i).setWeight(weight);
    }

    // delete weight attribute and set class index
    data.deleteAttributeAt(wIndex);
    if (data.classIndex() = -1)
      data.setClassIndex(data.numAttributes() - 1);

    // save data
    XRFFSaver saver = new XRFFSaver();
    saver.setFile(new File(args[2]));
    saver.setInstances(data);
    saver.writeBatch();
  }
}

