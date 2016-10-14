//  Ajavt: Temporal Expression Tagger for Estonian
//  Copyright (C) 2009-2016  University of Tartu
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

package ee.ut.soras.ajavtV2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.FraasisPaiknemiseKoht;
import ee.ut.soras.ajavtV2.mudel.TuvastamisReegel;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat.ASTE;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.LiitumisReegel;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.PotentsLiidetavateKandidaatideJada;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.SemLeidmiseMudel;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.SemLeidmiseMudelImpl3;
import ee.ut.soras.ajavtV2.mudel.sonamallid.MallileVastavus;
import ee.ut.soras.ajavtV2.mudel.sonamallid.NegatiivneMuster;
import ee.ut.soras.ajavtV2.mudel.sonamallid.SonaKlass;
import ee.ut.soras.ajavtV2.mudel.sonamallid.SonaMall;
import ee.ut.soras.ajavtV2.util.LogiPidaja;
import ee.ut.soras.ajavtV2.util.MustridXMLFailist;
import ee.ut.soras.wrappers.EstyhmmWrapper;
import ee.ut.soras.wrappers.impl.EstyhmmWrapperImpl;

/**
 *    Ajav&auml;ljendite tuvastaja peamoodul (n&ouml; tuvastamismoodul). Koondab endas ajav&auml;ljendite
 *  tuvastamise p&otilde;hietappide loogikat.
 *  <p>
 *  Tavaline tuvastamisprotsess koosneb alametappidest:
 *  <ol>
 *  	<li><code>uuendaReegleid</code> - reeglite sisselugemine XML failist.</li>
 *  	<li><code>EelTootlus.morfAnalyysiJaTuvastaArvsonafraasid</code> - teksti morf analyys ja heuristiline tekstianalyys 
 *          (arvsonafraaside tuvastamine, lausel6ppude kindlaksm22ramine ja muu vajalik).</li>
 *	    <li><code>eraldaAjavaljendiKandidaadid</code> - eraldab etteantud tekstis ajavaljendid. Maaratleb ajavaljendite
 *          esialgsed fraasipiirid + tuvastab ajavaljendiga seotud vaiksemad semantilised jupid. Ajavaljendi taielik
 *          lahendamine tuleb hiljem. Eraldamine toimub tuvastamisreeglite alusel.</li>
 *      <li><code>lahendaYlekattedAjavaljenditeVahelJaRakendaNegMustreid</code> - kui suurem ajavaljend katab taielikult yle
 *          vaiksema, siis vaiksem eemaldatakse. Samuti eemaldatakse ajavaljendid, mille negatiivsed mustrid leiavad 
 *          rahuldamist;</li>
 *      <li><code>liidaKorvutiseisvadAjavaljendiFraasid2</code> - liidab (voimalusel) korvutiseisvad eraldatud ajavaljendid
 *          yheks valjendiks (nt <i>kell 12 + hommikul, 1999 + juunis</i>); Kui mitte-eraldiseisev kandidaat ei l2bi
 *          liitumisprotsessi, eemaldatakse see yldse (nt eemaldatakse <i>umbes</i>, kui sellele ei j2rgne nt kandidaat
 *          <i>kell pool kolm</i> ). Samuti eemaldatakse liitmisel saadud pikema kandidaadi poolt t2ielikult ylekaetud
 *          kandidaadid; Liitmine toimub liitumisreeglite alusel;</li>
 *      <li><code>eraldaAjavahemikudJaLiidaFraasiks</code> - leiab, millised korvutiseisvaist ajavaljendeist voivad
 *          moodustada semantika poolest ajavahemiku ning liidab need kokku yheks semantiliseks ajavaljendiks;
 *          Kasutab sisseehitatud heuristikuid ajavahemike leidmiseks; </li>               
 *      <li><code>mudel.leiaSemantika</code> - lahendab ajavaljendite semantika (etteantud lahendusmudeli alusel); </li>
 *      <li><code>JarelTootlus.joondaAlgseTekstiga</code> - joondab saadud tulemused algse tekstiga; </li>
 *      <li><code>JarelTootlus.eraldamiseTulemusPretty</code> - kuvab l6pliku tulemuse (m2rgendatud tekst); </li>
 *  </ol>
 * 
 *   @author Siim Orasmaa
 *   
 */
public class AjaTuvastaja {

	final private static String versioon = "2016-10-14_01";
	
	private LogiPidaja logi;

	//==============================================================================
	//   	R e e g l i f a i l 
	//==============================================================================
	
	private String reegliFail = "reeglid.xml";
	
	//==============================================================================
	//   	S o n a k l a s s i d    j a   t u v a s t a m i s r e e g l i d 
	//==============================================================================

	private HashMap<String, SonaKlass> sonaKlassid = null;
	private List<TuvastamisReegel>         reeglid = null;
	private List<LiitumisReegel>   liitumisReeglid = null;
	private SemLeidmiseMudel                 mudel = new SemLeidmiseMudelImpl3();
	
	//==============================================================================
	//   	                 e s t y h m m - w r a p p e r 
	//==============================================================================
	
	private EstyhmmWrapper wrapper;

	//==============================================================================
	//==============================================================================
	//    T u v a s t a m i n e  ( D e b u g :  A i n u l t   f r a a s i d 
	//    + k i i r u s e   d e b u g )
	//==============================================================================
	//==============================================================================
	
