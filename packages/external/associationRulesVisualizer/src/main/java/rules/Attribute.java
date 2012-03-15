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
 *    Attribute.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package rules;

/**
 *
 * @author  beleg
 */
public class Attribute {
    
    private String name;
    private String value;
    
    /** Creates a new instance of Attribute */
    public Attribute(String name, String value) {
    	this.name = name;
        this.value = value;
    }
    
     
    /** Getter for property valeur.
     * @return Value of property valeur.
     *
     */
    public java.lang.String getValue() {
        return value;
    }    
    
    public void setValue(String value) {
    	this.value = value;	
    }
    
    public String getName() {
    	return name;	
    }

    public String toString() {
        return name + value;
    }
    
    public boolean equals(Attribute a) {
    	return this.name.equals(a.name) && this.value.equals(a.value);	
    }
}
