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
 *    FLR.java
 *    Copyright (C) 2002 Ioannis N. Athanasiadis
 *
 */

package weka.classifiers.misc;

import weka.classifiers.Classifier;
import weka.core.AdditionalMeasureProducer;
import weka.core.AttributeStats;
import weka.core.Capabilities;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Summarizable;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;

/**
 <!-- globalinfo-start -->
 * Fuzzy Lattice Reasoning Classifier (FLR) v5.0<br/>
 * <br/>
 * The Fuzzy Lattice Reasoning Classifier uses the notion of Fuzzy Lattices for creating a Reasoning Environment.<br/>
 * The current version can be used for classification using numeric predictors.<br/>
 * <br/>
 * For more information see:<br/>
 * <br/>
 * I. N. Athanasiadis, V. G. Kaburlasos, P. A. Mitkas, V. Petridis: Applying Machine Learning Techniques on Air Quality Data for Real-Time Decision Support. In: 1st Intl. NAISO Symposium on Information Technologies in Environmental Engineering (ITEE-2003), Gdansk, Poland, 2003.<br/>
 * <br/>
 * V. G. Kaburlasos, I. N. Athanasiadis, P. A. Mitkas, V. Petridis (2003). Fuzzy Lattice Reasoning (FLR) Classifier and its Application on Improved Estimation of Ambient Ozone Concentration.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Athanasiadis2003,
 *    address = {Gdansk, Poland},
 *    author = {I. N. Athanasiadis and V. G. Kaburlasos and P. A. Mitkas and V. Petridis},
 *    booktitle = {1st Intl. NAISO Symposium on Information Technologies in Environmental Engineering (ITEE-2003)},
 *    note = {Abstract in ICSC-NAISO Academic Press, Canada (ISBN:3906454339), pg.51},
 *    publisher = {ICSC-NAISO Academic Press},
 *    title = {Applying Machine Learning Techniques on Air Quality Data for Real-Time Decision Support},
 *    year = {2003}
 * }
 * 
 * &#64;unpublished{Kaburlasos2003,
 *    author = {V. G. Kaburlasos and I. N. Athanasiadis and P. A. Mitkas and V. Petridis},
 *    title = {Fuzzy Lattice Reasoning (FLR) Classifier and its Application on Improved Estimation of Ambient Ozone Concentration},
 *    year = {2003}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -R
 *  Set vigilance parameter rhoa.
 *  (a float in range [0,1])</pre>
 * 
 * <pre> -B
 *  Set boundaries File
 *  Note:  The boundaries file is a simple text file containing 
 *  a row with a Fuzzy Lattice defining the metric space.
 *  For example, the boundaries file could contain the following 
 *  the metric space for the iris dataset:
 *  [ 4.3  7.9 ]  [ 2.0  4.4 ]  [ 1.0  6.9 ]  [ 0.1  2.5 ]  in Class:  -1
 *  This lattice just contains the min and max value in each 
 *  dimension.
 *  In other kind of problems this may not be just a min-max 
 *  operation, but it could contain limits defined by the problem 
 *  itself.
 *  Thus, this option should be set by the user.
 *  If ommited, the metric space used contains the mins and maxs 
 *  of the training split.</pre>
 * 
 * <pre> -Y
 *  Show Rules</pre>
 * 
 <!-- options-end -->
 * 
 * For further information contact I.N.Athanasiadis (ionathan@iti.gr)
 *
 * @author Ioannis N. Athanasiadis (email: ionathan@iti.gr, alias: ionathan@ieee.org)
 * @version 5.0
 * @version $Revision: 1.10 $
 */
