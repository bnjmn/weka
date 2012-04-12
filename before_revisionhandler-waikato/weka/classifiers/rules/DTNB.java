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
 *    DecisionTable.java
 *    Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.classifiers.rules;

import java.util.BitSet;
import java.util.Enumeration;
import java.util.Vector;

import weka.attributeSelection.SubsetEvaluator;
import weka.attributeSelection.ASSearch;
import weka.attributeSelection.ASEvaluation;
import weka.attributeSelection.SubsetEvaluator;
import weka.classifiers.bayes.NaiveBayes;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.Capabilities;
import weka.core.Capabilities.Capability;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SerializedObject;
import weka.core.Utils;

/**
 *
 <!-- globalinfo-start -->
 * Class for building and using a decision table/naive bayes hybrid classifier. At each point 
 * in the search, the algorithm evaluates the merit of dividing the attributes into two disjoint
 * subsets: one for the decision table, the other for naive Bayes. A forward selection search is
 * used, where at each step, selected attributes are modeled by naive Bayes and the remainder
 * by the decision table, and all attributes are modelled by the decision table initially. At each
 * step, the algorithm also considers dropping an attribute entirely from the model.
 *
 * <br/>
 * For more information see: <br/>
 * <br/>
 * Mark Hall and Eibe Frank: Combining Naive Bayes and Decision Tables. (In Press). 
 * Proceedings of the 21st Florida Artificial Intelligence Society Conference (FLAIRS). AAAI press.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;inproceedings{Hall2008,
 *    author = {Mark Hall and Eibe Frank},
 *    booktitle = {Proceedings of the 21st Florida Artificial Intelligence Society Conference (FLAIRS)},
 *    pages = {???-???},
 *    publisher = {AAAI press},
 *    title = {Combining Naive Bayes and Decision Tables},
 *    year = {2008}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * 
 * <pre> -X &lt;number of folds&gt;
 *  Use cross validation to evaluate features.
 *  Use number of folds = 1 for leave one out CV.
 *  (Default = leave one out CV)</pre>
 * 
 * <pre> -E &lt;acc | rmse | mae | auc&gt;
 *  Performance evaluation measure to use for selecting attributes.
 *  (Default = accuracy for discrete class and rmse for numeric class)</pre>
 * 
 * <pre> -I
 *  Use nearest neighbour instead of global table majority.</pre>
 * 
 * <pre> -R
 *  Display decision table rules.
 * </pre>
 * 
 * 
 <!-- options-end -->
 *
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org)
 * @author Eibe Frank (eibe{[at]}cs{[dot]}waikato{[dot]}ac{[dot]}nz)
 *
 * @version $Revision: 1.1.2.2 $
 *
 */
public class DTNB extends DecisionTable {

  /**
   * The naive Bayes half of the hybrid
   */
  protected NaiveBayes m_NB;

  /**
   * The features used by naive Bayes
   */
  private int [] m_nbFeatures;

  /**
   * Percentage of the total number of features used by the decision table
   */
  private double m_percentUsedByDT;
  
  /**
   * Percentage of the features features that were dropped entirely
   */
  private double m_percentDeleted;

  static final long serialVersionUID = 2999557077765701326L;

