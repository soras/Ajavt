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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.FraasisPaiknemiseKoht;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat.ASTE;
import ee.ut.soras.ajavtV2.util.TextUtils;

/**
 * Jada, mis sisaldab tekstis j2rjestikku paiknevaid ja potentsiaalselt yheks fraasiks liidetavaid 
 * ajav2ljendikandidaate. Kasutusel liitumisreeglite rakendamise etapis vahetulemuste 
 * j22dvustamisel;
 * 
 * @author Siim Orasmaa
 */
public class PotentsLiidetavateKandidaatideJada {

	private List<AjavaljendiKandidaat>  kandidaadid = null;
	
	private int startPosition = -1;
	private int endPosition   = -1;

	/**
	 *   Konstruktor, mis loob tyhja potentsiaalselt liidetavate kandidaatide jada.
	 */
	public PotentsLiidetavateKandidaatideJada() {
		this.kandidaadid = new ArrayList<AjavaljendiKandidaat>();
	}
	
	//==============================================================================
	//  K 6 i g i    v 6 i m a l i k e    j a d a d e    g e n e r e e r i m i n e   
	//  s i s e n d t e k s t i     p 6 h j a l
	//==============================================================================
	
	/**
	 *   Genereerib antud (ajav2ljendikandidaatidega m2rgendatud) sisendtektsi p6hjal
	 *   k6ikv6imalikud jadad potentsiaalselt yheks fraasiks liidetavatest kandidaatidest.
	 *   Iga jada sisaldab tekstis k6rvutipaiknevaid kandidaate (v2hemalt kaht kandidaati) 
	 *   nende tekstis paiknemise j2rjekorras. Tagastab leitud jadad. <br>
	 *   
	 *   N2ide: Tekstis j2rjestikkupaiknevatest kandidaatidest <tt>"kell 8" "t2na" "hommikul"</tt> 
	 *   genereeritakse jadad:<br>
	 *   <tt>{"kell 8" "t2na"}</tt> <br>
	 *   <tt>{"t2na" "hommikul"}</tt> <br>
	 *   <tt>{"kell 8" "t2na" "hommikul"}</tt> <br>
	 */
	public static List<PotentsLiidetavateKandidaatideJada> genereeriPotentsJadad( List<AjavtSona> sonad ) throws Exception {
		List<PotentsLiidetavateKandidaatideJada> koikJadad = 
				new ArrayList<PotentsLiidetavateKandidaatideJada>();
		// Hoiame siin vahetulemust (selleks, et ei peaks igal sammul k6iki tekstist leitud
		// jadasid l2bi vaatama)
		List<PotentsLiidetavateKandidaatideJada> vahetulemJadad = null;
		//
		//  1) Genereerime k6ik potentsiaalsed jadad (m6ned koosnevadki vaid yhest ajav2ljendist)
		//
		for (int i = 0; i < sonad.size(); i++) {
			AjavtSona sona = sonad.get(i);
			if ( sona.onSeotudMoneAjavaljendiKandidaadiga() && vahetulemJadad == null){
				// Alustame vahetulemuste kogumist;
				// Lisame yhe tyhja jada (selle abil saab alati uusi jadasid alustada);
				vahetulemJadad = new ArrayList<PotentsLiidetavateKandidaatideJada>();
				vahetulemJadad.add( new PotentsLiidetavateKandidaatideJada() );				
			} else if (!sona.onSeotudMoneAjavaljendiKandidaadiga() && vahetulemJadad != null){
				// Lisame vahetulemused k6igi tulemuste hulka;
				// L6petame vahetulemuste kogumise;
				if (vahetulemJadad.size() > 1){
					koikJadad.addAll( vahetulemJadad.subList(1, vahetulemJadad.size()) );					
				}
				vahetulemJadad = null;
			}
			if ( sona.onSeotudMoneAjavaljendiKandidaadiga() ){
				List<FraasisPaiknemiseKoht> ajavaljendiFraasides = 
						sona.getAjavaljendiKandidaatides();
				List<AjavaljendiKandidaat> ajavaljendiKandidaadid = 
						sona.getAjavaljendiKandidaadid();
				for ( int j = 0; j < ajavaljendiKandidaadid.size(); j++ ) {
					FraasisPaiknemiseKoht fpk = ajavaljendiFraasides.get(j);
					if ( fpk == FraasisPaiknemiseKoht.AINUSSONA ||
						 fpk == FraasisPaiknemiseKoht.ALGUSES ){
						// Iga kandidaadi puhul: kui see alustab uut fraasi, proovime
						// seda lisada olemasolevate jadade otsa
						AjavaljendiKandidaat k = ajavaljendiKandidaadid.get(j);
						List<PotentsLiidetavateKandidaatideJada> uuedJadad = 
								new ArrayList<PotentsLiidetavateKandidaatideJada>();
						// Kontrollime k6iki olemasolevaid jadasid:
						for (PotentsLiidetavateKandidaatideJada jada : vahetulemJadad) {
							// Kui antud jada v6ib liita m6ne olemasoleva jada otsa
							// NB! Alati tekitatakse v2hemalt yks jada, mis koosneb 
							// ainult sellest ajav2ljendikandidaadist  
							if ( jada.kasSaabLaiendadaAjavaljendiKandidaadiga(k) ){
								uuedJadad.add( jada.laiendaUueksJadaks(k) );
							}
						}
						if (!uuedJadad.isEmpty()){
							vahetulemJadad.addAll( uuedJadad );
						}
					}
				}
			}
		}
		// Tyhjendame vahetulemuste puhvri
		if (vahetulemJadad != null){
			// Lisame vahetulemused k6igi tulemuste hulka;
			if (vahetulemJadad.size() > 1){
				koikJadad.addAll( vahetulemJadad.subList(1, vahetulemJadad.size()) );					
			}
		}
		//
		//  2) Eemaldame jadad, mis koosnevad vaid yhest ajav2ljendikandidaadist
		//
		Iterator<PotentsLiidetavateKandidaatideJada> iterator = koikJadad.iterator();
		while ( iterator.hasNext() ) {
			PotentsLiidetavateKandidaatideJada jada = 
					(PotentsLiidetavateKandidaatideJada) iterator.next();
			if ( jada.getKandidaadid() == null || (jada.getKandidaadid()).size() < 2 ){
				iterator.remove();
			}
		}
		// DEBUG:
		//for (PotentsLiidetavateKandidaatideJada jada : koikJadad) {
		//	System.out.println( jada.toString() );			
		//}
		return koikJadad;
	}
	
