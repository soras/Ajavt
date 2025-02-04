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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;

import ee.ut.soras.ajavtV2.AjaTuvastaja;
import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.MustriTahis;
import ee.ut.soras.ajavtV2.mudel.TuvastamisReegel;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.Granulaarsus;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;
import ee.ut.soras.ajavtV2.mudel.sonamallid.SonaMall.TYYP;
import ee.ut.soras.ajavtV2.util.SemDefValjadeParsija;
import ee.ut.soras.ajavtV2.util.SemDefValjadeParsija.FORMAT_OF_VALUE;


/**
 *   Tuvastamisreegli juurde kuuluv fraasimuster. Sisuliselt on tegu ~automaadiga, mis kontrollib, kuidas
 *  sonade kaupa etteantav tekst rahuldab teatud sonamallide jarjendit ehk fraasi mustrit.
 *  <p>
 *  Tegemist on lihtsustatud l6pliku mitte-deterministliku automaadiga (NFA).
 * 
 *  @author Siim Orasmaa
 */
public class FraasiMusterFSM implements FraasiMuster {

	//==============================================================================
	//   	S t a a t i l i s e d    m u u t u j a d
	//==============================================================================
	
	/**
	 *   Mustri unikaalne ID;
	 */
	private String mustriID;
	
	/**
	 *   Fraasimustri sisuks olevad sonamallid.
	 */
	private List <SonaMall> sonaMallid;
	
	/**
	 *   &Auml;raj&auml;etavuse informatsioon iga sonamalli kohta. Kui sonamall on
	 *  &auml;raj&auml;etav, jaetakse see l6plikult eraldatud ajavaljendist valja.
	 *  <p>
	 *  Vaikimisi peaksid olema k6ik sonamallid mitte-&auml;raj&auml;etavad.  
	 */
	private List <Boolean> onArajaetav;
	
	/**
	 *    Tuvastamisreegel, mille alla antud fraasimuster kuulub. 
	 */
	private TuvastamisReegel tuvastamisReegel = null;
	
	//==============================================================================
	//   	A u t o m a a d i      m u u t u j a d
	//==============================================================================	

	/**
	 *    Siirded, mis viivad uutesse olekutesse sonamallide sobitamise kaudu. 
	 */
	private List<Transition> transitions      = null;
	/**
	 *    Nn tyhisiirded: viivad uutesse olekutesse ilma sobitamiseta - eeldusel, 
	 *    et ollakse vastavas lahteolekus. 
	 */
	private List<Transition> emptyTransitions = null;

	
	/**
	 *    Automaadi jooksvad olekud.
	 */
	private List<StateAndPath> currentStates = null;
	
	
	/**
	 *    Automaadi algolekud. 
	 */
	private List<Integer> startStates = null;
	/**
	 *    Automaadi l6ppolek. 
	 */
	private int endState              = -1;
	
	//==============================================================================
	//   	K o n s t r u k t o r i d 
	//==============================================================================
	
	/**
	 *   Loob uue fraasimustri. Eeldab, et listid <code>sonaMallid</code> ja <code>valikulisus</code> on
	 *   konsistentsed, st vordsed suuruselt ning <code>i</code>-s element yhes listis vastab 
	 *   <code>i</code>-ndale elemendile teises listis.
	 * @param valikulisus
	 */
	public FraasiMusterFSM(List <SonaMall> sonaMallid, 
						   List <Boolean> valikulisus,
						   List <Boolean> araJaetavus,
						   String mustriID){
		this.mustriID    = mustriID;
		this.sonaMallid  = sonaMallid;
		this.onArajaetav = araJaetavus;		
		buildAutomaton(sonaMallid, valikulisus);
	}

	//==============================================================================
	//   	A u t o m a a d i    k o o s t i s o s a d
	//==============================================================================
	
	/**
	 *   Siire, mis viib automaadi olekust <code>sourceState</code> olekusse 
	 *   <code>targetState</code>. Kui tegemist on tavalise siirdega, kontrollitakse 
	 *   enne oleku vahetamist, kas sisends6na rahuldab siirdega seotud s6namalli 
	 *   (<code>requiredMatch</code>). Kui tegemist on tyhisiirdega 
	 *   (<code>requiredMatch == null</code>), on oleku vahetamise ainsaks eelduseks, 
	 *   et ollakse sobivas l2hteolekus (<code>sourceState</code>). 
	 */
	private class Transition {
		private int sourceState        = -1;
		private SonaMall requiredMatch = null;
		private int targetState        = -1;
		
