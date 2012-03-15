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
 *    RulesFilter.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package rules;

import java.util.LinkedList;

/**
 * @author beleg
 *
 * To change this generated comment edit the template variable "typecomment":
 * Window>Preferences>Java>Templates.
 * To enable and disable the creation of type comments go to
 * Window>Preferences>Java>Code Generation.
 */
public class RulesFilter {
	
	private Rule[] rules;
	
	public RulesFilter(Rule[] rules) {
		this.rules = rules;	
	}
	
	public Rule[] getRules() {
		
		LinkedList newRules = new LinkedList();
		
		for (int i =0; i < rules.length; i++) {
			if (rules[i].getCardinalities() != null)
				newRules.add(rules[i]);
		}
		
		return (Rule[]) newRules.toArray(new Rule[newRules.size()]);	
	}

}
