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

package ee.ut.soras.ajavtV2.mudel.ajavaljend;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ee.ut.soras.ajavtV2.AjaTuvastaja;
import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.FraasisPaiknemiseKoht;
import ee.ut.soras.ajavtV2.mudel.MustriTahis;
import ee.ut.soras.ajavtV2.mudel.TuvastamisReegel;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon.CONTEXT;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaKestvus;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaObjekt;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaObjekt.TYYP;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaPunkt;
import ee.ut.soras.ajavtV2.util.SemDefValjadeParsija;
import ee.ut.soras.ajavtV2.util.TextUtils;

/**
 *   Tekstist eraldatud ajav&auml;ljendikandidaat. Nagu nimigi ytleb, on tegemist potentsiaalse ajav&auml;ljendiga, 
 *  mis ei pruugi "p&auml;eva l&otilde;puks" yldse t&auml;isv&auml;&auml;rtusliku ajav&auml;ljendina arvesse minna.
 *  Praeguse seisuga loetakse ajav&auml;ljendi eraldamist <i>6nnestunuks</i> (kandidaati realiseerunuks), kui 
 *  selle kylge ilmub t&ouml;&ouml;tluse l&otilde;puks <tt>semantikaLahendus</tt>.
 *  <p>
 *  TODO: Astmete asemel v6ib luua ka eraldi objekti - SEOS - nt fraas, vahemik ja rinnastus tyypi seos.
 *  Erinevad eraldatud kandidaadid saaks siis siduda omavahel nende seoste kaudu.  
 *  
 *  @author Siim Orasmaa
 */
public class AjavaljendiKandidaat implements Comparable<AjavaljendiKandidaat> {
	
	/**
	 *   Millise mustri poolt on antud ajaväljendikandidaat eraldatud?
	 *   Võib olla ka mitme mustri kombinatsioon, sel juhul on mustritähiste
	 *   vahel eraldajaks '-';
	 */
	private String mustriID;
	
	/**
	 *    Milliste mustrit&auml;histe alla see ajav&auml;ljendikandidaat kuulub? 
	 *   Mustritahiste p&otilde;hjal otsustatakse, kuidas see kandidaat liitub teiste
	 *   kandidaatidega (nt fraasi v6i vahemiku moodustamisel). 
	 */
	private List<String> mustriTahised;
	
	/**
	 *    Millise eraldamisastmega on tegu?
	 *    K6ige esimene aste on mustri poolt eraldatud ajavaljendikandidaadid,
	 *    seej2rel tulevad fraasiks yhendatud kandidaadid ning lopuks
	 *    vahemikuks yhendatud ajavaljendikandidaadid.
	 */
	public static enum ASTE {
		/**
		 *   Antud kandidaat on eraldatud tuvastamisreegli fraasimustri alusel. 
		 */
		MUSTRI_POOLT_ERALDATUD,
		/**
		 *   Antud kandidaat on moodustatud k6rvutiseisvate eraldatud kandidaatide liitmisel ajapunktiks. 
		 */
		YHENDATUD_FRAASINA,
		/**
		 *   Antud kandidaat on moodustatud k6rvutiseisvate eraldatud kandidaatide liitmisel ajavahemikuks. 
		 */
		YHENDATUD_VAHEMIKUNA,
	};
	
	/**
	 *   Antud ajavaljendikandidaadi aste. Vaikimisi on alati tegu mustri poolt 
	 *  eraldatud kandidaadiga, st k6ige esimese astmega. 
	 */
	private ASTE aste = ASTE.MUSTRI_POOLT_ERALDATUD;
	
	/**
	 *   Fraas tekstis, mis kuulub selle ajavaljendikandidaadi alla. 
	 */
	private List<AjavtSona> fraas;
	
	/**
	 *   Selle ajavaljendikandidaadi alla kuuluvad teised kandidaadid. Kasutusel YHENDATUD_FRAASINA, 
	 *   YHENDATUD_VAHEMIKUNA ja RINNASTUSSEOS astmega kandidaatide puhul.
	 */
	private List<AjavaljendiKandidaat> alamKandidaadid = null;
	
	/**
	 *   Antud ajavaljendikandidaadiga seotud ylemkandidaat. M6eldakse just l2himat ylemkandidaati,
	 *   st vahetut ylemat. Yldiselt v6ib erinevate astmete p6hjal luua joonistada v2lja nn fraasipuu,
	 *   <br>
	 *   Arbitaarne n&auml;ide:
	 *   <pre>
	 *                                       YHENDATUD_VAHEMIKUNA
	 *                                                |
	 *                          /-------------------------------------------\
	 *                          |                                           |
	 *                   YHENDATUD_FRAASINA                                 |
	 *                          |                                           |
	 *              /------------------------\                              |
	 *              |                        |                              |
	 *    MUSTRI_POOLT_ERALDATUD  MUSTRI_POOLT_ERALDATUD            MUSTRI_POOLT_ERALDATUD
	 *       30.  jaanuaril            kella    9-st        kuni           10-ni
	 *   </pre>
	 */
	private AjavaljendiKandidaat ylemKandidaat = null;
	
	/**
	 *   Antud ajavaljendikandidaadi semantika leidmiseks vajalikud jupid. 
	 */
	private List<SemantikaDefinitsioon> semantikaEhitusklotsid;
	
	/**
	 *   Kas antud kandidaat v&otilde;ib olla eraldiseisev ajavaljend v6i mitte? Vaikimisi seadistame,
	 *   et k6iki kandidaate tuleb k2sitleda kui eraldiseisvaid ajavaljendeid.
	 */
	private boolean poleEraldiseisevAjavaljend = false;
	
	/**
	 *   Tuvastamisreeg(el/lid), mida kasutatakse antud ajavaljendikandidaadi semantika leidmisel. 
	 */
	private List<TuvastamisReegel> tuvastamisReeglid = null;
	
	//==============================================================================
	//   	A n k u r d a m i n e
	//==============================================================================
	
	/**
	 *   Antud ajav&auml;ljendi ankruks oleva ajav&auml;ljend. Kui seda pole m&auml;&auml;ratud, peaks
	 *   kogu arvutusk&auml;ik toimuma referentsaja baasil. Kui see on m&auml;&auml;ratud, tuleb leida
	 *   k&otilde;igepealt ankurdatudKandidaadi semantika ning v6tta referentsajana aluseks selle
	 *   kandidaadi semantika leidmisel.
	 */
	private AjavaljendiKandidaat ankurdatudKandidaat;
	
	/**
	 *    Antud ajav2ljendile l2him verb. Verbi grammatilise aja alusel v6ib heuristiliselt otsustada, 
	 *   kas ajav2ljendi poolt m22ratud aeg paigutub k6nehetke suhtes minevikku v6i olevikku/tulevikku. 
	 */
	private AjavtSona lahimVerb;	
	
	//==============================================================================
	//   	L o p l i k   s e m a n t i k a   l a h e n d a m i n e
	//==============================================================================
	
	/**
	 *   Ajavaljendi semantika lahendust sisaldav objekt. 
	 */
	private AjaObjekt semantikaLahendus = null;
	
	/**
	 *   Kas semantika lahendamine on l2biviidud? NB! L2biviidus ei t2henda veel seda,
	 *   et semantika 6nnestus ka leida, st <tt>semantikaLahendus != null</tt>.
	 */
	private boolean semLahendamineLabiviidud = false;
	
	//==============================================================================
	//   	M a r g e n d u s
	//==============================================================================

	/**
	 *   Positsioon listis <code>fraas</code>, millelt tuleks k2esolev kandidaat pooleks jagada.
	 *   Kui on -1, siis mingit poolitamist pole vaja.
	 */
	private int poolituskoht = -1;
	
	/**
	 *   Kas poolitades peaks s6na, mille kohalt poolitatakse, j22ma esimese alamkandidaadi sisse
	 *   (<code>poolitusOnInklusiivne == true</code>) v6i tuleks see yldse m2rgendusest v2lja
	 *   j2tta (<code>poolitusOnInklusiivne == false</code>). Vaikimisi <code>false</code>.
	 */
	private boolean poolitusOnInklusiivne = true;
	
	//==============================================================================
	//   	K o n s t r u k t o r
	//==============================================================================
	
	public AjavaljendiKandidaat(){
		(this.fraas) = new ArrayList<AjavtSona>();
		(this.semantikaEhitusklotsid) = new ArrayList<SemantikaDefinitsioon>();
	}
	
	//==============================================================================
	//   	   F r a a s i p i i r i d   j a   y l e k a t t u v u s ,
	//
	//      K o r v u t i a s e t s e v a t e    k a n d i d a a t i d e
	//                           y h e n d a m i n e   
	//==============================================================================
	
	/**
	 *    Leiab, kas antud ajavaljendifraas on (t&auml;ielikult) ylekaetud yhe v6i rohkema
	 *   teise ajavaljendifraasi poolt.
	 *   <p>
	 *   TODO: See meetod ei tuvasta korrektselt, loeb ka osalised ylekatted t2ielikuks...
	 */
	public boolean onYlekaetudTeisteAjavaljendifraasidePoolt(){
		// 1) Lihtsaim juht: ajavaljendiks on vaid yks s6na - kontrollime, kas leidub
		// ajavaljendeid, mis ulatuvad sellest s6nast kaugemale
		if (fraas.size() == 1){
			List<FraasisPaiknemiseKoht> ajavaljendiFraasides =
				((this.fraas).get(0)).getAjavaljendiKandidaatides();
			for (int i = 0; i < ajavaljendiFraasides.size(); i++) {
				if (ajavaljendiFraasides.get(i) != FraasisPaiknemiseKoht.AINUSSONA){
					return true;
				}
			}
		} else if (fraas.size() > 1) {
		// 2) Kui fraas on pikem kui yks s6na, tuleb kontrollida fraasi algust ja
		// l6ppu ning leida, kas yhes neis on mingi teise ajavaljendi keskosa
			
			// 2.1) Leiame koik ajavaljendid ja nende paiknemised fraasi alguses & lopus
			List<FraasisPaiknemiseKoht> avPaiknemisedAlguses =
				(this.fraas.get(0)).getAjavaljendiKandidaatides();
			List<AjavaljendiKandidaat> ajavaljendidAlguses = (this.fraas.get(0)).getAjavaljendiKandidaadid();
			// Koik ajavaljendid ja nende paiknemised fraasi lopus
			List<FraasisPaiknemiseKoht> avPaiknemisedLopus = 
				((this.fraas).get((this.fraas).size()-1)).getAjavaljendiKandidaatides();
			List<AjavaljendiKandidaat> ajavaljendidLopus = 
				((this.fraas).get((this.fraas).size()-1)).getAjavaljendiKandidaadid();
			
			// 2.2) kontrollime, kas m6ni neist katab selle ajavaljendi t2ielikult yle
			for (int i = 0; i < avPaiknemisedAlguses.size(); i++) {
				if (ajavaljendidAlguses.get(i) != this &&
					(avPaiknemisedAlguses.get(i) == FraasisPaiknemiseKoht.KESKEL || 
					 avPaiknemisedAlguses.get(i) == FraasisPaiknemiseKoht.ALGUSES)){
					// Kuna on n6utud t2ielik ylekate, peame kontrollima, kas antud
					// ajavaljend ulatub ka fraasi l6ppu (ja/v6i sealt edasi)... 
					AjavaljendiKandidaat ylekattevAjavaljend = ajavaljendidAlguses.get(i);
					for (int j = 0; j < ajavaljendidLopus.size(); j++) {
						AjavaljendiKandidaat avLopus = ajavaljendidLopus.get(j);
						if (avLopus.equals(ylekattevAjavaljend)){
							if (avPaiknemisedAlguses.get(i) == FraasisPaiknemiseKoht.KESKEL){
								// Kui selle ajavaljendi alguses oli mingi teise ajav keskosa,
								// siis piisab t2iesti, et leidsime, et selle ajav l6pus lihtsalt
								// eksisteerib see teine ajavaljend - ylekate on sellega t6estatud
								return true;
							} else {
								// Kui selle ajav alguses oli mingi teise ajav algusosa, on yle-
								// kattuvuse t6estamiseks tarvilik leida nyyd teise ajav keskosa
								if (avPaiknemisedLopus.get(j) == FraasisPaiknemiseKoht.KESKEL){
									return true;
								}
							}
						}
					}
				}
			}

		}
		return false;
	}
	
