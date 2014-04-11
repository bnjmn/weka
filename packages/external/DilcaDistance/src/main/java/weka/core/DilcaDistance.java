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
 *    DilcaDistance.java
 *    Copyright (C) 1999-2012 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.core;

import java.io.Serializable;
import weka.filters.unsupervised.attribute.ReplaceMissingValues;
import weka.filters.Filter;
import weka.filters.*;
import weka.filters.supervised.attribute.*;
import weka.filters.unsupervised.attribute.*;
import weka.filters.unsupervised.attribute.Remove;
import weka.core.DistanceFunction;
import weka.core.ContingencyTables;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.neighboursearch.PerformanceStats;
import weka.core.TechnicalInformation.Field;
import weka.core.TechnicalInformation.Type;
import weka.core.neighboursearch.PerformanceStats;
import weka.attributeSelection.FCBFSearch;
import weka.attributeSelection.SymmetricalUncertAttributeSetEval;
import java.util.Vector;
import java.util.Collections;
import java.util.Enumeration;
import weka.attributeSelection.AttributeSelection;


/**
 <!-- globalinfo-start -->
 * Implementing the Dilca function for categorical distance computation.<br/>
 * <br/>
 * In particular this is the implememntation of the non parametric version of the Dilca distance function. This approach allows to
learn value-to-value distances between each pair of values for each attribute of the dataset.
The distance between two values is computed indirectly based on the their distribution w.r.t. a set of related attributes (the context) carefully chosen<br/>
 * <br/>
 <!-- globalinfo-end -->
 *
 <!-- technical-bibtex-start -->
 * BibTeX:
 * <pre>
 * &#64;article{DBLP:journals/tkdd/IencoPM12,
	  author    = {Dino Ienco and
	               Ruggero G. Pensa and
	               Rosa Meo},
	  title     = {From Context to Distance: Learning Dissimilarity for Categorical
	               Data Clustering},
	  journal   = {TKDD},
	  volume    = {6},
	  number    = {1},
	  year      = {2012},
	  pages     = {1}
	}
 * </pre>
 * <p/>
 <!-- technical-bibtex-end -->
 *
 <!-- options-start -->
 * Valid options are: <p/>
 *  * 
 * <pre> -R &lt;col1,col2-col4,...&gt;
 *  Specifies list of columns to used in the calculation of the 
 *  distance. 'first' and 'last' are valid indices.
 *  (default: first-last)</pre>
 * 
 * <pre> -V
 *  Invert matching sense of column indices.</pre>
 * 
 <!-- options-end --> 
 *
 * @author Dino Ienco (dino.ienco@teledetection.fr)
 * @author Ruggero Pensa (pensa@di.unito.it)
 * @version $Revision: 8034 $
 */





public class DilcaDistance implements DistanceFunction, Serializable{

	/** The range of attributes to use for calculating the distance. */
	protected Range m_AttributeIndices = new Range("first-last");
	
	/** The Original dataset. */
	protected Instances m_Data;

	/** The value-to-value distance matrices for each Attribute **/
	protected Vector<double[][]> matricesDilca;
	
  	/** The boolean flags, whether an attribute will be used or not. */
  	protected int[] m_ActiveIndices;
	
	/** The method used to discretize instances if they are represented over continuous attribute*/
	protected Filter m_Disc ;
	
	private boolean supDiscr;
	
	protected ReplaceMissingValues m_RepMissValue;
	
	public DilcaDistance(){
		matricesDilca = new Vector<double[][]>();
		m_Disc = new weka.filters.unsupervised.attribute.Discretize();
		m_RepMissValue = new ReplaceMissingValues();
		supDiscr = false;
	}

  	/**
   	 * Returns an enumeration describing the available options.
     *
     * @return 		an enumeration of all the available options.
    */
  	public Enumeration listOptions() {
    	Vector<Option> result = new Vector<Option>();

    	result.addElement(new Option(
			"\tSpecifies list of columns to used in the calculation of the \n"
			+ "\tdistance. 'first' and 'last' are valid indices.\n"
			+ "\t(default: first-last)",
			"R", 1, "-R <col1,col2-col4,...>"));

    	result.addElement(new Option(
			"\tInvert matching sense of column indices.",
			"V", 0, "-V"));
			
		result.addElement(new Option(
			"\tUse Supervised Discretization instead of Unsupervised Discretization if the class value is available",
			"D", 0, "-D"));	

    	return result.elements();
  }

