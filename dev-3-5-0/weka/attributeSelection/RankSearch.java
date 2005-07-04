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
 *    RankSearch.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package  weka.attributeSelection;

import  java.util.*;
import  weka.core.*;

/** 
 * Class for evaluating a attribute ranking (given by a specified
 * evaluator) using a specified subset evaluator. <p>
 *
 * Valid options are: <p>
 *
 * -A <attribute/subset evaluator> <br>
 * Specify the attribute/subset evaluator to be used for generating the 
 * ranking. If a subset evaluator is specified then a forward selection
 * search is used to produce a ranked list of attributes.<p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.12 $
 */
public class RankSearch extends ASSearch implements OptionHandler {

  /** does the data have a class */
  private boolean m_hasClass;
 
  /** holds the class index */
  private int m_classIndex;
 
  /** number of attributes in the data */
  private int m_numAttribs;

  /** the best subset found */
  private BitSet m_best_group;

  /** the attribute evaluator to use for generating the ranking */
  private ASEvaluation m_ASEval;

  /** the subset evaluator with which to evaluate the ranking */
  private ASEvaluation m_SubsetEval;

  /** the training instances */
  private Instances m_Instances;

  /** the merit of the best subset found */
  private double m_bestMerit;

  /** will hold the attribute ranking */
  private int [] m_Ranking;

  /** add this many attributes in each iteration from the ranking */
  protected int m_add = 1;

  /** start from this point in the ranking */
  protected int m_startPoint = 0;

  /**
   * Returns a string describing this search method
   * @return a description of the search method suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "RankSearch : \n\n"
      +"Uses an attribute/subset evaluator to rank all attributes. "
      +"If a subset evaluator is specified, then a forward selection "
      +"search is used to generate a ranked list. From the ranked "
      +"list of attributes, subsets of increasing size are evaluated, ie. "
      +"The best attribute, the best attribute plus the next best attribute, "
      +"etc.... The best attribute set is reported. RankSearch is linear in "
      +"the number of attributes if a simple attribute evaluator is used "
      +"such as GainRatioAttributeEval.\n";
  }

  public RankSearch () {
    resetOptions();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeEvaluatorTipText() {
    return "Attribute evaluator to use for generating a ranking.";    
  }

  /**
   * Set the attribute evaluator to use for generating the ranking.
   * @param newEvaluator the attribute evaluator to use.
   */
  public void setAttributeEvaluator(ASEvaluation newEvaluator) {
    m_ASEval = newEvaluator;
  }

