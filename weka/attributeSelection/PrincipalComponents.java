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
 * @version $Revision: 1.10 $
 */
public class PrincipalComponents extends AttributeEvaluator 
  implements AttributeTransformer, OptionHandler {
  
  /** The data to transform analyse/transform */
  private Instances m_trainInstances;

  /** Keep a copy for the class attribute (if set) */
  private Instances m_trainCopy;

  /** The header for the transformed data format */
  private Instances m_transformedFormat;

  /** The header for data transformed back to the original space */
  private Instances m_originalSpaceFormat;

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

  /** sum of the eigenvalues */
  private double m_sumOfEigenValues = 0.0;
  
  /** Filters for original data */
  private ReplaceMissingValuesFilter m_replaceMissingFilter;
  private NormalizationFilter m_normalizeFilter;
  private NominalToBinaryFilter m_nominalToBinFilter;
  private AttributeFilter m_attributeFilter;
  
  /** used to remove the class column if a class column is set */
  private AttributeFilter m_attribFilter;

  /** The number of attributes in the pc transformed data */
  private int m_outputNumAtts = -1;
  
  /** normalize the input data? */
  private boolean m_normalize = true;

  /** the amount of varaince to cover in the original data when
      retaining the best n PC's */
  private double m_coverVariance = 0.95;

  /** transform the data through the pc space and back to the original
      space ? */
  private boolean m_transBackToOriginal = false;

  /** holds the transposed eigenvectors for converting back to the
      original space */
  private double [][] m_eTranspose;

  /**
   * Returns a string describing this attribute transformer
   * @return a description of the evaluator suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "Performs a principal components analysis and transformation of "
      +"the data. Use in conjunction with a Ranker search. Dimensionality "
      +"reduction is accomplished by choosing enough eigenvectors to "
      +"account for some percentage of the variance in the original data---"
      +"default 0.95 (95%). Attribute noise can be filtered by transforming "
      +"to the PC space, eliminating some of the worst eigenvectors, and "
      +"then transforming back to the original space.";
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
    Vector newVector = new Vector(3);
    newVector.addElement(new Option("\tDon't normalize input data." 
				    , "D", 0, "-D"));

    newVector.addElement(new Option("\tRetain enough PC attributes to account "
				    +"\n\tfor this proportion of variance in "
				    +"the original data. (default = 0.95)",
				    "R",1,"-R"));
    
    newVector.addElement(new Option("\tTransform through the PC space and "
				    +"\n\tback to the original space."
				    , "O", 0, "-O"));
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
    resetOptions();
    String optionString;

    optionString = Utils.getOption('R', options);
    if (optionString.length() != 0) {
      Double temp;
      temp = Double.valueOf(optionString);
      setVarianceCovered(temp.doubleValue());
    }
    setNormalize(!Utils.getFlag('D', options));

    setTransformBackToOriginal(Utils.getFlag('O', options));
  }

  /**
   * Reset to defaults
   */
  private void resetOptions() {
    m_coverVariance = 0.95;
    m_normalize = true;
    m_sumOfEigenValues = 0.0;
    m_transBackToOriginal = false;
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
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String varianceCoveredTipText() {
    return "Retain enough PC attributes to account for this proportion of "
      +"variance.";
  }

  /**
   * Sets the amount of variance to account for when retaining
   * principal components
   * @param vc the proportion of total variance to account for
   */
  public void setVarianceCovered(double vc) {
    m_coverVariance = vc;
  }

  /**
   * Gets the proportion of total variance to account for when
   * retaining principal components
   * @return the proportion of variance to account for
   */
  public double getVarianceCovered() {
    return m_coverVariance;
  }

  /**
   * Returns the tip text for this property
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String transformBackToOriginalTipText() {
    return "Transform through the PC space and back to the original space. "
      +"If only the best n PCs are retained (by setting varianceCovered < 1) "
      +"then this option will give a dataset in the original space but with "
      +"less attribute noise.";
  }

  /**
   * Sets whether the data should be transformed back to the original
   * space
   * @param b true if the data should be transformed back to the
   * original space
   */
  public void setTransformBackToOriginal(boolean b) {
    m_transBackToOriginal = b;
  }
  
  /**
   * Gets whether the data is to be transformed back to the original
   * space.
   * @return true if the data is to be transformed back to the original space
   */
  public boolean getTransformBackToOriginal() {
    return m_transBackToOriginal;
  }

  /**
   * Gets the current settings of PrincipalComponents
   *
   * @return an array of strings suitable for passing to setOptions()
   */
  public String[] getOptions () {

    String[] options = new String[4];
    int current = 0;

    if (!getNormalize()) {
      options[current++] = "-D";
    }

    options[current++] = "-R"; options[current++] = ""+getVarianceCovered();

    if (getTransformBackToOriginal()) {
      options[current++] = "-O";
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
    m_sumOfEigenValues = 0.0;

    if (data.checkForStringAttributes()) {
      throw  new Exception("Can't handle string attributes!");
    }
    m_trainInstances = data;

    // make a copy of the training data so that we can get the class
    // column to append to the transformed data (if necessary)
    m_trainCopy = new Instances(m_trainInstances);
    
    m_replaceMissingFilter = new ReplaceMissingValuesFilter();
    m_replaceMissingFilter.inputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, 
					m_replaceMissingFilter);

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

    // any eigenvalues less than 0 are not worth anything --- change to 0
    for (int i=0;i<m_eigenvalues.length;i++) {
      if (m_eigenvalues[i] < 0) {
	m_eigenvalues[i] = 0.0;
      }
    }
    m_sortedEigens = Utils.sort(m_eigenvalues);
    m_sumOfEigenValues = Utils.sum(m_eigenvalues);

    m_transformedFormat = setOutputFormat();
    if (m_transBackToOriginal) {
      m_originalSpaceFormat = setOutputFormatOriginal();
      
      // new ordered eigenvector matrix
      int numVectors = (m_transformedFormat.classIndex() < 0) 
	? m_transformedFormat.numAttributes()
	: m_transformedFormat.numAttributes()-1;

      double [][] orderedVectors = 
	new double [m_eigenvectors.length][numVectors+1];
      
      // try converting back to the original space
      for (int i=m_numAttribs;i>(m_numAttribs-numVectors);i--) {
	for (int j=1;j<=m_numAttribs;j++) {
	  orderedVectors[j][m_numAttribs-i+1] = 
	    m_eigenvectors[j][m_sortedEigens[i]];
	}
      }
      
      // transpose the matrix
      int nr = orderedVectors.length;
      int nc = orderedVectors[0].length;
      m_eTranspose = 
	new double [nc][nr];
      for (int i=0;i<nc;i++) {
	for (int j=0;j<nr;j++) {
	  m_eTranspose[i][j] = orderedVectors[j][i];
	}
      }
    }
  }

  /**
   * Returns just the header for the transformed data (ie. an empty
   * set of instances. This is so that AttributeSelection can
   * determine the structure of the transformed data without actually
   * having to get all the transformed data through getTransformedData().
   * @return the header of the transformed data.
   * @exception Exception if the header of the transformed data can't
   * be determined.
   */
  public Instances transformedHeader() throws Exception {
    if (m_eigenvalues == null) {
      throw new Exception("Principal components hasn't been built yet");
    }
    if (m_transBackToOriginal) {
      return m_originalSpaceFormat;
    } else {
      return m_transformedFormat;
    }
  }

  /**
   * Gets the transformed training data.
   * @return the transformed training data
   * @exception Exception if transformed data can't be returned
   */
  public Instances transformedData() throws Exception {
    if (m_eigenvalues == null) {
      throw new Exception("Principal components hasn't been built yet");
    }

    Instances output;

    if (m_transBackToOriginal) {
      output = new Instances(m_originalSpaceFormat);
    } else {
      output = new Instances(m_transformedFormat);
    }
    for (int i=0;i<m_trainCopy.numInstances();i++) {
      Instance converted = convertInstance(m_trainCopy.instance(i));
      output.add(converted);
    }

    return output;
  }

  /**
   * Evaluates the merit of a transformed attribute. This is defined
   * to be 1 minus the cumulative variance explained. Merit can't
   * be meaningfully evaluated if the data is to be transformed back
   * to the original space.
   * @param att the attribute to be evaluated
   * @return the merit of a transformed attribute
   * @exception Exception if attribute can't be evaluated
   */
  public double evaluateAttribute(int att) throws Exception {
    if (m_eigenvalues == null) {
      throw new Exception("Principal components hasn't been built yet!");
    }

    if (m_transBackToOriginal) {
      return 1.0; // can't evaluate back in the original space!
    }

    // return 1-cumulative variance explained for this transformed att
    double cumulative = 0.0;
    for (int i=m_numAttribs;i>=m_numAttribs-att;i--) {
      cumulative += m_eigenvalues[m_sortedEigens[i]];
    }

    return 1.0-cumulative/m_sumOfEigenValues;
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
    double cumulative = 0.0;
    Instances output = null;
    int numVectors=0;

    try {
      output = setOutputFormat();
      numVectors = (output.classIndex() < 0) 
	? output.numAttributes()
	: output.numAttributes()-1;
    } catch (Exception ex) {
    }

    result.append("Correlation matrix\n"+matrixToString(m_correlation)
		  +"\n\n");
    result.append("eigenvalue\tproportion\tcumulative\n");
    for (int i=m_numAttribs;i>(m_numAttribs-numVectors);i--) {
      cumulative+=m_eigenvalues[m_sortedEigens[i]];
      result.append(Utils.doubleToString(m_eigenvalues[m_sortedEigens[i]],9,5)
		    +"\t"+Utils.
		    doubleToString((m_eigenvalues[m_sortedEigens[i]] / 
				    m_sumOfEigenValues),
				     9,5)
		    +"\t"+Utils.doubleToString((cumulative / 
						m_sumOfEigenValues),9,5)
		    +"\t"+output.attribute(m_numAttribs-i).name()+"\n");
    }

    result.append("\nEigenvectors\n");
    for (int j=1;j<=numVectors;j++) {
      result.append(" V"+j+'\t');
    }
    result.append("\n");
    for (int j=1;j<=m_numAttribs;j++) {

      for (int i=m_numAttribs;i>(m_numAttribs-numVectors);i--) {
	result.append(Utils.
		      doubleToString(m_eigenvectors[j][m_sortedEigens[i]],7,4)
		      +"\t");
      }
      result.append(m_trainInstances.attribute(j-1).name()+'\n');
    }

    if (m_transBackToOriginal) {
      result.append("\nPC space transformed back to original space.\n"
		    +"(Note: can't evaluate attributes in the original "
		    +"space)\n");
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
   * Convert a pc transformed instance back to the original space
   */
  private Instance convertInstanceToOriginal(Instance inst)
    throws Exception {
    double[] newVals;

    if (m_hasClass) {
      newVals = new double[m_numAttribs+1];
    } else {
      newVals = new double[m_numAttribs];
    }

    if (m_hasClass) {
      // class is always appended as the last attribute
      newVals[m_numAttribs] = inst.value(inst.numAttributes()-1);
    }

    for (int i=1;i<m_eTranspose[0].length;i++) {
      double tempval = 0.0;
      for (int j=1;j<m_eTranspose.length;j++) {
	tempval += (m_eTranspose[j][i] * 
		    inst.value(j - 1));
       }
      newVals[i - 1] = tempval;
    }
    
    if (inst instanceof SparseInstance) {
      return new SparseInstance(inst.weight(), newVals);
    } else {
      return new Instance(inst.weight(), newVals);
    }      
  }

  /**
   * Transform an instance in original (unormalized) format. Convert back
   * to the original space if requested.
   * @param instance an instance in the original (unormalized) format
   * @return a transformed instance
   * @exception Exception if instance cant be transformed
   */
  public Instance convertInstance(Instance instance) throws Exception {

    if (m_eigenvalues == null) {
      throw new Exception("convertInstance: Principal components not "
			  +"built yet");
    }

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

    double cumulative = 0;
    for (int i = m_numAttribs; i >= 1; i--) {
      double tempval = 0.0;
      for (int j = 1; j <= m_numAttribs; j++) {
	tempval += (m_eigenvectors[j][m_sortedEigens[i]] * 
		    tempInst.value(j - 1));
       }
      newVals[m_numAttribs - i] = tempval;
      cumulative+=m_eigenvalues[m_sortedEigens[i]];
      if ((cumulative / m_sumOfEigenValues) >= m_coverVariance) {
	break;
      }
    }
    
    if (!m_transBackToOriginal) {
      if (instance instanceof SparseInstance) {
      return new SparseInstance(instance.weight(), newVals);
      } else {
	return new Instance(instance.weight(), newVals);
      }      
    } else {
      if (instance instanceof SparseInstance) {
	return convertInstanceToOriginal(new SparseInstance(instance.weight(), 
							    newVals));
      } else {
	return convertInstanceToOriginal(new Instance(instance.weight(),
						      newVals));
      }
    }
  }

  /**
   * Set up the header for the PC->original space dataset
   */
  private Instances setOutputFormatOriginal() throws Exception {
    FastVector attributes = new FastVector();
    
    for (int i=0;i<m_numAttribs;i++) {
      String att = m_trainInstances.attribute(i).name();
      attributes.addElement(new Attribute(att));
    }
    
    if (m_hasClass) {
      attributes.addElement(m_trainCopy.classAttribute().copy());
    }

    Instances outputFormat = 
      new Instances(m_trainCopy.relationName()+"->PC->original space",
		    attributes, 0);
    
    // set the class to be the last attribute if necessary
    if (m_hasClass) {
      outputFormat.setClassIndex(outputFormat.numAttributes()-1);
    }

    return outputFormat;
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

    double cumulative = 0.0;
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
       cumulative+=m_eigenvalues[m_sortedEigens[i]];

       if ((cumulative / m_sumOfEigenValues) >= m_coverVariance) {
	 break;
       }
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
