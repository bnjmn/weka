
/*
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * Scoreable.java
 * Copyright (C) 2001 Remco Bouckaert
 * 
 */
package weka.classifiers.bayes;

/**
 * Interface for allowing to score a classifier
 * 
 * @author Remco Bouckaert (rrb@xm.co.nz)
 * @version $Revision: 1.2 $
 */
public interface Scoreable {

  /**
   * score types
   */
  public static final int BAYES = 0;
  public static final int MDL = 1;
  public static final int ENTROPY = 2;
  public static final int AIC = 3;

  /**
   * Returns log-score
   */
  public double logScore(int nType);
}    // interface Scoreable




