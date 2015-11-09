//  Ajavt: Temporal Expression Tagger for Estonian
//  Copyright (C) 2009-2015  University of Tartu
//  Author:   Siim Orasmaa
//  Contact:  siim . orasmaa {at} ut . ee
//  
//  This program is free software; you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation; either version 2 of the License, or
//  (at your option) any later version.
//
//  This program is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.

package ee.ut.soras.ajavtV2.util;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;


/**
 *  Valik eeldefineeritud mustreid ja s&otilde;net&ouml;&ouml;tlusutiliite.
 * 
 * @author Siim Orasmaa
 */
public class TextUtils {

	/**
	 *   Kuup&otilde;hine aja esituse formaat. Rakenduseyleseks kasutamiseks sobitamisel.
	 */
	public final static Pattern kuuPohineFormaat = 
		Pattern.compile("\\d\\d\\d\\d-\\d\\d?-\\d\\d?T\\d\\d:\\d\\d");

	/**
	 *   TimeML dokumendi loomise kuupäeva formaat. NB! Eeldab, et atribuut "value" on alati enne atribuuti "functionInDocument"; 
	 */
	public final static Pattern timeMLDocumentCreationTime = 
		Pattern.compile("<[^>]*(([0-9X]{4}-[0-9X]{2}-[0-9X]{2}(T[0-9X]{2}:[0-9X]{2})?)[^>]*functionInDocument=\"CREATION_TIME\")[^>]*>");

	/**
	 *   <i>Tag</i> käsitsimärgendatud testifailis, mis märgib seda, et automaatmärgendusel tuleb vaikimisi kasutatava
	 *   reeglikomplekti asemel kasutada reegleid etteantud failist.
	 */
	public final static Pattern customRuleFile = Pattern.compile("<rules file=\"([^\"]+)\"\\s*/>", Pattern.CASE_INSENSITIVE);
	
	/**
	 *   Muster punktatsiooni eemaldamiseks s6na algusest. <tt>RegExpSonaMall</tt> ja <tt>TavaTekstSonaMall</tt>
	 *  sobitavad yldjuhul ainult s6nesid, mis on sellise puhastuse l2bi teinud.
	 *  <p>
	 *  Lisaks <tt>\p{Punct}</tt> klassi punktatsioonile eemaldame ka: 
	 *    <i>guillemet</i>-tyypi jutumargi (kasutusel sagedasti Postimehes)
	 */	
	private final static Pattern removePunctationFromBeginning =
		Pattern.compile("^(\\p{Punct}|\u00AB)+");
		
	/**
	 *   Muster punktatsiooni eemaldamiseks s6na l6pust. <tt>RegExpSonaMall</tt> ja <tt>TavaTekstSonaMall</tt>
	 *  sobitavad yldjuhul ainult s6nesid, mis on sellise puhastuse l2bi teinud.
	 *  <p> 
	 *  Lisaks <tt>\p{Punct}</tt> klassi punktatsioonile eemaldame ka: 
	 *    <i>guillemet</i>-tyypi jutumargi (kasutusel sagedasti Postimehes)
	 */
	private final static Pattern removePunctationFromEnding =
		Pattern.compile("(\\p{Punct}|\u00BB)+$");
	
	/**
	 *    Muster numbri ja spetsiifilise punktatsiooniga l&otilde;ppevate s&otilde;nede
	 *   leidmiseks. Sellised s&otilde;ned on erandid, millel ei eemaldata l&otilde;pust
	 *   punktatsiooni, muidu nende sobitamine praeguse mehhanismi kohaselt eba&otilde;nnestub...
	 */
	private final static Pattern endsWithNumbersAndSpecificPunctation =
		Pattern.compile(".*[0-9]+(\\.|:|/|%|-|'|\\))+$");
	
	public final static Pattern beginningOfQuote = Pattern.compile("^(\u00AB|\")+");
	public final static Pattern endOfQuote		 = Pattern.compile("(\u00BB|\")+$");
	
	/**
	 *   Murdarvu muster (punkt murdosa eraldajana). 
	 */
	public final static Pattern musterMurdArv    = Pattern.compile("^[0-9]+\\.[0-9]+$");
	
	/**
	 *    Märgendatud ajaväljendi korrektne ID (standardi TimeML järgi); 
	 */
	public final static Pattern validTimeMLtid = Pattern.compile("^t[0-9]+$");
	
	/**
	 *  <i>Trim</i> - eemaldame koik sone alguses ja lopus olevad tyhikud, 
	 * tabulaatorid jm <i>whitespace</i> symbolid (kuni esimese mittetyhikuni 
	 * algusest ning alates viimasest mittetyhikust lopus). 
	 * 
	 * @param source
	 * @return
	 */	
	public static String trim(String sone){
		return (ltrim(sone)).replaceAll("\\s+$", "");
	}
	
	/**
	 *  <i>Left-trim</i> - eemaldame koik sone alguses olevad tyhikud, 
	 * tabulaatorid jm <i>whitespace</i> symbolid (kuni esimese mittetyhikuni). 
	 * 
	 * @param source
	 * @return
	 */
	public static String ltrim(String source) {
		return source.replaceAll("^\\s+", "");
	}
	