	/**
	 *   Tuvastab etteantud sisendtekstist ajavaljendid. Kasutab relatiivsete ajavaljendite
	 *  leidmisel etteantud referentsaega (<tt>konehetk</tt>). Tegemist on versiooniga, mis
	 *  tagastab listi leitud ajavaljendifraasidest koos nende m2rgendusetega.
	 *  <p>
	 *  Kui eksisteerib <code>morfAnalyysitudSisendTekst</code>, eeldatakse, et tegemist on 
	 *  <code>sisendTekst</code>-i morf analyysi tulemusega ja kasutatakse esimest ka 
	 *  eeltootluses morf analyysi asemel. NB! <code>sisendTekst</code> on isegi sellisel
	 *  juhul vajalik.
	 *  <p>
	 *  Lipp <tt>allowOnlyPureTimeML</tt> maarab, kas valjundisse joudev margendus peab rangelt
	 *  vastama TimeML skeemile. NB! Skeemi rahuldamiseks eemaldatakse m6ned ajavaljendid ning
	 *  kustutatakse m6ned atribuudid, mist6ttu ei pruugi valjund olla enam terviklik. Tervikliku
	 *  valjundi saamiseks tuleks lubada korvalkaldeid skeemist ... 
	 *  <p> 
	 *  Tegemist on DEBUG-versiooniga: kaks viimast elementi tagastatavas listis on m66detud
	 *  ajad: eelviimane on eeltootluse aeg millisekundites ning viimane on ajavt aeg millisekundites;
	 * @throws Exception 
	 */
	public List<String> tuvastaAjavaljendidTekstisDebugSpeed(String [] konehetk, 
                                                             String sisendTekst,
                                              String morfAnalyysitudSisendTekst,
                                                     boolean allowOnlyPureTimeML) throws Exception{
		long preprocessingTime = 0L;
		long ajavtTime         = 0L;
		// --------------------------------------------------------------------
		//   *) Laeme failist ajavaljendite tuvastamise reeglid
		// --------------------------------------------------------------------
		if (this.sonaKlassid == null || this.reeglid == null){
			this.uuendaReegleid(reegliFail, false);
		}
		
		// --------------------------------------------------------------------
		//   *) Sooritame estyhmm abil sisendteksti morf analyysi (vajadusel)
		//   *) Paigutame morf analyysi tulemused andmemudelisse
		// --------------------------------------------------------------------
		List<AjavtSona> sonad = null;
		if (morfAnalyysitudSisendTekst == null){
			if (this.wrapper == null){
				this.wrapper = new EstyhmmWrapperImpl( "t3mesta -Y -cio utf8 +1", "UTF-8" );
			}
			try {
				long startTime = System.currentTimeMillis();
				sonad = EelTootlus.eeltootlusValiseMorfAnalyysiga( this.wrapper, sisendTekst );
				long endTime   = System.currentTimeMillis();
				preprocessingTime = endTime - startTime;
		 	} catch (Exception e) {
		 		throw e;
		 	}	
		} else {
			long startTime = System.currentTimeMillis();
			sonad = EelTootlus.eeltootlus( morfAnalyysitudSisendTekst );
			long endTime   = System.currentTimeMillis();
			preprocessingTime = endTime - startTime;
		}
	 	
	 	long startTime = System.currentTimeMillis();
		// --------------------------------------------------------------------
		//   *) Konfigureerime semantika lahendamise mudeli
		//     (st, milliseid semantikadefinitsioone eraldamisel ja
		//      normaliseerimisel arvestatakse ja milliseid mitte)
		// --------------------------------------------------------------------
		if (this.mudel != null){
			TuvastamisReegel.setSemanticsModel( (this.mudel).getMudeliTahised() );
		} else {
			throw new Exception("Arvutusmudel maaramata!");
		}
	 	
		// --------------------------------------------------------------------
		//   *) Asume ajavaljendeid tuvastama
		// --------------------------------------------------------------------
		this.eraldaAjavaljendiKandidaadid(sonad);
		this.lahendaYlekattedAjavaljenditeVahelJaRakendaNegMustreid(sonad);		
		//this.liidaKorvutiseisvadAjavaljendiFraasid(sonad);
		this.liidaKorvutiseisvadAjavaljendiFraasid2(sonad);
		this.eraldaAjavahemikudJaLiidaFraasiks(sonad);
		
		//this.koondaRinnastusSeosesOlevadAjavYhtseSemantikaAlla(sonad);
		(this.mudel).leiaSemantika(sonad, konehetk);

		// --------------------------------------------------------------------
		//   *) Joondame tulemuse esialgse tekstiga
		// --------------------------------------------------------------------	 		
 		try {
			JarelTootlus.joondaAlgseTekstiga( sonad, sisendTekst );
		} catch (Exception e) {
			throw e;
		}
		
		JarelTootlus.parandaTIDvaartused( sonad, allowOnlyPureTimeML );
		
		// --------------------------------------------------------------------
		//   *) Tagastame tulemuse
		// --------------------------------------------------------------------
		List<String> returnable = JarelTootlus.eraldamiseTulemusAinultValjendid(
				sonad, sisendTekst, allowOnlyPureTimeML, JarelTootlus.formatAsCreationTime(konehetk)); 
		long endTime = System.currentTimeMillis();
		ajavtTime = endTime - startTime;
		returnable.add(String.valueOf(preprocessingTime));
		returnable.add(String.valueOf(ajavtTime));
		return returnable;
	}
	
	//==============================================================================
	//==============================================================================
	//   T u v a s t a m i n e  ( T a v a l i n e   t e k s t   s i s e n d i k s )
	//==============================================================================
	//==============================================================================
	
	/**
	 *   Tuvastab etteantud sisendtekstist ajavaljendid. Kasutab relatiivsete ajavaljendite
	 *  leidmisel etteantud referentsaega (<tt>konehetk</tt>). Tulemusena tagastab j2rjendi
	 *  AjavtSona-dest, ylesleitud ajav2ljendid on seotud vastavate s6nadega.
	 *  <p>
	 *  Kui eksisteerib <code>morfAnalyysitudSisendTekst</code>, eeldatakse, et tegemist on 
	 *  <code>sisendTekst</code>-i morf analyysi tulemusega ja kasutatakse esimest ka 
	 *  eeltootluses morf analyysi asemel. NB! <code>sisendTekst</code> on isegi sellisel
	 *  juhul vajalik. 
	 *  <p>
	 *  Lipp <tt>allowOnlyPureTimeML</tt> maarab, kas valjundisse joudev margendus peab rangelt
	 *  vastama TimeML skeemile. NB! Skeemi rahuldamiseks eemaldatakse m6ned ajavaljendid ning
	 *  kustutatakse m6ned atribuudid, mist6ttu ei pruugi valjund olla enam terviklik. Tervikliku
	 *  valjundi saamiseks tuleks lubada korvalkaldeid skeemist ... 
	 *   <p>
	 *  @throws Exception 
	 */
	public List<AjavtSona> tuvastaAjavaljendidTekstis(String [] konehetk, 
                                                      String sisendTekst,
                                       String morfAnalyysitudSisendTekst,
                                             boolean allowOnlyPureTimeML) throws Exception {
		// --------------------------------------------------------------------
		//   *) Laeme failist ajavaljendite tuvastamise reeglid
		// --------------------------------------------------------------------
		if (this.sonaKlassid == null || this.reeglid == null){
			this.uuendaReegleid(reegliFail, false);
		}
		
		// --------------------------------------------------------------------
		//   *) Sooritame estyhmm abil sisendteksti morf analyysi (vajadusel)
		//   *) Paigutame morf analyysi tulemused andmemudelisse
		// --------------------------------------------------------------------
		List<AjavtSona> sonad = null;
		if (morfAnalyysitudSisendTekst == null){
			if (this.wrapper == null){
				this.wrapper = new EstyhmmWrapperImpl( "t3mesta -Y -cio utf8 +1", "UTF-8" );
			}
			sonad = EelTootlus.eeltootlusValiseMorfAnalyysiga( this.wrapper, sisendTekst );
		} else {
			sonad = EelTootlus.eeltootlus( morfAnalyysitudSisendTekst );
		}
	 	
		// --------------------------------------------------------------------
		//   *) Konfigureerime semantika lahendamise mudeli
		//     (st, milliseid semantikadefinitsioone eraldamisel ja
		//      normaliseerimisel arvestatakse ja milliseid mitte)
		// --------------------------------------------------------------------
		if (this.mudel != null){
			TuvastamisReegel.setSemanticsModel( (this.mudel).getMudeliTahised() );
		} else {
			throw new Exception("Arvutusmudel maaramata!");
		}
		
		// --------------------------------------------------------------------
		//   *) Asume ajavaljendeid tuvastama
		// --------------------------------------------------------------------
		this.eraldaAjavaljendiKandidaadid(sonad);
		this.lahendaYlekattedAjavaljenditeVahelJaRakendaNegMustreid(sonad);		
		//this.liidaKorvutiseisvadAjavaljendiFraasid(sonad);
		this.liidaKorvutiseisvadAjavaljendiFraasid2(sonad);
		this.eraldaAjavahemikudJaLiidaFraasiks(sonad);
		
		(this.mudel).leiaSemantika(sonad, konehetk);

		// --------------------------------------------------------------------
		//   *) Joondame tulemuse esialgse tekstiga
		// --------------------------------------------------------------------	 		
		JarelTootlus.joondaAlgseTekstiga( sonad, sisendTekst );
		JarelTootlus.parandaTIDvaartused( sonad, allowOnlyPureTimeML );
		
		// --------------------------------------------------------------------
		//   *) Tagastame tulemuse (AjavtSona listi) 
		// --------------------------------------------------------------------
		return sonad;
	}
	
