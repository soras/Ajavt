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
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.json.Json;
import javax.json.stream.JsonGenerator;
import javax.json.stream.JsonGeneratorFactory;
import javax.json.stream.JsonParser;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.FraasisPaiknemiseKoht;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaObjekt;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaObjekt.TYYP;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaPunkt;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.TimeMLDateTimePoint;
import ee.ut.soras.ajavtV2.util.TextUtils;

/**
 * J&auml;relt&ouml;&ouml;tlus: joondamine morfoloogilise analysaatori v2ljundi ja esialgse teksti vahel
 * ning l6plik teksti m&auml;rgendamine p&auml;rast ajav&auml;ljendite tuvastamist. 
 * 
 * @author Siim Orasmaa
 */
public class JarelTootlus {
	
	//==============================================================================
	//   	J o o n d a m i n e    a l g t e k s t i g a
	//==============================================================================

	/**
	 *   Joondab etteantud listi <tt>sonad</tt> esialgse tekstiga. M&auml;&auml;rab igas <tt>AjavtSona</tt>-s,
	 *   millisel positsioonil esialgses tekstis see algab, ja millisel positsioonil l6peb.
	 *   <p>
	 *   Lihtsustatud versioon; võimaldab vahele j&auml;tta lõigud sõnes <code>esialgneTekst</code>, mida
	 *   listis <code>sonad</code> ei esine. 
	 */
	public static void joondaAlgseTekstiga( List<AjavtSona> sonad, String esialgneTekst ) throws Exception {
		if (sonad != null && sonad.size() > 0){
			int indexInText  = 0;
			for (int i = 0; i < sonad.size(); i++) {
				AjavtSona sona = sonad.get( i );
				int k = esialgneTekst.indexOf(sona.getAlgSona(), indexInText);
				if (k != -1){
					sona.setStartPosition(k);
					sona.setEndPosition(k + (sona.getAlgSona()).length() - 1);
					indexInText = k + (sona.getAlgSona()).length();
					//System.out.println( " >" + sona.getAlgSona()+ "|"+ esialgneTekst.substring(k, k+(sona.getAlgSona()).length())+"<left:"+(esialgneTekst.length()-indexInText)+","+(sonad.size()-i)+">");
				} else {
					if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
						sona.setStartPosition(indexInText);
						sona.setEndPosition(indexInText);
						//throw new Exception("After text position: "+indexInText+"\n Failed to align input text and output of morphological analyser.");						
						//System.out.println( "@" + indexInText + " |"+ sona.getAlgSona()+ "|"+ esialgneTekst.substring(indexInText)+"@");
						
					}
				}
			}
		}
	}

	//==============================================================================
	//   	T I M E X    I D - d e     p a r a n d a m i n e
	//==============================================================================
	/**
	 *   Kirjutab kõigi ajaväljendite TID-id (ja TID-sid kasutavad lingid) ümber selliselt, et TID-id  
	 *   jooksevad tekstis järjest. Kuna systeem loob ja kustutab töö käigus palju rohkem ajaväljendeid 
	 *   kui neid jõuab lõplikku väljundisse, on väljundisse sattuvate TIMEX-ite id-d utoopiliselt suured,
	 *   katkendliku ja üpris segase järjekorraga - käesoleva meetodi eesmärgiks on see viga parandada;
	 *   <p>
	 *   &Uuml;htlasi asendab anchorTimeID, beginPoint ja endPoint vaartused CREATION_TIME vaartustega
	 *   t0;
	 */
	public static void parandaTIDvaartused(List<AjavtSona> sonad, boolean allowOnlyPureTimeML) {
		HashMap<String, String> oldIDtoNewID = new HashMap<String, String>();
		// 1) Omistame k6igile ajav2ljenditele uued TID v22rtused
		int ID = 1;
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> 				ajavaljendid = sona.getAjavaljendiKandidaadid();
				for (int j = 0; j < ajavaljendiKandidaatides.size(); j++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(j);
					AjavaljendiKandidaat ajav = ajavaljendid.get(j);
					if (ajav.getSemantikaLahendus() != null){
						if (koht.onFraasiAlgus()){
							HashMap<String, String> mapOfAttributesAndValues = 
									(ajav.getSemantikaLahendus()).asHashMapOfAttributeValue("");
							if (mapOfAttributesAndValues.get("tid") != null && !(mapOfAttributesAndValues.get("tid")).equals("t0")){
								String oldID = mapOfAttributesAndValues.get("tid");
								String newID = "t"+(ID++);
								oldIDtoNewID.put(oldID, newID);
								(ajav.getSemantikaLahendus()).setTimex3Attribute("tid", newID);
							}
						}
						if (koht.onFraasiLopp()){
							if ((ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null){
								List<AjaObjekt> relatedImplicitTIMEXES = (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES();
								for (AjaObjekt ajaObjekt : relatedImplicitTIMEXES) {
									HashMap<String, String> mapOfAttributesAndValues = 
											(ajaObjekt).asHashMapOfAttributeValue("");
									if (mapOfAttributesAndValues.get("tid") != null && !(mapOfAttributesAndValues.get("tid")).equals("t0")){
										String oldID = mapOfAttributesAndValues.get("tid");
										String newID = "t"+(ID++);
										oldIDtoNewID.put(oldID, newID);
										(ajaObjekt).setTimex3Attribute("tid", newID);
									}
								}
							}
						}
					}   // if
				}  // for
			}  // if 
		} // for
		String linkingAttributes [] = new String [] {"beginPoint", "endPoint", "anchorTimeID"};
		// 2) Parandame beginPoint, endPoint ja anchorTimeID lingid
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> 				ajavaljendid = sona.getAjavaljendiKandidaadid();
				for (int j = 0; j < ajavaljendiKandidaatides.size(); j++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(j);
					AjavaljendiKandidaat ajav = ajavaljendid.get(j);
					if (ajav.getSemantikaLahendus() != null){
						if (koht.onFraasiAlgus()){
							HashMap<String, String> mapOfAttributesAndValues = 
									(ajav.getSemantikaLahendus()).asHashMapOfAttributeValue("");
							for (String attribute : linkingAttributes) {
								if (mapOfAttributesAndValues.get( attribute ) != null){
									String oldID = mapOfAttributesAndValues.get( attribute );
									if (oldIDtoNewID.containsKey(oldID)){
										(ajav.getSemantikaLahendus()).setTimex3Attribute( attribute, oldIDtoNewID.get(oldID) );									
									} else if (oldID.equals("CREATION_TIME")){
										(ajav.getSemantikaLahendus()).setTimex3Attribute( attribute, "t0" );
									} else {
										// Kusagil tekkis viga, ning vana ankrut pole v6imalik enam leida
										(ajav.getSemantikaLahendus()).setTimex3Attribute( attribute, "??" );
									}
								}							
							}
						}
						if (koht.onFraasiLopp()){
							if ((ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null){
								List<AjaObjekt> relatedImplicitTIMEXES = (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES();
								for (AjaObjekt ajaObjekt : relatedImplicitTIMEXES) {
									HashMap<String, String> mapOfAttributesAndValues = 
											(ajaObjekt).asHashMapOfAttributeValue("");
									for (String attribute : linkingAttributes) {
										if (mapOfAttributesAndValues.get( attribute ) != null){
											String oldID = mapOfAttributesAndValues.get( attribute );
											if (oldIDtoNewID.containsKey(oldID)){
												(ajaObjekt).setTimex3Attribute( attribute, oldIDtoNewID.get(oldID) );									
											} else if (oldID.equals("CREATION_TIME")){
												(ajaObjekt).setTimex3Attribute( attribute, "t0" );
											} else {
												// Kusagil tekkis viga, ning vana ankrut pole v6imalik enam leida												
												(ajav.getSemantikaLahendus()).setTimex3Attribute( attribute, "??" );
											}
										}							
									}
								}
							}
						}
					}   // if
				}  // for
			}  // if 
		} // for
	}
	
	//==============================================================================
	//   	V a l j u n d i    k o n s t r u e e r i m i n e
	//==============================================================================

	//==============================================================================
	//   	T a v a t e k s t    s i s e n d 
	//==============================================================================
	
	/**
	 *   Tagastab v&auml;ljundi, kus:
	 *   <ul>
	 *     <li> Esialgses tekstis on m&auml;rgendatud ajav&auml;ljendid;
	 *     <li> Lisaks ajavaljenditele on m&auml;rgendatud ka debug-info (leitud arvufraasid, ankurdus ja grammatilised ajad);
	 *   </ul>
	 */
	public static String eraldamiseTulemusDebug(List<AjavtSona> sonad, String esialgneTekst, boolean allowOnlyPureTimeML, AjaPunkt creationTime) {
		StringBuffer sb = new StringBuffer(esialgneTekst);
		for (int i = sonad.size()-1; i > -1; i--) {
			AjavtSona sona = sonad.get(i);
			StringBuilder newSuffix = new StringBuilder();
			StringBuilder newPrefix = new StringBuilder();
			// Debuggimiseks ainult: margime erinevaid leitud s6na tunnuseid ...
			if (sona.onVerb()){
				newSuffix.append("_V_");
			}
			if (sona.getGrammatilineAeg() != AjavtSona.GRAMMATILINE_AEG.MAARAMATA){
				newSuffix.append( "{"+sona.getGrammatilineAeg()+"}" );
			}
			if (sona.isOnPotentsiaalneVahemikuAlgus()){
				newSuffix.append( "-st" );
			}
			if (sona.isOnPotentsiaalneVahemikuLopp()){
				newSuffix.append( "-ni" );
			}
			if (sona.onLauseLopp()){
				newSuffix.append( "-LL" );
			}
			if (sona.onVoimalikTsiteeringuLoppVoiAlgus()){
				newSuffix.append( "-tsitaat" );
			}			
			// Konstrueerime arvsona
			if (sona.getArvSonaFraasis() != FraasisPaiknemiseKoht.PUUDUB){
				String intValStr = null;
				if (sona.getArvSonaTaisArvVaartus() != null){
					intValStr = String.valueOf( sona.getArvSonaTaisArvVaartus().intValue() );
				} else {
					intValStr = String.valueOf( sona.getArvSonaMurdArvVaartus().doubleValue() );
				}
				if (sona.getArvSonaFraasis() == FraasisPaiknemiseKoht.ALGUSES || 
						sona.getArvSonaFraasis() == FraasisPaiknemiseKoht.AINUSSONA){
					newPrefix.insert(0, "<NUMBER VAL=\"" + intValStr + "\">");
				}
				if (sona.getArvSonaFraasis() == FraasisPaiknemiseKoht.LOPUS || 
						sona.getArvSonaFraasis() == FraasisPaiknemiseKoht.AINUSSONA){
					newSuffix.append( "</NUMBER>" );
				}	
			}
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> 				ajavaljendid = sona.getAjavaljendiKandidaadid();
				for (int j = 0; j < ajavaljendiKandidaatides.size(); j++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(j);
					AjavaljendiKandidaat ajav = ajavaljendid.get(j);
					if (ajav.getSemantikaLahendus() != null){
						if (koht.onFraasiAlgus()){
							StringBuilder ajavAnnotationHeader = new StringBuilder();
							ajavAnnotationHeader.append( "<"+TextUtils.resizeString(ajav.toString(), 5, true).toUpperCase()+" " );
							ajavAnnotationHeader.append( "M_ID=\"" );
							ajavAnnotationHeader.append( ajav.getMustriID() );
							ajavAnnotationHeader.append( "\" " );
							if (ajav.getLahimVerb() != null){
								ajavAnnotationHeader.append( "VERB=\"" +(ajav.getLahimVerb()).getAlgSona()+ "\" " );
							}
							if (ajav.getAnkurdatudKandidaat() != null){
								if ((ajav.getAnkurdatudKandidaat() != null)){
									List<AjavtSona> fraas = 
										(ajav.getAnkurdatudKandidaat()).getFraas();
									ajavAnnotationHeader.append( "ANKUR=\"" );
									for (AjavtSona ajavtSona : fraas) {
										ajavAnnotationHeader.append( ajavtSona.getAlgSona() );
										ajavAnnotationHeader.append( " " );
									}
									ajavAnnotationHeader.append( "\" " );
								}
							}
							// TIMEX attributes: type, value, mod, ...
							HashMap<String, String> mapOfAttributesAndValues = 
									(ajav.getSemantikaLahendus()).asHashMapOfAttributeValue("");
							String orderedAttribsAndValues = orderAttributeValuePairs(mapOfAttributesAndValues);
							ajavAnnotationHeader.append( orderedAttribsAndValues );
							
							String mustriTahisedAsString = ajav.getMustriTahisedAsString();
							if (mustriTahisedAsString != null && mustriTahisedAsString.length() > 0){
								ajavAnnotationHeader.append( "TAHISED=\"" );
								ajavAnnotationHeader.append( mustriTahisedAsString );
								ajavAnnotationHeader.append( "\"" );
							}
							ajavAnnotationHeader.append( ">" );
							newPrefix.insert(0, " ");
							newPrefix.insert(0, ajavAnnotationHeader);
							
						}
						if (koht.onFraasiLopp()){
							newSuffix.append(" ");
							newSuffix.append( "</"+TextUtils.resizeString(ajav.toString(), 5, true).toUpperCase()+">" );
							// Implitsiitsete kuvamine
							if ((ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null){
								List<AjaObjekt> relatedImplicitTIMEXES = (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES();
								for (AjaObjekt ajaObjekt : relatedImplicitTIMEXES) {
									HashMap<String, String> mapOfAttributesAndValues = 
										(ajaObjekt).asHashMapOfAttributeValue("");
									String orderedAttribsAndValues = orderAttributeValuePairs(mapOfAttributesAndValues);
									newSuffix.append( "<TIMEX " );
									newSuffix.append( orderedAttribsAndValues );
									newSuffix.append( "/>" );
								}
							}
						}						
					}
				}
			}
			if (sona.getStartPosition() < sb.length() && sona.getEndPosition()+1 <= sb.length() && 
				   -1 < sona.getStartPosition() && -1 < sona.getEndPosition()){
				sb.insert(sona.getEndPosition()+1, newSuffix );
				sb.insert(sona.getStartPosition(), newPrefix );				
			}
		}
		if (creationTime != null){
			HashMap<String, String> mapOfAttributesAndValues = (creationTime).asHashMapOfAttributeValue("");
			String orderedAttribsAndValues = orderAttributeValuePairs(mapOfAttributesAndValues);
			String ctTIMEX = "<TIMEX "+orderedAttribsAndValues+"/>\n";
			sb.insert(0, ctTIMEX);
		}		
		return sb.toString();
	}
	
	/**
	 *   Tagastab v&auml;ljundi, kus:
	 *   <ul>
	 *     <li> Esialgses tekstis on m&auml;rgendatud ajav&auml;ljendid;
	 *     <li> V2lja on toodud ainult l6plik m2rgendamise tulemus;     
	 *   </ul>
	 */
	public static String eraldamiseTulemusPretty(List<AjavtSona> sonad, String esialgneTekst, boolean allowOnlyPureTimeML, AjaPunkt creationTime) {
		StringBuffer sb = new StringBuffer(esialgneTekst);
		for (int i = sonad.size()-1; i > -1; i--) {
			AjavtSona sona = sonad.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				String newPrefix = "";
				String newSuffix = "";
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> 				ajavaljendid = sona.getAjavaljendiKandidaadid();
				for (int j = 0; j < ajavaljendiKandidaatides.size(); j++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(j);
					AjavaljendiKandidaat ajav = ajavaljendid.get(j);
					if (koht.onFraasiAlgus()){
						if (ajav.getSemantikaLahendus() != null){
							HashMap<String, String> attribValuePairs = 
								(allowOnlyPureTimeML) ?
									(getPurifiedTimeMLAnnotation(ajav.getSemantikaLahendus())) : 
										((ajav.getSemantikaLahendus()).asHashMapOfAttributeValue(""));
							if (attribValuePairs != null){
								newPrefix = doTagHeader( null, attribValuePairs, allowOnlyPureTimeML, false ) + newPrefix;								
							}
						}
					}
					if (koht.onFraasiLopp()){
						if (ajav.getSemantikaLahendus() != null){
							HashMap<String, String> attribValuePairs = 
								(allowOnlyPureTimeML) ?
									(getPurifiedTimeMLAnnotation(ajav.getSemantikaLahendus())) : 
										((ajav.getSemantikaLahendus()).asHashMapOfAttributeValue(""));
							if (attribValuePairs != null){
								newSuffix = newSuffix + doTagFooter( allowOnlyPureTimeML );								
							}							
							// Implitsiitsete kuvamine							
							if ( (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null ){
								for (AjaObjekt ajaObjekt : (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES()) {
									HashMap<String, String> mapOfAttributesAndValues =
										(allowOnlyPureTimeML) ?
												(getPurifiedTimeMLAnnotation(ajaObjekt)) : 
													((ajaObjekt).asHashMapOfAttributeValue(""));
									if (mapOfAttributesAndValues != null){
										newSuffix += doTagHeader(null, mapOfAttributesAndValues, allowOnlyPureTimeML, true);
									}
								}
							}
						}
					}
				}
				if (sona.getStartPosition() < sb.length() && sona.getEndPosition()+1 <= sb.length()
						&& -1 < sona.getStartPosition() && -1 < sona.getEndPosition()){
					sb.insert(sona.getEndPosition() + 1, newSuffix);
					sb.insert(sona.getStartPosition(), newPrefix);
				}
			}
		}
		if (creationTime != null){
			HashMap<String, String> mapOfAttributesAndValues = (creationTime).asHashMapOfAttributeValue("");
			sb.insert(0, "\n");
			sb.insert(0, doTagHeader(null, mapOfAttributesAndValues, allowOnlyPureTimeML, true) );
		}
		return sb.toString();
	}
	
	/**
	 *   Tagastab tulemusena eraldatud ajav2ljendite listi. Listis on leitud ajav2ljendifraasid koos neid
	 *   ymbritsevate TIMEX-tag'idega.
	 */
	public static List<String> eraldamiseTulemusAinultValjendid(List<AjavtSona> sonad, String esialgneTekst, boolean allowOnlyPureTimeML, AjaPunkt creationTime) {
		List<String> list = new LinkedList<String>();
		if (creationTime != null){
			HashMap<String, String> mapOfAttributesAndValues = (creationTime).asHashMapOfAttributeValue("");
			list.add( doTagHeader(null, mapOfAttributesAndValues, allowOnlyPureTimeML, true) );
		}
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> 			    ajavaljendid = sona.getAjavaljendiKandidaadid();
				for (int j = 0; j < ajavaljendiKandidaatides.size(); j++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(j);
					AjavaljendiKandidaat ajav = ajavaljendid.get(j);
					if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.ALGUSES){
						if (ajav.getSemantikaLahendus() != null){
							// Kogume ajavaljendi kokku ...
							StringBuilder sb = new StringBuilder();
							int algusPositsioon = -1;
							int loppPositsioon  = -1;
							int k = 0;							
							List<AjavtSona> ajavaljendiFraas = ajav.getFraas();
							while (k < ajavaljendiFraas.size()){
								AjavtSona sonaFraasis = ajavaljendiFraas.get(k);
								if (k == 0){
									algusPositsioon = sonaFraasis.getStartPosition();
								}
								if (k == ajavaljendiFraas.size() - 1){
									loppPositsioon  = sonaFraasis.getEndPosition();
								}
								k++;
							}
							if (algusPositsioon != -1 && loppPositsioon != -1){
								String extraAttribs = 
									"startPosition=\""+
										String.valueOf(algusPositsioon)+"\""+" endPosition=\""+String.valueOf(loppPositsioon)+"\"";
								HashMap<String, String> mapOfAttributesAndValues =
									(allowOnlyPureTimeML) ?
											(getPurifiedTimeMLAnnotation((ajav.getSemantikaLahendus()))) : 
												(((ajav.getSemantikaLahendus())).asHashMapOfAttributeValue(""));				
								if (mapOfAttributesAndValues != null){
									sb.append( doTagHeader(extraAttribs, mapOfAttributesAndValues, allowOnlyPureTimeML, false) );
									if (algusPositsioon < esialgneTekst.length() && loppPositsioon+1 <= esialgneTekst.length()){
										String str = esialgneTekst.substring(algusPositsioon, loppPositsioon+1);
										sb.append(str);										
									}
									sb.append( doTagFooter(allowOnlyPureTimeML) );
									list.add( sb.toString() );
								}				
							}
							// Implitsiitsete kuvamine
							if ((ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null){
								List<AjaObjekt> relatedImplicitTIMEXES = (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES();
								for (AjaObjekt ajaObjekt : relatedImplicitTIMEXES) {
									HashMap<String, String> mapOfAttributesAndValues =
										(allowOnlyPureTimeML) ?
												(getPurifiedTimeMLAnnotation((ajaObjekt))) : 
													(((ajaObjekt)).asHashMapOfAttributeValue(""));
									if (mapOfAttributesAndValues != null){
										list.add( doTagHeader("", mapOfAttributesAndValues, allowOnlyPureTimeML, true) );
									}
								}
							}							
						}
					}
				}
			}
		}
		return list;
	}

	//==============================================================================
	//   	T a v a t e k s t     v õ i    t 3 - o l p     s i s e n d    
	//==============================================================================
	
	/**
	 *   Tagastab tulemusena eraldatud ajav2ljendite listi, kus iga eraldatud ajav&auml;ljendi esitatud paisktabeli kujul. 
	 *   Paisktabeli v6tmed vastavad TIMEX tag'i atribuudinimedele (nt <i>value</i>, <i>mod</i>, <i>type</i> jne), lisaks on
	 *   kasutusel kolm spetsiifilist atribuuti:
	 *   <ul>
	 *   <li> <i>text</i> (eraldatud ajav2ljendifraas);
	 *   <li> <i>startPosition</i> (ajav alguspositsioon tekstis, ainult siis kui joondus on l2bi viidud);
	 *   <li> <i>endPosition</i> (ajav l6pppositsioon tekstis, ainult siis kui joondus on l2bi viidud);
	 *   <li> <i>tokens</i> (ajav seotud token'ite loetelu, ainult siis kui token'ite asukohad on m22ratud);
	 *   </ul>
	 *   <p>
	 *   NB! Ajavaljendi joudmine tulemuslisti eeldab, et ajavaljendiga seotud sonad on vahemalt kas joondatud algse
	 *   tekstiga voi seotud token'ite positsioonidega. Kui kumbagi tehtud pole, ei joua ka ajavaljend tulemustesse.
	 *   <p>
	 *   NB! Mone atribuudi vaartuseks voib olla ka <i>null</i>, sellised tuleks loplikust valjundist valja jatta. 
	 */
	public static List<HashMap<String, String>> eraldamiseTulemusAinultValjendidPaistabelitena(List<AjavtSona> sonad, String esialgneTekst, boolean allowOnlyPureTimeML, AjaPunkt creationTime) throws Exception {
		List<HashMap<String, String>> list = new LinkedList<HashMap<String, String>>();
		if (creationTime != null){
			HashMap<String, String> mapOfAttributesAndValues = (creationTime).asHashMapOfAttributeValue("");
			list.add( mapOfAttributesAndValues );
		}
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> 		    	ajavaljendid = sona.getAjavaljendiKandidaadid();
				for (int j = 0; j < ajavaljendiKandidaatides.size(); j++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(j);
					AjavaljendiKandidaat ajav = ajavaljendid.get(j);
					if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.ALGUSES){
						if (ajav.getSemantikaLahendus() != null){
							// Kogume andmed ajavaljendi esinemispositsiooni kohta
							// t2pne esinemispositsioon sisends6nes:
							int algusPositsioonString = -1;
							int loppPositsioonString  = -1;
							// token positsioon sisendis:
							int algusPositsioonToken  = -1;
							int loppPositsioonToken   = -1;
							// token positsioon systeemisiseses tokeniseerimises:
							int algusPositsioon_token = -1;
							int loppPositsioon_token  = -1;
							int k = 0;							
							List<AjavtSona> ajavaljendiFraas = ajav.getFraas();
							StringBuilder tokenPhrase = new StringBuilder();
							while (k < ajavaljendiFraas.size()){
								AjavtSona sonaFraasis = ajavaljendiFraas.get(k);
								tokenPhrase.append( sonaFraasis.getAlgSona() );
								if (k == 0){
									algusPositsioonString = sonaFraasis.getStartPosition();
									algusPositsioonToken  = sonaFraasis.getTokenPosition();
									algusPositsioon_token = sonaFraasis.getInnerTokenPosition();
								}
								if (k == ajavaljendiFraas.size() - 1){
									loppPositsioonString  = sonaFraasis.getEndPosition();
									loppPositsioonToken   = sonaFraasis.getTokenPosition();
									loppPositsioon_token  = sonaFraasis.getInnerTokenPosition();
								} else {
									tokenPhrase.append(" ");
								}
								k++;
							}
							// A) Positsioon on s6ne-t2psusega tekstis m22ratletud
							if (algusPositsioonString != -1 && loppPositsioonString != -1 ){
								// Leiame TIMEX atribuudid/v22rtused: type, value, mod ...
								HashMap<String, String> mapOfAttributesAndValues =
									(allowOnlyPureTimeML) ?
											(getPurifiedTimeMLAnnotation((ajav.getSemantikaLahendus()))) : 
												(((ajav.getSemantikaLahendus())).asHashMapOfAttributeValue(""));
								if (mapOfAttributesAndValues != null){
									// Lisame ylej22nud atribuudid: startPosition, endPosition, text
									mapOfAttributesAndValues.put("startPosition", String.valueOf(algusPositsioonString));
									mapOfAttributesAndValues.put("endPosition",    String.valueOf(loppPositsioonString));
									if (algusPositsioonString < esialgneTekst.length() && loppPositsioonString+1 <= esialgneTekst.length()){
										mapOfAttributesAndValues.put("text", esialgneTekst.substring(algusPositsioonString, loppPositsioonString+1) );										
									}
									list.add( mapOfAttributesAndValues );									
								}
							} else if (algusPositsioonToken != -1 && loppPositsioonToken != -1){
							// B) Positsioon on token-t2psusega tekstis m22ratletud
								// Leiame TIMEX atribuudid/v22rtused: type, value, mod ...
								HashMap<String, String> mapOfAttributesAndValues =
									(allowOnlyPureTimeML) ?
											(getPurifiedTimeMLAnnotation((ajav.getSemantikaLahendus()))) : 
												(((ajav.getSemantikaLahendus())).asHashMapOfAttributeValue(""));
								if (mapOfAttributesAndValues != null){
									// Konstrueerime token'ite loenduse stringi algse sisendi joonduseks
									StringBuilder tokensStr = new StringBuilder();
									for (int l = algusPositsioonToken; l <= loppPositsioonToken; l++) {
										tokensStr.append(l);
										if (l != loppPositsioonToken){ tokensStr.append(" "); }
									}
									mapOfAttributesAndValues.put( "tokens", tokensStr.toString() );
									// Konstrueerime token'ite loenduse stringi systeemisiseseks joonduseks
									tokensStr = new StringBuilder();
									for (int l = algusPositsioon_token; l <= loppPositsioon_token; l++) {
										tokensStr.append(l);
										if (l != loppPositsioon_token){ tokensStr.append(" "); }
									}
									mapOfAttributesAndValues.put( "_tokens", tokensStr.toString() );									
									// Konstrueerime fraasi
									if (tokenPhrase.length() > 0){
										mapOfAttributesAndValues.put("text", tokenPhrase.toString() );										
									}
									list.add( mapOfAttributesAndValues );									
								}
							} else {
								throw new Exception(" Unable to position temporal expression starting from word "+i+" "+sona.getAlgSona()+"");
							}
							
							// Implitsiitsete ehk ilma konkreetse positsioonita v2ljendite lisamine
							if ((ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null){
								List<AjaObjekt> relatedImplicitTIMEXES = (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES();
								for (AjaObjekt ajaObjekt : relatedImplicitTIMEXES) {
									HashMap<String, String> mapOfAttributesAndValues =
										(allowOnlyPureTimeML) ?
												(getPurifiedTimeMLAnnotation((ajaObjekt))) : 
													(((ajaObjekt)).asHashMapOfAttributeValue(""));
									if (mapOfAttributesAndValues != null){
										list.add( mapOfAttributesAndValues );								
									}
								}
							}							
						}
					}
				}
			}
		}
		return list;
	}
	
	/**
	 *  Konverteerib paisktabelite kujul antud eraldamise tulemuse s6nede listi kujul antud eraldamise tulemuseks.
	 *  <p>
	 *  Lipp <code>useDebugTokens</code> m2rgib seda, kas token-p6hine positsioneerimine peaks k2ima 
	 *  systeemisisese tokeniseerimise j2rgi (true) v6i tuleks kasutada sisendip6hist tokeniseerimist (false); 
	 */
	public static List<String> konverteeriEraldamiseTulemusSonedeListiks(List<HashMap<String, String>> eraldamiseTulemusPaistabelitena,
																		 boolean allowOnlyPureTimeML,
																		 boolean useDebugTokens){
		List<String> list = new LinkedList<String>();
		for (HashMap<String, String> timexAsAttribMap : eraldamiseTulemusPaistabelitena) {
			StringBuilder sb = new StringBuilder();
			String algusPositsioon = timexAsAttribMap.get("startPosition");
			String loppPositsioon  = timexAsAttribMap.get("endPosition");
			String tokens          = (useDebugTokens) ? (timexAsAttribMap.get("_tokens")) : (timexAsAttribMap.get("tokens"));
			String text            = timexAsAttribMap.get("text");
			if (text != null){
				String extraAttribs = "";
				if (algusPositsioon != null && loppPositsioon != null){
					extraAttribs = "startPosition=\""+algusPositsioon+"\""+" endPosition=\""+loppPositsioon+"\"";
				} else if (tokens != null){
					extraAttribs = "tokens=\""+tokens+"\"";
				}
				sb.append( doTagHeader(extraAttribs, timexAsAttribMap, allowOnlyPureTimeML, false) );
				sb.append( text );
				sb.append( doTagFooter(allowOnlyPureTimeML) );
				list.add( sb.toString() );
			} else {
				list.add( doTagHeader("", timexAsAttribMap, allowOnlyPureTimeML, true) );
			}
		}
		return list;
	}
	
	/**
	 *   Tagastab v&auml;ljundi, kus T3-OLP sisend on esitatud kujul:
	 *   <ul>
	 *     <li> iga lause eraldi real ning m2rgendatud on ajav2ljendid
	 *     <li> eemaldatud on lausestaja märgendid ja lausepiiride märgendid
	 *     <li> eemaldatud on ignore-märgendid     
	 *   </ul>
	 */
	public static String eraldamiseTulemusPrettyT3OLP(List<AjavtSona> sonad, AjaPunkt creationTime, boolean usePurifiedTimeML) {
		// V2ga umbkaudne hinnang suurusele
		StringBuilder valjund = new StringBuilder(sonad.size() * 20);
		if (creationTime != null){
			HashMap<String, String> mapOfAttributesAndValues = (creationTime).asHashMapOfAttributeValue("");
			valjund.append( doTagHeader("", mapOfAttributesAndValues, false, true) );
			valjund.append("\n");
		}
		for (AjavtSona sona : sonad) {
			// Konstrueerime sona
			StringBuilder sonaStr = new StringBuilder( sona.getAlgSona() );
			// Debuggimiseks ainult: margime, kas on tegu verbi, vahemiku alguse v6i lopuga
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> 						ajavaljendid = sona.getAjavaljendiKandidaadid();
				for (int i = 0; i < ajavaljendiKandidaatides.size(); i++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(i);
					AjavaljendiKandidaat ajav = ajavaljendid.get(i);
					if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.ALGUSES){
						if (ajav.getSemantikaLahendus() != null){
							StringBuilder ajavAnnotationHeader = 
								new StringBuilder("");
							
							// TIMEX attributes/values: type, value, mod, ...
							HashMap<String, String> mapOfAttributesAndValues = 
								(ajav.getSemantikaLahendus()).asHashMapOfAttributeValue("");
							ajavAnnotationHeader.append( doTagHeader(null, mapOfAttributesAndValues, usePurifiedTimeML, false) );
							
							sonaStr.insert(0, ajavAnnotationHeader + "");
						}
					}
					if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.LOPUS){
						if (ajav.getSemantikaLahendus() != null){
							sonaStr.append(doTagFooter(usePurifiedTimeML));
							// Implitsiitsete kuvamine
							if (ajav.getSemantikaLahendus() != null && (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null){
								List<AjaObjekt> relatedImplicitTIMEXES = (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES();
								for (AjaObjekt ajaObjekt : relatedImplicitTIMEXES) {
									HashMap<String, String> mapOfAttributesAndValues = 
										(ajaObjekt).asHashMapOfAttributeValue("");
									sonaStr.append(doTagHeader(null, mapOfAttributesAndValues, usePurifiedTimeML, true));
								}
							}							
						}
					}					
				}
			}
			valjund.append( sonaStr );
			if (sona.onLauseLopp()){
				valjund.append( "\n" );
			} else {
				valjund.append(" ");
			}
		}
		return valjund.toString();
	}
	
	/**
	 *   Tagastab v&auml;ljundi, kus T3-OLP sisend on esitatud kujul:
	 *   <ul>
	 *     <li> kui <code>skipIgnorePart == true</code>, siis on eemaldatud ignore-margendid koos sisuga
	 *          ning ilma sisuta m2rgendid on paigutatud, nagu need TimeML m2rgendi j2rgi peaksid olema.
	 *          Vastasel juhul kasutatakse ilma sisut m2rgendite paigutamisel viisi, mis peaks korpuse
	 *          t88tlust lihtsustama.
	 *     <li> alles on lausepiiride jms margendid
	 *     <li> iga token on eraldi real
	 *     <li> margendatud on ajavaljendid
	 *   </ul>
	 */
	public static String eraldamiseTulemusT3OLPEraldiReal(String sisendT3OLP, List<AjavtSona> sonad, AjaPunkt creationTime, 
															boolean usePurifiedTimeML, boolean skipIgnorePart) throws Exception {
		// V2ga umbkaudne hinnang suurusele
		StringBuilder valjund = new StringBuilder(sonad.size() * 20);
		if (creationTime != null){
			HashMap<String, String> mapOfAttributesAndValues = (creationTime).asHashMapOfAttributeValue("");
			valjund.append( doTagHeader("", mapOfAttributesAndValues, false, true) );
			valjund.append("\n");
		}
		// Loome mappingu tokenPosition'ite ja AjavtSona'de vahel
		// NB! Kuna yhele sisendi token'ile v6ib vastata mitu systeemisisest token'it, 
		// siis peame ka m2ppingud tegema listi, mitte yksikusse ajavaljendisse
		HashMap<Integer, List<AjavtSona>> tokenPosToAjavtSonaMap = new HashMap<Integer, List<AjavtSona>>();
		for (AjavtSona sona : sonad) {
			if (sona.getTokenPosition() != -1){
				int key = sona.getTokenPosition();
				if (!tokenPosToAjavtSonaMap.containsKey(key)){
					List<AjavtSona> listOfWords = new ArrayList<AjavtSona>(1);
					tokenPosToAjavtSonaMap.put(key, listOfWords);
				}
				(tokenPosToAjavtSonaMap.get(key)).add(sona);
			} else {
				throw new Exception(" Unable to map AjavtSona to tokenposition for "+sona.getAlgSona());
			}
		}
		// Kammime sisenditeksti l2bi, j2tame vahele ignore osad ning pistame sobivasse kohta vahele
		// ajav2ljendid
		String rida       = null;
		int tokenPosition   = 1;
		HashMap<String, Integer> timexTagStartPos          = new HashMap<String, Integer>();
		HashMap<Integer, List<String>> implicitDurStartPos = new HashMap<Integer, List<String>>();
		BufferedReader input = new BufferedReader( new StringReader(sisendT3OLP) );
		while ((rida = input.readLine()) != null){
			// ---------------------------------------------------------------------------------------
			//  Jätame vahele ignoreeritava osa
			// ---------------------------------------------------------------------------------------
			if (rida.length() > 0 && rida.equals("<ignoreeri>")){
				if (!skipIgnorePart){
					valjund.append(rida);
					valjund.append("\n");
				}
				tokenPosition++;
				while ((rida = input.readLine()) != null){
					if (!skipIgnorePart){
						valjund.append(rida);
						valjund.append("\n");
					}
					tokenPosition++;
					if (rida.length() > 0 && rida.equals("</ignoreeri>")){
						break;
					}
				}
				continue;
			}
			// ---------------------------------------------------------------------------------------
			//  Sisu
			// ---------------------------------------------------------------------------------------
			if (rida.length() > 0){
				StringBuilder sonaStr = new StringBuilder(rida);
				sonaStr.append("\n");
				
				if (tokenPosToAjavtSonaMap.containsKey(tokenPosition)){
					List<AjavtSona> sonadPositsioonil = tokenPosToAjavtSonaMap.get( tokenPosition );
					for (AjavtSona sona : sonadPositsioonil) {
						// -------------------------------------------------------------------------------
						//  Kas tegemist on ajav2ljendiga?
						// -------------------------------------------------------------------------------
						if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
							List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
							List<AjavaljendiKandidaat> 		    	ajavaljendid = sona.getAjavaljendiKandidaadid();
							for (int i = 0; i < ajavaljendiKandidaatides.size(); i++) {
								FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(i);
								AjavaljendiKandidaat ajav = ajavaljendid.get(i);
								if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.ALGUSES){
									if (ajav.getSemantikaLahendus() != null){
										StringBuilder ajavAnnotationHeader = 
											new StringBuilder("");
										
										// TIMEX attributes/values: type, value, mod, ...
										HashMap<String, String> mapOfAttributesAndValues = 
											(ajav.getSemantikaLahendus()).asHashMapOfAttributeValue("");
										// Kui yhele sisendi token'ile vastab mitu ajavt token'it, tuleb
										// t2psustada, millist alams6net m6eldakse. Teeme seda spetsiaalse
										// atribuudi "text" kaudu.
										String attribText = null;
										if (ajav.hasTokenBreakPositions()){
											attribText = doTextAttributeForTimex( ajav );
										}
										if (attribText == null){
											attribText = "";
										}
										attribText += " mustriID=\""+ajav.getMustriID()+"\" ";
										ajavAnnotationHeader.append( 
												doTagHeader(attribText, mapOfAttributesAndValues, usePurifiedTimeML, false) );
										sonaStr.insert(0, ajavAnnotationHeader + "\n");
										timexTagStartPos.put( mapOfAttributesAndValues.get("tid") , valjund.length()-1);
										// TODO: v6ib juhtuda, et lisatakse valjundisse enne salvestatavat positsiooni,
										// seega positsioon "aegub" ega ole enam korrektne
									}
								}
								if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.LOPUS){
									if (ajav.getSemantikaLahendus() != null){
										// Lisame implitsiitsed m2rgendid ning sulgeme v2ljendi
										addImplicitTimexesIfNecessaryAndCloseTimex(
												skipIgnorePart, usePurifiedTimeML, ajav, sonaStr, valjund, timexTagStartPos, 
												implicitDurStartPos);
									}
								}					
							}
						}
					}
				}
				valjund.append(sonaStr);
				tokenPosition++;
			}
		}
		// Lisame implitsiitsete kestvuste algustag'id, tekstipositsioonide kahanemise j2rjekorras
		if (!implicitDurStartPos.isEmpty()){
			List<Integer> startPosList = new ArrayList<Integer>( (implicitDurStartPos.keySet()) );
			Collections.sort(startPosList);
			for (int i = startPosList.size()-1; i > -1; i--) {
				Integer key = startPosList.get(i);
				List<String> startTags = implicitDurStartPos.get(key);
				for (String tag : startTags) {
					valjund.insert(key, tag);
				}
			}
		}
		return valjund.toString();
	}
	
	/**
	 *   Tagastab v&auml;ljundi, kus JSON kujul vabamorfi v2ljundile on lisatud ajav2ljendim2rgendused.
	 */
	public static String eraldamiseTulemusVabaMorfiJSON(String sisendJSON, List<AjavtSona> sonad, AjaPunkt creationTime, 
															boolean usePurifiedTimeML, boolean prettyPrint) throws Exception {
		// JSON-i generaatori loomine
		StringWriter sw = new StringWriter();
		Map<String, Object> properties = new HashMap<String, Object>(1);
		if (prettyPrint){
	        properties.put(JsonGenerator.PRETTY_PRINTING, true);			
		}
        JsonGeneratorFactory jgf = Json.createGeneratorFactory(properties);
		JsonGenerator jsonGenerator = jgf.createGenerator( sw );
		// vana JSON-i sisu parser
		BufferedReader inputReader = new BufferedReader( new StringReader(sisendJSON) );
		JsonParser parser = Json.createParser( inputReader );
		
		// JSON-i v6tmete rada ( k6ige pealmine on vaadeldav v6ti... )
		Stack<String> jsonKeyPath = new Stack<String>(); 
		// Viimane event
		JsonParser.Event lastEvent = null;
		// Viimasele event'ile vastav v22rtus (kui on)
		String lastString = "";
		
		// jooksva s6na tekstikuju
		String tokenText = null;
		
		// Referentsaeg: teeme kindlaks, kas v2ljundisse tuleb lisada teistsugune, kui oli sisendis
		String newDCTvalue = null;
		String konehetkJSONist [] = Main.prooviLuuaJSONsisendiP6hjalRefAeg(sisendJSON);
		// Kui JSON sisendis polnud referentsaega antud v6i see ei sobinud, paneme uue referentsaja
		if (konehetkJSONist == null){
	       HashMap<String, String> mapOfAttributesAndValues = (creationTime).asHashMapOfAttributeValue("");
	       newDCTvalue = mapOfAttributesAndValues.get("value");
		}
		
		// Loome mappingu tokenPosition'ite ja AjavtSona'de vahel
		// NB! Kuna yhele sisendi token'ile v6ib vastata mitu systeemisisest token'it, 
		// siis peame ka m2ppingud tegema listi, mitte yksikusse ajavaljendisse
		HashMap<Integer, List<AjavtSona>> tokenPosToAjavtSonaMap = new HashMap<Integer, List<AjavtSona>>();
		for (AjavtSona sona : sonad) {
			if (sona.getTokenPosition() != -1){
				int key = sona.getTokenPosition();
				if (!tokenPosToAjavtSonaMap.containsKey(key)){
					List<AjavtSona> listOfWords = new ArrayList<AjavtSona>(1);
					tokenPosToAjavtSonaMap.put(key, listOfWords);
				}
				(tokenPosToAjavtSonaMap.get(key)).add(sona);
			} else {
				throw new Exception(" Unable to map AjavtSona to tokenposition for "+sona.getAlgSona());
			}
		}
		int currentTokenPosition = 1;
		while (parser.hasNext()) {
			   JsonParser.Event event = parser.next();
			   switch(event) {
			      case START_ARRAY:
			    	  if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
			    		  jsonKeyPath.push( lastString );
			    		  jsonGenerator.writeStartArray( lastString );
			    	  } else {
			    		  jsonGenerator.writeStartArray();
			    	  }
				      lastEvent = event;
				      lastString = "";
			    	  break;
			      case END_ARRAY:
				      // ------------------------------------------------------------------
			    	  //   Sõna-järjendi lõpp: märgime, et viimane sõna lõpetas lause
				      // ------------------------------------------------------------------
				      if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals("words")){
				    	  
				      }
				      lastEvent = event;
				      lastString = "";
			    	  jsonKeyPath.pop();
			    	  jsonGenerator.writeEnd();
			    	  break;
			      case START_OBJECT:
			    	  if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
			    		  jsonGenerator.writeStartObject( lastString );
			    	  } else {
			    		  jsonGenerator.writeStartObject();
			    	  }
				      // ------------------------------------------------------------------
			    	  //   Kogu dokumendi algus: lisame kasutatud teksti loomise aja
				      // ------------------------------------------------------------------			    	  
			    	  if (jsonKeyPath.empty() && newDCTvalue != null){
			    		  jsonGenerator.write("dct", newDCTvalue );
			    	  }
				      // ------------------------------------------------------------------
			    	  //   Sõna-objekti algus
				      // ------------------------------------------------------------------
			    	  if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals("words")){
				      }
				      // ------------------------------------------------------------------
			    	  //   Analüüs-objekti algus
				      // ------------------------------------------------------------------
                      if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals("analysis")){
				      }
				      lastEvent = event;
				      lastString = "";
				      break;
			      case END_OBJECT:
				      // ------------------------------------------------------------------
			    	  //   Sõna-objekti lõpp
				      // ------------------------------------------------------------------
				      if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals( "words")){
				    	  if (tokenText == null){
				    		  throw new Exception(" Unable to find text form for a word ...");  
				    	  }
				    	  Integer positionKey = Integer.valueOf(currentTokenPosition);
				    	  if (tokenPosToAjavtSonaMap.containsKey( positionKey )){
				    		  List<AjavtSona> sonadPositsioonil = tokenPosToAjavtSonaMap.get(positionKey);
					    	  List<WritableTimexForJSON> associatedTimexes = new ArrayList<WritableTimexForJSON>();
							  HashMap<String, Integer> addedTimexIDs = new HashMap<String, Integer>();
				    		  for ( AjavtSona sona : sonadPositsioonil ) {
									// -------------------------------------------------------------------------------
									//  Kas tegemist on ajav2ljendiga?
				    			    //  Korjame kokku k6ik token'iga seotud ajavaljendid
									// -------------------------------------------------------------------------------
									if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
										List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
										List<AjavaljendiKandidaat> 		    	ajavaljendid = sona.getAjavaljendiKandidaadid();
										for (int i = 0; i < ajavaljendiKandidaatides.size(); i++) {
											FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(i);
											AjavaljendiKandidaat ajav = ajavaljendid.get(i);
											if (ajav.getSemantikaLahendus() != null){
												HashMap<String, String> mapOfAttributesAndValues =
														(usePurifiedTimeML) ?
																(getPurifiedTimeMLAnnotation((ajav.getSemantikaLahendus()))) : 
																	(((ajav.getSemantikaLahendus())).asHashMapOfAttributeValue(""));
												if (mapOfAttributesAndValues != null && mapOfAttributesAndValues.containsKey("tid")){
													//
													//   NB! Kui tuvastaja tokeniseering erineb sisendi omast (yhele sisendi
													//  s6nele vastab mitu tuvastaja s6net ), tekib siin oht, et v2ljastame
													//  yhe ajav2ljendi mitu korda (mitme tuvastaja s6ne korral); Selle 
													//  v2ltimiseks j2tame ajavaljendite ID-d meelde ja v2ljastame vaid 
													//  unikaalse ID-ga ajav2ljendid;
													//
													String TID = mapOfAttributesAndValues.get("tid");
													if (!addedTimexIDs.containsKey(TID)){
														addedTimexIDs.put( TID, 1 );
														associatedTimexes.add( new WritableTimexForJSON(ajav, mapOfAttributesAndValues, koht) );
													}
												}
											}
										}
									}
				    		  }
				    		  // Kirjutame k6ik token'iga seotud ajav2ljendid JSON objektideks
				    		  if (!associatedTimexes.isEmpty()){
				    			  jsonGenerator.writeStartArray("timexes");
				    			  for (WritableTimexForJSON writableJSON : associatedTimexes) {
				    				  writableJSON.writeAsJSONObject( jsonGenerator, usePurifiedTimeML );
								  }
				    			  jsonGenerator.writeEnd();
				    		  }
				    	  } else {
				    		  throw new Exception(" Unable to find AjavtSona associated with the word "+tokenText+" at position "+currentTokenPosition+"...");
				    	  }
				    	  currentTokenPosition++;
				    	  tokenText = null;
				      }
				      // ------------------------------------------------------------------
			    	  //   Analüüsi-objekti lõpp
				      // ------------------------------------------------------------------
				      if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals( "analysis")){
				      }
				      lastEvent = event;
				      lastString = "";
				      jsonGenerator.writeEnd();
				      break;
			      case VALUE_FALSE:
			    	  if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
						 jsonGenerator.write(lastString, false);
					  } else {
					     jsonGenerator.write(false);
					  }
				      lastEvent = event;
				      lastString = "";
				      break;
			      case VALUE_NULL:
			    	  if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
					     jsonGenerator.writeNull(lastString);
				      } else {
					     jsonGenerator.writeNull();
					  }
				      lastEvent = event;
				      lastString = "";
				      break;
			      case VALUE_TRUE:
			    	  if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
						 jsonGenerator.write(lastString, true);
					  } else {
					     jsonGenerator.write(true);
					  }
				      lastEvent = event;
				      lastString = "";
				      break;
			      case KEY_NAME:
			         lastEvent  = event;
			         lastString = parser.getString();
			         break;
			      case VALUE_STRING:
			    	  if (newDCTvalue != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME && 
			    			lastString.equalsIgnoreCase( "dct")){
			    		  // Kui DCT-le on m22ratud uus v22rtus, siis j2tame vana DCT ymberkirjutamata
			    		  lastEvent  = event;
					      lastString = parser.getString();
					      break;
			    	  }
			    	  if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
					      jsonGenerator.write(lastString, parser.getString());
				      } else {
				    	  jsonGenerator.write(parser.getString());
				      }
				      // ---------------------------------------------------
				      //   Jätame meelde sõna tekstilise kuju ...
				      // ---------------------------------------------------
				      if (!jsonKeyPath.empty() && (jsonKeyPath.peek()).equals( "words")){
					     if (lastString != null && lastString.equals("text")){
					        tokenText = parser.getString();
					     }			        	 
				      }
				      lastEvent  = event;
				      lastString = parser.getString();
				      break;
			      case VALUE_NUMBER:
			    	 if (lastEvent != null && lastEvent == javax.json.stream.JsonParser.Event.KEY_NAME){
				    	 if (parser.isIntegralNumber()){
				    		 jsonGenerator.write(lastString, parser.getLong());
				    	 } else {
				    		 jsonGenerator.write(lastString, parser.getBigDecimal());
				    	 }
			    	 } else {
			    		 if (parser.isIntegralNumber()){
			    			 jsonGenerator.write(parser.getLong());
			    		 } else {
			    			 jsonGenerator.write(parser.getBigDecimal());
			    		 }
			    	 }
			         lastEvent  = event;
			         lastString = "";
			         break;
			   }
		}
		jsonGenerator.close();
		inputReader.close();
		return sw.toString();
	}
	

	/**
	 *   Lisame implitsiitsed m2rgendid (kui neid leidub) ning sulgeme avatud ajav2ljendi. 
	 *   <br>
	 *   (t3-olp-ajav valjundi spetsiifiline)
	 */
	public static void addImplicitTimexesIfNecessaryAndCloseTimex(boolean skipIgnorePart, 
																  boolean usePurifiedTimeML, 
																  AjavaljendiKandidaat ajav,
																  StringBuilder sonaStr,
																  StringBuilder valjund,
																  HashMap<String, Integer> timexTagStartPos, 
																  HashMap<Integer, List<String>> implicitDurPos) throws Exception {
		//
		//  Implitsiitsete v2ljendite (ehk tekstilise sisuta ajav2ljendite) kuvamine
		//   
		if (skipIgnorePart){
			// Kuvame sisuta m2rgendid sellise strukuuri ja paigutusega, nagu need on TimeML m2rgenduses
			sonaStr.append(doTagFooter(usePurifiedTimeML));
			sonaStr.append("\n");
			if (ajav.getSemantikaLahendus() != null && (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null){
				List<AjaObjekt> relatedImplicitTIMEXES = (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES();
				for (AjaObjekt ajaObjekt : relatedImplicitTIMEXES) {
					HashMap<String, String> mapOfAttributesAndValues = 
						(ajaObjekt).asHashMapOfAttributeValue("");
					String attribText = " mustriID=\""+ajav.getMustriID()+"\" ";
					sonaStr.append( doTagHeader(attribText, mapOfAttributesAndValues, usePurifiedTimeML, true) );
					sonaStr.append("\n");													
				}
			}
		} else {
			// Kuvame sisuta m2rgendid selliselt, et koondkorpuse t88tluse peaks olema nii k6ige lihtsam
			List<HashMap<String, String>> implicitDurations = null;
			// B.1) K6igepealt ilma sisuta mittekestvused
			if (ajav.getSemantikaLahendus() != null && (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null){
				List<AjaObjekt> relatedImplicitTIMEXES = (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES();
				for (AjaObjekt ajaObjekt : relatedImplicitTIMEXES) {
					HashMap<String, String> mapOfAttributesAndValues = 
						(ajaObjekt).asHashMapOfAttributeValue("");
					// implitsiitsed mittekestvused kuvame m2rgenduse sees
					if (ajaObjekt.getType() != TYYP.DURATION){
						String attribText = " mustriID=\""+ajav.getMustriID()+"\" ";
						sonaStr.append( doTagHeader(attribText, mapOfAttributesAndValues, usePurifiedTimeML, true) );
						sonaStr.append("\n");													
					} else {
						// Implitsiitsed kestvused j2tame esialgu lihtsalt meelde ...
						implicitDurations = new ArrayList<HashMap<String, String>>();
						implicitDurations.add(mapOfAttributesAndValues);
					}
				}
			}
			sonaStr.append(doTagFooter(usePurifiedTimeML));
			sonaStr.append("\n");
			// B.2) Seej2rel ilma sisuta kestvused - need muudame "sisuga" kestvusteks, pannes m2rgendid ymbritsema
			// linkides beginPoint ja endPoint viidatud ajav2ljendeid ...
			if (implicitDurations != null){
				for (HashMap<String, String> implDuration : implicitDurations) {
					String beginPoint = implDuration.get("beginPoint");
					if (beginPoint != null && timexTagStartPos.containsKey(beginPoint)){
						int startPos = timexTagStartPos.get(beginPoint);
						//
						// NB! Me ei saa algustag'i kohe teksti panna: kattuvate implitsiitsete kestvuste korral 
						// v6ib yhe paikapanek teise positsiooni nihutada ning tag satub valele kohale (nt s6na keskele).
						// Seet6ttu ainult salvestame algustag'i asukoha.
						// 
						Integer key = startPos+1;
						if (!implicitDurPos.containsKey(key)){
							implicitDurPos.put(key, new ArrayList<String>());
						}
						String attribText = " mustriID=\""+ajav.getMustriID()+"\" ";
						(implicitDurPos.get(key)).add( doTagHeader(attribText, implDuration, usePurifiedTimeML, false)+"\n" );
						// Kestvuse l6pptag l2heb siiski paika
						sonaStr.append(doTagFooter(usePurifiedTimeML));
						sonaStr.append("\n");
					} else {
						throw new Exception (" Unable to locate beginPoint, specified in timex "+implDuration.get("tid")+".");
					}
				}				
			}
		}
	}
	
	
	/**
	 *   Tagastab v&auml;ljundi, kus:
	 *   <ul>
	 *     <li> Iga sona on eraldi real;
	 *     <li> Margendatud on ka debug-info (leitud arvufraasid ning k6ik leitud ajavaljendikandidaadid);
	 *   </ul>
	 */
	public static String eraldamiseTulemusIgaSonaEraldiRealDebug(List<AjavtSona> sonad, AjaPunkt creationTime) {
		// V2ga umbkaudne hinnang suurusele
		StringBuilder valjund = new StringBuilder(sonad.size() * 20);
		if (creationTime != null){
			HashMap<String, String> mapOfAttributesAndValues = (creationTime).asHashMapOfAttributeValue("");
			valjund.append( doTagHeader("", mapOfAttributesAndValues, false, true) );
			valjund.append("\n");
		}
		// Ehk - kui mitu tyhikut tuleb antud ajavaljendi kuvamisel taanduseks lisada...		
		for (AjavtSona sona : sonad) {
			// Konstrueerime sona
			StringBuilder sonaStr = new StringBuilder( sona.getAlgSona() );
			// Debuggimiseks ainult: margime, kas on tegu verbi, vahemiku alguse v6i lopuga
			if (sona.onVerb()){
				sonaStr.append( "_V_" );
			}
			if (sona.getGrammatilineAeg() != AjavtSona.GRAMMATILINE_AEG.MAARAMATA){
				sonaStr.append( "{" );
				sonaStr.append( sona.getGrammatilineAeg() );
				sonaStr.append( "}" );
			}			
			if (sona.isOnPotentsiaalneVahemikuAlgus()){
				sonaStr.append( "-st" );
			}
			if (sona.isOnPotentsiaalneVahemikuLopp()){
				sonaStr.append( "-ni" );
			}
			if (sona.onLauseLopp()){
				sonaStr.append( "-LL" );
			}
			if (sona.onVoimalikTsiteeringuLoppVoiAlgus()){
				sonaStr.append( "-tst" );
			}
			// Konstrueerime arvsona
			if (sona.getArvSonaFraasis() != FraasisPaiknemiseKoht.PUUDUB){
				String intValStr = null;
				if (sona.getArvSonaTaisArvVaartus() != null){
					intValStr = String.valueOf( sona.getArvSonaTaisArvVaartus().intValue() );
				} else {
					intValStr = String.valueOf( sona.getArvSonaMurdArvVaartus().doubleValue() );
				}
				if ((sona.getArvSonaFraasis()).onFraasiAlgus()){
					sonaStr.insert(0, "<NUMBER VAL=\"" + intValStr + "\">");
				}
				if ((sona.getArvSonaFraasis()).onFraasiLopp()){
					sonaStr.append( "</NUMBER>" );
				}	
			}
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> 						ajavaljendid = sona.getAjavaljendiKandidaadid();
				for (int i = 0; i < ajavaljendiKandidaatides.size(); i++) {
					FraasisPaiknemiseKoht koht = ajavaljendiKandidaatides.get(i);
					AjavaljendiKandidaat ajav = ajavaljendid.get(i);
					if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.ALGUSES){
						StringBuilder ajavAnnotationHeader = 
							new StringBuilder("<"+TextUtils.resizeString(ajav.toString(), 5, true).toUpperCase()+" ");
						//if (ajav.getMustriID() != null){
						//	ajavAnnotationHeader.append( "MID=\""+ajav.getMustriID()+"\" ");
						//}
						if (ajav.getLahimVerb() != null){
							ajavAnnotationHeader.append( "VERB=\""+(ajav.getLahimVerb()).getAlgSona()+"\" " );
						}						
						if (ajav.getAnkurdatudKandidaat() != null){
							if ((ajav.getAnkurdatudKandidaat() != null)){
								List<AjavtSona> fraas = 
									(ajav.getAnkurdatudKandidaat()).getFraas();
								ajavAnnotationHeader.append( "ANKUR=\"" );
								for (AjavtSona ajavtSona : fraas) {
									ajavAnnotationHeader.append( ajavtSona.getAlgSona()+" " );
								}
								ajavAnnotationHeader.append( "\" " );
							}
						}
						if (ajav.getSemantikaLahendus() != null){
							
							// TIMEX attributes/values: type, value, mod, ...
							HashMap<String, String> mapOfAttributesAndValues = 
								(ajav.getSemantikaLahendus()).asHashMapOfAttributeValue("");
							String orderedAttribsAndValues = orderAttributeValuePairs(mapOfAttributesAndValues);
							ajavAnnotationHeader.append( orderedAttribsAndValues ); 

						}
						String mustriTahisedAsString = ajav.getMustriTahisedAsString();
						if (mustriTahisedAsString != null && mustriTahisedAsString.length() > 0){
							ajavAnnotationHeader.append( "TAHISED=\"" );
							ajavAnnotationHeader.append( mustriTahisedAsString );
							ajavAnnotationHeader.append( "\"" );
						}						
						ajavAnnotationHeader.append( ">" );
						if (ajav.getSemantikaEhitusklotsid() != null){
							for (SemantikaDefinitsioon semDef : ajav.getSemantikaEhitusklotsid()) {
								ajavAnnotationHeader.append( "\n      "+semDef.toString() );
							}
						}
						sonaStr.insert(0, ajavAnnotationHeader + "\n");
					}
					if (koht == FraasisPaiknemiseKoht.AINUSSONA || koht == FraasisPaiknemiseKoht.LOPUS){
						sonaStr.append("\n" + "</"+TextUtils.resizeString(ajav.toString(), 5, true).toUpperCase()+">");
						// Implitsiitsete kuvamine
						if (ajav.getSemantikaLahendus() != null && (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null){
							List<AjaObjekt> relatedImplicitTIMEXES = (ajav.getSemantikaLahendus()).getRelatedImplicitTIMEXES();
							for (AjaObjekt ajaObjekt : relatedImplicitTIMEXES) {
								HashMap<String, String> mapOfAttributesAndValues = 
									(ajaObjekt).asHashMapOfAttributeValue("");
								String orderedAttribsAndValues = orderAttributeValuePairs(mapOfAttributesAndValues);
								sonaStr.append("\n");
								sonaStr.append( "<TIMEX " );
								sonaStr.append( orderedAttribsAndValues );
								sonaStr.append( "/>" );
							}
						}						
					}					
				}
			}
			valjund.append( sonaStr );
			valjund.append("\n");
		}
		return valjund.toString();
	}
	
	/**
	 *   TIMEX3 atribuudid sellises järjekorras, nagu näeb ette TimeML standard. Esialgu on loetletud vaid
	 *   need atribuudid, mis on realiseeritud käesolevas ajaväljendite tuvastajas.
	 *   
	 *   NB! Atribuut 'tokens' on lisatud.
	 */
	final public static String orderOfTIMEX3Attributes [] = {
		"tid", "type", "functionInDocument", "temporalFunction", "value", "mod", "anchorTimeID", "beginPoint", "endPoint", "quant", "freq" 
	};
	
	/**
	 *   Abimeetod, mis seab tabelis <code>attribValuePairs</code> olevad atribuudid nende harjumusp2rasesse esinemisj2rjekorda 
	 * ning tagastab s6nena, mille v6ib lisada TIMEX3 tag'i headerisse; Jätab välja atribuudid, mille väärtuseks on 
	 * <code>null</code>. 
	 */
	public static String orderAttributeValuePairs(HashMap<String, String> attribValuePairs) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < orderOfTIMEX3Attributes.length; i++) {
			String attrib = orderOfTIMEX3Attributes[i];
			if (attribValuePairs.containsKey(attrib) && attribValuePairs.get(attrib) != null){
				sb.append(attrib);
				sb.append("=\"");
				sb.append(attribValuePairs.get(attrib));
				sb.append("\" ");
			}
		}
		return sb.toString();
	}
	
	/**
	 *   Tagastab ajaväljendi märgenduse, mis on kooskõlas standardiga TimeML. Kui see ei onnestu (nt on puudu moni oluline atribuut - <code>value</code> voi <code>type</code>),
	 *  tagastab <code>null</code>, mis tahendab, et ajavaljend kuulub kustutamisele.
	 */
	public static HashMap<String, String> getPurifiedTimeMLAnnotation(AjaObjekt ajav){
		HashMap<String, String> attribValuePairs = ajav.asHashMapOfAttributeValue("");
		if (!attribValuePairs.containsKey("type") || !attribValuePairs.containsKey("value")){
			return null;
		}
		if (attribValuePairs.containsKey("mod") && (attribValuePairs.get("mod")).equals("FIRST_HALF")){
			attribValuePairs.put("mod", "START");
		}
		if (attribValuePairs.containsKey("mod") && (attribValuePairs.get("mod")).equals("SECOND_HALF")){
			attribValuePairs.put("mod", "END");
		}
		if (attribValuePairs.containsKey("beginPoint") && 
				!((TextUtils.validTimeMLtid).matcher(attribValuePairs.get("beginPoint"))).matches()){
			attribValuePairs.remove("beginPoint");
		}
		if (attribValuePairs.containsKey("endPoint") && 
				!((TextUtils.validTimeMLtid).matcher(attribValuePairs.get("endPoint"))).matches()){
			attribValuePairs.remove("endPoint");
		}
		if (attribValuePairs.containsKey("anchorTimeID") && 
				!((TextUtils.validTimeMLtid).matcher(attribValuePairs.get("anchorTimeID"))).matches()){
			attribValuePairs.remove("anchorTimeID");
		}
		if (attribValuePairs.containsKey("freq")){
			if ((attribValuePairs.get("freq")).matches("T?[0-9]+(Y|M|D|H)")){
				attribValuePairs.put("freq", "P"+(attribValuePairs.get("freq")) );		
			} else {
				attribValuePairs.remove("freq");
			}
		}		
		String value = attribValuePairs.get("value");
		if (value.matches(".*-W.*-WD.*")){
			attribValuePairs.put("value", value.replaceAll("-WD", "-X") );
		}
		if (value.matches(".*-W.*-WET.*")){
			attribValuePairs.put("value", value.replaceAll("-WE", "-X") );
		}		
		if (value.matches(".*TXX.*")){
			return null;
		}
		//if (value.matches("BC.*")){
		//	return null;
		//}		
		return attribValuePairs;
	}
	
	/**
	 *  Moodustab XML-atribuudi <code>text</code>, mis sisaldab ajav2ljendit poolt kaetud fraasi
	 *  (s6net) t2ies ulatuses; Kui fraas koosneb mitmest token'ist, on nende vahel eraldajateks
	 *  tyhikud.
	 */
	public static String doTextAttributeForTimex(AjavaljendiKandidaat ajav){
		StringBuilder additionStr = new StringBuilder();
		List<AjavtSona> fraas = ajav.getFraas();
		additionStr.append("text=");
		additionStr.append("\"");
		for (int j = 0; j < fraas.size(); j++) {
			AjavtSona ajavtSona = fraas.get(j);
			additionStr.append( ajavtSona.getAlgSona() );
			if (j < fraas.size()-1){
				additionStr.append( " " );
			}
		}
		additionStr.append("\"");
		return additionStr.toString();
	}
	
	/**
	 *   Vormindab etteantud ajahetke kui dokumendi loomise kuupaeva. Kokkuleppeliselt
	 *   saab loodava ajapunkti identifikaatoriks "t0";
	 */
	public static AjaPunkt formatAsCreationTime(String [] konehetk){
		AjaPunkt creationTime = new AjaPunkt( TYYP.POINT, new TimeMLDateTimePoint(konehetk) );
		if (creationTime.getDateTimePoint() != null){
			(creationTime.getDateTimePoint()).copyInputFieldsToOpenedFields();  // avame k6ik granulaarsused
		}
		creationTime.setTimex3Attribute("temporalFunction", null);
		creationTime.setTimex3Attribute("tid", "t0");
		creationTime.setTimex3Attribute("functionInDocument", "CREATION_TIME");
		return creationTime;
	}
	
	/**
	 *  Tagastab ajav2ljendi m2rgendi esimese osa (header'i); Lisa-atribuutide (<code>extraAttribs</code>) olemasolul pannakse 
	 *  need m2rgendi algusesse;
	 */
	public static String doTagHeader(String extraAttribs, HashMap<String, String> attribValuePairs, boolean usePurifiedTimeML, boolean doEmptyTag){
		StringBuilder sb = new StringBuilder();
		sb.append("<TIMEX");
		if (usePurifiedTimeML){
			sb.append("3");	
		}		
		sb.append(" ");
		if (extraAttribs != null){
			sb.append( extraAttribs );
			sb.append(" ");			
		}
		sb.append( orderAttributeValuePairs( attribValuePairs ) );
		if (doEmptyTag){
			sb.append("/");	
		}
		sb.append(">");
		return sb.toString();
	}

	/**
	 *  Tagastab ajav2ljendi m2rgendi teise osa (footer'i); 
	 */
	public static String doTagFooter(boolean usePurifiedTimeML){
		StringBuilder sb = new StringBuilder();
		sb.append("</TIMEX");
		if (usePurifiedTimeML){
			sb.append("3");	
		}
		sb.append(">");
		return sb.toString();
	}
	
}