  /**
   * Get the attribute evaluator used to generate the ranking.
   * @return the evaluator used to generate the ranking.
   */
  public ASEvaluation getAttributeEvaluator() {
    return m_ASEval;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String stepSizeTipText() {
    return "Add this many attributes from the ranking in each iteration.";
  }

  /**
   * Set the number of attributes to add from the rankining
   * in each iteration
   * @param ss the number of attribes to add.
   */
  public void setStepSize(int ss) {
    if (ss > 0) {
      m_add = ss;
    }
  }

  /**
   * Get the number of attributes to add from the rankining
   * in each iteration
   * @return the number of attributes to add.
   */
  public int getStepSize() {
    return m_add;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String startPointTipText() {
    return "Start evaluating from this point in the ranking.";
  }

  /**
   * Set the point at which to start evaluating the ranking
   * @param sp the position in the ranking to start at
   */
  public void setStartPoint(int sp) {
    if (sp >= 0) {
      m_startPoint = sp;
    }
  }

  /**
   * Get the point at which to start evaluating the ranking
   * @return the position in the ranking to start at
   */
  public int getStartPoint() {
    return m_startPoint;
  }

  /**
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(4);
    newVector.addElement(new Option("\tclass name of attribute evaluator to" 
			       + "\n\tuse for ranking. Place any" 
			       + "\n\tevaluator options LAST on the" 
			       + "\n\tcommand line following a \"--\"." 
			       + "\n\teg. -A weka.attributeSelection."
			       +"GainRatioAttributeEval ... " 
			       + "-- -M", "A", 1, "-A <attribute evaluator>"));
    newVector.addElement(new Option("\tnumber of attributes to be added from the"
                                    +"\n\tranking in each iteration (default = 1).", 
                                    "S", 1,"-S <step size>"));

    newVector.addElement(new Option("\tpoint in the ranking to start evaluating from. "
                                    +"\n\t(default = 0, ie. the head of the ranking).", 
                                    "R", 1,"-R <start point>"));

    if ((m_ASEval != null) && 
	(m_ASEval instanceof OptionHandler)) {
      newVector.addElement(new Option("", "", 0, "\nOptions specific to" 
				      + "evaluator " 
				      + m_ASEval.getClass().getName() 
				      + ":"));
      Enumeration enu = ((OptionHandler)m_ASEval).listOptions();

      while (enu.hasMoreElements()) {
        newVector.addElement(enu.nextElement());
      }
    }

    return newVector.elements();
  }


  /**
   * Parses a given list of options.
   *
   * Valid options are:<p>
   *
   * -A <attribute evaluator> <br>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception {
    String optionString;
    resetOptions();

    optionString = Utils.getOption('S', options);
    if (optionString.length() != 0) {
      setStepSize(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      setStartPoint(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('A', options);

    if (optionString.length() == 0) {
      throw  new Exception("An attribute evaluator  must be specified with" 
			   + "-A option");
    }

    setAttributeEvaluator(ASEvaluation.forName(optionString, 
				     Utils.partitionOptions(options)));
  }

  /**
   * Gets the current settings of WrapperSubsetEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] evaluatorOptions = new String[0];

    if ((m_ASEval != null) && 
	(m_ASEval instanceof OptionHandler)) {
      evaluatorOptions = ((OptionHandler)m_ASEval).getOptions();
    }

    String[] options = new String[8 + evaluatorOptions.length];
    int current = 0;

    options[current++] = "S"; options[current++] = ""+getStepSize();

    options[current++] = "R"; options[current++] = ""+getStartPoint();

    if (getAttributeEvaluator() != null) {
      options[current++] = "-A";
      options[current++] = getAttributeEvaluator().getClass().getName();
    }

    options[current++] = "--";
    System.arraycopy(evaluatorOptions, 0, options, current, 
		     evaluatorOptions.length);
    current += evaluatorOptions.length;

    while (current < options.length) {
      options[current++] = "";
    }

    return  options;
  }

  /**
   * Reset the search method.
   */
  protected void resetOptions () {
    m_ASEval = new GainRatioAttributeEval();
    m_Ranking = null;
  }

  /**
   * Ranks attributes using the specified attribute evaluator and then
   * searches the ranking using the supplied subset evaluator.
   *
   * @param ASEvaluator the subset evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @exception Exception if the search can't be completed
   */
  public int[] search (ASEvaluation ASEval, Instances data)
    throws Exception {
    
    double best_merit = -Double.MAX_VALUE;
    double temp_merit;
    BitSet temp_group, best_group=null;
    
    if (!(ASEval instanceof SubsetEvaluator)) {
      throw  new Exception(ASEval.getClass().getName() 
			   + " is not a " 
			   + "Subset evaluator!");
    }

    m_SubsetEval = ASEval;
    m_Instances = data;
    m_numAttribs = m_Instances.numAttributes();

    /*    if (m_ASEval instanceof AttributeTransformer) {
      throw new Exception("Can't use an attribute transformer "
			  +"with RankSearch");
			  } */
    if (m_ASEval instanceof UnsupervisedAttributeEvaluator || 
	m_ASEval instanceof UnsupervisedSubsetEvaluator) {
      m_hasClass = false;
      /*      if (!(m_SubsetEval instanceof UnsupervisedSubsetEvaluator)) {
	throw new Exception("Must use an unsupervised subset evaluator.");
	} */
    }
    else {
      m_hasClass = true;
      m_classIndex = m_Instances.classIndex();
    }

    if (m_ASEval instanceof AttributeEvaluator) {
      // generate the attribute ranking first
      Ranker ranker = new Ranker();
      ((AttributeEvaluator)m_ASEval).buildEvaluator(m_Instances);
      if (m_ASEval instanceof AttributeTransformer) {
	// get the transformed data a rebuild the subset evaluator
	m_Instances = ((AttributeTransformer)m_ASEval).
	  transformedData();
	((SubsetEvaluator)m_SubsetEval).buildEvaluator(m_Instances);
      }
      m_Ranking = ranker.search((AttributeEvaluator)m_ASEval, m_Instances);
    } else {
      GreedyStepwise fs = new GreedyStepwise();
      double [][]rankres; 
      fs.setGenerateRanking(true);
      ((SubsetEvaluator)m_ASEval).buildEvaluator(m_Instances);
      fs.search(m_ASEval, m_Instances);
      rankres = fs.rankedAttributes();
      m_Ranking = new int[rankres.length];
      for (int i=0;i<rankres.length;i++) {
	m_Ranking[i] = (int)rankres[i][0];
      }
    }

    // now evaluate the attribute ranking
    for (int i=m_startPoint;i<m_Ranking.length;i+=m_add) {
      temp_group = new BitSet(m_numAttribs);
      for (int j=0;j<=i;j++) {
	temp_group.set(m_Ranking[j]);
      }
      temp_merit = ((SubsetEvaluator)m_SubsetEval).evaluateSubset(temp_group);

      if (temp_merit > best_merit) {
	best_merit = temp_merit;;
	best_group = temp_group;
      }
    }
    m_bestMerit = best_merit;
    return attributeList(best_group);
  }
    
  /**
   * converts a BitSet into a list of attribute indexes 
   * @param group the BitSet to convert
   * @return an array of attribute indexes
   **/
  private int[] attributeList (BitSet group) {
    int count = 0;
    
    // count how many were selected
    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
	count++;
      }
    }

    int[] list = new int[count];
    count = 0;

    for (int i = 0; i < m_numAttribs; i++) {
      if (group.get(i)) {
	list[count++] = i;
      }
    }

    return  list;
  }

