/*
 *    M5Utils.java
 *    Copyright (C) 1998  Yong Wang
 *
 */

package weka.classifiers.m5;

import java.io.*;
import java.util.*;
import weka.core.*;


/**
 * Class for some small methods used in M5Java
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */

public final class M5Utils {

  /**
   * Tests if enumerated attribute(s) exists in the instances
   * @param inst instances
   * @return true if there is at least one; false if none
   */
  public final static boolean  hasEnumAttr(Instances inst){
    int j;
    boolean b=false;
    
    for(j=0;j<inst.numAttributes();j++)if(inst.attribute(j).isNominal() == true)b=true;

    return b;
  }

  /**
   * Tests if missing value(s) exists in the instances
   * @param inst instances
   * @return true if there is missing value(s); false if none
   */
  public final static boolean hasMissing(Instances inst){
    int i,j;
    boolean b=false;
    
    for(i=0;i<inst.numInstances();i++)
      for(j=0;j<inst.numAttributes();j++)
	if(inst.instance(i).isMissing(j) == true)b=true;

    return b;
  }

  /**
   * Returns the sum of the instances values of an attribute
   * @param attr an attribute
   * @param inst instances
   * @return the sum value
   */
  public final static double  sum(int attr,Instances inst) {
    int i;
    double sum=0.0;
    
    for(i=0;i<=inst.numInstances()-1;i++){
      sum += inst.instance(i).value(attr);
    }
    
    return sum;
  }

  /**
   * Returns the squared sum of the instances values of an attribute
   * @param attr an attribute
   * @param inst instances
   * @return the squared sum value
   */
  public final static double  sqrSum(int attr,Instances inst)
  {
    int i;
    double sqrSum=0.0,value;
    
    for(i=0;i<=inst.numInstances()-1;i++){
      value = inst.instance(i).value(attr);
      sqrSum += value * value;
    }
    
    return sqrSum;
  }
  
  /**
   * Returns the standard deviation value of the instances values of an attribute
   * @param attr an attribute
   * @param inst instances
   * @return the standard deviation value
   */
  public final static double  stdDev(int attr,Instances inst)
  {
    int i,count=0;
    double sd,va,sum=0.0,sqrSum=0.0,value;
    
    for(i=0;i<=inst.numInstances()-1;i++){
      count++;
      value=inst.instance(i).value(attr);
      sum +=  value;
      sqrSum += value * value;
    }
    
    if(count >1){
      va = (sqrSum - sum * sum / count)/count;
      va = Math.abs(va);
      sd = Math.sqrt(va);
    }
    else sd=0.0;

    return sd;
  }

  /**
   * Returns the absolute deviation value of the instances values of an attribute
   * @param attr an attribute
   * @param inst instances
   * @return the absolute deviation value
   */
  public final static double  absDev(int attr,Instances inst){
    
    int i;
    double average=0.0,absdiff=0.0,absDev;
    
    for(i=0;i<=inst.numInstances()-1;i++){
      average  += inst.instance(i).value(attr);
    }
    if(inst.numInstances() >1){
      average /= (double)inst.numInstances();
      for(i=0;i<=inst.numInstances()-1;i++){
	absdiff += Math.abs(inst.instance(i).value(attr) - average);
      }
      absDev = absdiff / (double)inst.numInstances();
    }
    else absDev=0.0;
   
    return absDev;
  }

  /**
   * Returns the variance value of the instances values of an attribute
   * @param attr an attribute
   * @param inst instances
   * @return the variance value
   */
  public final static double  variance(int attr,Instances inst) {
    int i,count=0;
    double value,sum=0.0,sqrSum=0.0,va;
    
    for(i=0;i<=inst.numInstances()-1;i++){
      value = inst.instance(i).value(attr);
      sum  += value;
      sqrSum += value * value;
      count ++;
    }
    if(count > 0){
      va=(sqrSum - sum * sum /count)/count;
    }
    else va=0.0;
    return va;
  }

  /**
   * Rounds a double
   * @param value the double value
   * @return the double rounded
   */
  public final static long  roundDouble(double value){

    long roundedValue;
    
    roundedValue = value>0? (long)(value+0.5) : -(long)(Math.abs(value)+0.5);
    
    return roundedValue;
  }

