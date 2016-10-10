//  Evaluation tools for Ajavt
//  Copyright (C) 2009-2016  University of Tartu
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

package ee.ut.soras.test_ajavt;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;
import ee.ut.soras.ajavtV2.EelTootlus;
import ee.ut.soras.ajavtV2.util.LogiPidaja;
import ee.ut.soras.wrappers.EstyhmmWrapper;

/**
 * 
 * Mitmesugused abimeetodid ajav&auml;ljendite tuvastaja automaatsele testijale.
 * @author Siim
 */
public class Abimeetodid {
	
	// =======================================================================================
	//        K o d e e r i n g u g a   s e o n d u v 
	// =======================================================================================
	
	/**
	 *    Kuvab koik voimalikud enkodeeringud.
	 */
	public static void listAllAvailableCharsets(){
	    Map<String, Charset> map = Charset.availableCharsets();
	    Iterator<String> it = map.keySet().iterator();
	    while (it.hasNext()) {
	        // Get charset name
	        String charsetName = (String)it.next();
	        System.out.println(charsetName);
	    }
	}
	
	/**
	 *  Leiab s6nes etteantud positsioonil asuva karakteri Unicode koodi (koodi kujul \\uXXXX, kus
	 * XXXX on kaheksandsysteemis arv). Tagastab leitud koodi s6nena "\\uXXXX";
	 */
	public static String getUnicodeValueOfChar(String str, int charPosition){
		if (0 <= charPosition && charPosition < str.length()){
			// Convert codepoint into hexadecimal format
			String hexValue = Integer.toHexString( str.codePointAt( charPosition ) );
			return "\\" + "u" + hexValue;
		}
		return null;
	}
	
	// =======================================================================================
	//       M ä r g e n d a t u d    a j a v ä l j e n d i t e     e r a l d a m i n e
	//                                    t e k s t i s t
	// =======================================================================================

	/**
	 *   Saab sisendiks listi eraldatud ajav2ljendifraasidest (koos m2rgendusega), eraldab nendest
	 *  v2ljendid sellisel kujul, et need oleksid sobival kujul k2sitsim2rgendusega v6rdlemiseks.
	 */
	public static List<MargendatudAjavaljend> eraldaMargendatudValjendidSonedest(
													List<String> tuvastatudAjavaljendid){
		List<MargendatudAjavaljend> margendatudAjavaljendid = new LinkedList<MargendatudAjavaljend>();
		if (tuvastatudAjavaljendid != null && !tuvastatudAjavaljendid.isEmpty()){
			for (String ajavaljendiFraas : tuvastatudAjavaljendid) {
				Object[] eralduseTulemus = 
					eraldaSonePositsiooniltAjavaljend(ajavaljendiFraas, 0, 0);
				if (eralduseTulemus[2] != null){
					MargendatudAjavaljend ajav = (MargendatudAjavaljend) eralduseTulemus[2];
					margendatudAjavaljendid.add(ajav);
				}
			}
		}
		return margendatudAjavaljendid;
	}
	
	/**
	 *  Eraldab sisendtekstist (tekst selle k6ige algsemal, k2sitsi-m2rgenduse kujul) m2rgendatud
	 * ajavaljendid ning tagastab listina <tt>MargendatudAjavaljend</tt>-objektidest.
	 */
	public static List<MargendatudAjavaljend> eraldaMargendatudValjendidTekstist(String tekst){
		LinkedList<MargendatudAjavaljend> ajavaljendid = new LinkedList<MargendatudAjavaljend>();
		int indeksTekstis  = 0;   // Tegelik indeks tekstis (arvestab ka m2rgendust)
		int subIndexInText = 0;   // Indeks tekstis, mis ignoreerib m2rgendust (TAG-e) ...
		while (indeksTekstis < tekst.length()){
			if (indeksTekstis + 1 < tekst.length()){
				String symbol = tekst.substring(indeksTekstis, indeksTekstis + 1);
				if (symbol.equals("<")){
					Object[] eralduseTulemus = 
						eraldaSonePositsiooniltAjavaljend(tekst, indeksTekstis, subIndexInText);
					indeksTekstis  = ((Integer)eralduseTulemus[0]).intValue();
					subIndexInText = ((Integer)eralduseTulemus[1]).intValue();
					if (eralduseTulemus[2] != null){
						MargendatudAjavaljend ajav = (MargendatudAjavaljend) eralduseTulemus[2];
						ajavaljendid.add( ajav );
					}
				} else {
					subIndexInText++;
				}
			}
			indeksTekstis++;
		}
		return ajavaljendid;
	}
	
