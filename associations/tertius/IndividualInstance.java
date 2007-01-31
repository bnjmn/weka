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
 *    IndividualInstance.java
 *    Copyright (C) 2003 Peter A. Flach, Nicolas Lachiche
 *
 *    Thanks to Amelie Deltour for porting the original C code to Java
 *    and integrating it into Weka.
 */

package weka.associations.tertius;

import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Peter A. Flach
 * @author Nicolas Lachiche
 * @version $Revision: 1.4 $
 */
public class IndividualInstance extends Instance {

  /** for serialization */
  private static final long serialVersionUID = -7903938733476585114L;
  
  private Instances m_parts;

  public IndividualInstance(Instance individual, Instances parts) {
	
    super(individual);
    m_parts = parts;
  }

  public IndividualInstance(IndividualInstance instance) {

    super(instance);
    m_parts = instance.m_parts;
  }

  public Object copy() {

    IndividualInstance result = new IndividualInstance(this);
    result.m_Dataset = m_Dataset;
    return result;
  }

  public Instances getParts() {
	
    return m_parts;
  }

}








