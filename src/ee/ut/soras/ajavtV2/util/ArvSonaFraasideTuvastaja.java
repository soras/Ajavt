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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.FraasisPaiknemiseKoht;
import ee.ut.soras.wrappers.mudel.MorfAnRida;

/**
 *   Arvsonafraase tuvastav komponent.
 *   <p>
 *   T&ouml;&ouml;p&otilde;him&otilde;te: tekst l&auml;bitakse s&otilde;na-s&otilde;nahaaval:
 *  iga s&otilde;na <code>sona</code> puhul kutsutakse v&auml;lja meetod 
 *  <code>tuvastaArvSonaFraas(AjavtSona sona)</code>, mis tuvastab arvs&otilde;nafraasi osa 
 *  s&otilde;nas <code>sona</code> ning lisab selle arvulise v&auml;&auml;rtuse
 *  tuvastaja-siseselt konstrueeritavale arvule. Kui on j&otilde;utud yhe arvs&otilde;nafraasi
 *  l&otilde;ppu (<code>kasTuvastamineOnPooleli == false</code>), on v&otilde;imalik meetodi
 *  <code>annaViimasenaTuvastatudArvsonad</code> abil viimase fraasi arvs&otilde;nade arvulised
 *  v&auml;&auml;rtused k&auml;tte saada (<b>NB! Aga kui tekstis j&otilde;utakse uue 
 *  arvs&otilde;nafraasini, muutuvad eelmise arvs&otilde;nafraasi arvulised v&auml;&auml;rtused
 *  k&auml;ttesaamatuks...</b>)  
 *  <p>
 *  NB! Eeldab, et sisend on kodeeringus utf8.
 * 
 * @author Siim Orasmaa
 */
public class ArvSonaFraasideTuvastaja {

	/************************************************************************************/
	/*** Soned, mille pohjal luuakse arvsonu tuvastavad regulaaravaldised ***************/
	/************************************************************************************/
	
	// Murdarvud ja tavaarvude loendist valjajaavad arvulist tahendust omavad sonad
	final private static Object murdJaEriArvulised[][] = {
		{"veerand", 		  new Double(0.25) },
		{"pool(e)?", 		  new Double(0.5) },
		{"kolmveerand", 	  new Double(0.75) },
		{"pool(e)?teis(e|t)", new Double(1.5) },
		{"veerandsa(d|j)a",   new Integer(25) },
		{"veerandtuhat", 	  new Integer(250) },
		{"poolsada",          new Integer(50) },
		{"poolesaja",         new Integer(50) },
		{"pooltuhat", 		  new Integer(500) }
	};
	
	final private static String yhelised[] = {
			"null(ine|is)?",
			"((\u00FC(ks|he(ne)?){1})|(esimene))",
			"(ka(ks|he(ne)?){1}|teine)",
			"kolm(e(ne)?|as)?",
			"nel(i|ja(ne)?|jas){1}",
			"vii(s|e(ne)?|es){1}",
			"kuu(s|e(ne)?|es){1}",
			"seits(e|me(ne)?|mes){1}",
			"kaheksa(ne|s)?",
			"\u00FCheksa(ne|s)?"};
	
