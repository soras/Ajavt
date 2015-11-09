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

package ee.ut.soras.ajavtV2;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.AjavtSona.GRAMMATILINE_AEG;
import ee.ut.soras.ajavtV2.util.ArvSonaFraasideTuvastaja;
import ee.ut.soras.ajavtV2.util.LogiPidaja;
import ee.ut.soras.ajavtV2.util.TextUtils;
import ee.ut.soras.wrappers.EstyhmmWrapper;
import ee.ut.soras.wrappers.impl.T3MestaReader;
import ee.ut.soras.wrappers.impl.T3OLPReader;
import ee.ut.soras.wrappers.impl.VabaMorfJSONReader;
import ee.ut.soras.wrappers.mudel.MorfAnRida;
import ee.ut.soras.wrappers.mudel.MorfAnSona;

/**
 *  Eelt&ouml;&ouml;tlus:
 *  <ol>
 *   <li> Morfoloogiline analyys & teksti sonadeks tykeldamine;
 *   <li> Ainult numbritest koosnevate s6nade edasine tykeldamine: "23.07.2009" => "23.",  "07.",  "2009"
 *   <li> Analyysitud s6nade pakendamine (AjavtSona) klassidesse;
 *   <li> Tuvastatakse heuristiliselt:
 *        <ol>
 *          <li> potentsiaalsed lauselõpud; </li>
 *          <li> potentsiaalsed ajavahemiku l&otilde;pp- ja alguspunktid; </li>
 *          <li> verbide grammatilised ajad; </li>
 *          <li> arvs6nafraasid; </li>
 *        </ol>
 *   </li>
 *  </ol> 
 * 
 * @author Siim Orasmaa
 */
public class EelTootlus { 
	
	public static LogiPidaja logi = null;
	
	/** Muster: Potentsiaalne lauselopp, millele jargneb potentsiaalne lausealgus. */	
	private static Pattern musterLauseLoppLauseAlgus = Pattern.compile("[?!.](\\s+)\\p{Lu}");
	/** Muster: Komakoht kahe sona vahel; */
	private static Pattern musterKomaSonadeVahel     = Pattern.compile("(\\p{Alpha}|[0-9])([,]\\s+)(\\p{Alpha}|[0-9])");

	/** Arvumuster, mille lopus on kriips. */
	private static Pattern musterArvudeKriipslopp    = Pattern.compile("^(\\d|[.,!?])+-$");
	/** Muster: -st lopuline sona v6i arv. Potentsiaalne vahemiku algus. */	
	private static Pattern musterSTlopp              = Pattern.compile("^.+(-)?st$");
	/** Muster: -st lopuline sona v6i arv, mis pole vahemiku algus. */	
	private static Pattern musterPoleSTlopp          = Pattern.compile("^(august|p\u00E4rast)$", Pattern.UNICODE_CASE);
	/** Muster: -ni lopuline sona v6i arv. Potentsiaalne vahemiku lopp. */	
	private static Pattern musterNIlopp              = Pattern.compile("^.+(-)?ni$");
	/** Muster: -ni lopuline sona v6i arv, pole vahemiku lopp. */
	private static Pattern musterPoleNIlopp          = Pattern.compile("^(juuni|kuni)$");
	
	/**
	 *   Muster: Verbi olevikulisust v2ljendavad tunnused <tt>t3mesta</tt> v&auml;ljundis (k6ik k6neviisid koos). 
	 */
	private final static Pattern verbOlevik = Pattern.compile("^(b|d|vad|te|me|n|neg o|t(a|akse)||k(s|sid|sime|sin|site)|neg ks|o|ge|gem|gu|neg g(e|em|u)|tagu|taks|neg vat|vat|tavat|tav|v),");

	/**
	 *   Muster: Verbi lihtminevikulisust v2ljendavad tunnused <tt>t3mesta</tt> v&auml;ljundis. 
	 */
	private final static Pattern verbLihtminevik = Pattern.compile("^(s|sid|sime|sin|site|ti|neg (nud|tud)),");

