/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * LED24.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.datagenerators.classifiers.classification;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.Utils;
import weka.datagenerators.DataGenerator;
import weka.datagenerators.ClassificationGenerator;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * This generator produces data for a display with 7 LEDs. The original output
 * consists of 10 concepts and 7 boolean attributes. Here, in addition to the 7
 * necessary boolean attributes, 17 other, irrelevant boolean attributes with
 * random values are added to make it harder. By default 10 percent of noise
 * are added to the data. <p/>
 *
 * More information can be found here: <br/>
 * Breiman,L., Friedman,J.H., Olshen,R.A., &amp; Stone,C.J. (1984).
 * Classification and Regression Trees.  Wadsworth International Group:
 * Belmont, California.  (see pages 43-49). <p/>
 *
 * Link: <br/>
 * <a href="http://www.ics.uci.edu/~mlearn/databases/led-display-creator/">http://www.ics.uci.edu/~mlearn/databases/led-display-creator/</a> <p/>
 * 
 *
 * Valid options are: <p/>
 *
 * -d <br/>
 *  enables debugging information to be output on the console. <p/>
 *
 * -r string <br/>
 *  Name of the relation of the generated dataset. <p/>
 *
 * -o filename<br/>
 *  writes the generated dataset to the given file using ARFF-Format.
 *  (default = stdout). <p/>
 *
 * -n num <br/>
 *  Number of examples. <p/>
 * 
 * -S num <br/>
 *  the seed value for initializing the random number generator.
 *  <p/>
 *
 * -N num <br/>
 *  the noise percentage. <p/>
 *
 * @author Richard Kirkby (rkirkby at cs dot waikato dot ac dot nz)
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision: 1.2 $
 */

