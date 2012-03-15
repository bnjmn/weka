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
 *    MIRI.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi;

import weka.classifiers.mi.miti.*;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Vector;

import weka.core.AdditionalMeasureProducer;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.OptionHandler;


/**
 <!-- globalinfo-start -->
 * MIRI (Multi Instance Rule Inducer): multi-instance classifier that utilizes partial MITI trees witha single positive leaf to learn and represent rules. For more information, see<br/>
 * <br/>
 * Hendrik Blockeel, David Page, Ashwin Srinivasan: Multi-instance Tree Learning. In: Proceedings of the International Conference on Machine Learning, 57-64, 2005.<br/>
 * <br/>
 * Luke Bjerring, Eibe Frank: Beyond Trees: Adopting MITI to Learn Rules and Ensemble Classifiers for Multi-instance Data. In: Proceedings of the Australasian Joint Conference on Artificial Intelligence, 2011.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Blockeel2005,
 *    author = {Hendrik Blockeel and David Page and Ashwin Srinivasan},
 *    booktitle = {Proceedings of the International Conference on Machine Learning},
 *    pages = {57-64},
 *    publisher = {ACM},
 *    title = {Multi-instance Tree Learning},
 *    year = {2005}
 * }
 * 
 * &#64;inproceedings{Bjerring2011,
 *    author = {Luke Bjerring and Eibe Frank},
 *    booktitle = {Proceedings of the Australasian Joint Conference on Artificial Intelligence},
 *    publisher = {Springer},
 *    title = {Beyond Trees: Adopting MITI to Learn Rules and Ensemble Classifiers for Multi-instance Data},
 *    year = {2011}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -M [1|2|3]
 *  The method used to determine best split:
 *  1. Gini; 2. MaxBEPP; 3. SSBEPP</pre>
 * 
 * <pre> -K [kBEPPConstant]
 *  The constant used in the tozero() hueristic</pre>
 * 
 * <pre> -L
 *  Scales the value of K to the size of the bags</pre>
 * 
 * <pre> -U
 *  Use unbiased estimate rather than BEPP, i.e. UEPP.</pre>
 * 
 * <pre> -B
 *  Uses the instances present for the bag counts at each node when splitting,
 *  weighted according to 1 - Ba ^ n, where n is the number of instances
 *  present which belong to the bag, and Ba is another parameter (default 0.5)</pre>
 * 
 * <pre> -Ba [multiplier]
 *  Multiplier for count influence of a bag based on the number of its instances</pre>
 * 
 * <pre> -A [number of attributes]
 *  The number of randomly selected attributes to split
 *  -1: All attributes
 *  -2: square root of the total number of attributes</pre>
 * 
 * <pre> -An [number of splits]
 *  The number of top scoring attribute splits to randomly pick from
 *  -1: All splits (completely random selection)
 *  -2: square root of the number of splits</pre>
 * 
 * <pre> -S &lt;num&gt;
 *  Random number seed.
 *  (default 1)</pre>
 * 
 * <pre> -D
 *  If set, classifier is run in debug mode and
 *  may output additional info to the console</pre>
 * 
 <!-- options-end -->
 *
 * @author Luke Bjerring
 * @author Eibe Frank
 */
public class MIRI extends MITI implements OptionHandler, AdditionalMeasureProducer {
  
  /** for serialization */
  static final long serialVersionUID = -218835168397644255L;

