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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import ee.ut.soras.ajavtV2.AjaTuvastaja;
import ee.ut.soras.ajavtV2.mudel.FraasiMustriFilter;
import ee.ut.soras.ajavtV2.mudel.MustriTahis;
import ee.ut.soras.ajavtV2.mudel.TuvastamisReegel;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat.ASTE;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.Granulaarsus;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.LiitumisReegel;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;
import ee.ut.soras.ajavtV2.mudel.sonamallid.AlgVormSonaMall;
import ee.ut.soras.ajavtV2.mudel.sonamallid.ArvuEriKujudSonaMall;
import ee.ut.soras.ajavtV2.mudel.sonamallid.FraasiMuster;
import ee.ut.soras.ajavtV2.mudel.sonamallid.FraasiMusterFSM;
import ee.ut.soras.ajavtV2.mudel.sonamallid.NegatiivneMuster;
import ee.ut.soras.ajavtV2.mudel.sonamallid.RegExpSonaMall;
import ee.ut.soras.ajavtV2.mudel.sonamallid.SonaKlass;
import ee.ut.soras.ajavtV2.mudel.sonamallid.SonaMall;
import ee.ut.soras.ajavtV2.mudel.sonamallid.TavaTekstSonaMall;

/**
 *  Failiparser, võimaldab XML failist meile vajalikud tuvastamisreeglid
 *  k&auml;tte saada.
 * 
 * @author Siim Orasmaa
 */
public class MustridXMLFailist {

	//==============================================================================
	//    K a s u t a t a v a d    t a g - i d     
	//==============================================================================
	
	final public static String TAG_SONAKLASS       	 	= "SonaKlass";	
	final public static String TAG_SONAKLASS_NIMI  	 	= "nimi";	
	final public static String TAG_ELEMENT       	 	= "Element";
	final public static String TAG_ELEMENT_TYYP       	= "tyyp";
	final public static String TAG_ELEMENT_TYYP_ALGVORM	= "algv";
	final public static String TAG_ELEMENT_TYYP_REGEXP	= "reg";
	final public static String TAG_ELEMENT_TYYP_ARVSONA	= "arvSona";
	final public static String TAG_ELEMENT_TYYP_ERI_ARV	= "eriArv";
	final public static String TAG_ELEMENT_TYYP_TEKST	= "tekst";
	final public static String TAG_ELEMENT_VAARTUS  	= "vaartus";
	final public static String TAG_ELEMENT_NUMPIIRANG 	= "arvuPiirang";
	final public static String TAG_ELEMENT_ARVULIIK 	= "arvuLiik";

	final public static String TAG_REEGEL        	 = "Reegel";
	final public static String TAG_MUSTER        	 = "Muster";
	final public static String TAG_MUSTER_VBSNJRG  	 = "vabaSonajarg";
	
	final public static String TAG_FILTER        	 = "Filter";
	final public static String TAG_MORF_TUNNUSED   	 = "morfTunnused";
	final public static String TAG_SEOTUD_MUSTRIOSA  = "seotudMustriosa";	
	
	final public static String TAG_SEMDEF        	 = "SemReegel";
	final public static String TAG_PRIORITEET        = "priority";	
	final public static String TAG_SEOTUD_KONTEKST   = "seotudKontekst";
	final public static String TAG_ATTRIB 		     = "attrib";	
	final public static String TAG_SEMFIELD 		 = "semField";
	final public static String TAG_OP             	 = "op";
	final public static String TAG_SEMLABEL 		 = "semLabel";	
	final public static String TAG_SEMVALUE 		 = "semValue";
	final public static String TAG_SEMVAL_EBATAPNE	 = "semValOnEbatapne";
	final public static String TAG_EXPLICIT_POINT	 = "explicitPoint";
	
	final public static String TAG_DIRECTION	 	 = "direction";
	final public static String TAG_MUDEL           	 = "mudel";

	final public static String TAG_MUSTRITAHIS     	 = "MustriTahis";
	final public static String TAG_MUSTRITAHISED   	 = "tahised";
	final public static String TAG_MUSTRITAHIS_PESA  = "poleEraldiSeisevAjav";
	
	final public static String TAG_NEGMUSTER       	   = "NegMuster";
	final public static String TAG_NEGMUSTER_STARTPOS  = "startPos";
	final public static String TAG_NEGMUSTER_PRESPUNCT = "preservePunct";

	final public static String TAG_LIITUMISREEGEL  	      = "LiitumisReegel";
	final public static String TAG_LIITUMISREEGEL_ASTE    = "tase";	
	final public static String TAG_LIITUMISREEGEL_FJRK    = "fikseeritudJarjekord";
	final public static String TAG_LIITUMISREEGEL_K6RVUTI = "tapseltKorvuti";
	
	/**
	 *  XML faili p&otilde;hjal loodav <i>Document Object Model</i>.
	 */
	private Document dom;
	
