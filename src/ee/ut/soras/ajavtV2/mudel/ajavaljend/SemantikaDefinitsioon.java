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

package ee.ut.soras.ajavtV2.mudel.ajavaljend;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.joda.time.Period;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaKestvus;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaObjekt;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaObjekt.TYYP;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaPunkt;
import ee.ut.soras.ajavtV2.util.LogiPidaja;
import ee.ut.soras.ajavtV2.util.SemDefValjadeParsija;
import ee.ut.soras.ajavtV2.util.SemDefValjadeParsija.FORMAT_OF_VALUE;
import ee.ut.soras.ajavtV2.util.TextUtils;

/**
 *   Semantika arvutamise yksikinstruktsioon. NB! Magistrit&ouml;&ouml;s kasutatakse selle klassi kohta nimetust
 *   <i>semantikaReegel</i>.
 *   <p>   
 *   Sisuliselt jupike ajavaljendi semantika definitsioonist (teise nimega v&otilde;iks seda objekti
 *   kutsuda veel <i>semantika Arvutuse Instruktsioon</i>, <i>arvutusOperatsioon</i>, <i>arvutusReegel</i> jms).
 *   Ajavaljendikandidaadi semantika leidmiseks sorteeritakse k&otilde;ik kandidaadi alla kuuluvad 
 *   semantikadefinitsioonid-arvutusreeglid muutuja <tt>priority</tt> alusel kasvavaks ning rakendatakse neid
 *   seej&auml;rel samm-sammult.
 *   
 *   @author Siim Orasmaa
 */
public class SemantikaDefinitsioon extends MustristSoltuv implements Comparable < SemantikaDefinitsioon >{

	public static LogiPidaja logi;
	
	/**
	 *  Arvutusoperatsioonide nimetused.
	 */	
	public static enum OP {
		            /** P&otilde;hik&auml;sklus: kirjutab kalendriv&auml;lja olemasoleva v&auml;&auml;rtuse yle etteantud v&auml;&auml;rtusega; */
		 SET,
		            /** P&otilde;hik&auml;sklus: liidab kalendriv&auml;lja olemasolevale v&auml;&auml;rtusele mingi arvu; */
		 ADD,
		            /** P&otilde;hik&auml;sklus: lahutab kalendriv&auml;lja olemasolevast v&auml;&auml;rtusest mingi arvu; */
		 SUBTRACT,
		            /** Tavaline SEEK: v2listab otsitavate hetkede seast referentsaja; Otsimise suund v6ib tulla grammatilisest ajast;  */
		 SEEK,
		            /** Inklusiivne SEEK: k2esolev hetk v6ib kuuluda otsitavate hetkede hulka; Otsimise suund v6ib tulla grammatilisest ajast; */
		 SEEK_IN, 
		            /**  Spetsiifiline algoritm: Baldwini aken.  */
		 BALDWIN_WINDOW,
		 /** 
		  *   Leiab etteantud granulaarsuse n-inda teatud omadustega alamosa (nt kuu granulaarsuse seest n-inda nadalapaeva); 
		  */
		 FIND_NTH_SUBGRAN, 
		 /** 
		  *  TIMEX3 mingi atribuudi väärtuste omistamine; Teatud juhul kaasneb atribuudi väärtustamisega ka mingi
		  *  lisategevus, mis oleneb aga rangelt määratavast atribuudist ...
		  *  <ul>
		  *     <li>"type"  - vajadusel muudab ajaobjekti kestvuseks;
		  *     <li>"mod"   - lisategevust pole;
		  *     <li>"value" - arvutatav "value" kirjutatakse yle;
		  *     <li>"beginPoint", "endPoint" - Esialgu võib väärtuseks olla suvaline sõne; Kasutusel ainult DURATION puhul;
		  *     <li>"anchorTimeID" - omistab atribuudi 'anchorTimeID' väärtuse;
		  *  </ul>
		  */ 
		 SET_attrib,
   		 /**  Algatab uue ajav&auml;ljendi - nn alguspunkti - loomise. Loodav punkt v6ib olla varjatud v6i tekstis 
 		  *   olemasolev (eksplitsiitne), vastavalt sellele, kuidas on m22ratud muutujas <code>isExplicitPoint</code>; 
 		  *   Kasule jargnevad kasud modifitseerivad loodud punkti; */
		 CREATE_beginPoint,
		 /**  Algatab uue ajav&auml;ljendi - nn l&otilde;pp-punkti - loomise. Loodav punkt v6ib olla varjatud v6i tekstis 
		  *   olemasolev (eksplitsiitne), vastavalt sellele, kuidas on m22ratud muutujas <code>isExplicitPoint</code>; 
		  *   Kasule jargnevad kasud modifitseerivad loodud punkti; */		 
		 CREATE_endPoint,		 
		 /** Katab kinni ajalise granulaarsuses n symbolit (praegu ainult aastate puhul); */
		 COVER_VAL,
		 /** Ankurdamine teise ajav2ljendi kylge, ankrut otsitakse 100-s6na raadiuses, lausepiire arvestamata; */
		 ANCHOR_TIMEX,
		 /** Ankurdamine teise ajav2ljendi kylge, ankru otsimisel ei v2ljuta lausepiiridest; */		 
		 ANCHOR_TIMEX_IN_SENTENCE,
		 /** M&auml;rgenduse-spetsiifiline: seab eraldatud fraasis koha, kust fraas tuleb l6plikus m2rgenduses pooleks teha;
		  *  <code>semValue</code> koosneb kahest komaga eraldatud v22rtusest: poolitusKoht(int), poolitusOnInklusiivne(boolean);
		  */
		 SET_HALVING_POINT
	};

	// Kontekstiga seotud konstandid
	public static enum CONTEXT {
		 // t2histab seda, et arvatava vahemiku otspunkt (number) eelneb sellele mustrile 
		 NUM_VAHEM_OTSPUNKT_EELNEB,
		 // t2histab seda, et arvatava vahemiku otspunkt (number) jargneb sellele mustrile		 
		 NUM_VAHEM_OTSPUNKT_JARGNEB,
		 // millised on k6rvalfraasi lubatud granulaarsused
		 KORVALFRAASI_GRAN,
		 // antud fraasi k6rval ei leidu teist ajavaljendifraasi
		 KORVALFRAAS_PUUDUB,
		 // kontrollib, kas k6rvalfraasis leidub selline label (praegu ei leia kusagil kasutust!)
		 KORVALFRAASI_LABEL,
		 // ankurdamine (ANCHOR_TIMEX) on edukalt l2bi viidud
		 ANKURDAMINE_LABIVIIDUD,
		 // kontrollib ankrupunkti granulaarsusi (NB! ankrupunkt == arvutamisel fookuses olev aeg, sh ka dokumendi loomise aeg)
		 ANKRU_GRAN
	};
	
