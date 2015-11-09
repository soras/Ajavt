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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 *  L&otilde;im v&auml;lise protsessi sisendvoost 
 * andmete lugemiseks; 
 * 
 * @author Siim Orasmaa
 */
public class SisendVooLugejaLoim extends Thread {
	
	 /**
	 *  Loetav sisendvoog;
	 */
	private InputStream sisendVoog;
	 /**
	 *  Sisendvoost loetud s&otilde;ne;
	 */
	private String sisendVooValjund = "-";
	
	/**
	 * Sisendvoo kodeering. Vaikimisi UTF8. 
	 */
	private String charset = "UTF8";
	
	public SisendVooLugejaLoim(InputStream is, String charset) {
		this.sisendVoog = is;
		this.charset = charset;
	}

    public void run()
    {
    	StringBuffer puhver = new StringBuffer();
        try {
           InputStreamReader isr = new InputStreamReader(sisendVoog, this.charset);
           BufferedReader br = new BufferedReader(isr);
           String line = null;
           while ( (line = br.readLine()) != null){
        	   (puhver.append(line)).append("\n");
           }
        } catch (IOException ioe){
           ioe.printStackTrace();  
        }
        sisendVooValjund = puhver.toString();
    }

	public String getSisendVooValjund() {
		return sisendVooValjund;
	}
    
}
