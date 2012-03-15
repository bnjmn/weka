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
 *    ParameterFactory.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package sortedListPanel;

import java.lang.reflect.*;

public class ParameterComparatorFactory {

    private Class comparatorClass;

    public ParameterComparatorFactory(String comparatorClassName) throws BadClassException{
	
	Class theClass = null;
	try{
	    theClass = Class.forName(comparatorClassName);
	}catch(ClassNotFoundException cnfe) {
	    System.out.println("Class Not Found");
	    throw new BadClassException();
	}
	Class superClass = theClass.getSuperclass();
	while (superClass != null && !superClass.getName().equals("sortedListPanel.ParameterComparator"))
	    superClass = superClass.getSuperclass();
	if (superClass == null)
	    throw new BadClassException();
	comparatorClass = theClass;
	
    }

    public ParameterComparator createParameterComparator(String critere) {
	
	Class[] parameters = new Class[1];
	String aString = new String();
	Class stringClass = aString.getClass();
	parameters[0] = stringClass;
	Constructor constr = null;
	try {
	    constr = comparatorClass.getConstructor(parameters);
	}
	catch(NoSuchMethodException e) {}
	String[] effectiveParameters = new String[1];
	effectiveParameters[0] = critere;
	ParameterComparator theComp = null;
	try {
	     theComp = (ParameterComparator) constr.newInstance(effectiveParameters);
	}catch(InstantiationException e) {
	    System.out.println("The class must be non abstract");
	    System.exit(1);
	}
	catch(IllegalAccessException e) {
	    System.out.println("The class is non accessible");
	    System.exit(1);
	}
	catch(InvocationTargetException e) {
	    e.getTargetException().printStackTrace();
	    System.exit(1);
	}
	return theComp;

    }

}