		public Transition(int source, int target, SonaMall requiredMatch) {
			this.sourceState = source;
			this.targetState = target;
			this.requiredMatch = requiredMatch;
		}
		
		public int attemptTransition(
				int source,
				AjavtSona sona,
				HashMap<String, MallileVastavus> kontrollitudSonaKlassid){
			if (source == this.sourceState){
				SonaMall kontrollitavMall = this.requiredMatch;
				if (kontrollitavMall == null){
					// null-siire: alati v6imalik sooritada ...
					return this.targetState;
				} else {
					// mitte-null siire: tuleb kontrollida s6na vastavust mallile
					MallileVastavus vastavus = MallileVastavus.EI_VASTA;
					if (kontrollitavMall.getTyyp() == TYYP.SONAKLASS){
						String klassiNimi = ((SonaKlass)kontrollitavMall).getNimi();
						if (kontrollitudSonaKlassid.containsKey( klassiNimi )){
							vastavus = kontrollitudSonaKlassid.get(klassiNimi);
						} else {
							vastavus = kontrollitavMall.vastabMallile(sona);
							kontrollitudSonaKlassid.put(klassiNimi, vastavus);
						}
					} else {
						vastavus = kontrollitavMall.vastabMallile(sona);
					}
					if (vastavus == MallileVastavus.VASTAB_LOPLIKULT){
						// T2ielik vastavus, siirdume uude olekusse
						return this.targetState;
					} else if (vastavus == MallileVastavus.EI_VASTA){
						// Mittevastavus
						return -1;
					} else {
						// Osalise vastavuse korral naaseme siirdeolekusse
						// NB! T2ieliku korrektsuse saavutamiseks tuleks 
						// kontrollida, millises j2rjekorras vastavused 
						// VASTAB_KESKOSA ning VASTAB_ALGUS saadakse!
						return this.sourceState;
					}
				}
			} else {
				return -1;
			}
		}
		
	}
	
	/**
	 *    Automaadi olek (<code>state</code>) ning selle olekuni j6udmise k2igus
	 *    tehtud positiivsed sobitamised (<code>pathOfMatches</code>). Positiivsete
	 *    sobitamiste alla kuuluvad ainult konkreetselt s6namalle rahuldanud s6nad,
	 *    tyhisiirete tulemused j22vad siit alt v2lja.
	 */
	private class StateAndPath {
		
		/** Olek, milleni on automaadi sobitamisel j6utud. */
		private int state = -1;
		/** Positiivsed sobitamised, mis on olekuni j6udes tehtud. <br>
		 *  Kujul: [sonamalli indeks, vastavuses sonad] */
		private HashMap<String, List<AjavtSona>> pathOfMatches = null;
		
		public StateAndPath(int state){
			this.state = state;			
		}
		
		public StateAndPath(int newState, StateAndPath prevStateAndPath, int sonaMalliIndeks, AjavtSona sona) {
			this.state = newState;
			if (prevStateAndPath.pathOfMatches != null){
				this.pathOfMatches = 
					new HashMap<String, List<AjavtSona>>(prevStateAndPath.pathOfMatches);				
			}
			if (sonaMalliIndeks != -1){
				this.addToPath(sonaMalliIndeks, sona);				
			}
		}
		
		public int getState(){
			return state;
		}
		
		public void addToPath(Integer key, AjavtSona sona){
			if (this.pathOfMatches == null){
				this.pathOfMatches = new HashMap<String, List<AjavtSona>>(); 
			}
			String strKey = key.toString();
			if ((this.pathOfMatches).containsKey(strKey)){
				if (sona != null){
					((this.pathOfMatches).get(strKey)).add(sona);					
				}
			} else {
				List<AjavtSona> list = new ArrayList<AjavtSona>();
				if (sona != null){
					list.add(sona);
				}
				(this.pathOfMatches).put(strKey, list);
			}
		}
		
		public HashMap<String, List<AjavtSona>> getPath(){
			return pathOfMatches;
		}
		
	}
	
	//==============================================================================
	//   	A u t o m a a d i   e h i t a m i n e
	//==============================================================================
	