  /**
   * Returns the largest (closest to positive infinity) long integer value that is not greater than the argument. 
   * @param value the double value
   * @return the floor integer
   */
  public final static long  floorDouble(double value){
    
    long floorValue;
   
    floorValue = value>0? (long)value : -(long)(Math.abs(value)+1);
    
    return floorValue;
  }
  
  /**
   * Rounds a double and converts it into a formatted right-justified String.
   * It is like %f format in C language.
   * @param value the double value
   * @param width the width of the string
   * @param afterDecimalPoint the number of digits after the decimal point
   * @return the double as a formatted string
   */
  
  public final static String  doubleToStringF(double value,int width,int afterDecimalPoint){
    
    StringBuffer stringBuffer;
    String resultString;
    double temp;
    int i,dotPosition;
    long precisionValue;
    
    if(afterDecimalPoint < 0) afterDecimalPoint = 0;
    precisionValue=0;
    temp = value * Math.pow(10.0,afterDecimalPoint);
    if(Math.abs(temp) < Long.MAX_VALUE){ 
      precisionValue = roundDouble(temp);
      
      if (precisionValue == 0){
	resultString = String.valueOf(0);
	stringBuffer = new StringBuffer(resultString);
	stringBuffer.append(".");
	for(i=1;i<=afterDecimalPoint;i++){
	  stringBuffer.append("0");
	}
	resultString = stringBuffer.toString();
      }
      else {
	resultString = String.valueOf(precisionValue);
	stringBuffer = new StringBuffer(resultString);
	dotPosition = stringBuffer.length() - afterDecimalPoint;
	while(dotPosition < 0) {stringBuffer.insert(0,0); dotPosition++;}
	stringBuffer.insert(dotPosition,".");
	if(stringBuffer.charAt(0) == '.')stringBuffer.insert(0,0);
	resultString = stringBuffer.toString();
      }
    }
    else resultString = new String("NaN");;
    
    // Fill in space characters.
    stringBuffer = new StringBuffer(Math.max(width,resultString.length()));
    for (i=0;i<stringBuffer.capacity()-resultString.length();i++)
      stringBuffer.append(' ');
    stringBuffer.append(resultString);

    return stringBuffer.toString();
  }

  /**
   * Rounds a double and converts it into a formatted right-justified String. If the double is not equal to zero and not in the range [10e-3,10e7] it is returned in scientific format.
   * It is like %g format in C language.
   * @param value the double value
   * @param width the width of the string
   * @param precision the number of valid digits
   * @return the double as a formatted string
   */
  public final static String  doubleToStringG(double value,int width,int precision){

    StringBuffer stringBuffer;
    String resultString;
    double temp;
    int i,dotPosition,exponent=0;
    long precisionValue;

    if(precision<=0)precision=1;
    precisionValue = 0;
    exponent = 0;
    if(value != 0.0){
      exponent = (int) floorDouble(Math.log(Math.abs(value))/Math.log(10));
      temp = value * Math.pow(10.0,precision-exponent-1);
      precisionValue = roundDouble(temp);         // then output value =  precisionValue * pow(10,exponent+1-precision)
      if(precision-1 != (int)(Math.log(Math.abs(precisionValue)+0.5)/Math.log(10))) {
	exponent ++;
	precisionValue = roundDouble(precisionValue/10.0);
      }
    }

    if (precisionValue == 0){                                           // value = 0.0
      resultString = String.valueOf("0");
    }
    else {	
      if(precisionValue >= 0)dotPosition=1;
      else dotPosition=2;
      if(exponent < -3 || precision-1+exponent > 7){                   // Scientific format.
	resultString = String.valueOf(precisionValue);
	stringBuffer = new StringBuffer(resultString);
	stringBuffer.insert(dotPosition,".");
	stringBuffer = deleteTrailingZerosAndDot(stringBuffer);
	stringBuffer.append("e").append(String.valueOf(exponent));
	resultString = stringBuffer.toString();
      }
      else {                                                          // 
	resultString = String.valueOf(precisionValue);
	stringBuffer = new StringBuffer(resultString);
	for(i=1;i<=-exponent;i++){
	  stringBuffer.insert(dotPosition-1,"0");
	}
	if(exponent <= -1) stringBuffer.insert(dotPosition,".");
	else if(exponent <= precision-1)stringBuffer.insert(dotPosition+exponent,".");
	else {
	  for(i=1;i<=exponent-(precision-1);i++){
	    stringBuffer.append("0");
	  }
	  stringBuffer.append(".");
	}

	// deleting trailing zeros and dot
	stringBuffer = deleteTrailingZerosAndDot(stringBuffer);
	resultString = stringBuffer.toString();
      }
    }
    // Fill in space characters.

    stringBuffer = new StringBuffer(Math.max(width,resultString.length()));
    for (i=0;i<stringBuffer.capacity()-resultString.length();i++)
      stringBuffer.append(' ');
    stringBuffer.append(resultString);

    return stringBuffer.toString();
  }