	/**
	 *  Parsib antud positsioonilt tekstis ajav2ljendifraasi m2rgenduse. Tagastab listi objektidest:
	 *  <ul>
	 *  	<li> 0 - <tt>indeksTekstis</tt> uus v22rtus p2rast parsimist (Integer)
	 *  	<li> 1 - <tt>subIndexInText</tt> uus v22rtus p2rast parsimist (Integer)
	 *  	<li> 2 - <tt>MargendatudAjavaljend</tt>-objekt, kui parsimine 6nnestus, vastasel juhul null
	 *  </ul>
	 *  <p>
	 *  TODO: praegune lahendus feilib topeltm2rgenduste korral ...
	 */
	private static Object [] eraldaSonePositsiooniltAjavaljend(String sone, 
															  int indeksTekstis,
															  int subIndexInText){
		Object [] tagastatavad = new Object[3];
		indeksTekstis++;
		// Leidsime ajavaljendi algusm2rgi: kogume kokku kogu ajav2ljendi
		int startPosition = -1;
		int endPosition   = -1;
		String algusTag = null;
		String fraas    = null;
		boolean potentialEmptyElementTag = false;
		StringBuilder sb = new StringBuilder(50);
		while (indeksTekstis < sone.length()){
			String s = sone.substring(indeksTekstis, indeksTekstis + 1);
			if (s.equals(">")){
				if (algusTag == null){
					algusTag = sb.toString();
					sb = new StringBuilder(50);
					startPosition = subIndexInText + 1;
					indeksTekstis++;
					if (potentialEmptyElementTag){
						// Kui m2rgendusel puudub sisu (empty-element tag), l6petame
						break;
					} else {
						// Kui m2rgendusel on sisu, j2tkame sisu kogumisega
						continue;						
					}
				} else if (fraas != null){
					// oleme j6udnud edukalt margenduse l6ppu
					break;
				}
			} else if (s.equals("<")){
				if (algusTag != null){
					potentialEmptyElementTag = false;
					fraas = sb.toString();
					endPosition = subIndexInText;
					sb = new StringBuilder(10);
				} else {
					// Midagi on viltu, igaks juhuks l6petame
					break;	
				}
			} else if (s.equals("/")){
				potentialEmptyElementTag = true;
			}
			sb.append(s);
			// Liigume edasi m2rgendite vahel, tekstis
			if (algusTag != null && fraas == null){
				subIndexInText++;
			}
			indeksTekstis++;
		}
		// 
		// 1) start-tag and end-tag present 
		if (endPosition != -1 && startPosition != -1 && algusTag != null && fraas != null){
			MargendatudAjavaljend ajav = 
				new MargendatudAjavaljend(startPosition, endPosition, algusTag, fraas);
			tagastatavad[2] = ajav;
		} else if (startPosition != -1 && endPosition == -1 && algusTag != null && potentialEmptyElementTag){
		// 2) empty-element tag only
			endPosition = startPosition;
			MargendatudAjavaljend ajav = 
				new MargendatudAjavaljend(startPosition, endPosition, algusTag, fraas);
			tagastatavad[2] = ajav;
			// V6tame sammu tagasi: meetodis 'eraldaMargendatudValjendidTekstist' liigutakse
			// pooleliolevas tsyklis sammu v6rra edasi, seega satume positsioonile, mis järgneb
			// täpselt '>'-märgile; 
			indeksTekstis--;
		}
		tagastatavad[0] = Integer.valueOf(indeksTekstis);
		tagastatavad[1] = Integer.valueOf(subIndexInText);
		return tagastatavad;
	}
	
	// =======================================================================================
	//        A j a v ä l j e n d i t e     o m a v a h e l i n e     l i n k i m i n e 
	// =======================================================================================
	
	/**
	 *   Saab sisendiks m2rgendatud ajav2ljendid, leiab, millised v2ljendid on yksteisega seotud
	 *   (atribuudilinkide <code>beginPoint</code>, <code>endPoint</code>, <code>anchorTimeID</code> kaudu) 
	 *   ning loob vastavad lingid ka klassi <code>MargendatudAjavaljend</code> isendite vahele.
	 *   <p>
	 *   Tagastab linkimisel ilmnenud probleemide kirjelduse; Yhtlasi kontrollib, kas ajav2ljendite ID-de 
	 *   seas leidub duplikaate: kui leidub, tuuakse see probleemide kirjelduses välja.
	 *   <p>
	 *   TODO: <code>anchorTimeID</code> linkide loomine praegu puudub...
	 */
	public static String looAjavaljenditeVaheleLingid(List<MargendatudAjavaljend> margendatudValjendid){
		StringBuilder sb = new StringBuilder();
		HashMap<String, String> seenTIDs = new HashMap<String, String>();
		for (int i = 0; i < margendatudValjendid.size(); i++) {
			MargendatudAjavaljend av = margendatudValjendid.get(i);
			HashMap<String, String> attrValues = av.getAttrValues();
			// TID kontroll
			if (attrValues.get("tid") != null){
				if (seenTIDs.containsKey( attrValues.get("tid") )){
					sb.append("(!) Warning! Already occupied TID '");
					sb.append(attrValues.get("tid"));
					sb.append("' used in:   ");
					sb.append(av.toString());
					sb.append("\n");
				} else {
					seenTIDs.put( attrValues.get("tid"), "1");
				}
			}
			// Algus ja lõpp-punkti sidumine
			if (attrValues.get("beginPoint") != null){
				String beginPointID = attrValues.get("beginPoint");
				if (beginPointID.matches("^[a-z][0-9]+$")){
					boolean pointFound = false;
					for (int j = 0; j < margendatudValjendid.size(); j++) {
						MargendatudAjavaljend av2 = margendatudValjendid.get(j);
						HashMap<String, String> attrValues2 = av2.getAttrValues();
						if (attrValues2.get("tid") != null && (attrValues2.get("tid")).equals(beginPointID)){
							av.setBeginPointLink(av2);
							pointFound = true;
							break;
						}
					}
					if (!pointFound){
						sb.append("(!) TIMEX3 for 'beginPoint' value '");
						sb.append(beginPointID);
						sb.append("' not found! Link not created.");
						sb.append("\n");
					}
				}
			}
			if (attrValues.get("endPoint") != null){
				String endPointID = attrValues.get("endPoint");
				if (endPointID.matches("^[a-z][0-9]+$")){
					boolean pointFound = false;
					for (int j = 0; j < margendatudValjendid.size(); j++) {
						MargendatudAjavaljend av2 = margendatudValjendid.get(j);
						HashMap<String, String> attrValues2 = av2.getAttrValues();
						if (attrValues2.get("tid") != null && (attrValues2.get("tid")).equals(endPointID)){
							av.setEndPointLink(av2);
							pointFound = true;
							break;
						}
					}
					if (!pointFound){
						sb.append("(!) TIMEX3 for 'endPoint' value '");
						sb.append(endPointID);
						sb.append("' not found! Link not created.");
						sb.append("\n");
					}
				}
			}
		}
		return sb.toString();
	}

	// =======================================================================================
	//        M ä r g e n d u s e    k o n t r o l l    j a     h i n d a m i n e
	// =======================================================================================

