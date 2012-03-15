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
 *    RulesParser.java
 *    Copyright (C) 2003 DESS IAGL of Lille
 *
 */

package rulesParser;

import java.io.File;
import java.io.StringBufferInputStream;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import rules.Rule;

/**
 *
 * @author  beleg
 */
public class RulesParser {
    
    private Rule[] rules;
    private String[] criteres;
    private File f;
    private String xmlString;
    private SAXParser parser;
    private RulesHandler theHandler;
    
    /** Creates a new instance of RulesParser */
    public RulesParser(File f) throws Exception{
    	
    	this.f = f;
    	SAXParserFactory factory =  SAXParserFactory.newInstance();
    	factory.setValidating(true);
			parser = factory.newSAXParser();
    	theHandler = new RulesHandler(); 
    	
    }
    
    public RulesParser() throws Exception {
      SAXParserFactory factory =  SAXParserFactory.newInstance();
      factory.setValidating(true);
                      parser = factory.newSAXParser();
      theHandler = new RulesHandler();
    }
    
    public void parse() throws Exception{
			parser.parse(f,theHandler);

    }
    
    public void parse(String xmlString) throws Exception {
      parser.parse(new StringBufferInputStream(xmlString), theHandler);
    }
    
    public Rule[] getRules() {
        return theHandler.getRules();
    }
    
    public String[] getCriteres() {
        return theHandler.getCriteres();
    }
    
}
