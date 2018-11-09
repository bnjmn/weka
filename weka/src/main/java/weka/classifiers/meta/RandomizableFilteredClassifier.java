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

import weka.core.*;

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
 *  default: "weka.filters.unsupervised.attribute.RandomProjection -N 10 -D Sparse1"</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 * <pre> -W
 *  Full name of base classifier.
 *  (default: weka.classifiers.lazy.IBk)</pre>
 *
 * <pre> -S num
 * Set the random number seed (default 1). </pre>
 *
 * <pre> 
 * Options specific to classifier weka.classifiers.lazy.IBk:
 * </pre>
 *
 * <pre> -I
 *  Weight neighbours by the inverse of their distance
 *  (use when k &gt; 1)</pre>
 *
 * <pre> -F
 *  Weight neighbours by 1 - their distance
 *  (use when k &gt; 1)</pre>
 *
 * <pre> -K &lt;number of neighbors&gt;
 *  Number of nearest neighbours (k) used in classification.
 *  (Default = 1)</pre>
 *
 * <pre> -E
 *  Minimise mean squared error rather than mean absolute
 *  error when using -X option with numeric prediction.</pre>
 *
 * <pre> -W &lt;window size&gt;
 *  Maximum number of training instances maintained.
 *  Training instances are dropped FIFO. (Default = no window)</pre>
 *
 * <pre> -X
 *  Select the number of nearest neighbours between 1
 *  and the k value specified using hold-one-out evaluation
 *  on the training data (use when k &gt; 1)</pre>
 *
 * <pre> -A
 *  The nearest neighbour search algorithm to use (default: weka.core.neighboursearch.LinearNNSearch).
 * </pre>
 *
 <!-- options-end -->
 *
 * @author Eibe Frank
 * @version $Revision: 9117 $
 */
public class RandomizableFilteredClassifier extends FilteredClassifier {

  /** for serialization */
  static final long serialVersionUID = -4523466618555717333L;

  /**
   * Returns a string describing this classifier
   * @return a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return   "A simple variant of the FilteredClassifier that instantiates the model with a randomizable filter, " +
            "more specifically, RandomProjection, and IBk as the base classifier. Other than this, and checking " +
            "that at least one of the two base schemes implements the Randomizable interface, it implements " +
            "exactly the same functionality as FilteredClassifier, which (now) also implements Randomizable.";
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

    return "weka.filters.unsupervised.attribute.RandomProjection -N 10 -D Sparse1";
  }

  /**
   * Default constructor.
   */
  public RandomizableFilteredClassifier() {

    m_Classifier = new weka.classifiers.lazy.IBk();
    m_Filter = new weka.filters.unsupervised.attribute.RandomProjection();
  }

  /**
   * Initializes an iterative classifier. (If the base classifier supports
   * this.)
   *
   * @param data the instances to be used in induction
   * @exception Exception if the model cannot be initialized
   */
  @Override public void initializeClassifier(Instances data) throws Exception {

    if (!(m_Classifier instanceof Randomizable) &&
            !(m_Filter instanceof Randomizable)) {
      throw new Exception("Either the classifier or the filter must implement the Randomizable interface.");
    }

    super.initializeClassifier(data);
  }

  /**
   * Build the classifier on the filtered data.
   *
   * @param data the training data
   * @throws Exception if the classifier could not be built successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    if (!(m_Classifier instanceof Randomizable) &&
        !(m_Filter instanceof Randomizable)) {
      throw new Exception("Either the classifier or the filter must implement the Randomizable interface.");
    }

    super.buildClassifier(data);
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
