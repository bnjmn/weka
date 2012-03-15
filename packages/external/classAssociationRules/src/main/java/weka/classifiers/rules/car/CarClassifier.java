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
 *    CarClassifier.java
 *    Copyright (C) 2004 Stefan Mutter
 *
 */
package weka.classifiers.rules.car;

import weka.classifiers.AbstractClassifier;
import weka.classifiers.Classifier;
import weka.core.Instances;
import weka.core.FastVector;
import weka.core.Instance;
import weka.associations.classification.LabeledItemSet;
import weka.associations.classification.CrTree;
import weka.associations.ItemSet;

/**
 * Abstract class that has to be implemented by all classifiers for class association rules
 * 
 * @author Stefan Mutter
 * @version $Revision$
 */

public abstract class CarClassifier extends AbstractClassifier {

    
    /**
     * Sorts the items accoridng to the sorting in the CrTree where the mined rules are stored.
     * In a CrTree the more frequent items are closer to the root.
     * @return the sorted item
     * @param instances the instance for which rules have been mined
     * @param instance the instance under consideration
     * @param pruneTree the CrTree where the rules are stored.
     * @throws Exception if it cannot be sorted
     */    
    public FastVector sortAttributes(Instances instances, Instance instance, CrTree pruneTree) throws Exception{
	
	FastVector result, itemSet = new FastVector();
	Instances forSorting = new Instances(instances,1);
        
	forSorting.add(instance);
        forSorting = LabeledItemSet.divide(forSorting,false);

        int[] items = new int[forSorting.numAttributes()];
        for(int i = 0;i<forSorting.numAttributes();i++)
            items[i]=(int)((forSorting.firstInstance()).value(i));
        ItemSet current = new ItemSet(1,items);
	result = pruneTree.sortItemSet(current);
	return result;
    }
    
}
   
