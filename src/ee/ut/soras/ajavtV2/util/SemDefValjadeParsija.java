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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import org.joda.time.DateTimeConstants;
import org.joda.time.Period;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.AjavtSona.GRAMMATILINE_AEG;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.Granulaarsus;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.TimeMLDateTimePoint;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.TimeMLDateTimePoint.VALUE_FIELD;


/**
 *  Erinevad abimeetodid klassi {@link SemantikaDefinitsioon} atribuudivaartuste parsimiseks.
 * 
 *  @author Siim
 */
public class SemDefValjadeParsija {
	
	//==============================================================================
	//   	s e m V a l u e    p a r s i m i n e
	//==============================================================================
	
	/**
	 *  <tt>SemantikaDefinitsioon</tt> atribuudi <tt>semValue</tt> vaartuse voimalik formaat.
	 */
	public static enum FORMAT_OF_VALUE {
		PARSE_FROM_SELF,
		PARSE_FROM_NUMERAL,
		INTEGER,
		FRACTION,
		DATE_OR_TIME,
		DATE_OR_TIME_INTERVAL,
		CONSTANT,
		CONSTANTS_INTERVAL,
		REF_TO_VAL,
		REF_TO_LAB,
		UNSET
	};
	
	public static FORMAT_OF_VALUE detectFormatOfValue(String value){
		if (value != null){
			// 1) Tegemist on tavalise, t2isarvulise v22rtusega
			if (value.matches("^(-|\\+)?[0-9]+$")){
				return FORMAT_OF_VALUE.INTEGER;
			}
			// 2) Tegemist on otsese kellaaja v6i kuup2evaga
			if (value.matches("^([0-9][0-9])(:|\\.)([0-9][0-9])$")){
				return FORMAT_OF_VALUE.DATE_OR_TIME;
			}
			// 3) Tegemist on kellaaja v6i kuup2eva vahemikuga
			if (value.matches("^([0-9][0-9])(:|\\.)([0-9][0-9])-([0-9][0-9])(:|\\.)([0-9][0-9])$")){
				return FORMAT_OF_VALUE.DATE_OR_TIME_INTERVAL;
			}			
			// 4) Tegemist on mingi konstant-s6nega (nagu nt MONDAY) v6i lipikuga (nt )
			if (value.matches("^([A-Z_]+)$")){
				return FORMAT_OF_VALUE.CONSTANT;
			}
			// 5) Tegemist on mingi konstant-s6nede vahemikuga (nagu nt MONDAY-FRIDAY)
			if (value.matches("^([A-Z_]+)-([A-Z_]+)$")){
				return FORMAT_OF_VALUE.CONSTANTS_INTERVAL;
			}
			// -------------------- viited k2esolevale sonamallile -----------------------
			// 6) V22rtuse tuleb parsida selle sonamalli poolt leitud alams6nest			
			if (value.matches("^PARSE_FROM_SELF:([0-9.]+)$")){
				return FORMAT_OF_VALUE.PARSE_FROM_SELF;
			}
			// ------------- viited k2esolevale sonamallile: numbri parsimine ------------
			// 7) V22rtuse tuleb parsida selle sonamalli poolt leitud arvs6na-alams6nest			
			if (value.matches("^PARSE_FROM_NUMERAL:([0-9.]+)$")){
				return FORMAT_OF_VALUE.PARSE_FROM_NUMERAL;
			}			
			// --------------------- viited teistele sonamallidele -----------------------
			// 8) Tegemist viitega teise semantikadefinitsiooni "semValue" osale
			if (value.matches("^REF_VAL:([A-Z_][A-Z0-9_]+)$")){
				return FORMAT_OF_VALUE.REF_TO_VAL;
			}
			// 9) Tegemist viitega teis semantikadefinitsiooni "semLabel" osale
			if (value.matches("^REF_LAB:([A-Z_][A-Z0-9_]+)$")){
				return FORMAT_OF_VALUE.REF_TO_LAB;
			}
			// 10) Tegemist on murdarvulise v22rtusega
			if (value.matches("^(-|\\+)?[0-9]+,[0-9]+$")){
				return FORMAT_OF_VALUE.FRACTION;
			}			
		}
		return FORMAT_OF_VALUE.UNSET;
	}
	
