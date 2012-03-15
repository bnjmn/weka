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
 *    AlgorithmConfiguration.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi.miti;

/**
 * Stores parameters that determine the configuration of the algorithm.
 */
public class AlgorithmConfiguration {
  
  /**
   * Default constructor that assigns default values.
   */
  public AlgorithmConfiguration() {
    this.method = weka.classifiers.mi.MITI.SPLITMETHOD_MAXBEPP;
    this.unbiasedEstimate = false;
    this.useBagStatistics = false;
    this.kBEPPConstant = 5;
    this.bagCountMultiplier = 0.5;
    this.attributeSplitChoices = 1;
    this.attributesToSplit = -1;
  }

  /**
   * Constructor that sets algorithm parameters based on arguments.
   */
  public AlgorithmConfiguration(int method, boolean unbiasedEstimate,
                                int kBEPPConstant, boolean bagStatistics,
                                double bagCountMultiplier, int attributesToSplit,
                                int attributeSplitChoices) {
    this.method = method;
    this.unbiasedEstimate = unbiasedEstimate;
    this.useBagStatistics = bagStatistics;
    this.kBEPPConstant = kBEPPConstant;
    this.bagCountMultiplier = bagCountMultiplier;
    this.attributesToSplit = attributesToSplit;
    this.attributeSplitChoices = attributeSplitChoices;
  }

  /**
   * The method used to score a split (1 = Gini, 2 = Max BEPP, 3 = SSBEPP)
   */
  public int method;

  /**
   * Determines whether an unbiased score is used to estimate the proportion
   * of positives
   */
  public boolean unbiasedEstimate;

  /**
   * The constant used to scale the influence of a node's size on its
   * split score.
   */
  public int kBEPPConstant;

  /**
   * Determines whether bag stats are used to score splits, or instance
   * stats.
   */
  public boolean useBagStatistics;

  /**
   * The value used to determine the influence of instance counts when
   * using bag counts. Multiplier is used as M^(instance count for bag),
   * where M is the bag count multiplier. Default value 0.5
   */
  public double bagCountMultiplier;

  /**
   * The number of attributes randomly selected to find the best from. -1:
   * All attributes, -2: Square root of total attribute count
   */
  public int attributesToSplit;

  /**
   * The number of top ranked attribute splits to randomly pick from
   * (default value 1 has no randomness)
   */
  public int attributeSplitChoices;
}
