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

package ee.ut.soras.ajavtV2.mudel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ee.ut.soras.ajavtV2.NumberTokenizer;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat.ASTE;
import ee.ut.soras.ajavtV2.util.TextUtils;
import ee.ut.soras.wrappers.mudel.MorfAnSona;

/**
 *   Ajav&auml;ljendite tuvastaja poolt analyysitav/tud s&otilde;na.
 * Kannab endas nii s&otilde;na (v&otilde;i lausem&auml;rgi) morf
 * analyysi ja yhestamise k&auml;igus leitud informatsiooni kui ka 
 * viidet sellele, milliste ajavaljendikandidaatide koosseisu sona
 * kuulub.
 * 
 * @author Siim Orasmaa
 */
public class AjavtSona extends MorfAnSona {

	//==============================================================================
	//   	J o o n d u s   e s i a l g s e   t e k s t i g a
	//==============================================================================
	
	/**
	 *  Selle s6na alguspositsioon esialgses tekstis;
	 */
	private int startPosition = -1;
	
	/**
	 *  Selle s6na lopp-positsioon esialgses tekstis;  
	 */
	private int endPosition   = -1;

	/**
	 *  Mitmes token on systeemisiseses token'ite j2rjestuses? Systeemisisene token'ite
	 *  jarjestus v6ib erineda token'ite j2rjestusest esialgses sisendis 
	 *  (MorfAnSona.tokenPosition): yks esialgse sisendi token v6ib systeemi sees olla 
	 *  jagatud mitmeks token'iks (nt numbrilistes kellaaegades tundide ja minutite 
	 *  eraldamine); 
	 */
	private int innerTokenPosition = -1;
	
	/**
	 *  Kas antud s6na paikneb positsioonil, kus esialgse sisendi token on katki tehtud? 
	 *  (Kasutatakse vaid <code>t3olp</code> sisendi korral).
	 */
	private boolean atTokenBreakPosition = false;
	
	//==============================================================================
	//   	A l g s o n a
	//==============================================================================
	
	/**
	 *  Algsona normaliseeritud kuju 1: sona algsel/analyysieelsel kujul on teatud 
	 *  erisymbolid (HTML olemid, erinevad kriipsud ja õ eri kujud) normaliseeritud,
	 *  taandatud kindlale symbolile.
	 */
	private String algSonaErisymbolidNormaliseeritud = null;
	
	/**
	 *   Algs6na normaliseeritud kuju 2: tuletatud kujust algSonaErisymbolidNormaliseeritud;
	 *   ymbritsevad kirjavahem2rgid on eemaldatud
	 */
	private String algSonaYmbritsevateMarkideta = null;	
	
	//==============================================================================
	//   	P a i k n e m i n e    a r v s o n a f r a a s i s     v o i
	//      n u m b r i t e k o m b i n a t s i o o n i   k o o s s e i s u s
	//==============================================================================
	
	/**
	 *   Kuidas antud sona paikneb arvsonafraasis?
	 */
	private FraasisPaiknemiseKoht arvSonaFraasis = FraasisPaiknemiseKoht.PUUDUB;
	
	/**
	 *   Arvsona taisarvuline vaartus. Praegu lubame, et defineeritud on korraga ainult
	 *  yks kahest vaartusest: kas taisarvuline vaartus voi murdarvuline vaartus.
	 */
	private Integer arvSonaTaisArvVaartus = null;

	/**
	 *   Arvsona murdarvuline vaartus. Praegu lubame, et defineeritud on korraga ainult
	 *  yks kahest vaartusest: kas taisarvuline vaartus voi murdarvuline vaartus.
	 */
	private Double arvSonaMurdArvVaartus = null;
	

	//==============================================================================
	//   	P a i k n e m i n e    e r a l d a t u d     a j a v a l j e n d i - 
	//      k a n d i d a a t i d e s
	//==============================================================================
	
	/**
	 *   Millistel positsioonidel paikneb antud sona listis <code>ajavaljendiKandidaadid</code> toodud
	 *   ajavaljendites?
	 */
	private List<FraasisPaiknemiseKoht> ajavaljendiKandidaatides = null;
	