	/**
	 *   Rakendamise prioriteetsus s6nena (nagu see on antud XML-definitsioonis). Tegeliku
	 *   rakendamisjarjekorra leidmisel kasutatakse vaartust <tt>priorityInt</tt>. 
	 */
	private String priority;
	
	/**
	 *   T&auml;isarvuline prioriteetsus. Mida v&auml;iksem, seda suurem prioriteetsus (ehk semantikareegel
	 *   tuleb teistest varem rakendamisele). V&auml;&auml;rtuse <tt>-1</tt> korral on prioriteetsus 
	 *   k&otilde;ige k&otilde;rgem.  
	 */
	private int priorityInt = -1;
	
	/**
	 *   Millisest s6naklassist antud semantika parsiti?
	 *   <p>
	 *   Sonaklassi puhul koosneb nimi ainult symbolitest A-Z ning _.
	 *   <p>
	 *   Kui v22rtus sisaldab vaid symboleid 0-9, viidatakse mustri n-indale
	 *   elemendile (lugemine alates 0-st), mis ei pruugi olla s6naklass. 
	 */
	private String sonaKlass;
	
	/**
	 *    Tapsustab, kontekstitingimused, mis peavad olema t2idetud, enne, kui seda definitsiooni
	 *   voib rakendada. Vaikimisi on kontekstitingimusi ei ole, st muster rakendub kohe, kui 
	 *   <tt>seotudMustriosa</tt> tingimused on t2idetud.
	 */
	private String seotudKontekst;
	
	/**
	 *   Kas antud semantikadefinitsioon on ebatapne, st vajab tapsustamist
	 *   sama v6i vaiksema granulaarsusega semantikadefinitsiooni poolt?
	 */
	private boolean semValueOnEbatapne;

	/**
	 *   Millise granulaarsuse kohta definitsioon k2ib? Tapsemalt: Millist granulaarsust
	 *   muudetakse arvutamise k2igus?
	 */
	private Granulaarsus granulaarsus;
	
	/**
	 *    Ajav2ljendi granulaarsus(ed) s6ne kujul. Kasutusel m6ningatel erijuhtudel, kui on 
	 *   tarvis kasutada keerukamaid, mitmest granulaarsusest koosnevaid kombinatsioone; 
	 *   Tavaline semantikadefinitsioon peaks selle asemel kasutama muutujat 
	 *   <tt>granulaarsus</tt>.
	 */
	private String granulaarsusStr;

	/**
	 *   Operatsioon, mida rakendatakse arvutamisel.
	 */
	private String op;
	
	/**
	 *   Granulaarsuse v&auml;&auml;rtus. Tegemist on parsimata s6nega, kust konkreetne 
	 *   v22rtus tuleb veel eraldada.
	 */
	private String semValue;
	
	/**
	 *   Kokkuleppelist semantikat edasiandev lipik. Kasutusel selliste konventsionaalsete
	 *   ajav&auml;ljendite nagu nt <i>talvel</i>, <i>hommikul</i> vms m&auml;rkimiseks, kuna
	 *   nende semantika on suuresti kokkuleppeline - st semantika avamine voib pohjustada 
	 *   ebatapsusi.
	 *   <p>
	 *   Lipikud j&auml;rgivad suuresti TIMEX2 vastavate lipikute nimetamise konventsiooni
	 *   (http://fofoca.mitre.org/annotation_guidelines/2005_timex2_standard_v1.1.pdf);
	 */
	private String semLabel;
	
	/**
	 *    Spetsiifiline argument, mis taidab erinevates semantikaoperatsioonides erinevaid
	 *   funktsioone.
	 *   <p>
	 *   Naide funktsioonist: <tt>SEEK</tt>-tyypi operatsiooni l2biviimisel on tegemist 
	 *   ajateljel liikumise suunaga. Suund -1 viitab minevikule, suund +1 viitab tulevikule.
	 *   Kui <tt>direction == 0</tt>, on suund m22ramata;
	 *   <p>
	 *   Argument omab funktsiooni <tt>SEEK</tt>-operatsioonides, 
	 *   <tt>ANCHOR_TIMEX</tt>-operatsioonides ning <tt>FIND_NTH_SUBGRAN</tt>-operatsioonis.  
	 */
	private String direction;

	/**
	 *   Millise semantika lahendamise mudeli alla antud semantikadefinitsioon kuulub?
	 *   Mittekohustuslik; kasutatakse, et eristada teatud mudeleid semantika arvutamisel. 
	 */
	private String mudel;
	
	/**
	 *   K&auml;suspetsiifiline: m22rab, kas <code>CREATE_beginPoint</code> voi <code>CREATE_endPoint</code>
	 *   abil loodav ajapunkt on eksplitsiitne (tekstis olemasolev/eraldiseisev) v6i varjatud
	 *   (tekstis pole valja toodud, aga saab tuletada). Vaikevaartuseks <code>false</code>.
	 */
	private boolean isExplicitPoint = false;
	
	/**
	 *    K&auml;suspetsiifiline: m&auml;&auml;rab k&auml;su <code>SET_attrib</code> poolt muudetava TIMEX3 atribuudi 
	 *    (on atribuudi nimi).
	 */
	private String attribute = null;
	
	//==============================================================================
	//   	S e m a n t i k a   a r v u t a m i n e 
	//==============================================================================	

