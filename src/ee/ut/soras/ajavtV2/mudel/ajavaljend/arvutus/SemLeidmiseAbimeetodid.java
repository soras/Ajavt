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

import java.util.List;

import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeFieldType;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.base.BaseLocal;

import ee.ut.soras.ajavtV2.mudel.ajavaljend.Granulaarsus;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaObjekt.TYYP;

/**
 *   Abimeetodid semantika leidmisel ja normaliseerimisel. Mitmesugused heuristikud.
 * 
 *   @author Siim Orasmaa
 */
public class SemLeidmiseAbimeetodid {

	/**
	 *   
	 *   HEURISTIK #1:
	 *   ---------------
	 *   Lahima ajatsykli aken (Baldwini aken).
	 *   
	 *   Lahima oopaeva (23 h) aken.
	 *      10 11 12 13 14 15 16 17 18 19 20   21   22 23 24 01 02 03 04 05 06 07 08 09 10
	 *      ?                                 (--)                                      ?      
	 *                     03 04 05 06 07 08   09   10 11 00 01 02 03  
	 *                     ?                  (--)                 ?
	 *      
	 *   Lahima 3-paevaosa aken.
	 *      06-12   12-18   18-00   00-06   06-12
	 *       MO      AF      EV      NI      MO
	 *       ?              (--)             ?
	 *   
	 *   Lahima 7-paeva aken.
	 *     N   R   L   P   E   T   K   N   R
	 *     ?             (---)             ?
	 *   
	 *   Lahima 11-kuu aken.
	 *      04  05  06  07  08  09  10  11  12  01  02  03  04
	 *      ?                      (--)                     ?
	 * 
	 *   Lahima 3 aastaaja aken.
	 *      03-05   06-08   09-11   12-02   03-05
	 *       SP      SU      FA      WI      SP
	 *       ?              (--)             ?
	 *       
	 *   Lahima 3 kvartali aken.
	 *      04-06   07-09   10-12   01-03   04-06
	 *       Q2      Q3       Q4      Q1     Q2  
	 *       ?               (--)            ?
	 *       
	 **/
	

	//==============================================================================
	//    Baldwini akna heuristik
	//==============================================================================

