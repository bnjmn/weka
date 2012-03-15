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
 *    Rule.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */
package rules;

import java.util.*;
/**
 *
 * @author  beleg
 */
public class Rule {

	private Attribute conclusion;
	private LinkedList condition;
	private LinkedList criteres;
	private LinkedList cardinalities;
	private String id;

	/** Creates a new instance of Rule */
	public Rule(
		String id,
		Attribute conclusion,
		LinkedList condition,
		LinkedList criteres,
		LinkedList cardinalites) {
		this.condition = condition;
		this.conclusion = conclusion;
		this.criteres = criteres;
		this.cardinalities = cardinalites;
		this.id = id;
	}

	/** Getter for property conclusion.
	 * @return Value of property conclusion.
	 *
	 */
	public Attribute getConclusion() {
		return conclusion;
	}

	/** Getter for property condition.
	 * @return Value of property condition.
	 *
	 */
	public java.util.LinkedList getCondition() {
		return condition;
	}

	/** Getter for property criteres.
	 * @return Value of property criteres.
	 *
	 */
	public java.util.LinkedList getCriteres() {
		return criteres;
	}

	/** Get a critere by name
	 */
	public Critere getCritere(String nameCritere) {
		Iterator it = criteres.listIterator();
		while (it.hasNext()) {
			Critere c = (Critere) it.next();
			if (nameCritere.equals(c.getName())) {
				return c;
			}
		}
		return null;
	}

	/** Getter for property cardinalites.
	 * @return Value of property cardinalites.
	 *
	 */
	public java.util.LinkedList getCardinalities() {
		return cardinalities;
	}

	/** Return the String representation of this object
	 *@return the String representation of this object
	 */
	public String toString() {
		String stringCondition = "";
		String stringConclusion = conclusion.toString();

		Iterator it = condition.listIterator();
		while (it.hasNext()) {
			stringCondition += it.next().toString();
			if (it.hasNext())
				stringCondition += " & ";
		}

		return id + ": " + stringCondition + " => " + stringConclusion;
	}

	/** Getter for property id.
	 * @return Value of property id.
	 *
	 */
	public java.lang.String getId() {
		return id;
	}

}
