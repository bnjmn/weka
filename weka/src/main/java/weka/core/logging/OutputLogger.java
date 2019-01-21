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
 * OutputLogger.java
 * Copyright (C) 2008-2012 University of Waikato, Hamilton, New Zealand
 */

package weka.core.logging;

import weka.core.RevisionUtils;
import weka.core.Tee;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Date;

/**
 * A logger that logs all output on stdout and stderr to a file.
 *
 * @author  fracpete (fracpete at waikato dot ac dot nz)
 * @version $Revision$
 */
public class OutputLogger
  extends FileLogger {

  /**
   * A print stream class to capture all data from stdout and stderr.
   *
   * @author  fracpete (fracpete at waikato dot ac dot nz)
   * @version $Revision$
   */
  public static class OutputPrintStream
    extends PrintStream {

    /** the owning logger. */
    protected OutputLogger m_Owner;

    /** the line feed. */
    protected String m_LineFeed;

    /**
     * Default constructor.
     *
     * @param owner		the owning logger
     * @param stream		the stream
     * @throws Exception	if something goes wrong
     */
    public OutputPrintStream(OutputLogger owner, PrintStream stream) throws Exception {
      super(stream);

      m_Owner    = owner;
      m_LineFeed = System.getProperty("line.separator");
    }

    /**
     * ignored.
     */
    public void flush() {
    }

    /**
     * prints the given int to the streams.
     *
     * @param x 	the object to print
     */
    public void print(int x) {
      m_Owner.append("" + x);
    }

    /**
     * prints the given boolean to the streams.
     *
     * @param x 	the object to print
     */
    public void print(boolean x) {
      m_Owner.append("" + x);
    }

    /**
     * prints the given string to the streams.
     *
     * @param x 	the object to print
     */
    public void print(String x) {
      m_Owner.append("" + x);
    }

    /**
     * prints the given object to the streams.
     *
     * @param x 	the object to print
     */
    public void print(Object x) {
      m_Owner.append("" + x);
    }

    /**
     * prints a new line to the streams.
     */
    public void println() {
      m_Owner.append(m_LineFeed);
    }

    /**
     * prints the given int to the streams.
     *
     * @param x 	the object to print
     */
    public void println(int x) {
      m_Owner.append(x + m_LineFeed);
    }

    /**
     * prints the given boolean to the streams.
     *
     * @param x 	the object to print
     */
    public void println(boolean x) {
      m_Owner.append(x + m_LineFeed);
    }

    /**
     * prints the given string to the streams.
     *
     * @param x 	the object to print
     */
    public void println(String x) {
      m_Owner.append(x + m_LineFeed);
    }

    /**
     * prints the given object to the streams (for Throwables we print the stack
     * trace).
     *
     * @param x 	the object to print
     */
    public void println(Object x) {
      m_Owner.append(x + m_LineFeed);
    }

    /**
     * Writes the byte to the stream.
     *
     * @param b 	the byte to write
     */
    @Override
    public void write(int b) {
      m_Owner.append("" + b);
    }

    /**
     * Writes the bytes to the stream.
     *
     * @param b 	the bytes to write
     */
    @Override
    public void write(byte[] b) throws IOException {
      StringBuilder builder = new StringBuilder();
      for (int i = 0; i < b.length; i++)
	builder.append("" + b[i]);
      print(builder.toString());
    }

    /**
     * Writes the bytes to the stream.
     *
     * @param buf 	the buffer to use
     * @param off 	the offset
     * @param len	the number of bytes to write
     */
    @Override
    public void write(byte[] buf, int off, int len) {
      StringBuilder builder = new StringBuilder();
      for (int i = off; i < off + len; i++)
	builder.append("" + buf[i]);
      print(builder.toString());
    }

    /**
     * Appends a subsequence of the specified character sequence to this output
     * stream.
     *
     * <p> An invocation of this method of the form
     * {@code out.append(csq, start, end)} when
     * {@code csq} is not {@code null}, behaves in
     * exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.print(csq.subSequence(start, end).toString())
     * }</pre>
     *
     * @param  csq
     *         The character sequence from which a subsequence will be
     *         appended.  If {@code csq} is {@code null}, then characters
     *         will be appended as if {@code csq} contained the four
     *         characters {@code "null"}.
     *
     * @param  start
     *         The index of the first character in the subsequence
     *
     * @param  end
     *         The index of the character following the last character in the
     *         subsequence
     *
     * @return  This output stream
     *
     * @throws  IndexOutOfBoundsException
     *          If {@code start} or {@code end} are negative, {@code start}
     *          is greater than {@code end}, or {@code end} is greater than
     *          {@code csq.length()}
     *
     * @since  1.5
     */
    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
      m_Owner.append(csq.subSequence(start, end).toString());
      return this;
    }

    /**
     * Appends the specified character sequence to this output stream.
     *
     * <p> An invocation of this method of the form {@code out.append(csq)}
     * behaves in exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.print(csq.toString())
     * }</pre>
     *
     * <p> Depending on the specification of {@code toString} for the
     * character sequence {@code csq}, the entire sequence may not be
     * appended.  For instance, invoking then {@code toString} method of a
     * character buffer will return a subsequence whose content depends upon
     * the buffer's position and limit.
     *
     * @param  csq
     *         The character sequence to append.  If {@code csq} is
     *         {@code null}, then the four characters {@code "null"} are
     *         appended to this output stream.
     *
     * @return  This output stream
     *
     * @since  1.5
     */
    @Override
    public PrintStream append(CharSequence csq) {
      m_Owner.append(csq.toString());
      return this;
    }

    /**
     * Appends the specified character to this output stream.
     *
     * <p> An invocation of this method of the form {@code out.append(c)}
     * behaves in exactly the same way as the invocation
     *
     * <pre>{@code
     *     out.print(c)
     * }</pre>
     *
     * @param  c
     *         The 16-bit character to append
     *
     * @return  This output stream
     *
     * @since  1.5
     */
    @Override
    public PrintStream append(char c) {
      m_Owner.append("" + c);
      return this;
    }
  }

  /** the stream object used for logging stdout. */
  protected OutputPrintStream m_StreamOut;

  /** the stream object used for logging stderr. */
  protected OutputPrintStream m_StreamErr;

  /** the Tee instance to redirect stdout. */
  protected Tee m_StdOut;

  /** the Tee instance to redirect stderr. */
  protected Tee m_StdErr;

  /**
   * Initializes the logger.
   */
  protected void initialize() {
    super.initialize();

    try {
      m_StdOut = new Tee(System.out);
      System.setOut(m_StdOut);
      m_StreamOut = new OutputPrintStream(this, m_StdOut.getDefault());
      m_StdOut.add(m_StreamOut);

      m_StdErr = new Tee(System.err);
      System.setErr(m_StdErr);
      m_StreamErr = new OutputPrintStream(this, m_StdErr.getDefault());
      m_StdErr.add(m_StreamErr);
    }
    catch (Exception e) {
      // ignored
    }
  }

  /**
   * Performs the actual logging. 
   *
   * @param level	the level of the message
   * @param msg		the message to log
   * @param cls		the classname originating the log event
   * @param method	the method originating the log event
   * @param lineno	the line number originating the log event
   */
  protected void doLog(Level level, String msg, String cls, String method, int lineno) {
    // append output to file
    append(
      m_DateFormat.format(new Date()) + " " + cls + " " + method + m_LineFeed
	+ level + ": " + msg + m_LineFeed);
  }

  /**
   * Returns the revision string.
   *
   * @return		the revision
   */
  public String getRevision() {
    return RevisionUtils.extract("$Revision$");
  }
}
