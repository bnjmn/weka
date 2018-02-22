import weka.core.Attribute;
import weka.core.Instances;
import weka.core.converters.ArffSaver;
import weka.filters.unsupervised.attribute.MergeTwoValues;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Enumeration;

/**
 * Generates binary ARFF files out of a nominal one, i.e., it takes the
 * input ARFF file and creates for all attribute values, files that contain
 * the "value" and "not_value" as values then. E.g., in case of the
 * weather data in generates three files for the "outlook" attribute:
 * - weather_sunny.arff   : sunny, not_sunny
 * - weather_overcast.arff: overcast, not_overcast
 * - weather_rainy.arff   : rainy, not_rainy
 */
public class Binarize {
  /**
   * takes 2 arguments:
   * - the input ARFF file
   * - the attribute index (starting with 1)
   */
  public static void main(String[] args) throws Exception {
    Instances       input;
    Instances       output;
    ArffSaver       saver;
    int             i;
    Enumeration     enm;
    String          currValue;
    String          value;
    int             attIndex;
    String          filename;
    int             renamed;
    MergeTwoValues  merge;
    int             index;

    // input file provided
    if (args.length != 2) {
      System.out.println(
          "\nUsage: " + Binarize.class.getClass().getName() 
                      + " <input> <attribute-index>\n");
      System.exit(1);
    }

    // load input
    input = new Instances(new BufferedReader(new FileReader(args[0])));
    input.setClassIndex(input.numAttributes() - 1);
    
    // generate output_files
    attIndex = Integer.parseInt(args[1]) - 1;
    for (i = 0; i < input.attribute(attIndex).numValues(); i++) {
      output    = new Instances(input);
      currValue = input.attribute(attIndex).value(i);

      // rename values
      enm     = input.attribute(attIndex).enumerateValues();
      renamed = -1;
      while (enm.hasMoreElements()) {
        value = enm.nextElement().toString();
        if (!value.equals(currValue)) {
          index = output.attribute(attIndex).indexOfValue(value);
          // rename the first not-value, others are merged with this one then
          if (renamed == -1) {
            renamed = index;
            output.renameAttributeValue(
                output.attribute(attIndex), value, "not_" + currValue);
          }
          else {
            merge = new MergeTwoValues();
            merge.setAttributeIndex(args[1]);
            merge.setFirstValueIndex("" + (renamed + 1));
            merge.setSecondValueIndex("" + (index + 1));
            merge.setInputFormat(output);
            output = MergeTwoValues.useFilter(output, merge);
            // rename value (since merge creates combined name)
            output.renameAttributeValue(
                output.attribute(attIndex), 
                "not_" + currValue + "_" + value, 
                "not_" + currValue);
          }
        }
      }

      // save file
      output.setRelationName(
          input.relationName() + "-" + currValue + "-and-not_" + currValue);
      filename = args[0].replaceAll(
                    ".[Aa][Rr][Ff][Ff]$", "-" + currValue + ".arff");
      saver = new ArffSaver();
      saver.setInstances(output);
      saver.setFile(new File(filename));
      saver.setDestination(new File(filename));
      saver.writeBatch();
    }
  }
}
