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

package ee.ut.soras.ajavtV2.mudel.sonamallid;

/**
 *  Millisel moel vastab sobitatav sona sonamallile?
 * 
 *  @author Siim Orasmaa
 */
public enum MallileVastavus {
	/**
	 *   Sobitatav sona ei vasta yldse sonamallile.
	 */
	EI_VASTA,
	/**
	 *   Sona vastab sonamalli algusele - st sonamalli n&auml;ol
	 *   on tegemist mingi fraasiga ning sona vastab selle 
	 *   algusele.
	 */
	VASTAB_ALGUS,
	/**
	 *   Sona vastab sonamalli keskosale - st sonamalli n&auml;ol
	 *   on tegemist mingi fraasiga ning sona vastab selle 
	 *   keskosale.
	 */	
	VASTAB_KESKOSA,
	/**
	 *   Kaks juhtu: kas sonamall koosnebki yhest sonast ning sobitatav
	 *   sona vastab sellele taielikult (1) v&otilde;i sonamall koosneb
	 *   fraasist ning etteantud sona rahuldab fraasi viimase elemendi
	 *   malli (2).
	 */		
	VASTAB_LOPLIKULT
}