	/**
	 *   Muster: Tingiva v6i kaudse k6neviisi verbi lihtminevikulisust v2ljendavad tunnused <tt>t3mesta</tt> v&auml;ljundis. 
	 */
	private final static Pattern verbTingivKaudneMinevik = Pattern.compile("^(k(s|sid|sime|sin|site)|neg ks|vat|tavat|neg vat),");
	
	/**
	 *   Muster: Yksiku verbi yldminevikulisust v2ljendavad tunnused.
	 */
	private final static Pattern verbYldMinevik = Pattern.compile("^(nu(ks|ksid|ksime|ksin|ksite|vat)|tu(ks|vat)|neg nuks),");
	
	/**
	 *   Muster: Verb-Kesks6na mineviku vorm.
	 */
	private final static Pattern verbKeskSonaMinevikuVorm = Pattern.compile("^(nud|tud),");

	/**
	 *   Muster: Minevikul6puline kesks6natyvi.
	 */
	private final static Pattern ksTyveMinevikuLopp = Pattern.compile("^(.+)(dud|tud|nud)$");
	
	/**
	 *   Muster: Olevikul6puline kesks6natyvi.
	 */
	private final static Pattern ksTyveOlevikuLopp = Pattern.compile("^(.+)(v)$");	

	
	/**
	 *   Kasutab heuristikuid (lausel6ppude maaramine, komade eraldamine s6nade kyljest),
	 *  et viia sisendtekst morf analysaatori/yhestaja jaoks sobivamale kujule.
	 */
	public static String prepareTextForMorphAnalysis(String tekst){
		// --------------------------------------------------------------------
		// *) Heuristik: paneme v6imalike lausel6pukohtade vahele reavahetused
		//    (morf analysaatori n6ue sisendile)
		// --------------------------------------------------------------------
		StringBuffer sb = new StringBuffer( tekst );
		Matcher matcher = musterLauseLoppLauseAlgus.matcher( sb );
		while (matcher.find()){
			if (matcher.groupCount() > 0){
				int startIndex = matcher.start(1);
				if (startIndex != -1){
					sb.replace(startIndex, startIndex+1, System.getProperty("line.separator") );
				}
			}
		}
		// --------------------------------------------------------------------		
		// Heuristik: t6stame komad eelnevatest t2htedest lahku: selleks, et 
		// morf yhestaja tootaks tekstil paremini, ning samuti selleks, et ei
		// tekiks yleliigseid liitmisi ... 
		// --------------------------------------------------------------------		
		matcher = musterKomaSonadeVahel.matcher( sb );
		while (matcher.find()){
			if (matcher.groupCount() > 0){
				int startIndex = matcher.start(2);
				if (startIndex != -1){
					sb.replace(startIndex, startIndex+1, " , " );
				}
			}
		}		
		return sb.toString();
	}
	
	/**
	 *   Eeltootluse läbiviimine, mille puhul analyysitakse sisentekst esmalt v2lise morf analysaatori (<code>t3mesta</code>)  
	 *   abil ning seej2rel viiakse l2bi tavalised eeltootluse sammud (vt meetodit eeltootlus ).
	 */
	public static List<AjavtSona> eeltootlusValiseMorfAnalyysiga(EstyhmmWrapper wrapper, String tekst)
			throws Exception {
		tekst = prepareTextForMorphAnalysis(tekst);
		// --------------------------------------------------------------------
		// *) Sooritame estyhmm abil sisendteksti morf analyysi
		// --------------------------------------------------------------------
		String morfAnOutput = wrapper.process(tekst);
		// --------------------------------------------------------------------
		// *) Paigutame morf analyysi tulemused andmemudelisse
		// --------------------------------------------------------------------
		return eeltootlus( morfAnOutput );
	}