	/**
	 *   Vordleb kaht etteantud margendust ning kontrollib TIMEX atribuutide (TYPE, VALUE, VALUE2, MOD, MOD2) 
	 *  vaartuseid. Jaadvustab kontrolli tulemused etteantud testimistulemuste alla. 
	 *  <p>
	 *  Tagastab <code>true</code>, kui yhtegi erinevust kasitsimargenduse ja automaatmargenduse vahel ei 
	 *  leitud, vastasel juhul <code>false</code>. 
	 */
	static public boolean kontrolliTIMEXatribuute(TestimisTulemusTERN tulemus,
			MargendatudAjavaljend kasitsiMargendatud,
			MargendatudAjavaljend automaatMargendatud) {
		boolean normaliseerimiseTulemusPositiivne = true; 
		HashMap<String, String> kM_attrValues = kasitsiMargendatud.getAttrValues();
		HashMap<String, String> aM_attrValues = automaatMargendatud.getAttrValues();
		String [] attributesToCheck = { "type", "value", "mod", "value2", "mod2", "quant", "freq" };
		for (int i = 0; i < attributesToCheck.length; i++) {
			String atribuut = attributesToCheck[i];
			boolean correctFound  = true;
			boolean spuriousFound = false;
			String kasitsiMargenduseVaartus  = kM_attrValues.get(atribuut);
			String automaatMargenduseVaartus = aM_attrValues.get(atribuut);
			if (kasitsiMargenduseVaartus != null){
				correctFound = false;
				tulemus.addToRelevantItems( atribuut.toUpperCase() );
				if (automaatMargenduseVaartus != null){
					if (kasitsiMargenduseVaartus.equals(automaatMargenduseVaartus)){
						tulemus.addToCorrectlyRecognized( atribuut.toUpperCase() );
						correctFound = true;
					}
				}
			}
			if (automaatMargenduseVaartus != null){
				tulemus.addToItemsExtracted( atribuut.toUpperCase() );
				if (kasitsiMargenduseVaartus == null){
					spuriousFound = true;
				}
			}
			if (!correctFound || spuriousFound){ normaliseerimiseTulemusPositiivne = false; }								
		}
		return normaliseerimiseTulemusPositiivne;
	}
	
