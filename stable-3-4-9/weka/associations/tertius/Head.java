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
 *    Head.java
 *    Copyright (C) 2003 Peter A. Flach, Nicolas Lachiche
 *
 *    Thanks to Amelie Deltour for porting the original C code to Java
 *    and integrating it into Weka.
 */

package weka.associations.tertius;

import weka.core.Instance;
import weka.core.Instances;
import java.util.Iterator;

/**
 * Class representing the head of a rule.
 *
 * @author  <a href="mailto:adeltour@netcourrier.com">Amelie Deltour</a>
 * @version $Revision: 1.3.2.1 $
 */

public class Head extends LiteralSet {
  
  /**
   * Constructor without storing the counter-instances.
   */
  public Head() {

    super();
  }

  /**
   * Constructor storing the counter-instances.
   *
   * @param instances The dataset.
   */
  public Head(Instances instances) {

    super(instances);
  }

  /**
   * Test if an instance can be kept as a counter-instance,
   * if a new literal is added to this head.
   *
   * @param instance The instance to test.
   * @param newLit The new literal.
   * @return True if the instance is still a counter-instance 
   * (if the negation of the new literal satisfies the instance).
   */
  public boolean canKeep(Instance instance, Literal newLit) {
    return newLit.negationSatisfies(instance);
  }

  /**
   * Test if this Head is included in a rule.
   * It is the literals of this Head are contained in the head of the other rule,
   * or if their negation is included in the body of the other rule.
   */  public boolean isIncludedIn(Rule otherRule) {
    Iterator iter = this.enumerateLiterals();
    while (iter.hasNext()) {
      Literal current = (Literal) iter.next();
      if (!otherRule.headContains(current)
	  && !otherRule.bodyContains(current.getNegation())) {
	return false;
      }
    }
    return true;
  }

  /**
   * Gives a String representation of this set of literals as a disjunction.
   */
  public String toString() {
    Iterator iter = this.enumerateLiterals();

    if (!iter.hasNext()) {
      return "FALSE";
    }

    StringBuffer text = new StringBuffer();
    while (iter.hasNext()) {
      text.append(iter.next().toString());
      if (iter.hasNext()) {
	text.append(" or ");
      }
    }
    return text.toString();
  }

}









