/*
 *    Errors.java
 *    Copyright (C) 1999 Yong Wang
 *
 */

package weka.classifiers.m5;

import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for containing the evaluation results of a model
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public final class Errors implements Serializable {
  int numInstances;         // number of total instances 
  int missingInstances;     // number of instances with missing class values 
  double sumErr;            // sum of errors 
  double sumAbsErr;         // sum of the absolute errors 
  double sumSqrErr;         // sum of the squared errors 
  double meanSqrErr;        // mean squared error 
  double rootMeanSqrErr;    // sqaure root of the mean squared error 
  double meanAbsErr;        // mean absolute error 

  /**
   * Constructs an object which could contain the evaluation results of a model
   * @param first the index of the first instance
   * @param last the index of the last instance
   */
  public Errors(int first,int last){
    numInstances     = last-first+1;    
    missingInstances = 0;
    sumErr           = 0.0;         
    sumAbsErr        = 0.0;      
    sumSqrErr        = 0.0;      
    meanSqrErr       = 0.0;     
    rootMeanSqrErr   = 0.0; 
    meanAbsErr       = 0.0;     
  }

  /**
   * Makes a copy of the Errors object
   * @return the copy
   */
  public final Errors  copy(){
    
    Errors e = new Errors(0,0);
    
    e.numInstances     = numInstances;
    e.missingInstances = missingInstances;
    e.sumErr           = sumErr;
    e.sumAbsErr        = sumAbsErr;
    e.sumSqrErr        = sumSqrErr;
    e.meanSqrErr       = meanSqrErr;
    e.rootMeanSqrErr   = rootMeanSqrErr;
    e.meanAbsErr       = meanAbsErr;
   
    return e;
  }

  /**
   * Converts the evaluation results of a model to a string
   * @return the converted string
   */
  public final String  toString(){

    StringBuffer text = new StringBuffer();
        
    if(this == null)text.append("    Errors:\t\tnull\n");
    else {
      text.append("    Number of instances:\t" + numInstances + " (" + missingInstances + " missing)\n");
      text.append("    Sum of errors:\t\t" + sumErr + "\n");
      text.append("    Sum of absolute errors:\t" + sumAbsErr + "\n");
      text.append("    Sum of squared errors:\t" + sumSqrErr + "\n");
      text.append("    Mean squared error:\t\t" + meanSqrErr + "\n");
      text.append("    Root mean squared error:\t" + rootMeanSqrErr + "\n");
      text.append("    Mean absolute error:\t" + meanAbsErr + "\n");
    }

    return text.toString();
  }

}