	/**
	 *   Eeltootluse läbiviimine <code>t3mesta</code> (puhas morf analyys, ilma lausepiiride jms m2rgenduseta) 
	 *   formaadis sisendi puhul.
	 *   
	 *   Etteantud sisendtekstil:
	 *   <li> Jagab teksti sonadeks ja eraldab s6nade morf analyysid;
	 *   <li> Teeb heuristiliselt kindlaks mitmed olulised tunnused (lauselopud,
	 *        verbide grammatilised ajad jms);
	 *   <li> Pakendab sonad koos analyysitulemuste/tunnustega klassidesse AjavtSona;
	 *   <li> Tuvastab tekstis arvsonafraasid;
	 */
	public static List<AjavtSona> eeltootlus(String morfAnalyzedText) throws Exception {
		// Moodustame sisendist BufferedReader'i
		BufferedReader inputReader = new BufferedReader( new StringReader(morfAnalyzedText) );
		// Eraldame morfoloogiliselt analyysitud tekstis6nad (+ lausem2rgistus)
		List<MorfAnSona> morfAnalyysitud = T3MestaReader.parseT3mestatext(inputReader);
		inputReader.close();
		// Loome uue listi, kuhu l2hevad tulemused ...
		List<AjavtSona>  eelt88deldud    = new ArrayList<AjavtSona>( morfAnalyysitud.size() );
		// V6tame kasutusele uue arvutuvastaja komponendi
		ArvSonaFraasideTuvastaja arvuTuvastaja = new ArvSonaFraasideTuvastaja( eelt88deldud );
		List<Object> eelnevaVerbiInfo = new ArrayList<Object>(2);
		AjavtSona eelmineSona = null;
		for (int i = 0; i < morfAnalyysitud.size(); i++) {
			MorfAnSona sona = morfAnalyysitud.get(i);
			// 1.1) Loome vastava AjaVT sõna (tuvastaja töötlusandmeid sisaldav sõna)
			lisaUusAjavtSona(eelt88deldud, sona, arvuTuvastaja);
			eelmineSona = eelt88deldud.get(eelt88deldud.size()-1);
			// 1.2) Uuendame infot grammatilise aja kohta
			if (eelmineSona.onVerb()){
				// verb
				maaraGrammatilineAeg(eelmineSona, false, eelnevaVerbiInfo);
			} else if (eelmineSona.onAdjektiivPos()){
				// omaduss6na (potentsiaalne kesks6na)
				maaraGrammatilineAeg(eelmineSona, true, eelnevaVerbiInfo);
			}
			// Kui s6na j2rel on kindel lausepiir/osalausepiir, läheb eelnevaVerbiInfo tühjendamisele
			if (eelmineSona != null && (eelmineSona.onOlpKindelPiir() || eelmineSona.onLauseLopp()) ){
				eelnevaVerbiInfo.clear();
			}
		}
		return eelt88deldud;
	}
	