	/**
	 *   Kontrollib mustrit2histe j2rgi, kas selle ajav2ljendikandidaadi otsa saab liita
	 *  etteantud ajav2ljendikandidaadi (eeldatavalt tekstis vahetult j2rgneva). Kontrollimisel
	 *  v6etakse aluseks tabelis <tt>lubatudPaarid</tt> defineeritud mustripaarid.
	 *  <p>
	 *  Tabelis <tt>kasutatudTahised</tt> hoitakse juba 2rakasutatud mustrit2hiseid: iga positiivse tulemuse
	 *  korral salvestatakse siia mustripaari vasaku liikme t2his. Yhe fraasi piires ei lubata kaks korda 
	 *  kasutada tabelis <tt>kasutatudTahised</tt> asuvaid mustrit2hiseid.
	 *  <p>
	 *  Lipp <tt>fikseeritudJarjekord</tt> m22rab, kas <tt>lubatudPaarid</tt> v6tmete loomisel on
	 *  fraaside j2rjekord olnud kindlalt fikseeritud v6i mitte.
	 *  <p> 
	 *  Lipp <tt>tapseltK6rvuti</tt> m22rab, kas <tt>teineAjavaljend</tt> j2rgneb sellele ajavaljendile
	 *  vahetult v6i on nende vahel veel ajavaljendeid.
	 */
	public boolean voibYhendadaMustriTahisteJargi(AjavaljendiKandidaat teineAjavaljend, 
										   		  HashMap<String, String> lubatudPaarid,
										   		  HashMap<String, String> kasutatudTahised,
										   		  boolean fikseeritudJarjekord,
										   		  boolean tapseltK6rvuti){
		if (this.mustriTahised != null && teineAjavaljend.mustriTahised != null){
			//  Kontrollime k6iki v6imalikke mustrit2histe paare, mis selle kandidaadi
			// ja j2rgmise kandidaadi mustrit2histe vahel saab moodustada
			for (int i = 0; i < (this.mustriTahised).size(); i++) {
				String selleMustriTahis = (this.mustriTahised).get(i);
				for (int j = 0; j < (teineAjavaljend.mustriTahised).size(); j++) {
					String j2rgmiseMustriTahis = (teineAjavaljend.mustriTahised).get(j);
					// Kontrollime, et neid t2hiseid poleks juba kasutatud
					if (!kasutatudTahised.containsKey(selleMustriTahis) && 
							!kasutatudTahised.containsKey(j2rgmiseMustriTahis)){
						List<String> paar = Arrays.asList(selleMustriTahis, j2rgmiseMustriTahis);
						String       v6ti = TextUtils.looMustriTahisteVoti(paar, !fikseeritudJarjekord);
						if (lubatudPaarid.containsKey(v6ti)){
							String fraasiN6uded = lubatudPaarid.get(v6ti);
							if (fraasiN6uded.matches("TK")){
								if (tapseltK6rvuti){
									kasutatudTahised.put(selleMustriTahis, "1");
								}
								return tapseltK6rvuti;
							} else{
								kasutatudTahised.put(selleMustriTahis, "1");
								return true;
							}
						}						
					}
				}
			}
		}
		return false; 
	}
	
	//==============================================================================
	//         A j a v a l j e n d i     l a i e n d a m i n e      t e i s e
	//                            a j a v a l j e n d i g a
	//==============================================================================
	
	/**
	 *   Laiendab kaesolevat kandidaati etteantud ajavaljendikandidaadiga, pikendades seda
	 *  vastavalt kas l6pust v6i algusest.
	 *  <p>
	 *  NB! K6ik <tt>av</tt>-d katvad senised ajavaljendikandidaadid (ka sellised, mis katavad
	 *  ainult osaliselt) eemaldatakse. Seega l6puks j22b endise <tt>av</tt> s6nu katma ainult
	 *  see ajavaljendikandidaat.
	 *  
	 *  @param av k6rvalpaiknev ajavaljendikandidaat, mis liidetakse selle kandidaadi kylge
	 *  @param laiendaLopust kas laiendada tuleb selle ajavaljendikandidaadi l6pust v6i algusest
	 */
	public void laiendaAjavaljendiKandidaadiga(AjavaljendiKandidaat av, boolean laiendaLopust){
		if ((this.fraas).size() > 0 && (av.fraas).size() > 0){
			// =================================================================
			//     M a r g e n d u s e   u u e n d a m i n e
			// =================================================================
			// 1) K6igepealt tuleb muuta kaesoleva fraasi viimast/esimest 
			//    s6na (et selle paiknemiskoht ajav-s oleks korrektne)
			int viimaseIndeksSelles     = -1;
			FraasisPaiknemiseKoht uusKohtSelles     = FraasisPaiknemiseKoht.PUUDUB;
				// A) Laiendamine valjendi lopust
			if (laiendaLopust){
				viimaseIndeksSelles     = (this.fraas).size()-1;
				uusKohtSelles           = FraasisPaiknemiseKoht.ALGUSES;
				// B) Laiendamine valjendi algusest			
			} else {
				viimaseIndeksSelles     = 0;
				uusKohtSelles           = FraasisPaiknemiseKoht.LOPUS;
			}
			// Sisuliselt: eemaldame vana margenduse ja lisame uue
			AjavtSona sonaSellesAvs     = (this.fraas).get(viimaseIndeksSelles);
			if ((this.fraas).size() > 1){
				uusKohtSelles = FraasisPaiknemiseKoht.KESKEL;
			}			
			sonaSellesAvs.eemaldaAjavaljendiKandidaat(this);
			sonaSellesAvs.margendaAjavaljendiKandidaadiga(this, uusKohtSelles);
			
			// 2) Nyyd eemaldame teise fraasi kyljest vana ajavaljendi, lisame selle fraasi kylge 			
			if (laiendaLopust){
				for (int i = 0; i < (av.fraas).size(); i++) {
					FraasisPaiknemiseKoht uusKoht = FraasisPaiknemiseKoht.LOPUS;
					// Eemaldame vana ajav kyljest k6ik kandidaadid, valimatult
					AjavtSona ajavtsona = (av.fraas).get(i);
					if (ajavtsona.onSeotudMoneAjavaljendiKandidaadiga()){
						List<AjavaljendiKandidaat> ajavaljendiKandidaadid 
													= ajavtsona.getAjavaljendiKandidaadid();
						for (int j = 0; j < (ajavaljendiKandidaadid).size(); j++) {
							ajavaljendiKandidaadid.get(j).eemaldaEnnastSonadeKyljest();
						}						
					}
					// Lisame uue ajav kylge					
					(this.fraas).add(ajavtsona);
					if (i < (av.fraas).size()-1){
						uusKoht = FraasisPaiknemiseKoht.KESKEL;
					}
					ajavtsona.margendaAjavaljendiKandidaadiga(this, uusKoht);
				}				
			} else {
				for (int i = (av.fraas).size()-1; i >= 0; i--) {
					FraasisPaiknemiseKoht uusKoht = FraasisPaiknemiseKoht.ALGUSES;					
					// Eemaldame vana ajav kyljest k6ik kandidaadid, valimatult					
					AjavtSona ajavtsona = (av.fraas).get(i);
					if (ajavtsona.onSeotudMoneAjavaljendiKandidaadiga()){
						List<AjavaljendiKandidaat> ajavaljendiKandidaadid 
													= ajavtsona.getAjavaljendiKandidaadid();
						for (int j = 0; j < (ajavaljendiKandidaadid).size(); j++) {
							ajavaljendiKandidaadid.get(j).eemaldaEnnastSonadeKyljest();
						}						
					}					
					// Lisame uue ajav kylge					
					(this.fraas).add(0, ajavtsona);
					if (i < (av.fraas).size()-1){
						uusKoht = FraasisPaiknemiseKoht.KESKEL;
					}
					ajavtsona.margendaAjavaljendiKandidaadiga(this, uusKoht);					
				}
			}

			// =================================================================
			//     S e m a n t i k a   u u e n d a m i n e
			// =================================================================
			// 3) Lisame semantikaehitusklotsid teiselt ajavaljendikandidaadilt
			for (SemantikaDefinitsioon semDef : av.semantikaEhitusklotsid) {
				(this.semantikaEhitusklotsid).add(semDef);
			}
			
			// 4) Viited uuetele reeglitele
			if (av.tuvastamisReeglid != null){
				for (TuvastamisReegel tvr : av.tuvastamisReeglid) {
					this.lisaTuvastamisReegel(tvr);
				}
			}
		}
	}
	
	//==============================================================================
	//     A j a v a l j e n d i     s e m a n t i k a    l a h e n d a m i n e
	//==============================================================================
	
