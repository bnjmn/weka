/*
 *    principalComponents.java
 *    Copyright (C) 1999 Mark Hall
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
 * @version $Revision 1.0 $
 */
public class PrincipalComponents extends AttributeTransformer {
  
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
  private double [][] m_covariance;

  /** Will hold the unordered linear transformations of the (normalized)
      original data */
  private double [][] m_eigenvectors;
  
  /** Eigenvalues for the corresponding eigenvectors */
  private double [] m_eigenvalues = null;

  /** Sorted eigenvalues */
  private int [] m_sortedEigens;
  
  /** Filters for original data */
  private ReplaceMissingValuesFilter m_replaceMissing;
  private NormalizationFilter m_normalize;
  private NominalToBinaryFilter m_nominalToBin;
  private AttributeFilter m_attributeFilter;
  
  /** used to remove the class column if a class column is set */
  private AttributeFilter m_attribFilter;

  /** The number of attributes in the transformed data */
  private int m_outputNumAtts = -1;

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

    if (data.checkForStringAttributes()) {
      throw  new Exception("Can't handle string attributes!");
    }
    m_trainInstances = data;

    // make a copy of the training data so that we can get the class
    // column to append to the transformed data (if necessary)
    m_trainCopy = new Instances(m_trainInstances);
    
    m_replaceMissing = new ReplaceMissingValuesFilter();
    m_replaceMissing.inputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, m_replaceMissing);

    m_normalize = new NormalizationFilter();
    m_normalize.inputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, m_normalize);


    m_nominalToBin = new NominalToBinaryFilter();
    m_nominalToBin.inputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, m_nominalToBin);

    if (m_trainInstances.classIndex() >=0) {
      m_hasClass = true;
      m_classIndex = m_trainInstances.classIndex();
      // get rid of the class column
      m_attributeFilter = new AttributeFilter();
      m_attributeFilter.setAttributeIndices(""+(m_classIndex+1));
      m_attributeFilter.setInvertSelection(false);
      m_attributeFilter.inputFormat(m_trainInstances);
      m_trainInstances = Filter.useFilter(m_trainInstances, m_attributeFilter);
    }

    m_numInstances = m_trainInstances.numInstances();
    m_numAttribs = m_trainInstances.numAttributes();

    fillCovariance();

    double [] d = new double[m_numAttribs+1]; 
    double [][] v = new double[m_numAttribs+1][m_numAttribs+1];

    jacobi(m_covariance, m_numAttribs, d, v);
    m_eigenvectors = (double [][])v.clone();
    
    m_eigenvalues = (double [])d.clone();
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
  private void fillCovariance() {
    m_covariance = new double[m_numAttribs+1][m_numAttribs+1];
    double [] att1 = new double [m_numInstances];
    double [] att2 = new double [m_numInstances];
    double corr;

    for (int i=1;i<=m_numAttribs;i++) {
      for (int j=1;j<=m_numAttribs;j++) {
	if (i == j) {
	  m_covariance[i][j] = 1.0;
	} else {
	  for (int k=0;k<m_numInstances;k++) {
	    att1[k] = m_trainInstances.instance(k).value(i-1);
	    att2[k] = m_trainInstances.instance(k).value(j-1);
	  }
	  corr = Utils.correlation(att1,att2,m_numInstances);
	  m_covariance[i][j] = corr;
	  m_covariance[i][j] = corr;
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

    result.append("Correlation matrix\n"+matrixToString(m_covariance)
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
   * @exception if instance cant be transformed
   */
  public Instance convertInstance(Instance instance) throws Exception {

    if (m_eigenvalues == null) {
      throw new Exception("convertInstance: Principal components not "
			  +"built yet");
    }

    //    System.out.println("Original: "+instance);

    Instance newInstance = new Instance(m_outputNumAtts);
    Instance tempInst = new Instance(instance);

    m_replaceMissing.input(tempInst);
    tempInst = m_replaceMissing.output();
    
    m_normalize.input(tempInst);
    tempInst = m_normalize.output();

    m_nominalToBin.input(tempInst);
    tempInst = m_nominalToBin.output();

    if (m_hasClass) {
      m_attributeFilter.input(tempInst);
      tempInst = m_attributeFilter.output();
      newInstance.setValue(m_outputNumAtts-1,
			   instance.value(instance.classIndex()));
    }
    //    System.out.println("normalized etc: "+tempInst);

    for (int i=m_numAttribs;i>=1;i--) {
      double tempval = 0.0;
      for (int j=1;j<=m_numAttribs;j++) {
	tempval += (m_eigenvectors[j][m_sortedEigens[i]] * 
		    tempInst.value(j-1));
       }
      newInstance.setValue(m_numAttribs-i,tempval);
    }
    
    return newInstance;
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
