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
 *    Critere.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */
package rules;

/**
 *
 * @author  beleg
 */
public class Critere {
    
    private String name;
    
    private float value;
    
    /** Creates a new instance of Critere */
    public Critere(String name, float value) {
           this.name = name;
           this.value = value;
    }
    
    /** Getter for property name.
     * @return Value of property name.
     *
     */
    public java.lang.String getName() {
        return name;
    }
    
    /** Getter for property value.
     * @return Value of property value.
     *
     */
    public float getValue() {
        return value;
    }    

    /** Setter for property value.
     * @param value New value of property value.
     *
     */
    public void setValue(float value) {
        this.value = value;
    }    
    
    public String toString() {
        return name + "=" + value;
    }
}
