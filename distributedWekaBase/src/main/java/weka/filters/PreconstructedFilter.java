package weka.filters;

import weka.core.Preconstructed;

/**
 * Marker interface for a filter that has been Preconstructed. Users of such a
 * filter can assume that it is Streamable and that input() can be called
 * immediately in order to process new instances; furthermore, such a filter
 * should make an output instance available immediately after input() is called.
 * 
 * Implementers of a PreconstructedFilter should perform any initialization and
 * processing necessary when setInputFormat() is called. They should not assume
 * or rely on there being anything but a header supplied to setInputFormat().
 * Once setInputFormat() has been called, the filter is deemed to be
 * "preconstructed".
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}com)
 * @version $Revision$
 */
public interface PreconstructedFilter extends Preconstructed {

  // calling super.reset() should result in the need to call
  // setInputFormat() again in order to initialize the filter and
  // isConstructed() returning false.
}