	/**
	 *   Tuvastab etteantud sisendtekstist ajavaljendid. Kasutab relatiivsete ajavaljendite
	 *  leidmisel etteantud referentsaega (<tt>konehetk</tt>). Tegemist on versiooniga, mis
	 *  tagastab tulemusena eraldatud ajav2ljendite listi, kus iga eraldatud ajav&auml;ljendi 
	 *  esitatud paisktabeli kujul.
	 *  <p>
	 *  Kui eksisteerib <code>morfAnalyysitudSisendTekst</code>, eeldatakse, et tegemist on 
	 *  <code>sisendTekst</code>-i morf analyysi tulemusega ja kasutatakse esimest ka 
	 *  eeltootluses morf analyysi asemel. NB! <code>sisendTekst</code> on isegi sellisel
	 *  juhul vajalik.
	 *  <p> 
	 *  Tagastatava paisktabeli v6tmed vastavad TIMEX3 tag'i atribuudinimedele (nt <i>value</i>, 
	 *  <i>mod</i>, <i>type</i> jne), lisaks on kasutusel kolm spetsiifilist atribuuti:
	 *   <ul>
	 *   <li> <i>startPosition</i> (ajav alguspositsioon tekstis);
	 *   <li> <i>endPosition</i> (ajav l6pppositsioon tekstis);
	 *   <li> <i>text</i> (eraldatud ajav2ljendifraas);
	 *   </ul>
	 *  <p>
	 *  Lipp <tt>allowOnlyPureTimeML</tt> maarab, kas valjundisse joudev margendus peab rangelt
	 *  vastama TimeML skeemile. NB! Skeemi rahuldamiseks eemaldatakse m6ned ajavaljendid ning
	 *  kustutatakse m6ned atribuudid, mist6ttu ei pruugi valjund olla enam terviklik. Tervikliku
	 *  valjundi saamiseks tuleks lubada korvalkaldeid skeemist ... 
	 *   <p>
	 *   NB! Mone atribuudi vaartuseks voib olla ka <code>null</code>, sellised tuleks loplikust 
	 *   valjundist valja jatta.   
	 *  <p>
	 *   Etteantud konehetk valjastatakse samuti (ilma tekstilise sisuta) margendina, mille
	 *   ID (atribuut <code>tid</code>) on alati <code>t0</code>.
	 *  <p>

	 * @throws Exception 
	 */
	public List<HashMap<String, String>> tuvastaAjavaljendidTekstisTulemusPaiskTabelitena
								(String [] konehetk, 
								 String sisendTekst, 
								 String morfAnalyysitudSisendTekst,
								 boolean allowOnlyPureTimeML) throws Exception {
		List<AjavtSona> sonad = this.tuvastaAjavaljendidTekstis(konehetk, sisendTekst, morfAnalyysitudSisendTekst, allowOnlyPureTimeML);
		return JarelTootlus.eraldamiseTulemusAinultValjendidPaistabelitena(
				sonad, sisendTekst, allowOnlyPureTimeML, JarelTootlus.formatAsCreationTime(konehetk));
	}
	
	//==============================================================================
	//==============================================================================
	//    T u v a s t a m i n e  ( t 3 - o l p )
	//==============================================================================
	//==============================================================================
	
	/**
	 *   Tuvastab etteantud t3-olp sisust (tekst, mis on läbinud morf. analüsaatori 
	 *   ja ühestaja ning sisaldab osalausete piire) ajaväljendid. Kasutab relatiivsete 
	 *   ajavaljendite leidmisel etteantud referentsaega (<tt>konehetk</tt>). 
	 *   Tulemusena tagastab j2rjendi AjavtSona-dest, ylesleitud ajav2ljendid on seotud
	 *   vastavate s6nadega.
	 *   <p>
	 */
	public List<AjavtSona> tuvastaAjavaljendidT3OLP(String [] konehetk, 
			                                        String sisendT3OLP,
			                               boolean allowOnlyPureTimeML,
	                                                     boolean debug) throws Exception {
		// --------------------------------------------------------------------
		//   *) Laeme failist ajavaljendite tuvastamise reeglid
		// --------------------------------------------------------------------
		if (this.sonaKlassid == null || this.reeglid == null){
			this.uuendaReegleid(reegliFail, true);
		} else {
			if (this.logi != null){
				kuvaSonaKlassidJaReeglid(this.logi);
			}
		}
		
		if (debug){
			LogiPidaja logi = new LogiPidaja(true);
			logi.setKirjutaLogiValjundisse(true);
			logi.setKirjutaLogiFaili(false);
			SemantikaDefinitsioon.logi = logi;
		}
		
		// --------------------------------------------------------------------
		//   *) Eeldame, et morf analyys on juba kenasti olemas
		//   *) Paigutame morf analyysi tulemused andmemudelisse
		// --------------------------------------------------------------------
		List<AjavtSona> sonad = null;
		sonad = EelTootlus.eeltootlusT3OLP( sisendT3OLP );

		// --------------------------------------------------------------------
		//   *) Konfigureerime semantika lahendamise mudeli
		//     (st, milliseid semantikadefinitsioone eraldamisel ja
		//      normaliseerimisel arvestatakse ja milliseid mitte)
		// --------------------------------------------------------------------
		if (this.mudel != null){
			TuvastamisReegel.setSemanticsModel( (this.mudel).getMudeliTahised() );
		} else {
			throw new Exception("Arvutusmudel maaramata!");
		}
		
		// --------------------------------------------------------------------
		//   *) Asume ajavaljendeid tuvastama
		// --------------------------------------------------------------------

		this.eraldaAjavaljendiKandidaadid(sonad);
		this.lahendaYlekattedAjavaljenditeVahelJaRakendaNegMustreid(sonad);		
		//this.liidaKorvutiseisvadAjavaljendiFraasid(sonad);
		this.liidaKorvutiseisvadAjavaljendiFraasid2(sonad);
		this.eraldaAjavahemikudJaLiidaFraasiks(sonad);
		
		(this.mudel).leiaSemantika(sonad, konehetk);
		
		// --------------------------------------------------------------------
		//   *) Joondame tulemuse esialgse tekstiga
		// --------------------------------------------------------------------	 		
		JarelTootlus.parandaTIDvaartused( sonad, allowOnlyPureTimeML );
		
		// --------------------------------------------------------------------
		//   *) Tagastame tulemuse
		// --------------------------------------------------------------------
		return sonad;
	}

	/**
	 *   Tuvastab etteantud t3-olp sisust (tekst, mis on läbinud morf. analüsaatori 
	 *   ja ühestaja ning sisaldab osalausete piire) ajaväljendid. Kasutab relatiivsete 
	 *   ajavaljendite leidmisel etteantud referentsaega (<tt>konehetk</tt>). Tegemist 
	 *   on versiooniga, mis tagastab tulemusena eraldatud ajav2ljendite listi, kus 
	 *   iga eraldatud ajav&auml;ljendi esitatud paisktabeli kujul.
	 *   <p> 
	 *   vt t2psemalt: {@link JarelTootlus.eraldamiseTulemusAinultValjendidPaistabelitena}
	 */
	public List<HashMap<String, String>> tuvastaAjavaljendidT3OLPTulemusPaiskTabelitena(String [] konehetk, 
                                                                                        String sisendT3OLP,
                                                                               boolean allowOnlyPureTimeML,
                                                                                             boolean debug) throws Exception {
		List<AjavtSona> sonad = this.tuvastaAjavaljendidT3OLP(konehetk, sisendT3OLP, allowOnlyPureTimeML, debug);
		return JarelTootlus.eraldamiseTulemusAinultValjendidPaistabelitena(
				sonad, null, allowOnlyPureTimeML, JarelTootlus.formatAsCreationTime(konehetk));
	}

