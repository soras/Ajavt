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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.joda.time.LocalDateTime;
import org.joda.time.Period;

import ee.ut.soras.ajavtV2.mudel.ajavaljend.Granulaarsus;

/**
 *  Yhe ajapunkti semantika representatsiooni sisaldav objekt.
 *  Toetab tyype POINT (DATE, TIME) ja RECURRENCE (SET).
 *  
 *  @author Siim Orasmaa
 */
public class AjaPunkt implements AjaObjekt {
	
	/**
	 *   Selle ajaobjekti unikaalne identifikaator; 
	 */
	private int ID;
	
	/**
	 *  Selle konkreetse ajaobjekti tyyp.
	 */
	private AjaObjekt.TYYP type = AjaObjekt.TYYP.UNK;
	
	private TimeMLDateTimePoint dateTimePoint = null;
	
	/**
	 *   Täidetud TIMEX3 atribuudid; 
	 */
	private HashMap<String, String> setTIMEX3Attribs = null;
	

	/**
	 *   Mitu positsiooni tuleb aastaarvu vasakult poolt kinni katta?
	 *   (Vajalik selleks, et anda edasi aastakymneid v6i sajandeid)
	 */
	private int yearCover = 0;
	
	private List<AjaObjekt> seotudImplitsiitsedTIMEXid  = null;
	private List<AjaObjekt> seotudEksplitsiitsedTIMEXid = null;

	public AjaPunkt(AjaObjekt.TYYP tyyp, String [] baasAeg) {
		this.ID = AjaObjektiID.annaJargmineVabaID();
		this.setTimex3Attribute("tid", "t"+this.ID);
		// NB! Vaikimisi on temporalFunction alati "true" -- eeldame, et on tegu arvutatava väljendiga
		this.setTimex3Attribute("temporalFunction", "true");
		this.dateTimePoint = new TimeMLDateTimePoint(baasAeg);
		this.type      = tyyp;
	}
	
	public AjaPunkt(AjaObjekt.TYYP tyyp, LocalDateTime baasAeg) {
		this.ID = AjaObjektiID.annaJargmineVabaID();
		this.setTimex3Attribute("tid", "t"+this.ID);
		// NB! Vaikimisi on temporalFunction alati "true" -- eeldame, et on tegu arvutatava väljendiga
		this.setTimex3Attribute("temporalFunction", "true");
		this.dateTimePoint = new TimeMLDateTimePoint(baasAeg);
		this.type      = tyyp;
	}
	
	public AjaPunkt(AjaObjekt.TYYP tyyp, TimeMLDateTimePoint refAeg) {
		this.ID = AjaObjektiID.annaJargmineVabaID();
		this.setTimex3Attribute("tid", "t"+this.ID);
		// NB! Vaikimisi on temporalFunction alati "true" -- eeldame, et on tegu arvutatava väljendiga
		this.setTimex3Attribute("temporalFunction", "true");
		if (refAeg != null){
			this.dateTimePoint = new TimeMLDateTimePoint(refAeg);			
		}
		this.type = tyyp;
	}
	
	//==============================================================================
	//   	Tag'i genereerimine. 
	//==============================================================================	
	
	/**
	 *  Genereerib TimeML'ile vastava VALUE osa; muudab ka vastavalt genereeritule TYPE atribuudi v&auml;&auml;rtust.
	 */
	public String doValuePartInTimeMLAndChangeTypeIfNecessary() throws Exception {
		StringBuilder sb = new StringBuilder();
		if (this.setTIMEX3Attribs == null  || 
				(this.setTIMEX3Attribs != null && (this.setTIMEX3Attribs).get("value") == null)){
			sb.append( (this.dateTimePoint).toValueString() );
			boolean timeExists = (this.dateTimePoint).hasModifiedTimeFields();
			// Muudame ka type v22rtust, vastavalt konstrueerimise tulemustele;
			if (timeExists){
				this.setTimex3Attribute("type", "TIME");
			} else {
				this.setTimex3Attribute("type", "DATE");
			}
		} else {
			this.setTimex3Attribute("type", "DATE");
			sb.append( (this.setTIMEX3Attribs).get("value") );
			if (this.type != AjaObjekt.TYYP.RECURRENCE){
				this.setTimex3Attribute("temporalFunction", "true");
			}
		}
		return sb.toString();
	}
	
