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

package ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus;

import java.util.HashMap;
import java.util.List;

import org.joda.time.Period;

import ee.ut.soras.ajavtV2.mudel.ajavaljend.Granulaarsus;

/**
 *  Abstraktne ajaobjekt. 
 *  
 *  @see AjaPunkt
 *  @see AjaKestvus
 *      
 *  @author Siim Orasmaa
 */
public interface AjaObjekt {
	
	/**
	 * Ajaobjekti v6imalikud tyybid.
	 */
	public static enum TYYP {
		/** TIMEX3 tyybid DATE ja TIME. */
		POINT,
		TIME,
		DURATION,
		/** TIMEX3 tyyp SET. */
		RECURRENCE,
		UNK;
		
		/**
		 *  Parsib tyybi etteantud s6nest;
		 */
		public static TYYP parsiTyyp(String tyyp){
			if (tyyp != null){
				for (TYYP type : values()) {
					if (type.toString().equals(tyyp)){
						return type;
					}
				}
			}
			return UNK;
		}
		
	};
	
	int getID();
	
	TYYP getType();
	
	void setField(Granulaarsus field, int value);
	
	void setField(Granulaarsus field, String label);
	
	void addToField(Granulaarsus field, int value);
	
	void addToField(Granulaarsus field, Period period, int direction);

	void seekField(Granulaarsus field, int direction, int soughtValue, boolean excludingCurrent);
	
	void seekField(Granulaarsus field, int direction, String soughtValue, boolean excludingCurrent);
	
	/**
	 *   Omistab väärtuse vastavale TIMEX3 atribuudile; Kui TIMEX3 atribuut juba omab mingit
	 *  väärtust, kirjutab vana väärtuse üle.
	 *  <p>
	 *   NB! Omistades väärtuse atribuudile <code>value</code>, kirjutatakse üle kalendripõhine/
	 *   arvutatav <code>value</code> väärtus. Kalendripõhise <code>value</code> saab taastada, 
	 *   omistades talle väärtuse <tt>null</tt>.
	 */
	void setTimex3Attribute(String attribute, String value);
	
	/**
	 *   Tagastab TIMEX3 atribuudi väärtuse või <code>null</code>, kui vastav atribuut on määramata.
	 */
	String getTimex3Attribute(String attribute);
	
	//==============================================================================
	//          G r a n u l a a r s u s t e - k a l e n d r i v 2 l j a d e
	//                    a v a m i n e    j a    s u l g m i n e
	//==============================================================================
	
	/**
	 *   Sulgeb granulaarsusest <tt>minGran</tt> v2iksemad granulaarsused (vastavalt ajaobjekti loogikale).
	 *   Kui <tt>digits > 0</tt>, kaetakse ka kinni granulaarsuse <tt>minGran</tt> vasakult vastav arv
	 *   positsioone;
	 *   <p>
	 *   NB! Kinnikatmine <tt>digits</tt> j2rgi on esialgu implementeeritud vaid ajapunktile ja 
	 *   aasta-granulaarsusele. 
	 */
	public void closeGranularitiesBelow(Granulaarsus minGran, int digits);
	
	
	public AjaObjekt clone();
	
	/**
	 *   Tagastab ajaobjekti kui TIMEX-tagi atribuutide v&auml;&auml;rtuste m&auml;&auml;rangute tabeli.
	 *   Tabelis on paarid (<i>atribuudi nimi</i>, <i>atribuudi väärtus</i>), nt (<i>type</i>, <i>DURATION</i>),
	 *   (<i>value</i>, <i>P3M</i>) jms; Kui mingi atribuut on ajaobjektis määramata, siis see tabelis ei esine.
	 *   <p>
	 *   Valikuline: kui <code>suffix</code> on määratud, lisatakse see iga atribuudinime lõppu. &Uuml;ldiselt võib selle
	 *   määramata jätta, eeskätt on see mõeldud just kasutamiseks vahemike erinevate otspunktide märgistamisel;
	 */
	public HashMap<String, String> asHashMapOfAttributeValue(String suffix);
	
