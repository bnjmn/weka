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
 *    RulesComparator.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */
package rules;

import java.util.LinkedList;

import sortedListPanel.ParameterComparator;

/**
 *
 * @author  beleg
 */
public class RulesComparator extends ParameterComparator {

	/** Creates a new instance of RulesComparator */
	public RulesComparator(String parameter) {
		super(parameter);
	}

	/** Compares its two arguments for order.  Returns a negative integer,
	 * zero, or a positive integer as the first argument is less than, equal
	 * to, or greater than the second.<p>
	 * Note: this comparator
	 * imposes orderings that are inconsistent with equals.
	 *
	 * @param o1 the first object to be compared.
	 * @param o2 the second object to be compared.
	 * @return a negative integer, zero, or a positive integer as the
	 * 	       first argument is less than, equal to, or greater than the
	 * 	       second.
	 * @throws ClassCastException if the arguments' types prevent them from
	 * 	       being compared by this Comparator.
	 *
	 */
	public int compare(Object o1, Object o2) {
		Rule r1 = (Rule) o1;
		Rule r2 = (Rule) o2;

		if (parameter.equals("nbAttributes")) {
			LinkedList cond1 = r1.getCondition();
			LinkedList cond2 = r2.getCondition();

			if (cond1.size() < cond2.size())
				return 1;
			if (cond1.size() == cond2.size())
				return 0;
			else
				return -1;
		} else if(parameter.equals("conclusion")) {
			
			Attribute a1 = r1.getConclusion();
			Attribute a2 = r2.getConclusion();
			
			return a1.toString().compareTo(a2.toString());
					
						
		}
		else {

			Critere c1 = r1.getCritere(parameter);
			Critere c2 = r2.getCritere(parameter);

			if (c1.getValue() < c2.getValue())
				return 1;
			if (c1.getValue() == c2.getValue())
				return 0;
			else
				return -1;
		}
	}
}
