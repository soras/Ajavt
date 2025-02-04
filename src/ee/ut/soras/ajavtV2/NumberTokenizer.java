//  Ajavt: Temporal Expression Tagger for Estonian
//  Copyright (C) 2009-2015  University of Tartu
//  Author:   Siim Orasmaa
//  Contact:  siim . orasmaa {at} ut . ee
//  
//  This program is released under dual license: either GNU General 
//  Public License v2.0 or Apache 2.0 License. 
//
//  Full copy of GNU General Public License v2.0 can be found at 
//  http://www.gnu.org/licenses/old-licenses/gpl-2.0.en.html .
//
//  Full copy of Apache 2.0 License can be found at 
//  http://www.apache.org/licenses/LICENSE-2.0 .
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. 

package ee.ut.soras.ajavtV2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ee.ut.soras.ajavtV2.util.TextUtils;

/**
 *  
 *  Klass, mille eesmärgiks on otsustada, kas numbreid sisaldav token (nt "N45", "9-11", "2009" vms)
 * tuleb enne ajaväljendite eraldamismustrite kontrollimist jagada eraldi tükkideks või tuleb seda
 * käsitleda tervikuna. Kui tükeldamist on tarvis, hallatakse seda samuti selle klassi raames;
 * 
 * @author Siim Orasmaa
 * 
 */
public class NumberTokenizer {

	/**
	 *   Muster kontrollimaks, kas antud sona sisaldab v2hemalt yhte numbrit, millega on k6rvuti mittenumber. 
	 */
	private final static Pattern musterSisaldabArvuJaMitteArvu  = Pattern.compile("(\\d\\D|\\D\\d)");
	
	/**
	 *   Kontrollib, kas antud sisendtokeni puhul on vajalik numbrite eraldi tokeniseerimine. 
	 */
	public static boolean isNumberTokenizationNeeded(String analyysitavSona){
		Matcher matcher1 = (musterSisaldabArvuJaMitteArvu).matcher(analyysitavSona);
		if ( matcher1.find() ){
			if (!passesHypenGuard(analyysitavSona)){
				return false;
			}
			if (!passesPointGuard(analyysitavSona)){
				return false;
			}
			if (!passesAcronymGuard(analyysitavSona)){
				return false;
			}
			if (!passesHTMLEntityGuard(analyysitavSona)){
				/* NB: praegu ei luba numbrimustrite tykeldamist, kui on sees numbrilised HTML olemid; Alternatiiv oleks HTML olemid 
				 * asendada ning seej2rel ikkagi lubada tykeldamist;
				 */
				return false;
			}
			return true;
		}
		return false;
	}
	
	
	private final static Pattern hypenPattern1 = 
		Pattern.compile("(\\d+([.:]\\d+)?-\\d+([.:]\\d+)?)(\\\\|/)(\\d+([.:]\\d+)?-\\d+([.:]\\d+)?)");

	private final static Pattern hypenPattern2 = 
		Pattern.compile("\\d+-\\d+-([a-z]|\u00F6).*");

	private final static Pattern hypenPattern3 = 
		Pattern.compile("\\d+-\\d+-");

	private final static Pattern hypenPattern4 = 
		Pattern.compile("\\d\\d\\d\\d-\\d+-\\d+");
	
	
	/**
	 *  Kontrollib, kas token rahuldab lubatud kriipsude mustreid.
	 */
	public static boolean passesHypenGuard(String analyysitavSona){
		String sona = TextUtils.normalizeSpecialSymbols(analyysitavSona);
		int occurrences = countNumberOfOccurrences(sona, "-");
		if (occurrences > 1){
			// Kui leidub rohkem kui yks kriips, laseme l2bi vaid piiratud juhud:
			// 1) Kellaajamuster, nt 9.00-10.30/10.00-11.30
			if ((hypenPattern1.matcher(sona)).matches()){
				return true;
			}
            // 2) Ajayhikutemuster, nt 14-20-aastased, 35-39-aastaste
			if ((hypenPattern2.matcher(sona)).matches()){
				return true;				
			}
            // 3) Ajayhikutemuster, nt 14-20- aastased
			if ((hypenPattern3.matcher(sona)).matches()){
				return true;				
			}
            // 4) ISO-muster, nt 2009-01-15
			if ((hypenPattern4.matcher(sona)).matches()){
				return true;				
			}
			return false;
		}
		return true;
	}

