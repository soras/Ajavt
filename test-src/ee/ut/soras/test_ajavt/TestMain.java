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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ee.ut.soras.ajavtV2.AjaTuvastaja;
import ee.ut.soras.ajavtV2.JarelTootlus;
import ee.ut.soras.ajavtV2.Main;
import ee.ut.soras.ajavtV2.util.LogiPidaja;
import ee.ut.soras.ajavtV2.util.TextUtils;
import ee.ut.soras.wrappers.EstyhmmWrapper;
import ee.ut.soras.wrappers.impl.EstyhmmWrapperImpl;

/**
 *   Ajav&auml;ljendite tuvastaja automaatne testimine.
 *   <p>
 *   Puhastab testkorpuse m&auml;rgenditest, laseb ajav&auml;ljendite tuvastajast l&auml;bi
 *   ning v&otilde;rdleb automaatselt saadud tulemust k&auml;sitsi m&auml;rgendusega. 
 *   V&auml;ljastab statistika m&auml;rgenduse t&auml;psuse, saagise jm kohta.
 *   <p>
 *   Lisaks v&otilde;ib tulemust automaatselt v&otilde;rrelda viimase saadud tulemusega,
 *   kui see on kataloogis logifailina olemas.
 *   <p>
 *   <p>
 *   NB: Ajaväljendite joondamist jälgides võib märgata, et väljendid testkopruses ja süsteemi 
 *   väljundis on kohati nihkes paari sümboli võrra. Nähtus on tingitud sellest, et enne kuvamist 
 *   eemaldatakse ajaväljendite ümbert tühikud, küll aga arvestatakse neid tühikuid ajaväljendi-
 *   positsioonide määramisel;
 * 
 *    @author Siim Orasmaa
 */
public class TestMain {

	// Testkorpuse lokatsioon
	private static String testCorpusLoc    = null;
	// Tekstkorpuse morfoloogilise analyysi lokatsioon
	private static String morphAnalysisLoc = null;
	
	
	// Ajaväljendite tuvastaja lokatsioon
	private static String ajaVTLoc = null;

	
	// Märgenduse standardile vastavuse valideerimine: XSD asukoht
	private static String timeMLxsdLoc = null;

	// Märgenduse standardile vastavuse valideerimine: DTD asukoht
	private static String timeMLdtdLoc = null;
	
	
	// Yhestajat k2ivitav k2sk v6i skripti lokatsioon (vaikimisi kasutatav)
	private static String yhestajaCmd = "t3mesta -Y -cio utf8 +1";
	
	// Kaust, kuhu l2hevad tulemused (automaatm2rgenduse v6rdlus k2sitsim2rgendusega + 
	// tulemuste v6rdlus eelmiste tulemustega) ...
	private static String resultsDir = null;
	
	// Tulemuste esitamise numbriformaat
	private static DecimalFormat dFormatter = null;
	
	// Ajavaljendite tuvastaja implementatsioon
	private static AjaTuvastaja tuvastaja = null;
	
