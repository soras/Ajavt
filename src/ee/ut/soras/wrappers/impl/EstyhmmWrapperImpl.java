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

package ee.ut.soras.wrappers.impl;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

import ee.ut.soras.wrappers.EstyhmmWrapper;
import ee.ut.soras.wrappers.SisendVooLugejaLoim;
import ee.ut.soras.wrappers.erind.EstYhmmErind;

/**
 *   Morf analysaatori m&auml;hise tavaline implementatsioon. Annab analyysitava 
 *  teksti morf analysaatori standardsisendisse, loeb tulemuse selle 
 *  standardv&auml;ljundist.  
 *  <p>
 *  
 * @author Siim Orasmaa
 */
public class EstyhmmWrapperImpl implements EstyhmmWrapper {
	
	/**
	 *  Morf analysaatorit k&auml;ivitav k&auml;sklus. 
	 */
	private String[] cmds = { "t3mesta", "-Y", "-cio", "utf8" };
	
	/**
	 *  Morf analysaatori sisendi ja v&auml;ljundi kodeering.
	 */
	private String charset = "UTF8";
	
	/**
	 *  Initsialiseerib uue morf analsaatori m&auml;hisklassi. Sisendiks (k&auml;surea-)k&auml;sk, mille abil
	 * morf analysaator k&auml;ivitatakse, ning sisend- ja v&auml;ljundteksti kodeering. 
	 * 
	 * @param cmd (k&auml;surea-)k&auml;sk morf analysaatori k&auml;ivitamiseks
	 * @param encoding v&auml;ljund- ja sisendteksti kodeering
	 */
	public EstyhmmWrapperImpl(String cmd, String encoding) {
		if (cmd != null && cmd.length() > 0){
			this.cmds = cmd.split("(\\s+)");
		}
		this.charset = encoding;
	}
	
	
	public String process(String text) throws Exception {
		// K2ivitame morf analysaatori eraldi protsessina
		Process p = null;
	    List<String> command = new ArrayList<String>( 4 );
	    for (String cmdName : cmds) { command.add( cmdName ); }
	    ProcessBuilder pb = new ProcessBuilder(command);
	    p = pb.start();

		// Loeme morf analysaatori v2ljundvoogudest...
		SisendVooLugejaLoim stdInLugeja = 
			new SisendVooLugejaLoim(p.getInputStream(), this.charset);
		SisendVooLugejaLoim stdErrLugeja = 
			new SisendVooLugejaLoim(p.getErrorStream(), this.charset);
		stdErrLugeja.start();
		stdInLugeja.start();
	    
		// Kirjutame morf analysaatori sisendvoogu
		BufferedWriter outToEstYhmm = new BufferedWriter(
				new OutputStreamWriter( p.getOutputStream(), this.charset ) 
                                                        );
		outToEstYhmm.append( text );
		outToEstYhmm.close();
	    
		// Ootame, kuni analysaator oma too l6petab
		int exitValue = p.waitFor();
		
		if (exitValue != 0){
			throw new EstYhmmErind("Exitvalue: "+ exitValue);
		}
		
		// Oluline! Ootame, kuni valjundvoogudest lugevad l6imed t88 l6petavad ...
		stdInLugeja.join();
		stdErrLugeja.join();
		
		p.getInputStream().close();
	    p.getOutputStream().close();
	    p.getErrorStream().close();
	    
		return stdInLugeja.getSisendVooValjund();
	}

}