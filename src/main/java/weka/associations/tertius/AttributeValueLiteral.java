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
 *    AttributeValueLiteral.java
 *    Copyright (C) 2003 Peter A. Flach, Nicolas Lachiche
 *
 *    Thanks to Amelie Deltour for porting the original C code to Java
 *    and integrating it into Weka.
 */

package weka.associations.tertius;

import weka.core.Instance;
import weka.core.RevisionUtils;
import weka.associations.Tertius;

/**
 * @author Peter A. Flach
 * @author Nicolas Lachiche
 * @version $Revision$
 */
public class AttributeValueLiteral
  extends Literal {

  /** for serialization */
  private static final long serialVersionUID = 4077436297281456239L;

  private String m_value;
  
  private int m_index;
  
  public AttributeValueLiteral(Predicate predicate, String value, 
			       int index, int sign, int missing) {

    super(predicate, sign, missing);
    m_value = value;
    m_index = index;
  }

  public boolean satisfies(Instance instance) {

    if (m_index == -1) {
      if (positive()) {
	return instance.isMissing(getPredicate().getIndex());
      } else {
	return !instance.isMissing(getPredicate().getIndex());
      }
    } else if (instance.isMissing(getPredicate().getIndex())) {
      if (positive()) {
	return false;
      } else {
	return m_missing != Tertius.EXPLICIT;
      }
    } else {
      if (positive()) {
	return (instance.value(getPredicate().getIndex()) == m_index);
      } else {
	return (instance.value(getPredicate().getIndex()) != m_index);
      }
    }
  }

  public boolean negationSatisfies(Instance instance) {

    if (m_index == -1) {
      if (positive()) {
	return !instance.isMissing(getPredicate().getIndex());
      } else {
	return instance.isMissing(getPredicate().getIndex());
      }
    } else if (instance.isMissing(getPredicate().getIndex())) {
      if (positive()) {
	return m_missing != Tertius.EXPLICIT;
      } else {
	return false;
      }
    } else {
      if (positive()) {
	return (instance.value(getPredicate().getIndex()) != m_index);
      } else {
	return (instance.value(getPredicate().getIndex()) == m_index);
      }
    }
  }

  public String toString() {

    StringBuffer text = new StringBuffer();
    if (negative()) {
      text.append("not ");
    }
    text.append(getPredicate().toString() + " = " + m_value);
    return text.toString();
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