	//==============================================================================
	//   L i i t u m i s r e e g l i t e l e     v a s t a v u s e  
	//   k o n t r o l l
	//==============================================================================
	
	/**
	 *   Filtreerib antud <tt>jada</tt>-de kollektsiooni antud liitumisreeglite abil ning j2tab 
	 *   alles vaid sellised jadad, mille k6igi elemendipaaride (st kandidaadipaaride) 
	 *   k6rvutipaiknemine on lubatud liitumisreeglite alusel; <br><br>
	 *   
	 *   N2ide: Kui meil on jadad <tt>{{"kell 8" "t2na"}, {"t2na" "hommikul"}, {"kell 8" "t2na" "hommikul"}}</tt>,  
	 *   vastavate jadadega seotud mustrit2hised <tt>{{"A" "B"}, {"B" "C"}, {"A" "B" "C"}}</tt> ja
	 *   (mustrit2histega m22ratud) liitumisreeglite hulk <tt>{{"B" "C"}}</tt>, 
	 *   siis j22b jadadesse p2rast filtreerimist alles vaid <tt>{{"t2na" "hommikul"}}.</tt><br><br>
	 *   
	 *   NB! Praegune implementatsioon eeldab, et k6ik liitumisreeglid on vaid yle 
	 *   kandidaadipaaride, st kontrollivad vaid  kahe kandidaadi k6rvutipaiknemist.
	 *   Liitumisreegleid, mis kirjeldavad rohkem kui kaht kandidaati ei arvestata;  
	 */
	public static void filtreeriPotentsJadadsidLiitumisReegliteJargi( List<PotentsLiidetavateKandidaatideJada> jadad,
																		List<LiitumisReegel> liitumisReeglid ) throws Exception {
		//
		//  1. V6tame v2lja k6ik fraas-tasemel liitumisreeglid
		//
		List<LiitumisReegel> fraasiksLiitumisReeglid =
				new ArrayList<LiitumisReegel>( liitumisReeglid );
		Iterator<LiitumisReegel> iteraator = fraasiksLiitumisReeglid.iterator();
		while (iteraator.hasNext()) {
			LiitumisReegel liitumisReegel = (LiitumisReegel) iteraator.next();
			if (!(liitumisReegel.getYhendamiseAste() == ASTE.YHENDATUD_FRAASINA)){
				iteraator.remove();
			}
		}
		//
		//  2. Eraldame k6rvutipaiknevate kandidaatide liitumist m22ravad mustrit2hised
		//
		HashMap<String, String> vabaJarjekorragaPaarid = new HashMap<String, String>();
		HashMap<String, String> fikseeritudJarjekorragaPaarid = new HashMap<String, String>();
		for (LiitumisReegel liitumisReegel : fraasiksLiitumisReeglid) {
			if (!liitumisReegel.onFikseeritudJarjekord()){
				// Paarid, mis v6ivad yhe fraasi piires paikneda suvalises j2rjekorras
				//         ehk lubatud nii AB kui ka BA; 
				String[] mustriTahised = liitumisReegel.getMustriTahised();
				String v6ti = TextUtils.looMustriTahisteVoti( 
						Arrays.asList(mustriTahised), true );
				if (v6ti != null && v6ti.length() > 0){
					if (liitumisReegel.isTapseltKorvuti()){
						// Tapselt k6rvuti, st lynki ei tohi vahel olla ( ainult AB ja BA )
						vabaJarjekorragaPaarid.put(v6ti, "TK");
					} else {
						// Lyngad lubatud ( st nii AB ja BA   kui ka  A_B ja B_A )
						vabaJarjekorragaPaarid.put(v6ti, "1");
					}
				}
			} else {
				// Paarid, mis v6ivad yhe fraasi piires paikneda vaid fikseeritud j2rjekorras ehk vaid AB; 
				String[] mustriTahised = liitumisReegel.getMustriTahised();
				String v6ti = TextUtils.looMustriTahisteVoti( 
						Arrays.asList(mustriTahised), false );
				if (v6ti != null && v6ti.length() > 0){
					if (liitumisReegel.isTapseltKorvuti()){
						// Tapselt k6rvuti, st lynki ei tohi vahel olla ( ainult AB )
						fikseeritudJarjekorragaPaarid.put(v6ti, "TK");
					} else {
						// Lyngad lubatud ( st nii AB  kui ka  A_B )
						fikseeritudJarjekorragaPaarid.put(v6ti, "1");
					}
				}
			}
		}
		//
		//  3. Otsustame liitumisreeglite alusel, millised jadad v6iks liita; 
		//     Yhtlasi kustutame jadad, mis liitumisreegleid tervikuna ei rahuldanud;
		//
		Iterator<PotentsLiidetavateKandidaatideJada> iterator = jadad.iterator();
		while ( iterator.hasNext() ) {
			boolean toDelete = false;
			PotentsLiidetavateKandidaatideJada jada = 
					(PotentsLiidetavateKandidaatideJada) iterator.next();
			List<AjavaljendiKandidaat> jadaKandidaadid = jada.getKandidaadid();
			if (jadaKandidaadid != null && !jadaKandidaadid.isEmpty()){
				HashMap<Integer, Boolean> voibYhendada = new HashMap<Integer, Boolean>();
				//
				// 3.1. Kontrollime k6rvutipaiknevate yhendatavust
				//
				for ( int i = 0; i < jadaKandidaadid.size(); i++ ) {
					if (i+1 < jadaKandidaadid.size()){
						AjavaljendiKandidaat kandidaat1 = jadaKandidaadid.get(i);
						AjavaljendiKandidaat kandidaat2 = jadaKandidaadid.get(i+1);
						HashMap<String, String> kasutatudTahised = new HashMap <String, String>();
						boolean v = (kandidaat1.voibYhendadaMustriTahisteJargi(
									 kandidaat2, vabaJarjekorragaPaarid, kasutatudTahised, false, true) ||
									 kandidaat1.voibYhendadaMustriTahisteJargi(
									 kandidaat2, fikseeritudJarjekorragaPaarid, kasutatudTahised, true, true));
						voibYhendada.put( i, v );
					}
				}
				//
				// 3.2. Kontrollime nn lynkadega yhendatavust ( aga ainult siis, kui leidus lynki )
				//
				if (voibYhendada.containsValue(false)){
					// 3.2.1. Leiame k6ik yhendatavatest moodustatavad grupid, nt:  
					//          kui eelnevalt lubati yhendada:  A+B   C+D+E+F    G  
					//                         siis grupid on: (A B) (C D E F)  (G) 
					List<List<AjavaljendiKandidaat>> alamJadad = new ArrayList<List<AjavaljendiKandidaat>>();
					List<AjavaljendiKandidaat> alamJada = new ArrayList<AjavaljendiKandidaat>();
					for ( int i = 0; i < jadaKandidaadid.size(); i++ ) {
						AjavaljendiKandidaat ak = jadaKandidaadid.get(i);
						alamJada.add(ak);
						if (i+1 < jadaKandidaadid.size() && !voibYhendada.get(i)){
							alamJadad.add(alamJada);
							alamJada = new ArrayList<AjavaljendiKandidaat>();
						}
					}
					if (!alamJada.isEmpty()){
						alamJadad.add(alamJada);
					}
					alamJada = null;
					// 3.2.2. Itereerime yle k6rvutipaiknevate gruppide ja proovime lynkasid lubavaid reegleid
					int i = 0;
					for ( int a = 0; a < alamJadad.size(); a++ ) {
						if (a+1 < alamJadad.size()){
							List<AjavaljendiKandidaat> alamJada1 = alamJadad.get(a);
							List<AjavaljendiKandidaat> alamJada2 = alamJadad.get(a+1);
							i += alamJada1.size();
							// Sanity check
							if (voibYhendada.get(i-1)){
								throw new Exception(
									"LynkasidLubavateLiitumisReegliteRakendamine: Positsioonilt "+String.valueOf(i-1)+" ei leitud grupipiiri, kuigi oleks oodanud! " );
							}
							for (AjavaljendiKandidaat kandidaat1 : alamJada1) {
								for (AjavaljendiKandidaat kandidaat2 : alamJada2) {
									HashMap<String, String> kasutatudTahised = new HashMap <String, String>();
									boolean v = (kandidaat1.voibYhendadaMustriTahisteJargi(
												 kandidaat2, vabaJarjekorragaPaarid, kasutatudTahised, false, false) ||
												 kandidaat1.voibYhendadaMustriTahisteJargi(
												 kandidaat2, fikseeritudJarjekorragaPaarid, kasutatudTahised, true, false));
									// J22dvustame tulemuse vaid juhul, kui antud paari puhul on lubatud kaks k6rvuti-
									// paiknevat gruppi yhendada ...
									//  (negatiivset tulemust ei j22dvusta: sellel oleks oht positiivne yle kirjutada)
									if (v){
										voibYhendada.put( i-1, v );
									}
								}	
							}
						}
					}
				}
				if (voibYhendada.containsValue(false)){
					// Leidsime paari, mida yhendada ei v6i: j2relikult ei v6i yhendada tervet jada
					toDelete = true;
					//System.out.println( "-----"+jada.toString() );
				}
			} else {
				// Tyhijada kuulub kustutamisele
				toDelete = true;
			}
			if ( toDelete ){
				iterator.remove();
				//System.out.println( "-----"+jada.toString() );
			}
		}
		// DEBUG:
		//System.out.println( "==========================================" );
		//for (int i = 0; i < jadad.size(); i++) {
		//	System.out.println( String.valueOf(i)+". " + jadad.get(i) );
		//}
		//System.out.println( "==========================================" );
	}
   
