/*
 *    Ivector.java
 *    Copyright (C) 1999 Yong Wang
 *
 */

package weka.classifiers.m5;

import java.io.*;
import java.util.*;

import weka.core.*;


/**
 * Class for handling integer vector
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public final class Ivector {

  /**
   * Converts a string
   * @param a an integer vector
   * @param first the index of the first element needed printing in a[]
   * @param last the index of the last element needed printing in a[]
   * @return the converted string
   */
  public final static String  toString(int []a,int first,int last){
    
    int i;
    StringBuffer text = new StringBuffer();

    text.append("Print integer vector:\n");
    for(i=first;i<=last;i++)text.append("\t" + a[i]);
    text.append("\n");

    return text.toString();
  }

  /**
   * Makes a copy of the first n elements in an integer vector
   * @param a an integer vector
   * @param n the number of elemented needed copying
   * @return the copy of the integer vector
   */ 
  public final static int[]  copy(int a[],int n) {
    int i,b[];
    
    b = new int[n];
    for(i=0;i<n;i++)b[i]=a[i];
    return b;
  }

  /**
   * Outputs a new integer vector which contains all the values in two integer vectors; assuming list1 and list2 are 
   *     incrementally sorted and no identical integers within each integer vector
   * @param list1 integer vector 1
   * @param list2 integer vector 2 Input: list1,list2 
   * @return the new integer vector
   */
  public final static int []  combine(int[] list1,int[] list2){
    int i,j,k,count;
    int[] list;
    
    list= new int[list1[0]+list2[0]+1];
    count = 0;
    i=1;
    j=1;
    while(i<=list1[0] && j<=list2[0]){
      if(list1[i] < list2[j]) {list[count+1] = list1[i]; count++; i++;}
      else if(list1[i] > list2[j]) {list[count+1] = list2[j]; count++; j++;}
      else {list[count+1] = list1[i]; count++; i++; j++;}
    }
    if(i>list1[0])for(k=j;k<=list2[0];k++){list[count+1]=list2[k];count++;}
    if(j>list2[0])for(k=i;k<=list1[0];k++){list[count+1]=list1[k];count++;}
    list[0]=count;

    return list;
  }
  
}
