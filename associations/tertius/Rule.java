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
 *    Rule.java
 *    Copyright (C) 2003 Peter A. Flach, Nicolas Lachiche
 *
 *    Thanks to Amelie Deltour for porting the original C code to Java
 *    and integrating it into Weka.
 */

package weka.associations.tertius;

import weka.core.Instance;
import weka.core.Instances;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;

/**
 * Class representing a rule with a body and a head.
 *
 * @author  <a href="mailto:adeltour@netcourrier.com">Amelie Deltour</a>
 * @version $Revision: 1.6 $
 */

public class Rule implements Serializable, Cloneable {

  /** for serialization */
  private static final long serialVersionUID = -7763378359090435505L;

  /** The body of the rule. */
  private Body m_body;

  /** The head of the rule. */
  private Head m_head;

  /** Can repeat predicates in the rule ? */
  private boolean m_repeatPredicate;

  /** Maximal number of literals in the rule. */
  private int m_maxLiterals;

  /** Can there be negations in the body ? */
  private boolean m_negBody;
  
  /** Can there be negations in the head ? */
  private boolean m_negHead;

  /** Is this rule a classification rule ? */
  private boolean m_classRule;

  /** Can there be only one literal in the head ? */
  private boolean m_singleHead;

  /** Number of instances in the data this rule deals with. */
  private int m_numInstances;

  /** Set of counter-instances of this rule. */
  private ArrayList m_counterInstances;

  /** Counter for the counter-instances of this rule. */
  private int m_counter;

  /** Confirmation of this rule. */
  private double m_confirmation;

  /** Optimistic estimate of this rule. */
  private double m_optimistic;

  /**
   * Constructor for a rule when the counter-instances are not stored,
   * giving all the constraints applied to this rule.
   * 
   * @param repeatPredicate True if predicates can be repeated.
   * @param maxLiterals Maximum number of literals.
   * @param negBody True if negation is allowed in the body.
   * @param negHead True if negation is allowed in the head. 
   * @param classRule True if the rule is a classification rule.
   * @param horn True if the rule is a horn clause.
   */
  public Rule(boolean repeatPredicate, int maxLiterals, boolean negBody, 
	      boolean negHead, boolean classRule, boolean horn) {

    m_body = new Body();
    m_head = new Head();
    m_repeatPredicate = repeatPredicate;
    m_maxLiterals = maxLiterals;
    m_negBody = negBody && !horn;
    m_negHead = negHead && !horn;
    m_classRule = classRule;
    m_singleHead = classRule || horn;
  }
    
  /**
   * Constructor for a rule when the counter-instances are stored,
   * giving all the constraints applied to this rule.
   * The counter-instances are initialized to all the instances in the dataset.
   * 
   * @param instances The dataset.
   * @param repeatPredicate True if predicates can be repeated.
   * @param maxLiterals Maximum number of literals.
   * @param negBody True if negation is allowed in the body.
   * @param negHead True if negation is allowed in the head. 
   * @param classRule True if the rule is a classification rule.
   * @param horn True if the rule is a horn clause.
   */
  public Rule(Instances instances,
	      boolean repeatPredicate, int maxLiterals, boolean negBody, 
	      boolean negHead, boolean classRule, boolean horn) {

    m_body = new Body(instances);
    m_head = new Head(instances);
    m_repeatPredicate = repeatPredicate;
    m_maxLiterals = maxLiterals;
    m_negBody = negBody && !horn;
    m_negHead = negHead && !horn;
    m_classRule = classRule;
    m_singleHead = classRule || horn;
    m_numInstances = instances.numInstances();
    m_counterInstances = new ArrayList(m_numInstances);
    Enumeration enu = instances.enumerateInstances();
    while (enu.hasMoreElements()) {
      m_counterInstances.add(enu.nextElement());
    }
  }

  /**
   * Returns a shallow copy of this rule.
   * The structured is copied but the literals themselves are not copied.
   *
   * @return A copy of this Rule.
   */
  public Object clone() {

    Object result = null;
    try {
      result = super.clone();
      /* Clone the body and the head. */
      ((Rule) result).m_body = (Body) m_body.clone();
      ((Rule) result).m_head = (Head) m_head.clone();
      /* Clone the set of counter-instances. */
      if (m_counterInstances != null) {
	((Rule) result).m_counterInstances
	  = (ArrayList) m_counterInstances.clone();
      }
    } catch (Exception e) {
      /* An exception is not supposed to happen here. */
      e.printStackTrace();
      System.exit(0);
    }
    return result;
  }