  /**
   * Gets the current settings. Returns empty array.
   *
   * @return 		an array of strings suitable for passing to setOptions()
   */
  	public String[] getOptions() {
    	Vector<String>	result;
    
    	result = new Vector<String>();
    
    	result.add("-R");
    	result.add(getAttributeIndices());
    
    	if (getInvertSelection())
      		result.add("-V");

		if (getSupervisedDiscretization())
      		result.add("-D");

    	return result.toArray(new String[result.size()]);
  	}

  	/**
   	 * Parses a given list of options.
     *
   	 * @param options 	the list of options as an array of strings
     * @throws Exception 	if an option is not supported
    */
  	public void setOptions(String[] options) throws Exception {
    	String	tmpStr;
    
    	tmpStr = Utils.getOption('R', options);
    	if (tmpStr.length() != 0)
      		setAttributeIndices(tmpStr);
    	else
      		setAttributeIndices("first-last");

    	setInvertSelection(Utils.getFlag('V', options));
		setSupervisedDiscretization(Utils.getFlag('D', options));
//		System.out.println("setto impostazioni :"+tmpStr);
  	}

  	/**
   	 * Returns the tip text for this property.
   	 *
   	 * @return 		tip text for this property suitable for
     * 			displaying in the explorer/experimenter gui
     */
  	public String attributeIndicesTipText() {
    	return 
        	"Specify range of attributes to act on. "
      		+ "This is a comma separated list of attribute indices, with "
      		+ "\"first\" and \"last\" valid values. Specify an inclusive "
      		+ "range with \"-\". E.g: \"first-3,5,6-10,last\".";
  	}

	/**
   * Sets the range of attributes to use in the calculation of the distance.
   * The indices start from 1, 'first' and 'last' are valid as well. 
   * E.g.: first-3,5,6-last
   * 
   * @param value	the new attribute index range
   */
  	public void setAttributeIndices(String value) {
    	m_AttributeIndices.setRanges(value);
    	//invalidate();
  	}

	/**
   * Gets the range of attributes used in the calculation of the distance.
   * 
   * @return		the attribute index range
   */
  	public String getAttributeIndices() {
    	return m_AttributeIndices.getRanges();
  	}

  	/**
   	 * Returns the tip text for this property.
     *
     * @return 		tip text for this property suitable for
     * 			displaying in the explorer/experimenter gui
     */
  	public String invertSelectionTipText() {
    	return 
        	"Set attribute selection mode. If false, only selected "
      		+ "attributes in the range will be used in the distance calculation; if "
      		+ "true, only non-selected attributes will be used for the calculation.";
  	}

  	/**
   	 * Returns the tip text for this property.
     *
     * @return 		tip text for this property suitable for
     * 			displaying in the explorer/experimenter gui
     */
  	public String supervisedDiscretizationTipText() {
    	return 
        	"Set the discretization method as supervised. If false, the  "
      		+ "unsupervised discretization is used by default. Take attention set it true for supervised "
      		+ "discretization if the class information is available ";
  	}
	
	/**
   * Sets whether the matching sense of attribute indices is inverted or not.
   * 
   * @param value	if true the matching sense is inverted
   */
  	public void setInvertSelection(boolean value) {
    	m_AttributeIndices.setInvert(value);
    	//invalidate();
  	}

	/**
   * Sets whether the discretization method need to be supervised or not.
   * 
   * @param value	if true the supervised Discretization is used
   */
  	public void setSupervisedDiscretization(boolean value) {
    	if (value) 
			m_Disc = new weka.filters.supervised.attribute.Discretize();
		else
			m_Disc = new weka.filters.unsupervised.attribute.Discretize();
		supDiscr = value;
	}

	
	/**
   * Gets whether the matching sense of attribute indices is inverted or not.
   * 
   * @return		true if the matching sense is inverted
   */
  	public boolean getInvertSelection() {
    	return m_AttributeIndices.getInvert();
  	}

