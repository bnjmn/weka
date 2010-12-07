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
 * RemoveTest.java
 * Copyright (C) 2009 University of Waikato, Hamilton, New Zealand
 */

package wekaexamples.filters;

import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.io.BufferedReader;
import java.io.FileReader;

/**
 * Example class to demonstrate the use of the Remove filter.
 * <p/>
 * Takes an ARFF file as first argument, the number of indices to remove
 * as second and thirdly whether to invert or not (true/false).
 * Dumps the generated data to stdout.
 * 
 * @author FracPete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class RemoveTest {
  
  public static void main(String[] args) throws Exception {
    Instances       inst;
    Instances       instNew;
    Remove          remove;

    inst   = new Instances(new BufferedReader(new FileReader(args[0])));
    remove = new Remove();
    remove.setAttributeIndices(args[1]);
    remove.setInvertSelection(new Boolean(args[2]).booleanValue());
    remove.setInputFormat(inst);
    instNew = Filter.useFilter(inst, remove);
    System.out.println(instNew);
  }
}