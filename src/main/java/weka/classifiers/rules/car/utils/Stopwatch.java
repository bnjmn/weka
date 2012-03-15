/*
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/*
 *    Stopwatch.java
 *    Copyright (C) 2001 FracPete
 *
 */

package weka.classifiers.rules.car.utils;

import java.util.Date;
import java.io.Serializable;

/**
* This class provides a simple method to measure runtime behaviour.
*
*
* @author FracPete (fracpete@cs.waikato.ac.nz)
* @version $Revision$ 
*/

public class Stopwatch implements Serializable
{
   /** The start time. */
   private Date            startTime;
   
   /** The end time. */
   private Date            endTime;
   
   /** Flag indicating whether or not it is still running. */
   private boolean         running;
   
   /** Flag indicating whether or not it is started at all. */
   private boolean         started;

   /**
   * Constructor
   * initializes the object
   */
   public Stopwatch()
   {
      super();

      started = false;
      running = false;
   }

   /**
   * starts the "clock"
   */
   public void start()
   {
      startTime = new Date();
      started   = true;
      running   = true;
   }

   /**
   * stops the "clock"
   */
   public void stop()
   {
      endTime = new Date();
      running = false;
   }

   /**
   * returns the seconds
   * @return            the stopped time in seconds
   */
   public double getSeconds()
   {
      return (getMilliSeconds() / 1000.0);
   }

   /**
   * returns the milliseconds
   * @return            the stopped time in milliseconds
   */
   public long getMilliSeconds()
   {
      // started?
      if (!started)
      {
         startTime = new Date();
         endTime   = startTime;
      }

      // still running?
      if (running)
         endTime = new Date();
   
      return (endTime.getTime() - startTime.getTime());
   }

   /**
    * returns the time in Seconds
    * @return time in seconds
    */
   public String toString()
   {
      return Double.toString(getSeconds());
   }
}