	/**
	 *   Viib l&auml;bi ajavaljendikandidaadi semantika rekursiivse lahendamise.
	 *   <p>
	 *   Kui semantika lahendamine 6nnestub, lisatakse selle ajav&auml;ljendikandidaadi
	 *   kylge objekt <tt>semantikaLahendus</tt>. Kui antud ajavaljendikandidaat esineb 
	 *   ylem-alamkandidaatide puus lehttipuna, tagastab ta p2rast semantika lahendamist
	 *   saadud <tt>semantikaLahendus</tt> objekti, et tema ylemkandidaadid saaksid
	 *   seda edasi t88delda. 
	 *   <p>
	 *   Igal juhul (vaatamata sellele, kas lahendamine 6nnestus v6i mitte) muudetakse lipp 
	 *   <tt>semLahendamineLabiviidud</tt> p2rast meetodi v2ljakutset t&otilde;eseks.
	 */
	public AjaObjekt lahendaSemantika(AjaObjekt baasAjaobjekt, String [] aegFookuses, String [] mudel){
		if ( this.alamKandidaadid != null && !(this.alamKandidaadid).isEmpty() ){
			// 1) Kui antud kandidaadil leidub alamkandidaate
			// ----------------------------------------------------------------------------------
			//   FRAAS:  alamosade t2henduste summeerimine
			// ----------------------------------------------------------------------------------
			if (this.aste == ASTE.YHENDATUD_FRAASINA){
				// A) Sorteerime alamkandidaadid
				Collections.sort(this.alamKandidaadid);
				// B) Kogume kokku alamkandidaate lahendused
				AjaObjekt muudetavAeg = (baasAjaobjekt != null) ? (baasAjaobjekt.clone()) : (null);
				for (AjavaljendiKandidaat alamKandidaat : this.alamKandidaadid) {
					AjaObjekt ao = alamKandidaat.lahendaSemantika( muudetavAeg, aegFookuses, mudel );
					if (ao != null){
						muudetavAeg = ao;
					}
				}
				
				// Kontrollime, kas saab yhendada. Saab vaid siis, kui k6ik alamkandidaadid
				// said yhesuguse ID-ga ajaobjekti (st - uut ajaobjekti luua ei tulnud, k6ik
				// said hakkama olemasoleva objekti muutmisega)
				// !!! Samuti on oluliseks tingimuseks, et ykski alamkandidaat pole loonud 
				// eksplitsiitseid ajaobjekte (vahemiku otspunkte)
				boolean        saabYhendada = true;
				AjaObjekt yhineAlamLahendus = null;
				for (int i = 0; i < (this.alamKandidaadid).size(); i++) {
					AjaObjekt alamLahendus = ((this.alamKandidaadid).get(i)).semantikaLahendus;
					if (alamLahendus != null){
						if (yhineAlamLahendus == null){
							yhineAlamLahendus = alamLahendus;
						} else {
							if (yhineAlamLahendus.getID() != alamLahendus.getID() || 
									alamLahendus.hasRelatedExplicitTimexesAsInterval()){
								// On loodud mingi uus objekt: j2relikult yhendada ei saa
								saabYhendada = false;
								break;
							}
						}
					}
				}
				if (saabYhendada && yhineAlamLahendus != null){
					// Selle objekti lahenduseks saab k6igi yhine lahendus
					this.semantikaLahendus = yhineAlamLahendus;
					// Kustutame alamKandidaatide lahendused: need on ebat2ielikud
					for (int i = 0; i < (this.alamKandidaadid).size(); i++) {
						if (((this.alamKandidaadid).get(i)).semantikaLahendus != null){
							((this.alamKandidaadid).get(i)).semantikaLahendus = null;
						}
					}
					// TODO: Kui yhendamine eba6nnestub, aga leidub yhine semantikalahendus,
					// j22b erinevatele alamkandidaatidele sama TID-iga lahendus ...
				}
			}
			// ----------------------------------------------------------------------------------
			//   VAHEMIK:  kahe otspunkti loomine
			// ----------------------------------------------------------------------------------
			if (this.aste == ASTE.YHENDATUD_VAHEMIKUNA){
				// TimeML järgi meil enam terviklikke vahemikke pole, seega palju tegevust
				// siin enam pole ...				
				for (AjavaljendiKandidaat alamKandidaat : this.alamKandidaadid) {
					alamKandidaat.lahendaSemantika( baasAjaobjekt, aegFookuses, mudel );
				}
				if ((this.alamKandidaadid).size() == 1){
					leiaAjavahemikuPuuduvOtspunktJaSeoVahemikuga( aegFookuses );
				}
			}
			this.semLahendamineLabiviidud = true;
		} else {
			// ----------------------------------------------------------------------------------
			//   INSTANT:  üksik ajapunkt / korduvus / kestvus
			// ----------------------------------------------------------------------------------
			// 2) Kui antud kandidaadil alamkandidaate ei leidu: oleme lehttipus ning peame
			//    esmalt ise lahenduse kokku panema
			if (this.semantikaEhitusklotsid != null && !(this.semantikaEhitusklotsid).isEmpty()){
				boolean ankurdamineLabiviidud = false;
				AjaObjekt           objekt = baasAjaobjekt;
				int               setPoint = 0;
				// Eksplitsiitne intervall: m6lemad otspunktid on tekstis määratletavad, otspunktide
				// sidumine vastavate alamfraaside külge toimub hiljem (vt teostaPoolitamine);
				AjaObjekt explicitInterval = null;				
				// Varjatud/Implitsiitsed ajaobjektid: lisame ilma sisuta märgendite kujul, tekstis
				// ei positsioneeru ...
				AjaObjekt beginPoint       = null;
				AjaObjekt endPoint         = null;
				// Kui on toimunud ankurdamine, siis siin on viide ankruks oleva kandidaadi TID-le 
				String anchorTimeID        = null;
				for (int i = 0; i < this.semantikaEhitusklotsid.size(); i++) {
					SemantikaDefinitsioon semDef = (this.semantikaEhitusklotsid).get(i);
					// 2.1) Kontrollime, kas arvutusreegel kuulub antud semantika leidmise mudeli alla
					if (semDef.vastabSemLeidmiseMudelile(mudel)){
						// 2.2) Kontrollime, kas kontekstitingimused on t2idetud
						if (!semDef.onKontekstistSoltuv() || 
								(semDef.onKontekstistSoltuv() && 
										kasKandidaadiKontekstOnRakendamiseksSobiv(semDef, ankurdamineLabiviidud, aegFookuses)) ){
							//
							//  2.2.1) Ankurdamisoperatsioon > kui ankruks olev ajav2ljend on lahendatud, toome selle
							//         baasobjektiks + muudame ka fookuses olevat aega
							//         NB! Toimib ainult enne ajaintervallide avamist;
							//         Eksplitsiitsete objektide korral praegu ei toimi;
							//
							if (semDef.onAnkurdamisOperatsioon()){
								if (this.ankurdatudKandidaat != null){
									if ((this.ankurdatudKandidaat).getSemantikaLahendus() != null){
										objekt = (this.ankurdatudKandidaat).getSemantikaLahendus().clone();
										anchorTimeID = ((this.ankurdatudKandidaat).getSemantikaLahendus()).getTimex3Attribute("tid");
										if (objekt instanceof AjaPunkt){
											aegFookuses = ((AjaPunkt)objekt).getUnderlyingDateTimeAsGranularitiesArray();
										}										
										//
										//    Heuristik: kui ankruks on ajaintervall, ankurdame ainult intervalli
										//  l6pp-punkti kylge. 
										//    NB! Ei toimi n2iteks juhul: 
										//  "[esmaspäevast reedeni] kestval üritusel on kohvik lahti [südaööni]" 
										//
										if (objekt.hasRelatedExplicitTimexesAsInterval()){
											AjaObjekt relatedExplicitPoint = (objekt.getRelatedExplicitTIMEXES()).get(1);
											objekt = (relatedExplicitPoint).clone();
											anchorTimeID = relatedExplicitPoint.getTimex3Attribute("tid");
											if (objekt instanceof AjaPunkt){
												aegFookuses = ((AjaPunkt)objekt).getUnderlyingDateTimeAsGranularitiesArray();
											}
										}
										semDef.sulgeGranulaarsusedVastavaltAnkrule(objekt);
										ankurdamineLabiviidud = true;
									} else if ((this.ankurdatudKandidaat).ylemKandidaat != null){
										AjavaljendiKandidaat ankurdatuYlemkandidaat = 
												(this.ankurdatudKandidaat).ylemKandidaat;
										if ( ankurdatuYlemkandidaat.getSemantikaLahendus() != null ){
											objekt = ((ankurdatuYlemkandidaat).getSemantikaLahendus()).clone();
											anchorTimeID = ((ankurdatuYlemkandidaat).getSemantikaLahendus()).getTimex3Attribute("tid");
											if (objekt instanceof AjaPunkt){
												aegFookuses = ((AjaPunkt)objekt).getUnderlyingDateTimeAsGranularitiesArray();
											}
											//
											//    Heuristik: kui ankruks on ajaintervall, ankurdame ainult intervalli
											//  l6pp-punkti kylge. 
											//    NB! Ei toimi n2iteks juhul: 
											//  "[esmaspäevast reedeni] kestval üritusel on kohvik lahti [südaööni]" 
											//
											if (objekt.hasRelatedExplicitTimexesAsInterval()){
												AjaObjekt relatedExplicitPoint = (objekt.getRelatedExplicitTIMEXES()).get(1);
												objekt = (relatedExplicitPoint).clone();												
												anchorTimeID = (relatedExplicitPoint).getTimex3Attribute("tid");
												if (objekt instanceof AjaPunkt){
													aegFookuses = ((AjaPunkt)objekt).getUnderlyingDateTimeAsGranularitiesArray();
												}
											}
											semDef.sulgeGranulaarsusedVastavaltAnkrule(objekt);
											ankurdamineLabiviidud = true;
										}
									}
								}
								continue;								
							}
							//
							//  2.2.2) Tavaline arvutusoperatsioon: muudame kas ajaobjekti v6i yhte
							//  ajaintervalli otspunktidest
							//							
							if (setPoint == 0){
								objekt = semDef.rakendaArvutusReeglit(objekt, aegFookuses, this.lahimVerb);
							} else {
								if (setPoint == 1){
									if (explicitInterval != null){
										objekt = semDef.rakendaArvutusReeglit((explicitInterval.getRelatedExplicitTIMEXES()).get(0), aegFookuses, this.lahimVerb);
										explicitInterval.setRelatedExplicitTIMEX(0, objekt);
									}
									if (beginPoint != null){
										beginPoint = semDef.rakendaArvutusReeglit(beginPoint, aegFookuses, this.lahimVerb);
									}
								}
								if (setPoint == 2){
									if (explicitInterval != null){
										objekt = semDef.rakendaArvutusReeglit((explicitInterval.getRelatedExplicitTIMEXES()).get(1), aegFookuses, this.lahimVerb);
										explicitInterval.setRelatedExplicitTIMEX(1, objekt);
									}
									if (endPoint != null){
										endPoint = semDef.rakendaArvutusReeglit(endPoint, aegFookuses, this.lahimVerb);
									}
								}
							}

							if (semDef.getOp() != null){
								String operatsioon = semDef.getOp();
								//
								//  2.2.3) Intervalli algatamisele v6i l6petamisele suunav operatsioon
								//         Ka beginPoint ja endPoint loomine;
								//
								if ((operatsioon).equals(SemantikaDefinitsioon.OP.CREATE_beginPoint.toString())){
									// Varjatud/implitsiitse otspunkti loomine
									if (!semDef.isExplicitPoint()){
										beginPoint = new AjaPunkt(TYYP.POINT, aegFookuses);
										setPoint = 1;										
									} else {
									// Nähtava/eksplitsiitse otspunkti loomine										
										if (objekt != null && objekt.hasRelatedExplicitTimexesAsInterval()){
											explicitInterval = objekt;
											setPoint = 1;									
										}										
									}
								}
								if ((operatsioon).equals(SemantikaDefinitsioon.OP.CREATE_endPoint.toString())){
									// Varjatud/implitsiitse otspunkti loomine
									if (!semDef.isExplicitPoint()){
										endPoint = new AjaPunkt(TYYP.POINT, aegFookuses);
									} 
									setPoint = 2;									
								}								
								//
								//  2.2.4) Poolituspunkti seadmine kandidaadiga seotud fraasis
								//
								if (operatsioon.equals(SemantikaDefinitsioon.OP.SET_HALVING_POINT.toString())){
									parsiPoolituseParameetrid( semDef.getSemValue() );
								}
							}
						}						
					}
				}
				this.semLahendamineLabiviidud = true;
				this.semantikaLahendus = (explicitInterval != null) ? (explicitInterval) : (objekt);
				if (this.semantikaLahendus != null){
					if (ankurdamineLabiviidud){
						// Kinnitame viite ankruajaväljendile
						if (anchorTimeID != null){
							if ((this.semantikaLahendus).getRelatedExplicitTIMEXES() == null){
								(this.semantikaLahendus).setTimex3Attribute("anchorTimeID", anchorTimeID);
								(this.semantikaLahendus).setTimex3Attribute("temporalFunction", "true");
							} else {
								for (AjaObjekt ajaobjekt : (this.semantikaLahendus).getRelatedExplicitTIMEXES()) {
									ajaobjekt.setTimex3Attribute("anchorTimeID", anchorTimeID);
									ajaobjekt.setTimex3Attribute("temporalFunction", "true");
								}
							}
						}
					}
					if (beginPoint != null){
						(this.semantikaLahendus).setTimex3Attribute("beginPoint", beginPoint.getTimex3Attribute("tid"));
						// TODO: esialgu h2kk, et punktid kaduma ei l2heks ...
						if (!(beginPoint.asHashMapOfAttributeValue("")).containsKey("value")){
							beginPoint.setTimex3Attribute("value", "XXXX-XX-XX");
						}
						(this.semantikaLahendus).addRelatedImplicitTIMEX(beginPoint);
					}
					if (endPoint != null){
						(this.semantikaLahendus).setTimex3Attribute("endPoint", endPoint.getTimex3Attribute("tid"));
						// TODO: esialgu h2kk, et punktid kaduma ei l2heks ...
						if (!(endPoint.asHashMapOfAttributeValue("")).containsKey("value")){
							endPoint.setTimex3Attribute("value", "XXXX-XX-XX");
						}				
						(this.semantikaLahendus).addRelatedImplicitTIMEX(endPoint);
					}
				}
				return this.semantikaLahendus;	
			}
		}
		return null;
	}
	