	final private static String teistkymnelised[] = {
			"k\u00FCm(me|ne(ne)?|nes)",
		  	"((\u00FCksteist(k\u00FCmmend)?)|(\u00FCheteist)|(\u00FCheteistk\u00FCmne(ne|s)?))",
		  	"((kaksteist(k\u00FCmmend)?)|(kaheteist)|(kaheteistk\u00FCmne(ne|s)?))",
		  	"((kolmteist(k\u00FCmmend)?)|(kolmeteist)|(kolmeteistk\u00FCmne(ne|s)?))",
		  	"((neliteist(k\u00FCmmend)?)|(neljateist)|(neljateistk\u00FCmne(ne|s)?))",
		  	"((viisteist(k\u00FCmmend)?)|(viieteist)|(viieteistk\u00FCmne(ne|s)?))",
		  	"((kuusteist(k\u00FCmmend)?)|(kuueteist)|(kuueteistk\u00FCmne(ne|s)?))",
		  	"((seitseteist(k\u00FCmmend)?)|(seitsmeteist)|(seitsmeteistk\u00FCmne(ne|s)?))",
		  	"((kaheksateist(k\u00FCmmend)?)|(kaheksateist)|(kaheksateistk\u00FCmne(ne|s)?))",
		  	"((\u00FCheksateist(k\u00FCmmend)?)|(\u00FCheksateist)|(\u00FCheksateistk\u00FCmne(ne|s)?))",
			//
			// NB! Variandid "yheteist", "kaheteist" jne on vajalikud liits6na seest (nt "yheteistaastane") 
		  	// arvude tuvastamiseks, lemmadega need ei tohiks klappida ...
			//
		};
	
	final private static String kymnelised[] = {
		 	"((kaksk\u00FCmmend)|(kahek\u00FCmne(ne|s)?))",
		 	"((kolmk\u00FCmmend)|(kolmek\u00FCmne(ne|s)?))",
		 	"((nelik\u00FCmmend)|(neljak\u00FCmne(ne|s)?))",
		 	"((viisk\u00FCmmend)|(viiek\u00FCmne(ne|s)?))",
		 	"((kuusk\u00FCmmend)|(kuuek\u00FCmne(ne|s)?))",
		 	"((seitsek\u00FCmmend)|(seitsmek\u00FCmne(ne|s)?))",
		 	"((kaheksak\u00FCmmend)|(kaheksak\u00FCmne(ne|s)?))",
		 	"((\u00FCheksak\u00FCmmend)|(\u00FCheksak\u00FCmne(ne|s)?))"
	};

	final private static String sajalised[] = {
	 		"sa(da|ja(ne)?|jas){1}",
	 		"((\u00FCkssada)|(\u00FChesaja(ne|s)?))",
	 		"((kakssada)|(kahesaja(ne|s)?))",
	 		"((kolmsada)|(kolmesaja(ne|s)?))",
	 		"((nelisada)|(neljasaja(ne|s)?))",
	 		"((viissada)|(viiesaja(ne|s)?))",
	 		"((kuussada)|(kuuesaja(ne|s)?))",
	 		"((seitsesada)|(seitsmesaja(ne|s)?))",
	 		"((kaheksasada)|(kaheksasaja(ne|s)?))",
	 		"((\u00FCheksasada)|(\u00FCheksasaja(ne|s)?))"
	};	
	
	final private static String tuhat = "tuha(t|nde(ne)?|ndes){1}"; 
	
	/**
	 *   Avaldis kontrollimaks, kas s&otilde;na on sobiv kanditaat arvuks (st omab sobivat prefiksit).
	 */
	final private static String prefiksiteKontrollAvaldis = 
		"((null|veeran|pool|\u00FCks|\u00FChe|esim|kaks|kahe|tei|kolm|nel|vii|kuu|seits|k\u00FCm|sa|tuha){1}).*"; 

	
	/************************************************************************************/
	/*** Kompileeritud regulaaravaldised arvsonade tuvastamiseks ************************/
	/************************************************************************************/
	
	private Map<Pattern, Object> murdJaEriArvulisteKompilRegExp;
	
	private Pattern yhelisedKompilRegExp[];
	private Pattern teistkymnelisedKompilRegExp[];
	private Pattern kymnelisedKompilRegExp[];
	private Pattern sajalisedKompilRegExp[];
	private Pattern tuhatKompilRegExp;
	
	private Pattern prefiksiKontrollKompilRegExp;
	
	/************************************************************************************/
	/*** Arvsonafraaside tuvastaja olekud ***********************************************/
	/************************************************************************************/

