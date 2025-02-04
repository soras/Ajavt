//  Evaluation tools for Ajavt
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
