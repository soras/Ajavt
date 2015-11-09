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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import ee.ut.soras.ajavtV2.util.TextUtils;

/**
 *  Testimistulemuste vektor, mis koondab enda <i>n</i> dokumendi testimisel saadud tulemusi.
 *  Tulemused salvestatakse atribuutide kaupa. V6imaldab salvestada nii yhe konkreetse 
 *  testjuhtumi tulemusi kui ka agregeerida tulemusi yle paljude testjuhtude.
 *  <p>
 *  Hindamine toimub TERN-stiilis: iga TIMEX atribuudi kohta tuuakse eraldi v2lja saagis ja t2psus. N2ide:
 *  <p>
 * <pre>
                       Saagis:             Tapsus:
 TIMEX            12/12 (100%)        12/12 (100%)
 EXTENT         11/12 (91.67%)      11/12 (91.67%)
 TYPE             12/12 (100%)        12/12 (100%)
 VALUE              6/12 (50%)          6/12 (50%)
 * </pre>
 *  
 * @author Siim Orasmaa
 */
public class TestimisTulemusTERN {
	
	/**
	 *   Eraldamist kirjeldavate atribuutide nimed.
	 */
	public final static String [] ERALDAMISE_TAHISED = { "TIMEX", "EXTENT" };

	/**
	 *   Normaliseerimist kirjeldavate atribuutide nimed.
	 */
	public final static String [] NORMALISEERIMISE_TAHISED = { "TYPE", "VALUE", "MOD", "VALUE2", "MOD2", "QUANT", "FREQ" };
	
	// ========================================================
	//     D o k u m e n d i    s t a t i s t i k a
	// ========================================================
	
	/**
	 *   Mitme testimise koondtulemusi see vektor sisaldab?
	 */
	private int documentsTested = 1;

	/**
	 *   Sonaloendusstatistika: mitu s6na (yle k6igi dokumentide) on testimise k2igus l2bitud?
	 */
	private int wordCount       = 0;
	
	/**
	 *   Mitu ajavaljendit oli kommentaaridega? 
	 */
	private int commentCount    = 0;
	
	// ========================================================
	//     M a r g e n d i t e    l o e n d u s e d
	// ========================================================
	
	/**
	 *   Mitu eraldamist olid korrektsed?
	 */
	private HashMap<String, Double> correctlyRecognized = new HashMap<String, Double>();
	
	/**
	 *   Mitu korrektset eraldamist oli yldse v6imalik teha? 
	 */
	private HashMap<String, Double> relevantItems       = new HashMap<String, Double>();

	/**
	 *   Mitu eraldamist tehti? (korrektsed ja mittekorrektsed koos) 
	 */
	private HashMap<String, Double> itemsExtracted      = new HashMap<String, Double>();
	
	// ==========================================================
	//     S a a g i s e d    &    t a p s u s e d
	// ==========================================================

	/**
	 *   Eraldamise saagis. 
	 */
	private HashMap<String, Double> recallOfExtraction          = new HashMap<String, Double>();
	
	/**
	 *   Eraldamise tapsus. 
	 */
	private HashMap<String, Double> precisionOfExtraction       = new HashMap<String, Double>();
	
	/**
	 *  Kas lisaks saagisele ja tapsusele tuleb arvutada ja kuvada ka F-skoor? 
	 *  Vaikimisi ei tule.
	 */
	private boolean reportFScore = false;

	/**
	 *   Eraldamise F-skoor (saagise ja tapsuse koondnaitaja). 
	 */
	private HashMap<String, Double> fscoreOfExtraction = new HashMap<String, Double>();
	
	//==============================================================================
	//   	T u l e m u s - a r v u t u s e d
	//==============================================================================
	
