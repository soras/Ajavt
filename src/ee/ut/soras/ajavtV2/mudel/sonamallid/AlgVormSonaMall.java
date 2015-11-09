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
import java.util.StringTokenizer;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.wrappers.mudel.MorfAnRida;


/**
 * Algvormil p&otilde;hinev s&otilde;namall. Kui meetodile
 * {@link #vastabMallile(AjavtSona sona)} etteantud {@code sona}
 * sisaldab kasv&otilde;i yhte analyysi, mille tulemusena saadud
 * algvorm on v&otilde;rdne siinse malliga, loeme s&otilde;na
 * malli rahuldavaks.
 * <p>
 * Algvorme voib olla mitu, sel juhul peab nende vahel eraldajaks
 * olema '|'. Kui kontrollitav sona rahuldab kasvoi yhte malli
 * juurde kuuluvat algvormi, loetakse sona kogu malli rahuldavaks. 
 * <p>
 * Kui algvorm on kohustuslik atribuut, siis lisaks sellele voib
 * esineda ka atribuut {@code sonaliigid}, mis maarab, et lisaks
 * algvormile vastavusele kontrollitakse ka sonaliigi vastavust:
 * kui sona rahuldab kyll algvormi, aga sonaliiki mitte, siis 
 * loetakse sona ka malli mitte rahuldavaks. 
 * <p>
 * 
 * 
 * @author Siim Orasmaa
 */
public class AlgVormSonaMall extends SonaMalliSemOsa implements SonaMall {

	private List <String> algvormid;
	private List <String> sonaliigid;
	
	/**
	 *  Loob etteantud algvormiMustri p&otilde;hjal uue s&otilde;namalli.
	 * Kui algvorme on mitu, peab nende vahel olema eraldajaks '|'.
	 * Algvormi lopus sulgudes v&otilde;ib olla toodud sonaliigi tapsustus - sellisel
	 * juhul loetakse sobitus korrektseks vaid siis, kui vastavad nii sona lemma kui
	 * ka sonaliik.
	 * <p>
	 * N&auml;ide algvormiMustrist:		
	 * <pre>
	 *       &amp;uuml;ks(_N_)|esimene(_O_)
	 * </pre>
	 *  
	 * @param algvormid
	 */
	public AlgVormSonaMall(String algvormiMuster) {
		this.algvormid  = new ArrayList<String>();
		this.sonaliigid = new ArrayList<String>();		
		if (algvormiMuster != null){
			StringTokenizer tokenizer = new StringTokenizer(algvormiMuster, "|");
			while (tokenizer.hasMoreTokens()) {
				String algv = (String) tokenizer.nextToken();
				if (algv.indexOf("(") > -1){
					String sonaliik = algv.substring(algv.indexOf("(")+1, algv.length()-1);
					algv = algv.substring(0, algv.indexOf("("));
					this.algvormid.add(algv);
					this.sonaliigid.add(sonaliik);
				} else {
					this.algvormid.add(algv);
					this.sonaliigid.add("");
				}
			}
		}
	}
	
	public MallileVastavus vastabMallile(AjavtSona sona) {
		if (!(this.algvormid.isEmpty()) && sona.kasLeidusAnalyys()){
			for (int i = 0; i < algvormid.size(); i++) {
				String algv = algvormid.get(i);
				for (MorfAnRida rida : sona.getAnalyysiTulemused()) {
					// 1) kontrollime, kas algvorm vastab sona lemmale
					if ((rida.getLemmaIlmaVahemarkideta()).equalsIgnoreCase(algv)){
						// 2) kui on antud sonaliigid, kontrollime ka sonaliigi
						// vastavust... - tagastame TRUE vaid siis, kui vastab
						String sonaliik = sonaliigid.get(i);
						if (sonaliik.length() == 0 || (rida.getSonaliik()).equals(sonaliik)){
							return MallileVastavus.VASTAB_LOPLIKULT;
						}
					}
				}
			}
		}
		return MallileVastavus.EI_VASTA;
	}
	
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("|");
		for (int i = 0; i < algvormid.size(); i++) {
			builder.append(algvormid.get(i));
			if (!sonaliigid.isEmpty() && (sonaliigid.get(i)).length()>0){
				builder.append("(");
				builder.append(sonaliigid.get(i));
				builder.append(")");
			}
			if (i < algvormid.size()-1){
				builder.append("|");
			}
		}
		builder.append("|");
		return builder.toString();
	}


	public TYYP getTyyp() {
		return SonaMall.TYYP.ALGVORM;
	}

}