  // The list of partial trees (i.e. rules)
  private ArrayList<MultiInstanceDecisionTree> rules;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  "MIRI (Multi Instance Rule Inducer): multi-instance classifier "
      + "that utilizes partial MITI trees with"
      + "a single positive leaf to learn and represent rules. For more "
      + "information, see\n\n"
      + getTechnicalInformation().toString();
  }

  /**
   * Returns an enumeration of the additional measure names.
   *
   * @return an enumeration of the measure names
   */
  @SuppressWarnings({ "rawtypes", "unchecked" })
    public Enumeration enumerateMeasures() {
    
    Vector newVector = new Vector(3);
    newVector.addElement("measureNumRules");
    newVector.addElement("measureNumPositiveRules");
    newVector.addElement("measureNumConditionsInPositiveRules");
    return newVector.elements();
  }
 
  /**
   * Returns the value of the named measure.
   *
   * @param additionalMeasureName the name of the measure to query for its value
   * @return the value of the named measure
   * @throws IllegalArgumentException if the named measure is not supported
   */
  public double getMeasure(String additionalMeasureName) {
    
    if (additionalMeasureName.equalsIgnoreCase("measureNumRules")) {
      return (double) rules.size() + 1;
    }
    if (additionalMeasureName.equalsIgnoreCase("measureNumPositiveRules")) {
      int total = 0;
      for (MultiInstanceDecisionTree rule : rules) {
        total += rule.numPosRulesAndNumPosConditions()[0];
      }
      return (double) total;
    }
    if (additionalMeasureName.equalsIgnoreCase("measureNumConditionsInPositiveRules")) {
      int total = 0;
      for (MultiInstanceDecisionTree rule : rules) {
        total += rule.numPosRulesAndNumPosConditions()[1];
      }
      return (double) total;
    }
    else {throw new IllegalArgumentException(additionalMeasureName 
			      + " not supported (MultiInstanceTreeRuleLearner)");
    }
  }
  
  /**
   * Generates the rule set based on the given training data.
   */
  @Override
    public void buildClassifier(Instances trainingData) throws Exception {

    // can classifier handle the data?
    getCapabilities().testWithFail(trainingData);
    
    rules = new ArrayList<MultiInstanceDecisionTree>();
    
    HashMap<Instance, Bag> instanceBags = new HashMap<Instance, Bag>();
    ArrayList<Instance> all = new ArrayList<Instance>();
    double totalInstances = 0;
    double totalBags = 0;
    for (Instance i : trainingData) {
      Bag bag = new Bag(i);
      for (Instance bagged : bag.instances()) {
        instanceBags.put(bagged, bag);
        all.add(bagged);
      }
      totalBags++;
      totalInstances += trainingData.numInstances();
    }
    
    double b_multiplier = totalInstances / totalBags;
    if (m_scaleK)
      for (Bag bag : instanceBags.values())
        bag.setBagWeightMultiplier(b_multiplier);
    
    // Populate the list with trees that have just 1 positive branch
    while(all.size() > 0) {
      MultiInstanceDecisionTree tree =
        new MultiInstanceDecisionTree(instanceBags,
                                      all,
                                      true);

      if (m_Debug) {
        System.out.println(tree.render());
      }
      
      if (tree.trimNegativeBranches()) {

        if (m_Debug) {
          System.out.println(tree.render());
        }

        rules.add(tree);
      } else {
        
        // Could not find a positive leaf
        break;
      }
      
      // Update the list of enabled instances for the next tree
      boolean atLeast1Positive = false;
      ArrayList<Instance> stillEnabled = new ArrayList<Instance>();
      for (Instance i : all)
        {
          Bag bag = instanceBags.get(i);
          if (bag.isEnabled())
            {
              stillEnabled.add(i);
              if (bag.isPositive())
                atLeast1Positive = true;
            }
          
        }
      all = stillEnabled;
      if (!atLeast1Positive)
        break;
    }
  }
  
  /**
   * Returns the distribution of "class probabilities" for a new bag.
   */
  public double[] distributionForInstance(Instance newBag)
    throws Exception {
    
    double [] distribution = new double[2];
    Instances contents = newBag.relationalValue(1);
    boolean positive = false;
    for (Instance i : contents)
      {
    	for (MultiInstanceDecisionTree tree : rules)
          {
            if (tree.isPositive(i))
              {
                positive = true;
                break;
              }
          }
      }
    
    distribution[1] = positive ? 1 : 0;
    distribution[0] = 1 - distribution[1];
    
    return distribution;	   
  }
  
  /**
   * Returns string representing the rule set.
   */
  @Override
    public String toString()
  {
    if (rules != null) {
    	String s = rules.size() + " rules\n";
    	for (MultiInstanceDecisionTree tree : rules)
          s += tree.render() + "\n";
        
        s += "\nNumber of positive rules: " + getMeasure("measureNumPositiveRules") + "\n";
        s += "Number of conditions in positive rules: " + getMeasure("measureNumConditionsInPositiveRules") + "\n";
        
    	return s;
    } else {
      return "No model built yet!";
    }
  }
 
  /**
   * Used to run the algorithm from the command-line.
   */
  public static void main(String[] options) {
	 runClassifier(new MIRI(), options);
  }
}

