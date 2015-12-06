package weka.core.converters.nifti;

import java.io.*;
import java.util.zip.*;


/**
 *  EndianCorrectInputStream extends DataInputStream; it will optionally flip
 *  bytes to read little endian data correctly. 
 */

public class EndianCorrectInputStream extends DataInputStream {

    private boolean bigendian = true;

    /**
     * Constructor for a disk file.
     * @param filename filename for datafile
     * @param be -- endian flag: if be (big endian) is false bytes will be flipped on read
     * @exception FileNotFoundException
     */
    public EndianCorrectInputStream(String filename, boolean be) throws FileNotFoundException {
	super(new FileInputStream(filename));
	bigendian = be;
    }

    /**
     * Constructor for an InputStream.
     * @param is InputStream to read data from
     * @param be -- endian flag: if be (big endian) is false bytes will be flipped on read
     */
    public EndianCorrectInputStream(InputStream is, boolean be)  {
	super(is);
	bigendian = be;
    }



    /**
     *  readShortCorrect will return a short from the stream
     */
    public short readShortCorrect() throws IOException {
        short val;

	val = readShort();
	if (bigendian) {
		return(val);
	}
	else {	
           int byte0 = (int) val & 0xff;
           int byte1 = ((int)val>>8) & 0xff;
           // swap the byte order
           return (short) ((byte0<<8) | (byte1)) ;
	}
    }
    /**
     *  flipShort will byte flip a short
     */
    public short flipShort(short val) {

           int byte0 = (int) val & 0xff;
           int byte1 = ((int)val>>8) & 0xff;
           // swap the byte order
           return (short) ((byte0<<8) | (byte1)) ;
    }


    /**
     *  readIntCorrect will return an int from the stream
     */
    public int readIntCorrect() throws IOException {
	int val;

        val = readInt();
	if (bigendian){
		return(val);
	}

	else {
           int byte0 = val & 0xff;
           int byte1 = (val>>8) & 0xff;
           int byte2 = (val>>16) & 0xff;
           int byte3 = (val>>24) & 0xff;
           // swap the byte order
           return (byte0<<24) | (byte1<<16) | (byte2<<8) | byte3;
	}
    }

    /**
     *  flipInt will flip the byte order of an int
     */
    public int flipInt(int val) {

           int byte0 = val & 0xff;
           int byte1 = (val>>8) & 0xff;
           int byte2 = (val>>16) & 0xff;
           int byte3 = (val>>24) & 0xff;
           // swap the byte order
           return (byte0<<24) | (byte1<<16) | (byte2<<8) | byte3;
    }

    /**
     *  readLongCorrect will return a long from the stream
     */
    public long readLongCorrect() throws IOException {
	long val;

        val = readLong();
	if (bigendian){
		return(val);
	}

	else {
		return(flipLong(val));
	}
    }

    /**
     *  flipLong will flip the byte order of a long
     */
    public long flipLong(long val) {

           long byte0 = val & 0xff;
           long byte1 = (val>>8) & 0xff;
           long byte2 = (val>>16) & 0xff;
           long byte3 = (val>>24) & 0xff;
           long byte4 = (val>>32) & 0xff;
           long byte5 = (val>>40) & 0xff;
           long byte6 = (val>>48) & 0xff;
           long byte7 = (val>>56) & 0xff;
           // swap the byte order
           return (long) ((byte0<<56) | (byte1<<48) | (byte2<<40) | (byte3<<32) | (byte4<<24) | (byte5<<16)| (byte6<<8) | byte7);
    }


    /**
     *  readFloatCorrect will return a float from the stream
     */ 
    public float readFloatCorrect() throws IOException {
	float val;

        if (bigendian){
		val = readFloat();
	}

	else {
	    int x = readUnsignedByte();
	    x |= ((int)readUnsignedByte()) << 8;
	    x |= ((int)readUnsignedByte()) << 16;
	    x |= ((int)readUnsignedByte()) << 24;
	    val =  (float)Float.intBitsToFloat(x);
	}
	return val;
    }

    /**
     *  flipFloat will flip the byte order of a float
     */ 
    public float flipFloat(float val) throws IOException {

		int x = Float.floatToIntBits(val);
		int y = flipInt(x);
	    	return Float.intBitsToFloat(y);
    }

    /**
     *  readDoubleCorrect will return a double from the stream
     */
    public double readDoubleCorrect() throws IOException {
	double val;
	if (bigendian){
	    val = readDouble();
	}
	else {
	    long x = readUnsignedByte();
	    x |= ((long)readUnsignedByte()) << 8;
	    x |= ((long)readUnsignedByte()) << 16;
	    x |= ((long)readUnsignedByte()) << 24;
	    x |= ((long)readUnsignedByte()) << 32;		
	    x |= ((long)readUnsignedByte()) << 40;
	    x |= ((long)readUnsignedByte()) << 48;
	    x |= ((long)readUnsignedByte()) << 56;
	    val = Double.longBitsToDouble(x);
	}
	return val;
    }
    
    /**
     *  flipDouble will flip the byte order of a double
     */ 
    public double flipDouble(double val) {

		long x = Double.doubleToLongBits(val);
		long y = flipLong(x);
	    	return Double.longBitsToDouble(y);
    }

}
