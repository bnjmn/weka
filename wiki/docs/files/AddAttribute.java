import weka.core.*;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Add;

import java.io.*;
import java.util.*;

/**
 * Adds a nominal and a numeric attribute to the dataset provided as first
 * parameter (and fills it with random values) and outputs the result to
 * stdout. It's either done via the Add filter (second option "filter") 
 * or manual with Java (second option "java").<p/>
 *
 * Usage: AddAttribute &lt;file.arff&gt; &lt;filter|java&gt; 
 *
 * @author FracPete (fracpete at waikato dot ac dot nz)
 */
public class AddAttribute {
  /**
   * adds the attributes
   *
   * @param args    the commandline arguments
   */
  public static void main(String[] args) throws Exception {
    if (args.length != 2) {
      System.out.println("\nUsage: <file.arff> <filter|java>\n");
      System.exit(1);
    }

    // load dataset
    Instances data = new Instances(new BufferedReader(new FileReader(args[0])));
    Instances newData = null;

    // filter or java?
    if (args[1].equals("filter")) {
      Add filter;
      newData = new Instances(data);
      // 1. nominal attribute
      filter = new Add();
      filter.setAttributeIndex("last");
      filter.setNominalLabels("A,B,C,D");
      filter.setAttributeName("NewNominal");
      filter.setInputFormat(newData);
      newData = Filter.useFilter(newData, filter);
      // 2. numeric attribute
      filter = new Add();
      filter.setAttributeIndex("last");
      filter.setAttributeName("NewNumeric");
      filter.setInputFormat(newData);
      newData = Filter.useFilter(newData, filter);
    }
    else if (args[1].equals("java")) {
      newData = new Instances(data);
      // add new attributes
      // 1. nominal
      FastVector values = new FastVector();
      values.addElement("A");
      values.addElement("B");
      values.addElement("C");
      values.addElement("D");
      newData.insertAttributeAt(new Attribute("NewNominal", values), newData.numAttributes());
      // 2. numeric
      newData.insertAttributeAt(new Attribute("NewNumeric"), newData.numAttributes());
    }
    else {
      System.out.println("\nUsage: <file.arff> <filter|java>\n");
      System.exit(2);
    }

    // random values
    Random rand = new Random(1);
    for (int i = 0; i < newData.numInstances(); i++) {
      // 1. nominal
      newData.instance(i).setValue(newData.numAttributes() - 2, rand.nextInt(4));  // index of labels A:0,B:1,C:2,D:3
      // 2. numeric
      newData.instance(i).setValue(newData.numAttributes() - 1, rand.nextDouble());
    }

    // output on stdout
    System.out.println(newData);
  }
}
