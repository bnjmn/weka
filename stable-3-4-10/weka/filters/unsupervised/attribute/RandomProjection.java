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
 *    RandomProjection.java
 *    Copyright (C) 2003 Ashraf M. Kibriya
 *
 */

package weka.filters.unsupervised.attribute;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.SelectedTag;
import weka.core.SparseInstance;
import weka.core.Tag;
import weka.core.Utils;
import weka.filters.Filter;
import weka.filters.UnsupervisedFilter;

import java.util.Enumeration;
import java.util.Random;
import java.util.Vector;

/** 
 * Reduces the dimensionality of the data by projecting 
 * it onto a lower dimensional subspace using a random 
 * matrix with columns of unit length (It will reduce 
 * the number of attributes in the data while preserving 
 * much of its variation like PCA, but at a much less
 * computational cost). <br>
 * It first applies the  NominalToBinary filter to 
 * convert all attributes to numeric before reducing the
 * dimension. It preserves the class attribute.
 *
 * <p> Valid filter-specific options are: <p>
 *
 * -N num <br>
 * The number of dimensions (attributes) the data should
 * be reduced to (default 10; exclusive of the class attribute, if it is set).
 * <p>
 *
 * -P percent <br>
 * The percentage of dimensions (attributes) the data should
 * be reduced to  (exclusive of the class attribute, if it is set). This 
 * -N option is ignored if this option is present or is greater 
 * than zero.<p>
 *
 * -D distribution num <br>
 * The distribution to use for calculating the random
 * matrix.<br>
 * <ul>
 * <li> 1 - Sparse distribution of: (default) <br>
 *      sqrt(3)*{+1 with prob(1/6), 0 with prob(2/3), -1 with prob(1/6)}</li>
 * <li> 2 - Sparse distribution of: <br>
 *      {+1 with prob(1/2), -1 with prob(1/2)}</li>
 * <li> 3 - Gaussian distribution </li>
 * </ul>
 *
 * -M <br>
 * Replace missing values using the ReplaceMissingValues filter <p>
 *
 * -R num <br>
 * Specify the random seed for the random number generator for
 * calculating the random matrix (default 42). <p>
 *
 * @author Ashraf M. Kibriya (amk14@cs.waikato.ac.nz) 
 * @version $Revision: 1.3.2.2 $ [1.0 - 22 July 2003 - Initial version (Ashraf M.
 *          Kibriya)]
 */
public class RandomProjection extends Filter implements UnsupervisedFilter, OptionHandler {


  /** Stores the number of dimensions to reduce the data to */
  private int m_k=10;

  /** Stores the dimensionality the data should be reduced to as percentage of the original dimension */
  private double m_percent=0.0;

  /** Is the random matrix will be computed using 
      Gaussian distribution or not */
  private boolean m_useGaussian=false;
 
  /** The types of distributions that can be used for 
      calculating the random matrix */
  public static final int SPARSE1=1, SPARSE2=2, GAUSSIAN=3;

  public static final Tag [] TAGS_DSTRS_TYPE = {
    new Tag(SPARSE1, "Sparse 1"),
    new Tag(SPARSE2, "Sparse 2"),
    new Tag(GAUSSIAN, "Gaussian"),
  };

  /** Stores the distribution to use for calculating the
      random matrix */
  private int m_distribution=SPARSE1;

 
  /** Should the missing values be replaced using 
      unsupervised.ReplaceMissingValues filter */
  private boolean m_replaceMissing=false;

  /** Keeps track of output format if it is defined or not */
  private boolean m_OutputFormatDefined=false;

  /** The NominalToBinary filter applied to the data before this filter */
  private Filter ntob; // = new weka.filters.unsupervised.attribute.NominalToBinary();

  /** The ReplaceMissingValues filter */
  private Filter replaceMissing;
    
  /** Stores the random seed used to generate the random matrix */
  private long m_rndmSeed=42;


  /** The random matrix */
  private double rmatrix[][];

  /** The random number generator used for generating the random matrix */
  private Random r;


