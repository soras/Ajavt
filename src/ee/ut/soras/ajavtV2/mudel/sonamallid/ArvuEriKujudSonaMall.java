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
import ee.ut.soras.ajavtV2.mudel.FraasisPaiknemiseKoht;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;
import ee.ut.soras.ajavtV2.util.TextUtils;
import ee.ut.soras.wrappers.mudel.MorfAnRida;

/**
 *  Arvu kirjutamise erinevaid kujusid pyydev mall. Erinevalt tavalisest s&otilde;namallist on
 *  siin lubatud ka mitmest s&otilde;nast koosnevate fraaside pyydmine.
 *  Mall katab j2rgmiseid arvukujusid:
 *  <ul>
 *    <li> Rooma numbrid; </li>
 *    <li> T&auml;isarve edasiandvad arvs&otilde;nafraasid; </li>
 *    <li> J&auml;rgarve edasiandvad arvs&otilde;nafraasid; </li>
 *    <li> Murdarve edasiandvad arvs&otilde;nafraasid; </li>
 *  </ul>
 *  <p>
 *  V&auml;lja j&auml;&auml;vad araabia numbritega edasiantud arvud, nende pyydmiseks
 *  tuleks kasutada regulaaravaldisi.
 * 
 * @author Siim Orasmaa
 */
public class ArvuEriKujudSonaMall extends SonaMalliSemOsa implements SonaMall {

	/**
	 *   Arvutyybi kitsendused. V6imalikud arvutyybid: _N_ (tavaline arv), _O_ (j2rgarv), _F_ (murdarv), _R_ (rooma number).
	 *   Kontrollitav arvsona peab rahuldama vahemalt yht etteantud tyypidest. 
	 */
	private String arvuTyybid [] = null;
	
	/**
	 *   Arvulise vaartuse alampiir. -1 t&auml;hendab, et defineerimata. 
	 */
	private int arvuVaartuseAlampiir = -1;
	
	/**
	 *   Arvulise vaartuse ylempiir. -1 t&auml;hendab, et defineerimata. 
	 */	
	private int arvuVaartuseYlempiir = -1;
	
	/**
	 *   Seab ylem ja alampiirid arvu vaartusele. Kui piiri vaartuseks antud -1 tahendab,
	 *   et piir on maaramata.
	 */
	public void setArvuVaartusePiirid(int arvuVaartuseAlampiir, int arvuVaartuseYlempiir){
		this.arvuVaartuseAlampiir = arvuVaartuseAlampiir;
		this.arvuVaartuseYlempiir = arvuVaartuseYlempiir;
	}
	
	/**
	 *  Vaikekonstruktor. Initsialiseerib malli arvsonafraase pyydma, maarates vaikimisi 
	 *  sobivateks tyypideks _N_, _O_, _F_. 
	 */
	public ArvuEriKujudSonaMall() {
		super();
		this.setArvuTyyp("_N_|_O_|_F_");
	}

	/**
	 *   Seab kitsenduse n6utavale arvutyybile (kas _N_, _O_, _F_ v6i _R_). Olemasolev
	 *  v6i vaikekitsendus kirjutatakse alati yle. V6ib anda ka mitu sobivat arvutyypi: 
	 *  sellisel juhul peab nende vahel olema eraldajaks mark |.
	 */
	public void setArvuTyyp(String inputArvuTyyp){
		if (inputArvuTyyp != null){
			String[] split = inputArvuTyyp.split("\\|");
			if (split != null && split.length > 0){
				this.arvuTyybid = split;
			}
		}
	}
	
	/**
	 *  Kas etteantud arv sobib malli poolt j2rgitava arvupiiri sisse.
	 */
	private boolean arvMahubEtteantudPiiridesse(int arv){
		if (arvuVaartuseAlampiir != -1 && arvuVaartuseAlampiir > arv){
			return false;
		}
		if (arvuVaartuseYlempiir != -1 && arvuVaartuseYlempiir < arv){
			return false;
		}		
		return true;
	}
	
	public TYYP getTyyp() {
		return TYYP.ERIARV;
	}
	
