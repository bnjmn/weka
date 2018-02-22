package main;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import weka.core.*;
import weka.filters.*;
import weka.filters.unsupervised.attribute.Remove;
import weka.filters.unsupervised.instance.RemoveWithValues;

/**
 * <b>Weka_ManageInstances is the class used to:
 * <ul>
 * 		<li>Select some particular data of an Instances.</li>
 * 		<li>Build and destroy an Instances.</li>
 * </ul></b>
 * <p>
 * Weka_ManageInstances is used by <i>Weka_Use</i> which loads an Instances from an ARFF files 
 * and uses these methods to select some specific attributes or lines.
 * </p>
 * <p>
 * There are some methods with "_dontCareOfLastAtt" at the end of their name. It's because, I use to put
 * the prediction at the end of Instance (so it's the last Attributes).
 * </p>
 */
public class Weka_ManageInstances {
	/*
	public static void main(String[] args) throws Exception
	{
	    	// Load Data
	    //String filename = "./ressources/ARFF/2014_05_12_14_23_15.arff";
	    //String filename = "./ressources/ARFF/test1.arff";
	    String filename = "./ressources/ARFF/1mn.arff";
		DataSource source = new DataSource(filename);
		
	    Instances data = source.getDataSet();
	    if (data.classIndex() == -1)
	        data.setClassIndex(data.numAttributes() - 1);
	    
	    	//______Row Selection_______//
	    data = percentSelection(data, 0.80, 0.90);
	    data = rowNumberSelection(data, 1, 140);	    
	    
	    	//____Colomun Selection________ //    
	    data = attributSelection(data, "1-30,28-60,62,63-68,70");
	    
	    	//______Row Selection_______//
	    data = operatorSelection(data, 4, '>', -0.3);
	    data = differentNextSelection (data);
	    
	    Instances newData = instancesFromString(data.toString());
	    System.out.println(newData);

	    //System.out.println("===============");
	    //data = concatInstances(data, data);
	    //System.out.println(data);
  	}
	*/
  	
	/**
	 * Convert a String which represents an ARFF file into an Instances.
	 * 
	 * @param arff
	 * 				String which represents an ARFF file.
	 * 
	 * @return	The Instances from the ARFF String.
	 * 
	 * @throws IOException
	 */
	public static Instances instancesFromString (String arff) throws IOException
	{
		StringReader reader = new StringReader(arff);
		Instances insts = new Instances (reader);
		if (insts.classIndex() == -1)
			insts.setClassIndex(insts.numAttributes() - 1);
		return insts;
	}

	/**
	 * <b>[Columns selection]</b>Select some attributes from a given Instances.
	 *   
	 * @param data
	 * 				An Instances of the data.
	 * @param option
	 * 				String which represents the attributes to remove.
	 * 				<i>"1-4"</i>  | <i>"28"</i>  | <i>"1-70,45,68-72"</i> | <i>""</i> | <i>...</i>
	 * 
	 * @return The new Instances of data without undesired attributes.
	 * 
	 * @throws Exception
	 */
	public static Instances attributSelection (Instances data, String option) throws Exception
	{		
	    String[] options = new String[2];
	    options[0] = "-R";
	    options[1] = option;
	    Remove remove = new Remove();
	    remove.setOptions(options);
	    remove.setInputFormat(data);
	    Instances newData = Filter.useFilter(data, remove);
	    if (newData.classIndex() == -1)
	    	newData.setClassIndex(newData.numAttributes() - 1);
	    return newData;	    
	}
	
	/**
	 * <b>[Rows selection]</b> Used to choose some lines of data by indicating between what
	 * percents select the rows.
	 * 
	 * @param data
	 * 				An Instances of the data.
	 * @param start
	 * 				Percent indicating the first line of the selection.
	 * @param end
	 * 				Percent indicating the last line of the selection.
	 * 
	 * @return The new Instances of data with only the desired rows.
	 */
	public static Instances percentSelection (Instances data, double start, double end)
	{
		if(end<start){
			double temp = start;
			start = end;
			end = temp;
		}		
	    int to_start   = (int) Math.round(data.numInstances() * start);
	    int to_end = Math.max ( (int) Math.round(data.numInstances() * end) - to_start, 1);
	    
	    Instances newData = new Instances(data, to_start, to_end);
		if (newData.classIndex() == -1)
	    	newData.setClassIndex(newData.numAttributes() - 1);
	    return (newData);		
	}
	