public class LED24
  extends ClassificationGenerator {
  
  /** the noise rate */
  protected double m_NoisePercent;
  
  protected static final int m_originalInstances[][] = {
    { 1, 1, 1, 0, 1, 1, 1 }, { 0, 0, 1, 0, 0, 1, 0 },
    { 1, 0, 1, 1, 1, 0, 1 }, { 1, 0, 1, 1, 0, 1, 1 },
    { 0, 1, 1, 1, 0, 1, 0 }, { 1, 1, 0, 1, 0, 1, 1 },
    { 1, 1, 0, 1, 1, 1, 1 }, { 1, 0, 1, 0, 0, 1, 0 },
    { 1, 1, 1, 1, 1, 1, 1 }, { 1, 1, 1, 1, 0, 1, 1 } };

  protected int m_numIrrelevantAttributes = 17;

  /**
   * initializes the generator with default values
   */
  public LED24() {
    super();

    setNoisePercent(defaultNoisePercent());
  }

  /**
   * Returns a string describing this data generator.
   *
   * @return a description of the data generator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return 
         "This generator produces data for a display with 7 LEDs. The original "
       + "output consists of 10 concepts and 7 boolean attributes. Here, in "
       + "addition to the 7 necessary boolean attributes, 17 other, irrelevant "
       + "boolean attributes with random values are added to make it harder. "
       + "By default 10 percent of noise are added to the data.\n"
       + "\n"
       + "More information can be found here:\n"
       + "Breiman,L., Friedman,J.H., Olshen,R.A., & Stone,C.J. (1984). "
       + "Classification and Regression Trees. Wadsworth International Group: "
       + "Belmont, California. (see pages 43-49).\n"
       + "\n"
       + "Link:\n"
       + "http://www.ics.uci.edu/~mlearn/databases/led-display-creator/";
  }

 /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector result = enumToVector(super.listOptions());

    result.add(new Option(
              "\tThe noise percentage. (default " 
              + defaultNoisePercent() + ")",
              "N", 1, "-N <num>"));

    return result.elements();
  }

  /**
   * Parses a list of options for this object. <p/>
   *
   * For list of valid options see class description.<p/>
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String        tmpStr;

    super.setOptions(options);

    tmpStr = Utils.getOption('N', options);
    if (tmpStr.length() != 0)
      setNoisePercent(Double.parseDouble(tmpStr));
    else
      setNoisePercent(defaultNoisePercent());
  }

  /**
   * Gets the current settings of the datagenerator.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    Vector        result;
    String[]      options;
    int           i;
    
    result  = new Vector();
    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);
    
    result.add("-N");
    result.add("" + getNoisePercent());
    
    return (String[]) result.toArray(new String[result.size()]);
  }

  /**
   * returns the default noise percentage
   */
  protected double defaultNoisePercent() {
    return 10;
  }
  
  /**
   * Gets the noise percentage.
   *
   * @return the noise percentage.
   */
  public double getNoisePercent() { 
    return m_NoisePercent; 
  }
  
  /**
   * Sets the noise percentage.
   *
   * @param value the noise percentage.
   */
  public void setNoisePercent(double value) { 
    if ( (value >= 0.0) && (value <= 100.0) )
      m_NoisePercent = value;
    else
      throw new IllegalArgumentException(
          "Noise percent must be in [0,100] (provided: " + value + ")!");
  }  
  
  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for
   *         displaying in the explorer/experimenter gui
   */
  public String noisePercentTipText() {
    return "The noise percent: 0 <= perc <= 100.";
  }

  /**
   * Return if single mode is set for the given data generator
   * mode depends on option setting and or generator type.
   * 
   * @return single mode flag
   * @throws Exception if mode is not set yet
   */
  public boolean getSingleModeFlag() throws Exception {
    return true;
  }

  /**
   * Initializes the format for the dataset produced. 
   * Must be called before the generateExample or generateExamples
   * methods are used.
   * Re-initializes the random number generator with the given seed.
   *
   * @return the format for the dataset 
   * @throws Exception if the generating of the format failed
   * @see  #getSeed()
   */
  public Instances defineDataFormat() throws Exception {
    FastVector      atts;
    FastVector      attValues;
    int             i;
    int             n;

    m_Random = new Random(getSeed());

    // number of examples is the same as given per option
    setNumExamplesAct(getNumExamples());

    // set up attributes
    atts = new FastVector();
    
    for (n = 1; n <= 24; n++) {
      attValues = new FastVector();
      for (i = 0; i < 2; i++)
        attValues.addElement("" + i);
      atts.addElement(new Attribute("att" + n, attValues));
    }
    
    attValues = new FastVector();
    for (i = 0; i < 10; i++)
      attValues.addElement("" + i);
    atts.addElement(new Attribute("class", attValues));
    
    // dataset
    m_DatasetFormat = new Instances(getRelationNameToUse(), atts, 0);
    
    return m_DatasetFormat;
  }

  /**
   * Generates one example of the dataset. 
   *
   * @return the generated example
   * @throws Exception if the format of the dataset is not yet defined
   * @throws Exception if the generator only works with generateExamples
   * which means in non single mode
   */
  public Instance generateExample() throws Exception {
    Instance      result;
    double[]      atts;
    int           i;
    int           selected;
    Random        random;

    result = null;
    random = getRandom();

    if (m_DatasetFormat == null)
      throw new Exception("Dataset format not defined.");

    atts     = new double[m_DatasetFormat.numAttributes()];
    selected = random.nextInt(10);
    for (i = 0; i < 7; i++) {
      if ((1 + (random.nextInt(100))) <= getNoisePercent())
        atts[i] = m_originalInstances[selected][i] == 0 ? 1 : 0;
      else
        atts[i] = m_originalInstances[selected][i];
    }

    for (i = 0; i < m_numIrrelevantAttributes; i++)
      atts[i + 7] = random.nextInt(2);

    atts[atts.length - 1] = selected;

    // create instance
    result  = new Instance(1.0, atts);
    result.setDataset(m_DatasetFormat);

    return result;
  }

  /**
   * Generates all examples of the dataset. Re-initializes the random number
   * generator with the given seed, before generating instances.
   *
   * @return the generated dataset
   * @throws Exception if the format of the dataset is not yet defined
   * @throws Exception if the generator only works with generateExample,
   * which means in single mode
   * @see   #getSeed()
   */
  public Instances generateExamples() throws Exception {
    Instances       result;
    int             i;

    result   = new Instances(m_DatasetFormat, 0);
    m_Random = new Random(getSeed());

    for (i = 0; i < getNumExamplesAct(); i++)
      result.add(generateExample());
    
    return result;
  }

  /**
   * Generates a comment string that documentates the data generator.
   * By default this string is added at the beginning of the produced output
   * as ARFF file type, next after the options.
   * 
   * @return string contains info about the generated rules
   * @throws Exception if the generating of the documentation fails
   */
  public String generateStart () {
    return "";
  }

  /**
   * Generates a comment string that documentats the data generator.
   * By default this string is added at the end of theproduces output
   * as ARFF file type.
   * 
   * @return string contains info about the generated rules
   * @throws Exception if the generating of the documentaion fails
   */
  public String generateFinished() throws Exception {
    return "";
  }

  /**
   * Main method for executing this class.
   *
   * @param args should contain arguments for the data producer: 
   */
  public static void main(String[] args) {
    try {
      DataGenerator.makeData(new LED24(), args);
    } 
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}