	/**
	 *   Tagastab s6nele vastava konstandi klassist <tt>org.joda.time.DateTimeConstants</tt>.
	 */
	public static int parseValueFromConstant(String dateTimeConstant){
		// -------------------      AM_PM     ------------------------
		if (dateTimeConstant.equals("AM")){
			return DateTimeConstants.AM;
		}
		if (dateTimeConstant.equals("PM")){
			return DateTimeConstants.PM;
		}		
		// -------------------  Nadalapaevad  ------------------------
		if (dateTimeConstant.equals("MONDAY")){
			return DateTimeConstants.MONDAY;
		}
		if (dateTimeConstant.equals("TUESDAY")){
			return DateTimeConstants.TUESDAY;
		}
		if (dateTimeConstant.equals("WEDNESDAY")){
			return DateTimeConstants.WEDNESDAY;
		}
		if (dateTimeConstant.equals("THURSDAY")){
			return DateTimeConstants.THURSDAY;
		}
		if (dateTimeConstant.equals("FRIDAY")){
			return DateTimeConstants.FRIDAY;
		}
		if (dateTimeConstant.equals("SATURDAY")){
			return DateTimeConstants.SATURDAY;
		}
		if (dateTimeConstant.equals("SUNDAY")){
			return DateTimeConstants.SUNDAY;
		}
		// ---------------------  Kuud  ------------------------------
		if (dateTimeConstant.equals("JANUARY")){
			return DateTimeConstants.JANUARY;
		}
		if (dateTimeConstant.equals("FEBRUARY")){
			return DateTimeConstants.FEBRUARY;
		}
		if (dateTimeConstant.equals("MARCH")){
			return DateTimeConstants.MARCH;
		}
		if (dateTimeConstant.equals("APRIL")){
			return DateTimeConstants.APRIL;
		}
		if (dateTimeConstant.equals("MAY")){
			return DateTimeConstants.MAY;
		}
		if (dateTimeConstant.equals("JUNE")){
			return DateTimeConstants.JUNE;
		}		
		if (dateTimeConstant.equals("JULY")){
			return DateTimeConstants.JULY;
		}
		if (dateTimeConstant.equals("AUGUST")){
			return DateTimeConstants.AUGUST;
		}
		if (dateTimeConstant.equals("SEPTEMBER")){
			return DateTimeConstants.SEPTEMBER;
		}
		if (dateTimeConstant.equals("OCTOBER")){
			return DateTimeConstants.OCTOBER;
		}		
		if (dateTimeConstant.equals("NOVEMBER")){
			return DateTimeConstants.NOVEMBER;
		}
		if (dateTimeConstant.equals("DECEMBER")){
			return DateTimeConstants.DECEMBER;
		}		
		return Integer.MIN_VALUE;
	}

