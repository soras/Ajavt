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
