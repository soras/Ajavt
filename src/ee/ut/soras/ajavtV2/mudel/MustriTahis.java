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
