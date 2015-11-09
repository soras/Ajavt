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

import java.util.List;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;

/**
 *  S&otilde;namall - sisuliselt yksiku s&otilde;na kirjeldus.
 *  K&otilde;ik fraasimustri elemendid laiendavad seda liidest.
 * 
 * @author Siim Orasmaa
 */
public interface SonaMall {
	
	//============================================================
	//   Y l d i n e   l i i d e s   +
	//     m a l l i g a   s o b i t a m i n e
	//  (pattern matching)
	//============================================================	
	
	/**
	 *  Sonamalli tyyp.
	 */
	public static enum TYYP { SONAKLASS, ARVSONAFRAAS, ALGVORM, REGEXP, TAVATEKST, ERIARV };
	
	/**
	 *  Leiab, kas & mil m&auml;&auml;ral vastab etteantud s&otilde;na k&auml;esolevale 
	 * s&otilde;namallile.  
	 * 
	 * @param sona morf analyysitud sona
	 */
	MallileVastavus vastabMallile(AjavtSona sona);
	
	/**
	 * Tagastab antud sonamalli tyybi.
	 */
	TYYP getTyyp();
	
	//============================================================
	//   S e m a n t i k a   l i i d e s   j a
	//     e r a l d a m i n e
	//  (defining and extracting semantics)	
	//============================================================
	
	/**
	 *   Lisab malli kylge selle semantika definitsiooni yhe osa
	 *   (<tt>semField</tt>): 
	 *  millist ajalist granulaarsust antud s6namall m6jutab.
	 *  <p>
	 *  Tasub t2heldada, et semantika defineerimine ei ole kohustuslik: 
	 *  s6namall v6ib olla t2iesti ilma igasuguse semantikata.  
	 */
	void lisaSemField(String semField);

	/**
	 *   Lisab malli kylge selle semantika definitsiooni yhe osa 
	 *   (<tt>semValue</tt>): 
	 *  millised on m6jutatava ajalise granulaarsuse v6imalikud 
	 *  v22rtused v6i m6jutamise instruktsioonid .
	 *  <p>
	 *  Tasub t2heldada, et semantika defineerimine ei ole kohustuslik: 
	 *  s6namall v6ib olla t2iesti ilma igasuguse semantikata.  
	 */
	void lisaSemValue(String semValue);
	
	/**
	 *   Operatsioon, mida rakendatakse semantika arvutamisel (SET, ADD v6i SEEK). Selle puudumisel
	 *   on semantikadefinitsioon m6ttetu.
	 */
	void lisaOp(String operatsioon);

	/**
	 *   Lisab konventsionaalset semantikat t&auml;histava lipiku. Kasutusel
	 *  kokkuleppelise semantikaga v&auml;ljendite, nagu nt <i>talvel, hommikul</i> jms
	 *  semantika edasiandmisel.
	 */
	void lisaSemLabel(String semLabel);
	
	/**
	 *   Margib, et etteantud semantikadefinitsioon (<tt>semValue</tt>) on
	 *   <i>ebat&auml;pne</i> ning v&otilde;imalusel tuleks seda t&auml;psustada,
	 *   v&otilde;ttes appi sama v&otilde;i peenema granulaarsusega 
	 *   semantikadefinitsiooni.
	 *   <p>
	 *   Vaikimisi on koik semantikadefinitsioonid <i>t&auml;psed</i>, st t&auml;psustamist ei vaja.
	 */
	void setSemValueOnEbatapne(boolean semValueOnEbatapne);
	
	
	/**
	 *   Lisab antud sonamallile SEEK-operatsiooni rakendamise suuna. Kasutusel ainult
	 *  SEEK-tyypi operatsioonide puhul;
	 * 
	 * @param seekDir suund
	 */
	void setSeekDirection(String seekDir);

	/**
	 *   Kasutatav semantika lahendamise mudel. Yldjuhul m22ramata, kasutatakse eksperimendi-spetsiifiliste
	 *  semantikadefinitsioonide eristamiseks. 
	 * 
	 * @param seekDir suund
	 */
	void setMudel(String mudel);
	
	/**
	 * <p>
	 *  Tagastab antud malliga seotud semantilise osa. Kui malliga
	 * yldse mingit semantilist osa seotud pole, tagastab vaartuse 
	 * <tt>null</tt>.
	 * <p>
	 *  Parameeter <tt>viimaneSobitunudSona</tt> peab olema s6na,
	 * mille etteandmine selle sama s6namalli meetodile 
	 * <tt>vastabMallile</tt> andis tulemuseks <tt>VASTAB_LOPLIKULT</tt>.
	 * Arvs6nafraas-sonamalli ja m6nikord ka regulaaravaldis-s6namalli
	 * puhul leiab <tt>viimaneSobitunudSona</tt> kasutust arvude parsimisel
	 * s6nast.
	 * </p> 
	 * 
	 * @param viimaneSobitunudSona viimane sonamalli rahuldanud s&otilde;na
	 */	
	List<SemantikaDefinitsioon> tagastaMalliSemantilineOsa(AjavtSona viimaneSobitunudSona);
	
}