	/**
   * Gets whether the supervised Discretization is used or not
   * 
   * @return		true if the supervised Discretization is used
   */
  	public boolean getSupervisedDiscretization() {
    	return supDiscr;
  	}



  	/**
   	 * initializes the attribute indices.
   	 */
  	protected void initializeAttributeIndices() {
    	m_AttributeIndices.setUpper(m_Data.numAttributes() - 1);
    	m_ActiveIndices = new int[m_Data.numAttributes()];
    	for (Integer i = 0; i < m_ActiveIndices.length; i++)
      		m_ActiveIndices[i] = (m_AttributeIndices.isInRange(i))?1:0;
  	}


  	/**
   	 * Sets the instances.
   	 * 
   	 * @param insts 	the instances to use
   	*/
  	public void setInstances(Instances insts) {
    	m_Data = insts;
		initializeAttributeIndices();
		
		if (!supDiscr && m_Data.classIndex() < 0)
			throw new RuntimeException("Trying to use Supervised Discretization over a dataset without an assigned class attribute");
			
		try{ 
			m_RepMissValue.setInputFormat(m_Data);
			m_Data = Filter.useFilter(m_Data,m_RepMissValue);
			m_Disc.setInputFormat(m_Data);
			m_Data = Filter.useFilter(m_Data,m_Disc);
		}catch(Exception e){
			System.out.println("Problem to clean the data with ReplaceMissingValues and Discretize Filter");
		}
		computeMatrix();
  	}

	public double distance(Instance first, Instance second) {
		return distance( first,  second, Double.POSITIVE_INFINITY);
	}
	
	public double distance(Instance first, Instance second, double cutOffValue) {

		m_RepMissValue.input(first);
		first = m_RepMissValue.output();

		m_RepMissValue.input(second);
		second = m_RepMissValue.output();
		try{
			m_Disc.input(first);
      		first = m_Disc.output();
		
			m_Disc.input(second);
      		second = m_Disc.output();
		}catch (Exception e){System.out.println(e.getMessage()); }
        /*		if (first.hasMissingValue()){
			for (int i=0; i<first.numAttributes(); ++i){
				if (first.isMissing(i) && i != first.classIndex()){				 
					first.setValue(i, (double) (Utils.maxIndex(m_Data.attributeStats(i).nominalCounts)) );
				}
			}
		}
		
		if (second.hasMissingValue()){
			for (int i=0; i<second.numAttributes(); ++i){
				if (second.isMissing(i) && i != second.classIndex()){
					second.setValue(i, (double) (Utils.maxIndex(m_Data.attributeStats(i).nominalCounts)) );
				}
			}
			
                        }*/
	
		double ris = 0.0;
		int indexMatrix=0;
		for (int i=0;i<first.numAttributes();++i){
			if (m_ActiveIndices[i] == 1 && i != first.classIndex()){
				double[][] weightDist = matricesDilca.get(indexMatrix);
				ris += weightDist[(int)first.value(i)][(int)second.value(i)] * weightDist[(int)first.value(i)][(int)second.value(i)] ; 
				indexMatrix++;
			}
		}
		double dist = Math.sqrt(ris);
		return (dist > cutOffValue)?Double.POSITIVE_INFINITY:dist;
	}

	public double distance(Instance first, Instance second, double cutOffValue, PerformanceStats stats) {
		return distance( first,  second,  cutOffValue);
	}
	
	public double distance(Instance first, Instance second, PerformanceStats stats) {
		return distance( first,  second);
	}

  	/**
   	 * returns the instances currently set.
     * 
     * @return 		the current instances
     */
  	public Instances getInstances() {
    	return m_Data;
  	}
	
	public void postProcessDistances(double[] distances) {  }
  
	/**
     * Update the distance function (if necessary) for the newly added instance.
     * 
     * @param ins		the instance to add
     */
  	public void update(Instance ins) {
		//System.out.println("====>UPDATE");
    	//validate();
    	//m_Ranges = updateRanges(ins, m_Ranges);
  		//m_Data.add(ins);
/*
		try{ 
			Filter filter = new ReplaceMissingValues();
			filter.setInputFormat(m_Data);
			m_Data = Filter.useFilter(m_Data,filter);
			Filter filter2 = new Discretize();
			filter2.setInputFormat(m_Data);
			m_Data = Filter.useFilter(m_Data,filter2);
		}catch(Exception e){
			System.out.println("Problem to clean the data with ReplaceMissingValues and Discretize Filter");
		}

		computeMatrix();
*/
	}

