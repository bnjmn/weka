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
 *   InstanceHandler.java
 *   Copyright (C) 2008 K.Hempstalk, University of Waikato, Hamilton, New Zealand.
 */

package weka.classifiers.meta.generators;

import weka.core.CapabilitiesHandler;
import weka.core.Instances;

/**
 * Whether the generator can handle instances directly
 * for setting the parameters.
 *
 * @author Kathryn Hempstalk (kah18 at cs.waikato.ac.nz)
 * @version $Revision$
 */
public interface InstanceHandler
  extends CapabilitiesHandler {

  /**
   * Builds the generator with a given set of instances.
   *
   * @param someinstances The instances that will be used to 
   * build up the probabilities for this generator.
   * @throws Exception if data cannot be handled
   */
  public void buildGenerator(Instances someinstances) throws Exception;
}
