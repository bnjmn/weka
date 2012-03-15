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
 * ParameterComparator.java
 *
 * Created on 5 fevrier 2003, 18:46
 */

/*
 *    ParameterComparator.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */
package sortedListPanel;


/**
 *
 * @author  beleg
 */
public abstract class ParameterComparator implements java.util.Comparator {
    
    protected String parameter;
    
    /** Creates a new instance of ParameterComparator */
    protected ParameterComparator(String parameter) {
        this.parameter = parameter;
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
    public abstract int compare(Object o1, Object o2);

    public boolean equals(Object o) {
	ParameterComparator c = null;
	try {
	    c = (ParameterComparator) o;
	}
	catch(ClassCastException e) {
	    return false;
	}
	if (!this.parameter.equals(c.parameter))
	    return false;
	else
	    return true;

    }

    public int hashCode() {
	return this.parameter.hashCode();
    }
    
}