	/**
	 *   Kas viimane meetodi <code>tuvastaArvSonaFraas</code> kaudu etteantud s&otilde;na
	 *  osutus arvs&otilde;naks v&otilde;i mitte?
	 */
	private boolean viimaneEtteantudSoneOliArvsona = false;
	
	private    int loodavTaisArv = -1;
	
	private double loodavMurdArv = -1.0;
	
	/**
	 *   Mitme sona pikkune on jooksvalt loodav arvsonafraas? 
	 */
	private int loodavaArvSonaFraasiPikkus = 0;
	
	private int eelmiseArvuKlass = 0;
	
	/**
	 *   Tekst, mida labime (meetodi <code>tuvastaArvSonaFraas(AjavtSona sona)</code> valjakutsetega)
	 *   ning millele hakkame leitud arvsonu kylge riputama.
	 */
	private List<AjavtSona> labitavTekst = null;
	
	/**
	 *  Loob uue arvsonafraaside tuvastaja, kompileerib tuvastaja regulaaravaldised.
	 *  
	 * @param labitavTekst tekst, milles margendatakse leitud arvsonafraasid
	 */
	public ArvSonaFraasideTuvastaja(List<AjavtSona> labitavTekst) {
		this.labitavTekst = labitavTekst;
		kompileeriRegulaarAvaldised();
	}
	
	
	/**
	 *   Kompileerime s&otilde;nemassiivide <code>yhelised, teistkymnelised, kymnelised,
	 *   sajalised</code> ja s&otilde;ne <code>tuhat</code> p&otilde;hjal 
	 *   regulaaravaldised. 
	 *   <p>
	 *   <b>NB! See meetod tuleb alati kutsuda v&auml;lja enne, kui hakatakse 
	 *   	arvs&otilde;nafraase tuvastama.</b>
	 */
	private void kompileeriRegulaarAvaldised(){
		// ***************** reg_avaldised murdarvuliste s6nade tuvastamiseks ...
		this.murdJaEriArvulisteKompilRegExp = new HashMap<Pattern, Object> ( (murdJaEriArvulised).length );
		for (Object murdArvuliseAvaldis[] : murdJaEriArvulised) {
			this.murdJaEriArvulisteKompilRegExp.put(
						Pattern.compile((String)murdArvuliseAvaldis[0], Pattern.UNICODE_CASE),
						murdArvuliseAvaldis[1]
			);
		}
		// ***************** reg_avaldised yheliste tuvastamiseks ...
		yhelisedKompilRegExp = new Pattern[yhelised.length];
		int i = 0;
		for (String yheliseAvaldis : yhelised) {
			yhelisedKompilRegExp[i++] = 
				Pattern.compile(yheliseAvaldis, Pattern.UNICODE_CASE);
		}
		// ***************** reg_avaldised ...-teistkymneliste tuvastamiseks ...
		teistkymnelisedKompilRegExp = new Pattern[teistkymnelised.length];
		i = 0;
		for (String teistkymneliseAvaldis : teistkymnelised) {
			teistkymnelisedKompilRegExp[i++] = 
				Pattern.compile(teistkymneliseAvaldis, Pattern.UNICODE_CASE);
		}
		// ***************** reg_avaldised kymneliste tuvastamiseks ...
		kymnelisedKompilRegExp = new Pattern[kymnelised.length];
		i = 0;
		for (String kymneliseAvaldis : kymnelised) {
			kymnelisedKompilRegExp[i++] = 
				Pattern.compile(kymneliseAvaldis, Pattern.UNICODE_CASE);
		}
		// ***************** reg_avaldised sajaliste tuvastamiseks ...
		sajalisedKompilRegExp = new Pattern[sajalised.length];
		i = 0;
		for (String sajaliseAvaldis : sajalised) {
			sajalisedKompilRegExp[i++] = 
				Pattern.compile(sajaliseAvaldis, Pattern.UNICODE_CASE);
		}
		// ***************** reg_avaldised "tuhande" tuvastamiseks ...
		tuhatKompilRegExp = Pattern.compile(tuhat, Pattern.UNICODE_CASE);
		// ***************** genereerime kontrollavaldised
		prefiksiKontrollKompilRegExp = Pattern.compile(
							prefiksiteKontrollAvaldis, Pattern.UNICODE_CASE);
	}
	
	
	
