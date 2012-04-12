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
 *    LiteralSet.java
 *    Copyright (C) 2003 Peter A. Flach, Nicolas Lachiche
 *
 *    Thanks to Amelie Deltour for porting the original C code to Java
 *    and integrating it into Weka.
 */

package weka.associations.tertius;

import weka.core.Instances;
import weka.core.Instance;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.ArrayList;

/**
 * Class representing a set of literals, being either the body or the head
 * of a rule.
 *
 * @author <a href="mailto:adeltour@netcourrier.com">Amelie Deltour</a>
 * @version $Revision: 1.4.2.1 $
 */

public abstract class LiteralSet implements Serializable, Cloneable {
  
  /** Literals contained in this set. */
  private ArrayList m_literals;

  /** Last literal added to this set. */
  private Literal m_lastLiteral;
  
  /** Number of instances in the data the set deals with. */
  private int m_numInstances;

  /** Set of counter-instances of this part of the rule. */
  private ArrayList m_counterInstances;
  /* For a body, counter-instances are the instances satisfying the body.
   * For a head, conter-instances are the instances satisfying the negation. */

  /** Counter for the number of counter-instances. */
  private int m_counter;

  /** 
   * Type of properties expressed in this set 
   * (individual or parts properties).
   */
  private int m_type;
  
  /**
   * Constructor for a set that does not store its counter-instances.
   */
  public LiteralSet() {

    m_literals = new ArrayList();
    m_lastLiteral = null;
    m_counterInstances = null;
    m_type = -1;
  }

  /**
   * Constructor initializing the set of counter-instances to all the instances.
   *
   * @param instances The dataset.
   */
  public LiteralSet(Instances instances) {

    this();
    m_numInstances = instances.numInstances();
    m_counterInstances = new ArrayList(m_numInstances);
    Enumeration enu = instances.enumerateInstances();
    while (enu.hasMoreElements()) {
      m_counterInstances.add(enu.nextElement());
    }
  }

  /**
   * Returns a shallow copy of this set.
   * The structured is copied but the literals themselves are not copied.
   *
   * @return A copy of this LiteralSet.
   */
  public Object clone() {

    Object result = null;
    try {
      result = super.clone();
      /* Clone the set of literals, but not the literals themselves. */
      ((LiteralSet) result).m_literals = (ArrayList) m_literals.clone();
      if (m_counterInstances != null) {
	/* Clone the set of instances, but not the instances themselves. */
	((LiteralSet) result).m_counterInstances 
	  = (ArrayList) m_counterInstances.clone();
      }
    } catch(Exception e) {
      /* An exception is not supposed to happen here. */
      e.printStackTrace();
      System.exit(0);
    }
    return result;
  }

  /**
   * Update the number of counter-instances of this set in the dataset.
   * This method should be used is the set does not store its counter-instances.
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
  }

  /**
   * Get the number of counter-instances of this LiteralSet.
   *
   * @return The number of counter-instances.
   */
  public int getCounterInstancesNumber() {

    if (m_counterInstances != null) {
      return m_counterInstances.size();
    } else {
      return m_counter;
    }
  }

  /**
   * Get the frequency of counter-instances of this LiteralSet in the data.
   *
   * @return The frequency of counter-instances.
   */
  public double getCounterInstancesFrequency() {

    return (double) getCounterInstancesNumber() / (double) m_numInstances;
  }

  /**
   * Test if this LiteralSet has more counter-instances than the threshold.
   *
   * @param minFrequency The frequency threshold.
   * @return True if there are more counter-instances than the threshold.
   */
  public boolean overFrequencyThreshold(double minFrequency) {

    return getCounterInstancesFrequency() >= minFrequency;
  }

  /**
   * Test if all the intances are counter-instances.
   *
   * @return True if all the instances are counter-instances.
   */
  public boolean hasMaxCounterInstances() {

    return getCounterInstancesNumber() == m_numInstances;
  }

  /**
   * Add a Literal to this set.
   *
   * @param element The element to add.
   */
  public void addElement(Literal element) {

    m_literals.add(element);
    /* Update the last literal. */
    m_lastLiteral = element;
    /* Update the type in the case of individual-based learning. */
    if (element instanceof IndividualLiteral) {
      int type = ((IndividualLiteral) element).getType();
      if (type > m_type) {
	m_type = type;
      }
    }
    /* Update the set of counter-instances. */
    if (m_counterInstances != null) {
      for (int i = m_counterInstances.size() - 1; i >= 0; i--) {
	Instance current = (Instance) m_counterInstances.get(i);
	if (!canKeep(current, element)) {
	  m_counterInstances.remove(i);
	}
      }
    }
  }

