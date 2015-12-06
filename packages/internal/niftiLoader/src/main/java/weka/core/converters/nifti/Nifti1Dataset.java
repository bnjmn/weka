package weka.core.converters.nifti;

import java.io.*;
import java.nio.*;
import java.text.*;
import java.util.*;
import java.util.zip.*;

	/** 
	* Nifti1Dataset is an API for reading/writing nifti-1 datasets.<br>
	* The definitive nifti-1 specification is the nifti1.h file on the
	* <a href="http://nifti.nimh.nih.gov/dfwg/">NIfTI DFWG website </a>
	*
	* <p>Notes: 
	* <br>Compression: files compressed with gzip are read using the Java 
	*	gunzip utility 
	*
	* <p>Endian-ness: Java uses big-endian by default.  nifti files 
	* determined to be little-endian as per the nifti spec (dim[0] field) 
	* will have the header and data swapped on read.  (Not sure about
	* write yet...) 
	*
	* <p>Datatypes: the nifti-1 format supports a large number of 
	* datatypes.  The current read methods typecast all data to doubles.  
	* This API will be extended to include methods to return image 	blobs 
	* of different datatypes.
	*
	*
        * <h3>Todo List</h3>
        * <ul>
	*
	* <li>
	* This API currently uses the traditional Java java.io methods.
	* Will explore performance gains by using the java.nio methods, 
	* including possibly memory mapping.  I think some of the new
	* java.io methods such as readFully() use nio anyway.
	* </li>
	*
	* <li>
	* For header read and write, should change to read/write fields
	* from/to 348 byte blob, and read/write blob from disk in 1 call. 
	* </li>
	*
	* <li>
	* Currently the writeHeader() method writes from the lower
	* level variables, not the "shortcut" variables ie pixdim[0] not qfac, 
	* dim[1] not XDIM, etc. need to think about this, use accessor methods
	* </li>
	*
	* <li>
	* Will need to change read/writeVol methods to call read/write
	* slice methds b/c blob_size has to be an int but should be a long.
	* 32 bit int only allows max volume 1625^3 which is too small.
	* </li>
	*
	* <li>
	* Add methods to manipulate qform/sform and roatation matrices
	* and quaternion representations
	* </li>
	*
	* <li>
	* Add simple method to make a Java AWT image from a slice
	* </li>
	*
	* <li>
	* <br>Possible extensions to this API:
        * 	Extend this API to be compatible with Analyze7.5 format ?
	* </li>
	* </ul>
	*
	*
	*
	* <h3>History of changes to Nifti1Dataset</h3>
	* <ul>
	* <li>
	* 3/1/2005 KF  Alpha version put into SourceForge CVS
	* </li>
	* <li>
	* 10/2005 KF  added copyHeader() routine
	* </li>
	* <li>
	* 2/20/2006 KF  Bug fix in readHeader() for endian setting, thanks to 
	* Jason Dai.
	* </li>
	* <li>
	* 3/2006 KF added a little code to readNiiExt() to check if extensions 
	* overrun image data.
	* </li>
	* </ul>
	*
	*/

public class Nifti1Dataset {

	//////////////////////////////////////////////////////////////////
	//
	//		Nifti-1 Defines        
	//
	//////////////////////////////////////////////////////////////////
	public static final String ANZ_HDR_EXT = ".hdr";
	public static final String ANZ_DAT_EXT = ".img";
	public static final String NI1_EXT = ".nii";
	public static final String GZIP_EXT = ".gz";
	public static final int ANZ_HDR_SIZE = 348;
	public static final long NII_HDR_SIZE = 352;
	public static final int  EXT_KEY_SIZE = 8;   // esize+ecode
	public static final String NII_MAGIC_STRING = "n+1";
	public static final String ANZ_MAGIC_STRING = "ni1";


	//////////////////////////////////////////////////////////////////
	//
	//		Nifti-1 Codes from C nifti1.h
	//
	//////////////////////////////////////////////////////////////////

	// intent codes for field intent_code
	public static final short NIFTI_INTENT_NONE        = 0;
	public static final short NIFTI_INTENT_CORREL      = 2;
	public static final short NIFTI_INTENT_TTEST       = 3;
	public static final short NIFTI_INTENT_FTEST       = 4;
	public static final short NIFTI_INTENT_ZSCORE      = 5;
	public static final short NIFTI_INTENT_CHISQ       = 6;
	public static final short NIFTI_INTENT_BETA        = 7;
	public static final short NIFTI_INTENT_BINOM       = 8;
	public static final short NIFTI_INTENT_GAMMA       = 9;
	public static final short NIFTI_INTENT_POISSON    = 10;
	public static final short NIFTI_INTENT_NORMAL     = 11;
	public static final short NIFTI_INTENT_FTEST_NONC = 12;
	public static final short NIFTI_INTENT_CHISQ_NONC = 13;
	public static final short NIFTI_INTENT_LOGISTIC   = 14;
	public static final short NIFTI_INTENT_LAPLACE    = 15;
	public static final short NIFTI_INTENT_UNIFORM    = 16;
	public static final short NIFTI_INTENT_TTEST_NONC = 17;
	public static final short NIFTI_INTENT_WEIBULL    = 18;
	public static final short NIFTI_INTENT_CHI        = 19;
	public static final short NIFTI_INTENT_INVGAUSS   = 20;
	public static final short NIFTI_INTENT_EXTVAL     = 21;
	public static final short NIFTI_INTENT_PVAL       = 22;
	public static final short NIFTI_INTENT_ESTIMATE  = 1001;
	public static final short NIFTI_INTENT_LABEL     = 1002;
	public static final short NIFTI_INTENT_NEURONAME = 1003;
	public static final short NIFTI_INTENT_GENMATRIX = 1004;
	public static final short NIFTI_INTENT_SYMMATRIX = 1005;
	public static final short NIFTI_INTENT_DISPVECT  = 1006;
	public static final short NIFTI_INTENT_VECTOR    = 1007;
	public static final short NIFTI_INTENT_POINTSET  = 1008;
	public static final short NIFTI_INTENT_TRIANGLE  = 1009;
	public static final short NIFTI_INTENT_QUATERNION =  1010;
	public static final short NIFTI_FIRST_STATCODE	= 2;
	public static final short NIFTI_LAST_STATCODE		= 22;


	// datatype codes for field datatype
	public static final short DT_NONE                    = 0;
	public static final short DT_BINARY                  = 1;
	public static final short NIFTI_TYPE_UINT8           = 2;
	public static final short NIFTI_TYPE_INT16           = 4;
	public static final short NIFTI_TYPE_INT32           = 8;
	public static final short NIFTI_TYPE_FLOAT32        = 16;
	public static final short NIFTI_TYPE_COMPLEX64      = 32;
	public static final short NIFTI_TYPE_FLOAT64        = 64;
	public static final short NIFTI_TYPE_RGB24         = 128;
	public static final short DT_ALL                   = 255;
	public static final short NIFTI_TYPE_INT8          = 256;
	public static final short NIFTI_TYPE_UINT16        = 512;
	public static final short NIFTI_TYPE_UINT32        = 768;
	public static final short NIFTI_TYPE_INT64        = 1024;
	public static final short NIFTI_TYPE_UINT64       = 1280;
	public static final short NIFTI_TYPE_FLOAT128     = 1536;
	public static final short NIFTI_TYPE_COMPLEX128   = 1792;
	public static final short NIFTI_TYPE_COMPLEX256   = 2048;

	// units codes for xyzt_units
	public static final short NIFTI_UNITS_UNKNOWN = 0;
	public static final short NIFTI_UNITS_METER   = 1;
	public static final short NIFTI_UNITS_MM      = 2;
	public static final short NIFTI_UNITS_MICRON  = 3;
	public static final short NIFTI_UNITS_SEC     = 8;
	public static final short NIFTI_UNITS_MSEC   = 16;
	public static final short NIFTI_UNITS_USEC   = 24;
	public static final short NIFTI_UNITS_HZ     = 32;
	public static final short NIFTI_UNITS_PPM    = 40;

	// slice order codes for slice_code
	public static final short NIFTI_SLICE_SEQ_INC =  1;
	public static final short NIFTI_SLICE_SEQ_DEC =  2;
	public static final short NIFTI_SLICE_ALT_INC =  3;
	public static final short NIFTI_SLICE_ALT_DEC =  4;

	// codes for qform_code sform_code
	public static final short NIFTI_XFORM_UNKNOWN      = 0;
	public static final short NIFTI_XFORM_SCANNER_ANAT = 1;
	public static final short NIFTI_XFORM_ALIGNED_ANAT = 2;
	public static final short NIFTI_XFORM_TALAIRACH    = 3;
	public static final short NIFTI_XFORM_MNI_152      = 4;


	//////////////////////////////////////////////////////////////////
	//
	//		public variables for nifti1 datasets
	//		eventually these should be non-public and settable
	//		only thru accessor methods
	//
	//////////////////////////////////////////////////////////////////
	// public variables derived from header info and filenames
	String 		ds_hdrname;	// file name for header
	String 		ds_datname;	// file name for data
	public boolean 	ds_is_nii;	// does dataset use single file .nii 
	public boolean		big_endian;	// does hdr appear to have BE format
	public short		XDIM,YDIM,ZDIM,TDIM,DIM5,DIM6,DIM7;	// from dim[] field
	public short		freq_dim,phase_dim,slice_dim;  // unpack dim_info
	public short		xyz_unit_code, t_unit_code;	// unpack xyzt_units;
	public short		qfac;				// unpack pixdim[0]
	Vector		extensions_list;		// vector of size/code pairs for ext.
	Vector		extension_blobs;		// vector of extension data

