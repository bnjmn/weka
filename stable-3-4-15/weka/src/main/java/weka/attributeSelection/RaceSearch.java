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
 *    RaceSearch.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package  weka.attributeSelection;

import java.io.*;
import java.util.*;
import weka.core.*;
import weka.experiment.PairedStats;
import weka.experiment.Stats;

/** 
 * Class for performing a racing search. <p>
 *
 * For more information see: <br>
 * Moore, A. W. and Lee, M. S. (1994). Efficient algorithms for minimising
 * cross validation error. Proceedings of the Eleventh International
 * Conference on Machine Learning. pp 190--198. <p>
 *
 * Valid options are:<p>
 *
 * -R race type<br>
 * 0 = forward, 1 = backward, 2 = schemata, 3 = rank. <p>
 * 
 * -L significance level <br>
 * significance level to use for t-tests. <p>
 *
 * -T threshold <br>
 * threshold for considering mean errors of two subsets the same <p>
 *
 * -F xval type <br>
 * 0 = 10 fold, 1 = leave-one-out (selected automatically for schemata race
 * <p>
 *
 * -A attribute evaluator <br>
 * the attribute evaluator to use when doing a rank search <p>
 *
 * -Q <br>
 * produce a ranked list of attributes. Selecting this option forces
 * the race type to be forward. Racing continues until *all* attributes
 * have been selected, thus producing a ranked list of attributes. <p>
 *
 * -N number to retain <br>
 * Specify the number of attributes to retain. Overides any threshold. 
 * Use in conjunction with -Q. <p>
 * 
 * -J threshold <br>
 * Specify a threshold by which the AttributeSelection module can discard
 * attributes. Use in conjunction with -Q. <p>
 *
 * -Z <br>
 * Turn on verbose output for monitoring the search <p>
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.14.2.2 $
 */
