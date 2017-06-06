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
 * ClassificationViaClustering.java
 * Copyright (C) 2007 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.meta;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.classifiers.rules.ZeroR;
import weka.clusterers.*;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.Utils;

/**
 * <!-- globalinfo-start -->
 * A simple meta-classifier that uses a clusterer for classification.
 * By default, the best single cluster for each class is found using the method Weka
 * applies for classes-to-clusters evaluation. All other clusters are left without
 * class labels and a test instance assigned to one of the unlabeled clusters is left
 * unclassified. Optionally, this behaviour can be changed so that each cluster is labeled
 * based on the proportion of training instances in each class that is assigned to it.
 * If the cluster is a probabilistic ones, cluster membership probabilities are
 * used instead of counts to establish class probability estimates.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -output-debug-info
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console
 * </pre>
 * 
 * <pre>
 * -W &lt;clusterer&gt
 *  Full name of clusterer.
 *  (default: weka.clusterers.SimpleKMeans)
 * </pre>
 *
 * <pre>
 * -label-all-clusters
 *  If set, all clusters are labeled probabilistically instead of just those ones that best fit a class
 * </pre>
 *
 * <pre>
 * Options specific to clusterer weka.clusterers.SimpleKMeans:
 * </pre>
 * 
 * <pre>
 * -N &lt;num&gt;
 *  number of clusters.
 *  (default 2).
 * </pre>
 * 
 * <pre>
 * -V
 *  Display std. deviations for centroids.
 * </pre>
 * 
 * <pre>
 * -M
 *  Replace missing values with mean/mode.
 * </pre>
 * 
 * <pre>
 * -S &lt;num&gt;
 *  Random number seed.
 *  (default 10)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class ClassificationViaClustering extends AbstractClassifier {

  /** for serialization */
  private static final long serialVersionUID = -5687069451420259135L;

  /** the cluster algorithm used (template) */
  protected Clusterer m_Clusterer;

  /** whether to label all clusters probabilistically instead of just finding one cluster for each class */
  protected boolean m_labelAllClusters;

  /** the actual cluster algorithm being used */
  protected Clusterer m_ActualClusterer;

  /** the original training data header */
  protected Instances m_OriginalHeader;

  /** the modified training data header */
  protected Instances m_ClusteringHeader;

  /** the mapping between clusters and classes */
  protected double[] m_ClustersToClasses;

  /** the class probabilities for each cluster */
  protected double[][] m_ClusterClassProbs;

  /** the default model */
  protected Classifier m_ZeroR;

  /**
   * default constructor
   */
  public ClassificationViaClustering() {
    super();

    m_Clusterer = new SimpleKMeans();
  }

  /**
   * Returns a string describing classifier
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  public String globalInfo() {
    return "A simple meta-classifier that uses a clusterer for classification. " +
            "By default, the best single cluster for each class is found using the method Weka " +
            "applies for classes-to-clusters evaluation. All other clusters are left without " +
            "class labels and a test instance assigned to one of the unlabeled clusters is left " +
            "unclassified. Optionally, this behaviour can be changed so that each cluster is labeled " +
            "based on the proportion of training instances in each class that is assigned to it. " +
            "If the cluster is a probabilistic ones, cluster membership probabilities are " +
            "used instead of counts to establish class probability estimates.";
  }

  /**
   * Gets an enumeration describing the available options.
   * 
   * @return an enumeration of all the available options.
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>();

    result.addElement(new Option("\tFull name of clusterer.\n" + "\t(default: "
      + defaultClustererString() + ")", "W", 1, "-W"));

    result.addElement(new Option("\tWhether to label all clusters.\n", "label-all-clusters",
            0, "-label-all-clusters"));

    result.addAll(Collections.list(super.listOptions()));

    result.addElement(new Option("", "", 0, "\nOptions specific to clusterer "
      + m_Clusterer.getClass().getName() + ":"));

    result
      .addAll(Collections.list(((OptionHandler) m_Clusterer).listOptions()));

    return result.elements();
  }

  /**
   * returns the options of the current setup
   * 
   * @return the current options
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-W");
    result.add("" + getClusterer().getClass().getName());

    if (getLabelAllClusters()) {
      result.add("-label-all-clusters");
    }

    Collections.addAll(result, super.getOptions());

    if (getClusterer() instanceof OptionHandler) {
      String[] options = ((OptionHandler) getClusterer()).getOptions();
      if (options.length > 0) {
        result.add("--");
        Collections.addAll(result, options);
      }
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Parses the options for this object.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -output-debug-info
   *  If set, classifier is run in debug mode and
   *  may output additional info to the console
   * </pre>
   * 
   * <pre>
   * -W &lt;clusterer&gt
   *  Full name of clusterer.
   *  (default: weka.clusterers.SimpleKMeans)
   * </pre>
   *
   * <pre>
   * -label-all-clusters
   *  If set, all clusters are labeled probabilistically instead of just those ones that best fit a class
   * </pre>
   *
   * <pre>
   * Options specific to clusterer weka.clusterers.SimpleKMeans:
   * </pre>
   * 
   * <pre>
   * -N &lt;num&gt;
   *  number of clusters.
   *  (default 2).
   * </pre>
   * 
   * <pre>
   * -V
   *  Display std. deviations for centroids.
   * </pre>
   * 
   * <pre>
   * -M
   *  Replace missing values with mean/mode.
   * </pre>
   * 
   * <pre>
   * -S &lt;num&gt;
   *  Random number seed.
   *  (default 10)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the options to use
   * @throws Exception if setting of options fails
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    setLabelAllClusters(Utils.getFlag("label-all-clusters", options));
    String tmpStr = Utils.getOption('W', options);
    if (tmpStr.length() > 0) {
      setClusterer(AbstractClusterer.forName(tmpStr, null));
      setClusterer(AbstractClusterer.forName(tmpStr,
        Utils.partitionOptions(options)));
    } else {
      setClusterer(AbstractClusterer.forName(defaultClustererString(), null));
      setClusterer(AbstractClusterer.forName(defaultClustererString(),
        Utils.partitionOptions(options)));
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * String describing default clusterer.
   * 
   * @return the classname
   */
  protected String defaultClustererString() {
    return SimpleKMeans.class.getName();
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String clustererTipText() {
    return "The clusterer to be used.";
  }

  /**
   * Set the base clusterer.
   * 
   * @param value the clusterer to use.
   */
  public void setClusterer(Clusterer value) {
    m_Clusterer = value;
  }

  /**
   * Get the clusterer used as the base learner.
   * 
   * @return the current clusterer
   */
  public Clusterer getClusterer() {
    return m_Clusterer;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String labelAllClustersTipText() {
    return "If true, all clusters are labeled probabilistically instead of just those ones that best fit a class.";
  }

  /**
   * Whether to label all clusters probabilistically instead of just finding one cluster for each class.
   *
   * @param l set to true if all clusters are to be labeled
   */
  public void setLabelAllClusters(boolean l) {
    m_labelAllClusters = l;
  }


  /**
   * Get whether to label all clusters probabilistically instead of just finding one cluster for each class.
   *
   * @return true if all clusters are to be labeled
   */
  public boolean getLabelAllClusters() {
    return m_labelAllClusters;
  }

  /**
   * Returns class probability distribution for the given instance.
   * 
   * @param instance the instance to be classified
   * @return the class probabilities
   * @throws Exception if an error occurred during the prediction
   */
  @Override
  public double[] distributionForInstance(Instance instance) throws Exception {

    if (m_ZeroR != null) {
      return  m_ZeroR.distributionForInstance(instance);
    } else {
      double[] result = new double[instance.numClasses()];

      if (m_ActualClusterer != null) {
        // build new instance
        double[] values = new double[m_ClusteringHeader.numAttributes()];
        int n = 0;
        for (int i = 0; i < instance.numAttributes(); i++) {
          if (i == instance.classIndex()) {
            continue;
          }
          values[n] = instance.value(i);
          n++;
        }
        Instance newInst = new DenseInstance(instance.weight(), values);
        newInst.setDataset(m_ClusteringHeader);

        if (!getLabelAllClusters()) {

          // determine cluster/class
          double r = m_ClustersToClasses[m_ActualClusterer.clusterInstance(newInst)];
          if (r == -1) {
            return result; // Unclassified
          } else {
            result[(int)r] = 1.0;
            return result;
          }
        } else {
          double[] classProbs = new double[instance.numClasses()];
          double[] dist = m_ActualClusterer.distributionForInstance(newInst);
          for (int i = 0; i < dist.length; i++) {
            for (int j = 0; j < instance.numClasses(); j++) {
              classProbs[j] += dist[i] * m_ClusterClassProbs[i][j];
            }
          }
          Utils.normalize(classProbs);
          return classProbs;
        }
      } else {
        return result; // Unclassified
      }
    }
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result;

    result = m_Clusterer.getCapabilities();

    // class
    result.disableAllClasses();
    result.disable(Capability.NO_CLASS);
    result.disable(Capability.NUMERIC_CLASS);
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    result.setOwner(this);

    return result;
  }

  /**
   * builds the classifier
   * 
   * @param data the training instances
   * @throws Exception if something goes wrong
   */
  @Override
  public void buildClassifier(Instances data) throws Exception {

     // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // save original header (needed for clusters to classes output)
    m_OriginalHeader = new Instances(data, 0);

    // remove class attribute for clusterer
    Instances clusterData = new Instances(data);
    clusterData.setClassIndex(-1);
    clusterData.deleteAttributeAt(m_OriginalHeader.classIndex());
    m_ClusteringHeader = new Instances(clusterData, 0);

    if (m_ClusteringHeader.numAttributes() == 0) {
      System.err
        .println("Data contains only class attribute, defaulting to ZeroR model.");
      m_ZeroR = new ZeroR();
      m_ZeroR.buildClassifier(data);
    } else {
      m_ZeroR = null;

      // build clusterer
      m_ActualClusterer = AbstractClusterer.makeCopy(m_Clusterer);
      m_ActualClusterer.buildClusterer(clusterData);

      if (!getLabelAllClusters()) {

        // determine classes-to-clusters mapping
        ClusterEvaluation eval = new ClusterEvaluation();
        eval.setClusterer(m_ActualClusterer);
        eval.evaluateClusterer(clusterData);
        double[] clusterAssignments = eval.getClusterAssignments();
        int[][] counts  = new int[eval.getNumClusters()][m_OriginalHeader.numClasses()];
        int[] clusterTotals  = new int[eval.getNumClusters()];
        double[] best = new double[eval.getNumClusters() + 1];
        double[] current = new double[eval.getNumClusters() + 1];
        for (int i = 0; i < data.numInstances(); i++) {
          Instance instance = data.instance(i);
          if (!instance.classIsMissing()) {
            counts[(int) clusterAssignments[i]][(int) instance.classValue()]++;
            clusterTotals[(int) clusterAssignments[i]]++;
          }
        }
        best[eval.getNumClusters()] = Double.MAX_VALUE;
        ClusterEvaluation.mapClasses(eval.getNumClusters(), 0, counts,
                clusterTotals, current, best, 0);
        m_ClustersToClasses = new double[best.length];
        System.arraycopy(best, 0, m_ClustersToClasses, 0, best.length);
      } else {
        m_ClusterClassProbs = new double[m_ActualClusterer.numberOfClusters()][data.numClasses()];
        for (int i = 0; i < data.numInstances(); i++) {
          Instance clusterInstance = clusterData.instance(i);
          Instance originalInstance = data.instance(i);
          if (!originalInstance.classIsMissing()) {
            double[] probs = m_ActualClusterer.distributionForInstance(clusterInstance);
            for (int j = 0; j < probs.length; j++) {
              m_ClusterClassProbs[j][(int) originalInstance.classValue()] += probs[j];
            }
          }
        }
        for (int i = 0; i < m_ClusterClassProbs.length; i++) {
          Utils.normalize(m_ClusterClassProbs[i]);
        }
      }
    }
  }

  /**
   * Returns a string representation of the classifier.
   * 
   * @return a string representation of the classifier.
   */
  @Override
  public String toString() {
    StringBuffer result;
    int n;
    boolean found;

    result = new StringBuffer();

    // title
    result.append(this.getClass().getName().replaceAll(".*\\.", "") + "\n");
    result.append(this.getClass().getName().replaceAll(".*\\.", "")
      .replaceAll(".", "=")
      + "\n");

    // model
    if (m_ActualClusterer != null) {
      // output clusterer
      result.append(m_ActualClusterer + "\n");

      if (!getLabelAllClusters()) {

        // clusters to classes
        result.append("Clusters to classes mapping:\n");
        for (int i = 0; i < m_ClustersToClasses.length - 1; i++) {
          result.append("  " + (i + 1) + ". Cluster: ");
          if (m_ClustersToClasses[i] < 0) {
            result.append("no class");
          } else {
            result.append(m_OriginalHeader.classAttribute().value(
                    (int) m_ClustersToClasses[i])
                    + " (" + ((int) m_ClustersToClasses[i] + 1) + ")");
          }
          result.append("\n");
        }
        result.append("\n");

        // classes to clusters
        result.append("Classes to clusters mapping:\n");
        for (int i = 0; i < m_OriginalHeader.numClasses(); i++) {
          result.append("  " + (i + 1) + ". Class ("
                  + m_OriginalHeader.classAttribute().value(i) + "): ");

          found = false;
          for (n = 0; n < m_ClustersToClasses.length - 1; n++) {
            if (((int) m_ClustersToClasses[n]) == i) {
              found = true;
              result.append((n + 1) + ". Cluster");
              break;
            }
          }

          if (!found) {
            result.append("no cluster");
          }
          result.append("\n");
        }
      } else {
        for (int i = 0; i < m_ClusterClassProbs.length; i++) {
          result.append("Probabilities for cluster " + i + ":\n\n");
          for (int j = 0; j < m_ClusterClassProbs[i].length; j++) {
            result.append(m_OriginalHeader.classAttribute().value(j) + ":\t" +
                    Utils.doubleToString(m_ClusterClassProbs[i][j], getNumDecimalPlaces()) + "\n");
          }
          result.append("\n");
        }
      }

      result.append("\n");
    } else {
      result.append("no model built yet\n");
    }

    return result.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Runs the classifier with the given options
   * 
   * @param args the commandline options
   */
  public static void main(String[] args) {
    runClassifier(new ClassificationViaClustering(), args);
  }
}