	// variables for fields in the nifti header 
	public int		sizeof_hdr;	// must be 348 bytes
	public StringBuffer	data_type_string;	// 10 char UNUSED
	public StringBuffer	db_name;	// 18 char UNUSED
	public int		extents;	// UNUSED
	public short		session_error;	// UNUSED
	public StringBuffer	regular;	// 1 char UNUSED
	public StringBuffer	dim_info;	// 1 char MRI slice ordering
	short		dim[];		// data array dimensions (8 shorts)
	public float		intent[];	// intents p1 p2 p3
	public short		intent_code;	// nifti intent code for dataset
	short		datatype;	// datatype of image blob
	short		bitpix;		// #bits per voxel
	public short		slice_start;	// first slice index
	public float		pixdim[];	// grid spacings
	public float		vox_offset;	// offset to data blob in .nii file
	public float		scl_slope;	// data scaling: slope
	public float		scl_inter;	// data scaling: intercept
	public short		slice_end;	// last slice index
	public byte		slice_code;	// slice timing order
	public byte		xyzt_units;	// units of pixdim[1-4]
	public float		cal_max;	// max display intensity
	public float		cal_min;	// min display intensity
	public float		slice_duration;	// time to acq. 1 slice
	public float		toffset;	// time axis shift
	public int		glmax;		// UNUSED
	public int		glmin;		// UNUSED
	public StringBuffer	descrip;	// 80 char any text you'd like (comment)
	public StringBuffer	aux_file;	// 24 char auxiliary file name
	public short		qform_code;	// code for quat. transform
	public short		sform_code;	// code for affine transform
	public float		quatern[];	// 3 floats Quaternion b,c,d params
	public float		qoffset[];	// 3 floats Quaternion x,y,z shift
	public float		srow_x[];	// 4 floats 1st row affine xform
	public float		srow_y[];	// 4 floats 2nd row affine xform
	public float		srow_z[];	// 4 floats 3rd row affine xform
	public StringBuffer	intent_name;	// 16 char name/meaning/id for data
	public StringBuffer	magic;		// 4 char id must be "ni1\0" or "n+1\0"
	public byte		extension[];	// 4 byte array, byte 0 is 0/1 indicating extensions or not




