/*
 *    Dvector.java
 *    Copyright (C) 1999 Yong Wang
 *
 */

package weka.classifiers.m5;

import java.io.*;
import java.util.*;
import weka.core.*;

/**
 * Class for handling a double vector.
 * @author Yong Wang (yongwang@cs.waikato.ac.nz)
 * @version $Revision: 1.3 $
 */
public final class Dvector {

  /**
   * Returns a copy of the first n elements of a double vector
   * @param a a double vector
   * @param n a[0:n-1] will be copied
   * @return a copy of a[0:n-1]
   */
  public static final double[]  copy(double a[],int n) {

    int i;
    double b[];
    
    b = new double[n];
    for(i=0;i<n;i++)b[i]=a[i];
    return b;
  }
  
  /**
   * Prints the indexed elements in a double vector
   * @param a a double vector
   * @param first the index of the first instance for printing
   * @param last the index of the last instance for printing
   */
  public static final void  print(double []a,int first,int last){
    
    int i;

    System.out.println("Print double vector:");
    for(i=first;i<=last;i++)System.out.print("\t" + M5Utils.doubleToStringG(a[i],1,3));
    System.out.println();
  }

}
