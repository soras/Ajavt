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

package ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus;

import java.util.List;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.AjavtSona.GRAMMATILINE_AEG;
import ee.ut.soras.ajavtV2.mudel.FraasisPaiknemiseKoht;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat.ASTE;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;
import ee.ut.soras.ajavtV2.util.SemDefValjadeParsija;

/**
 *  <p>
 *  Ajav&auml;ljendite semantika leidmise mudel #3. Lahendamine sisaldab järgmisi samme:
 *  <ul>
 *  	<li> Ajaväljendite ankurdamine.
 *          <ul>
 *            <li> Seob iga ajaväljendiga lähima verbi lause piirest;</li> 
 *             <li> Kui ajaväljend nõuab ankurdamist (sisaldab vastavat operatsiooni),
 *                   otsib sobiva ankrukandidaadi, aga mitte kaugemalt, kui 3 lause
 *                  raadiusest alates ankurdatavast;</li>
 *          </ul>
 *      </li>
 *  	<li> Ajaväljendite semantika lahendamine/normaliseerimine.
 *          <ul>
 *             <li> Lahendab ankurdamist mittevajavad kandidaadid; </li>
 *             <li> Lahendab ankurdamist vajavad kandidaadid; </li> 
 *             <li> (vajadusel) Teostab kandidaatide poolitamised ja lisab implitsiitsed lingid; </li>
 *          </ul>
 *      </li>
 *  </ul>
 *  <p>
 *  REMARK: Ebat2pne ankurdamise-defineerimine v6ib tuua sisse probleeme semantikadefinitsioonide rakendamisel. 
 *  Seet6ttu v6iks t2helepanu p88rata j2rgnevale:
 *  </p>
 *  <ul>
 *     <li> Yhes fraasis k6rvutipaikneda v6ivad jupid ei tohiks yks-teise-v6idu hakata ankurduma. 
 *          6ige Kitsendus "seotudKontekst" osas peaks selle 2ra hoidma - st ankurduma peaks vaid yks. 
 *     <li> Kui yks fraasi liikmetest tahab ankurdamist, lykatakse lahendi leidmine edasi, kuni ankru
 *          on leitud. Kuna siin on oht, et yks fraasiosa ankurdub teise kylge, on ankru leidmisel
 *          kitsendus: ankrut ei otsita ankurdatavaga samast fraasist.
 *     <li> Enne fraasi semantika leidmist sorteeritakse fraasi liikmed k6igepealt ankurdamise alusel
 *          (st, k6ige enne liikmed, millel on ankru) ning alles seej2rel granulaarsuste alusel (aasta,
 *          kuu, n2dal, p2ev, ... );
 *  </ul>
 *  <p>
 *     K&auml;esolev mudel peaks toetama intervallide lõhkumist v&auml;iksemateks juppideks, 
 *     nagu seda nõuab TimeML standard;
 *  </p>
 *  
 *  @author Siim Orasmaa
 */
public class SemLeidmiseMudelImpl3 implements SemLeidmiseMudel {
	private final String [] mudeliTahised = {"2" , "2.1", "3"};
	
	public String[] getMudeliTahised() {
		return mudeliTahised;
	}

	public void leiaSemantika(List<AjavtSona> sonad, String [] konehetk) {
		ankurdaAjavaljendid(sonad, konehetk);
		lahendaAjavaljenditeSemantika(sonad, konehetk);
	}

	//==============================================================================
	//   	A n k u r d a m i n e
	//==============================================================================

