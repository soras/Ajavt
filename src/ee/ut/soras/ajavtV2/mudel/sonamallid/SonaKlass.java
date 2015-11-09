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

import java.util.ArrayList;
import java.util.List;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;
import ee.ut.soras.wrappers.mudel.MorfAnSona;

/**
 *  Sonaklass on sonamall, mis koosneb teistest sonamallidest (<i>elementidest</i>)
 * ning selle rahuldamiseks peab sona rahuldama yht sonaklassi alla kuuluvat
 * sonamalli.
 * <p>
 * Kaesoleval juhul ei toeta implementatsioon rekursiooni, st sonaklassi 
 * elemendid ei tohi ise olla sonaklassid. 
 * 
 * @author Siim Orasmaa
 */
public class SonaKlass extends SonaMalliSemOsa implements SonaMall {

	private List<SonaMall> elemendid = null;
	
	private String nimi;
	
	public SonaKlass(String nimi) {
		super();
		this.nimi = nimi;
		this.elemendid = new ArrayList<SonaMall>();
	}

	public TYYP getTyyp() {
		return TYYP.SONAKLASS;
	}

	public String tagastaMallileVastavOsa(MorfAnSona sona, int mitmes) {
		return null;
	}

	public MallileVastavus vastabMallile(AjavtSona sona) {
		for (SonaMall sonaMall : elemendid) {
			MallileVastavus vastavus = sonaMall.vastabMallile(sona);
			if (vastavus != MallileVastavus.EI_VASTA){
				return vastavus;
			}
		}
		return MallileVastavus.EI_VASTA;
	}

	/**
	 *   Lisab kaesolevale sonaklassile uue liikme (elemendi);
	 */
	public void lisaElement(SonaMall element){
		elemendid.add(element);
	}
	
	/**
	 *   Tagastab sonaklassi liikmed (elemendid);
	 */
	public List<SonaMall> getElemendid() {
		return elemendid;
	}

	public String getNimi() {
		return nimi;
	}
	
	@Override
	public String toString() {
		return this.getNimi();
	}

	@Override
	public List <SemantikaDefinitsioon> tagastaMalliSemantilineOsa(AjavtSona viimaneSobitunudSona){
		// Leiame sonamalli, millega s6na meetodi "vastabMallile" v2ljakutsel
		// sobitus, ning eraldame selle s6namalli semantilise osa.
		for (SonaMall sonaMall : elemendid) {
			MallileVastavus vastavus = sonaMall.vastabMallile(viimaneSobitunudSona);
			if (vastavus == MallileVastavus.VASTAB_LOPLIKULT){
				List<SemantikaDefinitsioon> semDefsFromSonaMall = sonaMall.tagastaMalliSemantilineOsa(viimaneSobitunudSona);
				if (semDefsFromSonaMall != null && !semDefsFromSonaMall.isEmpty()){
					// margistame s6naklassiga
					for (SemantikaDefinitsioon semantikaDefinitsioon : semDefsFromSonaMall) {
						semantikaDefinitsioon.setSonaKlass(this.nimi);
					}
					// tagastame
					return semDefsFromSonaMall;
				}
			}
		}
		return null;
	}
	
}
