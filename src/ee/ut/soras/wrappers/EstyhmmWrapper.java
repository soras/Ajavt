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