	/**
	 * <b>[Rows selection]</b> Select some lines of data by indicating between what
	 * line numbers choose the rows.
	 * 
	 * @param data
	 * 				An Instances of the data.
	 * @param start
	 * 				Line number indicating the first line of the selection.
	 * @param end
	 * 				Line number indicating the last line of the selection.
	 * 
	 * @return The new Instances of data with only the desired rows.
	 */
	public static Instances rowNumberSelection (Instances data, int start, int end)
	{
		if(end<start){
			int temp = start;
			start = end;
			end = temp;
		}
		Instances newData = new Instances(data, start, end-start);
		if (newData.classIndex() == -1)
	    	newData.setClassIndex(newData.numAttributes() - 1);		
	    return (newData);		
	}
	
	/**
	 * <b>[Rows selection]</b> Pick up lines whose the attribute number attribute_index is 
	 * [ <i>'>'</i>, <i>'<'</i>, <i>'='</i> ] than the value.
	 * Ex: data = operatorSelection(data, 4, '>', -0.3); 
	 * Every lines whose their values in the column number 4 are greater than -0.3.
	 * 
	 * @param data
	 * 				An Instances of the data.
	 * @param attribute_index
	 * 				The index of attribute column to compare.
	 * @param operator
	 * 				Used to choose how to compare.
	 * 				<i>'>'</i> | <i>'<'</i> | <i>'='</i> 
	 * @param value
	 * 				The value used to compare.
	 * 
	 * @return The new Instances of data with only the desired rows.
	 * 
	 * @throws Exception
	 */
	public static Instances operatorSelection (Instances data, int attribute_index, char operator, double value) throws Exception
	{
		RemoveWithValues filter = new RemoveWithValues();
		if (attribute_index> data.numAttributes())
			attribute_index = data.numAttributes();
		
		int current = 0;
		double epsilon = 0.001;
		String[] options = new String [4];
		
		switch (operator){
			case '>':
				options = new String[4];
				value += epsilon;
				break;
			case '<':
				options = new String[5];
				options[current++]= "-V";
				break;
			case '=':
				// >=
				options = new String[4];
				options[current++] = "-C";
			    options[current++] = String.valueOf(attribute_index);
			    options[current++] = "-S";
			    options[current++] = String.valueOf(value);
				
				filter.setOptions(options);
			    filter.setInputFormat(data);
			    data = Filter.useFilter(data, filter);
			    
			    // <=				
			    current =0;
				options = new String[5];
				options[current++]= "-V";
				value += epsilon;
				break;
			default:
				System.out.println("ERROR: Weka_ManageInstance, operatorSelection, unknow operator.");
				return data;				
		}

	    options[current++] = "-C";
	    options[current++] = String.valueOf(attribute_index);
	    options[current++] = "-S";
	    options[current++] = String.valueOf(value);
		
		filter.setOptions(options);
	    filter.setInputFormat(data);
	    Instances newData = Filter.useFilter(data, filter);
	    if (newData.classIndex() == -1)
	    	newData.setClassIndex(newData.numAttributes() - 1);
	    return newData;
	}
	
	/**
	 * <b>[Rows selection]</b> Delete every lines followed by a row with the same values.
	 * Use equalsInstance().
	 * 
	 * @param data
	 * 				An Instances of the data.
	 * 
	 * @return The new Instances of data with only the desired rows.
	 */
	public static Instances differentNextSelection (Instances data)
	{
		Instances newData = data;
		
		for(int index = newData.numInstances()-1; index>0; index--)
		{
			Instance inst = newData.instance(index);
			Instance next = newData.instance(index-1);
			if (equalsInstance(inst, next))
				newData.delete(index);
		}		
		return (newData);
	}
	
	/**
	 * Used to know if two Instance are equal.
	 * 
	 * @param inst1
	 * 				The first Instance to compare.
	 * @param inst2
	 * 				The second Instance to compare.
	 * 
	 * @return (inst1 == inst2)
	 */
	public static boolean equalsInstance (Instance inst1, Instance inst2)
	{
		if(inst1 == null && inst2 == null)
			return true;		
		if(inst1 == null || inst2 == null)
			return false;
		
		if(inst1.numValues() != inst2.numValues())
			return false;
		for(int index=0; index<inst1.numValues(); index++){
			if (inst1.value(index) != inst2.value(index) && !(Double.isNaN(inst1.value(index)) && Double.isNaN(inst2.value(index))))
				return false;
		}			
		return true;
	}
	
	/**
	 * Used to know if two Instance are equal without see the last.
	 * 
	 * @param inst1
	 * 				The first Instance to compare.
	 * @param inst2
	 * 				The second Instance to compare.
	 * 
	 * @return (inst1 == inst2)
	 */
	public static boolean equalsInstance_dontCareOfLastAtt (Instance inst1, Instance inst2)
	{
		if(inst1.numValues() != inst2.numValues())
			return false;
		for(int index=0; index<inst1.numValues()-1; index++){
			if (inst1.value(index) != inst2.value(index) && !(Double.isNaN(inst1.value(index)) && Double.isNaN(inst2.value(index))))
				return false;
		}			
		return true;
	}
	
