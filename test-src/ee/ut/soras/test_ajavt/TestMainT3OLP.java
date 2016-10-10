//  Evaluation tools for Ajavt
//  Copyright (C) 2009-2016  University of Tartu
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
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ee.ut.soras.ajavtV2.AjaTuvastaja;
import ee.ut.soras.ajavtV2.JarelTootlus;
import ee.ut.soras.ajavtV2.Main;
import ee.ut.soras.ajavtV2.NumberTokenizer;
import ee.ut.soras.ajavtV2.util.LogiPidaja;
import ee.ut.soras.ajavtV2.util.TextUtils;

/**
 *   Ajav&auml;ljendite tuvastaja automaatne testimine <code>t3-olp-timex</code> korpusel.
 *   <p>
 *   Laseb <code>t3-olp-timex</code> korpuse ajav&auml;ljendite tuvastajast l&auml;bi,
 *   ning v&otilde;rdleb automaatselt saadud tulemust nn. "kuldstandard"-m&auml;rgendusega. 
 *   V&auml;ljastab statistika m&auml;rgenduse t&auml;psuse, saagise jm kohta.
 *   <p>
 * 
 *    @author Siim Orasmaa
 */
public class TestMainT3OLP {

	/**
	 *  t3-olp-timex testkorpuse lokatsioon. Yhes korpusefailis on koos
	 *  morf analyysi ning osalausestamise tulemused, iga token eraldi
	 *  real;
	 */
	private static String testCorpusLoc = null;
	
	/**
	 *  Hindamine: tulemusfailide ja v6rdluste kaust; 
	 */
	static String resultsDir = null;

	/** Ajaväljendite tuvastaja lokatsioon (vaikeasukoht). Kasutatakse reeglifailide leidmiseks. */
	private static String ajaVTLoc = null;
	
	
	/** Tulemuste esitamise numbriformaat */
	private static DecimalFormat dFormatter = null;
	
	/** Sisendiks antud reeglifail: ainult juhtudel, kui soovitakse kasutada vaikereeglite asemel midagi muud.
	 *  Kui testkorpusega on juba seotud mingi reeglifail, siis sisendiks antud reeglifaili ei kasutata. */
	private static String inputRulesFile = null;
	
	/** Ajavaljendite tuvastaja implementatsioon */
	private static AjaTuvastaja tuvastaja = null;
	