	/**
	 *   "Kirjutab antud semantikadefinitsiooni lahti" - st, viib l2bi asendused:
	 *    <ul>
	 *       <li>HOUR_OF_DAY asendatakse v22rtustega HOUR_OF_HALF_DAY ja AM_PM (kui pole tegemist ADD operatsiooniga)
	 *       <li>TIME asendatakse v22rtustega HOUR_OF_DAY ja MINUTE
	 *       <li>DATE asendatakse v22rtustega DAY_OF_MONTH ja MONTH
	 *    </ul>
	 * 
	 */
	public static List<SemantikaDefinitsioon> kirjutaLahtiSemantikadefinitsioon(SemantikaDefinitsioon semDef, 
																		 		FORMAT_OF_VALUE formatOfValue){
		List<SemantikaDefinitsioon> newSemDefs = new LinkedList<SemantikaDefinitsioon>();
		// ------ parsime lahti intervallid
		if (semDef.getOp() == null || !(semDef.onSET_attrib("value"))){ // Kui operatsiooniks on SET_VAL, siis mingit "intervallide avamist" ei toimu
			if (formatOfValue == FORMAT_OF_VALUE.CONSTANTS_INTERVAL ||
					formatOfValue == FORMAT_OF_VALUE.DATE_OR_TIME_INTERVAL){
					String parts[] = semDef.getSemValue().split("-");
					if (parts.length == 2){
						SemantikaDefinitsioon semDef2 = semDef.clone();				
						addSimpleOpp(SemantikaDefinitsioon.OP.CREATE_beginPoint.toString(),
									 semDef.getPriority(),
									 newSemDefs);
						semDef.setSemValue (parts[0]);
						newSemDefs.addAll(
								SemDefValjadeParsija.kirjutaLahtiSemantikadefinitsioon(
												semDef,
												detectFormatOfValue(semDef.getSemValue()) )
										 );
						addSimpleOpp(SemantikaDefinitsioon.OP.CREATE_endPoint.toString(), 
								     semDef.getPriority(),
								     newSemDefs);
						semDef2.setSemValue(parts[1]);
						newSemDefs.addAll(
								SemDefValjadeParsija.kirjutaLahtiSemantikadefinitsioon(
												semDef2,
												detectFormatOfValue(semDef2.getSemValue()) )
										 );
						return newSemDefs;
					}
				}
		}
		// ----- HOUR_OF_DAY lahtiparsimine
		if (semDef.getGranulaarsus() == Granulaarsus.HOUR_OF_DAY){
			try {
				int hourOfDayInt = Integer.parseInt(semDef.getSemValue());
				semDef.setGranulaarsus(Granulaarsus.HOUR_OF_HALF_DAY);
				SemantikaDefinitsioon semDef2 = semDef.clone();
				semDef2.setGranulaarsus(Granulaarsus.AM_PM);
				if (hourOfDayInt == 0 || hourOfDayInt == 24){
					// Joda-Time spetsiifiline: 12 == 0
					semDef.setSemValue("0");
					semDef2.setSemValue("AM");
				} else if (hourOfDayInt == 12){
					// Joda-Time spetsiifiline: 12 == 0					
					semDef.setSemValue("0");
					semDef2.setSemValue("PM");					
				} else if (hourOfDayInt < 13){
					semDef2.setSemValue("AM");
				} else if (hourOfDayInt > 12){
					semDef.setSemValue(String.valueOf(hourOfDayInt-12));
					semDef2.setSemValue("PM");
				}
				newSemDefs.add(semDef2);
			} catch (NumberFormatException e) {
			}
			newSemDefs.add(semDef);
			return newSemDefs;
		}
		// ------- parsime lahti DATE v6i TIME
		if (formatOfValue == FORMAT_OF_VALUE.DATE_OR_TIME){
			if (semDef.getGranulaarsus() == Granulaarsus.TIME){
				String parts[] = semDef.getSemValue().split("(\\.|:)");
				if (parts.length == 2){
					try {
						int hourOfDay = Integer.parseInt(parts[0]);
						int minOfHour = Integer.parseInt(parts[1]);
						SemantikaDefinitsioon semDef2 = semDef.clone();
						semDef.setGranulaarsus(Granulaarsus.HOUR_OF_DAY);
						semDef.setSemValue(String.valueOf(hourOfDay));
						newSemDefs.addAll(
								SemDefValjadeParsija.kirjutaLahtiSemantikadefinitsioon(
												semDef,
												detectFormatOfValue(semDef.getSemValue()) )
										 );
						semDef2.setGranulaarsus(Granulaarsus.MINUTE);
						semDef2.setSemValue(String.valueOf(minOfHour));
						newSemDefs.add(semDef2);
						return newSemDefs;
					} catch (NumberFormatException e) {
					}
				}
			}
			if (semDef.getGranulaarsus() == Granulaarsus.DATE){
				String parts[] = semDef.getSemValue().split("(\\.|:)");
				if (parts.length == 2){
					try {					
						int dayOfMonth = Integer.parseInt(parts[0]);
						int month = Integer.parseInt(parts[1]);
						SemantikaDefinitsioon semDef2 = semDef.clone();
						semDef.setGranulaarsus(Granulaarsus.MONTH);
						semDef.setSemValue(String.valueOf(month));
						newSemDefs.add(semDef);						
						semDef2.setGranulaarsus(Granulaarsus.DAY_OF_MONTH);
						semDef2.setSemValue(String.valueOf(dayOfMonth));
						newSemDefs.add(semDef2);
						return newSemDefs;
					} catch (NumberFormatException e) {
					}					
				}				
			}
		}			
		newSemDefs.add(semDef);
		return newSemDefs;
	}
	