	/**
	 * Compute the distance matrices for each attribute. 
	 * Each distance matrix contains distances between each pair of values of the same attribute
	 * This is the main step of the DILCA procedure
	 */

	protected void computeMatrix(){
		matricesDilca = new Vector<double[][]>(); 
		Instances reducedData = null;
		Remove remove = new Remove();
		try{
			int toDel = 0;
			for (int i=0; i<m_ActiveIndices.length;++i){	
				if (m_ActiveIndices[i] == 0) toDel++;
			}
			int[] attributeToBeDeleted = new int[toDel];
			int j=0;			
			for (int i=0; i<m_ActiveIndices.length;++i){	
				if (m_ActiveIndices[i] == 0){
					attributeToBeDeleted[j] = i;
					j++;
				} 
			}
//			System.out.println("attributi da cancellare: ");
//			for (int i=0; i< attributeToBeDeleted.length;++i){
//				System.out.println(attributeToBeDeleted[i]+" ");
//			}
			
			remove.setAttributeIndicesArray(attributeToBeDeleted);
			remove.setInputFormat(m_Data);
			reducedData = Filter.useFilter(new Instances(m_Data),remove);
//			System.out.println(reducedData);
		}catch(Exception e){
			System.out.println("Problem to reduce the data using the active indices choosen by the user: "+e.getMessage());
		}
		Vector<Vector<Integer>> nearestV = contextVector(reducedData);	
		for (int i=0;i<nearestV.size();i++){
//			System.out.println("\tfeature "+reducedData.attribute(i)+" con context lungo: "+nearestV.get(i).size());
//			for (Integer o: nearestV.get(i)){
//				System.out.println("\t\t "+ reducedData.attribute(o.intValue()));
//			}
			double [][]matrix = calculateFeatureDistance(reducedData, i,nearestV.get(i));
			
//			for (int k=0; k< matrix.length; ++k){
//				for (int j=0; j< matrix[k].length; ++j) System.out.print(matrix[k][j]+" ");
//				System.out.println();
//			}
			matricesDilca.add(matrix);
		}	
//		System.out.println();
//		System.out.println("====================");
//		System.out.println();			
		
	}



	/**
	 * Compute the distance matrices for a specified attribute. 
	 * @param reducedData	reduced data erasing attribute not specified in the selection
	 * @param indexF	index of the attribute from which the valut-to-value distance matrix is extracted
	 * @param nearest	the context (attribute set) of the associated attribute indexed by indexF 
	 * This is the main step of the DILCA procedure
	 */

	private double [][] calculateFeatureDistance(Instances data, int indexF,Vector<Integer> nearest){
		//vector that contains in each position the sum of the number of the previous attribute values
		int[] featValNum = new int[nearest.size()];
		int totNumFeature = 0;
		//to manage the case of unary attribute
		if (nearest.size() > 0){
			totNumFeature = data.attribute(nearest.get(0)).numValues();
			featValNum[0]=0;
		}
		
		for (Integer i=0;i<nearest.size()-1;i++){
			featValNum[i+1] = featValNum[i] + data.attribute(nearest.get(i)).numValues(); 
			totNumFeature+=data.attribute(nearest.get(i+1)).numValues();;
		}
		int numValuesF = data.attribute(indexF).numValues();
		double[][] matrixComputation = new double[numValuesF][totNumFeature];
		double [] normalization = new double[totNumFeature];
		
		for (int i=0;i<data.numInstances();i++){
				int firstValue= (int) data.instance(i).value(indexF);
			for (int j=0;j<nearest.size();j++){
				int secondValue = (int) data.instance(i).value(nearest.get(j));
				matrixComputation[firstValue][secondValue+featValNum[j]]++;
				normalization[secondValue+featValNum[j]]++;
			}
		}
		for (int i=0;i<matrixComputation.length;i++){
			for (Integer j=0;j<matrixComputation[i].length;j++){
				matrixComputation[i][j]=(normalization[j]==0)?0:matrixComputation[i][j]/normalization[j];
			}
		}
		
		double[][] result = new double[numValuesF][numValuesF];
		if (numValuesF == 1) result[0][0] = 0;
		
		for (int i=0;i<numValuesF;i++){
			for (int j=i+1;j<numValuesF;j++){
				result[i][j]=result[j][i]=eucl(matrixComputation[i],matrixComputation[j]);
			}
		}
		return result;
	}