  /**
   * Test if an instance is a counter-instance of this rule.
   * 
   * @param instance The instance to test.
   * @return True if the instance is a counter-instance.
   */
  public boolean counterInstance(Instance instance) {

    return ((m_body.counterInstance(instance) 
	     && m_head.counterInstance(instance)));
  }

  /**
   * Update the number of counter-instances of this rule in the dataset.
   * This method should be used is the rule does not store its counter-instances.
   *
   * @param instances The dataset.
   */
  public void upDate(Instances instances) {

    Enumeration enu = instances.enumerateInstances();
    m_numInstances = instances.numInstances();
    m_counter = 0;
    while (enu.hasMoreElements()) {
      if (this.counterInstance((Instance) enu.nextElement())) {
	m_counter++;
      }
    }
    m_head.upDate(instances);
    m_body.upDate(instances);
  }

  /**
   * Get the confirmation value of this rule.
   *
   * @return The confirmation.
   */
  public double getConfirmation() {

    return m_confirmation;
  }

  /**
   * Get the optimistic estimate of the confirmation obtained by refining
   * this rule.
   * 
   * @return The optimistic estimate.
   */
  public double getOptimistic() {

    return m_optimistic;
  }

  /*
   * Get the expected number of counter-instances of this rule,
   * calculated from the number of instances satisfying the body and
   * the number of instances satisfying the negation of the head.
   *
   * @return The expected number of counter-instances.
   */
  public double getExpectedNumber() {

    return (double) m_body.getCounterInstancesNumber() 
      * (double) m_head.getCounterInstancesNumber() 
      / (double) m_numInstances;
  }

  /**
   * Get the expected frequency of counter-instances of this rule.
   *
   * @return The expected frequency of counter-instances.
   */
  public double getExpectedFrequency() {

    return getExpectedNumber() / (double) m_numInstances;
  }

  /**
   * Get the observed number of counter-instances of this rule in the dataset.
   *
   * @return The observed number of counter-instances.
   */
  public int getObservedNumber() {

    if (m_counterInstances != null) {
      return m_counterInstances.size();
    } else {
      return m_counter;
    }
  }

  /**
   * Get the observed frequency of counter-instances of this rule in the dataset.
   * 
   * @return The expected frequency of counter-instances.
   */
  public double getObservedFrequency() {

    return (double) getObservedNumber() / (double) m_numInstances;
  }

  /**
   * Get the rate of True Positive instances of this rule.
   *
   * @return The TP-rate.
   */
  public double getTPRate() {

    int tp = m_body.getCounterInstancesNumber() - getObservedNumber();
    int fn = m_numInstances - m_head.getCounterInstancesNumber() - tp;
    return ((double) tp / (double) (tp + fn));
  }

  /**
   * Get the rate of False Positive instances of this rule.
   *
   * @return The FP-rate.
   */
  public double getFPRate() {

    int fp = getObservedNumber();
    int tn = m_head.getCounterInstancesNumber() - fp;
    return ((double) fp / (double) (fp + tn));
  }

  /**
   * Calculate the confirmation of this rule.
   */
  public void calculateConfirmation() {

    double expected = getExpectedFrequency();
    double observed = getObservedFrequency();
    if ((expected == 0) || (expected == 1)) {
      m_confirmation = 0;
    } else {
      m_confirmation = (expected - observed) / (Math.sqrt(expected) - expected);
    }
  }