	/**
	 *  Milliste ajavaljendikandidaatide koosseisus antud sona esineb?
	 */
	private List<AjavaljendiKandidaat> ajavaljendiKandidaadid = null;

	//==============================================================================
	//    A j a v a h e m i k k u   k u u l u m i s e    h e u r i s t i k a 
	//==============================================================================
	
	/**
	 *  Kas antud s6na on potentsiaalselt osa ajavahemiku fraasi algusest?  
	 */
	private boolean onPotentsiaalneVahemikuAlgus = false;

	/**
	 *  Kas antud s6na on potentsiaalselt osa ajavahemiku fraasi lopust?  
	 */
	private boolean onPotentsiaalneVahemikuLopp = false;

	//==============================================================================
	//    G r a m m a t i l i s e     a j a     h e u r i s t i k u d 
	//==============================================================================
	
	/**
	 *  Verbide grammatilised ajam&auml;&auml;rangud.
	 */
	public static enum GRAMMATILINE_AEG {
		/**
		 *  Aega ei saa antud s6na puhul välja tuua;
		 */
		MAARAMATA,
		/**
		 *  Verbi olevikuaeg;
		 */
		OLEVIK,
		/**
		 *  Verbi(fraasi) täisminevik;
		 */
		TAISMINEVIK,
		/**
		 *  Verbi lihtminevik;
		 */
		LIHTMINEVIK,
		/**
		 *  Verbi(fraasi) enneminevik;
		 */
		ENNEMINEVIK,
		/**
		 *  Verbi(fraasi) üldminevik;
		 */
		YLDMINEVIK,
		/**
		 *  Kesksõna minevikuaeg;
		 */
		KS_MINEVIK,
		/**
		 *  Kesksõna olevikuaeg;
		 */
		KS_OLEVIK		
	};
	
	/**
	 *  Kui antud s&otilde;na n&auml;ol on tegu verbiga, on siin kirjas grammatiline aeg. 
	 */
	private GRAMMATILINE_AEG grammatilineAeg = GRAMMATILINE_AEG.MAARAMATA;
	
	//==============================================================================
	//   	Konstruktor
	//==============================================================================
	
    public AjavtSona(String algSona) {
        super(algSona);
        if (algSona.length() <= 50){ // Häkk: kuna liiga pikkade sõnede töötlus võib Linuxi all regexp'id kokku jooksutada
         	 this.algSonaErisymbolidNormaliseeritud =
           		 TextUtils.normalizeSpecialSymbols(algSona);
             this.algSonaYmbritsevateMarkideta = 
              	 TextUtils.trimSurroundingPunctation(this.algSonaErisymbolidNormaliseeritud);
             if ((algSona).equals(this.algSonaYmbritsevateMarkideta) || 
                 (this.algSonaYmbritsevateMarkideta).length() == 0){
              	 // Kui punktuatsiooni eemaldamisel ei jäänud midagi alles, jätame
               	 // selle faasi välja ...
                 this.algSonaYmbritsevateMarkideta = this.algSonaErisymbolidNormaliseeritud;
             }
        }
    }
    
    /**
     *    Loob uue AjavtSona etteantud morfSona alusel, arvestades selle v6imalikku jupitamist. Kui
     *   uusAlgSona == null, siis vastab loodava AjavtSona positsioon tekstis t2pselt etteantava
     *   morfSona positsioonile; vastasel juhul on uusAlgSona mingi alamosa morfSona poolt m22ratud
     *   s6na tekstikujust. N2ide:
     *   <ul>
     *      <li>Vahemikule, nt '8-10', vastab tavaliselt yks morfSona, aga ajav2ljendite tuvastaja
     *          teeb selle juppideks (n2ites '8-' ja '10') ning loob iga jupi kohta uue AjavtSona; </li>
     *   </ul>
     */
    public AjavtSona(MorfAnSona morfSona, String uusAlgSona){
    	// Initsialiseerime MorfAnSona
    	super( (uusAlgSona != null) ? (uusAlgSona) : morfSona.getAlgSona() );
    	// TODO: siin oleks parem kasutada Clonable vms interface'i ...
    	// Kanname yle analyysiread
    	for (int i = 0; i < (morfSona.getAnalyysiTulemused()).size(); i++) {
			super.lisaAnalyysiRida((morfSona.getAnalyysiTulemused()).get(i));
		}
    	super.setTokenPosition( morfSona.getTokenPosition() );
    	// Initsialiseerime Algsona
    	String algSona = this.getAlgSona();
        if (algSona.length() <= 50){ // Häkk: kuna liiga pikkade sõnede töötlus võib Linuxi all regexp'id kokku jooksutada
        	 this.algSonaErisymbolidNormaliseeritud =
          		 TextUtils.normalizeSpecialSymbols(algSona);
            this.algSonaYmbritsevateMarkideta = 
             	 TextUtils.trimSurroundingPunctation(this.algSonaErisymbolidNormaliseeritud);
            if ((algSona).equals(this.algSonaYmbritsevateMarkideta) || 
                (this.algSonaYmbritsevateMarkideta).length() == 0){
             	 // Kui punktuatsiooni eemaldamisel ei jäänud midagi alles, jätame
              	 // selle faasi välja ...
                this.algSonaYmbritsevateMarkideta = this.algSonaErisymbolidNormaliseeritud;
            }
       }
    }

