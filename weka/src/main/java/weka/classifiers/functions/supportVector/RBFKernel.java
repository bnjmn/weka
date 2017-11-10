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
 *    RBFKernel.java
 *    Copyright (C) 1999-2017 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.functions.supportVector;

import weka.core.*;
import weka.core.Capabilities.Capability;

/**
 * <!-- globalinfo-start -->
 * The RBF kernel : K(x, y) = exp(-gamma*(x-y)^2)
 * <br><br>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start -->
 * Valid options are: <p>
 * 
 * <pre> -C &lt;num&gt;
 *  The size of the cache (a prime number), 0 for full cache and 
 *  -1 to turn it off.
 *  (default: 250007)</pre>
 * 
 * <pre> -G &lt;double&gt;
 *  The value to use for the gamma parameter (default: 0.01).</pre>
 * 
 * <pre> -output-debug-info
 *  Enables debugging output (if available) to be printed.
 *  (default: off)</pre>
 *
 * <!-- options-end -->
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Shane Legg (shane@intelligenesis.net) (sparse vector code)
 * @author Stuart Inglis (stuart@reeltwo.com) (sparse vector code)
 * @version $Revision$
 */
public class RBFKernel extends CachedKernel {

  /** for serialization (value needs to be consistent with J. Lindgren's implementation) */
  static final long serialVersionUID = 5247117544316387852L;

  /** The gamma parameter for the RBF kernel. */
  protected double m_gamma = 0.01;

  /** The diagonal values of the dot product matrix (name needs to be consistent with J. Lindgren's implementation). */
  protected double[] m_kernelPrecalc;

  /**
   * default constructor - does nothing.
   */
  public RBFKernel() {
    super();
  }

  /**
   * Creates a new <code>RBFKernel</code> instance.
   * 
   * @param data the training dataset used.
   * @param cacheSize the size of the cache (a prime number)
   * @param gamma the gamma to use
   * @throws Exception if something goes wrong
   */
  public RBFKernel(Instances data, int cacheSize, double gamma) throws Exception {

    super();

    setCacheSize(cacheSize);
    setGamma(gamma);

    buildKernel(data);
  }

  /**
   * Builds the kernel. Calls the super class method and then also initializes the cache for
   * the diagonal of the dot product matrix.
   */
  public void buildKernel(Instances data) throws Exception {
    super.buildKernel(data);

    m_kernelPrecalc = new double[data.numInstances()];
    for (int i = 0; i < data.numInstances(); i++) {
      double sum = 0;
      Instance inst = data.instance(i);
      for (int j = 0; j < inst.numValues(); j++) {
        if (inst.index(j) != data.classIndex()) {
          sum += inst.valueSparse(j) * inst.valueSparse(j);
        }
      }
      m_kernelPrecalc[i] = sum;
    }
  }

  /**
   * Returns a string describing the kernel
   * 
   * @return a description suitable for displaying in the explorer/experimenter
   *         gui
   */
  @Override
  public String globalInfo() {
    return "The RBF kernel : K(x, y) = exp(-gamma*(x-y)^2)";
  }

  /**
   * 
   * @param id1 the index of instance 1
   * @param id2 the index of instance 2
   * @param inst1 the instance 1 object
   * @return the dot product
   * @throws Exception if something goes wrong
   */
  @Override
  protected double evaluate(int id1, int id2, Instance inst1) throws Exception {

    if (id1 == id2) {
      return 1.0;
    } else {
      if (id1 == -1) {
        return Math.exp(-m_gamma * (dotProd(inst1, inst1) - 2 * dotProd(inst1, m_data.instance(id2))
                + m_kernelPrecalc[id2]));
      } else {
        return Math.exp(-m_gamma * (m_kernelPrecalc[id1] - 2 * dotProd(inst1, m_data.instance(id2))
                + m_kernelPrecalc[id2]));
      }
    }
  }

  /**
   * Returns the Capabilities of this kernel.
   * 
   * @return the capabilities of this object
   * @see Capabilities
   */
  @Override
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enableAllClasses();
    result.enable(Capability.MISSING_CLASS_VALUES);
    result.enable(Capability.NO_CLASS);

    return result;
  }

  /**
   * Sets the gamma value.
   * 
   * @param value the gamma value
   */
  @OptionMetadata(description = "The value to use for the gamma parameter (default: 0.01).",
          displayName = "gamma", commandLineParamName = "G",
          commandLineParamSynopsis = "-G <double>", displayOrder = 1)
  public void setGamma(double value) {
    m_gamma = value;
  }

  /**
   * Gets the gamma value.
   * 
   * @return the gamma value
   */
  public double getGamma() {
    return m_gamma;
  }

  /**
   * Returns the tip text for this property
   * 
   * @return tip text for this property suitable for displaying in the
   *         explorer/experimenter gui
   */
  public String gammaTipText() {
    return "The gamma value.";
  }

  /**
   * returns a string representation for the Kernel
   * 
   * @return a string representaiton of the kernel
   */
  @Override
  public String toString() {
    return "RBF Kernel: K(x,y) = exp(-" + m_gamma + "*(x-y)^2)";
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
}

