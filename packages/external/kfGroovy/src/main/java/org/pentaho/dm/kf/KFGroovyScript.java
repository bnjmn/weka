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
 *    KFGroovyScript.java
 *    Copyright (C) 2009 Pentaho Corporation
 *
 */

package org.pentaho.dm.kf;


/**
 * Interface implemented by all KnowledgeFlow Groovy
 * script classes
 * 
 * @author Mark Hall (mhall{[at]}pentaho{[dot]}org)
 * @version $Revision$
 */
public interface KFGroovyScript {
  
  /**
   * Allows the parent GroovyComponent to pass
   * itself to the script as an instance of GroovyHelper.
   * 
   * @param parent the parent GroovyHelper.
   */
  void setManager(GroovyHelper parent);
}
