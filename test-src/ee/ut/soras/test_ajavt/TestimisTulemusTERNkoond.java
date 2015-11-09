//  Evaluation tools for Ajavt
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

package ee.ut.soras.test_ajavt;

/**
 *   Testimise koondtulemus, kus on eraldi v2lja toodud ilmutatud kujul valjendite (tekstis reaalselt
 *   esinevad valjendid) testimise tulemus ning varjatud kujul valjendite (tekstile "t6lgendamisel"
 *   lisatud v2ljendid) testimise tulemus.
 *    
 *   @author Siim Orasmaa
 */
public class TestimisTulemusTERNkoond {

	private TestimisTulemusTERN tulemusIlmutatudValjendid = null;
	private TestimisTulemusTERN tulemusVarjatudValjendid  = null;
	
	public TestimisTulemusTERNkoond(
			TestimisTulemusTERN tulemusIlmutatudValjendid,
			TestimisTulemusTERN tulemusVarjatudValjendid) {
		super();
		this.tulemusIlmutatudValjendid = tulemusIlmutatudValjendid;
		this.tulemusVarjatudValjendid = tulemusVarjatudValjendid;
	}

	public TestimisTulemusTERN getTulemusIlmutatudValjendid() {
		return tulemusIlmutatudValjendid;
	}

	public void setTulemusIlmutatudValjendid(
			TestimisTulemusTERN tulemusIlmutatudValjendid) {
		this.tulemusIlmutatudValjendid = tulemusIlmutatudValjendid;
	}

	public TestimisTulemusTERN getTulemusVarjatudValjendid() {
		return tulemusVarjatudValjendid;
	}

	public void setTulemusVarjatudValjendid(
			TestimisTulemusTERN tulemusVarjatudValjendid) {
		this.tulemusVarjatudValjendid = tulemusVarjatudValjendid;
	}
	
}