	/**
	 *  Leiab ja tagastab (arv-)s&otilde;na <code>s</code> v&auml;&auml;rtuse objekt-kujul:
	 * t&auml;isarvude puhul on tagastatavaks objektiks <tt>Integer</tt>, murdarvude puhul
	 * <tt>Double</tt>. Tagastab <tt>null</tt>, kui <code>s</code> ei ole arvs&otilde;na 
	 * v&otilde;i <code>s</code> v&auml;&auml;rtust ei ole v&otilde;imalik mingil p&otilde;hjusel 
	 * leida.   
	 * 
	 * @param s oletatav arvs&otilde;na
	 */
	public Object leiaArvSonaArvulineVaartus(String s){
		// ************* vaatame, kas arvsona leidub murdarvuliste seas ******************
		for (Pattern pattern : murdJaEriArvulisteKompilRegExp.keySet()) {
			Matcher m = pattern.matcher(s);
			if (m.matches()){
				return murdJaEriArvulisteKompilRegExp.get(pattern);
			}
		}
		// ************* vaatame, kas arvsona leidub yheliste seas ***********************
		for (int i = 0; i < yhelisedKompilRegExp.length; i++) {
			Matcher m = yhelisedKompilRegExp[i].matcher(s);
			if (m.matches()){
				return new Integer(i);
			}
		}
		// ************ vaatame, kas arvsona leidub -teistkymneliste seas ****************
		for (int i = 0; i < teistkymnelisedKompilRegExp.length; i++) {
			Matcher m = teistkymnelisedKompilRegExp[i].matcher(s);
			if (m.matches()){
				return new Integer(10 + i);
			}
		}
		// ************ vaatame, kas arvsona leidub -kymneliste seas ****************
		for (int i = 0; i < kymnelisedKompilRegExp.length; i++) {
			Matcher m = kymnelisedKompilRegExp[i].matcher(s);
			if (m.matches()){
				return new Integer(10*(i + 2));
			}
		}
		// ************ vaatame, kas arvsona leidub sajaliste seas ****************
		for (int i = 0; i < sajalisedKompilRegExp.length; i++) {
			Matcher m = sajalisedKompilRegExp[i].matcher(s);
			if (m.matches()){
				return new Integer((i < 2) ? (100) : (100*i));
			}
		}
		// ********************* vaatame, kas arvsona on "tuhat" *******************		
		Matcher m = tuhatKompilRegExp.matcher(s);
		if (m.matches()){
			return new Integer(1000);
		}
		return null;
	}
	
	
	public boolean tuvastaArvSonaFraas(AjavtSona sona){
		boolean leidusArvSona = false;
		if (sona.kasLeidusAnalyys()){
			for (MorfAnRida anRida : sona.getAnalyysiTulemused()) {
				String lemma = anRida.getLemmaIlmaVahemarkideta();
				Matcher m = prefiksiKontrollKompilRegExp.matcher(lemma);
				if (m.matches()){
					Object arvObjektina = leiaArvSonaArvulineVaartus(lemma);
					// 1) Kui on tegemist taisarvuga
					if (arvObjektina != null && arvObjektina instanceof Integer){
						int arv = ((Integer)arvObjektina).intValue(); 
						leidusArvSona = true;
						int arvuKlass = leiaArvuKlass(arv);						
						//  kas alustame uue arvu konstrueerimist 
						// voi saame jatkata vanaga?
						if (viimaneEtteantudSoneOliArvsona){
							// Kui eelmine arv oli taisarv ...							
							if (loodavTaisArv > -1){
								if (arv == 1000 && loodavTaisArv < arv){ // Tuhandele eelnevad arvud
																	 // - tuhande kordseks
									loodavTaisArv *= arv;
									loodavaArvSonaFraasiPikkus++;
								} else if (esimeneArvuKlassOnSuurem(eelmiseArvuKlass, arvuKlass)){ 
															// Iga jargnev "arv" peab kuuluma vaiksemasse 
														    // klassi kui eelnevad
	 								loodavTaisArv += arv;
	 								loodavaArvSonaFraasiPikkus++;
								} else {
									//  kui ilmneb, et jargmine arvsona viitab suuremale/vordsele
									// arvule vorreldes eelmisega, jareldame, et tuleb hakata 
									// uut arvu konstrueerima
									sulgeArvSonaFraas(sona);
									loodavaArvSonaFraasiPikkus = 1;
									loodavTaisArv = arv;
								}
							} else {
							// Kui eelmine arv oli murdarv ...
								// Alustame jalle otsast peale
								sulgeArvSonaFraas(sona);
								loodavaArvSonaFraasiPikkus = 1;
								loodavTaisArv = arv;
							}
						} else {
							//  kustutame eelmise arvsonafraasi andmed, 
							// alustame uue konstrueerimisega
							loodavTaisArv = arv;
							loodavaArvSonaFraasiPikkus = 1;
						}
						eelmiseArvuKlass = arvuKlass;
						// Oluline: kui eelnevalt leidus arvs6na, murrame tsyklist
						// v2lja, muidu, kui sõnal on mitu morf analyysi, nt 'teine' ja
						// 'teine', saame l6puks kokku 2+2=4, mis pole paraku eriti 6ige
						// tulemus ...
						break;
					}
					// 2) Kui on tegemist murdarvuga
					if (arvObjektina != null && arvObjektina instanceof Double){
						if (viimaneEtteantudSoneOliArvsona){
							// Sulgeme eelneva fraasi ja alustame otsast peale
							sulgeArvSonaFraas(sona);
						}
						// Alustame otsast peale
						loodavaArvSonaFraasiPikkus = 1;
						loodavMurdArv = ((Double)arvObjektina).doubleValue();
						leidusArvSona = true;						
					}
				}
			}
		}
		if (!leidusArvSona){
			// kui "arvsonade jarjestikune voog" on j2lle katkenud,
			// voib oletada, et oleme j6udnud arvsonafraasi loppu
			if (viimaneEtteantudSoneOliArvsona){
				sulgeArvSonaFraas(sona);
			}
		}
		viimaneEtteantudSoneOliArvsona = leidusArvSona;
		return leidusArvSona;
	}
	
