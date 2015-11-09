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

import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;

/**
 * Regulaaravaldisel p&otilde;hinev s&otilde;namall.
 * 
 * @author Siim Orasmaa
 */
public class RegExpSonaMall extends SonaMalliSemOsa implements SonaMall {
	
	private Pattern regexp;
	
	/**
	 *  Loob etteantud regulaaravaldise p&otilde;hjal uue
	 * s&otilde;namalli.
	 * 
	 * @param regulaaravaldis
	 */
	public RegExpSonaMall(String regulaaravaldis) {
		try {
			this.regexp = Pattern.compile(regulaaravaldis);
		} catch (PatternSyntaxException e) {
			// kui ei 6nnestunud regulaaravaldist
			// luua, j22b see reegel nn. nullreegliks 
			this.regexp = null;
		}
	}

	public MallileVastavus vastabMallile(AjavtSona sona) {
		boolean vastabMallile = false;
		if (this.regexp != null){
			Matcher m = regexp.matcher(sona.getAlgSonaYmbritsevateMarkideta());
			vastabMallile = m.matches();
		}
		return (vastabMallile) ? (MallileVastavus.VASTAB_LOPLIKULT) : (MallileVastavus.EI_VASTA);
	}

	
	public String toString() {
		return "/"+regexp.pattern()+"/";
	}

	public TYYP getTyyp() {
		return SonaMall.TYYP.REGEXP;
	}

	@Override
	public List<SemantikaDefinitsioon> tagastaMalliSemantilineOsa(AjavtSona viimaneSobitunudSona){
		// Teeme kindlaks, kas semValue tuleb v6tta mustriga sobitunud s6nast
		int references [] = super.getSemValueReferences();
		List<SemantikaDefinitsioon> semDefs = super.tagastaMalliSemantilineOsa(viimaneSobitunudSona);
		if (references != null && semDefs != null && this.regexp != null && references.length == semDefs.size()){
			List<String> sobitunudAlamgrupid = 
				this.parsiSobitunudAlamgrupid(viimaneSobitunudSona, references);
			if (!sobitunudAlamgrupid.isEmpty()){
				for (int i = 0; i < references.length; i++) {
					SemantikaDefinitsioon semDef = semDefs.get(i);
					int reference = references[i];
					if (reference > -1 && semDef != null){
						if ((sobitunudAlamgrupid.get(i)).length() > 0){
							semDef.setSemValue( sobitunudAlamgrupid.get(i) );
						}
					}
				}
			}
		}
		return semDefs;
	}
	
	/**
	 *   Parsib selle regulaaravaldis-malliga sobituvast sonast <tt>viimaneSobitunudSona</tt> k6ik alamgrupid,
	 * mille indeksid on toodud massiivis <tt>references</tt>. Tagastab listi parsitud alams6nedest, sellises
	 * j2rjekorras, nagu indeksid olid toodud massiivis <tt>references</tt>. NB! Iga massiivi <tt>references</tt>
	 * element, mille v22rtus < -1, saab uues massiivis v22rtuse <tt>""</tt>. 
	 * 
	 * @param sobituvSona sona, mis sobitub antud regulaaravaldis-malliga
	 * @param references regulaaravaldise poolt eraldatud alamgruppide indeksid, mis tuleb v2lja parsida
	 * @return tagastab listi parsitud alams6nedest
	 */
	public List<String> parsiSobitunudAlamgrupid(AjavtSona sobituvSona, int references []){
		List<String> alamSoned = new LinkedList<String>();
		if (references != null){
			Matcher m = regexp.matcher(sobituvSona.getAlgSonaYmbritsevateMarkideta());
			if (m.matches()){			
				for (int i = 0; i < references.length; i++) {
					int reference = references[i];
					if (reference > -1){
						//
						//  indekseerimise loogika:  ((A)(B(C)))
						//  
						//   1     	((A)(B(C)))
						//   2     	(A)
						//   3     	(B(C))
						//   4     	(C)
						//
						if (reference <= m.groupCount()){
							alamSoned.add( m.group(reference) );
						}
					} else {
						alamSoned.add("");
					}
				}
			}
		}
		return alamSoned;
	}
	
}
