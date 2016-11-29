/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    RuleGeneration.java
 *    Copyright (C) 2004 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.associations;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.TreeSet;

import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;
import weka.core.Statistics;
import weka.core.Utils;

/**
 * Class implementing the rule generation procedure of the predictive apriori
 * algorithm.
 * 
 * Reference: T. Scheffer (2001). <i>Finding Association Rules That Trade
 * Support Optimally against Confidence</i>. Proc of the 5th European Conf. on
 * Principles and Practice of Knowledge Discovery in Databases (PKDD'01), pp.
 * 424-435. Freiburg, Germany: Springer-Verlag.
 * <p>
 * 
 * The implementation follows the paper expect for adding a rule to the output
 * of the <i>n</i> best rules. A rule is added if: the expected predictive
 * accuracy of this rule is among the <i>n</i> best and it is not subsumed by a
 * rule with at least the same expected predictive accuracy (out of an
 * unpublished manuscript from T. Scheffer).
 * 
 * @author Stefan Mutter (mutter@cs.waikato.ac.nz)
 * @version $Revision$
 */
public class RuleGeneration implements Serializable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = -8927041669872491432L;

  /** The items stored as an array of of integer. */
  protected int[] m_items;

  /** Counter for how many transactions contain this item set. */
  protected int m_counter;

  /** The total number of transactions */
  protected int m_totalTransactions;

  /** Flag indicating whether the list fo the best rules has changed. */
  protected boolean m_change = false;

  /**
   * The minimum expected predictive accuracy that is needed to be a candidate
   * for the list of the best rules.
   */
  protected double m_expectation;

  /**
   * Threshold. If the support of the premise is higher the binomial
   * distrubution is approximated by a normal one.
   */
  protected static final int MAX_N = 300;

  /**
   * The minimum support a rule needs to be a candidate for the list of the best
   * rules.
   */
  protected int m_minRuleCount;

  /**
   * Sorted array of the mied points of the intervals used for prior estimation.
   */
  protected double[] m_midPoints;

  /** Hashtable conatining the estimated prior probabilities. */
  protected Hashtable<Double, Double> m_priors;

  /** The list of the actual <i>n</i> best rules. */
  protected TreeSet<RuleItem> m_best;

  /** Integer indicating the generation time of a rule. */
  protected int m_count;

  /** The instances. */
  protected Instances m_instances;

  /**
   * Constructor
   * 
   * @param itemSet item set for that rules should be generated. The item set
   *          will form the premise of the rules.
   */
  public RuleGeneration(ItemSet itemSet) {

    m_totalTransactions = itemSet.getTotalTransactions();
    m_counter = itemSet.support();
    m_items = itemSet.getItems();
  }

  /**
   * calculates the probability using a binomial distribution. If the support of
   * the premise is too large this distribution is approximated by a normal
   * distribution.
   * 
   * @param accuracy the accuracy value
   * @param ruleCount the support of the whole rule
   * @param premiseCount the support of the premise
   * @return the probability value
   */
  public static final double binomialDistribution(double accuracy,
    double ruleCount, double premiseCount) {

    double mu, sigma;

    if (premiseCount < MAX_N) {
      return Math
        .pow(
          2,
          (Utils.log2(Math.pow(accuracy, ruleCount))
            + Utils.log2(Math.pow((1.0 - accuracy), (premiseCount - ruleCount))) + PriorEstimation
            .logbinomialCoefficient((int) premiseCount, (int) ruleCount)));
    } else {
      mu = premiseCount * accuracy;
      sigma = Math.sqrt((premiseCount * (1.0 - accuracy)) * accuracy);
      return Statistics.normalProbability(((ruleCount + 0.5) - mu)
        / (sigma * Math.sqrt(2)));
    }
  }

  /**
   * calculates the expected predctive accuracy of a rule
   * 
   * @param ruleCount the support of the rule
   * @param premiseCount the premise support of the rule
   * @param midPoints array with all mid points
   * @param priors hashtable containing the prior probabilities
   * @return the expected predictive accuracy
   */
  public static final double expectation(double ruleCount, int premiseCount,
    double[] midPoints, Hashtable<Double, Double> priors) {

    double numerator = 0, denominator = 0;
    for (double midPoint : midPoints) {
      Double actualPrior = priors.get(new Double(midPoint));
      if (actualPrior != null) {
        if (actualPrior.doubleValue() != 0) {
          double addend = actualPrior.doubleValue()
            * binomialDistribution(midPoint, ruleCount, premiseCount);
          denominator += addend;
          numerator += addend * midPoint;
        }
      }
    }
    if (denominator <= 0 || Double.isNaN(denominator)) {
      System.out.println("RuleItem denominator: " + denominator);
    }
    if (numerator <= 0 || Double.isNaN(numerator)) {
      System.out.println("RuleItem numerator: " + numerator);
    }
    return numerator / denominator;
  }

  /**
   * Generates all rules for an item set. The item set is the premise.
   * 
   * @param numRules the number of association rules the use wants to mine. This
   *          number equals the size <i>n</i> of the list of the best rules.
   * @param midPoints the mid points of the intervals
   * @param priors Hashtable that contains the prior probabilities
   * @param expectation the minimum value of the expected predictive accuracy
   *          that is needed to get into the list of the best rules
   * @param instances the instances for which association rules are generated
   * @param best the list of the <i>n</i> best rules. The list is implemented as
   *          a TreeSet
   * @param genTime the maximum time of generation
   * @return all the rules with minimum confidence for the given item set
   */
  public TreeSet<RuleItem> generateRules(int numRules, double[] midPoints,
    Hashtable<Double, Double> priors, double expectation, Instances instances,
    TreeSet<RuleItem> best, int genTime) {

    boolean redundant = false;
    ArrayList<Object> consequences = new ArrayList<Object>(), consequencesMinusOne = new ArrayList<Object>();
    ItemSet premise;
    RuleItem current = null;

    Hashtable<ItemSet, Integer> hashtable;

    m_change = false;
    m_midPoints = midPoints;
    m_priors = priors;
    m_best = best;
    m_expectation = expectation;
    m_count = genTime;
    m_instances = instances;

    // create rule body
    premise = null;
    premise = new ItemSet(m_totalTransactions, new int[m_items.length]);
    // premise.m_items = new int[m_items.length];
    System.arraycopy(m_items, 0, premise.getItems(), 0, m_items.length);
    premise.setCounter(m_counter);

    do {
      m_minRuleCount = 1;
      while (expectation(m_minRuleCount, premise.support(), m_midPoints,
        m_priors) <= m_expectation) {
        m_minRuleCount++;
        if (m_minRuleCount > premise.support()) {
          return m_best;
        }
      }
      redundant = false;
      for (int i = 0; i < instances.numAttributes(); i++) {
        if (i == 0) {
          for (int j = 0; j < m_items.length; j++) {
            if (m_items[j] == -1) {
              consequences = singleConsequence(instances, j, consequences);
            }
          }
          if (premise == null || consequences.size() == 0) {
            return m_best;
          }
        }
        ArrayList<Object> allRuleItems = new ArrayList<Object>();
        int index = 0;
        do {
          int h = 0;
          while (h < consequences.size()) {
            RuleItem dummie = new RuleItem();
            current = dummie.generateRuleItem(premise,
              (ItemSet) consequences.get(h), instances, m_count,
              m_minRuleCount, m_midPoints, m_priors);
            if (current != null) {
              allRuleItems.add(current);
              h++;
            } else {
              consequences.remove(h);
            }
          }
          if (index == i) {
            break;
          }
          consequencesMinusOne = consequences;
          consequences = ItemSet.mergeAllItemSets(consequencesMinusOne, index,
            instances.numInstances());
          hashtable = ItemSet.getHashtable(consequencesMinusOne,
            consequencesMinusOne.size());
          consequences = ItemSet.pruneItemSets(consequences, hashtable);
          index++;
        } while (consequences.size() > 0);
        for (int h = 0; h < allRuleItems.size(); h++) {
          current = (RuleItem) allRuleItems.get(h);
          m_count++;
          if (m_best.size() < numRules) {
            m_change = true;
            redundant = removeRedundant(current);
          } else {
            if (current.accuracy() > m_expectation) {
              m_expectation = (m_best.first()).accuracy();
              m_best.remove(m_best.first());
              m_change = true;
              redundant = removeRedundant(current);
              m_expectation = (m_best.first()).accuracy();
              while (expectation(m_minRuleCount, (current.premise()).support(),
                m_midPoints, m_priors) < m_expectation) {
                m_minRuleCount++;
                if (m_minRuleCount > (current.premise()).support()) {
                  break;
                }
              }
            }
          }
        }

      }
    } while (redundant);
    return m_best;
  }

  /**
   * Methods that decides whether or not rule a subsumes rule b. The defintion
   * of subsumption is: Rule a subsumes rule b, if a subsumes b AND a has got
   * least the same expected predictive accuracy as b.
   * 
   * @param a an association rule stored as a RuleItem
   * @param b an association rule stored as a RuleItem
   * @return true if rule a subsumes rule b or false otherwise.
   */
  public static boolean aSubsumesB(RuleItem a, RuleItem b) {

    if (a.m_accuracy < b.m_accuracy) {
      return false;
    }
    for (int k = 0; k < a.premise().getItems().length; k++) {
      if (a.premise().getItems()[k] != b.premise().getItems()[k]) {
        if ((a.premise().getItems()[k] != -1 && b.premise().getItems()[k] != -1)
          || b.premise().getItems()[k] == -1) {
          return false;
        }
      }
      if (a.consequence().getItems()[k] != b.consequence().getItems()[k]) {
        if ((a.consequence().getItems()[k] != -1 && b.consequence().getItems()[k] != -1)
          || a.consequence().getItems()[k] == -1) {
          return false;
        }
      }
    }
    return true;

  }

  /**
   * generates a consequence of length 1 for an association rule.
   * 
   * @param instances the instances under consideration
   * @param attNum an item that does not occur in the premise
   * @param consequences FastVector that possibly already contains other
   *          consequences of length 1
   * @return FastVector with consequences of length 1
   */
  public static ArrayList<Object> singleConsequence(Instances instances,
    int attNum, ArrayList<Object> consequences) {

    ItemSet consequence;

    for (int i = 0; i < instances.numAttributes(); i++) {
      if (i == attNum) {
        for (int j = 0; j < instances.attribute(i).numValues(); j++) {
          // consequence = new ItemSet(instances.numInstances());
          consequence = new ItemSet(instances.numInstances(), new int[instances.numAttributes()]);
          // consequence.m_items = new int[instances.numAttributes()];
          consequence.setCounter(0);
          for (int k = 0; k < instances.numAttributes(); k++) {
            consequence.getItems()[k] = -1;
          }
          consequence.getItems()[i] = j;
          consequences.add(consequence);
        }
      }
    }
    return consequences;

  }

  /**
   * Method that removes redundant rules out of the list of the best rules. A
   * rule is in that list if: the expected predictive accuracy of this rule is
   * among the best and it is not subsumed by a rule with at least the same
   * expected predictive accuracy
   * 
   * @param toInsert the rule that should be inserted into the list
   * @return true if the method has changed the list, false otherwise
   */
  public boolean removeRedundant(RuleItem toInsert) {

    boolean redundant = false, fSubsumesT = false, tSubsumesF = false;
    RuleItem first;
    int subsumes = 0;
    Object[] best = m_best.toArray();
    for (Object element : best) {
      first = (RuleItem) element;
      fSubsumesT = aSubsumesB(first, toInsert);
      tSubsumesF = aSubsumesB(toInsert, first);
      if (fSubsumesT) {
        subsumes = 1;
        break;
      } else {
        if (tSubsumesF) {
          m_best.remove(first);
          subsumes = 2;
          redundant = true;
        }
      }
    }
    if (subsumes == 0 || subsumes == 2) {
      m_best.add(toInsert);
    }
    return redundant;
  }

  /**
   * Gets the actual maximum value of the generation time
   * 
   * @return the actual maximum value of the generation time
   */
  public int count() {

    return m_count;
  }

  /**
   * Gets if the list fo the best rules has been changed
   * 
   * @return whether or not the list fo the best rules has been changed
   */
  public boolean change() {

    return m_change;
  }

  /**
   * Returns the revision string.
   * 
   * @return the revision
   */
  @Override
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