	/**
	 *   Rakendab yldistatud Baldwini akent, et leida hetkele <tt>currentDateTime</tt> l2himat 
	 *  ajahetke, mis vastab tingimustele <tt>field == soughtValue</tt>. Kui tingimustele vastav
	 *  hetk j22b v2lja Baldwini akna raamidest, toimib kui tavaline SET operatsioon, omistades
	 *  <tt>field := soughtValue</tt> ajahetke <tt>currentDateTime</tt> raames.
	 *  <p>
	 *  Praegu on implementeeritud ainult granulaarsuste 
	 *  <tt>DAY_OF_WEEK</tt>, <tt>MONTH</tt>, <tt>YEAR_OF_CENTURY</tt>  
	 *  toetus. 
   	 *  <p>
	 *  <i>What's the Date? High Accuracy Interpretation of Weekday Name,</i> Dale, Mazur (2009)
	 */
	public static LocalDateTime applyBaldwinWindow(Granulaarsus field, LocalDateTime currentDateTime, int soughtValue){
		// ---------------------------------
		//  DAY_OF_WEEK
		// ---------------------------------		
		if (field == Granulaarsus.DAY_OF_WEEK && 
				DateTimeConstants.MONDAY <= soughtValue && soughtValue <= DateTimeConstants.SUNDAY){
			int currentDayOfWeek = currentDateTime.getDayOfWeek();
			int addToCurrent = 0;
			// 1) Vaatame eelnevat 3-e p&auml;eva
			while (addToCurrent > -4){
				if (currentDayOfWeek == soughtValue){
					return currentDateTime.plusDays(addToCurrent);
				}
				currentDayOfWeek--;
				if (currentDayOfWeek < DateTimeConstants.MONDAY){
					currentDayOfWeek = DateTimeConstants.SUNDAY;
				}
				addToCurrent--;
			}
			// 2) Vaatame jargnevat 3-e p&auml;eva
			currentDayOfWeek = currentDateTime.getDayOfWeek();
			addToCurrent = 0;
			while (addToCurrent < 4){
				if (currentDayOfWeek == soughtValue){
					return currentDateTime.plusDays(addToCurrent);
				}
				currentDayOfWeek++;
				if (currentDayOfWeek > DateTimeConstants.SUNDAY){
					currentDayOfWeek = DateTimeConstants.MONDAY;
				}
				addToCurrent++;
			}
		}
		// ---------------------------------
		//  MONTH
		// ---------------------------------
		if (field == Granulaarsus.MONTH && 
					DateTimeConstants.JANUARY <= soughtValue && soughtValue <= DateTimeConstants.DECEMBER){
			int currentMonth = currentDateTime.getMonthOfYear();
			int addToCurrent = 0;
			// 1) Vaatame eelnevat 5-e kuud
			while (addToCurrent > -6){
				if (currentMonth == soughtValue){
					return currentDateTime.plusMonths(addToCurrent);
				}
				currentMonth--;
				if (currentMonth < DateTimeConstants.JANUARY){
					currentMonth = DateTimeConstants.DECEMBER;
				}
				addToCurrent--;
			}
			// 2) Vaatame jargnevat 5-e kuud
			currentMonth = currentDateTime.getMonthOfYear();
			addToCurrent = 0;
			while (addToCurrent < 6){
				if (currentMonth == soughtValue){
					return currentDateTime.plusMonths(addToCurrent);
				}
				currentMonth++;
				if (currentMonth > DateTimeConstants.DECEMBER){
					currentMonth = DateTimeConstants.JANUARY;
				}
				addToCurrent++;
			}
			// Kui otsitav kuu j2i aknast v2lja, k2sitleme seda kui "selle aasta" otsitud kuud
			return currentDateTime.withMonthOfYear(soughtValue);
		}
		// ---------------------------------
		//  YEAR_OF_CENTURY
		// ---------------------------------
		if (field == Granulaarsus.YEAR_OF_CENTURY && 0 <= soughtValue && soughtValue <= 99){
			// API tunnistab väärtuseid vahemikust 1 kuni 100
			if (soughtValue == 0){
				soughtValue = 100;
			}
			int currentYear = currentDateTime.getYearOfCentury();
			int addToCurrent = 0;
			// 1) Vaatame eelnevat 4-a aastakymmet 
			while (addToCurrent > -49){
				if (currentYear == soughtValue){
					return currentDateTime.plusYears(addToCurrent);
				}
				currentYear--;
				if (currentYear < 1){
					currentYear = 100;
				}
				addToCurrent--;
			}			
			// 2) Vaatame jargnevat 4-a aastakymmet
			currentYear = currentDateTime.getYearOfCentury();
			addToCurrent = 0;
			while (addToCurrent < 49){
				if (currentYear == soughtValue){
					return currentDateTime.plusYears(addToCurrent);
				}
				currentYear++;
				if (currentYear > 100){
					currentYear = 1;
				}
				addToCurrent++;
			}
			// Kui otsitav kuu j2i aknast v2lja, k2sitleme seda kui "selle sajandi" otsitud aastat
			return currentDateTime.withYearOfCentury(soughtValue);			
		}
		return currentDateTime;
	}
	
