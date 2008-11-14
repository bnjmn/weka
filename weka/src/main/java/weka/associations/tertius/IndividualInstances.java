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
 *    IndividualInstances.java
 *    Copyright (C) 2003 Peter A. Flach, Nicolas Lachiche
 *
 *    Thanks to Amelie Deltour for porting the original C code to Java
 *    and integrating it into Weka.
 *
 */

package weka.associations.tertius;

import weka.core.Instances;
import weka.core.Instance;
import weka.core.Attribute;
import weka.core.Utils;
import java.util.Enumeration;
import java.io.IOException;

/**
 * @author Peter A. Flach
 * @author Nicolas Lachiche
 * @version $Revision: 1.3.2.1 $
 */
public class IndividualInstances extends Instances {
    
  public IndividualInstances(Instances individuals, Instances parts) 
    throws Exception {
	
    super(individuals, individuals.numInstances());

    Attribute individualIdentifier = attribute("id");
    if (individualIdentifier == null) {
      throw new Exception("No identifier found in individuals dataset.");
    }
    Attribute partIdentifier = parts.attribute("id");
    if (partIdentifier == null) {
      throw new Exception("No identifier found in parts dataset.");
    }

    Enumeration enumIndividuals = individuals.enumerateInstances();
    while (enumIndividuals.hasMoreElements()) {
      Instance individual = (Instance) enumIndividuals.nextElement();
      Instances partsOfIndividual = new Instances(parts, 0);
      Enumeration enumParts = parts.enumerateInstances();
      while (enumParts.hasMoreElements()) {
	Instance part = (Instance) enumParts.nextElement();
	if (individual.value(individualIdentifier)
	    == part.value(partIdentifier)) {
	  partsOfIndividual.add(part);
	}
      }
      add(new IndividualInstance(individual, partsOfIndividual));
    }	
  }

}