	public MallileVastavus vastabMallile(AjavtSona sona) {
		if (this.arvuTyybid != null){
			for ( String arvutyyp : this.arvuTyybid ) {
				boolean rahuldabKitsendusi = true;
				// 1) Kontrollime arvutyypi				
				if (!vastabArvuTyybile(arvutyyp, sona)){
					// NB! Allj2rgnev kontroll toimib ainult siis, kui:
					//  --- j2rgarv on arvs6nafraasi l6pus;
					//  --- kogu fraas koosnebki vaid yhest j2rgarvust;
					// Kui j2rgarv on fraasi alguses ning j2rgnevad tavalised arvud, siis ei toimi ...
					// TODO: Kui j2rgarv on fraasi l6pus, aga mitte alguses (nt kahekümne esimene), eraldatakse
					// vaid l6pus olev j2rgarv
					rahuldabKitsendusi = false;
				}
				// 2) Kontrollime, kas arv mahub etteantud piiridesse ...
				if (arvutyyp.equals("_R_") && rahuldabKitsendusi){
					// Kontrollime, kas parsitud rooma number mahub antud piiridesse
					int arv = parsiRoomaNumber( sona.getAlgSonaYmbritsevateMarkideta() );
					if (!arvMahubEtteantudPiiridesse(arv)){
						rahuldabKitsendusi = false;
					}
				} else if (sona.getArvSonaFraasis() != FraasisPaiknemiseKoht.PUUDUB){
					Integer taisArvuna = sona.getArvSonaTaisArvVaartus();
					Double  murdArvuna = sona.getArvSonaMurdArvVaartus();			
					// Kontrollime, kas arvs6nafraas mahub etteantud piiridesse
					if ( (taisArvuna != null && !arvMahubEtteantudPiiridesse(taisArvuna.intValue())) ||
							(murdArvuna != null && !arvMahubEtteantudPiiridesse(murdArvuna.intValue())) ||
									(taisArvuna == null && murdArvuna == null) ){
						rahuldabKitsendusi = false;
					}
				} else {
					// Kui yhtki kontrolli ei l2bitud, eeldame vaikimisi, et kitsendusi ei rahulda
					rahuldabKitsendusi = false;
				}
				// 3) Kui etteantud s6na rahuldas kitsendusi (v2hemalt yhe) arvutyybi
				// raames, oleme edukalt vastavuse leidnud
				if (rahuldabKitsendusi){
					// Tavaliste arvs6nade, j2rgarvude ja murdarvude puhul peame veel kindlaks tegema,
					// millises arvufraasi osas sobitamine parajasti toimub ...
					if (arvutyyp.matches("_[NOF]_")){
						if (sona.getArvSonaFraasis() == FraasisPaiknemiseKoht.AINUSSONA ||
								sona.getArvSonaFraasis() == FraasisPaiknemiseKoht.LOPUS){
							return MallileVastavus.VASTAB_LOPLIKULT;
						} else if (sona.getArvSonaFraasis() == FraasisPaiknemiseKoht.ALGUSES){
							return MallileVastavus.VASTAB_ALGUS;
						} else if (sona.getArvSonaFraasis() == FraasisPaiknemiseKoht.KESKEL){
							return MallileVastavus.VASTAB_KESKOSA;
						}
					} else {
						// Rooma numbrite puhul eeldatakse, et kogu number sisaldus yhes s6nas
						return MallileVastavus.VASTAB_LOPLIKULT;
					}
				}
			}
		}
		return MallileVastavus.EI_VASTA;
	}

