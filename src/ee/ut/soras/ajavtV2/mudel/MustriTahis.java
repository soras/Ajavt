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

package ee.ut.soras.ajavtV2.mudel;

import ee.ut.soras.ajavtV2.mudel.ajavaljend.MustristSoltuv;

/**
 *  Mustriga seotud tahised. Ajavaljendi eraldamisel kantakse need tahised yle 
 *  ajavaljendikandidaadile, et liitmisreeglite p6hjal saaks otsustada, milliste
 *  tahistega ajavaljendid v6ivad liituda ja milliste tahistega ajavaljendid mitte.
 *  
 *  @author Siim Orasmaa
 */
public class MustriTahis extends MustristSoltuv {

	/**
	 *   Kas selle mustritahise alla kuuluv ajavaljend saab ta kuuluda ainult ajavaljendifraasi kooseisu (<tt>true</tt>)
	 *   voi voib see esineda tekstis teistest ajavaljenditest eraldi, soltumatult (<tt>false</tt>)? V22rtuse <tt>true</tt>
	 *   korral muudetakse t2hise alla kuuluv ajavaljendikandidaat mitte-eraldiseisvaks ning kui selle k6rvalt teisi 
	 *   kandidaate ei leita, siis see kustutatakse.
	 */
	private boolean ajavPoleEraldiSeisev = false;
	
	private String mustriTahised;

	public String getMustriTahised() {
		return mustriTahised;
	}

	public void setMustriTahised(String mustriTahised) {
		this.mustriTahised = mustriTahised;
	}
	
	public void setSeotudMustriOsa(String sisendSeotudMustriOsa){
		seotudMustriOsa = sisendSeotudMustriOsa;
	}

	public boolean isAjavPoleEraldiSeisev() {
		return ajavPoleEraldiSeisev;
	}

	public void setAjavPoleEraldiSeisev(boolean ajavPoleEraldiSeisev) {
		this.ajavPoleEraldiSeisev = ajavPoleEraldiSeisev;
	}
	
}