  /**
   * Returns a string describing classifier
   * @return a description suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return  
      "Class for building and using a decision table/naive bayes hybrid classifier. At each point "
      + "in the search, the algorithm evaluates the merit of dividing the attributes into two disjoint "
      + "subsets: one for the decision table, the other for naive Bayes. A forward selection search is "
      + "used, where at each step, selected attributes are modeled by naive Bayes and the remainder "
      + "by the decision table, and all attributes are modelled by the decision table initially. At each "
      + "step, the algorithm also considers dropping an attribute entirely from the model.\n\n"
      + "For more information, see: \n\n"
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

    result = new TechnicalInformation(Type.INPROCEEDINGS);
    result.setValue(Field.AUTHOR, "Mark Hall and Eibe Frank");
    result.setValue(Field.TITLE, "Combining Naive Bayes and Decision Tables");
    result.setValue(Field.BOOKTITLE, "Proceedings of the 21st Florida Artificial Intelligence "
                    + "Society Conference (FLAIRS)");
    result.setValue(Field.YEAR, "2008");
    result.setValue(Field.PAGES, "???-???");
    result.setValue(Field.PUBLISHER, "AAAI press");

    return result;
  }

  /**
   * Calculates the accuracy on a test fold for internal cross validation
   * of feature sets
   *
   * @param fold set of instances to be "left out" and classified
   * @param fs currently selected feature set
   * @return the accuracy for the fold
   * @throws Exception if something goes wrong
   */
  double evaluateFoldCV(Instances fold, int [] fs) throws Exception {

    int i;
    int ruleCount = 0;
    int numFold = fold.numInstances();
    int numCl = m_theInstances.classAttribute().numValues();
    double [][] class_distribs = new double [numFold][numCl];
    double [] instA = new double [fs.length];
    double [] normDist;
    DecisionTableHashKey thekey;
    double acc = 0.0;
    int classI = m_theInstances.classIndex();
    Instance inst;

    if (m_classIsNominal) {
      normDist = new double [numCl];
    } else {
      normDist = new double [2];
    }

    // first *remove* instances
    for (i=0;i<numFold;i++) {
      inst = fold.instance(i);
      for (int j=0;j<fs.length;j++) {
	if (fs[j] == classI) {
	  instA[j] = Double.MAX_VALUE; // missing for the class
	} else if (inst.isMissing(fs[j])) {
	  instA[j] = Double.MAX_VALUE;
	} else{
	  instA[j] = inst.value(fs[j]);
	}
      }
      thekey = new DecisionTableHashKey(instA);
      if ((class_distribs[i] = (double [])m_entries.get(thekey)) == null) {
	throw new Error("This should never happen!");
      } else {
	if (m_classIsNominal) {
	  class_distribs[i][(int)inst.classValue()] -= inst.weight();
	  inst.setWeight(-inst.weight());
	  m_NB.updateClassifier(inst);
	  inst.setWeight(-inst.weight());
	} else {
	  class_distribs[i][0] -= (inst.classValue() * inst.weight());
	  class_distribs[i][1] -= inst.weight();
	}
	ruleCount++;
      }
      m_classPriorCounts[(int)inst.classValue()] -= 
	inst.weight();	
    }
    double [] classPriors = m_classPriorCounts.clone();
    Utils.normalize(classPriors);

    // now classify instances
    for (i=0;i<numFold;i++) {
      inst = fold.instance(i);
      System.arraycopy(class_distribs[i],0,normDist,0,normDist.length);
      if (m_classIsNominal) {
	boolean ok = false;
	for (int j=0;j<normDist.length;j++) {
	  if (Utils.gr(normDist[j],1.0)) {
	    ok = true;
	    break;
	  }
	}

	if (!ok) { // majority class
	  normDist = classPriors.clone();
	} else {
	  Utils.normalize(normDist);
	}

	double [] nbDist = m_NB.distributionForInstance(inst);

	for (int l = 0; l < normDist.length; l++) {
	  normDist[l] = (Math.log(normDist[l]) - Math.log(classPriors[l]));
	  normDist[l] += Math.log(nbDist[l]);
	}
	normDist = Utils.logs2probs(normDist);
	// Utils.normalize(normDist);

	//	System.out.println(normDist[0] + " " + normDist[1] + " " + inst.classValue());

	if (m_evaluationMeasure == EVAL_AUC) {
	  m_evaluation.evaluateModelOnceAndRecordPrediction(normDist, inst);
	} else {
	  m_evaluation.evaluateModelOnce(normDist, inst);
	}
	/*	} else {					
	  normDist[(int)m_majority] = 1.0;
	  if (m_evaluationMeasure == EVAL_AUC) {
	    m_evaluation.evaluateModelOnceAndRecordPrediction(normDist, inst);						
	  } else {
	    m_evaluation.evaluateModelOnce(normDist, inst);					
	  }
	} */
      } else {
	if (Utils.eq(normDist[1],0.0)) {
	  double [] temp = new double[1];
	  temp[0] = m_majority;
	  m_evaluation.evaluateModelOnce(temp, inst);
	} else {
	  double [] temp = new double[1];
	  temp[0] = normDist[0] / normDist[1];
	  m_evaluation.evaluateModelOnce(temp, inst);
	}
      }
    }

    // now re-insert instances
    for (i=0;i<numFold;i++) {
      inst = fold.instance(i);

      m_classPriorCounts[(int)inst.classValue()] += 
	inst.weight();

      if (m_classIsNominal) {
	class_distribs[i][(int)inst.classValue()] += inst.weight();
	m_NB.updateClassifier(inst);
      } else {
	class_distribs[i][0] += (inst.classValue() * inst.weight());
	class_distribs[i][1] += inst.weight();
      }
    }
    return acc;
  }

