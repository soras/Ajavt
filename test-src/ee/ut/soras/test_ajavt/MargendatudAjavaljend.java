//  Evaluation tools for Ajavt
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

package ee.ut.soras.test_ajavt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ee.ut.soras.ajavtV2.util.TextUtils;

/**
 *  Tekstis m&auml;rgendatud ajav&auml;ljend. V&otilde;ib olla nii automaatselt
 * kui ka k&auml;sitisi m&auml;rgendatud. Eesm&auml;rgiks kapseldada m&auml;rgendused,
 * et neid saaks hiljem v&otilde;rrelda (nt kas on tegu kattuvate m&auml;rgendustega;
 * millised on erisused semantika edasi andmisel jms).
 * 
 * @author Siim Orasmaa
 */
public class MargendatudAjavaljend {
	
	public static final int PHRASE_NOT_MATCHING         = 0;
	public static final int PHRASES_OVERLAP_PARTIALLY   = 1;
	public static final int PHRASE_IS_EXACTLY_MATCHING  = 2;	
	
	public static final Pattern tidPart                = Pattern.compile("TID=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern typePart               = Pattern.compile("TYPE=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern valuePart              = Pattern.compile("\\s+(VAL|VALUE)=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern modPart                = Pattern.compile("MOD=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern startPosPart           = Pattern.compile("startPosition=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern endPosPart             = Pattern.compile("endPosition=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern tokensPart             = Pattern.compile("tokens=\"([^\"]+)\"",  Pattern.CASE_INSENSITIVE);
	public static final Pattern commentPart            = Pattern.compile("COMMENT=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern beginPointPart         = Pattern.compile("beginPoint=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern endPointPart           = Pattern.compile("endPoint=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern anchorTimeIDPart       = Pattern.compile("anchorTimeID=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern functionInDocumentPart = Pattern.compile("functionInDocument=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern quantPart              = Pattern.compile("quant=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern freqPart               = Pattern.compile("freq=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
	public static final Pattern textPart               = Pattern.compile("text=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);	

	/*
	 * ==========================================================================
	 *    A t r i b u u d i d    j a    t e k s t i s    p a i k n e m i n e
	 * ========================================================================== 
	 */
	
	/**
	 *   Ajavaljendi alguspositsioon tekstis. 
	 */
	private int startPosition = -1;

	/**
	 *   Ajavaljendi lopp-positsioon tekstis. 
	 */
	private int endPosition = -1;
	
	/**
	 *   Ajavaljendifraas tekstis: TIMEX-m2rgendite vahele j22v s6ne. 
	 */
	private String fraas;

	/**
	 *   t3-olp sisendi puhul: kui <code>fraas</code> on ebat2pne, on siin t2psustatud,
	 *   millist alamosa <code>fraas</code>'ist ajav2ljend tegelikult katab. Tyhikud
	 *   s6nes m2rgivad erinevate token'ite piire. 
	 */
	private String tapsustatudFraas;
	
	/**
	 *   TIMEX3 atribuudid (tid, value, mod, ...) ja nende v22rtused. 
	 */
	private HashMap<String, String> attributeValues;

	/*
	 * ==========================================================================
	 *        L i n g i d     a j a v 2 l j e n d i t e    v a h e l
	 * ========================================================================== 
	 */
	
	private MargendatudAjavaljend beginPointLink = null;
	
	
	private MargendatudAjavaljend endPointLink = null;
		
	/**
	 *   Ajavaljendid, mille jaoks on kaesolev valjend <code>beginPoint</code> ja/v6i <code>endPoint</code>.
	 */
	private List<MargendatudAjavaljend> isBeginOrEndPointTo = null;

	/*
	 * ==========================================================================
	 *       L i n g i d    p a r a l l e e l m 2 r g e n d u s e g a
	 * ========================================================================== 
	 */
	
	/**
	 *    Milline ajav2ljend paralleelm2rgendusest (kas siis k2sitsim2rgendus v6i automaatm2rgendus)
	 *    vastab sellele ajav2ljendile?
	 */
	private MargendatudAjavaljend alignedTimex = null;
	
	
	public MargendatudAjavaljend(int startPosition, int endPosition, String tagHeader, String fraas){
		this.startPosition   = startPosition;
		this.endPosition     = endPosition;
		this.fraas           = fraas;
		this.attributeValues = new HashMap<String, String>();
		parsiTagHeaderistElemendid(tagHeader);
	}

	/**
	 *  Parsib ajavaljendi tag-i headerist elemendid MOD, VALUE jne.
	 */
	private void parsiTagHeaderistElemendid(String tagHeader){
		// ---------------- start position
		Matcher stPosFinder = startPosPart.matcher(tagHeader);
		if (stPosFinder.find() && stPosFinder.groupCount() > 0){
			String startPosAsString = stPosFinder.group(1);
			try {
				int pos = Integer.parseInt(startPosAsString);
				this.startPosition = pos;
			} catch (NumberFormatException e) {
				// Do nothing ...
			}
		}
		// ---------------- end position
		Matcher endPosFinder = endPosPart.matcher(tagHeader);
		if (endPosFinder.find() && endPosFinder.groupCount() > 0){
			String endPosAsString = endPosFinder.group(1);
			try {
				int pos = Integer.parseInt(endPosAsString);
				this.endPosition = pos;
			} catch (NumberFormatException e) {
				// Do nothing ...
			}
		}
		// ---------------- tokens
		Matcher tokensFinder = tokensPart.matcher(tagHeader);
		if (tokensFinder.find() && tokensFinder.groupCount() > 0){
			// Kui asukoht on antud token'ite j2rjekorranumbrite
			// kaudu, eeldame, et  v6ib  s6ne-positsiooni-p6hise  
			// asukoha  yle   kirjutada ...
			String tokensStr = tokensFinder.group(1);
			String [] tokens = tokensStr.split("\\s+");
			String firstTokenStr = tokens[0];
			String lastTokenStr  = tokens[tokens.length - 1];
			try {
				int firstToken = Integer.parseInt(firstTokenStr);
				int lastToken  = Integer.parseInt(lastTokenStr);
				this.startPosition = firstToken;
				this.endPosition   = lastToken;
			} catch (NumberFormatException e) {
				// Do nothing ...
			}
		}
		// ---------------- text
		Matcher textFinder = textPart.matcher(tagHeader);
		if (textFinder.find() && textFinder.groupCount() > 0 && this.fraas != null){
			// t3-olp sisendi puhul: t2psustatud ajav2ljendifraas
			this.tapsustatudFraas = textFinder.group(1);
		}
		// TODO: Siin saaks muidugi k6vasti refaktoreerida, 
		// praktiliselt yhe avaldisega korjata k6ik atribuudid & vaartused ...
		// ---------------- tid
		Matcher tidFinder = tidPart.matcher(tagHeader);
		if (tidFinder.find() && tidFinder.groupCount() > 0){
			(this.attributeValues).put("tid", tidFinder.group(1));
		}		
		// ---------------- type
		Matcher typeFinder = typePart.matcher(tagHeader);
		if (typeFinder.find() && typeFinder.groupCount() > 0){
			(this.attributeValues).put("type", typeFinder.group(1));
		}
		// ---------------- value
		Matcher valFinder = valuePart.matcher(tagHeader);
		if (valFinder.find() && valFinder.groupCount() > 1){
			(this.attributeValues).put("value", valFinder.group(2));
		}
		// ---------------- mod
		Matcher modFinder = modPart.matcher(tagHeader);
		if (modFinder.find() && modFinder.groupCount() > 0){
			(this.attributeValues).put("mod", modFinder.group(1));
		}
		// ---------------- comment
		Matcher commentFinder = commentPart.matcher(tagHeader);
		if (commentFinder.find() && commentFinder.groupCount() > 0){
			(this.attributeValues).put("comment", commentFinder.group(1));
		}
		// ---------------- beginPoint
		Matcher beginPointFinder = beginPointPart.matcher(tagHeader);
		if (beginPointFinder.find() && beginPointFinder.groupCount() > 0){
			(this.attributeValues).put("beginPoint", beginPointFinder.group(1));
		}
		// ---------------- endPoint
		Matcher endPointFinder = endPointPart.matcher(tagHeader);
		if (endPointFinder.find() && endPointFinder.groupCount() > 0){
			(this.attributeValues).put("endPoint", endPointFinder.group(1));
		}
		// ---------------- anchorTimeID
		Matcher anchorTimeIDFinder = anchorTimeIDPart.matcher(tagHeader);
		if (anchorTimeIDFinder.find() && anchorTimeIDFinder.groupCount() > 0){
			(this.attributeValues).put("anchorTimeID", anchorTimeIDFinder.group(1));
		}
		// ---------------- quant
		Matcher quantFinder = quantPart.matcher(tagHeader);
		if (quantFinder.find() && quantFinder.groupCount() > 0){
			(this.attributeValues).put("quant", quantFinder.group(1));
		}
		// ---------------- freq
		Matcher freqFinder = freqPart.matcher(tagHeader);
		if (freqFinder.find() && freqFinder.groupCount() > 0){
			(this.attributeValues).put("freq", freqFinder.group(1));
		}
		// ---------------- functionInDocument
		Matcher functionInDocumentFinder = functionInDocumentPart.matcher(tagHeader);
		if (functionInDocumentFinder.find() && functionInDocumentFinder.groupCount() > 0){
			(this.attributeValues).put("functionInDocument", functionInDocumentFinder.group(1));
		}		
	}
	

	/**
	 *    Kas jooksval ajav2ljendil on selle v2ljendiga v6rreldes
	 *   ylekattuvaid positsioone?
	 */
	public boolean haveOverlappingPositions(MargendatudAjavaljend av){
		if (!this.isEmptyTag() && !av.isEmptyTag()){
			// ------ Jaame teise margenduse algusesse
			if (this.startPosition < av.startPosition &&
				av.startPosition <= this.endPosition){
					return true;
			}
			// ------ Jaame teise m2rgenduse sisse
			if (av.startPosition <= this.startPosition && 
				this.endPosition <= av.endPosition){
				return true;
			}
			// ------ Teine m2rgendus j22b meie sisse
			if (this.startPosition <= av.startPosition && 
					av.endPosition <= this.endPosition){
					return true;
			}
			// ------ Jaame teise margenduse loppu
			if (av.startPosition < this.startPosition &&
				this.startPosition <= av.endPosition){
					return true;
			}			
		}
		return false;
	}
	
	/**
	 *   Leiab, kas antud väljend on sisuta väljend. Sisuta väljendid pole seotud ühegi konkreetse lõiguga tekstis
	 *   (nende asukoht tekstis ei mängi mingit rolli) ning teiste väljenditega one need seotud mitmesuguste 
	 *   ID-linkide kaudu;
	 */
	public boolean isEmptyTag(){
		return ((this.endPosition == this.startPosition) && this.fraas == null);
	}
	
	/**
	 *   <p>
	 *   Vordleb kaht m2rgendust m2rgendatud fraaside (st tekstilise kuju) alusel: kas fraasid on v6rdsed,
	 *   katavad yksteist kuidagi v6i ei sobitu yldse.
	 *   </p>
	 *   <p>
	 *   Kui lipp <code>kasutaTapsustatudFraasi</code> on seatud, kasutatakse v6rdluses <code>this.fraas</code>
	 *   asemel <code>this.tapsustatudFraas</code>'i (eeldusel, et viimane on mitte-null);
	 *   </p>
	 */
	public int findPhrasesMatchingState(MargendatudAjavaljend av, boolean kasutaTapsustatudFraasi){
		if (!this.isEmptyTag() && !av.isEmptyTag()){
			if (this.fraas != null && av.fraas != null){
				String thisPhrase  = TextUtils.trim(this.fraas);
				String otherPhrase = TextUtils.trim(av.fraas);
				// Vajadusel toetume t2psustatud fraasile
				if (kasutaTapsustatudFraasi && this.tapsustatudFraas != null){
					thisPhrase = TextUtils.trim(this.tapsustatudFraas);
				}
				if (thisPhrase.equals(otherPhrase)){
					return MargendatudAjavaljend.PHRASE_IS_EXACTLY_MATCHING;
				} else {
					// 1) Lihtne juht: yks fraas sisaldub teises
					if (thisPhrase.lastIndexOf(otherPhrase) > -1){
						return MargendatudAjavaljend.PHRASES_OVERLAP_PARTIALLY;
					} else if (otherPhrase.lastIndexOf(thisPhrase) > -1){
						return MargendatudAjavaljend.PHRASES_OVERLAP_PARTIALLY;
					}
					// 2) Keerulisem juht: fraasid sisaldavad yhisosa, aga kumbki ei sisaldu t2pselt teise sees
					//   
					//                    'juuli keskel'
					//                           'kolmas juuli'
					//
					// 2.1) Pyyame thisPhrase' suvaliselt positsioonilt alata otherPhrase'i 
					for (int i = 0; i < thisPhrase.length(); i++) {
						int j = 0, k = i;
						while (j < otherPhrase.length() && k < thisPhrase.length()){
							if (thisPhrase.substring(k, k+1).equals(otherPhrase.substring(j, j+1))){
								k++;
								j++;						
							} else {
								break;
							}
						}
						if (k == thisPhrase.length()){
							return MargendatudAjavaljend.PHRASES_OVERLAP_PARTIALLY;
						}
					}
					// 2.2) Pyyame otherPhrase' suvaliselt positsioonilt alata thisPhrase'i 
					for (int i = 0; i < otherPhrase.length(); i++) {
						int j = 0, k = i;
						while (j < thisPhrase.length() && k < otherPhrase.length()){
							if (otherPhrase.substring(k, k+1).equals(thisPhrase.substring(j, j+1))){
								k++;
								j++;						
							} else {
								break;
							}
						}
						if (k == otherPhrase.length()){
							return MargendatudAjavaljend.PHRASES_OVERLAP_PARTIALLY;
						}
					}			
				}			
			}			
		}
		return MargendatudAjavaljend.PHRASE_NOT_MATCHING;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("<");
		if (this.isEmptyTag()){
			sb.append("^");
		}
		if (this.startPosition > 0 && this.endPosition > 0){
			sb.append(this.startPosition);
			sb.append(":");
			sb.append(this.endPosition);			
		}
		sb.append(">");
		sb.append(" ");
		sb.append("(");
		sb.append( (this.attributeValues).get("type") );
		sb.append(":");
		// Kui on tegemist dokumendi loomise ajaga, toome selle eraldi välja, ning muid vaartuseid ei kuvagi
		if ((this.attributeValues).containsKey("functionInDocument") && 
				((this.attributeValues).get("functionInDocument")).equals("CREATION_TIME")){
			sb.append( (this.attributeValues).get("functionInDocument") );
		} else {
		// Kui pole tegemist dokumendi loomise ajaga, kuvame ka muud vaartused ...
			sb.append( (this.attributeValues).get("value") );
			if ((this.attributeValues).containsKey("mod")){
				sb.append(",");
				sb.append( (this.attributeValues).get("mod") );
			}
			if ((this.attributeValues).containsKey("quant")){
				sb.append(",");
				sb.append( (this.attributeValues).get("quant") );
			}
			if ((this.attributeValues).containsKey("freq")){
				sb.append(",");
				sb.append( (this.attributeValues).get("freq") );
			}		
		}
		sb.append(") ");
		if (this.fraas != null){
			sb.append(": '");
			if (this.tapsustatudFraas == null){
				String modifiedFraas = (this.fraas).replaceAll("\\s+"," ");
				sb.append(TextUtils.trim(modifiedFraas));				
			} else {
				sb.append( TextUtils.trim(this.tapsustatudFraas) );
			}
			sb.append("'");
			if (this.tapsustatudFraas != null){
				sb.append("{T}");
			}
		}
		return sb.toString();
	}

	public HashMap<String, String> getAttrValues(){
		return this.attributeValues;
	}

	/**
	 *  Kas kesolev ajav2ljend osaleb m6nes vahemikuseoses (<code>beginPoint</code> ja/v6i <code>endPoint</code> seoses)?
	 */
	public boolean osalebVahemikuSeostes(){
		return (this.isBeginOrEndPointTo != null && ((this.isBeginOrEndPointTo).size() > 0)) || (this.beginPointLink != null) || (this.endPointLink != null);
	}
	
	public void addIsBeginOrEndPointToLink(MargendatudAjavaljend parent){
		if (this.isBeginOrEndPointTo == null){
			this.isBeginOrEndPointTo = new ArrayList<MargendatudAjavaljend>(2);
		}
		(this.isBeginOrEndPointTo).add(parent);
	}
	
	public List<MargendatudAjavaljend> getIsBeginOrEndPointToTimexes(){
		return this.isBeginOrEndPointTo;
	}
	
	public MargendatudAjavaljend getBeginPointLink() {
		return this.beginPointLink;
	}

	/**
	 *    Loob kahepoolsed lingid: <code>beginPointLink</code> viit k2esolevalt v2ljendilt etteantud v2ljendile ning
	 *    <code>isBeginOrEndPointTo</code> link etteantud v2ljendilt k2esolevale v2ljendile. 
	 */
	public void setBeginPointLink(MargendatudAjavaljend beginPointLink) {
		this.beginPointLink = beginPointLink;
		beginPointLink.addIsBeginOrEndPointToLink(this);
	}

	public MargendatudAjavaljend getEndPointLink() {
		return this.endPointLink;
	}

	/**
	 *    Loob kahepoolsed lingid: <code>endPointLink</code> viit k2esolevalt v2ljendilt etteantud v2ljendile ning
	 *    <code>isBeginOrEndPointTo</code> link etteantud v2ljendilt k2esolevale v2ljendile. 
	 */
	public void setEndPointLink(MargendatudAjavaljend endPointLink) {
		this.endPointLink = endPointLink;
		endPointLink.addIsBeginOrEndPointToLink(this);
	}
	
	/**
	 *  Leiab, kas joondus paralleelmargendusega on vastastikune - st - valjend, millele kaesolev valjend viitab, viitab
	 *  tagasi kaesolevale valjendile.
	 */
	public boolean isAlignmentMutual(){
		return (this.alignedTimex != null && ((this.alignedTimex).alignedTimex != null) && ((this.alignedTimex).alignedTimex == this));
	}
	
	/**
	 *  Leiab, kas antud margendusega on seotud paralleelmargendus. Kui vastet (paralleelmargendust) leitud pole,
	 *  tagastab <code>false</code>. 
	 */
	public boolean hasAlignedTimex(){
		return (this.alignedTimex != null);
	}

	public boolean isCreationTime(){
		return ((this.attributeValues).containsKey("functionInDocument") && 
					((this.attributeValues).get("functionInDocument")).equals("CREATION_TIME"));
	}
	
	public MargendatudAjavaljend getAlignedTimex() {
		return this.alignedTimex;
	}

	public void setAlignedTimex(MargendatudAjavaljend alignedTimex) {
		this.alignedTimex = alignedTimex;
	}
	
	/**
	 *   Kui antud ajav2ljendiga on edukalt joondatud m6ni teine ajav2ljend ning m6lemad v2ljendid osalevad
	 *   mingites seostes, luuakse joondused vastavate seose implitsiitsete liikmete vahele (<code>beginPoint</code> 
	 *   ja <code>endPoint</code> linke j2rgides).    
	 */
	public void generateImplicitAlignments(){
		MargendatudAjavaljend theOtherTimex = this.alignedTimex;
		if (theOtherTimex != null){
			if (this.beginPointLink != null && (theOtherTimex).beginPointLink != null){
				if ((this.beginPointLink).isEmptyTag()){
					(this.beginPointLink).setAlignedTimex(theOtherTimex.beginPointLink);
				}
				if ((theOtherTimex.beginPointLink).isEmptyTag()){
					(theOtherTimex.beginPointLink).setAlignedTimex(this.beginPointLink);
				}
			}
			if (this.endPointLink != null && (theOtherTimex).endPointLink != null){
				if ((this.endPointLink).isEmptyTag()){
					(this.endPointLink).setAlignedTimex(theOtherTimex.endPointLink);
				}
				if ((theOtherTimex.endPointLink).isEmptyTag()){
					(theOtherTimex.endPointLink).setAlignedTimex(this.endPointLink);
				}
			}
			if (this.getIsBeginOrEndPointToTimexes() != null && theOtherTimex.getIsBeginOrEndPointToTimexes() != null){
				List<MargendatudAjavaljend> thisBeginOrEndPointToTimexes  = this.getIsBeginOrEndPointToTimexes();
				List<MargendatudAjavaljend> otherBeginOrEndPointToTimexes = theOtherTimex.getIsBeginOrEndPointToTimexes();
				// Kui m6lemas hulgas leidub yks tekstilise sisuta tag, seome need omavahel
				MargendatudAjavaljend thisParentTimex  = null;
				MargendatudAjavaljend otherParentTimex = null;
				for (MargendatudAjavaljend margendatudAjavaljend : thisBeginOrEndPointToTimexes) {
					if (margendatudAjavaljend.isEmptyTag()){
						if (thisParentTimex == null){
							thisParentTimex = margendatudAjavaljend;
						} else {
							// Kui yks tekstilise sisuta on juba leitud, siis siduda ei oska ...
							thisParentTimex = null;
							break;
						}
					}
					
				}
				for (MargendatudAjavaljend margendatudAjavaljend : otherBeginOrEndPointToTimexes) {
					if (margendatudAjavaljend.isEmptyTag()){
						if (otherParentTimex == null){
							otherParentTimex = margendatudAjavaljend;
						} else {
							// Kui yks tekstilise sisuta on juba leitud, siis siduda ei oska ...
							otherParentTimex = null;
							break;
						}
					}
					
				}
				if (thisParentTimex != null && otherParentTimex != null){
					thisParentTimex.setAlignedTimex(otherParentTimex);
					otherParentTimex.setAlignedTimex(thisParentTimex);
				}
			}
		}
	}

	public int getStartPosition() {
		return startPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

	public String getFraas() {
		return fraas;
	}
	
}