	/**
	 *   Genereerib osalise p2ise: VAL väärtus ning ülejäänud TIMEX3 väärtused (anchorTimeID jms). 
	 *   Kõige l6ppu kleebitakse etteantud sufiks.
	 */
	public String getPartialTagHeader(String suffix) {
		HashMap<String, String> hashMapOfAttributeValue = asHashMapOfAttributeValue(suffix);
		StringBuilder sb = new StringBuilder();
		for (String key : (hashMapOfAttributeValue).keySet()) {
			if (!key.equalsIgnoreCase("TYPE") && 
					!key.equalsIgnoreCase("TID")){
				sb.append(key.toUpperCase());
				sb.append(suffix);
				sb.append("=\"");					
				sb.append((hashMapOfAttributeValue).get(key));
				sb.append("\" ");
			}
		}		
		return sb.toString();
	}

	//==============================================================================
	//     Operatsioonid
	//==============================================================================

	//==================
	//   S E T
	//==================
	
	public void setField(Granulaarsus field, int value){
		(this.dateTimePoint).setField(field, value);
	}
	
	public void setField(Granulaarsus field, String label){
		(this.dateTimePoint).setField(field, label);
	}
	
	//==================
	//   A D D 
	//==================
	
	public void addToField(Granulaarsus field, int value) {
		(this.dateTimePoint).addToField(field, value);
	}
	
	public void addToField(Granulaarsus field, Period period, int direction){
		(this.dateTimePoint).addToField(field, period, direction);
	}
	
	//==================
	//   S E E K
	//==================

	public void seekField(Granulaarsus field, int direction, int soughtValue, boolean excludingCurrent){
		(this.dateTimePoint).seekField(field, direction, soughtValue, excludingCurrent);
	}
	
	public void seekField(Granulaarsus field, int direction, String soughtValue, boolean excludingCurrent){
		(this.dateTimePoint).seekField(field, direction, soughtValue, excludingCurrent);
	}

	//==================
	//   O t h e r
	//==================
	
	/**
	 *  Rakendab Balwdini akna algoritmi (klassist {@link SemLeidmiseAbimeetodid}), leidmaks l2himat
	 *  ajahetke yldistatud Baldwini akna seest, mille puhul kehtib <tt>field = soughtValue</tt>. 
	 *  Esialgu defineeritud ainult piiratud arvu granulaarsuste korral.
	 */
	public void applyBalwinWindow(Granulaarsus field, String soughtValue){
		(this.dateTimePoint).applyBalwinWindow(field, soughtValue);
	}
	
	/**
	 *  Rakendab meetodit {@link SemLeidmiseAbimeetodid#findNthSubpartOfGranularity(Granulaarsus, Granulaarsus, int, int, LocalDateTime)}}. 
	 */
	public void findNthSubpartOfGranularity(Granulaarsus superField, Granulaarsus subField, int soughtValueOfSubField, int n){
		(this.dateTimePoint).findNthSubpartOfGranularity(superField, subField, soughtValueOfSubField, n);
	}
	
	private boolean setValuesExist(){
		return ((this.dateTimePoint != null && (this.dateTimePoint).hasModifiedFields()) ||
				(this.setTIMEX3Attribs != null && (this.setTIMEX3Attribs).get("value") != null));
	}

	//==============================================================================
	//   	V 2 l j a d e     s u l g e m i n e
	//==============================================================================
	
	public void closeGranularitiesBelow(Granulaarsus minGran, int digits){
		if (this.dateTimePoint != null){
			(this.dateTimePoint).closeGranularitiesBelow(minGran, digits);
		}
		SemLeidmiseAbimeetodid.closeGranularitiesBelow( seotudEksplitsiitsedTIMEXid, minGran, digits );
	}
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================
	
	/**
	 *  Leiab, kas antud ajapunktis leidub "kellaaeg"-täpsusega granulaarsuseid.
	 */
	public boolean hasTimeLevelGranularities(){
		return (this.dateTimePoint != null && (this.dateTimePoint).hasModifiedTimeFields());
	}
	