	//==============================================================================
	//   A j a v a h e m i k e   k o o s t i s o s a d e   s 2 i l i t a m i s e  
	//   h e u r i s t i k
	//==============================================================================
	
	/**
	 *   Filtreerib antud <tt>jada</tt>-de kollektsiooni ning kustutab: 1) jadad, mis h6lmavad
	 *   yhekorraga nii potentsiaalset vahemiku algust kui ka l6ppu, 2) jadad, mis kuuluvad
	 *   potentsiaalset vahemiku algust ja l6ppu h6lmavate jadade sisse, ei sobi yhele j2rgnevaist
	 *   mustreist alamjadadeks:<br><br>
	 *           (1)  ST+NI,          // Nt: t2naseST homseNI<br>
	 *           (2)  ...+ST+...+NI   // Nt: t2na hommikuST homme 6htuNI<br>
	 *           (3)  ST+...+NI+...   // Nt: yheksaST hommikul kymneNI 6htul;<br><br>
	 *   
	 *   N2ide: Kui meil on jadad <tt>{{t2na hommikust}, {homme 6htuni}, {t2na hommikust homme},
	 *   {hommikust homme 6htuni}, {t2na hommikust homme 6htuni}}</tt>, siis alles j22vad vaid
	 *   jadad, millel on l6pus <tt>-st</tt> v6i <tt>-ni</tt>: <tt>{{t2na hommikust}, 
	 *   {homme 6htuni}}</tt> (kuna ylemjada j2rgib mustrit (2) ning need sobivad selle mustri alamjadadeks).<br><br>
	 */
	public static void filtreeriPotentsJadadsidStNiJargi( List<PotentsLiidetavateKandidaatideJada> jadad ){
		//
		// 1) Leiame k6ik jadad, mis tuleks kustutada: jadad mis h6lmavad nii potentsiaalset vahemiku algust
		//    kui ka l6ppu, ning nende sees olevad jadad, mis t6en2oliselt teeksid vahemiku katki ...
		//
		HashSet<PotentsLiidetavateKandidaatideJada> toDelete = 
				new HashSet<PotentsLiidetavateKandidaatideJada>();
		Iterator<PotentsLiidetavateKandidaatideJada> iterator1 = jadad.iterator();
		while ( iterator1.hasNext() ) {
			PotentsLiidetavateKandidaatideJada jada1 = 
					(PotentsLiidetavateKandidaatideJada) iterator1.next();
			List<AjavtSona> ajavtSonad1 = jada1.getAjavtSonad();
			//  Teeme kindlaks, kas antud jada sees on nii vahemiku algus kui ka l6pp
			int vahemAlgus = -1;
			int vahemLopp  = -1;
			for (int i = 0; i < ajavtSonad1.size(); i++) {
				AjavtSona ajavtSona = ajavtSonad1.get(i);
				if (ajavtSona.isOnPotentsiaalneVahemikuAlgus()){
					vahemAlgus = i;
				}
				else if (ajavtSona.isOnPotentsiaalneVahemikuLopp()){
					vahemLopp = i;
				}
			}
			if (vahemAlgus != -1 && vahemLopp != -1){
				//  Kui leidsime jada, kus on olemas nii vahemiku algus kui l6pp:
				//   A. Teeme kindlaks potentsiaalse vahemiku tyybi; Eeldame, et 
				//      v6imalikud on vaid kolm tyypi:
				//     1)  ST+NI,          // Nt: t2naseST homseNI
				//     2)  ...+ST+...+NI   // Nt: t2na hommikuST homme 6htuNI
				//     3)  ST+...+NI+...   // Nt: yheksaST hommikul kymneNI 6htul
				//
				int vahemiku_tyyp = 0;
				if (vahemLopp == ajavtSonad1.size()-1 && vahemAlgus > 0){
					// Lisakitsendus: m6lemad jupid koosnevad v2hemalt kahest s6nast
					if (vahemLopp - vahemAlgus > 1){
						vahemiku_tyyp = 2;
					}
				} else if (vahemLopp < ajavtSonad1.size()-1 && vahemAlgus == 0){
					// Lisakitsendus: m6lemad jupid koosnevad v2hemalt kahest s6nast
					if (vahemLopp - vahemAlgus > 1){
						vahemiku_tyyp = 3;
					}
				}  else if (vahemLopp == ajavtSonad1.size()-1 && vahemAlgus == 0){
					// Lisakitsendus: ongi t2pselt kaks s6na
					if (vahemLopp - vahemAlgus == 1){
						vahemiku_tyyp = 1;
					}
				}
				
				//   B. Leiame k6ik vahemik-jada alamjadad
				if (jada1.startPosition == -1 || jada1.endPosition == -1){
					jada1.updateStartEndPositions();					
				}
				int j = 0;
				while (j < jadad.size()){
					PotentsLiidetavateKandidaatideJada jada2 = 
							(PotentsLiidetavateKandidaatideJada) jadad.get(j);
					if (jada2.startPosition == -1 || jada2.endPosition == -1){
						jada2.updateStartEndPositions();						
					}
					if ((jada2.startPosition > jada1.startPosition &&
						 jada2.endPosition <= jada1.endPosition) ||
						(jada2.startPosition >= jada1.startPosition &&
						 jada2.endPosition < jada1.endPosition)){
						// jada2 on t2ielikult jada1 sees: otsustame, kas tuleks kustutada
						//  1) Teeme alamjadas kindlaks vahemiku algust2hise ja l6put2hise 
						//     asukoha:
						List<AjavtSona> ajavtSonad2 = jada2.getAjavtSonad();
						int alamJadaVahemAlgus = -1;
						int alamJadaVahemLopp  = -1;
						for (int i = 0; i < ajavtSonad2.size(); i++) {
							AjavtSona alamSona = ajavtSonad2.get( i );
							if (alamSona.isOnPotentsiaalneVahemikuAlgus()){
								alamJadaVahemAlgus = i;
							} else if (alamSona.isOnPotentsiaalneVahemikuLopp()){
								alamJadaVahemLopp = i;
							}
						}
						//  2) Otsustame ylemjada tyybi ning alamjada algus/l6put2histe asu-
						//     kohtade j2rgi, kas alamjada tuleks kustutada:
						if (vahemiku_tyyp == 1 || vahemiku_tyyp == 2){
							//       Tyybid:
							//     1)  ST+NI,         // Nt: (t2naseST) (homseNI)
							//     2)  ...+ST+...+NI  // Nt: (t2na hommikuST) (homme 6htuNI)
							if (!(alamJadaVahemAlgus == ajavtSonad2.size()-1 || 
								  alamJadaVahemLopp == ajavtSonad2.size()-1) ||
								  (alamJadaVahemAlgus != -1 && alamJadaVahemLopp != -1)) {
								toDelete.add(jada2);
							}
						} else if (vahemiku_tyyp == 3){
							//       Tyyp:
							//     3)  ST+...+NI+... // Nt: (yheksaST hommikul) (kymneNI 6htul)
							if (!(alamJadaVahemAlgus == 0 || 
								  alamJadaVahemLopp == 0) || 
								 (alamJadaVahemAlgus != -1 && alamJadaVahemLopp != -1)){
								toDelete.add(jada2);
							}
						}
					}
					j++;
				}
				//  > m22rame ka jada enda kustutamisele, et lahti saada juhtudest nagu nt:
				//    {t2na hommikust homme 6htuni}
				toDelete.add(jada1);
			}
		}
	
		//
		// 2) Eemaldame k6ik "kustutamisele kuuluvad" jadad
		//
		for (PotentsLiidetavateKandidaatideJada jadax : toDelete) {
			jadad.remove(jadax);
			// DEBUG:
			//System.out.println( "::::" + jadax );
		}
	}

