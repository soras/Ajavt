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

package ee.ut.soras.ajavtV2.mudel.ajavaljend;

/**
 *  Ajavaljendi detailsus ehk granulaarsus.
 *  
 *  @author Siim Orasmaa
 */
public enum Granulaarsus {
		MINUTE      	 (0, "M" ),
		
		HOUR_OF_HALF_DAY (1, "H" ), 
		AM_PM			 (1, null),
		HOUR_OF_DAY		 (1, "H" ), // parsitakse lahti v22rtusteks AM_PM ja HOUR_OF_HALF_DAY
		TIME        	 (1, null), // parsitakse lahti v22rtusteks HOUR_OF_DAY ja MINUTE
		
		DAY_OF_WEEK 	 (2, "D" ),
		DAY_OF_MONTH	 (2, "D" ),
		DATE        	 (2, null), // parsitakse lahti v22rtusteks DAY_OF_MONTH ja MONTH
		
		WEEK_OF_YEAR	 (3, "W" ),
		
		MONTH       	 (4, "M" ),
		
		YEAR       		 (5, "Y" ), // mõeldakse täiskujul aastaarvu (nt 2010);
		YEAR_OF_CENTURY  (5, null), // mõeldakse sajandi alla kuuluvaid aastaid/aastakümneid (nt 2010 --> 10);
		CENTURY_OF_ERA   (6, "C" ); // mõeldakse aastarvu kaht esimest numbrit (nt 2010 --> 20);
	
	public final static int LOWEST  = 0;
	public final static int HIGHEST = 1;
	
	/**
	 *    S6ne, mis t2histab seda, et k6ik granulaarsused on lubatud. 
	 */
	public final static String ALL_GRANULARITIES = "*";
	
	/**
	 *    Granulaarsus-v2ljad sellises olukorras, milles neid on turvaline Joda Times LocalDate ja 
	 *    LocalTime objektide peal rakendada...
	 */
	public final static Granulaarsus fieldsInSafeOrder [] = {
		CENTURY_OF_ERA, YEAR, MONTH, WEEK_OF_YEAR, DAY_OF_MONTH, DAY_OF_WEEK, AM_PM, HOUR_OF_DAY, HOUR_OF_HALF_DAY, MINUTE   
	};
	
	/**
	 *    J&auml;me granulaarsuse aste. Mida v&auml;iksem number, seda v&auml;iksem
	 *   granulaarsus. J&auml;me granulaarsus ei tee vahet minutitel ja tundidel 
	 *   (k&otilde;ik kuulub "kellaaja" alla), samuti ei eristata n&auml;dalap&auml;evi
	 *   ning kuup&auml;evi.  
	 */
	private int coarseRank;
	
	/**
	 *   Antud granulaarsus ISO 8601 kestvus-tähisena. Võib ka puududa, sellisel juhul null.
	 */
	private String ISOdurationElement;
	
	
	private Granulaarsus(int coarseRank, String ISOdurationElement) {
		this.coarseRank         = coarseRank;
		this.ISOdurationElement = ISOdurationElement;
	}
	
	public static Granulaarsus getGranulaarsus(String granAsString){
		try {
			Granulaarsus gran = Granulaarsus.valueOf(granAsString);
			return gran;
		} catch (Exception e) {
			return null;
		}
	}
	
	public int compareByCoarseRank(Granulaarsus o) {
		if (this.coarseRank == o.coarseRank){
			return 0;
		} else {
			return (this.coarseRank > o.coarseRank) ? (1) : (-1);
		}
	}

	public int getCoarseRank() {
		return this.coarseRank;
	}
	
	public String getAsISOdurationElement(){
		return this.ISOdurationElement;
	}
	
}