   /**
   * returns a description of the search as a String
   * @return a description of the search
   */
  public String toString () {
    StringBuffer text = new StringBuffer();
    text.append("\tRankSearch :\n");
    text.append("\tAttribute evaluator : "
		+ getAttributeEvaluator().getClass().getName() +" ");
    if (m_ASEval instanceof OptionHandler) {
      String[] evaluatorOptions = new String[0];
      evaluatorOptions = ((OptionHandler)m_ASEval).getOptions();
      for (int i=0;i<evaluatorOptions.length;i++) {
	text.append(evaluatorOptions[i]+' ');
      }
    }
    text.append("\n");
    text.append("\tAttribute ranking : \n");
    int rlength = (int)(Math.log(m_Ranking.length) / Math.log(10) + 1);
    for (int i=0;i<m_Ranking.length;i++) {
      text.append("\t "+Utils.doubleToString((double)(m_Ranking[i]+1),
					     rlength,0)
		  +" "+m_Instances.attribute(m_Ranking[i]).name()+'\n');
    }
    text.append("\tMerit of best subset found : ");
    int fieldwidth = 3;
    double precision = (m_bestMerit - (int)m_bestMerit);
    if (Math.abs(m_bestMerit) > 0) {
      fieldwidth = (int)Math.abs((Math.log(Math.abs(m_bestMerit)) / Math.log(10)))+2;
    }
    if (Math.abs(precision) > 0) {
      precision = Math.abs((Math.log(Math.abs(precision)) / Math.log(10)))+3;
    } else {
      precision = 2;
    }

    text.append(Utils.doubleToString(Math.abs(m_bestMerit),
				     fieldwidth+(int)precision,
				     (int)precision)+"\n");
    return text.toString();
  }
}