	/**
	 *   Arvude vordlemiseks: tagastab etteantud arvu klassi (1, 11, 50, 100, 1000).
	 */
	private int leiaArvuKlass(int arv){
		if (arv >= 0 && arv < 10){
			return 1;
		} else if (arv < 20){
			return 11;			
		} else if (arv < 100){
			return 50;
		} else if (arv < 1000){
			return 100;
		} else {
			return 1000;
		}
	}

	/**
	 *   Arvude vordlemiseks yledefineeritud, leiab, kas esimene arvuklass on rangelt
	 *   suurem kui teine. Arvestab seda, et klass 11 on 'vordne' klassidega 1 ja 50. 
	 *   Vordsete klassidega arve ei saa yhendada yhtseks arvsonafraasiks: nt "yksteist yks"
	 *   ning "kolmkymmend seitseteist" tuleb laiali l6hkuda erinevateks arvudeks
	 *   11 ja 1 ning 30 ja 17. 
	 */
	private boolean esimeneArvuKlassOnSuurem(int esimene, int teine){
		if (esimene == 11){
			// Esimene on yheteistkymneline: esimene ei saa olla mingil juhul teisest
			// suurem
			return false;
		} else if (teine == 11){
			// Teine on yheteistkymneline: esimene vaid siis suurem, kui 
			// pole yheline ega kymneline			
			return (esimene > 50);
		} else {
			return (esimene > teine);
		}
	}
	
