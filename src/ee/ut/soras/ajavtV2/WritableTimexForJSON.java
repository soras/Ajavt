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

package ee.ut.soras.ajavtV2;

import java.util.HashMap;
import java.util.List;

import javax.json.stream.JsonGenerator;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.mudel.FraasisPaiknemiseKoht;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.AjavaljendiKandidaat;
import ee.ut.soras.ajavtV2.mudel.ajavaljend.arvutus.AjaObjekt;

/**
 *    Ajav2ljendikandidaat, mis tuleb kirjutada JSON formaadis objektiks. Yhtlasi sisaldab ka 
 *   loogikat ajav2ljendikandidaadi kirjutamiseks JSON objektina, kasutades etteantud 
 *   JsonGenerator'it;
 */
public class WritableTimexForJSON {
	
	private AjavaljendiKandidaat ajavaljendiKandidaat;
	private HashMap<String, String> timexAttributesAndValues;
	private FraasisPaiknemiseKoht kohtFraasis;
	
	public WritableTimexForJSON(AjavaljendiKandidaat ajavaljendiKandidaat,
								HashMap<String, String> timexfAttributesAndValues,
								FraasisPaiknemiseKoht kohtFraasis) {
		this.ajavaljendiKandidaat = ajavaljendiKandidaat;
		this.timexAttributesAndValues = timexfAttributesAndValues;
		this.kohtFraasis = kohtFraasis;
	}
	
	/**
	 *  Kirjutab antud ajav2ljendikandidaadi objektiks JSON objektide listis;
	 *  Kui ajav2ljendikandidaadi kyljes on lisaks ka ilma tekstilise sisuta kandidaate,
	 *  lisab ka need objektidena;
	 */
	public void writeAsJSONObject(JsonGenerator jsonGenerator, boolean usePurifiedTimeML){
		 jsonGenerator.writeStartObject();
		 //
		 // Kui kandidaat on fraasi alguses v6i on tegemist yhes6nalise kandidaadiga, kirjutame kogu TimeML m2rgenduse
		 //
		 if (kohtFraasis == FraasisPaiknemiseKoht.AINUSSONA || kohtFraasis == FraasisPaiknemiseKoht.ALGUSES){
			 	// Kirjutame atribuudid TID ja TEXT
				if (timexAttributesAndValues != null && timexAttributesAndValues.containsKey("tid")){
					jsonGenerator.write("tid", timexAttributesAndValues.get("tid"));
				}
				List<AjavtSona> fraas = ajavaljendiKandidaat.getFraas();
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < fraas.size(); i++) {
					AjavtSona ajavtSona = fraas.get(i);
					sb.append( ajavtSona.getAlgSona() );
					if (i+1 < fraas.size()){
						sb.append(" ");
					}
				}
				if (fraas != null && fraas.size() > 0){
					jsonGenerator.write( "text", sb.toString() );
				}
				// Kirjutame ylej22nud m2rgendid (alustame 1st: TID juba kirjutatud ja jaab valja)
				for (int i = 1; i < JarelTootlus.orderOfTIMEX3Attributes.length; i++) {
					String attrib = JarelTootlus.orderOfTIMEX3Attributes[i];
					if (timexAttributesAndValues != null && 
							timexAttributesAndValues.containsKey(attrib) && 
								timexAttributesAndValues.get(attrib) != null){
						jsonGenerator.write( attrib, timexAttributesAndValues.get(attrib) );
					}
				}
		 }
		 //
		 // Kui kandidaat on fraasi keskel v6i l6pus, kirjutame lihtsalt TID ja fraasi tekstiosa
		 //
		 else if (kohtFraasis == FraasisPaiknemiseKoht.KESKEL || kohtFraasis == FraasisPaiknemiseKoht.LOPUS){
			if (ajavaljendiKandidaat != null){
				if (timexAttributesAndValues != null && timexAttributesAndValues.containsKey("tid")){
					jsonGenerator.write("tid", timexAttributesAndValues.get("tid"));
				}
				List<AjavtSona> fraas = ajavaljendiKandidaat.getFraas();
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < fraas.size(); i++) {
					AjavtSona ajavtSona = fraas.get(i);
					sb.append( ajavtSona.getAlgSona() );
					if (i+1 < fraas.size()){
						sb.append(" ");
					}
				}
				if (fraas != null && fraas.size() > 0){
					jsonGenerator.write("text", sb.toString());
				}
			}
		 }
		 jsonGenerator.writeEnd();
		 //
		 //  Kui kandidaadiga on seotud ilma tekstilise sisuta ajav2ljendeid, lisame needki v2ljundisse ...
		 // 
		 if (kohtFraasis == FraasisPaiknemiseKoht.AINUSSONA || kohtFraasis == FraasisPaiknemiseKoht.LOPUS){
			 if (ajavaljendiKandidaat.getSemantikaLahendus() != null && (ajavaljendiKandidaat.getSemantikaLahendus()).getRelatedImplicitTIMEXES() != null){
				 List<AjaObjekt> relatedImplicitTIMEXES = (ajavaljendiKandidaat.getSemantikaLahendus()).getRelatedImplicitTIMEXES();
				 for (AjaObjekt ajaObjekt : relatedImplicitTIMEXES) {
					  jsonGenerator.writeStartObject();
					  HashMap<String, String> mapOfAttributesAndValues =
								(usePurifiedTimeML) ?
									(JarelTootlus.getPurifiedTimeMLAnnotation((ajaObjekt))) : 
										  ((ajaObjekt).asHashMapOfAttributeValue(""));
					  // Kirjutame implitsiitsed m2rgendid 
					  for (int i = 0; i < JarelTootlus.orderOfTIMEX3Attributes.length; i++) {
						  String attrib = JarelTootlus.orderOfTIMEX3Attributes[i];
						  if (mapOfAttributesAndValues != null && 
								  mapOfAttributesAndValues.containsKey(attrib) && 
								  	mapOfAttributesAndValues.get(attrib) != null){
								jsonGenerator.write( attrib, mapOfAttributesAndValues.get(attrib) );
						  }
					  }
					  jsonGenerator.writeEnd();
				 }
			 }			 
		 }
	}
	
	
}