  /**
   * Classifies an instance for internal leave one out cross validation
   * of feature sets
   *
   * @param instance instance to be "left out" and classified
   * @param instA feature values of the selected features for the instance
   * @return the classification of the instance
   * @throws Exception if something goes wrong
   */
  double evaluateInstanceLeaveOneOut(Instance instance, double [] instA)
  throws Exception {

    DecisionTableHashKey thekey;
    double [] tempDist;
    double [] normDist;

    thekey = new DecisionTableHashKey(instA);

    // if this one is not in the table
    if ((tempDist = (double [])m_entries.get(thekey)) == null) {
      throw new Error("This should never happen!");
    } else {
      normDist = new double [tempDist.length];
      System.arraycopy(tempDist,0,normDist,0,tempDist.length);
      normDist[(int)instance.classValue()] -= instance.weight();

      // update the table
      // first check to see if the class counts are all zero now
      boolean ok = false;
      for (int i=0;i<normDist.length;i++) {
	if (Utils.gr(normDist[i],1.0)) {
	  ok = true;
	  break;
	}
      }

      // downdate the class prior counts
      m_classPriorCounts[(int)instance.classValue()] -= 
	instance.weight(); 
      double [] classPriors = m_classPriorCounts.clone();
      Utils.normalize(classPriors);
      if (!ok) { // majority class	
	normDist = classPriors;
      } else {
	Utils.normalize(normDist);
      }

      m_classPriorCounts[(int)instance.classValue()] += 
      instance.weight();

      if (m_NB != null){
	// downdate NaiveBayes

	instance.setWeight(-instance.weight());
	m_NB.updateClassifier(instance);
	double [] nbDist = m_NB.distributionForInstance(instance);
	instance.setWeight(-instance.weight());
	m_NB.updateClassifier(instance);

	for (int i = 0; i < normDist.length; i++) {
	  normDist[i] = (Math.log(normDist[i]) - Math.log(classPriors[i]));
	  normDist[i] += Math.log(nbDist[i]);
	}
	normDist = Utils.logs2probs(normDist);
	// Utils.normalize(normDist);
      }

      if (m_evaluationMeasure == EVAL_AUC) {
	m_evaluation.evaluateModelOnceAndRecordPrediction(normDist, instance);						
      } else {
	m_evaluation.evaluateModelOnce(normDist, instance);
      }
      return Utils.maxIndex(normDist);
    }
  }

  /**
   * Sets up a dummy subset evaluator that basically just delegates
   * evaluation to the estimatePerformance method in DecisionTable
   */
  protected void setUpEvaluator() throws Exception {
    m_evaluator = new EvalWithDelete();
    m_evaluator.buildEvaluator(m_theInstances);
  }
  
  protected class EvalWithDelete extends SubsetEvaluator {
    
    // holds the list of attributes that are no longer in the model at all
    private BitSet m_deletedFromDTNB;
    