	/**
	 *   Sunnib tuvastaja sulgema arvsonafraasi, st lopetama poolelioleva tuvastamise 
	 *  ja margendama labitava teksti lopu arvsonafraasi margenditega tuvastatud fraasipikkuse
	 *  ulatuses.
	 *  <p>
	 *  Tavaliselt kutsutakse see meetod valja automaatselt, kui tekstis jargneb
	 *  arvsonafraasile esimene mitte-arvsona ning valjaspool klassi ArvSonaFraasideTuvastaja
	 *  tuleb seda meetodit kutsuda valja ainult yks kord: p2rast seda, kui viimane 
	 *  tekstis olev sona meetodist <code>tuvastaArvSonaFraas</code> labi lastud.
	 *  <p>
	 *   Fraasi sulgemise kaigus margendatakse labitava teksti viimased sonad arvsonafraasi
	 *   kuuluvateks sonadeks ning igale sonale riputatakse kylge (kogu) arvsonafraasi arvuline
	 *   vaartus. Kui <code>valjajaetavSona</code> on vordne labitava teksti viimase
	 *   sonaga, siis jaetakse see fraasist valja. 
	 */
	public void sulgeArvSonaFraas(AjavtSona valjajaetavSona){
		if ((loodavTaisArv > -1 || loodavMurdArv > -1.0) && 
				loodavaArvSonaFraasiPikkus > 0 && 
					this.labitavTekst != null){
			int startFrom = (this.labitavTekst.size() - 1);
			if (valjajaetavSona != null && (this.labitavTekst.get(this.labitavTekst.size()-1)).equals(valjajaetavSona)){
				startFrom--;
			}
			if (loodavaArvSonaFraasiPikkus == 1){
				(this.labitavTekst.get(startFrom)).setArvSonaFraasis(FraasisPaiknemiseKoht.AINUSSONA);
				if (loodavTaisArv > -1){
					(this.labitavTekst.get(startFrom)).setArvSonaTaisArvVaartus(new Integer(loodavTaisArv));
				} else {
					(this.labitavTekst.get(startFrom)).setArvSonaMurdArvVaartus(new Double(loodavMurdArv));
				}
			} else {
				int i = startFrom;
				while (i >= startFrom - loodavaArvSonaFraasiPikkus + 1){
					if (i == startFrom){
						(this.labitavTekst.get(i)).setArvSonaFraasis(FraasisPaiknemiseKoht.LOPUS);
						if (loodavTaisArv > -1){
							(this.labitavTekst.get(i)).setArvSonaTaisArvVaartus(new Integer(loodavTaisArv));
						} else {
							(this.labitavTekst.get(i)).setArvSonaMurdArvVaartus(new Double(loodavMurdArv));
						}						
					} else if (i == startFrom - loodavaArvSonaFraasiPikkus + 1){
						(this.labitavTekst.get(i)).setArvSonaFraasis(FraasisPaiknemiseKoht.ALGUSES);
						if (loodavTaisArv > -1){
							(this.labitavTekst.get(i)).setArvSonaTaisArvVaartus(new Integer(loodavTaisArv));
						} else {
							(this.labitavTekst.get(i)).setArvSonaMurdArvVaartus(new Double(loodavMurdArv));
						}						
					} else {
						(this.labitavTekst.get(i)).setArvSonaFraasis(FraasisPaiknemiseKoht.KESKEL);
						if (loodavTaisArv > -1){
							(this.labitavTekst.get(i)).setArvSonaTaisArvVaartus(new Integer(loodavTaisArv));
						} else {
							(this.labitavTekst.get(i)).setArvSonaMurdArvVaartus(new Double(loodavMurdArv));
						}						
					}
					i--;
				}
			}
			loodavTaisArv = -1;
			loodavMurdArv = -1.0;
			loodavaArvSonaFraasiPikkus = 0;
		}
	}

}