	/**
	 *   Sooritab selle semantikadefinitsiooni poolt kirjeldatud arvutusoperatsiooni etteantud ajaobjekti 
	 *  (<tt>alusObjekt</tt>) peal. Voib luua ka uue ajaobjekti, kui operatsioon seda ette naeb. Tagastab 
	 *  operatsiooni tulemusena muudetud voi loodud ajaobjekti.
	 */
	public AjaObjekt rakendaArvutusReeglit(AjaObjekt alusObjekt, String [] aegFookuses, AjavtSona lahimVerb){
		if (this.op != null){
			if ((this.op).equals(SemantikaDefinitsioon.OP.CREATE_beginPoint.toString()) && this.isExplicitPoint){
				// Kui alusobjekt on m22ramata v6i selleks on mitteajapunkt, loome uue 
				// intervalli eksplitsiitsete punktidena ...
				if (alusObjekt == null || !(alusObjekt instanceof AjaPunkt)){
					alusObjekt = new AjaPunkt( AjaObjekt.TYYP.POINT, aegFookuses );
					AjaPunkt algusPunkt = new AjaPunkt( AjaObjekt.TYYP.POINT, aegFookuses );
					AjaPunkt loppPunkt  = new AjaPunkt( AjaObjekt.TYYP.POINT, aegFookuses );
					alusObjekt.addRelatedExplicitTIMEX( algusPunkt );
					alusObjekt.addRelatedExplicitTIMEX( loppPunkt  );
				} else if (alusObjekt instanceof AjaPunkt){
					AjaPunkt algusPunkt = (AjaPunkt) alusObjekt.clone();
					AjaPunkt loppPunkt  = (AjaPunkt) alusObjekt.clone();
					alusObjekt.addRelatedExplicitTIMEX( algusPunkt );
					alusObjekt.addRelatedExplicitTIMEX( loppPunkt  );					
				}
				return alusObjekt;
			}
			if (alusObjekt == null){
				alusObjekt = new AjaPunkt( AjaObjekt.TYYP.POINT, aegFookuses );
			}
			int   semValueAsInt  = Integer.MIN_VALUE;
			float semValueAsFrac = Float.MIN_VALUE;
			// Kas on tegemist konkreetse Value v22rtusega?
			if (this.semLabel == null && this.semValue != null){
				// A. Value - parsime selle lahti
				FORMAT_OF_VALUE formatOfValue = SemDefValjadeParsija.detectFormatOfValue(this.semValue);
				if (formatOfValue == FORMAT_OF_VALUE.CONSTANT){
					// proovime konstandi parsimist
					semValueAsInt = SemDefValjadeParsija.parseValueFromConstant(this.semValue);
				} else if (formatOfValue == FORMAT_OF_VALUE.INTEGER){
					// proovime tavalise arvu parsimist
					try {
						String withOutPlus = (this.semValue).replace("+", "");
						semValueAsInt = Integer.parseInt(withOutPlus);
					} catch (NumberFormatException e) {
					}
				} else if (formatOfValue == FORMAT_OF_VALUE.FRACTION){
					// proovime murdarvu parsimist
					try {
						String withOutPlusAndWithPoint = ((this.semValue).replace("+", "")).replace(",", ".");
						semValueAsFrac = Float.parseFloat(withOutPlusAndWithPoint);
					} catch (NumberFormatException e) {
					}
				}
			}
			// SET, ADD, SEEK operatsioonid, BALDWIN_WINDOW algoritm, COVER_VAL
			if (this.granulaarsus != null){
				if ((this.op).equals(OP.SET.toString())){
					// ------------- konkreetne Value
					if (semValueAsInt != Integer.MIN_VALUE){
						alusObjekt.setField(this.granulaarsus, semValueAsInt);
					}
					// ------------- Value-Label ainult				
					if (this.semLabel != null){
						alusObjekt.setField(this.granulaarsus, this.semLabel);
					}
				}
				if ((this.op).equals(OP.ADD.toString()) || (this.op).equals(OP.SUBTRACT.toString())){
					// ------------- konkreetne Value
					if (semValueAsInt != Integer.MIN_VALUE){
						if ((this.op).equals(OP.SUBTRACT.toString())){
							semValueAsInt *= -1;
						}
						alusObjekt.addToField(this.granulaarsus, semValueAsInt);
					}
					// ------------- komakohaga value : parsime lahti kahe granulaarsusega perioodiks
					if (semValueAsFrac != Float.MIN_VALUE){
						Object[] perioodJaMOD = 
							SemDefValjadeParsija.parsiMurdarvulineAjakvantiteetPerioodiks(this.granulaarsus, semValueAsFrac);
						if (perioodJaMOD != null && perioodJaMOD[0] != null){
							alusObjekt.addToField(
									this.granulaarsus, 
									(Period)perioodJaMOD[0],
										((this.op).equals(OP.SUBTRACT.toString())) ? (-1) : (1) );
							if (perioodJaMOD.length == 2){
								alusObjekt.setTimex3Attribute( "mod", (String)perioodJaMOD[1] );
							}
						}
					}
					// ------------- ainult kestvuste korral: umbm22rane kestvus					
					if (semValueAsInt == Integer.MIN_VALUE && (alusObjekt instanceof AjaKestvus) && (this.semValue != null)){
						((AjaKestvus)alusObjekt).addToField(this.granulaarsus, this.semValue);
					}
				}
				if (this.direction != null){
					// Tavaline SEEK
					if ( (this.op).equals(OP.SEEK.toString()) ){
						// 1) Parsime lahti otsimissuuna					
						int direction = SemDefValjadeParsija.parseSeekDirection(this.direction, lahimVerb);						
						if (direction != Integer.MIN_VALUE){
							int valueToSeekAsInt = Integer.MIN_VALUE;
							// 2) Parsime lahti otsitava v22rtuse (valueToSeek == semValue)
							FORMAT_OF_VALUE formatOfValue = SemDefValjadeParsija.detectFormatOfValue(this.semValue);
							if (formatOfValue == FORMAT_OF_VALUE.CONSTANT){
								// proovime konstandi parsimist
								valueToSeekAsInt = SemDefValjadeParsija.parseValueFromConstant(this.semValue);
							} else if (formatOfValue == FORMAT_OF_VALUE.INTEGER){
								// proovime tavalise arvu parsimist
								try {
									String withOutPlus = (this.semValue).replace("+", "");
									valueToSeekAsInt = Integer.parseInt(withOutPlus);
								} catch (NumberFormatException e) {
								}					
							}
							if (valueToSeekAsInt != Integer.MIN_VALUE){
								// ------------- Otsitav v22rtus on konkreetne t2isarv
								alusObjekt.seekField(this.granulaarsus, direction, valueToSeekAsInt, true);	
							} else {
								// ------------- Otsitav v22rtus on Value-Label
								alusObjekt.seekField(this.granulaarsus, direction, this.semValue, true);
							}
						} else {
							// Suuna m22ramine eba6nnestus: langeme tagasi SET operatsiooniks ...
							// ------------- konkreetne Value
							if (semValueAsInt != Integer.MIN_VALUE){
								alusObjekt.setField(this.granulaarsus, semValueAsInt);
								// Hakk: kui on tegemist n2dalap2evaga, "muudame" ka n2dalat, et saada
								// 6ige formaat (kuup6hine formaat)
								if (this.granulaarsus == Granulaarsus.DAY_OF_WEEK){
									alusObjekt.addToField(Granulaarsus.WEEK_OF_YEAR, 0);
								}
							}
							// ------------- Value-Label ainult				
							if (this.semLabel != null){
								alusObjekt.setField(this.granulaarsus, this.semLabel);
							}
						}
					}
					// Referentsaja hetke mittev2listav SEEK; v6ib suuna v6tta (verbi) grammatilisest ajast;
					// NB! Kui suuna m22ramine eba6nnestub, "langeb tagasi" SET operatsiooniks
					if ( (this.op).equals(OP.SEEK_IN.toString()) ){
						// 1) Parsime lahti otsimissuuna					
						int seek_dir = SemDefValjadeParsija.parseSeekDirection(this.direction, lahimVerb);
						if (seek_dir != Integer.MIN_VALUE){
							int valueToSeekAsInt = Integer.MIN_VALUE;
							// 2) Parsime lahti otsitava v22rtuse (valueToSeek == semValue)
							FORMAT_OF_VALUE formatOfValue = SemDefValjadeParsija.detectFormatOfValue(this.semValue);
							if (formatOfValue == FORMAT_OF_VALUE.CONSTANT){
								// proovime konstandi parsimist
								valueToSeekAsInt = SemDefValjadeParsija.parseValueFromConstant(this.semValue);
							} else if (formatOfValue == FORMAT_OF_VALUE.INTEGER){
								// proovime tavalise arvu parsimist
								try {
									String withOutPlus = (this.semValue).replace("+", "");
									valueToSeekAsInt = Integer.parseInt(withOutPlus);
								} catch (NumberFormatException e) {
								}					
							}
							if (valueToSeekAsInt != Integer.MIN_VALUE){
								// ------------- Otsitav v22rtus on konkreetne t2isarv
								alusObjekt.seekField(this.granulaarsus, seek_dir, valueToSeekAsInt, false);	
							} else {
								// ------------- Otsitav v22rtus on Value-Label
								alusObjekt.seekField(this.granulaarsus, seek_dir, this.semValue, false);
							}
						} else {
							// Suuna m22ramine eba6nnestus: langeme tagasi SET operatsiooniks ...
							// ------------- konkreetne Value
							if (semValueAsInt != Integer.MIN_VALUE){
								alusObjekt.setField(this.granulaarsus, semValueAsInt);
								// Hakk: kui on tegemist n2dalap2evaga, "muudame" ka n2dalat, et saada
								// 6ige formaat (kuup6hine formaat)
								if (this.granulaarsus == Granulaarsus.DAY_OF_WEEK){
									alusObjekt.addToField(Granulaarsus.WEEK_OF_YEAR, 0);
								}
							}
							// ------------- Value-Label ainult				
							if (this.semLabel != null){
								alusObjekt.setField(this.granulaarsus, this.semLabel);
							}
						}
					}
				}
				// Yldistatud Baldwini akna konseptsiooni rakendamine 					
				if ((this.op).equals(OP.BALDWIN_WINDOW.toString())){
					if (alusObjekt instanceof AjaPunkt){
						FORMAT_OF_VALUE formatOfValue = SemDefValjadeParsija.detectFormatOfValue(this.semValue);
						if (formatOfValue == FORMAT_OF_VALUE.CONSTANT){
							// proovime konstandi parsimist
							int valueAsInt = SemDefValjadeParsija.parseValueFromConstant(this.semValue);
							if (valueAsInt != Integer.MIN_VALUE){
								// ------------- konkreetne Value								
								((AjaPunkt)alusObjekt).applyBalwinWindow(this.granulaarsus, String.valueOf(valueAsInt));
							} else {
								// ------------- Value lipik								
								((AjaPunkt)alusObjekt).applyBalwinWindow(this.granulaarsus, this.semValue);
							}
						} else {
							// ------------- Value t2isarv
							((AjaPunkt)alusObjekt).applyBalwinWindow(this.granulaarsus, this.semValue);							
						}
					}
				}
				// V22rtuste kinnikatmine
				if ((this.op).equals( OP.COVER_VAL.toString() )){
					// Siin tuleb semValue eraldi lahti parsida, kuna v6ib olla, et semLabel on olemas
					// ning seet6ttu semValue j22nud parsimata (nt aastaaegade korral)
					try {
						String withOutPlus = (this.semValue).replace("+", "");
						semValueAsInt = Integer.parseInt(withOutPlus);
					} catch (NumberFormatException e) {
						semValueAsInt = Integer.MIN_VALUE;
					}
					// ------------- konkreetne Value
					if (semValueAsInt != Integer.MIN_VALUE){
						alusObjekt.closeGranularitiesBelow(this.granulaarsus, semValueAsInt);
					}
				}
			}
			// FIND_NTH_SUBGRAN: etteantud granulaarsuse n-inda alamosa leidmine, 
			// kus alamosa vastab teatud tingimustele ...
			if ((this.op).equals( (OP.FIND_NTH_SUBGRAN).toString() ) && this.direction != null){
				if (this.granulaarsusStr != null && semValueAsInt != Integer.MIN_VALUE){
					String granStrings [] = (this.granulaarsusStr).split(":");
					if (granStrings.length > 1){
						// Parsime granulaarsused
						Granulaarsus superField = Granulaarsus.getGranulaarsus( granStrings[0] );
						Granulaarsus subField   = Granulaarsus.getGranulaarsus( granStrings[1] );
						// Parsime arvu alamosa jarjekorranumbri n
						int n = Integer.MIN_VALUE;
						String withOutPlus = (this.direction).replace("+", "");
						try {
							n = Integer.parseInt( withOutPlus );
						} catch (NumberFormatException e) {
						}
						if (n != Integer.MIN_VALUE){
							((AjaPunkt)alusObjekt).findNthSubpartOfGranularity(superField, subField, semValueAsInt, n);
						}
					}
				}
			}
			// SET_attrib: TIMEX3 mingi atribuudi muutmine/ylekirjutamine + sellest tulenev lisategevus
			if ((this.op).equals((OP.SET_attrib).toString()) && this.attribute != null){
				if (this.semValue != null){
					// --------------------       TYPE      ----------------------------------
					if ((this.attribute).equalsIgnoreCase("type")){
						// RECURRENCE
						if ((this.semValue).equals((AjaObjekt.TYYP.RECURRENCE).toString()) &&
								alusObjekt instanceof AjaPunkt){
							((AjaPunkt)alusObjekt).setType(TYYP.RECURRENCE);
						}
						// TIME
						if ((this.semValue).equals((AjaObjekt.TYYP.TIME).toString()) &&
								alusObjekt instanceof AjaPunkt){
							((AjaPunkt)alusObjekt).setType(TYYP.TIME);
						}					
						// DURATION
						if ((this.semValue).equals((AjaObjekt.TYYP.DURATION).toString())){
							alusObjekt = new AjaKestvus();
						}
					}
					// --------------------       MOD      ----------------------------------
					if ((this.attribute).equalsIgnoreCase("mod")){
						alusObjekt.setTimex3Attribute("mod", this.semValue);
					}
					// --------------------      VALUE      ----------------------------------
					if ((this.attribute).equalsIgnoreCase("value")){
						if (this.direction == null){
							alusObjekt.setTimex3Attribute("value", this.semValue);
						}
						// ----------------  Ajaline korduvus --------------------------------
						if (alusObjekt.getType() == TYYP.RECURRENCE){
							if (this.granulaarsus != null && (this.granulaarsus).getAsISOdurationElement() != null){
									if ((this.granulaarsus).compareByCoarseRank(Granulaarsus.HOUR_OF_DAY) == 1){
										if ((this.semValue).matches("[0-9]+")){
											String newVal = "P" + (this.semValue) + (this.granulaarsus).getAsISOdurationElement();
											alusObjekt.setTimex3Attribute("value", newVal);
										} else if ((this.semValue).matches("[0-9]+,[0-9]+")) {
											String [] perioodS6nena = 
												SemDefValjadeParsija.parsiMurdarvulineAjakvantiteetS6neks(granulaarsus, this.semValue);
											if (perioodS6nena != null){
												alusObjekt.setTimex3Attribute("value", perioodS6nena[0]);
											}
										}
									} else {
										if ((this.semValue).matches("[0-9]+")){
											String newVal = "PT" + (this.semValue) + (this.granulaarsus).getAsISOdurationElement();
											alusObjekt.setTimex3Attribute("value", newVal);
										} else if ((this.semValue).matches("[0-9]+,[0-9]+")) {
											String [] perioodS6nena = 
												SemDefValjadeParsija.parsiMurdarvulineAjakvantiteetS6neks(granulaarsus, this.semValue);
											if (perioodS6nena != null){
												alusObjekt.setTimex3Attribute("value", perioodS6nena[0]);
											}
										}
									}
							}
						}
					}
					// -----------------   beginPoint, endPoint  -----------------------------
					// Kestvuse alguspunkti/l6pp-punkti määramine, esialgu pole t2ielikult v2lja ehitatud
					if (alusObjekt instanceof AjaKestvus){
						if ((this.attribute).equalsIgnoreCase("beginPoint")){
							(alusObjekt).setTimex3Attribute("beginPoint", this.semValue);
						}
						if ((this.attribute).equalsIgnoreCase("endPoint")){
							(alusObjekt).setTimex3Attribute("endPoint", this.semValue);
						}				
					}
					// -----------------     anchorTimeID    -----------------------------
					if ((this.attribute).equalsIgnoreCase("anchorTimeID")){
						(alusObjekt).setTimex3Attribute("anchorTimeID", this.semValue);
					}
					// -----------------     temporalFunction    -------------------------
					if ((this.attribute).equalsIgnoreCase("temporalFunction")){
						(alusObjekt).setTimex3Attribute("temporalFunction", this.semValue);
					}					
					if (alusObjekt.getType() == TYYP.RECURRENCE){
						// -----------------        quant      -----------------------------				
						if ((this.attribute).equalsIgnoreCase("quant")){
							(alusObjekt).setTimex3Attribute("quant", this.semValue);						
						}
						// -----------------        freq       -----------------------------
						if ((this.attribute).equalsIgnoreCase("freq")){
							try {
								semValueAsInt = Integer.parseInt(this.semValue);
								if (semValueAsInt > 0){								
									String newVal = String.valueOf(semValueAsInt);
									if (this.granulaarsus != null && (this.granulaarsus).getAsISOdurationElement() != null){
										if ((this.granulaarsus).compareByCoarseRank(Granulaarsus.HOUR_OF_DAY) == 1){
											newVal = newVal + (this.granulaarsus).getAsISOdurationElement();
										} else {
											newVal = "T" + newVal + (this.granulaarsus).getAsISOdurationElement();
										}
									} else {
										newVal = newVal + "X";
									}
									(alusObjekt).setTimex3Attribute("freq", newVal);									
								}
							} catch (NumberFormatException e) {
							}
						}						
					}					
				}
			}
		}
		return alusObjekt;
	}
	