	/**
	 *    Kontrollib ajav2ljendite-vaheliste seoste loomise tulemusi ning sisuta ajav2ljendite m2rgendamise tulemust;
	 *    Kuvab tulemused etteantud testlogisse, kui <code>!noDetails</code>.
	 */
	static public void kontrolliJaKuvaLinkimiseTulemused( TestimisTulemusTERN tulemus,
	                                               List<MargendatudAjavaljend> automaatMargendus,
	                                               List<MargendatudAjavaljend> kasitsiMargendus,
	                                               LogiPidaja testLog,
	                                               boolean noDetails ){
		//
		//   1) Loome esialgsed lingid, toome välja esialgsed linkimisvead
		//		
		String KMlinkimiseVead = looAjavaljenditeVaheleLingid(kasitsiMargendus);
		String AMlinkimiseVead = looAjavaljenditeVaheleLingid(automaatMargendus);
		if (!noDetails){
			if (KMlinkimiseVead.length() > 0){
				testLog.println("Linkimisprobleemid (käsitsimärgendus)");
				testLog.println(KMlinkimiseVead);
				testLog.println("----------------------------------------------------");
			}
			if (AMlinkimiseVead.length() > 0){
				testLog.println("Linkimisprobleemid (automaatmärgendus)");
				testLog.println(AMlinkimiseVead);
				testLog.println("----------------------------------------------------");		
			}
		}	
		//
		//   2) J22dvustame vahemikuseostes osalevad ajaväljendid ning vastastikuste joonduste puhul
		//      laiendame vastavusi ka linkide kaudu seotud implitsiitsetele liikmetele
		//
		HashMap <MargendatudAjavaljend, String> vahemikuSeostesValjendid = new HashMap<MargendatudAjavaljend,String>();
		List <MargendatudAjavaljend> sortedKeys = new ArrayList<MargendatudAjavaljend>();
		for (MargendatudAjavaljend kasitsiMargendatud : kasitsiMargendus) {
			if (kasitsiMargendatud.osalebVahemikuSeostes()){
				vahemikuSeostesValjendid.put(kasitsiMargendatud, "k");
				sortedKeys.add(kasitsiMargendatud);
			}
		}
		for (MargendatudAjavaljend automaatMargendatud : automaatMargendus){
			if (!vahemikuSeostesValjendid.containsKey(automaatMargendatud) && automaatMargendatud.osalebVahemikuSeostes()){
				vahemikuSeostesValjendid.put(automaatMargendatud, "a");
				sortedKeys.add(automaatMargendatud);
				if (automaatMargendatud.isAlignmentMutual()){
					automaatMargendatud.generateImplicitAlignments();
				}
			}
		}
		HashMap<MargendatudAjavaljend, String> kuvatudValjendid = new HashMap<MargendatudAjavaljend, String>();
		//
		//  3) Leiame k6ik vahemikud etteantud kuldstandardi (k2sitsim2rgendus) j2rgi.
		//     M66dame m2rgendamise tulemusi ja j22dvustame tulemustesse;
		//
		for ( MargendatudAjavaljend margendatudAjavaljend : sortedKeys ) {
			if (!kuvatudValjendid.containsKey(margendatudAjavaljend) &&
					(vahemikuSeostesValjendid.get(margendatudAjavaljend)).equals("k") &&
							margendatudAjavaljend.getIsBeginOrEndPointToTimexes() != null){
				List<MargendatudAjavaljend> kParentTimexes = margendatudAjavaljend.getIsBeginOrEndPointToTimexes();
				for (MargendatudAjavaljend kParentTimex : kParentTimexes) {
					// ------------------------      D U R A T I O N    /    I N T E R V A L  -----------------------------------
					String sign = "(+)  ";
					boolean parentIsAligned = kParentTimex.hasAlignedTimex();
					if (kParentTimex.isEmptyTag() && !kuvatudValjendid.containsKey(kParentTimex)){
						tulemus.addToRelevantItems("TIMEX");
						if (parentIsAligned){
							tulemus.addToItemsExtracted("TIMEX");
							tulemus.addToCorrectlyRecognized("TIMEX");
						}
					}					
					if (parentIsAligned){
						if (kParentTimex.getAlignedTimex() != null){
							kuvatudValjendid.put(kParentTimex.getAlignedTimex(), "a");							
						}
						// Tyhja tag'i puhul kontrollime ka teisi atribuute
						if (kParentTimex.isEmptyTag() && 
								!kuvatudValjendid.containsKey(kParentTimex) && 
									(kParentTimex.getAlignedTimex()).isEmptyTag()){
							if (!kontrolliTIMEXatribuute(tulemus, kParentTimex, kParentTimex.getAlignedTimex())){
								sign = " (-) ";
							}
						}
						if (!noDetails){
							testLog.println(sign + "Interval: " + kParentTimex );
							testLog.println("               "+kParentTimex.getAlignedTimex() );
						}
					} else {
						sign = "(--) ";
						if (!noDetails){
							testLog.println(sign + "Interval: " + kParentTimex );
						}
					}
					kuvatudValjendid.put(kParentTimex, "k");
					// ------------------------          b e g i n P o i n t         -----------------------------------					
					sign = "    (--) ";
					MargendatudAjavaljend kBeginPoint = kParentTimex.getBeginPointLink();
					if (kBeginPoint != null){
						if (kBeginPoint.isEmptyTag() && !kuvatudValjendid.containsKey(kBeginPoint)){
							tulemus.addToRelevantItems("TIMEX");
						}
						if (kBeginPoint.hasAlignedTimex() && parentIsAligned){
							kuvatudValjendid.put(kBeginPoint.getAlignedTimex(), "a");
							// Kontrollime, kas parent'i vaste viitab selle beginPoint'i vastele ...
							MargendatudAjavaljend parallelBeginPointLink = (kParentTimex.getAlignedTimex()).getBeginPointLink();
							if (kBeginPoint.getAlignedTimex() != null && parallelBeginPointLink == kBeginPoint.getAlignedTimex()){
								sign = "     (+) ";
								if (kBeginPoint.isEmptyTag() && !kuvatudValjendid.containsKey(kBeginPoint)){
									tulemus.addToItemsExtracted("TIMEX");
									tulemus.addToCorrectlyRecognized("TIMEX");
								}
								// Tyhja tag'i puhul kontrollime ka teisi atribuute
								if (kBeginPoint.isEmptyTag() && 
										!kuvatudValjendid.containsKey(kBeginPoint) && 
											(kBeginPoint.getAlignedTimex()).isEmptyTag()){
									if (!kontrolliTIMEXatribuute(tulemus, kBeginPoint, kBeginPoint.getAlignedTimex())){
										sign = "     (-) ";
									}
								}								
							}
						}
						if (!noDetails){
							testLog.println(sign+" begin: "+kBeginPoint );
							if (kBeginPoint.hasAlignedTimex() && parentIsAligned){
								testLog.println("                 "+kBeginPoint.getAlignedTimex() );								
							}
						}
						kuvatudValjendid.put(kBeginPoint, "k");
					} else {
						if (!noDetails){
							testLog.println(sign+" begin: null");
						}						
					}
					// ------------------------           e n d P o i n t           -----------------------------------					
					sign = "    (--) ";
					MargendatudAjavaljend kEndPoint = kParentTimex.getEndPointLink();
					if (kEndPoint != null){
						if (kEndPoint.isEmptyTag() && !kuvatudValjendid.containsKey(kEndPoint)){
							tulemus.addToRelevantItems("TIMEX");
						}						
						if (kEndPoint.hasAlignedTimex() && parentIsAligned){
							kuvatudValjendid.put(kEndPoint.getAlignedTimex(), "a");
							// Kontrollime, kas parent'i vaste viitab selle beginPoint'i vastele ...
							MargendatudAjavaljend parallelEndPointLink = (kParentTimex.getAlignedTimex()).getEndPointLink();
							if (kEndPoint.getAlignedTimex() != null && parallelEndPointLink == kEndPoint.getAlignedTimex()){
								sign = "     (+) ";
								if (kEndPoint.isEmptyTag() && !kuvatudValjendid.containsKey(kEndPoint)){
									tulemus.addToItemsExtracted("TIMEX");
									tulemus.addToCorrectlyRecognized("TIMEX");
								}
								// Tyhja tag'i puhul kontrollime ka teisi atribuute
								if (kEndPoint.isEmptyTag() && 
										!kuvatudValjendid.containsKey(kEndPoint) && 
											(kEndPoint.getAlignedTimex()).isEmptyTag()){
									if (!kontrolliTIMEXatribuute(tulemus, kEndPoint, kEndPoint.getAlignedTimex())){
										sign = "     (-) ";
									}
								}
							}
						}
						if (!noDetails){
							testLog.println(sign+"   end: "+kEndPoint );
							if (kEndPoint.hasAlignedTimex() && parentIsAligned){
								testLog.println("                 "+kEndPoint.getAlignedTimex() );								
							}							
						}
						kuvatudValjendid.put(kEndPoint, "k");						
					} else {
						if (!noDetails){
							testLog.println(sign+"   end: null");
						}						
					}
				}
			}
		}
		if (!noDetails){
			testLog.println("----------------------------------------------------");
		}		
		//
		//   4) Leiame k6ik seostes osalevad v2ljendid, mis ei sobitunud yhegi punktis 1) v2ljatoodud seosega ...
		//
		for ( MargendatudAjavaljend margendatudAjavaljend : sortedKeys ) {
			if (!kuvatudValjendid.containsKey(margendatudAjavaljend)){
				String errorType = "  Redundant relations"; 
				if (vahemikuSeostesValjendid.get(margendatudAjavaljend).equals("k")){
			   	 	errorType = "Has missing relations";
				}
				if (margendatudAjavaljend.isEmptyTag()){
					if (vahemikuSeostesValjendid.get(margendatudAjavaljend).equals("k")){
						tulemus.addToRelevantItems("TIMEX");
					} else {
						tulemus.addToItemsExtracted("TIMEX");
					}
				}
				if (margendatudAjavaljend.hasAlignedTimex()){
					if (!noDetails){
						if (vahemikuSeostesValjendid.get(margendatudAjavaljend).equals("k")){
							testLog.println("(??) "+errorType+": "+margendatudAjavaljend );
							testLog.println("                            "+margendatudAjavaljend.getAlignedTimex() );
						} else {
							testLog.println("(??) "+errorType+": "+margendatudAjavaljend.getAlignedTimex() );
							testLog.println("                            "+margendatudAjavaljend );							
						}
					}
				} else {
					if (!noDetails){
						testLog.println("(??) "+errorType+": "+margendatudAjavaljend );
					}
				}
				if (margendatudAjavaljend.getBeginPointLink() != null){
					testLog.println("          begin: "+margendatudAjavaljend.getBeginPointLink().toString() );
				}
				if (margendatudAjavaljend.getEndPointLink() != null){
					testLog.println("            end: "+margendatudAjavaljend.getEndPointLink().toString() );
				}
				if (margendatudAjavaljend.getIsBeginOrEndPointToTimexes() != null){
					for (MargendatudAjavaljend margendatudAjavaljend2 : margendatudAjavaljend.getIsBeginOrEndPointToTimexes()) {
						testLog.println("          parent: "+margendatudAjavaljend2.toString() );
					}
				}
			}
		}
		//
		//   5) Leiame k6ik ylej22nud (ei osalenud yheski seoses ...)
		//		
		for (MargendatudAjavaljend automaatMargendatud : automaatMargendus){
			HashMap<String, String> attrValues = automaatMargendatud.getAttrValues();
			boolean isCreationTime = (attrValues.containsKey("functionInDocument") && 
									 (attrValues.get("functionInDocument").equals("CREATION_TIME")));
			if (automaatMargendatud.isEmptyTag() &&
					!vahemikuSeostesValjendid.containsKey(automaatMargendatud) && 
						!kuvatudValjendid.containsKey(automaatMargendatud) &&
							!automaatMargendatud.hasAlignedTimex() &&
								!isCreationTime){
				tulemus.addToItemsExtracted("TIMEX");
				if (!noDetails){
					testLog.println("(!) Redundant: "+automaatMargendatud);
				}
			}
		}
		if (!noDetails){
			testLog.println("----------------------------------------------------");
		}
	}
	