	// Sisendiks antud reeglifail: ainult juhtudel, kui soovitakse kasutada vaikereeglite asemel midagi muud.
	// Kui testkorpusega on juba seotud mingi reeglifail, siis sisendiks antud reeglifaili ei kasutata.
	private static String inputRulesFile = null;	
	
	
	/**
	 *   Peameetod. Jooksutab testi kõigil kataloogi alamkorpustel.
	 */
	public static void main(String[] args) {
		 boolean noDetails      = false;    // hindamisel ei kuvata detaile
		 boolean compareToLast  = false;    // võrdlemine viimase tulemusega
		 boolean debugLogi      = false;    // täiemahuline logimine
		 boolean onlyPureTimeML = false;    // lubame ainult puhast TimeML-i
		 boolean measureSpeed   = false;    // mõõdame protsessimise kiirust
		 boolean forceMorphAnalysis = false; // forseeritakse morf analyysi sooritamine,
		                                     // yhtlasi kirjutatakse yle eelneva morf 
		                                     // analyysi tulemus failis
		 if (args.length > 0){
			 for (int i = 0; i < args.length; i++) {
				if (args[i].equals("-noDetails")){
					noDetails = true;
				}
				if (args[i].equals("-compareToLast")){
					compareToLast = true;
				}
				if (args[i].equals("-debug")){
					debugLogi = true;
				}
				if (args[i].equals("-ajaVTLoc") && (i + 1 < args.length)){
					ajaVTLoc = args[i+1]; 
				}
				if (args[i].equals("-corpusLoc") && (i + 1 < args.length)){
					testCorpusLoc = args[i+1];
				}
				if (args[i].equals("-morfLoc") && (i + 1 < args.length)){
					morphAnalysisLoc = args[i+1];
				}
				if (args[i].equals("-validateTimeML") && (i + 1 < args.length)){
					timeMLxsdLoc   = args[i+1];
					onlyPureTimeML = true;
				}
				if (args[i].equals("-resultsLoc") && (i + 1 < args.length)){
					resultsDir = args[i+1];
				}
				if (args[i].equals("-rulesLoc") && (i + 1 < args.length)){
					inputRulesFile = args[i+1];
				}
				if (args[i].equals("-yhest") && (i + 1 < args.length)){
					yhestajaCmd = args[i+1]; 
					// Eeldame, et yhestaja lokatsioon v6ib tulla mitmeosalisena (terve k2suna)
					int j = i + 2;
					while (j < args.length && !(args[j].startsWith("-"))){
						yhestajaCmd += " " + args[j];
						j++;
					}
					forceMorphAnalysis = true;
				}				
			}
		 }
		 // Check for existence of important directories
		 if (testCorpusLoc == null || !(new File(testCorpusLoc)).exists()){
			 System.err.println("Unable to locate test corpus dir: \n  "+testCorpusLoc);
			 System.exit(-1);
		 }
		 if (!forceMorphAnalysis && (morphAnalysisLoc == null || !(new File(morphAnalysisLoc)).exists())){
			 System.err.println("Unable to locate test corpus morf analysis dir: \n  "+morphAnalysisLoc);
			 System.exit(-1);			 
		 }
		 if (ajaVTLoc == null || !(new File(ajaVTLoc)).exists()){
			 System.err.println("Unable to locate ajavt dir: \n  "+ajaVTLoc);
			 System.exit(-1);
		 }
		 if (onlyPureTimeML && timeMLxsdLoc == null){
			 System.err.println("Unable to locate TimeML xsd: \n  "+timeMLxsdLoc);
			 System.exit(-1);
		 }
		 if (resultsDir == null || !(new File(resultsDir)).exists()){
			 System.err.println("Unable to locate directory for results: \n  "+resultsDir);
			 System.exit(-1);			 
		 }
		 // Yhestaja lokatsiooni ei kontrolli: see v6ib tulla terve k2suna ...
		 initializeDecimalFormatter();
		 String tulemusteFail = null;
		 if (!compareToLast || (compareToLast && !noDetails)){
			 LogiPidaja testLog = viiLabiTERNTestimineKorpusKaustas(testCorpusLoc, "UTF-8", resultsDir, noDetails, debugLogi, onlyPureTimeML, measureSpeed, forceMorphAnalysis);
			 tulemusteFail = testLog.getRaportiFailiNimi();
		 }
		 // Kui n6utud, v6rdleme saadud tulemust eelmise tulemusega ...
		 if (compareToLast){
		    Abimeetodid.compareResultToLastLoggedResult( new File( resultsDir ), tulemusteFail, "test_", "",  "UTF-8" ); // Kui tulemustefail on null, v6rdleme viimase tulemusega
		 }
	}
	