  /**
   * Calculate the optimistic estimate of this rule.
   */
  public void calculateOptimistic() {

    int counterInstances = this.getObservedNumber();
    int body = m_body.getCounterInstancesNumber();
    int notHead = m_head.getCounterInstancesNumber();
    int n = m_numInstances;
    double expectedOptimistic;
    /* optimistic expected number of counter-instances */
    if (counterInstances <= body - notHead) {
      expectedOptimistic = (double) (notHead * (body - counterInstances)) 
	/ (double) (n * n);
    } else if (counterInstances <= notHead - body) {
      expectedOptimistic = (double) (body * (notHead - counterInstances)) 
	/ (double) (n * n);
    } else {
      expectedOptimistic = (double) ((notHead + body - counterInstances)
				     * (notHead + body - counterInstances)) 
	/ (double) (4 * n * n);
    }
    if ((expectedOptimistic == 0) || (expectedOptimistic == 1)) {
      m_optimistic = 0;
    } else {
      m_optimistic = expectedOptimistic 
	/ (Math.sqrt(expectedOptimistic) - expectedOptimistic);
    }
  }

  /**
   * Test if this rule is empty.
   * 
   * @return True if it is the empty rule.
   */
  public boolean isEmpty() {

    return (m_head.isEmpty() && m_body.isEmpty());
  }

  /**
   * Give the number of literals in this rule.
   * 
   * @return The number of literals.
   */
  public int numLiterals() {

    return m_body.numLiterals() + m_head.numLiterals();
  }

  /**
   * Add a literal to the body of the rule.
   *
   * @param newLit The literal to add.
   * @return The rule obtained by adding the literal, null if the literal can
   * not be added because of the constraints on the rule.
   */
  private Rule addTermToBody(Literal newLit) {

    if (!m_negBody && newLit.negative()
     	|| (m_classRule && newLit.getPredicate().isClass())
	|| (newLit instanceof IndividualLiteral
	    && (((IndividualLiteral) newLit).getType() 
		- m_body.getType()) > 1
	    && (((IndividualLiteral) newLit).getType() 
		- m_head.getType()) > 1)) {
      return null;
    } else {
      Rule result = (Rule) this.clone();
      result.m_body.addElement(newLit);
      /* Update the counter-instances. */
      if (m_counterInstances != null) {
	for (int i = result.m_counterInstances.size() - 1; i >= 0; i--) {
	  Instance current = (Instance) result.m_counterInstances.get(i);
	  if (!result.m_body.canKeep(current, newLit)) {
	    result.m_counterInstances.remove(i);
	  }
	}
      }
      return result;
    }
  }

  /**
   * Add a literal to the head of the rule.
   *
   * @param newLit The literal to add.
   * @return The rule obtained by adding the literal, null if the literal can
   * not be added because of the constraints on the rule.
   */
  private Rule addTermToHead(Literal newLit) {
    if ((!m_negHead && newLit.negative())
     	|| (m_classRule && !newLit.getPredicate().isClass()) 
     	|| (m_singleHead && !m_head.isEmpty())
	|| (newLit instanceof IndividualLiteral
	    && ((IndividualLiteral) newLit).getType() 
	    != IndividualLiteral.INDIVIDUAL_PROPERTY)) {
      return null;
    } else { 
      Rule result = (Rule) this.clone();
      result.m_head.addElement(newLit);
      /* Update counter-instances. */
      if (m_counterInstances != null) {
	for (int i = result.m_counterInstances.size() - 1; i >= 0; i--) {
	  Instance current = (Instance) result.m_counterInstances.get(i);
	  if (!result.m_head.canKeep(current, newLit)) {
	    result.m_counterInstances.remove(i);
	  }
	}
      }
      return result;
    }
  }

  /**
   * Refine a rule by adding a range of literals of a predicate, either to
   * the head or to the body of the rule.
   * 
   * @param pred The predicate to consider.
   * @param firstIndex The index of the first literal of the predicate to add.
   * @param lastIndex The index of the last literal of the predicate to add.
   * @param addTobody True if the literals should be added to the body.
   * @param addToHead True if the literals should be added to the head.
   * @return A list of rules obtained by refinement.
   */
  private SimpleLinkedList refine(Predicate pred, int firstIndex, int lastIndex, 
				 boolean addToBody, boolean addToHead) {

    SimpleLinkedList result = new SimpleLinkedList();
    Literal currentLit;
    Literal negation;
    Rule refinement;
    for (int i = firstIndex; i < lastIndex; i++) {
      currentLit = pred.getLiteral(i);
      if (addToBody) {
	refinement = addTermToBody(currentLit);
	if (refinement != null) {
	  result.add(refinement);
	}
      }
      if (addToHead) {
	refinement = addTermToHead(currentLit);
	if (refinement != null) {
	  result.add(refinement);
	}
      }
      negation = currentLit.getNegation();
      if (negation != null) {
	if (addToBody) {
	  refinement = addTermToBody(negation);
	  if (refinement != null) {
	    result.add(refinement);
	  }
	}
	if (addToHead) {
	  refinement = addTermToHead(negation);
	  if (refinement != null) {
	    result.add(refinement);
	  }
	}
      }
    }
    return result;
  }

