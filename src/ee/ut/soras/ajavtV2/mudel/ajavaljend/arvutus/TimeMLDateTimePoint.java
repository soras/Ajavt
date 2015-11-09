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

import java.util.HashMap;
import java.util.regex.Pattern;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;

import ee.ut.soras.ajavtV2.mudel.ajavaljend.Granulaarsus;

/**
 *  Object that represents roughly calendaric "value" attribute of TimeML TIMEX3 tag. Access 
 * methods allow one to perform well-defined calendar arithmetic operations on this point. 
 * Operations are based on functions supported by Joda-Time LocalTime, LocalDate and LocalDateTime 
 * objects. 
 * 
 * @author Siim Orasmaa
 */
public class TimeMLDateTimePoint {

	//==============================================================================
	//   	V a r i a b l e s   :    r e p r e s e n t a t i o n
	//==============================================================================
	
	/**
	 *  Calendar fields ('granularities') inside the "value" attribute of TimeML TIMEX3 tag.
	 * 
	 *  @author Siim Orasmaa
	 */
	public enum VALUE_FIELD {
		YEAR,
		MONTH_OR_WEEK,
		DAY,
		HOUR_OR_POD,
		MINUTE;
		
		public boolean matchesGranularity(Granulaarsus g){
			if (this.equals(MINUTE)){
				return g.equals(Granulaarsus.MINUTE);
			}
			if (this.equals(HOUR_OR_POD)){
				return g.equals(Granulaarsus.HOUR_OF_DAY) || g.equals(Granulaarsus.HOUR_OF_HALF_DAY);
			}
			if (this.equals(DAY)){
				return g.equals(Granulaarsus.DAY_OF_MONTH) || g.equals(Granulaarsus.DAY_OF_WEEK);
			}
			if (this.equals(MONTH_OR_WEEK)){
				return g.equals(Granulaarsus.MONTH) || g.equals(Granulaarsus.WEEK_OF_YEAR);
			}
			if (this.equals(YEAR)){
				return g.equals(Granulaarsus.YEAR) || g.equals(Granulaarsus.YEAR_OF_CENTURY) ||
									g.equals(Granulaarsus.CENTURY_OF_ERA);
			}
			return false;
		}
		
	};
	
	/**
	 *  Operations, that overwrite fields and do not depend of values of modifiable fields. 
	 */
	public final boolean SET_TYPE_OPERATION = true;

	/**
	 *  Operations, that increase/decrease values of modifiable fields, so the result depends on
	 *  the initial result of fields; 
	 */
	public final boolean ADD_TYPE_OPERATION = false;
	
	/**
	 *  Array that specifies order of VALUE_FIELD elements;
	 */
	public static final VALUE_FIELD valueFieldsInOrder [] = { VALUE_FIELD.YEAR, VALUE_FIELD.MONTH_OR_WEEK, 
                                                              VALUE_FIELD.DAY,  VALUE_FIELD.HOUR_OR_POD, 
                                                              VALUE_FIELD.MINUTE  };
	
	/**
	 *  Fields that are masked by X symbols. If a field is both in
	 *  maskedFields and openedFields, the first one is chosen for
	 *  final representation;  
	 */
	private HashMap<VALUE_FIELD, String> maskedFields = 
		new HashMap<TimeMLDateTimePoint.VALUE_FIELD, String>();
	
	/**
	 *  Fields that are opened and should appear in output. Fields
	 *  will be opened, when they are modified by some of the calendar
	 *  arithmetic operations. If a field is present both in maskedFields 
	 *  and openedFields, the first one is preferred.     
	 */
	private HashMap<VALUE_FIELD, String> openedFields = 
		new HashMap<TimeMLDateTimePoint.VALUE_FIELD, String>();

	/**
	 *  Fields given as initial input in object's constructor. If
	 *  during the whole normalization process no field is set in 
	 *  openedFields, these will be displayed.  
	 */
	private HashMap<VALUE_FIELD, String> inputFields = 
		new HashMap<TimeMLDateTimePoint.VALUE_FIELD, String>();

	/**
	 *  Granularities that have been directly modified by calendar 
	 *  arithmetic operations. "Direct modification" means that an
	 *  operation has been called with the granularity as an 
	 *  argument.
	 */
	private HashMap<Granulaarsus, String> modifiedGrans = 
		new HashMap<Granulaarsus, String>();
	
	//==============================================================================
	//   	V a r i a b l e s   :    c a l e n d a r   a r i t h m e t i c s
	//==============================================================================
	
	/**
	 *   Calendaric time object underlying this datetime representation.
	 */
	private LocalTime underlyingTime = null;
	
	/**
	 *   Calendaric date object underlying this datetime representation.
	 */
	private LocalDate underlyingDate = null;

	/**
   	 *     Part of day label, if set.
	 */
	private String partOfDay = null;
	
	/**
	 *    Whether at least one date value has been modified previously.
	 */
	private boolean dateModified = false;
	
	/**
	 *    Whether there was a function other than set used during normalization.    
	 */
	private boolean functionOtherThanSetUsed = false;
	
	//==============================================================================
	//   	I n i t i a l i z a t i o n
	//==============================================================================
	
	public TimeMLDateTimePoint(LocalDateTime lct) {
		this.underlyingTime = lct.toLocalTime();
		this.underlyingDate = lct.toLocalDate();
		inputFields.put(VALUE_FIELD.YEAR,          normalizeByAddingZeroes(VALUE_FIELD.YEAR,(this.underlyingDate).getYear()) );
		inputFields.put(VALUE_FIELD.MONTH_OR_WEEK, normalizeByAddingZeroes(VALUE_FIELD.MONTH_OR_WEEK,(this.underlyingDate).getMonthOfYear()) );
		inputFields.put(VALUE_FIELD.DAY,           normalizeByAddingZeroes(VALUE_FIELD.DAY,(this.underlyingDate).getDayOfMonth()) );
		inputFields.put(VALUE_FIELD.HOUR_OR_POD,   normalizeByAddingZeroes(VALUE_FIELD.HOUR_OR_POD,(this.underlyingTime).getHourOfDay()) );
		inputFields.put(VALUE_FIELD.MINUTE,        normalizeByAddingZeroes(VALUE_FIELD.MINUTE,(this.underlyingTime).getMinuteOfHour()) );
	}
	
	public TimeMLDateTimePoint(String [] granularities) {
		this( granularities[0], granularities[1], granularities[2], granularities[3], granularities[4] );
	}
	
	public TimeMLDateTimePoint(String year, String month, String day, String hour, String minute) {
		this.underlyingDate = new LocalDate();
		this.underlyingTime = new LocalTime();
		Pattern maskPattern = Pattern.compile("X+");
		if ((maskPattern.matcher(year)).matches()){
			(this.maskedFields).put(VALUE_FIELD.YEAR, year);
			(this.inputFields).put(VALUE_FIELD.YEAR, year);
		} else if (parseInteger(year) > -1){
			(this.inputFields).put(VALUE_FIELD.YEAR, normalizeByAddingZeroes(VALUE_FIELD.YEAR, year));
			this.underlyingDate = (this.underlyingDate).withYear( parseInteger(year) );
		}
		if ((maskPattern.matcher(month)).matches()){
			(this.maskedFields).put(VALUE_FIELD.MONTH_OR_WEEK, month);
			(this.inputFields).put(VALUE_FIELD.MONTH_OR_WEEK, month);
		} else if (parseInteger(month) > -1){
			(this.inputFields).put(VALUE_FIELD.MONTH_OR_WEEK, normalizeByAddingZeroes(VALUE_FIELD.MONTH_OR_WEEK, month));
			this.underlyingDate = (this.underlyingDate).withMonthOfYear( parseInteger(month) );
		}
		if ((maskPattern.matcher(day)).matches()){
			(this.maskedFields).put(VALUE_FIELD.DAY, day);
			(this.inputFields).put(VALUE_FIELD.DAY, day);
		} else if (parseInteger(day) > -1){
			(this.inputFields).put(VALUE_FIELD.DAY, normalizeByAddingZeroes(VALUE_FIELD.DAY, day));
			this.underlyingDate = (this.underlyingDate).withDayOfMonth( parseInteger(day) );
		}
		if ((maskPattern.matcher(hour)).matches()){
			(this.maskedFields).put(VALUE_FIELD.HOUR_OR_POD, hour);
			(this.inputFields).put(VALUE_FIELD.HOUR_OR_POD, hour);
		} else if (parseInteger(hour) > -1){
			(this.inputFields).put(VALUE_FIELD.HOUR_OR_POD, normalizeByAddingZeroes(VALUE_FIELD.HOUR_OR_POD, hour));
			this.underlyingTime = (this.underlyingTime).withHourOfDay( parseInteger(hour) );
		}
		if ((maskPattern.matcher(minute)).matches()){
			(this.maskedFields).put(VALUE_FIELD.MINUTE, minute);
			(this.inputFields).put(VALUE_FIELD.MINUTE, minute);
		} else if (parseInteger(minute) > -1){
			(this.inputFields).put(VALUE_FIELD.MINUTE, normalizeByAddingZeroes(VALUE_FIELD.MINUTE, minute));
			this.underlyingTime = (this.underlyingTime).withMinuteOfHour( parseInteger(minute) );
		}
	}
	
	public TimeMLDateTimePoint(TimeMLDateTimePoint refDateTime) {
		if (refDateTime.underlyingTime != null){
			this.underlyingTime = new LocalTime(refDateTime.underlyingTime);
		}
		if (refDateTime.underlyingDate != null){
			this.underlyingDate = new LocalDate(refDateTime.underlyingDate);
		}
		this.inputFields   = new HashMap<TimeMLDateTimePoint.VALUE_FIELD, String>(refDateTime.inputFields);
		this.maskedFields  = new HashMap<TimeMLDateTimePoint.VALUE_FIELD, String>(refDateTime.maskedFields);
		this.openedFields  = new HashMap<TimeMLDateTimePoint.VALUE_FIELD, String>(refDateTime.openedFields);
		this.modifiedGrans = new HashMap<Granulaarsus, String>(refDateTime.modifiedGrans);
		this.dateModified          = refDateTime.dateModified;
		this.functionOtherThanSetUsed = refDateTime.functionOtherThanSetUsed;
	}
	