  /**
   * Test if this set is empty.
   *
   * @return True if the set is empty.
   */
  public final boolean isEmpty() {

    return m_literals.size() == 0;
  }

  /**
   * Give the number of literals in this set.
   *
   * @return The number of literals.
   */
  public final int numLiterals() {

    return m_literals.size();
  }

  /**
   * Enumerate the literals contained in this set.
   *
   * @return An Iterator for the literals.
   */
  public final Iterator enumerateLiterals() {

    return m_literals.iterator();
  }

  /**
   * Give the last literal added to this set.
   *
   * @return The last literal added.
   */
  public Literal getLastLiteral() {

    return m_lastLiteral;
  }

  /**
   * Test if the negation of this LiteralSet is included in another LiteralSet.
   *
   * @param otherSet The other LiteralSet.
   * @return True if the negation of this LiteralSet is included in 
   * the other LiteralSet.
   */
  public boolean negationIncludedIn(LiteralSet otherSet) {

    Iterator iter = this.enumerateLiterals();
    while (iter.hasNext()) {
      Literal current = (Literal) iter.next();
      if (!otherSet.contains(current.getNegation())) {
	return false;
      }
    }      
    return true;
  }

  /**
   * Test if this LiteralSet contains a given Literal.
   *
   * @param lit The literal that is looked for.
   * @return True if this literal is contained in this LiteralSet.
   */
  public boolean contains(Literal lit) {

    return m_literals.contains(lit);
  }

  /**
   * Give the type of properties in this set (individual or part properties).
   */
  public int getType() {
	
    return m_type;
  }

  /**
   * Test if an individual instance, given a part instance of this individual,
   * is a counter-instance of this LiteralSet.
   *
   * @param individual The individual instance.
   * @param part The part instance.
   * @return True if the individual is a counter-instance.
   */
  public boolean counterInstance(Instance individual, Instance part) {
    Iterator iter = this.enumerateLiterals();
    while (iter.hasNext()) {
      IndividualLiteral current = (IndividualLiteral) iter.next();
      if (current.getType() == IndividualLiteral.INDIVIDUAL_PROPERTY
	  && !canKeep(individual, current))  {
	return false;
      } else if (current.getType() == IndividualLiteral.PART_PROPERTY
		 && !canKeep(part, current)) {
	return false;
      }
    }
    return true;
  }

  /**
   * Test if an instance is a counter-instance of this LiteralSet.
   *
   * @param instance The instance to test.
   * @return True if the instance is a counter-instance.
   */
  public boolean counterInstance(Instance instance) {
    if ((instance instanceof IndividualInstance)
	&& (m_type == IndividualLiteral.PART_PROPERTY)) {
      /* In the case of an individual instance, all the parts of the individual
       * have to be considered.
       * Part properties can be found in the body only, so here we test for
       * an instance satisfying the set.
       * It satisfies the set if there exists a part satisfying the set.
       */
      Enumeration enu 
	= ((IndividualInstance) instance).getParts().enumerateInstances();
      while (enu.hasMoreElements()) {
	if (counterInstance(instance, (Instance) enu.nextElement())) {
	  return true;
	}
      }
      return false;
    } else {
      /* Usual case. */
      Iterator iter = this.enumerateLiterals();
      while (iter.hasNext()) {
	Literal current = (Literal) iter.next();
	if (!canKeep(instance, current)) {
	  return false;
	}
      }
      return true;
    }
  }

  /**
   * Test if an instance can be kept as a counter-instance,
   * given a new literal.
   *
   * @param instance The instance to test.
   * @param newLit The new literal.
   * @return True if the instance is still a counter-instance.
   */
  public abstract boolean canKeep(Instance instance, Literal newLit);

  /**
   * Test if this LiteralSet is included in a rule.
   *
   * @param otherRule The rule to test.
   * @return True if this set of literals is included in the rule.
   */
  public abstract boolean isIncludedIn(Rule otherRule);
  
  /**
   * Gives a String representation for this set of literals.
   */
  public abstract String toString();  
}







