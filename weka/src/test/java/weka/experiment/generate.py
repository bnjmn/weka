#!/usr/bin/env python

###############################################################################
#
#    This program is free software: you can redistribute it and/or modify
#    it under the terms of the GNU General Public License as published by
#    the Free Software Foundation, either version 3 of the License, or
#    (at your option) any later version.
# 
#    This program is distributed in the hope that it will be useful,
#    but WITHOUT ANY WARRANTY; without even the implied warranty of
#    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
#    GNU General Public License for more details.
# 
#    You should have received a copy of the GNU General Public License
#    along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
###############################################################################

###############################################################################
#
#  Copyright (C) 2014 University of Waikato, Hamilton, NZ
#
###############################################################################

###############################################################################
#
# A python program to generate different test sets with their weighted mean
# and weighted variance and to export those test sets to Java with minimal loss
# of precision.
#
# Author: Benjamin Weber ( benweber at student dot ethz dot ch )
#
###############################################################################

def generate_numbers(count, mean, stdDev, ordering="Random"):
   """Generates a series of number according to the parameters with a uniform
   distribution.
   
   Possible orderings are: 'Random' (default), 'Sorted', 'Reverse Sorted'"""

   if (ordering != "Random" and ordering != "Sorted" and
           ordering != "Reverse Sorted"):
      return float("NaN")

   from random import random

   numbers = []

   while count > 0:

      # uniform distribution with a = 0 and b = 1 has mean = 0.5 and stdDev = 0.2887
      # doesn't handle high mean and low stdDev well
      numbers.append((random() - 0.5)*stdDev/0.2887 + mean)

      count -= 1

   if ordering == "Sorted":
      numbers.sort()
   elif ordering == "Reverse Sorted":
      numbers.sort()
      numbers.reverse()

   return numbers

def generate_weights(count, mean, stdDev, ordering="Random"):
   """Generates a series of weights according to the parameters

   Note: Guarantees that all weights are positive"""

   numbers = generate_numbers(count, mean, stdDev, ordering)

   return [abs(x) for x in numbers]

def export(number):
   """Exports a python float to a Java long to avoid loss of precision."""

   from struct import pack, unpack

   return str(unpack("<q",pack("<d", number))[0]) + "L"

def export_series(series):
   """Exports a series of python floats to Java longs to avoid loss of
   precision."""

   out = "{"
   for number in series:
      out += export(number)
      out += ", "
   out = out[:-2] + "}"

   return out

def print_testcase(variablePrefix, i, count, values_mean, values_stdDev,
        values_ordering, weights_mean, weights_stdDev, weights_ordering):

   from stats import mean, variance, sqrt_float
   # calculate values
   
   values = generate_numbers(count, values_mean,
             values_stdDev, values_ordering)
   weights = generate_weights(count, weights_mean,
              weights_stdDev, weights_ordering)
   
   m = mean(values, weights)
   v = variance(values, weights, m)
   stdDev = sqrt_float(v)
   
   weightedValues = []
   for value, weight in zip(values, weights):
       weightedValues.append(value)
       weightedValues.append(weight)
   
   # output values
   print("// Generated values with parameters:")
   print("// Count = {}".format(count))
   print("// values_mean = {:e}; values_stdDev = {:e}; values_ordering = {}"
      .format(values_mean, values_stdDev, values_ordering))
   print("// weights_mean = {:e}; weights_stdDev = {:e}; weights_ordering = {}"
      .format(weights_mean, weights_stdDev, weights_ordering))


   print("private static final long {}Mean{} = {}; // approx ~ {:e}"
      .format(variablePrefix, i, export(float(m)), float(m)))
   
   print("private static final long {}StdDev{} = {}; // approx ~ {:e}"
      .format(variablePrefix, i, export(stdDev), stdDev))

   print("private static final long[] {}WeightedValues{} = {};"
      .format(variablePrefix, i, export_series(weightedValues)))
   
   print("")

if __name__ == "__main__":

   variablePrefix = "generated"
   i = 1
  
   # common cases:
   for values_mean in [1e8, -1e-8]:
      for values_stdDev in [1e8, 1e-8]:
         for weights_mean, weights_stdDev in [
               (1, 0),
	       (1, 0.4),
	       (15, 7)
	       ]:

	    print_testcase(variablePrefix, i,
	       100, #count
	       values_mean, values_stdDev,
	       "Random", # values_ordering
	       weights_mean, weights_stdDev,
	       "Random", # weights_ordering
	       )
	    i += 1
   
   # cases with different sortings:
   for values_ordering in ["Sorted", "Reverse Sorted"]:
      for weights_ordering in ["Sorted", "Reverse Sorted"]:
         for values_mean, values_stdDev in [
	       (1e8, 1e8),
	       (-1e8, 1e-8)
	       ]:

	    print_testcase(variablePrefix, i,
	       50, #count
	       values_mean, values_stdDev, values_ordering,
	       1, # weights_mean
	       0.4, # weights_stdDev
	       weights_ordering
	       )
	    i += 1

   # long cases
   for values_mean, values_stdDev in [
         (1e-8, 1e-8),
	 (1e8, 1e8)
         ]:

      print_testcase(variablePrefix, i,
         1000, # count
         values_mean, values_stdDev,
         "Random", # values_ordering
         1, # weights_mean
         0.4, # weights_stdDev
         "Random", # weights_ordering
         )
      i += 1