	//==============================================================================
	//   	Y l e k a e t u d    k a n d i d a a t i d e   e e m a l d u s 
	//==============================================================================
	
	/**
	 *   Eemaldab ajav2ljendikandidaadid, mis on t2ielikult ylekaetud teiste 
	 *   ajav2ljendikandidaatide poolt.
	 */
	public void eemaldaYlekattuvadAjavaljendiKandidaadid(){
		List<AjavaljendiKandidaat> eemaldatavad = new ArrayList<AjavaljendiKandidaat>();
		// 1) Leiame, millised ajavaljendiKandidaadid tuleb eemaldada
		for (int i = 0; i < (this.ajavaljendiKandidaadid).size(); i++) {
			AjavaljendiKandidaat ajavaljend = (this.ajavaljendiKandidaadid).get(i);
			if (ajavaljend.onYlekaetudTeisteAjavaljendifraasidePoolt()){
				eemaldatavad.add(ajavaljend);
			}
		}
		// 2) "Organiseerime" eemaldamise
		for (AjavaljendiKandidaat ajavaljend : eemaldatavad) {
			ajavaljend.eemaldaEnnastSonadeKyljest();
		}
	}
	
	/**
	 *  Eemaldab s6naga seotud ajavaljendikandidaatide seast etteantud kandidaadi.
	 */
	public void eemaldaAjavaljendiKandidaat(AjavaljendiKandidaat ajav){
		Iterator<AjavaljendiKandidaat> ajavIterator	 = (this.ajavaljendiKandidaadid).iterator();
		Iterator<FraasisPaiknemiseKoht> kohtIterator = (this.ajavaljendiKandidaatides).iterator();
		while(ajavIterator.hasNext() && kohtIterator.hasNext()){
			AjavaljendiKandidaat av = ajavIterator.next();
			kohtIterator.next();
			if (av.equals(ajav)){
				ajavIterator.remove();
				kohtIterator.remove();
				break;
			}
		}
	}
	//==============================================================================
	//   	Vahemikuga seotud numbrite parsimine 
	//==============================================================================
	