	/**
	 *   Peameetod. Jooksutab testi kõigil kataloogi alamkorpustel.
	 */
	public static void main(String[] args) {
		 boolean noDetails      = false;    // hindamisel ei kuvata detaile
		 boolean compareToLast  = false;    // võrdlemine viimase tulemusega
		 boolean debugLogi      = false;    // täiemahuline logimine
		 boolean onlyPureTimeML = false;    // lubame ainult puhast TimeML-i
		 boolean measureSpeed   = false;    // mõõdame protsessimise kiirust
		 String filterFilesByPrefix = null; // kas hinnata ainult teatud prefiksiga failidel?
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
				if (args[i].equals("-resultsLoc") && (i + 1 < args.length)){
					resultsDir = args[i+1];
				}
				if (args[i].equals("-rulesLoc") && (i + 1 < args.length)){
					inputRulesFile = args[i+1];
				}
				if (args[i].equals("-filterByPrefix") && (i + 1 < args.length)){
					filterFilesByPrefix = args[i+1];
				}
			}
		 }
		 // Check for existence of given directories
		 if (testCorpusLoc == null || !(new File(testCorpusLoc)).exists()){
			 System.err.println("Unable to locate test corpus dir: \n  "+testCorpusLoc);
			 System.exit(-1);
		 }
		 if (ajaVTLoc == null || !(new File(ajaVTLoc)).exists()){
			 System.err.println("Unable to locate ajavt dir: \n  "+ajaVTLoc);
			 System.exit(-1);
		 }
		 if (resultsDir == null || !(new File(resultsDir)).exists()){
			 System.err.println("Unable to locate directory for results: \n  "+resultsDir);
			 System.exit(-1);			 
		 }
		 initializeDecimalFormatter();
		 LogiPidaja testLog = 
			 viiLabiTERNTestimineT3OLPTMXKorpusKaustas(testCorpusLoc, "UTF-8", resultsDir, noDetails, debugLogi, onlyPureTimeML, measureSpeed, filterFilesByPrefix);
		 if (compareToLast){
		   // Kui tulemustefail on null, v6rdleme viimase tulemusega
			 String inputPrefix = "test_t3olp";
			 if (filterFilesByPrefix != null){
				inputPrefix += '_' + filterFilesByPrefix;
			 }
			 Abimeetodid.compareResultToLastLoggedResult( new File( resultsDir ),
					 (testLog != null) ? (testLog.getRaportiFailiNimi()) : (null), inputPrefix,    "_t3olp",   "UTF-8"  ); 
		 }
		 
	}
	
	//==============================================================================
	//       T e s t i m i n e   :     t 3 - o l p - t m x     s i s e n d
	//
	//     t 3 - o l p - t m x = 
	//       koondkorpuse morf analyys + yhestamine + lausestus + ajaväljendite
	//       märgendus, k6ik yhes failis
	//==============================================================================
	
	/**
	 * 
	 *   Viib testimise l2bi etteantud korpusekaustas, eeldadase korpusefailidelt etteantud kodeeringut ning
	 *  kuvades vastavalt vajadusele testimistulemuste detaile (st ajav2ljendite joonduseid ja vigu). Yhtlasi
	 *  trykitakse tulemused ka ekraanile.
	 *  <p>
	 *  Kui <tt>requiredFilePrefix != null</tt>, siis toimub hindamine ainult failidel, mille nimes on prefiks 
	 *  <tt>requiredFilePrefix</tt>.
	 *  <p>
	 *  Testimisel kasutatakse TERN-stiilis hindamist: iga TIMEX atribuudi kohta tuuakse eraldi v2lja 
	 *  saagis ja tapsus.
	 *  <p>
	 *  Tagastab logifaili, kuhu testimisraport kirjutati. Raportivoog tagastatavas <tt>LogiPidaja</tt>-s on
	 *  juba suletud.
	 */
	public static LogiPidaja viiLabiTERNTestimineT3OLPTMXKorpusKaustas(String korpuseKaust, 
			                                                              String kodeering, 
			                                                           String tulemusKaust,
			                                                             boolean noDetails,
			                                                             boolean debugLogi, 
			                                                        boolean onlyPureTimeML, 
			                                                          boolean measureSpeed,
			                                                          String requiredFilePrefix){
		 File dir = new File( korpuseKaust );
		 String logFilePath = tulemusKaust + File.separator + "test_t3olp";
		 if (requiredFilePrefix != null){
			logFilePath = tulemusKaust + File.separator + "test_t3olp_" + requiredFilePrefix;
		 }
		 LogiPidaja testLog = new LogiPidaja(true, logFilePath);
		 // initsialiseerime tuvastaja 
		 initializeAjavt( ajaVTLoc, null );
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
 	            // If required: apply the prefix filter and allow only file names passing the filter
 	            if (requiredFilePrefix!=null && !filename.startsWith(requiredFilePrefix)){
 	            	continue;
 	            }
 	            if (filename.endsWith(".t3-olp-tmx") || filename.endsWith(".t3-olp-ajav")){
 	 	            testLog.println("----------------------------------------------");
 	 	            testLog.println("   "+filename+"");
 	 	            testLog.println("----------------------------------------------");
 	 	            File t3olptmxFile = new File(dir, filename);
 	 	            
 	 	            List<MargendatudAjavaljend> kasitsiMargendatud = new LinkedList<MargendatudAjavaljend>();
 	 	            String sisendTekst    = null;
 	 	            String customRuleFile = null;
					try {
						String [] failiSisu = eraldaT3olpTmxFailistMargendatudTekst(t3olptmxFile, kasitsiMargendatud);
						sisendTekst = failiSisu[0];
						if (failiSisu.length > 1){
							customRuleFile = failiSisu[1];
						}
					} catch (Exception e) {
						e.printStackTrace();
						System.exit(-1);
					}
 	 	            List<MargendatudAjavaljend> automMargendatud =
 	 	            	rakendaTekstilAjaVT(kasitsiMargendatud.get(0), sisendTekst, debugLogi, 
	        			              		onlyPureTimeML, customRuleFile, testLog, kiiruseTest);
 	 	            
 		 	        TestimisTulemusTERNkoond uusTulemusKoond = 
 		 	        	Abimeetodid.hindaMargendamiseTulemusiTERN( automMargendatud, kasitsiMargendatud, 
 		 	        								               testLog, noDetails, dFormatter, true );
 		 	        (uusTulemusKoond.getTulemusIlmutatudValjendid()).setWordCount( (int) countWords( sisendTekst )  );
 		 	        if (overAllResult == null){
 		 	        	overAllResult = uusTulemusKoond;
 		 	        } else {
 		 	        	(overAllResult.getTulemusIlmutatudValjendid()).lisaUusTulemus(uusTulemusKoond.getTulemusIlmutatudValjendid());
 		 	        	(overAllResult.getTulemusVarjatudValjendid() ).lisaUusTulemus(uusTulemusKoond.getTulemusVarjatudValjendid());
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
	 *   Laeb etteantud t3-olp-tmx failist sisse mällu, eraldab sellest ajaväljendid (paigutab listi ajavaljendid)
	 *  ning tagastab massiivina: 1. element on faili tekstiline sisu, 2. element on reeglifail, mida tuleb
	 *  automaatm2rgendamisel kasutada (mittekohustuslik element, v6ib ka puududa).
	 */
	public static String [] eraldaT3olpTmxFailistMargendatudTekst(File t3olpTmxFile, List<MargendatudAjavaljend> ajavaljendid) throws Exception {
		// 1) Laeme faili mällu
		StringBuilder sb = new StringBuilder();			
		try {
			FileInputStream fstream = new FileInputStream(t3olpTmxFile);
		    DataInputStream in = new DataInputStream(fstream);
		    BufferedReader sisend = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		    String rida = null;
			while ((rida = sisend.readLine()) != null){
				if (rida.length() > 0){
					sb.append(rida + "\n");
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		// 2) Eraldame tekstist ajaväljendid
		return eraldaTekstJaAjavaljendid( new BufferedReader(new StringReader(sb.toString())), ajavaljendid );
	}
	
	/**
	 *   Eraldab puhverdatud <code>t3-olp-tmx</code> sisendtekstist (<code>input</code>) ajavaljendid,
	 *   paigutab ajav2ljendid sisendiks antud nimestikku <code>ajavaljendid</code> ning tagastab 
	 *   massiivi, kus:
	 *      1. element on ajav2ljenditeta <code>t3-olp</code> tekst, 
	 *      2. element on reeglifail, mida tuleb teksti automaatm2rgendamisel kasutada (mittekohustuslik 
	 *      element, v6ib ka puududa). 
	 *   <p>
	 *   Ajav2ljendite korrektse positsioneerimise saavutamiseks peab k2esoleva meetodi loogika vastama
	 *   meetodite <code>EelTootlus.eraldaAnalyysiTulemusedT3OLP</code> ja <code>EelTootlus.lisaUusAjavtSona</code> 
	 *   token'ite positsioneerimise loogikale. 
	 */
	private static String [] eraldaTekstJaAjavaljendid(BufferedReader input, List<MargendatudAjavaljend> ajavaljendid) throws Exception{
		String customRuleFile = null;
		Pattern tekstiliseSisugaValjendAlgus = Pattern.compile("^<(TIMEX[^>]+)>$");
		Pattern tekstiliseSisugaValjendLopp  = Pattern.compile("^</TIMEX([^>]*)>$");
		Pattern tekstiliseSisutaValjend      = Pattern.compile("^<(TIMEX[^>]+)/\\s*>$");
		Stack<String> algusTagid      = new Stack<String>();  // algustag'id
		Stack<String> content         = new Stack<String>();  // algustag'ide vahele jaav tekstiline sisu
		Stack<Integer> algusTagPos    = new Stack<Integer>(); // algustag'ide _tokenPosition'id
		Stack<String> pesastatudTagid = new Stack<String>();  // Pinu, mis hoiab algustag'e sees kauem, kui "algusTagid";
		                                                      // Seda pinu tyhjendatakse vaid siis, kui viimane tag on 
															  // v2ljav6etud pinust "algusTagid";
		String rida;
		String sona              = null;
		StringBuilder output     = new StringBuilder();  // sisend, millest on ajav2ljendite m2rgistus eemaldatud
		boolean inSideSentence   = false; // lause sees
		boolean inSideCleft      = false; // kiillause sees
		int tokenPosition  = 1; // token'i positsioon sisendis
		int _tokenPosition = 1; // token'i positsioon systeemisiseses tokeniseerimises
		while ((rida = input.readLine()) != null){
			// ---------------------------------------------------------------------------------------
			//  Jätame vahele ignoreeritava osa
			// ---------------------------------------------------------------------------------------
			if (rida.length() > 0 && rida.equals("<ignoreeri>")){
				output.append(rida);
				output.append("\n");
				tokenPosition++;
				_tokenPosition++;
				while ((rida = input.readLine()) != null){
					tokenPosition++;
					_tokenPosition++;
					output.append(rida);
					output.append("\n");
					if (rida.length() > 0 && rida.equals("</ignoreeri>")){
						break;
					}
				}
				continue;
			}
			if (rida.length() > 0){
				// ---------------------------------------------------------------------------------------
				//  N6utakse eraldiseisvate reeglite kasutamist
				// ---------------------------------------------------------------------------------------
           		Matcher customRuleSet = (TextUtils.customRuleFile).matcher(rida);
           		if (customRuleSet.find()){
           			customRuleFile = customRuleSet.group(1);
           			continue;
           		}
				// ---------------------------------------------------------------------------------------
				//  Tegemist on ajaväljendiga
				// ---------------------------------------------------------------------------------------
	       		Matcher sisutaValjend = tekstiliseSisutaValjend.matcher(rida); // NB! Peab olema enne sisuga v2ljendit
	       		if (sisutaValjend.find()){
	   				String header = sisutaValjend.group(1);
	    			MargendatudAjavaljend ajav = 
	    				new MargendatudAjavaljend(-1, -1, header, null);
	    			ajavaljendid.add(ajav);
	    			continue;
	       		}				
	        	Matcher sisugaValjendiAlgus = tekstiliseSisugaValjendAlgus.matcher(rida);
	       		if (sisugaValjendiAlgus.find()){
	       			// tegemist on tekstilise sisuga v2ljendi algusega
	       			String header  = sisugaValjendiAlgus.group(1);
	       			algusTagid.push(header);
	       			algusTagPos.push( new Integer(_tokenPosition) );
	       			content.push( "" );
	       			pesastatudTagid.push(header);
	       			continue;
	       		}
	       		Matcher sisugaValjendiLopp = tekstiliseSisugaValjendLopp.matcher(rida);
	       		if (sisugaValjendiLopp.find()){
	       			// tegemist on tekstilise sisuga v2ljendi l6puga:
	       			// loome uue ajav2ljendi
	       			String header;
	       			String contentStr;
	       			Integer algusPos;
					try {
						header     = algusTagid.pop();
						contentStr = content.pop();
						algusPos   = algusTagPos.pop();
					} catch (Exception e) {
						e.printStackTrace();
						throw new Exception("Exception in input text position "+tokenPosition+" " );
					}
					MargendatudAjavaljend ajav = null;
					if (timexStackContainsReferredBeginAndEndPoints(header, pesastatudTagid)){
						// Tekstilise sisuta ajav2ljend, mis on olude sunnil esitatud kui tekstilise sisuga
						// v2ljend (t3-olp-ajav formaat); Interpreteerime kui sisuta v2ljendit;
						ajav = new MargendatudAjavaljend(-1, -1, header, null);
					} else {
						// Tavaline, tekstilise sisuga ajav2ljend;
		    			ajav = new MargendatudAjavaljend(algusPos, _tokenPosition-1, header, contentStr);						
					}
	    			ajavaljendid.add(ajav);
	    			if (algusTagid.isEmpty()){
	    				pesastatudTagid.clear();
	    			}
	       			continue;
	       		}
				// ---------------------------------------------------------------------------------------
				//  Lause algus ja l6pp
				// ---------------------------------------------------------------------------------------
				if (rida.equals("<s>")){
					output.append(rida);
					output.append("\n");
					inSideSentence = true;
				} else if (rida.equals("</s>")){
					output.append(rida);
					output.append("\n");
					inSideSentence = false;
					// Lausel6pp
				} else if (rida.equals("<kiil>")){
					output.append(rida);
					output.append("\n");
					inSideCleft = true;
				} else if (rida.equals("</kiil>")){
					output.append(rida);
					output.append("\n");
					inSideCleft = false;					
				} else if (rida.equals("<kindel_piir/>")){
					// Osalause piir
					output.append(rida);
					output.append("\n");
				} else if (inSideSentence){
					// ---------------------------------------------------------------------------------------
					//  Eeldame, et morf analyys on kujul:
					//
					//     Mees    mees+0 //_S_ sg n, //    mesi+s //_S_ sg in, //
					//     peeti    peet+0 //_S_ adt, sg p, //    pida+ti //_V_ ti, //
					//
					// ---------------------------------------------------------------------------------------
					// Analyyside vahel on eraldajaks t2pselt 4 tyhikut
					String [] parts = rida.split("\\s{4}");
					if (parts.length < 2){
						throw new IOException(" Unexpected t3-olp format: '"+rida+"'");
					} else {
						sona = parts[0];
						boolean numbersSuccessfullyExtracted = false;
						if (NumberTokenizer.isNumberTokenizationNeeded(sona)){
							// Kui s6ne sisaldab arve kujul "23.07.2009" (numbrid, mis on yksteisest punktatsiooniga
							// eraldatud), tokeniseerime sellise s6ne eraldi alams6nedeks ...
							List<String> subStrings = NumberTokenizer.extractNumbersWithTrailingPunctation(sona, true);
							// Siin teeme h2ki: anname k6ikidele sonadele sama analyysi (kuna eeldatavasti on
							// tegu ainult numbritega, ei ole sellest erilist lugu).
							for (int i = 0; i < subStrings.size(); i++) {
								String string = subStrings.get(i);
								for (int j = 0; j < content.size(); j++) {
									content.set(j, content.get(j) +" " + string);
								}
								// Kui _tokenPosition on oluline, liigutame seda ...
								if (_tokenPosition != -1){
									if (i < subStrings.size() - 1){
										_tokenPosition++;
									}
								}
							}
							numbersSuccessfullyExtracted = true;							
						}
						if (!numbersSuccessfullyExtracted) {
							for (int j = 0; j < content.size(); j++) {
								content.set(j, content.get(j) + " " + sona);
							}
						}
						sona = null;
					}
					output.append(rida);
					output.append("\n");
					//if (logi != null) { logi.println(rida); }
				}
				tokenPosition++;
				_tokenPosition++;
			}
	    }
		input.close();
		if (customRuleFile != null){
			return new String [] { output.toString(), customRuleFile };
		} else {
			return new String [] { output.toString() };
		}
	}
	
	/**
	 *  </p>
	 *  Teeb kindlaks, (1) kas timexHeader viitab ilmutatud otspunktidega ajaintervallile ning,
	 *  (2) kas antud otspunktid leiduvad intervall-timex'i sees olevate timex'ite hulgas (tagsInsideTimex).
	 *  Kui (1) ja (2) on rahuldatud, tagastab true, vastasel juhul false;
	 *  <p>
	 *  <p>
	 *  Meetod on oluline "t3-olp-ajav" sisendi puhul, kus kasutatakse erikuju ilmutatud otspunkte sisaldavate 
	 *  varjatud kestvuste edasiandmiseks. 
	 *  </p>
	 */
	public static boolean timexStackContainsReferredBeginAndEndPoints(String timexHeader, Stack<String> tagsInsideTimex){
		if (timexHeader.contains("beginPoint") && timexHeader.contains("endPoint")){
			Matcher begin = (MargendatudAjavaljend.beginPointPart).matcher(timexHeader);
			Matcher end   = (MargendatudAjavaljend.endPointPart).  matcher(timexHeader);
			String beginPointStr = null;
			String endPointStr   = null;
			if (begin.find() && begin.groupCount() > 0){
				beginPointStr = begin.group(1);
			}
			if (end.find() && end.groupCount() > 0){
				endPointStr = end.group(1);
			}
			if ( beginPointStr != null && endPointStr != null ){
				boolean beginFound = false;
				boolean endFound   = false;
				for (String tagInside : tagsInsideTimex) {
					Matcher tid = (MargendatudAjavaljend.tidPart).matcher(tagInside);
					if (tid.find() && tid.groupCount() > 0){
						String tidStr = tid.group(1);
						if (tidStr.equals(beginPointStr))   { beginFound = true; }
						else if (tidStr.equals(endPointStr)){ endFound   = true; }
					}
				}
				if (beginFound && endFound){
					return true;
				}
			}
		}
		return false;
	}
	
	//==============================================================================
	//       T e s t i m i n e   :     t 3 - o l p    +    t m x     s i s e n d
	//
	//  t 3 - o l p   =   kooondkorpuse morf analyys + yhestamine + lausestus
	//  t m x         =   eraldiseisev ajaväljendite märgistus
	//==============================================================================
	
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
	public static LogiPidaja viiLabiTERNTestimineT3OLPKorpusKaustas(String korpuseKaust, 
			                                                        String kodeering, 
			                                                       boolean noDetails,
			                                                       boolean debugLogi, 
			                                                  boolean onlyPureTimeML, 
			                                                    boolean measureSpeed){
		 File dir = new File( korpuseKaust );
	 	 LogiPidaja testLog = new LogiPidaja(true, "test_t3olp");
	 	 // initsialiseerime tuvastaja 
	 	 initializeAjavt( ajaVTLoc, null );
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
 	            if (filename.endsWith(".t3-olp")){
 	 	            testLog.println("----------------------------------------------");
 	 	            testLog.println("   "+filename+"");
 	 	            testLog.println("----------------------------------------------");
 	 	            // find corresponding tmx file
 	 	            String tmxFileName = filename.replaceAll("\\.t3-olp$", ".tmx");
 	 	            File t3olpFile = new File(dir, filename);
 	 	            File tmxFile   = new File(dir, tmxFileName);
 	 	            
 	 	            List<MargendatudAjavaljend> kasitsiMargendatud = new LinkedList<MargendatudAjavaljend>();
 	 	        	    eraldaTmxFailistMargendatudValjendid(tmxFile, kodeering);
 	 	            String sisend = eraldaT3olpFailistMargendatudTekst(t3olpFile);

 	 	            List<MargendatudAjavaljend> automMargendatud =
 	 	        	    rakendaTekstilAjaVT(kasitsiMargendatud.get(0), sisend, debugLogi, 
 	 	        			              onlyPureTimeML, null, testLog, kiiruseTest);
 	 	            
 		 	        TestimisTulemusTERNkoond uusTulemus = 
 		 	        	Abimeetodid.hindaMargendamiseTulemusiTERN( automMargendatud, kasitsiMargendatud, 
 		 	        								               testLog, noDetails, dFormatter, true );
 		 	      (overAllResult.getTulemusIlmutatudValjendid()).setWordCount( (int) countWords( sisend )  );
 		 	        if (overAllResult == null){
 		 	        	overAllResult = uusTulemus;
 		 	        } else {
 		 	        	(overAllResult.getTulemusIlmutatudValjendid()).lisaUusTulemus((uusTulemus.getTulemusIlmutatudValjendid()));
 		 	        	(overAllResult.getTulemusVarjatudValjendid()).lisaUusTulemus((uusTulemus.getTulemusVarjatudValjendid()));
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
			testLog.println(" Kommentaaridega ajavaljendeid: "+(overAllResult.getTulemusIlmutatudValjendid()).getCommentCount()+
					        " ("+dFormatter.format(percentCommented)+"%)" );
 		    testLog.println();
	    	testLog.println(" Mikrokeskmised: ");
	    	(overAllResult.getTulemusIlmutatudValjendid()).calculateResults();
	    	testLog.println();
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
	 *   Laeb etteantud failist TimeML-annotatsiooni ja tagastab listina.
	 */
	private static List<MargendatudAjavaljend> eraldaTmxFailistMargendatudValjendid(File inputFile, String kodeering){
		LinkedList<MargendatudAjavaljend> ajavaljendid = new LinkedList<MargendatudAjavaljend>();
		Pattern tekstiliseSisugaValjend = Pattern.compile("^<([^>]+)>([^<]+)</([^>]+)>$");
		Pattern tekstiliseSisutaValjend = Pattern.compile("^<([^>]+)/[^>]*>$");
        try{		
    		// 1) Kirjutame vana faili sisu ümber, eemaldades annoteerimismärgendid
            FileInputStream in = new FileInputStream(inputFile);
            BufferedReader br = new BufferedReader(new InputStreamReader(in, kodeering));
            String strLine;
            int lineNR = 0;
            // 2) Loeme faili rida-rida haaval, eemaldades koik annotatsioonid...
            while ((strLine = br.readLine()) != null) {
            	// 2.0) Eemaldame mittevajaliku annotatsiooni
            	strLine = strLine.replaceAll("<\\?xml[^>]+>", "");
            	strLine = strLine.replaceAll("</?TimeML[^>]+>", "");
            	
            	// 2.1) 
            	Matcher sisugaValjend = tekstiliseSisugaValjend.matcher(strLine);
           		if (sisugaValjend.find()){
           			// teine sobivus peakski andma meile vajaliku sisendkuup2eva
           			String header  = sisugaValjend.group(1);
           			String content = sisugaValjend.group(2);
           			String footer  = sisugaValjend.group(3);
        			MargendatudAjavaljend ajav = 
        				new MargendatudAjavaljend(-1, -1, header, content);
        			ajavaljendid.add(ajav);
           		} else {
           			Matcher sisutaValjend = tekstiliseSisutaValjend.matcher(strLine);
           			if (sisutaValjend.find()){
           				String header = sisutaValjend.group(1);
            			MargendatudAjavaljend ajav = 
            				new MargendatudAjavaljend(-1, -1, header, null);
            			ajavaljendid.add(ajav);
           			}
           		}
            	lineNR++;
            }
            // Sulgeme voo
            in.close();
        } catch (Exception e){ //Catch exception if any
            System.err.println("Error: " + e.getMessage());
        }
		return ajavaljendid;
	}	
	
	/**
	 *  Laeb etteantud t3olp faili sisu ja tagastab s6nena.
	 */
	private static String eraldaT3olpFailistMargendatudTekst(File t3olpFile){
		StringBuilder sb = new StringBuilder();			
		try {
			FileInputStream fstream = new FileInputStream(t3olpFile);
		    DataInputStream in = new DataInputStream(fstream);
		    BufferedReader sisend = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		    String rida = null;
			while ((rida = sisend.readLine()) != null){
				if (rida.length() > 0){
					sb.append(rida + "\n");
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.exit(-1);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return sb.toString();
	}
	
	/**
	 *   Rakendab etteantud t3olp sisendtekstil ajaväljendite tuvastajat ning tagastab
	 *   tuvastatud v2ljendid.
	 */
	private static List<MargendatudAjavaljend> rakendaTekstilAjaVT(MargendatudAjavaljend refAeg,
			                                                       String sisendTekst,
																   boolean debugLogi,
																   boolean onlyPureTimeML, 
																   String customRuleFile,
																   LogiPidaja testLog,
																   KiiruseTestiTulemus kiiruseTest){
		String [] referentsAeg = extractLocalDateTimeFromMargendatudAjavaljend(refAeg);
		try {
			if (referentsAeg == null){
				throw new Exception(" Unable to parse reference time from "+refAeg);
			}
			if (customRuleFile != null){
				// Switch to new rule file
				initializeAjavt( ajaVTLoc, customRuleFile );
			}
			long startTime = System.currentTimeMillis();
			List<HashMap<String, String>> results = 
				tuvastaja.tuvastaAjavaljendidT3OLPTulemusPaiskTabelitena(referentsAeg, sisendTekst, onlyPureTimeML, debugLogi);
			long endTime = System.currentTimeMillis();
			if (kiiruseTest != null){
				kiiruseTest.addAjavtProcessTimes( endTime - startTime );
				kiiruseTest.addFileSizeInBytes( (long)(sisendTekst.getBytes("UTF8")).length );
				kiiruseTest.addFileSizeInCPs( (long)(sisendTekst.codePointCount(0, sisendTekst.length())) );
			}
			List<String> resultsStrLst = 
				JarelTootlus.konverteeriEraldamiseTulemusSonedeListiks(results, onlyPureTimeML, true);
			if (customRuleFile != null){
				// Switch back to old rule file
				initializeAjavt( ajaVTLoc, null );
			}
			return Abimeetodid.eraldaMargendatudValjendidSonedest(resultsStrLst);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		return (new LinkedList<MargendatudAjavaljend>());
	}
	
	//==============================================================================
	//   	A b i m e e t o d i d
	//==============================================================================
	
	/**
	 *  Loendab s6nade (morf analyysitud yksuste) arvu t3olp sisendis;
	 */
	public static long countWords(String sisend){
		long wordCount = 0;
		try {
			BufferedReader input = new BufferedReader(new StringReader(sisend));
			String rida = null;
			boolean inSideSentence = false; // lause sees
			boolean inSideCleft    = false; // kiillause sees
			int tokenPosition = 1;
			while ((rida = input.readLine()) != null){
				// ---------------------------------------------------------------------------------------
				//  Jätame vahele ignoreeritava osa
				// ---------------------------------------------------------------------------------------
				if (rida.length() > 0 && rida.equals("<ignoreeri>")){
					tokenPosition++;
					while ((rida = input.readLine()) != null){
						tokenPosition++;
						if (rida.length() > 0 && rida.equals("</ignoreeri>")){
							break;
						}
					}
					continue;
				}
				if (rida.length() > 0){
					// ---------------------------------------------------------------------------------------
					//  Lause algus ja l6pp
					// ---------------------------------------------------------------------------------------
					if (rida.equals("<s>")){
						inSideSentence = true;
					} else if (rida.equals("</s>")){
						inSideSentence = false;
						// Lausel6pp
					} else if (rida.equals("<kiil>")){
						inSideCleft = true;
					} else if (rida.equals("</kiil>")){
						inSideCleft = false;					
					} else if (rida.equals("<kindel_piir/>")){
						// Osalause piir
					} else if (inSideSentence){
						// ---------------------------------------------------------------------------------------
						//  Eeldame, et morf analyys on kujul:
						//
						//     Mees    mees+0 //_S_ sg n, //    mesi+s //_S_ sg in, //
						//     peeti    peet+0 //_S_ adt, sg p, //    pida+ti //_V_ ti, //
						//
						// ---------------------------------------------------------------------------------------
						// Analyyside vahel on eraldajaks t2pselt 4 tyhikut
						String [] parts = rida.split("\\s{4}");
						if (parts.length < 2){
							throw new IOException(" Unexpected t3-olp format: '"+rida+"'");
						} else {
							wordCount++;
						}
					}
					tokenPosition++;
				}
			}
			input.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return wordCount;
	}
	
	/**
	 *  Kontrollib, kas on tegu CREATION_TIME tyypi v2ljendiga: kui on, tagastab LocalDateTime kujul.
	 */
	private static String [] extractLocalDateTimeFromMargendatudAjavaljend(MargendatudAjavaljend ajav){
		if (ajav.getAttrValues() != null){
			String functionInDocument = (ajav.getAttrValues()).get("functionInDocument");
			String value = (ajav.getAttrValues()).get("value");
			if (functionInDocument != null && value != null){
				if (functionInDocument.equals("CREATION_TIME")){
					return Main.looSonePohjalReferentsAeg(value);
				}
			}
		}
		return null;
	}
	
	
	/**
	 *  Initializes new temporal expressions resolver component. Logic for choosing the
	 *  rules file is following: 1) if customRuleFile is not specified, uses inputRulesFile 
	 *  (if specified) or "reeglid.txt" located in programDir (if inputRulesFile not 
	 *  specified); 2) if customRuleFile is specified, it will be always preferred over 
	 *  the other options. In case of inputRulesFile and customRuleFile - if they contain 
	 *  no path symbols (File.separator), it is assumed that they are in programDir, otherwise,
	 *  full paths are expected.
	 * 
	 * @param programDir program working directory; 
	 * @param customRuleFile location of custom rule file;
	 */
	private static void initializeAjavt(String programDir, String customRuleFile){
		tuvastaja = new AjaTuvastaja();
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
	
}
