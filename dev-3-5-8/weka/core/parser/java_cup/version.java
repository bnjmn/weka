/*
 * STANDARD ML OF NEW JERSEY COPYRIGHT NOTICE, LICENSE AND DISCLAIMER.
 * 
 * Copyright (c) 1989-1998 by Lucent Technologies
 * 
 * Permission to use, copy, modify, and distribute this software and its
 * documentation for any purpose and without fee is hereby granted, provided
 * that the above copyright notice appear in all copies and that both the
 * copyright notice and this permission notice and warranty disclaimer appear
 * in supporting documentation, and that the name of Lucent Technologies, Bell
 * Labs or any Lucent entity not be used in advertising or publicity pertaining
 * to distribution of the software without specific, written prior permission.
 *
 * Lucent disclaims all warranties with regard to this software, including all
 * implied warranties of merchantability and fitness. In no event shall Lucent
 * be liable for any special, indirect or consequential damages or any damages
 * whatsoever resulting from loss of use, data or profits, whether in an action
 * of contract, negligence or other tortious action, arising out of or in
 * connection with the use or performance of this software. 
 *
 * Taken from this URL:
 * http://www.smlnj.org/license.html
 * 
 * This license is compatible with the GNU GPL (see section "Standard ML of New
 * Jersey Copyright License"):
 * http://www.gnu.org/licenses/license-list.html#StandardMLofNJ
 */

/*
 * Copyright 1996-1999 by Scott Hudson, Frank Flannery, C. Scott Ananian
 */

package weka.core.parser.java_cup;

/** This class contains version and authorship information. 
 *  It contains only static data elements and basically just a central 
 *  place to put this kind of information so it can be updated easily
 *  for each release.  
 *
 *  Version numbers used here are broken into 3 parts: major, minor, and 
 *  update, and are written as v<major>.<minor>.<update> (e.g. v0.10a).  
 *  Major numbers will change at the time of major reworking of some 
 *  part of the system.  Minor numbers for each public release or 
 *  change big enough to cause incompatibilities.  Finally update
 *  letter will be incremented for small bug fixes and changes that
 *  probably wouldn't be noticed by a user.  
 *
 * @version last updated: 12/22/97 [CSA]
 * @author  Frank Flannery
 */

public class version {
  /** The major version number. */
  public static final int major = 0;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The minor version number. */
  public static final int minor = 11;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The update letter. */
  public static final String update = "a beta 20060608";

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** String for the current version. */
  public static final String version_str = "v" + major + "." + minor + update;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Full title of the system */
  public static final String title_str = "CUP " + version_str;

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** Name of the author */
  public static final String author_str =
      "Scott E. Hudson, Frank Flannery, Andrea Flexeder, Michael Petter and C. Scott Ananian";

  /*. . . . . . . . . . . . . . . . . . . . . . . . . . . . . .*/

  /** The command name normally used to invoke this program */ 
  public static final String program_name = "java_cup";
}