	/**
	 *   Siia on koondatud (veidi) keeruline loogika, mis tiirleb ajavahemiku otspunkti leidmist ja 
	 *  realiseerimist t2isv22rtusliku ajapunktina. Kui k6ik l2heb positiivse stsenaariumi j2rgi,
	 *  kinnitatakse t88 tulemusena antud kandidaadi semantika lahenduseks ajavahemik;
	 */
	private void leiaAjavahemikuPuuduvOtspunktJaSeoVahemikuga( String [] aegFookuses ){
		// --------------------------------
		//   behold! the ~spagetti code
		// --------------------------------
		//  Kontekst: yks vahemiku otspunktidest on avamata, st, ainult numbri v6i arvs6na 
		// kujul... Esialgu lihtsustame: vaatame vaid juhte, kus alamkanditaate on 1
		if ((this.alamKandidaadid).size() == 1){
			AjaObjekt yksOtsPunkt = ((this.alamKandidaadid).get(0)).semantikaLahendus;
			if (yksOtsPunkt != null && yksOtsPunkt.getType() == TYYP.POINT){
				//
				//  Nyyd on meil olemas yhe otspunkti sem lahendus, j2rgmised sammud on:
				//  (a) leida arvs6na/numbrikombinatsioon, mis pole mustri poolt eraldatud / 
				//      fraasiks yhendatud kandidaat;
				//  (b) parsida arvust (a) t2isarvuline semantika; 
				//  (c) leida teise otspunkti semantikadefinitsioon (mis peaks paiknema mustri 
				//      poolt eraldatud otspunkti all);
				//  (d) panna punktis (b) leitud semantika punktis (c) leitud semantikadefinitsiooni 
				//      arvuliseks v22rtuseks;
				//  (e) rakendada saadud semantikadefinitsiooni teise otspunkti semantika saamiseks;
				//  (f) luua uus ajavahemik ja panna semantikalahenduseks;
				//
				for (int i = 0; i < (this.fraas).size(); i++) {
					AjavtSona sona = (this.fraas).get(i);
					// Leiame, kas fraasi kuuluv s6na v6iks olla arv - seega potentsiaalselt yks vahemiku otspunktidest
					// NB! Meetod garanteerib, et leitud arv ei kuulu yhegi eraldatud mustri v6i fraasi alla
					List<Integer> arvSemantika = sona.parsiPotentsVahemikugaSeotudArvud();
					
					if (arvSemantika != null && arvSemantika.size() == 1){
						//  Leidsime potentsiaalse punkti (a) koos selle semantikaga (b):
						// nyyd tuleb leida selle punkti semantikadefinitsioon, mis peab kuuluma
						// yhe mustri poolt eraldatud kandidaadi semdeffi-de alla
						int liikumisSuund = (i < (this.fraas).size() - 1) ? (1) : (-1);
						int j = i + liikumisSuund;
						boolean lopetaOtsing = false;
						while (-1 < j && j < (this.fraas).size()){
							AjavtSona korvalOlevSona = (this.fraas).get(j);
							if (korvalOlevSona.onSeotudMoneAjavaljendiKandidaadiga()){
								for (AjavaljendiKandidaat kandidaat : korvalOlevSona.getAjavaljendiKandidaadid()) {
									if (kandidaat.getAste() == ASTE.MUSTRI_POOLT_ERALDATUD){
										if (sona.arvudeEsitusKujuOnYhilduv(korvalOlevSona)){
												//   Leiame, kas kandidaadi kyljes on vahemiku otspunkti 
												//  m22rav semantikadefinitsioon; Uue otspunkti semantika
											    //  m22ramisel v6tame aluseks juba leitud otspunkti 
											    //  semantika;
												AjaObjekt teineOtsPunkt = yksOtsPunkt.clone();
												for (SemantikaDefinitsioon semDef : kandidaat.semantikaEhitusklotsid) {
													if (semDef.onKontekstistSoltuv()){
														CONTEXT n6utavKontekst = (liikumisSuund > 0) ?
																				((SemantikaDefinitsioon.CONTEXT.NUM_VAHEM_OTSPUNKT_EELNEB)) :
																				((SemantikaDefinitsioon.CONTEXT.NUM_VAHEM_OTSPUNKT_JARGNEB)); 
														if (semDef.kasEtteantudKontekstSisaldubSeotudKontekstis(n6utavKontekst) != null){
															// (d) Paneme leitud semantikadefinitsiooni v22rtuseks t2isarvu (b); kirjutame
															// definitsiooni lahti;
															semDef.setSemValue( ( arvSemantika.get(0) ).toString() );
															List<SemantikaDefinitsioon> semantikadefinitsioonid = 
																SemDefValjadeParsija.kirjutaLahtiSemantikadefinitsioon(
																		semDef, 
																		SemDefValjadeParsija.detectFormatOfValue(semDef.getSemValue()));
															if (semantikadefinitsioonid != null){
																// (e) rakendades definitsioone, konstrueerime teise ajapunkti semantika 
																for (SemantikaDefinitsioon semantikaDef : semantikadefinitsioonid) {
																	teineOtsPunkt = semantikaDef.rakendaArvutusReeglit(teineOtsPunkt, aegFookuses, this.lahimVerb);																
																}
																if (teineOtsPunkt != null){
																	// (f) koondame otspunktid ajavahemikuks
																	if (this.semantikaLahendus == null){
																		this.semantikaLahendus = new AjaPunkt(TYYP.POINT, aegFookuses);
																	}
																	if (liikumisSuund > 0){
																		(this.semantikaLahendus).addRelatedExplicitTIMEX((AjaPunkt)teineOtsPunkt);
																		(this.semantikaLahendus).addRelatedExplicitTIMEX((AjaPunkt)yksOtsPunkt);
																	} else {
																		(this.semantikaLahendus).addRelatedExplicitTIMEX((AjaPunkt)yksOtsPunkt);
																		(this.semantikaLahendus).addRelatedExplicitTIMEX((AjaPunkt)teineOtsPunkt);																		
																	}
																	// Eemaldame lahenduse alamkandidaatidelt
																	((this.alamKandidaadid).get(0)).semantikaLahendus = null;
																}
																break;																
															}															
														}
													}
												} // for
										} // kui arvude esituskujud yhilduvad
										lopetaOtsing = true;
									} // kui leitud mustri poolt eraldatud arv
								}
							} // on seotud m6ne ajavkandidaadiga
							if (lopetaOtsing){
								break;
							}
							j += liikumisSuund;
						}
					}
				} // for
				
			} // yksOtsPunkt != null
		} // size() == 1
	}
	
	/**
	 *   Kontrollib, kas kontekst on sobiv, et rakendada kontekstin6udvat arvutusreeglit <tt>rakendatavDefinitsioon</tt>. 
	 *  Tagastab <tt>true</tt>, kui konteksti j2rgi v6iks semantikadefinitsiooni rakendada, vastasel juhul 
	 *  <tt>false</tt>.
	 */
	private boolean kasKandidaadiKontekstOnRakendamiseksSobiv(
						SemantikaDefinitsioon rakendatavDefinitsioon,
						boolean ankurdamineLabiviidud,
						String [] aegFookuses){
		if (rakendatavDefinitsioon.onKontekstistSoltuv()){
			//
			//   Semantikadefinitsiooni kontekstiosas v6ib olla mitu paralleelset kontekstin6uet.
			//  Seega vaatame l2bi k6ik v6imalikud variandid (CONTEXT-muutjad): kui kasv6i yks neist
			//  pole rahuldatud, on kogu kontekst rakendamiseks EBASOBIV. Kui aga saame ilma t6rgeteta
			//  l6puni, on kontekst vastus positiivne.
			//
			int rahuldatudKontekste = 0;
			String kontekstStr = 
				rakendatavDefinitsioon.kasEtteantudKontekstSisaldubSeotudKontekstis(CONTEXT.KORVALFRAASI_GRAN);
			//
			//   Kontrollime konteksti: K6RVALFRAASI_GRAN 
			//      (st, n6utakse k6rvalfraasilt mingit/mingeid granulaarsust/granulaarsuseid)
			//
			if (kontekstStr != null){
				List<String> n6utudPosGranulaarsused     = new LinkedList<String>();
				List<String> soovimatudNegGranulaarsused = new LinkedList<String>();
				SemDefValjadeParsija.parsiN6utudJaKeelatudTunnused( CONTEXT.KORVALFRAASI_GRAN, 
																	kontekstStr, 
																	n6utudPosGranulaarsused, 
																	soovimatudNegGranulaarsused);
				if (n6utudPosGranulaarsused.isEmpty())     { n6utudPosGranulaarsused = null; }
				if (soovimatudNegGranulaarsused.isEmpty()) { soovimatudNegGranulaarsused = null; }
				if (n6utudPosGranulaarsused != null || soovimatudNegGranulaarsused != null){
					boolean kontrolliTulemus = 
						kontrolliK6rvalFraasiTingimusi(n6utudPosGranulaarsused, 
													   soovimatudNegGranulaarsused, 
													   null, 
													   null);
					if (!kontrolliTulemus){
						return false;
					} else {
						rahuldatudKontekste++;
					}
				} else {
					// L2bikukkumine: n6utud granulaarsuseid ei 6nnestunud kontekstin6udest parsida!
					return false;
				}
			}
			//
			//   Kontrollime konteksti: K6RVALFRAAS_PUUDUB
			//      (st, antud ajav2ljendi k6rval ei tohi olla yhtegi teist ajav2ljendifraasi)
			//
			kontekstStr = 
				rakendatavDefinitsioon.kasEtteantudKontekstSisaldubSeotudKontekstis(CONTEXT.KORVALFRAAS_PUUDUB);
			if (kontekstStr != null){
				boolean kontekstPositiivseltRahuldatud = false;
				if (this.ylemKandidaat == null){
					// Lihtne heuristik: Kui ylemat pole, pole ka k6rvalfraasi
					kontekstPositiivseltRahuldatud = true;
				} else {
					// Kui ylem on, ei tohi see olla "yhendatud fraasina"
					kontekstPositiivseltRahuldatud = (this.ylemKandidaat.getAste() != ASTE.YHENDATUD_FRAASINA);
				}
				if (kontekstStr.startsWith("^")){
					// Eituse m2rk: eitame tingimuse positiivset tulemust
					kontekstPositiivseltRahuldatud = !kontekstPositiivseltRahuldatud;
				}
				if (kontekstPositiivseltRahuldatud){
					rahuldatudKontekste++;
				} else {
					// L2bikukkumine: K6rvalfraasi puudumise tingimus pole rahuldatud! 
					return false;
				}
			}
			//
			//   Kontrollime konteksti: K6RVALFRAASI_LABEL
			//      (k6rvalfraasil peavad olema v6i ei tohi olla n6utud lipikud (semLabel))
			//
			
			kontekstStr = 
				rakendatavDefinitsioon.kasEtteantudKontekstSisaldubSeotudKontekstis(CONTEXT.KORVALFRAASI_LABEL);
			if (kontekstStr != null){
				List<String> n6utudPosLipikud     = new LinkedList<String>();
				List<String> soovimatudNegLipikud = new LinkedList<String>();
				SemDefValjadeParsija.parsiN6utudJaKeelatudTunnused( CONTEXT.KORVALFRAASI_LABEL, 
																	kontekstStr, 
																	n6utudPosLipikud, 
																	soovimatudNegLipikud);
				if (n6utudPosLipikud.isEmpty())     { n6utudPosLipikud = null; }
				if (soovimatudNegLipikud.isEmpty()) { soovimatudNegLipikud = null; }
				if (n6utudPosLipikud != null || soovimatudNegLipikud != null){
					boolean kontrolliTulemus = 
						kontrolliK6rvalFraasiTingimusi(null, 
													   null, 
													   n6utudPosLipikud, 
													   soovimatudNegLipikud);
					if (!kontrolliTulemus){
						return false;
					} else {
						rahuldatudKontekste++;
					}
				} else {
					// L2bikukkumine: n6utud lipikuid ei 6nnestunud kontekstin6udest parsida!
					return false;
				}
			}			
			//
			//   Kontrollime konteksti: ANKURDAMINE_LABIVIIDUD
			//      (st, on leitud eelmine/j2rgmine ajav2ljend ning ankurdatud tolle semantika kylge)
			//
			kontekstStr = 
				rakendatavDefinitsioon.kasEtteantudKontekstSisaldubSeotudKontekstis(CONTEXT.ANKURDAMINE_LABIVIIDUD);
			if (kontekstStr != null){
				boolean kontekstPositiivseltRahuldatud = (ankurdamineLabiviidud == true);
				if (kontekstStr.startsWith("^")){
					// Eituse m2rk: eitame tingimuse positiivset tulemust
					kontekstPositiivseltRahuldatud = !kontekstPositiivseltRahuldatud;
				}				
				if (kontekstPositiivseltRahuldatud){
					rahuldatudKontekste++;
				} else {
					// L2bikukkumine: Ankurdamise l2biviiduse tingimus pole rahuldatud! 
					return false;
				}
			}
			//
			//   Kontrollime konteksti: ANKRU_GRAN
			//      (st, antud ajav2ljendi ankrupunkti granulaarsus on mingil kindlal kujul)
			//
			kontekstStr = 
				rakendatavDefinitsioon.kasEtteantudKontekstSisaldubSeotudKontekstis(CONTEXT.ANKRU_GRAN);
			if (kontekstStr != null && aegFookuses != null){
				boolean ajalineFookusL2bisKontrolli = 
					SemDefValjadeParsija.fookusaegRahuldabSeotudKontekstiTingimusi(kontekstStr, aegFookuses);
				if (ajalineFookusL2bisKontrolli){
					rahuldatudKontekste++;
				} else {
					return false;
				}
			}
			return (rahuldatudKontekste > 0);
		}
		return true;
	}
	