	/**
	 *   <p>
	 *   Viib l2bi TERN-stiilis tulemuste hindamise: iga TIMEX atribuudi kohta tuuakse eraldi v2lja saagis ja tapsus.
	 *   Trykib hindamise tulemuse etteantud logisse;
	 *   </p>
	 *   <p>
	 *   Kui lipp <code>kasutaFraasiTapsustust</code> on seatud, kasutatakse v6imalusel fraaside v6rdlemisel 
	 *   k2sitsim2rgenduse fraasides <code>MargendatudAjavaljend.fraas</code> asemel
	 *   <code>MargendatudAjavaljend.tapsustatudFraas</code>'i;
	 *   </p>
	 */
	static public TestimisTulemusTERNkoond hindaMargendamiseTulemusiTERN( List<MargendatudAjavaljend> automaatMargendus,
													   		         List<MargendatudAjavaljend> kasitsiMargendus,
													   		         LogiPidaja testLog,
													   		         boolean noDetails,
													   		         DecimalFormat formatter,
													   		         boolean kasutaFraasiTapsustust){
		
		TestimisTulemusTERN tulemusIlmutatudValjendid = new TestimisTulemusTERN();  // reaalselt esinenud v2ljendid
		TestimisTulemusTERN tulemusVarjatudValjendid  = new TestimisTulemusTERN();  // teksti t6lgendamisel lisatud v2ljendid 
		
		// A) Leiame "loomulikud" joondused (joondused ylekattuvate tekstipositsioonide alusel)
		//     Vaatluse alt jäävad täielikult välja ilma tekstilise sisuta ajaväljendid;
		// A.1) Leiame edukad / pooledukad eraldamised: iga kasitsiM2rgenduse kohta yritame leida
		//      automaatMargenduse, mis katab seda v2hemalt osaliselt. 
		for (MargendatudAjavaljend kasitsiMargendatud : kasitsiMargendus) {
			if (kasitsiMargendatud.isEmptyTag()){
				continue;
			}
			if ((kasitsiMargendatud.getAttrValues()).get("comment") != null){
				tulemusIlmutatudValjendid.addOneToCommentCount();
			}
			tulemusIlmutatudValjendid.addToRelevantItems("TIMEX");
			tulemusIlmutatudValjendid.addToRelevantItems("EXTENT");
			// Yritame leida parima automaatm2rgneduse: st sellise, mis katab
			// k2sitsim2rgendust v6imalikult t2pselt ...
			MargendatudAjavaljend parimAutomaatMargendatud = null;
			int bestOverlapState                           = MargendatudAjavaljend.PHRASE_NOT_MATCHING;
			for (MargendatudAjavaljend automaatMargendatud : automaatMargendus) {
				if (automaatMargendatud.isEmptyTag()){
					continue;
				}
				if (kasitsiMargendatud.haveOverlappingPositions(automaatMargendatud)){
					if (!kasitsiMargendatud.hasAlignedTimex() && !automaatMargendatud.hasAlignedTimex()){
						// Leiame, millisel m22ral tegelikud fraasid kattuvad:
						int overlapState = kasitsiMargendatud.findPhrasesMatchingState(automaatMargendatud, kasutaFraasiTapsustust);
						if (overlapState > bestOverlapState){
							bestOverlapState = overlapState;
							parimAutomaatMargendatud = automaatMargendatud;
						}
					}
				}
			}
			// Kui leiti sobiv automaatm2rgendus, j22dvustame tulemuse
			if (bestOverlapState > MargendatudAjavaljend.PHRASE_NOT_MATCHING){
				parimAutomaatMargendatud.setAlignedTimex(kasitsiMargendatud);
				kasitsiMargendatud.setAlignedTimex(parimAutomaatMargendatud);
				String normaliseerimiseTulemus = "   ";
				
				// Eraldamise hindamine on pehme: positiivse tulemuse anna juba see, kui
				// antud kohtalst midagi leiti							
				tulemusIlmutatudValjendid.addToItemsExtracted("TIMEX");							
				tulemusIlmutatudValjendid.addToCorrectlyRecognized("TIMEX");

				tulemusIlmutatudValjendid.addToItemsExtracted("EXTENT");
				// ------ EXTENT
				if (bestOverlapState == MargendatudAjavaljend.PHRASE_IS_EXACTLY_MATCHING){
					tulemusIlmutatudValjendid.addToCorrectlyRecognized("EXTENT");
				}
				
				// ------- Ylejaanud TIMEX atribuudid: VALUE, TYPE, ...
				if (!kontrolliTIMEXatribuute(tulemusIlmutatudValjendid, kasitsiMargendatud, parimAutomaatMargendatud)){
					normaliseerimiseTulemus = " - ";
				}
				
				if (!noDetails){
					testLog.println(normaliseerimiseTulemus+" Alignment found: "+kasitsiMargendatud+ 
								  							"\n                     "+parimAutomaatMargendatud);
					
					if ((kasitsiMargendatud.getAttrValues()).get("comment") != null){
						testLog.println("                     '"+(kasitsiMargendatud.getAttrValues()).get("comment")+"'");
					}
				}
			}
			
		}
		// A.2) Iga paariliseta j22nud automaatMargenduse puhul: yritame leida, kas see katab mingit
		//      k2sitsim2rgendust, ning kui katab, j22dvustame statistika
		for (MargendatudAjavaljend automaatMargendatud : automaatMargendus) {
			if (automaatMargendatud.isEmptyTag() || automaatMargendatud.hasAlignedTimex()){
				continue;
			}
			for (MargendatudAjavaljend kasitsiMargendatud : kasitsiMargendus) {
				if (kasitsiMargendatud.isEmptyTag()){
					continue;
				}
				// Seome paariliseta j22nud automaatm2rgenduse esimese k2sitsim2rgendusega, mis
				// positsioonide poolest klapib
				if (kasitsiMargendatud.haveOverlappingPositions(automaatMargendatud)){
					int overlapState = kasitsiMargendatud.findPhrasesMatchingState(automaatMargendatud, kasutaFraasiTapsustust);
					if (overlapState != MargendatudAjavaljend.PHRASE_NOT_MATCHING){
						automaatMargendatud.setAlignedTimex(kasitsiMargendatud);
						tulemusIlmutatudValjendid.addToItemsExtracted("TIMEX");
						tulemusIlmutatudValjendid.addToItemsExtracted("EXTENT");						
					}
				}
			}
		}
		if (!noDetails){
			testLog.println("----------------------------------------------------");
		}
		// A.3) Leiame yleolevad ajavaljendid - need, mis j2id k6igest hoolimata paariliseta
		for (MargendatudAjavaljend automaatMargendatud : automaatMargendus) {
			if (automaatMargendatud.isEmptyTag()){
				continue;
			}			
			if (!automaatMargendatud.hasAlignedTimex()){
				tulemusIlmutatudValjendid.addToItemsExtracted("TIMEX");
				tulemusIlmutatudValjendid.addToItemsExtracted("EXTENT");
				if (!noDetails){
					testLog.println("(!) Redundant: "+automaatMargendatud);
				}
			} else if (automaatMargendatud.hasAlignedTimex() &&
					 		!automaatMargendatud.isAlignmentMutual()){
				if (!noDetails){
					testLog.println("(!) Not connected: "+automaatMargendatud.getAlignedTimex());
					testLog.println("                   "+automaatMargendatud);
				}
			} else {
				// Kui keegi korrektsetest ajav2ljendile ei viita, on tegemist valesti yhendatud v2ljendiga 
				boolean isWronglyConnected = true;
				for (MargendatudAjavaljend kasitsimargendatud : kasitsiMargendus) {
					if (kasitsimargendatud.getAlignedTimex() == automaatMargendatud){
						isWronglyConnected = false;
					}
				}
				if (!noDetails && isWronglyConnected){
					testLog.println("(!) Wrongly connected: "+automaatMargendatud );
				}
			}				
		}
		// A.4) Leiame puuduolevad ajavaljendid
		for (MargendatudAjavaljend kasitsiMargendatud : kasitsiMargendus) {
			if (!kasitsiMargendatud.isEmptyTag() && !kasitsiMargendatud.hasAlignedTimex()){
				if (!noDetails){				
					testLog.println("(!) Missing: "+kasitsiMargendatud);
					if ((kasitsiMargendatud.getAttrValues()).get("comment") != null){
						testLog.println("    '"+(kasitsiMargendatud.getAttrValues()).get("comment")+"'");
					}
				}
			}
		}
		testLog.println("----------------------------------------------------");
		// B) Leiame seosed ja seostes osalevad v2ljendid
		// B.1) K6igepealt k2sitsi m2rgendatud v2ljendid		
		kontrolliJaKuvaLinkimiseTulemused(tulemusVarjatudValjendid, automaatMargendus, kasitsiMargendus, testLog, noDetails);
	    // Statistika : ilmutatud kujul väljendid
		testLog.println();
	    testLog.println("  Automaatselt eraldatud: "+(int)tulemusIlmutatudValjendid.getItemsExtracted("TIMEX"));
	    testLog.println("       Kasitsi eraldatud: "+(int)tulemusIlmutatudValjendid.getRelevantItems ("TIMEX"));
	    testLog.println();
	    tulemusIlmutatudValjendid.calculateResults();
	    testLog.println( tulemusIlmutatudValjendid.printResultTable(formatter) );
	    // Statistika : varjatud kujul väljendid
	    if ( tulemusVarjatudValjendid.hasItemsExtractedOrRelevantOfType("TIMEX") ){
		    testLog.println();
		    testLog.println( " Tekstilise sisuta margendid: ");
			testLog.println();
			int autoExtracted = (int)tulemusVarjatudValjendid.getItemsExtracted("TIMEX");
			int handExtracted = (int)tulemusVarjatudValjendid.getRelevantItems ("TIMEX");
			if (autoExtracted == -1){ autoExtracted = 0; }
			if (handExtracted == -1){ handExtracted = 0; }
		    testLog.println("  Automaatselt eraldatud: " + autoExtracted );
		    testLog.println("       Kasitsi eraldatud: " + handExtracted );
		    testLog.println();
		    tulemusVarjatudValjendid.calculateResults();
		    testLog.println( tulemusVarjatudValjendid.printResultTable(formatter) );
		    testLog.println();	    	
	    }
		return new TestimisTulemusTERNkoond(tulemusIlmutatudValjendid, tulemusVarjatudValjendid);
	}