	//==============================================================================
	//   Yhe  semantikadefinitsiooni  t2iendamine  teise  abil
	//==============================================================================	
	/**
	 *    Proovib parameetrina etteantud semantikadefinitsioonist saadud v2ljadega t2ita selle 
	 *   semantikadefinitsiooni puuduolevaid v2lju. Kui k6ik l2heb h2sti, st parameetrina etteantud
	 *   semantikadefinitsiooni v2ljad on selles semantikadefinitsioonis tyhjad v2ljad, t2idab need
	 *   ning tagastab v22rtuse <tt>null</tt>. 
	 *   <p>
	 *   Kui aga m6ni parameetris olev v2li on selles semantikadefinitsioonis juba t2idetud, siis
	 *   t2idedakse selles objektis valjad, mida saab ylekirjutamata t2ita, ning lisaks luuakse uus
	 *   objekt, mis on koopia sellest, ainult et ylekirjutamisele kuuluvad v2ljad on yle kirjutatud.
	 *   Uus objekt luuakse ka alati, kui sisendiks antud semantikadefinitsioon on defineeritud
	 *   mingi mudeli alla kuuluvana.
	 *   <p>
	 *   V2lja <tt>semValueOnEbatapne</tt> v6rdlemisel tehakse erand: kahe v22rtuse erinemise korral
	 *   j2etakse alati alles vaid v22rtus <tt>true</tt> ning uut objekti ei looda.
	 */
	public SemantikaDefinitsioon taidaValjadVoiLooUusSemantikaDefinitsioon(SemantikaDefinitsioon semDef) {
		List<Method> setMeetodid = new ArrayList<Method>();
		List<Method> getMeetodid = new ArrayList<Method>();
		// 1) T2idame v2ljad, mida saab t2ita ilma ylekirjutamata;
		//    j2tame meelde ylekirjutatavad v2ljad (vastavad set-get paarid)
		
		// ------------ prioriteet
		if (semDef.getPriority() != null){
			if (this.getPriority() == null){
				this.setPriority(semDef.getPriority());
			} else {
				try {
					Method setMeetod = (this.getClass()).getMethod("setPriority", String.class);
					Method getMeetod = (semDef.getClass()).getMethod("getPriority");
					if (setMeetod != null && getMeetod != null){
						setMeetodid.add(setMeetod);
						getMeetodid.add(getMeetod);
					}
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}
			}
		}		
		// ------------ Seotud kontekst
		if (semDef.getSeotudKontekst() != null){
			if (this.getSeotudKontekst() == null){
				this.setSeotudKontekst(semDef.getSeotudKontekst());
			} else {
				try {
					Method setMeetod = (this.getClass()).getMethod("setSeotudKontekst", String.class);
					Method getMeetod = (semDef.getClass()).getMethod("getSeotudKontekst");
					if (setMeetod != null && getMeetod != null){
						setMeetodid.add(setMeetod);
						getMeetodid.add(getMeetod);
					}
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}
			}
		}		
		// ------------ Muudetav v2li/granulaarsus
		if (semDef.getGranulaarsus() != null){
			if (this.getGranulaarsus() == null){
				this.setGranulaarsus(semDef.getGranulaarsus());
			} else {
				try {
					Method setMeetod = (this.getClass()).getMethod("setGranulaarsus", Granulaarsus.class);
					Method getMeetod = (semDef.getClass()).getMethod("getGranulaarsus");
					if (setMeetod != null && getMeetod != null){
						setMeetodid.add(setMeetod);
						getMeetodid.add(getMeetod);
					}
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}
			}
		}
		// ------------ Muudetav v2li/granulaarsus (s6ne kujul)
		if (semDef.getGranulaarsusStr() != null){
			if (this.getGranulaarsusStr() == null){
				this.setGranulaarsusStr(semDef.getGranulaarsusStr());
			} else {
				try {
					Method setMeetod = (this.getClass()).getMethod("setGranulaarsusStr", String.class);
					Method getMeetod = (semDef.getClass()).getMethod("getGranulaarsusStr");
					if (setMeetod != null && getMeetod != null){
						setMeetodid.add(setMeetod);
						getMeetodid.add(getMeetod);
					}
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}
			}
		}		
		// ------------ Rakendatav operatsioon
		if (semDef.getOp() != null){
			if (this.getOp() == null){
				this.setOp(semDef.getOp());
			} else {
				try {
					Method setMeetod = (this.getClass()).getMethod("setOp", String.class);
					Method getMeetod = (semDef.getClass()).getMethod("getOp");
					if (setMeetod != null && getMeetod != null){
						setMeetodid.add(setMeetod);
						getMeetodid.add(getMeetod);
					}
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}
			}
		}
		// ------------ Omistatav v22rtus
		if (semDef.getSemValue() != null){
			if (this.getSemValue() == null){
				this.setSemValue(semDef.getSemValue());
			} else {
				try {
					Method setMeetod = (this.getClass()).getMethod("setSemValue", String.class);
					Method getMeetod = (semDef.getClass()).getMethod("getSemValue");
					if (setMeetod != null && getMeetod != null){
						setMeetodid.add(setMeetod);
						getMeetodid.add(getMeetod);
					}
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}
			}
		}
		// ------------ Omistatav lipik (kui v22rtus on ebat2pne v6i puudu)
		if (semDef.getSemLabel() != null){
			if (this.getSemLabel() == null){
				this.setSemLabel(semDef.getSemLabel());
			} else {
				try {
					Method setMeetod = (this.getClass()).getMethod("setSemLabel", String.class);
					Method getMeetod = (semDef.getClass()).getMethod("getSemLabel");
					if (setMeetod != null && getMeetod != null){
						setMeetodid.add(setMeetod);
						getMeetodid.add(getMeetod);
					}
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}
			}
		}
		// ------------ Direction
		if (semDef.getDirection() != null){
			if (this.getDirection() == null){
				this.setDirection(semDef.getDirection());
			} else {
				try {
					Method setMeetod = (this.getClass()).getMethod("setDirection", String.class);
					Method getMeetod = (semDef.getClass()).getMethod("getDirection");
					if (setMeetod != null && getMeetod != null){
						setMeetodid.add(setMeetod);
						getMeetodid.add(getMeetod);
					}
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}
			}
		}
		// ------------ Mudel
		if (semDef.getMudel() != null){
			if (this.getMudel() == null){
				this.setMudel(semDef.getMudel());
			} else {
				//  Kui on t2psustatud ka mudel, siis seda yle ei kirjuta, vaid sunnime peale uue
				// objekti loomise - et teised semdefid saaksid j2lle puhast (ilma mudelita varianti) 
				// kasutada ...
				try {
					Method setMeetod = (this.getClass()).getMethod("setMudel", String.class);
					Method getMeetod = (semDef.getClass()).getMethod("getMudel");
					if (setMeetod != null && getMeetod != null){
						setMeetodid.add(setMeetod);
						getMeetodid.add(getMeetod);
					}
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}				
			}
		}
		// ------------ Attribute
		if (semDef.getAttribute() != null){
			if (this.getAttribute() == null){
				this.setAttribute(semDef.getAttribute());
			} else {
				try {
					Method setMeetod = (this.getClass()).getMethod("setAttribute", String.class);
					Method getMeetod = (semDef.getClass()).getMethod("getAttribute");
					if (setMeetod != null && getMeetod != null){
						setMeetodid.add(setMeetod);
						getMeetodid.add(getMeetod);
					}
				} catch (SecurityException e) {
				} catch (NoSuchMethodException e) {
				}
			}
		}
		// ------------ V22rtuse ebat2psus
		if (semDef.isSemValueOnEbatapne() != this.isSemValueOnEbatapne()){
			// yks neist on true --> j22bki true kui l6plik v22rtus - vaidlustamisele ei kuulu
			this.setSemValueOnEbatapne(true);
		}

		// ------------ Intervalli implitsiitsus/eksplitsiitsus
		if (semDef.isExplicitPoint != this.isExplicitPoint){
			this.isExplicitPoint = semDef.isExplicitPoint;
		}
		
		if (setMeetodid.isEmpty()){
			return null;
		} else {
			// 2) Loome koopia ja t2idame v2ljad, mis j2id ylekirjutamisest hoidumisel t2itmata
			SemantikaDefinitsioon uus = this.clone();
			for (int i = 0; i < setMeetodid.size(); i++) {
				Method setMethod = setMeetodid.get(i);
				Method getMethod = getMeetodid.get(i);
				try {
					setMethod.invoke(uus, getMethod.invoke(semDef) );
				} catch (IllegalArgumentException e) {
				} catch (IllegalAccessException e) {
				} catch (InvocationTargetException e) {
				}
			}
			return uus;
		}
	}
	