	/**
	 *   J2rgides <tt>AjaTuvastaja.eraldaAjavahemikudJaLiidaFraasiks</tt> poolt teostatud loogikat, 
	 *  tagastab selle s6na arvulise semantika j2rgmistel tingimustel:
	 *  <ul>
	 *    <li> s6na ei kuulu yhegi MUSTRI-POOLT-ERALDATUD v6i FRAAS-tyypi ajavaljendikandidaadi alla;
	 *    <li> s6na on arvs6nafraasi alguses v6i kannab endas numbritega edasiantud arvu... 
	 *  </ul>
	 */
	public List<Integer> parsiPotentsVahemikugaSeotudArvud(){
		List<Integer> numbrid = new ArrayList<Integer>();
		// 1) Kontrollime, et s6na ei kuuluks fraasi v6i mustri alla
		boolean eiKuuluMustriEgaFraasiAlla = true;
		if (this.onSeotudMoneAjavaljendiKandidaadiga()){
			for (AjavaljendiKandidaat ajavKandidaat : this.ajavaljendiKandidaadid) {
				if (ajavKandidaat.getAste() == ASTE.YHENDATUD_FRAASINA ||
					ajavKandidaat.getAste() == ASTE.MUSTRI_POOLT_ERALDATUD){
					eiKuuluMustriEgaFraasiAlla = false;
					break;
				}
			}
		}
		// 2) Parsime numbri
		if (eiKuuluMustriEgaFraasiAlla){
			if (this.getArvSonaFraasis().onFraasiAlgus() &&
					this.getArvSonaTaisArvVaartus() != null){
				// V6tame eelnevalt parsitud numbri arvs6nast
				numbrid.add( this.getArvSonaTaisArvVaartus() );
			} else if (this.getArvSonaFraasis() == FraasisPaiknemiseKoht.PUUDUB){
				// Parsime s6nest, numbrilisest esitusest				
				List<String> possibleNumbers = NumberTokenizer.extractNumbersWithTrailingPunctation(
											        this.getAlgSona(), 
											        false);
				for (int j = 0; j < possibleNumbers.size(); j++) {
					if ((possibleNumbers.get(j)).length() > 0){
						try {
							Integer integer = Integer.valueOf(possibleNumbers.get(j));
							if (integer != null){
								numbrid.add(integer);
							}
						} catch (NumberFormatException e) {
						}
					}
				}
			}
		}
		return (numbrid.isEmpty()) ? (null) : (numbrid);
	}

	/**
	 *   Teeb (heuristiliselt) kindlaks, kas arvude esituskuju kahes s6nas on sarnane.
	 *   Arvude esitluskuju poolt yhilduvaks loetakse s6nu siis, kui m6lemas s6nas 
	 *   esitatakse arvud kas ainult arvs6nadega v6i ainult numbritega
	 */
	public boolean arvudeEsitusKujuOnYhilduv(AjavtSona sona){
		return (this.arvSonaFraasis == sona.arvSonaFraasis ||
					(this.arvSonaFraasis != FraasisPaiknemiseKoht.PUUDUB && 
					 sona.arvSonaFraasis != FraasisPaiknemiseKoht.PUUDUB));
	}
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================	
	
	/**
	 *   Margendab kaesoleva sona etteantud ajavaljendikandidaadiga seotuks 
	 *   (st - s6na v6ib potenstiaalselt kuuluda etteantud ajavaljendifraasi:
	 *   kas see ka tegelikult nii on, selgub hilisema tootluse kaigus).
	 *   <p>
	 *   Yks sona voib ka ysna mitme ajavaljendifraasi koosseisu kuuluda.
	 */
	public void margendaAjavaljendiKandidaadiga(AjavaljendiKandidaat ajav, FraasisPaiknemiseKoht koht){
		if (this.ajavaljendiKandidaadid == null){
			this.ajavaljendiKandidaatides = new ArrayList<FraasisPaiknemiseKoht>();
			this.ajavaljendiKandidaadid = new ArrayList<AjavaljendiKandidaat>();
		}
		this.ajavaljendiKandidaadid.add(ajav);
		this.ajavaljendiKandidaatides.add(koht);
	}
	
	/**
	 *   Kas antud sonaga on seotud m6ni ajavaljendikandidaat (potentsiaalne ajavaljend)?
	 */
	public boolean onSeotudMoneAjavaljendiKandidaadiga(){
		return (this.ajavaljendiKandidaadid != null && (this.ajavaljendiKandidaadid).size()>0);
	}
	
	/**
	 *   Leiab, kas antud s6na v6ib olla tsiteeringu algus v6i l6pp. Tsiteeringu voimalikuks 
	 *  alguseks/l6puks loetakse s6na siis, kui yhel pool (aga mitte m6lemal pool!) temast 
	 *  paiknevad jutum2rgid.
	 */
	public boolean onVoimalikTsiteeringuLoppVoiAlgus(){
		String kontrollitavAlgSona = this.getAlgSonaErisymbolidNormaliseeritud();
		if (kontrollitavAlgSona != null){
			boolean beginQuote = (TextUtils.beginningOfQuote).matcher(kontrollitavAlgSona).matches();
			boolean endQuote   = (TextUtils.endOfQuote).matcher(kontrollitavAlgSona).matches();
			return ((beginQuote && !endQuote) || 
						(!beginQuote && endQuote) || 
							(beginQuote && endQuote && (kontrollitavAlgSona).length() == 1) );
		}
		return false;
	}
	