	/**
	 *   Arvutab tulemused/m66dikud (saagised, tapsused) seniste loendusandmete p6hjal. 
	 */
	public void calculateResults(){
	    /*
	 	*   Recall = number of relevant items recieved divided by all relevant items that were available;
	 	*      Here: Number of correctly recognized divided by the number of all correct items available;
	 	*/
		for (Iterator<String> iterator = (relevantItems.keySet()).iterator(); iterator.hasNext();) {
			String type = (String) iterator.next();
			if (relevantItems.get(type) < 1.0){
				recallOfExtraction.put(type, -1.0);
			} else {
				if (correctlyRecognized.containsKey(type)){
					double recall = correctlyRecognized.get(type) / relevantItems.get(type); 
					recallOfExtraction.put(type, recall);
				} else {
					recallOfExtraction.put(type, 0.0);
				}
			}
		}
	    /*
	 	 *   Precision = number of relevant items recieved divided by the number of all items recieved;
	 	 *      Here: Number of correctly recognized divided by the number of all recognized items;
	 	 */
		for (Iterator<String> iterator = (itemsExtracted.keySet()).iterator(); iterator.hasNext();) {
			String type = (String) iterator.next();
			if (itemsExtracted.get(type) < 1.0){
				precisionOfExtraction.put(type, -1.0);
			} else {
				if (correctlyRecognized.containsKey(type)){
					double precision = correctlyRecognized.get(type) / itemsExtracted.get(type); 
					precisionOfExtraction.put(type, precision);
				} else {
					precisionOfExtraction.put(type, 0.0);
				}
			}
		}
		// Ylej22nud: paneme v22rtused N/A ehk -1.0
		for (Iterator<String> iterator = (relevantItems.keySet()).iterator(); iterator.hasNext();) {
			String type = (String) iterator.next();
			if (!precisionOfExtraction.containsKey(type)){
				precisionOfExtraction.put(type, -1.0);
			}
		}
		if (reportFScore){
		    /*
		 	 *   F-measure = a weighted average of the precision and recall - the harmonic mean of 
		 	 *   precision and recall: 2 * prec * rec / (prec + rec)
		 	 */
			for (Iterator<String> iterator = (relevantItems.keySet()).iterator(); iterator.hasNext();) {
				String type = (String) iterator.next();
				if (precisionOfExtraction.containsKey(type) && recallOfExtraction.containsKey(type)){
					if (precisionOfExtraction.get(type) > -1.0 && recallOfExtraction.get(type) > -1.0 && 
						(precisionOfExtraction.get(type) > 0.0 || recallOfExtraction.get(type) > 0.0)){
						double rec  = recallOfExtraction.get(type);
						double prec = precisionOfExtraction.get(type);
						double fscore = 2 * prec * rec / (prec + rec);
						fscoreOfExtraction.put(type, fscore);
					}
				}
				if (!fscoreOfExtraction.containsKey(type)){
					fscoreOfExtraction.put(type, -1.0);
				}
			}	
		}
	}

	//==============================================================================
	//   	T e s t i m i s t u l e m u s t e     a g r e g e e r i m i n e
	//==============================================================================
	
	/**
	 *    Agregeerib selle testimistulemuse otsa etteantud testimistulemuse. 
	 *   Agregeerimisel summeeritakse k6ikide loendurite v22rtused ning m66dikute
	 *   (precision, recall) uuestiarvutamisel saadakse juba mitme testimistulemuse
	 *   (nt mitme dokumendi testimise) koondtulemus.
	 */
	public void lisaUusTulemus(TestimisTulemusTERN uusTulemus){
		// ---------- relevantItems
		for (Iterator<String> iterator = ((uusTulemus.relevantItems).keySet()).iterator(); iterator.hasNext();) {
			String type  = (String) iterator.next();
			Double value = (uusTulemus.relevantItems).get(type);
			if ((this.relevantItems).containsKey(type)){
				(this.relevantItems).put(
						type, 
						(this.relevantItems).get(type) + value);
			} else {
				(this.relevantItems).put(type, value);				
			}
		}		
		// ---------- correctlyRecognized
		for (Iterator<String> iterator = ((uusTulemus.correctlyRecognized).keySet()).iterator(); iterator.hasNext();) {
			String type  = (String) iterator.next();
			Double value = (uusTulemus.correctlyRecognized).get(type);
			if ((this.correctlyRecognized).containsKey(type)){
				(this.correctlyRecognized).put(
						type, 
						(this.correctlyRecognized).get(type) + value);
			} else {
				(this.correctlyRecognized).put(type, value);				
			}
		}
		// ---------- itemsExtracted
		for (Iterator<String> iterator = ((uusTulemus.itemsExtracted).keySet()).iterator(); iterator.hasNext();) {
			String type  = (String) iterator.next();
			Double value = (uusTulemus.itemsExtracted).get(type);
			if ((this.itemsExtracted).containsKey(type)){
				(this.itemsExtracted).put(
						type, 
						(this.itemsExtracted).get(type) + value);
			} else {
				(this.itemsExtracted).put(type, value);				
			}
		}
		// ---------- other		
		this.documentsTested += uusTulemus.documentsTested;
		this.wordCount += uusTulemus.wordCount;
		this.commentCount += uusTulemus.commentCount;
	}	
	
