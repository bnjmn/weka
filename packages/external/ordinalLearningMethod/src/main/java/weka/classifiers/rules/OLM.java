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
 *    OLM.java
 *    Copyright (C) 2009 TriDat Tran
 *
 */

package weka.classifiers.rules;

import weka.classifiers.Classifier;
import weka.classifiers.AbstractClassifier;
import weka.classifiers.Evaluation;
import java.io.*;
import java.util.*;
import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;

/**
 * 
 <!-- globalinfo-start -->
 * This class is an implementation of the Ordinal Learning Method (OLM).<br/>
 * Further information regarding the algorithm and variants can be found in:<br/>
 * <br/>
 * Arie Ben-David (1992). Automatic Generation of Symbolic Multiattribute Ordinal Knowledge-Based DSSs: methodology and Applications. Decision Sciences. 23:1357-1372.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{Ben-David1992,
 *    author = {Arie Ben-David},
 *    journal = {Decision Sciences},
 *    pages = {1357-1372},
 *    title = {Automatic Generation of Symbolic Multiattribute Ordinal Knowledge-Based DSSs: methodology and Applications},
 *    volume = {23},
 *    year = {1992}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -R &lt;integer&gt;
 *  The resolution mode. Valid values are:
 *  0 for conservative resolution, 1 for random resolution, 2 for average, and 3 for no resolution. (default 0).</pre>
 * 
 * <pre> -C &lt;integer&gt;
 *  The classification mode. Valid values are:
 *  0 for conservative classification, 1 for nearest neighbour classification. (default 0).</pre>
 * 
 * <pre> -U &lt;size&gt;
 *  SSet maximum size of rule base
 *  (default: -U &lt;number of examples&gt;)</pre>
 * 
 <!-- options-end -->
 *
 * @author TriDat Tran
 * @version $Revision$ 
 */