	/**
	 *    Ankurdamisoperatsiooni korral: sulgeb etteantud ajaobjektis k6ik granulaarsused,
	 *   mis on v2iksemad ankru definitsioonis olevatest granulaarsustest;  Granulaarsuste
	 *   sulgemine  on  vajalik  v2ltimaks  ankruks  oleva ajav2ljendi semantika t2ielikku 
	 *   kaasatulemist uude ajav2ljendisse, mis v6ib p6hjustada valesid tulemusi;
	 *   <p>
	 *   NB! Kui ankrus on mitu erinevat j2medat granulaarsust, suletakse ainult k&otilde;ige
	 *   v&auml;iksemast granulaarsusest v2iksemaid. 
	 */
	public void sulgeGranulaarsusedVastavaltAnkrule(AjaObjekt ankruLahendus){
		if (this.granulaarsusStr != null){
			List <String> n6utudPosGranulaarsused = new LinkedList<String>();
			SemDefValjadeParsija.parsiN6utudJaKeelatudTunnused(null, 
															   this.granulaarsusStr, 
															   n6utudPosGranulaarsused, 
															   null);
			if (!n6utudPosGranulaarsused.isEmpty()){
				//
				//  1) Selgitame v2lja, milline granulaarsuseist on v2him
				//
				Granulaarsus vaikseimGran = Granulaarsus.CENTURY_OF_ERA;
				for (String granulaarsusStr : n6utudPosGranulaarsused) {
					Granulaarsus gran = Granulaarsus.getGranulaarsus(granulaarsusStr);
					if (gran != null && vaikseimGran.compareByCoarseRank(gran) == 1){
						vaikseimGran = gran;
					}
				}
				//
				//  2) Sulgeme alates v2ikseimast granulaarsusest;
				//
				ankruLahendus.closeGranularitiesBelow(vaikseimGran, 0);
			}
		}
	}
	
	
	//==============================================================================
	//   	K l o o n i m i n e
	//==============================================================================
	
