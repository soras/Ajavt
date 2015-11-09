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