	/**
	 *   Seob antud objekti kylge implitsiitselt/varjatult avalduva ajaobjekti. Tavaliselt annavad sellised objektid
	 *   edasi semantikat, millele pole tekstis otseselt viidatud/rohutud. Nt valjendis "nelja paeva jooksul" on 
	 *   ilmutatud kujul kestvus ("4 paeva") ning varjatud kujul sisalduvad seal kestvuse otspunktid ("paevast N", 
	 *   "paevani M"). 
	 */
	public void addRelatedImplicitTIMEX(AjaObjekt objekt);

	/**
	 *   Tagastab antud ajavaljendiga seotud implitsiitselt avalduvad ajaobjektid. Tavaliselt annavad sellised objektid
	 *   edasi semantikat, millele pole tekstis otseselt viidatud/rohutud. Nt valjendis "nelja paeva jooksul" on 
	 *   ilmutatud kujul kestvus ("4 paeva") ning varjatud kujul sisalduvad seal kestvuse otspunktid ("paevast N", 
	 *   "paevani M"). 
	 *   <p>
	 *   Kui kaesoleva objektiga varjatud ajaobjekte seotud pole, tagastab <code>null</code>. 
	 */
	public List<AjaObjekt> getRelatedImplicitTIMEXES();
	
	/**
	 *   Seob antud objekti kylge tekstis ilmutatult avalduva ajaobjekti. Sellisteks objektideks on tavaliselt
	 *   keerukamate ajaväljendifraaside alamosad, näiteks vahemike ilmutatud otspunktid (fraasis "4.-5. aprillil"),
	 *   rinnastusseoses olevad ajapunktid ("2., 4. ja 6. mail") või keerukamad kestvused (fraasis "6-10 päeva").
	 *   Oluline on, et eeltoodud fraasid eraldatakse süsteemi poolt tervikuna, ent semantika avaldamisel märgendatakse
	 *   alamosad eraldi (nt "[4.]-[5. aprillil]");
	 */
	public void addRelatedExplicitTIMEX(AjaObjekt objekt);
	
	/**
	 *   Tagastab antud ajavaljendiga seotud eksplitsiitselt avalduvad ajaobjektid. Sellisteks objektideks on tavaliselt
	 *   keerukamate ajaväljendifraaside alamosad, näiteks vahemike ilmutatud otspunktid (fraasis "4.-5. aprillil"),
	 *   rinnastusseoses olevad ajapunktid ("2., 4. ja 6. mail") või keerukamad kestvused (fraasis "6-10 päeva").
	 *   Oluline on, et eeltoodud fraasid eraldatakse süsteemi poolt tervikuna, ent semantika avaldamisel märgendatakse
	 *   alamosad eraldi (nt "[4.]-[5. aprillil]");
	 *   <p>
	 *   Kui kaesoleva objektiga ilmutatud semantikaga ajaobjekte seotud pole, tagastab <code>null</code>.
	 */
	public List<AjaObjekt> getRelatedExplicitTIMEXES();	
	
	/**
	 *   Kirjutab yle juba varasemalt antud objekti kylge seotud eksplitsiitse ajav2ljendi. Yle kirjutatakse objekt,
	 *   mille indeks meetodi <code>getRelatedExplicitTIMEXES()</code> poolt tagastatavas listis on <code>index</code>;
	 *   Kui sellise indeksiga elementi listis ei leidu, ei tehta midagi. Uue elemendi lisamiseks listi tuleks kasutada
	 *   meetodit <code>addRelatedExplicitTIMEX(AjaObjekt objekt)</code>.
	 */
	public void setRelatedExplicitTIMEX(int index, AjaObjekt objekt);
	
	/**
	 *   Leiab, kas antud ajavaljendiga seotud eksplitsiitselt avalduvad ajaobjektid moodustavad ajaintervalli.
	 *   Sisuliselt kontrollib seotud eksp ajaobjektide olemasolu ning seda, kas ajaobjektide arv on 2.
	 */
	public boolean hasRelatedExplicitTimexesAsInterval();
}