	/**
	 *   Kontrollib, kas etteantud arvs6na tyyp vastab vahemalt etteantud arvutyybile (<tt>arvutyyp</tt>). 
	 *  Morf analyysis valjatoodud tyypide <tt>_N_</tt> ja <tt>_O_</tt> korral kontrollib 
	 *  lihtsalt, et vahemalt yks s6na morfoloogilistest analyysidest vastaks tyybile. Murdarvu tahistava tyybi 
	 *  <tt>_F_</tt> korral kontrollib, kas s6naga on seotud murdarv. Rooma numbri <tt>_R_</tt> korral yritab
	 *  sellest araabia numbrit parsida, kui 6nnestub, loetakse tyybiga sobitunuks.  
	 *  <p>
	 */
	private boolean vastabArvuTyybile(String arvutyyp, AjavtSona sona){
		// 1) On olemas taisarv: kontrollime analyysi tyype _O_ ja _N_		
		if (sona.getArvSonaTaisArvVaartus() != null && 
				sona.getArvSonaMurdArvVaartus() == null){
			for (MorfAnRida rida : sona.getAnalyysiTulemused()) {
				if (rida.getSonaliik() != null && 
						(rida.getSonaliik()).startsWith( arvutyyp )){
					return true;
				}
			}
		// 2) On olemas murdarv: sobib tyyp _F_
		} else if (sona.getArvSonaTaisArvVaartus() == null && 
					sona.getArvSonaMurdArvVaartus() != null &&
					 arvutyyp.matches("_F_")){
			return true;
		// 3) N6utud tyyp on _R_ - kontrollime, kas on tegu rooma numbriga ...
		} else if (arvutyyp.equals("_R_")){
			int arv = parsiRoomaNumber( sona.getAlgSonaYmbritsevateMarkideta() );
			if (arv != -1){
				return true;
			}
		}
		return false;
	}
	
	
	@Override
	public List<SemantikaDefinitsioon> tagastaMalliSemantilineOsa(AjavtSona viimaneSobitunudSona){
		int references [] = super.getSemValueReferences();
		List<SemantikaDefinitsioon> semRules = super.tagastaMalliSemantilineOsa(viimaneSobitunudSona);
		if (references != null && semRules != null && references.length == semRules.size() && this.arvuTyybid != null){
			
			for (int i = 0; i < references.length; i++) {
				SemantikaDefinitsioon semRule = semRules.get(i);
				int reference = references[i];
				if (reference > -1 && semRule != null){
					for ( String arvutyyp : this.arvuTyybid ) {
						if (vastabArvuTyybile(arvutyyp, viimaneSobitunudSona)){
							//  1) Rooma number: kontrollime, kas etteantud s6na vastab rooma
							//     m6nele numbrile ...
							if (arvutyyp.equals("_R_")){
								int arv = parsiRoomaNumber( viimaneSobitunudSona.getAlgSonaYmbritsevateMarkideta() );
								if (arv != -1 && arvMahubEtteantudPiiridesse(arv)){
									semRule.setSemValue( String.valueOf(arv) );
								}
							} else if ((viimaneSobitunudSona.getArvSonaFraasis()).onFraasiLopp()) {
							//  2) Arvs6nafraas (tavaline, murd- v6i j2rgarv)
								Integer taisArvuna = viimaneSobitunudSona.getArvSonaTaisArvVaartus();
								Double  murdArvuna = viimaneSobitunudSona.getArvSonaMurdArvVaartus();
								if (murdArvuna != null){
									// ---- tegemist on murdarvuga							
									if (arvMahubEtteantudPiiridesse( (murdArvuna).intValue() )){
										semRule.setSemValue( (murdArvuna.toString()).replace(".", ",") );
									}
								} else if (taisArvuna != null){
									// ---- tegemist on taisarvuga
									// NB! Siin tekib erijuht: kui semantikadefinitsiooni on juba lisatud murdarvuline
									// semValue, siis ei hakka me seda yle kirjutama, kuna murdarv on yldisem
									if (semRule.getSemValue() == null || 
											(semRule.getSemValue() != null && 
													!((TextUtils.musterMurdArv).matcher(semRule.getSemValue())).matches()) ){
										if (arvMahubEtteantudPiiridesse( (taisArvuna).intValue() )){
											semRule.setSemValue( taisArvuna.toString() );
										}								
									}
								}
							}
						}
					} // foreach arvuTyybid
				}
			} // foreach references
		}
		return semRules;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("arvu_erikujud{");
		for (String tyyp : arvuTyybid) {
			sb.append(tyyp);
			sb.append(" ");
		}
		sb.append("}");
		return sb.toString();
	}
	
	public static final Object romanToArabic [] [] = {
		{"I", 1}, {"V", 5}, {"X", 10}, {"L", 50}, {"C", 100}, {"D", 500}, {"M", 1000}
	};
	
	
	/**
	 *  Yritab yksiku rooma numbri konverteerimist araabia numbriks. Kui konverteerimine
	 *  eba6nnestub, tagastab -1;
	 */
	public static int parsiYksikRoomaNumber(String str){
		for (Object fromRomanToArabicTable[] : romanToArabic) {
			String roman = (String)fromRomanToArabicTable[0];
			if (roman.equalsIgnoreCase(str)){
				return (Integer)fromRomanToArabicTable[1];
			}
		}
		return -1;
	}
	
	
	/**
	 *   Parsib etteantud s6nast (pikkus v2hemalt 2) rooma (naturaalarvulise) numbri. Tagastab -1, 
	 *   kui numbri parsimine ei 6nnestunud.
	 *   <p>
	 *   Toetab ainult väikeseid, ilma katuseta rooma numbreid (vahemikust 1-4999);
	 */
	public static int parsiRoomaNumber(String str){
		int summa = 0; 
		boolean unknownSymbolFound = false;
		for( int i = 0 ; i < str.length(); i++ ){
			String substr = str.substring(i, i+1);
			int arabic1 = parsiYksikRoomaNumber(substr);
			if (arabic1 > -1){
				if (i == (str.length() - 1)){
					// Kui on viimane number, siis lihtsalt lisame
					summa += arabic1;
				} else {
					// Kui j2rgneb veel m6ni number, peame kontrollima paari:
					int arabic2 = parsiYksikRoomaNumber( str.substring(i+1, i+2) );
					if (arabic2 > - 1){
						// 1) Kui esimene on suurem/v6rdne kui teine, siis lihtsalt liidame
						if (arabic1 >= arabic2){
							summa += arabic1;
						} else {
						// 2) Kui teine on suurem kui esimene, tuleb liita (teine - esimene)
							summa += (arabic2 - arabic1);
							i++;
						}
					} else {
						unknownSymbolFound = true;
					}
				}
			} else {
				unknownSymbolFound = true;
			}
			// Kui rooma numbrit ei leidnud, katkestame
			if (unknownSymbolFound){
				summa = -1;
				break;
			}
		}
		return (summa > 0) ? (summa) : (-1);
	}
	
}
