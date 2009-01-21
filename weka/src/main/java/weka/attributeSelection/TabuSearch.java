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
 *    TabuSearch.java
 *    Copyright (C) 2009 Adrian Pino
 *    Copyright (C) 2009 University of Waikato, Hamilton, NZ
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
import weka.core.TechnicalInformation;
import weka.core.TechnicalInformationHandler;
import weka.core.Utils;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;


/**
 * Class for performing the TabuSearch method. <p>
 * 
 <!-- globalinfo-start -->
 * Tabu Search :<br/>
 * <br/>
 * Performs a search  through the space of attribute subsets. Evading local maximums by accepting bad and diverse solutions and make further search in the best soluions. Stops when there's not more improvement in n iterations<br/>
 * ;For more information see:<br/>
 * <br/>
 * Abdel-Rahman Hedar, Jue Wangy,, Masao Fukushima (2006). Tabu Search for Attribute Reduction in Rough Set Theory. .
 * <p/>
 <!-- globalinfo-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 * 
 * <pre> -Z &lt;numInitialSolution&gt;
 *  Specify the number of attributes 
 *  in the initial Solution..</pre>
 * 
 * <pre> -P &lt;diversificationProb&gt;
 *  Specify the diversification probabilities,
 *  if this value is near to 0 then the best attributes
 *   will have more probabilities of keeping in the new diverse solution</pre>
 * 
 * <pre> -S &lt;seed&gt;
 *  Set the random number seed.
 *  (default = 1)</pre>
 * 
 * <pre> -N &lt;number of neighbors&gt;
 *  Set the number of neighbors to generate.</pre>
 * 
 <!-- options-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;book{Abdel-RahmanHedar2006,
 *    author = {Abdel-Rahman Hedar, Jue Wangy, and Masao Fukushima},
 *    month = {July},
 *    title = {Tabu Search for Attribute Reduction in Rough Set Theory},
 *    year = {2006},
 *    language = {English}
 * }
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 * from the Book: Tabu Search for Attribute Reduction in Rough Set Theory, Abdel-Rahman Hedar, Jue Wangy, and Masao Fukushima.
 * 
 * @author Adrian Pino (apinoa@facinf.uho.edu.cu)
 * @version $Revision$
 *
 */