	//==============================================================================
	//   K a s    k o r v a l f r a a s    v a s t a b    t i n g i m u s t e l e ? 
	//==============================================================================
	
	/**
	 *   Kontrollib, kas k6rvalfraas vastab etteantud kontekstin6uetele. Tagastab
	 *   <tt>true</tt>, kui k6ik tingimused leidsid loogilist rahuldust, vastasel juhul aga
	 *   <tt>false</tt>. Loogiliseks rahulduseks loeme, kui:
	 *   <ul>
	 *      <li> Igast n6utud tunnuste hulgast esineb k6rvalfraasis v2hemalt yks;
	 *      <li> Mitte ykski keelatud tunnus k6rvalfraasides ei esine;
	 *   </ul>
	 *   <p>
	 *   Kontrollitakse semantikadefinitsioonides sisalduvate tunnuste vastavust tingimustele, 
	 *   vaatluse alt j2etakse v2lja ankurdamis-semantikadefinitsioonid; 
	 *   <p>
	 *   <i>K6rvalfraasi liikmeteks</i> loeme selle kandidaadi vahetu ylema k6iki alamkandidaate
	 *   (va see kandidaat). Ylem peab olema kas fraasina v6i vahemikuna yhendatud ajav2ljend.
	 *   Kui sellel fraasil k6rvalfraase ei leidu, kontrollitakse positiivsete kontekstin6uete 
	 *   olemasolu: kui neid leidub, tagastatakse <tt>false</tt>.
	 */
	private boolean kontrolliK6rvalFraasiTingimusi(List<String> n6utudPosGranulaarsused,
												   List<String> soovimatudNegGranulaarsused,
												   List<String> n6utudLipikud,
												   List<String> keelatudLipikud){
		
		// Kontrollime, kas selle kandidaadiga yhendatud k6rvalkandidaadid sisaldavad n6utuid / ei 
		// sisalda keelatuid tunnuseid
		// 1) Leiame, kas see ajav2ljend kuulub mingisse fraasi v6i vahemikku
		AjavaljendiKandidaat ylemKandidaat = this.ylemKandidaat;
		if (ylemKandidaat != null && (ylemKandidaat.aste == ASTE.YHENDATUD_FRAASINA ||
									  ylemKandidaat.aste == ASTE.YHENDATUD_VAHEMIKUNA)){
			// Leiame fraasi/vahemiku teised liikmed
			List<AjavaljendiKandidaat> ylemaAlamKandidaadid = 
					ylemKandidaat.leiaLehtTasemeAlamKandidaadid();
			boolean positiveGranularityFound = false;
			boolean positiveLabelFound       = false;
			boolean kontrolliGranulaarsusi = (n6utudPosGranulaarsused != null || soovimatudNegGranulaarsused != null);
			boolean kontrolliLipikuid      = (n6utudLipikud != null || keelatudLipikud != null);
			for (AjavaljendiKandidaat ajavaljendiKandidaat : ylemaAlamKandidaadid) {
				if (ajavaljendiKandidaat != this){
					//  Kontrollime teise kandidaadi semantikadefinitsioonide tunnuseid 
					List<SemantikaDefinitsioon>	semDefsOfKorvalseisevAv = 
									ajavaljendiKandidaat.semantikaEhitusklotsid;
					for (SemantikaDefinitsioon semDef : semDefsOfKorvalseisevAv) {
						if (semDef.onAnkurdamisOperatsioon()){
							// ankurdamisoperatsioone ei arvesta
							continue;
						}
						//
						//  A) Granulaarsuste kontrollimine
						//
						if (kontrolliGranulaarsusi && semDef.getGranulaarsus() != null){
							String semDefGran = (semDef.getGranulaarsus()).toString();
							// Kontrollime, et poleks yhtki negatiivset granulaarsust
							if (soovimatudNegGranulaarsused != null){
								for (String gran : soovimatudNegGranulaarsused) {
									if (gran.equals( semDefGran )){
										return false;
									}
								}
							}
							// Kontrollime, kas on m6ni positiivne granulaarsus
							if (n6utudPosGranulaarsused != null){
								for (String gran : n6utudPosGranulaarsused) {
									if (gran.equals( semDefGran )){
										positiveGranularityFound = true;
									}
								}												
							}
						} // if
						
						//
						//  B) Lipikute kontrollimine
						//
						if (kontrolliLipikuid && semDef.getSemLabel() != null){
							// Kontrollime, et poleks yhtki negatiivset lipikut
							if (keelatudLipikud != null){
								for (String lipik : keelatudLipikud) {
									if (lipik.equals(semDef.getSemLabel())){
										return false;
									}
								}
							}
							// Kontrollime, kas on m6ni positiivne lipik
							if (n6utudLipikud != null){
								for (String lipik : n6utudLipikud) {
									if (lipik.equals(semDef.getSemLabel())){
										positiveLabelFound = true;
									}									
								}
							}												
						} // if
						
					} // for
					
				} // if
			} // for
			
			// 
			//   Kontroll on l2bi, langetame kontrolli tulemuste p6hjal otsuse
			//    - k6ik negatiivsed on juba kontrollitud (neg leidmisel tagastati false);
			//    - tuleb kontrollida, kas m6ni n6utud positiivne j22nud rahuldamata;
			// 
			
			// N6uti positiivset granulaarsust, aga ei leitud ...
			if (n6utudPosGranulaarsused != null && !positiveGranularityFound){
				return false;
			}
			// N6uti positiivset lipikut, aga ei leitud ...			
			if (n6utudLipikud != null && !positiveLabelFound){
				return false;
			}			
			return true;
		} else {
			//
			// 2) Kui ylemkanditaati pole ...
			//
			if (n6utudPosGranulaarsused != null){
				//  ... ning pos granulaarsusi n6utakse, on l2bikukkumine
				return false;
			}
			if (n6utudLipikud != null){
				//  ... ning mingeid lipikuid n6utakse, on l2bikukkumine
				return false;
			}
			//
			//   + kui positiivseid n6udmisi polnud, v6ime lugeda tingimused t2idetuks.
			//
			return true;
		}
	}

	//==============================================================================
	//   K a n d i d a a d i    p o o l i t a m i n e 
	//==============================================================================
	
	/**
	 *   Kui on n6utud (muutuja <code>poolitusKoht</code> on seatud), algatab ajav2ljendikandidaadi poolitamise.
	 *   Poolitamist saab teha vaid siis, kui semantika on lahendatud (semantikalahendused leitud); Poolitamise
	 *   pohiliseks eesmargiks on ilmutatud kujul olevad vahemiku otspunktid tekstis eraldiseisvate kandidaatidena 
	 *   margendada;
	 */
	public void teostaPoolitamisedKuiVaja(){
		if (this.poolituskoht != -1 && this.poolituskoht < (this.fraas).size() && this.semantikaLahendus != null){
			// 1) Kui k2esoleva objekti semantikalahenduseks on intervall
			if ((this.semantikaLahendus).hasRelatedExplicitTimexesAsInterval()  &&  this.ylemKandidaat == null  &&  this.alamKandidaadid == null){
				teostaPoolitamine();
				this.aste = ASTE.YHENDATUD_VAHEMIKUNA;
			}
			// 2) Kui on ainult yks alamkandidaat-otspunkt (vahemiku teine otspunkt on leitud automaatselt, meetodi
			//    leiaAjavahemikuPuuduvOtspunktJaSeoVahemikuga abil)
			else if (this.aste == ASTE.YHENDATUD_VAHEMIKUNA && (this.semantikaLahendus).hasRelatedExplicitTimexesAsInterval() &&
				this.alamKandidaadid != null && (this.alamKandidaadid).size() == 1){
				// Kustutame vanad seosed, loome uued ...
				(this.alamKandidaadid.get(0)).eemaldaEnnastSonadeKyljest();
				this.alamKandidaadid = null;
				// teostame poolitamise samamoodi, nagu juhul 1)
				teostaPoolitamine();
			}
			// 3) Kui on intervall on mingil p6hjusel sattunud alamkandidaadi otsa ning ylemkandidaat on fraas
			else if ((this.semantikaLahendus).hasRelatedExplicitTimexesAsInterval() && this.aste == ASTE.MUSTRI_POOLT_ERALDATUD && 
							this.ylemKandidaat != null && (this.ylemKandidaat).getSemantikaLahendus() == null){
				// Proovime k2esoleva kandidaadi ylema kyljest lahti siduda 
				if ((this.ylemKandidaat).eemaldaAlamkandidaat(this)){
					// kui lahtisidumine 6nnestus, teostame poolitamise samamoodi, nagu juhul 1)
					teostaPoolitamine();
					this.aste = ASTE.YHENDATUD_VAHEMIKUNA;
				}
			}
		}
	}
	
