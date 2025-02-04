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

package ee.ut.soras.wrappers.erind;

/**
 *  Erind, mis visatakse siis, kui estyhmm katkestab t&ouml;&ouml; 
 *  ja l&otilde;petab veateatega.
 * 
 * @author Siim Orasmaa
 */
public class EstYhmmErind extends Exception {
	private static final long serialVersionUID = 1L;

	private String msg = "";
	
	public EstYhmmErind(String msg) {
		if (msg != null){
			this.msg = msg;
		}
	}
	
	@Override
	public String getMessage() {
		return this.msg;
	}
	
}