	/**
	 *   Eeltootluse läbiviimine <code>t3olp</code> formaadis failide puhul.
	 *   Etteantud sisendtekstil:
	 *   <li> Jagab teksti sonadeks ja eraldab s6nade morf analyysid;
	 *   <li> Teeb heuristiliselt kindlaks mitmed olulised tunnused (verbide grammatilised ajad jms);
	 *   <li> Pakendab sonad koos analyysitulemuste/tunnustega klassidesse AjavtSona;
	 *   <li> Tuvastab tekstis arvsonafraasid;
	 */
	public static List<AjavtSona> eeltootlusT3OLP(String sisendT3OLP) throws Exception {
		// Moodustame sisendist BufferedReader'i
		BufferedReader inputReader = new BufferedReader(new StringReader(sisendT3OLP));
		// Eraldame morfoloogiliselt analyysitud tekstis6nad (+ lausem2rgistus)
		List<MorfAnSona> morfAnalyysitud = T3OLPReader.parseT3OLPtext(inputReader);
		inputReader.close();
		// Loome uue listi, kuhu l2hevad tulemused ...
		List<AjavtSona>  eelt88deldud    = new ArrayList<AjavtSona>( morfAnalyysitud.size() );
		// V6tame kasutusele uue arvutuvastaja komponendi
		ArvSonaFraasideTuvastaja arvuTuvastaja = new ArvSonaFraasideTuvastaja( eelt88deldud );
		List<Object> eelnevaVerbiInfo = new ArrayList<Object>(2);
		AjavtSona eelmineSona = null;
		for (int i = 0; i < morfAnalyysitud.size(); i++) {
			MorfAnSona sona = morfAnalyysitud.get(i);
			// 1.1) Loome vastava AjaVT sõna (tuvastaja töötlusandmeid sisaldav sõna)
			lisaUusAjavtSona(eelt88deldud, sona, arvuTuvastaja);
			eelmineSona = eelt88deldud.get(eelt88deldud.size()-1);
			// 1.2) Uuendame infot grammatilise aja kohta
			if (eelmineSona.onVerb()){
				// verb
				maaraGrammatilineAeg(eelmineSona, false, eelnevaVerbiInfo);
			} else if (eelmineSona.onAdjektiivPos()){
				// omaduss6na (potentsiaalne kesks6na)
				maaraGrammatilineAeg(eelmineSona, true, eelnevaVerbiInfo);
			}
			// Kui s6na j2rel on kindel lausepiir/osalausepiir, läheb eelnevaVerbiInfo tühjendamisele
			if (eelmineSona != null && (eelmineSona.onOlpKindelPiir() || eelmineSona.onLauseLopp())){
				eelnevaVerbiInfo.clear();
			}
		}
		return eelt88deldud;
	}
	
	/**
	 *   Eeltootluse läbiviimine vabamorfi <code>json</code> formaadis failide puhul.
	 *   Etteantud sisendtekstil:
	 *   <li> Jagab teksti sonadeks ja eraldab s6nade morf analyysid;
	 *   <li> Teeb heuristiliselt kindlaks mitmed olulised tunnused (verbide grammatilised ajad jms);
	 *   <li> Pakendab sonad koos analyysitulemuste/tunnustega klassidesse AjavtSona;
	 *   <li> Tuvastab tekstis arvsonafraasid;
	 */
	public static List<AjavtSona> eeltootlusJSON(String sisendJSON) throws Exception {
		// Moodustame sisendist BufferedReader'i
		BufferedReader inputReader = new BufferedReader(new StringReader(sisendJSON));
		// Eraldame morfoloogiliselt analyysitud tekstis6nad (+ lausem2rgistus)
		List<MorfAnSona> morfAnalyysitud = VabaMorfJSONReader.parseJSONtext(inputReader);
		inputReader.close();
		// Loome uue listi, kuhu l2hevad tulemused ...
		List<AjavtSona>  eelt88deldud    = new ArrayList<AjavtSona>( morfAnalyysitud.size() );
		// V6tame kasutusele uue arvutuvastaja komponendi
		ArvSonaFraasideTuvastaja arvuTuvastaja = new ArvSonaFraasideTuvastaja( eelt88deldud );
		List<Object> eelnevaVerbiInfo = new ArrayList<Object>(2);
		AjavtSona eelmineSona = null;
		for (int i = 0; i < morfAnalyysitud.size(); i++) {
			MorfAnSona sona = morfAnalyysitud.get(i);
			// 1.1) Loome vastava AjaVT sõna (tuvastaja töötlusandmeid sisaldav sõna)
			lisaUusAjavtSona(eelt88deldud, sona, arvuTuvastaja);
			eelmineSona = eelt88deldud.get(eelt88deldud.size()-1);
			// 1.2) Uuendame infot grammatilise aja kohta
			if (eelmineSona.onVerb()){
				// verb
				maaraGrammatilineAeg(eelmineSona, false, eelnevaVerbiInfo);
			} else if (eelmineSona.onAdjektiivPos()){
				// omaduss6na (potentsiaalne kesks6na)
				maaraGrammatilineAeg(eelmineSona, true, eelnevaVerbiInfo);
			}
			// Kui s6na j2rel on kindel lausepiir/osalausepiir, läheb eelnevaVerbiInfo tühjendamisele
			if (eelmineSona != null && (eelmineSona.onOlpKindelPiir() || eelmineSona.onLauseLopp())){
				eelnevaVerbiInfo.clear();
			}
		}
		return eelt88deldud;
	}
	
	
	
