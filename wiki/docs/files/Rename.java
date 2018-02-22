import weka.core.Attribute;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Vector;

/**
 * renames all the labels of nominal attributes to numbers, they way they
 * appear, e.g., attribute a1 has the labels "what", "so" and "ever" are
 * renamed to "0", "1" and "2".
 *
 * @author FracPete
 */
public class Rename {
  /**
   * takes two parameters as input:
   * 1. input arff file
   * 2. output arff file (transformed)
   * 3. (optional) the attribute index (1-based), otherwise all attributes
   *    except class are changed
   * Assumption: last attribute is class attribute
   */
  public static void main(String[] args) throws Exception {
    if (args.length < 2)
      throw new Exception("Needs at least input and output ARFF file!");

    // read arff file
    Instances arff = new Instances(new BufferedReader(new FileReader(args[0])));
    arff.setClassIndex(arff.numAttributes() - 1);

    // determine attributes to modify
    Integer[] indices = null;
    if (args.length > 2) {
      indices    = new Integer[1];
      indices[0] = new Integer(Integer.parseInt(args[2]) - 1);
    }
    else {
      Vector v = new Vector();
      for (int i = 0; i < arff.numAttributes() - 2; i++) {
        if (arff.attribute(i).isNominal())
          v.add(new Integer(i));
      }
      indices = (Integer[]) v.toArray(new Integer[v.size()]);
    }
    
    // rename labels of all nominal attributes
    for (int i = 0; i < indices.length; i++) {
      int attInd = indices[i].intValue();
      Attribute att = arff.attribute(attInd);
      for (int n = 0; n < att.numValues(); n++) {
        arff.renameAttributeValue(att, att.value(n), "" + n);
      }
    }

    // save arff file
    BufferedWriter writer = new BufferedWriter(new FileWriter(args[1]));
    writer.write(arff.toString());
    writer.newLine();
    writer.flush();
    writer.close();
  }
}
