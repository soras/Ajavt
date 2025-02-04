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

package ee.ut.soras.wrappers;



/**
 *   M&auml;his estyhmm k&auml;ivitamiseks v&auml;lise protsessina.
 *  Voimaldab anda estyhmmile analyysimiseks teksti ette
 *  ning tagastab saadud analyysitulemused.
 * 
 * @author Siim Orasmaa
 */
public interface EstyhmmWrapper {

	/**
	 *  K&auml;ivitab estyhmm-i v&auml;lise protsessina, annab talle edasi
	 * laused s&otilde;nest <code>text</code> ning tagastab estyhmmilt saadud
	 * analyysi tulemused. 
	 * 
	 * @param text laused, mis antakse estyhmmile analyysimiseks
	 * @return estyhmmi v&auml;ljund (analyysi tulemused)
	 * @throws Exception erind, kui teksti t&ouml;&ouml;tlemise mingis etapis
	 *          ilmnes t&otilde;rge
	 */
	public String process(String text) throws Exception;

}