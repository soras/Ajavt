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

import ee.ut.soras.ajavtV2.mudel.AjavtSona;

/**
 *  T&auml;iesti tavalist teksti sobitav s&otilde;namall. Ignoreerib v6rdlemisel
 *  suur-ja v2iket2htede erinevusi ning sobitatava teksti l6pus ja alguses olevat 
 *  punktatsiooni.
 *  
 *  @author Siim Orasmaa
 */
public class TavaTekstSonaMall extends SonaMalliSemOsa implements SonaMall {

	private String tekst;
	
	public TavaTekstSonaMall(String tekst) {
		this.tekst = tekst;
	}
	
	public TYYP getTyyp() {
		return TYYP.TAVATEKST;
	}

	public MallileVastavus vastabMallile(AjavtSona sona) {
		String sobitatav = sona.getAlgSonaYmbritsevateMarkideta();
		if (sobitatav.equalsIgnoreCase(tekst)){
			return MallileVastavus.VASTAB_LOPLIKULT;
		}
		return MallileVastavus.EI_VASTA;
	}
	
	@Override
	public String toString() {
		return "'"+this.tekst+"'";
	}
	
}