	/**
	 *   Leiab j2rgmise vaba positsiooni systeemisiseses token'ite j2rjestuses (AjavtSona-de j2rjestused), 
	 *  arvestades seda, milline oli eelmise MorfAnSona positsioon j2rjestuses ja kuidas see erines
	 *  vastava AjavtSona positsioonist jarjestuses;
	 */
	private static int findNextInnerTokenPosition(List<AjavtSona> tulemusList, MorfAnSona morfAnSona){
		//  Kui on lisatud m6ni s6na, tuleb positsioon arvutada viimase s6na morfi
		// positsiooni ja ajavts6na positsiooni arvestades; 
		if (!tulemusList.isEmpty()){
			int lastTokenPosition = tulemusList.get(tulemusList.size() - 1).getTokenPosition();
			int lastInnerPosition = tulemusList.get(tulemusList.size() - 1).getInnerTokenPosition();
			int tokenPositionsDifference = lastInnerPosition - lastTokenPosition;
			int currentTokenPosition = morfAnSona.getTokenPosition();
   		    return currentTokenPosition + tokenPositionsDifference;
		}
		// Vaikimisi on sama, mis morfAnSona positsioon (kui pole veel midagi lisatud v6i on lisatud kommentaariread)
		return morfAnSona.getTokenPosition();
	}
	