	private static void addSimpleOpp(String op, String priority, List<SemantikaDefinitsioon> semDefs){
		SemantikaDefinitsioon semDef = new SemantikaDefinitsioon();
		semDef.setPriority(priority);
		semDef.setOp(op);
		semDefs.add(semDef);
	}
	
	//==============================================================================
	//   	  s e o t u d K o n t e k s t    p a r s i m i n e
	//==============================================================================
	
	/**
	 *   Parsib etteantud kontekstikirjeldusest (st objekti <tt>SemantikaDefinitsioon</tt>  
	 *  v&auml;li <tt>seotudKontekst</tt>) n6utud ja keelatud tunnuste loetelud.
	 *  <p>
	 *  Kui on maaratud <tt>prefiks</tt>, kustutatakse see enne parsimist koos kooloniga
	 *  <tt>kontekstStr</tt>-i eesotsast. Tunnused peavad paiknema komaga yksteisest eraldatult. 
	 *  Negatiivsete tunnuste ees on m2rk ^
	 *  <p>
	 *   Paigutab parsitud tunnused vastavalt listidesse <tt>n6utudTunnused</tt> ja
	 *   <tt>keelatudTunnused</tt>.
	 */
	public static void parsiN6utudJaKeelatudTunnused(SemantikaDefinitsioon.CONTEXT prefiks, 
										             String kontekstStr,
										             List<String> n6utudTunnused,
										             List<String> keelatudTunnused){
		if (prefiks != null){
			kontekstStr = kontekstStr.replaceAll( (prefiks.toString())+":?", "");
		}
		String allListedElements [] = kontekstStr.split(",");
		if (allListedElements.length > 0){
			for (int i = 0; i < allListedElements.length; i++) {
				String element = TextUtils.trim( allListedElements[i] );
				if (!element.startsWith("^")){
					if (n6utudTunnused != null){
						n6utudTunnused.add(element);							
					}
				} else {
					if (keelatudTunnused != null){
						keelatudTunnused.add( element.replaceAll("\\^", "") );							
					}
				}
			}	
		}
	}
	
	//==============================================================================
	//   	  S E E K - d i r e c t i o n    p a r s i m i n e
	//==============================================================================
	
	/**
	 *   Seek-direction konstant; M2rgib, et seek-direction tuleb v6tta l2hima verbi aja j2rgi; 
	 */
	static final private String LABEL_VERBI_AEG = "VERBI_AEG"; 
	
	/**
	 *   Parsib s6nest SEEK-operatsiooni suuna: -1 == minevik, +1 tulevik. Kasutab vajadusel
	 *  l2hima verbi grammatilist aega suuna m22ramisel. Tagastab <tt>Integer.MIN_VALUE</tt>, 
	 *  kui parsimine eba6nnestub. 
	 */
	public static int parseSeekDirection(String direction, AjavtSona lahimVerb){
		int dirAsInteger = Integer.MIN_VALUE;
		if (direction.equals(LABEL_VERBI_AEG)){
			if (lahimVerb != null){
				if (lahimVerb.getGrammatilineAeg() != GRAMMATILINE_AEG.MAARAMATA){
					if (lahimVerb.getGrammatilineAeg() == GRAMMATILINE_AEG.OLEVIK ||
							lahimVerb.getGrammatilineAeg() == GRAMMATILINE_AEG.KS_OLEVIK){
						// katsetada ka lahimVerb.getVerbiAeg() == VERBI_AEG.TAISMINEVIK
						return 1;
					} else {
						return -1;
					}
				}
			}
		} else {
			String withOutPlus = (direction).replace("+", "");
			try {
				dirAsInteger = Integer.parseInt( withOutPlus );
			} catch (NumberFormatException e) {
			}
		}
		return dirAsInteger;
	}

