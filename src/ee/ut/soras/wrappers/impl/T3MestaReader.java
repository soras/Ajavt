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

package ee.ut.soras.wrappers.impl;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import ee.ut.soras.ajavtV2.util.TextUtils;
import ee.ut.soras.wrappers.mudel.MorfAnSona;

/**
 *  T3mesta formaadis (t3mesta morf analyys + yhestamine) sisendist morfoloogilise analyysi 
 *  v2ljalugemine ning paigutamine MorfAnSona andmeobjektidesse; 
 * 
 * @author Siim Orasmaa
 */
public class T3MestaReader {

	/** Muster: Potentsiaalne lausealgus. */
	private static Pattern musterLauseAlgus = Pattern.compile("^\\p{Lu}");
	/** Muster: Potentsiaalne lauselopp. */	
	private static Pattern musterLauseLopp = Pattern.compile("[?!.]$");
	
	/**
	 *   Kontrollib, kas etteantud s6na algab suurt2hega (potentsiaalne lausealgus);
	 */
	private static boolean kasSonaOnSuurt2heliseAlgusega(String sona){
		return (musterLauseAlgus.matcher(TextUtils.trimSurroundingPunctation(sona))).find();
	}

	/**
	 *   Kontrollib, kas etteantud s6na l6peb lausel6pum2rgiga;
	 */
	private static boolean kasSonaLopebLauselopuMargiga(MorfAnSona sona){
		return (musterLauseLopp.matcher(sona.getAlgSona()).find());
	}
	
	/**
	 *  Eraldab T3mesta formaadile vastavast sisendvoost morf analyysi tulemused ning paigutame andmemudelisse (MorfAnSona-de
	 *  jarjendisse). 
	 *  Eeldab, et morf analyys on kujul:
	 *  <pre>
	 *    Mees    mees+0 //_S_ sg n, //    mesi+s //_S_ sg in, //
	 *    peeti    peet+0 //_S_ adt, sg p, //    pida+ti //_V_ ti, //
	 *    ...
	 *  </pre>
	 * 
	 * @param input sisendvoog, milles on v&auml;liselt protsessilt morf analyysi tulemused
	 * @return nimestik eraldatud morf analyysi tulemustest
	 * @throws IOException kui sisendvoost lugemisel peaks ilmnema mingi t&otilde;rge
	 */
	public static List<MorfAnSona> parseT3mestatext( BufferedReader input ) throws Exception {
		List<MorfAnSona> tulemused = new ArrayList<MorfAnSona>();
		String rida;
		String sona = null;
		MorfAnSona eelmineSona = null;
		int tokenPosition = 1;
		while ((rida = input.readLine()) != null){
			// ---------------------------------------------------------------------------------------
			//  Eeldame, et analyys on kujul:
			//
			//     Mees    mees+0 //_S_ sg n, //    mesi+s //_S_ sg in, //
			//     peeti    peet+0 //_S_ adt, sg p, //    pida+ti //_V_ ti, //
			//
			// ---------------------------------------------------------------------------------------
			if (rida.length() > 0){
				// Analyyside vahel on eraldajaks t2pselt 4 tyhikut
				String [] parts = rida.split("\\s{4}");
				if (parts.length < 2){
					throw new IOException("Unable to parse the output of morphological analyzer - unexpected format: "+rida);
				} else {
					sona = parts[0];
					MorfAnSona jooksevSona = new MorfAnSona( sona );
					jooksevSona.setTokenPosition(tokenPosition);
					for (int i = 1; i < parts.length; i++) {
						jooksevSona.lisaAnalyysiRida( TextUtils.ltrim( parts[i] ) );
					}
					// ---------------------------------------------------------------------------------------
					//   "Eelmise s6na" heuristikud
					// ---------------------------------------------------------------------------------------
					if (eelmineSona != null){
						// 1.0.0) Lausel6puheuristik: kui s6na algab suure t2hega ja 
						//      eelmise l6pus on lausel6pum2rk
						if (kasSonaOnSuurt2heliseAlgusega(sona)){
							if (kasSonaLopebLauselopuMargiga(eelmineSona)){
								eelmineSona.setOnLauseLopp(true);
							}
						}	
						// 1.0.5) Osalausete heuristik (ysna rumal): kui s6na l6pus on koma,
						// v6ib tegemist olla osalause l6puga  
						if ((eelmineSona.getAlgSona()).endsWith(",")){
							eelmineSona.setOlpOnKindelPiir(true);
						}
					}
					tulemused.add(jooksevSona);
					eelmineSona = tulemused.get(tulemused.size()-1);
					sona = null;
				}
				//if (logi != null) { logi.println(rida); }
			}
			tokenPosition++;
		}
		return tulemused;
	}
	
}
