/*
 *    principalComponents.java
 *    Copyright (C) 2000 Mark Hall
 *
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
package weka.attributeSelection;

import  java.io.*;
import  java.util.*;
import  weka.core.*;
import  weka.filters.*;

/**
 * Class for performing principal components analysis/transformation.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.6 $
 */
public class PrincipalComponents extends AttributeTransformer 
  implements OptionHandler {
  
  /** The data to transform analyse/transform */
  private Instances m_trainInstances;

  /** Keep a copy for the class attribute (if set) */
  private Instances m_trainCopy;

  /** Data has a class set */
  private boolean m_hasClass;

  /** Class index */
  private int m_classIndex;

  /** Number of attributes */
  private int m_numAttribs;

  /** Number of instances */
  private int m_numInstances;

  /** Correlation matrix for the original data */
  private double [][] m_correlation;

  /** Will hold the unordered linear transformations of the (normalized)
      original data */
  private double [][] m_eigenvectors;
  
  /** Eigenvalues for the corresponding eigenvectors */
  private double [] m_eigenvalues = null;

  /** Sorted eigenvalues */
  private int [] m_sortedEigens;
  
  /** Filters for original data */
  private ReplaceMissingValuesFilter m_replaceMissingFilter;
  private NormalizationFilter m_normalizeFilter;
  private NominalToBinaryFilter m_nominalToBinFilter;
  private AttributeFilter m_attributeFilter;
  
  /** used to remove the class column if a class column is set */
  private AttributeFilter m_attribFilter;

  /** The number of attributes in the transformed data */
  private int m_outputNumAtts = -1;

  /** normalize the input data? */
  private boolean m_normalize = true;

  /**
   * Returns a string describing this attribute transformer
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Performs a principal components analysis and transformation of "
      +"the data. Use in conjunction with a Ranker search. Dimensionality "
      +"reduction can be accomplished by setting an appropriate threshold "
      +"in Ranker. One heuristic is to choose enough eigenvectors ("
      +"transformed attributes) to account for around 95% of the variance "
      +"in the original data. This can be done by setting a threshold of "
      +"0.04 for the Ranker search.";
  }

  /**
   * Returns an enumeration describing the available options <p>
   *
   * -N <classifier>
   * Don't normalize the input data. <p>
   *
   * @return an enumeration of all the available options
   **/
  public Enumeration listOptions () {
    Vector newVector = new Vector(1);
    newVector.addElement(new Option("\tDon't normalize input data." 
				    , "N", 0, "-N <classifier>"));
    
    return  newVector.elements();
  }

  /**
   * Parses a given list of options.
   *
   * Valid options are:<p>
   * -N <classifier>
   * Don't normalize the input data. <p>
   *
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions (String[] options)
    throws Exception
  {
    setNormalize(!Utils.getFlag('N', options));

  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String normalizeTipText() {
    return "Normalize input data.";
  }

  /**
   * Set whether input data will be normalized.
   * @param n true if input data is to be normalized
   */
  public void setNormalize(boolean n) {
    m_normalize = n;
  }

  /**
   * Gets whether or not input data is to be normalized
   * @return true if input data is to be normalized
   */
  public boolean getNormalize() {
    return m_normalize;
  }

    /**
   * Gets the current settings of ClassifierSubsetEval
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {

    String[] options = new String[1];
    int current = 0;

    if (!getNormalize()) {
      options[current++] = "-N";
    }

    while (current < options.length) {
      options[current++] = "";
    }
    
    return  options;
  }

  /**
   * Initializes principal components and performs the analysis
   * @param data the instances to analyse/transform
   * @exception Exception if analysis fails
   */
  public void buildEvaluator(Instances data) throws Exception {
    buildAttributeConstructor(data);
  }

  private void buildAttributeConstructor (Instances data) throws Exception {
    m_eigenvalues = null;
    m_outputNumAtts = -1;
    m_attributeFilter = null;
    m_nominalToBinFilter = null;

    if (data.checkForStringAttributes()) {
      throw  new Exception("Can't handle string attributes!");
    }
    m_trainInstances = data;

    // make a copy of the training data so that we can get the class
    // column to append to the transformed data (if necessary)
    m_trainCopy = new Instances(m_trainInstances);
    
    m_replaceMissingFilter = new ReplaceMissingValuesFilter();
    m_replaceMissingFilter.inputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, m_replaceMissingFilter);

    if (m_normalize) {
      m_normalizeFilter = new NormalizationFilter();
      m_normalizeFilter.inputFormat(m_trainInstances);
      m_trainInstances = Filter.useFilter(m_trainInstances, m_normalizeFilter);
    }

    m_nominalToBinFilter = new NominalToBinaryFilter();
    m_nominalToBinFilter.inputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, 
					m_nominalToBinFilter);
    
    // delete any attributes with only one distinct value or are all missing
    Vector deleteCols = new Vector();
    for (int i=0;i<m_trainInstances.numAttributes();i++) {
      if (m_trainInstances.numDistinctValues(i) <=1) {
	deleteCols.addElement(new Integer(i));
	//      System.err.println("att : "+(i+1)+ " num vals : "+m_trainInstances.numDistinctValues(i));
      }
    }

    if (m_trainInstances.classIndex() >=0) {
      // get rid of the class column
      m_hasClass = true;
      m_classIndex = m_trainInstances.classIndex();
      deleteCols.addElement(new Integer(m_classIndex));
    }

    // remove columns from the data if necessary
    if (deleteCols.size() > 0) {
      m_attributeFilter = new AttributeFilter();
      int [] todelete = new int [deleteCols.size()];
      for (int i=0;i<deleteCols.size();i++) {
	todelete[i] = ((Integer)(deleteCols.elementAt(i))).intValue();
      }
      m_attributeFilter.setAttributeIndicesArray(todelete);
      m_attributeFilter.setInvertSelection(false);
      m_attributeFilter.inputFormat(m_trainInstances);
      m_trainInstances = Filter.useFilter(m_trainInstances, m_attributeFilter);
    }

    m_numInstances = m_trainInstances.numInstances();
    m_numAttribs = m_trainInstances.numAttributes();

    fillCorrelation();

    double [] d = new double[m_numAttribs+1]; 
    double [][] v = new double[m_numAttribs+1][m_numAttribs+1];

    jacobi(m_correlation, m_numAttribs, d, v);
    m_eigenvectors = (double [][])v.clone();
    
    m_eigenvalues = (double [])d.clone();
    /*    for (int i=0;i<m_eigenvalues.length;i++) {
      System.err.println("eig val : "+i+" "+m_eigenvalues[i]);
      } */

    // any eigenvalues less than 0 are not worth anything --- change to 0
    for (int i=0;i<m_eigenvalues.length;i++) {
      if (m_eigenvalues[i] < 0) {
	m_eigenvalues[i] = 0.0;
      }
    }
    m_sortedEigens = Utils.sort(m_eigenvalues);
  }

  /**
   * Gets the transformed training data.
   * @return the transformed training data
   * @exception Exception if transformed data can't be returned
   */
  public Instances getTransformedData() throws Exception {
    if (m_eigenvalues == null) {
      throw new Exception("Principal components hasn't been built yet");
    }

    Instances output = setOutputFormat();
    for (int i=0;i<m_trainCopy.numInstances();i++) {
      Instance converted = convertInstance(m_trainCopy.instance(i));
      output.add(converted);
    }
    
    return output;
  }

  /**
   * Evaluates the merit of a transformed attribute. This is defined
   * to be 1 minus the cumulative variance explained.
   * @param att the attribute to be evaluated
   * @return the merit of a transformed attribute
   * @exception Exception if attribute can't be evaluated
   */
  public double evaluateAttribute(int att) throws Exception {
    if (m_eigenvalues == null) {
      throw new Exception("Principal components hasn't been built yet!");
    }

    // return 1-cumulative variance explained for this transformed att
    double sum = Utils.sum(m_eigenvalues);
    double cumulative = 0.0;
    for (int i=m_numAttribs;i>=m_numAttribs-att;i--) {
      cumulative += m_eigenvalues[m_sortedEigens[i]];
    }

    return 1.0-cumulative/sum;
  }

  /**
   * Fill the correlation matrix
   */
  private void fillCorrelation() {
    m_correlation = new double[m_numAttribs+1][m_numAttribs+1];
    double [] att1 = new double [m_numInstances];
    double [] att2 = new double [m_numInstances];
    double corr;

    for (int i=1;i<=m_numAttribs;i++) {
      for (int j=1;j<=m_numAttribs;j++) {
	if (i == j) {
	  m_correlation[i][j] = 1.0;
	} else {
	  for (int k=0;k<m_numInstances;k++) {
	    att1[k] = m_trainInstances.instance(k).value(i-1);
	    att2[k] = m_trainInstances.instance(k).value(j-1);
	  }
	  corr = Utils.correlation(att1,att2,m_numInstances);
	  m_correlation[i][j] = corr;
	  m_correlation[i][j] = corr;
	}
      }
    }
  }

  /**
   * Return a summary of the analysis
   * @return a summary of the analysis.
   */
  private String principalComponentsSummary() {
    StringBuffer result = new StringBuffer();
    double sum = Utils.sum(m_eigenvalues);
    double cumulative = 0.0;
    Instances output = null;

    try {
      output = setOutputFormat();
    } catch (Exception ex) {
    }

    result.append("Correlation matrix\n"+matrixToString(m_correlation)
		  +"\n\n");
    result.append("eigenvalue\tproportion\tcumulative\n");
    for (int i=m_numAttribs;i>=1;i--) {
      cumulative+=m_eigenvalues[m_sortedEigens[i]];
      result.append(Utils.doubleToString(m_eigenvalues[m_sortedEigens[i]],9,5)
		    +"\t"+Utils.
		    doubleToString((m_eigenvalues[m_sortedEigens[i]]/sum),
				     9,5)
		    +"\t"+Utils.doubleToString((cumulative/sum),9,5)
		    +"\t"+output.attribute(m_numAttribs-i).name()+"\n");
    }

    result.append("\nEigenvectors\n");
    for (int j=1;j<=m_numAttribs;j++) {
      result.append(" V"+j+'\t');
    }
    result.append("\n");
    for (int j=1;j<=m_numAttribs;j++) {

      for (int i=m_numAttribs;i>=1;i--) {
	result.append(Utils.
		      doubleToString(m_eigenvectors[j][m_sortedEigens[i]],7,4)
		      +"\t");
      }
      result.append(m_trainInstances.attribute(j-1).name()+'\n');
    }
    return result.toString();
  }

  /**
   * Returns a description of this attribute transformer
   * @return a String describing this attribute transformer
   */
  public String toString() {
    if (m_eigenvalues == null) {
      return "Principal components hasn't been built yet!";
    } else {
      return "\tPrincipal Components Attribute Transformer\n\n"
	+principalComponentsSummary();
    }
  }

  /**
   * Return a matrix as a String
   * @return a String describing a matrix
   */
  private String matrixToString(double [][] matrix) {
    StringBuffer result = new StringBuffer();
    int size = matrix.length-1;

    for (int i=1;i<=size;i++) {
      for (int j=1;j<=size;j++) {
	result.append(Utils.doubleToString(matrix[i][j],6,2)+" ");
	if (j == size) {
	  result.append('\n');
	}
      }
    }
    return result.toString();
  }

  /**
   * Transform an instance in original (unormalized) format.
   * @param instance an instance in the original (unormalized) format
   * @return a transformed instance
   * @exception Exception if instance cant be transformed
   */
  public Instance convertInstance(Instance instance) throws Exception {

    if (m_eigenvalues == null) {
      throw new Exception("convertInstance: Principal components not "
			  +"built yet");
    }

    //    System.out.println("Original: "+instance);

    double[] newVals = new double[m_outputNumAtts];
    Instance tempInst = (Instance)instance.copy();

    m_replaceMissingFilter.input(tempInst);
    tempInst = m_replaceMissingFilter.output();
    
    if (m_normalize) {
      m_normalizeFilter.input(tempInst);
      tempInst = m_normalizeFilter.output();
    }

    m_nominalToBinFilter.input(tempInst);
    tempInst = m_nominalToBinFilter.output();

    if (m_attributeFilter != null) {
      m_attributeFilter.input(tempInst);
      tempInst = m_attributeFilter.output();
    }

    if (m_hasClass) {
       newVals[m_outputNumAtts - 1] = instance.value(instance.classIndex());
    }
    //    System.out.println("normalized etc: "+tempInst);

    for (int i = m_numAttribs; i >= 1; i--) {
      double tempval = 0.0;
      for (int j = 1; j <= m_numAttribs; j++) {
	tempval += (m_eigenvectors[j][m_sortedEigens[i]] * 
		    tempInst.value(j - 1));
      }
      newVals[m_numAttribs - i] = tempval;
    }
    
    if (instance instanceof SparseInstance) {
      return new SparseInstance(instance.weight(), newVals);
    } else {
      return new Instance(instance.weight(), newVals);
    }      
  }

  /**
   * Set the format for the transformed data
   * @return a set of empty Instances (header only) in the new format
   * @exception Exception if the output format can't be set
   */
  private Instances setOutputFormat() throws Exception {
    if (m_eigenvalues == null) {
      return null;
    }

    FastVector attributes = new FastVector();
     for (int i=m_numAttribs;i>=1;i--) {
       StringBuffer attName = new StringBuffer();
       for (int j=1;j<=m_numAttribs;j++) {
	 attName.append(Utils.
			doubleToString(m_eigenvectors[j][m_sortedEigens[i]],
				       5,3)
			+m_trainInstances.attribute(j-1).name());
	 if (j != m_numAttribs) {
	   if (m_eigenvectors[j+1][m_sortedEigens[i]] >= 0) {
	     attName.append("+");
	   }
	 }
       }
       attributes.addElement(new Attribute(attName.toString()));
     }
     
     if (m_hasClass) {
       attributes.addElement(m_trainCopy.classAttribute().copy());
     }

     Instances outputFormat = 
       new Instances(m_trainInstances.relationName()+"_principal components",
		     attributes, 0);

     // set the class to be the last attribute if necessary
     if (m_hasClass) {
       outputFormat.setClassIndex(outputFormat.numAttributes()-1);
     }
     
     m_outputNumAtts = outputFormat.numAttributes();
     return outputFormat;
  }

  // jacobi routine adapted from numerical recipies
  // note arrays are from 1..n inclusive
  void jacobi(double [][] a, int n, double [] d, double [][] v) {
    int j,iq,ip,i;
    double tresh,theta,tau,t,sm,s,h,g,c;
    double [] b;
    double [] z;

    b = new double [n+1];
    z = new double [n+1];
    for (ip=1;ip<=n;ip++) {
      for (iq=1;iq<=n;iq++) v[ip][iq]=0.0;
      v[ip][ip]=1.0;
    }
    for (ip=1;ip<=n;ip++) {
      b[ip]=d[ip]=a[ip][ip];
      z[ip]=0.0;
    }
    //    *nrot=0;
    for (i=1;i<=50;i++) {
      sm=0.0;
      for (ip=1;ip<=n-1;ip++) {
	for (iq=ip+1;iq<=n;iq++)
	  sm += Math.abs(a[ip][iq]);
      }
      if (sm == 0.0) {
	//	free_vector(z,1,n);
	//	free_vector(b,1,n);
	return;
      }
      if (i < 4)
	tresh=0.2*sm/(n*n);
      else
	tresh=0.0;
      for (ip=1;ip<=n-1;ip++) {
	for (iq=ip+1;iq<=n;iq++) {
	  g=100.0*Math.abs(a[ip][iq]);
	  if (i > 4 && (double)(Math.abs(d[ip])+g) == (double)Math.abs(d[ip])
	      && (double)(Math.abs(d[iq])+g) == (double)Math.abs(d[iq]))
	    a[ip][iq]=0.0;
	  else if (Math.abs(a[ip][iq]) > tresh) {
	    h=d[iq]-d[ip];
	    if ((double)(Math.abs(h)+g) == (double)Math.abs(h))
	      t=(a[ip][iq])/h;
	    else {
	      theta=0.5*h/(a[ip][iq]);
	      t=1.0/(Math.abs(theta)+Math.sqrt(1.0+theta*theta));
	      if (theta < 0.0) t = -t;
	    }
	    c=1.0/Math.sqrt(1+t*t);
	    s=t*c;
	    tau=s/(1.0+c);
	    h=t*a[ip][iq];
	    z[ip] -= h;
	    z[iq] += h;
	    d[ip] -= h;
	    d[iq] += h;
	    a[ip][iq]=0.0;
	    for (j=1;j<=ip-1;j++) {
	      //	      rotate(a,j,ip,j,iq)
	      g=a[j][ip];
	      h=a[j][iq];
	      a[j][ip]=g-s*(h+g*tau);
	      a[j][iq]=h+s*(g-h*tau);
	    }
	    for (j=ip+1;j<=iq-1;j++) {
	      //	      rotate(a,ip,j,j,iq)
	      g=a[ip][j];
	      h=a[j][iq];
	      a[ip][j]=g-s*(h+g*tau);
	      a[j][iq]=h+s*(g-h*tau);
	    }
	    for (j=iq+1;j<=n;j++) {
	      //	      rotate(a,ip,j,iq,j)

	      g=a[ip][j];
	      h=a[iq][j];
	      a[ip][j]=g-s*(h+g*tau);
	      a[iq][j]=h+s*(g-h*tau);
	    }
	    for (j=1;j<=n;j++) {
	      //	      rotate(v,j,ip,j,iq)

	      g=v[j][ip];
	      h=v[j][iq];
	      v[j][ip]=g-s*(h+g*tau);
	      v[j][iq]=h+s*(g-h*tau);
	    }
	    //	    ++(*nrot);
	  }
	}
      }
      for (ip=1;ip<=n;ip++) {
	b[ip] += z[ip];
	d[ip]=b[ip];
	z[ip]=0.0;
      }
    }
    System.err.println("Too many iterations in routine jacobi");
  }

  /**
   * Main method for testing this class
   * @param argv should contain the command line arguments to the
   * evaluator/transformer (see AttributeSelection)
   */
  public static void main(String [] argv) {
    try {
      System.out.println(AttributeSelection.
			 SelectAttributes(new PrincipalComponents(), argv));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}