public class RaceSearch extends ASSearch implements RankedOutputSearch, 
						    OptionHandler {

  /* the training instances */
  private Instances m_Instances = null;

  /** search types */
  private static final int FORWARD_RACE = 0;
  private static final int BACKWARD_RACE = 1;
  private static final int SCHEMATA_RACE = 2;
  private static final int RANK_RACE = 3;
  public static final Tag [] TAGS_SELECTION = {
    new Tag(FORWARD_RACE, "Forward selection race"),
    new Tag(BACKWARD_RACE, "Backward elimination race"),
    new Tag(SCHEMATA_RACE, "Schemata race"),
    new Tag(RANK_RACE, "Rank race")
      };
  
  /** the selected search type */
  private int m_raceType = FORWARD_RACE;
  
  /** xval types */
  private static final int TEN_FOLD = 0;
  private static final int LEAVE_ONE_OUT = 1;
  public static final Tag [] XVALTAGS_SELECTION = {
    new Tag(TEN_FOLD, "10 Fold"),
    new Tag(LEAVE_ONE_OUT, "Leave-one-out"),
      };

  /** the selected xval type */
  private int m_xvalType = TEN_FOLD;
  
  /** the class index */
  private int m_classIndex;

  /** the number of attributes in the data */
  private int m_numAttribs;

  /** the total number of partially/fully evaluated subsets */
  private int m_totalEvals;

  /** holds the merit of the best subset found */
  private double m_bestMerit = -Double.MAX_VALUE;

  /** the subset evaluator to use */
  private HoldOutSubsetEvaluator m_theEvaluator = null;

  /** the significance level for comparisons */
  private double m_sigLevel = 0.001;

  /** threshold for comparisons */
  private double m_delta = 0.001;

  /** the number of samples above which to begin testing for similarity
      between competing subsets */
  private int m_samples = 20;

  /** number of cross validation folds---equal to the number of instances
      for leave-one-out cv */
  private int m_numFolds = 10;

  /** the attribute evaluator to generate the initial ranking when
      doing a rank race */
  private ASEvaluation m_ASEval = new GainRatioAttributeEval();

  /** will hold the attribute ranking produced by the above attribute
      evaluator if doing a rank search */
  private int [] m_Ranking;

  /** verbose output for monitoring the search and debugging */
  private boolean m_debug = false;

  /** If true then produce a ranked list of attributes by fully traversing
      a forward hillclimb race */
  private boolean m_rankingRequested = false;

  /** The ranked list of attributes produced if m_rankingRequested is true */
  private double [][] m_rankedAtts;

  /** The number of attributes ranked so far (if ranking is requested) */
  private int m_rankedSoFar;

  /** The number of attributes to retain if a ranking is requested. -1
      indicates that all attributes are to be retained. Has precedence over
      m_threshold */
  private int m_numToSelect = -1;

  private int m_calculatedNumToSelect = -1;

  /** the threshold for removing attributes if ranking is requested */
  private double m_threshold = -Double.MAX_VALUE;

  /**
   * Returns a string describing this search method
   * @return a description of the search method suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Races the cross validation error of competing "
      +"attribute subsets. Use in conjuction with a ClassifierSubsetEval. "
      +"RaceSearch has four modes:\n\nforward selection "
      +"races all single attribute additions to a base set (initially "
      +" no attributes), selects the winner to become the new base set "
      +"and then iterates until there is no improvement over the base set. "
      +"\n\nBackward elimination is similar but the initial base set has all "
      +"attributes included and races all single attribute deletions. "
      +"\n\nSchemata search is a bit different. Each iteration a series of "
      +"races are run in parallel. Each race in a set determines whether "
      +"a particular attribute should be included or not---ie the race is "
      +"between the attribute being \"in\" or \"out\". The other attributes "
      +"for this race are included or excluded randomly at each point in the "
      +"evaluation. As soon as one race "
      +"has a clear winner (ie it has been decided whether a particular "
      +"attribute should be inor not) then the next set of races begins, "
      +"using the result of the winning race from the previous iteration as "
      +"new base set.\n\nRank race first ranks the attributes using an "
      +"attribute evaluator and then races the ranking. The race includes "
      +"no attributes, the top ranked attribute, the top two attributes, the "
      +"top three attributes, etc.\n\nIt is also possible to generate a "
      +"raked list of attributes through the forward racing process. "
      +"If generateRanking is set to true then a complete forward race will "
      +"be run---that is, racing continues until all attributes have been "
      +"selected. The order that they are added in determines a complete "
      +"ranking of all the attributes.\n\nRacing uses paired and unpaired "
      +"t-tests on cross-validation errors of competing subsets. When there "
      +"is a significant difference between the means of the errors of two "
      +"competing subsets then the poorer of the two can be eliminated from "
      +"the race. Similarly, if there is no significant difference between "
      +"the mean errors of two competing subsets and they are within some "
      +"threshold of each other, then one can be eliminated from the race. ";
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String raceTypeTipText() {
    return "Set the type of search.";
  }

  /**
   * Set the race type
   *
   * @param d the type of race
   */
  public void setRaceType (SelectedTag d) {
    
    if (d.getTags() == TAGS_SELECTION) {
      m_raceType = d.getSelectedTag().getID();
    }
    if (m_raceType == SCHEMATA_RACE && !m_rankingRequested) {
      try {
	setFoldsType(new SelectedTag(LEAVE_ONE_OUT,
				     XVALTAGS_SELECTION));
	setSignificanceLevel(0.01);
      } catch (Exception ex) {
      }
    } else {
      try {
	setFoldsType(new SelectedTag(TEN_FOLD,
				     XVALTAGS_SELECTION));
	setSignificanceLevel(0.001);
      } catch (Exception ex) {
      }
    }
  }

  /**
   * Get the race type
   *
   * @return the type of race
   */
  public SelectedTag getRaceType() {
    return new SelectedTag(m_raceType, TAGS_SELECTION);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String significanceLevelTipText() {
    return "Set the significance level to use for t-test comparisons.";
  }

  /**
   * Sets the significance level to use
   * @param sig the significance level
   */
  public void setSignificanceLevel(double sig) {
    m_sigLevel = sig;
  }

  /**
   * Get the significance level
   * @return the current significance level
   */
  public double getSignificanceLevel() {
    return m_sigLevel;
  }
  
  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String thresholdTipText() {
    return "Set the error threshold by which to consider two subsets "
      +"equivalent.";
  }

  /**
   * Sets the threshold for comparisons
   * @param t the threshold to use
   */
  public void setThreshold(double t) {
    m_delta = t;
  }

  /**
   * Get the threshold
   * @return the current threshold
   */
  public double getThreshold() {
    return m_delta;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String foldsTipText() {
    return "Set the number of folds to use for x-val error estimation. "
      +"Leave-one-out is selected automatically for schemata search.";
  }

  /**
   * Set the xfold type
   *
   * @param d the type of xval
   */
  public void setFoldsType (SelectedTag d) {
    
    if (d.getTags() == XVALTAGS_SELECTION) {
      m_xvalType = d.getSelectedTag().getID();
    }
  }

  /**
   * Get the xfold type
   *
   * @return the type of xval
   */
  public SelectedTag getFoldsType () {
    return new SelectedTag(m_xvalType, XVALTAGS_SELECTION);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String debugTipText() {
    return "Turn on verbose output for monitoring the search's progress.";
  }

  /**
   * Set whether verbose output should be generated.
   * @param d true if output is to be verbose.
   */
  public void setDebug(boolean d) {
    m_debug = d;
  }

  /**
   * Get whether output is to be verbose
   * @return true if output will be verbose
   */
  public boolean getDebug() {
    return m_debug;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String attributeEvaluatorTipText() {
    return "Attribute evaluator to use for generating an initial ranking. "
      +"Use in conjunction with a rank race";    
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
  public String generateRankingTipText() {
    return "Use the racing process to generate a ranked list of attributes. "
      +"Using this mode forces the race to be a forward type and then races "
      +"until all attributes have been added, thus giving a ranked list";
  }
  
  /**
   * Records whether the user has requested a ranked list of attributes.
   * @param doRank true if ranking is requested
   */
  public void setGenerateRanking(boolean doRank) {
    m_rankingRequested = doRank;
    if (m_rankingRequested) {
      try {
	setRaceType(new SelectedTag(FORWARD_RACE,
				    TAGS_SELECTION));
      } catch (Exception ex) {
      }
    }
  }

  /**
   * Gets whether ranking has been requested. This is used by the
   * AttributeSelection module to determine if rankedAttributes()
   * should be called.
   * @return true if ranking has been requested.
   */
  public boolean getGenerateRanking() {
    return m_rankingRequested;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numToSelectTipText() {
    return "Specify the number of attributes to retain. Use in conjunction "
      +"with generateRanking. The default value "
      +"(-1) indicates that all attributes are to be retained. Use either "
      +"this option or a threshold to reduce the attribute set.";
  }

  /**
   * Specify the number of attributes to select from the ranked list
   * (if generating a ranking). -1
   * indicates that all attributes are to be retained.
   * @param n the number of attributes to retain
   */
  public void setNumToSelect(int n) {
    m_numToSelect = n;
  }

  /**
   * Gets the number of attributes to be retained.
   * @return the number of attributes to retain
   */
  public int getNumToSelect() {
    return m_numToSelect;
  }

  /**
   * Gets the calculated number of attributes to retain. This is the
   * actual number of attributes to retain. This is the same as
   * getNumToSelect if the user specifies a number which is not less
   * than zero. Otherwise it should be the number of attributes in the
   * (potentially transformed) data.
   */
  public int getCalculatedNumToSelect() {
    if (m_numToSelect >= 0) {
      m_calculatedNumToSelect = m_numToSelect;
    }
    return m_calculatedNumToSelect;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String selectionThresholdTipText() {
    return "Set threshold by which attributes can be discarded. Default value "
      + "results in no attributes being discarded. Use in conjunction with "
      + "generateRanking";
  }

  /**
   * Set the threshold by which the AttributeSelection module can discard
   * attributes.
   * @param threshold the threshold.
   */
  public void setSelectionThreshold(double threshold) {
    m_threshold = threshold;
  }

  /**
   * Returns the threshold so that the AttributeSelection module can
   * discard attributes from the ranking.
   */
  public double getSelectionThreshold() {
    return m_threshold;
  }


  /**
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector();
     newVector.addElement(new Option("\tType of race to perform.\n\t"
				     +"(default = 0).",
				     "R", 1 ,"-R <0 = forward | 1 = backward "
				     +"race | 2 = schemata | 3 = rank>"));
     newVector.addElement(new Option("\tSignificance level for comaparisons"
				     +"\n\t(default = 0.001(forward/backward/"
				     +"rank)/0.01(schemata)).",
				     "L",1,"-L <significance>"));
     newVector.addElement(new Option("\tThreshold for error comparison.\n\t"
				     +"(default = 0.001).",
				     "T",1,"-T <threshold>"));
     
     newVector.addElement(new Option("\tAttribute ranker to use if doing a "
			   +"\n\trank search. Place any\n\t"
			   +"evaluator options LAST on the" 
			   + "\n\tcommand line following a \"--\"." 
			   + "\n\teg. -A weka.attributeSelection."
			   +"GainRatioAttributeEval ... " 
			   + "-- -M.\n\t(default = GainRatioAttributeEval)", 
			   "A", 1, "-A <attribute evaluator>"));
    
     newVector.addElement(new Option("\tFolds for cross validation\n\t"
			    +"(default = 0 (1 if schemata race)",
			    "F",1,"-F <0 = 10 fold | 1 = leave-one-out>"));
     newVector.addElement(new Option("\tGenerate a ranked list of attributes."
				     +"\n\tForces the search to be forward\n."
				     +"\tand races until all attributes have\n"
				     +"\tselected, thus producing a ranking.",
				     "Q",0,"-Q"));

    newVector
      .addElement(new Option("\tSpecify number of attributes to retain from "
			     +"\n\tthe ranking. Overides -T. Use "
			     +"in conjunction with -Q"
			     ,"N",1
			     , "-N <num to select>"));

    newVector
      .addElement(new Option("\tSpecify a theshold by which attributes" 
			     + "\n\tmay be discarded from the ranking."
			     +"\n\tUse in conjuction with -Q","J",1
			     , "-J <threshold>"));

     newVector.addElement(new Option("\tVerbose output for monitoring the "
				     +"search.",
				     "Z",0,"-Z"));
     if ((m_ASEval != null) && 
	 (m_ASEval instanceof OptionHandler)) {
       newVector.addElement(new Option("", "", 0, "\nOptions specific to " 
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
   * -R race type<br>
   * 0 = forward, 1 = backward, 2 = schemata, 3 = rank. <p>
   * 
   * -L significance level <br>
   * significance level to use for t-tests. <p>
   *
   * -T threshold <br>
   * threshold for considering mean errors of two subsets the same <p>
   *
   * -F xval type <br>
   * 0 = 10 fold, 1 = leave-one-out (selected automatically for schemata race
   * <p>
   *
   * -A attribute evaluator <br>
   * the attribute evaluator to use when doing a rank search <p>
   *
   * -Q <br>
   * produce a ranked list of attributes. Selecting this option forces
   * the race type to be forward. Racing continues until *all* attributes
   * have been selected, thus producing a ranked list of attributes. <p>
   *
   * -N number to retain <br>
   * Specify the number of attributes to retain. Overides any threshold. 
   * Use in conjunction with -Q. <p>
   * 
   * -J threshold <br>
   * Specify a threshold by which the AttributeSelection module can discard
   * attributes. Use in conjunction with -Q. <p>
   *
   * -Z <br>
   * Turn on verbose output for monitoring the search <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception {
    String optionString;
    resetOptions();
    
    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      setRaceType(new SelectedTag(Integer.parseInt(optionString),
				  TAGS_SELECTION));
    }
    
    optionString = Utils.getOption('F', options);
    if (optionString.length() != 0) {
      setFoldsType(new SelectedTag(Integer.parseInt(optionString),
				  XVALTAGS_SELECTION));
    }

    optionString = Utils.getOption('L', options);
    if (optionString.length() !=0) {
      Double temp;
      temp = Double.valueOf(optionString);
      setSignificanceLevel(temp.doubleValue());
    }

    optionString = Utils.getOption('T', options);
    if (optionString.length() !=0) {
      Double temp;
      temp = Double.valueOf(optionString);
      setThreshold(temp.doubleValue());
    }

    optionString = Utils.getOption('A', options);
    if (optionString.length() != 0) {
      setAttributeEvaluator(ASEvaluation.forName(optionString, 
			    Utils.partitionOptions(options)));
    }

    setGenerateRanking(Utils.getFlag('Q', options));

    optionString = Utils.getOption('J', options);
    if (optionString.length() != 0) {
      Double temp;
      temp = Double.valueOf(optionString);
      setSelectionThreshold(temp.doubleValue());
    }
    
    optionString = Utils.getOption('N', options);
    if (optionString.length() != 0) {
      setNumToSelect(Integer.parseInt(optionString));
    }

    setDebug(Utils.getFlag('Z', options));
  }

  /**
   * Gets the current settings of BestFirst.
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    int current = 0;
    String[] evaluatorOptions = new String[0];

    if ((m_ASEval != null) && 
	(m_ASEval instanceof OptionHandler)) {
      evaluatorOptions = ((OptionHandler)m_ASEval).getOptions();
    }
    String[] options = new String[17+evaluatorOptions.length];

    options[current++] = "-R"; options[current++] = ""+m_raceType;
    options[current++] = "-L"; options[current++] = ""+getSignificanceLevel();
    options[current++] = "-T"; options[current++] = ""+getThreshold();
    options[current++] = "-F"; options[current++] = ""+m_xvalType;
    if (getGenerateRanking()) {
      options[current++] = "-Q";
    }
    options[current++] = "-N"; options[current++] = ""+getNumToSelect();
    options[current++] = "-J"; options[current++] = ""+getSelectionThreshold();
    if (getDebug()) {
      options[current++] = "-Z";
    }
    
    if (getAttributeEvaluator() != null) {
      options[current++] = "-A";
      options[current++] = getAttributeEvaluator().getClass().getName();
      options[current++] = "--";
      System.arraycopy(evaluatorOptions, 0, options, current, 
		       evaluatorOptions.length);
      current += evaluatorOptions.length;
    }

    
    while (current < options.length) {
      options[current++] = "";
    }

    return  options;
  }




  /**
   * Searches the attribute subset space by racing cross validation
   * errors of competing subsets
   *
   * @param ASEvaluator the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array (not necessarily ordered) of selected attribute indexes
   * @exception Exception if the search can't be completed
   */
  public int[] search (ASEvaluation ASEval, Instances data)
    throws Exception {
    if (!(ASEval instanceof SubsetEvaluator)) {
      throw  new Exception(ASEval.getClass().getName() 
			   + " is not a " 
			   + "Subset evaluator! (RaceSearch)");
    }

    if (ASEval instanceof UnsupervisedSubsetEvaluator) {
      throw new Exception("Can't use an unsupervised subset evaluator "
			  +"(RaceSearch).");
    }

    if (!(ASEval instanceof HoldOutSubsetEvaluator)) {
      throw new Exception("Must use a HoldOutSubsetEvaluator, eg. "
			  +"weka.attributeSelection.ClassifierSubsetEval "
			  +"(RaceSearch)");
    }

    if (!(ASEval instanceof ErrorBasedMeritEvaluator)) {
      throw new Exception("Only error based subset evaluators can be used, "
			  +"eg. weka.attributeSelection.ClassifierSubsetEval "
			  +"(RaceSearch)");
    }

    m_Instances = new Instances(data);
    m_Instances.deleteWithMissingClass();
    if (m_Instances.numInstances() == 0) {
      throw new Exception("All instances have missing class! (RaceSearch)");
    }
    if (m_rankingRequested && m_numToSelect > m_Instances.numAttributes()-1) {
      throw new Exception("More attributes requested than exist in the data "
			  +"(RaceSearch).");
    }
    m_theEvaluator = (HoldOutSubsetEvaluator)ASEval;
    m_numAttribs = m_Instances.numAttributes();
    m_classIndex = m_Instances.classIndex();

    if (m_rankingRequested) {
      m_rankedAtts = new double[m_numAttribs-1][2];
      m_rankedSoFar = 0;
    }

    if (m_xvalType == LEAVE_ONE_OUT) {
      m_numFolds = m_Instances.numInstances();
    } else {
      m_numFolds = 10;
    }

    Random random = new Random(1); // I guess this should really be a parameter?
    m_Instances.randomize(random);
    int [] bestSubset=null;

    switch (m_raceType) {
    case FORWARD_RACE:
    case BACKWARD_RACE: 
      bestSubset = hillclimbRace(m_Instances, random);
      break;
    case SCHEMATA_RACE:
      bestSubset = schemataRace(m_Instances, random);
      break;
    case RANK_RACE:
      bestSubset = rankRace(m_Instances, random);
      break;
    }

    return bestSubset;
  }

  public double [][] rankedAttributes() throws Exception {
    if (!m_rankingRequested) {
      throw new Exception("Need to request a ranked list of attributes "
			  +"before attributes can be ranked (RaceSearch).");
    }
    if (m_rankedAtts == null) {
      throw new Exception("Search must be performed before attributes "
			  +"can be ranked (RaceSearch).");
    }
    
    double [][] final_rank = new double [m_rankedSoFar][2];
    for (int i=0;i<m_rankedSoFar;i++) {
      final_rank[i][0] = m_rankedAtts[i][0];
      final_rank[i][1] = m_rankedAtts[i][1];
    }

    if (m_numToSelect <= 0) {
      if (m_threshold == -Double.MAX_VALUE) {
	m_calculatedNumToSelect = final_rank.length;
      } else {
	determineNumToSelectFromThreshold(final_rank);
      }
    }

    return final_rank;
  }

  private void determineNumToSelectFromThreshold(double [][] ranking) {
    int count = 0;
    for (int i = 0; i < ranking.length; i++) {
      if (ranking[i][1] > m_threshold) {
	count++;
      }
    }
    m_calculatedNumToSelect = count;
  }

  /**
   * Print an attribute set.
   */
  private String printSets(char [][]raceSets) {
    StringBuffer temp = new StringBuffer();
    for (int i=0;i<raceSets.length;i++) {
      for (int j=0;j<m_numAttribs;j++) {
	temp.append(raceSets[i][j]);
      }
      temp.append('\n');
    }
    return temp.toString();
  }

  /**
   * Performs a schemata race---a series of races in parallel.
   * @param data the instances to estimate accuracy over.
   * @param random a random number generator
   * @return an array of selected attribute indices.
   */
  private int [] schemataRace(Instances data, Random random) throws Exception {
    // # races, 2 (competitors in each race), # attributes
    char [][][] parallelRaces;
    int numRaces = m_numAttribs-1;
    Random r = new Random(42);
    int numInstances = data.numInstances();
    Instances trainCV; Instances testCV;
    Instance testInstance;

    // statistics on the racers
    Stats [][] raceStats = new Stats[numRaces][2];
    
    parallelRaces = new char [numRaces][2][m_numAttribs-1];
    char [] base = new char [m_numAttribs];
    for (int i=0;i<m_numAttribs;i++) {
      base[i] = '*';
    }

    int count=0;
    // set up initial races
    for (int i=0;i<m_numAttribs;i++) {
      if (i != m_classIndex) {
	parallelRaces[count][0] = (char [])base.clone();
	parallelRaces[count][1] = (char [])base.clone();
	parallelRaces[count][0][i] = '1';
	parallelRaces[count++][1][i] = '0';
      }
    }
    
    if (m_debug) {
      System.err.println("Initial sets:\n");
      for (int i=0;i<numRaces;i++) {
	System.err.print(printSets(parallelRaces[i])+"--------------\n");
      }
    }
    
    BitSet randomB = new BitSet(m_numAttribs);
    char [] randomBC = new char [m_numAttribs];

    // notes which bit positions have been decided
    boolean [] attributeConstraints = new boolean[m_numAttribs];
    double error;
    int evaluationCount = 0;
    raceSet: while (numRaces > 0) {
      boolean won = false;
      for (int i=0;i<numRaces;i++) {
	raceStats[i][0] = new Stats();
	raceStats[i][1] = new Stats();
      }

      // keep an eye on how many test instances have been randomly sampled
      int sampleCount = 0;
      // run the current set of races
      while (!won) {
	// generate a random binary string
	for (int i=0;i<m_numAttribs;i++) {
	  if (i != m_classIndex) {
	    if (!attributeConstraints[i]) {
	      if (r.nextDouble() < 0.5) {
		randomB.set(i);
	      } else {
		randomB.clear(i);
	      }
	    } else { // this position has been decided from previous races
	      if (base[i] == '1') { 
		randomB.set(i);
	      } else {
		randomB.clear(i);
	      }
	    }
	  }
	}
	
	// randomly select an instance to test on
	int testIndex = Math.abs(r.nextInt() % numInstances);


        // We want to randomize the data the same way for every 
        // learning scheme.
	trainCV = data.trainCV(numInstances, testIndex, new Random (1));
	testCV = data.testCV(numInstances, testIndex);
	testInstance = testCV.instance(0);
	sampleCount++;
	/*	if (sampleCount > numInstances) {
	  throw new Exception("raceSchemata: No clear winner after sampling "
			      +sampleCount+" instances.");
			      } */
	
	m_theEvaluator.buildEvaluator(trainCV);
	
	// the evaluator must retrain for every test point
	error = -((HoldOutSubsetEvaluator)m_theEvaluator).
	  evaluateSubset(randomB, 
			 testInstance,
			 true);
	evaluationCount++;
	
	// see which racers match this random subset
	for (int i=0;i<m_numAttribs;i++) {
	  if (randomB.get(i)) {
	    randomBC[i] = '1';
	  } else {
	    randomBC[i] = '0';
	  }
	}
	//	System.err.println("Random subset: "+(new String(randomBC)));

        checkRaces: for (int i=0;i<numRaces;i++) {
	  // if a pair of racers has evaluated more than num instances
	  // then bail out---unlikely that having any more atts is any
	  // better than the current base set.
	  if (((raceStats[i][0].count + raceStats[i][1].count) / 2) > 
	      (numInstances)) {
	    break raceSet;
	  }
	  for (int j=0;j<2;j++) {
	    boolean matched = true;
	    for (int k =0;k<m_numAttribs;k++) {
	      if (parallelRaces[i][j][k] != '*') {
		if (parallelRaces[i][j][k] != randomBC[k]) {
		  matched = false;
		  break;
		}
	      }
	    }
	    if (matched) { // update the stats for this racer
	      //	      System.err.println("Matched "+i+" "+j);
	      raceStats[i][j].add(error);

		// does this race have a clear winner, meaning we can
		// terminate the whole set of parallel races?
		if (raceStats[i][0].count > m_samples &&
		    raceStats[i][1].count > m_samples) {
		  raceStats[i][0].calculateDerived();
		  raceStats[i][1].calculateDerived();
		  //		  System.err.println(j+" : "+(new String(parallelRaces[i][j])));
		  //		  System.err.println(raceStats[i][0]);
		  //		  System.err.println(raceStats[i][1]);
		  // check the ttest
		  double prob = ttest(raceStats[i][0], raceStats[i][1]);
		  //		  System.err.println("Prob :"+prob);
		  if (prob < m_sigLevel) { // stop the races we have a winner!
		    if (raceStats[i][0].mean < raceStats[i][1].mean) {
		      base = (char [])parallelRaces[i][0].clone();
		      m_bestMerit = raceStats[i][0].mean;
		      if (m_debug) {
			System.err.println("contender 0 won ");
		      }
		    } else {
		      base = (char [])parallelRaces[i][1].clone();
		      m_bestMerit = raceStats[i][1].mean;
		      if (m_debug) {
			System.err.println("contender 1 won");
		      }
		    }
		    if (m_debug) {
		      System.err.println((new String(parallelRaces[i][0]))
				 +" "+(new String(parallelRaces[i][1])));
		      System.err.println("Means : "+raceStats[i][0].mean
					 +" vs"+raceStats[i][1].mean);
		      System.err.println("Evaluations so far : "
					 +evaluationCount);
		    }
		    won = true;
		    break checkRaces;
		  }
		}
	     
	    }
	  }
	}
      }

      numRaces--;
      // set up the next set of races if necessary
      if (numRaces > 0 && won) {
	parallelRaces = new char [numRaces][2][m_numAttribs-1];
	raceStats = new Stats[numRaces][2];
	// update the attribute constraints
	for (int i=0;i<m_numAttribs;i++) {
	  if (i != m_classIndex && !attributeConstraints[i] &&
	      base[i] != '*') {
	    attributeConstraints[i] = true;
	    break;
	  }
	}
	count=0;
	for (int i=0;i<numRaces;i++) {
	  parallelRaces[i][0] = (char [])base.clone();
	  parallelRaces[i][1] = (char [])base.clone();
	  for (int j=count;j<m_numAttribs;j++) {
	    if (j != m_classIndex && parallelRaces[i][0][j] == '*') {
	      parallelRaces[i][0][j] = '1';
	      parallelRaces[i][1][j] = '0';
	      count = j+1;
	      break;
	    }
	  }
	}
	
	if (m_debug) {
	  System.err.println("Next sets:\n");
	  for (int i=0;i<numRaces;i++) {
	    System.err.print(printSets(parallelRaces[i])+"--------------\n");
	  }
	}
      }
    }

    if (m_debug) {
      System.err.println("Total evaluations : "
			 +evaluationCount);
    }
    return attributeList(base);
  }

  // t-test for unequal sample sizes and same variance. Returns probability
  // that observed difference in means is due to chance.
  private double ttest(Stats c1, Stats c2) throws Exception {
    double n1 = c1.count; double n2 = c2.count;
    double v1 = c1.stdDev * c1.stdDev;
    double v2 = c2.stdDev * c2.stdDev;
    double av1 = c1.mean;
    double av2 = c2.mean;
    
    double df = n1 + n2 - 2;
    double cv = (((n1 - 1) * v1) + ((n2 - 1) * v2)) /df;
    double t = (av1 - av2) / Math.sqrt(cv * ((1.0 / n1) + (1.0 / n2)));
    
    return Statistics.incompleteBeta(df / 2.0, 0.5,
				     df / (df + (t * t)));
  }
    
  /**
   * Performs a rank race---race consisting of no attributes, the top
   * ranked attribute, the top two attributes etc. The initial ranking
   * is determined by an attribute evaluator.
   * @param data the instances to estimate accuracy over
   * @param random a random number generator
   * @return an array of selected attribute indices.
   */
  private int [] rankRace(Instances data, Random random) throws Exception {
    char [] baseSet = new char [m_numAttribs];
    char [] bestSet;
    double bestSetError;
    for (int i=0;i<m_numAttribs;i++) {
      if (i == m_classIndex) {
	baseSet[i] = '-';
      } else {
	baseSet[i] = '0';
      }
    }

    int numCompetitors = m_numAttribs-1;
    char [][] raceSets = new char [numCompetitors+1][m_numAttribs];
    int winner;
    
    if (m_ASEval instanceof AttributeEvaluator) {
      // generate the attribute ranking first
      Ranker ranker = new Ranker();
      ((AttributeEvaluator)m_ASEval).buildEvaluator(data);
      m_Ranking = ranker.search((AttributeEvaluator)m_ASEval,data);
    } else {
      GreedyStepwise fs = new GreedyStepwise();
      double [][]rankres; 
      fs.setGenerateRanking(true);
      ((SubsetEvaluator)m_ASEval).buildEvaluator(data);
      fs.search(m_ASEval, data);
      rankres = fs.rankedAttributes();
      m_Ranking = new int[rankres.length];
      for (int i=0;i<rankres.length;i++) {
	m_Ranking[i] = (int)rankres[i][0];
      }
    }

    // set up the race
    raceSets[0] = (char [])baseSet.clone();
    for (int i=0;i<m_Ranking.length;i++) {
      raceSets[i+1] = (char [])raceSets[i].clone();
      raceSets[i+1][m_Ranking[i]] = '1';
    }
    
    if (m_debug) {
      System.err.println("Initial sets:\n"+printSets(raceSets));
    }
    
    // run the race
    double [] winnerInfo = raceSubsets(raceSets, data, true, random);
    bestSetError = winnerInfo[1];
    bestSet = (char [])raceSets[(int)winnerInfo[0]].clone();
    m_bestMerit = bestSetError;
    return attributeList(bestSet);
  }
  
  /**
   * Performs a hill climbing race---all single attribute changes to a
   * base subset are raced in parallel. The winner is chosen and becomes
   * the new base subset and the process is repeated until there is no
   * improvement in error over the base subset.
   * @param data the instances to estimate accuracy over
   * @param random a random number generator
   * @return an array of selected attribute indices.
   */
  private int [] hillclimbRace(Instances data, Random random) throws Exception {
    double baseSetError;
    char [] baseSet = new char [m_numAttribs];
    int rankCount = 0;

    for (int i=0;i<m_numAttribs;i++) {
      if (i != m_classIndex) {
	if (m_raceType == FORWARD_RACE) {
	  baseSet[i] = '0';
	} else {
	  baseSet[i] = '1';
	} 
      } else {
	baseSet[i] = '-';
      }
    }

    int numCompetitors = m_numAttribs-1;
    char [][] raceSets = new char [numCompetitors+1][m_numAttribs];
    int winner;

    raceSets[0] = (char [])baseSet.clone();
    int count = 1;
    // initialize each race set to 1 attribute
    for (int i=0;i<m_numAttribs;i++) {
      if (i != m_classIndex) {
	raceSets[count] = (char [])baseSet.clone();
	if (m_raceType == BACKWARD_RACE) {
	  raceSets[count++][i] = '0';
	} else {
	  raceSets[count++][i] = '1';
	}
      }
    }

    if (m_debug) {
      System.err.println("Initial sets:\n"+printSets(raceSets));
    }
    
    // race the initial sets (base set either no or all features)
    double [] winnerInfo = raceSubsets(raceSets, data, true, random);
    baseSetError = winnerInfo[1];
    m_bestMerit = baseSetError;
    baseSet = (char [])raceSets[(int)winnerInfo[0]].clone();
    if (m_rankingRequested) {
      m_rankedAtts[m_rankedSoFar][0] = (int)(winnerInfo[0]-1);
      m_rankedAtts[m_rankedSoFar][1] = winnerInfo[1];
      m_rankedSoFar++;
    }

    boolean improved = true;
    int j;
    // now race until there is no improvement over the base set or only
    // one competitor remains
    while (improved) {
      // generate the next set of competitors
      numCompetitors--;
      if (numCompetitors == 0) { //race finished!
	break;
      }
      j=0;
      // +1. we'll race against the base set---might be able to bail out
      // of the race if none from the new set are statistically better
      // than the base set. Base set is stored in loc 0.
      raceSets = new char [numCompetitors+1][m_numAttribs];
      for (int i=0;i<numCompetitors+1;i++) {
	raceSets[i] = (char [])baseSet.clone();
	if (i > 0) {
	  for (int k=j;k<m_numAttribs;k++) {
	    if (m_raceType == 1) {
	      if (k != m_classIndex && raceSets[i][k] != '0') {
		raceSets[i][k] = '0';
		j = k+1;
		break;
	      }
	    } else {
	      if (k != m_classIndex && raceSets[i][k] != '1') {
		raceSets[i][k] = '1';
		j = k+1;
		break;
	      }
	    }
	  }
	}
      }
      
      if (m_debug) {
	System.err.println("Next set : \n"+printSets(raceSets));
      }
      improved = false;
      winnerInfo = raceSubsets(raceSets, data, true, random);
      String bs = new String(baseSet); 
      String win = new String(raceSets[(int)winnerInfo[0]]);
      if (bs.compareTo(win) == 0) {
	// race finished
      } else {
	if (winnerInfo[1] < baseSetError || m_rankingRequested) {
	  improved = true;
	  baseSetError = winnerInfo[1];
	  m_bestMerit = baseSetError;
	  // find which att is different
	  if (m_rankingRequested) {
	    for (int i = 0; i < baseSet.length; i++) {
	      if (win.charAt(i) != bs.charAt(i)) {
		m_rankedAtts[m_rankedSoFar][0] = i;
		m_rankedAtts[m_rankedSoFar][1] = winnerInfo[1];
		m_rankedSoFar++;
	      }
	    }
	  }
	  baseSet = (char [])raceSets[(int)winnerInfo[0]].clone();
	} else {
	  // Will get here for a subset whose error is outside the delta
	  // threshold but is not *significantly* worse than the base
	  // subset
	  //throw new Exception("RaceSearch: problem in hillClimbRace");
	}
      }
    }
    return attributeList(baseSet);
  }

  /**
   * Convert an attribute set to an array of indices
   */
  private int [] attributeList(char [] list) {
    int count = 0;

    for (int i=0;i<m_numAttribs;i++) {
      if (list[i] == '1') {
	count++;
      }
    }

    int [] rlist = new int[count];
    count = 0;
     for (int i=0;i<m_numAttribs;i++) {
       if (list[i] == '1') {
	 rlist[count++] = i;
       }
     }

     return rlist;
  }

  /**
   * Races the leave-one-out cross validation errors of a set of
   * attribute subsets on a set of instances.
   * @param raceSets a set of attribute subset specifications
   * @param data the instances to use when cross validating
   * @param baseSetIncluded true if the first attribute set is a
   * base set generated from the previous race
   * @param random a random number generator
   * @return the index of the winning subset
   * @exception Exception if an error occurs during cross validation
   */
  private double [] raceSubsets(char [][]raceSets, Instances data,
				boolean baseSetIncluded, Random random) 
    throws Exception {
    // the evaluators --- one for each subset
    ASEvaluation [] evaluators = 
      m_theEvaluator.makeCopies(m_theEvaluator, raceSets.length);

    // array of subsets eliminated from the race
    boolean [] eliminated = new boolean [raceSets.length];

    // individual statistics
    Stats [] individualStats = new Stats [raceSets.length];

    // pairwise statistics
    PairedStats [][] testers = 
      new PairedStats[raceSets.length][raceSets.length];

    /** do we ignore the base set or not? */
    int startPt = m_rankingRequested ? 1 : 0;

    for (int i=0;i<raceSets.length;i++) {
      individualStats[i] = new Stats();
      for (int j=i+1;j<raceSets.length;j++) {
	testers[i][j] = new PairedStats(m_sigLevel);
      }
    }
    
    BitSet [] raceBitSets = new BitSet[raceSets.length];
    for (int i=0;i<raceSets.length;i++) {
      raceBitSets[i] = new BitSet(m_numAttribs);
      for (int j=0;j<m_numAttribs;j++) {
	if (raceSets[i][j] == '1') {
	  raceBitSets[i].set(j);
	}
      }
    }

    // now loop over the data points collecting leave-one-out errors for
    // each attribute set
    Instances trainCV;
    Instances testCV;
    Instance testInst;
    double [] errors = new double [raceSets.length];
    int eliminatedCount = 0;
    int processedCount = 0;
    // if there is one set left in the race then we need to continue to
    // evaluate it for the remaining instances in order to get an
    // accurate error estimate
    Stats clearWinner = null;
    int foldSize=1;
    processedCount = 0;
    race: for (int i=0;i<m_numFolds;i++) {

      // We want to randomize the data the same way for every 
      // learning scheme.
      trainCV = data.trainCV(m_numFolds, i, new Random (1));
      testCV = data.testCV(m_numFolds, i);
      foldSize = testCV.numInstances();
      
      // loop over the surviving attribute sets building classifiers for this
      // training set
      for (int j=startPt;j<raceSets.length;j++) {
	if (!eliminated[j]) {
	  evaluators[j].buildEvaluator(trainCV);
	}
      }

      for (int z=0;z<testCV.numInstances();z++) {
	testInst = testCV.instance(z);
	processedCount++;

	// loop over surviving attribute sets computing errors for this
	// test point
	for (int zz=startPt;zz<raceSets.length;zz++) {
	  if (!eliminated[zz]) {
	    if (z == 0) {// first test instance---make sure classifier is built
	      errors[zz] = -((HoldOutSubsetEvaluator)evaluators[zz]).
		evaluateSubset(raceBitSets[zz], 
			       testInst,
			       true);
	    } else { // must be k fold rather than leave one out
	      errors[zz] = -((HoldOutSubsetEvaluator)evaluators[zz]).
		evaluateSubset(raceBitSets[zz], 
			       testInst,
			       false);
	    }
	  }
	}

	// now update the stats
	for (int j=startPt;j<raceSets.length;j++) {
	  if (!eliminated[j]) {
	    individualStats[j].add(errors[j]);
	    for (int k=j+1;k<raceSets.length;k++) {
	      if (!eliminated[k]) {
		testers[j][k].add(errors[j], errors[k]);
	      }
	    }
	  }
	}
      
	// test for near identical models and models that are significantly
	// worse than some other model
	if (processedCount > m_samples-1 && 
	    (eliminatedCount < raceSets.length-1)) {
	  for (int j=0;j<raceSets.length;j++) {
	    if (!eliminated[j]) {
	      for (int k=j+1;k<raceSets.length;k++) {
		if (!eliminated[k]) {
		  testers[j][k].calculateDerived();
		  // near identical ?
		  if ((testers[j][k].differencesSignificance == 0) && 
		      (Utils.eq(testers[j][k].differencesStats.mean, 0.0) ||
		      (Utils.gr(m_delta, Math.abs(testers[j][k].
						  differencesStats.mean))))) {
		    // if they're exactly the same and there is a base set
		    // in this race, make sure that the base set is NOT the
		    // one eliminated.
		    if (Utils.eq(testers[j][k].differencesStats.mean, 0.0)) {

		      if (baseSetIncluded) { 
			if (j != 0) {
			  eliminated[j] = true;
			} else {
			  eliminated[k] = true;
			}
			eliminatedCount++;
		      } else {
			eliminated[j] = true;
		      }
		      if (m_debug) {
			System.err.println("Eliminating (identical) "
					   +j+" "+raceBitSets[j].toString()
					   +" vs "+k+" "
					   +raceBitSets[k].toString()
					   +" after "
					   +processedCount
					   +" evaluations\n"
					   +"\nerror "+j+" : "
					   +testers[j][k].xStats.mean
					   +" vs "+k+" : "
					   +testers[j][k].yStats.mean
					   +" diff : "
					   +testers[j][k].differencesStats
					   .mean);
		      }
		    } else {
		      // eliminate the one with the higer error
		      if (testers[j][k].xStats.mean > 
			  testers[j][k].yStats.mean) {
			eliminated[j] = true;
			eliminatedCount++;
			if (m_debug) {
			  System.err.println("Eliminating (near identical) "
					   +j+" "+raceBitSets[j].toString()
					   +" vs "+k+" "
					   +raceBitSets[k].toString()
					   +" after "
					   +processedCount
					   +" evaluations\n"
					   +"\nerror "+j+" : "
					   +testers[j][k].xStats.mean
					   +" vs "+k+" : "
					   +testers[j][k].yStats.mean
					   +" diff : "
					   +testers[j][k].differencesStats
					   .mean);
			}
			break;
		      } else {
			eliminated[k] = true;
			eliminatedCount++;
			if (m_debug) {
			  System.err.println("Eliminating (near identical) "
					   +k+" "+raceBitSets[k].toString()
					   +" vs "+j+" "
					   +raceBitSets[j].toString()
					   +" after "
					   +processedCount
					   +" evaluations\n"
					   +"\nerror "+k+" : "
					   +testers[j][k].yStats.mean
					   +" vs "+j+" : "
					   +testers[j][k].xStats.mean
					   +" diff : "
					   +testers[j][k].differencesStats
					     .mean);
			}
		      }
		    }
		  } else {
		    // significantly worse ?
		    if (testers[j][k].differencesSignificance != 0) {
		      if (testers[j][k].differencesSignificance > 0) {
			eliminated[j] = true;
			eliminatedCount++;
			if (m_debug) {
			  System.err.println("Eliminating (-worse) "
					   +j+" "+raceBitSets[j].toString()
					   +" vs "+k+" "
					   +raceBitSets[k].toString()
					   +" after "
					   +processedCount
					   +" evaluations"
					   +"\nerror "+j+" : "
					   +testers[j][k].xStats.mean
					   +" vs "+k+" : "
					   +testers[j][k].yStats.mean);
			}
			break;
		      } else {
			eliminated[k] = true;
			eliminatedCount++;
			if (m_debug) {
			  System.err.println("Eliminating (worse) "
					   +k+" "+raceBitSets[k].toString()
					   +" vs "+j+" "
					   +raceBitSets[j].toString()
					   +" after "
					   +processedCount
					   +" evaluations"
					   +"\nerror "+k+" : "
					   +testers[j][k].yStats.mean
					   +" vs "+j+" : "
					   +testers[j][k].xStats.mean);
			}
		      }
		    }
		  }
		}    
	      }
	    }
	  }
	}
	// if there is a base set from the previous race and it's the
	// only remaining subset then terminate the race.
	if (eliminatedCount == raceSets.length-1 && baseSetIncluded &&
	    !eliminated[0] && !m_rankingRequested) {
	  break race;
	}
      }
    }

    if (m_debug) {
      System.err.println("*****eliminated count: "+eliminatedCount);
    }
    double bestError = Double.MAX_VALUE;
    int bestIndex=0;
    // return the index of the winner
    for (int i=startPt;i<raceSets.length;i++) {
      if (!eliminated[i]) {
	individualStats[i].calculateDerived();
	if (m_debug) {
	  System.err.println("Remaining error: "+raceBitSets[i].toString()
			     +" "+individualStats[i].mean);
	}
	if (individualStats[i].mean < bestError) {
	  bestError = individualStats[i].mean;
	  bestIndex = i;
	}
      }
    }

    double [] retInfo = new double[2];
    retInfo[0] = bestIndex;
    retInfo[1] = bestError;
    
    if (m_debug) {
      System.err.print("Best set from race : ");
      
      for (int i=0;i<m_numAttribs;i++) {
	if (raceSets[bestIndex][i] == '1') {
	  System.err.print('1');
	} else {
	  System.err.print('0');
	}
      }
      System.err.println(" :"+bestError+" Processed : "+(processedCount)
			 +"\n"+individualStats[bestIndex].toString());
    }
    return retInfo;
  }

  public String toString() {
    StringBuffer text = new StringBuffer();
    
    text.append("\tRaceSearch.\n\tRace type : ");
    switch (m_raceType) {
    case FORWARD_RACE: 
      text.append("forward selection race\n\tBase set : no attributes");
      break;
    case BACKWARD_RACE:
      text.append("backward elimination race\n\tBase set : all attributes");
      break;
    case SCHEMATA_RACE:
      text.append("schemata race\n\tBase set : no attributes");
      break;
    case RANK_RACE:
      text.append("rank race\n\tBase set : no attributes\n\t");
      text.append("Attribute evaluator : "
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
      break;
    }
    text.append("\n\tCross validation mode : ");
    if (m_xvalType == TEN_FOLD) {
      text.append("10 fold");
    } else {
      text.append("Leave-one-out");
    }

    text.append("\n\tMerit of best subset found : ");
    int fieldwidth = 3;
    double precision = (m_bestMerit - (int)m_bestMerit);
    if (Math.abs(m_bestMerit) > 0) {
      fieldwidth = (int)Math.abs((Math.log(Math.abs(m_bestMerit)) / 
				  Math.log(10)))+2;
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

  /**
   * Reset the search method.
   */
  protected void resetOptions () {
    m_sigLevel = 0.001;
    m_delta = 0.001;
    m_ASEval = new GainRatioAttributeEval();
    m_Ranking = null;
    m_raceType = FORWARD_RACE;
    m_debug = false;
    m_theEvaluator = null;
    m_bestMerit = -Double.MAX_VALUE;
    m_numFolds = 10;
  }
}

