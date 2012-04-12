/*
 *    Option.java
 *    Copyright (C) 1999 Eibe Frank
 *
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

package weka.core;

/** 
 * Class to store information about an option.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public class Option {

  // =================
  // Private variables
  // =================

  /**
   * What does this option do?
   */

  private String theDescription;

  /**
   * The synopsis.
   */

  private String theSynopsis;

  /**
   * What's the option's name?
   */

  private String theName;

  /**
   * How many arguments does it take?
   */

  private int theNumArguments;

  // ===============
  // Public methods.
  // ===============

  /**
   * Creates new option with the given parameters.
   * @String description the option's description
   * @String name the option's name
   * @String numArguments the number of arguments
   */

  public Option(String description, String name, 
		int numArguments, String synopsis) {
  
    theDescription = description;
    theName = name;
    theNumArguments = numArguments;
    theSynopsis = synopsis;
  }

  /**
   * Returns the option's description.
   * @return the option's description
   */

  public String description() {
  
    return theDescription;
  }

  /**
   * Returns the option's name.
   * @return the option's name
   */

  public String name() {

    return theName;
  }

  /**
   * Returns the option's number of arguments.
   * @return the option's number of arguments
   */

  public int numArguments() {
  
    return theNumArguments;
  }

  /**
   * Returns the option's synopsis.
   * @return the option's synopsis
   */

  public String synopsis() {
  
    return theSynopsis;
  }
}

