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