	/**
	 *   Teostab k2esoleva kandidaadi poolitamise j2rgnevalt:
	 *   <ul>
	 *      <li> Jagab fraasi poolituskohalt pooleks ning moodustab m6lema poolega sidumiseks uue alamkandidaadi;
	 *      <li> Kopeerib k2esoleva kandidaadi semantika lahenduse poolte semantika lahenduseks; Kui k2esoleva
	 *           kandidaadi lahendus on intervall, kopeerib vastavate otspunktide lahendused poolte lahendusteks; 
	 *           Kustutab k2esoleva kandidaadi semantika lahenduse;
	 *      <li> Seab loodud kandidaadid k2esoleva kandidaadi alamkandidaatideks;
	 *   </ul>
	 *   Tagastab t6ese kui poolitamine 6nnestus, vastasel juhul on tagastusv22rtuseks v22r;
	 */
	private boolean teostaPoolitamine(){
		// 1) Teostame nn. eraldamist puudutava osa poolitamist: moodustame kaks uut kandidaati ning seome s6nadega
		List<AjavtSona> algusOsa = null;
		List<AjavtSona> lopuOsa  = null;
		if (this.poolitusOnInklusiivne){
			algusOsa = (this.fraas).subList(0, this.poolituskoht + 1);
			lopuOsa  = new ArrayList<AjavtSona>((this.fraas).subList(this.poolituskoht + 1, (this.fraas).size()));
		} else {
			algusOsa = (this.fraas).subList(0, this.poolituskoht);
			lopuOsa  = new ArrayList<AjavtSona>((this.fraas).subList(this.poolituskoht + 1, (this.fraas).size()));
		}
		if (algusOsa != null && !algusOsa.isEmpty() && lopuOsa != null && !lopuOsa.isEmpty() && this.semantikaLahendus != null){
			AjavaljendiKandidaat a1 = new AjavaljendiKandidaat();
			AjaTuvastaja.seoSonadKandidaadiKylge(a1, algusOsa);
			a1.setMustriID(this.mustriID);
			AjavaljendiKandidaat a2 = new AjavaljendiKandidaat();
			a2.setMustriID(this.mustriID);
			AjaTuvastaja.seoSonadKandidaadiKylge(a2, lopuOsa);
			// 2) Teostame semantika osa poolitamise: kanname ylemkandidaadi semantika tervenisti alamkanditaatidele
			//    v6i - vahemike korral, kanname semantika alamkandidaatidele otspunktide kaupa ... 
			if ((this.semantikaLahendus).hasRelatedExplicitTimexesAsInterval()){
				a1.semLahendamineLabiviidud = true;
				if (((this.semantikaLahendus)).getRelatedExplicitTIMEXES() != null){
					a1.semantikaLahendus = (((this.semantikaLahendus).getRelatedExplicitTIMEXES()).get(0)).clone();						
				}
				a2.semLahendamineLabiviidud = true;
				if (((this.semantikaLahendus)).getRelatedExplicitTIMEXES() != null){
					a2.semantikaLahendus = (((this.semantikaLahendus).getRelatedExplicitTIMEXES()).get(1)).clone();						
				}
			} else {
				a1.semLahendamineLabiviidud = true;
				a1.semantikaLahendus = (this.semantikaLahendus).clone();
				a2.semLahendamineLabiviidud = true;
				a2.semantikaLahendus = (this.semantikaLahendus).clone();					
			}
			List<AjavaljendiKandidaat> alamkandidaatideks = new ArrayList<AjavaljendiKandidaat>(2);
			alamkandidaatideks.add(a1);
			alamkandidaatideks.add(a2);
			this.seoKylgeAlamkandidaadid( alamkandidaatideks );
			this.semantikaLahendus = null;
			return true;
		}
		return false;
	}

	/**
	 *   Parsib etteantud s6nest poolituse parameetrid ning seadistab nendele vastavalt muutujad
	 *   <code>poolitusOnInklusiivne</code> ja <code>poolituskoht</code>. Eeldab, et sobivad
	 *   v22rtused on s6nes komaga eraldatult: esimesena poolituskoht, teisena inklusiivsus;  
	 */
	private void parsiPoolituseParameetrid(String semValue){
		if (semValue != null){
			String [] partsOfVal = semValue.split(",");
			try {
				int poolitusk = Integer.parseInt(partsOfVal[0]);
				this.poolituskoht = poolitusk;
			} catch (NumberFormatException e) {
			}
			if (partsOfVal.length > 1){
				// Vaikimisi on true, seega kontrollime ainult false juhtu ...
				if (partsOfVal[1].matches("(?i)\\s*(0|false)\\s*")){
					this.poolitusOnInklusiivne = false;
				}
			}
		}
	}

	//==============================================================================
	//      I m p l i t s i i t s e t e    l i n k i d e    l i s a m i n e
	//==============================================================================
	
	/**
	 *    Loome antud ajavaljendikandidaadiga seotud implitsiitsed ajalise semantika avaldumised. 
	 *    Sisuliselt m6eldakse nende all TimeML atribuutide <code>beginPoint</code> ja 
	 *    <code>endPoint</code> taga olevaid, tekstis mitteesinevaid ajavaljendeid.
	 */
	public void lisaImplitsiitsedLingid(){
		// tegemist on vahemikuga
		if (this.aste == ASTE.YHENDATUD_VAHEMIKUNA && (this.alamKandidaadid).size() == 2){
			AjavaljendiKandidaat beginPoint = (this.alamKandidaadid).get(0);
			AjavaljendiKandidaat endPoint   = (this.alamKandidaadid).get(1);
			if (beginPoint.getSemantikaLahendus() != null && endPoint.getSemantikaLahendus() != null){
				AjaObjekt beginSem = beginPoint.getSemantikaLahendus();
				AjaObjekt endSem   = endPoint.getSemantikaLahendus();
				if ( (beginSem.getType() == TYYP.TIME || beginSem.getType() == TYYP.POINT) && 
					 (endSem.getType() == TYYP.TIME   || endSem.getType() == TYYP.POINT) ){
					// Esialgu lisame implitsiitse lingi ainult siis, kui m6lemad eksplitsiitsed on ajapunktid
					AjaKestvus intervall = new AjaKestvus();
					intervall.setTimex3Attribute("beginPoint", (beginPoint.getSemantikaLahendus()).getTimex3Attribute("tid"));
					intervall.setTimex3Attribute("endPoint", (endPoint.getSemantikaLahendus()).getTimex3Attribute("tid")    );
					intervall.setValueAsPeriodBetweenPoints(
							(AjaPunkt)(beginPoint.getSemantikaLahendus()), 
							(AjaPunkt)(endPoint.getSemantikaLahendus()));
					// implitsiitse vahemiku lisame teise otspunkti jarele
					(endPoint.getSemantikaLahendus()).addRelatedImplicitTIMEX( intervall );
				}
			}
		}
	}
	
	
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================	
	
	public String getMustriID() {
		return mustriID;
	}

	public void setMustriID(String mustriID) {
		this.mustriID = mustriID;
	}	
	
	/**
	 *   Parsib etteantud mustritahised lahti ja kinnitab selle ajavaljendikandidaadi kylge. 
	 *   <p>
	 *  Kui moni mustritahistest margib, et antud ajavaljendikandidaat ei tohi olla eraldiseisev ajavaljend, 
	 *  omistab antud kandidaadi muutuja <tt>poleEraldiseisevAjavaljend</tt> vaartuseks <tt>true</tt>;   
	 */
	public void lisaMustriTahised(List<MustriTahis> sisendMustriTahised){
		if (this.mustriTahised == null){
			this.mustriTahised = new ArrayList<String>( sisendMustriTahised.size() + 3 );
		}
		for (int i = 0; i < sisendMustriTahised.size(); i++) {
			MustriTahis mustriTahis = sisendMustriTahised.get(i);
			String mustriTahisedSonena = mustriTahis.getMustriTahised();
			if (mustriTahisedSonena != null){
				String[] alamTahised = mustriTahisedSonena.split(",");
				for (int j = 0; j < alamTahised.length; j++) {
					(this.mustriTahised).add( TextUtils.trim( alamTahised[j] ) );
				}
				if (mustriTahis.isAjavPoleEraldiSeisev()){
					this.setPoleEraldiseisevAjavaljend( true );
				}
			}
		}
	}
	
	public String getMustriTahisedAsString(){
		StringBuilder sb = new StringBuilder();
		if (this.mustriTahised != null){
			for (int i = 0; i < this.mustriTahised.size(); i++) {
				sb.append(this.mustriTahised.get(i));
				sb.append(" ");
			}
		}
		return sb.toString();		
	}	
	
	//=============================================
	//    Y l e m / a l a m k a n d i d a a d i d
	//            f r a a s i p u u s
	//=============================================
	
	/**
	 *    Leiab rekursiivselt k6ige k6rgema antud kandidaadi ylemkandidaadi. Kui
	 *   antud kandidaadil ylemkandidaate pole, on selleks kandidaat ise.  
	 */
	public AjavaljendiKandidaat leiaK6igeK6rgemYlemkandidaat(){
		return (this.ylemKandidaat == null) ? (this) : ( (this.ylemKandidaat).leiaK6igeK6rgemYlemkandidaat() );
	}

	/**
	 *   Leiab rekursiivselt k6ik antud kandidaadi leht-taseme alamkandidaadid.
	 *  Kui antud kandidaat on ise leht, tagastab ta iseenda.
	 */
	public List<AjavaljendiKandidaat> leiaLehtTasemeAlamKandidaadid(){
		List<AjavaljendiKandidaat> tagastatavadAlamKandidaadid = 
										new LinkedList<AjavaljendiKandidaat>();
		if (this.alamKandidaadid == null){
			// Kui antud kandidaat on ise leht, tagastab ta ennast
			tagastatavadAlamKandidaadid.add(this);
		} else {
			// Kui antud kandidaat on mitteleht, tagastab ta k6ik oma alamad lehed
			for (AjavaljendiKandidaat ajavaljendiKandidaat : this.alamKandidaadid) {
				tagastatavadAlamKandidaadid.addAll(ajavaljendiKandidaat.leiaLehtTasemeAlamKandidaadid());
			}
		}
		return tagastatavadAlamKandidaadid;
	}
	
	/**
	 *  Seob antud ajavaljendikandidaadi kylge rea alamkandidaate. Yldiseks eesm2rgiks 
	 *  on moodustada ajavaljendikandidaatide puu, milles saab navigeerida viitade 
	 *  <tt>ylemKandidaat</tt> ja <tt>alamKandidaadid</tt> abil. 
	 */
	public void seoKylgeAlamkandidaadid(List<AjavaljendiKandidaat> alamkandidaadid){
		StringBuilder sb = new StringBuilder();
		// 1) Paneme k6ik alamkandidaadid viitama sellele kandidaadile
		for (int i = 0; i < alamkandidaadid.size(); i++) {
			AjavaljendiKandidaat alamKandidaat = alamkandidaadid.get(i);
			sb.append( alamKandidaat.getMustriID() );
			alamKandidaat.ylemKandidaat = this;
			if (i < alamkandidaadid.size() - 1){
				sb.append( "-" );
			}
		}
		// 2) Paneme selle kandidaadi viitama alamkandidaatidele
		this.alamKandidaadid = alamkandidaadid;
		this.mustriID = sb.toString();
	}
	
