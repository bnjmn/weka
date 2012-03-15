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
 *    Literal.java
 *    Copyright (C) 2003 Peter A. Flach, Nicolas Lachiche
 *
 *    Thanks to Amelie Deltour for porting the original C code to Java
 *    and integrating it into Weka.
 *
 */

package weka.associations.tertius;

import weka.core.Instance;
import weka.core.RevisionHandler;

import java.io.Serializable;

/**
 * @author Peter A. Flach
 * @author Nicolas Lachiche
 * @version $Revision$
 */
public abstract class Literal
  implements Serializable, RevisionHandler {

  /** for serialization */
  private static final long serialVersionUID = 2675363669503575771L;
  
  private Predicate m_predicate;

  public static final int NEG = 0;

  public static final int POS = 1;

  private int m_sign;

  private Literal m_negation;

  protected int m_missing;

  public Literal(Predicate predicate, int sign, int missing) {

    m_predicate = predicate;
    m_sign = sign;
    m_negation = null;
    m_missing = missing;
  }

  public Predicate getPredicate() {

    return m_predicate;
  }

  public Literal getNegation() {

    return m_negation;
  }

  public void setNegation(Literal negation) {

    m_negation = negation;
  }

  public boolean positive() {

    return m_sign == POS;
  }

  public boolean negative() {

    return m_sign == NEG;
  }

  public abstract boolean satisfies(Instance instance);
  
  public abstract boolean negationSatisfies(Instance instance);
  
  public abstract String toString();
}