	//==============================================================================
	//   Y l e k a t t u v u s e    j 2 r g i    f i l t r e e r i m i n e
	//   Y l e k a t t u v u s e    k o n t r o l l  
	//==============================================================================

	/**
	 *   Filtreerib antud <tt>jada</tt>-de kollektsiooni ylekattuvuste alusel: kustutab
	 *   jadad, mis on t2ielikult v6i osaliselt ylekaetud teiste, pikemate jadade poolt; <br><br>
	 *   
	 *   N2ide: T2ielik kattuvus. Kui meil on jadad <tt>{{"kell 8" "t2na"}, {"t2na" "hommikul"}, 
	 *   {"kell 8" "t2na" "hommikul"}}</tt> (selliselt, et asukoha poolest paiknevad esimene ja teine jada 
	 *   kolmanda sees), siis p2rast filtreerimist j22b alles vaid kolmas jada: <tt>{{"kell 8" "t2na" "hommikul"}}.
	 *   </tt><br><br>
	 *   
	 *   Osalise kattuvuse korral: hoolimata jadade pikkusest j2etakse alles tekstis eespool paiknev jada, 
	 *   eemaldatakse tagapool paiknev.
	 */
	public static void filtreeriPotentsJadadsidYleKattuvuseJargi( List<PotentsLiidetavateKandidaatideJada> jadad ){
		boolean leidubOsalineKattuvus = false;
		// 1) Eemaldame t2ielikult teiste jadade poolt ylekaetud jadad
		Iterator<PotentsLiidetavateKandidaatideJada> iterator1 = jadad.iterator();
		while ( iterator1.hasNext() ) {
			boolean toDelete = false;
			PotentsLiidetavateKandidaatideJada jada1 = 
					(PotentsLiidetavateKandidaatideJada) iterator1.next();
			if (jada1.startPosition == -1 || jada1.endPosition == -1){
				jada1.updateStartEndPositions();
			}
			// Leiame, kas m6ni jada katab selle kandidaadi t2ielikult yle
			Iterator<PotentsLiidetavateKandidaatideJada> iterator2 = jadad.iterator();
			while ( iterator2.hasNext() ) {
				PotentsLiidetavateKandidaatideJada jada2 = 
						(PotentsLiidetavateKandidaatideJada) iterator2.next();
				if (jada2.startPosition == -1 || jada2.endPosition == -1){
					jada2.updateStartEndPositions();
				}
				if ((jada1.startPosition > jada2.startPosition &&
					 jada1.endPosition <= jada2.endPosition) ||
				    (jada1.startPosition >= jada2.startPosition &&
					 jada1.endPosition < jada2.endPosition)){
					// Leidsime kandidaadi, mis katab antud kandidaadi t2ielikult yle
					toDelete = true;
					break;
				}
				if ((jada1.startPosition < jada2.startPosition &&
					 jada1.endPosition >= jada2.startPosition  &&
					 jada1.endPosition < jada2.endPosition) ||
					 (jada2.startPosition < jada1.startPosition &&
					  jada2.endPosition >= jada1.startPosition  &&
					  jada2.endPosition < jada1.endPosition)){
					// J2tame meelde, et leidusid osalise kattuvusega kandidaadid
					leidubOsalineKattuvus = true;
				}
			}
			if ( toDelete ){
				iterator1.remove();
				//System.out.println( "-----"+jada1.toString() );
			}
		}
		
		// 2) Osaliste kattuvuste korral: j2tame alles eespool-paikneva Jada
		if (leidubOsalineKattuvus){
			iterator1 = jadad.iterator();
			while ( iterator1.hasNext() ) {
				PotentsLiidetavateKandidaatideJada jada1 = 
						(PotentsLiidetavateKandidaatideJada) iterator1.next();
				Iterator<PotentsLiidetavateKandidaatideJada> iterator2 = jadad.iterator();
				while ( iterator2.hasNext() ) {
					PotentsLiidetavateKandidaatideJada jada2 = 
							(PotentsLiidetavateKandidaatideJada) iterator2.next();
					if ((jada2.startPosition < jada1.startPosition &&
						 jada2.endPosition >= jada1.startPosition  &&
						 jada2.endPosition < jada1.endPosition)){
						// Teine Jada katab esimest osaliselt: eemaldame esimese Jada;
						iterator1.remove();
						break;
					}
				}
			}
		}
		
		// DEBUG:
		//System.out.println( "==========================================" );
		//for (PotentsLiidetavateKandidaatideJada jada : jadad) {
		//	System.out.println( jada.toString() );			
		//}
		//System.out.println( "==========================================" );
	}
	
