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

package ee.ut.soras.ajavtV2.mudel.ajavaljend;

import ee.ut.soras.ajavtV2.mudel.MustriTahis;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat.ASTE;

/**
 *  Kindlate t&auml;histega (<tt>MustriTahis</tt>) ajav&auml;ljendikandidaatide liitumist lubav/kontrolliv reegel. 
 * 
 *  @see MustriTahis
 * 
 *  @author Siim Orasmaa
 */
public class LiitumisReegel {

	/**
	 *  Kas mustritahised peavad olema t&auml;pselt selles jarjekorras, nagu need on
	 *  muutujas <tt>mustriTahised</tt>? Kui <tt>false</tt>, siis on jarjekord vaba.
	 */
	private boolean fikseeritudJarjekord = false;
	
	/**
	 *   Kas mustritahiste alla kuuluvad fraasid peavad olema vahetult yksteisega k6rvuti,
	 *  v6i v6ib nende vahele j22da m6ni teine ajavaljend, mis (ainult) yhega neist sobitub?
	 *  Kui <tt>true</tt>, siis fraaside vahele teisi ajav2ljendeid ei lubata.
	 */
	private boolean tapseltKorvuti = true;
	
	/**
	 *  Mustritahised, mis maaravad, millised k6rvutiseisvad ajavaljendikandidaadid
	 *  v6ivad liituda.
	 */
	private String [] mustriTahised;
	
	/**
	 * Millise astmega on yhendamisel tekkiv uus, yhendkandidaat?
	 */
	private ASTE yhendamiseAste = ASTE.YHENDATUD_FRAASINA;

	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================
	
	public boolean onFikseeritudJarjekord() {
		return fikseeritudJarjekord;
	}

	public void setFikseeritudJarjekord(boolean fikseeritudJarjekord) {
		this.fikseeritudJarjekord = fikseeritudJarjekord;
	}

	public String[] getMustriTahised() {
		return mustriTahised;
	}

	public void setMustriTahised(String[] mustriTahised) {
		this.mustriTahised = mustriTahised;
	}

	public ASTE getYhendamiseAste() {
		return yhendamiseAste;
	}

	public void setYhendamiseAste(ASTE yhendamiseAste) {
		this.yhendamiseAste = yhendamiseAste;
	}
	
	public boolean isTapseltKorvuti() {
		return tapseltKorvuti;
	}

	public void setTapseltKorvuti(boolean tapseltKorvuti) {
		this.tapseltKorvuti = tapseltKorvuti;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.fikseeritudJarjekord){
			sb.append("<s> ");
		}
		if (this.mustriTahised != null){
			for (String mustriTahis : this.mustriTahised) {
				sb.append(mustriTahis+" ");
			}
		}
		return sb.toString();
	}
	
}