	/**
	 *  XML-dokumendist parsitud s&otilde;naklassid. Leiavad rakendust meetodis ...   
	 */
	private HashMap<String, SonaKlass> sonaKlassid;
	
	//==============================================================================
	//    M u s t e r - t a g ' i   a l a m o s i   kirjeldavad regulaaravaldised
	//==============================================================================
	
	private Pattern musterSonaKlass  = Pattern.compile("^!?([A-Z0-9_]+)\\??$");
	private Pattern musterRegexp     = Pattern.compile("^!?/([^/]+)/\\??$");
	private Pattern musterAlgvorm    = Pattern.compile("^!?\\|(.+)\\|\\??$");
	private Pattern musterValikuline = Pattern.compile("^(.+)\\?$");
	private Pattern musterArajaetav  = Pattern.compile("^!(.+)$");
	

	//==============================================================================
	//    F a i l i s t  /  s i s e n d v o o s t   p a r s i m i n e
	//==============================================================================
	
	/**
	 *  V&otilde;tab etteantud nimega XML failist fraasimustrid, tagastab fraasimustrite
	 * nimestiku. Kui faili avamisel v&otilde;i parsimisel peaks esinema vigu, kirjutatakse
	 * veateated logisse <code>logi</code>.
	 * 
	 * @param failiNimi XML faili nimi
	 * @param logi logi, kuhu parsimisvigadest teada anda
	 * @return XML-failist parsitud fraasimustrid
	 */
	public void votaMustridXMLFailist(String failiNimi, AjaTuvastaja tuvastaja) throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		// parsime faili builder-i abil, et saada 
		// XML-failist DOM esitust 
		dom = db.parse(failiNimi);
		// 1) Parsime dokumendist s6naklassid, riputame kylge endale ja tuvastajale
		this.sonaKlassid = parsiDokumendistSonaKlassid();
		tuvastaja.setSonaKlassid( this.sonaKlassid );
		
		// 2) Parsime dokumendist reeglid ja riputame tuvastajale
		tuvastaja.setReeglid( parsiDokumendistTuvastamisReeglid() );
		
