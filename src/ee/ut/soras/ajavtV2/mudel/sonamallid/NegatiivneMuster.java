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

import java.util.List;
import java.util.regex.Pattern;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat;

/**
 *   Negatiivne muster filtreerimaks v&auml;lja ajavaljendikandidaate, mis tegelikult
 *  ei tohiks olla ajavaljenditena eraldatud (nagu nt "August" (Gailit) eraldamine kuunimena). 
 *  
 *  @author Siim Orasmaa
 */
public class NegatiivneMuster {

	/**
	 *   Mustri alguspositsioon, suhtestatuna eraldatud ajavaljendi s6napositsioonidega.
	 *  See t2hendab: -1 n2eb ette mustri sobitamise alustamist yks positsioon enne 
	 *  eraldatud ajavaljendikandidaadi algust; 0 tahendab, et sobitamist alustatakse 
	 *  kohakuti eraldatud ajavaljendikandidaadiga (esimesest s6nast) jne.  
	 */
	private int algusPositsioon = Integer.MIN_VALUE;
	
	/**
	 *  Muster s6nede kujul (regulaaravaldiste s6ne-kuju); 
	 */
	private String musterSonena []       = null;

	/**
	 *  Muster kompileeritud regulaaravaldistena; 
	 */
	private Pattern musterRegExpidena [] = null;
	
	
	/**
	 *  Kas sobitamisel s2ilitatakse s6na ymbritsevad m2rgid?
	 *  Vaikimisi ei s2ilitata; 
	 */
	private boolean sailitaYmbritsevadMargid = false;
	
	/**
	 *   Kompileerib regulaaravaldised <tt>musterRegExpidena</tt> vastavalt mustritele
	 *  <tt>musterSonena</tt>.
	 */
	private void kompileeriRegulaarAvaldised(){
		if (this.musterSonena != null && (this.musterSonena).length > 0){
			musterRegExpidena = new Pattern[ (this.musterSonena).length ];
			for (int i = 0; i < (this.musterSonena).length; i++) {
				String regExpDescript = this.musterSonena[i];
				try{
					musterRegExpidena[i] = Pattern.compile( regExpDescript );					
				} catch (Exception e) {
					// Kui kompileerimine eba6nnestus, jaab lihtsalt null vaartus
				}
			}	
		}
	}
	
	//==============================================================================
	//   	M u s t r i   s o b i t a m i n e 
	//==============================================================================	
	
	/**
	 *   Sobitab teksti negatiivse mustriga alates etteantud ajavaljendi alguspositsioonist.
	 *  Kui sobitamine 6nnestus terve mustri ulatuses, tagastab <tt>true</tt>.
	 */
	public boolean sobitaMustriga(List<AjavtSona> sonad, int ajavAlgusPos){
		if (this.algusPositsioon != Integer.MIN_VALUE){
			if (this.musterRegExpidena == null){
				kompileeriRegulaarAvaldised();
			}
			if (this.musterRegExpidena != null){
				int startPos = ajavAlgusPos + this.algusPositsioon;
				if (0 <= startPos){
					int i = 0;					
					while ( startPos + i < sonad.size() && 
								i < (this.musterRegExpidena).length ){
						AjavtSona sona  = sonad.get(startPos + i);
						Pattern pattern = this.musterRegExpidena[i];
						if (pattern != null){
							boolean wordMatches = false;
							if (!sailitaYmbritsevadMargid){
								wordMatches = 
									pattern.matcher(sona.getAlgSonaYmbritsevateMarkideta()).matches();
							} else {
								wordMatches = 
									pattern.matcher( sona.getAlgSonaErisymbolidNormaliseeritud() ).matches();
							}
							if (!wordMatches){
								return false;
							}
						} else {
							return false;
						}
						i++;
					}
					//  Kui oleme j6udnud mustri l6puni 6nnelikult,
					// saame deklareerida, et on sobitunud t2ies ulatuses
					if (i == (this.musterRegExpidena).length){
						return true;
					}
				}
			}			
		}
		return false;
	}
	
	/**
	 *    Sobitab eraldatud ajavaljendikandidaai all olevaid s6nu negatiivse mustriga ning kui 
	 *   leiab sobitumise, eemaldab ajavaljendikandidaadi s6nade kyljest.
	 */
	public void kontrolliNegatiivsetMustritJaVajaduselEemaldaAjav(List<AjavtSona> sonad, 
																 int ajavAlgusPos,
																 AjavaljendiKandidaat kandidaat){
		boolean sobitusMustriga = sobitaMustriga(sonad, ajavAlgusPos);
		if (sobitusMustriga){
			kandidaat.eemaldaEnnastSonadeKyljest();
		}
	}
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================
	
	public int getAlgusPositsioon() {
		return algusPositsioon;
	}

	public void setAlgusPositsioon(int algusPositsioon) {
		this.algusPositsioon = algusPositsioon;
	}

	public boolean isSailitaYmbritsevadMargid() {
		return sailitaYmbritsevadMargid;
	}

	public void setSailitaYmbritsevadMargid(boolean sailitaYmbritsevadMargid) {
		this.sailitaYmbritsevadMargid = sailitaYmbritsevadMargid;
	}

	/**
	 *   Annab negatiivse mustri kirjelduse, tyhikutega eraldatud regulaaravaldiste jada kujul.
	 *  N&auml;ide:
	 *  <pre>
	 *  /August/ /[A-Z][a-z]+/    
	 *  </pre>
	 */
	public void setMusterSonena(String musterSonena) {
		String potentsiaalsedAvaldised [] = musterSonena.split("\\s+");
		if ((potentsiaalsedAvaldised).length > 0){
			int sobivaidAvaldisi = 0;			
			// 1) Eemaldame regulaaravaldise margid ning loendame, palju on sobivaid avaldisi
			for (int i = 0; i < (potentsiaalsedAvaldised).length; i++) {
				String string = potentsiaalsedAvaldised[i];
				string = string.replace("/", "");
				potentsiaalsedAvaldised[i] = string;
				if (potentsiaalsedAvaldised[i].length() > 0){
					sobivaidAvaldisi++;
				}
			}
			// 2) Salvestame sobivad avaldised eraldi
			if (sobivaidAvaldisi > 0){
				this.musterSonena = new String[ sobivaidAvaldisi ];
				int j = 0;
				for (int i = 0; i < potentsiaalsedAvaldised.length; i++) {
					if (potentsiaalsedAvaldised[i].length() > 0){
						this.musterSonena[j++] = potentsiaalsedAvaldised[i];
					}
				}
			}
		}
	}

	public String toString(){
		StringBuilder sb = new StringBuilder();
		if (this.musterSonena != null){
			sb.append("-(");
			sb.append( (this.algusPositsioon) );
			sb.append(")- ");
			for (String alamMuster : this.musterSonena) {
				sb.append(alamMuster+" ");
			}
			sb.append(" --");
		} else {
			sb.append("-- null --");
		}
		return sb.toString();
	}
	
}