	public SemantikaDefinitsioon clone() {
		SemantikaDefinitsioon uusSemDef = new SemantikaDefinitsioon();
		uusSemDef.seotudMustriOsa = this.seotudMustriOsa;
		uusSemDef.seotudKontekst  = this.seotudKontekst;
		uusSemDef.sonaKlass = this.sonaKlass;		
		uusSemDef.priority = this.priority;
		uusSemDef.priorityInt = this.priorityInt;
		uusSemDef.granulaarsus = this.granulaarsus;
		uusSemDef.granulaarsusStr = this.granulaarsusStr;		
		uusSemDef.op = this.op;
		uusSemDef.semValue = this.semValue;		
		uusSemDef.semLabel = this.semLabel;
		uusSemDef.direction = this.direction;
		uusSemDef.semValueOnEbatapne = this.semValueOnEbatapne;
		uusSemDef.mudel = this.mudel;
		uusSemDef.isExplicitPoint = this.isExplicitPoint;
		uusSemDef.attribute = this.attribute;
		return uusSemDef;
	}
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================	
	
	/**
	 *   Kontrollib, kas antud semantikadefinitsioon kuulub etteantud mudeli alla.
	 *   Kui etteantud mudel on komposiitmudel (st, <tt>semantikaMudel</tt> koosneb 
	 *   rohkem kui yhest mudelist), kontrollib, et v2hemalt yks etteantud mudelitest
	 *   vastab selle semantikadefinitsiooni mudelile (muutuja <tt>mudel</tt>). 
	 *   NB! Kui selle semantikadefinitsiooni mudel on m22ramata (<tt>mudel == null</tt>),
	 *   tagastab alati <tt>true</tt>. 
	 */
	public boolean vastabSemLeidmiseMudelile(String [] semantikaMudel){
		if (this.mudel != null){
			if (semantikaMudel != null){
				for (String semMudel : semantikaMudel) {
					if (semMudel.equals(this.mudel)){
						return true;
					}
				}
			}
			return false;
		}
		return true;
	}
	