	/**
	 * 
	 *   Viib testimise l2bi etteantud korpusekaustas, eeldadase korpusefailidelt etteantud kodeeringut ning
	 *  kuvades vastavalt vajadusele testimistulemuste detaile (st ajav2ljendite joonduseid ja vigu). Yhtlasi
	 *  trykitakse tulemused ka ekraanile.
	 *  <p>
	 *  Testimisel kasutatakse TERN-stiilis hindamist: iga TIMEX atribuudi kohta tuuakse eraldi v2lja 
	 *  saagis ja tapsus.
	 *  <p>
	 *  Tagastab logifaili, kuhu testimisraport kirjutati. Raportivoog tagastatavas <tt>LogiPidaja</tt>-s on
	 *  juba suletud.
	 */
	public static LogiPidaja viiLabiTERNTestimineKorpusKaustas(String korpuseKaust, 
			                                                      String kodeering,
			                                                 String tulemustekaust,
			                                                     boolean noDetails,
			                                                     boolean debugLogi, 
			                                                boolean onlyPureTimeML, 
			                                                  boolean measureSpeed, 
			                                            boolean forceMorphAnalysis){
		 File dir           = new File( korpuseKaust );
	 	 LogiPidaja testLog = new LogiPidaja(true, tulemustekaust + File.separator + "test");
	 	 // initsialiseerime tuvastaja 
	 	 initializeAjavt( ajaVTLoc, null, yhestajaCmd, ajaVTLoc );
 	 	 testLog.setKirjutaLogiValjundisse(true);
 	 	 String[] corpora = dir.list();
 	     if (corpora != null){
 	    	KiiruseTestiTulemus kiiruseTest = null;
 	    	if (measureSpeed){
 	    		kiiruseTest = new KiiruseTestiTulemus();
 	    	}
 	    	// Sorteerime, et j2rjekord oleks "platvormists6ltumatu" ...
 	 	    List<String> corporaList = Arrays.asList(corpora);
 	 	    Collections.sort(corporaList, String.CASE_INSENSITIVE_ORDER);
 	 	    TestimisTulemusTERNkoond overAllResult = null;
 	        for (int i = 0; i < corporaList.size(); i++) {
 	            // Get filename of file or directory
 	            String filename = corporaList.get(i);
 	            if (filename.endsWith(".tml")){
 	 	            testLog.println("----------------------------------------------");
 	 	            testLog.println("   "+filename+"");
 	 	            testLog.println("----------------------------------------------");
 	 	            String customRuleFile = null;
 	 	            String refTimeAndTexts [] = eraldaKorpusestPuhasJaMargendatudTekstTimeML(
 	 	            								testCorpusLoc+File.separator+filename, kodeering);
 	 	            if (refTimeAndTexts[3] != null){
 	 	            	customRuleFile = refTimeAndTexts[3]; 
 	 	            }
 		 	        List<MargendatudAjavaljend> kasitsiMargendatud = Abimeetodid.eraldaMargendatudValjendidTekstist(refTimeAndTexts[2]);
 		 	        List<MargendatudAjavaljend> automMargendatud = 
 				 	        	rakendaTekstilAjaVT( refTimeAndTexts[0], 
 				 	        						 refTimeAndTexts[1], 
 				 	        						 debugLogi,
 				 	        						 onlyPureTimeML,
 				 	        						 customRuleFile,
 				 	        						 testLog,
 				 	        						 kiiruseTest,
 				 	        				  forceMorphAnalysis,
 				 	        				         new File(dir, filename));
 		 	        TestimisTulemusTERNkoond uusTulemus = 
 		 	        	Abimeetodid.hindaMargendamiseTulemusiTERN( automMargendatud, kasitsiMargendatud, 
 		 	        								   testLog, noDetails, dFormatter, false );
 		 	        if (kiiruseTest != null){
 		 	        	kiiruseTest.printResultsOfLastTagging(testLog);
 		 	        }
 		 	        (uusTulemus.getTulemusIlmutatudValjendid()).setWordCount( (int) countWords(refTimeAndTexts[1])  );
 		 	        if (overAllResult == null){
 		 	        	overAllResult = uusTulemus;
 		 	        } else {
 		 	        	(overAllResult.getTulemusIlmutatudValjendid()).lisaUusTulemus(uusTulemus.getTulemusIlmutatudValjendid());
 		 	        	(overAllResult.getTulemusVarjatudValjendid()). lisaUusTulemus(uusTulemus.getTulemusVarjatudValjendid());
 		 	        } 	            	
 	            }
 	        }
 	        // L6puks: prindime valja koondtulemuse
 	        testLog.println(); 	        
 	        testLog.println("---------------------------------------------------------");
 		    testLog.println(" Koondtulemus "+(overAllResult.getTulemusIlmutatudValjendid()).getDocumentsTested()+" tekstifaili hindamisel");
 		    testLog.println(" (Kokku "+(overAllResult.getTulemusIlmutatudValjendid()).getWordCount()+" s6na)");
 		    testLog.println("---------------------------------------------------------");
 		    testLog.println();
	    	testLog.println(" Hinnati "+
	    			dFormatter.format( (overAllResult.getTulemusIlmutatudValjendid()).getRelevantItems("TIMEX") )
	    				+" kasitsi margendatud valjendi tuvastamist.");
		    double percentCommented = ((overAllResult.getTulemusIlmutatudValjendid()).getCommentCount()*100.0)/
		    							((int)(overAllResult.getTulemusIlmutatudValjendid()).getRelevantItems("TIMEX"));
			testLog.println(" Kommentaaridega ajavaljendeid: "+ (overAllResult.getTulemusIlmutatudValjendid()).getCommentCount()+
					        " ("+dFormatter.format(percentCommented)+"%)" );
 		    testLog.println();
	    	testLog.println(" Mikrokeskmised: ");
	    	testLog.println();
	    	(overAllResult.getTulemusIlmutatudValjendid()).calculateResults();
	    	testLog.println((overAllResult.getTulemusIlmutatudValjendid()).printResultTable(dFormatter));
 	        if (kiiruseTest != null){
 	        	kiiruseTest.printOverallResults(testLog);
 	        }
 	        if ((overAllResult.getTulemusVarjatudValjendid()).hasItemsExtractedOrRelevantOfType("TIMEX")){
 	 		    testLog.println();
 		    	testLog.println(" Mikrokeskmised (tekstilise sisuta margendid): ");
 		    	(overAllResult.getTulemusVarjatudValjendid()).calculateResults();
 		    	testLog.println();
 				int autoExtracted = (int)(overAllResult.getTulemusVarjatudValjendid()).getItemsExtracted("TIMEX");
 				int handExtracted = (int)(overAllResult.getTulemusVarjatudValjendid()).getRelevantItems ("TIMEX");
 				if (autoExtracted == -1){ autoExtracted = 0; }
 				if (handExtracted == -1){ handExtracted = 0; }
 			    testLog.println("  Automaatselt eraldatud: " + autoExtracted );
 			    testLog.println("       Kasitsi eraldatud: " + handExtracted );
 			    testLog.println();
 		    	testLog.println((overAllResult.getTulemusVarjatudValjendid()).printResultTable(dFormatter));
 	        }
 	     } else {
 	    	 System.err.println(" Ei 6nnestunud leida testikorpust: \n"+ testCorpusLoc);
 	     }
 	     testLog.setKirjutaLogiFaili(false);
 	     return testLog;
	}
	