	/**
	 *  Kontrollib, kas k2esolev jada neelab t2ielikult alla etteantud ajav2ljendi-
	 *  kandidaadi, st kas kandidaat paikneb antud jada sees ning on lyhem kui jada;
	 */
	public boolean kasJadaNeelabAjavaljendiKandidaadi( AjavaljendiKandidaat k ){
		if (this.kandidaadid != null){
			if (this.startPosition == -1 || this.endPosition == -1){
				updateStartEndPositions();					
			}
			if (k.getFraas() != null && !(k.getFraas()).isEmpty()){
				AjavtSona ajavtSona1 = (k.getFraas()).get(0);
				AjavtSona ajavtSona2 = (k.getFraas()).get((k.getFraas()).size()-1);
				int kStart = ajavtSona1.getInnerTokenPosition();
				int kEnd   = ajavtSona2.getInnerTokenPosition();
				return ((this.startPosition <= kStart && kEnd < this.endPosition) ||
						(this.startPosition < kStart && kEnd <= this.endPosition));
			}
		}
		return false;
	}
			
	//==============================================================================
	//   	J a d a      l a i e n d a m i n e
	//==============================================================================
	
	/**
	 *  Kontrollib, kas etteantud kandidaadi <tt>k</tt> v6ib lisada selle jada l6ppu.
	 *  Tagastab <tt>true</tt>, kui see jada on tyhi v6i kui selle jada viimane s6na
	 *  eelneb vahetult antud kandidaadi esimesele s6nale ning viimane s6na pole 
	 *  lausel6pp, k6igil muudel juhtudel <tt>false</tt>;
	 */
	public boolean kasSaabLaiendadaAjavaljendiKandidaadiga( AjavaljendiKandidaat k ){
		if (this.kandidaadid == null || ((this.kandidaadid).size()) == 0){
			return true;
		} else {
			List<AjavtSona> jadaSonad = this.getAjavtSonad();
			List<AjavtSona> uuedSonad = k.getFraas();
			if (jadaSonad != null && uuedSonad != null){
				AjavtSona viimaneSona = jadaSonad.get( jadaSonad.size()-1 );
				AjavtSona esimeneSona = uuedSonad.get( 0 );
				return (!viimaneSona.onLauseLopp() && 
						 viimaneSona.getInnerTokenPosition() + 1 == esimeneSona.getInnerTokenPosition());
			}
		}
		return false;
	}
	
