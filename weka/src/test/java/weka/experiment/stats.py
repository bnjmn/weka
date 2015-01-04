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
# A python module to precisely calculate the weighted mean and weighted
# variance of a series of data points as well as computing the closest float
# to the square root of a fractions.Fraction number.
#
# Author: Benjamin Weber ( benweber at student dot ethz dot ch )
#
###############################################################################

from fractions import Fraction

def mean(series, weights):
   """Precisely calculates the mean of a series of floats"""

   if len(series) <> len(weights):
      return float("NaN")

   _sum = Fraction(0)
   total_weight = Fraction(0)

   i = 0
   length = len(series)
   while (i < length):

      _sum += Fraction.from_float(series[i])*Fraction.from_float(weights[i])
      total_weight += Fraction.from_float(weights[i])

      i += 1

   if (total_weight <= 0):
      return float("NaN")

   return _sum/total_weight

def variance(series, weights, mean):
   """Precisely calculates the variance of a series of floats
   
   Note: uses N-1 as correction factor.
   Note: assumes mean to be an instance of Fraction"""

   if len(series) <> len(weights):
      return float("NaN")

   if not isinstance(mean, Fraction):
      return float("NaN")

   factor = Fraction(0)
   total_weight = Fraction(-1)

   i = 0
   length = len(series)
   while (i < length):
      
      delta = Fraction.from_float(series[i]) - mean
      weight = Fraction.from_float(weights[i])
      factor += delta*delta*weight

      total_weight += weight

      i += 1

   if total_weight <= 0:
      return float("NaN")

   return factor/total_weight

def sqrt_float(x):
   """Finds the closest float to the square root of x using simple bisection."""

   if (x < 0):
      return float("NaN")

   if x == 0 or x == 1:
      return float(x)

   if (x > 1):
      y1 = Fraction(1)
      y2 = x
   else:
      y1 = x
      y2 = Fraction(1)

   # using a simple bisection
   while float(y1) != float(y2):
      avg = (y1 + y2)/2
      if (avg*avg < x):
         y1 = avg
      else:
         y2 = avg

   return float(y1)