	/**
	 *   Rakendab yldistatud Baldwini akent, et leida hetkele <tt>currentDateTime</tt> l2himat 
	 *  ajahetke, mis vastab tingimustele <tt>field == soughtValue</tt>. Kui tingimustele vastav
	 *  hetk j22b v2lja Baldwini akna raamidest, toimib kui tavaline SET operatsioon, omistades
	 *  <tt>field := soughtValue</tt> ajahetke <tt>currentDateTime</tt> raames.
	 *  <p>
	 *  NB! Praegu on implementeeritud ainult aastaaegade ja kvartalite lahendamine. 
   	 *  <p>
	 *  <i>What's the Date? High Accuracy Interpretation of Weekday Name,</i> Dale, Mazur (2009)
	 */
	public static LocalDateTime applyBaldwinWindow(Granulaarsus field, LocalDateTime currentDateTime, String soughtValue){
		// ---------------------------------
		//  SEASONs
		// ---------------------------------		
		if (field == Granulaarsus.MONTH && soughtValue != null && soughtValue.matches("(SP|SU|FA|WI)")){
			LocalDate movingFocus = new LocalDate(currentDateTime);
			int addToCurrent = 0;
			// 1) Vaatame eelnevat 4-a kuud
			while (addToCurrent > -4){
				String currentSeason = getSeason(movingFocus);
				if (currentSeason.equals(soughtValue)){
					return currentDateTime.plusMonths(addToCurrent);
				}
				movingFocus = movingFocus.plusMonths(-1);
				addToCurrent--;
			}
			// 2) Vaatame jargnevat 4-a kuud
			movingFocus = new LocalDate(currentDateTime);
			addToCurrent = 0;
			while (addToCurrent < 4){
				String currentSeason = getSeason(movingFocus);
				if (currentSeason.equals(soughtValue)){
					return currentDateTime.plusMonths(addToCurrent);
				}
				movingFocus = movingFocus.plusMonths(1);
				addToCurrent++;
			}
			// Kui otsitav aastaaeg j2i aknast v2lja, k2sitleme seda kui "selle aasta" otsitud aastaaega
			return setMiddleOfSeason(currentDateTime, soughtValue);
		}
		// ---------------------------------
		//  QUARTER
		// ---------------------------------
		if (field == Granulaarsus.MONTH && soughtValue != null && soughtValue.matches("Q(1|2|3|4)")){
			LocalDate movingFocus = new LocalDate(currentDateTime);
			int addToCurrent = 0;
			// 1) Vaatame eelnevat 4-a kuud
			while (addToCurrent > -4){
				String currentSeason = getQuarterOfYear(movingFocus);				
				if (currentSeason.equals(soughtValue)){
					return currentDateTime.plusMonths(addToCurrent);
				}
				movingFocus = movingFocus.plusMonths(-1);
				addToCurrent--;
			}
			// 2) Vaatame jargnevat 4-a kuud
			movingFocus = new LocalDate(currentDateTime);
			addToCurrent = 0;
			while (addToCurrent < 4){
				String currentSeason = getQuarterOfYear(movingFocus);				
				if (currentSeason.equals(soughtValue)){
					return currentDateTime.plusMonths(addToCurrent);
				}
				movingFocus = movingFocus.plusMonths(1);
				addToCurrent++;
			}
			// Kui otsitav kvartal j2i aknast v2lja, k2sitleme seda kui "selle aasta" otsitud kvartalit
			return setMiddleOfQuarterOfYear(currentDateTime, soughtValue);
		}
		return null;
	}

	//==============================================================================
	//    Ajapunkti n-inda alamosa leidmine
	//==============================================================================
	
