/*
 *    BIRCHCluster.java
 *    Copyright (C) 2001 Gabi Schmidberger.
 *
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

package weka.datagenerators;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Option;
import weka.core.Utils;

import java.io.Serializable;

import java.util.Random;
import java.util.Enumeration;
import java.util.Vector;

/**
 * Cluster data generator designed for the BIRCH System
 *
 * Dataset is generated with instances in K clusters.
 * Instances are 2-d data points.
 * Each cluster is characterized by the number of data points in it
 * its radius and its center. The location of the cluster centers is
 * determined by the pattern parameter. Three patterns are currently
 * supported grid, sine and random.
 * todo:
 *
 * (out of: BIRCH: An Efficient Data Clustering Method for Very Large
 * Databases; T. Zhang, R. Ramkrishnan, M. Livny; 1996 ACM)
 *
 * 
 * Class to generate data randomly by producing a decision list.
 * The decision list consists of rules.
 * Instances are generated randomly one by one. If decision list fails
 * to classify the current instance, a new rule according to this current
 * instance is generated and added to the decision list.<p>
 *
 * The option -V switches on voting, which means that at the end
 * of the generation all instances are
 * reclassified to the class value that is supported by the most rules.<p>
 *
 * This data generator can generate 'boolean' attributes (= nominal with
 * the values {true, false}) and numeric attributes. The rules can be
 * 'A' or 'NOT A' for boolean values and 'B < random_value' or
 * 'B >= random_value' for numeric values.<p> 
 *
 * Valid options are:<p>
 *
 * -G <br>
 * The pattern for instance generation is grid.<br>
 * This flag cannot be used at the same time as flag I.
 * The pattern is random, if neither flag G nor flag I is set.<p>
 *
 * -I <br>
 * The pattern for instance generation is sine.<br>
 * This flag cannot be used at the same time as flag G.
 * The pattern is random, if neither flag G nor flag I is set.<p>
 *
 * -N num .. num <br>
 * The range of the number of instances in each cluster (default 1..50).<br>
 * Lower number must be between 0 and 2500, upper number must be between
 * 50 and 2500.<p>
 *
 * -R num .. num <br>
 * The range of the radius of the clusters (default 0.1 .. SQRT(2)).<br>
 * Lower number must be between 0 and SQRT(2), upper number must be between<br>
 * SQRT(2) and SQRT(32).<p>
 *
 * -M num <br>
 * Distance multiplier, only used if pattern is grid (default 4). <p>
 *
 * -C num <br>
 * Number of cycles, only used if pattern is sine (default 4). <p>
 *
 * -O <br>
 * Flag for input order is ordered. If flag is not set then input
 * order is randomized.<p>
 *
 * -P num<br>
 * Noise rate in percent. Can be between 0% and 30% (default 0%).<br>
 * (Remark: The original algorithm only allows noise up to 10%.)<p>
 *
 * -S seed <br>
 * Random number seed for random function used (default 1). <p>
 *
 * @author Gabi Schmidberger (gabi@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $ 
 **/
