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
 *    ClojureClassifier.java
 *    Copyright (C) 2013 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.clojure;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

import mikera.cljutils.Clojure;
import weka.classifiers.AbstractClassifier;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;
import clojure.lang.IFn;

/**
 * <!-- globalinfo-start --> A wrapper classifier for executing a classifier
 * implemented in Clojure. The Clojure implementation is expected to have at
 * least the following two functions:<br/>
 * <br/>
 * learn-classifier [insts options-string]<br/>
 * distribution-for-instance [inst model]<br/>
 * <br/>
 * The learn-classifier function takes an Instances object and an options string
 * (which can be null). It is expected to return the learned model as some kind
 * of serializable data structure. The distribution-for-instance function takes
 * a Instance to be predicted and the model as arguments and returns the
 * prediction as an array of doubles.<br/>
 * <br/>
 * The Clojure implementation can optionally provide a model-to-string [model
 * header] function to return a textual description of the model. See the
 * weka-clj.simple classifier included in the source code of the
 * clojureClassifier package
 * <p/>
 * <!-- globalinfo-end -->
 * 
 * <!-- options-start --> Valid options are:
 * <p/>
 * 
 * <pre>
 * -N &lt;namespace&gt;
 *  Namespace of the Clojure classifier to use.
 * </pre>
 * 
 * <!-- options-end -->
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public class ClojureClassifier extends AbstractClassifier implements
  OptionHandler, Serializable {

  /** For serialization */
  private static final long serialVersionUID = 7912299994165292964L;

  /**
   * The namespace of the Clojure classifier to use (weka-clj.simple is a simple
   * majority class classifier included with the clojureClassifier package)
   */
  protected String m_namespace = "weka-clj.simple";

  /** Options to pass to the Clojure classifier */
  protected String m_schemeOptions = "";

  /** Holds the model learned by the Clojure classifier */
  protected Object m_model;

  /** Reference to the distribution-for-instance function */
  protected transient IFn m_distributionForInstanceFunction;

  protected transient boolean m_clojureRequire;

  /** Training data header */
  protected Instances m_header;

  public String globalInfo() {
    return "A wrapper classifier for executing a classifier implemented in Clojure. "
      + "The Clojure implementation is expected to have at least the following "
      + "two functions:\n\nlearn-classifier [insts options-string]\n"
      + "distribution-for-instance [inst model]\n\n"
      + "The learn-classifier function takes an Instances object and an options "
      + "string (which can be null). It is expected to return the learned model "
      + "as some kind of serializable data structure. The distribution-for-instance "
      + "function takes a Instance to be predicted and the model as arguments and "
      + "returns the prediction as an array of doubles.\n\nThe Clojure implementation "
      + "can optionally provide a model-to-string [model header] function to return "
      + "a textual description of the model. See the weka-clj.simple classifier "
      + "included in the source code of the clojureClassifier package.\n\nYour own "
      + "Clojure classifier only needs to be available on the classpath to be "
      + "accessible to the ClojureClassifier. The easiest thing to do is to place "
      + "the jar file containing your Clojure implementation in the lib directory "
      + "of the clojureClassifier package (i.e. in ${user.home}/wekafiles/packages/"
      + "clojureClassifier/lib";
  }

  @Override
  public Enumeration<Option> listOptions() {
    Vector<Option> result = new Vector<Option>();

    result.add(new Option("\tNamespace of the Clojure classifier to use.", "N",
      1, "-N <namespace>"));

    return result.elements();
  }

  @Override
  public void setOptions(String[] options) throws Exception {
    String ns = Utils.getOption('N', options);

    if (ns.length() > 0) {
      setNamespace(ns);
    }

    // scheme options after the --
    String[] schemeOpts = Utils.partitionOptions(options);
    if (schemeOpts != null && schemeOpts.length > 0) {
      String sO = Utils.joinOptions(schemeOpts);
      setSchemeOptions(sO);
    }
  }

  @Override
  public String[] getOptions() {
    Vector<String> result = new Vector<String>();

    if (m_namespace != null && m_namespace.length() > 0) {
      result.add("-N");
      result.add(getNamespace());
    }

    if (m_schemeOptions != null && m_schemeOptions.length() > 0) {
      result.add("--");
      result.add(m_schemeOptions);
    }

    return result.toArray(new String[result.size()]);
  }

  /**
   * Tip text for displaying in the GUI
   * 
   * @return the tip text for this property
   */
  public String namespaceTipText() {
    return "The namespace of the Clojure classifier to use. weka-clj.simple"
      + " is a simple Clojure implementation of majority class classifier "
      + "included with the package.";
  }

  /**
   * Set the namespace of the Clojure classifier to use
   * 
   * @param ns the namspace of the Clojure classifier
   */
  public void setNamespace(String ns) {
    m_namespace = ns;
  }

  /**
   * Get the namespace of the Clojure classifier to use
   * 
   * @return the namspace of the Clojure classifier
   */
  public String getNamespace() {
    return m_namespace;
  }

  /**
   * Tip text for displaying in the GUI
   * 
   * @return the tip text for this property
   */
  public String schemeOptionsTipText() {
    return "The options to pass to the Clojure classifier";
  }

  /**
   * Set the options to pass to the Clojure classifier
   * 
   * @param s the options (as a string) to pass to the Clojure classifier
   */
  public void setSchemeOptions(String s) {
    m_schemeOptions = s;
  }

  /**
   * Get the options to pass to the Clojure classifier
   * 
   * @return the options (as a string) to pass to the Clojure classifier
   */
  public String getSchemeOptions() {
    return m_schemeOptions;
  }

  /**
   * Set the model learned by the Clojure classifier
   * 
   * @param model the model learned by the clojure classifier
   */
  public void setModel(Object model) {
    m_model = model;
  }

  /**
   * Get the model learned by the Clojure classifier
   * 
   * @return the model learned by the Clojure classifier
   */
  public Object getModel() {
    return m_model;
  }

  @Override
  public void buildClassifier(Instances insts) throws Exception {

    if (m_namespace == null || m_namespace.length() == 0) {
      throw new Exception("No namespace for Clojure classifier specified!");
    }

    m_header = new Instances(insts, 0);

    ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(
      ClojureClassifier.class.getClassLoader());
    Clojure.require(m_namespace);
    m_clojureRequire = true;

    if (Clojure.eval("(resolve (symbol \"" + m_namespace
      + "/learn-classifier\"))") == null) {
      throw new Exception("Namespace " + m_namespace
        + " does not have a learn-classifier function!");
    }

    IFn learnFn = (IFn) Clojure.eval(m_namespace + "/learn-classifier");
    m_model = learnFn.invoke(insts, m_schemeOptions);

    Thread.currentThread().setContextClassLoader(prevCL);
  }

  @Override
  public double[] distributionForInstance(Instance inst) throws Exception {

    if (m_model == null) {
      throw new Exception("No model built yet!");
    }

    if (m_namespace == null || m_namespace.length() == 0) {
      throw new Exception("No namespace for Clojure classifier specified!");
    }

    ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(
      ClojureClassifier.class.getClassLoader());
    if (!m_clojureRequire) {
      Clojure.require(m_namespace);
      m_clojureRequire = true;
    }

    if (m_distributionForInstanceFunction == null) {
      if (Clojure.eval("(resolve (symbol \"" + m_namespace
        + "/distribution-for-instance\"))") == null) {
        throw new Exception("Namespace " + m_namespace
          + " does not have a distribution-for-instance function!");
      }

      m_distributionForInstanceFunction = (IFn) Clojure.eval(m_namespace
        + "/distribution-for-instance");
    }

    Object result = m_distributionForInstanceFunction.invoke(inst, m_model);
    if (!(result instanceof double[])) {
      throw new Exception(
        "distribution-for-instance function should return an array of doubles");
    }

    Thread.currentThread().setContextClassLoader(prevCL);

    return (double[]) result;
  }

  @Override
  public String toString() {
    if (m_model == null) {
      return "No model built yet";
    }

    if (m_namespace == null || m_namespace.length() == 0) {
      return "No namspace for Clojure classifier specified.";
    }

    ClassLoader prevCL = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(
      ClojureClassifier.class.getClassLoader());
    if (!m_clojureRequire) {
      Clojure.require(m_namespace);
      m_clojureRequire = true;
    }

    if (Clojure.eval("(resolve (symbol \"" + m_namespace
      + "/model-to-string\"))") != null) {
      IFn stringFn = (IFn) Clojure.eval(m_namespace + "/model-to-string");

      String result = stringFn.invoke(m_model, m_header).toString();
      Thread.currentThread().setContextClassLoader(prevCL);

      return result;
    }

    // just return the default if the clojure classifier does not have
    // a model-to-string function
    return m_model.toString();
  }

  // public static void main(String[] args) {
  // try {
  // Instances train = new Instances(new java.io.FileReader(args[0]));
  // train.setClassIndex(train.numAttributes() - 1);
  //
  // ClojureClassifier classifier = new ClojureClassifier();
  // classifier.setNamespace("weka-clj.simple");
  //
  // classifier.buildClassifier(train);
  // System.out.println("Clojure classifier:\n\n" + classifier.toString());
  //
  // // predict for the first instance
  // double[] preds = classifier.distributionForInstance(train.instance(0));
  // for (int i = 0; i < preds.length; i++) {
  // System.out.print(" " + preds[i]);
  // }
  // System.out.println();
  // } catch (Exception ex) {
  // ex.printStackTrace();
  // }
  // }

  /**
   * Main method for testing this class.
   * 
   * @param argv the options
   */
  public static void main(String[] args) {
    runClassifier(new ClojureClassifier(), args);
  }
}