	//==============================================================================
	//   	I n c r e a s e r s   &   G e t t e r s   &   S e t t e r s 
	//==============================================================================

	// -------------------------------------
	//  correctlyRecognized
	// -------------------------------------

	/**
	 *   Lisab yhe (masina poolt) korrektse eraldamise. 
	 */
	public void addToCorrectlyRecognized(String type){
		if ((this.correctlyRecognized).containsKey(type)){
			(this.correctlyRecognized).put(type, 
					(this.correctlyRecognized).get(type).doubleValue() + 1.0 );
		} else {
			(this.correctlyRecognized).put(type, 1.0 );
		}
	}
	
	public double getCorrectlyRecognized(String type) {
		if ((this.correctlyRecognized).containsKey(type)){
			return correctlyRecognized.get(type);
		} else {
			return -1.0;
		}
	}

	public void setCorrectlyRecognized(String type, double correctlyRecognized) {
		(this.correctlyRecognized).put(type, correctlyRecognized);
	}

	public double getCorrectlyRecognizedOverAllTypes(){
		double sum = 0.0;
		for (Iterator<String> iterator = (correctlyRecognized.keySet()).iterator(); iterator.hasNext();) {
			String type = (String) iterator.next();
			sum += correctlyRecognized.get(type);
		}
		return sum;
	}
	
	// -------------------------------------
	//  overallCorrectItemsForRecognition
	// -------------------------------------
	
	/**
	 *   Lisab yhe eraldamist n6udva yksuse. 
	 */
	public void addToRelevantItems(String type){
		if ((this.relevantItems).containsKey(type)){
			(this.relevantItems).put(type, 
					(this.relevantItems).get(type).doubleValue() + 1.0 );
		} else {
			(this.relevantItems).put(type, 1.0 );
		}
	}
	
	public double getRelevantItems(String type) {
		if ((this.relevantItems).containsKey(type)){
			return relevantItems.get(type);
		} else {
			return -1.0;
		}
	}

	public void setRelevantItems(String type, double relevantItems) {
		(this.relevantItems).put(type, relevantItems);
	}
	
	public double getRelevantItemsOverAllTypes(){
		double sum = 0.0;
		for (Iterator<String> iterator = (relevantItems.keySet()).iterator(); iterator.hasNext();) {
			String type = (String) iterator.next();
			sum += relevantItems.get(type);
		}
		return sum;
	}
	
	// -------------------------------------
	//  itemsExtracted
	// -------------------------------------
	
	/**
	 *   Lisab yhe automaatse eraldamise (ei ole teada, kas korrektne v6i mitte). 
	 */
	public void addToItemsExtracted(String type){
		if ((this.itemsExtracted).containsKey(type)){
			(this.itemsExtracted).put(type, 
					(this.itemsExtracted).get(type).doubleValue() + 1.0 );
		} else {
			(this.itemsExtracted).put(type, 1.0 );
		}
	}
	