	/**
	 *    Leiab <tt>currentDateTime</tt> granulaarsuse <tt>superField</tt> 
	 *   <i>n</i>-inda alamosa, mis vastab tingimustele <tt>subField == soughtValueOfSubField</tt>.
	 *   <p>
	 *   Negatiivsete <i>n</i> vaartuste korral voetakse alamosa "tagantpoolt": vaartus 
	 *   <i>n</i> == -1 tahistab <i>viimast</i>, <i>n</i> == -2 tahistab <i>eelviimast</i>
	 *   jne alamosa.
	 *   <p>
	 *   Praegu on defineeritud ainult <i>kuu n-inda nadalapaeva leidmise</i> operatsioon (
	 *   <tt>superField == MONTH</tt>, <tt>subField == DAY_OF_WEEK</tt>, <tt>soughtValueOfSubField == a weekdayname</tt> ).
	 */
	public static LocalDateTime findNthSubpartOfGranularity(Granulaarsus superField,
															Granulaarsus subField,
															int soughtValueOfSubField,
															int n,
															LocalDateTime currentDateTime
															){
		if (superField == Granulaarsus.MONTH){
			// --------------------------------------		
			//  Kuu n-inda nadalapaeva leidmine ...
			// --------------------------------------
			if (subField == Granulaarsus.DAY_OF_WEEK &&
			    DateTimeConstants.MONDAY <= soughtValueOfSubField && 
			    soughtValueOfSubField <= DateTimeConstants.SUNDAY){
					if (n > 0){
						//
						// Algoritm:  
						//    http://msdn.microsoft.com/en-us/library/aa227532(VS.60).aspx
						//
						// Kerime kaesoleva kuu esimese kuupaeva peale ...
						LocalDateTime newDate = currentDateTime.withDayOfMonth(1);
						// Leiame esimese otsitud nadalapaeva
						while (newDate.getDayOfWeek() != soughtValueOfSubField){
							newDate = newDate.plusDays(1);
						}
						int currentMonth = newDate.getMonthOfYear();
						newDate = newDate.plusDays( (n - 1) * 7 );
						if (currentMonth == newDate.getMonthOfYear()){
							// Kui kuu j2i kindlalt samaks, tagastame leitud nadalapaeva
							return newDate;
						}
					} else if (n < 0){
						// Negatiivsete vaartuste korral otsime lahendust lopust:
						// Kerime kuu viimase vaartuse peale
						LocalDateTime newDate = currentDateTime.withDayOfMonth( 
													currentDateTime.dayOfMonth().getMaximumValue() );
						// Leiame viimase otsitud nadalapaeva
						while (newDate.getDayOfWeek() != soughtValueOfSubField){
							newDate = newDate.minusDays(1);
						}
						int currentMonth = newDate.getMonthOfYear();
						newDate = newDate.minusDays( ((n*(-1)) - 1) * 7 );
						if (currentMonth == newDate.getMonthOfYear()){
							// Kui kuu j2i kindlalt samaks, tagastame leitud nadalapaeva
							return newDate;
						}				
					}
			}
			// -------------------------------------------------
			//   Kuu n-inda nädala/nädalavahetuse leidmine ...
			// -------------------------------------------------
			// -------------------------------------------------------------------
			//   Teeme eelduse, et kuu esimene nädal on nädal, mis sisaldab kuu
			//  esimest nadalapaeva {soughtValueOfSubField};
			//   Ning analoogselt, kuu viimane nädal on nädal, mis sisaldab kuu 
			//  viimast nadalapaeva {soughtValueOfSubField};
			// -------------------------------------------------------------------
			if (subField == Granulaarsus.WEEK_OF_YEAR &&
						DateTimeConstants.MONDAY <= soughtValueOfSubField && 
						soughtValueOfSubField <= DateTimeConstants.SUNDAY){
				if (n > 0){
					// Kerime kaesoleva kuu esimese paeva peale ...
					LocalDateTime newDate = currentDateTime.withDayOfMonth(1);
					// Leiame kuu esimese neljapaeva/laupaeva
					while (newDate.getDayOfWeek() != soughtValueOfSubField){
						newDate = newDate.plusDays(1);
					}
					newDate = newDate.plusDays( (n - 1) * 7 );
					return newDate;
				} else if (n < 0){
					// Negatiivsete vaartuste korral otsime lahendust lopust:
					// Kerime kuu viimase vaartuse peale
					LocalDateTime newDate = currentDateTime.withDayOfMonth( 
												currentDateTime.dayOfMonth().getMaximumValue() );
					// Leiame viimase neljapaeva/laupaeva
					while (newDate.getDayOfWeek() != soughtValueOfSubField){
						newDate = newDate.minusDays(1);
					}
					newDate = newDate.minusDays( ((n*(-1)) - 1) * 7 );
					return newDate;
				}
			}	
		}
		return null;
	}
	
	
	//==============================================================================
	//    Ajalipikute leidmine
	//==============================================================================
	
	/**
	 *   Tagastab kuupaevale vastava kvartali lipiku. 
	 */
	public static String getQuarterOfYear(LocalDate date){
		int month = date.getMonthOfYear();
		if (1 <= month && month <= 3){
			return "Q1";
		}
		if (4 <= month && month <= 6){
			return "Q2";
		}
		if (7 <= month && month <= 9){
			return "Q3";
		}
		if (10 <= month && month <= 12){
			return "Q4";
		}		
		return null;
	}

	/**
	 *   Viib ajapunkti <tt>dateTime</tt> kvartali <tt>quarter</tt> keskmisele kuule. 
	 */
	public static LocalDateTime setMiddleOfQuarterOfYear(LocalDateTime dateTime, String quarter){
		if (quarter.equals("Q1")){
			return dateTime.withMonthOfYear(DateTimeConstants.FEBRUARY);
		}
		if (quarter.equals("Q2")){
			return dateTime.withMonthOfYear(DateTimeConstants.MAY);
		}
		if (quarter.equals("Q3")){
			return dateTime.withMonthOfYear(DateTimeConstants.AUGUST);
		}
		if (quarter.equals("Q4")){
			return dateTime.withMonthOfYear(DateTimeConstants.NOVEMBER);
		}
		return dateTime;
	}
	
	/**
	 *   Tagastab kuupaevale vastava aastaaja lipiku. 
	 */
	public static String getSeason(LocalDate date){
		int month = date.getMonthOfYear();
		if (12 <= month || month <= 2){
			return "WI";
		}
		if (3 <= month && month <= 5){
			return "SP";
		}
		if (6 <= month && month <= 8){
			return "SU";
		}
		if (9 <= month && month <= 11){
			return "FA";
		}		
		return null;
	}
	
