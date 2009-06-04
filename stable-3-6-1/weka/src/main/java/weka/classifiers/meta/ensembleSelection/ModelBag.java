/*
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

/*
 *    EnsembleSelection.java
 *    Copyright (C) 2006 David Michael
 *
 */

package weka.classifiers.meta.ensembleSelection;

import weka.classifiers.Evaluation;
import weka.core.Instances;
import weka.core.RevisionHandler;
import weka.core.RevisionUtils;

import java.util.Random;

/**
 * This class is responsible for the duties of a bag of models. It is designed
 * for use with the EnsembleSelection meta classifier. It handles shuffling the
 * models, doing sort initialization, performing forward selection/ backwards
 * elimination, etc.
 * <p/>
 * We utilize a simple "virtual indexing" scheme inside. If we shuffle and/or
 * sort the models, we change the "virtual" order around. The elements of the
 * bag are always those elements with virtual index 0..(m_bagSize-1). Each
 * "virtual" index maps to some real index in m_models. Not every model in
 * m_models gets a virtual index... the virtual indexing is what defines the
 * subset of models of which our Bag is composed. This makes it easy to refer to
 * models in the bag, by their virtual index, while maintaining the original
 * indexing for our clients.
 * 
 * @author  David Michael
 * @version $Revision: 1.2 $
 */
