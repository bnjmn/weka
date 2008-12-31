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
 *    ScatterSearchV1.java
 *    Copyright (C) 2008 Adrian Pino
 *    Copyright (C) 2008 University of Waikato, Hamilton, NZ
 *
 */

package weka.attributeSelection;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Enumeration;
import java.util.List;
import java.util.Random;
import java.util.Vector;

import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.RevisionUtils;
import weka.core.SelectedTag;
import weka.core.Tag;
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;


/**
 * Class for performing the Sequential Scatter Search. <p>
 *
 <!-- globalinfo-start -->
 * Scatter Search :<br/>
 * <br/>
 * Performs an Scatter Search  through the space of attribute subsets. Start with a population of many significants and diverses subset  stops when the result is higher than a given treshold or there's not more improvement<br/>
 * For more information see:<br/>
 * <br/>
 * Felix Garcia Lopez (2004). Solving feature subset selection problem by a Parallel Scatter Search. Elsevier.
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -Z &lt;num&gt;
 *  Specify the number of subsets to generate 
 *  in the initial population..</pre>
 * 
 * <pre> -T &lt;threshold&gt;
 *  Specify the treshold used for considering when a subset is significant.</pre>
 * 
 * <pre> -R &lt;0 = greedy combination | 1 = reduced greedy combination &gt;
 *  Specify the kind of combiantion 
 *  for using it in the combination method.</pre>
 * 
 * <pre> -S &lt;seed&gt;
 *  Set the random number seed.
 *  (default = 1)</pre>
 * 
 * <pre> -D
 *  Verbose output for monitoring the search.</pre>
 * 
 <!-- options-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;book{L�pez2004,
 *    author = {F�lix Garc�a L�pez},
 *    month = {October},
 *    publisher = {Elsevier},
 *    title = {Solving feature subset selection problem by a Parallel Scatter Search},
 *    year = {2004},
 *    language = {English}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 * from the Book: Solving feature subset selection problem by a Parallel Scatter Search, F�lix Garc�a L�pez.
 * @author Adrian Pino (apinoa@facinf.uho.edu.cu)
 * @version $Revision$
 *
 */