	// =======================================================================================
	//      T e k s t i f a i l i d e     v õ r d l e m i n e     s i s u     j ä r g i  
	// =======================================================================================
	
	/**
	 *   V6rdlem etteantud tulemust (logifailis <tt>lastFileName</tt>) sellele ajaliselt vahetult eelneva
	 *   tulemusega ning leiab erinevused (toob v2lja, millised read on failides muudetud).
	 *   <p>
	 *   Eeldab, et k6ik uuritavad logifailid paiknevad kaustas <tt>logDir</tt> ning failinimed on kujul:
	 *   <br>
	 *   <code>{inputPrefix}</code><code>{ISO-formaadis kuupäev}.txt</code>
	 *   <p>
	 *   Tulemused kirjutatakse samma kausta: faili, mille nimi on kujul:
	 *   <br>
	 *   <code>vordlus{outputSuffix}</code><code>{ISO-formaadis kuupäev}.txt</code>  
	 *   <p>
	 *   Kui <tt>lastFileName</tt> on <tt>null</tt>, v6etakse selle asemele jooksvast kaustast ajaliselt
	 *   hiliseim logifail.
	 */
	static void compareResultToLastLoggedResult(File logDir, String lastFileName, String inputPrefix, String outputSuffix, String encoding){
		 File dir = logDir;
		 // V6tame kataloogist testilogide failid 
		 String[] filesInCurrentDir = dir.list( new TestLogFilenameFilter(inputPrefix) );
		 if (filesInCurrentDir != null && filesInCurrentDir.length > 1){
			String logDirAsStr = logDir.getAbsolutePath() + File.separator;
			LogiPidaja vordluseLog = new LogiPidaja(true, logDirAsStr + "vordlus" + outputSuffix);
			vordluseLog.setKirjutaLogiValjundisse(true);
			vordluseLog.println("--------------------------------------------------------------------------------");
			vordluseLog.println("  Testitulemuste kataloogis:\n  "+dir);
			vordluseLog.println("  Leiame erinevused kahe viimase tulemuse vahel:");
			// Sorteerime kuup2eva kasvamise j2rjekorda (ajastamp failinimes!)
			Arrays.sort( filesInCurrentDir );
			//
			// 1) Leiame k2esoleva testi tulemuste logifaili ning talle ajaliselt vahetult eelneva
			//    testi logifaili
			//
			int i = filesInCurrentDir.length - 1;
			String foundFirstFile = null;
			String foundSecndFile = null;
			while (i > 0){
				String fileName1 = filesInCurrentDir[i];
				if (lastFileName == null || (lastFileName).equals(fileName1)){
					int j = i - 1;
					if (j >= 0){
						String fileName2 = filesInCurrentDir[j];
						foundFirstFile = fileName1;
						foundSecndFile = fileName2;
					}
				}
				if (foundSecndFile != null){
					break;
				}
				i--;
			}
			//
			// 2) V6rdleme failide sisu omavahel, testifail-testifail haaval
			//
			if (foundFirstFile != null && foundSecndFile != null){
				vordluseLog.println("    "+foundSecndFile);
				vordluseLog.println("    "+foundFirstFile);
				vordluseLog.println("--------------------------------------------------------------------------------");
				vordluseLog.println();
				System.out.println("          ---> "+vordluseLog.getRaportiFailiNimi());
				String secondFileWithPath = logDir.getAbsolutePath() + File.separator + foundSecndFile;
				String firstFileWithPath  = logDir.getAbsolutePath() + File.separator + foundFirstFile;
				Abimeetodid.compareResultsFileByFile(secondFileWithPath, firstFileWithPath, encoding, vordluseLog);
			} else {
				vordluseLog.println(" Vorreldatavaid faile ei 6nnestunud leida! ");
			}
			vordluseLog.setKirjutaLogiFaili(false);
		} else {
			System.out.println();
			System.out.println(" Logifaile pole v6rdlemiseks piisaval arvul! ");
		}
	}