	//==============================================================================
	//   	C a l e n d a r    A r i t h m e t i c s
	//==============================================================================
	
	//==================
	//   S E T
	//==================
	
	public void setField(Granulaarsus field, int value){
		// ------ Time
		if (field == Granulaarsus.AM_PM){
			try {			
				this.underlyingTime = (this.underlyingTime).withField(DateTimeFieldType.halfdayOfDay(), value);
				updateTimeRepresentation(field, null, false, SET_TYPE_OPERATION);
			} catch (Exception e) {
			}			
		}
		if (field == Granulaarsus.HOUR_OF_HALF_DAY){
			try {
				if (value == 12){ value = 0; }
				this.underlyingTime = (this.underlyingTime).withField(DateTimeFieldType.hourOfHalfday(), value);
				// NB! Tunni seadistamisel nullime ka minutid, et ei tekiks nt ankurdamisel kummalisi väärtuseid
				this.underlyingTime = (this.underlyingTime).withField(DateTimeFieldType.minuteOfHour(), 0);
				updateTimeRepresentation(field, null, false, SET_TYPE_OPERATION);
			} catch (Exception e) {
			}				
		}
		if (field == Granulaarsus.MINUTE){
			try {			
				this.underlyingTime = (this.underlyingTime).withField(DateTimeFieldType.minuteOfHour(), value);
				updateTimeRepresentation(field, null, false, SET_TYPE_OPERATION);
			} catch (Exception e) {
			}			
		}
		// ------ Kuup2evad ja n2dalad
		if (field == Granulaarsus.DAY_OF_WEEK){
			try {			
				this.underlyingDate = (this.underlyingDate).withField(DateTimeFieldType.dayOfWeek(), value);
				dateModified = true;
				updateDateRepresentation(field, null, false, SET_TYPE_OPERATION);
			} catch (Exception e) {
			}
		}
		if (field == Granulaarsus.WEEK_OF_YEAR){
			try {			
				this.underlyingDate = (this.underlyingDate).withField(DateTimeFieldType.weekOfWeekyear(), value);
				dateModified = true;
				updateDateRepresentation(field, null, false, SET_TYPE_OPERATION);
			} catch (Exception e) {
			}
		}		
		if (field == Granulaarsus.DAY_OF_MONTH){
			try {
				this.underlyingDate = (this.underlyingDate).withField(DateTimeFieldType.dayOfMonth(), value);
				dateModified = true;
				updateDateRepresentation(field, null, false, SET_TYPE_OPERATION);
			} catch (Exception e) {
			}
		}
		if (field == Granulaarsus.MONTH){
			try {
				this.underlyingDate = (this.underlyingDate).withField(DateTimeFieldType.monthOfYear(), value);
				dateModified = true;
				updateDateRepresentation(field, null, false, SET_TYPE_OPERATION);
			} catch (Exception e) {
			}			
		}
		if (field == Granulaarsus.YEAR){
			try {
				this.underlyingDate = (this.underlyingDate).withField(DateTimeFieldType.year(), value);
				dateModified = true;
				updateDateRepresentation(field, null, false, SET_TYPE_OPERATION);
			} catch (Exception e) {
			}
		}
		if (field == Granulaarsus.YEAR_OF_CENTURY){
			try {
				this.underlyingDate = (this.underlyingDate).withField(DateTimeFieldType.yearOfCentury(), value);
				dateModified = true;
				// NB! Toimib nagu tavalise aastaarvu muutmine
				updateDateRepresentation(field, null, false, SET_TYPE_OPERATION);
			} catch (Exception e) {
			}
		}		
		if (field == Granulaarsus.CENTURY_OF_ERA){
			try {
				this.underlyingDate = (this.underlyingDate).withField(DateTimeFieldType.centuryOfEra(), value);
				dateModified = true;
				updateDateRepresentation(field, null, false, SET_TYPE_OPERATION);
			} catch (Exception e) {
			}			
		}		
	}
	
	public void setField(Granulaarsus field, String label){
		if (field == Granulaarsus.TIME){
			// k6ik p2evaosad on eeldatavasti TIME all
			updateTimeRepresentation(field, label, false, SET_TYPE_OPERATION);
		}
		if (field == Granulaarsus.DAY_OF_WEEK){
			// t88p2ev v6i n2dalal6pp: sunnime peale n2dalap6hise esituse
			openedFields.put(VALUE_FIELD.DAY, label);
			updateDateRepresentation(field, label, true, SET_TYPE_OPERATION);
			dateModified = true;
		}
		if (field == Granulaarsus.MONTH){
			openedFields.put(VALUE_FIELD.MONTH_OR_WEEK, label);
			updateDateRepresentation(field, label, true, SET_TYPE_OPERATION);
			dateModified = true;
		}
	}
	
	
	//==================
	//   A D D 
	//==================
	