	/**
	 *  Kontrollib, kas käesolev semantikareegel on <code>SET_attrib</code>-reegel, mis muudab
	 *  TIMEX3 välja <code>muudetavAtribuut</code>. 
	 */
	public boolean onSET_attrib(String muudetavAtribuut){
		return ( this.op != null && 
					(this.op).equals((SemantikaDefinitsioon.OP.SET_attrib).toString()) &&
						this.attribute != null &&
							(this.attribute).equalsIgnoreCase(muudetavAtribuut) );
	}
	
	/**
	 *   Kas antud semantikaarvutus vajab enne rakendamist konteksti kontrollimist?
	 *  Kui  ei  vaja,  on  ilmselt  tegu  ajavaljendikandidaadi  esmast,  st  lokaalset,
	 *  t2hendust edasiandva reegliga, mida v6ib koheselt rakendada, kui k6ik selle 
	 *  koostisosad (OP, FIELD, VALUE jms) on olemas.
	 */
	public boolean onKontekstistSoltuv(){
		return (this.seotudKontekst != null && (this.seotudKontekst).length() > 0);
	}
	
	/**
	 *   Kas antud semantikaarvutuse n2ol on tegemist ankurdamisoperatsiooni kirjeldusega?
	 *  Vajalik, kuna ankurdamisreegleid tuleb vaadelda eraldi tavalistest arvutusreeglitest.
	 */
	public boolean onAnkurdamisOperatsioon(){
		return (this.op != null && 
					((this.op).equals((OP.ANCHOR_TIMEX).toString()) || 
					 (this.op).equals((OP.ANCHOR_TIMEX_IN_SENTENCE).toString())	) );
	}
	