	/**
	 *  Eemaldab etteantud ajaväljendikandidaadi selle kandidaadi alamate seast.
	 *  Eemaldamine viiakse läbi vaid siis, kui eemaldatav kandidaat on paiknemise
	 *  poolest üks otsmistest kandidaatidest;
	 *  <p>
	 *  NB! Eeldab, et etteantud kandidaat on k2esoleva kandidaadi poolt kaetud
	 *  t2ielikult, mitte osaliselt.
	 *  <p>
	 *  Tagastab selle, kas eemaldamine 6nnestus.   
	 */
	public boolean eemaldaAlamkandidaat(AjavaljendiKandidaat kandidaat){
		boolean success = false;
		if (this.fraas != null && (this.fraas).size() > 0){
			// 1) Muudame selle kandidaadi fraasi selliselt, et eemaldatava kandidaadiga seotud s6nad kustutatakse
			AjavtSona esiSona = (this.fraas).get(0);
			AjavtSona viiSona = this.fraas.get( (this.fraas).size()-1 );
			if ((esiSona.getAjavaljendiKandidaadid()).contains(kandidaat)){
				if ( (kandidaat.fraas).size() < (this.fraas).size()){
					// V6tame k6ik juppideks ja paneme ainult 6iged jupid tagasi ...
					LinkedList<AjavtSona> uusFraas = 
						new LinkedList<AjavtSona>( (this.fraas).subList( (kandidaat.fraas).size(), (this.fraas).size() ) );
					this.eemaldaEnnastSonadeKyljest();
					for (int i = 0; i < uusFraas.size(); i++) {
						FraasisPaiknemiseKoht koht = FraasisPaiknemiseKoht.AINUSSONA;
						if (uusFraas.size() > 1){
							if (i == 0){
								koht = FraasisPaiknemiseKoht.ALGUSES;
							} else if (i == uusFraas.size() - 1){
								koht = FraasisPaiknemiseKoht.LOPUS;
							} else {
								koht = FraasisPaiknemiseKoht.KESKEL;
							}
						}
						this.lisaFraasiUusSona(uusFraas.get(i), koht);
					}
					success = alamKandidaadid.remove(kandidaat);					
				}
			} else if ((viiSona.getAjavaljendiKandidaadid()).contains(kandidaat)){
				if ((this.fraas).size() - (kandidaat.fraas).size() > 0){
					// V6tame k6ik juppideks ja paneme ainult 6iged jupid tagasi ...
					LinkedList<AjavtSona> uusFraas = 
						new LinkedList<AjavtSona>( (this.fraas).subList( 0, (this.fraas).size() - (kandidaat.fraas).size() ) );
					this.eemaldaEnnastSonadeKyljest();
					for (int i = 0; i < uusFraas.size(); i++) {
						FraasisPaiknemiseKoht koht = FraasisPaiknemiseKoht.AINUSSONA;
						if (uusFraas.size() > 1){
							if (i == 0){
								koht = FraasisPaiknemiseKoht.ALGUSES;
							} else if (i == uusFraas.size() - 1){
								koht = FraasisPaiknemiseKoht.LOPUS;
							} else {
								koht = FraasisPaiknemiseKoht.KESKEL;
							}
						}
						this.lisaFraasiUusSona(uusFraas.get(i), koht);
					}					
					success = alamKandidaadid.remove(kandidaat);
				}
			}
		}
		if (success){
			kandidaat.ylemKandidaat = null;
		}
		return success;
	}
	
