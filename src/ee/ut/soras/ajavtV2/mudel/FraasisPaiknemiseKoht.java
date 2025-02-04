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

package ee.ut.soras.ajavtV2.mudel;

/**
 *   M&auml;&auml;rab, kuidas paikneb AjavtSona mingis margendatud fraasis:
 *   <blockquote>
 *   	<li><code>PUUDUB</code> - sona ei paikne viidatud fraasis;</li>
 *   	<li><code>ALGUSES</code> - sona on esimene sona mingis fraasis;</li>
 *      <li><code>KESKEL</code> - sona on mingi fraasi keskel;</li>
 *      <li><code>LOPUS</code> - sona on mingi fraasi viimane sona;</li>
 *      <li><code>AINUSSONA</code> - kui fraas koosnebki vaid yhest sonast;</li>
 *   </blockquote>
 */
public enum FraasisPaiknemiseKoht { PUUDUB, ALGUSES, KESKEL, LOPUS, AINUSSONA;

	/**
	 *   Kas antud fraasis paiknemise koht on fraasi algus? Fraasi alguse alla
	 *  loetakse jargmised fraasispaiknemise kohad: <tt>ALGUSES</tt> ja <tt>AINUSSONA</tt>.
	 */
	public boolean onFraasiAlgus(){
		return (this == ALGUSES || this == AINUSSONA);
	}

	/**
	 *   Kas antud fraasis paiknemise koht on fraasi lopp? Fraasi lopu alla
	 *  kuuluvad jargmised fraasispaiknemise kohad: <tt>LOPUS</tt> ja <tt>AINUSSONA</tt>.
	 */
	public boolean onFraasiLopp(){
		return (this == LOPUS || this == AINUSSONA);
	}
}