	/**
	 *   Leiab, kas antud ajapunkti <code>value</code>-esitus on hagune (nt 
	 *   vaartus PRESENT_REF, umbmaarane paevaosaviide AF voi eksisteerib 
	 *   semantikat tapsustav <code>mod</code>).
	 *   Tagastab <code>true</code>, kui esitus sisaldab hagusaid komponente;
	 */
	public boolean outPutWillBeFuzzy(){
		// -- pre-set value
		return (this.getTimex3Attribute("value") != null ||
					// -- set mod
					this.getTimex3Attribute("mod") != null || 			
						// -- datetime output contains labels
						this.dateTimePoint != null && (this.dateTimePoint).hasLabelsInOutput()
				);
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
	
	public TYYP getType() {
		return this.type;
	}
	
	public TimeMLDateTimePoint getDateTimePoint(){
		return this.dateTimePoint;
	}
	
	public void setType(TYYP uusTyyp) {
		this.type = uusTyyp;
		if (this.type == TYYP.RECURRENCE && (this.dateTimePoint != null)){
			// Korduvuse korral tuleb aja kuvamine seadistada selliselt, et vaikimisi on kuup2ev/kellaaeg X-idega kaetud ning
			// esimese muutuse korral (releaseMaskAtLowerGranularities) vabastatakse muudetav granulaarsus + v2iksemad 
			// granulaarsused X-ide alt ...
			(this.dateTimePoint).setMaskedFields("XXXX", "XX", "XX", "XX", "XX");
		}
	}
	
	public AjaObjekt clone(){
		AjaPunkt uusAp  = new AjaPunkt(this.type, this.dateTimePoint);
		uusAp.yearCover = this.yearCover;
		uusAp.setTIMEX3Attribs = 
			(this.setTIMEX3Attribs != null) ? (new HashMap<String, String>(this.setTIMEX3Attribs)) : (null);
		uusAp.setTimex3Attribute("tid", "t"+uusAp.ID);
		uusAp.seotudEksplitsiitsedTIMEXid = 
			(this.seotudEksplitsiitsedTIMEXid != null) ? (new ArrayList<AjaObjekt>(this.seotudEksplitsiitsedTIMEXid)) : (null);
		return uusAp;
	}
	
	public LocalDateTime getAsLocalDateTime(){
		return (this.dateTimePoint).getAsLocalDateTime();
	}
	
	public String [] getUnderlyingDateTimeAsGranularitiesArray(){
		return (this.dateTimePoint).getUnderlyingDateTimeAsGranularitiesArray();
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
	
	public void addRelatedExplicitTIMEX(AjaObjekt objekt) {
		if (this.seotudEksplitsiitsedTIMEXid == null){
			this.seotudEksplitsiitsedTIMEXid = new ArrayList<AjaObjekt>(2); 
		}
		(this.seotudEksplitsiitsedTIMEXid).add(objekt);
	}

	public void setRelatedExplicitTIMEX(int index, AjaObjekt objekt){
		if (this.seotudEksplitsiitsedTIMEXid != null && 
			index < (this.seotudEksplitsiitsedTIMEXid).size()){
			(this.seotudEksplitsiitsedTIMEXid).set(index, objekt);
		}
	}

	public List<AjaObjekt> getRelatedExplicitTIMEXES() {
		return this.seotudEksplitsiitsedTIMEXid;
	}
	
	public boolean hasRelatedExplicitTimexesAsInterval(){
		return (this.seotudEksplitsiitsedTIMEXid != null && (this.seotudEksplitsiitsedTIMEXid).size()==2);
	}
	
	//==============================================================================
	//   	A b i m e e t o d i d 
	//==============================================================================

	public int getID() {
		return this.ID;
	}
	
	@Override
	public String toString() {
		return "AP: "+this.getPartialTagHeader("");
	}
	
	public HashMap<String, String> asHashMapOfAttributeValue(String suffix){
		HashMap<String, String> attributesAndValues = new HashMap<String, String>(8);
		if (suffix == null){ suffix = ""; }
		if (this.setValuesExist()){
			try {
				attributesAndValues.put( "value" + suffix, (this.doValuePartInTimeMLAndChangeTypeIfNecessary()) );
			} catch (Exception e) {
			}			
		} 
		if (this.type == TYYP.RECURRENCE){
			setTimex3Attribute("type", "SET");
		}
		if (this.type == TYYP.TIME){
			setTimex3Attribute("type", "TIME");
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
	
}