	/**
	 *    Moodustab etteantud morfoloogilise analyysi sonast uue AjavtSona ning lisab tulemusList-i.
	 *  Erijuhtudel (vt tapsemalt <code>TextUtils.extractNumbersWithTrailingPunctation</code>) jagatakse 
	 *  moodustatava sona mitmeks sonaks ning lisatakse listi mitu elementi.
	 *  <p>
	 *   Lisaks tulemuslisti taiendamisele toimub siin meetodis ka arvsonafraaside tuvastamine klassi 
	 *   <code>ArvSonaFraasideTuvastaja</code> abil: kui leitakse, et antud sona lopetab arvsonafraasi,
	 *   margendatakse viimased tulemusListi elemendid arvsonafraasi kuuluvateks.
	 *   <p>
	 */
	private static void lisaUusAjavtSona(List<AjavtSona> tulemusList, 
										 MorfAnSona morfAnSona, 
										 ArvSonaFraasideTuvastaja arvuTuvastaja){
		String analyysitavSona = morfAnSona.getAlgSona();
		if ((morfAnSona.getAlgSona()).length() > 50){
			// Häkk: Kui "sõnaks" on väga pikk märkide joru, võib see põhjustada järgneva 
			// analüüsi regulaaravaldiste kokkujooksmist Linux'i all, seetõttu lühendame 
			// selliseid sõnu (aga ainult analüüsiks!); 
			analyysitavSona = (morfAnSona.getAlgSona()).substring(0, 50);
		}
		boolean numbersSuccessfullyExtracted = false;
		if (NumberTokenizer.isNumberTokenizationNeeded(analyysitavSona)){
			// Kui s6ne sisaldab arve kujul "23.07.2009" (numbrid, mis on yksteisest punktatsiooniga
			// eraldatud), tokeniseerime sellise s6ne eraldi alams6nedeks ...
			List<String> subStrings = NumberTokenizer.
											extractNumbersWithTrailingPunctation(analyysitavSona, true);
			// Leiame viimase positsiooni ajavt-sõnade järjestuses 
			int innerTokenPosition = findNextInnerTokenPosition( tulemusList, morfAnSona );
			// Siin teeme h2ki: anname k6ikidele sonadele sama analyysi (kuna eeldatavasti on
			// tegu ainult numbritega, ei ole sellest erilist lugu).
			for (int i = 0; i < subStrings.size(); i++) {
				String string = subStrings.get(i);
				AjavtSona ajavtSona = new AjavtSona( morfAnSona, string );				
				leiaKasOnTeguPotentsiaalseAjavahemikuOsaga(ajavtSona, string);
				ajavtSona.setInnerTokenPosition( innerTokenPosition );
				if (subStrings.size() > 1){
					ajavtSona.setAtTokenBreakPosition(true);
				}
				tulemusList.add(ajavtSona);
				innerTokenPosition++;
				//  Uuendame lause- ja osalausepiiride informatsiooni:
				// kui algne s6na jagati tykkideks, lykkuvad piirid edasi ...
				if (i < subStrings.size()-1){
					ajavtSona.setOnLauseLopp( false );
			    	ajavtSona.setOlpOnKindelPiir( false );
				} else {
					ajavtSona.setOnLauseLopp( morfAnSona.onLauseLopp() );
			    	ajavtSona.setOlpOnKindelPiir( morfAnSona.onOlpKindelPiir() );
				}
			}
			numbersSuccessfullyExtracted = true;
		}
		if (!numbersSuccessfullyExtracted) {
			AjavtSona ajavtSona = new AjavtSona( morfAnSona, null );
			// Uuendame lause- ja osalausepiiride informatsiooni
	    	ajavtSona.setOnLauseLopp( morfAnSona.onLauseLopp() );
	    	ajavtSona.setOlpOnKindelPiir( morfAnSona.onOlpKindelPiir() );
	    	// Uuendame vahemiku-informatsiooni
			leiaKasOnTeguPotentsiaalseAjavahemikuOsaga(ajavtSona, analyysitavSona);
			// Leiame viimase positsiooni ajavt-sõnade järjestuses 
			int innerTokenPosition = findNextInnerTokenPosition( tulemusList, morfAnSona );
			tulemusList.add(ajavtSona);
			ajavtSona.setInnerTokenPosition( innerTokenPosition );
			// Viimasena: kui on tegu arvsonafraasiga, astume selle tuvastamises sammukese edasi
			arvuTuvastaja.tuvastaArvSonaFraas(ajavtSona);
		}
	}


	/**
	 *   Kontrollib, kas potentsiaalselt voib olla tegu ajaperioodi algust voi loppu markiva
	 *  sonaga ning positiivsete kontrollitulemuste korral taidab <tt>ajavtsona</tt> vastavad valjad.
	 *  <p>
	 *  Vahemikuotspunktide kindlakstegemisel j2lgitakse lihtsaid heuristikuid:
	 *  <ul>
	 *    <li> <i>-st</i> suffiks (+ seestytlev k22ne) on potentsiaalne vahemiku algus;
	 *    <li> <i>-</i>  suffiks numbrite jarel on potentsiaalne vahemiku algus;
	 *    <li> <i>-ni</i> suffiks (+ rajav k22ne) on potentsiaalne vahemiku lopp;
	 *  </ul>
	 */
	private static void leiaKasOnTeguPotentsiaalseAjavahemikuOsaga(AjavtSona ajavtsona, String sona){
		String sonaLowerCase                  = sona.toLowerCase();
		String sonaLowerCaseWithOutPunctation = TextUtils.trimSurroundingPunctation(sonaLowerCase);
		// 1) ST-ga l6ppev s6na: peab olema seestytlevas k22ndes
		if ((musterSTlopp.matcher( sonaLowerCaseWithOutPunctation )).matches()){
			// Kontrollime, kas on seestytlevas 
			if (ajavtsona.getAnalyysiTulemused() != null){
				List<MorfAnRida> analyysiTulemused = ajavtsona.getAnalyysiTulemused();
				for (MorfAnRida morfAnRida : analyysiTulemused) {
					if (morfAnRida.leiaKasMorfTunnusEsineb("el")){
						// Kontrollime, et pole stopps6na (morf analyysi vea t6ttu)
						if (!musterPoleSTlopp.matcher( sonaLowerCaseWithOutPunctation ).matches()){
							ajavtsona.setOnPotentsiaalneVahemikuAlgus(true);
						}
					}
				}
			}
		}
		// 2) Numbrid, mille l6pus on kriips: 
		if ((musterArvudeKriipslopp.matcher(sonaLowerCase)).matches()){
			ajavtsona.setOnPotentsiaalneVahemikuAlgus(true);
		}
		// 3) NI-ga l6ppev s6na: peab olema rajavas k22ndes
		if ((musterNIlopp.matcher(sonaLowerCaseWithOutPunctation)).matches()){
			// Kontrollime, kas on rajavas 
			if (ajavtsona.getAnalyysiTulemused() != null){
				List<MorfAnRida> analyysiTulemused = ajavtsona.getAnalyysiTulemused();
				for (MorfAnRida morfAnRida : analyysiTulemused) {
					if (morfAnRida.leiaKasMorfTunnusEsineb("ter")){
						// Kontrollime, et pole stopps6na (morf analyysi vea t6ttu)
						if (!musterPoleNIlopp.matcher( sonaLowerCaseWithOutPunctation ).matches()){
							ajavtsona.setOnPotentsiaalneVahemikuLopp(true);
						}
					}
				}
			}			
		}
	}
	