  /**
   * Returns an enumeration describing the available options.
   *
   * @return an enumeration of all the available options.
   */
  public Enumeration listOptions() {

    Vector newVector = new Vector(2);

    newVector.addElement(new Option(
	      "\tThe number of dimensions (attributes) the data should be reduced to\n"
             +"\t(default 10; exclusive of the class attribute, if it is set).",
	      "N", 1, "-N <number>"));

    newVector.addElement(new Option(
	      "\tThe distribution to use for calculating the random matrix.\n"
	     +"\tSparse1 is:\n"
	     +"\t  sqrt(3)*{-1 with prob(1/6), 0 with prob(2/3), +1 with prob(1/6)}\n"
	     +"\tSparse2 is:\n"
	     +"\t  {-1 with prob(1/2), +1 with prob(1/2)}\n",
	      "D", 1, "-D [SPARSE1|SPARSE2|GAUSSIAN]"));

    //newVector.addElement(new Option(
    //	      "\tUse Gaussian distribution for calculating the random matrix.",
    //	      "G", 0, "-G"));

    newVector.addElement(new Option(
	      "\tThe percentage of dimensions (attributes) the data should\n"
	     +"\tbe reduced to (exclusive of the class attribute, if it is set). This -N\n"
	     +"\toption is ignored if this option is present or is greater\n"
	     +"\tthan zero.",
	      "P", 1, "-P <percent>"));

    newVector.addElement(new Option(
	      "\tReplace missing values using the ReplaceMissingValues filter",
	      "M", 0, "-M"));

    newVector.addElement(new Option(
	      "\tThe random seed for the random number generator used for\n"
	     +"\tcalculating the random matrix (default 42).",
	      "R", 0, "-R <num>"));
 
    return newVector.elements();
  }

  /**
   * Parses the options for this object. Valid options are: <p>
   *
   * -N num <br>
   * The number of dimensions (attributes) the data should
   * be reduced to (exclusive of the class attribute). <p>
   *
   * -P percent <br>
   * The percentage of dimensions (attributes) the data should
   * be reduced to  (exclusive of the class attribute). This 
   * -N option is ignored if this option is present or is greater 
   * than zero.<p>
   *
   * -D distribution num <br>
   * The distribution to use for calculating the random
   * matrix.<br>
   * <ul>
   * <li> 1 - Sparse distribution of: (default) <br>
   *      sqrt(3)*{+1 with prob(1/6), 0 with prob(2/3), -1 with prob(1/6)}</li>
   * <li> 2 - Sparse distribution of: <br>
   *      {+1 with prob(1/2), -1 with prob(1/2)}</li>
   * <li> 3 - Gaussian distribution </li>
   * </ul>
   *
   * -M <br>
   * Replace missing values using the ReplaceMissingValues filter <p>
   *
   * -R num <br>
   * Specify the random seed for the random number generator for
   * calculating the random matrix. <p>
   * 
   * @param options the list of options as an array of strings
   * @exception Exception if an option is not supported
   */
  public void setOptions(String[] options) throws Exception {


    String mString = Utils.getOption('P', options);
    if (mString.length() != 0) {
	setPercent(Double.parseDouble(mString)); //setNumberOfAttributes((int) Integer.parseInt(mString));
    } else {
        setPercent(0.0);
	mString = Utils.getOption('N', options);
	if (mString.length() != 0) 
	    setNumberOfAttributes(Integer.parseInt(mString));	    
	else	    
	    setNumberOfAttributes(10);
    }    
    
    mString = Utils.getOption('R', options);
    if(mString.length()!=0) {
	setRandomSeed( Long.parseLong(mString) );
    }

    mString = Utils.getOption('D', options);
    if(mString.length()!=0) {
	if(mString.equalsIgnoreCase("sparse1"))
	   setDistribution( new SelectedTag(SPARSE1, TAGS_DSTRS_TYPE) );
	else if(mString.equalsIgnoreCase("sparse2"))
	   setDistribution( new SelectedTag(SPARSE2, TAGS_DSTRS_TYPE) );
	else if(mString.equalsIgnoreCase("gaussian"))
	   setDistribution( new SelectedTag(GAUSSIAN, TAGS_DSTRS_TYPE) );	   
    }

    if(Utils.getFlag('M', options))
	setReplaceMissingValues(true);
    else
	setReplaceMissingValues(false);


   //if(Utils.getFlag('G', options))
   //    setUseGaussian(true);
   //else
   //    setUseGaussian(false);
    
  }