    public void buildEvaluator(Instances data) throws Exception {
      m_NB = null;
      m_deletedFromDTNB = new BitSet(data.numAttributes());
      // System.err.println("Here");
    }
    
   private int setUpForEval(BitSet subset) throws Exception {
     
     int fc = 0;
     for (int jj = 0;jj < m_numAttributes; jj++) {
	if (subset.get(jj)) {
	  fc++;
	}
     }

     //int [] nbFs = new int [fc];
     //int count = 0;

     for (int j = 0; j < m_numAttributes; j++) {
	m_theInstances.attribute(j).setWeight(1.0); // reset weight
	if (j != m_theInstances.classIndex()) {
	  if (subset.get(j)) {
	//    nbFs[count++] = j;
	    m_theInstances.attribute(j).setWeight(0.0); // no influence for NB
	  }
	}
     }
     
     // process delete set
     for (int i = 0; i < m_numAttributes; i++) {
	if (m_deletedFromDTNB.get(i)) {
	   m_theInstances.attribute(i).setWeight(0.0); // no influence for NB
	}
     }
     
     if (m_NB == null) {
	// construct naive bayes for the first time
	m_NB = new NaiveBayes();
	m_NB.buildClassifier(m_theInstances);
     }
     return fc;
   }

    public double evaluateSubset(BitSet subset) throws Exception {
      int fc = setUpForEval(subset);
      
      return estimatePerformance(subset, fc);
    }
    
    public double evaluateSubsetDelete(BitSet subset, int potentialDelete) throws Exception {
      
      int fc = setUpForEval(subset);
      
      // clear potentail delete for naive Bayes
      m_theInstances.attribute(potentialDelete).setWeight(0.0);
      //copy.clear(potentialDelete);
      //fc--;
      return estimatePerformance(subset, fc);
    }
    
    public BitSet getDeletedList() {
      return m_deletedFromDTNB;
    }
  }

  protected ASSearch m_backwardWithDelete;

  /**
   * Inner class implementing a special forwards search that looks for a good
   * split of attributes between naive Bayes and the decision table. It also
   * considers dropping attributes entirely from the model.
   */
  protected class BackwardsWithDelete extends ASSearch {

    public String globalInfo() {
      return "Specialized search that performs a forward selection (naive Bayes)/"
        + "backward elimination (decision table). Also considers dropping attributes "
        + "entirely from the combined model.";
    }

    public String toString() {
      return "";
    }

    public int [] search(ASEvaluation eval, Instances data)
      	throws Exception {
	int i;
	double best_merit = -Double.MAX_VALUE;
	double temp_best = 0, temp_merit = 0, temp_merit_delete = 0;
	int temp_index=0;
	BitSet temp_group;
	BitSet best_group = null;

	int numAttribs = data.numAttributes();

	if (best_group == null) {
	  best_group = new BitSet(numAttribs);
	}

	
	int classIndex = data.classIndex();
	for (i = 0; i < numAttribs; i++) {
	  if (i != classIndex) {
	    best_group.set(i);
	  }
	}

	//System.err.println(best_group);
	
	// Evaluate the initial subset
        //	best_merit = m_evaluator.evaluateSubset(best_group);
        best_merit = ((SubsetEvaluator)eval).evaluateSubset(best_group);

	//System.err.println(best_merit);

	// main search loop
	boolean done = false;
	boolean addone = false;
	boolean z;
	boolean deleted = false;
	while (!done) {
	  temp_group = (BitSet)best_group.clone();
	  temp_best = best_merit;
	  
	  done = true;
	  addone = false;
	  for (i = 0; i < numAttribs;i++) {
	    z = ((i != classIndex) && (temp_group.get(i)));

	    if (z) {
	      // set/unset the bit
	      temp_group.clear(i);

              //	      temp_merit = m_evaluator.evaluateSubset(temp_group);
	      temp_merit = ((SubsetEvaluator)eval).evaluateSubset(temp_group);
              //	      temp_merit_delete = ((EvalWithDelete)m_evaluator).evaluateSubsetDelete(temp_group, i);
	      temp_merit_delete = ((EvalWithDelete)eval).evaluateSubsetDelete(temp_group, i);
	      boolean deleteBetter = false;
	      //System.out.println("Merit: " + temp_merit + "\t" + "Delete merit: " + temp_merit_delete);
	      if (temp_merit_delete >= temp_merit) {
		temp_merit = temp_merit_delete;
		deleteBetter = true;
	      }
	      
	      z = (temp_merit >= temp_best);

	      if (z) {
		temp_best = temp_merit;
		temp_index = i;
		addone = true;
		done = false;
		if (deleteBetter) {
		  deleted = true;
		} else {
		  deleted = false;
		}
	      }

	      // unset this addition/deletion
		temp_group.set(i);
	    }
	  }
	  if (addone) {
	    best_group.clear(temp_index);
	    best_merit = temp_best;
	    if (deleted) {
              //	      ((EvalWithDelete)m_evaluator).getDeletedList().set(temp_index);
	      ((EvalWithDelete)eval).getDeletedList().set(temp_index);
	    }
	    //System.err.println("----------------------");
	    //System.err.println("Best subset: (dec table)" + best_group);
	    //System.err.println("Best subset: (deleted)" + ((EvalWithDelete)m_evaluator).getDeletedList());
	    //System.err.println(best_merit);
	  }
	}
	return attributeList(best_group);
      }
      
