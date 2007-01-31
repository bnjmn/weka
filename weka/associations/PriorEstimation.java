/*
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

/*
 * PriorEstimation.java
 * Copyright (C) 2004 Stefan Mutter
 *
 */

package weka.associations;

import weka.core.Instances;
import weka.core.SpecialFunctions;
import weka.core.Utils;

import java.io.Serializable;
import java.util.Hashtable;
import java.util.Random;

/**
 * Class implementing the prior estimattion of the predictive apriori algorithm 
 * for mining association rules. 
 *
 * Reference: T. Scheffer (2001). <i>Finding Association Rules That Trade Support 
 * Optimally against Confidence</i>. Proc of the 5th European Conf.
 * on Principles and Practice of Knowledge Discovery in Databases (PKDD'01),
 * pp. 424-435. Freiburg, Germany: Springer-Verlag. <p>
 *
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision: 1.5 $ */

 public class PriorEstimation implements Serializable{
    
    /** for serialization */
    private static final long serialVersionUID = 5570863216522496271L;

    /** The number of rnadom rules. */
    protected int m_numRandRules;
    
    /** The number of intervals. */
    protected int m_numIntervals;
    
    /** The random seed used for the random rule generation step. */
    protected static final int SEED = 0;
    
    /** The maximum number of attributes for which a prior can be estimated. */
    protected static final int MAX_N = 1024;
    
    /** The random number generator. */
    protected Random m_randNum;
    
    /** The instances for which association rules are mined. */
    protected Instances m_instances;
    
    /** Flag indicating whether standard association rules or class association rules are mined. */
    protected boolean m_CARs;
    
    /** Hashtable to store the confidence values of randomly generated rules. */    
    protected Hashtable m_distribution;
    
    /** Hashtable containing the estimated prior probabilities. */
    protected  Hashtable m_priors;
    
    /** Sums up the confidences of all rules with a certain length. */
    protected double m_sum;
    
    /** The mid points of the discrete intervals in which the interval [0,1] is divided. */
    protected double[] m_midPoints;
    
    
    
   /**
   * Constructor 
   *
   * @param instances the instances to be used for generating the associations
   * @param numRules the number of random rules used for generating the prior
   * @param numIntervals the number of intervals to discretise [0,1]
   * @param car flag indicating whether standard or class association rules are mined
   */
    public PriorEstimation(Instances instances,int numRules,int numIntervals,boolean car) {
        
       m_instances = instances;
       m_CARs = car;
       m_numRandRules = numRules;
       m_numIntervals = numIntervals;
       m_randNum = m_instances.getRandomNumberGenerator(SEED);
    }
    /**
   * Calculates the prior distribution.
   *
   * @exception Exception if prior can't be estimated successfully
   */
    public final void generateDistribution() throws Exception{
        
        boolean jump;
        int i,maxLength = m_instances.numAttributes(), count =0,count1=0, ruleCounter;
        int [] itemArray;
        m_distribution = new Hashtable(maxLength*m_numIntervals);
        RuleItem current;
        ItemSet generate;
        
        if(m_instances.numAttributes() == 0)
            throw new Exception("Dataset has no attributes!");
        if(m_instances.numAttributes() >= MAX_N)
            throw new Exception("Dataset has to many attributes for prior estimation!");
        if(m_instances.numInstances() == 0)
            throw new Exception("Dataset has no instances!");
        for (int h = 0; h < maxLength; h++) {
            if (m_instances.attribute(h).isNumeric())
                throw new Exception("Can't handle numeric attributes!");
        } 
        if(m_numIntervals  == 0 || m_numRandRules == 0)
            throw new Exception("Prior initialisation impossible");
       
        //calculate mid points for the intervals
        midPoints();
        
        //create random rules of length i and measure their support and if support >0 their confidence
        for(i = 1;i <= maxLength; i++){
            m_sum = 0;
            int j = 0;
            count = 0;
            count1 = 0;
            while(j < m_numRandRules){
                count++;
                jump =false;
                if(!m_CARs){
                    itemArray = randomRule(maxLength,i,m_randNum);
                    current = splitItemSet(m_randNum.nextInt(i), itemArray);
                }
                else{
                    itemArray = randomCARule(maxLength,i,m_randNum);
                    current = addCons(itemArray);
                }
                int [] ruleItem = new int[maxLength];
                for(int k =0; k < itemArray.length;k++){
                    if(current.m_premise.m_items[k] != -1)
                        ruleItem[k] = current.m_premise.m_items[k];
                    else
                        if(current.m_consequence.m_items[k] != -1)
                            ruleItem[k] = current.m_consequence.m_items[k];
                        else
                            ruleItem[k] = -1;
                }
                ItemSet rule = new ItemSet(ruleItem);
                updateCounters(rule);
                ruleCounter = rule.m_counter;
                if(ruleCounter > 0)
                    jump =true;
                updateCounters(current.m_premise);
                j++;
                if(jump){
                    buildDistribution((double)ruleCounter/(double)current.m_premise.m_counter, (double)i);
                }
             }
            
            //normalize
            if(m_sum > 0){
                for(int w = 0; w < m_midPoints.length;w++){
                    String key = (String.valueOf(m_midPoints[w])).concat(String.valueOf((double)i));
                    Double oldValue = (Double)m_distribution.remove(key);
                    if(oldValue == null){
                        m_distribution.put(key,new Double(1.0/m_numIntervals));
                        m_sum += 1.0/m_numIntervals;
                    }
                    else
                        m_distribution.put(key,oldValue);
                }
                for(int w = 0; w < m_midPoints.length;w++){
                    double conf =0;
                    String key = (String.valueOf(m_midPoints[w])).concat(String.valueOf((double)i));
                    Double oldValue = (Double)m_distribution.remove(key);
                    if(oldValue != null){
                        conf = oldValue.doubleValue() / m_sum;
                        m_distribution.put(key,new Double(conf));
                    }
                }
            }
            else{
                for(int w = 0; w < m_midPoints.length;w++){
                    String key = (String.valueOf(m_midPoints[w])).concat(String.valueOf((double)i));
                    m_distribution.put(key,new Double(1.0/m_numIntervals));
                }
            }
        }
        
    }
    
    /**
     * Constructs an item set of certain length randomly.
     * This method is used for standard association rule mining.
     * @param maxLength the number of attributes of the instances
     * @param actualLength the number of attributes that should be present in the item set
     * @param randNum the random number generator
     * @return a randomly constructed item set in form of an int array
     */
    public final int[] randomRule(int maxLength, int actualLength, Random randNum){
     
        int[] itemArray = new int[maxLength];
        for(int k =0;k < itemArray.length;k++)
            itemArray[k] = -1;
        int help =actualLength;
        if(help == maxLength){
            help = 0;
            for(int h = 0; h < itemArray.length; h++){
                itemArray[h] = m_randNum.nextInt((m_instances.attribute(h)).numValues());
            }
        }
        while(help > 0){
            int mark = randNum.nextInt(maxLength);
            if(itemArray[mark] == -1){
                help--;
                itemArray[mark] = m_randNum.nextInt((m_instances.attribute(mark)).numValues());
            }
       }
        return itemArray;
    }
    
    
    /**
     * Constructs an item set of certain length randomly.
     * This method is used for class association rule mining.
     * @param maxLength the number of attributes of the instances
     * @param actualLength the number of attributes that should be present in the item set
     * @param randNum the random number generator
     * @return a randomly constructed item set in form of an int array
     */
     public final int[] randomCARule(int maxLength, int actualLength, Random randNum){
     
        int[] itemArray = new int[maxLength];
        for(int k =0;k < itemArray.length;k++)
            itemArray[k] = -1;
        if(actualLength == 1)
            return itemArray;
        int help =actualLength-1;
        if(help == maxLength-1){
            help = 0;
            for(int h = 0; h < itemArray.length; h++){
                if(h != m_instances.classIndex()){
                    itemArray[h] = m_randNum.nextInt((m_instances.attribute(h)).numValues());
                }
            }
        }
        while(help > 0){
            int mark = randNum.nextInt(maxLength);
            if(itemArray[mark] == -1 && mark != m_instances.classIndex()){
                help--;
                itemArray[mark] = m_randNum.nextInt((m_instances.attribute(mark)).numValues());
            }
       }
        return itemArray;
    }
   
     /**
      * updates the distribution of the confidence values.
      * For every confidence value the interval to which it belongs is searched
      * and the confidence is added to the confidence already found in this
      * interval.
      * @param conf the confidence of the randomly created rule
      * @param length the legnth of the randomly created rule
      */     
    public final void buildDistribution(double conf, double length){
     
        double mPoint = findIntervall(conf);
        String key = (String.valueOf(mPoint)).concat(String.valueOf(length));
        m_sum += conf;
        Double oldValue = (Double)m_distribution.remove(key);
        if(oldValue != null)
            conf = conf + oldValue.doubleValue();
        m_distribution.put(key,new Double(conf));
        
    }
    
    /**
     * searches the mid point of the interval a given confidence value falls into
     * @param conf the confidence of a rule
     * @return the mid point of the interval the confidence belongs to
     */    
     public final double findIntervall(double conf){
        
        if(conf == 1.0)
            return m_midPoints[m_midPoints.length-1];
        int end   = m_midPoints.length-1;
        int start = 0;
        while (Math.abs(end-start) > 1) {
            int mid = (start + end) / 2;
            if (conf > m_midPoints[mid])
                start = mid+1;
            if (conf < m_midPoints[mid]) 
                end = mid-1;
            if(conf == m_midPoints[mid])
                return m_midPoints[mid];
        }
        if(Math.abs(conf-m_midPoints[start]) <=  Math.abs(conf-m_midPoints[end]))
            return m_midPoints[start];
        else
            return m_midPoints[end];
    }
    
    
     /**
      * calculates the numerator and the denominator of the prior equation
      * @param weighted indicates whether the numerator or the denominator is calculated
      * @param mPoint the mid Point of an interval
      * @return the numerator or denominator of the prior equation
      */     
    public final double calculatePriorSum(boolean weighted, double mPoint){
  
      double distr, sum =0, max = logbinomialCoefficient(m_instances.numAttributes(),(int)m_instances.numAttributes()/2);
      
      
      for(int i = 1; i <= m_instances.numAttributes(); i++){
              
          if(weighted){
            String key = (String.valueOf(mPoint)).concat(String.valueOf((double)i));
            Double hashValue = (Double)m_distribution.get(key);
            
            if(hashValue !=null)
                distr = hashValue.doubleValue();
            else
                distr = 0;
                //distr = 1.0/m_numIntervals;
            if(distr != 0){
              double addend = Utils.log2(distr) - max + Utils.log2((Math.pow(2,i)-1)) + logbinomialCoefficient(m_instances.numAttributes(),i);
              sum = sum + Math.pow(2,addend);
            }
          }
          else{
              double addend = Utils.log2((Math.pow(2,i)-1)) - max + logbinomialCoefficient(m_instances.numAttributes(),i);
              sum = sum + Math.pow(2,addend);
          }
      }
      return sum;
  }
    /**
     * Method that calculates the base 2 logarithm of a binomial coefficient
     * @param upperIndex upper Inedx of the binomial coefficient
     * @param lowerIndex lower index of the binomial coefficient
     * @return the base 2 logarithm of the binomial coefficient
     */    
   public static final double logbinomialCoefficient(int upperIndex, int lowerIndex){
   
     double result =1.0;
     if(upperIndex == lowerIndex || lowerIndex == 0)
         return result;
     result = SpecialFunctions.log2Binomial((double)upperIndex, (double)lowerIndex);
     return result;
   }
   
   /**
    * Method to estimate the prior probabilities
    * @throws Exception throws exception if the prior cannot be calculated
    * @return a hashtable containing the prior probabilities
    */   
   public final Hashtable estimatePrior() throws Exception{
   
       double distr, prior, denominator, mPoint;
       
       Hashtable m_priors = new Hashtable(m_numIntervals);
       denominator = calculatePriorSum(false,1.0);
       generateDistribution();
       for(int i = 0; i < m_numIntervals; i++){ 
            mPoint = m_midPoints[i];
            prior = calculatePriorSum(true,mPoint) / denominator;
            m_priors.put(new Double(mPoint), new Double(prior));
       }
       return m_priors;
   }  
   
   /**
    * split the interval [0,1] into a predefined number of intervals and calculates their mid points
    */   
   public final void midPoints(){
        
        m_midPoints = new double[m_numIntervals];
        for(int i = 0; i < m_numIntervals; i++)
            m_midPoints[i] = midPoint(1.0/m_numIntervals, i);
   }
     
   /**
    * calculates the mid point of an interval
    * @param size the size of each interval
    * @param number the number of the interval.
    * The intervals are numbered from 0 to m_numIntervals.
    * @return the mid point of the interval
    */   
   public double midPoint(double size, int number){
    
       return (size * (double)number) + (size / 2.0);
   }
    
   /**
    * returns an ordered array of all mid points
    * @return an ordered array of doubles conatining all midpoints
    */   
   public final double[] getMidPoints(){
    
       return m_midPoints;
   }
   
   
   /**
    * splits an item set into premise and consequence and constructs therefore
    * an association rule. The length of the premise is given. The attributes
    * for premise and consequence are chosen randomly. The result is a RuleItem.
    * @param premiseLength the length of the premise
    * @param itemArray a (randomly generated) item set
    * @return a randomly generated association rule stored in a RuleItem
    */   
    public final RuleItem splitItemSet (int premiseLength, int[] itemArray){
        
       int[] cons = new int[m_instances.numAttributes()];
       System.arraycopy(itemArray, 0, cons, 0, itemArray.length);
       int help = premiseLength;
       while(help > 0){
            int mark = m_randNum.nextInt(itemArray.length);
            if(cons[mark] != -1){
                help--;
                cons[mark] =-1;
            }
       }
       if(premiseLength == 0)
            for(int i =0; i < itemArray.length;i++)
                itemArray[i] = -1;
       else
           for(int i =0; i < itemArray.length;i++)
               if(cons[i] != -1)
                    itemArray[i] = -1;
       ItemSet premise = new ItemSet(itemArray);
       ItemSet consequence = new ItemSet(cons);
       RuleItem current = new RuleItem();
       current.m_premise = premise;
       current.m_consequence = consequence;
       return current;
    }

    /**
     * generates a class association rule out of a given premise.
     * It randomly chooses a class label as consequence.
     * @param itemArray the (randomly constructed) premise of the class association rule
     * @return a class association rule stored in a RuleItem
     */    
    public final RuleItem addCons (int[] itemArray){
        
        ItemSet premise = new ItemSet(itemArray);
        int[] cons = new int[itemArray.length];
        for(int i =0;i < itemArray.length;i++)
            cons[i] = -1;
        cons[m_instances.classIndex()] = m_randNum.nextInt((m_instances.attribute(m_instances.classIndex())).numValues());
        ItemSet consequence = new ItemSet(cons);
        RuleItem current = new RuleItem();
        current.m_premise = premise;
        current.m_consequence = consequence;
        return current;
    }
    
    /**
     * updates the support count of an item set
     * @param itemSet the item set
     */    
    public final void updateCounters(ItemSet itemSet){
        
        for (int i = 0; i < m_instances.numInstances(); i++) 
            itemSet.upDateCounter(m_instances.instance(i));
    }
  

}