	/**
	 *   M&auml;&auml;rab sisendiks antud s6na (verbi v6i omaduss6na) grammatilise aja.
	 *   <p>
	 *   Kui <tt>onOmadusSona == false</tt>, eeldatakse, et sisends6na on verb. Sellisel
	 *   juhul t2histab parameeter <tt>sona</tt> jooksvalt vaadeldavat verbi, parameeter 
	 *   <tt>eelnevaVerbiInfo</tt> kannab olulist infot eelmise verbi kohta, mida uuendatakse 
	 *   jooksvalt.
	 *   <p>
	 *   Kui <tt>onOmadusSona == true</tt>, eeldatakse, et sisends6na on omaduss6na ning
	 *   kontrollitakse, kas on tegu kesks6naga, mille puhul on v6imalik m22rata kesks6na
	 *   aeg.
	 */
	private static void maaraGrammatilineAeg(AjavtSona sona, 
											 	boolean onOmadusSona, 
											 		List <Object> eelnevaVerbiInfo){
		// 1) Tegemist on verbiga ...
		if (!onOmadusSona){
			// Eraldame eelmise verbi kohta k2iva informatsiooni ...
			String eelnevaVerbiMod = null;
			AjavtSona eelnevVerb   = null;
			if (eelnevaVerbiInfo != null && eelnevaVerbiInfo.size() == 2){
				eelnevaVerbiMod = (String)   (eelnevaVerbiInfo.get(0)); 
				eelnevVerb      = (AjavtSona)(eelnevaVerbiInfo.get(1));
			}
			List<MorfAnRida> analyysiTulemused = sona.getAnalyysiTulemused();
			for (MorfAnRida morfAnRida : analyysiTulemused) {
				if (morfAnRida.getVormiNimetused() != null){
					if (verbOlevik.matcher(morfAnRida.getVormiNimetused()).matches()){
						// Siia alla kuulub ka verb-kesks6na olevikuvorm
						sona.setGrammatilineAeg(GRAMMATILINE_AEG.OLEVIK);
						if ((morfAnRida.getLemmaIlmaVahemarkideta()).equals("ole")){
							if (verbTingivKaudneMinevik.matcher(morfAnRida.getVormiNimetused()).matches()){
								// tingiv ja kaudne k6neviis
								eelnevaVerbiMod = morfAnRida.getLemmaIlmaVahemarkideta()+"_ot";								
							} else {
								// kindel k6neviis								
								eelnevaVerbiMod = morfAnRida.getLemmaIlmaVahemarkideta()+"_o";								
							}
						}
					}
					if (verbLihtminevik.matcher(morfAnRida.getVormiNimetused()).matches()){
						sona.setGrammatilineAeg(GRAMMATILINE_AEG.LIHTMINEVIK);
						if ((morfAnRida.getLemmaIlmaVahemarkideta()).equals("ole")){
							eelnevaVerbiMod = morfAnRida.getLemmaIlmaVahemarkideta()+"_m";
						}
					}
					if (verbYldMinevik.matcher(morfAnRida.getVormiNimetused()).matches()){
						sona.setGrammatilineAeg(GRAMMATILINE_AEG.YLDMINEVIK);
					}					
					if (verbKeskSonaMinevikuVorm.matcher(morfAnRida.getVormiNimetused()).matches()){
						boolean verbiAegMaaratud = false;
						if (eelnevVerb != null && eelnevaVerbiMod != null){
							// eelneb eitus
							if (eelnevaVerbiMod.equals("pole") || eelnevaVerbiMod.equals("ei")){
								eelnevVerb.setGrammatilineAeg(GRAMMATILINE_AEG.LIHTMINEVIK);
								sona.setGrammatilineAeg(GRAMMATILINE_AEG.LIHTMINEVIK);
							}
							// eelneb jaatus
							if (eelnevaVerbiMod.equals("ole_o")){
								eelnevVerb.setGrammatilineAeg(GRAMMATILINE_AEG.TAISMINEVIK);
								sona.setGrammatilineAeg(GRAMMATILINE_AEG.TAISMINEVIK);
								verbiAegMaaratud = true;
							} else if (eelnevaVerbiMod.equals("ole_ot")){
								eelnevVerb.setGrammatilineAeg(GRAMMATILINE_AEG.YLDMINEVIK);
								sona.setGrammatilineAeg(GRAMMATILINE_AEG.YLDMINEVIK);
								verbiAegMaaratud = true;
							} else if (eelnevaVerbiMod.equals("ole_m")){
								eelnevVerb.setGrammatilineAeg(GRAMMATILINE_AEG.ENNEMINEVIK);
								sona.setGrammatilineAeg(GRAMMATILINE_AEG.ENNEMINEVIK);
								verbiAegMaaratud = true;
							}
						}
						if (!verbiAegMaaratud){
							sona.setGrammatilineAeg(GRAMMATILINE_AEG.KS_MINEVIK);
						}
					}
				}
			}
			// Uuendame eelneva verbi infot: lisame k2esoleva verbi info
			String algSona = sona.getAlgSona();
			if (algSona.equals("pole") || algSona.equals("ei")){
				eelnevaVerbiMod = algSona;
			}
			eelnevaVerbiInfo.clear();
			eelnevaVerbiInfo.add(eelnevaVerbiMod);
			eelnevaVerbiInfo.add(sona);			
		} else {
		// 2) Tegemist on omaduss6naga ...
			List<MorfAnRida> analyysiTulemused = sona.getAnalyysiTulemused();
			for (MorfAnRida morfAnRida : analyysiTulemused) {
				if (morfAnRida.getSonaliik() != null && (morfAnRida.getSonaliik()).equals("_A_")){
					if (morfAnRida.getLemmaIlmaVahemarkideta() != null){
						String lemmaIlmaVahemarkideta = morfAnRida.getLemmaIlmaVahemarkideta();
						if (ksTyveMinevikuLopp.matcher(lemmaIlmaVahemarkideta).matches()){
							sona.setGrammatilineAeg(GRAMMATILINE_AEG.KS_MINEVIK);
							break;
						} else if (ksTyveOlevikuLopp.matcher(lemmaIlmaVahemarkideta).matches()){
							sona.setGrammatilineAeg(GRAMMATILINE_AEG.KS_OLEVIK);
							break;
						}
					}					
				}
			}	
		}
	}
	
}