public class BIRCHCluster extends ClusterGenerator
  implements OptionHandler,
	     Serializable {


  /**@serial minimal number of instances per cluster (option N)*/ 
  private int m_MinInstNum = 1;
  
  /**@serial maximal number of instances per cluster (option N)*/ 
  private int m_MaxInstNum = 50;
  
  /**@serial minimum radius (option R)*/ 
  private double m_MinRadius= 0.1;
  
  /**@serial maximum radius (option R)*/ 
  private double m_MaxRadius = Math.sqrt(2.0);
  
  /**@serial  Constant set for choice of pattern. (option G)*/
  public static final int GRID = 0;
  /**@serial  Constant set for choice of pattern. (option I)*/
  public static final int SINE = 1;
  /**@serial  Constant set for choice of pattern. (default)*/
  public static final int RANDOM = 2;
  
  /**@serial pattern (changed with options G or S)*/ 
  private int m_Pattern = RANDOM;
  
  /**@serial distance multiplier (option M)*/
  private double m_DistMult = 4.0;

  /**@serial number of cycles (option C)*/
  private int m_NumCycles = 4;

  /**@serial  Constant set for input order (option O)*/
  public static final int ORDERED = 0;
  /**@serial  Constant set for input order (default)*/
  public static final int RANDOMIZED = 1;

  /**@serial input order (changed with option O)*/ 
  private int m_InputOrder = RANDOMIZED;

  /**@serial noise rate in percent (option P,  between 0 and 30)*/ 
  private double m_NoiseRate = 0.0;

  /**@serial random number generator seed (option S)*/ 
  private int m_Seed = 1;
 
  /**@serial dataset format*/ 
  private Instances m_DatasetFormat = null;

  /**@serial random number generator*/ 
  private Random m_Random = null;

  /**@serial debug flag*/ 
  private int m_Debug = 0;

  /**@serial cluster list */
  private FastVector m_ClusterList;

  // following are used for pattern is GRID
  /**@serial grid size*/
  private int m_GridSize;

  /**@serial grid width*/
  private double m_GridWidth;
  
  /**********************************************************************
   * class to represent cluster
   */
  private class Cluster implements Serializable {

    // number of instances for this cluster
    private int m_InstNum;

    // radius of cluster
    //   variance is radius ** 2 / 2
    private double m_Radius;

    // center of cluster = array of Double values
    private double [] m_Center;

    /*
     * Constructor, used for pattern = RANDOM
     *
     * @param instNum the number of instances
     * @param radius radius of the cluster
     * @param center 
     */
    private Cluster(int instNum, double radius, Random random) {
      m_InstNum = instNum;
      m_Radius = radius;
      m_Center = new double[m_NumAttributes];
      for (int i = 0; i < m_NumAttributes; i++) {
	m_Center[i] = random.nextDouble() * (double) m_NumClusters;
      }
    }

    /*
     * Constructor, used for pattern = GRID
     *
     * @param instNum the number of instances
     * @param radius radius of the cluster
     * @param gridVector vector for grid positions
     * @param gridWidth factor for grid position
     */
      // center is defined in the constructor of cluster
    private Cluster(int instNum,
		    double radius,
		    int [] gridVector,
		    double gridWidth) {
      m_InstNum = instNum;
      m_Radius = radius;
      m_Center = new double[m_NumAttributes];
      for (int i = 0; i < m_NumAttributes; i++) {
	m_Center[i] = ((double) gridVector[i] + 1.0) * gridWidth;
      }
      
    }
   
    private int getInstNum () { return m_InstNum; }
    private double getRadius () { return m_Radius; }
    private double getVariance () { return Math.pow(m_Radius, 2.0) / 2.0; }
    private double getStdDev () { return (m_Radius / Math.pow(2.0, 0.5)); }
    private double [] getCenter () { return m_Center; }
    private double getCenterValue (int dimension) throws Exception {
      if (dimension >= m_Center.length)
	throw new Exception("Current system has only " +
			    m_Center.length + " dimensions.");
      return m_Center[dimension];
    }
    
  } // end class Cluster

  /**********************************************************************
   * class to represent Vector for placement of the center in space
   */
  private class GridVector implements Serializable {

    // array of integer
    private int [] m_GridVector;

    //  one higher then the highest possible integer value
    //  in any of the integers in the gridvector
    private int m_Base;

    // size of vector
    private int m_Size;

    /*
     * Constructor
     *
     * @param numDim number of dimensions = number of attributes
     * @param base is one higher then the highest possible integer value
     * in any of the integers in the gridvector
     */
    private GridVector(int numDim, int base) {

      m_Size = numDim;
      m_Base = base;
      m_GridVector = new int [numDim];
      for (int i = 0; i < numDim; i++) {
	m_GridVector[i] = 0;
      }
    }

    /*
     * returns the integer array
     *
     * @return the integer array
     */
    private int [] getGridVector() {
      return m_GridVector;
    }

    /*
     * Overflow has occurred when integer is zero.
     *
     *@param digit the input integer
     *@return true if digit is 0
     */
    private boolean overflow(int digit) {
      return (digit == 0);
    }
    /*
     * Adds one to integer and sets to zero, if new value was
     * equal m_Base.
     *
     *@param digit the input integer
     *@return new integer object
     */
    private int addOne(int digit) {

      int value = digit + 1;
      if (value >= m_Base) value = 0;
      return value;
    }

    /*
     * add 1 to vector
     */
    private void addOne() {

      m_GridVector[0] = addOne(m_GridVector[0]);
      int i = 1;
      while (overflow(m_GridVector[i - 1]) && i < m_Size) {
        m_GridVector[i] = addOne(m_GridVector[i]);
	i++;
      }
	
    }

      
  } // end class GridVector


  
  /**
   * Returns a string describing this data generator.
   *
   * @return a description of the data generator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    
    return "A data generator that produces data points in "
           + "clusters.";
  }

  /**
   * Sets the upper and lower boundary for instances per cluster.
   *
   * @param newToFrom the string containing the upper and lower boundary for
   * instances per cluster separated by ..
   */
  public void setInstNums(String fromTo) {

    int i = fromTo.indexOf("..");
    String from = fromTo.substring(0, i);
    setMinInstNum(Integer.parseInt(from));
    String to = fromTo.substring(i + 2, fromTo.length());
    setMaxInstNum(Integer.parseInt(to));
    
  }
  
  /**
   * Gets the upper and lower boundary for instances per cluster.
   *
   * @return the string containing the upper and lower boundary for
   * instances per cluster separated by ..
   */
  public String getInstNums() {
    String fromTo = "" + 
      getMinInstNum() + ".." +
      getMaxInstNum();
    return fromTo;
  }
  
   /**
   * Gets the lower boundary for instances per cluster.
   *
   * @return the the lower boundary for instances per cluster
   */
  public int getMinInstNum() { return m_MinInstNum; }
  
  /**
   * Sets the lower boundary for instances per cluster.
   *
   * @param newMinInstNum new lower boundary for instances per cluster
   */
  public void setMinInstNum(int newMinInstNum) {
    m_MinInstNum = newMinInstNum;
  }

  /**
   * Gets the upper boundary for instances per cluster.
   *
   * @return the upper boundary for instances per cluster
   */
  public int getMaxInstNum() { return m_MaxInstNum; }
  
  /**
   * Sets the upper boundary for instances per cluster.
   *
   * @param newMaxInstNum new upper boundary for instances per cluster
   */
  public void setMaxInstNum(int newMaxInstNum) {
    m_MaxInstNum = newMaxInstNum;
  }

  /**
   * Sets the upper and lower boundary for the radius of the clusters.
   *
   * @param newToFrom the string containing the upper and lower boundary for
   * the radius  of the clusters, separated by ..
   */
  public void setRadiuses(String fromTo) {
    int i = fromTo.indexOf("..");
    String from = fromTo.substring(0, i);
    setMinRadius(Double.valueOf(from).doubleValue());
    String to = fromTo.substring(i + 2, fromTo.length());
    setMaxRadius(Double.valueOf(to).doubleValue());
  }

  /**
   * Gets the upper and lower boundary for the radius of the clusters.
   *
   * @return the string containing the upper and lower boundary for
   * the radius  of the clusters, separated by ..
   */
  public String getRadiuses() {
    String fromTo = "" + 
      Utils.doubleToString(getMinRadius(), 2) + ".." +
      Utils.doubleToString(getMaxRadius(), 2);
    return fromTo;
  }

  /**
   * Gets the lower boundary for the radiuses of the clusters.
   *
   * @return the lower boundary for the radiuses of the clusters
   */
  public double getMinRadius() { return m_MinRadius; }
  
  /**
   * Sets the lower boundary for the radiuses of the clusters.
   *
   * @param newMinRadius new lower boundary for the radiuses of the clusters
   */
  public void setMinRadius(double newMinRadius) {
    m_MinRadius = newMinRadius;
  }

  /**
   * Gets the upper boundary for the radiuses of the clusters.
   *
   * @return the upper boundary for the radiuses of the clusters
   */
  public double getMaxRadius() { return m_MaxRadius; }
  
  /**
   * Sets the upper boundary for the radiuses of the clusters.
   *
   * @param newMaxRadius new upper boundary for the radiuses of the clusters
   */
  public void setMaxRadius(double newMaxRadius) {
    m_MaxRadius = newMaxRadius;
  }

  /**
   * Gets the grid flag (option G).
   *
   * @return true if grid flag is set
   */
  public boolean getGridFlag() { return m_Pattern == GRID; }
  
  /**
   * Gets the sine flag (option S).
   *
   * @return true if sine flag is set
   */
  public boolean getSineFlag() { return m_Pattern == SINE; }
  
  /**
   * Gets the pattern type.
   *
   * @return the current pattern type
   */
  public int getPattern() { return m_Pattern; }

  /**
   * Sets the pattern type.
   *
   * @param newPattern new pattern type 
   */
  public void setPattern(int newPattern) {
    m_Pattern = newPattern;
  }

  /**
   * Gets the distance multiplier.
   *
   * @return the distance multiplier
   */
  public double getDistMult() { return m_DistMult; }
  
  /**
   * Sets the distance multiplier.
   *
   * @param newDistMult new distance multiplier
   */
  public void setDistMult(double newDistMult) {
    m_DistMult = newDistMult;
  }

  /**
   * Gets the number of cycles.
   *
   * @return the number of cycles
   */
  public int getNumCycles() { return m_NumCycles; }
  
  /**
   * Sets the the number of cycles.
   *
   * @param newNumCycles new number of cycles 
   */
  public void setNumCycles(int newNumCycles) {
    m_NumCycles = newNumCycles;
  }

  /**
   * Gets the input order.
   *
   * @return the current input order
   */
  public int getInputOrder() { return m_InputOrder; }

  /**
   * Sets the input order.
   *
   * @param newInputOrder new input order 
   */
  public void setInputOrder(int newInputOrder) {
    m_InputOrder = newInputOrder;
  }

  /**
   * Gets the ordered flag (option O).
   *
   * @return true if ordered flag is set
   */
  public boolean getOrderedFlag() { return m_InputOrder == ORDERED; }

  /**
   * Gets the percentage of noise set.
   *
   * @return the percentage of noise set
   */
  public double getNoiseRate() { return m_NoiseRate; }
  
  /**
   * Sets the percentage of noise set.
   *
   * @param newNoiseRate new percentage of noise 
   */
  public void setNoiseRate(double newNoiseRate) {
    m_NoiseRate = newNoiseRate;
  }

  /**
   * Gets the random generator.
   *
   * @return the random generator
   */
  public Random getRandom() {
    if (m_Random == null) {
      m_Random = new Random (getSeed());
    }
    return m_Random;
  }
  
  /**
   * Sets the random generator.
   *
   * @param newRandom is the random generator.
   */
  public void setRandom(Random newRandom) {
    m_Random = newRandom;
  }

  /**
   * Gets the random number seed.
   *
   * @return the random number seed.
   */
  public int getSeed() { return m_Seed; }
  
  /**
   * Sets the random number seed.
   *
   * @param newSeed the new random number seed.
   */
  public void setSeed(int newSeed) { m_Seed = newSeed; }  

  /**
   * Gets the dataset format.
   *
   * @return the dataset format.
   */
  public Instances getDatasetFormat() { return m_DatasetFormat; }
  
  /**
   * Sets the dataset format.
   *
   * @param newDatasetFormat the new dataset format.
   */
  public void setDatasetFormat(Instances newDatasetFormat) { 
    m_DatasetFormat = newDatasetFormat;
  }  

  /**
   * Gets the single mode flag.
   *
   * @return true if methode generateExample can be used.
   */
  public boolean getSingleModeFlag() { return (false); }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(5);
    
    newVector.addElement(new Option(
              "\tSet pattern to grid (default is random).",
              "G", 1, "-G"));

    newVector.addElement(new Option(
              "\tSet pattern to sine (default is random).",
              "S", 1, "-S"));

    newVector.addElement(new Option(
              "\tThe range of number of instances per cluster (default 1..50).",
              "N", 1, "-N <num>..<num>"));

    newVector.addElement(new Option(
              "\tThe range of radius per cluster (default 0.1..sqrt(2)).",
              "R", 1, "-R <num>..<num>"));
    
    newVector.addElement(new Option(
              "\tThe distance multiplier (default 4).",
              "M", 1, "-M <num>"));

    newVector.addElement(new Option(
              "\tThe number of cycles (default 4).",
              "C", 1, "-C <num>"));

    newVector.addElement(new Option(
              "\tSet input order to ordered (default is randomized).",
              "O", 1, "-O"));

    newVector.addElement(new Option(
              "\tThe noise rate in percent (default 0).",
              "P", 1, "-P <num>"));

    newVector.addElement(new Option(
              "\tThe Seed for random function (default 1).",
              "S", 1, "-S"));
 
    return newVector.elements();
  }

  /**
   * Sets all options to their default values. <p>
   */
  public void setDefaultOptions() {

    m_MinInstNum = 1;
    m_MaxInstNum = 50;
    m_MinRadius = 0.1;
    m_MaxRadius = Math.sqrt(2.0);
    m_Pattern = RANDOM;
    m_DistMult = 4;
    m_NumCycles = 4;
    m_InputOrder = RANDOMIZED;
    m_NoiseRate = 0.0;
    m_Seed = 1;
  }
  
  /**
   * Parses a list of options for this object. <p>
   *
   * For list of valid options see class description.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    setDefaultOptions();
    String num;
    String fromTo;


    fromTo = Utils.getOption('N', options);
    if (fromTo.length() != 0) {
      setInstNums(fromTo);
    }
    
    fromTo = Utils.getOption('R', options);
    if (fromTo.length() != 0) {
      setRadiuses(fromTo);
    } 

    boolean grid = Utils.getFlag('G', options);
    boolean sine = Utils.getFlag('I', options);

    if (grid && sine)
      throw new Exception("Flags G and I can only be set mutually exclusiv.");

    if (grid)
      setPattern(GRID);
    if (sine)
      setPattern(SINE);

    num = Utils.getOption('M', options);
    if (num.length() != 0) {
      if (!grid)
	throw new Exception("Option M can only be used with GRID pattern.");
      setDistMult(Double.valueOf(num).doubleValue());
    } 

    num = Utils.getOption('C', options);
    if (num.length() != 0) {
      if (!sine)
	throw new Exception("Option C can only be used with SINE pattern.");
      setNumCycles((int)Double.valueOf(num).doubleValue());
    } 

    boolean ordered = Utils.getFlag('O', options);
    if (ordered)
      setInputOrder(ORDERED);

    num = Utils.getOption('P', options);
    if (num.length() != 0) {
      setNoiseRate(Double.valueOf(num).doubleValue());
    } 

    num = Utils.getOption('S', options);
    if (num.length() != 0) {
      setSeed(Integer.parseInt(num));
    } 
  }

  /**
   * Gets the current settings of the datagenerator BIRCHCluster.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [20];
    int i = 0;
    options[i++] = "-N"; options[i++] = "" + getInstNums();

    options[i++] = "-R"; options[i++] = "" + getRadiuses();

    if (getGridFlag()) {
      options[i++] = "-G"; options[i++] = "";
      options[i++] = "-D"; options[i++] = "" + getDistMult();
    }

    if (getSineFlag()) {
      options[i++] = "-I"; options[i++] = "";
      options[i++] = "-C"; options[i++] = "" + getNumCycles();
    }

    if (getOrderedFlag()) {
      options[i++] = "-O"; options[i++] = "";
    }

    options[i++] = "-P"; options[i++] = "" + getNoiseRate();
    
    while (i < options.length) {
      options[i++] = "";
    }
    return options;
  }
    

 
  /**
   * Initializes the format for the dataset produced. 
   *
   * @return the output data format
   * @exception Exception data format could not be defined 
   */

  public Instances defineDataFormat() throws Exception {

    Random random = new Random (getSeed());
    setRandom(random);
    Instances dataset;
    FastVector attributes = new FastVector(3);
    Attribute attribute;
    boolean classFlag = getClassFlag();
    
    FastVector classValues = null;
    if (classFlag) classValues = new FastVector (m_NumClusters);     

    // define dataset
    for (int i = 0; i < getNumAttributes(); i++) {
      attribute = new Attribute("X" + i); 
      attributes.addElement(attribute);
    }
    
    if (classFlag) {
      for (int i = 0; i < m_NumClusters; i++) {
	classValues.addElement("c" + i);
      }
      attribute = new Attribute ("class", classValues); 
      attributes.addElement(attribute);
    }

    dataset = new Instances(getRelationName(), attributes, 0);
    if (classFlag) 
      dataset.setClassIndex(m_NumAttributes);

    // set dataset format of this class
    Instances format = new Instances(dataset, 0);
    setDatasetFormat(format);


    m_ClusterList = defineClusters(random);

    System.out.println("dataset" + dataset.numAttributes());
    return dataset; 
  }

  /**
   * Generate an example of the dataset. 
   * @return the instance generated
   * @exception Exception if format not defined or generating <br>
   * examples one by one is not possible, because voting is chosen
   */

  public Instance generateExample() throws Exception {

    throw new Exception("Examples cannot be generated" +
                                           " one by one.");
  }

  /**
   * Generate all examples of the dataset. 
   * @return the instance generated
   * @exception Exception if format not defined 
   */

  public Instances generateExamples() throws Exception {

    Random random = getRandom();
    Instances data = getDatasetFormat();
    if (data == null) throw new Exception("Dataset format not defined.");

    // generate examples
    if (getOrderedFlag())
      data = generateExamples(random, data);
    else
      throw new Exception("RANDOMIZED is not yet implemented.");
  
    return (data);
  }

  /**
   * Generate all examples of the dataset. 
   * @return the instance generated
   * @exception Exception if format not defined
   */

  public Instances generateExamples(Random random,
				    Instances format) throws Exception {
    Instance example = null;
    
    if (format == null) throw new Exception("Dataset format not defined.");

    // generate examples for one cluster after another
    int cNum = 0;
    for (Enumeration enu = m_ClusterList.elements();
	 enu.hasMoreElements(); cNum++) {
      Cluster cl  = (Cluster) enu.nextElement();
      double stdDev = cl.getStdDev();
      int instNum = cl.getInstNum();
      double [] center = cl.getCenter();
      String cName = "c" + cNum;

      for (int i = 0; i < instNum; i++) {
	
	// generate example
	example = generateInstance (format,
				    random,
				    stdDev,
				    center,
				    cName);
       
	if (example != null)
	  example.setDataset(format);
	format.add(example);
      }
    }

    return (format);
  }

  /**
   * Generate an example of the dataset. 
   * @return the instance generated
   * @exception Exception if format not defined or generating <br>
   * examples one by one is not possible, because voting is chosen
   */
  private Instance generateInstance (Instances format,
				     Random randomG,
				     double stdDev,
				     double [] center,
				     String cName
				     ) {
    Instance example;
    int numAtts = m_NumAttributes;
    if (getClassFlag()) numAtts++;

    example = new Instance(numAtts);
    example.setDataset(format);
        
    for (int i = 0; i < m_NumAttributes; i++) {
      example.setValue(i, randomG.nextGaussian() * stdDev + center[i]); 
    }
    
    if (getClassFlag()) {
      example.setClassValue(cName);
    }
    return example; 
  }

 /**
   * Defines the clusters 
   *
   * @param random random number generator
   */
  private FastVector defineClusters(Random random)
   throws Exception {

    if (m_Pattern == GRID)
      return defineClustersGRID(random);
    else
      return defineClustersRANDOM(random);
  }

  /**
   * Defines the clusters if pattern is GRID
   *
   * @param random random number generator
   */
  private FastVector defineClustersGRID(Random random)
   throws Exception {

    FastVector clusters = new FastVector(m_NumClusters);
    double diffInstNum = (double) (m_MaxInstNum - m_MinInstNum);
    double minInstNum = (double) m_MinInstNum;
    double diffRadius = m_MaxRadius - m_MinRadius;
    Cluster cluster;

    // compute gridsize
    double gs = Math.pow(m_NumClusters, 1.0 / m_NumAttributes);
    
    if (gs - ((double) ((int) gs))  > 0.0) {
      m_GridSize = (int) (gs + 1.0);
    } else { m_GridSize = (int) gs; }

    // compute gridwidth
    m_GridWidth = ((m_MaxRadius + m_MinRadius) / 2) * m_DistMult;

    System.out.println("GridSize= " + m_GridSize);
    System.out.println("GridWidth= " + m_GridWidth);
    // initialize gridvector with zeros
    GridVector gv = new GridVector(m_NumAttributes, m_GridSize);

    for (int i = 0; i < m_NumClusters; i++) {
      int instNum = (int) (random.nextDouble() * diffInstNum
                                   + minInstNum);
      double radius = (random.nextDouble() * diffRadius) + m_MinRadius;

      // center is defined in the constructor of cluster
      cluster = new Cluster(instNum, radius,
			    gv.getGridVector(), m_GridWidth);
      clusters.addElement((Object) cluster);
      gv.addOne();
    }
    return clusters;
  }

 /**
   * Defines the clusters if pattern is RANDOM
   *
   * @param random random number generator
   */
  private FastVector defineClustersRANDOM(Random random)
   throws Exception {

    FastVector clusters = new FastVector(m_NumClusters);
    double diffInstNum = (double) (m_MaxInstNum - m_MinInstNum);
    double minInstNum = (double) m_MinInstNum;
    double diffRadius = m_MaxRadius - m_MinRadius;
    Cluster cluster;

    for (int i = 0; i < m_NumClusters; i++) {
      int instNum = (int) (random.nextDouble() * diffInstNum
                                   + minInstNum);
      double radius = (random.nextDouble() * diffRadius) + m_MinRadius;

      // center is defined in the constructor of cluster
      cluster = new Cluster(instNum, radius, random);
      clusters.addElement((Object) cluster);
    }
    return clusters;
  }


  /**
   * Compiles documentation about the data generation after
   * the generation process
   *
   * @return string with additional information about generated dataset
   * @exception Exception no input structure has been defined
   */
  public String generateFinished() throws Exception {

    StringBuffer docu = new StringBuffer();
    Instances format = getDatasetFormat();//just for exception

    // string is empty
    docu.append("\n%\n%\n");
    return docu.toString();
  }
  
  /**
   * Compiles documentation about the data generation before
   * the generation process
   *
   * @return string with additional information 
   */
  public String generateStart() {

    StringBuffer docu = new StringBuffer();
    // string is empty
    docu.append("\n%\n%\n");

    int sumInst = 0;
    int cNum = 0;
    for (Enumeration enu = m_ClusterList.elements();
	 enu.hasMoreElements(); cNum++) {
      Cluster cl  = (Cluster) enu.nextElement();
      docu.append("%\n");
      docu.append("% Cluster: c"+ cNum + "\n");
      docu.append("% ----------------------------------------------\n");
      docu.append("% StandardDeviation: "
		  + Utils.doubleToString(cl.getStdDev(), 2) + "\n");
      docu.append("% Number of instances: "
		  + cl.getInstNum() + "\n");
      sumInst += cl.getInstNum();
      double [] center = cl.getCenter();
      docu.append("% "); 
      for (int i = 0; i < center.length - 1; i++) {
        docu.append(Utils.doubleToString(center[i], 2) + ", ");
      }
      docu.append(Utils.doubleToString(center[center.length - 1], 2) + "\n");
    }
    docu.append("\n% ----------------------------------------------\n"); 
    docu.append("% Total number of instances: " + sumInst + "\n");
    docu.append("%                            in " + cNum + " clusters\n");
    docu.append("% Pattern chosen           : ");
    if (getGridFlag()) docu.append("GRID, "
	     + "distance multiplier = " +
	     Utils.doubleToString(m_DistMult, 2) + "\n");
    else
      if (getSineFlag()) docu.append("SINE\n");
      else
	docu.append("RANDOM\n");
    return docu.toString();
  }
  

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments for the data producer: 
   */
  public static void main(String [] argv) {

    try {
      ClusterGenerator.makeData(new BIRCHCluster(), argv);
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }
}