	/**
	 *  Ehitab lihtsustatud mittedeterministliku automaadi, millel v6ib olla mitu 
	 *  algolekut. Naiteks fraasimustrile <code>IGA? JARGMINE? EELMINE? NADALAPAEV</code>
	 *  ehitatakse j2rgnev automaat:
	 *  
	 *  <pre>
	 *          IGA      JARGMINE    EELMINE      NADALAPAEV
	 *         _____      _____      _______      __________
	 *        /     \\   /     \\   /       \\  /          \\
	 *  -> (0)        (1)        (2)         (3)            ((4))
	 *        \     //   \      //  \       // 
	 *         -----      -----      -------
	 *    
	 *   (n)   = olek
	 *   ((m)) = lopp-olek
	 *    ____ = s6namalli sobitamise siire, \\ siirde suund
	 *    ---- = tyhisiire, // siirde suund
	 *  </pre>
	 *  Algolekuks on olek 0 ning k6ik olekud, millesse saab olekust 0 tyhisiirde kaudu.
	 * 
	 */
	private void buildAutomaton(List <SonaMall> sonaMallid, List <Boolean> valikulisus){
		// Automaat on praegu suhteliselt lineaarne: alati on sonaMallid.size()+1 
		// olekut, esimene olek [0] ning viimane [sonaMallid.size()]
		endState       = sonaMallid.size();
		// Algusolekud: olek 0 + { k6ik olekud, kuhu saab olekust 0 tyhi-siirdeid pidi }
		startStates    = new ArrayList<Integer>();
		startStates.add(0);		
		// Lisame v6imalikud siirded
		transitions      = new ArrayList<FraasiMusterFSM.Transition>();
		emptyTransitions = new ArrayList<FraasiMusterFSM.Transition>();
		for (int i = 0; i < sonaMallid.size(); i++) {
			// Esimest tyypi siire on lihtsalt j2rgnevate s6nade vahel
			SonaMall mall      = sonaMallid.get(i);
			Transition t = new Transition(i, i+1, mall);
			transitions.add(t);
			// Teist tyypi siire v6imaldab s6nu vahele j2tta
			boolean valikuline = valikulisus.get(i);
			if (valikuline){
				t = new Transition(i, i+1, null);
				emptyTransitions.add(t);				
			}
		}
		// Leiame olekud, kuhu saab algusolekust tyhisiirete abil; lisame need algusolekute hulka
		List<Integer> reachedStates = new ArrayList<Integer>();
		reachedStates.addAll(startStates);
		while (!reachedStates.isEmpty()){
			Integer state = reachedStates.remove(0);
			for (Transition trans : emptyTransitions) {
				// yritame sooritada tyhisiiret
				int resultState = trans.attemptTransition(state, null, null);
				if (resultState > -1 && resultState != state.intValue()){
					startStates.add( resultState );
					reachedStates.add( resultState );
				}
			}
		}
		
	}
	
	//==============================================================================
	//   	R a k e n d u s  : 
	//               m u s t r i g a    s o b i t a m i n e 
	//==============================================================================

	/**
	 *  Kontrollib, kas etteantud s&otilde;na vastab fraasimustri mingile osale. 
	 * Fraasimuster peab ise j&auml;rge, mitu vastavust ta juba on leidnud, ning kui 
	 * j&auml;rjestikku on olnud piisavalt palju positiivseid kontrollimistulemusi
	 * rahuldamaks kogu mustrit, konstrueeritakse uus ajavaljendikandidaat ning seotakse
	 * sonadega. 
	 */
	public void kontrolliMustrileVastavust(
			AjavtSona sona, 
			HashMap<String, MallileVastavus> kontrollitudSonaKlassid) throws Exception {
		
		// olekud, millesse saab jooksvatest olekutest mingite siirete kaudu
		ArrayList<StateAndPath> nextStates = new ArrayList<StateAndPath>();
		boolean endStateReached = false;
		
		// initsialiseerime vajadusel jooksvad olekud
		if (currentStates == null){
			currentStates = new ArrayList<StateAndPath>();
		}
		
		// lubame alati alustada k6igist algolekutest
		for (Integer startState : startStates) {
			StateAndPath initialState = new StateAndPath(startState);
			currentStates.add( initialState );
		}

		List<StateAndPath> reachedStates = new LinkedList<StateAndPath>();

		// Sooritame mitte-null siirded
		for (StateAndPath stateAndPath : currentStates) {
			int state = stateAndPath.getState();
			for (Transition trans : transitions) {
				// yritame siiret sooritada
				int resultState = trans.attemptTransition(state, sona, kontrollitudSonaKlassid);
				if (resultState > -1){
					if (resultState == endState){
						endStateReached = true;
					}
					StateAndPath newSP = new StateAndPath(resultState, stateAndPath, state, sona); 
					nextStates.add( newSP );
					reachedStates.add( newSP );
				}
			}
		}

		// Sooritame null-siirded
		while (!reachedStates.isEmpty()){
			StateAndPath stateAndPath = reachedStates.remove(0);
			Integer state = stateAndPath.getState();
			for (Transition trans : emptyTransitions) {
				// yritame siiret sooritada
				int resultState = trans.attemptTransition(state, sona, kontrollitudSonaKlassid);
				if (resultState > -1 && resultState != state.intValue()){
					StateAndPath newSP = new StateAndPath(resultState, stateAndPath, -1, null); 
					nextStates.add( newSP );					
					reachedStates.add( newSP );
					if (resultState == endState){
						endStateReached = true;
					}
				}
			}
		}
		if (endStateReached){
			// loome kandidaadid
			konstrueeriJaKinnitaEraldatudAjavaljendiKandidaadid(nextStates);
			// Eemaldame l6pp-oleku jooksvate olekute hulgast
			Iterator<StateAndPath> iterator = nextStates.iterator();
			while (iterator.hasNext()) {
				StateAndPath state = (StateAndPath) iterator.next();
				if (state.getState() == endState){
					iterator.remove();
				}
			}
		}
		// uued jooksvad olekud
		currentStates = nextStates;
	}
	
	
	//==============================================================================
	//       E r a l d a t u d    
	//                   a j a v a l j e n d i k a n d i d a a d i
	//                                         k o n s t r u e e r i m i n e 
	//==============================================================================
	