	/**
	 *   Saab sisendiks (ajavaljendikandidaatidega seotud) fraasi. Eraldab fraasist k6rgeima 
	 *  astmega ajavaljendikandidaadid, ning v6tab endale alamkandidaatideks (t2psemalt vaata
	 *  meetodit <tt>seoKylgeAlamkandidaadid</tt>). Yldiseks eesm2rgiks on moodustada 
	 *  ajavaljendikandidaatide puu, milles saab navigeerida viitade <tt>ylemKandidaat</tt> 
	 *  ja <tt>alamKandidaadid</tt> abil. 
	 *  <p>
	 *  NB! Eeldame, et ajavaljendikandidaatide seas EI esine osalisi ylekatteid (nt VAHEMIK
	 *  algab kusagilt FRAASi keskelt vms).
	 */
	public void votaAntudSonadegaSeotudKandidaadidAlamkandidaatideks(List<AjavtSona> fraas){
		List<AjavaljendiKandidaat> alamkandidaadid 		  = new ArrayList<AjavaljendiKandidaat>();
		// 1) Kogume etteantud fraasist kokku alamkandidaadid 
		AjavtSona viimaseYlekandidaadiViimaneSona = null;
		for (int i = 0; i < fraas.size(); i++) {
			AjavtSona sona = fraas.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				if (viimaseYlekandidaadiViimaneSona == null){
					List<FraasisPaiknemiseKoht> kandidaatFraasides = sona.getAjavaljendiKandidaatides();
					List<AjavaljendiKandidaat>                   kandidaadid = sona.getAjavaljendiKandidaadid();
					// Otsime kandidaati, millele on m6ni ylemkandidaat seatud
					AjavaljendiKandidaat leitudKandidaat = null;
					for (int j = 0; j < kandidaadid.size(); j++) {
						AjavaljendiKandidaat kandidaat      = kandidaadid.get(j);
						FraasisPaiknemiseKoht fpk = kandidaatFraasides.get(j);
						// Meid huvitavad ainult kandidaatide alguskohad tekstis
						if (fpk.onFraasiAlgus()){
							if (kandidaat.ylemKandidaat != null){
								// Leidsime esimese ylemkandidaadiga kandidaadi: sobib
								leitudKandidaat = kandidaat;
								break;
							} else {
								//   Kui ylemkandidaat on null, j2tame meelde k6ige esimese
								// s6naga seotud kandidaadi (juhuks, kui yhelgi kandidaatidest /
								// pole ylemkandidaati)
								if (leitudKandidaat == null){
									leitudKandidaat = kandidaat;
								}
							}
						}
					}
					if (leitudKandidaat != null){
						AjavaljendiKandidaat k6rgeimYlemKandidaat = leitudKandidaat.leiaK6igeK6rgemYlemkandidaat();
						alamkandidaadid.add(k6rgeimYlemKandidaat);
						viimaseYlekandidaadiViimaneSona = 
							(k6rgeimYlemKandidaat.fraas).get((k6rgeimYlemKandidaat.fraas).size() - 1);
					}
				}
				if (sona.equals(viimaseYlekandidaadiViimaneSona)){
					// oleme j6udnud viimase s6nani (mis on juba arvestatud), nyyd v6ime alustad j2lle
					// uute s6nade lisamist
					viimaseYlekandidaadiViimaneSona = null;
				}
			}	
		}
		// 2) Seome alamkandidaadid selle kandidaadiga
		if (!alamkandidaadid.isEmpty()){
			this.seoKylgeAlamkandidaadid(alamkandidaadid);			
		}
	}
	
	public void lisaTuvastamisReegel(TuvastamisReegel tr){
		if (this.tuvastamisReeglid == null){
			(this.tuvastamisReeglid) = new ArrayList<TuvastamisReegel>();
		}
		(this.tuvastamisReeglid).add(tr);
	}
	
	public List<TuvastamisReegel> getTuvastamisReeglid(){
		return (this.tuvastamisReeglid);
	}
	
	/**
	 *   Ajav&auml;ljend eemaldab ennast k6igi listis <code>fraas</code> olevate s6nade 
	 *   kyljest. Sisuliselt t2hendab see ajavaljendi kustutamist.  
	 */
	public void eemaldaEnnastSonadeKyljest(){
		for (int i = 0; i < (this.fraas).size(); i++) {
			((this.fraas).get(i)).eemaldaAjavaljendiKandidaat(this);
		}
	}	
	
	public void lisaSemantikaEhitusklots(SemantikaDefinitsioon semEhitusklots){
		(this.semantikaEhitusklotsid).add(semEhitusklots);
	}
	
	public void setPoleEraldiseisevAjavaljend(boolean poleEraldiseisevAjavaljend) {
		this.poleEraldiseisevAjavaljend = poleEraldiseisevAjavaljend;
	}

	public boolean isPoleEraldiseisevAjavaljend() {
		return this.poleEraldiseisevAjavaljend;
	}

	/**
	 *   Lisab selle kandidaadiga seotud fraasi l6ppu uue sona. Selle kandidaadi ja sona vahele luuakse 
	 *  m6lemapoolne link:<br>
	 *  AjavaljendiKandidaat sisaldab endas listi temasse kuuluvatest sonadest (1) ning iga sona kyljes
	 *  on omakorda list k6igist ajavaljendikandidaateist (2), mille koosseisu sona kuulub. 
	 */
	public void lisaFraasiUusSona(AjavtSona sona, FraasisPaiknemiseKoht paiknemiseKoht){
		(this.fraas).add(sona);
		sona.margendaAjavaljendiKandidaadiga(this, paiknemiseKoht);
	}

	public List<AjavtSona> getFraas() {
		return fraas;
	}
	
	public ASTE getAste() {
		return aste;
	}

	public void setAste(ASTE aste) {
		this.aste = aste;
	}

	/**
	 *   Tagastab antud ajavaljendikandidaadiga seotud vahima ning suurima 'j&auml;meda' granulaarsuse astme.
	 *   Tagastatavas massiivis on esimene elementi vahim ning teine element suurim granulaarsuse aste.
	 *   <p>
	 *   Granulaarsuste leidmisel ei arvesta ankurdamisega seotud semantikadefinitsioone.
	 */
	private int[] getLowestAndHighestGranularityCroaseRank(){
		int [] lowestAndHighestRank = new int [2];
		lowestAndHighestRank[Granulaarsus.LOWEST ] = -1;
		lowestAndHighestRank[Granulaarsus.HIGHEST] = -1;
		if ((this.semantikaEhitusklotsid).size() > 0){
			lowestAndHighestRank[Granulaarsus.LOWEST] = Integer.MAX_VALUE;
			for (SemantikaDefinitsioon semDef : this.semantikaEhitusklotsid) {
				if (semDef.onAnkurdamisOperatsioon()){
					continue;
				}
				if (semDef.getGranulaarsus() != null){
					int croaseRank = (semDef.getGranulaarsus()).getCoarseRank();
					if (croaseRank < lowestAndHighestRank[Granulaarsus.LOWEST]){
						lowestAndHighestRank[Granulaarsus.LOWEST] = croaseRank;
					}
					if (croaseRank > lowestAndHighestRank[Granulaarsus.HIGHEST]){
						lowestAndHighestRank[Granulaarsus.HIGHEST] = croaseRank;
					}
				}
			}
			if (lowestAndHighestRank[Granulaarsus.LOWEST] == Integer.MAX_VALUE){
				lowestAndHighestRank[Granulaarsus.LOWEST] = -1;
			}
		}
		return lowestAndHighestRank;
	}
	
	public AjavtSona getLahimVerb() {
		return lahimVerb;
	}

	public void setLahimVerb(AjavtSona lahimVerb) {
		this.lahimVerb = lahimVerb;
	}	
	
	/**
	 *    Kontrollib,  kas  kandidaadi semantikalahendus on kontekstist otseselt
	 *  s6ltuv. Tehniliselt loeme lahenduse kontekstist s6ltuvaks, kui kandidaadi
	 *  kyljes on v2hemalt yks semantikadefinitsioon, milles on defineeritud muutuja 
	 *  <tt>seotudKontekst</tt> v22rtus.
	 *  <p>
	 *  NB! Ankurdamisoperatsioone ning nende kontekstists6ltuvusi ei arvestata,
	 *  vaadeldakse ainult otseselt arvutamise/lahendamisega seotud operatsioone.
	 */
	public boolean lahendusOnKontekstistSoltuv(){
		if (this.semantikaEhitusklotsid != null){
			for (SemantikaDefinitsioon semDef : this.semantikaEhitusklotsid) {
				if (semDef.onAnkurdamisOperatsioon()){
					continue;
				}
				if (semDef.onKontekstistSoltuv()){
					return true;
				}
			}
		}
		return false;		
	}
	
	/**
	 *   Tagastab t6ese, kui antud kandidaat vajab enne semantika leidmist ankurdamist (nt teise kandidaadi kylge);
	 *   Sisuliselt kontrollib, kas kandidaadiga seotud semantikadefinitsioonide hulgas on semantikadefinitsioon
	 *   operatsiooniga <tt>ANCHOR_TIMEX</tt>. Arvestab ainult mudeli <tt>mudel</tt> alla kuuluvaid definitsioone. 
	 */
	public boolean vajabAnkurdamist(String [] mudel){
		if (this.semantikaEhitusklotsid != null){
			for (SemantikaDefinitsioon semDef : this.semantikaEhitusklotsid) {
				if (semDef.vastabSemLeidmiseMudelile(mudel)){
					if (semDef.getOp() != null && 
							(semDef.onAnkurdamisOperatsioon()) &&
								kasKandidaadiKontekstOnRakendamiseksSobiv(semDef, false, null)){
						return true;
					}					
				}
			}
		}
		return false;
	}
	
	/**
	 *    Leiab rekursiivselt, kas antud ajav2ljendikandidaat v6i m6ni temaga samma fraasipuusse
	 *   kuuluv kandidaat vajab ankurdamist. Kui vajab, tagastab <tt>true</tt>;
	 */
	public boolean vajabAnkurdamistRek(String [] mudel){
		if (this.ylemKandidaat != null){
			return (this.ylemKandidaat).vajabAnkurdamistRek(mudel);
		} else if (this.alamKandidaadid != null){
			List<AjavaljendiKandidaat> leiaLehtTasemeAlamKandidaadid = 
				this.leiaLehtTasemeAlamKandidaadid();
			for (AjavaljendiKandidaat alamKandidaat : leiaLehtTasemeAlamKandidaadid) {
				if (alamKandidaat.vajabAnkurdamist(mudel)){
					return true;
				}
			}
		} else {
			return this.vajabAnkurdamist(mudel);
		}
		return false;
	}
	
	public AjaObjekt getSemantikaLahendus() {
		return semantikaLahendus;
	}

	public void setSemantikaLahendus(AjaObjekt semantikaLahendus) {
		this.semantikaLahendus = semantikaLahendus;
	}

	/**
	 *   Kas semantika lahendamine on l2biviidud? NB! L2biviidus ei t2henda veel seda, et
	 *   semantika 6nnestus ka leida, st <tt>semantikaLahendus != null</tt>.
	 *   <p>
	 *   Peaasjalikult vajalik selleks, et pidada j2rge, millised ajavaljendikandidaadid on
	 *   l6pliku lahenduse leidnud ja millised mitte.
	 */
	public boolean isSemLahendamineLabiviidud() {
		return semLahendamineLabiviidud;
	}

	public void setSemLahendamineLabiviidud(boolean semLahendamineLabiviidud) {
		this.semLahendamineLabiviidud = semLahendamineLabiviidud;
	}
	
	public List<SemantikaDefinitsioon> getSemantikaEhitusklotsid() {
		return semantikaEhitusklotsid;
	}

	public AjavaljendiKandidaat getAnkurdatudKandidaat() {
		return ankurdatudKandidaat;
	}

	public void setAnkurdatudKandidaat(AjavaljendiKandidaat ankurdatudKandidaat) {
		this.ankurdatudKandidaat = ankurdatudKandidaat;
	}
	
	public void setPoolitus(int poolitusKoht, boolean poolitusOnInklusiivne){
		this.poolituskoht = poolitusKoht;
		this.poolitusOnInklusiivne = poolitusOnInklusiivne;
	}
	
	/**
	 * Leiab, kas antud ajav2ljendikandidaat katab m6nda tekstipositsiooni, 
	 * kus esialgse sisendi token'eid on poolitatud. (t3-olp sisendi spetsiifiline)
	 */
	public boolean hasTokenBreakPositions(){
		if (this.fraas != null){
			for (AjavtSona sona : this.fraas) {
				if (sona.isAtTokenBreakPosition()){
					return true;
				}
			}
		}
		return false;
	}
	
	/** Debug only. */
	@SuppressWarnings("unused")
	private String tagastaFraas(){
		StringBuilder sb = new StringBuilder();
		if (this.fraas != null){
			for (AjavtSona sona : this.fraas) {
				sb.append(sona.getAlgSona()+" ");
			}
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		String s = "timex";
		if (this.aste == ASTE.YHENDATUD_FRAASINA){
			s = "fraas";
		} else if (this.aste == ASTE.YHENDATUD_VAHEMIKUNA){
			s = "vahemik";
		}
		return s;
	}
	
	//==============================================================================
	//   	C o m p a r e   b y    g r a n u l a r i t y 
	//==============================================================================
	
	/**
	 * 	 Sorteerimise loogika:
	 *   <ul>
	 *     <li> Esiteks sorteerime ankurdamise alusel: k6ige enne kandidaadid, mille ankur on leitud;
	 *     <li> Teisena sorteerime granulaarsuse alusel: suurema granulaarsusega AjavaljendiKandidaadid (YEAR, MONTH, ... ) tulevad k6ige ees;
	 *   </ul>
	 */
	public int compareTo(AjavaljendiKandidaat o) {
		// 0) Sorteerime ankurdamise alusel		
		if (this.ankurdatudKandidaat != null){
			if (o.ankurdatudKandidaat == null){
				return -1;
			}
		} else if (o.ankurdatudKandidaat != null){
			if (this.ankurdatudKandidaat == null){
				return 1;
			}			
		}
		int thisLowestAndHighest []  = this.getLowestAndHighestGranularityCroaseRank();
		int otherLowestAndHighest [] = o.getLowestAndHighestGranularityCroaseRank();
		// 1) Lihtsad juhud: granulaarsused ei kattu yhelgi tasandil
		if (thisLowestAndHighest[Granulaarsus.LOWEST] > otherLowestAndHighest[Granulaarsus.HIGHEST]){
			return -1;
		} else if (thisLowestAndHighest[Granulaarsus.HIGHEST] < otherLowestAndHighest[Granulaarsus.LOWEST]){
			return 1;
		} 
		//
		//  VARASEM LOOGIKA: kui granulaarsused kattusid, otsustasime l6pliku tulemuse
		//  "lowest" v2ljade alusel: eespool oli see, kelle "lowest" on suurem...
		//
		//  NYYD: Kattuvate granulaarsuste puhul loeme kandidaadid v6rdseiks;
		//
		return 0;
	}

	/**
	 *    Kontrollib, kas kaesolev kandidaat saab ennast ankurdada etteantud kandidaadi kylge. Sisuliselt
	 *   t2hendab ankurdamine seda, et ajav2ljendi semantika leidmisel v6etakse aluseks etteantud 
	 *   kandidaadi semantika lahendus; Lisatingimusena: ankruks ei lubata v6tta DURATION-tyypi 
	 *   ajav2ljendeid ning RECURRENCE-tyypi ajav2ljendeid;
	 *   <p>
	 *    Lipp <tt>lubaAnkurdadaSamasFraasis</tt> m22rab, kas ankrukandidaat v6ib kuuluda samasse fraasi
	 *   (omada sama ylemkandidaati fraasipuus) kui k2esolev kandidaat.
	 *    <p>
	 *    NB! Praegu kontrollib ainult yhe/esimese ankurdamisOperatsiooni tingimusi - seega eeldab,
	 *    et neid pole rohkem kui 1.
	 */
	public boolean sobibAnkruKandidaadiks(AjavaljendiKandidaat ankruKandidaat, int suund, boolean lubaAnkurdadaSamasFraasis){
		if (this.semantikaEhitusklotsid != null){
			//
			//  0) Kontrollime, ega ylemkandidaadid pole samad 
			//                      n i n g 
			//     tegu on kindlalt eraldiseisva ajaväljendiga
			//
			if (!lubaAnkurdadaSamasFraasis){
				if (this.ylemKandidaat != null && ankruKandidaat.ylemKandidaat != null){
					if ((this.ylemKandidaat).equals(ankruKandidaat.ylemKandidaat)){
						return false;
					}
				}
			}
			if (ankruKandidaat.poleEraldiseisevAjavaljend){
				return false;
			}
			for (SemantikaDefinitsioon semDef : this.semantikaEhitusklotsid) {
				if (semDef.onAnkurdamisOperatsioon()){
					//
					// 1) Kontrollime konteksti (granulaarsuseid)
					//
					if (semDef.getGranulaarsusStr() != null){
						boolean positiveGranularityFound = false;
						AjaObjekt.TYYP ajavTyyp = TYYP.UNK;
						boolean sisaldabEksplitsiitsetIntervalli = false;
						//
						//  1.1) Parsime semantikadefinitsioonist kontekstin6uded
						//     (n6utud ja keelatud granulaarsused)
						//
						List<String> n6utudPosGranulaarsused     = new LinkedList<String>();
						List<String> soovimatudNegGranulaarsused = new LinkedList<String>();
						SemDefValjadeParsija.parsiN6utudJaKeelatudTunnused(null, 
																		   semDef.getGranulaarsusStr(), 
																		   n6utudPosGranulaarsused, 
																		   soovimatudNegGranulaarsused);
						
						//
						//  1.2) Kontrollime etteantud ajav2ljendi vastavust nendele n6uetele
						//
						List<SemantikaDefinitsioon>	ankruSemDefid = 
										ankruKandidaat.semantikaEhitusklotsid;
						for (SemantikaDefinitsioon ankruSemDef : ankruSemDefid) {
							if (ankruSemDef.onAnkurdamisOperatsioon()){
								// ankurdamisoperatsioone ei arvesta
								continue;
							}
							// jatame meelde ajav2ljendi tyybi
							if (ankruSemDef.getOp() != null){
								if (ankruSemDef.onSET_attrib("type") && ankruSemDef.getSemValue() != null){
									ajavTyyp = AjaObjekt.TYYP.parsiTyyp( ankruSemDef.getSemValue() );
								}
								if ((ankruSemDef.getOp()).equals(SemantikaDefinitsioon.OP.CREATE_beginPoint.toString()) && ankruSemDef.isExplicitPoint()){
									// Eksplitsiitse intervalli korral sobib ankruks h2sti intervalli otspunkt
									sisaldabEksplitsiitsetIntervalli = true;
								}								
							}
							if (ankruSemDef.getGranulaarsus() != null){
								String ankruGran = (ankruSemDef.getGranulaarsus()).toString();
								// Kontrollime, et poleks yhtki negatiivset granulaarsust
								if (soovimatudNegGranulaarsused != null){
									for (String granStr : soovimatudNegGranulaarsused) {
										if (granStr.equals( ankruGran )){
											return false;
										}
									}
								}
								// Kontrollime, kas on m6ni positiivne granulaarsus
								if (n6utudPosGranulaarsused != null){
									for (String granStr : n6utudPosGranulaarsused) {
										if (granStr.equals( ankruGran )){
											positiveGranularityFound = true;
										}
									}												
								}
							} // if
						} // for
						//
						//  1.3) Erandjuht: n6uetes on k6ik granulaarsused olid lubatud
						//						
						if ((Granulaarsus.ALL_GRANULARITIES).equals(semDef.getGranulaarsusStr())){
							positiveGranularityFound = true;
						}
						return (positiveGranularityFound  &&  !ajavTyyp.equals(AjaObjekt.TYYP.RECURRENCE) && 
								(!ajavTyyp.equals(AjaObjekt.TYYP.DURATION) || sisaldabEksplitsiitsetIntervalli));
					}	
				}
			}			
		}
		return false;
	}
	
}
