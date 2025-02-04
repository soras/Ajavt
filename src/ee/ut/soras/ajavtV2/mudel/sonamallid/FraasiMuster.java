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