	/**
	 * Return the index of the similar Instance of data which has only its last different value.
	 * 
	 * @param data
	 * 				The instances of the data.
	 * @param inst
	 * 				The instance to look for in data.
	 * 
	 * @return The index of an Instance which goes only its last different value different from inst.
	 */
	public static int indexOfSame_dontCareOfLastAtt (Instances data, Instance inst)
	{
		if(data.firstInstance().numValues() != inst.numValues())
			return -1;
		
		for (int i=0; i<data.numInstances(); i++)
		{			
			if (equalsInstance_dontCareOfLastAtt (data.instance(i), inst))
				return i;
		}
		return -1;
	}
	
	/**
	 * <b>[Add lines] </b>Return a concatenation of the given Instances.
	 * 
	 * @param inst1
	 * 				First Instances (head).
	 * @param inst2
	 * 				Second Instances to add (tail).
	 * 
	 * @return ( inst1 ^ inst2 )
	 */
	public static Instances concatInstances (Instances inst1, Instances inst2)
	{
		ArrayList<Instance> instAL = new ArrayList<Instance>();		
		for (int i=0; i<inst2.numInstances(); i++)
			instAL.add(inst2.instance(i));
		for (int i=0; i<instAL.size(); i++)
			inst1.add(instAL.get(i));
		return (inst1);
	}
	
	/**
	 * <b>[Add lines] </b>Add the Instance at the end of the Instances if the last is different.
	 * 
	 * @param data
	 * 				An Instances of the data.
	 * 
	 * @return The new Instances of data with only the desired rows.
	 */
	public static Instances addDifferentWithPrevious (Instances data, Instance inst)
	{
		Instances newData = data;		
		if (!equalsInstance(data.instance(data.numInstances()-1), inst))
			newData.add(inst);			
		return (newData);
	}
	
	/**
	 * <b>[Add lines] </b>Add the Instance at the end of the Instances if all instance are different.
	 * 
	 * @param data
	 * 				An Instances of the data.
	 * 
	 * @return The new Instances of data with only the desired rows.
	 */
	public static Instances addDifferentWithAll (Instances data, Instance inst)
	{
		Instances newData = data;
		
		for(int i=0; i<newData.numInstances(); i++)
		{
			if (equalsInstance(data.instance(i), inst))
				return newData;
		}

		newData.add(inst);			
		return (newData);
	}
	
	/**
	 * <b>[Add lines] </b>Used to add a new Instance in the learningInstances and replace 
	 * an older one which got the same value for a different prediction/last attributes.
	 * 
	 * @param data
	 * 				The learningInstance.
	 * @param inst
	 * 				The new instance to replace an older prediction.
	 * 
	 * @param addEvenIfNoSimilar
	 * 				Add inst at data even if there is no similar instance (not only replace).
	 * 
	 * @return The new learningInstances.
	 */
	public static Instances addDifferentWithAll_dontCareOfLastAtt (Instances data, Instance inst, boolean addEvenIfNoSimilar)
	{
		Instances newData = data;
		int i = indexOfSame_dontCareOfLastAtt (newData, inst);
		if(i!=-1)
		{
			newData.delete(i);
			newData.add(inst);
		}
		else if(addEvenIfNoSimilar)
			newData.add(inst);
		
		return newData;
	}
	
