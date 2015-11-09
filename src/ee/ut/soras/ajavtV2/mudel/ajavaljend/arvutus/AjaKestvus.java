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

package ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.time.Period;

import ee.ut.soras.ajavtV2.mudel.ajavaljend.Granulaarsus;

/**
 *   DURATION-tyypi semantikalahendus, m&auml;rgib mingi ajalise kestvuse.
 *  Võib olla nii ilmutatud (omab konkreetseid otspunkte ajateljel), kui ka ilmutamata
 *  (otspunktid teadmata või nende tuletamine mittetriviaalne); 
 * 
 * @author Siim Orasmaa
 */
public class AjaKestvus implements AjaObjekt {

	/**
	 *   Selle ajaobjekti unikaalne identifikaator; 
	 */
	private int ID;
	
	private Period periood          = null;

	/**
	 *   Täidetud TIMEX3 atribuudid; 
	 */
	private HashMap<String, String> setTIMEX3Attribs = null;
	
	/** 
	 *   Valjad, mille v22rtuseks on omistatud X (ehk - v22rtus teadmata/tundmata);  
	 */
	private HashMap<Granulaarsus, String> setUnkFields = null;
	
	List<AjaObjekt> seotudImplitsiitsedTIMEXid  = null;
	List<AjaObjekt> seotudEksplitsiitsedTIMEXid = null;
	
	public AjaKestvus() {
		this.ID = AjaObjektiID.annaJargmineVabaID();
		this.setTimex3Attribute("tid", "t"+this.ID);
		// NB! Kestvusel on temporalFunction alati vaikimisi "false" -- eeldame, et tegu pole arvutatava väljendiga
		this.setTimex3Attribute("temporalFunction", "false");
	}
	
	public void addToField(Granulaarsus field, int value) {
		if (this.periood == null){
			this.periood = new Period();
		}
		// ------ Kell
		if (field == Granulaarsus.HOUR_OF_HALF_DAY || field == Granulaarsus.HOUR_OF_DAY){
			try {
				this.periood = (this.periood).plusHours(value);
			} catch (Exception e) {
			}				
		} else if (field == Granulaarsus.MINUTE){
			try {			
				this.periood = (this.periood).plusMinutes(value);
			} catch (Exception e) {
			}			
		} else 
		// ------ P2evad, n2dalad, kuud, aastad
		if (field == Granulaarsus.DAY_OF_WEEK || field == Granulaarsus.DAY_OF_MONTH){
			try {			
				this.periood = (this.periood).plusDays(value);
			} catch (Exception e) {
			}
		} else
		if (field == Granulaarsus.WEEK_OF_YEAR){
			try {			
				this.periood = (this.periood).plusWeeks(value);
			} catch (Exception e) {
			}
		} else
		if (field == Granulaarsus.MONTH){
			try {
				this.periood = (this.periood).plusMonths(value);
			} catch (Exception e) {
			}			
		} else
		if (field == Granulaarsus.YEAR){
			this.periood = (this.periood).plusYears(value);
		} else
		if (field == Granulaarsus.CENTURY_OF_ERA){
			this.periood = (this.periood).plusYears(value*100);
		}
	}

	public void addToField(Granulaarsus field, Period period, int direction){
		if (this.periood == null){
			this.periood = new Period();
		}		
		try {			
			this.periood = (this.periood).plus( period );
		} catch (Exception e) {
		}
	}

	public void addToField(Granulaarsus field, String fuzzyPeriod){
		if ( field != null  &&  fuzzyPeriod.matches("X{1,2}") ){
			if (this.setUnkFields == null){
				this.setUnkFields = new HashMap<Granulaarsus, String>();
			}
			(this.setUnkFields).put(field, fuzzyPeriod);
		}
	}

	public void closeGranularitiesBelow(Granulaarsus minGran, int digits) {
		SemLeidmiseAbimeetodid.closeGranularitiesBelow(seotudEksplitsiitsedTIMEXid, minGran, digits);
	}
	
	public void seekField(Granulaarsus field, int direction, int soughtValue,
			boolean excludingCurrent) {
		// Antud ajav2ljendiliigi puhul defineerimata ...
	}

	public void seekField(Granulaarsus field, int direction,
			String soughtValue, boolean excludingCurrent) {
		// Antud ajav2ljendiliigi puhul defineerimata ...
	}

	public void setField(Granulaarsus field, int value) {
		// 	T2pselt sama, mis add
		this.addToField(field, value);
	}

	public void setField(Granulaarsus field, String label) {
		// Antud ajav2ljendiliigi puhul defineerimata ...
	}
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================

	public int getID() {
		return this.ID;
	}
	
	public TYYP getType() {
		return TYYP.DURATION;
	}
	