  /**
   * Refine a rule by adding literal from a set of predictes.
   * 
   * @param predicates The predicates available.
   * @return The list of the children obtained by refining the rule.
   */
  public SimpleLinkedList refine(ArrayList predicates) {

    SimpleLinkedList result = new SimpleLinkedList();
    Predicate currentPred;
    boolean addToBody;
    boolean addToHead;

    if (this.numLiterals() == m_maxLiterals) {
      return result;
    }

    if (this.isEmpty()) {
      /* Literals can be added on both sides of the rule. */
      for (int i = 0; i < predicates.size(); i++) {
	currentPred = (Predicate) predicates.get(i);
	result.addAll(refine(currentPred,
			     0, currentPred.numLiterals(),
			     true, true));
      }
    } else if (m_body.isEmpty() || m_head.isEmpty()) {
      /* Literals can be added to the empty side only. */
      LiteralSet side;
      Literal last;
      if (m_body.isEmpty()) {
	side = m_head;
	addToBody = true;
	addToHead = false;
      } else { // m_head.isEmpty()
	side = m_body;
	addToBody = false;
	addToHead = true;
      }
      last = side.getLastLiteral();
      currentPred = last.getPredicate();
      if (m_repeatPredicate) {
	result.addAll(refine(currentPred,
			     currentPred.indexOf(last) + 1, 
			     currentPred.numLiterals(),
			     addToBody, addToHead));
      }
      for (int i = predicates.indexOf(currentPred) + 1; i < predicates.size(); 
	   i++) {
	currentPred = (Predicate) predicates.get(i);
	result.addAll(refine(currentPred,
			     0, currentPred.numLiterals(),
			     addToBody, addToHead));		
      }
    } else {
      Literal lastLitBody = m_body.getLastLiteral();
      Literal lastLitHead = m_head.getLastLiteral();
      Predicate lastPredBody = lastLitBody.getPredicate();
      Predicate lastPredHead = lastLitHead.getPredicate();
      int lastLitBodyIndex = lastPredBody.indexOf(lastLitBody);
      int lastLitHeadIndex = lastPredHead.indexOf(lastLitHead);
      int lastPredBodyIndex = predicates.indexOf(lastPredBody);
      int lastPredHeadIndex = predicates.indexOf(lastPredHead);
      Predicate inferiorPred;
      Predicate superiorPred;
      int inferiorLit;
      int superiorLit;
      addToBody = (m_head.numLiterals() == 1
		   && (lastPredBodyIndex < lastPredHeadIndex 
		       || (lastPredBodyIndex == lastPredHeadIndex 
			   && lastLitBodyIndex < lastLitHeadIndex)));
      addToHead = (m_body.numLiterals() == 1
		   && (lastPredHeadIndex < lastPredBodyIndex
		       || (lastPredHeadIndex == lastPredBodyIndex
			   && lastLitHeadIndex < lastLitBodyIndex)));      
      if (addToBody || addToHead) {
	/* Add literals in the gap between the body and the head. */
	if (addToBody) {
	  inferiorPred = lastPredBody;
	  inferiorLit = lastLitBodyIndex;
	  superiorPred = lastPredHead;
	  superiorLit = lastLitHeadIndex;
	} else { // addToHead
	  inferiorPred = lastPredHead;
	  inferiorLit = lastLitHeadIndex;
	  superiorPred = lastPredBody;
	  superiorLit = lastLitBodyIndex;
	}
	if (predicates.indexOf(inferiorPred) 
	    < predicates.indexOf(superiorPred)) {
	  if (m_repeatPredicate) {
	    result.addAll(refine(inferiorPred, 
				 inferiorLit + 1, inferiorPred.numLiterals(),
				 addToBody, addToHead));
	  }
	  for (int j = predicates.indexOf(inferiorPred) + 1; 
	       j < predicates.indexOf(superiorPred); j++) {
	    currentPred = (Predicate) predicates.get(j);
	    result.addAll(refine(currentPred,
				 0, currentPred.numLiterals(),
				 addToBody, addToHead));
	  }
	  if (m_repeatPredicate) {
	    result.addAll(refine(superiorPred,
				 0, superiorLit,
				 addToBody, addToHead));
	  }
	} else { 
	  //((inferiorPred.getIndex() == superiorPred.getIndex())
	  //&& (inferiorLit < superiorLit))
	  if (m_repeatPredicate) {
	    result.addAll(refine(inferiorPred,
				 inferiorLit + 1, superiorLit,
				 addToBody, addToHead));
	  }
	}	
      }	
      /* Add other literals. */
      if (predicates.indexOf(lastPredBody) > predicates.indexOf(lastPredHead)) {
	superiorPred = lastPredBody;
	superiorLit = lastPredBody.indexOf(lastLitBody);
      } else if (predicates.indexOf(lastPredBody) 
		 < predicates.indexOf(lastPredHead)) {
	superiorPred = lastPredHead;
	superiorLit = lastPredHead.indexOf(lastLitHead);
      } else {
	superiorPred = lastPredBody;
	if (lastLitBodyIndex > lastLitHeadIndex) {
	  superiorLit = lastPredBody.indexOf(lastLitBody);
	} else {
	  superiorLit = lastPredHead.indexOf(lastLitHead);
	}
      }
      if (m_repeatPredicate) {
	result.addAll(refine(superiorPred,
			     superiorLit + 1, superiorPred.numLiterals(),
			     true, true));
      }
      for (int j = predicates.indexOf(superiorPred) + 1; j < predicates.size(); 
	   j++) {
	currentPred = (Predicate) predicates.get(j);
	result.addAll(refine(currentPred,
			     0, currentPred.numLiterals(),
			     true, true));
      }
    }
    return result;
  }

