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

package ee.ut.soras.ajavtV2.mudel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;
import ee.ut.soras.ajavtV2.mudel.sonamallid.FraasiMuster;
import ee.ut.soras.ajavtV2.mudel.sonamallid.NegatiivneMuster;
import ee.ut.soras.ajavtV2.mudel.sonamallid.RegExpSonaMall;
import ee.ut.soras.ajavtV2.mudel.sonamallid.SonaMall;
import ee.ut.soras.ajavtV2.mudel.sonamallid.SonaMall.TYYP;
import ee.ut.soras.ajavtV2.util.ArvSonaFraasideTuvastaja;
import ee.ut.soras.ajavtV2.util.SemDefValjadeParsija;
import ee.ut.soras.ajavtV2.util.SemDefValjadeParsija.FORMAT_OF_VALUE;

/**
 *  Tuvastamisreegel. Koosneb laias laastus kahest kihist:
 *  <ul>
 *    <li> {@link FraasiMuster}, mille abil eraldatakse tekstist (esialgsed) 
 *      ajavaljendifraasid. 
 *    <li> {@link SemantikaDefinitsioon}-id, mis kirjeldavad samme, mis tuleb
 *      labi viia eraldatud ajavaljendi semantika leidmiseks. 
 *  </ul>
 *  Kahe kihi vahel voib olla {@link FraasiMustriFilter}, mille abil filtreeritakse
 * mingite (nt morfoloogiliste) tunnuste kontrollimise abil valja kandidaadid, mis 
 * ei sobi ajavaljenditeks.
 * <p>
 *  Ka fraasimustri elementide - sonamallide - kyljes voivad olla {@link SemantikaDefinitsioon}-id,
 * mis aga on lynklikud ning muutuvad rakendatavateks alles siis, kui panna nad kokku vastavate
 * tuvastamisreegli alla kuuluvate SemantikaDefinitsioonidega. 
 *  
 * @author Siim Orasmaa
 */
public class TuvastamisReegel {
	
	/**
	 *   Kasutatav semantika leidmise mudel.  
	 */
	private static String [] semanticsModel = null;
	
	/**
	 *   Sonamallide jarjend ehk fraasimuster. Selle jargi leitakse ajav2ljendeid tekstist yles.
	 */
	private FraasiMuster fraasiMuster;
	
	/**
	 *    Negatiivsed mustrid. Nende j2rgi kustutakse eraldatud ajav2ljendikandidaatide
	 *   seast sellised, mis tegelikult ajav2ljendikandidaatideks ei k6lba.  
	 */
	private List<NegatiivneMuster> negMustrid = null;	

	/**
	 *   Semantikad t&auml;psustavad fraasimustriFiltrid. Kannavad endas ka semantikadefinitsioone
	 *  ehk arvutusk&auml;ike antud fraasimustri erinevate alamfraaside semantika arvutamiseks.
	 */
	private List<FraasiMustriFilter> fraasiMustriFiltrid = null; 
	
	/**
	 *    Kas antud tuvastamisreegli alla kuulub m6ni mustritahis? Tagastab <tt>true</tt>, kui m6ne
	 *   selle tuvastamisreegli all oleva fraasimustrifiltriga on seotud mustritahis. 
	 */
	private boolean leidubMustriTahiseid = false;
	