	/**
	 * Compute the distance matrices for a specified attribute. 
	 * @param data		data to be analyzed
	 * @param indexF	index of the attribute from which the valut-to-value distance matrix is extracted
	 * @param nearest	the context (attribute set) of the associated attribute indexed by indexF 
	 * This is the main step of the DILCA procedure
	 */
	private Vector<Vector<Integer>> contextVector(Instances data) {
		int n_attributes = data.numAttributes();
		Vector<Vector<Integer>> nearIndex = new Vector<Vector<Integer>>(n_attributes);
		int originalClass = data.classIndex(); 
		AttributeSelection attSel = new AttributeSelection();  // package weka.filters.supervised.attribute!
		
		for (int i=0; i< n_attributes; i++){
			try{
				
				Vector<Integer> actual = new Vector<Integer>();
				if (data.attribute(i).numValues() > 1){
					data.setClassIndex(i);
					SymmetricalUncertAttributeSetEval eval = new SymmetricalUncertAttributeSetEval();
					FCBFSearch search = new FCBFSearch();
					attSel.setEvaluator(eval);
					attSel.setSearch(search);
					attSel.SelectAttributes(data);
					int[] indices = attSel.selectedAttributes();
					
					for (int j=0;j<indices.length;++j) {
						if ( indices[j] != i ) actual.add(indices[j]);
					}
				}	
					nearIndex.add(actual);	
			}catch(Exception e){
				System.out.println("problem to select attribute for the context: "+e.getMessage()+" a suggestion: try to replaceMissingValues in the original dataset");
			}
		}
		data.setClassIndex(originalClass);
		return nearIndex;
	}


  /**
   * Returns a string describing this object.
   * 
   * @return 		a description of the evaluator suitable for
   * 			displaying in the explorer/experimenter gui
   */	
	public String globalInfo() {
    return 
        "This is the implememntation of the non parametric version of the Dilca distance function.\n\n" 
	  + "This approach allows to learn value-to-value distances between each pair of values for each attribute of the dataset."
	  +	"The distance between two values is computed indirectly based on the their distribution w.r.t. a set of related attributes (the context) carefully choosen.\n\n"
      + "For more information, see:\n\n"
      + getTechnicalInformation().toString();
  }
  
	/**
   * Returns an instance of a TechnicalInformation object, containing 
   * detailed information about the technical background of this class,
   * e.g., paper reference or book this class is based on.
   * 
   * @return 		the technical information about this class
   */
  public TechnicalInformation getTechnicalInformation() {
    TechnicalInformation 	result;
    result = new TechnicalInformation(Type.ARTICLE);
    result.setValue(Field.AUTHOR, "Dino Ienco, Ruggero G. Pensa and Rosa Meo");
    result.setValue(Field.TITLE, "From Context to Distance: Learning Dissimilarity for Categorical Data Clustering");
    result.setValue(Field.JOURNAL, "TKDD");
	result.setValue(Field.VOLUME, "6");
	result.setValue(Field.NUMBER, "1");
	result.setValue(Field.YEAR, "2012");
	result.setValue(Field.PAGES, "1");
    return result;
  }

  public String toString(){
	String res = "";
	for (double[][] p : matricesDilca){
		for (int i=0; i< p.length;++i){
			for (int j=0; j< p[i].length;++j){
				res += p[i][j]+" ";
			}
			res += "\n";
		}
		res += "\n\n\n";
	}
	return res;
  }

	private double eucl(double[] a, double[]b){
		double ris=0;
		for (Integer i=0;i<a.length;i++){
			ris += (a[i]-b[i]) * (a[i]-b[i]);
		}
		return Math.sqrt(ris/a.length);
	}

  public void clean() {
  }

}