public class TabuSearch extends ASSearch
  implements OptionHandler, TechnicalInformationHandler{

  /** for serialization */
  static final long serialVersionUID = -8812132617585120414L;

  /** number of attributes in the data */
  private int m_numAttribs;

  /** holds the class index */
  private int m_classIndex;

  /** random number generation */
  private Random m_random;

  /** seed for random number generation */
  private int m_seed;

  /** probabilities of diversification */
  private double m_diversificationProb;

  /** number of iterations for getting the best subset*/
  private int m_numIterations;

  /** total number of subsets evaluated during a search */
  private int m_totalEvals;

  /** holds the best Subset found */
  protected Subset m_Sbest;

  /** holds the number of neighborhood to generate from a Subset*/
  private int m_numNeighborhood;

  /** time for procesing the search method */
  private long m_processinTime;

  /** holds the solution size*/
  private int m_initialSize;

  /**Subset holding the initial solution*/
  private Subset m_initialSolution;

  /**attribute ranking*/
  private List<Subset> m_rankedAttribs;

  /**tabu List*/
  private List<BitSet> m_vectorTabu;

  /**Evaluator used to know the significance of a subset (for guiding the search)*/
  private SubsetEvaluator ASEvaluator =null;


  /**
   * Searches the attribute subset space using Tabu Search.
   *
   * @param ASEvaluator the attribute evaluator to guide the search
   * @param data the training instances.
   * @return an array of selected attribute indexes
   * @exception Exception if the search can't be completed
   */
  public int[] search(ASEvaluation ASEval, Instances data)
  throws Exception{
    m_totalEvals = 0;
    m_processinTime =System.currentTimeMillis ();

    m_numAttribs =data.numAttributes ();
    m_classIndex =data.classIndex ();

    ASEvaluator =(SubsetEvaluator)ASEval;

    m_random = new Random(m_seed);

    int numN = m_numNeighborhood;
    numN = (m_numNeighborhood <= 0) ? 3*m_numAttribs/4 : m_numNeighborhood;


    if(m_numAttribs <= 14) m_numIterations = m_numAttribs;
    else if(m_numAttribs > 14) m_numIterations = m_numAttribs/3;

    m_rankedAttribs = RankEachAttribute ();

    /** generating an initial Solution based in SFS*/
    if(m_initialSize < 0)m_initialSize = m_numAttribs;
    m_initialSolution = GenerateInitialSolution(new Subset(new BitSet(m_numAttribs), 0), m_initialSize);


    /**Initializing Vector Tabu*/
    m_vectorTabu =new ArrayList<BitSet>();
    BitSet tabu1 = new BitSet(m_numAttribs);
    BitSet tabu2 = new BitSet(m_numAttribs);

    tabu2.set (0,m_numAttribs-1);
    if (m_classIndex >= 0) tabu2.set (m_classIndex, false);

    m_vectorTabu.add (tabu1);
    m_vectorTabu.add (tabu2);


    /**For Controlling the Search*/
    int iterationCounter = 0;
    int numGenerationNeighborForDiv = (m_numAttribs/m_numIterations >= 2) ? m_numAttribs/m_numIterations : 3;
    int numTotalWImp = 0;
    int numTotalWImpForFinishing = m_numIterations/2;

    BitSet S = m_initialSolution.subset;
    m_Sbest = m_initialSolution;

    List<Subset> RedSet = new ArrayList<Subset>();
    RedSet.add (m_Sbest.clone ());

    while(iterationCounter < m_numIterations && numTotalWImp < numTotalWImpForFinishing){
      iterationCounter ++;

      List<Subset> neighborhood = null;
      int counterGenerationNeighborWImp = 0;
      while (counterGenerationNeighborWImp < numGenerationNeighborForDiv) {

        neighborhood = generateNeighborhood(S, numN);
        if(neighborhood != null){
          S =((Subset)neighborhood.get (0)).subset;
          double Smerit = ASEvaluator.evaluateSubset (S);
          m_totalEvals ++;

          RedSet.add (new Subset((BitSet)S.clone (), Smerit));

          m_vectorTabu.add ((BitSet)S.clone ());


          if(Smerit > m_Sbest.merit){
            m_Sbest = new Subset((BitSet)S.clone (), Smerit);
            Subset aux = shake ();
            if(aux != null)
              m_Sbest = aux.clone ();
          }
          else{
            counterGenerationNeighborWImp ++;
            numTotalWImp ++;
          }
        }
        else break;
      }
      S = diversify (neighborhood);
    }

    Subset elite = eliteReducts(RedSet, RedSet.size ());

    if(elite.merit >= m_Sbest.merit)
      return attributeList (elite.subset);

    m_processinTime = System.currentTimeMillis () - m_processinTime;

    return attributeList (m_Sbest.subset);
  }

  /**
   * Generates a list of neighborhood given a Solution, flipping randomly the state of many attributes.
   *
   * @param S the Solution from which we are going to find the neighborhood
   * @param numNeighborhood number of attributes for flipping
   * @return a Subset list containing the neighborhood
   * @exception Exception if the evaluation can not be complete
   */
  private List<Subset> generateNeighborhood(BitSet S, int numNeighborhood)
  throws Exception{
    int counter = 0;
    List<Subset> neighborhood = new ArrayList<Subset>();

    int numAttribs = (m_classIndex == -1) ? m_numAttribs  : m_numAttribs - 1;

    if(numNeighborhood >= numAttribs){
      for (int i = 0; i < m_numAttribs; i++) {
        if(i == m_classIndex)continue;

        BitSet aux = (BitSet)S.clone ();
        aux.flip (i);

        if(!m_vectorTabu.contains (aux)){
          neighborhood.add (new Subset((BitSet)aux.clone (), ASEvaluator.evaluateSubset (aux)));
          m_totalEvals ++;
        }
      }
    }
    else{
      while (counter < numNeighborhood) {
        BitSet aux = (BitSet)S.clone ();

        int randomNumber = m_random.nextInt (m_numAttribs);
        if(randomNumber == m_classIndex)
          continue;

        aux.flip (randomNumber);
        if(!m_vectorTabu.contains (aux)){
          neighborhood.add (new Subset((BitSet)aux.clone (), ASEvaluator.evaluateSubset (aux)));
          m_totalEvals ++;
          counter ++;
        }
      }
    }

    if(neighborhood.isEmpty ())
      return null;

    return bubbleSubsetSort (neighborhood);
  }

  /**
   * Generate a diverse Bitset given a List of Subset
   *
   * @param neighborhood the list from which are going to be generate the diverse solution
   * @return a diverse Bitset
   */
  public BitSet diversify(List<Subset> neighborhood){

    if(neighborhood == null)
      return m_Sbest.subset;

    BitSet result = new BitSet(m_numAttribs);

    double [] counts = new double[m_numAttribs];
    int numNeighborhood = neighborhood.size ();



    for (int i = 0; i < m_numAttribs; i++) {
      if(i == m_classIndex)
        continue;

      int counter = 0;

      for (int j = 0; j < numNeighborhood; j++) {
        if(((Subset)neighborhood.get (j)).subset.get (i))
          counter ++;
      }
      counts [i] = counter/(double)numNeighborhood;
    }

    for (int i = 0; i < m_numAttribs; i++) {
      double randomNumber = m_random.nextDouble ();
      double ocurrenceAndRank = counts [i] * (m_diversificationProb) + doubleRank (i) * (1 - m_diversificationProb);

      if(randomNumber > ocurrenceAndRank)
        result.set (i);
    }

    return result;
  }


  /**
   * Try to improve the best Solution obtained till now in the way of Sequential Backward Selection(SBS)
   *
   * @param ASEvaluator the attribute evaluator to guide the search
   * @return null if there is not improvement otherwise a Subset better than the obtained till now.
   */
  private Subset shake()
  throws Exception{
    Subset bestCopy = m_Sbest.clone ();
    boolean anyImprovement = false;

    for (int i = m_rankedAttribs.size ()-1; i >= 0; i --) {
      Subset ranking = m_rankedAttribs.get (i);
      int attributeIndex = ranking.subset.nextSetBit (0);

      BitSet aux = (BitSet)bestCopy.subset.clone ();
      if(aux.get (attributeIndex)){
        aux.set (attributeIndex, false);

        if(m_vectorTabu.contains (aux))
          continue;

        double tempMerit = ASEvaluator.evaluateSubset (aux);
        m_totalEvals ++;

        if(tempMerit >= bestCopy.merit){
          bestCopy = new Subset((BitSet)aux.clone (), tempMerit);
          anyImprovement = true;
        }
      }
    }
    if(anyImprovement)
      return bestCopy;
    return null;
  }

  /**
   * Find a better solution by intersecting and SFS of the best elite Reducts found till that moment
   *
   * @param RedSet the Subset List of the Elite Reducted found till that moment
   * @param numSubset number of elites to intersect
   * @return a the resulting Subset
   * @exception Exception if any evaluation can't be completed
   */
  public Subset eliteReducts(List<Subset>RedSet, int numSubset)
  throws Exception{

    if(numSubset <= 0)
      return null;

    List<Subset>orderedRedSet = bubbleSubsetSort (RedSet);
    int numAttribsOfBest = m_Sbest.cardinality ();

    BitSet result =new BitSet(m_numAttribs);
    result.set (0,m_numAttribs-1,true);

    BitSet aux;

    for (int i = 0; i < numSubset; i++) {
      aux = ((Subset)orderedRedSet.get (i)).subset;
      result.and (aux);
    }

    int diff = numAttribsOfBest - result.cardinality ();

    Subset resultSet = GenerateInitialSolution (new Subset(result,ASEvaluator.evaluateSubset (result)),result.cardinality () + diff-1);
    m_totalEvals ++;

    if(resultSet == null)
      return null;

    return resultSet;
  }


  /**
   * Generate an initial solution based in a Sequential Forward Selection (SFS)
   *
   * @param size number of posible attributes in the initial Solution
   * @return an initial Subset containing the initial Solution
   * @exception Exception if the evaluation can not be completed
   */
  public Subset GenerateInitialSolution(Subset initial, int size)
  throws Exception{

    List<Subset> rankedAttribsCopy = new ArrayList<Subset>();

    for (int i = 0; i<m_rankedAttribs.size (); i++)
      rankedAttribsCopy.add (m_rankedAttribs.get (i));

    Subset solution = initial.clone ();

    while(solution.cardinality () < size) {

      Subset tempSubset =new Subset();
      double bestMerit =solution.merit;
      int bestIndex =-1;

      for (int i = 0; i<rankedAttribsCopy.size (); i++) {
        Subset candidate = ((Subset)rankedAttribsCopy.get (i)).clone ();
        if(solution.subset.get (candidate.subset.nextSetBit (0)))
          continue;

        tempSubset =joinSubsets(solution, rankedAttribsCopy.get (i));

        if(tempSubset.merit > bestMerit){
          bestMerit = tempSubset.merit;
          bestIndex = i;
        }
      }

      if(bestIndex == -1) break;

      solution =joinSubsets (solution, rankedAttribsCopy.get (bestIndex));
      rankedAttribsCopy.remove (bestIndex);
    }

    return solution;
  }



  //--------Auxiliar methods****

  /**
   * get the importance of an individual attribute according to his position in m_rankedAttribs List.
   *
   * @param idAttrib the index attribute of the attribute that we want to get the doubleRank
   * @return a double giving the result. If is a good attribute the result will be a number near of 0(inclusive),
   *                                     If is a bad one the result will be near of 1(exclusive).
   */
  private double doubleRank(int idAttrib){
    int rankSize = m_rankedAttribs.size ();
    int rankAttribute = -1;

    if(idAttrib == m_classIndex) return -1;

    for (int i = 0; i < rankSize; i++) {
      if(((Subset)m_rankedAttribs.get (i)).subset.nextSetBit (0) == idAttrib){
        rankAttribute = i;
        break;
      }
    }

    return rankAttribute/(double)rankSize;
  }

  /**
   * Rank all the attributes individually acording to their merits
   *
   * @return an ordered List  of Subsets with just one attribute
   * @exception Exception if the evaluation can not be completed
   */
  private List<Subset> RankEachAttribute()
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

    return new Subset((BitSet)b1, newMerit);
  }

  /**
   * converts a BitSet into a list of attribute indexes
   *
   * @param group the BitSet to convert
   * @return an array of attribute indexes
   */
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


  // Auxiliar Class for handling solutions and its respective merit
  public class Subset implements Serializable{

    double merit;
    BitSet subset;

    public Subset(BitSet subset, double merit){
      this.subset = (BitSet)subset.clone ();
      this.merit = merit;
    }

    public Subset(){
      this.subset =null;
      this.merit =-1.0;
    }

    public boolean isEqual(Subset otherSubset){
      if(subset.equals (otherSubset.subset))return true;
      return false;
    }

    public Subset clone(){
      return new Subset((BitSet)subset.clone (), this.merit);
    }

    public int cardinality(){
      if(subset == null) return 0;
      return subset.cardinality ();
    }

    public void flip(int index)
    throws Exception{

      subset.flip (index);
      merit = ASEvaluator.evaluateSubset (subset);
      m_totalEvals ++;
    }

    public boolean contains(int indexAttribute){
      return subset.get (indexAttribute);
    }
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



  /**
   * Returns a string describing this search method
   * @return a description of the search suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Tabu Search :\n\nPerforms a search  "
    +"through "
    +"the space of attribute subsets. Evading local maximums by accepting bad and diverse solutions "
    +"and make further search in the best soluions."
    +" Stops when there's not more improvement in n iterations\n;"
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
    result.setValue(Field.AUTHOR, "Abdel-Rahman Hedar, Jue Wangy, and Masao Fukushima");
    result.setValue(Field.MONTH, "July");
    result.setValue(Field.YEAR, "2006");
    result.setValue(Field.TITLE, "Tabu Search for Attribute Reduction in Rough Set Theory");
    result.setValue(Field.LANGUAGE, "English");

    return result;
  }

  /**
   * Returns the revision string.
   *
   * @return the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }

  public TabuSearch() {
    resetOptions();
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
  public String diversificationProbTipText() {
    return "Set the probability of diversification. This is the probability of "
    +"change of search subspace in an abrupt way";
  }

  /**
   * set the probability of diverification
   * @param p the probability of change of search subspace in an abrupt way
   */
  public void setDiversificationProb(double p) {
    m_diversificationProb = p;
  }

  /**
   * get the probability of diversification
   * @return the probability of diversification
   */
  public double getDiversificationProb() {
    return m_diversificationProb;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numNeighborhoodTipText() {
    return "Set the number of current solution's neighborhood"+
    " to generate for looking for a better solution";
  }

  /**
   * set the number of neighborhood
   * @param n the number of neighborhood
   */
  public void setNumNeighborhood(int n) {
    m_numNeighborhood = n;
  }

  /**
   * get the number of neighborhood
   * @return the number of neighborhood
   */
  public int getNumNeighborhood() {
    return m_numNeighborhood;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String initialSizeTipText() {
    return "Set the number of attributes that are going to be in the initial Solution";
  }

  /**
   * set the number of attributes that are going to be in the initial Solution
   * @param n the number of attributes
   */
  public void setInitialSize(int n) {
    m_initialSize = n;
  }

  /**
   * get the number of of attributes that are going to be in the initial Solution
   * @return the number of attributes
   */
  public int getInitialSize() {
    return m_initialSize;
  }


  public Enumeration listOptions () {
    Vector newVector = new Vector(4);

    newVector.addElement(new Option("\tSpecify the number of attributes "
        + "\n\tin the initial Solution.."
        ,"Z",1
        , "-Z <numInitialSolution>"));
    newVector.addElement(new Option("\tSpecify the diversification probabilities,"
        + "\n\tif this value is near to 0 then the best attributes"
        + "\n\t will have more probabilities of keeping in the new diverse solution"
        , "P", 1
        , "-P <diversificationProb>"));
    newVector.addElement(new Option("\tSet the random number seed."
        +"\n\t(default = 1)"
        , "S", 1, "-S <seed>"));
    newVector.addElement(new Option("\tSet the number of neighbors to generate."
        , "N", 1, "-N <number of neighbors>"));

    return  newVector.elements();
  }

  /**
   * 
   <!-- options-start -->
   * Valid options are: <p/>
   * 
   * <pre> -Z &lt;numInitialSolution&gt;
   *  Specify the number of attributes 
   *  in the initial Solution..</pre>
   * 
   * <pre> -P &lt;diversificationProb&gt;
   *  Specify the diversification probabilities,
   *  if this value is near to 0 then the best attributes
   *   will have more probabilities of keeping in the new diverse solution</pre>
   * 
   * <pre> -S &lt;seed&gt;
   *  Set the random number seed.
   *  (default = 1)</pre>
   * 
   * <pre> -N &lt;number of neighbors&gt;
   *  Set the number of neighbors to generate.</pre>
   * 
   <!-- options-end -->
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
      setInitialSize (Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('P', options);
    if (optionString.length() != 0) {
      setDiversificationProb (Double.parseDouble(optionString));
    }

    optionString = Utils.getOption('S', options);
    if (optionString.length() != 0) {
      setSeed(Integer.parseInt(optionString));
    }

    optionString = Utils.getOption('N', options);
    if (optionString.length() != 0) {
      setNumNeighborhood (Integer.parseInt(optionString));
    }

  }

  /**
   * Gets the current settings of TabuSearch.
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {
    String[] options = new String[8];
    int current = 0;

    options[current++] = "-Z";
    options[current++] = "" + getInitialSize ();

    options[current++] = "-P";
    options[current++] = ""+getDiversificationProb ();

    options[current++] = "-S";
    options[current++] = "" + getSeed();

    options[current++] = "-N";
    options[current++] = "" + getNumNeighborhood ();


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

    FString.append("\nTabu Search "
        + "\n\tInitial Size: "+getInitialSize ());


    if(getInitialSize () > 0){
      FString.append("\n\tInitial Solution (Generated by SFS):");
      FString.append("\n\tmerit:"+"\t\t"+" subset");
      FString.append("\n\t"+ printSubset (m_initialSolution));

    }
    FString.append("\tdiversificationProb: "
        +Utils.doubleToString(Math.abs(getDiversificationProb ()),8,3)+"\n");

    FString.append("\tTotal number of subsets evaluated: "
        + m_totalEvals + "\n");

    FString.append("\tMerit of best subset found: "
        +Utils.doubleToString(Math.abs(m_Sbest.merit),8,3)+"\n");

    /*FString.append("\tTime procesing the search space: "
        +(double)m_processinTime/1000+" seconds\n"); */

    return FString.toString();
  }

  protected void resetOptions () {
    m_initialSize = -1;
    m_numNeighborhood = -1;
    m_seed = 1;
    m_diversificationProb = 1.0;
    m_totalEvals = 0;
    m_processinTime = 0;
  }

}