	/**
	 *   Loob ja tagastab eraldatud sonu katva ajavaljendikandidaadi. Juhul kui eraldamine
	 *   peaks mingil p&otilde;hjusel eba&otilde;nnestuma, tagastab vaartuse <tt>null</tt>.
	 */
	private AjavaljendiKandidaat konstrueeriJaKinnitaEraldatudAjavaljendiKandidaadid(ArrayList<StateAndPath> nextStates){
		for (StateAndPath stateAndPath : nextStates) {
			if (stateAndPath.getState() == endState){
				AjavaljendiKandidaat ajavaljendiKandidaat = null;
				HashMap<String, List<AjavtSona>> malleRahuldavadAlamkandidaadid = stateAndPath.getPath();
				// 1) Kogume kokku eraldatud sonad, paigutame teksti kulgemise jarjekorda				
				List<AjavtSona> eraldatudSonad = new ArrayList<AjavtSona>();
				for (int i = 0; i < endState; i++) {
					String strKey = String.valueOf(i);
					if (malleRahuldavadAlamkandidaadid.containsKey(strKey) && !(onArajaetav.get(i))){
						eraldatudSonad.addAll(malleRahuldavadAlamkandidaadid.get(strKey));
					}
				}
				if (!eraldatudSonad.isEmpty()){
					// 2) Loome eraldatud sonade kohta uue kandidaadi, lisame talle eraldatud semantilise 
					//    osa arvutusk2igu juhised
					ajavaljendiKandidaat = new AjavaljendiKandidaat();
					if (lisaKandidaadileSemantikaDefinitsioonid(ajavaljendiKandidaat, malleRahuldavadAlamkandidaadid)){
						// 3) Seome sonad ja kandidaadi
						AjaTuvastaja.seoSonadKandidaadiKylge(ajavaljendiKandidaat, eraldatudSonad);
					
						// 4) Seome ajavaljendiKandidaadiga tuvastamisreegli
						ajavaljendiKandidaat.lisaTuvastamisReegel(this.tuvastamisReegel);		
					}
					ajavaljendiKandidaat.setMustriID( this.mustriID );
				}
			}
		}
		return null;
	}
	