	/**
	 *   Viib ajapunkti <tt>dateTime</tt> aastaaja <tt>season</tt> keskmisele kuule. 
	 */
	public static LocalDateTime setMiddleOfSeason(LocalDateTime dateTime, String season){
		if (season.equals("WI")){
			return dateTime.withMonthOfYear(DateTimeConstants.JANUARY);
		}
		if (season.equals("SP")){
			return dateTime.withMonthOfYear(DateTimeConstants.APRIL);
		}
		if (season.equals("SU")){
			return dateTime.withMonthOfYear(DateTimeConstants.JULY);
		}
		if (season.equals("FA")){
			return dateTime.withMonthOfYear(DateTimeConstants.OCTOBER);
		}
		return dateTime;
	}
	
	/**
	 *    Tagastab kellaajale vastava <i>part-of-day</i> lipiku.
	 */
	public static String getPartOfDay(LocalTime time){
		int hourOfDay   = time.getHourOfDay();
		if (0 <= hourOfDay && hourOfDay <= 5){
			return "NI";
		}
		if (6 <= hourOfDay && hourOfDay <= 11){
			return "MO";
		}
		if (12 <= hourOfDay && hourOfDay <= 17){
			return "AF";
		}
		if (18 <= hourOfDay && hourOfDay <= 23){
			return "EV";
		}		
		return null;
	}
	
	/**
	 *    Tagastab kuup&auml;evas kajastuvale ajale vastavalt, kas on tegu toopaeva (WD) voi nadalavahetusega (WE). 
	 */
	public static String getWordDayOrWeekend(LocalDate date){
		int weekDay = date.getDayOfWeek();
		if (weekDay == DateTimeConstants.SATURDAY || weekDay == DateTimeConstants.SUNDAY){
			return "WE";
		} else {
			return "WD";
		}
	}

	/**
	 *  Tagastab antud kalendriv2ljaga seotud ekstreemum (min v6i max) v22rtuse;
	 */
	public static int getLocalDateTimeFieldExtremum(BaseLocal partial, DateTimeFieldType type, boolean getMax){
		DateTimeField field = type.getField(partial.getChronology());
		return (getMax) ? (field.getMaximumValue(partial)) : (field.getMinimumValue(partial));
	}
	
	//==============================================================================
	//==============================================================================
	//==============================================================================
	//   
	//                Eksplitsiitsete otspunktide terviklik muutmine     
	//
	//==============================================================================
	//==============================================================================
	//==============================================================================
	
	public static void closeGranularitiesBelow(List<AjaObjekt> seotudEksplitsiitsedTIMEXid, Granulaarsus minGran, int digits) {
		if (seotudEksplitsiitsedTIMEXid != null){
			for (AjaObjekt ajaObjekt : seotudEksplitsiitsedTIMEXid) {
				ajaObjekt.closeGranularitiesBelow(minGran, digits);
			}			
		}
	}
	
	//==============================================================================
	//   	K a h e    o t s p u n k t i    v a a r t u s t e
	//      y h i l d a m i n e
	//==============================================================================
	
	/**
	 *   Saab sisendiks ajaintervalli otspunktide massiivi, yritab teostada granulaarsuste
	 *   Jagamist intervalli osade vahel;
	 */
	public static void jagaOtspunktideVahelPuuduvaidVaartuseid(List<AjaObjekt> seotudEksplitsiitsedTIMEXid){
		if (seotudEksplitsiitsedTIMEXid != null && seotudEksplitsiitsedTIMEXid.size() == 2){
			AjaObjekt algusPunkt = seotudEksplitsiitsedTIMEXid.get(0);
			AjaObjekt loppPunkt  = seotudEksplitsiitsedTIMEXid.get(1);
			if (algusPunkt instanceof AjaPunkt && loppPunkt instanceof AjaPunkt){
				// 1) Kui yks otspunkt on korduvus, muudame m6lemad korduvusteks:
				if ((algusPunkt).getType()==TYYP.RECURRENCE){
					((AjaPunkt)loppPunkt).setType(TYYP.RECURRENCE);
				} else if ((loppPunkt).getType()==TYYP.RECURRENCE){
					((AjaPunkt)algusPunkt).setType(TYYP.RECURRENCE);
				}
				// 2) Muude v22rtuste jagamine, heuristiliselt: v6tame esimeselt
				// punktilt ja kanname yle teisele punktile ...
				// NB! Praegu j22b v2lja: see meetod oli liiga keerukas ning l6ppkokkuv6ttes v2hekasulik
				// ((AjaPunkt)loppPunkt).takeMissingUpperGranularitiesFromPoint((AjaPunkt)algusPunkt);
				
			}
		}
	}	
	
}