		// 3) Parsime dokumendist liitumisreeglid, lisame tuvastajale
		tuvastaja.setLiitumisReeglid( parsiDokumendistLiitumisReeglid() );
	}
	
	
	/**
	 *  V&otilde;tab etteantud XML-andmeid sisaldavast sisendvoost ajavaljendite tuvastamise
	 *  reeglid ning kinnitab antud ajav2ljendite tuvastaja kylge.
	 *  
	 */
	public void votaMustridXMLSisendvoost(InputStream in, AjaTuvastaja tuvastaja) throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		// parsime faili builder-i abil, et saada 
		// XML-failist DOM esitust 
		dom = db.parse(in);
			
		// 1) Parsime dokumendist s6naklassid, riputame kylge endale ja tuvastajale
		this.sonaKlassid = parsiDokumendistSonaKlassid();
		tuvastaja.setSonaKlassid( this.sonaKlassid );

		// 2) Parsime dokumendist reeglid ja riputame tuvastajale
		tuvastaja.setReeglid( parsiDokumendistTuvastamisReeglid() );
		
		// 3) Parsime dokumendist liitumisreeglid, lisame tuvastajale
		tuvastaja.setLiitumisReeglid( parsiDokumendistLiitumisReeglid() );		
	}
	
	//==============================================================================
	//  T u v a s t a m i s r e e g l i    a l a m o s a d e     p a r s i m i n e
	//==============================================================================
	
	/**
	 *  Ekstraktib XML-faili DOM esitusest tuvastamisreeglid. Selle meetodi k&auml;ivitamine
	 * eeldab, et eelnevalt on XML-fail edukalt avatud ning selle p&otilde;hjal on loodud
	 * <i>Document Object Model</i> (klassimuutujas <code>dom</code>);
	 * 
	 * @return nimestik fraasimustritest
	 */
	private List <TuvastamisReegel> parsiDokumendistTuvastamisReeglid(){
		List <TuvastamisReegel> reeglid = new ArrayList<TuvastamisReegel>();
		int mustriLoendur = 1;		
		// v6tame dokumendi juurelemendi
		Element juur = dom.getDocumentElement();
		// v6tane k6ik elemendid, mis on m2rgistatud kui "Reegel"-itena
		NodeList reegelNodes = juur.getElementsByTagName(TAG_REEGEL);
		if (reegelNodes != null && reegelNodes.getLength() > 0){
			reeglid = new ArrayList<TuvastamisReegel>( reegelNodes.getLength() );
			for (int i = 0; i < reegelNodes.getLength(); i++) {
				//--------------------  REEGEL  ----------------------------
				Element reegelElem = (Element) reegelNodes.item(i);
				Element musterElem = getElementByTagName(reegelElem, TAG_MUSTER);
				if (musterElem != null){
					// ----------------  MUSTER  -------------------------------					
					String muster = musterElem.getTextContent();
					FraasiMuster fm = parsiFraasiMuster(muster, mustriLoendur++);
					// Ylejaanu parsimine omab motet vaid juhul, kui Fraasimuster on edukalt loodud
					if (fm != null){
						TuvastamisReegel reegel = new TuvastamisReegel();
						reegel.setFraasiMuster(fm);
						reeglid.add(reegel);						
						// ----------   fraasimustrifiltrid   (olemasolul) ---------
						NodeList filterNodes = reegelElem.getElementsByTagName(TAG_FILTER);
						if (filterNodes != null && filterNodes.getLength() > 0){
							for (int j = 0; j < filterNodes.getLength(); j++) {
								Element filterElem = (Element) filterNodes.item(j);
								FraasiMustriFilter fmf = parsiFraasiMustriFilter(filterElem);
								if (fmf != null){
									reegel.lisaFraasiMustriFilter(fmf);
								}
							}
						}
						// ------   v2ljaspoole fraasimustrifiltreid j22v   ----------
						// ------    semantiline osa (kui see on lisatud)   ----------
						NodeList semDefNodes = reegelElem.getElementsByTagName(TAG_SEMDEF);
						List<SemantikaDefinitsioon> semDefinitsioonid = new LinkedList<SemantikaDefinitsioon>();
						if (semDefNodes != null && semDefNodes.getLength() > 0){
							for (int j = 0; j < semDefNodes.getLength(); j++) {
								Element semDefElem = (Element) semDefNodes.item(j);
								// Kontrollime, et oleks tegu vahetu j2rglasega, mitte filtri alamosaga
								if ((semDefElem.getParentNode()).equals(reegelElem)){
									SemantikaDefinitsioon semDef = looUusSemDefElemendiPohjal(semDefElem);
									if (semDef != null){
										semDefinitsioonid.add(semDef);
									}
								}
							}
							
						}
						
						// ------  v2ljaspoole fraasimustrifiltreid j22vad ----------
						// ------------   m u s t r i t a h i s e d   ---------------
						NodeList mustriTahisNodes = reegelElem.getElementsByTagName(TAG_MUSTRITAHIS);
						List<MustriTahis> mustriTahised = new LinkedList<MustriTahis>();
						if (mustriTahisNodes != null && mustriTahisNodes.getLength() > 0){
							for (int j = 0; j < mustriTahisNodes.getLength(); j++) {
								Element tahisElem = (Element) mustriTahisNodes.item(j);
								// Kontrollime, et oleks tegu vahetu j2rglasega, mitte filtri alamosaga										
								if ((tahisElem.getParentNode()).equals(reegelElem)){
									MustriTahis tahis = looUusMustriTahisDefElemendiPohjal(tahisElem);
									if (tahisElem != null){
										mustriTahised.add(tahis);
									}
								}
							}
						}
						
						// salvestame v2ljastpoolt fraasimustrifiltreid leitud osa ...
						if (!semDefinitsioonid.isEmpty() || !mustriTahised.isEmpty()){
							FraasiMustriFilter fmf = new FraasiMustriFilter(null, null);
							if (!semDefinitsioonid.isEmpty()){
								fmf.setSemDefinitsioonid(semDefinitsioonid);
							}
							if (!mustriTahised.isEmpty()){
								for (MustriTahis mustriTahis : mustriTahised) {
									fmf.addMustriTahis(mustriTahis);
								}
							}								
							reegel.lisaFraasiMustriFilter(fmf);
						}						
						
						// -----    Tuvastamisreeglile on lisatud yks v6i mitu negatiivset mustrit
						NodeList negMusterNodes = reegelElem.getElementsByTagName( TAG_NEGMUSTER );
						if (negMusterNodes != null && negMusterNodes.getLength() > 0){
							List <NegatiivneMuster> negMustrid = new LinkedList <NegatiivneMuster> ();
							for (int j = 0; j < negMusterNodes.getLength(); j++) {
								Element negMusterNode = (Element) negMusterNodes.item(j);
								NegatiivneMuster negMustr = looUusNegMusterElemendiPohjal(negMusterNode);
								if (negMustr != null){
									negMustrid.add( negMustr );
								}
							}
							if ( !negMustrid.isEmpty() ){
								reegel.setNegMustrid( negMustrid );
							}
						}
					} // fm != null
				}
				
			}
		}
		return reeglid;
	}
	
	/**
	 *  Ekstraktib XML-faili DOM esitusest sonaklassid. Selle meetodi k&auml;ivitamine
	 * eeldab, et eelnevalt on XML-fail edukalt avatud ning selle p&otilde;hjal on loodud
	 * <i>Document Object Model</i> (klassimuutujas <code>dom</code>);
	 * 
	 * @return nimestik fraasimustritest
	 */
	private HashMap<String, SonaKlass> parsiDokumendistSonaKlassid(){
		HashMap<String, SonaKlass> sonaKlassid = new HashMap<String, SonaKlass>();		
		// v6tame dokumendi juurelemendi
		Element juur = dom.getDocumentElement();
		// v6tane k6ik elemendid, mis on m2rgistatud kui "Reegel"-itena
		NodeList sonaKlassNodes = juur.getElementsByTagName(TAG_SONAKLASS);
		if (sonaKlassNodes != null && sonaKlassNodes.getLength() > 0){
			sonaKlassid = new HashMap<String, SonaKlass>( sonaKlassNodes.getLength() );
			for (int i = 0; i < sonaKlassNodes.getLength(); i++) {
				//--------------------  SONAKLASS  ----------------------------
				SonaKlass sonaKlass = null;
				Element sonaKlassElem = (Element) sonaKlassNodes.item(i);
				String klassiNimi = getElementsAttributeByName(sonaKlassElem, TAG_SONAKLASS_NIMI);
				if (klassiNimi != null){
                     // Loome uue sonaklassi
					 sonaKlass = new SonaKlass(klassiNimi);					 
					 // Leiame ja loome sonaklassi alla kuuluvad mustrijupid
					 NodeList elementNodes = sonaKlassElem.getElementsByTagName(TAG_ELEMENT);
					 if (elementNodes != null && elementNodes.getLength() > 0){
						 for (int j = 0; j < elementNodes.getLength(); j++) {
							 //--------------------  SONAKLASSI ELEMENT  ------------------------
							 Element elem = (Element) elementNodes.item(j);
							 SonaMall sonaMall = parsiDokumendiElemendistSonaMall(elem);
							 if (sonaMall != null){
								 sonaKlass.lisaElement(sonaMall);
							 }
						 }
					 }
					 if (!(sonaKlass.getElemendid()).isEmpty()){
						 // Kui loodud sonaklass on valiidne, st, mittetyhi, lisame paisktabelisse
						 sonaKlassid.put(klassiNimi, sonaKlass);
					 }
				}
			}	
		}
		return sonaKlassid;
	}
	
	/**
	 *    Parsib XML-kujul sonaklassi elemendist andmed ning loob nende p6hjal uue sonamalli. 
	 *   Tagastab loodud sonamalli.
	 *   
	 * @param elem XML-element, millest andmed parsida...
	 * @return loodud sonamalli
	 */
	private SonaMall parsiDokumendiElemendistSonaMall(Element elem){
		SonaMall sonaMall = null;
		String tyyp = getElementsAttributeByName(elem, TAG_ELEMENT_TYYP);
		if (tyyp != null){
			// -----------------------  ALGVORM ------------------------------
			if (tyyp.compareTo(TAG_ELEMENT_TYYP_ALGVORM)==0){
				String vaartus = getElementsAttributeByName(elem, TAG_ELEMENT_VAARTUS);
				if (vaartus != null){
					sonaMall = new AlgVormSonaMall(vaartus);
				}
			}
			// ------------  ARVSONAFRAAS v6i ERIKUJULINE ARV ----------------
			if (tyyp.compareTo(TAG_ELEMENT_TYYP_ARVSONA)==0 || 
					tyyp.compareTo(TAG_ELEMENT_TYYP_ERI_ARV)==0){
				sonaMall = new ArvuEriKujudSonaMall();
				// Piirang v6imalike vaartuste vahemikule
				String numPiirang = getElementsAttributeByName(elem, TAG_ELEMENT_NUMPIIRANG);
				if (numPiirang != null){
					String range[] = numPiirang.split("-");
					int lowerBound = -1;
					int upperBound = -1;
					if (range.length == 2){
						try {
							lowerBound = Integer.parseInt(range[0]);
							upperBound = Integer.parseInt(range[1]);							
						} catch (NumberFormatException e) {
						}
					} else if (numPiirang.startsWith("-")){
						try {
							upperBound = Integer.parseInt(range[0]);							
						} catch (NumberFormatException e) {
						}
					} else {
						try {						
							lowerBound = Integer.parseInt(range[0]);
						} catch (NumberFormatException e) {
						}						
					}
					((ArvuEriKujudSonaMall)sonaMall).setArvuVaartusePiirid(lowerBound, upperBound);
				}
				// Piirang arvu tyybile (tavaline v6i j2rgarv)
				String arvuTyyp = getElementsAttributeByName(elem, TAG_ELEMENT_ARVULIIK);
				if (arvuTyyp != null){
					((ArvuEriKujudSonaMall)sonaMall).setArvuTyyp(arvuTyyp);
				}
			}
			// --------------------  REGULAARAVALDIS -------------------------
			if (tyyp.compareTo(TAG_ELEMENT_TYYP_REGEXP)==0){
				String vaartus = getElementsAttributeByName(elem, TAG_ELEMENT_VAARTUS);
				if (vaartus != null){
					sonaMall = new RegExpSonaMall(vaartus);
				}				
			}
			// ----------------------  TAVALINE TEKST  -----------------------
			if (tyyp.compareTo(TAG_ELEMENT_TYYP_TEKST)==0){
				String vaartus = getElementsAttributeByName(elem, TAG_ELEMENT_VAARTUS);
				if (vaartus != null){
					sonaMall = new TavaTekstSonaMall(vaartus);
				}				
			}
			// -------------------- Lisame semantilise osa -------------------
			if (sonaMall != null){
				lisaSonaMallileSemOsa(sonaMall, elem);
			}
		}
		return sonaMall;
	}
	
	/**
	 *  Ekstraktib XML-elementist semantika-definitsiooni osa ning lisab etteantud s6namallile.
	 *  
	 * @param sonaMall
	 * @param elem
	 */
	private void lisaSonaMallileSemOsa(SonaMall sonaMall, Element elem){
		// ------------ Muudetav v2li/granulaarsus
		String semField = getElementsAttributeByName(elem, TAG_SEMFIELD);
		if (semField != null){
			sonaMall.lisaSemField(semField);
		}
		// ------------ Rakendatav operatsioon
		String op = getElementsAttributeByName(elem, TAG_OP);
		if (op != null){
			sonaMall.lisaOp(op);
		}
		// ------------ Omistatav v22rtus
		String semValue = getElementsAttributeByName(elem, TAG_SEMVALUE);
		if (semValue != null){
			sonaMall.lisaSemValue(semValue);
		}
		// ------------ Omistatav lipik (kui v22rtus on ebat2pne v6i puudu)
		String semLabel = getElementsAttributeByName(elem, TAG_SEMLABEL);
		if (semLabel != null){
			sonaMall.lisaSemLabel(semLabel);
		}
		// ------------ V22rtuse ebat2psus
		String semEbatapne = getElementsAttributeByName(elem, TAG_SEMVAL_EBATAPNE);
		if (semEbatapne != null){
			if (semEbatapne.equals("1")){
				sonaMall.setSemValueOnEbatapne(true);
			}
		}
		// ------------ Otsimissuund (ainult SEEK tyypi k2skude korral)		
		String direction = getElementsAttributeByName(elem, TAG_DIRECTION);
		if (direction != null){
			sonaMall.setSeekDirection(direction);
		}		
	}
	
	/**
	 *  Ekstraktib XML-elementist semantika-definitsiooni (~ semantikareegli) kirjelduse ning 
	 *  loob selle pohjal uue <tt>SemantikaDefinitsioon</tt> objekti.
	 */
	private SemantikaDefinitsioon looUusSemDefElemendiPohjal(Element elem){
		SemantikaDefinitsioon semDef = new SemantikaDefinitsioon();
		// ------------ kasutatud mustriosa
		String seotudMustriOsa = getElementsAttributeByName(elem, TAG_SEOTUD_MUSTRIOSA);
		if (seotudMustriOsa != null){
			semDef.setSeotudMustriOsa(seotudMustriOsa);
		}
		// ------------ kontekstitingimused
		String seotudKontekst = getElementsAttributeByName(elem, TAG_SEOTUD_KONTEKST);
		if (seotudKontekst != null){
			semDef.setSeotudKontekst(seotudKontekst);
		} else {
			// Et eristuda mustreist, millel seotudmustriosa on != null ja != ""
			semDef.setSeotudKontekst("");
		}
		// ------------ prioriteet
		String priority = getElementsAttributeByName(elem, TAG_PRIORITEET);
		if (priority != null){
			semDef.setPriority(priority);
		}
		// ------------ Rakendatav operatsioon
		String op = getElementsAttributeByName(elem, TAG_OP);
		if (op != null){
			semDef.setOp(op);
		}
		// ------------ Muudetav v2li/granulaarsus
		String semField = getElementsAttributeByName(elem, TAG_SEMFIELD);
		if (semField != null){
			if (Granulaarsus.getGranulaarsus(semField) != null){
				semDef.setGranulaarsus(Granulaarsus.getGranulaarsus(semField));
			}
			if (op != null){
				if (op.equals(SemantikaDefinitsioon.OP.ANCHOR_TIMEX.toString()) ||
					op.equals(SemantikaDefinitsioon.OP.ANCHOR_TIMEX_IN_SENTENCE.toString()) ||
					op.equals(SemantikaDefinitsioon.OP.FIND_NTH_SUBGRAN.toString())){
					semDef.setGranulaarsusStr(semField);
				}
			}
			
		}		
		// ------------ Omistatav v22rtus
		String semValue = getElementsAttributeByName(elem, TAG_SEMVALUE);
		if (semValue != null){
			semDef.setSemValue(semValue);
		}
		// ------------ Omistatav lipik (kui v22rtus on ebat2pne v6i puudu)
		String semLabel = getElementsAttributeByName(elem, TAG_SEMLABEL);
		if (semLabel != null){
			semDef.setSemLabel(semLabel);
		}
		// ------------ V22rtuse ebat2psus
		String semEbatapne = getElementsAttributeByName(elem, TAG_SEMVAL_EBATAPNE);
		if (semEbatapne != null){
			if (semEbatapne.equals("1")){
				semDef.setSemValueOnEbatapne(true);
			}
		}
		// ------------ Otsimissuund (ainult SEEK tyypi k2skude korral)		
		String direction = getElementsAttributeByName(elem, TAG_DIRECTION);
		if (direction != null){
			semDef.setDirection( direction );
		}
		// ------------ Semantika lahendamise mudel	
		String mudel = getElementsAttributeByName(elem, TAG_MUDEL);
		if (mudel != null){
			semDef.setMudel( mudel );
		}
		// ------------ Eksplitsiitne punkt (kaskude CREATE_beginPoint, CREATE_endPoint korral)	
		String explicitPoint = getElementsAttributeByName(elem, TAG_EXPLICIT_POINT);
		if (explicitPoint != null){
			semDef.setIsExplicitPoint(explicitPoint);
		}
		// ------------ Muudetav atribuut (kasu SET_attrib korral)
		String attrib = getElementsAttributeByName(elem, TAG_ATTRIB);
		if (attrib != null){
			semDef.setAttribute(attrib);
		}		
		return semDef;
	}
	
	/**
	 *  Ekstraktib XML-elementist mustritahise kirjelduse ning loob selle pohjal uue 
	 *  <tt>MustriTahis</tt> objekti.
	 */
	private MustriTahis looUusMustriTahisDefElemendiPohjal(Element elem){
		MustriTahis mustriTahis = new MustriTahis();
		// ------------ kasutatud mustriosa
		String seotudMustriOsa = getElementsAttributeByName(elem, TAG_SEOTUD_MUSTRIOSA);
		if (seotudMustriOsa != null){
			mustriTahis.setSeotudMustriOsa(seotudMustriOsa);
		}
		// ------------ kasutatud mustriosa
		String mustriTahisedSonena = getElementsAttributeByName(elem, TAG_MUSTRITAHISED);
		if (mustriTahisedSonena != null){
			mustriTahis.setMustriTahised(mustriTahisedSonena);
		}
		// ------------ viidatud ajav2ljend pole eraldiseisev ajav2ljend
		String poleEraldiseisevAjavaljend = getElementsAttributeByName(elem, TAG_MUSTRITAHIS_PESA);
		if (poleEraldiseisevAjavaljend != null){
			mustriTahis.setAjavPoleEraldiSeisev( poleEraldiseisevAjavaljend.matches("(1|true)") );
		}		
		return mustriTahis;
	}
	
	/**
	 *  Ekstraktib XML-elementist negatiivse mustri kirjelduse ning 
	 *  loob selle pohjal uue <tt>NegatiivneMuster</tt> objekti.
	 */
	private NegatiivneMuster looUusNegMusterElemendiPohjal(Element elem){
		NegatiivneMuster negMuster = null;
		String muster = elem.getTextContent();
		if (muster != null){
			negMuster = new NegatiivneMuster();
			negMuster.setMusterSonena(muster);
			// ------------ startPos
			String startPos = getElementsAttributeByName(elem, TAG_NEGMUSTER_STARTPOS);
			if (startPos != null){
				try {
					int startingPos = Integer.parseInt( startPos );
					negMuster. setAlgusPositsioon( startingPos );
				} catch (NumberFormatException e) {
				}
			}
			// ------------ preserve surrounding punctuation while matching a word
			String preservePunct = getElementsAttributeByName(elem, TAG_NEGMUSTER_PRESPUNCT);
			if (preservePunct != null){
				if (preservePunct.matches("(?i)true")){
					negMuster.setSailitaYmbritsevadMargid( true );
				}
			}
		}
		return negMuster;
	}
	/**
	 *    Parsib etteantud mustrist sonamallide jarjendi ja loob selle p6hjal uue fraasimustri.
	 *  Muster v6ib sisaldada nii uusi sonamalli definitsioone (vastavalt algvormid m2rkide /.../ ja 
	 *  regulaaravaldised markide |...| vahel) kui ka viiteid olemasolevatele sonaklassidele 
	 *  (l&auml;bivate suurt&auml;htedega soned).
	 * <p>
	 *  Kysimark sonamalli l6pus margib, et tegemist on <i>valikulise</i> sonamalliga. Kui koik mustrisse 
	 *  kuuluvad mallid on margitud valikuliseks, tagastab see meetod <code>null</code>-i, kuna selliseid
	 *  mustreid me ei luba (joondub potentsiaalselt iga fraasiga tekstis).
	 * <p>
	 * N&auml;ide mustrist:		
	 * <pre>
	 *        |kell|kl| ARV_2KOHTA /:/? ARV_2KOHTA? 
	 * </pre>
	 *   
	 */
	private FraasiMuster parsiFraasiMuster(String muster, int mustriID){
		List <SonaMall> mallid       = new ArrayList<SonaMall>();
		List <Boolean> valikulisused = new ArrayList<Boolean>();
		List <Boolean> areJaetavad   = new ArrayList<Boolean>();
		// Jaotame mustri tyhikute kohalt juppideks ja parsime jupid
		StringTokenizer tokenizer = new StringTokenizer(muster);
		int valikulisi   = 0;
		int araJaetavaid = 0;
		while(tokenizer.hasMoreTokens()){
			String element = tokenizer.nextToken();
			SonaMall sonaMall = null;
			Matcher regMatcher = (musterRegexp.matcher(element));
			Matcher algMatcher = (musterAlgvorm.matcher(element));
			Matcher klaMatcher = (musterSonaKlass.matcher(element));
			if (regMatcher.matches()){
				// --------------------  REGULAARAVALDIS -------------------------
					sonaMall = new RegExpSonaMall( regMatcher.group(1) );
			} else if (algMatcher.matches()){
				// -----------------------  ALGVORM ------------------------------
					sonaMall = new AlgVormSonaMall( algMatcher.group(1) );				
			} else if (klaMatcher.matches()){
				// ----------------------  SONAKLASS  -----------------------------
					String key = klaMatcher.group(1);
					if (sonaKlassid.containsKey(key)){
						sonaMall = sonaKlassid.get(key);
					}
			}
			if (sonaMall != null){
				// Teeme kindlaks valikulisuse				
				if ((musterValikuline.matcher(element)).matches()){
					valikulisused.add(new Boolean(true));
					valikulisi++;
				} else {
					valikulisused.add(new Boolean(false));
				}
				// Teeme kindlaks arajaetavuse				
				if ((musterArajaetav.matcher(element)).matches()){
					areJaetavad.add(new Boolean(true));
					araJaetavaid++;
				} else {
					areJaetavad.add(new Boolean(false));
				}				
				// Lisame				
				mallid.add(sonaMall);				
			}
		}
		// Kui yhtki sonamalli ei leitud, hylgame mustri
		if (mallid.size() == 0){
			return null;
		}
		// Kui k6ik sonamallid on valikulised v6i arajaetavad, hylgame mustri
		if (valikulisi == mallid.size() || araJaetavaid == mallid.size()){
			return null;
		}
		return new FraasiMusterFSM(mallid, valikulisused, areJaetavad, "m"+String.valueOf(mustriID) );
		//return new FraasiMusterLihtne(mallid, valikulisused, areJaetavad, "m"+String.valueOf(mustriID) );
	}
	
	/**
	 *  Parsib etteantud fraasimustrifiltrile viitavast XML elemendist fraasimustrifiltri. 
	 *  <p>
	 *  Tagastab null, kui antud filtri definitsiooni alt ei 6nnestunud parsida yhtegi
	 *  semantikadefinitsiooni. 
	 */
	private FraasiMustriFilter parsiFraasiMustriFilter(Element fraasiMustriFilterElem){
		String seotudMustriOsa = getElementsAttributeByName(fraasiMustriFilterElem, TAG_SEOTUD_MUSTRIOSA);
		String morfTunnused    = getElementsAttributeByName(fraasiMustriFilterElem, TAG_MORF_TUNNUSED);
		FraasiMustriFilter filter = new FraasiMustriFilter(morfTunnused, seotudMustriOsa);
		NodeList semDefNodes = fraasiMustriFilterElem.getElementsByTagName(TAG_SEMDEF);
		// 1) Parsime semantikadefinitsioonid
		if (semDefNodes != null && semDefNodes.getLength() > 0){
			List<SemantikaDefinitsioon> semDefinitsioonid = new LinkedList<SemantikaDefinitsioon>();
			for (int j = 0; j < semDefNodes.getLength(); j++) {
				Element semDefElem = (Element) semDefNodes.item(j);
				SemantikaDefinitsioon semDef = looUusSemDefElemendiPohjal(semDefElem);
				if (semDef != null){
					semDefinitsioonid.add(semDef);
				}
			}
			if (!semDefinitsioonid.isEmpty()){
				filter.setSemDefinitsioonid(semDefinitsioonid);
			} else {
				return null;
			}
		}
		NodeList mustriTahisNodes = fraasiMustriFilterElem.getElementsByTagName(TAG_MUSTRITAHIS);
		// 2) Parsime mustritahised (mittekohustuslik osa)
		if (mustriTahisNodes != null && mustriTahisNodes.getLength() > 0){
			for (int j = 0; j < mustriTahisNodes.getLength(); j++) {
				Element mustriTahisElem = (Element) mustriTahisNodes.item(j);
				MustriTahis       tahis = looUusMustriTahisDefElemendiPohjal(mustriTahisElem);
				if (tahis != null){
					filter.addMustriTahis( tahis );
				}
			}
		}
		return filter;
	}
	
	//==============================================================================
	//   L i i t u m i s r e e g l i t e    p a r s i m i n e
	//==============================================================================
	
	/**
	 *  Ekstraktib XML-faili DOM esitusest liitumisreeglid. Selle meetodi k&auml;ivitamine
	 * eeldab, et eelnevalt on XML-fail edukalt avatud ning selle p&otilde;hjal on loodud
	 * <i>Document Object Model</i> (klassimuutujas <code>dom</code>);
	 * 
	 * @return nimestik fraasimustritest
	 */
	private List <LiitumisReegel> parsiDokumendistLiitumisReeglid(){
		List <LiitumisReegel> reeglid = new ArrayList<LiitumisReegel>();
		// v6tame dokumendi juurelemendi
		Element juur = dom.getDocumentElement();
		// v6tane k6ik elemendid, mis on m2rgistatud kui "Reegel"-itena
		NodeList reegelNodes = juur.getElementsByTagName(TAG_LIITUMISREEGEL);
		if (reegelNodes != null && reegelNodes.getLength() > 0){
			for (int i = 0; i < reegelNodes.getLength(); i++) {
				//--------------------  REEGEL  ----------------------------
				Element reegelElem = (Element) reegelNodes.item(i);
				// ------------  muster					
				String muster = reegelElem.getTextContent();
				if (muster != null){
					StringTokenizer tokenizer     = new StringTokenizer(muster);
					if (tokenizer.countTokens() > 0){
						LiitumisReegel liitumisReegel = new LiitumisReegel();						
						String [] alamOsad = new String[ tokenizer.countTokens() ];
						int j = 0;
						while (tokenizer.hasMoreElements()) {
							String mustriOsa = (String) tokenizer.nextElement();
							alamOsad[j++]    = mustriOsa;
						}
						liitumisReegel.setMustriTahised(alamOsad);

						// ------------ aste
						String reegliAste = 
							getElementsAttributeByName(reegelElem, TAG_LIITUMISREEGEL_ASTE);
						if (reegliAste != null){
							if (reegliAste.equals("FRAAS")){
								liitumisReegel.setYhendamiseAste(ASTE.YHENDATUD_FRAASINA);								
							}
							if (reegliAste.equals("VAHEMIK")){
								liitumisReegel.setYhendamiseAste(ASTE.YHENDATUD_VAHEMIKUNA);								
							}
						}
						
						// ------------ fraasiJarjekordFikseeritud
						String fraasiJarjekordFikseeritud = 
							getElementsAttributeByName(reegelElem, TAG_LIITUMISREEGEL_FJRK);
						if (fraasiJarjekordFikseeritud != null){
							liitumisReegel.setFikseeritudJarjekord(
									fraasiJarjekordFikseeritud.matches("(1|true)")
							);
						}
						
						// ------------ fraasiJarjekordFikseeritud
						String peavadOlemaTapseltKorvuti = 
							getElementsAttributeByName(reegelElem, TAG_LIITUMISREEGEL_K6RVUTI);
						if (peavadOlemaTapseltKorvuti != null){
							liitumisReegel.setTapseltKorvuti(
									peavadOlemaTapseltKorvuti.matches("(1|true)")
							);
						}
						
						reeglid.add(liitumisReegel);
					}
				}
			}
		}
		return reeglid;
	}	
					
	//==============================================================================
	//   A b i m e e t o d i d
	//==============================================================================
	
	/**
	 *  Tagastab vanemelemendi (<code>parentElement</code>) alla kuuluva elemendi, mille silt
	 *  (<i>tag</i>) on v&otilde;rdne s&otilde;nega <code>tagName</code>. Kui selliseid elemente 
	 *  on mitu, tagastatakse vaid esimene.
	 * 
	 * @param parentElement vanemelement
	 * @param tagName silt
	 * @return
	 */
	private static Element getElementByTagName(Element parentElement, String tagName){
		NodeList nl = parentElement.getElementsByTagName(tagName);
		if(nl != null && nl.getLength() > 0) {
			return (Element)nl.item(0);
		}
		return null;
	}

	/**
	 *   Votab etteantud XML-elemendist attribuudi ja tagastab selle vaartuse sonena.
	 *  Kui soovitud atribuut on puudu v6i selle vaartus on maaramata, tagastab <code>null</code>;
	 */
	private static String getElementsAttributeByName(Element element, String attribName){
		String attrib = element.getAttribute(attribName);
		if (attrib != null && attrib.length() > 0){
			return attrib;
		}
		return null;
	}
	
}