      /**
       * converts a BitSet into a list of attribute indexes 
       * @param group the BitSet to convert
       * @return an array of attribute indexes
       **/
      protected int[] attributeList (BitSet group) {
	int count = 0;
	BitSet copy = (BitSet)group.clone();
	
	/* remove any that have been completely deleted from DTNB
	BitSet deleted = ((EvalWithDelete)m_evaluator).getDeletedList();
	for (int i = 0; i < m_numAttributes; i++) {
	  if (deleted.get(i)) {
	    copy.clear(i);
	  }
	} */
	
	// count how many were selected
	for (int i = 0; i < m_numAttributes; i++) {
	  if (copy.get(i)) {
	    count++;
	  }
	}

	int[] list = new int[count];
	count = 0;

	for (int i = 0; i < m_numAttributes; i++) {
	  if (copy.get(i)) {
	    list[count++] = i;
	  }
	}

	return  list;
      }
  }

  private void setUpSearch() {
    m_backwardWithDelete = new BackwardsWithDelete();
  }
  
  /**
   * Generates the classifier.
   *
   * @param data set of instances serving as training data 
   * @throws Exception if the classifier has not been generated successfully
   */
  public void buildClassifier(Instances data) throws Exception {

    m_saveMemory = false;

    if (data.classAttribute().isNumeric()) {
      throw new Exception("Can only handle nominal class!");
    }

    if (m_backwardWithDelete == null) {
      setUpSearch();
      m_search = m_backwardWithDelete;
    }

    /*    if (m_search != m_backwardWithDelete) {
      m_search = m_backwardWithDelete;
      } */
    super.buildClassifier(data);

    // new NB stuff

    // delete the features used by the decision table (not the class!!)
    for (int i = 0; i < m_theInstances.numAttributes(); i++) {
      m_theInstances.attribute(i).setWeight(1.0); // reset all weights
    }
    // m_nbFeatures = new int [m_decisionFeatures.length - 1];
     int count = 0;

    for (int i = 0; i < m_decisionFeatures.length; i++) {
      if (m_decisionFeatures[i] != m_theInstances.classIndex()) {
	count++;
//	m_nbFeatures[count++] = m_decisionFeatures[i];
	m_theInstances.attribute(m_decisionFeatures[i]).setWeight(0.0); // No influence for NB
      }
    }
    
    double numDeleted = 0;
    // remove any attributes that have been deleted completely from the DTNB
    BitSet deleted = ((EvalWithDelete)m_evaluator).getDeletedList();
    for (int i = 0; i < m_theInstances.numAttributes(); i++) {
      if (deleted.get(i)) {
	m_theInstances.attribute(i).setWeight(0.0);
	// count--;
	numDeleted++;
	// System.err.println("Attribute "+i+" was eliminated completely");
      }
    }
    
    m_percentUsedByDT = (double)count / (m_theInstances.numAttributes() - 1);
    m_percentDeleted = numDeleted / (m_theInstances.numAttributes() -1);

    m_NB = new NaiveBayes();
    m_NB.buildClassifier(m_theInstances);

    m_dtInstances = new Instances(m_dtInstances, 0);
    m_theInstances = new Instances(m_theInstances, 0);
  }

