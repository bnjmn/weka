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
 *    VFI.java
 *    Copyright (C) 2000 Mark Hall.
 *
 */

package weka.classifiers.misc;

import weka.classifiers.Classifier;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.WeightedInstancesHandler;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Classification by voting feature intervals. Intervals are constucted around each class for each attribute (basically discretization). Class counts are recorded for each interval on each attribute. Classification is by voting. For more info see:<br/>
 * <br/>
 * G. Demiroz, A. Guvenir: Classification by voting feature intervals. In: 9th European Conference on Machine Learning, 85-92, 1997.<br/>
 * <br/>
 * Have added a simple attribute weighting scheme. Higher weight is assigned to more confident intervals, where confidence is a function of entropy:<br/>
 * weight (att_i) = (entropy of class distrib att_i / max uncertainty)^-bias
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Demiroz1997,
 *    author = {G. Demiroz and A. Guvenir},
 *    booktitle = {9th European Conference on Machine Learning},
 *    pages = {85-92},
 *    publisher = {Springer},
 *    title = {Classification by voting feature intervals},
 *    year = {1997}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 * Faster than NaiveBayes but slower than HyperPipes. <p><p>
 *
 * <pre>
 *  Confidence: 0.01 (two tailed)
 *
 * Dataset                   (1) VFI '-B  | (2) Hyper (3) Naive
 *                         ------------------------------------
 * anneal.ORIG               (10)   74.56 |   97.88 v   74.77
 * anneal                    (10)   71.83 |   97.88 v   86.51 v
 * audiology                 (10)   51.69 |   66.26 v   72.25 v
 * autos                     (10)   57.63 |   62.79 v   57.76
 * balance-scale             (10)   68.72 |   46.08 *   90.5  v
 * breast-cancer             (10)   67.25 |   69.84 v   73.12 v
 * wisconsin-breast-cancer   (10)   95.72 |   88.31 *   96.05 v
 * horse-colic.ORIG          (10)   66.13 |   70.41 v   66.12
 * horse-colic               (10)   78.36 |   62.07 *   78.28
 * credit-rating             (10)   85.17 |   44.58 *   77.84 *
 * german_credit             (10)   70.81 |   69.89 *   74.98 v
 * pima_diabetes             (10)   62.13 |   65.47 v   75.73 v
 * Glass                     (10)   56.82 |   50.19 *   47.43 *
 * cleveland-14-heart-diseas (10)   80.01 |   55.18 *   83.83 v
 * hungarian-14-heart-diseas (10)   82.8  |   65.55 *   84.37 v
 * heart-statlog             (10)   79.37 |   55.56 *   84.37 v
 * hepatitis                 (10)   83.78 |   63.73 *   83.87
 * hypothyroid               (10)   92.64 |   93.33 v   95.29 v
 * ionosphere                (10)   94.16 |   35.9  *   82.6  *
 * iris                      (10)   96.2  |   91.47 *   95.27 *
 * kr-vs-kp                  (10)   88.22 |   54.1  *   87.84 *
 * labor                     (10)   86.73 |   87.67     93.93 v
 * lymphography              (10)   78.48 |   58.18 *   83.24 v
 * mushroom                  (10)   99.85 |   99.77 *   95.77 *
 * primary-tumor             (10)   29    |   24.78 *   49.35 v
 * segment                   (10)   77.42 |   75.15 *   80.1  v
 * sick                      (10)   65.92 |   93.85 v   92.71 v
 * sonar                     (10)   58.02 |   57.17     67.97 v
 * soybean                   (10)   86.81 |   86.12 *   92.9  v
 * splice                    (10)   88.61 |   41.97 *   95.41 v
 * vehicle                   (10)   52.94 |   32.77 *   44.8  *
 * vote                      (10)   91.5  |   61.38 *   90.19 *
 * vowel                     (10)   57.56 |   36.34 *   62.81 v
 * waveform                  (10)   56.33 |   46.11 *   80.02 v
 * zoo                       (10)   94.05 |   94.26     95.04 v
 *                          ------------------------------------
 *                                (v| |*) |  (9|3|23)  (22|5|8) 
 * </pre> 					
 * <p>
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -C
 *  Don't weight voting intervals by confidence</pre>
 * 
 * <pre> -B &lt;bias&gt;
 *  Set exponential bias towards confident intervals
 *  (default = 1.0)</pre>
 * 
 <!-- options-end -->
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.13 $
 */
