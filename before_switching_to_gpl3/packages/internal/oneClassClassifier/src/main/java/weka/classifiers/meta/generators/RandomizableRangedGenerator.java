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
 * RandomizableRangedGenerator.java
 * Copyright (C) 2008 University of Waikato, Hamilton, New Zealand
 */

package weka.classifiers.meta.generators;

import weka.core.Option;
import weka.core.Utils;

import java.util.Enumeration;
import java.util.Vector;

/**
 * Abstract superclass for generators that take ranges and use a seeded random
 * number generator internally
 * 
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public abstract class RandomizableRangedGenerator
  extends RandomizableGenerator
  implements Ranged {

  /** for serialization. */
  private static final long serialVersionUID = -5766761200929361752L;

  /** The lower range of this generator. */
  protected double m_LowerRange = 0.0;

  /** The upper range of this generator. */
  protected double m_UpperRange = 1.0;

  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {
    Vector result = new Vector();

    Enumeration enu = super.listOptions();
    while (enu.hasMoreElements())
      result.addElement(enu.nextElement());

    result.addElement(new Option(
	"\tSets the lower range of the generator\n"
	+ "\t(default: 0)",
	"L", 1, "-L <num>"));

    result.addElement(new Option(
	"\tSets the upper range of the generator\n"
	+ "\t(default: 1)",
	"U", 1, "-U <num>"));
    
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

    tmpStr = Utils.getOption("L", options);
    if (tmpStr.length() != 0)
      setLowerRange(Double.parseDouble(tmpStr));
    else
      setLowerRange(0.0);

    tmpStr = Utils.getOption("U", options);
    if (tmpStr.length() != 0)
      setUpperRange(Double.parseDouble(tmpStr));
    else
      setUpperRange(1.0);
  }

  /**
   * Gets the current settings of the Classifier.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {
    Vector<String>	result;
    String[]		options;
    int			i;

    result = new Vector<String>();

    options = super.getOptions();
    for (i = 0; i < options.length; i++)
      result.add(options[i]);
    
    result.add("-L");
    result.add("" + m_LowerRange);
    
    result.add("-U");
    result.add("" + m_UpperRange);

    return result.toArray(new String[result.size()]);
  }

  /**
   * Gets the lower range of the generator.
   *
   * @return 		The lower range of this generator.
   */
  public double getLowerRange() {
    return m_LowerRange;
  }

  /**
   * Sets the lower range.
   *
   * @param value 	The lower range of the generator.
   */
  public void setLowerRange(double value) {
    m_LowerRange = value;
  }
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String lowerRangeTipText() {
    return "The lower range.";
  }

  /**
   * Gets the upper range of the generator.
   *
   * @return 		The upper range of this generator.
   */
  public double getUpperRange() {
    return m_UpperRange;
  }

  /**
   * Sets the upper range.
   *
   * @param value 	The upper range of the generator.
   */
  public void setUpperRange(double value) {
    m_UpperRange = value;
  }    
  
  /**
   * Returns the tip text for this property.
   * 
   * @return 		tip text for this property suitable for
   * 			displaying in the explorer/experimenter gui
   */
  public String upperRangeTipText() {
    return "The upper range.";
  }
}