  /**
   * Calculates the class membership probabilities for the given 
   * test instance.
   *
   * @param instance the instance to be classified
   * @return predicted class probability distribution
   * @exception Exception if distribution can't be computed
   */
  public double [] distributionForInstance(Instance instance)
  throws Exception {

    DecisionTableHashKey thekey;
    double [] tempDist;
    double [] normDist;

    m_disTransform.input(instance);
    m_disTransform.batchFinished();
    instance = m_disTransform.output();

    m_delTransform.input(instance);
    m_delTransform.batchFinished();
    Instance dtInstance = m_delTransform.output();

    thekey = new DecisionTableHashKey(dtInstance, dtInstance.numAttributes(), false);

    // if this one is not in the table
    if ((tempDist = (double [])m_entries.get(thekey)) == null) {
      if (m_useIBk) {
	tempDist = m_ibk.distributionForInstance(dtInstance);
      } else {  
	// tempDist = new double [m_theInstances.classAttribute().numValues()];
//	tempDist[(int)m_majority] = 1.0;
	
	tempDist = m_classPriors.clone();
	// return tempDist; ??????
      }
    } else {
      // normalise distribution
      normDist = new double [tempDist.length];
      System.arraycopy(tempDist,0,normDist,0,tempDist.length);
      Utils.normalize(normDist);
      tempDist = normDist;			
    }

    double [] nbDist = m_NB.distributionForInstance(instance);
    for (int i = 0; i < nbDist.length; i++) {
      tempDist[i] = (Math.log(tempDist[i]) - Math.log(m_classPriors[i]));
      tempDist[i] += Math.log(nbDist[i]);

      /*tempDist[i] *= nbDist[i];
      tempDist[i] /= m_classPriors[i];*/
    }
    tempDist = Utils.logs2probs(tempDist);
    Utils.normalize(tempDist);

    return tempDist;
  }

  public String toString() {

    String sS = super.toString();
    if (m_displayRules && m_NB != null) {
      sS += m_NB.toString();			
    }
    return sS;
  }
  
  /**
   * Returns the number of rules
   * @return the number of rules
   */
  public double measurePercentAttsUsedByDT() {
    return m_percentUsedByDT;
  }
  