  /**
   * Test if this rule subsumes another rule.
   *
   * @param otherRule The other rule.
   * @return True if the other rule is subsumed.
   */
  public boolean subsumes(Rule otherRule) {    

    if (this.numLiterals() > otherRule.numLiterals()) {
      return false;
    }
    return (m_body.isIncludedIn(otherRule) && m_head.isIncludedIn(otherRule));
  }

  /**
   * Test if this rule and another rule correspond to the same clause.
   *
   * @param otherRule The other rule.
   * @return True if both rules correspond to the same clause.
   */
  public boolean sameClauseAs(Rule otherRule) {

    return (this.numLiterals() == otherRule.numLiterals()
	    && this.subsumes(otherRule));
  }

  /**
   * Test if this rule is equivalent to another rule.
   *
   * @param otherRule The other rule.
   * @return True if both rules are equivalent.
   */
  public boolean equivalentTo(Rule otherRule) {

    return (this.numLiterals() == otherRule.numLiterals()
	    && m_head.negationIncludedIn(otherRule.m_body)
	    && m_body.negationIncludedIn(otherRule.m_head));
  }

  /**
   * Test if the body of the rule contains a literal.
   * 
   * @param lit The literal to look for.
   * @return True if the literal is contained in the body of the rule.
   */
  public boolean bodyContains(Literal lit) {

    return m_body.contains(lit);
  }

  /**
   * Test if the head of the rule contains a literal.
   * 
   * @param lit The literal to look for.
   * @return True if the literal is contained in the head of the rule.
   */
  public boolean headContains(Literal lit) {

    return m_head.contains(lit);
  }

  /**
   * Test if this rule is over the frequency threshold.
   *
   * @param minFrequency The frequency threshold.
   * @return True if the rule is over the threshold.
   */
  public boolean overFrequencyThreshold(double minFrequency) {

    return (m_body.overFrequencyThreshold(minFrequency) 
	    && m_head.overFrequencyThreshold(minFrequency));
  }

