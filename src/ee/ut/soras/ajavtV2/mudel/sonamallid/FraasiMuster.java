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

import java.util.HashMap;
import java.util.List;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.TuvastamisReegel;

/**
 *  Liides fraasimustrite sobitamise loogikat implementeerivatele klassidele. 
 *  
 *  @author Siim Orasmaa
 */
public interface FraasiMuster {

	/**
	 *  Kontrollib, kas etteantud s&otilde;na vastab fraasimustri mingile osale. 
	 * Fraasimuster peab ise j&auml;rge, mitu vastavust ta juba on leidnud, ning kui 
	 * j&auml;rjestikku on olnud piisavalt palju positiivseid kontrollimistulemusi
	 * rahuldamaks kogu mustrit, konstrueeritakse uus ajavaljendikandidaat ning seotakse
	 * sonadega. 
	 */
	public void kontrolliMustrileVastavust(AjavtSona sona,
			HashMap<String, MallileVastavus> kontrollitudSonaKlassid) throws Exception;

	public String getMustriID();

	public void setMustriID(String mustriID);

	public List<SonaMall> getSonaMallid();

	public void setTuvastamisReegel(TuvastamisReegel tuvastamisReegel);

	public String toString();

}