	/**
	 * Is there only different Instance in this Instances?
	 * 
	 * @param data 
	 * @return
	 */
	public static boolean gotOnlyDifferentInst (Instances data)
	{
		Instance inst;
		for(int i=0; i< data.numInstances(); i++)
		{
			inst = data.instance(i);
			for(int y=i; y<data.numInstances(); y++)
			{
				if (equalsInstance(data.instance(y), inst))
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Is there only different Instance in this Instances? (without take care
	 * of the last attribute)
	 * 
	 * @param data 
	 * @return
	 */
	public static boolean gotOnlyDifferentInst_dontCareOfLastAtt (Instances data)
	{
		Instance inst;
		for(int i=0; i< data.numInstances(); i++)
		{
			inst = data.instance(i);
			for(int y=i+1; y<data.numInstances(); y++)
			{
				if (equalsInstance(data.instance(y), inst))
					return false;
			}
		}
		return true;
	}
	
	/**
	 * Return the index of the Instance in the Instances. Or -1.
	 * 
	 * @param data
	 * @param inst
	 * 
	 * @return
	 */
	public static int indexOfInstance (Instances data, Instance inst)
	{
		for(int i=0; i<data.numInstances(); i++)
		{
			if(equalsInstance(data.instance(i), inst))
				return i;
		}
		return -1;
	}
	
	/**
	 * <b>[Rows Selection]</b> Delete every Instance inst of Data.
	 * 
	 * @param data
	 * @param inst
	 * 
	 * @return
	 */
	public static Instances deleteInstance(Instances data, Instance inst)
	{
		Instances newData = data;		
		int i=0;
		while(i!=-1 && data.numInstances()>2)
		{
			i = indexOfInstance (newData, inst);
			if(i!=-1)
				newData.delete(i);
		}	
		return newData;
	}
	
	/**
	 * Compute the distance between two same Attribute in percent, using their min and max.
	 *  
	 * @param att1
	 * @param att2
	 * @param min
	 * @param max
	 * 
	 * @return The distance in percent between the two Attribute.
	 */
	public static double distanceBetweenAttribute (double att1, double att2, double min, double max)
	{
		if(max != min && !Double.isNaN(min) && !Double.isNaN(max))
			return Math.abs ((att1 - att2)/ (max - min));
		return (Double.NaN);
	}
	
	/**
	 * Compute the distance between two Instance in percent, using the min and max of their Attribute.
	 * 
	 * @param inst1
	 * @param inst2
	 * @param valueMinMax
	 * 
	 * @return The distance in percent between the two Instance.
	 */
	public static double distanceBetweenInstance (Instance inst1, Instance inst2, ArrayList<Double> valueMinMax)
	{
		if( inst1.numAttributes() != inst2.numAttributes() || valueMinMax.size() != inst1.numAttributes()*2 )
			return Double.NaN;
		
		double distance = 0;
		for(int i=0; i<inst1.numAttributes(); i++)
			distance += distanceBetweenAttribute (inst1.value(i), inst2.value(i), valueMinMax.get(i), valueMinMax.get(i+1));	
		return ( distance/ inst1.numAttributes() );
	}
	
	/**
	 * Return an Instance of data which is the closest to inst, usign valueMinMax, an ArrayList of the value min and max
	 * taken by each Attribute of data.
	 * 
	 * @param data
	 * @param inst
	 * @param valueMinMax
	 * 						ArrayList of the value min and max taken by each Attribute of data.
	 * 
	 * @return The closest Instance of data to inst.
	 */
	public static Instance getClosestInstance (Instances data, Instance inst, ArrayList<Double> valueMinMax)
	{
		int index = indexOfInstance(data, inst);
		if (index != -1)
			return data.instance(index);
					
		index=0;
		double dist = distanceBetweenInstance (data.instance(0) ,inst, valueMinMax);
		
		for(int i=1; i<data.numInstances(); i++)
		{
			if(dist > distanceBetweenInstance (data.instance(i) ,inst, valueMinMax) )
			{
				dist = distanceBetweenInstance (data.instance(i) ,inst, valueMinMax);
				index = i;
			}
		}
		return data.instance(index);
	}
	
	/**
	 * <b>[Rows Selection]</b> Return an Instances without the closest Instance of inst.
	 * 
	 * @param data
	 * @param inst
	 * @param valueMinMax
	 * 						ArrayList of the value min and max taken by each Attribute of data.
	 * 
	 * @return The new data without the closest Instance of inst.
	 */
	public static Instances deleteClosestInstance (Instances data, Instance inst, ArrayList<Double> valueMinMax)
	{		
		Instances newData = data;
		
		Instance instToDel = Weka_ManageInstances.getClosestInstance (newData, inst, valueMinMax);
		newData = Weka_ManageInstances.deleteInstance(newData, instToDel);
		
		return newData;
	}
	
	/**
	 * <b>[Rows Selection]</b> Return an Instances without the numberToDel closest Instance of inst. 
	 * 
	 * @param data
	 * @param inst
	 * @param valueMinMax
	 * 						ArrayList of the value min and max taken by each Attribute of data.
	 * @param numberToDel
	 * 						The number of Instance of data close to inst to delete.
	 * 
	 * @return The new data without the numberToDel closest Instance of inst.
	 */
	public static Instances deleteClosestInstance (Instances data, Instance inst, ArrayList<Double> valueMinMax, int numberToDel)
	{
		Instances newData = data;		
		for(int i=0; i<numberToDel && newData.numInstances()>numberToDel+5 ; i++)
		{
			Instance instToDel = Weka_ManageInstances.getClosestInstance (newData, inst, valueMinMax);
			newData = Weka_ManageInstances.deleteInstance(newData, instToDel);
		}		
		return newData;
	}
}
