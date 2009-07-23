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

/**
 *   NominalAttributeGenerator.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

import weka.core.Instances;
import weka.core.Attribute;

/**
 * Used to indicate this generator can be used to generate 
 * artificial instances for nominal attributes.
 *
 *
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @version $Revision$
 */
public interface NominalAttributeGenerator{

   /**
   * Sets up the generator with the counts required for generation.
   *
   * @param someinstances The instances to count up.
   * @param att The attribute to count up with.
   */
    public void buildGenerator(Instances someinstances, Attribute att);

}