  /**
   * Test if the body of the rule is true.
   *
   * @return True if the body is always satisfied.
   */
  public boolean hasTrueBody() {

    return (!m_body.isEmpty()
	    && m_body.hasMaxCounterInstances());
  }

  /**
   * Test if the head of the rule is false.
   *
   * @return True if the body is never satisfied.
   */
  public boolean hasFalseHead() {

    return (!m_head.isEmpty()
	    && m_head.hasMaxCounterInstances());
  }

  /**
   * Return a String giving the confirmation and optimistic estimate of 
   * this rule.
   * 
   * @return A String with the values of the rule.
   */
  public String valuesToString() {

    StringBuffer text = new StringBuffer();
    DecimalFormat decimalFormat = new DecimalFormat("0.000000");
    text.append(decimalFormat.format(getConfirmation()));
    text.append(" ");
    text.append(decimalFormat.format(getObservedFrequency()));
    return text.toString();
  }

  /**
   * Return a String giving the TP-rate and FP-rate of 
   * this rule.
   * 
   * @return A String with the values of the rule.
   */
  public String rocToString() {

    StringBuffer text = new StringBuffer();
    DecimalFormat decimalFormat = new DecimalFormat("0.000000");
    text.append(decimalFormat.format(getConfirmation()));
    text.append(" ");
    text.append(decimalFormat.format(getTPRate()));
    text.append(" ");
    text.append(decimalFormat.format(getFPRate()));
    return text.toString();
  }

  /**
   * Retrun a String for this rule.
   *
   * @return The String describing this rule.
   */
  public String toString() {

    StringBuffer text = new StringBuffer();
    text.append(m_body.toString());
    text.append(" ==> ");
    text.append(m_head.toString());
    return text.toString();
  }

  /**
   * Comparator used to compare two rules according to their confirmation value.
   */
  public static Comparator confirmationComparator = new Comparator() {

      public int compare(Object o1, Object o2) {

	Rule r1 = (Rule) o1;
	Rule r2 = (Rule) o2;
	double conf1 = r1.getConfirmation();
	double conf2 = r2.getConfirmation();
	if (conf1 > conf2) {
	  return -1;
	} else if (conf1 < conf2) {
	  return 1;
	} else {
	  return 0;
	}
      }
    };

  /**
   * Comparator used to compare two rules according to their observed number
   * of counter-instances.
   */
  public static Comparator observedComparator = new Comparator() {

      public int compare(Object o1, Object o2) {

	Rule r1 = (Rule) o1;
	Rule r2 = (Rule) o2;
	double obs1 = r1.getObservedFrequency();
	double obs2 = r2.getObservedFrequency();
	if (obs1 < obs2) {
	  return -1;
	} else if (obs1 > obs2) {
	  return 1;
	} else {
	  return 0;
	}
      }
    };

  /**
   * Comparator used to compare two rules according to their optimistic estimate.
   */
  public static Comparator optimisticComparator = new Comparator() {

      public int compare(Object o1, Object o2) {

	Rule r1 = (Rule) o1;
	Rule r2 = (Rule) o2;
	double opt1 = r1.getOptimistic();
	double opt2 = r2.getOptimistic();
	if (opt1 > opt2) {
	  return -1;
	} else if (opt1 < opt2) {
	  return 1;
	} else {
	  return 0;
	}
      }
    };

  /**
   * Comparator used to compare two rules according to their confirmation and 
   * then their observed number of counter-instances.
   */
  public static Comparator confirmationThenObservedComparator 
    = new Comparator() {
	public int compare(Object o1, Object o2) {

	  int confirmationComparison = confirmationComparator.compare(o1, o2);
	  if (confirmationComparison != 0) {
	    return confirmationComparison;
	  } else {
	    return observedComparator.compare(o1, o2);
	  }
	}
      };
  
  /**
   * Comparator used to compare two rules according to their optimistic estimate
   * and then their observed number of counter-instances.
   */
  public static Comparator optimisticThenObservedComparator = new Comparator() {
      public int compare(Object o1, Object o2) {
	int optimisticComparison = optimisticComparator.compare(o1, o2);
	if (optimisticComparison != 0) {
	  return optimisticComparison;
	} else {
	  return observedComparator.compare(o1, o2);
	}
      }
    };
}


