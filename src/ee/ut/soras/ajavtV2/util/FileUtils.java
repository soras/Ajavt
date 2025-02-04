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

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

public class FileUtils {

	/**
	 *  Kirjutab etteantud sisu content uude faili nimega outputFileName kasutades kodeeringut encoding.
	 */
	public static void printIntoFile(String content, String encoding, String outputFileName) throws Exception {
		// Stream to write file
		FileOutputStream fout = null;
	    // Open an output stream
	    fout = new FileOutputStream (outputFileName);
	    OutputStreamWriter out = 
	    	new OutputStreamWriter(new BufferedOutputStream(fout), encoding);
	    // Pring content
	    out.write(content);
	    out.flush();
	    out.close();
	    // Close our output stream
	    fout.close();
	}
	
	/**
	 *  Modifitseerib etteantud failinime originalFileName, muutes 2ra selle laiendi
	 *  (uueks laiendiks saab newFileExtension) ning kirjutab etteantud sisu content
	 *  uue nimega faili.
	 *  <p>
	 *  Tagastab true, kui kogu operatsioon 6nnestus.
	 */
	public static boolean printIntoFile(String content, String encoding, 
										String originalFileName, String newFileExtension) throws Exception {
		// Muudame 2ra laiendi
		File oldFile = new File(originalFileName);
		String oldFileName = oldFile.getName();
		if (oldFileName != null && oldFileName.length() > 0){
			String namePart = oldFileName.replaceAll("\\.[^.]+$", "");
			if (namePart != null && namePart.length() > 0){
				String newName  = namePart + "." + newFileExtension;
				// Stream to write file
				FileOutputStream fout = null;
			    // Open an output stream
			    fout = new FileOutputStream (newName);
			    OutputStreamWriter out = 
			    	new OutputStreamWriter(new BufferedOutputStream(fout), encoding);
			    // Pring content
			    out.write(content);
			    out.flush();
			    out.close();
			    // Close our output stream
			    fout.close();
			    return true;
			}
		}
		return false;
	}
	
}