	//==============================================================================
	//   	F o o k u s a j a    g r a n u l a a r s u s e   k o n t r o l l
	//==============================================================================
	
	public static boolean fookusaegRahuldabSeotudKontekstiTingimusi(String kontekstStr, String [] aegFookuses){
		boolean eitus = kontekstStr.startsWith("^");
		kontekstStr = kontekstStr.replaceAll( "^[^:]+:", "");
		String allListedElements [] = kontekstStr.split(",");
		if (allListedElements.length == 2 && (aegFookuses.length == (TimeMLDateTimePoint.valueFieldsInOrder).length)){
			String gran      = TextUtils.trim( allListedElements[0] );
			String condition = TextUtils.trim( allListedElements[1] );
			Granulaarsus g = Granulaarsus.getGranulaarsus(gran);
			try {
				Pattern p = Pattern.compile(condition);
				if (g != null && p != null){
					// Viime kokku granulaarsuse ning sellele vastava "TIMEX3 value" välja
					for (Granulaarsus granulaarsus : Granulaarsus.fieldsInSafeOrder) {
						if (g.compareByCoarseRank(granulaarsus) == 1){
							for (int i = 0; i < (TimeMLDateTimePoint.valueFieldsInOrder).length; i++) {
								VALUE_FIELD field = (TimeMLDateTimePoint.valueFieldsInOrder)[i];
								if ( field.matchesGranularity(granulaarsus)){
									return eitus ^ ((p.matcher(aegFookuses[i])).matches());
								}
							}
						}
					}	
				}
			} catch (Exception e) {
				// Kui regulaaravaldise kompileerimine feilib ...
			}
		}
		return eitus ^ false;
	}
	
	
	
	
	//==============================================================================
	//    Murdarvulise ajamaarangu lahtiparsimine taisarvulisteks maaranguteks
	//==============================================================================
	
