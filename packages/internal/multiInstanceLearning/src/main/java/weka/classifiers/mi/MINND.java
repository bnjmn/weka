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
 * MINND.java
 * Copyright (C) 2005 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.mi;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Vector;

import weka.classifiers.AbstractClassifier;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.MultiInstanceCapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;

/**
 * <!-- globalinfo-start --> Multiple-Instance Nearest Neighbour with
 * Distribution learner.<br/>
 * <br/>
 * It uses gradient descent to find the weight for each dimension of each
 * exeamplar from the starting point of 1.0. In order to avoid overfitting, it
 * uses mean-square function (i.e. the Euclidean distance) to search for the
 * weights.<br/>
 * It then uses the weights to cleanse the training data. After that it searches
 * for the weights again from the starting points of the weights searched
 * before.<br/>
 * Finally it uses the most updated weights to cleanse the test exemplar and
 * then finds the nearest neighbour of the test exemplar using partly-weighted
 * Kullback distance. But the variances in the Kullback distance are the ones
 * before cleansing.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * Xin Xu (2001). A nearest distribution approach to multiple-instance learning.
 * Hamilton, NZ.
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- technical-bibtex-start --> BibTeX:
 * 
 * <pre>
 * &#64;misc{Xu2001,
 *    address = {Hamilton, NZ},
 *    author = {Xin Xu},
 *    note = {0657.591B},
 *    school = {University of Waikato},
 *    title = {A nearest distribution approach to multiple-instance learning},
 *    year = {2001}
 * }
 * </pre>
 * <p/>
 * <!-- technical-bibtex-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -K &lt;number of neighbours&gt;
 *  Set number of nearest neighbour for prediction
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -S &lt;number of neighbours&gt;
 *  Set number of nearest neighbour for cleansing the training data
 *  (default 1)
 * </pre>
 * 
 * <pre>
 * -E &lt;number of neighbours&gt;
 *  Set number of nearest neighbour for cleansing the testing data
 *  (default 1)
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Xin Xu (xx5@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class MINND extends AbstractClassifier implements OptionHandler,
  MultiInstanceCapabilitiesHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -4512599203273864994L;

  /** The number of nearest neighbour for prediction */
  protected int m_Neighbour = 1;

  /** The mean for each attribute of each exemplar */
  protected double[][] m_Mean = null;

  /** The variance for each attribute of each exemplar */
  protected double[][] m_Variance = null;

  /** The dimension of each exemplar, i.e. (numAttributes-2) */
  protected int m_Dimension = 0;

  /** header info of the data */
  protected Instances m_Attributes;;

  /** The class label of each exemplar */
  protected double[] m_Class = null;

  /** The number of class labels in the data */
  protected int m_NumClasses = 0;

  /** The weight of each exemplar */
  protected double[] m_Weights = null;

  /** The very small number representing zero */
  static private double m_ZERO = 1.0e-45;

  /** The learning rate in the gradient descent */
  protected double m_Rate = -1;

  /** The minimum values for numeric attributes. */
  private double[] m_MinArray = null;

  /** The maximum values for numeric attributes. */
  private double[] m_MaxArray = null;

  /** The stopping criteria of gradient descent */
  private final double m_STOP = 1.0e-45;

  /** The weights that alter the dimnesion of each exemplar */
  private double[][] m_Change = null;

  /** The noise data of each exemplar */
  private double[][] m_NoiseM = null, m_NoiseV = null, m_ValidM = null,
    m_ValidV = null;

  /**
   * The number of nearest neighbour instances in the selection of noises in the
   * training data
   */
  private int m_Select = 1;

  /**
   * The number of nearest neighbour exemplars in the selection of noises in the
   * test data
   */
  private int m_Choose = 1;

  /** The decay rate of learning rate */
  private final double m_Decay = 0.5;

  /**
   * Returns a string describing this filter
   * 
   * @return a description of the filter suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String globalInfo() {
    return "Multiple-Instance Nearest Neighbour with Distribution learner.\n\n"
      + "It uses gradient descent to find the weight for each dimension of "
      + "each exeamplar from the starting point of 1.0. In order to avoid "
      + "overfitting, it uses mean-square function (i.e. the Euclidean "
      + "distance) to search for the weights.\n "
      + "It then uses the weights to cleanse the training data. After that "
      + "it searches for the weights again from the starting points of the "
      + "weights searched before.\n "
      + "Finally it uses the most updated weights to cleanse the test exemplar "
      + "and then finds the nearest neighbour of the test exemplar using "
      + "partly-weighted Kullback distance. But the variances in the Kullback "
      + "distance are the ones before cleansing.\n\n"
      + "For more information see:\n\n" + getTechnicalInformation().toString();
  }

  /**
   * Returns an instance of a TechnicalInformation object, containing detailed
   * information about the technical background of this class, e.g., paper
   * reference or book this class is based on.
   * 
   * @return the technical information about this class
   */
  @Override
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation result;

    result = new TechnicalInformation(Type.MISC);
    result.setValue(Field.AUTHOR, "Xin Xu");
    result.setValue(Field.YEAR, "2001");
    result.setValue(Field.TITLE,
      "A nearest distribution approach to multiple-instance learning");
    result.setValue(Field.SCHOOL, "University of Waikato");
    result.setValue(Field.ADDRESS, "Hamilton, NZ");
    result.setValue(Field.NOTE, "0657.591B");

    return result;
  }

  /**
   * Returns default capabilities of the classifier.
   * 
   * @return the capabilities of this classifier
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.RELATIONAL_ATTRIBUTES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // other
    result.enable(Capability.ONLY_MULTIINSTANCE);

    return result;
  }

  /**
   * Returns the capabilities of this multi-instance classifier for the
   * relational data.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getMultiInstanceCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.disableAllClasses();
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * As normal Nearest Neighbour algorithm does, it's lazy and simply records
   * the exemplar information (i.e. mean and variance for each dimension of each
   * exemplar and their classes) when building the model. There is actually no
   * need to store the exemplars themselves.
   * 
   * @param exs the training exemplars
   * @throws Exception if the model cannot be built properly
   */
  @Override
  public void buildClassifier(Instances exs) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(exs);

    // remove instances with missing class
    Instances newData = new Instances(exs);
    newData.deleteWithMissingClass();

    int numegs = newData.numInstances();
    m_Dimension = newData.attribute(1).relation().numAttributes();
    m_Attributes = newData.stringFreeStructure();
    m_Change = new double[numegs][m_Dimension];
    m_NumClasses = exs.numClasses();
    m_Mean = new double[numegs][m_Dimension];
    m_Variance = new double[numegs][m_Dimension];
    m_Class = new double[numegs];
    m_Weights = new double[numegs];
    m_NoiseM = new double[numegs][m_Dimension];
    m_NoiseV = new double[numegs][m_Dimension];
    m_ValidM = new double[numegs][m_Dimension];
    m_ValidV = new double[numegs][m_Dimension];
    m_MinArray = new double[m_Dimension];
    m_MaxArray = new double[m_Dimension];
    for (int v = 0; v < m_Dimension; v++) {
      m_MinArray[v] = m_MaxArray[v] = Double.NaN;
    }

    for (int w = 0; w < numegs; w++) {
      updateMinMax(newData.instance(w));
    }

    // Scale exemplars
    Instances data = m_Attributes;

    for (int x = 0; x < numegs; x++) {
      Instance example = newData.instance(x);
      example = scale(example);
      for (int i = 0; i < m_Dimension; i++) {
        m_Mean[x][i] = example.relationalValue(1).meanOrMode(i);
        m_Variance[x][i] = example.relationalValue(1).variance(i);
        if (Utils.eq(m_Variance[x][i], 0.0)) {
          m_Variance[x][i] = m_ZERO;
        }
        m_Change[x][i] = 1.0;
      }
      /*
       * for(int y=0; y < m_Variance[x].length; y++){
       * if(Utils.eq(m_Variance[x][y],0.0)) m_Variance[x][y] = m_ZERO;
       * m_Change[x][y] = 1.0; }
       */

      data.add(example);
      m_Class[x] = example.classValue();
      m_Weights[x] = example.weight();
    }

    for (int z = 0; z < numegs; z++) {
      findWeights(z, m_Mean);
    }

    // Pre-process and record "true estimated" parameters for distributions
    for (int x = 0; x < numegs; x++) {
      Instance example = preprocess(data, x);
      if (getDebug()) {
        System.out
          .println("???Exemplar " + x + " has been pre-processed:"
            + data.instance(x).relationalValue(1).sumOfWeights() + "|"
            + example.relationalValue(1).sumOfWeights() + "; class:"
            + m_Class[x]);
      }
      if (Utils.gr(example.relationalValue(1).sumOfWeights(), 0)) {
        for (int i = 0; i < m_Dimension; i++) {
          m_ValidM[x][i] = example.relationalValue(1).meanOrMode(i);
          m_ValidV[x][i] = example.relationalValue(1).variance(i);
          if (Utils.eq(m_ValidV[x][i], 0.0)) {
            m_ValidV[x][i] = m_ZERO;
          }
        }
        /*
         * for(int y=0; y < m_ValidV[x].length; y++){
         * if(Utils.eq(m_ValidV[x][y],0.0)) m_ValidV[x][y] = m_ZERO; }
         */
      } else {
        m_ValidM[x] = null;
        m_ValidV[x] = null;
      }
    }

    for (int z = 0; z < numegs; z++) {
      if (m_ValidM[z] != null) {
        findWeights(z, m_ValidM);
      }
    }

  }

  /**
   * Pre-process the given exemplar according to the other exemplars in the
   * given exemplars. It also updates noise data statistics.
   * 
   * @param data the whole exemplars
   * @param pos the position of given exemplar in data
   * @return the processed exemplar
   * @throws Exception if the returned exemplar is wrong
   */
  public Instance preprocess(Instances data, int pos) throws Exception {
    Instance before = data.instance(pos);
    if ((int) before.classValue() == 0) {
      m_NoiseM[pos] = null;
      m_NoiseV[pos] = null;
      return before;
    }

    Instances after_relationInsts = before.attribute(1).relation()
      .stringFreeStructure();
    Instances noises_relationInsts = before.attribute(1).relation()
      .stringFreeStructure();

    Instances newData = m_Attributes;
    Instance after = new DenseInstance(before.numAttributes());
    Instance noises = new DenseInstance(before.numAttributes());
    after.setDataset(newData);
    noises.setDataset(newData);

    for (int g = 0; g < before.relationalValue(1).numInstances(); g++) {
      Instance datum = before.relationalValue(1).instance(g);
      double[] dists = new double[data.numInstances()];

      for (int i = 0; i < data.numInstances(); i++) {
        if (i != pos) {
          dists[i] = distance(datum, m_Mean[i], m_Variance[i], i);
        } else {
          dists[i] = Double.POSITIVE_INFINITY;
        }
      }

      int[] pred = new int[m_NumClasses];
      for (int n = 0; n < pred.length; n++) {
        pred[n] = 0;
      }

      for (int o = 0; o < m_Select; o++) {
        int index = Utils.minIndex(dists);
        pred[(int) m_Class[index]]++;
        dists[index] = Double.POSITIVE_INFINITY;
      }

      int clas = Utils.maxIndex(pred);
      if ((int) before.classValue() != clas) {
        noises_relationInsts.add(datum);
      } else {
        after_relationInsts.add(datum);
      }
    }

    int relationValue;
    relationValue = noises.attribute(1).addRelation(noises_relationInsts);
    noises.setValue(0, before.value(0));
    noises.setValue(1, relationValue);
    noises.setValue(2, before.classValue());

    relationValue = after.attribute(1).addRelation(after_relationInsts);
    after.setValue(0, before.value(0));
    after.setValue(1, relationValue);
    after.setValue(2, before.classValue());

    if (Utils.gr(noises.relationalValue(1).sumOfWeights(), 0)) {
      for (int i = 0; i < m_Dimension; i++) {
        m_NoiseM[pos][i] = noises.relationalValue(1).meanOrMode(i);
        m_NoiseV[pos][i] = noises.relationalValue(1).variance(i);
        if (Utils.eq(m_NoiseV[pos][i], 0.0)) {
          m_NoiseV[pos][i] = m_ZERO;
        }
      }
      /*
       * for(int y=0; y < m_NoiseV[pos].length; y++){
       * if(Utils.eq(m_NoiseV[pos][y],0.0)) m_NoiseV[pos][y] = m_ZERO; }
       */
    } else {
      m_NoiseM[pos] = null;
      m_NoiseV[pos] = null;
    }

    return after;
  }

  /**
   * Calculates the distance between two instances
   * 
   * @param first the first instance
   * @param second the second instance
   * @return the distance between the two given instances
   */
  private double distance(Instance first, double[] mean, double[] var, int pos) {

    double diff, distance = 0;

    for (int i = 0; i < m_Dimension; i++) {
      // If attribute is numeric
      if (first.attribute(i).isNumeric()) {
        if (!first.isMissing(i)) {
          diff = first.value(i) - mean[i];
          if (Utils.gr(var[i], m_ZERO)) {
            distance += m_Change[pos][i] * var[i] * diff * diff;
          } else {
            distance += m_Change[pos][i] * diff * diff;
          }
        } else {
          if (Utils.gr(var[i], m_ZERO)) {
            distance += m_Change[pos][i] * var[i];
          } else {
            distance += m_Change[pos][i] * 1.0;
          }
        }
      }

    }

    return distance;
  }

  /**
   * Updates the minimum and maximum values for all the attributes based on a
   * new exemplar.
   * 
   * @param ex the new exemplar
   */
  private void updateMinMax(Instance ex) {
    Instances insts = ex.relationalValue(1);
    for (int j = 0; j < m_Dimension; j++) {
      if (insts.attribute(j).isNumeric()) {
        for (int k = 0; k < insts.numInstances(); k++) {
          Instance ins = insts.instance(k);
          if (!ins.isMissing(j)) {
            if (Double.isNaN(m_MinArray[j])) {
              m_MinArray[j] = ins.value(j);
              m_MaxArray[j] = ins.value(j);
            } else {
              if (ins.value(j) < m_MinArray[j]) {
                m_MinArray[j] = ins.value(j);
              } else if (ins.value(j) > m_MaxArray[j]) {
                m_MaxArray[j] = ins.value(j);
              }
            }
          }
        }
      }
    }
  }

  /**
   * Scale the given exemplar so that the returned exemplar has the value of 0
   * to 1 for each dimension
   * 
   * @param before the given exemplar
   * @return the resultant exemplar after scaling
   * @throws Exception if given exampler cannot be scaled properly
   */
  private Instance scale(Instance before) throws Exception {

    Instances afterInsts = before.relationalValue(1).stringFreeStructure();
    Instance after = new DenseInstance(before.numAttributes());
    after.setDataset(m_Attributes);

    for (int i = 0; i < before.relationalValue(1).numInstances(); i++) {
      Instance datum = before.relationalValue(1).instance(i);
      Instance inst = (Instance) datum.copy();

      for (int j = 0; j < m_Dimension; j++) {
        if (before.relationalValue(1).attribute(j).isNumeric()) {
          inst.setValue(j, (datum.value(j) - m_MinArray[j])
            / (m_MaxArray[j] - m_MinArray[j]));
        }
      }
      afterInsts.add(inst);
    }

    int attValue = after.attribute(1).addRelation(afterInsts);
    after.setValue(0, before.value(0));
    after.setValue(1, attValue);
    after.setValue(2, before.value(2));

    return after;
  }

  /**
   * Use gradient descent to distort the MU parameter for the exemplar. The
   * exemplar can be in the specified row in the given matrix, which has
   * numExemplar rows and numDimension columns; or not in the matrix.
   * 
   * @param row the given row index
   * @param mean
   */
  public void findWeights(int row, double[][] mean) {

    double[] neww = new double[m_Dimension];
    double[] oldw = new double[m_Dimension];
    System.arraycopy(m_Change[row], 0, neww, 0, m_Dimension);
    // for(int z=0; z<m_Dimension; z++)
    // System.out.println("mu("+row+"): "+origin[z]+" | "+newmu[z]);
    double newresult = target(neww, mean, row, m_Class);
    double result = Double.POSITIVE_INFINITY;
    double rate = 0.05;
    if (m_Rate != -1) {
      rate = m_Rate;
    }
    // System.out.println("???Start searching ...");
    search: while (Utils.gr((result - newresult), m_STOP)) { // Full step
      oldw = neww;
      neww = new double[m_Dimension];

      double[] delta = delta(oldw, mean, row, m_Class);

      for (int i = 0; i < m_Dimension; i++) {
        if (Utils.gr(m_Variance[row][i], 0.0)) {
          neww[i] = oldw[i] + rate * delta[i];
        }
      }

      result = newresult;
      newresult = target(neww, mean, row, m_Class);

      // System.out.println("???old: "+result+"|new: "+newresult);
      while (Utils.gr(newresult, result)) { // Search back
        // System.out.println("search back");
        if (m_Rate == -1) {
          rate *= m_Decay; // Decay
          for (int i = 0; i < m_Dimension; i++) {
            if (Utils.gr(m_Variance[row][i], 0.0)) {
              neww[i] = oldw[i] + rate * delta[i];
            }
          }
          newresult = target(neww, mean, row, m_Class);
        } else {
          for (int i = 0; i < m_Dimension; i++) {
            neww[i] = oldw[i];
          }
          break search;
        }
      }
    }
    // System.out.println("???Stop");
    m_Change[row] = neww;
  }

  /**
   * Delta of x in one step of gradient descent: delta(Wij) = 1/2 * sum[k=1..N,
   * k!=i](sqrt(P)*(Yi-Yk)/D - 1) * (MUij - MUkj)^2 where D =
   * sqrt(sum[j=1..P]Kkj(MUij - MUkj)^2) N is number of exemplars and P is
   * number of dimensions
   * 
   * @param x the weights of the exemplar in question
   * @param rowpos row index of x in X
   * @param Y the observed class label
   * @return the delta for all dimensions
   */
  private double[] delta(double[] x, double[][] X, int rowpos, double[] Y) {
    double y = Y[rowpos];

    double[] delta = new double[m_Dimension];
    for (int h = 0; h < m_Dimension; h++) {
      delta[h] = 0.0;
    }

    for (int i = 0; i < X.length; i++) {
      if ((i != rowpos) && (X[i] != null)) {
        double var = (y == Y[i]) ? 0.0 : Math.sqrt((double) m_Dimension - 1);
        double distance = 0;
        for (int j = 0; j < m_Dimension; j++) {
          if (Utils.gr(m_Variance[rowpos][j], 0.0)) {
            distance += x[j] * (X[rowpos][j] - X[i][j])
              * (X[rowpos][j] - X[i][j]);
          }
        }
        distance = Math.sqrt(distance);
        if (distance != 0) {
          for (int k = 0; k < m_Dimension; k++) {
            if (m_Variance[rowpos][k] > 0.0) {
              delta[k] += (var / distance - 1.0) * 0.5
                * (X[rowpos][k] - X[i][k]) * (X[rowpos][k] - X[i][k]);
            }
          }
        }
      }
    }
    // System.out.println("???delta: "+delta);
    return delta;
  }

  /**
   * Compute the target function to minimize in gradient descent The formula is:<br/>
   * 1/2*sum[i=1..p](f(X, Xi)-var(Y, Yi))^2
   * <p/>
   * where p is the number of exemplars and Y is the class label. In the case of
   * X=MU, f() is the Euclidean distance between two exemplars together with the
   * related weights and var() is sqrt(numDimension)*(Y-Yi) where Y-Yi is either
   * 0 (when Y==Yi) or 1 (Y!=Yi)
   * 
   * @param x the weights of the exemplar in question
   * @param rowpos row index of x in X
   * @param Y the observed class label
   * @return the result of the target function
   */
  public double target(double[] x, double[][] X, int rowpos, double[] Y) {
    double y = Y[rowpos], result = 0;

    for (int i = 0; i < X.length; i++) {
      if ((i != rowpos) && (X[i] != null)) {
        double var = (y == Y[i]) ? 0.0 : Math.sqrt((double) m_Dimension - 1);
        double f = 0;
        for (int j = 0; j < m_Dimension; j++) {
          if (Utils.gr(m_Variance[rowpos][j], 0.0)) {
            f += x[j] * (X[rowpos][j] - X[i][j]) * (X[rowpos][j] - X[i][j]);
            // System.out.println("i:"+i+" j: "+j+" row: "+rowpos);
          }
        }
        f = Math.sqrt(f);
        // System.out.println("???distance between "+rowpos+" and "+i+": "+f+"|y:"+y+" vs "+Y[i]);
        if (Double.isInfinite(f)) {
          System.exit(1);
        }
        result += 0.5 * (f - var) * (f - var);
      }
    }
    // System.out.println("???target: "+result);
    return result;
  }

  /**
   * Use Kullback Leibler distance to find the nearest neighbours of the given
   * exemplar. It also uses K-Nearest Neighbour algorithm to classify the test
   * exemplar
   * 
   * @param ex the given test exemplar
   * @return the classification
   * @throws Exception if the exemplar could not be classified successfully
   */
  @Override
  public double classifyInstance(Instance ex) throws Exception {

    ex = scale(ex);

    double[] var = new double[m_Dimension];
    for (int i = 0; i < m_Dimension; i++) {
      var[i] = ex.relationalValue(1).variance(i);
    }

    // The Kullback distance to all exemplars
    double[] kullback = new double[m_Class.length];

    // The first K nearest neighbours' predictions */
    double[] predict = new double[m_NumClasses];
    for (int h = 0; h < predict.length; h++) {
      predict[h] = 0;
    }
    ex = cleanse(ex);

    if (ex.relationalValue(1).numInstances() == 0) {
      if (getDebug()) {
        System.out.println("???Whole exemplar falls into ambiguous area!");
      }
      return 1.0; // Bias towards positive class
    }

    double[] mean = new double[m_Dimension];
    for (int i = 0; i < m_Dimension; i++) {
      mean[i] = ex.relationalValue(1).meanOrMode(i);
    }

    // Avoid zero sigma
    for (int h = 0; h < var.length; h++) {
      if (Utils.eq(var[h], 0.0)) {
        var[h] = m_ZERO;
      }
    }

    for (int i = 0; i < m_Class.length; i++) {
      if (m_ValidM[i] != null) {
        kullback[i] = kullback(mean, m_ValidM[i], var, m_Variance[i], i);
      } else {
        kullback[i] = Double.POSITIVE_INFINITY;
      }
    }

    for (int j = 0; j < m_Neighbour; j++) {
      int pos = Utils.minIndex(kullback);
      predict[(int) m_Class[pos]] += m_Weights[pos];
      kullback[pos] = Double.POSITIVE_INFINITY;
    }

    if (getDebug()) {
      System.out
        .println("???There are still some unambiguous instances in this exemplar! Predicted as: "
          + Utils.maxIndex(predict));
    }
    return Utils.maxIndex(predict);
  }

  /**
   * Cleanse the given exemplar according to the valid and noise data statistics
   * 
   * @param before the given exemplar
   * @return the processed exemplar
   * @throws Exception if the returned exemplar is wrong
   */
  public Instance cleanse(Instance before) throws Exception {

    Instances insts = before.relationalValue(1).stringFreeStructure();
    Instance after = new DenseInstance(before.numAttributes());
    after.setDataset(m_Attributes);

    for (int g = 0; g < before.relationalValue(1).numInstances(); g++) {
      Instance datum = before.relationalValue(1).instance(g);
      double[] minNoiDists = new double[m_Choose];
      double[] minValDists = new double[m_Choose];
      // int noiseCount = 0, validCount = 0; NOT USED
      double[] nDist = new double[m_Mean.length];
      double[] vDist = new double[m_Mean.length];

      for (int h = 0; h < m_Mean.length; h++) {
        if (m_ValidM[h] == null) {
          vDist[h] = Double.POSITIVE_INFINITY;
        } else {
          vDist[h] = distance(datum, m_ValidM[h], m_ValidV[h], h);
        }

        if (m_NoiseM[h] == null) {
          nDist[h] = Double.POSITIVE_INFINITY;
        } else {
          nDist[h] = distance(datum, m_NoiseM[h], m_NoiseV[h], h);
        }
      }

      for (int k = 0; k < m_Choose; k++) {
        int pos = Utils.minIndex(vDist);
        minValDists[k] = vDist[pos];
        vDist[pos] = Double.POSITIVE_INFINITY;
        pos = Utils.minIndex(nDist);
        minNoiDists[k] = nDist[pos];
        nDist[pos] = Double.POSITIVE_INFINITY;
      }

      int x = 0, y = 0;
      while ((x + y) < m_Choose) {
        if (minValDists[x] <= minNoiDists[y]) {
          // validCount++; NOT USED
          x++;
        } else {
          // noiseCount++; NOT USED
          y++;
        }
      }
      if (x >= y) {
        insts.add(datum);
      }

    }

    after.setValue(0, before.value(0));
    after.setValue(1, after.attribute(1).addRelation(insts));
    after.setValue(2, before.value(2));

    return after;
  }

  /**
   * This function calculates the Kullback Leibler distance between two normal
   * distributions. This distance is always positive. Kullback Leibler distance
   * = integral{f(X)ln(f(X)/g(X))} Note that X is a vector. Since we assume
   * dimensions are independent f(X)(g(X) the same) is actually the product of
   * normal density functions of each dimensions. Also note that it should be
   * log2 instead of (ln) in the formula, but we use (ln) simply for
   * computational convenience.
   * 
   * The result is as follows, suppose there are P dimensions, and f(X) is the
   * first distribution and g(X) is the second: Kullback =
   * sum[1..P](ln(SIGMA2/SIGMA1)) + sum[1..P](SIGMA1^2 / (2*(SIGMA2^2))) +
   * sum[1..P]((MU1-MU2)^2 / (2*(SIGMA2^2))) - P/2
   * 
   * @param mu1 mu of the first normal distribution
   * @param mu2 mu of the second normal distribution
   * @param var1 variance(SIGMA^2) of the first normal distribution
   * @param var2 variance(SIGMA^2) of the second normal distribution
   * @return the Kullback distance of two distributions
   */
  public double kullback(double[] mu1, double[] mu2, double[] var1,
    double[] var2, int pos) {
    int p = mu1.length;
    double result = 0;

    for (int y = 0; y < p; y++) {
      if ((Utils.gr(var1[y], 0)) && (Utils.gr(var2[y], 0))) {
        result += ((Math.log(Math.sqrt(var2[y] / var1[y])))
          + (var1[y] / (2.0 * var2[y]))
          + (m_Change[pos][y] * (mu1[y] - mu2[y]) * (mu1[y] - mu2[y]) / (2.0 * var2[y])) - 0.5);
      }
    }

    return result;
  }

  /**
   * Returns an enumeration describing the available options
   * 
   * @return an enumeration of all the available options
   */
  @Override
  public Enumeration<Option> listOptions() {

    Vector<Option> result = new Vector<Option>(3);

    result.addElement(new Option(
      "\tSet number of nearest neighbour for prediction\n" + "\t(default 1)",
      "K", 1, "-K <number of neighbours>"));

    result.addElement(new Option(
      "\tSet number of nearest neighbour for cleansing the training data\n"
        + "\t(default 1)", "S", 1, "-S <number of neighbours>"));

    result.addElement(new Option(
      "\tSet number of nearest neighbour for cleansing the testing data\n"
        + "\t(default 1)", "E", 1, "-E <number of neighbours>"));

    result.addAll(Collections.list(super.listOptions()));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   * <p/>
   * 
   * <!-- options-start --> Valid options are:
   * <p/>
   * 
   * <pre>
   * -K &lt;number of neighbours&gt;
   *  Set number of nearest neighbour for prediction
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -S &lt;number of neighbours&gt;
   *  Set number of nearest neighbour for cleansing the training data
   *  (default 1)
   * </pre>
   * 
   * <pre>
   * -E &lt;number of neighbours&gt;
   *  Set number of nearest neighbour for cleansing the testing data
   *  (default 1)
   * </pre>
   * 
   * <!-- options-end -->
   * 
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  @Override
  public void setOptions(String[] options) throws Exception {

    String numNeighbourString = Utils.getOption('K', options);
    if (numNeighbourString.length() != 0) {
      setNumNeighbours(Integer.parseInt(numNeighbourString));
    } else {
      setNumNeighbours(1);
    }

    numNeighbourString = Utils.getOption('S', options);
    if (numNeighbourString.length() != 0) {
      setNumTrainingNoises(Integer.parseInt(numNeighbourString));
    } else {
      setNumTrainingNoises(1);
    }

    numNeighbourString = Utils.getOption('E', options);
    if (numNeighbourString.length() != 0) {
      setNumTestingNoises(Integer.parseInt(numNeighbourString));
    } else {
      setNumTestingNoises(1);
    }

    super.setOptions(options);

    Utils.checkForRemainingOptions(options);
  }

  /**
   * Gets the current settings of the Classifier.
   * 
   * @return an array of strings suitable for passing to setOptions
   */
  @Override
  public String[] getOptions() {

    Vector<String> result = new Vector<String>();

    result.add("-K");
    result.add("" + getNumNeighbours());

    result.add("-S");
    result.add("" + getNumTrainingNoises());

    result.add("-E");
    result.add("" + getNumTestingNoises());

    Collections.addAll(result, super.getOptions());

    return result.toArray(new String[result.size()]);
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numNeighboursTipText() {
    return "The number of nearest neighbours to the estimate the class prediction of test bags.";
  }

  /**
   * Sets the number of nearest neighbours to estimate the class prediction of
   * tests bags
   * 
   * @param numNeighbour the number of citers
   */
  public void setNumNeighbours(int numNeighbour) {
    m_Neighbour = numNeighbour;
  }

  /**
   * Returns the number of nearest neighbours to estimate the class prediction
   * of tests bags
   * 
   * @return the number of neighbours
   */
  public int getNumNeighbours() {
    return m_Neighbour;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numTrainingNoisesTipText() {
    return "The number of nearest neighbour instances in the selection of noises in the training data.";
  }

  /**
   * Sets the number of nearest neighbour instances in the selection of noises
   * in the training data
   * 
   * @param numTraining the number of noises in training data
   */
  public void setNumTrainingNoises(int numTraining) {
    m_Select = numTraining;
  }

  /**
   * Returns the number of nearest neighbour instances in the selection of
   * noises in the training data
   * 
   * @return the number of noises in training data
   */
  public int getNumTrainingNoises() {
    return m_Select;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String numTestingNoisesTipText() {
    return "The number of nearest neighbour instances in the selection of noises in the test data.";
  }

  /**
   * Returns The number of nearest neighbour instances in the selection of
   * noises in the test data
   * 
   * @return the number of noises in test data
   */
  public int getNumTestingNoises() {
    return m_Choose;
  }

  /**
   * Sets The number of nearest neighbour exemplars in the selection of noises
   * in the test data
   * 
   * @param numTesting the number of noises in test data
   */
  public void setNumTestingNoises(int numTesting) {
    m_Choose = numTesting;
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
   * Main method for testing.
   * 
   * @param args the options for the classifier
   */
  public static void main(String[] args) {
    runClassifier(new MINND(), args);
  }
}
