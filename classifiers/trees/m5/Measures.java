/*
 *    Measures.java
 *    Copyright (C) 1999 Yong Wang
 *
 */

package weka.classifiers.m5;

import java.io.*;
import java.util.*;

import weka.core.*;


/**
 * Class for performance measures
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */

public final class Measures {

  int type;                  /* type for printing */
  double correlation;        /* correlation coefficient */
  double meanAbsErr;         /* mean absolute error */
  double meanSqrErr;         /* mean sqaured error */
  
  /**
   * Constructs a Measures object which could containing the performance measures
   */
  public  Measures(){
    
    type = 0;
    correlation =0.0;
    meanAbsErr =0.0;
    meanSqrErr =0.0;
  }

  /**
   * Converts the performance measures to a string
   * @param absDev the absolute deviation of the class attribute
   * @param sd the standard deviation of the class attribute
   * @set the type of the performance measures, is one of "t","T","f","F","x", for training, test, training of one fold in cross-validation, test of one test in cross-validation, cross-validation final resuls respectively
   * @param smooth either "u" or "s" for unsmoothed or smoothed
   * @return the converted string
   */
  public final String  toString(double absDev,double sd,String set,String smooth){
    
    StringBuffer text = new StringBuffer();

    switch(type){
    case 0:
      text.append("    Correlation coefficient:\t\t" + M5Utils.doubleToStringF(correlation,9,3) + "\t\t\t      " + set + smooth);
      text.append("    Mean absolute error:\t\t" + M5Utils.doubleToStringG(meanAbsErr,9,4) + "\t\t\t      " + set + smooth);
      text.append("    Root mean squared error:\t\t" + M5Utils.doubleToStringG(Math.sqrt(Math.abs(meanSqrErr)),9,4) + "\t\t\t      " + set + smooth);
      text.append("    Relative absolute error:\t\t" + M5Utils.doubleToStringF(meanAbsErr/absDev*100.0,9,2) + " %\t\t\t      " + set + smooth);
      text.append("    Root relative squared error:\t" + M5Utils.doubleToStringF(Math.sqrt(Math.abs(meanSqrErr))/sd*100.0,9,2) + " %\t\t\t      " + set + smooth);
      break;
    case 1:
      text.append("    Correlation coefficient:\t\t" + M5Utils.doubleToStringF(correlation,9,3) + "\t\t\t      " + set + smooth);
      text.append("    Mean absolute error:\t\t" + M5Utils.doubleToStringG(meanAbsErr,9,4) + "\t\t\t      " + set + smooth);
      text.append("    Root mean squared error:\t\t" + M5Utils.doubleToStringG(Math.sqrt(Math.abs(meanSqrErr)),9,4) + "\t\t\t      " + set + smooth);
      text.append("    Relative absolute error:\t\tundefined\t\t\t      " + set + smooth);
      text.append("    Root relative squared error:\tundefined\t\t\t      " + set + smooth);
      break;
    case 2:
      text.append("    Correlation coefficient:\t\t" + M5Utils.doubleToStringF(correlation,9,3) + "\t\t\t      " + set + smooth);
      text.append("    Mean absolute error:\t\tundefined\t\t\t      " + set + smooth);
      text.append("    Root mean squared error:\t\tundefined\t\t\t      " + set + smooth);
      text.append("    Relative absolute error:\t\tundefined\t\t\t      " + set + smooth);
      text.append("    Root relative squared error:\tundefined\t\t\t      " + set + smooth);
      break;
    default:
      M5Utils.errorMsg("wrong type in Measures.print().");
    }

    return text.toString();
  }

  /**
   * Adds up performance measures for cross-validation
   * @param m performance measures of a fold
   */
  public final void  incremental(Measures m){

    correlation += m.correlation;
    meanAbsErr  += m.meanAbsErr;
    meanSqrErr  += m.meanSqrErr;
  }



}