	/**
	 *   Ankurdab tekstist leidunud ajavaljendikandidaadid.
	 *   <p>
	 *   <ul>
	 *   	<li> Ajav2ljendiga seotakse ajav2ljendile l2him verb;
	 *      <li> Kui ajav2ljend n6uab ankurdamist, seotakse sellega ajav2ljendile l2him teine ajav2ljend;
	 *   </ul>
	 */
	private void ankurdaAjavaljendid(List<AjavtSona> sonad, String [] konehetk){
		// 0) Ankurdame verbide & teiste ajav2ljenditega
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> ajavaljendiKandidaadid    = sona.getAjavaljendiKandidaadid();
				AjavtSona lahimVerb  = null;
				boolean verbiOtsitud = false;
				for (int j = 0; j < ajavaljendiKandidaatides.size(); j++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(j);
					if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.ALGUSES){
						AjavaljendiKandidaat ajavaljendiKandidaat = ajavaljendiKandidaadid.get(j);						
						// 0.1) Leiame l2hima verbi ja ankurdame
						if (!verbiOtsitud){
							lahimVerb = leiaLahimVerbLausepiirest(sonad, i);
							verbiOtsitud = true;
						}
						if (lahimVerb != null){
							(ajavaljendiKandidaat).setLahimVerb(lahimVerb);
						} 
						// 0.2) Kui vaja, otsime l2hima teise ajav2ljendi, kuhu kylge ankurdada
						if (ajavaljendiKandidaat.vajabAnkurdamist(mudeliTahised)){
							AjavaljendiKandidaat sobivAnkrukandidaat = 
								leiaSobivAnkrukandidaatArvestaLausepiire(sonad, i, ajavaljendiKandidaat, 3);
							ajavaljendiKandidaat.setAnkurdatudKandidaat(sobivAnkrukandidaat);
						}
					}
				}
			}
		}
	}
	
	
	/**
	 *     Leiab tekstist kandidaadile <i>ajavk</i> l&auml;hima kandidaadi, mille kylge saab <i>ajavk</i>
	 *    ankurdada. NB! L&auml;him kandidaat ei v6i omada selle kandidaadiga yhist ylemkandidaati fraasipuus.
	 *    <p>
	 *    NB! Sobivate ankrute leidmist piiratakse j2rgmiselt: esiteks ei otsita s6nast kunagi kaugemalt kui
	 *    etteantud <tt>wordRadius</tt> s6na ulatusest; <br>
	 *    Teiseks, kui ankurdamiseoperatsiooniks on {@link SemantikaDefinitsioon.OP#ANCHOR_TIMEX_IN_SENTENCE},
	 *    ei minda ankru otsimisel kaugemale lause l6ppu t2histavatest s6nadest;
	 */
	private AjavaljendiKandidaat leiaSobivAnkrukandidaat( List<AjavtSona> sonad, 
												          int i, 
												          AjavaljendiKandidaat ajavk,
												          int wordRadius){
		int minDist = Integer.MAX_VALUE;
		int suund   = parsiSuundKustTulebAnkruLeidaNingOtsinguKitsendus(ajavk);
		boolean jaaLausePiiridesse = (suund > 1);
		if (suund > 1){ suund -= 10; }
		
		AjavaljendiKandidaat kandidaatInMinDist       = null;
		//
		//   1) Otsime tekstis eelnevate kandidaatide seast
		//
		int j = i - 1;
		boolean lahimLeitud = false;
		if (suund == 0 || suund == -1){
			while (j > -1 && !lahimLeitud){
				AjavtSona sona = sonad.get(j);
				if (jaaLausePiiridesse && sona.onLauseLopp()){
					break;
				}
				if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
					List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
					List<AjavaljendiKandidaat> ajavaljendid = sona.getAjavaljendiKandidaadid();
					for (int k = 0; k < ajavaljendiKandidaatides.size(); k++) {
						FraasisPaiknemiseKoht koht     = ajavaljendiKandidaatides.get(k);
						if (koht == FraasisPaiknemiseKoht.AINUSSONA || 
								koht == FraasisPaiknemiseKoht.LOPUS){
							AjavaljendiKandidaat kandidaat = ajavaljendid.get(k);
							if (kandidaat.getAste() == ASTE.MUSTRI_POOLT_ERALDATUD && 
										ajavk.sobibAnkruKandidaadiks(kandidaat, -1, false)){
								kandidaatInMinDist = kandidaat;
								minDist = i - j;
								lahimLeitud = true;
								break;
							}
						}
					}
				}
				if (j + wordRadius < i){
					break;
				}
			    j--;
			}			
		}
		//
		//   2) Otsime tekstis jargnevate kandidaatide seast
		//
		j = i + (ajavk.getFraas()).size();
		lahimLeitud = false;
		if (suund == 0 || suund == 1){
			while (j < sonad.size() && !lahimLeitud){
				AjavtSona sona = sonad.get(j);
				if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
					List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
					List<AjavaljendiKandidaat> ajavaljendid = sona.getAjavaljendiKandidaadid();
					for (int k = 0; k < ajavaljendiKandidaatides.size(); k++) {
						FraasisPaiknemiseKoht koht     = ajavaljendiKandidaatides.get(k);
						if (koht == FraasisPaiknemiseKoht.AINUSSONA || 
								koht == FraasisPaiknemiseKoht.ALGUSES){
							AjavaljendiKandidaat kandidaat = ajavaljendid.get(k);
							if (kandidaat.getAste() == ASTE.MUSTRI_POOLT_ERALDATUD &&
											ajavk.sobibAnkruKandidaadiks(kandidaat, 1, false)){
								if ((j - i) < minDist){
									kandidaatInMinDist = kandidaat;
									minDist = j - i;
								}
								lahimLeitud = true;
								break;							
							}
						}
					}
				}
				if (j - wordRadius > i || (jaaLausePiiridesse && sona.onLauseLopp())){
					break;
				}			
			    j++;
			}			
		}
		return kandidaatInMinDist;
	}
	
	/**
	 *     Leiab tekstist kandidaadile <i>ajavk</i> l&auml;hima kandidaadi, mille kylge saab <i>ajavk</i>
	 *    ankurdada. NB! L&auml;him kandidaat ei v6i omada selle kandidaadiga yhist ylemkandidaati fraasipuus.
	 *    <p>
	 *    NB! Sobivate ankrute leidmist piiratakse j2rgmiselt: esiteks ei otsita s6nast kunagi kaugemalt kui
	 *    <tt>sentenceRadius</tt> j2rgnevast v6i eelnevast lausest; <br>
	 *    Teiseks, kui ankurdamiseoperatsiooniks on {@link SemantikaDefinitsioon.OP#ANCHOR_TIMEX_IN_SENTENCE},
	 *    ei minda ankru otsimisel kaugemale lause l6ppu t2histavatest s6nadest;
	 */
	private AjavaljendiKandidaat leiaSobivAnkrukandidaatArvestaLausepiire( List<AjavtSona> sonad, 
												                           int i, 
												                           AjavaljendiKandidaat ajavk,
												                           int sentenceRadius){
		int minDist = Integer.MAX_VALUE;
		int suund   = parsiSuundKustTulebAnkruLeidaNingOtsinguKitsendus(ajavk);
		boolean jaaLausePiiridesse = (suund > 1);
		if (suund > 1){ suund -= 10; }
		
		AjavaljendiKandidaat kandidaatInMinDist       = null;
		int sentencesPassed = 0;
		//
		//   1) Otsime tekstis eelnevate kandidaatide seast
		//
		int j = i - 1;
		boolean lahimLeitud = false;
		if (suund == 0 || suund == -1){
			while (j > -1 && !lahimLeitud){
				AjavtSona sona = sonad.get(j);
				if (sona.onLauseLopp()){
					sentencesPassed++;
				}
				if (sentenceRadius < sentencesPassed){
					break;
				}
				if (jaaLausePiiridesse && sona.onLauseLopp()){
					break;
				}
				if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
					List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
					List<AjavaljendiKandidaat> ajavaljendid = sona.getAjavaljendiKandidaadid();
					for (int k = 0; k < ajavaljendiKandidaatides.size(); k++) {
						FraasisPaiknemiseKoht koht     = ajavaljendiKandidaatides.get(k);
						if (koht == FraasisPaiknemiseKoht.AINUSSONA || 
								koht == FraasisPaiknemiseKoht.LOPUS){
							AjavaljendiKandidaat kandidaat = ajavaljendid.get(k);
							if (kandidaat.getAste() == ASTE.MUSTRI_POOLT_ERALDATUD && 
										ajavk.sobibAnkruKandidaadiks(kandidaat, -1, false)){
								kandidaatInMinDist = kandidaat;
								minDist = i - j;
								lahimLeitud = true;
								break;
							}
						}
					}
				}
			    j--;
			}			
		}
		//
		//   2) Otsime tekstis jargnevate kandidaatide seast
		//
		j = i + (ajavk.getFraas()).size();
		lahimLeitud = false;
		sentencesPassed = 0;
		if (suund == 0 || suund == 1){
			while (j < sonad.size() && !lahimLeitud){
				AjavtSona sona = sonad.get(j);
				if (sona.onLauseLopp()){
					sentencesPassed++;
				}
				if (sentenceRadius < sentencesPassed){
					break;
				}
				if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
					List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
					List<AjavaljendiKandidaat> ajavaljendid = sona.getAjavaljendiKandidaadid();
					for (int k = 0; k < ajavaljendiKandidaatides.size(); k++) {
						FraasisPaiknemiseKoht koht     = ajavaljendiKandidaatides.get(k);
						if (koht == FraasisPaiknemiseKoht.AINUSSONA || 
								koht == FraasisPaiknemiseKoht.ALGUSES){
							AjavaljendiKandidaat kandidaat = ajavaljendid.get(k);
							if (kandidaat.getAste() == ASTE.MUSTRI_POOLT_ERALDATUD &&
											ajavk.sobibAnkruKandidaadiks(kandidaat, 1, false)){
								if ((j - i) < minDist){
									kandidaatInMinDist = kandidaat;
									minDist = j - i;
								}
								lahimLeitud = true;
								break;							
							}
						}
					}
				}
				if (jaaLausePiiridesse && sona.onLauseLopp()){
					break;
				}			
			    j++;
			}			
		}
		return kandidaatInMinDist;
	}
	
	//==============================================================================
	//   	S e m a n t i k a   l a h e n d a m i n e 
	//==============================================================================
	
	/**
	 *     Lahendab ajav&auml;ljendite semantika.
	 *     Algoritm: 
	 *     <ol>
	 *      <li>Lahendame kandidaadid, mille semantikadefinitsioon ankurdamist ei n6ua;
	 *       <p>
	 *       <i>Leiame ylemkandidaadi ning sooritame tipust alla kogu vahemiku/fraasi jms lahendamise.</i>
	 *      </li>
	 *      <li>Lahendame kandidaadid, mis n6udsid ankurdamist;
	 *       <p>
	 *       <i>V6tame aluseks ankruks oleva kandidaadi semantika lahenduse.</i>
	 *       <p>
	 *       <i>Sooritame esimese punktiga analoogse tipust alla lahendamise.</i>
	 *       <li>Avame lahendatud ajaväljendite ülemised granulaarsused, teostame vahemike poolitamised
	 *           eraldiseisvateks kandidaatideks ning lisame vajadusel implitsiitsed vahemiku otspunktid;
	 *      </li>
	 *     </ol>
	 *     <p>
	 */
	private void lahendaAjavaljenditeSemantika( List<AjavtSona> sonad, String [] konehetk ){
		//  1) Lahendame kandidaadid, mis ei vaja ankurdamist
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> ajavaljendiKandidaadid    = sona.getAjavaljendiKandidaadid();
				//  Valime lahendamiseks v2lja ainult yhe kandidaadi. Esialgu huvitavad meid ainult:
				// pelgalt mustri poolt eraldatud valjendid v6i fraasiks yhendatud valjendid
				for (int j = 0; j < ajavaljendiKandidaatides.size(); j++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(j);
					// Meid huvitavad ainult lahendamata valjendid: st sellised, mis antud s6nast algavad ...
					// Oluline kriteerium: kandidaat ei tohi vajada ankurdamist ...
					if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.ALGUSES){
						AjavaljendiKandidaat kandidaat = ajavaljendiKandidaadid.get(j);
						if (kandidaat.getAste() == ASTE.MUSTRI_POOLT_ERALDATUD && 
								!kandidaat.isSemLahendamineLabiviidud() &&
								 	!kandidaat.vajabAnkurdamistRek(mudeliTahised)){
							// Lahendame terve ajavaljendikandidaatide puu, alates leitud lehest
							AjavaljendiKandidaat k6rgeimYlem = kandidaat.leiaK6igeK6rgemYlemkandidaat();
							k6rgeimYlem.lahendaSemantika( null, konehetk, mudeliTahised );
						}
					}
				}
			}
		}
		//  2) Lahendame kandidaadid, mille lahendus on s6ltub teiste kandidaatide lahendustest
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> ajavaljendiKandidaadid    = sona.getAjavaljendiKandidaadid();
				//  Valime lahendamiseks v2lja ainult yhe kandidaadi. Esialgu huvitavad meid ainult:
				// pelgalt mustri poolt eraldatud valjendid v6i fraasiks yhendatud valjendid
				for (int j = 0; j < ajavaljendiKandidaatides.size(); j++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(j);
					// Meid huvitavad ainult lahendamata valjendid: st sellised, mis antud s6nast algavad ...
					// Vaatame kandidaate, mille lahendamist ennist l2bi ei viidud (ankru-v22rtus oli puudu)
					if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.ALGUSES){
						AjavaljendiKandidaat kandidaat = ajavaljendiKandidaadid.get(j);
						if (kandidaat.getAste() == ASTE.MUSTRI_POOLT_ERALDATUD && 
								!kandidaat.isSemLahendamineLabiviidud()){
							// Lahendame terve ajavaljendikandidaatide puu, alates leitud lehest
							AjavaljendiKandidaat k6rgeimYlem = kandidaat.leiaK6igeK6rgemYlemkandidaat();
							k6rgeimYlem.lahendaSemantika( null, konehetk, mudeliTahised );
						}
					}
				}
			}
		}
		// 3) Teostame poolitamised ja lisame implitsiitsed lingid
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> ajavaljendiKandidaadid    = sona.getAjavaljendiKandidaadid();
				//  Valime lahendamiseks v2lja ainult yhe kandidaadi. Esialgu huvitavad meid ainult:
				// pelgalt mustri poolt eraldatud valjendid v6i fraasiks yhendatud valjendid
				for (int j = 0; j < ajavaljendiKandidaatides.size(); j++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(j);
					// Meid huvitavad ainult lahendamata valjendid: st sellised, mis antud s6nast algavad ...
					if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.ALGUSES){
						AjavaljendiKandidaat kandidaat = ajavaljendiKandidaadid.get(j);
						if (kandidaat.getSemantikaLahendus() != null){
							kandidaat.teostaPoolitamisedKuiVaja();
						}
						if (kandidaat.getAste() == ASTE.YHENDATUD_VAHEMIKUNA){
							// Lisame puuduolevad, implitsiitsed ajaväljendid (nt otspunktide yhendamine vahemikuks
							// v6i vahemikule otspunktide lisamine);
							kandidaat.lisaImplitsiitsedLingid();
						}
					}
				}
			}
		}
	}
	
	/**
	 *   Parsib ankurdatavalt ajav2ljendikandidaadilt suuna, millelt tuleb tekstis ajav2ljendit otsida, ning
	 *  otsimise kitsenduse (kas tuleb j22da lause piiridesse v6i mitte). 
	 *  <p>
	 *  Kui lausepiiridesse j22mise n6uet pole, on suunad on t2histatud j2rgmiselt: -1 == tekstis enne v2ljendit, 
	 *  1 == tekstis v2ljendi j2rel, 0 == otsida m6lemalt poolt;    
	 *  <p>
	 *  Lausepiiridesse j22mise n6ude korral on suunanumber t2pselt 10 v6rra suurem: 9 == tekstis enne v2ljendit,
	 *  11 == tekstis v2ljendi j2rel, 10 == otsida m6lemalt poolt;
	 */
	private int parsiSuundKustTulebAnkruLeidaNingOtsinguKitsendus(AjavaljendiKandidaat ajavk){
		int addToReturnValue = 0;
		for (SemantikaDefinitsioon semDef : ajavk.getSemantikaEhitusklotsid()) {
			if (semDef.onAnkurdamisOperatsioon()){
				//
				// 1) Kontrollime, kas ankurdamisel tuleb jaada lause piiridesse;
				// 				
				if ((semDef.getOp()).equals((SemantikaDefinitsioon.OP.ANCHOR_TIMEX_IN_SENTENCE).toString())){
					addToReturnValue = 10;
				}				
				//
				// 2) Kontrollime suunda (-1 v6i 9 == tekstis eespool ning 1 v6i 11 == tekstis tagapool);
				// 
				if (semDef.getDirection() != null){
					int dir = SemDefValjadeParsija.parseSeekDirection( semDef.getDirection(), null );
					if (dir != Integer.MIN_VALUE){
						return addToReturnValue + dir;
					}
				}
				addToReturnValue = 0;
			}
		}
		return 0;
	}	

	//==============================================================================
	//    	V e r b i g a    s i d u m i n e
	//==============================================================================
	
	/**
	 *   Naiivne meetod ajav&auml;ljendikandidaadile l&auml;hima verbi leidmiseks.
	 *   <p>
	 *   L&auml;himat verbi otsitakse ainult lausepiirest ning arvestatakse vaid
	 *   verbe, mille puhul on m&auml;&auml;ratud verbi grammatiline aeg. Kui lauses 
	 *   yhtegi verbi ei leidu, tagastatakse <tt>null</tt>.
	 */
	public static AjavtSona leiaLahimVerbLausepiirest(List<AjavtSona> sonad, int jooksevPositsioon){
		AjavtSona lahimVerb = null;
		int distants = -1;
		// --- Lahim verb enne sona		
		int j = jooksevPositsioon - 1;
		while (j >= 0){
			AjavtSona sona = sonad.get(j);
			if (sona.onVerb() && sona.getGrammatilineAeg() != GRAMMATILINE_AEG.MAARAMATA){
				distants = jooksevPositsioon - j;
				lahimVerb = sona;
				break;
			}				
			if (sona.onLauseLopp() || sona.onVoimalikTsiteeringuLoppVoiAlgus()){
				break;
			}
			j--;
		}
		// --- Jätame vahele positsioonid, millel paikneb mingi (ajaväljend)
		boolean viimaneOliVoimalikLauseLopp = false;
		while (jooksevPositsioon < sonad.size()){
			// Kui s6na jooksval positsioonil on lausel6pp
			AjavtSona jooksevSona = sonad.get(jooksevPositsioon);
			if (jooksevSona.onLauseLopp()){
				viimaneOliVoimalikLauseLopp = true;
				break;
			}
			if (!jooksevSona.onSeotudMoneAjavaljendiKandidaadiga()){
				jooksevPositsioon--;
				break;
			}
			jooksevPositsioon++;
		}
		if (viimaneOliVoimalikLauseLopp){
			// kui ajav2ljendiga koos l6ppes ka lause, ei tasu minna verbi teisest lausest otsima
			return lahimVerb;
		}
		// ---
		j = jooksevPositsioon + 1;
		while (j < sonad.size()){
			AjavtSona sona = sonad.get(j);
			if (sona.onVerb() && sona.getGrammatilineAeg() != GRAMMATILINE_AEG.MAARAMATA && 
					(distants == -1 || j - jooksevPositsioon < distants)){
				distants = j - jooksevPositsioon;
				lahimVerb = sona;
				break;
			}			
			if (sona.onLauseLopp() || sona.onVoimalikTsiteeringuLoppVoiAlgus()){
				break;
			}
			j++;			
		}
		return lahimVerb;
	}
	
}