public class OLM extends AbstractClassifier 
implements OptionHandler, TechnicalInformationHandler {

  /**
   * For serialization 
   */
  private static final long serialVersionUID = -381974207649598344L;

  //protected Instance ist;
  protected int printR;
  protected int numExamples;


  /* The conflict resolution modes */
  public static final int RESOLUTION_NONE = 3;
  public static final int RESOLUTION_AVERAGE = 2;
  public static final int RESOLUTION_RANDOM = 1;
  public static final int RESOLUTION_CONSERVATIVE = 0;
  public static final Tag [] TAGS_RESOLUTION = {
    new Tag(RESOLUTION_NONE, "No conflict resolution"),
    new Tag(RESOLUTION_AVERAGE, "Resolution using average"),
    new Tag(RESOLUTION_RANDOM, "Random resolution"),
    new Tag(RESOLUTION_CONSERVATIVE, "Conservative resolution")
  };

  /** The conflict resolution mode */
  protected int m_resolutionMode = RESOLUTION_CONSERVATIVE;

  /* The classification modes */
  public static final int CLASSIFICATION_CONSERVATIVE = 1;
  public static final int CLASSIFICATION_NEARESTNEIGHBOUR = 0;
  public static final Tag[] TAGS_CLASSIFICATION = {
    new Tag(CLASSIFICATION_NEARESTNEIGHBOUR, "Nearest neighbour classification"),
    new Tag(CLASSIFICATION_CONSERVATIVE, "Conservative classification")
  };

  /** The classification mode */
  protected int m_classificationMode = CLASSIFICATION_CONSERVATIVE;

  protected int upperBaseLimit = -1;
  protected int randSeed = 0;
  protected Random rand = new Random(0);

  protected boolean print_msg = false;

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();
    result.disableAll();

    // attributes
    result.enable(Capability.NOMINAL_ATTRIBUTES);
    result.enable(Capability.NUMERIC_ATTRIBUTES);
    result.enable(Capability.MISSING_VALUES);

    // class
    result.enable(Capability.NOMINAL_CLASS);
    result.enable(Capability.MISSING_CLASS_VALUES);

    // instances
    result.setMinimumNumberInstances(1);

    return result;
  }

  /**
   * Returns a string describing the classifier.
   * @return a description suitable for displaying in the 
   * explorer/experimenter gui
   */
  public String globalInfo() {
    return "This class is an implementation of the Ordinal Learning "
    + "Method (OLM).\n" 
    + "Further information regarding the algorithm and variants "
    + "can be found in:\n\n"
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
    TechnicalInformation        result;
    TechnicalInformation        additional;

    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Arie Ben-David");
    result.setValue(Field.YEAR, "1992");
    result.setValue(Field.TITLE, "Automatic Generation of Symbolic Multiattribute Ordinal Knowledge-Based DSSs: methodology and Applications");
    result.setValue(Field.JOURNAL, "Decision Sciences");
    result.setValue(Field.PAGES, "1357-1372");
    result.setValue(Field.VOLUME, "23");

    return result;
  }


  /**
   * Classifies a given instance.
   *
   * @param inst the instance to be classified
   * @return the classification
   */
  public double classifyInstance(Instance inst) {
    return olmrules.classify(inst);
  }

  /**
   * Returns an enumeration describing the available options
   * Valid options are: 
   * @return an enumeration of all the available options
   */
  public Enumeration listOptions() {
    Vector newVector = new Vector(3);

    newVector.addElement(new Option(
        "\tThe resolution mode. Valid values are:\n" +
        "\t0 for conservative resolution, 1 for random resolution," +
        "\t2 for average, and 3 for no resolution. (default 0).",
        "R", 1, "-R <integer>"));

    newVector.addElement(new Option(
        "\tThe classification mode. Valid values are:\n" +
        "\t0 for conservative classification, 1 for nearest neighbour classification." +
        " (default 0).",
        "C", 1, "-C <integer>"));

    newVector.addElement(new Option("\tSSet maximum size of rule base\n" +
        "\t(default: -U <number of examples>)","U", 1, "-U <size>"));

    return newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -R &lt;integer&gt;
   *  The resolution mode. Valid values are:
   *  0 for conservative resolution, 1 for random resolution, 2 for average, and 3 for no resolution. (default 0).</pre>
   * 
   * <pre> -C &lt;integer&gt;
   *  The classification mode. Valid values are:
   *  0 for conservative classification, 1 for nearest neighbour classification. (default 0).</pre>
   * 
   * <pre> -U &lt;size&gt;
   *  SSet maximum size of rule base
   *  (default: -U &lt;number of examples&gt;)</pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String resolutionMode = Utils.getOption('R', options);
    if (resolutionMode.length() > 0) {
      setResolutionMode(new SelectedTag(Integer.parseInt(resolutionMode), 
          TAGS_RESOLUTION));
    }

    String classificationMode = Utils.getOption('C', options);
    if (classificationMode.length() > 0) {
      setClassificationMode(new SelectedTag(Integer.parseInt(classificationMode), 
          TAGS_CLASSIFICATION));
    }

    String upperBase = Utils.getOption('U', options);
    if (upperBase.length() != 0) 
      upperBaseLimit = Integer.parseInt(upperBase); 
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    String [] options = new String [6];
    int current = 0;

    if(upperBaseLimit == -1) upperBaseLimit = numExamples;

    options[current++] = "-R"; options[current++] = "" + m_resolutionMode;
    options[current++] = "-C"; options[current++] = "" + m_classificationMode;
    options[current++] = "-U"; options[current++] = "" + upperBaseLimit;

    return options;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String resolutionModeTipText() {
    return "The resolution mode to use.";
  }

  /**
   * Sets the resolution mode.
   *
   * @param newMethod the new evaluation mode.
   */
  public void setResolutionMode(SelectedTag newMethod) {

    if (newMethod.getTags() == TAGS_RESOLUTION) {
      m_resolutionMode = newMethod.getSelectedTag().getID();
    }
  }

  /**
   * Gets the resolution mode.
   *
   * @return the evaluation mode.
   */
  public SelectedTag getResolutionMode() {

    return new SelectedTag(m_resolutionMode, TAGS_RESOLUTION);
  }

  /**
   * Sets the classification mode.
   * 
   * @param newMethod the new classification mode.
   */
  public void setClassificationMode(SelectedTag newMethod) {
    m_classificationMode = newMethod.getSelectedTag().getID();
  }

  /**
   * Gets the classification mode.
   * 
   * @return the classiciation mode
   */
  public SelectedTag getClassificationMode() {
    return new SelectedTag(m_classificationMode, TAGS_CLASSIFICATION);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String classificationModeTipText() {
    return "The classification mode to use.";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String ruleSizeTipText() {
    return "Set the rule base size\n" +
    "0 - unlimited\n";
  }

  public int getRuleSize(){ return upperBaseLimit;}
  public void setRuleSize(int s){ upperBaseLimit = s;}

  /**
   * Class to store CISE (Consistent and Irredundant Set of Examples) rules 
   */
  private class OLMRules implements Serializable{
    private Vector rules;

    /**
     * Constructor
     */
    public OLMRules()
    {
      rules = new Vector();
    }

    public int distance(Instance inst1, Instance inst2)
    {
      double values1[] = inst1.toDoubleArray();
      double values2[] = inst2.toDoubleArray();
      int classindex = inst1.classIndex();
      int numAtt = inst1.numAttributes();
      int dist = 0;

      for(int i=0; i < numAtt; i++)
      {
        if(i != classindex)
          dist += Math.abs(values1[i] - values2[i]);
      }

      return dist; 
    }

    public Instance averageRule(Instance inst1, Instance inst2)
    {
      Instance inst = inst1;
      double values1[] = inst1.toDoubleArray();
      double values2[] = inst2.toDoubleArray();
      int classindex = inst1.classIndex();
      int numAtt = inst1.numAttributes();

      for(int i=0; i < numAtt; i++)
      {
        inst.setValue(i,Math.round((values1[i] + values2[i])/2));
      } 

      return inst;
    }

    public void printRules()
    {
      Instance inst;
      for(int i=0; i < rules.size(); i++)
      {
        inst = (Instance)rules.elementAt(i);
        System.out.print(i+": ");
        System.out.println(inst.toString());
      }
    }
    
    public String toString() {
      StringBuffer temp = new StringBuffer();
      temp.append("OLM:\n\nInstances in rule set:\n\n");
      for(int i=0; i < rules.size(); i++)
      {
        Instance inst = (Instance)rules.elementAt(i);
        temp.append(i+": ");
        temp.append(inst.toString() + "\n");
      }
      return temp.toString();
    }
    
    /**
     * Checks if the input (non-class) attributes in inst1 is greater
     * than in inst2.
     *
     * @param inst1 Instance1
     * @param inst2 Instance2
     */
    private boolean isGreaterInput(Instance inst1, Instance inst2)
    {
      double values1[] = inst1.toDoubleArray();
      double values2[] = inst2.toDoubleArray();
      int classindex = inst1.classIndex();
      int numAtt = inst1.numAttributes();

      for(int i=0; i < numAtt; i++)
      {
        if(i!= classindex && values1[i] < values2[i]) 
          return false;
      }
      return true;
    }

    private boolean isEqualInput(Instance inst1, Instance inst2)
    {
      double values1[] = inst1.toDoubleArray();
      double values2[] = inst2.toDoubleArray();
      int classindex = inst1.classIndex();
      int numAtt = inst1.numAttributes();

      for(int i=0; i < numAtt; i++)
      {
        if(i!= classindex && values1[i] != values2[i]) 
          return false;
      }
      return true;
    }

    private boolean isGreaterOutput(Instance inst1, Instance inst2)
    {
      return (inst1.toDoubleArray())[inst1.classIndex()] > 
      (inst2.toDoubleArray())[inst2.classIndex()];
    }

    private boolean isEqualOutput(Instance inst1, Instance inst2)
    {
      return (inst1.toDoubleArray())[inst1.classIndex()] == 
        (inst2.toDoubleArray())[inst2.classIndex()];
    }

    private void fillMissing(Instance inst)
    {
      ;
    }

    public void addRule(Instance inst)
    {
      // add new rule?
      boolean addr = true;
      boolean b = false;
      int classindex = inst.classIndex();
      // Fill in missing values.
      fillMissing(inst);
      // Compare E with each rule in CISE
      for(int i=0; i < rules.size(); i++)
      {
        b = false;
        // Checks of Redudancies.
        if(isEqualOutput(inst, (Instance)rules.elementAt(i))) 
        {
          // Is E redundant : i.e EI(1) > EI(2) and EO(1) = EO(2)
          if(isGreaterInput(inst, (Instance)rules.elementAt(i)))
          {
            // E is redundant w.r.t rule i, we discard E
            addr = false;
            if(print_msg)
              System.out.println(inst.toString() + " is (1) redundant wrt " +
                  ((Instance)rules.elementAt(i)).toString());
            continue;
          }
          else if(isGreaterInput((Instance)rules.elementAt(i), inst))
          {
            if(print_msg)
              System.out.println(((Instance)rules.elementAt(i)).toString() + 
                  " is (2) redundant wrt " + inst.toString());
            // rule i is redundant w.r.t E, discard rule i
            rules.removeElementAt(i);
            i--;
            continue;
          }
        }

        // is E inconsistent and has a higher output?
        if(isGreaterInput((Instance)rules.elementAt(i), inst) && 
            !isGreaterOutput((Instance)rules.elementAt(i), inst))
        {

          // Conservative
          if (m_resolutionMode == RESOLUTION_CONSERVATIVE)
          {
            // discard E
            addr = false;
          }
          // Random
          if (m_resolutionMode == RESOLUTION_RANDOM)
          {
            // select random rule to keep
            if(rand.nextBoolean()) 
            {
              addr = addr || true;
              rules.removeElementAt(i);
              i--;
            }
            else
              addr = false;
          }
          // No Conflict Resolution, ignore new rule
          if (m_resolutionMode == RESOLUTION_NONE)
          {
            addr = false; 
          }
          // Average
          if (m_resolutionMode == RESOLUTION_AVERAGE)
          {
            // create 'average rule'
            if(print_msg)
              System.out.print(inst.toString() + " - " +
                  ((Instance)rules.elementAt(i)).toString());
            inst = averageRule(inst, (Instance)rules.elementAt(i));
            System.out.println(" : Average : " + inst.toString());
            // Remove current rule
            rules.removeElementAt(i);
            // test average rule
            addr = true;
            i = 0;
          }
          continue;
        }
        // is E inconsistent and has a lower output?
        if(isGreaterInput(inst, (Instance)rules.elementAt(i)) && 
            !isGreaterOutput(inst, (Instance)rules.elementAt(i)))
        {
          // Conservative
          if (m_resolutionMode == RESOLUTION_CONSERVATIVE)
          {
            // discard rule i
            if(print_msg)
              System.out.println("Discard rule "+
                  ((Instance)rules.elementAt(i)).toString());
            b = true;
            rules.removeElementAt(i);
            i--;
          }
          // Random
          if (m_resolutionMode == RESOLUTION_RANDOM)
          {
            // select random rule to keep
            if(rand.nextBoolean()) 
            {
              addr = addr || true;
              rules.removeElementAt(i);
              i--;
            }
            else
              addr = false;
          }
          // No Conflict Resolution, ignore new rule
          if (m_resolutionMode == RESOLUTION_NONE)
          {
            addr = false; 
          }
          // Average
          if (m_resolutionMode == RESOLUTION_AVERAGE)
          {
            // create 'average rule'
            if(print_msg)
              System.out.print(inst.toString() + " - " +
                  ((Instance)rules.elementAt(i)).toString());
            inst = averageRule(inst, (Instance)rules.elementAt(i));
            if(print_msg)
              System.out.println(" : Average : " + inst.toString());
            // Remove current rule
            rules.removeElementAt(i);
            // test average rule
            addr = true;
            i = 0;
          }
          continue;
        }
        // check if the rule is inconsistent
        if(isEqualInput(inst,(Instance)rules.elementAt(i)))
        {
          if(isGreaterOutput(inst,(Instance)rules.elementAt(i)))
          {
            // Conservative
            if (m_resolutionMode == RESOLUTION_CONSERVATIVE)                
            {
              // discard E
              addr = false;
            }
            // random
            if (m_resolutionMode == RESOLUTION_RANDOM)
            {
              // select random rule to keep
              if(rand.nextBoolean()) 
              {
                addr = addr || true;
                rules.removeElementAt(i);
                i--;
              }
              else
                addr = false;
            }
            // No Conflict Resolution, ignore new rule
            if (m_resolutionMode == RESOLUTION_NONE)
            {
              addr = false; 
            }
            // Average
            if (m_resolutionMode == RESOLUTION_AVERAGE)
            {
              // create 'average rule'
              if(print_msg)
                System.out.print(inst.toString() + " - " +
                    ((Instance)rules.elementAt(i)).toString());
              inst = averageRule(inst, (Instance)rules.elementAt(i));
              if(print_msg)
                System.out.println(" : 2Average : " + inst.toString());
              // Remove current rule
              rules.removeElementAt(i);
              // test average rule
              addr = true;
              i = 0;
            }
            continue;
          }
          else if(isGreaterOutput((Instance)rules.elementAt(i),inst)) 
          {

            // Conservative
            if (m_resolutionMode == RESOLUTION_CONSERVATIVE)                
            {
              //discard rule i
              rules.removeElementAt(i);
              i--;
            }
            //random
            if (m_resolutionMode == RESOLUTION_RANDOM)
            {
              // select random rule to keep
              if(rand.nextBoolean()) 
              {
                addr = addr || true;
                rules.removeElementAt(i);
                i--;
              }
              else
                addr = false;
            }
            // No Conflict Resolution, ignore new rule
            if (m_resolutionMode == RESOLUTION_NONE)
            {
              addr = false; 
            }
            // Average
            if (m_resolutionMode == RESOLUTION_AVERAGE)
            {
              // create 'average rule'
              if(print_msg)
                System.out.print(inst.toString() + " - " +
                    ((Instance)rules.elementAt(i)).toString());
              inst = averageRule(inst, (Instance)rules.elementAt(i));
              if(print_msg)
                System.out.println(" : Average : " + inst.toString());
              // Remove current rule
              rules.removeElementAt(i);
              // test average rule
              addr = true;
              i = 0;
            }
            continue;
          }
        }
      }

      if(b) System.out.println("broke out of loop totally!!");
      // insert the new rule if it has not been discarded, based on 
      // output order (decreasing order)
      // System.out.println("Adding Rule");
      int i = 0;
      double output = inst.toDoubleArray()[classindex];

      // Check Rule Base Limit
      if(addr && ( upperBaseLimit <= 0 || upperBaseLimit > rules.size()))
      {
        while(i < rules.size() && 
            (((Instance)rules.elementAt(i)).toDoubleArray())
            [classindex] > output) i++;

        if(i == rules.size())
          rules.addElement(inst);
        else if(i == 0)
          rules.insertElementAt(inst, 0);
        else
          rules.insertElementAt(inst, i); 
      }
      return;
    }

    public double classify(Instance inst)
    {
      Instance tInst;

      // fill in missing values
      fillMissing(inst);

      // Conservative
      if (m_classificationMode == CLASSIFICATION_CONSERVATIVE)
      {
        for(int i=0; i < rules.size(); i++)
        {
          tInst = (Instance)rules.elementAt(i);
          if(isGreaterInput(inst, tInst))
          {
            return (tInst.toDoubleArray())[inst.classIndex()];
          }
        }

        return (((Instance)rules.lastElement()).toDoubleArray())
        [inst.classIndex()];
      }
      // Nearest Neightbour
      int cDist = -1;
      int elem = -1;
      if (m_classificationMode == CLASSIFICATION_NEARESTNEIGHBOUR)
      {
        for(int i=0; i < rules.size(); i++)
        {
          tInst = (Instance)rules.elementAt(i);
          if(cDist == -1 || (distance(inst, tInst) < cDist))
          {
            cDist = distance(inst, tInst);
            elem = i;
          }
          if(print_msg)
            System.out.println(((Instance)rules.elementAt(i)).toString() +
                " - " +
                inst.toString() +
                ": Distance is " + distance(inst,tInst));
        }
        if(print_msg)
          System.out.println(((Instance)rules.elementAt(elem)).toString() +
              " is closest to " +
              inst.toString());

        return (((Instance)rules.elementAt(elem)).toDoubleArray())
        [inst.classIndex()];
      }

      return 0;
    }
  }

  private OLMRules olmrules;
  /**
   * Generates the classifier.
   *
   * @param data the data to be used
   * @exception Exception if the classifier can't built successfully
   */
  public void buildClassifier(Instances data) throws Exception 
  {
    // can classifier handle the data?
    getCapabilities().testWithFail(data);

    data = new Instances(data);
    numExamples = data.numInstances();
    Enumeration e = data.enumerateInstances();

    // Checks on data not implemented.

    // reset random generator to produce the same results each time
    rand = new Random(0);
    // Options
    if(print_msg)
      System.out.println("Resolution mode: " + m_resolutionMode);
    if(print_msg)
      System.out.println("Classification: " + m_classificationMode);
    if(print_msg)
      System.out.println("Rule size: " + upperBaseLimit);

    // initialize rules set.
    olmrules = new OLMRules();
    int i = 0;
    // fill in rules.
    if(print_msg)
      System.out.println("Printing Rule Process");
    while(e.hasMoreElements())
    {
      Instance ins = (Instance)e.nextElement();
      if(print_msg)
        System.out.println("Trying to add (" +
            ins.toString() + ") Rule");
      olmrules.addRule(ins); 
      if(print_msg)
        System.out.println("Result:");
      if(print_msg)
        olmrules.printRules();
      i++;

      // System.out.println("Added rule " + i);
    }
    //System.out.println("Rule set built!!");

    // print rule set:

  }

  /**
   * Prints a description of the classifier.
   *
   * @return a description of the classifier as a string
   */
  public String toString() {
    if (olmrules == null) {
      return "OLM: No model built yet.";
    }
    return olmrules.toString();
  }

  /**
   * Returns the revision string.
   * 
   * @return            the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  /**
   * Main method for testing this class
   */
  public static void main(String[] args) {

    runClassifier(new OLM(), args);
  }
}

