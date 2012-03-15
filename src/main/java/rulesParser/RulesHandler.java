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
 *    RulesHandler.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package rulesParser;

import org.xml.sax.helpers.DefaultHandler;
import java.util.*;
import rules.*;
import org.xml.sax.*;

/**
 * @author beleg
 *
 * The Handler that parse the XML file of rules 
 */
public class RulesHandler extends DefaultHandler {

	private LinkedList theRules;
	private LinkedList theCriteria;

	private Attribute conclusion;
	private LinkedList condition;
	private LinkedList criteres;
	private LinkedList cardinalities;
	private int id;
	private Attribute a;
	boolean isACondition = false;
	boolean isAConclusion = false;
	private float confidence;
	private float frequency;
	private LinkedList cardAttributes;
	private String nameAttr;
	private String valueAttr;

	public RulesHandler() {

		theRules = new LinkedList();
		theCriteria = new LinkedList();
		id = 0;

	}

	public Rule[] getRules() {
		int s = theRules.size();
		Rule[] res = new Rule[s];
		Iterator it = theRules.listIterator();
		for (int i = 0; i < s; i++) {
			res[i] = (Rule) it.next();
		}
		return res;
	}

	public String[] getCriteres() {
		int s = theCriteria.size();
		String[] res = new String[s];
		Iterator it = theCriteria.listIterator();
		for (int i = 0; i < s; i++) {
			res[i] = (String) it.next();
		}
		return res;
	}

	public void startElement(
		String uri,
		String localName,
		String qName,
		Attributes attr) {
		String element = qName.toUpperCase();

		if (element.equals("RULE")) {
			id++;
			condition = new LinkedList();
			criteres = new LinkedList();
			cardinalities = new LinkedList();
		}

		if (element.equals("LHS"))
			isACondition = true;

		if (element.equals("RHS"))
			isAConclusion = true;

		if (element.equals("ITEM")) {
			String nameAttr = attr.getValue(0);
			String valueAttr = attr.getValue(1);
			a = new Attribute(nameAttr, valueAttr);
			if (isACondition)
				condition.add(a);
			if (isAConclusion)
				conclusion = a;
		}

		if (element.equals("CRITERE")) {
			String attr1 = attr.getValue(0);
			String attr2 = attr.getValue(1);

			float critereValue = 0;
			try {
				critereValue = Float.parseFloat(attr2);
			} catch (NumberFormatException e) {
				// add the errors gestion here				
			}
			Critere c = new Critere(attr1, critereValue);
			criteres.add(c);

			if (!contains(attr1))
				theCriteria.add(attr1);

		}

		if (element.equals("CARD")) {
			cardAttributes = new LinkedList();
		}

		if (element.equals("ATTR")) {
			nameAttr = attr.getValue(0);
			a = new Attribute(nameAttr, null);
			cardAttributes.add(a);
		}

		if (element.equals("VALUE")) {
			valueAttr = attr.getValue(0);
			a = (Attribute) cardAttributes.getLast();
			Attribute newAttribute = new Attribute(a.getName(), valueAttr);
			cardAttributes.remove(cardAttributes.getLast());
			cardAttributes.add(newAttribute);
		}

		if (element.equals("DIMENSION")) {
			String attr1 = attr.getValue(0);
			String attr2 = attr.getValue(1);

			confidence = Float.parseFloat(attr1);
			frequency = Float.parseFloat(attr2);

			Cardinality c =
				new Cardinality(
					new LinkedList(cardAttributes),
					confidence,
					frequency);
			cardinalities.add(c);
		}

	}

	private boolean contains(String critere) {
		Iterator it = theCriteria.listIterator();
		while (it.hasNext()) {
			String tmp = (String) it.next();
			if (critere.equals(tmp))
				return true;
		}
		return false;
	}

	public void endElement(String uri, String localName, String qName)
		throws SAXException {

		String element = qName.toUpperCase();

		if (element.equals("RULE")) {
			String rid = "R" + id;
			if (cardinalities.size() == 0)
				cardinalities = null;
			Rule r =
				new Rule(rid, conclusion, condition, criteres, cardinalities);
			theRules.add(r);
		}

		if (element.equals("LHS"))
			isACondition = false;

		if (element.equals("RHS"))
			isAConclusion = false;

		if (element.equals("ATTR"))
			cardAttributes.remove(cardAttributes.getLast());
			
	}

}