  /**
   * Deletes the trailing zeros and decimal point in a stringBuffer
   * @param stringBuffer string buffer
   * return string buffer with deleted trailing zeros and decimal point
   */
  public final static StringBuffer  deleteTrailingZerosAndDot(StringBuffer stringBuffer){
    
    while (stringBuffer.charAt(stringBuffer.length()-1) == '0' || stringBuffer.charAt(stringBuffer.length()-1) == '.'){
      if(stringBuffer.charAt(stringBuffer.length()-1) == '0')
	stringBuffer.setLength(stringBuffer.length()-1);
      else {
	stringBuffer.setLength(stringBuffer.length()-1);
	break;
      }
    }
    return stringBuffer;
  }
  
  /**
   * Returns the smoothed values according to the smoothing formula (np+kq)/(n+k)
   * @param p a double, normally is the prediction of the model at the current node
   * @param q a double, normally is the prediction of the model at the up node
   * @param n the number of instances at the up node
   * @param k the smoothing constance, default =15
   * @return the smoothed value
   */
  public final static double  smoothenValue(double p,double q,int n,int k)
  {
    return (n*p+k*q)/(double)(n+k);
  }
  
  /**
   * Returns the correlation coefficient of two double vectors
   * @param y1 double vector 1
   * @param y2 double vector 2
   * @param n the length of two double vectors
   * @return the correlation coefficient
   */
  public final static double correlation(double y1[],double y2[],int n) {

    int i;
    double av1=0.0,av2=0.0,y11=0.0,y22=0.0,y12=0.0,c;
    
    if(n<=1)return 1.0;
    for(i=0;i<n;i++){
      av1 += y1[i];
      av2 += y2[i];
    }
    av1 /= (double)n;
    av2 /= (double)n;
    for(i=0;i<n;i++){
      y11 += (y1[i] - av1) * (y1[i] - av1);
      y22 += (y2[i] - av2) * (y2[i] - av2);
      y12 += (y1[i] - av1) * (y2[i] - av2);
    }
    if(y11*y22 == 0.0) c=1.0;
    else c = y12 / Math.sqrt(Math.abs(y11*y22));
    
    return c;
  }
  
  /** 
   * Tests if two double values are equal to each other
   * @param a double 1
   * @param b double 2
   * @return true if equal; false if not equal
   */
  public final static boolean  eqDouble(double a,double b){
    
    if(Math.abs(a) < 1e-10 && Math.abs(b) < 1e-10) return true;
    double c = Math.abs(a) + Math.abs(b);
    if(Math.abs(a-b) < c * 1e-10) return true;
    else return false;
  }
  
  /** 
   * Prints error message and exits
   * @param err error message
   */
  public final static void errorMsg(String err){
    System.out.print("Error: ");
    System.out.println(err);
    System.exit(1);
  }
  
  /** 
   * Prints sepearating line
   */
  public final static String  separatorToString() {
    return "--------------------------------------------------------------------------------\n";
  }
  
  /**
   * Prints the head lines of the output
   */
  public final static String  headToString() {
    
    StringBuffer text =  new StringBuffer();
    
    text.append("M5Java version "+Options.VERSION + "\n");
    text.append("Copyright (C) 1997 - 1998 Yong Wang (yongwang@cs.waikato.ac.nz)\n");
    text.append("This is free software, and you are welcome to redistribute it under certain\n");
    text.append("conditions (see source code files for details).\n\n");

    return text.toString();
  }
  
  
}


