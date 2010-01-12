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
 * RandomizableGenerator.java
 * Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.meta.generators;

import weka.core.Option;
import weka.core.Utils;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/**
 * An abstract superclass for generators that use a seeded internal random 
 * number generator.
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public abstract class RandomizableGenerator
  extends Generator {

  /** for serialization. */
  private static final long serialVersionUID = -4182619078970023472L;

  /** The random number generator. */
  protected Random m_Random = new Random(1);

  /** The seed to the random number generator. */
  protected long m_Seed = 1;

  /**
   * Returns an enumeration describing the available options.
   *
   * @return 		an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements())
      result.addElement(enu.nextElement());

    result.addElement(new Option(
	"\tSets the seed of the random number generator of the generator"
	+ "\t(default: 1)",
	"S", 1, "-S <seed>"));

    return result.elements();
  }

  /**
   * Parses a given list of options.
   *
   * @param options 	the list of options as an array of strings
   * @throws Exception 	if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {
    String	tmpStr;
    
    super.setOptions(options);

    tmpStr = Utils.getOption("S", options);
    if (tmpStr.length() != 0)
      setSeed(Long.parseLong(tmpStr));
    else
      setSeed(1);
  }

  /**
   * Gets the current settings of the generator.
   *
   * @return 		An array of strings suitable for passing to setOptions.
   */
  public String[] getOptions() {
    Vector<String>	result;
    String[]		options;
    int			i;

    result = new Vector<String>();

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);
    
    result.add("-S");
    result.add("" + m_Seed);

    return result.toArray(new String[result.size()]);
  }

  /**
   * Sets the seed to the random number generator.
   *
   * @param value 	The new seed for the random number generator.
   */
  public void setSeed(long value) {
    m_Seed   = value;
    m_Random = new Random(m_Seed);
  }

  /**
   * Gets the current random number generator seed.
   *
   * @return 		The current random number generator seed.
   */
  public long getSeed() {
    return m_Seed;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String seedTipText() {
    return "The seed value for the random number generator.";
  }
}
