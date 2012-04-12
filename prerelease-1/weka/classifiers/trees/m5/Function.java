/*
 *    Function.java
 *    Copyright (C) 1999 Yong Wang
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
package weka.classifiers.m5;

import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for handling a linear function.
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version 1.0
 */

public final class Function implements Serializable {

  int terms[];
  double coeffs[];

  /**
   * Constructs a function of constant value
   */
  public Function() {
    terms = new int[1];
    terms[0] = 0;
    coeffs = new double[1];
    coeffs[0] = 0.0;
  }

  /**
   * Constucts a function with all attributes except the class in the inst
   * @param inst instances 
   */
  public Function(Instances inst) {
    
    int i,count=0;
    
    terms = new int[inst.numAttributes()];
    for(i=0;i<inst.numAttributes()-1;i++)
      if(i != inst.classIndex()) terms[++count]=i;
    terms[0] = count;

    coeffs = new double[count+1];
  }

  /**
   * Constructs a function with one attribute 
   * @param attr an attribute
   */
  public Function(int attr) {
    terms = new int[2];
    terms[0] = 1;
    terms[1] = attr;
    coeffs = new double[2];
    coeffs[0] = 0.0;
    coeffs[1] = 0.0;
  }
  
  /**
   * Makes a copy of a function
   * @return the copy of the function
   */
  public final Function  copy() {

    Function fcopy = new Function();
    
    fcopy.terms = Ivector.copy(terms,terms[0]+1);
    fcopy.coeffs = Dvector.copy(coeffs,terms[0]+1);

    return fcopy;
  }

  /**
   * Converts a function to a string
   * @param inst instances 
   * @param startPoint the starting point on the screen; used to feed line before reaching beyond 80 characters
   * @return the converted string
   * @exception Exception if something goes wrong
   */ 
  public final String  toString(Instances inst,int startPoint) throws Exception {
    
    int i,j,count1,count,precision=3;
    String string;
    StringBuffer text = new StringBuffer();

    count1 = count = startPoint + inst.classAttribute().name().length() + 3;   
    string = M5Utils.doubleToStringG(coeffs[0],1,precision);
    if(coeffs[0]>=0.0)count += string.length();
    else count += 1 + string.length();
    text.append(inst.classAttribute().name() + " = " + string);
    for(i=1;i<=terms[0];i++){
      string = M5Utils.doubleToStringG(Math.abs(coeffs[i]),1,precision);
      count += 3 + string.length() + inst.attribute(terms[i]).name().length();
      if(count>80){
	text.append("\n");
	for(j=1;j<=count1-1;j++)text.append(" ");
	count = count1-1 + 3 + string.length() + inst.attribute(terms[i]).name().length();
      }
      if(coeffs[i] >= 0.0)text.append(" + "); else text.append(" - ");
      text.append(string + inst.attribute(terms[i]).name());
    }
    
    return text.toString();
  }

  /**
   * Constructs a new function of which the variable list is a combination of those of two functions
   * @param f1 function 1
   * @param f2 function 2
   * @return the newly constructed function
   */
  public final static Function  combine(Function f1,Function f2) {
    Function f= new Function();
    
    f.terms = Ivector.combine(f1.terms,f2.terms);
    f.coeffs = new double[f.terms[0]+1];

    return f;
  }

  /**
   * Evaluates a function 
   * @param inst instances 
   * @return the evaluation results
   * @exception Exception if something goes wrong
   */
  public final Errors  errors(Instances inst) throws Exception {
    int i;
    double tmp;
    Errors e = new Errors(0,inst.numInstances()-1);

    for(i=0;i<=inst.numInstances()-1;i++){
      tmp = this.predict(inst.instance(i)) - inst.instance(i).classValue();
      e.sumErr += tmp;
      e.sumAbsErr += Math.abs(tmp);
      e.sumSqrErr += tmp * tmp;
    }
  
    e.meanAbsErr = e.sumAbsErr / e.numInstances;
    e.meanSqrErr = (e.sumSqrErr - e.sumErr * e.sumErr / e.numInstances)/e.numInstances;
    e.meanSqrErr = Math.abs(e.meanSqrErr);
    e.rootMeanSqrErr = Math.sqrt(e.meanSqrErr);
    
    return e;
  }

  /**
   * Returns the predicted value of instance i by a function
   * @param i instance i
   * @param inst instances 
   * @return the predicted value
   */
  public final double  predict(Instance instance){
    int j;
    double y;
    
    y = coeffs[0];
    for(j=1;j<=terms[0];j++){
      y += coeffs[j] * instance.value(terms[j]);
    }
    return y;
  }
  
  /**
   * Detects the most insignificant variable in the funcion 
   * @param sdy the standard deviation of the class variable
   * @param inst instances 
   * @return the index of the most insignificant variable in the function
   */
  public final int  insignificant(double sdy,Instances inst)
  {
    int j,jmin=-1,jmax=-1;
    double min=2.0,max=2.5,sdx,contribution;
    
    for(j=1;j<=terms[0];j++){
      sdx=M5Utils.stdDev(terms[j],inst);
      if(sdy==0.0)contribution = 0.0;
      else contribution = Math.abs(coeffs[j] * sdx / sdy);
      if(contribution <  min){
	min=contribution;jmin=j;
      }
      if(contribution > max){
	max=contribution;jmax=j;
      }
    }
    if(max>2.5)jmin=jmax; 
    
    return jmin;
  }
  
  /**
   * Removes a term from the function 
   * @param j the j-th index in the variable list in the function
   * @return the new function with the term removed
   */
  public final Function  remove(int j) {
    int i;
    Function f = new Function();
  
    f.terms = new int[terms[0]];
    f.terms[0] = terms[0]-1;
    for(i=1;i<j;i++)f.terms[i] = terms[i];
    for(i=j;i<=terms[0]-1;i++){
      f.terms[i] = terms[i+1];
    }
    f.coeffs = new double[f.terms[0]+1];
   
    return f;
  }

  
}