	//==============================================================================
	//   	M u s t r i t     r a h u l d a n u d      f r a a s i o s a l e
	//   v a s t a v a t e    s e m a n t i k a d e f i n i t s i o o n i d e
	//      t 2 i e n d a m i n e
	//==============================================================================
	/**
	 *   T&auml;iendab listis <tt>semDefidSonaMallidest</tt> olevaid semantikadefinitsioone,
	 *  kasutades antud tuvastamisreegli alla kuuluvad semantikadefinitsioone, vastavalt sellele, 
	 *  millised alamosad mustrist on leidnud rahuldamist. Sisuliselt t&auml;hendab see, et 
	 *  t&auml;idetakse l&uuml;ngad (puuduolevad atribuudid/v&auml;ljad) <tt>semDefidSonaMallidest</tt>-e
	 *  seas, kasutades tuvastamisreegli semantikadefinitsioonides olemasolevaid atribuute/v&auml;lju.
	 *  <p>
	 *  Tagastatakse list uutest semantikadefinitsioonidest, mis on loodud t&auml;iendamise k&auml;igus -
	 *  yhtegi vana semantikadefinitsiooni see ei sisalda;
	 *  Tagastatakse tyhi list juhul, kui t&auml;iendamine ei 6nnestu (nt t&auml;iendamisel n6utud 
	 *  kontekstitingimused pole rahuldatud) v6i antud tuvastamisreegli alla ei kuulu yhtegi 
	 *  semantikadefinitsiooni. 
	 */
	public List<SemantikaDefinitsioon> taiendaSonaMallidestSaadudSemDeffe(
									HashMap<String, List<AjavtSona>> malliRahuldavadAlamFraasid, 
									HashMap<String, String> rahuldatudMustriosad,
									List<SemantikaDefinitsioon> semDefidSonaMallidest,
									HashMap<Integer, String> semDefidSonaMallidestIndeksMustris,
									List<SonaMall> mustriSonamallid){
		
		List<SemantikaDefinitsioon> uuedSemDefid = new LinkedList<SemantikaDefinitsioon>();
		if (this.fraasiMustriFiltrid != null && !this.fraasiMustriFiltrid.isEmpty()){
			for (FraasiMustriFilter mustriFilter : this.fraasiMustriFiltrid) {
				// 1) Leiame, kas leitud alamfraasid l2hevad l2bi toodud filtri ...
				List<SemantikaDefinitsioon> semDefid = 
					mustriFilter.rakendaFiltritJaTagastaSonaMallid(malliRahuldavadAlamFraasid, rahuldatudMustriosad);
				// 2) Kui l2ksid l2bi, viime l2bi semantikadefinitsioonide t2iendamise
				if (semDefid != null){
					uuedSemDefid.addAll(parsiRahuldatudMustriosaleVastavadSemDefid(
														semDefid, 
														malliRahuldavadAlamFraasid, 
														rahuldatudMustriosad, 
														semDefidSonaMallidest, 
														semDefidSonaMallidestIndeksMustris,
														mustriSonamallid));
				}
			}
		}
		return uuedSemDefid;
	}
	