  /**
   * Returns an enumeration of the additional measure names
   * @return an enumeration of the measure names
   */
  public Enumeration enumerateMeasures() {
    Vector newVector = new Vector(2);
    newVector.addElement("measureNumRules");
    newVector.addElement("measurePercentAttsUsedByDT");
    return newVector.elements();
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
    } else if (additionalMeasureName.compareToIgnoreCase("measurePercentAttsUsedByDT") == 0) {
      return measurePercentAttsUsedByDT();
    } else {
      throw new IllegalArgumentException(additionalMeasureName 
	  + " not supported (DecisionTable)");
    }
  }

  /**
   * Returns default capabilities of the classifier.
   *
   * @return      the capabilities of this classifier
   */
  public Capabilities getCapabilities() {
    Capabilities result = super.getCapabilities();

    result.disable(Capability.NUMERIC_CLASS);
    result.disable(Capability.DATE_CLASS);

    return result;
  }

  /**
   * Sets the search method to use
   * 
   * @param search
   */
  public void setSearch(ASSearch search) {
    // Search method cannot be changed.
    // Must be BackwardsWithDelete
    return;
  }

  /**
   * Gets the current search method
   * 
   * @return the search method used
   */
  public ASSearch getSearch() {
    if (m_backwardWithDelete == null) {
      setUpSearch();
      //      setSearch(m_backwardWithDelete);
      m_search = m_backwardWithDelete;
    }
    return m_search;
  }

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(7);

    newVector.addElement(new Option(
	"\tUse cross validation to evaluate features.\n" +
	"\tUse number of folds = 1 for leave one out CV.\n" +
	"\t(Default = leave one out CV)",
	"X", 1, "-X <number of folds>"));

    newVector.addElement(new Option(
	"\tPerformance evaluation measure to use for selecting attributes.\n" +
	"\t(Default = accuracy for discrete class and rmse for numeric class)",
	"E", 1, "-E <acc | rmse | mae | auc>"));

    newVector.addElement(new Option(
	"\tUse nearest neighbour instead of global table majority.",
	"I", 0, "-I"));

    newVector.addElement(new Option(
	"\tDisplay decision table rules.\n",
	"R", 0, "-R")); 

    return newVector.elements();
  }

  /**
   * Parses the options for this object. <p/>
   *
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -X &lt;number of folds&gt;
   *  Use cross validation to evaluate features.
   *  Use number of folds = 1 for leave one out CV.
   *  (Default = leave one out CV)</pre>
   * 
   * <pre> -E &lt;acc | rmse | mae | auc&gt;
   *  Performance evaluation measure to use for selecting attributes.
   *  (Default = accuracy for discrete class and rmse for numeric class)</pre>
   * 
   * <pre> -I
   *  Use nearest neighbour instead of global table majority.</pre>
   * 
   * <pre> -R
   *  Display decision table rules.
   * </pre>
   * 
   <!-- options-end -->
   *
   * @param options the list of options as an array of strings
   * @throws Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {

    String optionString;

    resetOptions();

    optionString = Utils.getOption('X',options);
    if (optionString.length() != 0) {
      setCrossVal(Integer.parseInt(optionString));
    }

    m_useIBk = Utils.getFlag('I',options);

    m_displayRules = Utils.getFlag('R',options);

    optionString = Utils.getOption('E', options);
    if (optionString.length() != 0) {
      if (optionString.equals("acc")) {
	setEvaluationMeasure(new SelectedTag(EVAL_ACCURACY, TAGS_EVALUATION));
      } else if (optionString.equals("rmse")) {
	setEvaluationMeasure(new SelectedTag(EVAL_RMSE, TAGS_EVALUATION));
      } else if (optionString.equals("mae")) {
	setEvaluationMeasure(new SelectedTag(EVAL_MAE, TAGS_EVALUATION));
      } else if (optionString.equals("auc")) {
	setEvaluationMeasure(new SelectedTag(EVAL_AUC, TAGS_EVALUATION));
      } else {
	throw new IllegalArgumentException("Invalid evaluation measure");
      }
    }
  }

  /**
   * Gets the current settings of the classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [9];
    int current = 0;

    options[current++] = "-X"; options[current++] = "" + getCrossVal();

    if (m_evaluationMeasure != EVAL_DEFAULT) {
      options[current++] = "-E";
      switch (m_evaluationMeasure) {
      case EVAL_ACCURACY:
	options[current++] = "acc";
	break;
      case EVAL_RMSE:
	options[current++] = "rmse";
	break;
      case EVAL_MAE:
	options[current++] = "mae";
	break;
      case EVAL_AUC:
	options[current++] = "auc";
	break;
      }
    }
    if (m_useIBk) {
      options[current++] = "-I";
    }
    if (m_displayRules) {
      options[current++] = "-R";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    return options;
  }

  /**
   * Main method for testing this class.
   *
   * @param argv the command-line options
   */
  public static void main(String [] argv) {
    runClassifier(new DTNB(), argv);
  }
}
