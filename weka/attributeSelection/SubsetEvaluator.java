/*
 *    SubsetEvaluator.java
 *    Copyright (C) 1999 Mark Hall
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

package weka.attributeSelection;
import java.io.*;
import java.util.*;
import weka.core.*;


/** 
 * Abstract attribute subset evaluator.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version 1.0 March 1999 (Mark)
 */
public abstract class SubsetEvaluator extends ASEvaluation {

    // ===============
    // Public methods.
    // ===============

    /**
     * evaluates a subset of attributes
     *
     * @param subset a bitset representing the attribute subset to be 
     * evaluated 
     * @exception Exception if the subset could not be evaluated
     */
    public abstract double evaluateSubset(BitSet subset) throws Exception;

}