	public HashMap<String, String> asHashMapOfAttributeValue(String suffix){
		HashMap<String, String> attributesAndValues = new HashMap<String, String>(8);
		if (suffix == null){ suffix = ""; }
		attributesAndValues.put("type" + suffix,  (TYYP.DURATION).toString());
		if (this.periood != null){
			normalizeWeeksToDays();
			String periodAsString = (this.periood).toString();
			// Eemaldame - margid, kuna meile pole oluline, mis suunas periood jookseb
			periodAsString = periodAsString.replaceAll("-", "");
			attributesAndValues.put("value" + suffix, periodAsString );
		} else if (this.setTIMEX3Attribs != null && ((this.setTIMEX3Attribs).get("value") != null)) {
			this.setTimex3Attribute("temporalFunction", "true");
			attributesAndValues.put("value" + suffix, (this.setTIMEX3Attribs).get("value") );
		} else if (this.setUnkFields != null){
			this.setTimex3Attribute("temporalFunction", "true");
			attributesAndValues.put("value" + suffix, buildISOdurationValAccordingToSetUnkFields() );
		}		
		if (this.setTIMEX3Attribs != null){
			// Add all missing attributes.
			for ( String key : (this.setTIMEX3Attribs).keySet() ) {
				if (!attributesAndValues.containsKey( key ) ){
					attributesAndValues.put( key + suffix, (this.setTIMEX3Attribs).get(key) );
				}
			}
		}
		return attributesAndValues;
	}
	
	public AjaObjekt clone(){
		AjaKestvus uusKestvus = new AjaKestvus();
		if (this.periood != null){
			uusKestvus.periood = new Period(this.periood);
		} else {
			uusKestvus.periood = null;
		}
		if (this.setUnkFields != null){
			uusKestvus.setUnkFields = new HashMap<Granulaarsus, String>(this.setUnkFields);
		} else {
			uusKestvus.setUnkFields = null;
		}
		if (this.setTIMEX3Attribs != null){
			uusKestvus.setTIMEX3Attribs = new HashMap<String, String>(this.setTIMEX3Attribs);
			uusKestvus.setTimex3Attribute("tid", "t"+uusKestvus.ID);
		}
		uusKestvus.seotudEksplitsiitsedTIMEXid = (this.seotudEksplitsiitsedTIMEXid != null) ? (new ArrayList<AjaObjekt>(this.seotudEksplitsiitsedTIMEXid)) : (null);		
		return uusKestvus;
	}
	
	/**
	 *   Kui antud kestvusega seotud konkreetne periood sisaldab nadalaid (ja lisaks ka teisi granulaarsuseid), teisendab 
	 *   nadalad perioodis paevadeks.
	 */
	private void normalizeWeeksToDays(){
		if (this.periood != null){
			int countNonZeroValues = 0;
			for (int value : (this.periood).getValues()) {
				if (value != 0){  countNonZeroValues++;  }
			}
			if ((this.periood).getWeeks() != 0 && countNonZeroValues > 1){
				int addDays = (this.periood).getWeeks() * 7;
				this.periood = (this.periood).withWeeks( 0 );
				if ((this.periood).getDays() >= 0){
					this.periood = (this.periood).plusDays(addDays); 
				} else{
					this.periood = (this.periood).minusDays(addDays);
				}
			}
		}
	}
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================

	/**
	 *   Votab kaesoleva kestvuse vaartuseks kahe ettentud ajapunkti vahelise perioodi,
	 *   esitades selle umbm22rase kestvusena (XX-ide arvuliste v22rtuste asemel).
	 *   Kestvuse yhikuks saab k6ige madalam muudetud granulaarsus m6lema ajapunkti
	 *   peale kokku; Kui muudetud granulaarsusi ei leidu, ei tehta midagi ...
	 *   <br><br>
	 *   TODO: Kestvuste t2pse ulatuse v2ljatoomine v6rreldes kahe otspunktiga seotud 
	 *   LocalDateTime objekte on problemaatiline: eeldab vastavate objektide
	 *   normaliseerimist (nt v2ljendiga "aprillikuu" seotud LocalDateTime tuleks 
	 *   normaliseerida aprilli esimesele p2evale [kui on tegemist beginPoint'iga] 
	 *   ning aprilli viimasele p2evale, viimasele minutile [kui on tegemist 
	 *   endPoint'iga] ning alles siis v6ib perioodi v2lja arvutada);
	 */
	public void setValueAsPeriodBetweenPoints(AjaPunkt beginPoint, AjaPunkt endPoint){
		if ((beginPoint.getType() == TYYP.POINT || beginPoint.getType() == TYYP.TIME) && 
		      (endPoint.getType() == TYYP.POINT || endPoint.getType() == TYYP.TIME)){
			if (beginPoint.getDateTimePoint() != null && endPoint.getDateTimePoint() != null){
				Granulaarsus lowest1 = (beginPoint.getDateTimePoint()).getLowestChangedGranularity();
				Granulaarsus lowest2 = (endPoint.getDateTimePoint()).getLowestChangedGranularity();
				if (lowest1 != null && lowest2 != null){
					Granulaarsus lowestOf2 = (lowest1.compareByCoarseRank(lowest2) == 1) ? (lowest2) : (lowest1);
					this.addToField(lowestOf2, "XX");
				} else if (lowest1 != null){
					this.addToField(lowest1, "XX");
				} else if (lowest2 != null){
					this.addToField(lowest2, "XX");
				}
			}
			this.setTimex3Attribute("temporalFunction", "true");
		}
	}
	
