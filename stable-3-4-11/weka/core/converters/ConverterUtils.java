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
 *    ConverterUtils.java
 *    Copyright (C) 2000 Mark Hall
 *
 */

package weka.core.converters;

import java.io.StreamTokenizer;
import java.io.Serializable;
import java.io.IOException;

/**
 * Utility routines for the converter package.
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision 1.0 $
 * @see Serializable
 */
public class ConverterUtils implements Serializable {

  /**
   * Gets token, skipping empty lines.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if reading the next token fails
   */
  public static void getFirstToken(StreamTokenizer tokenizer) 
    throws IOException {
    
    while (tokenizer.nextToken() == StreamTokenizer.TT_EOL){};
    if ((tokenizer.ttype == '\'') ||
	(tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    } else if ((tokenizer.ttype == StreamTokenizer.TT_WORD) &&
	       (tokenizer.sval.equals("?"))) {
      tokenizer.ttype = '?';
    }
  }

  /**
   * Gets token.
   *
   * @param tokenizer the stream tokenizer
   * @exception IOException if reading the next token fails
   */
  public static void getToken(StreamTokenizer tokenizer) throws IOException {
    
    tokenizer.nextToken();
    if (tokenizer.ttype== StreamTokenizer.TT_EOL) {
      return;
    }

    if ((tokenizer.ttype == '\'') ||
	(tokenizer.ttype == '"')) {
      tokenizer.ttype = StreamTokenizer.TT_WORD;
    } else if ((tokenizer.ttype == StreamTokenizer.TT_WORD) &&
	       (tokenizer.sval.equals("?"))) {
      tokenizer.ttype = '?';
    }
  }

  /**
   * Throws error message with line number and last token read.
   *
   * @param theMsg the error message to be thrown
   * @param tokenizer the stream tokenizer
   * @exception IOExcpetion containing the error message
   */
  public static void errms(StreamTokenizer tokenizer, String theMsg) 
    throws IOException {
    
    throw new IOException(theMsg + ", read " + tokenizer.toString());
  }
}