	public double getItemsExtracted(String type) {
		if ((this.itemsExtracted).containsKey(type)){
			return itemsExtracted.get(type);
		} else {
			return -1.0;
		}
	}

	public void setItemsExtracted(String type, double itemsExtracted) {
		(this.itemsExtracted).put(type, itemsExtracted);
	}

	public double getItemsExtractedOverAllTypes(){
		double sum = 0.0;
		for (Iterator<String> iterator = (itemsExtracted.keySet()).iterator(); iterator.hasNext();) {
			String type = (String) iterator.next();
			sum += itemsExtracted.get(type);
		}
		return sum;
	}
	
	// -------------------------------------
	//  measures
	// -------------------------------------
	
	public double getRecallOfExtraction(String type){
		if ((this.recallOfExtraction).containsKey(type)){
			return recallOfExtraction.get(type);
		} else {
			return -1.0;
		}
	}
	
	public double getPrecisionOfExtraction(String type){
		if ((this.precisionOfExtraction).containsKey(type)){
			return precisionOfExtraction.get(type);
		} else {
			return -1.0;
		}
	}
	
	// -------------------------------------
	//  other
	// -------------------------------------
	
	public int getDocumentsTested() {
		return documentsTested;
	}

	public void setDocumentsTested(int documentsTested) {
		this.documentsTested = documentsTested;
	}

	public int getWordCount() {
		return wordCount;
	}

	public void setWordCount(int wordCount) {
		this.wordCount = wordCount;
	}

	public int getCommentCount() {
		return commentCount;
	}

	public void setCommentCount(int commentCount) {
		this.commentCount = commentCount;
	}
	
	public void addOneToCommentCount(){
		this.commentCount++;
	}
	
	public boolean isReportFScore() {
		return reportFScore;
	}

	public void setReportFScore(boolean reportFScore) {
		this.reportFScore = reportFScore;
	}	
	
	//==============================================================================
	//   	T r y k k i m i n e
	//==============================================================================