	/**
	 *   Parsib murdarvulise ajamaarangu (nt <i>1.5 aastat</i>) lahti sobivate granulaarsustega
	 *  perioodiks (nt <i>1 aasta ja 6 kuud</i>). Kui parsimine eba6nnestub, tagastab <tt>null</tt>.
	 *  Onnestumise korral tagastab massiivi, mille esimene objekt on ajaperiood (Joda Time'i <tt>Period</tt>) 
	 *  ning teine objekt uus MOD vaartus. Teine objekt voib ka yldse puududa - see lisatakse vaid juhul, 
	 *  kui parsitud periood on ebatapne (nt on ebatapne kuu parsimine paevadeks).
	 */
	public static Object [] parsiMurdarvulineAjakvantiteetPerioodiks(Granulaarsus field, double murdarvuline){
		Period periood  = new Period();
		String modifier = null;
		// ----------------------------------------------------------
		//     Mitte-t2isarvulise ajaperioodi lisamine...
		// ----------------------------------------------------------
		int [] taisArvuOsaJaMurdArvuOsa = 
			separateIntegerPartAndFloatingPart(murdarvuline);
		if (field == Granulaarsus.MINUTE){
			// minutid
			try {
				// Esialgu lisame siin ainult taisosa, sekundid jaavad mangust valja
				periood = (periood).plusMinutes( taisArvuOsaJaMurdArvuOsa[0] );
			} catch (Exception e) {
			}			
		} else 
		if (field == Granulaarsus.HOUR_OF_HALF_DAY || field == Granulaarsus.HOUR_OF_DAY){
			// minutid ja tunnid				
			try {			
				periood = (periood).plusMinutes( (taisArvuOsaJaMurdArvuOsa[1]*60)/100 );
				periood = (periood).plusHours( taisArvuOsaJaMurdArvuOsa[0] );
			} catch (Exception e) {
			}			
		} else
		if (field == Granulaarsus.DAY_OF_WEEK || field == Granulaarsus.DAY_OF_MONTH){
			// p2evad ja tunnid				
			try {			
				periood = (periood).plusHours( (taisArvuOsaJaMurdArvuOsa[1]*24)/100 );
				periood = (periood).plusDays( taisArvuOsaJaMurdArvuOsa[0] );
			} catch (Exception e) {
			}
		} else
		if (field == Granulaarsus.WEEK_OF_YEAR){
			// p2evad ja n2dalad				
			try {			
				periood = (periood).plusDays( (taisArvuOsaJaMurdArvuOsa[1]*7)/100 );
				periood = (periood).plusWeeks( taisArvuOsaJaMurdArvuOsa[0] );
			} catch (Exception e) {
			}
		} else 
		if (field == Granulaarsus.MONTH){
			// p2evad ja kuud 
			// (NB! Muutub ligikaudseks, kuna pole t2pselt teada, millise
			// kuuga on tegu ning kuupikkused on erinevad)
			try {			
				periood  = (periood).plusDays( (taisArvuOsaJaMurdArvuOsa[1]*30)/100 );
				periood  = (periood).plusMonths( taisArvuOsaJaMurdArvuOsa[0] );
				modifier = "APPROX";
			} catch (Exception e) {
			}
		} else 
		if (field == Granulaarsus.YEAR){
			// kuud ja aastad
			try {			
				periood = (periood).plusMonths( (taisArvuOsaJaMurdArvuOsa[1]*12)/100 );
				periood = (periood).plusYears( taisArvuOsaJaMurdArvuOsa[0] );
			} catch (Exception e) {
			}
		}
		if (field == Granulaarsus.CENTURY_OF_ERA){
			// sajandid ja aastad
			try {
				// Aastad
				periood = (periood).plusYears( taisArvuOsaJaMurdArvuOsa[1] );
				// Esialgu periood sajandites m66tmist ei toeta, seega konverteerime sajandid aastateks
				periood = (periood).plusYears( taisArvuOsaJaMurdArvuOsa[0]*100 );
			} catch (Exception e) {
			}
		}		
		Object [] tagastatav = null;
		if (modifier != null){
			tagastatav = new Object[2];
			tagastatav[0] = periood;
			tagastatav[1] = modifier;
		} else {
			tagastatav = new Object[1];
			tagastatav[0] = periood;
		}
		return tagastatav;
	}
	
	/**
	 *   Parsib murdarvulise ajamaarangu (nt <i>1.5 aastat</i>) lahti sobivate granulaarsustega
	 *  perioodiks (nt <i>1 aasta ja 6 kuud</i>). Tagastab tulemused s6ne kujul: esimene s6ne 
	 *  on ajam22rangule vastav ISO-formaadis periood, teine modifikaator (TimeML "mod") 
	 *  (kui see esines).
	 *  <br>
	 *  Kasutab meetodit parsiMurdarvulineAjakvantiteetPerioodiks;
	 */
	public static String[] parsiMurdarvulineAjakvantiteetS6neks(Granulaarsus granulaarsus, String value){
		float f;
		try {
			f = Float.parseFloat( (value).replaceAll(",", ".") );
			Object[] perioodJaMOD = 
				parsiMurdarvulineAjakvantiteetPerioodiks(granulaarsus, f);
			if (perioodJaMOD != null && perioodJaMOD[0] != null){
				String returnable [] = null;
				if (perioodJaMOD.length == 2){
					returnable = new String[2];
					returnable[0] = ((Period)perioodJaMOD[0]).toString();
					returnable[1] = (String)perioodJaMOD[1];
				} else {
					returnable = new String[1];
					returnable[0] = ((Period)perioodJaMOD[0]).toString();
				}
				return returnable;
			}

		} catch (NumberFormatException e) {
		}
		return null;
	}
	
	/**
	 *  Lahutab arvu kaheks: t2isarvu osa ja peale koma j2rgnev murdarvu osa.
	 *  Murdarvu osa v6etakse t2psusega kaks kohta p2rast koma. 
	 */
	private static int[] separateIntegerPartAndFloatingPart(double value){
		int [] parts = new int[2];
		parts[0] = (int) value;
		parts[1] = (int)((value-(int)value)*100);
		return parts;
	}
	
}
