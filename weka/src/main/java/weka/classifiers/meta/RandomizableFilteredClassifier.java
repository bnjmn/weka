/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    RandomizableFilteredClassifier.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.meta;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Random;

import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;
import weka.core.Randomizable;
import weka.filters.Filter;

/**
 <!-- globalinfo-start -->
 * Class for running an arbitrary classifier on data that has been passed through an arbitrary filter. Like the classifier, the structure of the filter is based exclusively on the training data and test instances will be processed by the filter without changing their structure.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -F &lt;filter specification&gt;
 *  Full class name of filter to use, followed
 *  by filter options.
 *  eg: "weka.filters.unsupervised.attribute.Remove -V -R 1,2"</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.trees.J48)</pre>
 * 
 * <pre> 
 * Options specific to classifier weka.classifiers.trees.J48:
 * </pre>
 * 
 * <pre> -U
 *  Use unpruned tree.</pre>
 * 
 * <pre> -C &lt;pruning confidence&gt;
 *  Set confidence threshold for pruning.
 *  (default 0.25)</pre>
 * 
 * <pre> -M &lt;minimum number of instances&gt;
 *  Set minimum number of instances per leaf.
 *  (default 2)</pre>
 * 
 * <pre> -R
 *  Use reduced error pruning.</pre>
 * 
 * <pre> -N &lt;number of folds&gt;
 *  Set number of folds for reduced error
 *  pruning. One fold is used as pruning set.
 *  (default 3)</pre>
 * 
 * <pre> -B
 *  Use binary splits only.</pre>
 * 
 * <pre> -S
 *  Don't perform subtree raising.</pre>
 * 
 * <pre> -L
 *  Do not clean up after the tree has been built.</pre>
 * 
 * <pre> -A
 *  Laplace smoothing for predicted probabilities.</pre>
 * 
 * <pre> -Q &lt;seed&gt;
 *  Seed for random data shuffling (default 1).</pre>
 * 
 <!-- options-end -->
 *
 * @author Eibe Frank
 * @version $Revision: 9117 $
 */
public class RandomizableFilteredClassifier extends FilteredClassifier implements Randomizable {

  /** for serialization */
  static final long serialVersionUID = -4523466618555717333L;

  /** The random number seed. */
  protected int m_Seed = 1;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return   "A simple variant of the FilteredClassifier that implements the Randomizable interface, useful for "
      + "building ensemble classifiers using the RandomCommittee meta learner. It requires " +
      "that either the filter or the base learner implement the Randomizable interface.";
  }

  /**
   * String describing default classifier.
   * 
   * @return the default classifier classname
   */
  protected String defaultClassifierString() {
    
    return "weka.classifiers.lazy.IBk";
  }

  /**
   * String describing default filter.
   */
  protected String defaultFilterString() {

    return "weka.filters.unsupervised.attribute.RandomProjection";
  }

  /**
   * Default constructor.
   */
  public RandomizableFilteredClassifier() {

    m_Classifier = new weka.classifiers.lazy.IBk();
    m_Filter = new weka.filters.unsupervised.attribute.RandomProjection();
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration<Option> listOptions() {

    Vector<Option> newVector = new Vector<Option>(1);

    newVector.addElement(new Option(
          "\tRandom number seed.\n"
          + "\t(default 1)",
          "S", 1, "-S <num>"));

    newVector.addAll(Collections.list(super.listOptions()));
    
    if (getFilter() instanceof OptionHandler) {
      newVector.addElement(new Option(
        "",
        "", 0, "\nOptions specific to filter "
          + getFilter().getClass().getName() + ":"));
      newVector.addAll(Collections.list(((OptionHandler)getFilter()).listOptions()));
    }
    
    return newVector.elements();
  }

  /**
   * Parses a given list of options. Valid options are:<p>
   *
   * -W classname <br>
   * Specify the full class name of the base learner.<p>
   *
   * -I num <br>
   * Set the number of iterations (default 10). <p>
   *
   * -S num <br>
   * Set the random number seed (default 1). <p>
   *
   * Options after -- are passed to the designated classifier.<p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String seed = Utils.getOption('S', options);
    if (seed.length() != 0) {
      setSeed(Integer.parseInt(seed));
    } else {
      setSeed(1);
    }

    super.setOptions(options);
    
    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    Vector<String> options = new Vector<String>();
   
    options.add("-S");
    options.add("" + getSeed());

    Collections.addAll(options, super.getOptions());
    
    return options.toArray(new String[0]);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {

    return "The random number seed to be used.";
  }

  /**
   * Set the seed for random number generation.
   *
   * @param seed the seed
   */
  public void setSeed(int seed) {

    m_Seed = seed;
  }

  /**
   * Gets the seed for the random number generations
   *
   * @return the seed for the random number generation
   */
  public int getSeed() {

    return m_Seed;
  }

  /**
   * Build the classifier on the filtered data.
   *
   * @param data the training data
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    if (m_Classifier == null) {
      throw new Exception("No base classifiers have been set!");
    }

    if (!(m_Classifier instanceof Randomizable) &&
        !(m_Filter instanceof Randomizable)) {
      throw new Exception("Either the classifier or the filter must implement " +
                          "the Randomizable interface.");
    }

    getCapabilities().testWithFail(data);

    // get fresh instances object
    data = new Instances(data);
    
    if (data.numInstances() == 0) {
      throw new Exception("No training instances.");
    }

    try {

    // get a random number generator
    Random r = data.getRandomNumberGenerator(m_Seed);

    if (m_Filter instanceof Randomizable) {
      ((Randomizable)m_Filter).setSeed(r.nextInt());
    }

    m_Filter.setInputFormat(data);  // filter capabilities are checked here
    data = Filter.useFilter(data, m_Filter);

    // can classifier handle the data?
    getClassifier().getCapabilities().testWithFail(data);

    m_FilteredInstances = data.stringFreeStructure();

    if (m_Classifier instanceof Randomizable) {
      ((Randomizable)m_Classifier).setSeed(r.nextInt());
    }
    m_Classifier.buildClassifier(data);

    } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  /**
   * Output a representation of this classifier
   * 
   * @return a representation of this classifier
   */
  public String toString() {

    if (m_FilteredInstances == null) {
      return "RandomizableFilteredClassifier: No model built yet.";
    }

    String result = "RandomizableFilteredClassifier using "
      + getClassifierSpec()
      + " on data filtered through "
      + getFilterSpec()
      + "\n\nFiltered Header\n"
      + m_FilteredInstances.toString()
      + "\n\nClassifier Model\n"
      + m_Classifier.toString();
    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 9117 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param argv should contain the following arguments:
   * -t training file [-T test file] [-c class index]
   */
  public static void main(String [] argv)  {
    runClassifier(new RandomizableFilteredClassifier(), argv);
  }
}
