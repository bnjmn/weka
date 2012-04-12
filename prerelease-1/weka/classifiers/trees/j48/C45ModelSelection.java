/*
 *    C45ModelSelection.java
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


package weka.classifiers.j48;

import java.util.*;
import weka.core.*;

/**
 * Class for selecting a C4.5-type split for a given dataset.
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */

public class C45ModelSelection extends ModelSelection {
  
  // ==================
  // Private variables.
  // ==================

  private int minNoObJ;               // Minimum number of objects in interval.
  private Instances allDatA; // All the training data

  // ===============
  // Public methods.
  // ===============

  /**
   * Initializes the split selection method with the given parameters.
   * @param minNoObj minimum number of instances that have to occur in at least two
   * subsets induced by split
   * @param allData FULL training dataset (necessary for
   * selection of split points).
   */

  public C45ModelSelection(int minNoObj, Instances allData){
    minNoObJ = minNoObj;
    allDatA = allData;
  }

  /**
   * Selects C4.5-type split for the given dataset.
   */

  public final ClassifierSplitModel selectModel(Instances data){

    double minResult;
    double currentResult;
    C45Split [] currentModel;
    C45Split bestModel = null;
    NoSplit noSplitModel = null;
    double averageInfoGain = 0;
    int validModels = 0;
    boolean multiVal = true;
    Distribution checkDistribution;
    Attribute attribute;
    double sumOfWeights;
    int i;
    
    try{

      // Check if all Instances belong to one class or if not
      // enough Instances to split.

      checkDistribution = new Distribution(data);
      noSplitModel = new NoSplit(checkDistribution);
      if (Utils.sm(checkDistribution.total(),2*minNoObJ) ||
	  Utils.eq(checkDistribution.total(),
		   checkDistribution.perClass(checkDistribution.maxClass())))
	return noSplitModel;

      // Check if all attributes are nominal and have a 
      // lot of values.

      if (allDatA != null) {
	Enumeration enum = data.enumerateAttributes();
	while (enum.hasMoreElements()) {
	  attribute = (Attribute) enum.nextElement();
	  if ((attribute.isNumeric()) ||
	      (Utils.sm((double)attribute.numValues(),
			(0.3*(double)allDatA.numInstances())))){
	    multiVal = false;
	    break;
	  }
	}
      } 

      currentModel = new C45Split[data.numAttributes()];
      sumOfWeights = data.sumOfWeights();

      // For each attribute.
 
      for (i = 0; i < data.numAttributes(); i++){
	
	// Apart of class attribute.
	
	if (i != (data).classIndex()){
	  
	  // Get models for current attribute.
	  
	  currentModel[i] = new C45Split(i,minNoObJ,sumOfWeights);
	  currentModel[i].buildClassifier(data);
	  
	  // Check if useful split for current attribute
	  // exists and check for enumerated attributes with 
	  // a lot of values.
	  
	  if (currentModel[i].checkModel())
	    if (allDatA != null) {
	      if ((data.attribute(i).isNumeric()) ||
		  (multiVal || Utils.sm((double)data.attribute(i).numValues(),
					(0.3*(double)allDatA.numInstances())))){
		averageInfoGain = averageInfoGain+currentModel[i].infoGain();
		validModels++;
	      } 
	    } else {
	      averageInfoGain = averageInfoGain+currentModel[i].infoGain();
	      validModels++;
	    }
	}else
	  currentModel[i] = null;
      }
      
      // Check if any useful split was found.

      if (validModels == 0)
	return noSplitModel;
      averageInfoGain = averageInfoGain/(double)validModels;

      // Find "best" attribute to split on.

      minResult = 0;
      for (i=0;i<data.numAttributes();i++){
	if ((i != (data).classIndex()) &&
	    (currentModel[i].checkModel()))
	  
	  // Use 1E-3 here to get a closer approximation to the original
	  // implementation.
	  
	  if ((currentModel[i].infoGain() >= (averageInfoGain-1E-3)) &&
	      Utils.gr(currentModel[i].gainRatio(),minResult)){ 
	    bestModel = currentModel[i];
	    minResult = currentModel[i].gainRatio();
	  } 
      }

      // Check if useful split was found.

      if (Utils.eq(minResult,0))
	return noSplitModel;
      
      // Add all Instances with unknown values for the corresponding
      // attribute to the distribution for the model, so that
      // the complete distribution is stored with the model. 
      
      bestModel.distribution().
	addInstWithUnknown(data,bestModel.attIndex());
      
      // Set the split point analogue to C45 if attribute numeric.

      if (allDatA != null)
	bestModel.setSplitPoint(allDatA);
      return bestModel;
    }catch(Exception e){
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Selects C4.5-type split for the given dataset.
   */

  public final ClassifierSplitModel selectModel(Instances train, Instances test) {

    return selectModel(train);
  }
}