	//==============================================================================
	//==============================================================================
	//    T u v a s t a m i n e  ( v a b a m o r f i - J S O N )
	//==============================================================================
	//==============================================================================
	/**
	 *   Tuvastab etteantud vabamorfi JSON sisust (tekst, mis on läbinud morf. analüsaatori 
	 *   ja ühestaja) ajaväljendid. Kasutab relatiivsete ajavaljendite leidmisel etteantud 
	 *   referentsaega (<tt>konehetk</tt>).
	 */
	public List<AjavtSona> tuvastaAjavaljendidVabamorfJSON(String [] konehetk, 
			                                                String sisendJSON,
			                                      boolean allowOnlyPureTimeML,
	                                                            boolean debug) throws Exception {
		// --------------------------------------------------------------------
		//   *) Laeme failist ajavaljendite tuvastamise reeglid
		// --------------------------------------------------------------------
		if (this.sonaKlassid == null || this.reeglid == null){
			this.uuendaReegleid(reegliFail, true);
		} else {
			if (this.logi != null){
				kuvaSonaKlassidJaReeglid(this.logi);
			}
		}
		
		if (debug){
			LogiPidaja logi = new LogiPidaja(true);
			logi.setKirjutaLogiValjundisse(true);
			logi.setKirjutaLogiFaili(false);
			SemantikaDefinitsioon.logi = logi;
		}
		
		// --------------------------------------------------------------------
		//   *) Eeldame, et morf analyys on juba kenasti olemas
		//   *) Paigutame morf analyysi tulemused andmemudelisse
		// --------------------------------------------------------------------
		List<AjavtSona> sonad = null;
		sonad = EelTootlus.eeltootlusJSON( sisendJSON );
		
		// --------------------------------------------------------------------
		//   *) Konfigureerime semantika lahendamise mudeli
		//     (st, milliseid semantikadefinitsioone eraldamisel ja
		//      normaliseerimisel arvestatakse ja milliseid mitte)
		// --------------------------------------------------------------------
		if (this.mudel != null){
			TuvastamisReegel.setSemanticsModel( (this.mudel).getMudeliTahised() );
		} else {
			throw new Exception("Arvutusmudel maaramata!");
		}
		
		// --------------------------------------------------------------------
		//   *) Asume ajavaljendeid tuvastama
		// --------------------------------------------------------------------

		this.eraldaAjavaljendiKandidaadid(sonad);
		this.lahendaYlekattedAjavaljenditeVahelJaRakendaNegMustreid(sonad);		
		//this.liidaKorvutiseisvadAjavaljendiFraasid(sonad);
		this.liidaKorvutiseisvadAjavaljendiFraasid2(sonad);
		this.eraldaAjavahemikudJaLiidaFraasiks(sonad);
		
		(this.mudel).leiaSemantika(sonad, konehetk);
		
		// --------------------------------------------------------------------
		//   *) Joondame tulemuse esialgse tekstiga
		// --------------------------------------------------------------------	 		
 		JarelTootlus.parandaTIDvaartused( sonad, allowOnlyPureTimeML );
		
		// --------------------------------------------------------------------
		//   *) Tagastame tulemuse
		// --------------------------------------------------------------------
 		return sonad;
	}
	
	/**
	 *  M2hismeetod ehk wrapper meetodi <tt>tuvastaAjavaljendidVabamorfJSON</tt> lihtsamaks
	 *  v2ljakutsumiseks PyVabamorfi abil; N6uab ainult s6ne kujul JSON sisendit ning v2ljastab
	 *  tulemused samuti ainult JSON s6ne kujul, peites keerukamad andmestruktuurid;  
	 */
	public String tuvastaAjavaljendidPyVabamorfJSON(String konehetkStr, 
            							  		    String sisendJSON,
            									    boolean allowOnlyPureTimeML,
            									    boolean debug) throws Exception {
		String konehetk [] = Main.prooviLuuaJSONsisendiP6hjalRefAeg(sisendJSON);
		if (konehetk == null){
			konehetk = Main.looSonePohjalReferentsAeg(konehetkStr);
		}
		if (konehetk == null){
			konehetk = Main.looSonePohjalReferentsAeg(null);
		}
		List<AjavtSona> tulemAjavtSonad = 
				this.tuvastaAjavaljendidVabamorfJSON(konehetk, sisendJSON, allowOnlyPureTimeML, debug);
		return JarelTootlus.eraldamiseTulemusVabaMorfiJSON(sisendJSON, tulemAjavtSonad, 
			   JarelTootlus.formatAsCreationTime(konehetk), allowOnlyPureTimeML, false);
	}
	
	//==============================================================================	
	//==============================================================================
	//   	A l a m e t a p i d
	//==============================================================================
	//==============================================================================	

	/**
	 *  1.1) Reeglite uuendamine XML-failist.
	 */
	public void uuendaReegleid(String reegliFailiNimi, boolean kuvaDebug) throws ParserConfigurationException, SAXException, IOException{
		MustridXMLFailist mustriLugeja = new MustridXMLFailist();
		// 1) Parsime XML-reeglifailist sonaklassid ja reeglid
		mustriLugeja.votaMustridXMLFailist(reegliFailiNimi, this);
		
		// 2) Debug: kuvame sisu
		if (kuvaDebug) { kuvaSonaKlassidJaReeglid(this.logi); }
	}

	/**
	 *  1.2) Reeglite uuendamine sisendvoost.
	 * 
	 * @param reegliFailiNimi
	 */
	public void uuendaReegleidFromInputStream(InputStream in, boolean kuvaDebug) throws ParserConfigurationException, SAXException, IOException{
		if (in != null) {
			MustridXMLFailist mustriLugeja = new MustridXMLFailist();
			// 1) Parsime XML-reeglifailist sonaklassid ja reeglid
			mustriLugeja.votaMustridXMLSisendvoost(in, this);
			
			// 2) Debug: kuvame sisu
			if (kuvaDebug) { kuvaSonaKlassidJaReeglid(this.logi); }
		}
	}
	