	public String getPriority() {
		return priority;
	}

	public void setPriority(String priority) {
		this.priority = priority;
		try {
			Integer i = Integer.valueOf(priority);
			this.priorityInt = i;
		} catch (Exception e) {
		}
	}	
	
	public String getSeotudMustriOsa() {
		return seotudMustriOsa;
	}

	public void setSeotudMustriOsa(String seotudMustriOsa) {
		this.seotudMustriOsa = seotudMustriOsa;
	}
	
	public String getSeotudKontekst() {
		return seotudKontekst;
	}
	
	/**
	 *  Leiab, kas <i>etteantudKontekst</i> (nt K6RVALFRAASI_GRAN v6i ANKURDAMINE_LABIVIIDUD) sisaldub
	 * antud semantikadefinitsiooni <i>seotudKontekst</i> osas. Sisaldumise korral tagastatakse vastav
	 * alamosa <i>seotudKontekst</i>-i seest, mittesisaldumise korral tagastatakse <tt>null</tt>.
	 * <p>
	 * Eeldab, et <i>seotudKontekst</i> sees kasutatakse erinevate n6utud kontekstide eraldajana m2rk <tt>&amp;</tt> 
	 */
	public String kasEtteantudKontekstSisaldubSeotudKontekstis(CONTEXT etteantudKontekst){
		if (this.seotudKontekst != null){
			String[] seotudKontekstid     = (this.seotudKontekst).split("(&amp;|&)");
			String   etteantudKontekstPos = etteantudKontekst.toString();
			String   etteantudKontekstNeg = "^"+etteantudKontekst.toString(); 
			for (int i = 0; i < seotudKontekstid.length; i++) {
				String seotudKontekstiOsa = TextUtils.trim(seotudKontekstid[i]);
				if (seotudKontekstiOsa.startsWith(etteantudKontekstPos) || 
						seotudKontekstiOsa.startsWith(etteantudKontekstNeg)){
					return seotudKontekstiOsa;
				}
			}
		}
		return null;
	}


	public void setSeotudKontekst(String seotudKontekst) {
		this.seotudKontekst = seotudKontekst;
	}

	public String getOp() {
		return op;
	}

	public void setOp(String op) {
		this.op = op;
	}	
	
	public String getSemLabel() {
		return semLabel;
	}

	public void setSemLabel(String semLabel) {
		this.semLabel = semLabel;
	}

	public Granulaarsus getGranulaarsus() {
		return granulaarsus;
	}

	public void setGranulaarsus(Granulaarsus granulaarsus) {
		this.granulaarsus = granulaarsus;
	}
	
	
	public String getGranulaarsusStr() {
		return this.granulaarsusStr;
	}

	public void setGranulaarsusStr(String granulaarsusStr) {
		this.granulaarsusStr = granulaarsusStr;
	}

	public String getSonaKlass() {
		return sonaKlass;
	}

	public void setSonaKlass(String sonaKlass) {
		this.sonaKlass = sonaKlass;
	}

	public boolean isSemValueOnEbatapne() {
		return semValueOnEbatapne;
	}

	public void setSemValueOnEbatapne(boolean semValueOnEbatapne) {
		this.semValueOnEbatapne = semValueOnEbatapne;
	}

	public String getSemValue() {
		return semValue;
	}

	public void setSemValue(String semValue) {
		this.semValue = semValue;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}	
	
	public String getMudel() {
		return mudel;
	}

	public void setMudel(String mudel) {
		this.mudel = mudel;
	}
	
	public void setIsExplicitPoint(String booleanAsStr){
		this.isExplicitPoint = (booleanAsStr.matches("(?i)true"));
	}
	
	public boolean isExplicitPoint(){
		return this.isExplicitPoint;
	}
	
	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}	
	
	//==============================================================================
	//   	C o m p a r e   b y    p r i o r i t y 
	//==============================================================================

	public int compareTo(SemantikaDefinitsioon o) {
		// V6rdleb priority-v22rtuste alusel
		// NB! (-1)-v22rtused on suuremad kui yksk6ik mis teised seadistatud v22rtused
		// Loogika: mida v2iksem priority, seda parem - enne rakendamist sorteeritakse
		//  semantikareeglid priority-v22rtuse kasvamise j2rjekorda ...
		return (this.priorityInt > o.priorityInt) ? (1) : ((this.priorityInt < o.priorityInt) ? (-1) : (0));
	}
	
	//==============================================================================
	//   	v a l j u n d
	//==============================================================================

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.seotudKontekst != null && (this.seotudKontekst).length() > 0){
			sb.append(" K ");
		}
		sb.append("< ");
		sb.append(this.op);
		sb.append("(");
		if (this.attribute != null){
			sb.append(this.attribute);
			sb.append(",");			
		}
		sb.append(this.granulaarsus);
		sb.append(",");
		sb.append(this.semValue);
		sb.append(",");
		sb.append(this.semLabel);
		if (this.direction != null){
			sb.append(",");
			sb.append(this.direction);			
		}
		if (this.mudel != null){
			sb.append(",");
			sb.append(this.mudel);			
		}		
		sb.append(") >");
		return sb.toString();
	}
}
