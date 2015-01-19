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
 *    package-info.java
 *    Copyright (C) 2015 University of Waikato, Hamilton, New Zealand
 *
 */

/**
 * Package for a framework for simple, flexible and performant expression
 * languages</p>
 * 
 * <h1>Introduction & Overview</h1>
 * 
 * The {@link weka.core.expressionlanguage} package provides functionality to
 * easily create simple languages.</p>
 * 
 * It does so through creating an AST (abstract syntax tree) that can then be
 * evaluated.</p>
 * 
 * At the heart of the AST is the {@link weka.core.expressionlanguage.core.Node}
 * interface. It's an empty interface to mark types to be an AST node.</br>
 * Thus there are no real constraints on AST nodes so that they have as much
 * freedom as possible to reflect abstractions of programs.</p>
 * 
 * To give a common base to build upon the {@link weka.core.expressionlanguage.common.Primitives}
 * class provides the subinterfaces for the primitive boolean 
 * ({@link weka.core.expressionlanguage.common.Primitives.BooleanExpression}),
 * double ({@link weka.core.expressionlanguage.common.Primitives.DoubleExpression})
 * and String ({@link weka.core.expressionlanguage.common.Primitives.StringExpression})
 * types.</br>
 * It furthermore provides implementations of constants and variables of those
 * types.</p>
 * 
 * Most extensibility is achieved through adding macros to a language. Macros
 * allow for powerful meta-programming since they directly work with AST nodes.
 * </br>
 * The {@link weka.core.expressionlanguage.core.Macro} interface defines what a
 * macro looks like.</p>
 * 
 * Variable and macro lookup is done through 
 * {@link weka.core.expressionlanguage.core.VariableDeclarations} and
 * {@link weka.core.expressionlanguage.core.MacroDeclarations} resp. Furthermore,
 * both can be combined through 
 * {@link weka.core.expressionlanguage.common.VariableDeclarationsCompositor}
 * and {@link weka.core.expressionlanguage.common.MacroDeclarationsCompositor}
 * resp.</br>
 * This really allows to add built-in variables and powerful built-in functions
 * to a language.</p>
 * 
 * Useful implementations are:</br>
 * <ul>
 * <li>{@link weka.core.expressionlanguage.common.SimpleVariableDeclarations}</li>
 * <li>{@link weka.core.expressionlanguage.common.MathFunctions}</li>
 * <li>{@link weka.core.expressionlanguage.common.IfElseMacro}</li>
 * <li>{@link weka.core.expressionlanguage.common.JavaMacro}</li>
 * <li>{@link weka.core.expressionlanguage.common.NoVariables}</li>
 * <li>{@link weka.core.expressionlanguage.common.NoMacros}</li>
 * <li>{@link weka.core.expressionlanguage.weka.InstancesHelper}</li>
 * <li>{@link weka.core.expressionlanguage.weka.StatsHelper}</li>
 * </ul>
 * 
 * The described framework doesn't touch the syntax of a language so far. The
 * syntax is seen as a separate element of a language.</br>
 * If a program is given in a textual representation (e.g. "A + sqrt(2.0)" is a
 * program in a textual representation), this textual representation declares
 * how the AST looks like. That's why the parser's job is to build the AST.</br>
 * There is a parser in the {@link weka.core.expressionlanguage.parser} package.</br>
 * However the framework allows for other means to construct an AST if needed.</p>
 * 
 * Built-in operators like (+, -, *, / etc) are a special case, since they can
 * be seen as macros, however they are strongly connected to the parser too.</br>
 * To separate the parser and these special macros there  is the
 * {@link weka.core.expressionlanguage.common.Operators} class which can be used
 * by the parser to delegate operator semantics elsewhere.
 * 
 * <h2>A word on parsers</h2>
 * 
 * Currently the parser is generated through the CUP parser generator and jflex
 * lexer generator. While parser generators are powerful tools they suffer from
 * some unfortunate drawbacks:</p>
 * <ul>
 * <li>The parsers are generated. So there is an additional indirection between
 * the grammar file (used for parser generation) and the generated code.</li>
 * <li>The grammar files usually have their own syntax which may be quite
 * different from the programming language otherwise used in a project.</li>
 * <li>In more complex grammars it's easy to introduce ambiguities and unwanted
 * valid syntax.</li>
 * </ul>
 * It's for these reasons why the parser is kept as simple as possible and with
 * as much functionality delegated elsewhere as possible.
 * 
 * <h2>Summary</h2>
 * 
 * A flexible AST structure is given by the
 * {@link weka.core.expressionlanguage.core.Node} interface. The
 * {@link weka.core.expressionlanguage.core.Macro} interface allows for powerful
 * meta-programming which is an important part of the extensibility features. The
 * {@link weka.core.expressionlanguage.common.Primitives} class gives a good
 * basis for the primitive boolean, double & String types.</br>
 * The parser is responsible for building up the AST structure. It delegates
 * operator semantics to {@link weka.core.expressionlanguage.common.Operators}.
 * Symbol lookup is done through the
 * {@link weka.core.expressionlanguage.core.VariableDeclarations} and
 * {@link weka.core.expressionlanguage.core.MacroDeclarations} interfaces which
 * can be combined with the
 * {@link weka.core.expressionlanguage.common.VariableDeclarationsCompositor}
 * and {@link weka.core.expressionlanguage.common.MacroDeclarationsCompositor}
 * classes resp.</p>
 * 
 * <h1>Usage</h1>
 * 
 * With the described framework it's possible to create languages in a declarative
 * way. Examples can be found in
 * {@link weka.filters.unsupervised.attribute.MathExpression},
 * {@link weka.filters.unsupervised.attribute.AddExpression} and
 * {@link weka.filters.unsupervised.instance.SubsetByExpression}.</p>
 * 
 * A commonly used language is:<p>
 * 
 * <code><pre>
 * // exposes instance values and 'ismissing' macro
 * InstancesHelper instancesHelper = new InstancesHelper(dataset);
 * 
 * // creates the AST
 * Node node = Parser.parse(
 *   // expression
 *   expression, // textual representation of the program
 *   // variables
 *   instancesHelper,
 *   // macros
 *   new MacroDeclarationsCompositor(
 *     instancesHelper,
 *     new MathFunctions(),
 *     new IfElseMacro(),
 *     new JavaMacro()
 *   )
 * );
 *
 * // type checking is neccessary, but allows for greater flexibility
 * if (!(node instanceof DoubleExpression))
 *   throw new Exception("Expression must be of boolean type!");
 *    
 * DoubleExpression program = (DoubleExpression) node;
 * </pre></code>
 * 
 * <h1>History</h1>
 * 
 * Previously there were three very similar languages in the
 * <code>weka.core.mathematicalexpression</code> package,
 * <code>weka.core.AttributeExpression</code> class and the
 * <code>weka.filters.unsupervised.instance.subsetbyexpression</code> package.</br>
 * Due to their similarities it was decided to unify them into one expressionlanguage.
 * However backwards compatibility was an important goal, that's why there are
 * some quite redundant parts in the language (e.g. both 'and' and '&' are operators
 * for logical and).
 * 
 * @author Benjamin Weber ( benweber at student dot ethz dot ch )
 * @version $Revision: 1000 $
 */
package weka.core.expressionlanguage;