	/**
	 *  2) Eraldab etteantud tekstist (<code>sonad</code>) ajavaljendikandidaadid. 
	 *  Lisaks eraldamisele (st ajavaljendi piiri maaramisele) pannakse kandidaatidele
	 *  kaasa ka nii palju semantikat, kui eraldamisel on 6nnestunud fraasist parsida. 
	 *  See leiab kasutust hiljem, ajavaljendikandidaadi lahendamisel.
	 *   <p>
	 *   Loob nn MUSTER-tyypi ajavaljendikandidaadid (ASTE == MUSTRI_POOLT_ERALDATUD).
	 */
	private void eraldaAjavaljendiKandidaadid(List<AjavtSona> sonad) throws Exception {
		// ----------------------------------------------------------------
		//   Kaime kogu teksti sona-sonahaaval labi: leiame arvsonafraasid
		//  ning margistame esialgsed ajavaljendifraasid...
		// ----------------------------------------------------------------
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			// Sonaklassid, mille sobivust antud sonaga on juba kontrollitud (esimene kuni viimane s6na)
			HashMap<String, MallileVastavus> kontrollitudSonaKlassid = new HashMap<String, MallileVastavus>(sonaKlassid.size());
			
			// Kas oleme j6udnud viimase s6nani tekstis?
			boolean onViimaneSona = (i == sonad.size() - 1);
			// Sonaklassid, mille sobivust antud sonaga on juba kontrollitud (teksti/lause l6pp, dummy s6na)			
			HashMap<String, MallileVastavus> kontrollitudDummySonaKlassid = 
				(onViimaneSona || sona.onLauseLopp()) ? (new HashMap<String, MallileVastavus>(sonaKlassid.size())) : (null);
				
			// ------------------------------------------------------------
			//  Kontrollime sona sobimist k6igi reeglite fraasimustritesse
			// ------------------------------------------------------------
			for (TuvastamisReegel reegel : reeglid) {
				(reegel.getFraasiMuster()).kontrolliMustrileVastavust(sona, kontrollitudSonaKlassid);
				// ---------------------------------------------------------------------
				//  Kui on tegu viimase s6naga tekstis v6i lausel6puga, sulgeme 
				//  poolelioleva eraldamise
				// ---------------------------------------------------------------------
				if (onViimaneSona || sona.onLauseLopp()){
					AjavtSona dummyWord = new AjavtSona("*");
					(reegel.getFraasiMuster()).kontrolliMustrileVastavust( dummyWord, kontrollitudDummySonaKlassid );
				}
			}
		}
	}
	
	/**
	 *  3.1) Lahendab eraldatud ajav&auml;ljendikandidaatide vahel esinevad ylekatted. 
	 *  Loogika on lihtne: iga ajav2ljendikandidaat, mis t2ielikult ylekaetud m6ne 
	 *  teise ajav2ljendikandidaadi poolt, eemaldatakse. Osalised ylekatted on 
	 *  problemaatilised, need j22vad alles.
	 *  <p>
	 *  Eemaldab t2ielikult ylekaetud nn MUSTER-tyypi ajavaljendikandidaadid.
	 *  <p>
	 *  3.2) Rakendab negatiivseid mustreid, eemaldades ajavaljendid, mis on osaliselt
	 *  v6i t2ielikult negatiivsete mustrite all.
	 *  <p>
	 */
	private void lahendaYlekattedAjavaljenditeVahelJaRakendaNegMustreid(List<AjavtSona> sonad){
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			// 1) Eemaldame ylekattuvad ajavaljendikandidaadid
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				sona.eemaldaYlekattuvadAjavaljendiKandidaadid();
			}
			// 2) Kui on veel midagi alles: proovime negatiivsete mustrite rakendamist			
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<AjavaljendiKandidaat> ajavaljendiKandidaadid 
						= sona.getAjavaljendiKandidaadid();
				List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides 
						= sona.getAjavaljendiKandidaatides();
				int j = 0;
				while (j < ajavaljendiKandidaadid.size()){
					if ( (ajavaljendiKandidaatides.get(j)).onFraasiAlgus() ){
						AjavaljendiKandidaat ajavaljendiKandidaat = ajavaljendiKandidaadid.get(j);
						if (ajavaljendiKandidaat.getTuvastamisReeglid() != null){
							for (TuvastamisReegel tuvastamisReegel : ajavaljendiKandidaat.getTuvastamisReeglid()) {
								if (tuvastamisReegel.getNegMustrid() != null){
									for (NegatiivneMuster negatiivneMuster : tuvastamisReegel.getNegMustrid()) {
										negatiivneMuster.
											kontrolliNegatiivsetMustritJaVajaduselEemaldaAjav(sonad, i, ajavaljendiKandidaat);
									}
								}
							}
						}
					}
					j++;
				}
			}
		}
	}

	//==============================================================================
	//    K o r v u t i p a i k n e v a t e   f r a a s i d e   l i i t m i n e     
	//==============================================================================
	
	/**
	 *  5) (UUS-LOOGIKA) Liidab korvutiseisvad ajavaljendikandidaadid liitmisreeglite alusel 
	 *  fraasideks. Tehnilisemalt: yhendab k6rvutiseisvad, MUSTER-tyypi, kandidaadid 
	 *  FRAAS-tyypi kandidaatideks.
	 *  <p>
	 *  Eemaldab ajavaljendikandidaadid, mis on margitud mitte-eraldiseisvaks ning
	 *  mille k6rval ei leidu yhtki teist & eraldiseisvat ajavaljendikandidaati. Samuti 
	 *  eemaldab yksikud (FRAAS-iks mitteyhendatud) kandidaadid, mis j22vad t2ielikult mingi
	 *  FRAAS-tyypi kandidaadi sisse;
	 *  <p>
	 */
	private void liidaKorvutiseisvadAjavaljendiFraasid2(List<AjavtSona> sonad) throws Exception {
		List<PotentsLiidetavateKandidaatideJada> jadad = null;
		if (this.liitumisReeglid != null){
			// 1) Genereerime k6ikv6imalikud potentsiaalsed jadad
			jadad =	PotentsLiidetavateKandidaatideJada.genereeriPotentsJadad( sonad );
			// 2) Filtreerime jadasid liitumisreeglite j2rgi (j2tame alles vaid reeglite j2rgi fraasiks liituvad kandidaadid)
			if (!jadad.isEmpty()){
				PotentsLiidetavateKandidaatideJada.filtreeriPotentsJadadsidLiitumisReegliteJargi(jadad, this.liitumisReeglid);					
			}
			// 3) Filtreerime jadasid: hoiame 2ra vahemike liitmise;
			if (!jadad.isEmpty()){
				PotentsLiidetavateKandidaatideJada.filtreeriPotentsJadadsidStNiJargi(jadad);
			}
			// 4) Filtreerime jadasid ylekattuvuste j2rgi (kustutame jadad, mis on pikemate poolt t2ielikult ylekaetud)
			if (!jadad.isEmpty()){
				PotentsLiidetavateKandidaatideJada.filtreeriPotentsJadadsidYleKattuvuseJargi(jadad);					
			}
			// 5) Moodustame sobivate jadade p6hjal uued ajav2ljendikandidaadid
			if (!jadad.isEmpty()){
				for (PotentsLiidetavateKandidaatideJada jada : jadad) {
					AjavaljendiKandidaat ajav = new AjavaljendiKandidaat();
					ajav.setAste(ASTE.YHENDATUD_FRAASINA);
					ajav.seoKylgeAlamkandidaadid( jada.getKandidaadid() );
					AjaTuvastaja.seoSonadKandidaadiKylge( ajav, jada.getAjavtSonad() );
				}
			}
		}
		// 6) Kustutame: A. mitteeraldiseisvad kandidaadid ning
		//               B. kandidaadid, mis kaetakse jadade poolt t2ielikult yle;
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
				List<AjavaljendiKandidaat> kandidaadid = sona.getAjavaljendiKandidaadid();
				List<AjavaljendiKandidaat> toDelete = new ArrayList<AjavaljendiKandidaat>();
				for (AjavaljendiKandidaat kandidaat : kandidaadid) {
					//  Kontrollime, kas tegu on mitte-eraldiseisva kandidaadiga
					//  (nt "kohaliku aja järgi"), mis saab ajav2ljendi moodustada vaid
					//  pikema fraasi koosseisus, mitte yksinda
					if (kandidaat.leiaK6igeK6rgemYlemkandidaat() == kandidaat &&
						kandidaat.isPoleEraldiseisevAjavaljend()){
						toDelete.add( kandidaat );
					} else if (kandidaat.leiaK6igeK6rgemYlemkandidaat() == kandidaat && 
								jadad != null && !jadad.isEmpty()){
						// Kontrollime, kas m6ni jada neelab antud yksiku kandidaadi 
						// t2ielikult, st on sellest pikem ning h6lmab selle t2ielikult;
						//  Nt DATE '1934 aasta sügisel' sisse j22b DURATION '1934 aasta';
						for (PotentsLiidetavateKandidaatideJada jada : jadad) {
							if (jada.kasJadaNeelabAjavaljendiKandidaadi(kandidaat)){
								toDelete.add( kandidaat );
								break;
							}
						}
					}
				}
				if (!toDelete.isEmpty()){
					for (AjavaljendiKandidaat kandidaat : toDelete) {
						kandidaat.eemaldaEnnastSonadeKyljest();
					}
				}
			}
		}
		if (jadad != null && !jadad.isEmpty()){
			jadad.clear();
		}
	}
	
	//==============================================================================
	//   	V a h e m i k e   l e i d m i s e   h e u r i s t i k
	//==============================================================================
	
	private static Pattern musterNumbrikombinatsioon = Pattern.compile("^(\\d+)(\\d|[.,!?]|-)+(-(st|ks|ni|nda)+)?$", Pattern.CASE_INSENSITIVE);
	private static Pattern musterKuniSonaVoiMark     = Pattern.compile("^(kuni|KUNI|-)$");
	
	private static int ON_NUMBRIKOMBINATSIOON 			 = 1;
	private static int ON_ARVSONAFRAAS		  			 = 2;
	private static int ON_LEITUD_AJAVALJEND	  			 = 3;
	private static int ON_AJAVALJEND_NUMBRIKOMBINATSIOON = 4;
	
	/**
	 * Abiklass, mis esindab muudetavat t&auml;isarvu.
	 *  
	 * @author Siim Orasmaa
	 */
	class MutableInteger {
		
		private int integer = Integer.MIN_VALUE;
		
		MutableInteger(int integer){
			this.integer = integer;
		}

		public int getInteger() {
			return integer;
		}

		public void setInteger(int integer) {
			this.integer = integer;
		}
		
	};
	
	/**
	 *    6) Leiab tekstist ajavaljendikandidaadid ja numbrid/arvsonad, mis moodustavad 
	 *   k6rvutiseistes ajavahemiku, ning seob need yhtse semantikaga fraasiks. 
	 *   NB! Eeldab, et ajav2ljendite fraasideks liitmine on juba l2bi viidud;
	 *  <p>
	 *   Yhendab "vahemikuna-klassifitseeruvad", MUSTER-tyypi v6i FRAAS-tyypi kandidaadid, 
	 *   VAHEMIK-tyypi kandidaatide alla.
	 *  <p>
	 *   Tegevusloogika lyhikirjeldus:
	 *   <ul>
	 *     <li> Vahemike otsimise p&auml;&auml;stab valla (ajavaljendikandidaati kuuluva) seestytlevas k&auml;&auml;ndes 
	 *          s&otilde;na, s&otilde;na "kuni" v&otilde;i sidekriipsu esinemine tekstis;
	 *     <li> Kuna eelnevalt leiti vahemik-fraasi keskosa (heuristiliselt eeldame, et oli keskosa - alati see ei pea paika),
	 *          siis j2rgmisena leiame l6puosa; L6puosa peab olema numbrikombinatsioon, arvs6nafraas v6i ajavaljend;
	 *     <li> Kui l6puosa on edukalt leitud, leiame algusosa, mis peab olema samuti yks kolmest: numbrikombinatsioon,
	 *          arvs6nafraas v6i ajavaljend;
	 *     <li> Kui l6puosa on edukalt leitud, leiame algusosa, mis peab olema samuti yks kolmest: numbrikombinatsioon,
	 *          arvs6nafraas v6i ajavaljend;
	 *     <li> Kontrollime, et v2hemalt yks kahest leitud poolest oleks ajav2ljendikandidaat: kui pole, katkestame;
	 *   </ul>   
	 */
	private void eraldaAjavahemikudJaLiidaFraasiks(List<AjavtSona> sonad){
		// Millistelt positsioonidelt oleme juba vahemikud eraldanud
		HashMap<String, String> addedFromLocation = new HashMap<String, String>();
		// Murdepunkt fraasis
		int murdePunkt = -1;
		boolean murdePunktOnInklusiivne = false;
		for (int i = 0; i < sonad.size(); i++) {
			if (addedFromLocation.containsKey(String.valueOf(i))){
				continue;
			}
			AjavtSona sona = sonad.get(i);
			// Kas tegemist on s6naga "kuni" v6i kriipsuga ("-")?
			boolean onKuniSonanaVoiKriips = musterKuniSonaVoiMark.matcher( sona.getAlgSonaErisymbolidNormaliseeritud() ).matches();
			if ((sona.isOnPotentsiaalneVahemikuAlgus() && !sona.onLauseLopp()) || (onKuniSonanaVoiKriips && !sona.onLauseLopp())){
				//
				// Valideerime alguspunkti - see peab kindlalt kas "kuni", numbrikombinatsioon,
				// potents. algusega arvsonafraas v6i ajavaljendifraas... Kui seda ei ole, 
				// j2tame vahele;
				// 
				if (!onKuniSonanaVoiKriips && teeKindlaksSonaKuuluvus(sona) == 0){
					continue;
				}
				// 
				// Leidsime potentsiaalse vahemiku keskosa/alguse
				//
				murdePunktOnInklusiivne = !(onKuniSonanaVoiKriips);
				LinkedList<AjavtSona> uusFraas = new LinkedList<AjavtSona>();
				if (onKuniSonanaVoiKriips){ uusFraas.add(sona); }
				// 
				// 1) Kontrollime potsentsiaalset vahemiku loppu
				//
				int lopuPikkus   = 0;  // pikkus s6nades, kriipsu v6i s6na "kuni" lugemata
				int lopuKuuluvus = 0;
				if (i + 1 < sonad.size()){
					AjavtSona jargmSona = sonad.get(i+1);
					lopuKuuluvus = teeKindlaksSonaKuuluvus(jargmSona);
					if (lopuKuuluvus >= ON_LEITUD_AJAVALJEND){
						MutableInteger fraasiLoplikKuuluvus = new MutableInteger( lopuKuuluvus );
						boolean lisamineOnnestus = koguKokkuJaLisaAjavaljendKonstrueeritavvaFraasi(
														uusFraas, jargmSona, true, fraasiLoplikKuuluvus);
						lopuKuuluvus = (lisamineOnnestus) ? (fraasiLoplikKuuluvus.getInteger()) : (0);
					} else if (lopuKuuluvus > 0){
						boolean lisamineOnnestus = koguKokkuJaLisaArvKonstrueeritavvaFraasi(uusFraas, sonad, addedFromLocation, i+1, true);
						lopuKuuluvus = (lisamineOnnestus) ? (lopuKuuluvus) : (0);
					}
					lopuPikkus = (!murdePunktOnInklusiivne) ? (uusFraas.size()) : (uusFraas.size() + 1);
				}
				if (lopuKuuluvus == 0){
					continue;
				}
				// 
				// 2) Kontrollime potsentsiaalset vahemiku algust
				//
				int alguseKuuluvus   = 0;
				int eelnevaSonaIndex = (onKuniSonanaVoiKriips) ? (i-1) : (i);
				if (eelnevaSonaIndex > -1){
					AjavtSona eelnvSona = sonad.get(eelnevaSonaIndex);
					alguseKuuluvus = teeKindlaksSonaKuuluvus(eelnvSona);
					if (alguseKuuluvus >= ON_LEITUD_AJAVALJEND){
						MutableInteger fraasiLoplikKuuluvus = new MutableInteger( alguseKuuluvus );
						boolean lisamineOnnestus = koguKokkuJaLisaAjavaljendKonstrueeritavvaFraasi(
														uusFraas, eelnvSona, false, fraasiLoplikKuuluvus);
						alguseKuuluvus = (lisamineOnnestus) ? (fraasiLoplikKuuluvus.getInteger()) : (0);
					} else if (alguseKuuluvus > 0){
						boolean lisamineOnnestus = koguKokkuJaLisaArvKonstrueeritavvaFraasi(uusFraas, sonad, addedFromLocation, eelnevaSonaIndex, false);
						alguseKuuluvus = (lisamineOnnestus) ? (alguseKuuluvus) : (0);						
					}
					murdePunkt = uusFraas.size() - lopuPikkus;
				}
				if (alguseKuuluvus == 0){
					continue;
				}
				//
				// 3) Kahest poolest peab olema vahemalt yks eraldatud ajavaljendina, vastasel korral ei saa
				//    me korrektselt yhendamist l2bi viia ...
				//
				if (alguseKuuluvus < ON_LEITUD_AJAVALJEND && lopuKuuluvus < ON_LEITUD_AJAVALJEND){
					continue;
				} else {
					//  Kui m6lemad on eraldatud (s6nadest koosnevate) ajavaljenditena, kontrollime, et 
					// leiduksid vastavalt potentsiaalne vahemiku algus ja potentsiaalne vahemiku l6pp:
					//  TODO: Jargnev kontroll v2listab ka ainult nimetavas fraaside liitmised vahemikuks
					//        (nt "1. märts - 2. aprill");
					if (alguseKuuluvus == ON_LEITUD_AJAVALJEND && lopuKuuluvus == ON_LEITUD_AJAVALJEND){
						int tunnuseidLeitud = 0;
						for (AjavtSona ajavtSona : uusFraas) {
							if (ajavtSona.isOnPotentsiaalneVahemikuAlgus() && tunnuseidLeitud == 0){
								tunnuseidLeitud++;
							}
							if (ajavtSona.isOnPotentsiaalneVahemikuLopp() && tunnuseidLeitud == 1){
								tunnuseidLeitud++;
							}							
						}
						if (tunnuseidLeitud != 2){
							continue;
						}
					}
					AjavaljendiKandidaat ajav = new AjavaljendiKandidaat();
					ajav.setAste(ASTE.YHENDATUD_VAHEMIKUNA);
					ajav.votaAntudSonadegaSeotudKandidaadidAlamkandidaatideks(uusFraas);
					AjaTuvastaja.seoSonadKandidaadiKylge(ajav, uusFraas);
					if (0 <= murdePunkt && murdePunkt < uusFraas.size()){
						ajav.setPoolitus(murdePunkt, murdePunktOnInklusiivne);
					}
					addedFromLocation.put(String.valueOf(i), "added");
					murdePunkt = -1;
					murdePunktOnInklusiivne = false;
				}
			} 
		}
	}
	
	/**
	 *   Abimeetod meetodile <tt>eraldaAjavahemikudJaLiidaFraasiks</tt>. Teeb kindlaks, kas sona
	 *  on numbrikombinatsioon, ajavaljendikandidaadi osa voi arvsonafraasi osa. Tagastab 0, kui 
	 *  s6na ei kuulu yldse eelnevasse loetellu.
	 */
	private static int teeKindlaksSonaKuuluvus(AjavtSona sona){
		if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
			return ( musterNumbrikombinatsioon.matcher( sona.getAlgSonaErisymbolidNormaliseeritud() ).matches() ) ? 
					(ON_AJAVALJEND_NUMBRIKOMBINATSIOON) : (ON_LEITUD_AJAVALJEND);
		} else if (sona.getArvSonaFraasis() != FraasisPaiknemiseKoht.PUUDUB) {
			return ON_ARVSONAFRAAS;
		} else if ( musterNumbrikombinatsioon.matcher( sona.getAlgSonaErisymbolidNormaliseeritud() ).matches() ){
			return ON_NUMBRIKOMBINATSIOON;
		}
		return 0;
	}
	
	/**
	 *  Lisab etteantud s6naga <tt>sona</tt> seotud ajav2ljendifraasi v6i yksiku ajav2ljendi
	 * fraasi <tt>uusFraas</tt> loppu (<tt>lisaLoppu == true</tt>) v6i algusesse (<tt>lisaLoppu == false</tt>).
	 * <p>
	 * Enne ylekandmist kontrollitakse, kas fraasispaiknemisekohad klapivad. St l6ppu lubatakse lisada vaid
	 * algavaid ajav2ljendifraase ning algusesse lubatakse vaid lisada l6ppevaid ajav2ljendifraase; Samuti
	 * kontrollitakse, et lisatava ajav2ljendifraasi l6pus v6i keskel poleks lausel6ppu - st yhendamine ei
	 * tohiks v2ljuda lausepiiridest.
	 * Probleemide korral ylekannet ei toimu. 
	 * <p>
	 * Muutujasse <tt>fraasiKuuluvus</tt> pannakse fraasi "l6pliku kuuluvuse" t2his: kui fraas sisaldab 
	 * ainult ON_AJAVALJEND_NUMBRIKOMBINATSIOON kuuluvusega elemente, on l6plikuks kuuluvuseks ON_AJAVALJEND_NUMBRIKOMBINATSIOON,
	 * vastasel juhul v6etakse kuuluvuseks ON_LEITUD_AJAVALJEND.
	 * <p>
	 * Tagastab <tt>true</tt> kui ylekandmine viidi l2bi, vastasel juhul <tt>false</tt>. 
	 */
	private static boolean koguKokkuJaLisaAjavaljendKonstrueeritavvaFraasi(LinkedList<AjavtSona> uusFraas,
																	  	   AjavtSona sona,
																	  	   boolean lisaLoppu,
																	  	   MutableInteger fraasiKuuluvus){
		if (sona.onSeotudMoneAjavaljendiKandidaadiga()){
			List<FraasisPaiknemiseKoht> paiknemisKohad        = sona.getAjavaljendiKandidaatides();
			List<AjavaljendiKandidaat> ajavaljendiKandidaadid = sona.getAjavaljendiKandidaadid();
			//
			//  NB! Teeme eelduse, et FRAAS-eraldused on ajavaljendiKandidaadid-listis k6ige viimased 
			//  ning seej2rel tulevad tavalised MUSTER-eraldused. Kas v6ib juhtuda, et see eeldus 
			//  m6nikord ei kehti?
			//
			for (int i = paiknemisKohad.size()-1; i > -1; i--) {
				FraasisPaiknemiseKoht kohtFraasis         = paiknemisKohad.get(i);
				AjavaljendiKandidaat ajavaljendiKandidaat = ajavaljendiKandidaadid.get(i);
				if (lisaLoppu){
					// Loppu lisamiseks peab olema algav fraas
					if (kohtFraasis.onFraasiAlgus()){
						// Leiame kandidaadiga seotud fraasi	
						List<AjavtSona> fraas1 = ajavaljendiKandidaat.getFraas();
						// Kontrollime, et lisatava fraasi keskel pole lauselõppu -
						// kui on, loeme lisamise ebaõnnestunuks ...
						for (int j = 0; j < fraas1.size(); j++) {
							AjavtSona ajavtSona = fraas1.get(j);
							if (ajavtSona.onLauseLopp() && j < fraas1.size() - 1){
								return false;
							}
						}
						// T2iendame uut fraasi leitud fraasiga
						for (int j = 0; j < fraas1.size(); j++) {
							AjavtSona ajavtSona = fraas1.get(j);
							if (teeKindlaksSonaKuuluvus(ajavtSona) == ON_LEITUD_AJAVALJEND){
								fraasiKuuluvus.setInteger(ON_LEITUD_AJAVALJEND);
							}
							uusFraas.addLast( ajavtSona );
						}
						return true;
					}
				} else {
					// Algusesse lisamiseks peab olema l6ppev fraas					
					if (kohtFraasis.onFraasiLopp()){
						// Leiame kandidaadiga seotud fraasi					
						List<AjavtSona> fraas1 = ajavaljendiKandidaat.getFraas();
						// Kontrollime, et lisatava fraasi keskel pole lauselõppu -
						// kui on, loeme lisamise ebaõnnestunuks ...
						for (int j = fraas1.size()-1; j > -1 ; j--) {
							AjavtSona ajavtSona = fraas1.get(j);
							if (ajavtSona.onLauseLopp()){
								return false;
							}
						}
						// T2iendame uut fraasi leitud fraasiga						
						for (int j = fraas1.size()-1; j > -1 ; j--) {
							AjavtSona ajavtSona = fraas1.get(j);
							uusFraas.addFirst(ajavtSona);
						}
						return true;						
					}					
				}
			}
		}
		return false;
	}

	/**
	 *  Lisab etteantud s6naga <tt>sona</tt>-st algava numbrikombinatsiooni v6i arvs6nafraasi
	 * fraasi <tt>uusFraas</tt> loppu (<tt>lisaLoppu == true</tt>) v6i algusesse (<tt>lisaLoppu == false</tt>).
	 * <p>
	 * Arvs6nafraasi korral kontrollitakse enne ylekandmist, kas fraasispaiknemisekohad klapivad. Loogika 
	 * analoogne meetodis <tt>koguKokkuJaLisaAjavaljendKonstrueeritavvaFraasi</tt> rakendatule.
	 * <p>
	 * Tagastab <tt>true</tt> kui ylekandmine viidi l2bi, vastasel juhul <tt>false</tt>. 
	 */
	private static boolean koguKokkuJaLisaArvKonstrueeritavvaFraasi(LinkedList<AjavtSona> uusFraas,
															 List<AjavtSona> sonad,
															 HashMap<String, String> addedFromLocation,
															 int currentWordIndex,
		  	   												 boolean lisaLoppu){
		int i = currentWordIndex;
		int eelmineKuuluvus = -1;
		boolean addingWasSuccessful = false;
		while (0 <= i && i < sonad.size()){
			if (addedFromLocation.containsKey(String.valueOf(i))){
				return addingWasSuccessful;
			}
			AjavtSona ajavtSona = sonad.get(i);
			// Kui j6udsime tagurpidi liikudes lausel6puni, l6petame
			if (ajavtSona.onLauseLopp() && !lisaLoppu){
				return addingWasSuccessful;
			}
			int sonaKuuluvus = teeKindlaksSonaKuuluvus(ajavtSona);
			if (eelmineKuuluvus == -1 || (sonaKuuluvus != 0 && sonaKuuluvus == eelmineKuuluvus)){
				if (sonaKuuluvus == ON_ARVSONAFRAAS && eelmineKuuluvus == -1){
					// Arvs6nafraasi puhul kontrollime, et oleks ikka kindlalt fraasi algus/l6puga
					FraasisPaiknemiseKoht arvSonaFraasis = ajavtSona.getArvSonaFraasis();
					if (lisaLoppu && !arvSonaFraasis.onFraasiAlgus()){
						return addingWasSuccessful;
					} else if (!lisaLoppu && !arvSonaFraasis.onFraasiLopp()){
						return addingWasSuccessful;
					}
					// TODO: mitme j2rjestikku paikneva arvs6nafraasi korral siin t6ket pole,
					// liidame yksteise otsa k6ik ...
				}
				if (lisaLoppu){
					uusFraas.addLast(ajavtSona);
				} else {
					uusFraas.addFirst(ajavtSona);
				}
				addingWasSuccessful = true;
			} else {
				// Kui oleme j6udnud mingi teistsugust "tyypi" s6nani, on aeg l6petada
				return addingWasSuccessful;
			}
			// Kui j6udsime edaspidi liikudes lausel6puni, l6petame			
			if (ajavtSona.onLauseLopp()){
				return addingWasSuccessful;
			}
			i += (lisaLoppu) ? (1) : (-1);
			eelmineKuuluvus = sonaKuuluvus;
		}
		return addingWasSuccessful;
	}
	
	//==============================================================================
	//   	D e b u g g i n g 
	//==============================================================================
	
	public void kuvaSonaKlassidJaReeglid(LogiPidaja logi){
		if (logi != null){
			Set<String> keys = (this.sonaKlassid).keySet();
			logi.println("=============================");
			logi.println("   SonaKlassid ("+keys.size()+")");
			logi.println("=============================");
			for (String k : keys) {
				if ((this.sonaKlassid).containsKey(k)){
					logi.println(k+" = ");
					SonaKlass klass = (this.sonaKlassid).get(k);
					for (SonaMall mall : klass.getElemendid()) {
						logi.println("     "+mall.toString());
					}
				}
			}
			logi.println();			
			logi.println("=============================");
			logi.println("   Reeglid ("+(this.reeglid.size())+")");
			logi.println("=============================");
			for (TuvastamisReegel reegel : this.reeglid) {
				logi.println(" "+reegel);
				if (reegel.getNegMustrid() != null){
					for (NegatiivneMuster negMuster : reegel.getNegMustrid()) {
						logi.println("   "+negMuster.toString());
					}
				}
			}
			logi.println();
			
			if (this.liitumisReeglid != null){
				logi.println("=============================");
				logi.println("   LiitumisReeglid ("+(this.liitumisReeglid.size())+")");
				logi.println("=============================");
				for (LiitumisReegel reegel : this.liitumisReeglid) {
					logi.println(" "+reegel);
				}
			}
			logi.println();
		}
	}

	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================	
	
	public String getReegliFail() {
		return reegliFail;
	}

	public void setReegliFail(String reegliFail) {
		this.reegliFail = reegliFail;
	}	
	
	public void setLogi(LogiPidaja logi) {
		this.logi = logi;
	}

	public HashMap<String, SonaKlass> getSonaKlassid() {
		return sonaKlassid;
	}
	
	public void setSonaKlassid(HashMap<String, SonaKlass> sonaKlassid) {
		this.sonaKlassid = sonaKlassid;
	}
	
	public List<LiitumisReegel> getLiitumisReeglid() {
		return liitumisReeglid;
	}

	public void setLiitumisReeglid(List<LiitumisReegel> liitumisReeglid) {
		this.liitumisReeglid = liitumisReeglid;
	}

	public List<TuvastamisReegel> getReeglid() {
		return reeglid;
	}
	
	public void setReeglid(List<TuvastamisReegel> reeglid) {
		this.reeglid = reeglid;
	}
	
	public static String getVersioon() {
		return versioon;
	}

	public EstyhmmWrapper getWrapper() {
		return wrapper;
	}

	public void setWrapper(EstyhmmWrapper wrapper) {
		this.wrapper = wrapper;
	}

	public SemLeidmiseMudel getMudel() {
		return mudel;
	}

	public void setMudel(SemLeidmiseMudel mudel) {
		this.mudel = mudel;
	}

	/**
	 *  Alametapp ajavaljendikandidaadi konstrueerimisel: Seob etteantud sonad kandidaadiga.
	 */
	public static void seoSonadKandidaadiKylge(AjavaljendiKandidaat ajav, List<AjavtSona> eraldatudSonad){
		for (int i = 0; i < eraldatudSonad.size(); i++) {
			FraasisPaiknemiseKoht koht = FraasisPaiknemiseKoht.AINUSSONA;
			if (eraldatudSonad.size() > 1){
				if (i == 0){
					koht = FraasisPaiknemiseKoht.ALGUSES;
				} else if (i == eraldatudSonad.size() - 1){
					koht = FraasisPaiknemiseKoht.LOPUS;
				} else {
					koht = FraasisPaiknemiseKoht.KESKEL;
				}
			}
			ajav.lisaFraasiUusSona(eraldatudSonad.get(i), koht);
		}
	}
	
}