	public String printResultTable(DecimalFormat dFormatter){
		StringBuilder sb       = new StringBuilder();
		StringBuilder warnings = new StringBuilder();
		List<String> labels = new ArrayList<String>();
		labels.add(TextUtils.resizeString(" ", 10, true));
		labels.add(TextUtils.resizeString("Saagis:", 20, false));
		labels.add(TextUtils.resizeString("Tapsus:", 20, false));
		if (reportFScore){
			labels.add(TextUtils.resizeString("F-skoor:", 15, false));
		}
		for (String columnLabel : labels) {
			sb.append( columnLabel );
		}
		sb.append( System.getProperty("line.separator") );
    	//testLog.println(" Eraldamise saagis:    "+s
		for (Iterator<String> iterator = (getAllUsedTypesInViewableOrder()).iterator(); iterator.hasNext();) {
			String type = (String) iterator.next();
			StringBuilder sb2 = new StringBuilder();
			sb2.append( " " );
			sb2.append( TextUtils.resizeString(type, 10, true) );
			// ----- Eraldamise saagis
			
				StringBuilder sb3 = new StringBuilder();
				if (correctlyRecognized.containsKey(type) || relevantItems.containsKey(type)){
					if (correctlyRecognized.containsKey(type)){
						sb3.append( dFormatter.format(correctlyRecognized.get(type)) );
					} else {
						sb3.append( dFormatter.format(0.0) );
					}
					sb3.append("/");
					if (relevantItems.containsKey(type)){
						sb3.append( dFormatter.format(relevantItems.get(type)) );
					} else {
						sb3.append( dFormatter.format(0.0) );
					}				
					sb3.append(" (");
					if (recallOfExtraction.containsKey(type)){
						if (recallOfExtraction.get(type) < -0.0){
							sb3.append( "N/A)" );
						} else {
							if (recallOfExtraction.get(type) > 1.0){
								warnings.append(" Warning! Recall greater than 100% for '"+type+"'.");
								warnings.append( System.getProperty("line.separator") );
							}
							sb3.append( dFormatter.format((recallOfExtraction.get(type))*100.0)+"%)" );
						}
					} else {
						sb3.append( "N/A)" );
					}					
				}
				sb2.append( TextUtils.resizeString(sb3.toString(), 19, false) );
				sb2.append( " " );
			
			// ----- Eraldamise tapsus

				sb3 = new StringBuilder();
				if (correctlyRecognized.containsKey(type) || itemsExtracted.containsKey(type)) {
					if (correctlyRecognized.containsKey(type)){
						sb3.append( dFormatter.format(correctlyRecognized.get(type)) );
					} else {
						sb3.append( dFormatter.format(0.0) );
					}
					sb3.append("/");
					if (itemsExtracted.containsKey(type)){
						sb3.append( dFormatter.format(itemsExtracted.get(type)) );
					} else {
						sb3.append( dFormatter.format(0.0) );
					}				
					sb3.append(" (");
					if (precisionOfExtraction.containsKey(type)){
						if (precisionOfExtraction.get(type) < 0.0){
							sb3.append( "N/A)" );
						} else {
							if (precisionOfExtraction.get(type) > 1.0){
								warnings.append(" Warning! Precision greater than 100% for '"+type+"'.");
								warnings.append( System.getProperty("line.separator") );
							}
							sb3.append( dFormatter.format((precisionOfExtraction.get(type))*100.0)+"%)" );
						}
					} else {
						sb3.append( "N/A)" );
					}					
				}
				sb2.append( TextUtils.resizeString(sb3.toString(), 19, false) );
				
			// ----- Eraldamise F-skoor
				
				if (reportFScore){
					sb3 = new StringBuilder();
					if (fscoreOfExtraction.containsKey(type)){
						if (fscoreOfExtraction.get(type) < 0.0){
							sb3.append( "N/A" );
						} else {
							if (fscoreOfExtraction.get(type) > 1.0){
								warnings.append(" Warning! F-score greater than 100% for '"+type+"'.");
								warnings.append( System.getProperty("line.separator") );
							}
							sb3.append( dFormatter.format((fscoreOfExtraction.get(type))*100.0)+"%" );
						}
					} else {
						sb3.append( "N/A" );
					}
					sb2.append( TextUtils.resizeString(sb3.toString(), 14, false) );
				}
				
			sb2.append( System.getProperty("line.separator") );
			sb.append(sb2.toString());
		}
		if (warnings.length() > 0){
			sb.append( System.getProperty("line.separator") );
			sb.append( warnings );
			sb.append( System.getProperty("line.separator") );
		}
		return sb.toString();
	}
	
	public boolean hasItemsExtractedOrRelevantOfType(String type){
		return ( ((int)this.getItemsExtracted(type) != -1) || 
		         ((int)this.getRelevantItems (type) != -1));
	}
	
	//==============================================================================
	//   	A b i m e e t o d i d
	//==============================================================================
	
	private Set<String> getAllUsedTypes(){
		Set <String> setOfAllTypes = new HashSet<String>();
		if (!this.relevantItems.isEmpty()){
			setOfAllTypes.addAll( (this.relevantItems).keySet() );
		}
		if (!this.itemsExtracted.isEmpty()){
			setOfAllTypes.addAll( (this.itemsExtracted).keySet() );
		}
		if (!this.correctlyRecognized.isEmpty()){
			setOfAllTypes.addAll( (this.correctlyRecognized).keySet() );
		}
		return setOfAllTypes;
	}
	
	private List<String> getAllUsedTypesInViewableOrder(){
		Set <String> setOfAllTypes = this.getAllUsedTypes();
		List <String> listOfAllTypes = new LinkedList<String>();
		for (String string : ERALDAMISE_TAHISED) {
			if (setOfAllTypes.contains(string)){
				listOfAllTypes.add(string);
			}
		}
		for (String string : NORMALISEERIMISE_TAHISED) {
			if (setOfAllTypes.contains(string)){
				listOfAllTypes.add(string);
			}
		}		
		return listOfAllTypes;
	}	

}