	/**
	 *   Teeb k2esolevast jadast koopia, lisab selle l6pu etteantud kandidaadi <tt>k</tt>
	 *   (NB! kandidaat peab vastama meetodi <tt>kasSaabLaiendadaAjavaljendiKandidaadiga()</tt> 
	 *   poolt kontrollitavatele tingimustele) ning tagastab saadud jada;
	 */
	public PotentsLiidetavateKandidaatideJada laiendaUueksJadaks( AjavaljendiKandidaat k ){
		PotentsLiidetavateKandidaatideJada jada = new PotentsLiidetavateKandidaatideJada();
		jada.kandidaadid = new ArrayList<AjavaljendiKandidaat>( this.kandidaadid );
		jada.addAjavaljendiKandidaat(k);
		return jada;
	}
	
	//==============================================================================
	//   	S 6 n a - t a s e m e l     l i g i p 2 2 s    j a d a l e
	//==============================================================================
	
	/**
	 *  Leiab jada esimese ja viimase s6na indeksid tekstis ning omistab
	 *  muutujatesse <tt>startPosition</tt> ja <tt>endPosition</tt>;
	 */
	public void updateStartEndPositions(){
		if (this.kandidaadid != null){
			AjavaljendiKandidaat esimeneKandidaat = (this.kandidaadid).get(0);
			AjavaljendiKandidaat viimaneKandidaat = (this.kandidaadid).get(
				(this.kandidaadid).size()-1 );
			if (esimeneKandidaat.getFraas() != null){
				AjavtSona ajavtSona = (esimeneKandidaat.getFraas()).get(0);
				this.startPosition = ajavtSona.getInnerTokenPosition();
			}
			if (viimaneKandidaat.getFraas() != null){
				AjavtSona ajavtSona = (viimaneKandidaat.getFraas()).get( 
						(viimaneKandidaat.getFraas()).size()-1 );
				this.endPosition = ajavtSona.getInnerTokenPosition();
			}
		}
	}
	