	/**
	 *  Eemaldame sone <code>source</code> algusest ja lopust punktatsiooni m&auml;rgid (java 
	 *  regulaaravaldise klassi <tt>\p{Punct}</tt> symbolid).
	 *  <p>
	 *  K&auml;sitleb erijuhtudena numbrimustreid (nt <i>28.05.2008. , 21:</i> jms),
	 *  mille puhul jatab lopust margid <tt>: / - .</tt> eemaldamata.
	 */
	public static String trimSurroundingPunctation(String source){
		if (endsWithNumbersAndSpecificPunctation.matcher(source).matches()){
			return (removePunctationFromBeginning.matcher(source)).replaceAll("");
		} else {
			return (removePunctationFromEnding.matcher(
					(removePunctationFromBeginning.matcher(source)).replaceAll("") )).replaceAll("");			
		}
	}

	/**
	 *   <p>
	 *   Normaliseerib (taandab yhele kujule) teatud erisymbolid s6nes:
	 *   <li>kriipsud (nt -, --, —)</li>
	 *   <li>HTML olemid &amp;lt; &amp;gt; &amp;amp &amp;pos; &amp;quot;</li>
	 *   <li>õ erikujud (ô, ō, ǒ) - !praegu jääb välja, tarbetu</li>
	 *   </p>
	 *   <br>
	 *   Infot symbolite ja nendele vastavate koodide kohta:<br>
	 *   http://www.fileformat.info/info/unicode/<br>
	 *   http://www.w3schools.com/tags/ref_entities.asp
	 *   <br>
	 *   <br>
	 */
	public static String normalizeSpecialSymbols(String input){
		// 1. Asendame k6ik kriipsude erikujud yhe kindla kriipsuga
		input = input.replaceAll("-{2,}", "-");
		input = input.replaceAll("(\u2212|\uFF0D|\u02D7|\uFE63|\u002D)", "-");
		input = input.replaceAll("(\u2010|\u2011|\u2012|\u2013|\u2014|\u2015)", "-");
		// 2. Asendame HTML olemid nendele vastavate märkidega
		input = input.replaceAll("&(quot|#34);", "\"");
		input = input.replaceAll("&(apos|#39);", "'");
		input = input.replaceAll("&(amp|#38);", "&");
		input = input.replaceAll("&(lt|#60);", "<");
		input = input.replaceAll("&(gt|#62);", ">");
		// 3. Viime õ ühtsele kujule - esialgu paistab, et see on tarbetu
		//input = input.replaceAll("(\u014D|\u00F4|\u01D2)", "\u00F5"); // väike õ  
		//input = input.replaceAll("(\u00D4|\u014C|\u01D1)", "\u00D5"); // suur Õ
		return input;
	}
	
	
	/**
	 *   Muudab etteantud sonet <code>str</code> selliselt, et selle pikkus on <code>expectedLength</code>. 
	 *  Kui sone on lyhem, lisab selle l6ppu v6i algusesse puuduoleva arvu tyhikuid (vastavalt sellele, kas 
	 *  <tt>modifyEnd</tt> on <tt>true</tt> v6i <tt>false</tt>); 
	 *  Kui sone on pikem ning <tt>modifyEnd == true</tt>, l6ikab selle l6pust yleoleva otsa maha.
	 *  Kui sone on pikem ning <tt>modifyEnd == false</tt>, l6ikab selle algusest yleoleva otsa maha. 
	 */
	public static String resizeString(String str, int expectedLength, boolean modifyEnd){
		if (str.length() == expectedLength){
			return str;
		} else {
			if (str.length() > expectedLength){
				if (modifyEnd){
					// L6ikame s6ne parajaks l6pust					
					return str.substring(0, expectedLength);
				} else {
					// L6ikame s6ne parajaks algusest
					return str.substring(str.length() - expectedLength, str.length());
				}
			} else {
				// Lisame l6ppu puuduoleva arvu tyhikuid
				StringBuilder sb = new StringBuilder(str);				
				int missingLength = expectedLength - str.length();
				while (missingLength > 0){
					if (modifyEnd){						
						sb.append(" ");
					} else {
						sb.insert(0, " ");
					}
					missingLength--;
				}
				return sb.toString();
			}
		}
	}

	/**
	 *   M22rab kindlaks, kas etteantud s6ne <tt>s</tt> sisaldab tähti <code>a-z</code> või mitte.
	 */
	public static boolean containsLetters(String s){
		for (int i = 0; i < s.length(); i++) {
			String subStr = s.substring(i, i+1);
			if (subStr.matches("[A-Za-z]")){
				return true;
			}
		}
		return false;
	}
	
	/**
	 *    Loob etteantud mustritahiste listi p6hjal paisktabeli v6tme. Sisuliselt toimub
	 *   t2histe konkateneerimine, mille k2igus pannakse t2histe vahele eraldajaks + m2rgid;
	 *   Kui lipp <tt>sorteeri</tt> on seatud, sorteeritakse t2hised enne konkateneerimist
	 *   t2hestikuliselt kasvavaks;   
	 */
	public static String looMustriTahisteVoti(List<String> mustriTahised, boolean sorteeri){
		if (sorteeri){
			Collections.sort(mustriTahised);
		}
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < mustriTahised.size(); i++) {
			sb.append( mustriTahised.get(i) );
			if (i < mustriTahised.size()-1){
				sb.append("+");
			}
		}
		return sb.toString();
	}

}
