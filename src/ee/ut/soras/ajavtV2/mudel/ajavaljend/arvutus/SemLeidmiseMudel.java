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

package ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus;

import java.util.List;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;

/**
 *  Mudel tekstist eraldatud ajav&auml;ljendite semantika leidmiseks.
 * 
 * @author Siim Orasmaa
 */
public interface SemLeidmiseMudel {
	
	/**
	 *   Viib l&auml;bi tekstist eraldatud ajav&auml;ljendite semantika leidmise. 
	 * Meetod teostab ise vajadusel erinevad eelt&ouml;&ouml;tluse protseduurid 
	 * (nt ajav&auml;ljendite ankurdamine verbide ja teiste ajav&auml;ljenditega).
	 * <p>
	 * T&ouml;&ouml; tulemusena peaks iga korrektselt eraldatud ajav&auml;ljendi(kandidaadi)
	 * alla tekkima semantikalahendus.
	 * 
	 * @param sonad sisendtekst
	 * @param konehetk k&otilde;nehetk teksti loomisel
	 */
	void leiaSemantika(List<AjavtSona> sonad, String [] konehetk);
	
	
	/**
	 *  Semantika leidmise mudeli t2hised.
	 */
	String [] getMudeliTahised();
}