	/**
	 *   Sisuliselt on selle meetodi peaeesm&auml;rk yhildada listid <tt>semDefidSonaMallidest</tt> 
	 *  ning <tt>tuvastamisReegiSemDefid</tt>, vastavalt sellele, milline muster on tegelikult
	 *  yles leitud (kajastub <tt>malliRahuldavadAlamFraasid</tt>, <tt>rahuldatudMustriosad</tt>).
	 *  Tagastatakse uus list yhildamise tulemusel saadud (l&otilde;plikest) semantikadefinitsioonidest.
	 *  <p>
	 *  Iga <tt>tuvastamisReegiSemDefid</tt> element tapsustab atribuudi <tt>seotudMustriOsa</tt> kaudu,
	 *  milline on muster, mille korral selles elemendis toodud definitsioon v6ib realiseeruda. Antud
	 *  meetodis yhildataksegi realiseerunud definitsioonid nende vastetega (sama s6naklassi alla kuuluvate
	 *  semantikadefinitsioonidega) listis <tt>semDefidSonaMallidest</tt>, kirjutades vajadusel viimases 
	 *  listis yle t2itmata/tyhjad atribuudid.
	 *  <p>
	 *  Kui ykski listi <tt>tuvastamisReegiSemDefid</tt> element ei realiseerunud, tagastatakse tyhilist;
	 *  <p>
	 *  Lisaks: kui tuvastamisreegli semantikadefinitsiooni semValue on viide mingile mustriga sobitunud 
	 *  s6ne alamosale, leiab viidatud alamosa, parsib sealt s6ne ja paneb uueks semValue v22rtuseks. 
	 */
	private List<SemantikaDefinitsioon> parsiRahuldatudMustriosaleVastavadSemDefid(
									List<SemantikaDefinitsioon> tuvastamisReegliSemDefid,
									HashMap<String, List<AjavtSona>> malliRahuldavadAlamFraasid, 
									HashMap<String, String> rahuldatudMustriosad,
									List<SemantikaDefinitsioon> semDefidSonaMallidest,
									HashMap<Integer, String> semDefidSonaMallidestIndeksMustris,
									List<SonaMall> mustriSonamallid){
		List<SemantikaDefinitsioon> uuedSemDefid = new ArrayList<SemantikaDefinitsioon>();
		// ===========================================================================
		//   Iga tuvastamisreegli alla kuuluva semantikadefinitsiooni korral 
		// ===========================================================================
		for (int i = 0; i < tuvastamisReegliSemDefid.size(); i++) {
			SemantikaDefinitsioon tuvastamisReegliSemDef = tuvastamisReegliSemDefid.get(i);
			if (tuvastamisReegliSemDef.vastabSemLeidmiseMudelile(TuvastamisReegel.semanticsModel)){
				if (tuvastamisReegliSemDef.getSeotudMustriOsa() != null){
					// ===========================================================================
					//   1) Kontrollime, kas ja millised semantikadefinitsiooniga seotud mustri
					//    osad on leidnud rahuldamist. Ehk - kas esmased tingimused definitsiooni
					//    kasutuselevõtuks on täidetud.
					// ===========================================================================
					List<String> rahuldatudAlamOsad = 
						tuvastamisReegliSemDef.leiaPositiivseltRahuldatudMustriosad(
								malliRahuldavadAlamFraasid, 
								rahuldatudMustriosad);
					if (rahuldatudAlamOsad != null && !rahuldatudAlamOsad.isEmpty()){
						// ===========================================================================
						//   2) Leiame rahuldatud alamosadega seotud semantikadefinitsioonid.
						// ===========================================================================
						for (String leitudAlamosa : rahuldatudAlamOsad) {
							//
							// A) Otsime rahuldatud alammustrile vastavat semantikadefinitsiooni
							//           - sonamallidest saadud definitsioonide hulgast
							// 
							boolean leidusSonaMallideHulgas = false;
							for (int j = 0; j < semDefidSonaMallidest.size(); j++) {
								SemantikaDefinitsioon semDefSonaMallist = semDefidSonaMallidest.get(j);
								if ((semDefSonaMallist.getSonaKlass()).equals( leitudAlamosa )){
									//
									// A.1)  Alamosa vastas t2pselt s6naklassi nimele;
									//
									leidusSonaMallideHulgas = true;
									SemantikaDefinitsioon uusDef = 
										semDefSonaMallist.taidaValjadVoiLooUusSemantikaDefinitsioon(tuvastamisReegliSemDef);
								    if (uusDef != null){
								    	uuedSemDefid.add(uusDef);
								    }
								} else if (semDefidSonaMallidestIndeksMustris.containsKey(Integer.valueOf(j))){
									//
									// A.2)  Alamosa t2pselt s6naklassi nimele ei vastanud; 
									//       Kontrollime siis, kas s6naklassinime indeks sufiksiga
									//       laiendades saame vastavuse;
									//       (st - need juhud, kus mustris on mitu sama nimega klassi)
									//
									String klassiNimiLaiendusega =
										semDefSonaMallist.getSonaKlass() + "_" + 
										semDefidSonaMallidestIndeksMustris.get(Integer.valueOf(j));
									if ((klassiNimiLaiendusega).equals(leitudAlamosa)){
										leidusSonaMallideHulgas = true;
										SemantikaDefinitsioon uusDef = 
											semDefSonaMallist.taidaValjadVoiLooUusSemantikaDefinitsioon(tuvastamisReegliSemDef);
									    if (uusDef != null){
									    	uuedSemDefid.add(uusDef);
									    }										
									}
								}
							}
							//
							// B) Kui s6namallide hulgast alamosa ei leidunud, loome kloonimise teel täiesti 
							//    uue semantikadefinitsiooni;
							//
							//    Lisaks: kui semVal osa viitab mingile mustriosale, v6tame sealt v22rtuse
							//
							if (!leidusSonaMallideHulgas){
								// TODO: Kas tuleks kontrollida enne uue loomist, kas on t2isv22rtuslik, st
								// on olemas "op", "semField", "semValue" jms. ?
								SemantikaDefinitsioon clone = tuvastamisReegliSemDef.clone();
								uuedSemDefid.add(clone);
								//
								//  B.2)  Kui semValue on viide, parsime 6ige v22rtuse ja asendame
								//
								if (clone.getSemValue() != null){
									FORMAT_OF_VALUE formatOfValue = 
										SemDefValjadeParsija.detectFormatOfValue( clone.getSemValue() );
									if (formatOfValue == FORMAT_OF_VALUE.PARSE_FROM_SELF || 
											formatOfValue == FORMAT_OF_VALUE.PARSE_FROM_NUMERAL){
										// Parsime kas otse regulaaravaldisest v6i regulaaravaldise poolt
										// leitud arvs6nast ...
										String refferedNumber = (clone.getSemValue()).split(":")[1];
										try {
											int indeks [] = { Integer.parseInt(refferedNumber) };
											//
											// Eeldame siin nyyd, et:
											//  * leitudAlamosa on numbriline indeks, viitab vastavale sonamallile
											//      * saab kasutada malliRahuldavadAlamFraasid-ist p2rimiseks
											//      * saab kasutada mustriSonamallides indeksina...
											//  Seega saame rahuldatud mustri alamosast parsida v2lja otsitud viidatava
											//  v22rtuse
											if (malliRahuldavadAlamFraasid.containsKey(leitudAlamosa)){
												int mustriIndeks = Integer.parseInt(leitudAlamosa);
												if (mustriIndeks > -1 && mustriIndeks < mustriSonamallid.size()){
													SonaMall sonaMall = mustriSonamallid.get(mustriIndeks);
													if (sonaMall.getTyyp()==TYYP.REGEXP){
														RegExpSonaMall regExp = (RegExpSonaMall) sonaMall;
														List<String> sobitunudAlamgrupid = 
																regExp.parsiSobitunudAlamgrupid(
																   (malliRahuldavadAlamFraasid.get(leitudAlamosa)).get(0),
																   indeks
																);
														if (!sobitunudAlamgrupid.isEmpty()){
															if (formatOfValue == FORMAT_OF_VALUE.PARSE_FROM_SELF){
																// a) Kirjutame leitud v22rtuse otse semValue kohale
																clone.setSemValue(sobitunudAlamgrupid.get(0));																
															} else {
																// b) Parsime leitud (arvs6na) numbrilise v22rtuse 
																// ja paneme semvalue kohale
																ArvSonaFraasideTuvastaja arvuTuvastaja = new ArvSonaFraasideTuvastaja( null );
																// TODO: See, et peame siin praktiliselt k6ik arvs6nade-regulaaravaldised uuesti 
																// kompileerima, ei ole efektiivsuse seisukohast mitte hea asi ...																
																Object arvuna = arvuTuvastaja.leiaArvSonaArvulineVaartus( 
																		(sobitunudAlamgrupid.get(0)).toLowerCase() 
																);
																if (arvuna != null){
																	if (arvuna instanceof Integer){
																		clone.setSemValue( ((Integer)arvuna).toString() );
																	} else if (arvuna instanceof Double){
																		clone.setSemValue( 
																				(((Double)arvuna).toString()).replace(".", ",") 
																		);
																	}
																}
															}
														}
													}
												}
											}
										} catch (NumberFormatException e) {
										}
									}
								}
								
							}
						}
					}
					
				}
			}
		}		
		return uuedSemDefid;
	}
	