	/**
	 * 
	 *   Laeb etteantud TimeML-annoteeritud korpuse failist m&auml;llu, eraldades kaks osa:
	 *   <ul>
	 *      <li> annoteerimata ehk m&auml;rgendusest puhastatud korpus (tagastatava massiivi element indeksiga 1)
	 *      <li> annoteeritud ehk esialgne korpus (tagastatava massiivi element indeksiga 2)
	 *   </ul>
     *  Lisaks salvestab tagastatava massiivi 0 elemendis korpuse p2isest eraldatud referentsaja. <br>
     *  Kui testimisel tuleb kasutada vaikereeglitest erinevat reeglikomplekti, tagastatakse uue reeglifaili
     *  nimi viimase elemendina (element indeksiga 3).
	 * 
	 * @param inputFileName testkorpus, millest margendid eemaldada
	 * @param kodeering korpuse tekstikodeering
	 * @return (referentsaeg, annoteerimata tekst, annoteeritud tekst)
	 */
	private static String [] eraldaKorpusestPuhasJaMargendatudTekstTimeML(String inputFileName, String kodeering){
		String returnable [] = {null, "", "", null};
		StringBuilder taggedText   = new StringBuilder();
		StringBuilder unTaggedText = new StringBuilder();
        boolean docCreationTimeFound = false;		
        try{		
    		// 1) Kirjutame vana faili sisu ümber, eemaldades annoteerimismärgendid
            FileInputStream in = new FileInputStream(inputFileName);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, kodeering));
            String strLine;
            int lineNR = 0;
            // 2) Loeme faili rida-rida haaval, eemaldades koik annotatsioonid...
            while ((strLine = br.readLine()) != null) {
            	// 2.0) Eemaldame mittevajaliku annotatsiooni
            	strLine = strLine.replaceAll("<\\?xml[^>]+>", "");
            	strLine = strLine.replaceAll("</?TimeML[^>]+>", "");
            	strLine = strLine.replaceAll("</?TimeML>", "");
            	// 2.1) Eraldame referentsaja 
           		Matcher matcher = (TextUtils.timeMLDocumentCreationTime).matcher(strLine);
           		if (matcher.find()){
           			// teine sobivus peakski andma meile vajaliku sisendkuup2eva
           			returnable[0] = matcher.group(2);
           			docCreationTimeFound = true;
           		}
            	// 2.2) Eraldame reeglifaili nime 
           		Matcher matcher2 = (TextUtils.customRuleFile).matcher(strLine);
           		if (matcher2.find()){
           			returnable[3] = matcher2.group(1);
           		}
                // 2.3) Jaadvustame 6igesti margendatud tekstirea
           		taggedText.append(strLine);
           		taggedText.append("\n");
                // 2.4) Kustutame tekstirealt ajaväljendi märgistuse, jaadvustame "puhta" rea            		
                strLine = strLine.replaceAll("<[^>]+>", "");
           		unTaggedText.append(strLine);
           		unTaggedText.append("\n");                
            	lineNR++;
            }
            // Sulgeme voo
            in.close();
        } catch (Exception e){ //Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
        returnable[1] = unTaggedText.toString();
        returnable[2] = taggedText.toString();
        if (!docCreationTimeFound){
   			System.err.println(" Ei suutnud leida dokumendi loomise kuup2eva korpusest\n" +
					inputFileName + "\n");
        }
		return returnable;
	}	
	
	
	/**
	 *   Rakendab annoteerimata tekstil automaatset ajavaljendite tuvastamist.
	 *   <p>
	 *   Kui lipuke <code>onlyPureTimeML</code> on seatud, kontrollib yhtlasi margenduse
	 *   vastavust TimeML standardi definitsioonile, mis antud vastavates DTD ja XSD 
	 *   failides. Logisse kuvatakse kontrolli tulemus.
	 */
	private static List<MargendatudAjavaljend> rakendaTekstilAjaVT(String referenceTime, 
																   String text, 
																   boolean debugLogi,
																   boolean onlyPureTimeML,
																   String customRuleFile,
																   LogiPidaja testLog,
																   KiiruseTestiTulemus kiiruseTest,
																   boolean forceMorphAnalysis,
																   File timexFile){
		String [] refAeg = Main.looSonePohjalReferentsAeg(referenceTime);
		try {
			// ---------------- Morfoloogilise analyysi hankimine
			String morphAnalysis = null;
			File morphFile = Abimeetodid.getMorphAnalysisFile(timexFile, morphAnalysisLoc);
			if (morphFile == null){
				throw new Exception(" Unable to construct morph analysis file name for "+timexFile+"!");
			}
			if (forceMorphAnalysis){
				// Forsseerime morfoloogilist analyysi: loome uue analyysi ja salvestame faili
				morphAnalysis = 
					Abimeetodid.performNewMorphologicalAnalysisAndSaveToFile(text, 
							                                            morphFile, 
							                                              "UTF-8",
							                                 tuvastaja.getWrapper());
			} else {
				// Kasutame ara vana, juba olemasolevat morfoloogilist analyysi
				morphAnalysis = Abimeetodid.fetchMorphologicalAnalysisFromFile(morphFile, "UTF-8");
			}

			// ---------------- Tuvastaja rakendamine
			List<String> tuvastatudAjavaljendid = null;
			if (customRuleFile != null){
				// Vahetame reeglifaili
				initializeAjavt( ajaVTLoc, customRuleFile, yhestajaCmd, ajaVTLoc );
			}
			if (kiiruseTest == null){
				// tuvastaja.setLogi(testLog);
				tuvastatudAjavaljendid = 
					JarelTootlus.konverteeriEraldamiseTulemusSonedeListiks(
						tuvastaja.tuvastaAjavaljendidTekstisTulemusPaiskTabelitena(refAeg, text, morphAnalysis, onlyPureTimeML), onlyPureTimeML, false);
			} else {
				tuvastatudAjavaljendid = 
					executeAjavtAndSpeedTests(refAeg, text, debugLogi, onlyPureTimeML, testLog, kiiruseTest);
			}
			//if (debugLogi){
			//	tuvastaja.tuvastaAjavaljendidDebug(refAeg, text, null);
			//}
			if (customRuleFile != null){
				// Paneme tagasi vaike-reeglifaili
				initializeAjavt( ajaVTLoc, null, yhestajaCmd, ajaVTLoc );
			}
			return Abimeetodid.eraldaMargendatudValjendidSonedest(tuvastatudAjavaljendid);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println(" Viga ajav2ljendite tuvastamisel tekstist...");
		}
		return null;
	}
	
	/**
	 *   K&auml;ivitab ajav&auml;ljendite tuvastaja koos kiirustestidega. Kuna t3mesta't kutsutakse
	 *   v2lja 2 korda, v6ib ka eeldada, et kogu protsess v6tab aega ligi 2x kauem.
	 */
	private static List<String> executeAjavtAndSpeedTests(String [] refAeg, 
															   String text, 
			   									         boolean debugLogi,
			   									    boolean onlyPureTimeML, 
			   									        LogiPidaja testLog,
			   									  KiiruseTestiTulemus kiiruseTest) throws Exception{
		List<String> tuvastatudAjavaljendid = null;
		try {
			// Selguse m6ttes m66dame ka ainult t3mesta aega. NB! See teeb koguprotsessi ysna palju aeglasemaks
			long start = System.currentTimeMillis();
			String morfAnalyysiTulemus = (tuvastaja.getWrapper()).process( text );
			long end = System.currentTimeMillis();
			kiiruseTest.addT3MestaTimes( end - start );
			
			tuvastatudAjavaljendid = 
				tuvastaja.tuvastaAjavaljendidTekstisDebugSpeed(refAeg, text, null, onlyPureTimeML);
			kiiruseTest.addAjavtProcessTimes( tuvastatudAjavaljendid.remove(tuvastatudAjavaljendid.size()-1) );
			kiiruseTest.addPreprocessExecTimes( tuvastatudAjavaljendid.remove(tuvastatudAjavaljendid.size()-1) );
			kiiruseTest.addFileSizeInBytes( (long)(text.getBytes("UTF8")).length );
			kiiruseTest.addFileSizeInCPs( (long)(text.codePointCount(0, text.length())) );
		} catch (UnsupportedEncodingException e) {
			throw e;
		} catch (Exception e) {
			throw e;
		}		
		return tuvastatudAjavaljendid;
	}
	
	/**
	 *  Initializes temporal expressions resolver component. Logic for choosing the rules 
	 *  file is following: 1) if customRuleFile is not specified, uses inputRulesFile 
	 *  (if specified) or "reeglid.txt" located in programDir (if inputRulesFile not 
	 *  specified); 2) if customRuleFile is specified, it will be always preferred over 
	 *  the other options. In case of inputRulesFile and customRuleFile - if they contain 
	 *  no path symbols (File.separator), it is assumed that they are in programDir, 
	 *  otherwise, full paths are expected.
	 * 
	 * @param programDir program working directory; 
	 * @param customRuleFile location of custom rule file; 
	 * @param yhestajaCmd directory for the script that launches morphological analyser; 
	 * @param tempFilesDir directory where temporal files are located;
	 */
	private static void initializeAjavt(String programDir, String customRuleFile, String yhestajaCmd, String tempFilesDir){
		tuvastaja = new AjaTuvastaja();
		// 1) M22rame reeglifaili
		if (customRuleFile == null){
			if (inputRulesFile == null){
				tuvastaja.setReegliFail( programDir + File.separator + "reeglid.xml" );				
			} else if ( inputRulesFile.indexOf(File.separator) == -1 ){
				// If rulefile contains no path symbols, assume that it is in programDir 
				tuvastaja.setReegliFail( programDir + File.separator + inputRulesFile );
			} else {
				tuvastaja.setReegliFail( inputRulesFile );
			}
		} else if ( customRuleFile.indexOf(File.separator) == -1 ){
			// If rulefile contains no path symbols, assume that it is in programDir 
			tuvastaja.setReegliFail( programDir + File.separator + customRuleFile );
		} else {
			tuvastaja.setReegliFail( customRuleFile );
		}		
		// 2) M22rame ise yhestaja-wrapperi ...		
		//EstyhmmWrapper wrapper = new EstyhmmTempFilesWrapperImpl(yhestajaCmd, new File(tempFilesDir), "UTF8");
		EstyhmmWrapper wrapper = new EstyhmmWrapperImpl( yhestajaCmd, "UTF8" );
		tuvastaja.setWrapper(wrapper);
	}
	
	/**
	 *    Initsialiseerib komaarvude formaatija selliselt, et selle tulemus oleks yle k6ikide
	 *  lokaatide ja platvormide yhene.
	 */
	private static void initializeDecimalFormatter(){
		DecimalFormatSymbols dfs = new DecimalFormatSymbols();
	    dfs.setDecimalSeparator('.');
	    dFormatter = new DecimalFormat("#.#", dfs);
	}
	
	/**
	 *  Leiab s6nade arvu etteantud tekstis.
	 */
	private static long countWords(String text){
		Pattern whiteSpace = Pattern.compile("(\\s)");
		long wordCount = 0;
		boolean foundWordComponentsBeforeCurrentLoc = false;
		for (int i = 0; i < text.length(); i++) {
			String s = text.substring(i, i+1);
			if ((whiteSpace.matcher(s)).matches()){
				if (foundWordComponentsBeforeCurrentLoc){
					wordCount++;
				}
				foundWordComponentsBeforeCurrentLoc = false;
			} else {
				foundWordComponentsBeforeCurrentLoc = true;
			}
		}
		return wordCount;
	}
	
}