	/**
	 *  Tagastab antud jadasse kuuluvate kandidaatide k6ik <tt>AjavtSona</tt>-d nende tekstis 
	 *  paiknemise j2rjekorras;
	 */
	public List<AjavtSona> getAjavtSonad(){
		List<AjavtSona> tulem = new ArrayList<AjavtSona>();
		if (this.kandidaadid != null){
			for (int i = 0; i < (this.kandidaadid).size(); i++) {
				AjavaljendiKandidaat kandidaat = (this.kandidaadid).get(i);
				if (kandidaat.getFraas() != null){
					for (int j = 0; j < (kandidaat.getFraas()).size(); j++) {
						AjavtSona ajavtSona = (kandidaat.getFraas()).get(j);
						tulem.add( ajavtSona ); 
					}					
				}
			}
		}
		return tulem;
	}

	/**
	 *  Tagastab antud jadasse kuuluvate kandidaatide k6ikide s6nade <tt>FraasisPaiknemiseKoha</tt>-d nende tekstis 
	 *  paiknemise j2rjekorras;
	 */
	public List<FraasisPaiknemiseKoht> getFraasisPaiknemiseKohad(){
		List<FraasisPaiknemiseKoht> tulem = new ArrayList<FraasisPaiknemiseKoht>();
		if (this.kandidaadid != null){
			for (int i = 0; i < (this.kandidaadid).size(); i++) {
				AjavaljendiKandidaat kandidaat = (this.kandidaadid).get(i);
				if (kandidaat.getFraas() != null){
					List<AjavtSona> fraas = kandidaat.getFraas();
					for (int j = 0; j < fraas.size(); j++) {
						if (fraas.size() == 1){
							tulem.add( FraasisPaiknemiseKoht.AINUSSONA );
						} else if (j == 0){
							tulem.add( FraasisPaiknemiseKoht.ALGUSES );
						} else if (j == fraas.size()-1){
							tulem.add( FraasisPaiknemiseKoht.LOPUS );
						} else {
							tulem.add( FraasisPaiknemiseKoht.KESKEL );
						}
					}					
				}
			}
		}
		return tulem;
	}
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================
	
	public void addAjavaljendiKandidaat( AjavaljendiKandidaat kandidaat ){
		if (this.kandidaadid == null){
			this.kandidaadid = new ArrayList<AjavaljendiKandidaat>();
		}
		(this.kandidaadid).add(kandidaat);
	}
	
	public List<AjavaljendiKandidaat> getKandidaadid() {
		return kandidaadid;
	}
	
	public int getStartPosition() {
		updateStartEndPositions();
		return startPosition;
	}
	
	public int getEndPosition() {
		updateStartEndPositions();
		return endPosition;
	}
	
	public String toString(){
		if ( this.kandidaadid  !=  null ){
			StringBuilder sb = new StringBuilder();
			for ( AjavaljendiKandidaat kandidaat : this.kandidaadid ) {
				sb.append( "{" );
				if (kandidaat.getFraas() != null){
					for ( AjavtSona sona : kandidaat.getFraas() ) {
						sb.append( sona.getAlgSona()+"|" );
					}
				}
				sb.append( "} " );
			}
			return sb.toString();
		}
		return "NULL";
	}
	
}