	/**
	 *   Milline on s6na paiknemine (paiknemiskohad) erinevates ajavaljendikandidaatides?
	 */
	public List<FraasisPaiknemiseKoht> getAjavaljendiKandidaatides() {
		return ajavaljendiKandidaatides;
	}

	/**
	 *   Millised ajavaljendikandidaadid on antud s6naga seotud?
	 */
	public List<AjavaljendiKandidaat> getAjavaljendiKandidaadid() {
		return ajavaljendiKandidaadid;
	}

	/**
	 *  Milline on s6na paiknemine (paiknemiskoht) arvsonafraasis?
	 */
	public FraasisPaiknemiseKoht getArvSonaFraasis() {
		return arvSonaFraasis;
	}

	public void setArvSonaFraasis(FraasisPaiknemiseKoht arvSonaFraasis) {
		this.arvSonaFraasis = arvSonaFraasis;
	}

	public Integer getArvSonaTaisArvVaartus() {
		return arvSonaTaisArvVaartus;
	}

	public void setArvSonaTaisArvVaartus(Integer arvSonaVaartus) {
		this.arvSonaTaisArvVaartus = arvSonaVaartus;
	}
	
	public Double getArvSonaMurdArvVaartus() {
		return arvSonaMurdArvVaartus;
	}

	public void setArvSonaMurdArvVaartus(Double arvSonaMurdArvVaartus) {
		this.arvSonaMurdArvVaartus = arvSonaMurdArvVaartus;
	}

	public boolean isOnPotentsiaalneVahemikuAlgus() {
		return onPotentsiaalneVahemikuAlgus;
	}

	public void setOnPotentsiaalneVahemikuAlgus(boolean onPotentsiaalneVahemikuAlgus) {
		this.onPotentsiaalneVahemikuAlgus = onPotentsiaalneVahemikuAlgus;
	}

	public boolean isOnPotentsiaalneVahemikuLopp() {
		return onPotentsiaalneVahemikuLopp;
	}

	public void setOnPotentsiaalneVahemikuLopp(boolean onPotentsiaalneVahemikuLopp) {
		this.onPotentsiaalneVahemikuLopp = onPotentsiaalneVahemikuLopp;
	}

	public int getStartPosition() {
		return startPosition;
	}

	public void setStartPosition(int startPosition) {
		this.startPosition = startPosition;
	}

	public int getEndPosition() {
		return endPosition;
	}

	public void setEndPosition(int endPosition) {
		this.endPosition = endPosition;
	}
	
	/**
	 *  Tagastab positsiooni systeemisiseses tokeniseerimises.
	 */
	public int getInnerTokenPosition() {
		return this.innerTokenPosition;
	}

	/**
	 *  Seadistab positsiooni systeemisiseses tokeniseerimises.
	 */
	public void setInnerTokenPosition(int innerTokenPosition) {
		this.innerTokenPosition = innerTokenPosition;
	}

	/**
	 *  Kas esialgne t3-olp sisend on selle s6na kohalt katki tehtud?
	 */
	public boolean isAtTokenBreakPosition() {
		return this.atTokenBreakPosition;
	}

	public void setAtTokenBreakPosition(boolean atTokenBreakPosition) {
		this.atTokenBreakPosition = atTokenBreakPosition;
	}

	public String getAlgSonaYmbritsevateMarkideta() {
		if (this.algSonaYmbritsevateMarkideta != null){
			return this.algSonaYmbritsevateMarkideta;
		} else {
			return this.getAlgSona();
		}
	}
	
	public String getAlgSonaErisymbolidNormaliseeritud() {
		if (this.algSonaErisymbolidNormaliseeritud != null){
			return this.algSonaErisymbolidNormaliseeritud;
		} else {
			return this.getAlgSona();
		}
	}

	public GRAMMATILINE_AEG getGrammatilineAeg() {
		return grammatilineAeg;
	}

	public void setGrammatilineAeg(GRAMMATILINE_AEG verbiAeg) {
		this.grammatilineAeg = verbiAeg;
	}
	
}
