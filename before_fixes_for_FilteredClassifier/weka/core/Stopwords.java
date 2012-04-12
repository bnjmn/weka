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
 *    Stopwords.java
 *    Copyright (C) 2001 Eibe Frank
 */

package weka.core;

import java.util.*;

/**
 * Class that can test whether a given string is a stop word.
 * Lowercases all words before the test.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @author Ashraf M. Kibriya (amk14@cs.waikato.ac.nz)
 * @version $Revision: 1.2 $
 */
public class Stopwords {
  
  /** The hashtable containing the list of stopwords */
  private static Hashtable m_Stopwords = null;

  static {
   
    if (m_Stopwords == null) {
      m_Stopwords = new Hashtable();
      Double dummy = new Double(0);

      
      //Stopwords list from Rainbow
      m_Stopwords.put("a", dummy);
      m_Stopwords.put("able", dummy);
      m_Stopwords.put("about", dummy);
      m_Stopwords.put("above", dummy);
      m_Stopwords.put("according", dummy);
      m_Stopwords.put("accordingly", dummy);
      m_Stopwords.put("across", dummy);
      m_Stopwords.put("actually", dummy);
      m_Stopwords.put("after", dummy);
      m_Stopwords.put("afterwards", dummy);
      m_Stopwords.put("again", dummy);
      m_Stopwords.put("against", dummy);
      m_Stopwords.put("all", dummy);
      m_Stopwords.put("allow", dummy);
      m_Stopwords.put("allows", dummy);
      m_Stopwords.put("almost", dummy);
      m_Stopwords.put("alone", dummy);
      m_Stopwords.put("along", dummy);
      m_Stopwords.put("already", dummy);
      m_Stopwords.put("also", dummy);
      m_Stopwords.put("although", dummy);
      m_Stopwords.put("always", dummy);
      m_Stopwords.put("am", dummy);
      m_Stopwords.put("among", dummy);
      m_Stopwords.put("amongst", dummy);
      m_Stopwords.put("an", dummy);
      m_Stopwords.put("and", dummy);
      m_Stopwords.put("another", dummy);
      m_Stopwords.put("any", dummy);
      m_Stopwords.put("anybody", dummy);
      m_Stopwords.put("anyhow", dummy);
      m_Stopwords.put("anyone", dummy);
      m_Stopwords.put("anything", dummy);
      m_Stopwords.put("anyway", dummy);
      m_Stopwords.put("anyways", dummy);
      m_Stopwords.put("anywhere", dummy);
      m_Stopwords.put("apart", dummy);
      m_Stopwords.put("appear", dummy);
      m_Stopwords.put("appreciate", dummy);
      m_Stopwords.put("appropriate", dummy);
      m_Stopwords.put("are", dummy);
      m_Stopwords.put("around", dummy);
      m_Stopwords.put("as", dummy);
      m_Stopwords.put("aside", dummy);
      m_Stopwords.put("ask", dummy);
      m_Stopwords.put("asking", dummy);
      m_Stopwords.put("associated", dummy);
      m_Stopwords.put("at", dummy);
      m_Stopwords.put("available", dummy);
      m_Stopwords.put("away", dummy);
      m_Stopwords.put("awfully", dummy);
      m_Stopwords.put("b", dummy);
      m_Stopwords.put("be", dummy);
      m_Stopwords.put("became", dummy);
      m_Stopwords.put("because", dummy);
      m_Stopwords.put("become", dummy);
      m_Stopwords.put("becomes", dummy);
      m_Stopwords.put("becoming", dummy);
      m_Stopwords.put("been", dummy);
      m_Stopwords.put("before", dummy);
      m_Stopwords.put("beforehand", dummy);
      m_Stopwords.put("behind", dummy);
      m_Stopwords.put("being", dummy);
      m_Stopwords.put("believe", dummy);
      m_Stopwords.put("below", dummy);
      m_Stopwords.put("beside", dummy);
      m_Stopwords.put("besides", dummy);
      m_Stopwords.put("best", dummy);
      m_Stopwords.put("better", dummy);
      m_Stopwords.put("between", dummy);
      m_Stopwords.put("beyond", dummy);
      m_Stopwords.put("both", dummy);
      m_Stopwords.put("brief", dummy);
      m_Stopwords.put("but", dummy);
      m_Stopwords.put("by", dummy);
      m_Stopwords.put("c", dummy);
      m_Stopwords.put("came", dummy);
      m_Stopwords.put("can", dummy);
      m_Stopwords.put("cannot", dummy);
      m_Stopwords.put("cant", dummy);
      m_Stopwords.put("cause", dummy);
      m_Stopwords.put("causes", dummy);
      m_Stopwords.put("certain", dummy);
      m_Stopwords.put("certainly", dummy);
      m_Stopwords.put("changes", dummy);
      m_Stopwords.put("clearly", dummy);
      m_Stopwords.put("co", dummy);
      m_Stopwords.put("com", dummy);
      m_Stopwords.put("come", dummy);
      m_Stopwords.put("comes", dummy);
      m_Stopwords.put("concerning", dummy);
      m_Stopwords.put("consequently", dummy);
      m_Stopwords.put("consider", dummy);
      m_Stopwords.put("considering", dummy);
      m_Stopwords.put("contain", dummy);
      m_Stopwords.put("containing", dummy);
      m_Stopwords.put("contains", dummy);
      m_Stopwords.put("corresponding", dummy);
      m_Stopwords.put("could", dummy);
      m_Stopwords.put("course", dummy);
      m_Stopwords.put("currently", dummy);
      m_Stopwords.put("d", dummy);
      m_Stopwords.put("definitely", dummy);
      m_Stopwords.put("described", dummy);
      m_Stopwords.put("despite", dummy);
      m_Stopwords.put("did", dummy);
      m_Stopwords.put("different", dummy);
      m_Stopwords.put("do", dummy);
      m_Stopwords.put("does", dummy);
      m_Stopwords.put("doing", dummy);
      m_Stopwords.put("done", dummy);
      m_Stopwords.put("down", dummy);
      m_Stopwords.put("downwards", dummy);
      m_Stopwords.put("during", dummy);
      m_Stopwords.put("e", dummy);
      m_Stopwords.put("each", dummy);
      m_Stopwords.put("edu", dummy);
      m_Stopwords.put("eg", dummy);
      m_Stopwords.put("eight", dummy);
      m_Stopwords.put("either", dummy);
      m_Stopwords.put("else", dummy);
      m_Stopwords.put("elsewhere", dummy);
      m_Stopwords.put("enough", dummy);
      m_Stopwords.put("entirely", dummy);
      m_Stopwords.put("especially", dummy);
      m_Stopwords.put("et", dummy);
      m_Stopwords.put("etc", dummy);
      m_Stopwords.put("even", dummy);
      m_Stopwords.put("ever", dummy);
      m_Stopwords.put("every", dummy);
      m_Stopwords.put("everybody", dummy);
      m_Stopwords.put("everyone", dummy);
      m_Stopwords.put("everything", dummy);
      m_Stopwords.put("everywhere", dummy);
      m_Stopwords.put("ex", dummy);
      m_Stopwords.put("exactly", dummy);
      m_Stopwords.put("example", dummy);
      m_Stopwords.put("except", dummy);
      m_Stopwords.put("f", dummy);
      m_Stopwords.put("far", dummy);
      m_Stopwords.put("few", dummy);
      m_Stopwords.put("fifth", dummy);
      m_Stopwords.put("first", dummy);
      m_Stopwords.put("five", dummy);
      m_Stopwords.put("followed", dummy);
      m_Stopwords.put("following", dummy);
      m_Stopwords.put("follows", dummy);
      m_Stopwords.put("for", dummy);
      m_Stopwords.put("former", dummy);
      m_Stopwords.put("formerly", dummy);
      m_Stopwords.put("forth", dummy);
      m_Stopwords.put("four", dummy);
      m_Stopwords.put("from", dummy);
      m_Stopwords.put("further", dummy);
      m_Stopwords.put("furthermore", dummy);
      m_Stopwords.put("g", dummy);
      m_Stopwords.put("get", dummy);
      m_Stopwords.put("gets", dummy);
      m_Stopwords.put("getting", dummy);
      m_Stopwords.put("given", dummy);
      m_Stopwords.put("gives", dummy);
      m_Stopwords.put("go", dummy);
      m_Stopwords.put("goes", dummy);
      m_Stopwords.put("going", dummy);
      m_Stopwords.put("gone", dummy);
      m_Stopwords.put("got", dummy);
      m_Stopwords.put("gotten", dummy);
      m_Stopwords.put("greetings", dummy);
      m_Stopwords.put("h", dummy);
      m_Stopwords.put("had", dummy);
      m_Stopwords.put("happens", dummy);
      m_Stopwords.put("hardly", dummy);
      m_Stopwords.put("has", dummy);
      m_Stopwords.put("have", dummy);
      m_Stopwords.put("having", dummy);
      m_Stopwords.put("he", dummy);
      m_Stopwords.put("hello", dummy);
      m_Stopwords.put("help", dummy);
      m_Stopwords.put("hence", dummy);
      m_Stopwords.put("her", dummy);
      m_Stopwords.put("here", dummy);
      m_Stopwords.put("hereafter", dummy);
      m_Stopwords.put("hereby", dummy);
      m_Stopwords.put("herein", dummy);
      m_Stopwords.put("hereupon", dummy);
      m_Stopwords.put("hers", dummy);
      m_Stopwords.put("herself", dummy);
      m_Stopwords.put("hi", dummy);
      m_Stopwords.put("him", dummy);
      m_Stopwords.put("himself", dummy);
      m_Stopwords.put("his", dummy);
      m_Stopwords.put("hither", dummy);
      m_Stopwords.put("hopefully", dummy);
      m_Stopwords.put("how", dummy);
      m_Stopwords.put("howbeit", dummy);
      m_Stopwords.put("however", dummy);
      m_Stopwords.put("i", dummy);
      m_Stopwords.put("ie", dummy);
      m_Stopwords.put("if", dummy);
      m_Stopwords.put("ignored", dummy);
      m_Stopwords.put("immediate", dummy);
      m_Stopwords.put("in", dummy);
      m_Stopwords.put("inasmuch", dummy);
      m_Stopwords.put("inc", dummy);
      m_Stopwords.put("indeed", dummy);
      m_Stopwords.put("indicate", dummy);
      m_Stopwords.put("indicated", dummy);
      m_Stopwords.put("indicates", dummy);
      m_Stopwords.put("inner", dummy);
      m_Stopwords.put("insofar", dummy);
      m_Stopwords.put("instead", dummy);
      m_Stopwords.put("into", dummy);
      m_Stopwords.put("inward", dummy);
      m_Stopwords.put("is", dummy);
      m_Stopwords.put("it", dummy);
      m_Stopwords.put("its", dummy);
      m_Stopwords.put("itself", dummy);
      m_Stopwords.put("j", dummy);
      m_Stopwords.put("just", dummy);
      m_Stopwords.put("k", dummy);
      m_Stopwords.put("keep", dummy);
      m_Stopwords.put("keeps", dummy);
      m_Stopwords.put("kept", dummy);
      m_Stopwords.put("know", dummy);
      m_Stopwords.put("knows", dummy);
      m_Stopwords.put("known", dummy);
      m_Stopwords.put("l", dummy);
      m_Stopwords.put("last", dummy);
      m_Stopwords.put("lately", dummy);
      m_Stopwords.put("later", dummy);
      m_Stopwords.put("latter", dummy);
      m_Stopwords.put("latterly", dummy);
      m_Stopwords.put("least", dummy);
      m_Stopwords.put("less", dummy);
      m_Stopwords.put("lest", dummy);
      m_Stopwords.put("let", dummy);
      m_Stopwords.put("like", dummy);
      m_Stopwords.put("liked", dummy);
      m_Stopwords.put("likely", dummy);
      m_Stopwords.put("little", dummy);
      m_Stopwords.put("ll", dummy); //added to avoid words like you'll,I'll etc.
      m_Stopwords.put("look", dummy);
      m_Stopwords.put("looking", dummy);
      m_Stopwords.put("looks", dummy);
      m_Stopwords.put("ltd", dummy);
      m_Stopwords.put("m", dummy);
      m_Stopwords.put("mainly", dummy);
      m_Stopwords.put("many", dummy);
      m_Stopwords.put("may", dummy);
      m_Stopwords.put("maybe", dummy);
      m_Stopwords.put("me", dummy);
      m_Stopwords.put("mean", dummy);
      m_Stopwords.put("meanwhile", dummy);
      m_Stopwords.put("merely", dummy);
      m_Stopwords.put("might", dummy);
      m_Stopwords.put("more", dummy);
      m_Stopwords.put("moreover", dummy);
      m_Stopwords.put("most", dummy);
      m_Stopwords.put("mostly", dummy);
      m_Stopwords.put("much", dummy);
      m_Stopwords.put("must", dummy);
      m_Stopwords.put("my", dummy);
      m_Stopwords.put("myself", dummy);
      m_Stopwords.put("n", dummy);
      m_Stopwords.put("name", dummy);
      m_Stopwords.put("namely", dummy);
      m_Stopwords.put("nd", dummy);
      m_Stopwords.put("near", dummy);
      m_Stopwords.put("nearly", dummy);
      m_Stopwords.put("necessary", dummy);
      m_Stopwords.put("need", dummy);
      m_Stopwords.put("needs", dummy);
      m_Stopwords.put("neither", dummy);
      m_Stopwords.put("never", dummy);
      m_Stopwords.put("nevertheless", dummy);
      m_Stopwords.put("new", dummy);
      m_Stopwords.put("next", dummy);
      m_Stopwords.put("nine", dummy);
      m_Stopwords.put("no", dummy);
      m_Stopwords.put("nobody", dummy);
      m_Stopwords.put("non", dummy);
      m_Stopwords.put("none", dummy);
      m_Stopwords.put("noone", dummy);
      m_Stopwords.put("nor", dummy);
      m_Stopwords.put("normally", dummy);
      m_Stopwords.put("not", dummy);
      m_Stopwords.put("nothing", dummy);
      m_Stopwords.put("novel", dummy);
      m_Stopwords.put("now", dummy);
      m_Stopwords.put("nowhere", dummy);
      m_Stopwords.put("o", dummy);
      m_Stopwords.put("obviously", dummy);
      m_Stopwords.put("of", dummy);
      m_Stopwords.put("off", dummy);
      m_Stopwords.put("often", dummy);
      m_Stopwords.put("oh", dummy);
      m_Stopwords.put("ok", dummy);
      m_Stopwords.put("okay", dummy);
      m_Stopwords.put("old", dummy);
      m_Stopwords.put("on", dummy);
      m_Stopwords.put("once", dummy);
      m_Stopwords.put("one", dummy);
      m_Stopwords.put("ones", dummy);
      m_Stopwords.put("only", dummy);
      m_Stopwords.put("onto", dummy);
      m_Stopwords.put("or", dummy);
      m_Stopwords.put("other", dummy);
      m_Stopwords.put("others", dummy);
      m_Stopwords.put("otherwise", dummy);
      m_Stopwords.put("ought", dummy);
      m_Stopwords.put("our", dummy);
      m_Stopwords.put("ours", dummy);
      m_Stopwords.put("ourselves", dummy);
      m_Stopwords.put("out", dummy);
      m_Stopwords.put("outside", dummy);
      m_Stopwords.put("over", dummy);
      m_Stopwords.put("overall", dummy);
      m_Stopwords.put("own", dummy);
      m_Stopwords.put("p", dummy);
      m_Stopwords.put("particular", dummy);
      m_Stopwords.put("particularly", dummy);
      m_Stopwords.put("per", dummy);
      m_Stopwords.put("perhaps", dummy);
      m_Stopwords.put("placed", dummy);
      m_Stopwords.put("please", dummy);
      m_Stopwords.put("plus", dummy);
      m_Stopwords.put("possible", dummy);
      m_Stopwords.put("presumably", dummy);
      m_Stopwords.put("probably", dummy);
      m_Stopwords.put("provides", dummy);
      m_Stopwords.put("q", dummy);
      m_Stopwords.put("que", dummy);
      m_Stopwords.put("quite", dummy);
      m_Stopwords.put("qv", dummy);
      m_Stopwords.put("r", dummy);
      m_Stopwords.put("rather", dummy);
      m_Stopwords.put("rd", dummy);
      m_Stopwords.put("re", dummy);
      m_Stopwords.put("really", dummy);
      m_Stopwords.put("reasonably", dummy);
      m_Stopwords.put("regarding", dummy);
      m_Stopwords.put("regardless", dummy);
      m_Stopwords.put("regards", dummy);
      m_Stopwords.put("relatively", dummy);
      m_Stopwords.put("respectively", dummy);
      m_Stopwords.put("right", dummy);
      m_Stopwords.put("s", dummy);
      m_Stopwords.put("said", dummy);
      m_Stopwords.put("same", dummy);
      m_Stopwords.put("saw", dummy);
      m_Stopwords.put("say", dummy);
      m_Stopwords.put("saying", dummy);
      m_Stopwords.put("says", dummy);
      m_Stopwords.put("second", dummy);
      m_Stopwords.put("secondly", dummy);
      m_Stopwords.put("see", dummy);
      m_Stopwords.put("seeing", dummy);
      m_Stopwords.put("seem", dummy);
      m_Stopwords.put("seemed", dummy);
      m_Stopwords.put("seeming", dummy);
      m_Stopwords.put("seems", dummy);
      m_Stopwords.put("seen", dummy);
      m_Stopwords.put("self", dummy);
      m_Stopwords.put("selves", dummy);
      m_Stopwords.put("sensible", dummy);
      m_Stopwords.put("sent", dummy);
      m_Stopwords.put("serious", dummy);
      m_Stopwords.put("seriously", dummy);
      m_Stopwords.put("seven", dummy);
      m_Stopwords.put("several", dummy);
      m_Stopwords.put("shall", dummy);
      m_Stopwords.put("she", dummy);
      m_Stopwords.put("should", dummy);
      m_Stopwords.put("since", dummy);
      m_Stopwords.put("six", dummy);
      m_Stopwords.put("so", dummy);
      m_Stopwords.put("some", dummy);
      m_Stopwords.put("somebody", dummy);
      m_Stopwords.put("somehow", dummy);
      m_Stopwords.put("someone", dummy);
      m_Stopwords.put("something", dummy);
      m_Stopwords.put("sometime", dummy);
      m_Stopwords.put("sometimes", dummy);
      m_Stopwords.put("somewhat", dummy);
      m_Stopwords.put("somewhere", dummy);
      m_Stopwords.put("soon", dummy);
      m_Stopwords.put("sorry", dummy);
      m_Stopwords.put("specified", dummy);
      m_Stopwords.put("specify", dummy);
      m_Stopwords.put("specifying", dummy);
      m_Stopwords.put("still", dummy);
      m_Stopwords.put("sub", dummy);
      m_Stopwords.put("such", dummy);
      m_Stopwords.put("sup", dummy);
      m_Stopwords.put("sure", dummy);
      m_Stopwords.put("t", dummy);
      m_Stopwords.put("take", dummy);
      m_Stopwords.put("taken", dummy);
      m_Stopwords.put("tell", dummy);
      m_Stopwords.put("tends", dummy);
      m_Stopwords.put("th", dummy);
      m_Stopwords.put("than", dummy);
      m_Stopwords.put("thank", dummy);
      m_Stopwords.put("thanks", dummy);
      m_Stopwords.put("thanx", dummy);
      m_Stopwords.put("that", dummy);
      m_Stopwords.put("thats", dummy);
      m_Stopwords.put("the", dummy);
      m_Stopwords.put("their", dummy);
      m_Stopwords.put("theirs", dummy);
      m_Stopwords.put("them", dummy);
      m_Stopwords.put("themselves", dummy);
      m_Stopwords.put("then", dummy);
      m_Stopwords.put("thence", dummy);
      m_Stopwords.put("there", dummy);
      m_Stopwords.put("thereafter", dummy);
      m_Stopwords.put("thereby", dummy);
      m_Stopwords.put("therefore", dummy);
      m_Stopwords.put("therein", dummy);
      m_Stopwords.put("theres", dummy);
      m_Stopwords.put("thereupon", dummy);
      m_Stopwords.put("these", dummy);
      m_Stopwords.put("they", dummy);
      m_Stopwords.put("think", dummy);
      m_Stopwords.put("third", dummy);
      m_Stopwords.put("this", dummy);
      m_Stopwords.put("thorough", dummy);
      m_Stopwords.put("thoroughly", dummy);
      m_Stopwords.put("those", dummy);
      m_Stopwords.put("though", dummy);
      m_Stopwords.put("three", dummy);
      m_Stopwords.put("through", dummy);
      m_Stopwords.put("throughout", dummy);
      m_Stopwords.put("thru", dummy);
      m_Stopwords.put("thus", dummy);
      m_Stopwords.put("to", dummy);
      m_Stopwords.put("together", dummy);
      m_Stopwords.put("too", dummy);
      m_Stopwords.put("took", dummy);
      m_Stopwords.put("toward", dummy);
      m_Stopwords.put("towards", dummy);
      m_Stopwords.put("tried", dummy);
      m_Stopwords.put("tries", dummy);
      m_Stopwords.put("truly", dummy);
      m_Stopwords.put("try", dummy);
      m_Stopwords.put("trying", dummy);
      m_Stopwords.put("twice", dummy);
      m_Stopwords.put("two", dummy);
      m_Stopwords.put("u", dummy);
      m_Stopwords.put("un", dummy);
      m_Stopwords.put("under", dummy);
      m_Stopwords.put("unfortunately", dummy);
      m_Stopwords.put("unless", dummy);
      m_Stopwords.put("unlikely", dummy);
      m_Stopwords.put("until", dummy);
      m_Stopwords.put("unto", dummy);
      m_Stopwords.put("up", dummy);
      m_Stopwords.put("upon", dummy);
      m_Stopwords.put("us", dummy);
      m_Stopwords.put("use", dummy);
      m_Stopwords.put("used", dummy);
      m_Stopwords.put("useful", dummy);
      m_Stopwords.put("uses", dummy);
      m_Stopwords.put("using", dummy);
      m_Stopwords.put("usually", dummy);
      m_Stopwords.put("uucp", dummy);
      m_Stopwords.put("v", dummy);
      m_Stopwords.put("value", dummy);
      m_Stopwords.put("various", dummy);
      m_Stopwords.put("ve", dummy); //added to avoid words like I've,you've etc.
      m_Stopwords.put("very", dummy);
      m_Stopwords.put("via", dummy);
      m_Stopwords.put("viz", dummy);
      m_Stopwords.put("vs", dummy);
      m_Stopwords.put("w", dummy);
      m_Stopwords.put("want", dummy);
      m_Stopwords.put("wants", dummy);
      m_Stopwords.put("was", dummy);
      m_Stopwords.put("way", dummy);
      m_Stopwords.put("we", dummy);
      m_Stopwords.put("welcome", dummy);
      m_Stopwords.put("well", dummy);
      m_Stopwords.put("went", dummy);
      m_Stopwords.put("were", dummy);
      m_Stopwords.put("what", dummy);
      m_Stopwords.put("whatever", dummy);
      m_Stopwords.put("when", dummy);
      m_Stopwords.put("whence", dummy);
      m_Stopwords.put("whenever", dummy);
      m_Stopwords.put("where", dummy);
      m_Stopwords.put("whereafter", dummy);
      m_Stopwords.put("whereas", dummy);
      m_Stopwords.put("whereby", dummy);
      m_Stopwords.put("wherein", dummy);
      m_Stopwords.put("whereupon", dummy);
      m_Stopwords.put("wherever", dummy);
      m_Stopwords.put("whether", dummy);
      m_Stopwords.put("which", dummy);
      m_Stopwords.put("while", dummy);
      m_Stopwords.put("whither", dummy);
      m_Stopwords.put("who", dummy);
      m_Stopwords.put("whoever", dummy);
      m_Stopwords.put("whole", dummy);
      m_Stopwords.put("whom", dummy);
      m_Stopwords.put("whose", dummy);
      m_Stopwords.put("why", dummy);
      m_Stopwords.put("will", dummy);
      m_Stopwords.put("willing", dummy);
      m_Stopwords.put("wish", dummy);
      m_Stopwords.put("with", dummy);
      m_Stopwords.put("within", dummy);
      m_Stopwords.put("without", dummy);
      m_Stopwords.put("wonder", dummy);
      m_Stopwords.put("would", dummy);
      m_Stopwords.put("would", dummy);
      m_Stopwords.put("x", dummy);
      m_Stopwords.put("y", dummy);
      m_Stopwords.put("yes", dummy);
      m_Stopwords.put("yet", dummy);
      m_Stopwords.put("you", dummy);
      m_Stopwords.put("your", dummy);
      m_Stopwords.put("yours", dummy);
      m_Stopwords.put("yourself", dummy);
      m_Stopwords.put("yourselves", dummy);
      m_Stopwords.put("z", dummy);
      m_Stopwords.put("zero", dummy);

    }
  }
  
  /** 
   * Returns true if the given string is a stop word.
   */
  public static boolean isStopword(String str) {

    return m_Stopwords.containsKey(str.toLowerCase());
  }
}


