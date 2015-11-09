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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;

/**
 *  Defineeriva komponendi (<tt>SemDef</tt> v6i <tt>MustriTahis</tt>) mustrists6ltuv osa.
 *  V6imaldab kontrollida, kas eraldamise tulemus (malli rahuldavad alamfraasid ning rahuldatud mustriosad)
 *  vastavad komponendi peale pandud mustrikitsendusele (<tt>seotudMustriOsa</tt>).
 * 
 * @author Siim Orasmaa
 */
public class MustristSoltuv {
	
	/**
	 *   Milline mustriosa (v6ib koosneda rohkem kui yhest alammustrist) kutsub esile
	 *   antud komponendi (olgu selleks siis semantikadefinitsiooni v6i mustritahis)
	 *   rakendamise? Esialgu kasutusel ainult tag'ide SemDef ja MustriTahis korral;
	 *   <p>
	 *   Analoogselt v2lja <tt>sonaKlass</tt> v22rtustele: A-Z ja _ t2histavad s6naklasse,
	 *   0-9 viidet aga mustri alamosale, mis ei pruugi olla s6naklass.
	 *   <p>  
	 * 	 N&auml;ide mustrist:		
	 * 	 <pre>
	 *        ^JARGMINE NADALAPAEV
	 *   </pre>
	 *   T&auml;hendus: lahendusk&auml;ik rakendub, kui s&otilde;namalli JARGMINE sobitamine
	 *   eba6nnestus ning s6namalli NADALAPAEV sobitamine 6nnestus.
	 *   <p>
	 *   <b>Loogika SemDef puhul:</b>
	 *   Kui mustriosaks on yksik s6naklassinimi, t2idetakse lihtsalt s6naklassi alla kuuluvad
	 *   semantikadefinitsioonis puuduolevad v2ljad, uut semantikadefinitsiooni objekti ei 
	 *   looda. Kui muster on keerulisem v6i t2itmisele kuuluvad v2ljad on juba olemas, 
	 *   luuakse uus <tt>SemantikaDefinitsioon</tt> objekt.
	 *   <p>
	 *   <b>Loogika MustriTahis puhul:</b>
	 *   Kui mustriosa klapid eraldatud ajav2ljendi rahuldatud alamosadega, kleebitakse 
	 *   ajavaljendi kylge MustriTahis all toodud t2hised.
	 */
	protected String seotudMustriOsa;
	
	//==============================================================================
	//   	M u s t r i g a     s o b i t a m i n e
	//==============================================================================
	
	/**
	 *     Kontrollib, kas <tt>malliRahuldavadAlamFraasid</tt> ja/voi <tt>rahuldatudMustriosad</tt> 
	 *    vastavad selle komponendi mustrile (<tt>seotudMustriOsa</tt>) ning kui vastavad,
	 *    tagastab j2rjendi positiivselt sobitunud alamosadest. Tagastab <tt>null</tt> kui
	 *    sobitamine mustriga ei 6nnestunud. Positiivselt sobitunud alamosade all m6eldakse alamosasid,
	 *    mis olid olemas, kui nende olemasolu mustris n6uti.
	 *    <p>
	 *    NB! Seega, kui kogu muster koosneb ainult negatiivsetest n2idetest (st toodud vaid elemendid, 
	 *    mida ei tohi olemas olla), tagastatakse siin alati null, hoolimata
	 *    sellest, milline tulemus oli. J2relikult: selliseid mustreid praegu ei toetata.
	 *    <p>  
	 * 	  N&auml;ide mustrist:		
	 * 	  <pre>
	 *        ^JARGMINE NADALAPAEV
	 *    </pre>
	 *    Et muster saaks rahuldatud, ei tohi olla leitud sonaklassi <tt>JARGMINE</tt> ning peab olema leitud 
	 *    sonaklass <tt>NADALAPAEV</tt>. 
	 *    <p> 
	 *    <b>Reegliskeemi parema selguse</b> huvides tuleks mitme positiivse n2ite olemasolul ymbritseda
	 *    n2ide, mida soovitakse t2iendada, sulgudega, n2iteks nii:
	 * 	  <pre>
	 *        IGA ^JARGMINE (NADALAPAEV)
	 *    </pre>
	 *    Kui meetod leiab, et rahuldatud on nii IGA kui ka NADALAPAEV sonamall, tagastab ta just viimase,
	 *    kuna see on sulgudega ymbritsetud.
	 */
	public List<String> leiaPositiivseltRahuldatudMustriosad(
							HashMap<String, List<AjavtSona>> malliRahuldavadAlamFraasid, 
							HashMap<String, String> rahuldatudMustriosad){
		if (this.seotudMustriOsa != null){
			// 1) parsime mustri lahti
			StringTokenizer st = new StringTokenizer(this.seotudMustriOsa);
			List<String> muster = new ArrayList<String>( st.countTokens() );
			while (st.hasMoreTokens()){
				muster.add( st.nextToken() );
			}
			List<String> positiivseltRahuldatudMustriOsad = new ArrayList<String>();
			String esileToodudMustriOsa = null;
			// 2) Kontrollime mustrile vastavust
			for (int i = 0; i < (muster).size(); i++) {
				String mustriElement = (muster).get(i);
				// Kas on tegu "esiletoodud" mustriosaga (sulud ymber)?
				boolean esileToodud = (mustriElement.startsWith("(") && mustriElement.endsWith(")"));
				if (esileToodud){
					mustriElement = mustriElement.replace("(", "");
					mustriElement = mustriElement.replace(")", "");
				}
				boolean eitus        = mustriElement.startsWith("^");
				if (eitus){
					mustriElement = mustriElement.replace("^", "");
					// Kontrollime, et antud mustrit EI esinenud - kui esines, on mittesobivus (mismatch)
					if (rahuldatudMustriosad.containsKey(mustriElement) || 
							malliRahuldavadAlamFraasid.containsKey(mustriElement)){
						return null;
					}
				} else {
					// Kontrollime, et antud mustrit esines - kui ei esinenud, on mittesobivus (mismatch)
					if (!rahuldatudMustriosad.containsKey(mustriElement) && 
							!malliRahuldavadAlamFraasid.containsKey(mustriElement)){
						return null;
					} else {
						positiivseltRahuldatudMustriOsad.add(mustriElement);
						if (esileToodud){ esileToodudMustriOsa = mustriElement; }						
					}
				}
			}
			if (esileToodudMustriOsa != null){
				positiivseltRahuldatudMustriOsad.clear();
				positiivseltRahuldatudMustriOsad.add(esileToodudMustriOsa);
			}
			return (!positiivseltRahuldatudMustriOsad.isEmpty()) ? (positiivseltRahuldatudMustriOsad) : (null);
		}
		return null;
	}
	
}
