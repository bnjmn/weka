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
 *    Split.java
 *    Copyright (C) 2011 University of Waikato, Hamilton, New Zealand
 *
 */
package weka.classifiers.mi.miti;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import weka.core.Attribute;
import weka.core.Instance;

/**
 * Represents a split in the decision tree.
 * 
 * @author Luke Bjerring
 * @version $Revision$
 */
public class Split implements Serializable {

  /** ID added to avoid warning */
  private static final long serialVersionUID = 147371323803237346L;

  // The attribute used for the split
  public Attribute attribute;

  // The split point, in case the attribute is numeric
  public double splitPoint;

  // The score associated with the split
  public double score;

  // Whether the split is on a nominal attribute
  public boolean isNominal = false;

  /**
   * Finds the best split based on the given arguments.
   */
  public static Split getBestSplitPoint(final Attribute a,
    ArrayList<Instance> enabled, HashMap<Instance, Bag> instanceBags,
    AlgorithmConfiguration settings) {

    // Get the split method
    IBestSplitMeasure bsm;
    if (settings.method == weka.classifiers.mi.MITI.SPLITMETHOD_GINI) {
      bsm = new Gini();
    } else if (settings.method == weka.classifiers.mi.MITI.SPLITMETHOD_SSBEPP) {
      bsm = new SSBEPP();
    } else {
      bsm = new MaxBEPP();
    }

    // Nominal values get a different method
    if (a.isNominal()) {
      return getBestNominalSplitPoint(a, enabled, instanceBags, settings, bsm);
    }

    // Order the data by the attribute we're looking at
    Collections.sort(enabled, new Comparator<Instance>() {
      @Override
      public int compare(Instance arg0, Instance arg1) {
        return Double.compare(arg0.value(a), arg1.value(a));
      }
    });

    Split split = null;

    SufficientStatistics ss;
    if (!settings.useBagStatistics) {
      ss = new SufficientInstanceStatistics(enabled, instanceBags);
    } else {
      ss = new SufficientBagStatistics(enabled, instanceBags,
        settings.bagCountMultiplier);
    }

    // Iterate through all splits, and score them, keeping the best one
    for (int i = 0; i < enabled.size() - 1; i++) {

      ss.updateStats(enabled.get(i), instanceBags);

      if (enabled.get(i).value(a) == enabled.get(i + 1).value(a)) {
        continue;
      }

      double splitPoint = (enabled.get(i).value(a) + enabled.get(i + 1)
        .value(a)) / 2;

      double score = bsm.getScore(ss, settings.kBEPPConstant,
        settings.unbiasedEstimate);

      if (split == null) {
        split = new Split();
        split.attribute = a;
        split.score = score;
        split.splitPoint = splitPoint;
        continue;
      }

      if (score > split.score) {
        split.score = score;
        split.splitPoint = splitPoint;
      }
    }

    return split;
  }

  /**
   * Computes split for a nominal attribute based on given arguments.
   */
  private static Split getBestNominalSplitPoint(Attribute a,
    ArrayList<Instance> enabled, HashMap<Instance, Bag> instanceBags,
    AlgorithmConfiguration settings, IBestSplitMeasure bsm) {
    Split s = new Split();
    s.isNominal = true;
    s.attribute = a;
    SufficientStatistics[] ss = new SufficientStatistics[a.numValues()];
    if (!settings.useBagStatistics) {
      for (int i = 0; i < a.numValues(); i++) {
        ss[i] = new SufficientInstanceStatistics(enabled, instanceBags);
      }
    } else {
      for (int i = 0; i < a.numValues(); i++) {
        ss[i] = new SufficientBagStatistics(enabled, instanceBags,
          settings.bagCountMultiplier);
      }
    }
    for (Instance i : enabled) {
      ss[(int) i.value(a)].updateStats(i, instanceBags);
    }
    double[] totals = new double[a.numValues()];
    double[] positiveCounts = new double[a.numValues()];
    for (int i = 0; i < a.numValues(); i++) {
      totals[i] = ss[i].totalCountLeft();
      positiveCounts[i] = ss[i].positiveCountLeft();
    }
    s.score = bsm.getScore(totals, positiveCounts, settings.kBEPPConstant,
      settings.unbiasedEstimate);
    return s;
  }
}
