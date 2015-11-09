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