	public void setTimex3Attribute(String attribute, String value){
		if (this.setTIMEX3Attribs == null){
			this.setTIMEX3Attribs = new HashMap<String, String>();
		}
		(this.setTIMEX3Attribs).put(attribute, value);
	}
	
	public String getTimex3Attribute(String attribute){
		if (this.setTIMEX3Attribs != null){
			return (this.setTIMEX3Attribs).get(attribute); 
		}
		return null;
	}

	private static final Object fieldsInISOOrder [][] = {
		{Granulaarsus.CENTURY_OF_ERA,   "CE"},
		{Granulaarsus.YEAR,             "Y"},
		{Granulaarsus.MONTH,            "M"},
		{Granulaarsus.WEEK_OF_YEAR,     "W"},
		{Granulaarsus.DAY_OF_MONTH,     "D"},
		{Granulaarsus.HOUR_OF_HALF_DAY, "H"},
		{Granulaarsus.HOUR_OF_DAY,      "H"},
		{Granulaarsus.MINUTE,           "M"}
	};
	
	/**
	 *  Ehitab ISO-duration'ile sarnase kestvuste/ajaperioodi mustri, asendades 
	 *  konkreetsed kvantiteedid nende umbmäärasuse t2histega, nagu on määratud 
	 *  tabelis <code>setUnkFields</code>;
	 *  <p>
	 *  N2iteks, ajav2ljendi <i>aastaid</i> semantika saab esitada kujul <code>PXY</code>.
	 */
	private String buildISOdurationValAccordingToSetUnkFields(){
		StringBuilder durationToBuild = new StringBuilder();
		durationToBuild.append( "P" );
		int addTimeSymbol = 0;
		for (Object [] granAndApperv : fieldsInISOOrder) {
			Granulaarsus gran = (Granulaarsus)granAndApperv[0];
			String granSymbol = (String)granAndApperv[1];
			if (addTimeSymbol == 0 && gran.compareByCoarseRank(Granulaarsus.DAY_OF_WEEK) == -1){
				addTimeSymbol = 1;
			}
			if ((this.setUnkFields).containsKey(gran)){
				if (addTimeSymbol == 1){
					durationToBuild.append( "T" );
					addTimeSymbol = 2;
				}
				durationToBuild.append( (this.setUnkFields).get(gran) );
				durationToBuild.append( granSymbol );
			}
		}
		return durationToBuild.toString();
	}

	public void addRelatedImplicitTIMEX(AjaObjekt objekt){
		if (this.seotudImplitsiitsedTIMEXid == null){
			this.seotudImplitsiitsedTIMEXid = new ArrayList<AjaObjekt>(2); 
		}
		(this.seotudImplitsiitsedTIMEXid).add(objekt);
	}

	public List<AjaObjekt> getRelatedImplicitTIMEXES(){
		return this.seotudImplitsiitsedTIMEXid;
	}
	
	public void setRelatedExplicitTIMEX(int index, AjaObjekt objekt){
		if (this.seotudEksplitsiitsedTIMEXid != null && 
			index < (this.seotudEksplitsiitsedTIMEXid).size()){
			(this.seotudEksplitsiitsedTIMEXid).set(index, objekt);
		}
	}
	
	public void addRelatedExplicitTIMEX(AjaObjekt objekt) {
		if (this.seotudEksplitsiitsedTIMEXid == null){
			this.seotudEksplitsiitsedTIMEXid = new ArrayList<AjaObjekt>(2); 
		}
		(this.seotudEksplitsiitsedTIMEXid).add(objekt);
	}

	public List<AjaObjekt> getRelatedExplicitTIMEXES() {
		return this.seotudEksplitsiitsedTIMEXid;
	}
	
	public boolean hasRelatedExplicitTimexesAsInterval(){
		return (this.seotudEksplitsiitsedTIMEXid != null && (this.seotudEksplitsiitsedTIMEXid).size()==2);
	}
	
}