public class VFI 
  extends Classifier 
  implements OptionHandler, WeightedInstancesHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 8081692166331321866L;
  
  /** The index of the class attribute */
  protected int m_ClassIndex;

  /** The number of classes */
  protected int m_NumClasses;

  /** The training data */
  protected Instances m_Instances = null;

  /** The class counts for each interval of each attribute */
  protected double [][][] m_counts;

  /** The global class counts */
  protected double [] m_globalCounts;

  /** The lower bounds for each attribute */
  protected double [][] m_intervalBounds;

  /** The maximum entropy for the class */
  protected double m_maxEntrop;

  /** Exponentially bias more confident intervals */
  protected boolean m_weightByConfidence = true;

  /** Bias towards more confident intervals */
  protected double m_bias = -0.6;

  private double TINY = 0.1e-10;

  /**
   * Returns a string describing this search method
   * @return a description of the search method suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Classification by voting feature intervals. Intervals are "
      +"constucted around each class for each attribute ("
      +"basically discretization). Class counts are "
      +"recorded for each interval on each attribute. Classification is by "
      +"voting. For more info see:\n\n"
      + getTechnicalInformation().toString() + "\n\n"
      +"Have added a simple attribute weighting scheme. Higher weight is "
      +"assigned to more confident intervals, where confidence is a function "
      +"of entropy:\nweight (att_i) = (entropy of class distrib att_i / "
      +"max uncertainty)^-bias";
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "G. Demiroz and A. Guvenir");
    result.setValue(Field.TITLE, "Classification by voting feature intervals");
    result.setValue(Field.BOOKTITLE, "9th European Conference on Machine Learning");
    result.setValue(Field.YEAR, "1997");
    result.setValue(Field.PAGES, "85-92");
    result.setValue(Field.PUBLISHER, "Springer");
    
    return result;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(
    new Option("\tDon't weight voting intervals by confidence",
	       "C", 0,"-C"));
    newVector.addElement(
    new Option("\tSet exponential bias towards confident intervals\n"
	       +"\t(default = 1.0)",
	       "B", 1,"-B <bias>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -C
   *  Don't weight voting intervals by confidence</pre>
   * 
   * <pre> -B &lt;bias&gt;
   *  Set exponential bias towards confident intervals
   *  (default = 1.0)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String optionString;
    
    setWeightByConfidence(!Utils.getFlag('C', options));
    
    optionString = Utils.getOption('B', options);
    if (optionString.length() != 0) {
      Double temp = new Double(optionString);
      setBias(temp.doubleValue());
    }

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String weightByConfidenceTipText() {
    return "Weight feature intervals by confidence";
  }

  /**
   * Set weighting by confidence
   * @param c true if feature intervals are to be weighted by confidence
   */
  public void setWeightByConfidence(boolean c) {
    m_weightByConfidence = c;
  }

  /**
   * Get whether feature intervals are being weighted by confidence
   * @return true if weighting by confidence is selected
   */
  public boolean getWeightByConfidence() {
    return m_weightByConfidence;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String biasTipText() {
    return "Strength of bias towards more confident features";
  }

  /**
   * Set the value of the exponential bias towards more confident intervals
   * @param b the value of the bias parameter
   */
  public void setBias(double b) {
    m_bias = -b;
  }

  /**
   * Get the value of the bias parameter
   * @return the bias parameter
   */
  public double getBias() {
    return -m_bias;
  }

  /**
   * Gets the current settings of VFI
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[3];
    int current = 0;
    
    if (!getWeightByConfidence()) {
      options[current++] = "-C";
    }

    options[current++] = "-B"; options[current++] = ""+getBias();
    while (current < options.length) {
      options[current++] = "";
    }
    
    return options;
  }
  

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.STRING_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(0);
    
    return result;
  }

  /**
   * Generates the classifier.
   *
   * @param instances set of instances serving as training data 
   * @throws Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances instances) throws Exception {

    if (!m_weightByConfidence) {
      TINY = 0.0;
    }

    // can classifier handle the data?
    getCapabilities().testWithFail(instances);

    // remove instances with missing class
    instances = new Instances(instances);
    instances.deleteWithMissingClass();

    m_ClassIndex = instances.classIndex();
    m_NumClasses = instances.numClasses();
    m_globalCounts = new double [m_NumClasses];
    m_maxEntrop = Math.log(m_NumClasses) / Math.log(2);

    m_Instances = new Instances(instances, 0); // Copy the structure for ref

    m_intervalBounds = 
      new double[instances.numAttributes()][2+(2*m_NumClasses)];

    for (int j = 0; j < instances.numAttributes(); j++) {
      boolean alt = false;
      for (int i = 0; i < m_NumClasses*2+2; i++) {
	if (i == 0) {
	  m_intervalBounds[j][i] = Double.NEGATIVE_INFINITY;
	} else if (i == m_NumClasses*2+1) {
	  m_intervalBounds[j][i] = Double.POSITIVE_INFINITY;
	} else {
	  if (alt) {
	    m_intervalBounds[j][i] = Double.NEGATIVE_INFINITY;
	    alt = false;
	  } else {
	    m_intervalBounds[j][i] = Double.POSITIVE_INFINITY;
	    alt = true;
	  }
	}
      }
    }

    // find upper and lower bounds for numeric attributes
    for (int j = 0; j < instances.numAttributes(); j++) {
      if (j != m_ClassIndex && instances.attribute(j).isNumeric()) {
	for (int i = 0; i < instances.numInstances(); i++) {
	  Instance inst = instances.instance(i);
	  if (!inst.isMissing(j)) {
	    if (inst.value(j) < 
		m_intervalBounds[j][((int)inst.classValue()*2+1)]) {
	      m_intervalBounds[j][((int)inst.classValue()*2+1)] = 
		inst.value(j);
	    }
	    if (inst.value(j) > 
		m_intervalBounds[j][((int)inst.classValue()*2+2)]) {
	      m_intervalBounds[j][((int)inst.classValue()*2+2)] = 
		inst.value(j);
	    }
	  }
	}
      }
    }

    m_counts = new double [instances.numAttributes()][][];

    // sort intervals
    for (int i = 0 ; i < instances.numAttributes(); i++) {
      if (instances.attribute(i).isNumeric()) {
	int [] sortedIntervals = Utils.sort(m_intervalBounds[i]);
	// remove any duplicate bounds
	int count = 1;
	for (int j = 1; j < sortedIntervals.length; j++) {
	  if (m_intervalBounds[i][sortedIntervals[j]] != 
	      m_intervalBounds[i][sortedIntervals[j-1]]) {
	    count++;
	  }
	}
	double [] reordered = new double [count];
	count = 1;
	reordered[0] = m_intervalBounds[i][sortedIntervals[0]];
	for (int j = 1; j < sortedIntervals.length; j++) {
	   if (m_intervalBounds[i][sortedIntervals[j]] != 
	      m_intervalBounds[i][sortedIntervals[j-1]]) {
	     reordered[count] =  m_intervalBounds[i][sortedIntervals[j]];
	     count++;
	   }
	}
	m_intervalBounds[i] = reordered;
	m_counts[i] = new double [count][m_NumClasses];
      } else if (i != m_ClassIndex) { // nominal attribute
	m_counts[i] = 
	  new double [instances.attribute(i).numValues()][m_NumClasses];
      }
    }

    // collect class counts
    for (int i = 0; i < instances.numInstances(); i++) {
      Instance inst = instances.instance(i);
      m_globalCounts[(int)instances.instance(i).classValue()] += inst.weight();
      for (int j = 0; j < instances.numAttributes(); j++) {
	if (!inst.isMissing(j) && j != m_ClassIndex) {
	  if (instances.attribute(j).isNumeric()) {
	    double val = inst.value(j);
	   
	    int k;
	    for (k = m_intervalBounds[j].length-1; k >= 0; k--) {
	      if (val > m_intervalBounds[j][k]) {
		m_counts[j][k][(int)inst.classValue()] += inst.weight();
		break;
	      } else if (val == m_intervalBounds[j][k]) {
		m_counts[j][k][(int)inst.classValue()] += 
		  (inst.weight() / 2.0);
		m_counts[j][k-1][(int)inst.classValue()] += 
		  (inst.weight() / 2.0);;
		break;
	      }
	    }
	   
	  } else {
	    // nominal attribute
	    m_counts[j][(int)inst.value(j)][(int)inst.classValue()] += 
	      inst.weight();;
	  }
	}
      }
    }
  }

  /**
   * Returns a description of this classifier.
   *
   * @return a description of this classifier as a string.
   */
  public String toString() {
    if (m_Instances == null) {
      return "FVI: Classifier not built yet!";
    }
    StringBuffer sb = 
      new StringBuffer("Voting feature intervals classifier\n");

    /* Output the intervals and class counts for each attribute */
    /*    for (int i = 0; i < m_Instances.numAttributes(); i++) {
      if (i != m_ClassIndex) {
	sb.append("\n"+m_Instances.attribute(i).name()+" :\n");
	 if (m_Instances.attribute(i).isNumeric()) {
	   for (int j = 0; j < m_intervalBounds[i].length; j++) {
	     sb.append(m_intervalBounds[i][j]).append("\n");
	     if (j != m_intervalBounds[i].length-1) {
	       for (int k = 0; k < m_NumClasses; k++) {
		 sb.append(m_counts[i][j][k]+" ");
	       }
	     }
	     sb.append("\n");
	   }
	 } else {
	   for (int j = 0; j < m_Instances.attribute(i).numValues(); j++) {
	     sb.append(m_Instances.attribute(i).value(j)).append("\n");
	     for (int k = 0; k < m_NumClasses; k++) {
	       sb.append(m_counts[i][j][k]+" ");
	     }
	     sb.append("\n");
	   }
	 }
      }
      } */
    return sb.toString();
  }

  /**
   * Classifies the given test instance.
   *
   * @param instance the instance to be classified
   * @return the predicted class for the instance 
   * @throws Exception if the instance can't be classified
   */
  public double [] distributionForInstance(Instance instance) 
    throws Exception {
    double [] dist = new double[m_NumClasses];
    double [] temp = new double[m_NumClasses];
    double weight = 1.0;


    for (int i = 0; i < instance.numAttributes(); i++) {
      if (i != m_ClassIndex && !instance.isMissing(i)) {
	double val = instance.value(i);
	boolean ok = false;
	if (instance.attribute(i).isNumeric()) {
	  int k;
	  for (k = m_intervalBounds[i].length-1; k >= 0; k--) {
	    if (val > m_intervalBounds[i][k]) {
	      for (int j = 0; j < m_NumClasses; j++) {
		if (m_globalCounts[j] > 0) {
		  temp[j] = ((m_counts[i][k][j]+TINY) / 
			     (m_globalCounts[j]+TINY));
		}
	      }
	      ok = true;
	      break;
	    } else if (val == m_intervalBounds[i][k]) {
	      for (int j = 0; j < m_NumClasses; j++) {
		if (m_globalCounts[j] > 0) {
		  temp[j] = ((m_counts[i][k][j] + m_counts[i][k-1][j]) / 2.0) +
		    TINY;
		  temp[j] /= (m_globalCounts[j]+TINY);
		}
	      }
	      ok = true;
	      break;
	    }
	  }
	  if (!ok) {
	    throw new Exception("This shouldn't happen");
	  }
	} else { // nominal attribute
	  ok = true;
	  for (int j = 0; j < m_NumClasses; j++) {
	    if (m_globalCounts[j] > 0) {
	      temp[j] = ((m_counts[i][(int)val][j]+TINY) / 
			 (m_globalCounts[j]+TINY));
	    }
	  }	  
	}
	
	double sum = Utils.sum(temp);
	if (sum <= 0) {
	  for (int j = 0; j < temp.length; j++) {
	    temp[j] = 1.0 / (double)temp.length;
	  }
	} else {
	  Utils.normalize(temp, sum);
	}

	if (m_weightByConfidence) {
	  weight = weka.core.ContingencyTables.entropy(temp);
	  weight = Math.pow(weight, m_bias);
	  if (weight < 1.0) {
	    weight = 1.0;
	  }
	}

	for (int j = 0; j < m_NumClasses; j++) {
	  dist[j] += (temp[j] * weight);
	}
      }
    }
   
    double sum = Utils.sum(dist);
    if (sum <= 0) {
      for (int j = 0; j < dist.length; j++) {
	dist[j] = 1.0 / (double)dist.length;
      }
      return dist;
    } else {
      Utils.normalize(dist, sum);
      return dist;
    }
  }
	
  /**
   * Main method for testing this class.
   *
   * @param args should contain command line arguments for evaluation
   * (see Evaluation).
   */
  public static void main(String [] args) {
    runClassifier(new VFI(), args);
  }
}