	/**
	 *   Alametapp ajavaljendikandidaadi konstrueerimisel - lisab semantika leidmise instruktsioonid. 
	 *   Meetodi alametapid:
	 *   <ol>
	 *    <li> Eraldab rahuldatud s6namallide semantilise osa (poolikut semantikat sisaldavad klotsid);
	 *    <li> T2idab lyngad semantilises osas tuvastamisreegli all olevate semantikadefinitsioonide abil;
	 *    <li> T2idab lyngad semantilises osas teiste semantikadefinitsioonide abil (kui nii on ette n2htud);
	 *    <li> Kirjutab kompleksseid granulaarsuseid (nt DATE, HOUR_OF_DAY) sisaldavad semdef-id lahti v2ikseid 
	 *         granulaarsuseid sisaldavateks semdef-ideks;
	 *    <li> Sorteerib semantika arvutuse instruktsioonid prioriteetsuse j2rgi ja kinnitab kandidaadi kylge.
	 *   </ol>
	 *   Tagastab <tt>true</tt>, kui vahemalt yks semantikadefinitsioon kinnitati ajavaljendi kylge. (Filtreerimine
	 *  voib kaasa tuua selle, et reaalselt ei jaa kinnitamiseks alles yhtki semantikadefinitsiooni).
	 */
	private boolean lisaKandidaadileSemantikaDefinitsioonid(AjavaljendiKandidaat ajav, HashMap<String, List<AjavtSona>> malliRahuldavadAlamFraasid){
		boolean eraldatiVahemaltYksSemDef = false;
		// 1) Eraldame semantilise osa rahuldatud s6namallidest, yhtlasi leiame, millised
		//    mallid said rahuldatud ja millised mitte
		List<SemantikaDefinitsioon>           semDefidSonaMallidest = new ArrayList<SemantikaDefinitsioon>();
		HashMap<Integer, String> semDefidSonaMallidestIndeksMustris = new HashMap<Integer, String>();
		HashMap<String, String>                rahuldatudMustriosad = new HashMap<String, String>();
		for (int i = 0; i < sonaMallid.size(); i++) {
			String voti = String.valueOf(i);
			if (malliRahuldavadAlamFraasid.containsKey(voti)){
				List<AjavtSona> malliRahuldavAlamFraas = malliRahuldavadAlamFraasid.get(voti);
				AjavtSona viimaneSobitunudSona = malliRahuldavAlamFraas.get(malliRahuldavAlamFraas.size()-1);
				List<SemantikaDefinitsioon> semDefs =  
					(sonaMallid.get(i)).tagastaMalliSemantilineOsa(viimaneSobitunudSona);
				if (semDefs != null && !semDefs.isEmpty()){
					if ((semDefs.get(0)).getSonaKlass() != null){
						rahuldatudMustriosad.put( (semDefs.get(0)).getSonaKlass(), "1" );
						// Juhuks, kui peaks olema mitu yhe s6naklassi liiget, lisame ka variandi,
						// kus on nime l6ppu lisatud eristamise tarbeks indeks ...
						rahuldatudMustriosad.put( (semDefs.get(0)).getSonaKlass() + "_" + voti, "1" );
						// J2tame meelde, milline 'semDefidSonaMallidest' element on seotud millise
						// 'voti'-elemendiga
						for (int j = 0; j < semDefs.size(); j++) {
							semDefidSonaMallidestIndeksMustris.put(
									Integer.valueOf( semDefidSonaMallidest.size() + j ),
									voti);								
						}
					}
					semDefidSonaMallidest.addAll(semDefs);					
				}
			}
		}
		
		// 2) T2idame lyngad olemasolevates semantikadefinitsioonides, vajadusel lisame ka uued 
		//    semantikadefinitsioonid
		if (this.tuvastamisReegel != null){
			List<SemantikaDefinitsioon> semDefidUued = 
				(this.tuvastamisReegel).taiendaSonaMallidestSaadudSemDeffe(
											malliRahuldavadAlamFraasid, 
											rahuldatudMustriosad, 
											semDefidSonaMallidest, 
											semDefidSonaMallidestIndeksMustris,
											this.sonaMallid);
			semDefidSonaMallidest.addAll(semDefidUued);
		}
			
		// 3) Asendame viited teiste semantikadefinitsioonide alamosadele nende 6igete
		// v22rtustega ...
		for (SemantikaDefinitsioon semDefinitsioon1 : semDefidSonaMallidest) {
			// ****** semValue viited
			FORMAT_OF_VALUE formatOfValue = 
				SemDefValjadeParsija.detectFormatOfValue( semDefinitsioon1.getSemValue() );
			// Tegemist on viitega teise semantikadefinitsiooni semValue osale...
			if (formatOfValue == FORMAT_OF_VALUE.REF_TO_VAL){
				String refferedName = (semDefinitsioon1.getSemValue()).split(":")[1];
				for (SemantikaDefinitsioon semDefinitsioon2 : semDefidSonaMallidest) {
					if (semDefinitsioon2.getSonaKlass() != null && 
							semDefinitsioon2.getSonaKlass().equals(refferedName) &&
								semDefinitsioon2.getSemValue() != null){
						semDefinitsioon1.setSemValue(semDefinitsioon2.getSemValue());
					}
				}
			}
			// Tegemist on viitega teise semantikadefinitsiooni semLabel osale...
			if (formatOfValue == FORMAT_OF_VALUE.REF_TO_LAB){
				String refferedName = (semDefinitsioon1.getSemValue()).split(":")[1];
				for (SemantikaDefinitsioon semDefinitsioon2 : semDefidSonaMallidest) {
					if (semDefinitsioon2.getSonaKlass() != null && 
							semDefinitsioon2.getSonaKlass().equals(refferedName) && 
								semDefinitsioon2.getSemLabel() != null){
						semDefinitsioon1.setSemValue(semDefinitsioon2.getSemLabel());
					}
				}
			}			
		}
		
		// 4) Yhendame hulgad ning asendame tehislikud v22rtused HOUR_OF_DAY, TIME, DATE nende 
        //    ekvivalentsete lahtikirjutustega. 
		ListIterator<SemantikaDefinitsioon> listIterator = semDefidSonaMallidest.listIterator();
		while (listIterator.hasNext()) {
			SemantikaDefinitsioon semDefinitsioon = (SemantikaDefinitsioon) listIterator.next();
			FORMAT_OF_VALUE formatOfValue   = SemDefValjadeParsija.detectFormatOfValue(semDefinitsioon.getSemValue());
			// Kirjutame semValue osa lahti...
			if (semDefinitsioon.getGranulaarsus() == Granulaarsus.HOUR_OF_DAY || 
					(semDefinitsioon.getSemLabel() == null     &&
					 formatOfValue != FORMAT_OF_VALUE.UNSET    &&
					 formatOfValue != FORMAT_OF_VALUE.FRACTION &&
					 formatOfValue != FORMAT_OF_VALUE.INTEGER  &&
					 formatOfValue != FORMAT_OF_VALUE.CONSTANT)){
				List<SemantikaDefinitsioon> lahtikirjutatudDefinitsioonid = 
						SemDefValjadeParsija.kirjutaLahtiSemantikadefinitsioon(semDefinitsioon, 
																			   formatOfValue);
				listIterator.remove();
				for (SemantikaDefinitsioon lahtiKirjutatudSemDef : lahtikirjutatudDefinitsioonid) {
					listIterator.add(lahtiKirjutatudSemDef);
				}
			}
			
		}
		
		// 5) Sorteerime elemendid prioriteetsuse j2rgi
		Collections.sort(semDefidSonaMallidest);
		
		// 6) Kinnitame elemendid ajavaljendikandidaadi kylge
		for (int i = 0; i < semDefidSonaMallidest.size(); i++) {
			SemantikaDefinitsioon semDefinitsioon = semDefidSonaMallidest.get(i);
			ajav.lisaSemantikaEhitusklots(semDefinitsioon);
			eraldatiVahemaltYksSemDef = true;
		}
		
		// 7) Lisame ajavaljendi kylge mustritahised
		if (eraldatiVahemaltYksSemDef && 
				this.tuvastamisReegel != null && 
					(this.tuvastamisReegel).kasLeidubMustriTahiseid()){
			List<MustriTahis> mustriTahised = 
				(this.tuvastamisReegel).parsiRahuldatudMustriosaleVastavadMustriTahised(
																		malliRahuldavadAlamFraasid, 
																		rahuldatudMustriosad);
			ajav.lisaMustriTahised( mustriTahised );
		}
		return eraldatiVahemaltYksSemDef;
	}
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================

	public String getMustriID() {
		return mustriID;
	}

	public void setMustriID(String mustriID) {
		this.mustriID = mustriID;
	}	
	
	public List<SonaMall> getSonaMallid() {
		return sonaMallid;
	}

	public void setTuvastamisReegel(TuvastamisReegel tuvastamisReegel) {
		this.tuvastamisReegel = tuvastamisReegel;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		if (this.mustriID != null){
			sb.append(this.mustriID);
			sb.append(" ");
		}
		sb.append("< ");
		for (int i = 0; i < sonaMallid.size(); i++) {
			SonaMall mall = sonaMallid.get(i);
			sb.append(mall);
			//Boolean ongiValikuline = (this.onValikuline).get(i);			
			//if (ongiValikuline.booleanValue()){
			//	sb.append("?");
			//}
			sb.append(" ");
		}
		sb.append(">");
		return sb.toString();
	}
	
}