public class FLR
    extends Classifier
    implements Serializable, Summarizable, AdditionalMeasureProducer,
               TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = 3337906540579569626L;
  
  public static final float EPSILON = 0.000001f;
  
  /** the RuleSet: a vector keeping the learned Fuzzy Lattices */
  private Vector learnedCode; 
  /** a double keeping the vignilance parameter rhoa */
  private double m_Rhoa = 0.5; 
  /** a Fuzzy Lattice keeping the metric space */
  private FuzzyLattice bounds; 
  /** a File pointing to the boundaries file (bounds.txt) */
  private File m_BoundsFile = new File(""); 
  /** a flag indicating whether the RuleSet will be displayed */
  private boolean m_showRules = true; 
  /** an index of the RuleSet (keeps how many rules are needed for each class) */
  private int index[]; 
  /** an array of the names of the classes */
  private String classNames[]; 


  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    // attributes
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.DATE_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);
    
    return result;
  }

  /**
   * Builds the FLR Classifier
   *
   * @param data the training dataset (Instances)
       * @throws Exception if the training dataset is not supported or is erroneous
   */
  public void buildClassifier(Instances data) throws Exception {
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    // remove instances with missing class
    data = new Instances(data);
    data.deleteWithMissingClass();

    // Exceptions statements
    for (int i = 0; i < data.numAttributes(); i++) {
      if (i != data.classIndex()) {
        AttributeStats stats = data.attributeStats(i);
        if(data.numInstances()==stats.missingCount ||
           Double.isNaN(stats.numericStats.min) ||
           Double.isInfinite(stats.numericStats.min))
          throw new Exception("All values are missing!" +
              data.attribute(i).toString());
      } //fi
    } //for

    if (!m_BoundsFile.canRead()) {
      setBounds(data);
    }
    else
      try {
        BufferedReader in = new BufferedReader(new FileReader(m_BoundsFile));
        String line = in.readLine();
        bounds = new FuzzyLattice(line);
      }
      catch (Exception e) {
        throw new Exception("Boundaries File structure error");
      }

    if (bounds.length() != data.numAttributes() - 1) {
      throw new Exception("Incompatible bounds file!");
    }
    checkBounds();

    // Variable Declerations and Initialization
    index = new int[data.numClasses()];
    classNames = new String[data.numClasses()];
    for (int i = 0; i < data.numClasses(); i++) {
      index[i] = 0;
      classNames[i] = "missing Class Name";
    }

    double rhoa = m_Rhoa;
    learnedCode = new Vector();
    int searching;
    FuzzyLattice inputBuffer;

    // Build Classifier (Training phase)
    if (data.firstInstance().classIsMissing())
      throw new Exception("In first instance, class is missing!");

    // set the first instance to be the first Rule in the model
    FuzzyLattice Code = new FuzzyLattice(data.firstInstance(), bounds);
    learnedCode.addElement(Code);
    index[Code.getCateg()]++;
    classNames[Code.getCateg()] = data.firstInstance().stringValue(data.
        firstInstance().classIndex());
    // training iteration
    for (int i = 1; i < data.numInstances(); i++) { //for all instances
      Instance inst = data.instance(i);
      int flag =0;
      for(int w=0;w<inst.numAttributes()-1;w++){
        if(w!=inst.classIndex() && inst.isMissing(w))
          flag=flag+1;
      }
      if (!inst.classIsMissing()&&flag!=inst.numAttributes()-1) {
        inputBuffer = new FuzzyLattice( (Instance) data.instance(i), bounds);
        double[] sigma = new double[ (learnedCode.size())];

        for (int j = 0; j < learnedCode.size(); j++) {
          FuzzyLattice num = (FuzzyLattice) learnedCode.get(j);
          FuzzyLattice den = inputBuffer.join(num);
          double numden = num.valuation(bounds) / den.valuation(bounds);
          sigma[j] = numden;
        } //for int j

        do {
          int winner = 0;
          double winnerf = sigma[0];

          for (int j = 1; j < learnedCode.size(); j++) {
            if (winnerf < sigma[j]) {
              winner = j;
              winnerf = sigma[j];
            } //if
          } //for

          FuzzyLattice num = inputBuffer;
          FuzzyLattice winnerBox = (FuzzyLattice) learnedCode.get(winner);
          FuzzyLattice den = winnerBox.join(num);
          double numden = num.valuation(bounds) / den.valuation(bounds);

          if ( (inputBuffer.getCateg() == winnerBox.getCateg()) &&
              (rhoa < (numden))) {
            learnedCode.setElementAt(winnerBox.join(inputBuffer), winner);
            searching = 0;
          }
          else {
            sigma[winner] = 0;
            rhoa += EPSILON;
            searching = 0;
            for (int j = 0; j < learnedCode.size(); j++) {
              if (sigma[j] != 0.0) {
                searching = 1;
              } //fi
            } //for

            if (searching == 0) {
              learnedCode.addElement(inputBuffer);
              index[inputBuffer.getCateg()]++;
              classNames[inputBuffer.getCateg()] = data.instance(i).stringValue(
                  data.instance(i).classIndex());
            } //fi
          } //else
        }
        while (searching == 1);
      } //if Class is missing

    } //for all instances
  } //buildClassifier

  /**
   * Classifies a given instance using the FLR Classifier model
   *
   * @param instance the instance to be classified
   * @return the class index into which the instance is classfied
   */

  public double classifyInstance(Instance instance) {

    FuzzyLattice num, den, inputBuffer;
    inputBuffer = new FuzzyLattice(instance, bounds); // transform instance to fuzzy lattice

    // calculate excitations and winner
    double[] sigma = new double[ (learnedCode.size())];
    for (int j = 0; j < learnedCode.size(); j++) {
      num = (FuzzyLattice) learnedCode.get(j);
      den = inputBuffer.join(num);
      sigma[j] = (num.valuation(bounds) / den.valuation(bounds));
    } //for j

    //find the winner Code (hyperbox)
    int winner = 0;
    double winnerf = sigma[0];
    for (int j = 1; j < learnedCode.size(); j++) {
      if (winnerf < sigma[j]) {
        winner = j;
        winnerf = sigma[j];
      } //fi
    } //for j

    FuzzyLattice currentBox = (FuzzyLattice) learnedCode.get(winner);
    return (double) currentBox.getCateg();
  } //classifyInstance

  /**
   * Returns a description of the classifier.
   *
   * @return String describing the FLR model
   */
  public String toString() {
    if (learnedCode != null) {
      String output = "";
      output = "FLR classifier\n=======================\n Rhoa = " + m_Rhoa;
      if (m_showRules) {
        output = output + "\n Extracted Rules (Fuzzy Lattices):\n\n";
        output = output + showRules();
        output = output + "\n\n Metric Space:\n" + bounds.toString();
      }
      output = output + "\n Total Number of Rules:    " + learnedCode.size() +
          "\n";
      for (int i = 0; i < index.length; i++) {
        output = output + " Rules pointing in Class " + classNames[i] + " :" +
            index[i] + "\n";
      }
      return output;
    }
    else {
      String output = "FLR classifier\n=======================\n Rhoa = " +
          m_Rhoa;
      output = output + "No model built";
      return output;
    }
  } //toString

  /**
   * Returns a superconcise version of the model
   *
   * @return String descibing the FLR model very shortly
   */
  public String toSummaryString() {
    String output = "";
    if (learnedCode == null) {
      output += "No model built";
    }
    else {
      output = output + "Total Number of Rules: " + learnedCode.size();
    }
    return output;
  } //toSummaryString

  /**
   * Returns the induced set of Fuzzy Lattice Rules
   *
   * @return String containing the ruleset
   *
   */
  public String showRules() {
    String output = "";
    for (int i = 0; i < learnedCode.size(); i++) {
      FuzzyLattice Code = (FuzzyLattice) learnedCode.get(i);
      output = output + "Rule: " + i + " " + Code.toString();
    }
    return output;
  } //showRules

  /**
   * Returns an enumeration describing the available options. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -R
   *  Set vigilance parameter rhoa.
   *  (a float in range [0,1])</pre>
   * 
   * <pre> -B
   *  Set boundaries File
   *  Note:  The boundaries file is a simple text file containing 
   *  a row with a Fuzzy Lattice defining the metric space.
   *  For example, the boundaries file could contain the following 
   *  the metric space for the iris dataset:
   *  [ 4.3  7.9 ]  [ 2.0  4.4 ]  [ 1.0  6.9 ]  [ 0.1  2.5 ]  in Class:  -1
   *  This lattice just contains the min and max value in each 
   *  dimension.
   *  In other kind of problems this may not be just a min-max 
   *  operation, but it could contain limits defined by the problem 
   *  itself.
   *  Thus, this option should be set by the user.
   *  If ommited, the metric space used contains the mins and maxs 
   *  of the training split.</pre>
   * 
   * <pre> -Y
   *  Show Rules</pre>
   * 
   <!-- options-end -->
   *
   * @return enumeration an enumeration of valid options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
	"\tSet vigilance parameter rhoa.\n"
	+ "\t(a float in range [0,1])", 
	"R", 1, "-R"));
    
    newVector.addElement(new Option(
	"\tSet boundaries File\n"
	+ "\tNote:  The boundaries file is a simple text file containing \n"
	+ "\ta row with a Fuzzy Lattice defining the metric space.\n"
	+ "\tFor example, the boundaries file could contain the following \n"
	+ "\tthe metric space for the iris dataset:\n"
	+ "\t[ 4.3  7.9 ]  [ 2.0  4.4 ]  [ 1.0  6.9 ]  [ 0.1  2.5 ]  in Class:  -1\n"
	+ "\tThis lattice just contains the min and max value in each \n"
	+ "\tdimension.\n"
	+ "\tIn other kind of problems this may not be just a min-max \n"
	+ "\toperation, but it could contain limits defined by the problem \n"
	+ "\titself.\n"
	+ "\tThus, this option should be set by the user.\n"
	+ "\tIf ommited, the metric space used contains the mins and maxs \n"
	+ "\tof the training split.", 
	"B", 1, "-B"));

    newVector.addElement(new Option(
	"\tShow Rules", 
	"Y", 0, "-Y"));
    
    return newVector.elements();
  } //listOptions

  /**
   * Parses a given list of options. <p/>
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -R
   *  Set vigilance parameter rhoa.
   *  (a float in range [0,1])</pre>
   * 
   * <pre> -B
   *  Set boundaries File
   *  Note:  The boundaries file is a simple text file containing 
   *  a row with a Fuzzy Lattice defining the metric space.
   *  For example, the boundaries file could contain the following 
   *  the metric space for the iris dataset:
   *  [ 4.3  7.9 ]  [ 2.0  4.4 ]  [ 1.0  6.9 ]  [ 0.1  2.5 ]  in Class:  -1
   *  This lattice just contains the min and max value in each 
   *  dimension.
   *  In other kind of problems this may not be just a min-max 
   *  operation, but it could contain limits defined by the problem 
   *  itself.
   *  Thus, this option should be set by the user.
   *  If ommited, the metric space used contains the mins and maxs 
   *  of the training split.</pre>
   * 
   * <pre> -Y
   *  Show Rules</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported (
   */
  public void setOptions(String[] options) throws Exception {
    // Option -Y
    m_showRules = Utils.getFlag('Y', options);
    // Option -R
    String rhoaString = Utils.getOption('R', options);
    if (rhoaString.length() != 0) {
      m_Rhoa = Double.parseDouble(rhoaString);
      if (m_Rhoa < 0 || m_Rhoa > 1) {
        throw new Exception(
            "Vigilance parameter (rhoa) should be a real number in range [0,1]");
      }
    }
    else
      m_Rhoa = 0.5;

      // Option -B
    String boundsString = Utils.getOption('B', options);
    if (boundsString.length() != 0) {
      m_BoundsFile = new File(boundsString);
    } //fi
    Utils.checkForRemainingOptions(options);
  } //setOptions

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String[] getOptions() {
    String[] options = new String[5];
    int current = 0;
    options[current++] = "-R";
    options[current++] = "" + getRhoa();
    if (m_showRules) {
      options[current++] = "-Y";
    }
    if (m_BoundsFile.toString() != "") {
      options[current++] = "-B";
      options[current++] = "" + getBoundsFile();
    }
    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  } // getOptions

  /**
   * Get rhoa
   * @return the value of this parameter
   */
  public double getRhoa() {
    return m_Rhoa;
  }

  /**
   * Get boundaries File
   * @return the value of this parameter
   */
  public String getBoundsFile() {
    return m_BoundsFile.toString();
  }

  /**
   * Get ShowRules parameter
   * @return the value of this parameter
   */
  public boolean getShowRules() {
    return m_showRules;
  }

  /**
   * Set rhoa
   * @param newRhoa sets the rhoa value
   * @throws Exception if rhoa is not in range [0,1]
   */
  public void setRhoa(double newRhoa) throws Exception {
    if (newRhoa < 0 || newRhoa > 1) {
      throw new Exception(
          "Vigilance parameter (rhoa) should be a real number in range [0,1]!!!");
    }
    m_Rhoa = newRhoa;
  }

  /**
   * Set Boundaries File
   * @param newBoundsFile a new file containing the boundaries
   */
  public void setBoundsFile(String newBoundsFile) {
    m_BoundsFile = new File(newBoundsFile);
  }

  /**
   * Set ShowRules flag
   * @param flag the new value of this parameter
   */
  public void setShowRules(boolean flag) {
    m_showRules = flag;
  }

  /**
   * Sets the metric space from the training set using the min-max stats, in case -B option is not used.
   * @param data is the training set
   */
  public void setBounds(Instances data) {
    // Initialize minmax stats
    bounds = new FuzzyLattice(data.numAttributes() - 1);
    int k = 0;
    for (int i = 0; i < data.numAttributes(); i++) {
      if (i != data.classIndex()) {
        AttributeStats stats = data.attributeStats(i);
        bounds.setMin(k, stats.numericStats.min);
        bounds.setMax(k, stats.numericStats.max);
        k = k + 1;
      } //if
    } //for
  } //setBounds

  /**
   * Checks the metric space
   */
  public void checkBounds() {
    for (int i = 0; i < bounds.length(); i++) {
      if (bounds.getMin(i) == bounds.getMax(i))
        bounds.setMax(i, bounds.getMax(i) + EPSILON);
    }
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String rhoaTipText() {
    return " The vigilance parameter value" + " (default = 0.75)";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String boundsFileTipText() {
    return " Point the filename containing the metric space";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String showRulesTipText() {
    return " If true, displays the ruleset.";
  }

  /**
   * Returns the value of the named measure
   * @param additionalMeasureName the name of the measure to query for its value
   * @return the value of the named measure
   * @throws IllegalArgumentException if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    if (additionalMeasureName.compareToIgnoreCase("measureNumRules") == 0) {
      return measureNumRules();
    }
    else {
      throw new IllegalArgumentException(additionalMeasureName +
                                         " not supported (FLR)");
    }
  }

  /**
   * Returns an enumeration of the additional measure names
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    Vector newVector = new Vector(1);
    newVector.addElement("measureNumRules");
    return newVector.elements();
  }

  /**
   * Additional measure Number of Rules
   * @return the number of rules induced
   */
  public double measureNumRules() {
    if (learnedCode == null)
      return 0.0;
    else
      return (double) learnedCode.size();
  }

  /**
   * Returns a description of the classifier suitable for
   * displaying in the explorer/experimenter gui
   * @return the description
   */
  public String globalInfo() {
    return 
        "Fuzzy Lattice Reasoning Classifier (FLR) v5.0\n\n"
      + "The Fuzzy Lattice Reasoning Classifier uses the notion of Fuzzy "
      + "Lattices for creating a Reasoning Environment.\n"
      + "The current version can be used for classification using numeric predictors.\n\n"
      + "For more information see:\n\n"
      + getTechnicalInformation().toString();
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
    TechnicalInformation 	additional;
   
    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "I. N. Athanasiadis and V. G. Kaburlasos and P. A. Mitkas and V. Petridis");
    result.setValue(Field.TITLE, "Applying Machine Learning Techniques on Air Quality Data for Real-Time Decision Support");
    result.setValue(Field.BOOKTITLE, "1st Intl. NAISO Symposium on Information Technologies in Environmental Engineering (ITEE-2003)");
    result.setValue(Field.YEAR, "2003");
    result.setValue(Field.ADDRESS, "Gdansk, Poland");
    result.setValue(Field.PUBLISHER, "ICSC-NAISO Academic Press");
    result.setValue(Field.NOTE, "Abstract in ICSC-NAISO Academic Press, Canada (ISBN:3906454339), pg.51");
    
    additional = result.add(Type.UNPUBLISHED);
    additional.setValue(Field.AUTHOR, "V. G. Kaburlasos and I. N. Athanasiadis and P. A. Mitkas and V. Petridis");
    additional.setValue(Field.TITLE, "Fuzzy Lattice Reasoning (FLR) Classifier and its Application on Improved Estimation of Ambient Ozone Concentration");
    additional.setValue(Field.YEAR, "2003");
    
    return result;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.10 $");
  }

  /**
   * Main method for testing this class.
   *
   * @param args should contain command line arguments for evaluation
   * (see Evaluation).
   */

  public static void main(String[] args) {
    runClassifier(new FLR(), args);
  }

  /**
   * <p>Fuzzy Lattice implementation in WEKA </p>
   *
   * @author Ioannis N. Athanasiadis
   * email: ionathan@iti.gr
   * alias: ionathan@ieee.org
   * @version 5.0
   */
  private class FuzzyLattice
    implements Serializable, RevisionHandler {

    /** for serialization */
    static final long serialVersionUID = -3568003680327062404L;
    
    private double min[];
    private double max[];
    private int categ;
    private String className;

    //Constructors

    /**
     * Constructs a Fuzzy Lattice from a instance
     * @param dR the instance
     * @param bounds the boundaries file
     */
    public FuzzyLattice(Instance dR, FuzzyLattice bounds) {
      min = new double[dR.numAttributes() - 1];
      max = new double[dR.numAttributes() - 1];
      int k = 0;
      for (int i = 0; i < dR.numAttributes(); i++) {
        if (i != dR.classIndex()) {
          if (!dR.isMissing(i)) {
            min[k] = (dR.value(i) > bounds.getMin(k)) ? dR.value(i) :
                bounds.getMin(k);
            max[k] = (dR.value(i) < bounds.getMax(k)) ? dR.value(i) :
                bounds.getMax(k);
            k = k + 1;
          } //if(!dR.isMissing(i))
          else {
            min[k] = bounds.getMax(k);
            max[k] = bounds.getMin(k);
            k = k + 1;
          } //else
        } //if(i!=dR.classIndex())
      } //for (int i=0; i<dR.numAttributes();i++)
      categ = (int) dR.value(dR.classIndex());
      className = dR.stringValue(dR.classIndex());
    } //FuzzyLattice

    /**
     * Constructs an empty Fuzzy Lattice of a specific dimension pointing
     * in Class "Metric Space" (-1)
     * @param length the dimention of the Lattice
     */
    public FuzzyLattice(int length) {
      min = new double[length];
      max = new double[length];

      for (int i = 0; i < length; i++) {
        min[i] = 0;
        max[i] = 0;
      }
      categ = -1;
      className = "Metric Space";
    }

    /**
     * Converts a String to a Fuzzy Lattice pointing in Class "Metric Space" (-1)
     * Note that the input String should be compatible with the toString() method.
     * @param rule the input String.
     */
    public FuzzyLattice(String rule) {
      int size = 0;
      for (int i = 0; i < rule.length(); i++) {
        String s = rule.substring(i, i + 1);
        if (s.equalsIgnoreCase("[")) {
          size++;
        }
      }
      min = new double[size];
      max = new double[size];

      int i = 0;
      int k = 0;
      String temp = "";
      int s = 0;
      do {
        String character = rule.substring(s, s + 1);
        temp = temp + character;
        if (character.equalsIgnoreCase(" ")) {
          if (!temp.equalsIgnoreCase(" ")) {
            k = k + 1;
            if (k % 4 == 2) {
              min[i] = Double.parseDouble(temp);
            } //if
            else if (k % 4 == 3) {
              max[i] = Double.parseDouble(temp);
              i = i + 1;
            } //else
          } // if (!temp.equalsIgnoreCase(" ") ){
          temp = "";
        } //if (character.equalsIgnoreCase(seperator)){
        s = s + 1;
      }
      while (i < size);
      categ = -1;
      className = "Metric Space";
    }

    // Functions

    /**
     * Calculates the valuation function of the FuzzyLattice
     * @param bounds corresponding boundaries
     * @return the value of the valuation function
     */
    public double valuation(FuzzyLattice bounds) {
      double resp = 0.0;
      for (int i = 0; i < min.length; i++) {
        resp += 1 -
            (min[i] - bounds.getMin(i)) / (bounds.getMax(i) - bounds.getMin(i));
        resp += (max[i] - bounds.getMin(i)) /
            (bounds.getMax(i) - bounds.getMin(i));
      }
      return resp;
    }

    /**
     * Calcualtes the length of the FuzzyLattice
     * @return the length
     */
    public int length() {
      return min.length;
    }

    /**
     * Implements the Join Function
     * @param lattice the second fuzzy lattice
     * @return the joint lattice
     */
    public FuzzyLattice join(FuzzyLattice lattice) { // Lattice Join
      FuzzyLattice b = new FuzzyLattice(lattice.length());
      int i;
      for (i = 0; i < lattice.min.length; i++) {
        b.min[i] = (lattice.min[i] < min[i]) ? lattice.min[i] :
            min[i];
        b.max[i] = (lattice.max[i] > max[i]) ? lattice.max[i] :
            max[i];
      }
      b.categ = categ;
      b.className = className;
      return b;
    }

    // Get-Set Functions

    public int getCateg() {
      return categ;
    }

    public void setCateg(int i) {
      categ = i;
    }

    public String getClassName() {
      return className;
    }

    public void setClassName(String s) {
      className = s;
    }

    public double getMin(int i) {
      return min[i];
    }

    public double getMax(int i) {
      return max[i];
    }

    public void setMin(int i, double val) {
      min[i] = val;
    }

    public void setMax(int i, double val) {
      max[i] = val;
    }

    /**
     * Returns a description of the Fuzzy Lattice
     * @return the Fuzzy Lattice and the corresponding Class
     */
    public String toString() {
      String rule = "";
      for (int i = 0; i < min.length; i++) {
        rule = rule + "[ " + min[i] + "  " + max[i] + " ]  ";
      }
      rule = rule + "in Class:  " + className + " \n";
      return rule;
    }
    
    /**
     * Returns the revision string.
     * 
     * @return		the revision
     */
    public String getRevision() {
      return RevisionUtils.extract("$Revision: 1.10 $");
    }
  }
}

