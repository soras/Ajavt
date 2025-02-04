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
import java.util.List;
import java.util.StringTokenizer;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.Granulaarsus;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;

/**
 *   S6namalli semantikat defineeriv osa. Kuna see on k6igil s6namallidel
 *   suures osas yhine, on selle jaoks eraldi klass, mida k6ik s6namallid
 *   laiendavad.
 *   
 *   @author Siim
 */
public class SonaMalliSemOsa {
	
	private String granulaarsus        = null;
	private boolean semValueOnEbatapne = false;
	private String op                  = null;
	private String semLabel            = null;
	private String semValue            = null;
	private String direction           = null;
	private String mudel               = null;
	
	
	/**
	 *  Kas m6ni v22rtus on seatud meetodi 'lisa_' kaudu? 
	 *  Kui pole, on tegu tyhja sem osaga, mida pole erilist edasi anda.
	 */
	private boolean hasAnyValuesFilled    = false;

	public void lisaOp(String op){
		this.op = op;
		this.hasAnyValuesFilled = true;
	}
	
	public void lisaSemField(String semField){
		if (semField != null && semField.length()>0){
			this.granulaarsus = semField;
			this.hasAnyValuesFilled = true;
		}		
	}

	public void lisaSemValue(String semValue){
		this.semValue = semValue;
		this.hasAnyValuesFilled = true;
	}

	public void lisaSemLabel(String semLabel){
		this.semLabel = semLabel;
		this.hasAnyValuesFilled = true;
	}
	
	public void setSemValueOnEbatapne(boolean semValueOnEbatapne){
		this.semValueOnEbatapne = semValueOnEbatapne;
		this.hasAnyValuesFilled = true;
	}
	
	public void setSeekDirection(String seekDir){
		this.direction = seekDir;
		this.hasAnyValuesFilled = true;
	}
	
	public void setMudel(String mudel){
		this.mudel = mudel;
		this.hasAnyValuesFilled = true;		
	}
	
	public List<SemantikaDefinitsioon> tagastaMalliSemantilineOsa(AjavtSona viimaneSobitunudSona){
		if (this.op != null && (this.op).indexOf(",") > -1){
			// Kui operatsioone on m22ratud mitu (yksteisest komadega eraldatuna), siis eeldame, et
			// ka teisi v2lju on toodud mitu ning loome mitu semantikadefinitsioon objekti
			List<SemantikaDefinitsioon> semDefs = new ArrayList<SemantikaDefinitsioon>();
			StringTokenizer stForOp = new StringTokenizer(this.op, ",");
			StringTokenizer stForGran   = (this.granulaarsus != null) ? (new StringTokenizer(this.granulaarsus,",")) : (null);
			StringTokenizer stForValue  = (this.semValue     != null) ? (new StringTokenizer(this.semValue,",")) : (null);
			StringTokenizer stForLabel  = (this.semLabel     != null) ? (new StringTokenizer(this.semLabel,",")) : (null);
			StringTokenizer stForDirect = (this.direction    != null) ? (new StringTokenizer(this.direction,",")) : (null);
			StringTokenizer stForMudel  = (this.mudel        != null) ? (new StringTokenizer(this.mudel,","))     : (null);
			while (stForOp.hasMoreElements()) {
				String subOp = (String) stForOp.nextElement();
				SemantikaDefinitsioon semDef = new SemantikaDefinitsioon();
				// ---- op
				semDef.setOp(subOp);
				// ---- granulaarsus
				if (stForGran != null && stForGran.hasMoreElements()){
					Granulaarsus gran = Granulaarsus.getGranulaarsus(stForGran.nextToken());
					semDef.setGranulaarsus(gran);
				}
				// ---- semValue
				if (stForValue != null && stForValue.hasMoreElements()){
					semDef.setSemValue(stForValue.nextToken());
				}
				// ---- semLabel
				if (stForLabel != null && stForLabel.hasMoreElements()){
					semDef.setSemLabel(stForLabel.nextToken());
				}
				// ---- semDir
				if (stForDirect != null && stForDirect.hasMoreElements()){
					semDef.setDirection(stForDirect.nextToken());
				}
				// ---- sem lahendamise Mudel
				if (stForMudel != null && stForMudel.hasMoreElements()){
					semDef.setMudel(stForMudel.nextToken());
				}				
				semDefs.add(semDef);
			}
			return semDefs;
		} else {
			// Kui operatsioone pole yldse m22ratud v6i on vaid yks, eeldame vaikimisi, et ka teistel
			// v2ljadel on m22ratud maksimaalselt yks v22rtus, ning loome ainult yhe semantikadefinitsiooni
			if (this.hasAnyValuesFilled){
				List<SemantikaDefinitsioon> semDefs = new ArrayList<SemantikaDefinitsioon>();
					SemantikaDefinitsioon semDef = new SemantikaDefinitsioon();
					semDef.setGranulaarsus(Granulaarsus.getGranulaarsus(this.granulaarsus));
					semDef.setOp(this.op);
					semDef.setSemValue(this.semValue);
					semDef.setSemLabel(this.semLabel);			
					semDef.setSemValueOnEbatapne(this.semValueOnEbatapne);			
					semDef.setDirection(this.direction);
					semDef.setMudel(this.mudel);
					semDefs.add(semDef);
				return semDefs;
			} else {
				return null;
			}
		}
	}
	
	/**
	 *    Kui semantikav22rtused tuleb parsida k2esolevast s6namallist (nt konkreetne kuup2evanumber 
	 *   v6i kellaaja minutiosa), tagastab viited parsitavatele alamosadele.
	 *   <p>
	 *   Kui tagastusv22rtuseks on null, semantikav22rtust s6namallist parsima ei pea.
	 *   Tagastusv22rtuseks on massiiv viidetega s6namallide alamosadele, kust v22rtus v6tta.
	 *   V22rtus -1 massiivis t2histab samuti seda, et vastava semantikadefinitsiooni v22rtust
	 *   s6namallist parsima ei pea.
	 *   <p>
	 *   Praegu kasutusel ainult regulaaravaldiss6namallide puhul (alamosa==regulaaravaldise grupp) 
	 *   ning arvs6nafraasmallide puhul (alamosa != 0).  
	 */
	public int [] getSemValueReferences(){
		if (this.semValue != null){
			StringTokenizer st   = new StringTokenizer(this.semValue, ",");
			int [] referencesInt = new int [ st.countTokens() ];
			int i = 0;
			while (st.hasMoreElements()) {
				String possibleRef = (String) st.nextElement();
				referencesInt[i] = -1;
				if ((possibleRef).startsWith("REF:")){
					possibleRef = possibleRef.replaceAll("REF:", "");
					int referencePart = -1;
					try {
						referencePart = Integer.parseInt(possibleRef);
						referencesInt[i] = referencePart; 
					} catch (NumberFormatException e) {
					}
				}
				i++;
			}
			return referencesInt;
		}
		return null;
	}
}