public class ScatterSearchV1 extends ASSearch
  implements OptionHandler, TechnicalInformationHandler {

  /** for serialization */
  static final long serialVersionUID = -8512041420388121326L;

  /** number of attributes in the data */
  private int m_numAttribs;

  /** holds the class index */
  private int m_classIndex;

  /** holds the treshhold that delimits the good attributes */
  private double m_treshold;

  /** the initial threshold */
  private double m_initialThreshold;

  /** the kind of comination betwen parents ((0)greedy combination/(1)reduced greedy combination)*/
  int m_typeOfCombination;

  /** random number generation */
  private Random m_random;

  /** seed for random number generation */
  private int m_seed;

  /** verbose output for monitoring the search and debugging */
  private boolean m_debug = false;

  /** holds a report of the search */
  private StringBuffer m_InformationReports;

  /** total number of subsets evaluated during a search */
  private int m_totalEvals;

  /** holds the merit of the best subset found */
  protected double m_bestMerit;

  /** time for procesing the search method */
  private long m_processinTime;

  /** holds the Initial Population of Subsets*/
  private List<Subset> m_population;

  /** holds the population size*/
  private int m_popSize;

  /** holds the user selected initial population size */
  private int m_initialPopSize;

  /** if no initial user pop size, then this holds the initial
   * pop size calculated from the number of attributes in the data
   * (for use in the toString() method)
   */
  private int m_calculatedInitialPopSize;

  /** holds the subsets most significants and diverses
   * of the population (ReferenceSet).
   *
   * (transient because the subList() method returns
   * a non serializable Object).
   */
  private transient List<Subset> m_ReferenceSet;

  /** holds the greedy combination(reduced or not) of all the subsets of the ReferenceSet*/
  private transient List<Subset> m_parentsCombination;

  /**holds the attributes ranked*/
  private List<Subset> m_attributeRanking;

  /**Evaluator used to know the significance of a subset (for guiding the search)*/
  private SubsetEvaluator ASEvaluator =null;


  /** kind of combination */
  protected static final int COMBINATION_NOT_REDUCED = 0;
  protected static final int COMBINATION_REDUCED = 1;  ;
  public static final Tag [] TAGS_SELECTION = {
    new Tag(COMBINATION_NOT_REDUCED, "Greedy Combination"),
    new Tag(COMBINATION_REDUCED, "Reduced Greedy Combination")
  };

  /**
   * Returns a string describing this search method
   * @return a description of the search suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Scatter Search :\n\nPerforms an Scatter Search  "
      +"through "
      +"the space of attribute subsets. Start with a population of many significants and diverses subset "
      +" stops when the result is higher than a given treshold or there's not more improvement\n"
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
    TechnicalInformation        result;

    result = new TechnicalInformation(Type.BOOK);
    result.setValue(Field.AUTHOR, "F�lix Garc�a L�pez");
    result.setValue(Field.MONTH, "October");
    result.setValue(Field.YEAR, "2004");
    result.setValue(Field.TITLE, "Solving feature subset selection problem by a Parallel Scatter Search");
    result.setValue(Field.PUBLISHER, "Elsevier");
    result.setValue(Field.LANGUAGE, "English");

    return result;
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.0$");
  }

  public ScatterSearchV1 () {
    resetOptions();
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String tresholdTipText() {
    return "Set the treshold that subsets most overcome to be considered as significants";
  }

  /**
   * Set the treshold
   *
   * @param treshold for identifyng significant subsets
   */
  public void setTreshold (double treshold) {
    m_initialThreshold = treshold;
  }

  /**
   * Get the treshold
   *
   * @return the treshold that subsets most overcome to be considered as significants
   */
  public double getTreshold () {
    return m_initialThreshold;
  }


  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String populationSizeTipText() {
    return "Set the number of subset to generate in the initial Population";
  }

  /**
   * Set the population size
   *
   * @param size the number of subset in the initial population
   */
  public void setPopulationSize (int size) {
    m_initialPopSize = size;
  }

  /**
   * Get the population size
   *
   * @return the number of subsets to generate in the initial population
   */
  public int getPopulationSize () {
    return m_initialPopSize;
  }


  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String combinationTipText() {
    return "Set the kind of combination for using it to combine ReferenceSet subsets.";
  }

  /**
   * Set the kind of combination
   *
   * @param c the kind of combination of the search
   */
  public void setCombination (SelectedTag c) {
    if (c.getTags() == TAGS_SELECTION) {
      m_typeOfCombination = c.getSelectedTag().getID();
    }
  }

  /**
   * Get the combination
   *
   * @return the kind of combination used in the Combination method
   */
  public SelectedTag getCombination () {
    return new SelectedTag(m_typeOfCombination, TAGS_SELECTION);
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "Set the random seed.";
  }

  /**
   * set the seed for random number generation
   * @param s seed value
   */
  public void setSeed(int s) {
    m_seed = s;
  }

  /**
   * get the value of the random number generator's seed
   * @return the seed for random number generation
   */
  public int getSeed() {
    return m_seed;
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
   * Returns an enumeration describing the available options.
   * @return an enumeration of all the available options.
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(6);

    newVector.addElement(new Option("\tSpecify the number of subsets to generate "
    	                            + "\n\tin the initial population.."
				    ,"Z",1
				    , "-Z <num>"));
    newVector.addElement(new Option("\tSpecify the treshold used for considering when a subset is significant."
				    , "T", 1
				    , "-T <threshold>"));
    newVector.addElement(new Option("\tSpecify the kind of combiantion "
				    + "\n\tfor using it in the combination method."
				    , "R", 1, "-R <0 = greedy combination | 1 = reduced greedy combination >"));
	  newVector.addElement(new Option("\tSet the random number seed."
                                    +"\n\t(default = 1)"
                                    , "S", 1, "-S <seed>"));
    newVector.addElement(new Option("\tVerbose output for monitoring the search.","D",0,"-D"));

    return  newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * Valid options are: <p>
   *
   * -Z <br>
   * Specify the number of subsets to generate in the initial population.<p>
   *
   * -T <start set> <br>
   * Specify the treshold used for considering when a subset is significant. <p>
   *
   * -R <br>
   * Specify the kind of combiantion. <p>
   *
   * -S <br>
   *  Set the random number seed.
   *  (default = 1)
   *
   * -D <br>
   *  Verbose output for monitoring the search
   *  (default = false)
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   *
   **/
  public void setOptions (String[] options)
    throws Exception {
    String optionString;
    resetOptions();

    optionString = Utils.getOption('Z', options);
    if (optionString.length() != 0) {
      setPopulationSize(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('T', options);
    if (optionString.length() != 0) {
      setTreshold(Double.parseDouble(optionString));
    }

    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      setCombination(new SelectedTag(Integer.parseInt(optionString),
				   TAGS_SELECTION));
    } else {
      setCombination(new SelectedTag(COMBINATION_NOT_REDUCED, TAGS_SELECTION));
    }

    optionString = Utils.getOption('S', options);
    if (optionString.length() != 0) {
      setSeed(Integer.parseInt(optionString));
    }

    setDebug(Utils.getFlag('D', options));
  }

  /**
   * Gets the current settings of ReliefFAttributeEval.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[9];
    int current = 0;

    options[current++] = "-T";
    options[current++] = "" + getTreshold ();

    options[current++] = "-Z";
    options[current++] = ""+getPopulationSize ();

    options[current++] = "-R";
    options[current++] = ""+String.valueOf (getCombination ().getSelectedTag ().getID ());

    options[current++] = "-S";
    options[current++] = "" + getSeed();

    if (getDebug())
      options[current++] = "-D";

    while (current < options.length)
      options[current++] = "";

    return  options;
  }

  /**
   * returns a description of the search.
   * @return a description of the search as a String.
   */
  public String toString() {
    StringBuffer FString = new StringBuffer();
    FString.append("\tScatter Search "
		   + "\n\tInit Population: "+m_calculatedInitialPopSize);

    FString.append("\n\tKind of Combination: "
		   +getCombination ().getSelectedTag ().getReadable ());

		FString.append("\n\tRandom number seed: "+m_seed);

		FString.append("\n\tDebug: "+m_debug);

    FString.append("\n\tTreshold: "
		   +Utils.doubleToString(Math.abs(getTreshold ()),8,3)+"\n");

    FString.append("\tTotal number of subsets evaluated: "
		    + m_totalEvals + "\n");

    FString.append("\tMerit of best subset found: "
		    +Utils.doubleToString(Math.abs(m_bestMerit),8,3)+"\n");

    /* FString.append("\tTime procesing the search space: "
		    +(double)m_processinTime/1000+" seconds\n"); */

    if(m_debug)
      return FString.toString()+"\n\n"+m_InformationReports.toString ();

    return FString.toString();
  }


  /**
   * Searches the attribute subset space using Scatter Search.
   *
   * @param ASEval the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array of selected attribute indexes
   * @exception Exception if the search can't be completed
   */
  public int[] search(ASEvaluation ASEval, Instances data)
    throws Exception{

    m_totalEvals = 0;
    m_popSize = m_initialPopSize;
    m_calculatedInitialPopSize = m_initialPopSize;
    m_treshold = m_initialThreshold;
    m_processinTime =System.currentTimeMillis ();
    m_InformationReports = new StringBuffer();

    m_numAttribs =data.numAttributes ();
    m_classIndex =data.classIndex ();

    if(m_popSize<=0) {
      m_popSize =m_numAttribs/2;
      m_calculatedInitialPopSize = m_popSize;
    }

    ASEvaluator =(SubsetEvaluator)ASEval;

    if(!(m_treshold >= 0)){
      m_treshold =calculateTreshhold();
      m_totalEvals++;
    }

    m_random = new Random(m_seed);

    m_attributeRanking =RankEachAttribute();

    CreatePopulation(m_popSize);

    int bestSolutions =m_popSize/4;
    int divSolutions =m_popSize/4;

    if(m_popSize < 4){

      bestSolutions = m_popSize/2;
      divSolutions = m_popSize/2;

      if(m_popSize == 1) return attributeList(((Subset)m_population.get (0)).subset);
    }


    m_ReferenceSet =new ArrayList<Subset>();

    for (int i = 0; i<m_population.size (); i++) {
      m_ReferenceSet.add (m_population.get (i)) ;
    }


    GenerateReferenceSet(m_ReferenceSet, bestSolutions, divSolutions);


    m_InformationReports.append ("Population: "+m_population.size ()+"\n");
    m_InformationReports.append ("merit    \tsubset\n");

    for (int i = 0; i < m_population.size (); i++)
    	m_InformationReports.append (printSubset (m_population.get (i)));


    m_ReferenceSet =m_ReferenceSet.subList (0,bestSolutions+divSolutions);


    /*TEST*/
    m_InformationReports.append ("\nReferenceSet:");
    m_InformationReports.append ("\n----------------Most Significants Solutions--------------\n");
    for (int i = 0; i<m_ReferenceSet.size (); i++) {
      if(i ==bestSolutions) m_InformationReports.append ("----------------Most Diverses Solutions--------------\n");
      m_InformationReports.append(printSubset (m_ReferenceSet.get (i)));
    }


    Subset bestTemp =new Subset(new BitSet(m_numAttribs),0);

    while (!(bestTemp.isEqual (m_ReferenceSet.get (0))) /*|| (m_treshold > bestTemp.merit)*/) {
	  //while(){
	      CombineParents();
		    ImproveSolutions();
   // }
        bestTemp =m_ReferenceSet.get (0);

        int numBest =m_ReferenceSet.size ()/2;
        int numDiverses =m_ReferenceSet.size ()/2;

	      UpdateReferenceSet(numBest, numDiverses);
	      m_ReferenceSet = m_ReferenceSet.subList (0,numBest+numDiverses);

    }

    m_InformationReports.append("\nLast Reference Set Updated:\n");
    m_InformationReports.append ("merit    \tsubset\n");

    for (int i = 0; i <m_ReferenceSet.size (); i++)
    	m_InformationReports.append (printSubset (m_ReferenceSet.get (i)));


 	  m_bestMerit =bestTemp.merit;

 	  m_processinTime =System.currentTimeMillis () -m_processinTime;

    return attributeList (bestTemp.subset);
  }

  /**
   * Generate the a ReferenceSet containing the n best solutions and the m most diverse solutions of
   * the initial Population.
   *
   * @param ReferenceSet the ReferenceSet for storing these solutions
   * @param bestSolutions the number of the most pure solutions.
   * @param divSolutions the number of the most diverses solutions acording to the bestSolutions.
   */
  public void GenerateReferenceSet(List<Subset> ReferenceSet, int bestSolutions, int divSolutions){

    //Sorting the Initial ReferenceSet
    ReferenceSet =bubbleSubsetSort (ReferenceSet);

    // storing all the attributes that are now in the ReferenceSet (just till bestSolutions)
    BitSet allBits_RefSet =getAllBits (ReferenceSet.subList (0,bestSolutions));

    // for stopping when ReferenceSet.size () ==bestSolutions+divSolutions
    int refSetlength =bestSolutions;
    int total =bestSolutions+divSolutions;

    while (refSetlength <total) {

      List<Integer> aux =new ArrayList<Integer>();

      for (int i =refSetlength; i <ReferenceSet.size (); i ++) {
        aux.add (SimetricDiference (((Subset)ReferenceSet.get (i)).clone (),allBits_RefSet));
	  }


	  int mostDiv =getIndexofBiggest(aux);
	  ReferenceSet.set(refSetlength, ReferenceSet.get (refSetlength+mostDiv));
	  //ReferenceSet.remove (refSetlength +mostDiv);

	  refSetlength++;

	  allBits_RefSet =getAllBits (ReferenceSet.subList (0,refSetlength));
	}

	  ReferenceSet =filterSubset (ReferenceSet,refSetlength);
  }

  /**
   * Update the ReferenceSet putting the new obtained Solutions there
   *
   * @param numBestSolutions the number of the most pure solutions.
   * @param numDivsSolutions the number of the most diverses solutions acording to the bestSolutions.
  */
  public void UpdateReferenceSet(int numBestSolutions, int numDivsSolutions){

    for (int i = 0; i <m_parentsCombination.size (); i++) m_ReferenceSet.add (i, m_parentsCombination.get (i));

      GenerateReferenceSet (m_ReferenceSet,numBestSolutions,numDivsSolutions);
  }

  /**
   * Improve the solutions previously combined by adding the attributes that improve that solution
   * @exception Exception if there is some trouble evaluating the candidate solutions
   */
  public void ImproveSolutions()
    throws Exception{

    for (int i = 0; i<m_parentsCombination.size (); i++) {

      BitSet aux1 =(BitSet)((Subset)m_parentsCombination.get (i)).subset.clone ();
      List<Subset> ranking =new ArrayList<Subset>();

      /*
      for(int j=aux1.nextClearBit (0); j<=m_numAttribs; j=aux1.nextClearBit(j+1)){
      	if(j ==m_classIndex)continue;

      	BitSet aux2 =new BitSet(m_numAttribs);
      	aux2.set (j);

      	double merit =ASEvaluator.evaluateSubset (aux2);
      	m_totalEvals++;

      	ranking.add (new Subset((BitSet)aux2.clone (), merit));
      }

      ranking =bubbleSubsetSort (ranking);
      */

      for (int k =0; k <m_attributeRanking.size (); k ++) {
        Subset s1 =((Subset)m_attributeRanking.get (k)).clone ();
        BitSet b1 =(BitSet)s1.subset.clone ();

        Subset s2 =((Subset)m_parentsCombination.get (i)).clone ();
        BitSet b2 =(BitSet)s2.subset.clone ();

        if(b2.get (b1.nextSetBit (0))) continue;

        b2.or (b1);
        double newMerit =ASEvaluator.evaluateSubset (b2);
        m_totalEvals++;

        if(newMerit <= s2.merit)break;

        m_parentsCombination.set (i,new Subset(b2,newMerit));
	    }

      filterSubset (m_parentsCombination,m_ReferenceSet.size());
    }
  }

   /**
   * Combine all the posible pair solutions existing in the Population
   *
   * @exception Exception if there is some trouble evaluating the new childs
   */
  public void CombineParents()
    throws Exception{

    m_parentsCombination =new ArrayList<Subset>();

    // this two 'for' are for selecting parents in the refSet
	for (int i= 0; i <m_ReferenceSet.size ()-1; i ++) {
	  for (int j= i+1; j <m_ReferenceSet.size (); j ++) {

	    // Selecting parents
	    Subset parent1 =m_ReferenceSet.get (i);
	    Subset parent2 =m_ReferenceSet.get (j);

	    // Initializing childs Intersecting parents
	    Subset child1 = intersectSubsets (parent1, parent2);
	    Subset child2 =child1.clone ();

	    // Initializing childs Intersecting parents
	    Subset simDif =simetricDif (parent1, parent2, getCombination ().getSelectedTag ().getID ());

	    BitSet aux =(BitSet)simDif.subset.clone ();

	    boolean improvement =true;

	    while (improvement) {

	      Subset best1 =getBestgen (child1,aux);
	      Subset best2 =getBestgen (child2,aux);

	      if(best1 !=null || best2!=null){

	        if(best2 ==null){
	       	  child1 =best1.clone ();
	       	  continue;
	       	}
	       	if(best1 ==null){
	       	  child2 =best2.clone ();
	       	  continue;
	       	}
	       	if(best1 !=null && best2 !=null){
	       	  double merit1 =best1.merit;
	       	  double merit2 =best2.merit;

	       	  if(merit1 >merit2){
	       	  	child1 =best1.clone ();
	       	  	continue;
	       	  }
	       	  if(merit1 <merit2){
	       	  	child2 =best2.clone ();
	       	  	continue;
	       	  }
	       	  if(merit1 ==merit2){
	       	  	if(best1.subset.cardinality () > best2.subset.cardinality ()){
	       	  	  child2 =best2.clone ();
	       	  	  continue;
	       	  	}
	       	  	if(best1.subset.cardinality () < best2.subset.cardinality ()){
	       	  	  child1 =best1.clone ();
	       	  	  continue;
	       	  	}
	       	  	if(best1.subset.cardinality () == best2.subset.cardinality ()){
	       	  	  double random = m_random.nextDouble ();
	       	  	  if(random < 0.5)child1 =best1.clone ();
	       	  	  else child2 =best2.clone ();
	       	  	  continue;
	       	    }
	       	  }
	        }

	      }else{
	       	m_parentsCombination.add (child1);
	       	m_parentsCombination.add (child2);
	       	improvement =false;
	      }
	    }
	  }
    }
    m_parentsCombination = filterSubset (m_parentsCombination,m_ReferenceSet.size());

    GenerateReferenceSet (m_parentsCombination,m_ReferenceSet.size ()/2, m_ReferenceSet.size ()/2);
    m_parentsCombination = m_parentsCombination.subList (0, m_ReferenceSet.size ());

  }
  /**
   * Create the initial Population
   *
   * @param popSize the size of the initial population
   * @exception Exception if there is a trouble evaluating any solution
   */
  public void CreatePopulation(int popSize)
    throws Exception{

    InitPopulation(popSize);

	/** Delimit the best attributes from the worst*/
	int segmentation =m_numAttribs/2;

	/*TEST*/
  /*  System.out.println ("AttributeRanking");
    for (int i = 0; i <attributeRanking.size (); i++){
      if(i ==segmentation)System.out.println ("-------------------------SEGMENTATION------------------------");
      printSubset (attributeRanking.get (i));
    }
    */
	for (int i = 0; i<m_popSize; i++) {

	  List<Subset> attributeRankingCopy = new ArrayList<Subset>();
	  for (int j = 0; j<m_attributeRanking.size (); j++) attributeRankingCopy.add (m_attributeRanking.get (j));


	  double last_evaluation =-999;
	  double current_evaluation =0;

	  boolean doneAnew =true;

	  while (true) {

	    // generate a random number in the interval[0..segmentation]
	  	int random_number = m_random.nextInt (segmentation+1) /*generateRandomNumber (segmentation)*/;

	  	if(doneAnew && i <=segmentation)random_number =i;
	  	doneAnew =false;

	  	Subset s1 =((Subset)attributeRankingCopy.get (random_number)).clone ();
	  	Subset s2 =((Subset)m_population.get (i)).clone ();


	  	// trying to add a new gen in the chromosome i of the population
	  	Subset joiners =joinSubsets (s1, s2 );

	  	current_evaluation =joiners.merit;

	  	if(current_evaluation > last_evaluation){
	  	  m_population.set (i,joiners);
	  	  last_evaluation =current_evaluation;

	  	  try {
	        attributeRankingCopy.set (random_number, attributeRankingCopy.get (segmentation+1));
	  	    attributeRankingCopy.remove (segmentation+1);
          }catch (IndexOutOfBoundsException ex) {
            attributeRankingCopy.set (random_number,new Subset(new BitSet(m_numAttribs),0));
          continue;
           }
	  	}
	  	else{
	  	// there's not more improvement
	  	break;
	  	}


	  }
	}

	//m_population =bubbleSubsetSort (m_population);
  }


    /**
   * Rank all the attributes individually acording to their merits
   *
   * @return an ordered List  of Subsets with just one attribute
   * @exception Exception if the evaluation can not be completed
   */
  public List<Subset> RankEachAttribute()
    throws Exception{

    List<Subset> result =new ArrayList<Subset>();

    for (int i = 0; i<m_numAttribs; i++) {
      if(i==m_classIndex)continue;

	  BitSet an_Attribute =new BitSet(m_numAttribs);
	  an_Attribute.set (i);

	  double merit =ASEvaluator.evaluateSubset (an_Attribute);
      m_totalEvals++;

	  result.add (new Subset(an_Attribute, merit));
	}

	return bubbleSubsetSort(result);
  }


    //..........


// m�todos auxiliares generales


    /**
   * Evaluate each gen of a BitSet inserted in a Subset and get the most significant for that Subset
   *
   * @return a new Subset with the union of subset and the best gen of gens.
   *  in case that there's not improvement with each gen return null
   * @exception Exception if the evaluation of can not be completed
   */
  public Subset getBestgen(Subset subset, BitSet gens)
    throws Exception{
    Subset result =null;

    double merit1 =subset.merit;

    for(int i =gens.nextSetBit(0); i >=0; i =gens.nextSetBit(i+1)){
      BitSet aux =(BitSet)subset.subset.clone ();

	  if(aux.get (i))continue;
	  aux.set (i);

	  double merit2 =ASEvaluator.evaluateSubset (aux);
	  m_totalEvals++;

	  if(merit2 >merit1){
	    merit1 =merit2;
	    result =new Subset(aux,merit1);
	  }
	}

	return result;
  }

  /**
   * Sort a List of subsets according to their merits
   *
   * @param subsetList the subsetList to be ordered
   * @return a List with ordered subsets
   */
  public List<Subset> bubbleSubsetSort(List<Subset> subsetList){
    List<Subset> result =new ArrayList<Subset>();

    for (int i = 0; i<subsetList.size ()-1; i++) {
      Subset subset1 =subsetList.get (i);
      double merit1 =subset1.merit;

      for (int j = i+1; j<subsetList.size (); j++) {
      	Subset subset2 =subsetList.get (j);
      	double merit2 =subset2.merit;

      	if(merit2 > merit1){
      	  Subset temp =subset1;

      	  subsetList.set (i,subset2);
      	  subsetList.set (j,temp);

      	  subset1 =subset2;
      	  merit1 =subset1.merit;
      	}
	    }
    }
	  return subsetList;
  }


  /**
   * get the index in a List where this have the biggest number
   *
   * @param simDif the Lists of numbers for getting from them the index of the bigger
   * @return an index that represents where the bigest number is.
   */
  public int getIndexofBiggest(List<Integer> simDif){
    int aux =-99999;
    int result1 =-1;
    List<Integer> equalSimDif =new ArrayList<Integer>();

    if(simDif.size ()==0) return -1;

    for (int i = 0; i<simDif.size (); i++) {
      if(simDif.get (i) >aux){
        aux =simDif.get (i);
      	result1 =i;
      }
	}

	for (int i =0; i <simDif.size (); i++) {
	  if(simDif.get (i) ==aux){
      	equalSimDif.add (i);
    }
	}

	int finalResult =equalSimDif.get (m_random.nextInt (equalSimDif.size ()) /*generateRandomNumber (equalSimDif.size ()-1)*/);

	return finalResult;
  }

   /**
   * Save in Bitset all the gens that are in many others subsets.
   *
   * @param subsets the Lists of subsets for getting from them all their gens
   * @return a Bitset with all the gens contained in many others subsets.
   */
  public BitSet getAllBits(List<Subset> subsets){
    BitSet result =new BitSet(m_numAttribs);

    for (int i =0; i <subsets.size (); i ++) {
      BitSet aux =((Subset)subsets.get (i)).clone ().subset;

      for(int j=aux.nextSetBit(0); j>=0; j=aux.nextSetBit(j+1)) {
      	result.set (j);
	    }
	  }

	return result;
  }

   /**
   * Creating space for introducing the population
   *
   * @param popSize the number of subset in the initial population
   */
  public void InitPopulation(int popSize){
    m_population =new ArrayList<Subset>();
    for (int i = 0; i<popSize; i++)m_population.add (new Subset(new BitSet(m_numAttribs),0));
  }

   /**
   * Join two subsets
   *
   * @param subset1 one of the subsets
   * @param subset2 the other subset
   * @return a new Subset that is te result of the Join
   * @exception Exception if the evaluation of the subsets can not be completed
   */
  public Subset joinSubsets(Subset subset1, Subset subset2)
    throws Exception{
    BitSet b1 =(BitSet)subset1.subset.clone ();
    BitSet b2 =(BitSet)subset2.subset.clone ();

    b1.or (b2);

    double newMerit =ASEvaluator.evaluateSubset (b1);
    m_totalEvals++;

    return new Subset((BitSet)b1.clone (), newMerit);
    }

  /**
   * Intersects two subsets
   *
   * @param subset1 one of the subsets
   * @param subset2 the other subset
   * @return a new Subset that is te result of the intersection
   * @exception Exception if the evaluation of the subsets can not be completed
   */
  public Subset intersectSubsets(Subset subset1, Subset subset2)
    throws Exception{
    BitSet b1 =(BitSet)subset1.subset.clone ();
    BitSet b2 =(BitSet)subset2.subset.clone ();

    b1.and (b2);

    double newMerit =ASEvaluator.evaluateSubset (b1);
    m_totalEvals++;

    return new Subset((BitSet)b1.clone (), newMerit);
  }

  public Subset simetricDif(Subset subset1, Subset subset2, int mode)
    throws Exception{
    BitSet b1 =(BitSet)subset1.subset.clone ();
    BitSet b2 =(BitSet)subset2.subset.clone ();

    b1.xor (b2);

    double newMerit =ASEvaluator.evaluateSubset (b1);
    m_totalEvals++;

    Subset result =new Subset((BitSet)b1.clone (), newMerit);

    if(mode == COMBINATION_REDUCED){

      double avgAcurracy =0;
      int totalSolutions =0;
      List<Subset> weightVector =new ArrayList<Subset>();

      BitSet res =result.subset;
      for(int i=res.nextSetBit(0); i>=0; i=res.nextSetBit(i+1)){

      	double merits =0;
        int numSolutions =0;
        Subset solution =null;

        for (int j = 0; j <m_ReferenceSet.size (); j ++) {
          solution =(Subset)m_ReferenceSet.get (j);
          if(solution.subset.get (i)){
            merits +=solution.merit;
            numSolutions ++;
          }
	    }
	    BitSet b =new BitSet(m_numAttribs);
	    b.set (i);
	    Subset s =new Subset(b, merits/(double)numSolutions);
	    weightVector.add (s);

	    avgAcurracy +=merits;
        totalSolutions ++;

      }
      avgAcurracy =avgAcurracy/(double)totalSolutions;

      BitSet newResult =new BitSet(m_numAttribs);
      for (int i = 0; i<weightVector.size (); i++) {
      	Subset aux =weightVector.get (i);
      	if(aux.merit >=avgAcurracy){
      	  newResult.or (aux.subset);
      	}
	  }
	  double merit =ASEvaluator.evaluateSubset (newResult);
	  result =new Subset(newResult, merit);

    }

    return result;
  }

  public int generateRandomNumber(int limit){

    return (int)Math.round (Math.random ()*(limit+0.4));
  }



    /**
   * Calculate the treshold of a dataSet given an evaluator
   *
   * @return the treshhold of the dataSet
   * @exception Exception if the calculation can not be completed
   */
  public double calculateTreshhold()
    throws Exception{
    BitSet fullSet =new BitSet(m_numAttribs);

    for (int i= 0; i< m_numAttribs; i++) {
      if(i ==m_classIndex)continue;
       	fullSet.set (i);
	}

	return ASEvaluator.evaluateSubset (fullSet);
  }

    /**
   * Calculate the Simetric Diference of two subsets
   *
   * @return the Simetric Diference
   * @exception Exception if the calculation can not be completed
   */
    public int SimetricDiference(Subset subset, BitSet bitset){
      BitSet aux =subset.clone ().subset;
      aux.xor (bitset);

      return aux.cardinality ();
    }

    /**
   * Filter a given Lis of Subsets removing the equals subsets
   * @param subsetList to filter
   * @param preferredSize the preferred size of the new List (if it is -1, then the filter is make it
   *                      for all subsets, else then the filter method stops when the given preferred
   *                      size is reached or all the subset have been filtered).
   * @return a new List filtered
   * @exception Exception if the calculation can not be completed
   */
    public List<Subset> filterSubset(List<Subset> subsetList, int preferredSize){
      if(subsetList.size () <=preferredSize && preferredSize !=-1)return subsetList;

      for (int i =0; i <subsetList.size ()-1; i ++) {
      	for (int j =i+1; j <subsetList.size (); j ++) {
      	  Subset focus =subsetList.get (i);
      	  if(focus.isEqual (subsetList.get (j))){
      	    subsetList.remove (j);
      	    j--;

      	    if(subsetList.size () <=preferredSize && preferredSize !=-1)return subsetList;
      	  }
		}
	  }
	  return subsetList;
    }
    //..........


// Test Methods

  public String printSubset(Subset subset){
    StringBuffer bufferString = new StringBuffer();

    if(subset == null){
      //System.out.println ("null");
      return "";
    }

    BitSet bits =subset.subset;
    double merit =subset.merit;
    List<Integer> indexes =new ArrayList<Integer>();

    for (int i = 0; i<m_numAttribs; i++) {
      if(bits.get (i)){
      	//System.out.print ("1");
        indexes.add (i+1);
      }
      //else System.out.print ("0");
    }
    bufferString.append (Utils.doubleToString (merit,8,5)+"\t "+indexes.toString ()+"\n");
    //System.out.print (" with a merit of: "+merit);

    return bufferString.toString ();
  }

    //........

  protected void resetOptions () {
  	m_popSize = -1;
  	m_initialPopSize = -1;
  	m_calculatedInitialPopSize = -1;
  	m_treshold = -1;
  	m_typeOfCombination = COMBINATION_NOT_REDUCED;
  	m_seed = 1;
  	m_debug = true;
  	m_totalEvals = 0;
  	m_bestMerit = 0;
  	m_processinTime = 0;
  }

    /**
   * converts a BitSet into a list of attribute indexes
   * @param group the BitSet to convert
   * @return an array of attribute indexes
   **/
  public int[] attributeList (BitSet group) {
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


    // Auxiliar Class for handling Chromosomes and its respective merit

  public class Subset implements Serializable {

    double merit;
    BitSet subset;

    public Subset(BitSet subset, double merit){
      this.subset =(BitSet)subset.clone ();
      this.merit =merit;
    }

    public boolean isEqual(Subset othersubset){
      if(subset.equals (othersubset.subset))return true;
      return false;
    }

    public Subset clone(){
      return new Subset((BitSet)subset.clone (), merit);
    }
  }
  //..........

}