	//////////////////////////////////////////////////////////////////
	/**
	* Constructor for a dataset existing on disk.
	* @param name - name of the nifti-1 dataset.  The name can have
	*	the .hdr, .img, or .nii extension, optionally followed by a .gz
	*	compression suffix.  Or, the name can be specified with no 
	*	extensions or suffixes, in which case the program will look in
	* 	this order for files with these extensions: .hdr (.gz) .img (.gz)
	*	.nii (.gz)
	*/
	public Nifti1Dataset(String name) {

		setDefaults();
		checkName(name);

	return;
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Constructor for creation of a new dataset.  Default values are
	* set, programmer must set or reset all fields appropriate for
	* the new dataset.
	*
	*/
	public Nifti1Dataset() {

		setDefaults();

	return;
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Read header information into memory
	* @exception IOException 
	* @exception FileNotFoundException
	*/
	public void readHeader() throws IOException, FileNotFoundException {

		DataInputStream dis;
		EndianCorrectInputStream ecs;
		short s, ss[];
		byte bb[];
		int i;

		if (ds_hdrname.endsWith(".gz"))
			dis = new DataInputStream(new GZIPInputStream(new FileInputStream(ds_hdrname)));
		else
			dis = new DataInputStream(new FileInputStream(ds_hdrname));
		try {

		///// first, read dim[0] to get endian-ness
		dis.skipBytes(40);  
		s = dis.readShort();
		dis.close();
		if ((s < 1) || (s > 7))
			big_endian = false;
		else
			big_endian = true;


		///// get input stream that will flip bytes if necessary 
		if (ds_hdrname.endsWith(".gz"))
			ecs = new EndianCorrectInputStream(new GZIPInputStream(new FileInputStream(ds_hdrname)),big_endian);
		else
			ecs = new EndianCorrectInputStream(ds_hdrname,big_endian);

		sizeof_hdr = ecs.readIntCorrect();

		bb = new byte[10];
		ecs.readFully(bb,0,10);
		data_type_string = new StringBuffer(new String(bb));

		bb = new byte[18];
		ecs.readFully(bb,0,18);
		db_name = new StringBuffer(new String(bb));

		extents = ecs.readIntCorrect();

		session_error = ecs.readShortCorrect();
			
		regular = new StringBuffer();
		regular.append((char)(ecs.readUnsignedByte()));
		
		dim_info = new StringBuffer();
		dim_info.append((char)(ecs.readUnsignedByte()));
		ss = unpackDimInfo((int)dim_info.charAt(0));
		freq_dim = ss[0];
		phase_dim = ss[1];
		slice_dim = ss[2];

		for (i=0; i<8; i++)
			dim[i] = ecs.readShortCorrect();
		if (dim[0] > 0)
			XDIM = dim[1];
		if (dim[0] > 1)
			YDIM = dim[2];
		if (dim[0] > 2)
			ZDIM = dim[3];
		if (dim[0] > 3)
			TDIM = dim[4];

		for (i=0; i<3; i++)
			intent[i] = ecs.readFloatCorrect();

		intent_code = ecs.readShortCorrect();

		datatype = ecs.readShortCorrect();

		bitpix = ecs.readShortCorrect();

		slice_start = ecs.readShortCorrect();

		for (i=0; i<8; i++)
			pixdim[i] = ecs.readFloatCorrect();
		qfac = (short) Math.floor((double)(pixdim[0]));
		
		vox_offset = ecs.readFloatCorrect();

		scl_slope = ecs.readFloatCorrect();
		scl_inter = ecs.readFloatCorrect();

		slice_end = ecs.readShortCorrect();

		slice_code = (byte) ecs.readUnsignedByte();

		xyzt_units = (byte) ecs.readUnsignedByte();
		ss = unpackUnits((int)xyzt_units);
		xyz_unit_code =  ss[0];
		t_unit_code =  ss[1];

		cal_max = ecs.readFloatCorrect();
		cal_min = ecs.readFloatCorrect();

		slice_duration = ecs.readFloatCorrect();

		toffset = ecs.readFloatCorrect();
		
		glmax = ecs.readIntCorrect();
		glmin = ecs.readIntCorrect();

		bb = new byte[80];
		ecs.readFully(bb,0,80);
		descrip = new StringBuffer(new String(bb));

		bb = new byte[24];
		ecs.readFully(bb,0,24);
		aux_file = new StringBuffer(new String(bb));

		qform_code = ecs.readShortCorrect();
		sform_code = ecs.readShortCorrect();

		for (i=0; i<3; i++)
			quatern[i] = ecs.readFloatCorrect();
		for (i=0; i<3; i++)
			qoffset[i] = ecs.readFloatCorrect();

		for (i=0; i<4; i++)
			srow_x[i] = ecs.readFloatCorrect();
		for (i=0; i<4; i++)
			srow_y[i] = ecs.readFloatCorrect();
		for (i=0; i<4; i++)
			srow_z[i] = ecs.readFloatCorrect();


		bb = new byte[16];
		ecs.readFully(bb,0,16);
		intent_name = new StringBuffer(new String(bb));

		bb = new byte[4];
		ecs.readFully(bb,0,4);
		magic = new StringBuffer(new String(bb));

		}
		catch (IOException ex) {
			throw new IOException("Error: unable to read header file "+ds_hdrname+": "+ex.getMessage());
		}
		

		/////// Read possible extensions
		if (ds_is_nii) 
			readNiiExt(ecs);
		else
			readNp1Ext(ecs);


	ecs.close();

	return;	
	}

	////////////////////////////////////////////////////////////////////
	//
	// Copy all in memory header field settings from datset A to this dataset
	// Extension data not set, fields set to no extension
	//
	////////////////////////////////////////////////////////////////////
	public void copyHeader(Nifti1Dataset A) {

	int i;

	ds_hdrname = 	new String(A.ds_hdrname);  
	ds_datname = 	new String(A.ds_datname);
	ds_is_nii = 	A.ds_is_nii;
	big_endian = 	A.big_endian;
	sizeof_hdr = 	A.sizeof_hdr;
	data_type_string = new StringBuffer(A.data_type_string.toString());
	db_name = new StringBuffer(A.db_name.toString());
	extents = 	A.extents;
	session_error =	A.session_error;
	regular = new StringBuffer(A.regular.toString());
	dim_info = new StringBuffer(A.dim_info.toString());
	freq_dim=A.freq_dim; 
	phase_dim=A.phase_dim; 
	slice_dim=A.slice_dim;
	for (i=0; i<8; i++)
		dim[i] = A.dim[i];
	XDIM=A.XDIM; YDIM=A.YDIM; ZDIM=A.ZDIM; TDIM=A.TDIM;
	DIM5=A.DIM5; DIM6=A.DIM6; DIM7=A.DIM7;
	for (i=0; i<3; i++)
		intent[i] = A.intent[i];
	intent_code = A.intent_code;
	datatype = A.datatype;
	bitpix = A.bitpix;	
	slice_start = A.slice_start;
	qfac = 1;
	for (i=0; i<8; i++)
		pixdim[i] = A.pixdim[i];
	
	vox_offset = A.vox_offset;
	scl_slope = A.scl_slope;
	scl_inter = A.scl_inter;
	slice_end = A.slice_end;
	slice_code = A.slice_code;
	xyzt_units = A.xyzt_units;
	xyz_unit_code = A.xyz_unit_code;
	t_unit_code = A.t_unit_code;

	cal_max = A.cal_max;
	cal_min = A.cal_min;
	slice_duration = A.slice_duration;
	toffset = A.toffset;
	glmax = A.glmax;
	glmin = A.glmin;

	descrip = new StringBuffer(A.descrip.toString());
	aux_file = new StringBuffer(A.aux_file.toString());

	qform_code = A.qform_code;
	sform_code = A.sform_code;

	for (i=0; i<3; i++) {
		quatern[i] = A.quatern[i];
		qoffset[i] = A.qoffset[i];
	}

	for (i=0; i<4; i++) {
		srow_x[i] = A.srow_x[i];
		srow_y[i] = A.srow_y[i];
		srow_z[i] = A.srow_z[i];
	}

	intent_name = new StringBuffer(A.intent_name.toString());

	magic = new StringBuffer(A.magic.toString());

	for (i=0; i<4; i++)
		extension[i] = (byte)0;

	return;
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Read extension data from nii (1 file) dataset
	* @param ecs is an InputStream open and pointing to begining of 
	* 	extensions array
	* @exception IOException 
	*/
	private void readNiiExt(EndianCorrectInputStream ecs) throws IOException {

		int size_code[];
		int start_addr;
		byte eblob[];

		// read 4 ext bytes if it is nii, bytes 348-351 must be there
		try {
			ecs.readFully(extension,0,4);
		}
		catch (IOException ex) {
			throw new IOException("Error: i/o error reading extension bytes on header file "+ds_hdrname+": "+ex.getMessage());
		}
		

		/// jump thru extensions getting sizes and codes
		size_code = new int[2];
		if ( extension[0] != (byte) 0 ) {
			start_addr=ANZ_HDR_SIZE+4;

			size_code[0] = 0;
			size_code[1] = 0;

			/// in nii files, vox_offset is end of hdr/ext,
			/// beginning of data.  
			while (start_addr < (int) vox_offset) {
				try {
				size_code = new int[2];
				size_code[0] = ecs.readIntCorrect();
				size_code[1] = ecs.readIntCorrect();
				eblob = new byte[size_code[0]-EXT_KEY_SIZE];
				ecs.readFully(eblob,0,size_code[0]-EXT_KEY_SIZE);
				extension_blobs.add(eblob);
				}
				catch (IOException ex) {
					printHeader();
					throw new EOFException("Error: i/o error reading extension data for extension "+(extensions_list.size()+1)+" on header file "+ds_hdrname+": "+ex.getMessage());
				}

				extensions_list.add(size_code);
				start_addr += (size_code[0]);

				// check if extensions appeared to overrun data blob
				// when extensions are done, start_addr should == vox_offset
				if (start_addr > (int) vox_offset) {
					printHeader();
					throw new IOException("Error: Data  for extension "+(extensions_list.size())+" on header file "+ds_hdrname+" appears to overrun start of image data.");
				}
			} // while not yet at data blob

		}	// if there are extensions


	return;
	}

	//////////////////////////////////////////////////////////////////
	////
	/*
	* Read extension data from n+1 (2 file) dataset
	* @param ecs is an InputStream open and pointing to begining of 
	* 	extensions array
	* @exception IOException 
	*/
	private void readNp1Ext(EndianCorrectInputStream ecs) throws IOException, EOFException {

		int size_code[];
		byte eblob[];


		// read 4 ext bytes if it is n+1, bytes 348-351 do NOT
		// need to be there
		try {
			ecs.readFully(extension,0,4);
		}
		catch (EOFException ex) {
			return;
		}
		catch (IOException ex) {
			throw new IOException("Error: i/o error reading extension bytes on header file "+ds_hdrname+": "+ex.getMessage());
		}
		

		/// jump thru extensions getting sizes and codes
		size_code = new int[2];
		if ( extension[0] != (byte) 0 ) {

			size_code[0] = 0;
			size_code[1] = 0;

			/// in np1 files, read to end of hdr file looking
			/// for extensions
			while (true) {
				try {
				size_code = new int[2];
				size_code[0] = ecs.readIntCorrect();
				size_code[1] = ecs.readIntCorrect();
				eblob = new byte[size_code[0]-EXT_KEY_SIZE];
				ecs.readFully(eblob,0,size_code[0]-EXT_KEY_SIZE);
				extension_blobs.add(eblob);
				}
				catch (EOFException ex) {
					return;
				}
				catch (IOException ex) {
					throw new EOFException("Error: i/o error reading extension data for extension "+(extensions_list.size()+1)+" on header file "+ds_hdrname+": "+ex.getMessage());
				}

				extensions_list.add(size_code);
			} // while not yet at EOF

		}	// if there are extensions


	return;
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Get list of extensions and return it as nx2 array
        * @return nx2 array where n = # of extensions, array elem 0
	* is the size in bytes of that extension and array elem 1 is
	* the extension code.
	*/
	public int[][] getExtensionsList() {

		int n,i;
		int size_code[];
		int extlist[][];

		size_code = new int[2];
		n = extensions_list.size();
		extlist = new int[n][2];
		

		for (i=0; i<n; i++) {
			size_code = (int[]) extensions_list.get(i);
			extlist[i][0] = size_code[0];
			extlist[i][1] = size_code[1];
		}

		return(extlist);
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Remove an extension from a header
	* @param index number of the extension to remove (0 based)
	*/
	public void removeExtension(int index) {

		int n;
		int size_code[] = new int[2];

		n = extensions_list.size();

		if (index >= n) {
			System.out.println("\nERROR: could not remove extension "+index+1+" from "+ds_hdrname+". It only has "+n+" extensions.");
			return;
		}

		// remove extension from lists
		size_code = (int[]) extensions_list.get(index);
		extensions_list.remove(index);
		extension_blobs.remove(index);

		// readjust vox_offset if necessary
		if (ds_is_nii)
			vox_offset -= size_code[0];

	return;
	}

	//////////////////////////////////////////////////////////////////
	/**
	* Add an extension stored in a file to a header
	* @param code -- code identifying the extension
	* @param filename -- filename containing the extension.  The entire
	* file will be added as an extension
	*/
	public void addExtension(int code, String filename) throws IOException {

		File f;
		long l;
		int size_code[] = new int[2];
		DataInputStream dis;
		byte b[];
		int i, il, pad;

		f = new File(filename);
		l = f.length();

		//// check length, compute padding
		//// 8bytes of size+code plus ext data must be mult. of 16
		if (l > Integer.MAX_VALUE) {
			throw new IOException("Error: maximum extension size is "+Integer.MAX_VALUE+"bytes. "+filename+" is "+l+" bytes.");
		}
		il = (int)l;
		pad =  (il+EXT_KEY_SIZE)%16;
		if (pad != 0)
			pad = 16-pad;
		///System.out.println("\next file size is "+l+", padding with "+pad);

		/// read the extension data from the file
		b = new byte[il+pad];
		try {
		dis = new DataInputStream(new FileInputStream(filename));
		dis.readFully(b,0,il);
		dis.close();
		}
		catch(IOException ex) {
			throw new IOException("Error reading extension data for "+ds_hdrname+" from file "+filename+". :"+ex.getMessage());
		}
		for(i=il; i<il+pad; i++)
			b[i] = 0;


		/// well, if we got this far, I guess we really have to add it
		size_code[0] = il+pad+EXT_KEY_SIZE;
		size_code[1] = code;
		extensions_list.add(size_code);
		extension_blobs.add(b);
		extension[0] = 1;

		// update vox_offset for nii files
		if (ds_is_nii)
			vox_offset += size_code[0];

	return;
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Write header information to disk file
	* @exception IOException 
	* @exception FileNotFoundException
	*/
	public void writeHeader() throws IOException, FileNotFoundException {

		EndianCorrectOutputStream ecs;
		ByteArrayOutputStream baos;
		FileOutputStream fos;
		short s, ss[];
		byte b, bb[], ext_blob[];
		int hsize;
		int i,n;
		int extlist[][];


		// header is 348 except nii and anz/hdr w/ extensions is 352
		hsize = ANZ_HDR_SIZE;
		if ( (ds_is_nii) || (extension[0] != 0) )
			hsize += 4;

		try {

		baos = new ByteArrayOutputStream(hsize);
		fos = new FileOutputStream(ds_hdrname);

		ecs = new EndianCorrectOutputStream(baos,big_endian);


		ecs.writeIntCorrect(sizeof_hdr);

		if (data_type_string.length() >= 10) {
			ecs.writeBytes(data_type_string.substring(0,10));
		}
		else {
			ecs.writeBytes(data_type_string.toString());
			for (i=0; i<(10-data_type_string.length()); i++)
				ecs.writeByte(0);
		}

		if (db_name.length() >= 18) {
			ecs.writeBytes(db_name.substring(0,18));
		}
		else {
			ecs.writeBytes(db_name.toString());
			for (i=0; i<(18-db_name.length()); i++)
				ecs.writeByte(0);
		}

		ecs.writeIntCorrect(extents);

		ecs.writeShortCorrect(session_error);

		ecs.writeByte((int) regular.charAt(0));

		b = packDimInfo(freq_dim, phase_dim, slice_dim);
		ecs.writeByte((int) b);

		for (i=0; i<8; i++)
			ecs.writeShortCorrect(dim[i]);

		for (i=0; i<3; i++)
			ecs.writeFloatCorrect(intent[i]);
			
		ecs.writeShortCorrect(intent_code);

		ecs.writeShortCorrect(datatype);

		ecs.writeShortCorrect(bitpix);

		ecs.writeShortCorrect(slice_start);
		
		for (i=0; i<8; i++)
			ecs.writeFloatCorrect(pixdim[i]);


		ecs.writeFloatCorrect(vox_offset);

		ecs.writeFloatCorrect(scl_slope);
		ecs.writeFloatCorrect(scl_inter);

		ecs.writeShortCorrect(slice_end);

		ecs.writeByte((int)slice_code);

		ecs.writeByte((int)packUnits(xyz_unit_code,t_unit_code));


		ecs.writeFloatCorrect(cal_max);
		ecs.writeFloatCorrect(cal_min);

		ecs.writeFloatCorrect(slice_duration);

		ecs.writeFloatCorrect(toffset);
		
		ecs.writeIntCorrect(glmax);
		ecs.writeIntCorrect(glmin);

		ecs.write(setStringSize(descrip,80),0,80);
		ecs.write(setStringSize(aux_file,24),0,24);


		ecs.writeShortCorrect(qform_code);
		ecs.writeShortCorrect(sform_code);

		for (i=0; i<3; i++)
			ecs.writeFloatCorrect(quatern[i]);
		for (i=0; i<3; i++)
			ecs.writeFloatCorrect(qoffset[i]);

		for (i=0; i<4; i++)
			ecs.writeFloatCorrect(srow_x[i]);
		for (i=0; i<4; i++)
			ecs.writeFloatCorrect(srow_y[i]);
		for (i=0; i<4; i++)
			ecs.writeFloatCorrect(srow_z[i]);


		ecs.write(setStringSize(intent_name,16),0,16);
		ecs.write(setStringSize(magic,4),0,4);


		// nii or anz/hdr w/ ext. gets 4 more
		if ( (ds_is_nii) || (extension[0] != 0) ) {
			for (i=0; i<4; i++)
				ecs.writeByte((int)extension[i]);
		}

		/** write the header blob to disk */
		baos.writeTo(fos);

		}
		catch (IOException ex) {
			throw new IOException("Error: unable to write header file "+ds_hdrname+": "+ex.getMessage());
		}



		/** write the extension blobs **/
		try {

		////// extensions
		if (extension[0] != 0) {

			baos = new ByteArrayOutputStream(EXT_KEY_SIZE);
			ecs = new EndianCorrectOutputStream(baos,big_endian);
			extlist = getExtensionsList();
			n = extlist.length;
			for(i=0; i<n; i++) {
				// write size, code
				ecs.writeIntCorrect(extlist[i][0]);
				ecs.writeIntCorrect(extlist[i][1]);
				baos.writeTo(fos);
				baos.reset();

				// write data blob
				ext_blob = (byte[]) extension_blobs.get(i);
				fos.write(ext_blob,0,extlist[i][0]-EXT_KEY_SIZE);
			}
		}

		fos.close();
		}

		catch (IOException ex) {
			throw new IOException("Error: unable to write header extensions for file "+ds_hdrname+": "+ex.getMessage());
		}

	return;	
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Print header information to standard out.
	*/
	public void printHeader() {

		int i;
		int extlist[][], n;
	
		System.out.println("\n");
		System.out.println("Dataset header file:\t\t\t\t"+ds_hdrname);
		System.out.println("Dataset data file:\t\t\t\t"+ds_datname);
		System.out.println("Size of header:\t\t\t\t\t"+sizeof_hdr);
		System.out.println("File offset to data blob:\t\t\t"+vox_offset);

		System.out.print("Endianness:\t\t\t\t\t");
		if (big_endian)
			System.out.println("big");
		else
			System.out.println("little");

		System.out.println("Magic filetype string:\t\t\t\t"+magic);



		///// Dataset datatype, size, units
		System.out.println("Datatype:\t\t\t\t\t"+datatype+" ("+decodeDatatype(datatype)+")");
		System.out.println("Bits per voxel:\t\t\t\t\t"+bitpix);
		System.out.println("Scaling slope and intercept:\t\t\t"+scl_slope+" "+scl_inter);


		System.out.print("Dataset dimensions (Count, X,Y,Z,T...):\t\t");
		for (i=0; i<=dim[0]; i++)
			System.out.print(dim[i]+" ");
		System.out.println("");

		System.out.print("Grid spacings (X,Y,Z,T,...):\t\t\t");
		for (i=1; i<=dim[0]; i++)
			System.out.print(pixdim[i]+" ");
		System.out.println("");

		System.out.println("XYZ  units:\t\t\t\t\t"+xyz_unit_code+" ("+decodeUnits(xyz_unit_code)+")");
		System.out.println("T units:\t\t\t\t\t"+t_unit_code+" ("+decodeUnits(t_unit_code)+")");
		System.out.println("T offset:\t\t\t\t\t"+toffset);


		System.out.print("Intent parameters:\t\t\t\t");
		for (i=0; i<3; i++)
			System.out.print(intent[i]+" ");
		System.out.println("");
		System.out.println("Intent code:\t\t\t\t\t"+intent_code+" ("+decodeIntent(intent_code)+")");

		System.out.println("Cal. (display) max/min:\t\t\t\t"+cal_max+" "+cal_min);


		///// Slice order/timing stuff
		System.out.println("Slice timing code:\t\t\t\t"+slice_code+" ("+decodeSliceOrder((short)slice_code)+")");
		System.out.println("MRI slice ordering (freq, phase, slice index):\t"+freq_dim+" "+phase_dim+" "+slice_dim);

		System.out.println("Start/end slice:\t\t\t\t"+slice_start+" "+slice_end);
		System.out.println("Slice duration:\t\t\t\t\t"+slice_duration);


		///// Orientation stuff
		System.out.println("Q factor:\t\t\t\t\t"+qfac);
		System.out.println("Qform transform code:\t\t\t\t"+qform_code+" ("+decodeXform(qform_code)+")");
		System.out.println("Quaternion b,c,d params:\t\t\t"+quatern[0]+" "+quatern[1]+" "+quatern[2]);
		System.out.println("Quaternion x,y,z shifts:\t\t\t"+qoffset[0]+" "+qoffset[1]+" "+qoffset[2]);

		System.out.println("Affine transform code:\t\t\t\t"+sform_code+" ("+decodeXform(sform_code)+")");
		System.out.print("1st row affine transform:\t\t\t");
		for (i=0; i<4; i++)
			System.out.print(srow_x[i]+" ");
		System.out.println("");
		System.out.print("2nd row affine transform:\t\t\t");
		for (i=0; i<4; i++)
			System.out.print(srow_y[i]+" ");
		System.out.println("");
		System.out.print("3rd row affine transform:\t\t\t");
		for (i=0; i<4; i++)
			System.out.print(srow_z[i]+" ");
		System.out.println("");


		///// comment stuff
		System.out.println("Description:\t\t\t\t\t"+descrip);
		System.out.println("Intent name:\t\t\t\t\t"+intent_name);
		System.out.println("Auxiliary file:\t\t\t\t\t"+aux_file);
		System.out.println("Extension byte 1:\t\t\t\t\t"+(int)extension[0]);


		///// unused stuff
		System.out.println("\n\nUnused Fields");
		System.out.println("----------------------------------------------------------------------");
		System.out.println("Data type string:\t\t\t"+data_type_string);
		System.out.println("db_name:\t\t\t\t\t"+db_name);
		System.out.println("extents:\t\t\t\t\t"+extents);
		System.out.println("session_error:\t\t\t\t\t"+session_error);
		System.out.println("regular:\t\t\t\t\t"+regular);
		System.out.println("glmax/glmin:\t\t\t\t\t"+glmax+" "+glmin);
		System.out.println("Extension bytes 2-4:\t\t\t\t"+(int)extension[1]+" "+(int)extension[2]+" "+(int)extension[3]);
		
		
		////// extensions
		if (extension[0] != 0) {
			extlist = getExtensionsList();
			n = extlist.length;
			System.out.println("\n\nExtensions");
			System.out.println("----------------------------------------------------------------------");
			System.out.println("#\tCode\tSize");
			for(i=0; i<n; i++)
				System.out.println((i+1)+"\t"+extlist[i][1]+"\t"+extlist[i][0]);
			System.out.println("\n");
		}


	return;
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Print a voxel timecourse to standard out.
	* @param d 1D double array of timecourse values, length TDIM
	*/
	public void printDoubleTmcrs(double d[]) {

		short i;
		NumberFormat nf;

		nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(6);
		nf.setGroupingUsed(false);

		for (i=0; i<TDIM; i++)
			System.out.println(nf.format(d[i]));

	return;
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Check if a valid dataset (header+data) exists.  Note that
	* some operations (e.g. header editing) do not actually require
	* that a data file exist.  Use existsHdr() existsDat() in those cases.
	* Gzipped files with .gz extension are permitted.
	* @return true if header and data file exist, else false
	*/
	public boolean exists() {
		return (existsHdr() && existsDat());
	}

	/**
	* Check if a valid dataset header file exists.
	* @return true if header file exist, else false
	*/
	public boolean existsHdr() {
		File f;
		f = new File(ds_hdrname);
		if (f.exists())
			return true;
		f = new File(ds_hdrname+GZIP_EXT);
		if (f.exists())
			return true;
	return(false);
	}

	/**
	* Check if a valid dataset data file exists.
	* @return true if data file exist, else false
	*/
	public boolean existsDat() {
		File f;
		f = new File(ds_datname);
		if (f.exists())
			return true;
		f = new File(ds_datname+GZIP_EXT);
		if (f.exists())
			return true;
	return(false);
	}

	//////////////////////////////////////////////////////////////////
	/**
	* Set the filename for the dataset header file
	* @param s filename for the dataset header file
	*/
	public void setHeaderFilename(String s) {
		
		if (s.endsWith(NI1_EXT))
			setToNii();
		else
			setToNi1();

		ds_hdrname = s;
		if (ds_is_nii) {
			if (! ds_hdrname.endsWith(NI1_EXT)) 
				ds_hdrname = ds_hdrname + NI1_EXT;
		}
		else {
			if (! ds_hdrname.endsWith(ANZ_HDR_EXT))
				ds_hdrname = ds_hdrname + ANZ_HDR_EXT;
		}
	return;
	}

	//////////////////////////////////////////////////////////////////
	/**
	* Get the filename for the dataset header file
	* @return String with the disk filename for the dataset header file
	*/
	public String getHeaderFilename() {
		return(ds_hdrname);
	}



	//////////////////////////////////////////////////////////////////
	/**
	* Set the filename for the dataset data file
	* @param s filename for the dataset data file
	*/
	public void setDataFilename(String s) {
		ds_datname = s;
		if (ds_is_nii) {
			if (! ds_datname.endsWith(NI1_EXT))
				ds_datname = ds_datname + NI1_EXT;
		}
		else {
			if (! ds_datname.endsWith(ANZ_DAT_EXT))
				ds_datname = ds_datname + ANZ_DAT_EXT;
		}

	return;
	}

	//////////////////////////////////////////////////////////////////
	/**
	* Get the filename for the dataset data file
	* @return  filename for the dataset data file
	*/
	public String getDataFilename() {
		return(ds_datname);
	}


	//////////////////////////////////////////////////////////////////
	/*
	* Set fields to make this a nii (n+1) (single file) dataset
	* switching from nii to anz/hdr affects
	* -- magic field "n+1\0" not "ni1\0"
	* -- vox_offset must include 352+extensions
	* -- internal ds_is_nii flag
	*
	* NOTE: all changes are in memory, app still needs to
	* write header and data for change to occur on disk
	*
	* maybe add auto set of dat name to hdr name, strip img/hdr ??
	*/
	private void setToNii() {
		int i,n;
		int extlist[][];

		ds_is_nii = true;
		magic = new StringBuffer(NII_MAGIC_STRING); 

		vox_offset = NII_HDR_SIZE;
		if (extension[0] != 0) {
			extlist = getExtensionsList();
			n = extlist.length;
			for(i=0; i<n; i++)
				vox_offset += extlist[i][0];
		}
	return;
	}


	//////////////////////////////////////////////////////////////////
	/*
	* Set fields to make this a ni1 (2 file img/hdr) dataset
	* switching from nii to anz/hdr affects
	* -- magic field "n+1\0" vs "ni1\0"
	* -- vox_offset does notinclude 352+extensions
	* -- internal ds_is_nii flag
	*
	* NOTE: all changes are in memory, app still needs to
	* write header and data for change to occur on disk
	*
	* maybe add auto set of dat name to hdr name, strip img/hdr ??
	*/
	private void setToNi1() {
		int n;
		int extlist[][];

		ds_is_nii = false;
		magic = new StringBuffer(ANZ_MAGIC_STRING); 

		// if there was stuff after header before data it cannot
		// survive transition from nii to ni1
		vox_offset = 0;

	return;
	}



	//////////////////////////////////////////////////////////////////
	/**
	* Set the dataset dimensions
	*/
	public void setDims(short a, short x, short y, short z, short t, short d5, short d6, short d7) {

		dim[0] = a;
		dim[1] = x;
		dim[2] = y;
		dim[3] = z;
		dim[4] = t;
		dim[5] = d5;
		dim[6] = d6;
		dim[7] = d7;
		
		XDIM = x;
		YDIM = y;
		ZDIM = z;
		TDIM = t;

		return;
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Set the dataset datatype.  (bitpix will also be set accordingly.)
	* @param code nifti-1 datatype code
	*/
	public void setDatatype(short code) {
		datatype = code;
		bitpix = (short)(bytesPerVoxel(code)*8);
	return;
	}

	//////////////////////////////////////////////////////////////////
	/**
	* Get the dataset datatype.  
	* @return  datatype code (note: it is not guaranteed to be a valid
	* code, what is there is what you get...)
	*/
	public short getDatatype() {
		return(datatype);
	}
	
	//////////////////////////////////////////////////////////////////
	/**
	* Get the bitpix field
	* @return  bitpix: number of bits per pixel
	*/
	public short getBitpix() {
		return(bitpix);
	}
	

	//////////////////////////////////////////////////////////////////
	/**
	* Decode the nifti intent codes                            
	* @param icode nifti intent code
	* @return a terse string describing the intent
	*/
	public String decodeIntent(short icode) {

		switch(icode) {

			case NIFTI_INTENT_NONE:
				return("NIFTI_INTENT_NONE");
			case NIFTI_INTENT_CORREL:
				return("NIFTI_INTENT_CORREL");
			case NIFTI_INTENT_TTEST:
				return("NIFTI_INTENT_TTEST");
			case NIFTI_INTENT_FTEST:
				return("NIFTI_INTENT_FTEST");
			case NIFTI_INTENT_ZSCORE:
				return("NIFTI_INTENT_ZSCORE");
			case NIFTI_INTENT_CHISQ:
				return("NIFTI_INTENT_CHISQ");
			case NIFTI_INTENT_BETA:
				return("NIFTI_INTENT_BETA");
			case NIFTI_INTENT_BINOM:
				return("NIFTI_INTENT_BINOM");
			case NIFTI_INTENT_GAMMA:
				return("NIFTI_INTENT_GAMMA");
			case NIFTI_INTENT_POISSON:
				return("NIFTI_INTENT_POISSON");
			case NIFTI_INTENT_NORMAL:
				return("NIFTI_INTENT_NORMAL");
			case NIFTI_INTENT_FTEST_NONC:
				return("NIFTI_INTENT_FTEST_NONC");
			case NIFTI_INTENT_CHISQ_NONC:
				return("NIFTI_INTENT_CHISQ_NONC");
			case NIFTI_INTENT_LOGISTIC:
				return("NIFTI_INTENT_LOGISTIC");
			case NIFTI_INTENT_LAPLACE:
				return("NIFTI_INTENT_LAPLACE");
			case NIFTI_INTENT_UNIFORM:
				return("NIFTI_INTENT_UNIFORM");
			case NIFTI_INTENT_TTEST_NONC:
				return("NIFTI_INTENT_TTEST_NONC");
			case NIFTI_INTENT_WEIBULL:
				return("NIFTI_INTENT_WEIBULL");
			case NIFTI_INTENT_CHI:
				return("NIFTI_INTENT_CHI");
			case NIFTI_INTENT_INVGAUSS:
				return("NIFTI_INTENT_INVGAUSS");
			case NIFTI_INTENT_EXTVAL:
				return("NIFTI_INTENT_EXTVAL");
			case NIFTI_INTENT_PVAL:
				return("NIFTI_INTENT_PVAL");
			case NIFTI_INTENT_ESTIMATE:
				return("NIFTI_INTENT_ESTIMATE");
			case NIFTI_INTENT_LABEL:
				return("NIFTI_INTENT_LABEL");
			case NIFTI_INTENT_NEURONAME:
				return("NIFTI_INTENT_NEURONAME");
			case NIFTI_INTENT_GENMATRIX:
				return("NIFTI_INTENT_GENMATRIX");
			case NIFTI_INTENT_SYMMATRIX:
				return("NIFTI_INTENT_SYMMATRIX");
			case NIFTI_INTENT_DISPVECT:
				return("NIFTI_INTENT_DISPVECT");
			case NIFTI_INTENT_VECTOR:
				return("NIFTI_INTENT_VECTOR");
			case NIFTI_INTENT_POINTSET:
				return("NIFTI_INTENT_POINTSET");
			case NIFTI_INTENT_TRIANGLE:
				return("NIFTI_INTENT_TRIANGLE");
			case NIFTI_INTENT_QUATERNION:
				return("NIFTI_INTENT_QUATERNION");
			default:
				return("INVALID_NIFTI_INTENT_CODE");
			}
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Decode the nifti datatype codes                            
	* @param dcode nifti datatype code
	* @return a terse string describing the datatype
	*/
	public String decodeDatatype(short dcode) {

		switch(dcode) {

		case DT_NONE:
			return("DT_NONE");
		case DT_BINARY:
			return("DT_BINARY");
		case NIFTI_TYPE_UINT8:
			return("NIFTI_TYPE_UINT8");
		case NIFTI_TYPE_INT16:
			return("NIFTI_TYPE_INT16");
		case NIFTI_TYPE_INT32:
			return("NIFTI_TYPE_INT32");
		case NIFTI_TYPE_FLOAT32:
			return("NIFTI_TYPE_FLOAT32");
		case NIFTI_TYPE_COMPLEX64:
			return("NIFTI_TYPE_COMPLEX64");
		case NIFTI_TYPE_FLOAT64:
			return("NIFTI_TYPE_FLOAT64");
		case NIFTI_TYPE_RGB24:
			return("NIFTI_TYPE_RGB24");
		case DT_ALL:
			return("DT_ALL");
		case NIFTI_TYPE_INT8:
			return("NIFTI_TYPE_INT8");
		case NIFTI_TYPE_UINT16:
			return("NIFTI_TYPE_UINT16");
		case NIFTI_TYPE_UINT32:
			return("NIFTI_TYPE_UINT32");
		case NIFTI_TYPE_INT64:
			return("NIFTI_TYPE_INT64");
		case NIFTI_TYPE_UINT64:
			return("NIFTI_TYPE_UINT64");
		case NIFTI_TYPE_FLOAT128:
			return("NIFTI_TYPE_FLOAT128");
		case NIFTI_TYPE_COMPLEX128:
			return("NIFTI_TYPE_COMPLEX128");
		case NIFTI_TYPE_COMPLEX256:
			return("NIFTI_TYPE_COMPLEX256");
		default:
			return("INVALID_NIFTI_DATATYPE_CODE");
		}
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Return bytes per voxel for each nifti-1 datatype           
	* @param dcode nifti datatype code
	* @return a short with number of bytes per voxel, 0 for unknown,
	*  -1 for 1 bit
	*/
	public short bytesPerVoxel(short dcode) {

		switch(dcode) {

		case DT_NONE:
			return(0);
		case DT_BINARY:
			return(-1);
		case NIFTI_TYPE_UINT8:
			return(1);
		case NIFTI_TYPE_INT16:
			return(2);
		case NIFTI_TYPE_INT32:
			return(4);
		case NIFTI_TYPE_FLOAT32:
			return(4);
		case NIFTI_TYPE_COMPLEX64:
			return(8);
		case NIFTI_TYPE_FLOAT64:
			return(8);
		case NIFTI_TYPE_RGB24:
			return(3);
		case DT_ALL:
			return(0);
		case NIFTI_TYPE_INT8:
			return(1);
		case NIFTI_TYPE_UINT16:
			return(2);
		case NIFTI_TYPE_UINT32:
			return(4);
		case NIFTI_TYPE_INT64:
			return(8);
		case NIFTI_TYPE_UINT64:
			return(8);
		case NIFTI_TYPE_FLOAT128:
			return(16);
		case NIFTI_TYPE_COMPLEX128:
			return(16);
		case NIFTI_TYPE_COMPLEX256:
			return(32);
		default:
			return(0);
		}
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Decode the nifti slice order codes                            
	* @param code nifti slice order code
	* @return a terse string describing the slice order
	*/
	public String decodeSliceOrder(short code) {

		switch(code) {

			case NIFTI_SLICE_SEQ_INC:
				return("NIFTI_SLICE_SEQ_INC");
			case NIFTI_SLICE_SEQ_DEC:
				return("NIFTI_SLICE_SEQ_DEC");
			case NIFTI_SLICE_ALT_INC:
				return("NIFTI_SLICE_ALT_INC");
			case NIFTI_SLICE_ALT_DEC:
				return("NIFTI_SLICE_ALT_DEC");
			default:
				return("INVALID_NIFTI_SLICE_SEQ_CODE");
		}
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Decode the nifti xform codes                            
	* @param code nifti xform code
	* @return a terse string describing the coord. system
	*/
	public String decodeXform(short code) {

		switch(code) {
			case NIFTI_XFORM_UNKNOWN:
				return("NIFTI_XFORM_UNKNOWN");
			case NIFTI_XFORM_SCANNER_ANAT:
				return("NIFTI_XFORM_SCANNER_ANAT");
			case NIFTI_XFORM_ALIGNED_ANAT:
				return("NIFTI_XFORM_ALIGNED_ANAT");
			case NIFTI_XFORM_TALAIRACH:
				return("NIFTI_XFORM_TALAIRACH");
			case NIFTI_XFORM_MNI_152:
				return("NIFTI_XFORM_MNI_152");
			default:
				return("INVALID_NIFTI_XFORM_CODE");
		}
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Decode the nifti unit codes                            
	* @param code nifti units code
	* @return a terse string describing the unit
	*/
	public String decodeUnits(short code) {

		switch(code) {
			case NIFTI_UNITS_UNKNOWN:
				return("NIFTI_UNITS_UNKNOWN");
			case NIFTI_UNITS_METER:
				return("NIFTI_UNITS_METER");
			case NIFTI_UNITS_MM:
				return("NIFTI_UNITS_MM");
			case NIFTI_UNITS_MICRON:
				return("NIFTI_UNITS_MICRON");
			case NIFTI_UNITS_SEC:
				return("NIFTI_UNITS_SEC");
			case NIFTI_UNITS_MSEC:
				return("NIFTI_UNITS_MSEC");
			case NIFTI_UNITS_USEC:
				return("NIFTI_UNITS_USEC");
			case NIFTI_UNITS_HZ:
				return("NIFTI_UNITS_HZ");
			case NIFTI_UNITS_PPM:
				return("NIFTI_UNITS_PPM");
			default:
				return("INVALID_NIFTI_UNITS_CODE");
		}
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Check the header fields for valid settings
	* @return 0 if all checks are passed else, error code
	*/
	public short checkHeader() {
	

	// check that each dim claimed to be in use is not 0
	// check for negative voxel sizes
	// check for 348
	// check for magic code n+1 or ni1
	// check bitpix divisible by 8 and in sync with datatype

	return 0;
	}


	////////////////////////////////////////////////////////////////////
	//
	// check the input filename for anz/nii extensions, gz compression
	// extension so as to get the actual disk filenames for hdr and data
	// The purpose of this routine is to find the file(s) the user 
	// means, letting them specify with a variety of extensions, eg
	// they may or may not add the /img/hdr/nii extensions, may or may
	// not add the .gz extension.
	//
	// name: the string the user gave as input file
	//
	//
	////////////////////////////////////////////////////////////////////
	private void checkName(String name) {

		String wname;
		File f, nii_file, niig_file, anzh_file, anzhg_file, anzd_file, anzdg_file;

		wname = new String(name);


		// strip off .gz suffix, will determine gz by disk filenames
		if (wname.endsWith(GZIP_EXT))
			wname = wname.substring(0,wname.length()-GZIP_EXT.length());

		// get filenames if only basename is specified
		// note that the checkName routine is not checking whether a
		// valid dataset exists (use exists() for that).  But, if only
		// a basename was specified, we need to check for existing
		// files to see if they meant .anz .hdr .nii etc..
		if (!wname.endsWith(ANZ_HDR_EXT) && !wname.endsWith(ANZ_DAT_EXT) && !wname.endsWith(NI1_EXT) ) {
			// files to look for
			nii_file = new File(wname+NI1_EXT);	
			niig_file = new File(wname+NI1_EXT+GZIP_EXT);	
			anzh_file = new File(wname+ANZ_HDR_EXT);
			anzhg_file = new File(wname+ANZ_HDR_EXT+GZIP_EXT);
			anzd_file = new File(wname+ANZ_DAT_EXT);
			anzdg_file = new File(wname+ANZ_DAT_EXT+GZIP_EXT);
		
			if (nii_file.exists())
				wname = wname+NI1_EXT;
			else if (niig_file.exists())
				wname = wname+NI1_EXT;
			else if (anzh_file.exists())
				wname = wname+ANZ_HDR_EXT;
			else if (anzhg_file.exists())
				wname = wname+ANZ_HDR_EXT;
			else if (anzd_file.exists())
				wname = wname+ANZ_HDR_EXT;
			else if (anzdg_file.exists())
				wname = wname+ANZ_HDR_EXT;
		}

		///// if an anz or nii suffix is given, use that to find file names
		if (wname.endsWith(ANZ_HDR_EXT)) {
			ds_hdrname = new String(wname);
			ds_datname = wname.substring(0,wname.length()-ANZ_HDR_EXT.length())+ANZ_DAT_EXT;
		}

		else if (wname.endsWith(ANZ_DAT_EXT)) {
			ds_datname = new String(wname);
			ds_hdrname = wname.substring(0,wname.length()-ANZ_DAT_EXT.length())+ANZ_HDR_EXT;
		}

		else if (wname.endsWith(NI1_EXT)) {
			ds_datname = new String(wname);
			ds_hdrname = new String(wname);
			ds_is_nii = true;
		}


		/// add gz suffix if gzipped file exists
		f = new File(ds_hdrname+GZIP_EXT);
		if (f.exists())
			ds_hdrname = ds_hdrname+GZIP_EXT;
		f = new File(ds_datname+GZIP_EXT);
		if (f.exists())
			ds_datname = ds_datname+GZIP_EXT;
		
	return;
	}


	////////////////////////////////////////////////////////////////////
	//
	// Set default settings for all nifti1 header fields and some
	// other fields for this class
	//
	////////////////////////////////////////////////////////////////////
	private void setDefaults() {

	int i;
	ByteOrder bo;

	ds_hdrname = 	new String("");  
	ds_datname = 	new String("");
	ds_is_nii = 	false;		// use single file .nii format or not
	bo = ByteOrder.nativeOrder();
	if (bo == ByteOrder.BIG_ENDIAN)
		big_endian = 	true;	// endian flag, need to flip little end.
	else
		big_endian = 	false;	
	sizeof_hdr = 	ANZ_HDR_SIZE;		// must be 348 bytes
	data_type_string = new StringBuffer(); // 10 char UNUSED
	for (i=0; i<10; i++)
		data_type_string.append("\0");
	db_name = new StringBuffer();	// 18 char UNUSED
	for (i=0; i<18; i++)
		db_name.append("\0");
	extents = 	0;		// UNUSED
	session_error =	0;		// UNUSED
	regular = new StringBuffer("\0");	// UNUSED
	dim_info = new StringBuffer("\0");	// MRI slice ordering
	freq_dim=0; phase_dim=0; slice_dim=0;
	dim = new short[8];		// data array dimensions (8 shorts)
	for (i=0; i<8; i++)
		dim[i] = 0;
	XDIM=0; YDIM=0; ZDIM=0; TDIM=0;
	intent = new float[3];		// intents p1 p2 p3
	for (i=0; i<3; i++)
		intent[i] = (float)0.0;
	intent_code = NIFTI_INTENT_NONE;
	datatype = DT_NONE;		// datatype of image blob
	bitpix = 0;			// #bits per voxel
	slice_start = 0;		// first slice index
	pixdim = new float[8];		// grid spacings
	pixdim[0] = 1; qfac = 1;
	for (i=1; i<8; i++)
		pixdim[i] = (float)0.0;
	
	vox_offset = (float)0.0;	// offset to data blob in .nii file
					// for .nii files default is 352 but
					// at this point don't know filetype
	scl_slope = (float)0.0;		// data scaling: slope
	scl_inter = (float)0.0;		// data scaling: intercept
	slice_end = 0;			// last slice index
	slice_code = (byte) 0;		// slice timing order
	xyzt_units = (byte) 0;		// units of pixdim[1-4]
	xyz_unit_code = NIFTI_UNITS_UNKNOWN;
	t_unit_code = NIFTI_UNITS_UNKNOWN;

	cal_max = (float) 0.0;		// max display intensity
	cal_min = (float) 0.0;		// min display intensity
	slice_duration = (float) 0.0;	// time to acq. 1 slice
	toffset = (float) 0.0;		// time axis shift
	glmax = 0;			// UNUSED
	glmin = 0;			// UNUSED

	descrip = new StringBuffer();	// 80 char any text you'd like (comment)
	for (i=0; i<80; i++)
		descrip.append("\0");
	aux_file = new StringBuffer();	// 24 char auxiliary filename
	for (i=0; i<24; i++)
		aux_file.append("\0");


	qform_code = NIFTI_XFORM_UNKNOWN;	// code for quat. xform
	sform_code = NIFTI_XFORM_UNKNOWN;	// code for affine xform

	quatern = new float[3];		// 3 floats Quaternion b,c,d params
	qoffset = new float[3];		// 3 floats Quaternion x,y,z shift
	for (i=0; i<3; i++) {
		quatern[i] = (float)0.0;
		qoffset[i] = (float)0.0;
	}

	srow_x = new float[4];		// 4 floats 1st row affine xform
	srow_y = new float[4];		// 4 floats 2nd row affine xform
	srow_z = new float[4];		// 4 floats 3rd row affine xform
	for (i=0; i<4; i++) {
		srow_x[i] = (float)0.0;
		srow_y[i] = (float)0.0;
		srow_z[i] = (float)0.0;
	}

	intent_name = new StringBuffer(); // 16 char name/meaning/id for data
	for (i=0; i<16; i++)
		intent_name.append("\0");

	// defaulting to n+1 nii as per vox_offset 0
	magic = new StringBuffer(NII_MAGIC_STRING); // 4 char id must be "ni1\0" or "n+1\0"

	extension = new byte[4];	// 4 byte array, [0] is for extensions
	for (i=0; i<4; i++)
		extension[i] = (byte)0;

	extensions_list = new Vector(5);	// list of int[2] size/code pairs for exts.
	extension_blobs = new Vector(5);	// vector to hold data in each ext.

	return;
	}

	////////////////////////////////////////////////////////////////////
	//
	// Unpack/pack the 3 bitfields in the dim_info char                 
	//	bits 0,1 freq_dim
	//	bits 2,3 phase_dim
	//	bits 4,5 slice_dim
	//
	////////////////////////////////////////////////////////////////////
	private short[] unpackDimInfo(int b) {
		short s[];

		s = new short[3];
		s[0] = (short) (b & 3);
		s[1] = (short) ((b >>> 2) & 3);
		s[2] = (short) ((b >>> 4) & 3);
		return s;
	}
	private byte packDimInfo(short freq, short phase, short slice) {
		
		int i = 0;

		i = (i & ((int)(slice)&3)) << 2;
		i = (i & ((int)(phase)&3)) << 2;
		i = (i & ((int)(freq)&3));
		return ((byte)i);
	}


	////////////////////////////////////////////////////////////////////
	//
	// Unpack/pack the 2 bitfields in the xyzt_units field
	//	bits 0,1,2 are the code for units for xyz
	//	bits 3,4,5 are the code for units for t, no need to shift
	//	bits for t code, the code is a multiple of 8.
	//
	////////////////////////////////////////////////////////////////////
	private short[] unpackUnits(int b) {
		short s[];

		s = new short[2];
		s[0] = (short) (b & 007);
		s[1] = (short) (b & 070);
		return s;
	}

	private byte packUnits(short space, short time) {

		
		return( (byte) (((int) (space) & 007) | ((int)(time) & 070)) );
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Read one 3D volume from disk and return it as 3D double array
	* @param ttt T dimension of vol to read (0 based index)
	* @return 3D double array, scale and offset have been applied.
	* Array indices are [Z][Y][X], assuming an xyzt ordering of dimensions.
	* ie  indices are data[dim[3]][dim[2]][dim[1]]
	* @exception IOException
	* 
	*/
	public double[][][] readDoubleVol(short ttt) throws IOException {

	double data[][][];
	byte b[];
	EndianCorrectInputStream ecs;
	short ZZZ;
	int i,j,k;
	

	// for 2D volumes, zdim may be 0
	ZZZ = ZDIM;
	if (dim[0] == 2)
		ZZZ = 1;


	// allocate 3D array
	data = new double[ZZZ][YDIM][XDIM];

	// read bytes from disk
	b = readVolBlob(ttt);

	// read the correct datatypes from the byte array
	// undo signs if necessary, add scaling
	ecs = new EndianCorrectInputStream(new ByteArrayInputStream(b),big_endian);
	switch (datatype) {

		case NIFTI_TYPE_INT8:
		case NIFTI_TYPE_UINT8:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				data[k][j][i] = (double) (ecs.readByte());
				if ((datatype == NIFTI_TYPE_UINT8) && (data[k][j][i] < 0) )
					data[k][j][i] = Math.abs(data[k][j][i]) + (double)(1<<7);
				if (scl_slope != 0)
					data[k][j][i] = data[k][j][i] * scl_slope + scl_inter;
			}
			break;

		case NIFTI_TYPE_INT16:
		case NIFTI_TYPE_UINT16:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				data[k][j][i] = (double) (ecs.readShortCorrect());
				if ((datatype == NIFTI_TYPE_UINT16) && (data[k][j][i] < 0))
					data[k][j][i] = Math.abs(data[k][j][i]) + (double)(1<<15);
				if (scl_slope != 0)
					data[k][j][i] = data[k][j][i] * scl_slope + scl_inter;
			}
			break;

		case NIFTI_TYPE_INT32:
		case NIFTI_TYPE_UINT32:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				data[k][j][i] = (double) (ecs.readIntCorrect());
				if ( (datatype == NIFTI_TYPE_UINT32) && (data[k][j][i] < 0) )
					data[k][j][i] = Math.abs(data[k][j][i]) + (double)(1<<31);
				if (scl_slope != 0)
					data[k][j][i] = data[k][j][i] * scl_slope + scl_inter;
			}
			break;


		case NIFTI_TYPE_INT64:
		case NIFTI_TYPE_UINT64:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				data[k][j][i] = (double) (ecs.readLongCorrect());
				if ( (datatype == NIFTI_TYPE_UINT64) && (data[k][j][i] < 0) )
					data[k][j][i] = Math.abs(data[k][j][i]) + (double)(1<<63);
				if (scl_slope != 0)
					data[k][j][i] = data[k][j][i] * scl_slope + scl_inter;
			}
			break;


		case NIFTI_TYPE_FLOAT32:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				data[k][j][i] = (double) (ecs.readFloatCorrect());
				if (scl_slope != 0)
					data[k][j][i] = data[k][j][i] * scl_slope + scl_inter;
			}
			break;


		case NIFTI_TYPE_FLOAT64:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				data[k][j][i] = (double) (ecs.readDoubleCorrect());
				if (scl_slope != 0)
					data[k][j][i] = data[k][j][i] * scl_slope + scl_inter;
			}
			break;


		case DT_NONE:
		case DT_BINARY:
		case NIFTI_TYPE_COMPLEX64:
		case NIFTI_TYPE_FLOAT128:
		case NIFTI_TYPE_RGB24:
		case NIFTI_TYPE_COMPLEX128:
		case NIFTI_TYPE_COMPLEX256:
		case DT_ALL:
		default:
			throw new IOException("Sorry, cannot yet read nifti-1 datatype "+decodeDatatype(datatype));
		}

	ecs.close();
	b = null;


	return(data);
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Write one 3D double array to disk. Data is written in datatype
	* of this Nifti1Dataset instance.  Data is "un-scaled" as per 
	* scale/offset fields of this Nifti1Dataset instance, before being 
	* written to disk.
	* @param data 3D array of array data
	* Array indices are [Z][Y][X], assuming an xyzt ordering of dimensions.
	* ie  indices are data[dim[3]][dim[2]][dim[1]]
	* @param ttt T dimension of vol to write (0 based index)
	* @exception IOException
	* 
	*/
	public void writeVol(double data[][][], short ttt) throws IOException {
	
	short i,j,k;
	short ZZZ;
	int blob_size;
	EndianCorrectOutputStream ecs;
	ByteArrayOutputStream baos;

	// for 2D volumes, zdim may be 0
	ZZZ = ZDIM;
	if (dim[0] == 2)
		ZZZ = 1;

	blob_size = XDIM*YDIM*ZZZ*bytesPerVoxel(datatype);
	baos = new ByteArrayOutputStream(blob_size);
	ecs = new EndianCorrectOutputStream(baos,big_endian);


	switch (datatype) {

		case NIFTI_TYPE_INT8:
		case NIFTI_TYPE_UINT8:
		case NIFTI_TYPE_INT16:
		case NIFTI_TYPE_UINT16:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				if (scl_slope == 0)
					ecs.writeShortCorrect((short)(data[k][j][i]));
				else
					ecs.writeShortCorrect((short)((data[k][j][i] - scl_inter) / scl_slope));
			}
			break;


		case NIFTI_TYPE_INT32:
		case NIFTI_TYPE_UINT32:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				if (scl_slope == 0)
					ecs.writeIntCorrect((int)(data[k][j][i]));
				else
					ecs.writeIntCorrect((int)((data[k][j][i] - scl_inter) / scl_slope));
			}
			break;

		case NIFTI_TYPE_INT64:
		case NIFTI_TYPE_UINT64:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				if (scl_slope == 0)
					ecs.writeLongCorrect((long)Math.rint(data[k][j][i]));
				else
					ecs.writeLongCorrect((long)Math.rint((data[k][j][i] - scl_inter) / scl_slope));
			}
			break;
		case NIFTI_TYPE_FLOAT32:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				if (scl_slope == 0)
					ecs.writeFloatCorrect((float)(data[k][j][i]));
				else
					ecs.writeFloatCorrect((float)((data[k][j][i] - scl_inter) / scl_slope));
			}
			break;
		case NIFTI_TYPE_FLOAT64:
			for (k=0; k<ZZZ; k++)
			for (j=0; j<YDIM; j++)
			for (i=0; i<XDIM; i++) {
				if (scl_slope == 0)
					ecs.writeDoubleCorrect(data[k][j][i]);
				else
					ecs.writeDoubleCorrect((data[k][j][i] - scl_inter) / scl_slope);
			}
			break;




		case DT_NONE:
		case DT_BINARY:
		case NIFTI_TYPE_COMPLEX64:
		case NIFTI_TYPE_FLOAT128:
		case NIFTI_TYPE_RGB24:
		case NIFTI_TYPE_COMPLEX128:
		case NIFTI_TYPE_COMPLEX256:
		case DT_ALL:
		default:
			throw new IOException("Sorry, cannot yet write nifti-1 datatype "+decodeDatatype(datatype));

		}


		writeVolBlob(baos,ttt);
		ecs.close();

	return;
	}

		// BufferedInputStream to use for gzipped data (made into member variable for speed by eibe)
		private BufferedInputStream bis = null;
		private long currentPosition = 0;

	//////////////////////////////////////////////////////////////////
	/*
	* Read one 3D volume blob from disk to byte array
	* ttt T dimension of vol to read (0 based index)
	* return byte array
	* @exception IOException
	*/
	private byte[] readVolBlob(short ttt) throws IOException {

	byte b[];
	RandomAccessFile raf;
	short ZZZ;
	long skip_head, skip_data;
	int blob_size;
	
	// for 2D volumes, zdim may be 0
	ZZZ = ZDIM;
	if (dim[0] == 2)
		ZZZ = 1;

	blob_size = XDIM*YDIM*ZZZ*bytesPerVoxel(datatype);
	b = new byte[blob_size];

	skip_head=(long)vox_offset;
	skip_data = (long) (ttt*blob_size);

	///System.out.println("\nblob_size: "+blob_size+"  skip_head: "+skip_head+"  skip_data: "+skip_data);



	// read the volume from disk into a byte array
	// read compressed data with BufferedInputStream
	if (ds_datname.endsWith(".gz")) {

		// Check whether the stream has already been opened and we are at the correct position
		if ((bis == null) || (currentPosition != skip_head + skip_data)) {
			bis = new BufferedInputStream(new GZIPInputStream(new FileInputStream(ds_datname)));
			currentPosition = 0;
		}
		if (currentPosition != skip_head + skip_data) { // Are we at the wrong position in the stream?
			bis.skip(skip_head + skip_data);
			currentPosition = skip_head + skip_data;
		}

		// Read the actual data
		bis.read(b,0,blob_size);

		// Skip to the start of the next blob (if there's another blob left)
		bis.mark(blob_size);
		long skipped = 0, toSkip = blob_size;
		while ((toSkip > 0) && ((skipped = bis.skip(toSkip)) != 0)) {
			toSkip -= skipped;
		}

		// Check whether we have reached the end of the dataset (i.e., there is no further blob available)
		if (toSkip > 0) {
			bis.close();
			bis = null;
			currentPosition = 0;
		} else {
			bis.reset();
			currentPosition += blob_size;
		}
	}
	// read uncompressed data with RandomAccessFile
	else {
	raf = new RandomAccessFile(ds_datname, "r");
	raf.seek(skip_head+skip_data);
	raf.readFully(b,0,blob_size);
	raf.close();
	}

	return b;
	}


	//////////////////////////////////////////////////////////////////
	/*
	* Write one 3D volume blob from byte array to disk
	* ByteArrayOutputStream baos -- blob of bytes to write
	* ttt T dimension of vol to write (0 based index)
	* @exception IOException
	*/
	private void writeVolBlob(ByteArrayOutputStream baos, short ttt) throws IOException {

	RandomAccessFile raf;
	short ZZZ;
	long skip_head, skip_data;
	
	// for 2D volumes, zdim may be 0
	ZZZ = ZDIM;
	if (dim[0] == 2)
		ZZZ = 1;

	skip_head=(long)vox_offset;
	skip_data = (long) (ttt*XDIM*YDIM*ZZZ*bytesPerVoxel(datatype));

	//System.out.println("\nwriteVolBlob "+ttt+" skip_head: "+skip_head+"  skip_data: "+skip_data);



	// can't write random access compressed yet
	if (ds_datname.endsWith(".gz")) {
		throw new IOException("Sorry, can't write to compressed image data file: "+ds_datname);
	}
	// write data blob
	else {
	raf = new RandomAccessFile(ds_datname, "rwd");
	raf.seek(skip_head+skip_data);
	raf.write(baos.toByteArray());
	raf.close();
	}

	return;
	}


	//////////////////////////////////////////////////////////////////
	/**
	* Read all the data into one byte array.
	* Note that since the data is handled as bytes, NO byte swapping
	* has been performed.  Applications converting from the byte
	* array to int/float datatypes will need to swap bytes if 
	* necessary.
	* return byte array with all image data
	* @exception IOException
	*/
	public byte[] readData() throws IOException {

	int i;
	byte b[];
	GZIPInputStream gis;
	DataInputStream dis;
	long skip_head,blob_size;
	

	/// compute size of data, allocate array
	blob_size = 1;
	for (i=1; i<=dim[0]; i++)
		blob_size *= dim[i];
	blob_size *= bytesPerVoxel(datatype);

	if (blob_size > Integer.MAX_VALUE)
		throw new IOException("\nSorry, cannot yet handle data arrays bigger than "+Integer.MAX_VALUE+" bytes.  "+ds_datname+" has "+blob_size+" bytes.");

	b = new byte[(int)blob_size];

	skip_head=(long)vox_offset;

	///System.out.println("\nblob_size: "+blob_size+"  skip_head: "+skip_head);


	// read the volume from disk into a byte array
	if (ds_datname.endsWith(".gz")) 
		dis = new DataInputStream(new GZIPInputStream(new FileInputStream(ds_datname)));
	else
		dis = new DataInputStream(new FileInputStream(ds_datname));

	dis.skipBytes((int)skip_head);
	dis.readFully(b,0,(int)blob_size);
	dis.close();

	return b;
	}



	//////////////////////////////////////////////////////////////////
	/**
	* Write a byte array of data to disk, starting at vox_offset,
	* beginning of image data.  It is assumed that usually the entire
	* data array will be written with this call.
	* Note that since the data is handled as bytes, NO byte swapping
	* will be performed.  Applications needing byteswapping must
	* swap the bytes correctly in the input array b.
	* @param b byte array of image data
	* @exception IOException
	*/
	public void writeData(byte b[]) throws IOException {

	RandomAccessFile raf;
	int skip_head;
	

	skip_head=(int)vox_offset;


	// can't write random access compressed yet
	if (ds_datname.endsWith(".gz")) 
		throw new IOException("Sorry, can't write to compressed image data file: "+ds_datname);


	// write data blob
	raf = new RandomAccessFile(ds_datname, "rwd");
	raf.seek(skip_head);
	raf.write(b,0,b.length);
	raf.close();

	return;
	}

	//////////////////////////////////////////////////////////////////
	/**
	* Read one 1D timecourse from a 4D dataset, ie all T values for
	* a given XYZ location.  Scaling is applied.
	* @param x X dimension of vol to read (0 based index)
	* @param y Y dimension of vol to read (0 based index)
	* @param z Z dimension of vol to read (0 based index)
	* @return 1D double array
	* @exception IOException
	*/
	public double[] readDoubleTmcrs(short x, short y, short z) throws IOException {

	GZIPInputStream gis;
	EndianCorrectInputStream ecs;
	short ZZZ,i;
	long skip_head, skip_data, skip_vol, skip_vol2;
	double data[];
	
	// for 2D volumes, zdim may be 0
	ZZZ = ZDIM;
	if (dim[0] == 2)
		ZZZ = 1;


	data = new double[TDIM];

	skip_head=(long)(vox_offset);
	skip_data = (long) ((z*XDIM*YDIM + y*XDIM + x)*bytesPerVoxel(datatype));
	skip_vol = (XDIM*YDIM*ZZZ*bytesPerVoxel(datatype))-bytesPerVoxel(datatype);
	skip_vol2 = 0;

	///System.out.println("\nskip_head: "+skip_head+"  skip_data: "+skip_data+"  skip_vol: "+skip_vol);



	// get input handle
	if (ds_datname.endsWith(".gz")) 
		ecs = new EndianCorrectInputStream(new GZIPInputStream(new FileInputStream(ds_datname)),big_endian);
	else
		ecs = new EndianCorrectInputStream(new FileInputStream(ds_datname),big_endian);


	// skip header stuff and data up to 1st voxel
	ecs.skip((int) (skip_head+skip_data));


	// loop over all timepoints
	for (i=0; i<TDIM; i++) {

		// skip 0 first time, then vol skip
		ecs.skip(skip_vol2);
		skip_vol2 = skip_vol;

	// read voxel value, convert to double, fix sign, scale
	switch (datatype) {

		case NIFTI_TYPE_INT8:
		case NIFTI_TYPE_UINT8:
			data[i] = (double) (ecs.readByte());
			if ((datatype == NIFTI_TYPE_UINT8) && (data[i] < 0) )
				data[i] = Math.abs(data[i]) + (double)(1<<7);
			if (scl_slope != 0)
				data[i] = data[i] * scl_slope + scl_inter;
			break;

		case NIFTI_TYPE_INT16:
		case NIFTI_TYPE_UINT16:
			data[i] = (double) (ecs.readShortCorrect());
			if ((datatype == NIFTI_TYPE_UINT16) && (data[i] < 0))
				data[i] = Math.abs(data[i]) + (double)(1<<15);
			if (scl_slope != 0)
				data[i] = data[i] * scl_slope + scl_inter;
			break;

		case NIFTI_TYPE_INT32:
		case NIFTI_TYPE_UINT32:
			data[i] = (double) (ecs.readIntCorrect());
			if ( (datatype == NIFTI_TYPE_UINT32) && (data[i] < 0) )
				data[i] = Math.abs(data[i]) + (double)(1<<31);
			if (scl_slope != 0)
				data[i] = data[i] * scl_slope + scl_inter;
			break;


		case NIFTI_TYPE_INT64:
		case NIFTI_TYPE_UINT64:
			data[i] = (double) (ecs.readLongCorrect());
			if ( (datatype == NIFTI_TYPE_UINT64) && (data[i] < 0) )
				data[i] = Math.abs(data[i]) + (double)(1<<63);
			if (scl_slope != 0)
				data[i] = data[i] * scl_slope + scl_inter;
			break;


		case NIFTI_TYPE_FLOAT32:
			data[i] = (double) (ecs.readFloatCorrect());
			if (scl_slope != 0)
				data[i] = data[i] * scl_slope + scl_inter;
			break;


		case NIFTI_TYPE_FLOAT64:
			data[i] = (double) (ecs.readDoubleCorrect());
			if (scl_slope != 0)
				data[i] = data[i] * scl_slope + scl_inter;
			break;


		case DT_NONE:
		case DT_BINARY:
		case NIFTI_TYPE_COMPLEX64:
		case NIFTI_TYPE_FLOAT128:
		case NIFTI_TYPE_RGB24:
		case NIFTI_TYPE_COMPLEX128:
		case NIFTI_TYPE_COMPLEX256:
		case DT_ALL:
		default:
			throw new IOException("Sorry, cannot yet read nifti-1 datatype "+decodeDatatype(datatype));
		}
	


	}  // loop over TDIM

	ecs.close();


	return data;
	}


	//////////////////////////////////////////////////////////////////
	/*
	* truncate or pad a string to make it the needed length
	* @param s input string
	* @param n desired length
	* @return s String of length n with as much of s as possible, padded
	* with 0 if necessary
	*/
	private byte[] setStringSize(StringBuffer s, int n) {

		byte b[];
		int i,slen;

		slen = s.length();

		if (slen >= n)
			return(s.toString().substring(0,n).getBytes());

		b = new byte[n];
		for (i=0; i<slen; i++)
			b[i] = (byte)s.charAt(i);
		for (i=slen; i<n; i++)
			b[i] = 0;

		return(b);
	}
		

	//////////////////////////////////////////////////////////////////
	/**
	* main routine is used only for testing
	* @param args the command-line arguments (file name)
	*/
	public static void main(String[] args) {
		

		Nifti1Dataset nds = new Nifti1Dataset(args[0]);
		try {
			nds.readHeader();
			nds.printHeader();
		}
		catch (IOException ex) {
			System.out.println("\nCould not read header file for "+args[0]+": "+ex.getMessage());
		}

		return;
	}


}