	public void addToField(Granulaarsus field, int value) {
		if (field == Granulaarsus.MINUTE || 
			field == Granulaarsus.HOUR_OF_HALF_DAY ||
			field == Granulaarsus.HOUR_OF_DAY){
			// Put date and time together
			LocalDateTime ajaFookus = getAsLocalDateTime(); 
			// perform operation
			if (field == Granulaarsus.MINUTE){
				ajaFookus = ajaFookus.plusMinutes(value);
				this.underlyingTime = ajaFookus.toLocalTime();
				this.underlyingDate = ajaFookus.toLocalDate();
				if (dateModified){
					updateDateRepresentation(Granulaarsus.DAY_OF_MONTH, null, false, ADD_TYPE_OPERATION);
				}
				updateTimeRepresentation(field, null, false, ADD_TYPE_OPERATION);
				functionOtherThanSetUsed = true;
			}
			if (field == Granulaarsus.HOUR_OF_DAY || field == Granulaarsus.HOUR_OF_HALF_DAY){
				ajaFookus = ajaFookus.plusHours(value);
				this.underlyingTime = ajaFookus.toLocalTime();
				this.underlyingDate = ajaFookus.toLocalDate();
				if (dateModified){
					updateDateRepresentation(Granulaarsus.DAY_OF_MONTH, null, false, ADD_TYPE_OPERATION);
				}
				updateTimeRepresentation(field, null, false, ADD_TYPE_OPERATION);
				functionOtherThanSetUsed = true;
			}			
		}
		if (field == Granulaarsus.DAY_OF_MONTH){
			this.underlyingDate = (this.underlyingDate).plusDays(value);
			updateDateRepresentation(field, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			dateModified = true;
		}
		if (field == Granulaarsus.DAY_OF_WEEK){
			this.underlyingDate = (this.underlyingDate).plusDays(value);
			updateDateRepresentation(field, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			dateModified = true;
		}
		if (field == Granulaarsus.WEEK_OF_YEAR){
			this.underlyingDate = (this.underlyingDate).plusWeeks(value);
			updateDateRepresentation(field, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			dateModified = true;
		}
		if (field == Granulaarsus.MONTH){
			this.underlyingDate = (this.underlyingDate).plusMonths(value);
			updateDateRepresentation(field, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			dateModified = true;
		}
		if (field == Granulaarsus.YEAR){
			this.underlyingDate = (this.underlyingDate).plusYears(value);
			updateDateRepresentation(field, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			dateModified = true;
		}
		if (field == Granulaarsus.YEAR_OF_CENTURY){
			// NB! Toimib nagu tavalise aastaarvu muutmine			
			this.underlyingDate = (this.underlyingDate).plusYears(value);
			updateDateRepresentation(field, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			dateModified = true;
		}		
		if (field == Granulaarsus.CENTURY_OF_ERA){
			this.underlyingDate = (this.underlyingDate).plusYears(value*100);
			updateDateRepresentation(field, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			dateModified = true;
		}		
	}
	
	public void addToField(Granulaarsus field, Period period, int direction){
		if (field.compareByCoarseRank(Granulaarsus.WEEK_OF_YEAR) == -1){
			// ---- paev, tund, minut
			LocalDateTime ajaFookus = getAsLocalDateTime(); 
			try {
				if (direction > 0){
					ajaFookus = ajaFookus.plus( period );
				} else {
					ajaFookus = ajaFookus.minus( period );
				}
				this.functionOtherThanSetUsed = true;
			} catch (Exception e) {
				// Erindi korral jaabki muutmata
			}
			this.underlyingTime = ajaFookus.toLocalTime();
			this.underlyingDate = ajaFookus.toLocalDate();
			if (field.compareByCoarseRank(Granulaarsus.DAY_OF_MONTH) == 0){
				this.dateModified = true;
				updateTimeRepresentation(Granulaarsus.HOUR_OF_DAY, null, false, ADD_TYPE_OPERATION);
			} else {
				updateTimeRepresentation(Granulaarsus.MINUTE, null, false, ADD_TYPE_OPERATION);
			}
			if (dateModified){
				updateDateRepresentation(Granulaarsus.DAY_OF_MONTH, null, false, ADD_TYPE_OPERATION);				
			}
		} else {
			// ---- nadal, kuu, aasta, sajand
			try {
				if (direction > 0){
					this.underlyingDate = (this.underlyingDate).plus( period );
				} else {
					this.underlyingDate = (this.underlyingDate).minus( period );
				}
				this.functionOtherThanSetUsed = true;
				this.dateModified = true;
			} catch (Exception e) {
				// Erindi korral jaabki muutmata
			}			
			if (field == Granulaarsus.MONTH || field == Granulaarsus.WEEK_OF_YEAR){
				updateDateRepresentation(Granulaarsus.DAY_OF_MONTH, null, false, ADD_TYPE_OPERATION);
			}
			if (field == Granulaarsus.YEAR){
				updateDateRepresentation(Granulaarsus.MONTH, null, false, ADD_TYPE_OPERATION);
			}
			if (field == Granulaarsus.CENTURY_OF_ERA){
				updateDateRepresentation(Granulaarsus.YEAR, null, false, ADD_TYPE_OPERATION);
			}			
		}
	}
	
	//==================
	//   S E E K
	//==================

	public void seekField(Granulaarsus field, int direction, int soughtValue, boolean excludingCurrent){
		// ---------------------------------
		//  DAY_OF_MONTH
		// ---------------------------------
		if (field == Granulaarsus.DAY_OF_MONTH && direction != 0 && soughtValue == 0){
			int dir = (direction > 0) ? (1) : (-1);
			LocalDate ajaFookus = new LocalDate(this.underlyingDate);
			LocalDate nihutatudFookus = ajaFookus.plusDays(1 * dir);
			ajaFookus = nihutatudFookus;
			this.underlyingDate = ajaFookus;
			updateDateRepresentation(Granulaarsus.DAY_OF_MONTH, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			this.dateModified = true;
		}
		// ---------------------------------
		//  DAY_OF_WEEK
		// ---------------------------------
		if (field == Granulaarsus.DAY_OF_WEEK && 
				soughtValue >= DateTimeConstants.MONDAY && soughtValue <= DateTimeConstants.SUNDAY &&
					direction != 0){
			int dir = (direction > 0) ? (1) : (-1);
			LocalDate ajaFookus = new LocalDate(this.underlyingDate);
			// Algne p2ev ehk p2ev, millest tahame tingimata m66duda
			int algneNadalapaev = (excludingCurrent) ? (ajaFookus.getDayOfWeek()) : (-1);
			if (!excludingCurrent){
				// V6tame sammu tagasi, et esimene nihe tooks meid t2pselt k2esolevale p2evale				
				ajaFookus = ajaFookus.plusDays(dir * (-1));
			}
			int count = 0;
			while( true ){
				LocalDate nihutatudFookus = ajaFookus.plusDays(1 * dir);
				ajaFookus = nihutatudFookus;				
				int uusNadalapaev = ajaFookus.getDayOfWeek();
				if (algneNadalapaev != -1){
					 if (algneNadalapaev == uusNadalapaev){
						 continue;
					 } else {
						 algneNadalapaev = -1;
					 }
				}
				if (uusNadalapaev == soughtValue){
					algneNadalapaev = uusNadalapaev;
					count++;
					if (count == Math.abs(direction)){
						break;
					}
				}
			}
			this.underlyingDate = ajaFookus;
			updateDateRepresentation(Granulaarsus.DAY_OF_MONTH, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			this.dateModified = true;
		}
		// ---------------------------------
		//  WEEK OF YEAR
		// ---------------------------------
		if (field == Granulaarsus.WEEK_OF_YEAR && soughtValue == 0 && direction != 0){
			int dir = (direction > 0) ? (1) : (-1);
			LocalDate ajaFookus = new LocalDate( this.underlyingDate ); 
			// Algne n2dal ehk n2dal, millest tahame m88duda
			int algneNadal = (excludingCurrent) ? (ajaFookus.getWeekOfWeekyear()) : (-1);
			if (!excludingCurrent){
				// V6tame sammu tagasi, et esimene nihe tooks meid t2pselt k2esolevale p2evale				
				ajaFookus = ajaFookus.plusDays(dir * (-1));
			}			
			int count = 0;
			while( true ){
				LocalDate nihutatudFookus = ajaFookus.plusDays(1 * dir);
				ajaFookus = nihutatudFookus;
				int uusNadal= nihutatudFookus.getWeekOfWeekyear();
				if (algneNadal != -1){
					 if (algneNadal == uusNadal){
						 continue;
					 } else {
						 algneNadal = -1;
					 }
				}
				if (soughtValue == 0){
					algneNadal = uusNadal;
					count++;
					if (count == Math.abs(direction)){
						break;
					}
				}
			}
			this.underlyingDate = ajaFookus;
			updateDateRepresentation(Granulaarsus.WEEK_OF_YEAR, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			this.dateModified = true;
		}
		// ---------------------------------
		//  MONTH
		// ---------------------------------
		if (field == Granulaarsus.MONTH && 
				(soughtValue == 0 || 
						DateTimeConstants.JANUARY <= soughtValue && soughtValue <= DateTimeConstants.DECEMBER) && 
					direction != 0){
			int dir = (direction > 0) ? (1) : (-1);
			LocalDate ajaFookus = new LocalDate(this.underlyingDate); 
			// Algne kuu ehk kuu, millest tahame m88duda
			int algneKuu = (excludingCurrent) ? (ajaFookus.getMonthOfYear()) : (-1);
			if (!excludingCurrent){
				// V6tame sammu tagasi, et esimene nihe tooks meid t2pselt k2esolevale kuule		
				ajaFookus = ajaFookus.plusMonths(dir * (-1));
			}			
			int count = 0;
			while( true ){
				LocalDate nihutatudFookus = ajaFookus.plusMonths(1 * dir);
				ajaFookus = nihutatudFookus;
				int uusKuu = nihutatudFookus.getMonthOfYear();
				if (algneKuu != -1){
					 if (algneKuu == uusKuu){
						 continue;
					 } else {
						 algneKuu = -1;
					 }
				}
				if (soughtValue == 0 || (soughtValue != 0 && uusKuu == soughtValue)){
					algneKuu = uusKuu;
					count++;
					if (count == Math.abs(direction)){
						break;
					}
				}
			}
			this.underlyingDate = ajaFookus;
			updateDateRepresentation(Granulaarsus.MONTH, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			this.dateModified = true;
		}		
		// ---------------------------------
		//   YEAR
		// ---------------------------------
		if (field == Granulaarsus.YEAR && soughtValue == 0 && direction != 0){
			int dir = (direction > 0) ? (1) : (-1);
			LocalDate ajaFookus = new LocalDate(this.underlyingDate); 
			// Algne aasta ehk aasta, millest tahame m88duda
			int algneAasta = ( excludingCurrent ) ? ( ajaFookus.getYear() ) : ( -1 );
			if (!excludingCurrent){
				// V6tame sammu tagasi, et esimene nihe tooks meid t2pselt k2esolevale kuule		
				ajaFookus = ajaFookus.plusMonths(dir * (-1));
			}				
			int count = 0;
			while( true ){
				LocalDate nihutatudFookus = ajaFookus.plusMonths(1 * dir);
				ajaFookus = nihutatudFookus;
				int uusAasta = nihutatudFookus.getYear();
				if (algneAasta != -1){
					 if (algneAasta == uusAasta){
						 continue;
					 } else {
						 algneAasta = -1;
					 }
				}
				if (soughtValue == 0){
					algneAasta = uusAasta;
					count++;
					if (count == Math.abs(direction)){
						break;
					}
				}
			}
			this.underlyingDate = ajaFookus;
			updateDateRepresentation(Granulaarsus.YEAR, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			this.dateModified = true;
		}
		// ---------------------------------
		//   YEAR_OF_CENTURY
		// ---------------------------------
		if (field == Granulaarsus.YEAR_OF_CENTURY && direction != 0){
			int minValue = SemLeidmiseAbimeetodid.
							getLocalDateTimeFieldExtremum(
									this.underlyingDate, DateTimeFieldType.yearOfCentury(), false);
			int maxValue = SemLeidmiseAbimeetodid.
							getLocalDateTimeFieldExtremum(
									this.underlyingDate, DateTimeFieldType.yearOfCentury(), true);
			if (minValue <= soughtValue && soughtValue <= maxValue){
				int dir = (direction > 0) ? (1) : (-1);
				LocalDate ajaFookus = new LocalDate(this.underlyingDate);
				// Algne aasta ehk aasta, millest tahame m88duda
				int algneAasta = ( excludingCurrent ) ? ( ajaFookus.getYearOfCentury() ) : ( -1 );
				if (!excludingCurrent){
					// V6tame sammu tagasi, et esimene nihe tooks meid t2pselt k2esolevale aastale		
					ajaFookus = ajaFookus.plusYears(dir * (-1));
				}				
				int count      = 0;
				int cycleCount = 0;
				while( true ){
					LocalDate nihutatudFookus = ajaFookus.plusYears(1 * dir);
					cycleCount++;
					ajaFookus = nihutatudFookus;
					int uusAasta = nihutatudFookus.getYearOfCentury();
					if (algneAasta != -1){
						 if (algneAasta == uusAasta){
							 continue;
						 } else {
							 algneAasta = -1;
						 }
					}
					if (uusAasta == soughtValue){
						algneAasta = uusAasta;
						count++;
						if (count == Math.abs(direction)){
							break;
						}
					}
				}
				this.underlyingDate = ajaFookus;
				updateDateRepresentation(Granulaarsus.YEAR, null, false, ADD_TYPE_OPERATION);
				functionOtherThanSetUsed = true;
				this.dateModified = true;
			}
		}		
		// ---------------------------------
		//   CENTURY_OF_ERA
		// ---------------------------------
		if (field == Granulaarsus.CENTURY_OF_ERA && soughtValue == 0 && direction != 0){
			int dir = (direction > 0) ? (1) : (-1);
			LocalDate ajaFookus = new LocalDate(this.underlyingDate); 
			// Algne saj ehk sajand, millest tahame m88duda
			int algneSajand = ( excludingCurrent ) ? ( ajaFookus.getCenturyOfEra() ) : ( Integer.MIN_VALUE );
			if (!excludingCurrent){
				// V6tame sammu tagasi, et esimene nihe tooks meid t2pselt k2esolevale aastale		
				ajaFookus = ajaFookus.plusYears(dir * (-10));
			}				
			int count = 0;
			while( true ){
				LocalDate nihutatudFookus = ajaFookus.plusYears(10 * dir);
				ajaFookus = nihutatudFookus;
				int uusSajand = nihutatudFookus.getCenturyOfEra();
				if (algneSajand != Integer.MIN_VALUE){
					 if (algneSajand == uusSajand){
						 continue;
					 } else {
						 algneSajand = Integer.MIN_VALUE;
					 }
				}
				if (soughtValue == 0){
					algneSajand = uusSajand;
					count++;
					if (count == Math.abs(direction)){
						break;
					}
				}
			}
			this.underlyingDate = ajaFookus;
			updateDateRepresentation(Granulaarsus.CENTURY_OF_ERA, null, false, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			this.dateModified = true;
		}
		
	}
	
	public void seekField(Granulaarsus field, int direction, String soughtValue, boolean excludingCurrent){
		// ---------------------------------
		//  PART OF DAY
		// ---------------------------------
		if (field == Granulaarsus.TIME && 
				soughtValue != null && soughtValue.matches("(NI|MO|AF|EV)") 
					&& direction != 0){
			int dir = (direction > 0) ? (1) : (-1);
			// Loome k2esolevat ajafookust t2ielikult kirjeldava objekti
			LocalDateTime ajaFookus = getAsLocalDateTime(); 
			// AlgneMargend ehk p2evaosa, millest peame ennem m66da saama, kuni v6ime uue
			// v22rtuse omaks v6tta
			String algneMargend = (excludingCurrent) ? (SemLeidmiseAbimeetodid.getPartOfDay(ajaFookus.toLocalTime())) : (null);
			if (!excludingCurrent){
				// V6tame sammu tagasi, et esimene nihe tooks meid t2pselt k2esolevale tunnile	
				ajaFookus = ajaFookus.plusHours(dir * (-1));
			}
			int count = 0;
			while( true ){
				LocalDateTime uusFookus = ajaFookus.plusHours(1*dir);
				ajaFookus = uusFookus;				
				String newPartOfDay = SemLeidmiseAbimeetodid.getPartOfDay(uusFookus.toLocalTime());
				if (algneMargend != null){
					 if (algneMargend.equals(newPartOfDay)){
						 continue;
					 } else {
						 algneMargend = null;
					 }
				}
				if (newPartOfDay != null && newPartOfDay.equals(soughtValue)){
					algneMargend = newPartOfDay;
					count++;
					if (count == Math.abs(direction)){
						break;
					}
				}
			}
			this.partOfDay = soughtValue;
			updateTimeRepresentation(Granulaarsus.TIME, soughtValue, true, ADD_TYPE_OPERATION);
			this.underlyingTime = ajaFookus.toLocalTime();
			this.underlyingDate = ajaFookus.toLocalDate();
			functionOtherThanSetUsed = true;
		}
		// ---------------------------------
		//  WORKDAY OR WEEKEND
		// ---------------------------------
		if (field == Granulaarsus.DAY_OF_WEEK && 
				soughtValue != null && soughtValue.matches("(WD|WE)") && direction != 0){
			int dir = (direction > 0) ? (1) : (-1);
			LocalDate ajaFookus = new LocalDate(this.underlyingDate); 
			// Algne p2ev ehk p2ev, millest tahame tingimata m66duda
			String algneMargend = (excludingCurrent) ? (SemLeidmiseAbimeetodid.getWordDayOrWeekend(ajaFookus)) : (null);
			if (!excludingCurrent){
				// V6tame sammu tagasi, et esimene nihe tooks meid t2pselt k2esolevale p2evale
				ajaFookus = ajaFookus.plusDays(dir * (-1));
			}
			int count = 0;
			while( true ){
				LocalDate nihutatudFookus = ajaFookus.plusDays(1 * dir);
				ajaFookus = nihutatudFookus;
				String toopaevVoiNadalalopp = SemLeidmiseAbimeetodid.getWordDayOrWeekend(ajaFookus);
				if (algneMargend != null){
					 if (algneMargend.equals(toopaevVoiNadalalopp)){
						 continue;
					 } else {
						 algneMargend = null;
					 }
				}
				if (toopaevVoiNadalalopp.equals(soughtValue)){
					algneMargend = toopaevVoiNadalalopp;
					count++;
					if (count == Math.abs(direction)){
						break;
					}
				}
			}
			this.underlyingDate  = ajaFookus;
			(this.openedFields).put(VALUE_FIELD.DAY, soughtValue);
			updateDateRepresentation(Granulaarsus.DAY_OF_WEEK, soughtValue, true, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			this.dateModified = true;
		}
		// ---------------------------------
		//  SEASONs
		// ---------------------------------		
		if (field == Granulaarsus.MONTH && 
				soughtValue != null && soughtValue.matches("(SP|SU|FA|WI)") && direction != 0){
			int dir = (direction > 0) ? (1) : (-1);
			LocalDate ajaFookus = new LocalDate(this.underlyingDate); 
			// Algne aastaaeg ehk aastaaeg, millest tahame tingimata m66duda
			String algneMargend = (excludingCurrent) ? (SemLeidmiseAbimeetodid.getSeason(ajaFookus)) : (null);
			if (!excludingCurrent){
				// V6tame sammu tagasi, et esimene nihe tooks meid t2pselt k2esolevale kuule
				ajaFookus = ajaFookus.plusMonths(dir * (-1));
			}
			int count = 0;
			while( true ){
				LocalDate nihutatudFookus = ajaFookus.plusMonths(1 * dir);
				ajaFookus = nihutatudFookus;
				String aastaaeg = SemLeidmiseAbimeetodid.getSeason(ajaFookus);
				if (algneMargend != null){
					 if (algneMargend.equals(aastaaeg)){
						 continue;
					 } else {
						 algneMargend = null;
					 }
				}
				if (aastaaeg.equals(soughtValue)){
					algneMargend = aastaaeg;
					count++;
					if (count == Math.abs(direction)){
						break;
					}
				}
			}
			this.underlyingDate = ajaFookus;
			// Detsembri puhul liigume j2rgmisesse aastasse (st - talve loetakse aasta algusest) ...
			if ( (this.underlyingDate).getMonthOfYear() == DateTimeConstants.DECEMBER ){
				this.underlyingDate = (this.underlyingDate).plusMonths(1);
			}
			openedFields.put(VALUE_FIELD.MONTH_OR_WEEK, soughtValue);
			updateDateRepresentation(Granulaarsus.MONTH, soughtValue, true, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			this.dateModified = true;
		}
		// ---------------------------------
		//  QUARTERs
		// ---------------------------------		
		if (field == Granulaarsus.MONTH && 
				soughtValue != null && soughtValue.matches("Q(1|2|3|4)") && direction != 0){
			int dir = (direction > 0) ? (1) : (-1);
			LocalDate ajaFookus = new LocalDate(this.underlyingDate); 
			// Algne kvartal ehk kvartal, millest tahame tingimata m66duda
			String algneMargend = (excludingCurrent) ? (SemLeidmiseAbimeetodid.getQuarterOfYear(ajaFookus)) : (null);
			if (!excludingCurrent){
				// V6tame sammu tagasi, et esimene nihe tooks meid t2pselt k2esolevale kuule
				ajaFookus = ajaFookus.plusMonths(dir * (-1));
			}
			int count = 0;
			while( true ){
				LocalDate nihutatudFookus = ajaFookus.plusMonths(1 * dir);
				ajaFookus = nihutatudFookus;
				String kvartal = SemLeidmiseAbimeetodid.getQuarterOfYear(ajaFookus);
				if (algneMargend != null){
					 if (algneMargend.equals(kvartal)){
						 continue;
					 } else {
						 algneMargend = null;
					 }
				}
				if (kvartal.equals(soughtValue)){
					algneMargend = kvartal;
					count++;
					if (count == Math.abs(direction)){
						break;
					}
				}
			}
			this.underlyingDate = ajaFookus;
			String newQuarterVal = SemLeidmiseAbimeetodid.getQuarterOfYear( this.underlyingDate );
			openedFields.put(VALUE_FIELD.MONTH_OR_WEEK, newQuarterVal );
			updateDateRepresentation(Granulaarsus.MONTH, newQuarterVal, true, ADD_TYPE_OPERATION);
			functionOtherThanSetUsed = true;
			this.dateModified = true;
		}
	}
	
	//=====================
	//    B A L D W I N
	//     W I N D O W
	//=====================
	
	/**
	 *  Applies the algorithm of Baldwin's window (as implemented in {@link SemLeidmiseAbimeetodid}),
	 *  in order to find closest moment of time within a Baldwin's window, which satisfies the 
	 *  constraint <tt>field = soughtValue</tt>.
	 *  <br>
	 *  <br>
	 *  Only limited number of granularities are supported currently.
	 */
	public void applyBalwinWindow(Granulaarsus field, String soughtValue){
		if (soughtValue != null){
			LocalDateTime ajaFookus = getAsLocalDateTime();
			try {
				int soughtValueAsInt = Integer.parseInt(soughtValue);
				// Only some date fields currently supported
				LocalDateTime uusFookus = SemLeidmiseAbimeetodid.applyBaldwinWindow(field, ajaFookus, soughtValueAsInt);
				this.underlyingTime = uusFookus.toLocalTime();
				this.underlyingDate = uusFookus.toLocalDate();
				if (field == Granulaarsus.DAY_OF_WEEK){
					// Force the usage of month, in order to get canonical form in output 
					field = Granulaarsus.DAY_OF_MONTH;
				}
				updateDateRepresentation(field, null, false, ADD_TYPE_OPERATION);
				functionOtherThanSetUsed = true;
				this.dateModified = true;
			} catch (NumberFormatException e) {
				LocalDateTime uusFookus = SemLeidmiseAbimeetodid.applyBaldwinWindow(field, ajaFookus, soughtValue);
				if (uusFookus != null){
					// NB! Currently only season labels and quarter labels are supported
					if (field == Granulaarsus.MONTH){
						this.underlyingTime = uusFookus.toLocalTime();
						this.underlyingDate = uusFookus.toLocalDate();
						openedFields.put(VALUE_FIELD.MONTH_OR_WEEK, soughtValue);
						updateDateRepresentation(Granulaarsus.MONTH, soughtValue, true, ADD_TYPE_OPERATION);
						functionOtherThanSetUsed = true;
						this.dateModified = true;
					}
				}
			}
		}
	}
	
	//=====================
	//  F I N D   N - TH 
	//   S U B G R A N
	//=====================
	
	/**
	 *  Applies method {@link SemLeidmiseAbimeetodid#findNthSubpartOfGranularity(Granulaarsus, Granulaarsus, int, int, LocalDateTime)}}. 
	 */
	public void findNthSubpartOfGranularity(Granulaarsus superField, Granulaarsus subField, int soughtValueOfSubField, int n){
		LocalDateTime ajaFookus = getAsLocalDateTime(); 
		LocalDateTime uusFookus = SemLeidmiseAbimeetodid.findNthSubpartOfGranularity(superField, 
				                                                                     subField, 
				                                                                     soughtValueOfSubField, 
				                                                                     n, 
				                                                                     ajaFookus);
		if (uusFookus != null){
			if (superField == Granulaarsus.MONTH){
				this.underlyingTime = uusFookus.toLocalTime();
				this.underlyingDate = uusFookus.toLocalDate();
				if (subField == Granulaarsus.DAY_OF_WEEK){
					if (!(this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK)){
						// open month and dayOfMonth, otherwise fields will remain closed
						manageMaskedFields(Granulaarsus.MONTH, null, false, SET_TYPE_OPERATION);
					} 
					updateDateRepresentation(Granulaarsus.DAY_OF_MONTH, null, false, ADD_TYPE_OPERATION);
					functionOtherThanSetUsed = true;
					this.dateModified = true;
				} else if (subField == Granulaarsus.WEEK_OF_YEAR){
					if (!(this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK)){
						// open month and dayOfMonth, otherwise fields will remain closed
						manageMaskedFields(Granulaarsus.MONTH, null, false, SET_TYPE_OPERATION);
					}
					updateDateRepresentation(Granulaarsus.WEEK_OF_YEAR, null, false, ADD_TYPE_OPERATION);
					functionOtherThanSetUsed = true;
					this.dateModified = true;
				} 				
			}
		}
	}
	
	//==============================================================================
	//   	U p d a t e    r e p r e s e n t a t i o n
	//==============================================================================
	
	/**
	 *    Updates date representation (year, month, day) in openedFields. All fields greater than changedGranularity 
	 *   will be opened. Field that contains changedGranularity will be overwritten when 
	 *   skipOverwritingChangedOne == false;
	 *   <br><br>
	 *   When flag releaseMaskAtLowerGranularities is set, the input flag skipRemovingMaskFromChangedOne will be used
	 *   to decide, whether mask will be removed from the field associated with changedGranularity.
	 */
	private void updateDateRepresentation(Granulaarsus changedGranularity, String label, 
					boolean skipOverwritingChangedOne, boolean setOrAddOperation){
		// Memorize modification of the field 
		(this.modifiedGrans).put(changedGranularity, "1");
		if (label == null){
			if ((isModified(Granulaarsus.DAY_OF_WEEK) && isModified(Granulaarsus.WEEK_OF_YEAR)) &&
				(changedGranularity == Granulaarsus.WEEK_OF_YEAR || changedGranularity == Granulaarsus.DAY_OF_WEEK)){
				// Only if week/month is not masked
				if (!(this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK)){
					// If week has been set, and now day of week is added, switch to month-based representation
					// month-based representation is always prefered over week-based one
					(this.openedFields).put(VALUE_FIELD.MONTH_OR_WEEK, 
							normalizeByAddingZeroes(VALUE_FIELD.MONTH_OR_WEEK, (this.underlyingDate).getMonthOfYear()) );
					(this.openedFields).put(VALUE_FIELD.DAY, 
							normalizeByAddingZeroes(VALUE_FIELD.DAY, (this.underlyingDate).getDayOfMonth()) );
				}
			}			
		}
		// Usual cases: full year, month, day
		for (int i = 0; i < 3; i++) {
			VALUE_FIELD field = valueFieldsInOrder[i];
			if ( field.matchesGranularity(changedGranularity) ){
				//
				// A) Open all upper granularities
				//
				int j = i;
				while (j > -1){
					VALUE_FIELD upperOrSameField = valueFieldsInOrder[j];
					if (j == i && skipOverwritingChangedOne){
						j--;
						continue;
					}
					if (upperOrSameField == VALUE_FIELD.YEAR){
						String yearStr = String.valueOf((this.underlyingDate).getYear());
						// If the year is BeforeCommonEra, remove minus; "BC" should be sufficient;
						if (yearStr.startsWith("-")){
							yearStr = yearStr.substring(1);
						}
						yearStr = normalizeByAddingZeroes( VALUE_FIELD.YEAR, yearStr );
						if (((this.underlyingDate).era()).get() == DateTimeConstants.BCE){
							yearStr = "BC" + yearStr;
						}
						(this.openedFields).put( upperOrSameField, yearStr );
					}
					if (upperOrSameField == VALUE_FIELD.MONTH_OR_WEEK){
						if ( (isModified(Granulaarsus.WEEK_OF_YEAR) && !isModified(Granulaarsus.DAY_OF_WEEK))  || 
							 (isModified(Granulaarsus.DAY_OF_WEEK)  && !isModified(Granulaarsus.WEEK_OF_YEAR)) ||
							 (isModified(Granulaarsus.DAY_OF_WEEK)  && isModified(Granulaarsus.WEEK_OF_YEAR) && 
										 (this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) ) ||
							 (isModified(Granulaarsus.DAY_OF_WEEK) && isModified(Granulaarsus.WEEK_OF_YEAR) &&
							  changedGranularity == Granulaarsus.DAY_OF_WEEK && label != null)
										 
						   ){
							(this.openedFields).put(upperOrSameField, 
								"W"+normalizeByAddingZeroes(VALUE_FIELD.MONTH_OR_WEEK, (this.underlyingDate).getWeekOfWeekyear()) );
							if ((this.maskedFields).containsKey(upperOrSameField)){
								(this.maskedFields).put(upperOrSameField, "WXX");
							}
						} else {
							(this.openedFields).put(upperOrSameField, 
								normalizeByAddingZeroes(VALUE_FIELD.MONTH_OR_WEEK, (this.underlyingDate).getMonthOfYear()) );
							if ((this.maskedFields).containsKey(upperOrSameField)){
								(this.maskedFields).put(upperOrSameField, "XX");
							}
						}
					}
					if (upperOrSameField == VALUE_FIELD.DAY){
						if ( (isModified(Granulaarsus.WEEK_OF_YEAR) && !isModified(Granulaarsus.DAY_OF_WEEK))  || 
							 (isModified(Granulaarsus.DAY_OF_WEEK)  && !isModified(Granulaarsus.WEEK_OF_YEAR)) ||
							 (isModified(Granulaarsus.DAY_OF_WEEK)  &&  isModified(Granulaarsus.WEEK_OF_YEAR) && 
									 (this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK))
						   ){
							(this.openedFields).put(upperOrSameField, 
								String.valueOf((this.underlyingDate).getDayOfWeek()) );					
						} else {
							(this.openedFields).put(upperOrSameField, 
								normalizeByAddingZeroes(VALUE_FIELD.DAY, (this.underlyingDate).getDayOfMonth()) );							
						}
					}
					j--;
				}
				break;
			}
		}
		// century of era has been set: cut year to century level
		if (isModified(Granulaarsus.CENTURY_OF_ERA) && !isModified(Granulaarsus.YEAR) && 
				!isModified(Granulaarsus.YEAR_OF_CENTURY) && (this.openedFields).containsKey(VALUE_FIELD.YEAR) &&
						(this.openedFields).get(VALUE_FIELD.YEAR).matches("(BC)?\\d{2,4}")){
			String newYearVal = String.valueOf((this.underlyingDate).getCenturyOfEra());
			if (newYearVal.length() == 1){
				newYearVal = "0" + newYearVal;
			}
			if (((this.underlyingDate).era()).get() == DateTimeConstants.BCE){
				newYearVal = "BC" + newYearVal;
			}
			openedFields.put( VALUE_FIELD.YEAR, newYearVal );
		}
		// Manage masks on fields: open or close fields, if necessary
		manageMaskedFields(changedGranularity, label, skipOverwritingChangedOne, setOrAddOperation);
	}
	
	
	/**
	 *   Updates time representation (hour, AM/PM, minute) in openedFields. If hour-field will be modified, 
	 *   any existing values on minute-field will be deleted. If setLabel contains a part-of-day label and 
	 *   granularity is TIME, AM/PM settings will be modified, according to current hourOfHalfday value in 
	 *   underlyingTime.
	 *   <br><br>
	 *   When flag releaseMaskAtLowerGranularities is set, the input flag skipRemovingMaskFromChangedOne will
	 *   be used to decide, whether mask will be removed from the field associated with changedGranularity.
	 */
	private void updateTimeRepresentation(Granulaarsus field, String setLabel, 
			boolean skipOverwritingChangedOne, boolean setOrAddOperation){
		// Memorize label and modification of the field 
		if (field == Granulaarsus.TIME && setLabel != null){
			this.partOfDay = setLabel;
		}
		(this.modifiedGrans).put(field, "1");
		if (setLabel == null){
			if (!skipOverwritingChangedOne){
				//
				// HOUR_OF_HALF_DAY:
				//
				if (field == Granulaarsus.AM_PM){
					(this.openedFields).put(VALUE_FIELD.HOUR_OR_POD, 
							normalizeByAddingZeroes(VALUE_FIELD.HOUR_OR_POD, (this.underlyingTime).getHourOfDay()) );
					//(this.openedFields).remove(VALUE_FIELD.MINUTE);
				}
				if (field == Granulaarsus.HOUR_OF_HALF_DAY){
					if (isModified(Granulaarsus.AM_PM)){
						(this.openedFields).put(VALUE_FIELD.HOUR_OR_POD, 
								normalizeByAddingZeroes(VALUE_FIELD.HOUR_OR_POD, (this.underlyingTime).getHourOfDay()) );
						(this.openedFields).remove(VALUE_FIELD.MINUTE);
					} else {
						// Add to openedFields, so we can assume openedFields contains at least one value
						(this.openedFields).put(VALUE_FIELD.HOUR_OR_POD, 
								normalizeByAddingZeroes( VALUE_FIELD.HOUR_OR_POD, (this.underlyingTime).getHourOfDay()) );
					}
				}
				//
				//   MINUTE: if HOUR_OR_POD is unset or contains part-of-day, cover it with a mask
				//
				if (field == Granulaarsus.MINUTE){
					(this.openedFields).put(VALUE_FIELD.MINUTE, 
							normalizeByAddingZeroes(VALUE_FIELD.MINUTE, (this.underlyingTime).getMinuteOfHour()) );
				}
			}
		} else if (field == Granulaarsus.TIME){
			if (!(this.openedFields).containsKey(VALUE_FIELD.HOUR_OR_POD)){
				// If hour-part is still unset, fill it with the part of day ...
				(this.openedFields).put(VALUE_FIELD.HOUR_OR_POD, setLabel);
				(this.openedFields).remove(VALUE_FIELD.MINUTE);
			} else if (((this.openedFields).get(VALUE_FIELD.HOUR_OR_POD)).matches("[A-Z]+")){
				// If hour-part is set, allow only overwriting the part-of-day
				(this.openedFields).put(VALUE_FIELD.HOUR_OR_POD, setLabel);
				(this.openedFields).remove(VALUE_FIELD.MINUTE);
			}
		}
		// Update hours according to part of time
		if ((this.openedFields).containsKey(VALUE_FIELD.HOUR_OR_POD) && isModified(Granulaarsus.HOUR_OF_HALF_DAY) 
				&& this.partOfDay != null && !isModified(Granulaarsus.AM_PM)){
			int hourOfHalfDay = (this.underlyingTime).get(DateTimeFieldType.hourOfHalfday());
			//
			// Hour_of_Day:
			//    01 02 03 04 05 06 07 08 09 10 11 12 13 14 15 16 17 18 19 20 21 22 23 24
			// Hour_of_HalfDay:
			//    01 02 03 04 05 06 07 08 09 10 11 00 01 02 03 04 05 06 07 08 09 10 11 00
			//                 AM                 |                 PM                |
			//
			if ((this.partOfDay).equals("NI")){
				if (hourOfHalfDay >= 8 && hourOfHalfDay < 12){
					this.setField(Granulaarsus.AM_PM, DateTimeConstants.PM);
				} else {
					this.setField(Granulaarsus.AM_PM, DateTimeConstants.AM);
				}
				return;					
			}
			if ((this.partOfDay).equals("MO")){
				if (hourOfHalfDay == 0){
					this.setField(Granulaarsus.AM_PM, DateTimeConstants.PM);
				} else {
					this.setField(Granulaarsus.AM_PM, DateTimeConstants.AM);
				}
			}
			if ((this.partOfDay).equals("AF")){
				this.setField(Granulaarsus.AM_PM, DateTimeConstants.PM);
			}
			if ((this.partOfDay).equals("DT")){
				if (hourOfHalfDay == 0){
					this.setField(Granulaarsus.AM_PM, DateTimeConstants.PM);
				} else if (9 <= hourOfHalfDay && hourOfHalfDay < 12) {
					this.setField(Granulaarsus.AM_PM, DateTimeConstants.AM);
				} else if (hourOfHalfDay <= 7){
					this.setField(Granulaarsus.AM_PM, DateTimeConstants.PM);
				}
			}
			if ((this.partOfDay).equals("EV")){
				if (hourOfHalfDay == 0){
					this.setField(Granulaarsus.AM_PM, DateTimeConstants.AM);						
				} else {
					this.setField(Granulaarsus.AM_PM, DateTimeConstants.PM);
				}
			}
		}
		// Manage masks on fields: open or close fields, if necessary
		manageMaskedFields(field, setLabel, skipOverwritingChangedOne, setOrAddOperation);
	}
	
	
	/**
	 *   Updates representation, closes all granularities below minGran.
	 *   If minGran == YEAR and digits is defined, number of digits are
	 *   removed from the right side of the year number.
	 *   <br><br> 
	 *   Special case is minGran == YEAR and digits == 4, then year is 
	 *   put under the mask XXXX and lower granularities are preserved.
	 */
	public void closeGranularitiesBelow(Granulaarsus minGran, int digits){
		if (minGran.compareByCoarseRank(Granulaarsus.TIME) == 1){
			this.partOfDay = null;
		}
		boolean skipRemovingLowerGranularities = false;
		if ( digits > 0 && minGran == Granulaarsus.YEAR && ((this.openedFields).containsKey(VALUE_FIELD.YEAR)) ){
			if (((this.openedFields).get(VALUE_FIELD.YEAR)).matches("(BC)?\\d{4}")){
				if (digits > 3){
					(this.maskedFields).put(VALUE_FIELD.YEAR, "XXXX" );
					skipRemovingLowerGranularities = true;
				} else {
					String year = ((this.openedFields).get(VALUE_FIELD.YEAR));
					(this.openedFields).put(VALUE_FIELD.YEAR, year.substring(0, year.length() - digits) );
				}
			}
		}
		if (!skipRemovingLowerGranularities){
			for (Granulaarsus granulaarsus : Granulaarsus.fieldsInSafeOrder) {
				if (minGran.compareByCoarseRank(granulaarsus) == 1){
					for (int i = 0; i < valueFieldsInOrder.length; i++) {
						VALUE_FIELD field = valueFieldsInOrder[i];
						if ( field.matchesGranularity(granulaarsus) ){
							(this.openedFields).remove(field);
						}
					}
					
				}
			}			
		}
	}
	
	/**
	 *    Opens/closes masked fields, cosidering the nature of current operation. 
	 *   Current operation is defined by input parameters changedGranularity, label and 
	 *   setOrAddOperation. The goal is to maintain consistency (masked fields vs opened
	 *   fields) in final representation. For example, if current representation is 
	 *   2009-XX-XXTXX:XX, then setting monthOfYear to 03 should open further fields 
	 *   (produce: 2009-03), while modifying a dayOfMonth should be undefined operation 
	 *   and should produce XXXX-XX-XX.  
	 *   <br><br> 
	 *   If setOrAddOperations == true, then current operation is of type SET, otherwise
	 *   it is of type ADD;
	 */
	private void manageMaskedFields(Granulaarsus changedGranularity, String label, 
					boolean skipOverwritingChangedOne, boolean setOrAddOperation){
		VALUE_FIELD removeMaskFromFieldsEqualOrLowerThan = null;
		VALUE_FIELD addMaskToFieldsEqualOrGreaterThan    = null;
		if (setOrAddOperation){
			//
			// ----------------- operation of type SET -------------------------
			//
			//                   * * *   YEAR   * * *                           
			if (changedGranularity == Granulaarsus.YEAR || 
					changedGranularity == Granulaarsus.YEAR_OF_CENTURY || 
						changedGranularity == Granulaarsus.CENTURY_OF_ERA){
				// moving downwards, opening
				(this.maskedFields).remove(VALUE_FIELD.YEAR);
				removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.YEAR;
			}
			//                   * * *  MONTH   * * *
			if (changedGranularity == Granulaarsus.MONTH){
				// moving downwards, opening
				if (!(this.maskedFields).containsKey(VALUE_FIELD.YEAR) || label != null){
					(this.maskedFields).remove(VALUE_FIELD.MONTH_OR_WEEK);
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.DAY;					
				} else if (!isModified(Granulaarsus.WEEK_OF_YEAR) && 
							 !isModified(Granulaarsus.DAY_OF_WEEK) && 
						 		!isModified(Granulaarsus.DAY_OF_MONTH)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.DAY;
				}
				// moving upwards, closing
				if ((this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.YEAR;
				}
			}
			//                   * * *  WEEK   * * *
			if (changedGranularity == Granulaarsus.WEEK_OF_YEAR){
				// moving downwards, opening
				if (!(this.maskedFields).containsKey(VALUE_FIELD.YEAR)){
					(this.maskedFields).remove(VALUE_FIELD.MONTH_OR_WEEK);
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.DAY;					
				} else if (!isModified(Granulaarsus.DAY_OF_WEEK) && 
					 		   !isModified(Granulaarsus.DAY_OF_MONTH)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.DAY;
				}
				// moving upwards, closing
				if ((this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) || 
						(this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.MONTH_OR_WEEK;
				}
			}
			//                   * * *  WEEK-DAY   * * *
			if (changedGranularity == Granulaarsus.DAY_OF_WEEK){
				// moving downwards, opening
				if (!(this.maskedFields).containsKey(VALUE_FIELD.DAY) || label != null){
					(this.maskedFields).remove(VALUE_FIELD.DAY);
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.HOUR_OR_POD;					
				} else if (!isModified(Granulaarsus.HOUR_OF_HALF_DAY) && 
				 		   		!isModified(Granulaarsus.TIME)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.HOUR_OR_POD;
				}
				// moving upwards, closing
				if ((this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) || 
						(this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					if (label != null){
						addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.MONTH_OR_WEEK;						
					} else {
						addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.DAY;
					}
				}
			}
			//                   * * *  MONTH-DAY   * * *
			if (changedGranularity == Granulaarsus.DAY_OF_MONTH){
				// moving downwards, opening
				if (!(this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK)){ //  && isModified(Granulaarsus.MONTH)
						(this.maskedFields).remove(VALUE_FIELD.DAY);
						removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.HOUR_OR_POD;
				} else if (!isModified(Granulaarsus.HOUR_OF_HALF_DAY) && !isModified(Granulaarsus.TIME)){
						removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.HOUR_OR_POD;
				}
				// moving upwards, closing
				if ((this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) || (this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.DAY;
				}
			}
			//                   * * *    HOUR    * * *
			if (changedGranularity == Granulaarsus.AM_PM && label == null && !skipOverwritingChangedOne){
				// moving downwards, opening
				(this.maskedFields).remove(VALUE_FIELD.HOUR_OR_POD);
				removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.MINUTE;
			}
			if (changedGranularity == Granulaarsus.TIME && label != null){
				// moving downwards, opening
				(this.maskedFields).remove(VALUE_FIELD.HOUR_OR_POD);
				removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.MINUTE;
				// moving upwards, closing
				if (dateModified && (this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.DAY;
				}
			}
			if ((changedGranularity == Granulaarsus.HOUR_OF_HALF_DAY ||
					changedGranularity == Granulaarsus.HOUR_OF_DAY) && label == null){
				// moving downwards, opening
				// if AM_PM is unset, cover hour-part with X's in maskedFields;
				// if AM_PM is set, remove X's from hour-part in maskedField and bring out numerical hour-part ...
				if (isModified(Granulaarsus.AM_PM)){
					(this.maskedFields).remove(VALUE_FIELD.HOUR_OR_POD);					
				} else {
					(this.maskedFields).put(VALUE_FIELD.HOUR_OR_POD, "XX");					
				}
				removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.MINUTE;
				// moving upwards, closing
				if (dateModified && (this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.DAY;
				}
			}
			//                   * * *    MINUTE    * * *
			if (changedGranularity == Granulaarsus.MINUTE){
				// moving downwards, opening
				(this.maskedFields).remove(VALUE_FIELD.MINUTE);
				// moving upwards, closing
				// if HOUR_OR_POD is unset or contains part-of-day, cover it with a mask
				if (!(this.openedFields).containsKey(VALUE_FIELD.HOUR_OR_POD) || 
						((this.openedFields).containsKey(VALUE_FIELD.HOUR_OR_POD) && 
						((this.openedFields).get(VALUE_FIELD.HOUR_OR_POD)).matches("[A-Z]+"))){
					// If part of day label is set or hour-part is missing, cover the field with mask
					(this.openedFields).remove(VALUE_FIELD.HOUR_OR_POD);
					(this.maskedFields).put(VALUE_FIELD.HOUR_OR_POD, "XX");
				}
				if (dateModified && (this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.DAY;
				}
			}			
		} else {
			//
			// ----------------- operation of type ADD -------------------------
			//
			//                   * * *   YEAR   * * *                           
			if ((changedGranularity == Granulaarsus.YEAR || 
					changedGranularity == Granulaarsus.YEAR_OF_CENTURY) || 
						changedGranularity == Granulaarsus.CENTURY_OF_ERA){
				
				if (!(this.maskedFields).containsKey(VALUE_FIELD.YEAR)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.YEAR;					
				} else if (!isModified(Granulaarsus.MONTH) && !isModified(Granulaarsus.WEEK_OF_YEAR)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.MONTH_OR_WEEK;
				}
				if ((this.maskedFields).containsKey(VALUE_FIELD.YEAR)){
					if (changedGranularity == Granulaarsus.YEAR){
						(this.maskedFields).put(VALUE_FIELD.YEAR, "XXXX");
					}
					if (changedGranularity == Granulaarsus.YEAR_OF_CENTURY){
						(this.maskedFields).put(VALUE_FIELD.YEAR, "XXXX");
					}
					if (changedGranularity == Granulaarsus.CENTURY_OF_ERA){
						(this.maskedFields).put(VALUE_FIELD.YEAR, "XX");
					}
				}
			} 
			//                   * * *  MONTH   * * *
			if ((changedGranularity == Granulaarsus.MONTH)){
				// moving downwards, opening
				if (!(this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) || label != null){
					(this.maskedFields).remove(VALUE_FIELD.MONTH_OR_WEEK);
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.DAY;					
				} else if (!isModified(Granulaarsus.WEEK_OF_YEAR) && 
						 		!isModified(Granulaarsus.DAY_OF_WEEK) && 
						 			!isModified(Granulaarsus.DAY_OF_MONTH)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.DAY;					
				}
				// moving upwards, closing
				if ((this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.YEAR;
				}
			}
			//                   * * *  WEEK   * * *
			if (changedGranularity == Granulaarsus.WEEK_OF_YEAR){
				// moving downwards, opening
				if (!(this.maskedFields).containsKey(VALUE_FIELD.DAY) || label != null){
					(this.maskedFields).remove(VALUE_FIELD.MONTH_OR_WEEK);
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.DAY;					
				} else if (!isModified(Granulaarsus.DAY_OF_WEEK) && 
				 		   !isModified(Granulaarsus.DAY_OF_MONTH)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.DAY;
				}
				// moving upwards, closing
				if ((this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) || 
						(this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.MONTH_OR_WEEK;
				}
			}
			//                   * * *  WEEK-DAY   * * *
			if (changedGranularity == Granulaarsus.DAY_OF_WEEK){
				// moving downwards, opening
				if (!(this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.HOUR_OR_POD;					
				} else if (!isModified(Granulaarsus.HOUR_OF_HALF_DAY) && 
				 		   		!isModified(Granulaarsus.TIME)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.HOUR_OR_POD;
				}
				// moving upwards, closing
				if ((this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) || 
						(this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					if (label != null){
						addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.MONTH_OR_WEEK;						
					} else {
						addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.DAY;
					}
				}
			}
			//                   * * *  MONTH-DAY   * * *
			if (changedGranularity == Granulaarsus.DAY_OF_MONTH){
				// moving downwards, opening
				if (!(this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.HOUR_OR_POD;					
				} else if (!isModified(Granulaarsus.HOUR_OF_HALF_DAY) && 
				 		   		!isModified(Granulaarsus.TIME)){
					removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.HOUR_OR_POD;
				}
				// moving upwards, closing
				if ((this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) || (this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.DAY;
				}
			}
			//                   * * *    HOUR    * * *
			if ((changedGranularity == Granulaarsus.HOUR_OF_HALF_DAY ||
						changedGranularity == Granulaarsus.HOUR_OF_DAY) && label == null){
				// moving downwards, opening
				if (isModified(Granulaarsus.AM_PM)){
					(this.maskedFields).remove(VALUE_FIELD.HOUR_OR_POD);					
				} else {
					(this.maskedFields).put(VALUE_FIELD.HOUR_OR_POD, "XX");					
				}
				removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.MINUTE;
				// moving upwards, closing
				if (dateModified && (this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.DAY;
				}
			}
			if (changedGranularity == Granulaarsus.TIME && label != null){
				// moving downwards, opening
				(this.maskedFields).remove(VALUE_FIELD.HOUR_OR_POD);
				removeMaskFromFieldsEqualOrLowerThan = VALUE_FIELD.MINUTE;
				// moving upwards, closing
				if (dateModified && (this.maskedFields).containsKey(VALUE_FIELD.DAY)){
					addMaskToFieldsEqualOrGreaterThan = VALUE_FIELD.DAY;
				}
			}
			if (changedGranularity == Granulaarsus.MINUTE){
				// moving upwards, closing
				// if HOUR_OR_POD is unset or contains part-of-day, cover it with a mask
				if (!(this.openedFields).containsKey(VALUE_FIELD.HOUR_OR_POD) || 
						((this.openedFields).containsKey(VALUE_FIELD.HOUR_OR_POD) && 
						((this.openedFields).get(VALUE_FIELD.HOUR_OR_POD)).matches("[A-Z]+"))){
					// If part of day label is set or hour-part is missing, cover the field with mask
					(this.openedFields).remove(VALUE_FIELD.HOUR_OR_POD);
					(this.maskedFields).put(VALUE_FIELD.HOUR_OR_POD, "XX");
				}
			}
		}
		// ----------------- remove mask from lower fields ---------------------
		if (removeMaskFromFieldsEqualOrLowerThan != null){
			int j = 0;
			boolean fieldFound = false;
			while (j < valueFieldsInOrder.length){
				VALUE_FIELD lowerOrSameField = valueFieldsInOrder[j];
				if (lowerOrSameField == removeMaskFromFieldsEqualOrLowerThan){
					fieldFound = true;
				}
				if (fieldFound){
					(this.maskedFields).remove(lowerOrSameField);
				}
				j++;
			}			
		}
		// ------------------ add mask to greater fields ---------------------
		if (addMaskToFieldsEqualOrGreaterThan != null){
			int j = valueFieldsInOrder.length - 1;
			boolean fieldFound = false;
			while (j > -1){
				VALUE_FIELD greaterOrSameField = valueFieldsInOrder[j];
				if (greaterOrSameField == addMaskToFieldsEqualOrGreaterThan){
					fieldFound = true;
				}
				if (fieldFound){
					if (greaterOrSameField == VALUE_FIELD.YEAR){
						(this.maskedFields).put(greaterOrSameField, "XXXX");
					}
					if (greaterOrSameField == VALUE_FIELD.MONTH_OR_WEEK){
						if (this.hasWeekBasedFormatInOutput()){
							(this.maskedFields).put(greaterOrSameField, "WXX");
						} else {
							(this.maskedFields).put(greaterOrSameField, "XX");
						}
					}
					if (greaterOrSameField == VALUE_FIELD.DAY){
						(this.maskedFields).put(greaterOrSameField, "XX");
					}
					if (greaterOrSameField == VALUE_FIELD.HOUR_OR_POD){
						(this.maskedFields).put(greaterOrSameField, "XX");
					}
					if (greaterOrSameField == VALUE_FIELD.MINUTE){
						(this.maskedFields).put(greaterOrSameField, "XX");
					}
				}
				j--;
			}			
		}		
	}
	
	//==============================================================================
	//   	M a s k i n g
	//==============================================================================
	
	/**
	 *   Takes masks as inputs; adds masks to corresponding fields (in this.maskedFields).
	 */
	public void setMaskedFields(String year, String month, String day, String hour, String minute) {
		Pattern maskPattern = Pattern.compile("X+");
		if (year != null && (maskPattern.matcher(year)).matches()){
			(this.maskedFields).put(VALUE_FIELD.YEAR, year);
		}
		if (month != null && (maskPattern.matcher(month)).matches()){
			(this.maskedFields).put(VALUE_FIELD.MONTH_OR_WEEK, month);
		}
		if (day != null && (maskPattern.matcher(day)).matches()){
			(this.maskedFields).put(VALUE_FIELD.DAY, day);
		}
		if (hour != null && (maskPattern.matcher(hour)).matches()){
			(this.maskedFields).put(VALUE_FIELD.HOUR_OR_POD, hour);
		}
		if (minute != null && (maskPattern.matcher(minute)).matches()){
			(this.maskedFields).put(VALUE_FIELD.MINUTE, minute);
		}
	}
		
	//==============================================================================
	//   	C l o n i n g
	//==============================================================================
	
	public TimeMLDateTimePoint clone(){
		TimeMLDateTimePoint newDtp = new TimeMLDateTimePoint(this);
		return newDtp;
	}
	
	//==============================================================================
	//   	G e t t e r s    a n d    s t r i n g     g e n e r a t o r s
	//==============================================================================
	
	/**
	 *   A shortcut method, finds whether given granularity <code>g</code> has been modified
	 *   (appears in <code>this.modifiedGrans</code>).
	 */
	public boolean isModified(Granulaarsus g){
		return (this.modifiedGrans).containsKey(g);
	}
	
	/**
	 *   Whether calendar operations other than setField have been used; 
	 */
	public boolean hasFunctionOtherThanSetUsed(){
		return this.functionOtherThanSetUsed;
	}
	
	/**
	 *   Whether at least one field in openedFields has been set or modified.
	 */
	public boolean hasModifiedFields(){
		return !((this.openedFields).isEmpty());
	}

	/**
	 *   Finds, whether current datetime point has opened or masked fields with date 
	 *   granularities.
	 */
	public boolean hadModifiedDateFields(){
		return this.dateModified;
	}
	
	/**
	 *   Finds, whether current datetime point has opened or masked fields with time 
	 *   granularities. 
	 */
	public boolean hasModifiedTimeFields(){
		return (openedFields.containsKey(VALUE_FIELD.HOUR_OR_POD) || 
				maskedFields.containsKey(VALUE_FIELD.HOUR_OR_POD) );
	}
	
	/**
	 *    Finds, whether this date-time representation will contain any labels.
	 */
	public boolean hasLabelsInOutput(){
		return 
		((this.openedFields).containsKey(VALUE_FIELD.HOUR_OR_POD) && ((this.openedFields).get(VALUE_FIELD.HOUR_OR_POD)).matches("[A-Z]+"))
				||
		((this.openedFields).containsKey(VALUE_FIELD.DAY) && ((this.openedFields).get(VALUE_FIELD.DAY)).matches("[A-Z]+")) 
				||
		((this.openedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) && ((this.openedFields).get(VALUE_FIELD.MONTH_OR_WEEK)).matches("[A-Z]+[0-9]*"));
						
	}
	
	/**
	 *    Finds, whether this date-time representation has week-based format;
	 */
	public boolean hasWeekBasedFormatInOutput(){
		return 
		  ( ((this.openedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) && ((this.openedFields).get(VALUE_FIELD.MONTH_OR_WEEK)).matches("W\\d+")) ||
		    ((this.maskedFields).containsKey(VALUE_FIELD.MONTH_OR_WEEK) && ((this.maskedFields).get(VALUE_FIELD.MONTH_OR_WEEK)).matches("WX+")) 
		  );
	}
	
	/**
	 *  Generates TimeML-compatible "value" attribute, based on current representation.
	 */
	public String toValueString() throws Exception {
		HashMap<VALUE_FIELD, String> displayableFields = this.openedFields;
		if (openedFields.size() == 0){
			displayableFields = this.inputFields;
		}
		if (displayableFields.size() == 0 && maskedFields.size() == 0){
			throw new Exception("No calendaric fields have been set for "+((this.getClass()).getCanonicalName())+" object.");
		}
		StringBuilder sb = new StringBuilder();
		// 1) Try to construct full extent date-time string
		for (int i = 0; i < valueFieldsInOrder.length; i++) {
			VALUE_FIELD field = valueFieldsInOrder[i];
			if (maskedFields.containsKey(field)){
				if (i > 0 && i < 3){ sb.append("-"); }
				if (i == 3)        { sb.append("T"); }
				if (i == 4)        { sb.append(":"); }
				sb.append( maskedFields.get(field) );
			} else if (displayableFields.containsKey(field)){
				if (i > 0 && i < 3){ sb.append("-"); }
				if (i == 3)        { sb.append("T"); }
				if (i == 4)        { sb.append(":"); }
				sb.append( displayableFields.get(field) );
			} else {
				break;
			}
		}
		// 2) Try to construct only time string
		if (sb.length() == 0){
			for (int i = 3; i < valueFieldsInOrder.length; i++) {
				VALUE_FIELD field = valueFieldsInOrder[i];
				if (maskedFields.containsKey(field)){
					if (i == 3)        { sb.append("T"); }
					if (i == 4)        { sb.append(":"); }
					sb.append( maskedFields.get(field) );
				} else if (displayableFields.containsKey(field)){
					if (i == 3)        { sb.append("T"); }
					if (i == 4)        { sb.append(":"); }
					sb.append( displayableFields.get(field) );
				} else {
					break;
				}
			}
		}
		return sb.toString();
	}

	/**
	 *  Adds zeroes to specified field value, if necessary.
	 */
	public static String normalizeByAddingZeroes(VALUE_FIELD field, int value){
		return normalizeByAddingZeroes(field, String.valueOf(value));
	}
	
	/**
	 *  Adds zeroes to specified field value, if necessary.
	 */
	public static String normalizeByAddingZeroes(VALUE_FIELD field, String value){
		if (value.matches("\\d+")){
			if (field == VALUE_FIELD.YEAR){
				while (value.length() < 4){
					value = "0"+value;
				}
			}
			if (value.length()==1){
				if (field == VALUE_FIELD.MONTH_OR_WEEK){
					return "0"+value;
				}
				if (field == VALUE_FIELD.DAY){
					return "0"+value;
				}
				if (field == VALUE_FIELD.HOUR_OR_POD){
					return "0"+value;
				}
				if (field == VALUE_FIELD.MINUTE){
					return "0"+value;
				}					
			}
		}
		return value;
	}
	
	public static String getFieldShapeInXXXes(VALUE_FIELD field){
		if (field == VALUE_FIELD.YEAR){
			return "XXXX";
		} else {
			return "XX";
		}
	}

	/**
	 *   Finds lowest modified granularity in this point. Also performs some 
	 *   unification on the result (e.g if dayOfWeek is lowest, returned granularity
	 *   will be dayOfMonth).
	 */
	public Granulaarsus getLowestChangedGranularity(){
		Granulaarsus lowest = null;
		for (Granulaarsus gran : Granulaarsus.fieldsInSafeOrder) {
			if (isModified(gran)){
				lowest = gran;
			}
		}
		if (lowest == Granulaarsus.CENTURY_OF_ERA){
			lowest = Granulaarsus.YEAR;
		}
		if (lowest == Granulaarsus.DAY_OF_WEEK){
			lowest = Granulaarsus.DAY_OF_MONTH;
		}
		if (lowest == Granulaarsus.WEEK_OF_YEAR){
			lowest = Granulaarsus.DAY_OF_MONTH;
		}
		if (lowest == Granulaarsus.HOUR_OF_HALF_DAY){
			lowest = Granulaarsus.HOUR_OF_DAY;
		}
		if (lowest == Granulaarsus.AM_PM){
			lowest = Granulaarsus.HOUR_OF_DAY;
		}
		if (lowest == null && 
				isModified(Granulaarsus.TIME)){
			lowest = Granulaarsus.HOUR_OF_DAY;
		}
		if (lowest == null && 
				isModified(Granulaarsus.YEAR_OF_CENTURY)){
			lowest = Granulaarsus.YEAR;
		}
		return lowest;
	}
	
	//==============================================================================
	//   	H e l p   f u n c t i o n s
	//==============================================================================
	
	/**
	 *   If current object has all 5 VALUE_FIELD-s set in inputFields, copies content
	 *   of inputFields to openedFields. 
	 *   <br><br>
	 *   NB! This is rather a hack way for cases, when nothing will be modified in 
	 *   datetime point. If there will be modifications of the object in the future,
	 *   this way should not be used. 
	 */
	public void copyInputFieldsToOpenedFields(){
		if (inputFields.size() == 5){
			for (VALUE_FIELD vf : inputFields.keySet()) {
				openedFields.put(vf, inputFields.get(vf));
			}
		}
	}
	
	public LocalDateTime getAsLocalDateTime(){
		return new LocalDateTime( 
			(this.underlyingDate).getYear(),
			(this.underlyingDate).getMonthOfYear(),
			(this.underlyingDate).getDayOfMonth(),
			(this.underlyingTime).getHourOfDay(),
			(this.underlyingTime).getMinuteOfHour()
		); 
	}
	
	public String [] getUnderlyingDateTimeAsGranularitiesArray(){
		return new String [] { normalizeByAddingZeroes(VALUE_FIELD.YEAR,          (this.underlyingDate).getYear()), 
							   normalizeByAddingZeroes(VALUE_FIELD.MONTH_OR_WEEK, (this.underlyingDate).getMonthOfYear()),
							   normalizeByAddingZeroes(VALUE_FIELD.DAY,           (this.underlyingDate).getDayOfMonth()),
							   normalizeByAddingZeroes(VALUE_FIELD.HOUR_OR_POD,   (this.underlyingTime).getHourOfDay()),
							   normalizeByAddingZeroes(VALUE_FIELD.MINUTE,        (this.underlyingTime).getMinuteOfHour())
							 };
	}
	
	private static int parseInteger(String str){
		int i = -1;
		try {
			i = Integer.parseInt(str);
		} catch (NumberFormatException e) {
		}
		return i;
	}
}