	private final static Pattern pointPattern1 = 
		Pattern.compile("\\d+\\.\\d+\\.\\d+\\.\\d+.*");
	
	/**
	 *  Kontrollib, kas token rahuldab lubatud punktide mustreid.
	 */
	public static boolean passesPointGuard(String analyysitavSona){
		// 1) Numbrimuster, mida ei l6huta: 3.2.12.2.8. ,  3.2.1.14.1.1. ,  3.2.12.2.8.3.1.1. ,  3.2.12.2.8.1.
		if ((pointPattern1.matcher(analyysitavSona)).matches()){
			return false;				
		}					
		return true;
	}
	
	private final static Pattern acronymPattern1 = 
		Pattern.compile("[A-Z][a-zA-Z]*-?\\d+.*");
	
	/**
	 *  Kontrollib, kas token ei rahuldaks akronyymide mustrit.
	 */
	public static boolean passesAcronymGuard(String analyysitavSona){
		// 1) Akronyymimustrid, mida ei l6huta: A2000, Niva-2123, F-2000, VAZ-2106 
		if ((acronymPattern1.matcher(analyysitavSona)).matches()){
			return false;				
		}					
		return true;
	}
	
	private final static Pattern htmlEntityPattern1 = Pattern.compile(".*&#[0-9]+;.*");
		
	/**
	*  Kontrollib, et token ei sisaldaks numbritega edasiantud HTML-olemit;
	*/
	public static boolean passesHTMLEntityGuard(String analyysitavSona){
			if ((htmlEntityPattern1.matcher(analyysitavSona)).matches()){
				return false;				
			}					
			return true;
	}
	
	/**
	 *  Leiab, mitu korda esineb subString superString'i sees.
	 */
	public static int countNumberOfOccurrences(String superString, String subString){
		int occs = 0;
		int nextIndex = 0;
		while (nextIndex != -1){
			nextIndex = superString.indexOf( subString, nextIndex );
			if (nextIndex != -1){
				occs++;
				nextIndex++;
			}
		}
		return occs;
	}
	
	/**
	 *   Jagab numbreid & punktatsiooni sisaldava terviksone eraldi sonedeks, nt
	 *   <ul>
	 *   <li> "2007.03"    => "2007."  "03"   
	 *   <li> "03.05.2009" => "03."  "05."   "2009"
	 *   <li> "35,6" => "35,6"
	 *   </ul>
	 *   <p>
	 *   NB! Kui numbrite vahel on eraldajaks koma, siis tykeldamist ei toimu.
	 *   <p>
	 *   Lipp <tt>keepNonDigits</tt> m22rab, kas mittenumbrid tuleb alles hoida v6i mitte.
	 */
	public static List<String> extractNumbersWithTrailingPunctation(String source, boolean keepNonDigits){
		List<String> results = new ArrayList<String>();
		StringBuilder sb = new StringBuilder();
		boolean lastWasAcceptable = false; // ehk oli koma v6i number
		//boolean
		for (int i = 0; i < source.length(); i++) {
			String substr = source.substring(i, i+1);
			boolean thisIsDigit = (substr.compareTo("0") >= 0 && substr.compareTo("9") <= 0);
			boolean thisIsComma = (substr.equals(","));
			boolean thisIsAcceptable = (thisIsDigit || thisIsComma);
			if (i > 0 && thisIsAcceptable && !lastWasAcceptable){
				results.add(sb.toString());
				sb = new StringBuilder();
			}
			if (keepNonDigits || (!keepNonDigits && thisIsAcceptable)){
				sb.append(substr);				
			}
			lastWasAcceptable = thisIsAcceptable;
			if (i == source.length() -1 && sb.length() > 0){
				// Ja viimane ...
				results.add(sb.toString());
			}
		}
		return results;
	}
	
	
}
