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