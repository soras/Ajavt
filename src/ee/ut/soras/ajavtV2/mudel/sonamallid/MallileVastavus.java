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
