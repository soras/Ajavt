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

package ee.ut.soras.ajavtV2.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logide kirjutamist haldav klass.
 * 
 * @author Siim
 */
public class LogiPidaja {

	private boolean kirjutaLogiFaili       = false;
	private boolean kirjutaLogiValjundisse = false;
	private PrintStream raportivoog = null;
	private String failinimePrefiks = "logi";
	private String raportiFailiNimi = "";
	private String kodeering        = "UTF-8";
	
	public LogiPidaja(boolean kirjutaLogiFaili) {
		setKirjutaLogiFaili(kirjutaLogiFaili);
	}
	
	public LogiPidaja(boolean kirjutaLogiFaili, String failinimePrefiks) {
		this.failinimePrefiks = failinimePrefiks;		
		setKirjutaLogiFaili(kirjutaLogiFaili);
	}	
		
	private void initsialiseeriFailiVoog(){
		Date curDate = new Date();
		String raportiFailiNimi = konstrueeriRaportiFailiNimi(curDate);
		this.raportiFailiNimi = raportiFailiNimi;
		try
		{
			FileOutputStream fileOutput = new FileOutputStream(raportiFailiNimi, true);
			raportivoog = new PrintStream ( fileOutput, true, kodeering );
			println ("---------------------------------------------------------");
			println (curDate. toString());
			println ();
		}
		catch (IOException e)
		{
			e.printStackTrace();
			System.err.println ("Ilmnes t6rge ning faili "+raportiFailiNimi+" ei 6nnestunud andmeid lisada.");
			raportivoog = null;
		} 		
	}
	
	private String konstrueeriRaportiFailiNimi(Date curDate) {
		SimpleDateFormat formaat = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS");
		return this.failinimePrefiks+"_" + formaat.format(curDate) + ".txt";
	}

	public void println(){
		print("\n");
	}	
	
	public void println(String line){
		print(line + "\n");
	}
	
	public void print(String line){
		if (raportivoog != null){
			raportivoog.print(line);
			raportivoog.flush();
		}
		if (kirjutaLogiValjundisse){
			System.out.print(line);			
		}
	}

	public void printErrStackTraces(Exception exp){
		if (raportivoog != null){
			exp.printStackTrace(raportivoog);			
		}
		exp.printStackTrace();
	}
	
	private void sulgeRaportiVoog()
	{
		if (raportivoog != null)
		{
			Date kuup2ev = new Date();
			println ("---------------------------------------------------------");
			println ( );
			println ( String.valueOf("T88 l6pp: " + kuup2ev. toString()) );
			println ( );
			raportivoog. close ();
		}
	}

	public boolean isKirjutaLogiFaili() {
		return kirjutaLogiFaili;
	}

	public void setKirjutaLogiFaili(boolean kirjutaLogiFaili) {
		if (!(this.kirjutaLogiFaili) && kirjutaLogiFaili){
			initsialiseeriFailiVoog();
		} else if (this.kirjutaLogiFaili && !kirjutaLogiFaili){
			sulgeRaportiVoog();
		}
		this.kirjutaLogiFaili = kirjutaLogiFaili;
	}
	
	public boolean isKirjutaLogiValjundisse() {
		return kirjutaLogiValjundisse;
	}

	public void setKirjutaLogiValjundisse(boolean kirjutaLogiValjundisse) {
		this.kirjutaLogiValjundisse = kirjutaLogiValjundisse;
	}

	public String getRaportiFailiNimi() {
		if (raportiFailiNimi != null && raportiFailiNimi.lastIndexOf(File.separator) > -1){
			int k = raportiFailiNimi.lastIndexOf(File.separator);
			return raportiFailiNimi.substring( k + ((File.separator).length()) );
		} else {
			return raportiFailiNimi;			
		}
	}
	
	public PrintStream getRaportiVoog(){
		return this.raportivoog;
	}
}
