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
 *    Predicate.java
 *    Copyright (C) 2003 Peter A. Flach, Nicolas Lachiche
 *
 *    Thanks to Amelie Deltour for porting the original C code to Java
 *    and integrating it into Weka.
 */

package weka.associations.tertius;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * @author Peter A. Flach
 * @author Nicolas Lachiche
 * @version $Revision: 1.2.2.1 $
 */
public class Predicate implements Serializable {

  private ArrayList m_literals;

  private String m_name;

  private int m_index;

  private boolean m_isClass;

  public Predicate(String name, int index, boolean isClass) {

    m_literals = new ArrayList();
    m_name = name;
    m_index = index;
    m_isClass = isClass;
  }

  public void addLiteral(Literal lit) {

    m_literals.add(lit);
  }

  public Literal getLiteral(int index) {

    return (Literal) m_literals.get(index);
  }

  public int getIndex() {

    return m_index;
  }

  public int indexOf(Literal lit) {

    int index = m_literals.indexOf(lit);
    return ((index != -1) 
	    ? index
	    : m_literals.indexOf(lit.getNegation()));	     
  }

  public int numLiterals() {

    return m_literals.size();
  }

  public boolean isClass() {

    return m_isClass;
  }

  public String toString() {

    return m_name;
  }

  public String description() {

    StringBuffer text = new StringBuffer();
    text.append(this.toString() + "\n");
    for (int i = 0; i < numLiterals(); i++) {
	Literal lit = getLiteral(i);
	Literal neg = lit.getNegation();
	text.append("\t" + lit + "\t" + neg + "\n");
    }
    return text.toString();
  }
}