	/**
	 *  V6rdleme tulemusi fail-faili haaval ja kirjutame tulemused vordluslogisse.
	 */
	static void compareResultsFileByFile(String firstFile, String secondFile, String encoding, LogiPidaja vordluseLog){
		try {
			LinkedList<String> contentBlockInFirst = new LinkedList<String>();
			LinkedList<String> contentBlockInSecnd = new LinkedList<String>();
			LinkedList<String> fileNameInFirst = new LinkedList<String>();
			LinkedList<String> fileNameInSecnd = new LinkedList<String>();
			BufferedReader brFirst = new BufferedReader(new InputStreamReader(new FileInputStream(firstFile),  encoding));
			BufferedReader brSecnd = new BufferedReader(new InputStreamReader(new FileInputStream(secondFile), encoding));
			boolean proceed = true;
			String strLineInFirst;
			String strLineInSecnd;   
			while ( proceed ) {
				strLineInFirst = brFirst.readLine();
				if (strLineInFirst != null){
					// -------------------------------------------------------------------
					// ------------------    Kogume kokku failinime   --------------------
					// ------------------           esimeses          --------------------
					// -------------------------------------------------------------------
					if (fileNameInFirst.size() < 3){
						if (strLineInFirst.matches("-{46}") || fileNameInFirst.size() == 1){
							fileNameInFirst.add(strLineInFirst);
						}
					}
					// -------------------------------------------------------------------
					// ------------------    Leiame vastava failinime   ------------------
					// ------------------            teises             ------------------
					// -------------------------------------------------------------------
					if ( fileNameInFirst.size() == 3 && fileNameInSecnd.size() < 3 ){
						while ((strLineInSecnd = brSecnd.readLine()) != null){
							if (strLineInSecnd.matches("-{46}") || fileNameInSecnd.size() == 1){
								fileNameInSecnd.add( strLineInSecnd );
							}
							if (fileNameInSecnd.size() == 3){
								break;
							}
						}
						if ( fileNameInSecnd.size() != 3 || 
								!(fileNameInFirst.get(1)).equals(fileNameInSecnd.get(1)) ){
							System.err.println("Error: Unable to find");
							System.err.println(fileNameInFirst.get(1));
							System.err.println("in file "+secondFile);
							vordluseLog.println("Error: Unable to find");
							vordluseLog.println(fileNameInFirst.get(1));
							vordluseLog.println("in file "+secondFile);
							vordluseLog.setKirjutaLogiFaili(false);
							brFirst.close();
							brSecnd.close();
							System.exit(-1);
						}
					}
				} else {
					proceed = false;
				}
				// -------------------------------------------------------------------
				// ---------------    Kogume kokku v6rreldavad sisud   ---------------
				// -------------------------------------------------------------------
				if (fileNameInFirst.size() == 3 && fileNameInSecnd.size() == 3){
					for (String string : fileNameInFirst) {
						vordluseLog.println(string);
					}
					fileNameInFirst.clear();
					fileNameInSecnd.clear();
					// -------------------------------------------------------------------
					// ---------------------      Esimese   sisu        ------------------
					// -------------------------------------------------------------------
					while ((strLineInFirst = brFirst.readLine()) != null){
						if (strLineInFirst.matches("-{46}")){
							fileNameInFirst.add(strLineInFirst);
							break;
						} else {
							contentBlockInFirst.add(strLineInFirst);
						}
					}
					// -------------------------------------------------------------------
					// ---------------------       Teise    sisu        ------------------
					// -------------------------------------------------------------------
					while ((strLineInSecnd = brSecnd.readLine()) != null){
						if (strLineInSecnd.matches("-{46}")){
							fileNameInSecnd.add(strLineInSecnd);
							break;
						} else {
							contentBlockInSecnd.add(strLineInSecnd);
						}
					}
					// -------------------------------------------------------------------
					// -----------------------   Vordleme sisusid     --------------------
					// -------------------------------------------------------------------
					try {
						Patch patch = DiffUtils.diff( contentBlockInFirst, contentBlockInSecnd );
						for (Delta delta: patch.getDeltas()) {
							(vordluseLog.getRaportiVoog()).println("========= "+delta.getType()+" =========");
							int oldStart  = (delta.getOriginal()).getPosition();
							@SuppressWarnings("rawtypes")
							List oldLines = (delta.getOriginal()).getLines();
							int oldEnd    = (delta.getOriginal()).getPosition()+Math.max(oldLines.size()-1, 0);
							(vordluseLog.getRaportiVoog()).println("Old line(s) "+String.valueOf(oldStart)+"-"+String.valueOf(oldEnd)+"");
							for (int i = 0; i < oldLines.size(); i++) {
								(vordluseLog.getRaportiVoog()).println(" "+String.valueOf(oldStart+i)+": "+String.valueOf(oldLines.get(i)));
							}
							int newStart  = (delta.getRevised()).getPosition();
							@SuppressWarnings("rawtypes")
							List newLines = (delta.getRevised()).getLines();
							int newEnd    = (delta.getRevised()).getPosition()+Math.max(newLines.size()-1, 0);
							(vordluseLog.getRaportiVoog()).println("New line(s) "+String.valueOf(newStart)+"-"+String.valueOf(newEnd)+"");
							for (int i = 0; i < newLines.size(); i++) {
								(vordluseLog.getRaportiVoog()).println(" "+String.valueOf(newStart+i)+": "+String.valueOf(newLines.get(i)));
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					contentBlockInFirst.clear();
					contentBlockInSecnd.clear();
					// -------------------------------------------------------------------
					// -----   Kui j6uti l6ppu, kontrollime, kas m6lemas korraga  --------
					// -------------------------------------------------------------------
					if (strLineInFirst == null && strLineInSecnd != null){
						System.err.println("Error: "+firstFile+ " ends before "+secondFile+"! Unable to compare all.");
						vordluseLog.println("Error: "+firstFile+ " ends before "+secondFile+"! Unable to compare all.");
						vordluseLog.setKirjutaLogiFaili(false);
						brFirst.close();
						brSecnd.close();
						System.exit(-1);
					}
					if (strLineInFirst != null && strLineInSecnd == null){
						System.err.println("Error: "+secondFile+ " ends before "+firstFile+"! Unable to compare all.");
						vordluseLog.println("Error: "+secondFile+ " ends before "+firstFile+"! Unable to compare all.");
						vordluseLog.setKirjutaLogiFaili(false);
						brFirst.close();
						brSecnd.close();
						System.exit(-1);
					}
				}
			}
			brFirst.close();
			brSecnd.close();
		} catch (Exception e){ //Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}
	
	/**
	 *  Logifailide nimede filter.
	 *  
	 *  @author Siim Orasmaa
	 */
	static class TestLogFilenameFilter implements FilenameFilter {
		private String prefix = "";
		
		TestLogFilenameFilter(String prefix){
			this.prefix = prefix;
		}
		
		public boolean accept(File dir, String name) {
			return name.matches(prefix+"(_|[0-9]|-)+\\.txt");
		}
	};
	
	// =======================================================================================
	//        M o r f    a n a l y y s i    c a c h e ' i    h a l d a m i n e  
	// =======================================================================================
	
	/**
	 *   Loob etteantud TIMEX-annoteeritud korpusefaili (<code>timexFile</code>) nime põhjal vastava 
	 *  morfoloogilise analüüsi faili nime, mis asub kaustas <code>path</code>. Sisuliselt asendatakse
	 *  faili nimes laiend (tml) laiendiga (mrf). Kui <code>path == null</code>, pannakse uus fail
	 *  samasse kausta, kus on <code>timexFile</code>.
	 */
	public static File getMorphAnalysisFile(File timexFile, String path){
		if (timexFile.exists() && timexFile.isFile()){
			String name = timexFile.getName();
			name = name.replaceAll("\\.tml$", ".mrf");
			if (path != null){
				return new File(path, name);
			} else {
				return new File(timexFile.getParentFile(), name);				
			}
		}
		return null;
	}
	
	/**
	 *   Sooritab etteantud tekstil (<code>text</code>) morfoloogilise analyysi ja yhestamise,
	 *  kirjutab tulemused kodeeringus <code>kodeering</code> faili <code>file</code> ning 
	 *  yhtlasi tagastab tulemused s6nena.
	 */
	public static String performNewMorphologicalAnalysisAndSaveToFile(String text, 
			                                                            File file, 
			                                                     String kodeering, 
			                                               EstyhmmWrapper wrapper) throws Exception{
		text = EelTootlus.prepareTextForMorphAnalysis(text);
		String morfAnOutput = wrapper.process(text);
		FileOutputStream fileOutput = new FileOutputStream(file, false);
		PrintStream voog = new PrintStream ( fileOutput, true, kodeering );
		voog.println(morfAnOutput);
		voog.close();
		return morfAnOutput;
	}
	
	/**
	 *   Laeb morfoloogilise analyysi etteantud nimega failist.
	 */
	public static String fetchMorphologicalAnalysisFromFile(File file, String kodeering) throws Exception {
		if (file.exists() && file.isFile()){
	        FileInputStream in = new FileInputStream(file);
	        BufferedReader br = new BufferedReader(new InputStreamReader(in, kodeering));
	        StringBuilder sb = new StringBuilder();
	        String strLine;
	        while ((strLine = br.readLine()) != null) {
	        	sb.append(strLine);
	        	sb.append("\n");
	        }
	        in.close();
			return sb.toString();			
		} else {
			throw new Exception(" Unable to open file "+file.getAbsolutePath()+"!");
		}
	}
	
}