	//==============================================================================
	//    	P a r s i m e   t u v a s t a m i s r e e g l i g a    s e o t u d   
	//      m u s t r i t a h i s e d
	//==============================================================================	

	/**
	 *   Parsib antud tuvastamisreegli kyljest mustritahised ja tagastab need.
	 */
	public List<MustriTahis> parsiRahuldatudMustriosaleVastavadMustriTahised(
								HashMap<String, List<AjavtSona>> malliRahuldavadAlamFraasid, 
								HashMap<String, String> rahuldatudMustriosad){
		List<MustriTahis> mustriTahised = new ArrayList<MustriTahis>();
		if (this.fraasiMustriFiltrid != null && !this.fraasiMustriFiltrid.isEmpty()){
			for (int i = 0; i < (this.fraasiMustriFiltrid).size(); i++) {
				FraasiMustriFilter filter = fraasiMustriFiltrid.get(i);
				if (filter.leidubMustriTahis()){
					List<MustriTahis> tahised = 
						filter.rakendaFiltritJaTagastaMustriTahised(malliRahuldavadAlamFraasid, rahuldatudMustriosad);
					if (tahised != null){
						mustriTahised.addAll(tahised);
					}
				}
			}			
		}
		return mustriTahised;
	}
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================
	
	/**
	 *  Leiab, kas antud tuvastamisreegliga on seotud m6ni mustritahis?
	 */
	public boolean kasLeidubMustriTahiseid(){
		return this.leidubMustriTahiseid;
	}
	
	public FraasiMuster getFraasiMuster() {
		return fraasiMuster;
	}

	public void setFraasiMuster(FraasiMuster fm) {
		// Loome kahepoolse seose tuvastamisreegli ja fraasimustri vahele ...
		fm.setTuvastamisReegel(this);
		this.fraasiMuster = fm;
	}
	
	public void lisaFraasiMustriFilter(FraasiMustriFilter mustriFilter){
		if (this.fraasiMustriFiltrid == null){
			this.fraasiMustriFiltrid = new LinkedList<FraasiMustriFilter>();
		}
		if (mustriFilter.leidubMustriTahis()){
			this.leidubMustriTahiseid = true;
		}
		this.fraasiMustriFiltrid.add(mustriFilter);
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append(this.fraasiMuster);
		return sb.toString();
	}
	
	public static void setSemanticsModel(String [] newModel){
		semanticsModel = newModel;
	}
	
	public static String [] getSemanticsModel(){
		return semanticsModel;
	}

	public List<NegatiivneMuster> getNegMustrid() {
		return negMustrid;
	}

	public void setNegMustrid(List<NegatiivneMuster> negMustrid) {
		this.negMustrid = negMustrid;
	}
	
}