  /**
   * Gets the current settings of the filter.
   *
   * @return an array of strings suitable for passing to setOptions
   */
  public String [] getOptions() {

    String [] options = new String [10];
    int current = 0;

    //if (getUseGaussian()) {
    //  options[current++] = "-G";
    //}

    if (getReplaceMissingValues()) {
      options[current++] = "-M";
    }

    if (getPercent() == 0) {
      options[current++] = "-N";
      options[current++] = "" + getNumberOfAttributes();
    }
    else {
      options[current++] = "-P";
      options[current++] = "" + getPercent();
    }
    
    options[current++] = "-R";
    options[current++] = "" + getRandomSeed();
    
    SelectedTag t = getDistribution();
    options[current++] = "-D";
    options[current++] = ""+t.getSelectedTag().getReadable();


    while (current < options.length) {
      options[current++] = "";
    }

    return options;
  }
    
   
  /**
   * Returns a string describing this filter
   *
   * @return a description of the filter suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {

    return "Reduces the dimensionality of the data by projecting"
	 + " it onto a lower dimensional subspace using a random"
	 + " matrix with columns of unit length (i.e. It will reduce"
	 + " the number of attributes in the data while preserving"
	 + " much of its variation like PCA, but at a much less"
	 + " computational cost).\n"
	 + "It first applies the  NominalToBinary filter to" 
	 + " convert all attributes to numeric before reducing the"
	 + " dimension. It preserves the class attribute.";
  }


  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String numberOfAttributesTipText() {

    return "The number of dimensions (attributes) the data should"
         + " be reduced to.";
  }

  /** Sets the number of attributes (dimensions) the data should be reduced to */
  public void setNumberOfAttributes(int  newAttNum) {
      m_k = newAttNum;
  }
  
