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
 *    XMLRulesProducer.java
 *    Copyright (C) 2010 University of Waikato, Hamilton, New Zealand
 *
 */

package weka.associations;

/**
 * Interface to an association rule learner that can
 * represent its rules in XML. The DTD is:<br><br>
 * <pre>
 * <?xml version="1.0" encoding="iso-8859-15" ?>
 * <!ELEMENT RULES (RULE*)>
 * <!ELEMENT RULE (LHS, RHS, CRITERE*, CARD?)>
 * <!ELEMENT LHS (ITEM*)>
 * <!ELEMENT RHS (ITEM*)>
 * <!ELEMENT CARD (ATTR)>
 * <!ELEMENT ATTR (VALUE+)>
 * <!ELEMENT VALUE (ATTR*, DIMENSION?)>
 * <!ATTLIST CRITERE name CDATA #REQUIRED value CDATA #REQUIRED>
 * <!ATTLIST ITEM name CDATA #REQUIRED value CDATA #REQUIRED>
 * <!ATTLIST ATTR name CDATA #REQUIRED>
 * <!ATTLIST VALUE value CDATA #REQUIRED>
 * <!ATTLIST DIMENSION confidence CDATA #REQUIRED frequency CDATA #REQUIRED>
 * </pre>
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 *
 */
public interface XMLRulesProducer {
  
  /**
   * Returns a string that encodes association rules in XML format.
   * 
   * @return a string that encodes association rules in XML format.
   */
  String xmlRules();
}