public class ModelBag
  implements RevisionHandler {
  
  /**
   * The "models", as a multidimensional array of predictions for the
   * validation set. The first index is the model index, the second index is
   * the index of the instance, and the third is the typical "class" index for
   * a prediction's distribution. This is given to us in the constructor, and
   * we never change it.
   */
  private double m_models[][][];
  
  /**
   * Maps each model in our virtual indexing scheme to its original index as
   * it is in m_models. The first m_bag_size elements here are considered our
   * bag. Throughout the code, we use the index in to this array to refer to a
   * model. When we shuffle the models, we really simply shuffle this array.
   * When we want to refer back to the original model, it is easily looked up
   * in this array. That is, if j = m_model_index[i], then m_models[j] is the
   * model referred to by "virtual index" i. Models can easily be accessed by
   * their virtual index using the "model()" method.
   */
  private int m_modelIndex[];
  
  /**
   * The number of models in our bag. 1 <= m_bag_size <= m_models.length
   */
  private int m_bagSize;
  
  /**
   * The total number of models chosen thus far for this bag. This value is
   * important when calculating the predictions for the bag. (See
   * computePredictions).
   */
  private int m_numChosen;
  
  /**
   * The number of times each model has been chosen. Also can be thought of as
   * the weight for each model. Indexed by the "virtual index".
   */
  private int m_timesChosen[];
  
  /**
   * If true, print out debug information.
   */
  private boolean m_debug;
  
  /**
   * Double representing the best performance achieved thus far in this bag.
   * This Must be updated each time we make a change to the bag that improves
   * performance. This is so that after all hillclimbing is completed, we can
   * go back to the best ensemble that we encountered during hillclimbing.
   */
  private double m_bestPerformance;
  
  /**
   * Array representing the weights for all the models which achieved the best
   * performance thus far for the bag (i.e., the weights that achieved
   * m_bestPerformance. This Must be updated each time we make a change to the
   * bag (that improves performance, by calling updateBestTimesChosen. This is
   * so that after all hillclimbing is completed, we can go back to the best
   * ensemble that we encountered during hillclimbing. This array, unlike
   * m_timesChosen, uses the original indexing as taken from m_models. That
   * way, any time getModelWeights is called (which returns this array), the
   * array is in the correct format for our client.
   */
  private int m_bestTimesChosen[];
  
  /**
   * Constructor for ModelBag.
   * 
   * @param models
   *            The complete set of models from which to draw our bag. First
   *            index is for the model, second is for the instance. The last
   *            is a prediction distribution for that instance. Models are
   *            represented by this array of predictions for validation data,
   *            since that's all ensemble selection needs to know.
   * @param bag_percent
   *            The percentage of the set of given models that should be used
   *            in the Model Bag.
   * @param debug
   *            Whether the ModelBag should print debug information.
   * 
   */
  public ModelBag(double models[][][], double bag_percent, boolean debug) {
    m_debug = debug;
    if (models.length == 0) {
      throw new IllegalArgumentException(
      "ModelBag needs at least 1 model.");
    }
    m_bagSize = (int) ((double) models.length * bag_percent);
    m_models = models;
    m_modelIndex = new int[m_models.length];
    m_timesChosen = new int[m_models.length];
    m_bestTimesChosen = m_timesChosen;
    m_bestPerformance = 0.0;
    
    // Initially, no models are chosen.
    m_numChosen = 0;
    // Prepare our virtual indexing scheme. Initially, the indexes are
    // the same as the original.
    for (int i = 0; i < m_models.length; ++i) {
      m_modelIndex[i] = i;
      m_timesChosen[i] = 0;
    }
  }
  
  /**
   * Swap model at virtual index i with model at virtual index j. This is used
   * to shuffle the models. We do not change m_models, only the arrays which
   * use the virtual indexing; m_modelIndex and m_timesChosen.
   * 
   * @param i	first index
   * @param j	second index
   */
  private void swap(int i, int j) {
    if (i != j) {
      int temp_index = m_modelIndex[i];
      m_modelIndex[i] = m_modelIndex[j];
      m_modelIndex[j] = temp_index;
      
      int tempWeight = m_timesChosen[i];
      m_timesChosen[i] = m_timesChosen[j];
      m_timesChosen[j] = tempWeight;
    }
  }
  
  /**
   * Shuffle the models. The order in m_models is preserved, but we change our
   * virtual indexes around.
   * 
   * @param rand	the random number generator to use
   */
  public void shuffle(Random rand) {
    if (m_models.length < 2)
      return;
    
    for (int i = 0; i < m_models.length; ++i) {
      int swap_index = rand.nextInt(m_models.length - 1);
      if (swap_index >= i)
	++swap_index; // don't swap with itself
      swap(i, swap_index);
    }
  }
  
  /**
   * Convert an array of weights using virtual indices to an array of weights
   * using real indices.
   * 
   * @param virtual_weights	the virtual indices
   * @return			the real indices
   */
  private int[] virtualToRealWeights(int virtual_weights[]) {
    int real_weights[] = new int[virtual_weights.length];
    for (int i = 0; i < real_weights.length; ++i) {
      real_weights[m_modelIndex[i]] = virtual_weights[i];
    }
    return real_weights;
  }
  
  /**
   * 
   */
  private void updateBestTimesChosen() {
    m_bestTimesChosen = virtualToRealWeights(m_timesChosen);
  }
  
  /**
   * Sort initialize the bag.
   * 
   * @param num
   *            the Maximum number of models to initialize with
   * @param greedy
   *            True if we do greedy addition, up to num. Greedy sort
   *            initialization adds models (up to num) in order of best to
   *            worst performance until performance no longer improves.
   * @param instances
   *            the data set (needed for performance evaluation)
   * @param metric
   *            metric for which to optimize. See EnsembleMetricHelper
   * @return returns an array of indexes which were selected, in order
   *         starting from the model with best performance.
   * @throws Exception if something goes wrong
   */
  public int[] sortInitialize(int num, boolean greedy, Instances instances,
      int metric) throws Exception {
    
    // First, get the performance of each model
    double performance[] = new double[m_bagSize];
    for (int i = 0; i < m_bagSize; ++i) {
      performance[i] = evaluatePredictions(instances, model(i), metric);
    }
    int bestModels[] = new int[num]; // we'll use this to save model info
    // Now sort the models by their performance... note we only need the
    // first "num",
    // so we don't actually bother to sort the whole thing... instead, we
    // pick the num best
    // by running num iterations of selection sort.
    for (int i = 0; i < num; ++i) {
      int max_index = i;
      double max_value = performance[i];
      for (int j = i + 1; j < m_bagSize; ++j) {
	// Find the best model which we haven't already selected
	if (performance[j] > max_value) {
	  max_value = performance[j];
	  max_index = j;
	}
      }
      // Swap ith model in to the ith position (selection sort)
      this.swap(i, max_index);
      // swap performance numbers, too
      double temp_perf = performance[i];
      performance[i] = performance[max_index];
      performance[max_index] = temp_perf;
      
      bestModels[i] = m_modelIndex[i];
      if (!greedy) {
	// If we're not being greedy, we just throw the model in
	// no matter what
	++m_timesChosen[i];
	++m_numChosen;
      }
    }
    // Now the best "num" models are all sorted and in position.
    if (greedy) {
      // If the "greedy" option was specified, do a smart sort
      // initialization
      // that adds models only so long as they help overall performance.
      // This is what was done in the original Caruana paper.
      double[][] tempPredictions = null;
      double bestPerformance = 0.0;
      if (num > 0) {
	++m_timesChosen[0];
	++m_numChosen;
	updateBestTimesChosen();
      }
      for (int i = 1; i < num; ++i) {
	tempPredictions = computePredictions(i, true);
	double metric_value = evaluatePredictions(instances,
	    tempPredictions, metric);
	if (metric_value > bestPerformance) {
	  // If performance improved, update the appropriate info.
	  bestPerformance = metric_value;
	  ++m_timesChosen[i];
	  ++m_numChosen;
	  updateBestTimesChosen();
	} else {
	  // We found a model that doesn't help performance, so we
	  // stop adding models.
	  break;
	}
      }
    }
    updateBestTimesChosen();
    if (m_debug) {
      System.out.println("Sort Initialization added best " + m_numChosen
	  + " models to the bag.");
    }
    return bestModels;
  }
  
  /**
   * Add "weight" to the number of times each model in the bag was chosen.
   * Typically for use with backward elimination.
   * 
   * @param weight	the weight to add
   */
  public void weightAll(int weight) {
    for (int i = 0; i < m_bagSize; ++i) {
      m_timesChosen[i] += weight;
      m_numChosen += weight;
    }
    updateBestTimesChosen();
  }
  
  /**
   * Forward select one model. Will add the model which has the best effect on
   * performance. If replacement is false, and all models are chosen, no
   * action is taken. If a model can be added, one always is (even if it hurts
   * performance).
   * 
   * @param withReplacement
   *            whether a model can be added more than once.
   * @param instances
   *            The dataset, for calculating performance.
   * @param metric
   *            The metric to which we will optimize. See EnsembleMetricHelper
   * @throws Exception if something goes wrong
   */
  public void forwardSelect(boolean withReplacement, Instances instances,
      int metric) throws Exception {
    
    double bestPerformance = -1.0;
    int bestIndex = -1;
    double tempPredictions[][];
    for (int i = 0; i < m_bagSize; ++i) {
      // For each model in the bag
      if ((m_timesChosen[i] == 0) || withReplacement) {
	// If the model has not been chosen, or we're allowing
	// replacement
	// Get the predictions we would have if we add this model to the
	// ensemble
	tempPredictions = computePredictions(i, true);
	// And find out how the hypothetical ensemble would perform.
	double metric_value = evaluatePredictions(instances,
	    tempPredictions, metric);
	if (metric_value > bestPerformance) {
	  // If it's better than our current best, make it our NEW
	  // best.
	  bestIndex = i;
	  bestPerformance = metric_value;
	}
      }
    }
    if (bestIndex == -1) {
      // Replacement must be false, with more hillclimb iterations than
      // models. Do nothing and return.
      if (m_debug) {
	System.out.println("Couldn't add model.  No action performed.");
      }
      return;
    }
    // We picked bestIndex as our best model. Update appropriate info.
    m_timesChosen[bestIndex]++;
    m_numChosen++;
    if (bestPerformance > m_bestPerformance) {
      // We find the peak of our performance over all hillclimb
      // iterations.
      // If this forwardSelect step improved our overall performance,
      // update
      // our best ensemble info.
      updateBestTimesChosen();
      m_bestPerformance = bestPerformance;
    }
  }
  
  /**
   * Find the model whose removal will help the ensemble's performance the
   * most, and remove it. If there is only one model left, we leave it in. If
   * we can remove a model, we always do, even if it hurts performance.
   * 
   * @param instances
   *            The data set, for calculating performance
   * @param metric
   *            Metric to optimize for. See EnsembleMetricHelper.
   * @throws Exception if something goes wrong
   */
  public void backwardEliminate(Instances instances, int metric)
  throws Exception {
    
    // Find the best model to remove. I.e., model for which removal improves
    // performance the most (or hurts it least), and remove it.
    if (m_numChosen <= 1) {
      // If we only have one model left, keep it, as a bag
      // which chooses no models doesn't make much sense.
      return;
    }
    double bestPerformance = -1.0;
    int bestIndex = -1;
    double tempPredictions[][];
    for (int i = 0; i < m_bagSize; ++i) {
      // For each model in the bag
      if (m_timesChosen[i] > 0) {
	// If the model has been chosen at least once,
	// Get the predictions we would have if we remove this model
	tempPredictions = computePredictions(i, false);
	// And find out how the hypothetical ensemble would perform.
	double metric_value = evaluatePredictions(instances,
	    tempPredictions, metric);
	if (metric_value > bestPerformance) {
	  // If it's better than our current best, make it our NEW
	  // best.
	  bestIndex = i;
	  bestPerformance = metric_value;
	}
      }
    }
    if (bestIndex == -1) {
      // The most likely cause of this is that we didn't have any models
      // we could
      // remove. Do nothing & return.
      if (m_debug) {
	System.out
	.println("Couldn't remove model.  No action performed.");
      }
      return;
    }
    // We picked bestIndex as our best model. Update appropriate info.
    m_timesChosen[bestIndex]--;
    m_numChosen--;
    if (m_debug) {
      System.out.println("Removing model " + m_modelIndex[bestIndex]
                                                          + " (" + bestIndex + ") " + bestPerformance);
    }
    if (bestPerformance > m_bestPerformance) {
      // We find the peak of our performance over all hillclimb
      // iterations.
      // If this forwardSelect step improved our overall performance,
      // update
      // our best ensemble info.
      updateBestTimesChosen();
      m_bestPerformance = bestPerformance;
    }
    // return m_model_index[best_index]; //translate to original indexing
    // and return
  }
  
  /**
   * Find the best action to perform, be it adding a model or removing a
   * model, and perform it. Some action is always performed, even if it hurts
   * performance.
   * 
   * @param with_replacement
   *            whether we can add a model more than once
   * @param instances
   *            The dataset, for determining performance.
   * @param metric
   *            The metric for which to optimize. See EnsembleMetricHelper.
   * @throws Exception if something goes wrong
   */
  public void forwardSelectOrBackwardEliminate(boolean with_replacement,
      Instances instances, int metric) throws Exception {
    
    // Find the best action to perform, be it adding a model or removing a
    // model,
    // and do it.
    double bestPerformance = -1.0;
    int bestIndex = -1;
    boolean added = true;
    double tempPredictions[][];
    for (int i = 0; i < m_bagSize; ++i) {
      // For each model in the bag:
      // Try removing the model
      if (m_timesChosen[i] > 0) {
	// If the model has been chosen at least once,
	// Get the predictions we would have if we remove this model
	tempPredictions = computePredictions(i, false);
	// And find out how the hypothetical ensemble would perform.
	double metric_value = evaluatePredictions(instances,
	    tempPredictions, metric);
	if (metric_value > bestPerformance) {
	  // If it's better than our current best, make it our NEW
	  // best.
	  bestIndex = i;
	  bestPerformance = metric_value;
	  added = false;
	}
      }
      if ((m_timesChosen[i] == 0) || with_replacement) {
	// If the model hasn't been chosen, or if we can choose it more
	// than once, try adding it:
	// Get the predictions we would have if we added the model
	tempPredictions = computePredictions(i, true);
	// And find out how the hypothetical ensemble would perform.
	double metric_value = evaluatePredictions(instances,
	    tempPredictions, metric);
	if (metric_value > bestPerformance) {
	  // If it's better than our current best, make it our NEW
	  // best.
	  bestIndex = i;
	  bestPerformance = metric_value;
	  added = true;
	}
      }
    }
    if (bestIndex == -1) {
      // Shouldn't really happen. Possible (I think) if the model bag is
      // empty. Just return.
      if (m_debug) {
	System.out.println("Couldn't add or remove model.  No action performed.");
      }
      return;
    }
    // Now we've found the best change to make:
    // * bestIndex is the (virtual) index of the model we should change
    // * added is true if the model should be added (false if should be
    // removed)
    int changeInWeight = added ? 1 : -1;
    m_timesChosen[bestIndex] += changeInWeight;
    m_numChosen += changeInWeight;
    if (bestPerformance > m_bestPerformance) {
      // We find the peak of our performance over all hillclimb
      // iterations.
      // If this forwardSelect step improved our overall performance,
      // update
      // our best ensemble info.
      updateBestTimesChosen();
      m_bestPerformance = bestPerformance;
    }
  }
  
  /**
   * returns the model weights
   * 
   * @return		the model weights
   */
  public int[] getModelWeights() {
    return m_bestTimesChosen;
  }
  
  /**
   * Returns the "model" at the given virtual index. Here, by "model" we mean
   * its predictions with respect to the validation set. This is just a
   * convenience method, since we use the "virtual" index more than the real
   * one inside this class.
   * 
   * @param index
   *            the "virtual" index - the one for internal use
   * @return the predictions for the model for all validation instances.
   */
  private double[][] model(int index) {
    return m_models[m_modelIndex[index]];
  }
  
  /**
   * Compute predictions based on the current model, adding (or removing) the
   * model at the given (internal) index.
   * 
   * @param index_to_change
   *            index of model we're adding or removing
   * @param add
   *            whether we add it. If false, we remove it.
   * @return the predictions for all validation instances
   */
  private double[][] computePredictions(int index_to_change, boolean add) {
    double[][] predictions = new double[m_models[0].length][m_models[0][0].length];
    for (int i = 0; i < m_bagSize; ++i) {
      if (m_timesChosen[i] > 0) {
	for (int j = 0; j < m_models[0].length; ++j) {
	  for (int k = 0; k < m_models[0][j].length; ++k) {
	    predictions[j][k] += model(i)[j][k] * m_timesChosen[i];
	  }
	}
      }
    }
    for (int j = 0; j < m_models[0].length; ++j) {
      int change = add ? 1 : -1;
      for (int k = 0; k < m_models[0][j].length; ++k) {
	predictions[j][k] += change * model(index_to_change)[j][k];
	predictions[j][k] /= (m_numChosen + change);
      }
    }
    return predictions;
  }
  
  /**
   * Return the performance of the given predictions on the given instances
   * with respect to the given metric (see EnsembleMetricHelper).
   * 
   * @param instances
   *            the validation data
   * @param temp_predictions
   *            the predictions to evaluate
   * @param metric
   *            the metric for which to optimize (see EnsembleMetricHelper)
   * @return the performance
   * @throws Exception if something goes wrong
   */
  private double evaluatePredictions(Instances instances,
      double[][] temp_predictions, int metric) throws Exception {
    
    Evaluation eval = new Evaluation(instances);
    for (int i = 0; i < instances.numInstances(); ++i) {
      eval.evaluateModelOnceAndRecordPrediction(temp_predictions[i],
	  instances.instance(i));
    }
    return EnsembleMetricHelper.getMetric(eval, metric);
  }
  
  /**
   * Gets the individual performances of all the models in the bag.
   * 
   * @param instances
   *            The validation data, for which we want performance.
   * @param metric
   *            The desired metric (see EnsembleMetricHelper).
   * @return the performance
   * @throws Exception if something goes wrong
   */
  public double[] getIndividualPerformance(Instances instances, int metric)
    throws Exception {
    
    double[] performance = new double[m_bagSize];
    for (int i = 0; i < m_bagSize; ++i) {
      performance[i] = evaluatePredictions(instances, model(i), metric);
    }
    return performance;
  }
  
  /**
   * Returns the revision string.
   * 
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision: 1.2 $");
  }
}