  /** 
   *  Gets the current number of attributes (dimensionality) to which the data 
   *  will be reduced to.
   */
  public int getNumberOfAttributes() {
      return m_k;
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String percentTipText() {

      return  " The percentage of dimensions (attributes) the data should"
            + " be reduced to  (inclusive of the class attribute). This "
	    + " NumberOfAttributes option is ignored if this option is"
	    + " present or is greater than zero.";
  }

  /** Sets the percent the attributes (dimensions) of the data should be reduced to */
  public void setPercent(double newPercent) {
      if(newPercent > 0)
	  newPercent /= 100;
      m_percent = newPercent;
  }

  /** Gets the percent the attributes (dimensions) of the data will be reduced to */
  public double getPercent() {
      return m_percent * 100;
  }


  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String randomSeedTipText() {
      return  "The random seed used by the random"
	     +" number generator used for generating"
	     +" the random matrix ";
  }

  /** Sets the random seed of the random number generator */
  public void setRandomSeed(long seed) {
      m_rndmSeed = seed;
  }

  /** Gets the random seed of the random number generator */
  public long getRandomSeed() {
      return m_rndmSeed;
  }


  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String  distributionTipText() {
      return "The distribution to use for calculating the random matrix.\n"
	    +"Sparse1 is:\n"
	    +" sqrt(3) * { -1 with prob(1/6), \n"
	    +"               0 with prob(2/3),  \n"
            +"              +1 with prob(1/6) } \n"
	    +"Sparse2 is:\n"
	    +" { -1 with prob(1/2), \n"
	    +"   +1 with prob(1/2) } ";
      
  }
  /** Sets the distribution to use for calculating the random matrix */
  public void setDistribution(SelectedTag newDstr) {

      if (newDstr.getTags() == TAGS_DSTRS_TYPE) {
	  m_distribution = newDstr.getSelectedTag().getID();
      }
  }

  /** Returns the current distribution that'll be used for calculating the 
     random matrix */
  public SelectedTag getDistribution() {
      return new SelectedTag(m_distribution, TAGS_DSTRS_TYPE);
  }

  /**
   * Returns the tip text for this property
   *
   * @return tip text for this property suitable for
   * displaying in the explorer/experimenter gui
   */
  public String replaceMissingValuesTipText() {

    return "If set the filter uses weka.filters.unsupervised.attribute.ReplaceMissingValues"
	 + " to replace the missing values";
  }

  /** 
   * Sets either to use replace missing values filter or not
   */
  public void setReplaceMissingValues(boolean t) {
      m_replaceMissing = t;
  }

  /** Gets the current setting for using ReplaceMissingValues filter */
  public boolean getReplaceMissingValues() {
      return m_replaceMissing;
  }

  /**
   * Sets the format of the input instances.
   *
   * @param instanceInfo an Instances object containing the input 
   * instance structure (any instances contained in the object are 
   * ignored - only the structure is required).
   * @return true if the outputFormat may be collected immediately
   * @exception Exception if the input format can't be set 
   * successfully
   */
  public boolean setInputFormat(Instances instanceInfo) throws Exception {      
    super.setInputFormat(instanceInfo);
    /*
    if (instanceInfo.classIndex() < 0) {
      throw new UnassignedClassException("No class has been assigned to the instances");
    }
    */
    
    for(int i=0; i<instanceInfo.numAttributes(); i++) {        
	if( i!=instanceInfo.classIndex() && instanceInfo.attribute(i).isNominal() ) {
            if(instanceInfo.classIndex()>=0)
                ntob = new weka.filters.supervised.attribute.NominalToBinary();
            else
                ntob = new weka.filters.unsupervised.attribute.NominalToBinary();
            
            break;
	}
    }

    //r.setSeed(m_rndmSeed); //in case the setRandomSeed() is not
                           //called we better set the seed to its 
                           //default value of 42.
    boolean temp=true;
    if(replaceMissing!=null) {
	replaceMissing = new weka.filters.unsupervised.attribute.ReplaceMissingValues();
	if(replaceMissing.setInputFormat(instanceInfo))
	    temp=true;
	else
	    temp=false;
    }
    
    if(ntob!=null) {
	if(ntob.setInputFormat(instanceInfo)) {
	    setOutputFormat();
	    return temp && true;
	}
	else { 
	    return false;
	}
    }
    else {
	setOutputFormat();
	return temp && true;
    }
  }

   
  /**
   * Input an instance for filtering.
   *
   * @param instance the input instance
   * @return true if the filtered instance may now be
   * collected with output().
   * @exception IllegalStateException if no input format has been set
   */
  public boolean input(Instance instance) throws Exception {

    Instance newInstance=null;

    if (getInputFormat()==null) {
	throw new IllegalStateException("No input instance format defined");
    }
    if(m_NewBatch) {
      resetQueue();
      //if(ntob!=null) 
      //	  ntob.m_NewBatch=true;
      m_NewBatch = false;
    }
    
    boolean replaceDone=false;
    if(replaceMissing!=null) {
	if(replaceMissing.input(instance)) {
	    if(m_OutputFormatDefined == false)
		setOutputFormat();
	    newInstance = replaceMissing.output();
	    replaceDone = true;
	}
	else
	    return false;;
    }

    if(ntob!=null) {
	if(replaceDone==false)
	    newInstance = instance;
	if(ntob.input(newInstance)) {
	    if(m_OutputFormatDefined == false) 
		setOutputFormat();
	    newInstance = ntob.output();
	    newInstance = convertInstance(newInstance);
	    push(newInstance);
	    return true;	
	}
	else {
	    return false;
	}
    }
    else {
	if(replaceDone==false)
	    newInstance = instance;
	newInstance = convertInstance(newInstance);
	push(newInstance);
	return true;
    }
  }


  /**
   * Signify that this batch of input to the filter is finished.
   *
   * @return true if there are instances pending output
   * @exception NullPointerException if no input structure has been defined,
   * @exception Exception if there was a problem finishing the batch.
   */
  public boolean batchFinished() throws Exception {
      if (getInputFormat() == null) {
	  throw new NullPointerException("No input instance format defined");
      }
      
      boolean conversionDone=false;
      if(replaceMissing!=null) {
	  if(replaceMissing.batchFinished()) {
	      Instance newInstance, instance;
	      
	      while((instance=replaceMissing.output())!=null) {
		  if(!m_OutputFormatDefined)
		      setOutputFormat();
		  if(ntob!=null) {
		      ntob.input(instance);
		  }
		  else {
		      newInstance = convertInstance(instance);
		      push(newInstance);
		  }
	      }

	      if(ntob!=null) {
		  if(ntob.batchFinished()) {
		      //Instance newInstance, instance;
		      while((instance=ntob.output())!=null) {
			  if(!m_OutputFormatDefined)
			      setOutputFormat();
			  newInstance = convertInstance(instance);
			  push(newInstance);
		      }
		      ntob = null;		      
		  }
	      }
	      replaceMissing = null;
	      conversionDone=true;
	  }
      }

      if(conversionDone==false && ntob!=null) {
	  if(ntob.batchFinished()) {
	      Instance newInstance, instance;
	      while((instance=ntob.output())!=null) {
		  if(!m_OutputFormatDefined)
		      setOutputFormat();
		  newInstance = convertInstance(instance);
		  push(newInstance);
	      }
	      ntob = null;
	  }
      }
      m_OutputFormatDefined=false;
      return super.batchFinished();
  }
    

  /** Sets the output format */  
  private void setOutputFormat() {
      Instances currentFormat;
      if(ntob!=null) {
	  currentFormat = ntob.getOutputFormat();
      }
      else 
	  currentFormat = getInputFormat();
      
      if(m_percent>0)
	  { m_k = (int) ((getInputFormat().numAttributes()-1)*m_percent); 
	  // System.out.print("numAtts: "+currentFormat.numAttributes());
	  // System.out.print("percent: "+m_percent);
	  // System.out.print("percent*numAtts: "+(currentFormat.numAttributes()*m_percent));
	  // System.out.println("m_k: "+m_k);
	  }

      Instances newFormat;
      int newClassIndex=-1;
      FastVector attributes = new FastVector();
      for(int i=0; i<m_k; i++) {
	  attributes.addElement( new Attribute("K"+(i+1)) );
      }
      if(currentFormat.classIndex()!=-1)  {  //if classindex is set
	  //attributes.removeElementAt(attributes.size()-1);
	  attributes.addElement(currentFormat.attribute(currentFormat.classIndex()));
	  newClassIndex = attributes.size()-1;
      }

      newFormat = new Instances(currentFormat.relationName(), attributes, 0);
      if(newClassIndex!=-1)
	  newFormat.setClassIndex(newClassIndex);
      m_OutputFormatDefined=true;

      r = new Random();
      r.setSeed(m_rndmSeed);

      rmatrix = new double[m_k][currentFormat.numAttributes()];
      if(m_distribution==GAUSSIAN) {
	  for(int i=0; i<rmatrix.length; i++) 
	      for(int j=0; j<rmatrix[i].length; j++) 
		  rmatrix[i][j] = r.nextGaussian();
      }
      else {
	  boolean useDstrWithZero = (m_distribution==SPARSE1);
	  for(int i=0; i<rmatrix.length; i++) 
	      for(int j=0; j<rmatrix[i].length; j++) 
		  rmatrix[i][j] = rndmNum(useDstrWithZero);
      }

      setOutputFormat(newFormat);
  }


  /** converts a single instance to the required format */
  private Instance convertInstance(Instance currentInstance) {
      
      Instance newInstance;
      double vals[] = new double[getOutputFormat().numAttributes()];
      int classIndex = (ntob==null) ? getInputFormat().classIndex():ntob.getOutputFormat().classIndex();
      int attNum = m_k;
      //double d = Math.sqrt(1D/attNum);
      
      for(int i=0; i<attNum; i++) {
	  boolean ismissing=false;
	  for(int j=0; j<currentInstance.numValues(); j++) {
	      if(classIndex!=-1 && j==classIndex) //ignore the class value for now
		  continue;
	      if(!currentInstance.isMissing(j)) {
		  vals[i] += rmatrix[i][j] * currentInstance.value(j);
	      }
	      //else {
	      //  ismissing=true;
	      //  vals[i] = currentInstance.missingValue();
	      //  break;
	      //}
	  }
	  //if(ismissing)
	  //    break;
      }
      if(classIndex!=-1) {
	  vals[m_k] = currentInstance.value(classIndex);
      }

      if(currentInstance instanceof SparseInstance) {
	  newInstance = new SparseInstance(currentInstance.weight(), vals);
      }
      else {
	  newInstance = new Instance(currentInstance.weight(), vals);
      }
      newInstance.setDataset(getOutputFormat());
      
      return newInstance;
  }



  private static final int weights[] = {1, 1, 4};
  private static final int vals[] = {-1, 1, 0};
  private static final int weights2[] = {1, 1};
  private static final int vals2[] = {-1, 1};
  private static final double sqrt3 = Math.sqrt(3);

  /**
     returns a double x such that
     x = sqrt(3) * { -1 with prob. 1/6, 0 with prob. 2/3, 1 with prob. 1/6 }
   */
  private double rndmNum(boolean useDstrWithZero) {
      if(useDstrWithZero)
	  return sqrt3 * vals[weightedDistribution(weights)];
      else
	  return vals2[weightedDistribution(weights2)];
  }

  /** Calculates a weighted distribution */
  private int weightedDistribution(int [] weights) {
      int sum=0; 
      
      for(int i=0; i<weights.length; i++) 
	  sum += weights[i];
      
      int val = (int)Math.floor(r.nextDouble()*sum);
      
      for(int i=0; i<weights.length; i++) {
	  val -= weights[i];
	  if(val<0)
	      return i;
      }
      return -1;
  }  

  /**
   * Main method for testing this class.
   *
   * @param argv should contain arguments to the filter: 
   * use -h for help
   */
  public static void main(String [] argv) {

    try {
      if (Utils.getFlag('b', argv)) {
 	Filter.batchFilterFile(new RandomProjection(), argv);
      } else {
	Filter.filterFile(new RandomProjection(), argv);
      }
    } catch (Exception ex) {
      System.out.println(ex.getMessage());
    }
  }

}
