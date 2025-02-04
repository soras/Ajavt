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

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.List;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.LocalDateTime;

import ee.ut.soras.ajavtV2.mudel.AjavtSona;
import ee.ut.soras.ajavtV2.util.FileUtils;
import ee.ut.soras.ajavtV2.util.LogiPidaja;
import ee.ut.soras.wrappers.mudel.MorfAnRida;

/**
 *  Ajav&auml;ljendite tuvastaja (ver 2) k&auml;sureap&otilde;hine liides.
 *  <p>
 *  Eeldab, et sisendtekst on UTF8 kodeeringus.
 * 
 * @author Siim Orasmaa
 */
public class Main {
	
	private static void kuvaAbiInfo(){
		System.out.println();
		System.out.println("Ajavt, ver "+ AjaTuvastaja.getVersioon());
		System.out.println();
		System.out.println(" Sisendi formaadi m22ramine:");
		System.out.println("  -format text  -- puhas tekst (vaikimisi);");
		System.out.println("                   (NB! selle lipu kasutamiseks peab T3MESTA olema");
		System.out.println("                    systeemis saadaval, vt allpool);");
		System.out.println("          t3olp -- osalausestatud T3MESTA v2ljund;");
		System.out.println("          json  -- vabamorfi JSON v2ljund;");
		System.out.println(" V2ljund on samas formaadis, mis sisend - lisatud on vaid ajav2ljendi-");
		System.out.println(" m2rgendid.");
		System.out.println();
		System.out.println(" Sisendi ja v2ljundi allika m22ramine: ");
		System.out.println("  -pyvabamorf          -- yhe rea kaupa JSON sisendi t88tlus: loeb standard-");
		System.out.println("                          sisendist rea, analyysib seda ja kirjutab tulemuse");
		System.out.println("                          standardv2ljundisse.");
		System.out.println("  -in  stdin           -- standardsisendist lugemine (vaikimisi); ");
		System.out.println("       file <fileName> -- sisend loetakse failist <fileName>;");
		System.out.println("  -out stdout          -- standardv2ljundisse kirjutamine (vaikimisi);");
		System.out.println("       file <fileName> -- v2ljund kirjutatakse faili <fileName>;");
		System.out.println();
		System.out.println("   NB! Eeldatakse, et sisend on alati UTF-8 kodeeringus, v2ljundisse ");
		System.out.println("  kirjutatav sisu on samuti alati UTF-8 kodeeringus. ");
		System.out.println();
		System.out.println(" Muud t2psustused:");
		System.out.println("  -r <rulesFile> -- teistsuguse reeglifaili kasutamine;");
		System.out.println("                    (vaikimisi on failiks 'reeglid.xml')");
		System.out.println("  -TimeML        -- v2ljund peaks olema rangelt TimeML-ile");
		System.out.println("                    vastav (TimeML-i alamosa);");
		System.out.println("  -unesc_DBS     -- sisendis kahekordsete \\ m2rkide asendamine yhekordsetega;");
		System.out.println("  -pretty_print  -- JSON-i v2ljastamine ilusti joondatult;");
		System.out.println();
		System.out.println(" DEBUG lipud:");
		System.out.println("  -par_debug -- osaline DEBUG valjund (esialgne tekst, kus on m2rgendatud");
		System.out.println("                ajavaljendid ning samuti m6ned olulisemad analyysi k2igus ");
		System.out.println("                leitud teksti tunnused (arvs6nad, grammatiline aeg jms). ");			
		System.out.println("  -debug     -- taielik DEBUG valjund (sisseloetud reeglid, morf analyysi");
		System.out.println("                tulemus ning 'margendatud' tekst, iga s6na eraldi real ) ");
		System.out.println();
		System.out.println(" Kasutamine puhtal tekstil (vaikerezhiim):");
		System.out.println();
		System.out.println(" *) Arvutisse peab olema installeeritud Filosofti morfoloogiline analysaator");
		System.out.println("    ja yhestaja T3MESTA;");
		System.out.println();
		System.out.println("--> Programmi standardsisendisse tuleb anda eestikeelsetest lausetest");
		System.out.println("    koosnev tekst (kodeeringus UTF-8);");
		System.out.println("<-- Programm yritab leida lausetest ajavaljendeid, margistab leitud");
		System.out.println("    ajavaljendid ning kuvab standardvaljundisse tulemuse;");
		System.out.println();						
		System.out.println("  echo Homme tulen sinna ka . | java -jar Ajavt.jar ");
		System.out.println("    ==> Leitakse etteantud lauses esinevad ajavaljendid, referentsajana");
		System.out.println("        kasutatakse programmi kaivitamise hetke;");
		System.out.println("  echo Homme tulen sinna ka . | java -jar Ajavt.jar \"2008-03-21T15:30\"");
		System.out.println("    ==> Leitakse etteantud lauses esinevad ajavaljendid, referentsajana");
		System.out.println("        kasutatakse parameetrina etteantud aega (2008-03-21T15:30);");
		System.out.println("        NB! Palume jalgida, et parameetrina antud referentsaeg jaaks jutu-");
		System.out.println("        markide vahele.");
		System.out.println();
	}
	
	
	public static void main(String[] args){
		// --------------------------------------------------------------------
		//   *) V6tame k2surealt etteantud parameetrid
		// --------------------------------------------------------------------
		String [] referentsAeg      = null;
		String referentsAegStr      = null;
		String rulesFile            = null;
		boolean allowOnlyPureTimeML = false;
		String format               = "text";
		String inputType            = "stdin";
		String inputFile            = null;
		String outputType           = "stdout";
		String outputFile           = null;
		// DEBUG lipud
		boolean logIntoFile         = false;
		boolean fullDebug           = false;
		boolean partialDebug        = false;
		boolean fullAnnotation      = false;
		boolean t3olpAlignmentOnly  = false;
		boolean unescapeDoubleBackSlashes = false;
		boolean prettyPrintJson           = false;
		boolean pyVabamorfProcessing      = false;
		if (args.length > 0){
			for (int i = 0; i < args.length; i++) {
				// Ainult "puhta" TimeML-i v2ljastamine (yritame v2hemalt) ...
				if (args[i].equalsIgnoreCase("-TimeML")){
					allowOnlyPureTimeML = true;
				}
				// Referentspunkt ehk k6neaeg ...
				if (args[i].matches("[0-9X]{4}-[0-9X]{2}-[0-9X]{2}T[0-9X]{2}:[0-9X]{2}")){
					referentsAeg = looSonePohjalReferentsAeg(args[i]);
					referentsAegStr = args[i];
				}
				// Teistsugune reeglifail (v6i faili asukoht)
				if (args[i].matches("-r") && (i + 1 < args.length)){
					rulesFile = args[i+1];
				}
				// Kuvame abiinfo
				if (args[i].matches("(?i)(-){1,2}(h|help|abi|appi)")){
					kuvaAbiInfo();
					System.exit(0);
				}
				// Sisendi/v2ljundi formaat
				if (args[i].matches("-format")  &&  i+1<args.length  &&  args[i+1].matches("(text|t3olp|json)")){
					format = args[i+1];
				}
				if (args[i].matches("(?i)(-){1,2}pyvabamorf")){
					pyVabamorfProcessing = true;
				}
				// Sisendi allikas
				if (args[i].matches("-in")  &&  i+1<args.length){
					if (args[i+1].matches("(stdin|file)")){
						inputType = args[i+1];
						if (inputType.equalsIgnoreCase("file")){
							if (i+2<args.length){
								inputFile = args[i+2];
							} else {
								kuvaAbiInfo();
								System.exit(0);								
							}
						}						
					} else {
						kuvaAbiInfo();
						System.exit(0);						
					}
				}
				// V2ljundi allikas
				if (args[i].matches("-out")  &&  i+1<args.length){
					if (args[i+1].matches("(stdout|file)")){
						outputType = args[i+1];
						if (outputType.equalsIgnoreCase("file")){
							if (i+2<args.length){
								outputFile = args[i+2];
							} else {
								kuvaAbiInfo();
								System.exit(0);								
							}
						}						
					} else {
						kuvaAbiInfo();
						System.exit(0);						
					}
				}
				// k6iksugu debug lipud
				if (args[i].matches("-debug")){
					fullDebug = true;
				}
				if (args[i].matches("-alignmentOnly")){
					t3olpAlignmentOnly = true;
				}
				if (args[i].matches("-par_debug")){
					partialDebug = true;
				}
				if (args[i].matches("-log")){
					logIntoFile = true;
				}
				// Kaigaste \\ muutmine kujule \
				if (args[i].matches("(?i)(-){1,2}(unesc_DBS)")){
					unescapeDoubleBackSlashes = true;
				}
				// JSON v2ljundi pretty print
				if (args[i].matches("(?i)(-){1,2}(pretty_?print)")){
					prettyPrintJson = true;
				}
			}
		}
		// ==================================================
		//   *) Pyvabamorfi JSON rezhiim
		// ==================================================s		
		if (pyVabamorfProcessing){
			AjaTuvastaja tuvastaja = new AjaTuvastaja();
			Pattern emptyString = Pattern.compile("^\\s*$");
			try {
				if (rulesFile != null){
					tuvastaja.uuendaReegleid(rulesFile, false);
				}
				// ---- V6tame sisendi standardsisendist
				Scanner sc = new Scanner(System.in, "UTF-8");
				PrintStream ps = new PrintStream(System.out, false, "UTF-8");
				while ( sc.hasNextLine() ) {
					String line = sc.nextLine();
					if ((emptyString.matcher(line)).matches()){
						ps.println( line );
						ps.flush();
						//System.out.println(line);
						//System.out.flush();
					} else {
						String result = tuvastaja.tuvastaAjavaljendidPyVabamorfJSON(referentsAegStr, line, allowOnlyPureTimeML, false);
						ps.println( result );
						ps.flush();
						//System.out.println(result);
						//System.out.flush();						
					}
				}		
			} catch (Exception e) {
				e.printStackTrace();
				System.exit(-1);
			}
			System.exit(0);
		}
		// --------------------------------------------------------------------
		//   *) Sisend
		// --------------------------------------------------------------------
		String sisendSone = null;	
		if (inputType.equalsIgnoreCase("stdin")){
			// ---- V6tame sisendi standardsisendist
			try {
				sisendSone = readInputFromStdIn();
			} catch (Exception e) {
				System.err.println("Viga: Standardsisendist lugemine ebaonnestus!");
				e.printStackTrace();
				System.exit(-1);
			}
		} else if (inputType.equalsIgnoreCase("file") && inputFile != null){
			// ---- V6tame sisendi failist
			try {
				sisendSone = readInputFromFile(inputFile);
			} catch (Exception e) {
				System.err.println("Viga: Failist lugemine ebaonnestus!");
				e.printStackTrace();
				System.exit(-1);
			} 
		}
		// --------------------------------------------------------------------
		//   *) Formaadist l2htuv t88tlus
		// --------------------------------------------------------------------
		// Kui sisends6ne on null v6i tyhi, kuvame veateate
		if (sisendSone == null || sisendSone.length() == 0){
			System.err.println("Viga: Sisendit pole antud (v6i sisendiks on tyhis6ne).");
			kuvaAbiInfo();
			System.exit(-1);
		}
		if (unescapeDoubleBackSlashes){
			sisendSone = sisendSone.replace("\\\\", "\\");
		}
		// Kui referentsaega pole antud, v6tame hetkeaja
		if (referentsAeg == null){
			referentsAeg = looSonePohjalReferentsAeg(null);
		}
		AjaTuvastaja tuvastaja = new AjaTuvastaja();
		if (rulesFile != null){
			tuvastaja.setReegliFail(rulesFile);
		}
		// T88 tulemus (teksti s6nad, mille kylge on seotud tuvastatud ajav2ljendid)
		List<AjavtSona> tulemAjavtSonad = null;
		try {
			if ( format.equalsIgnoreCase("text") ){
				tulemAjavtSonad = tuvastaja.tuvastaAjavaljendidTekstis(referentsAeg, sisendSone, null, allowOnlyPureTimeML);
			} else if ( format.equalsIgnoreCase("t3olp") ){
				tulemAjavtSonad = tuvastaja.tuvastaAjavaljendidT3OLP(referentsAeg, sisendSone, allowOnlyPureTimeML, fullDebug);
			} else if ( format.equalsIgnoreCase("json") ){
				tulemAjavtSonad = tuvastaja.tuvastaAjavaljendidVabamorfJSON(referentsAeg, sisendSone, allowOnlyPureTimeML, false);
			}	
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		// --------------------------------------------------------------------
		//   *) V2ljund
		// --------------------------------------------------------------------
		try {
			if (outputType.equalsIgnoreCase("stdout")){
				PrintStream ps = new PrintStream(System.out, false, "UTF-8");
				if ( format.equalsIgnoreCase("text") ){
					ps.println( 
							JarelTootlus.eraldamiseTulemusPretty(
							tulemAjavtSonad, sisendSone, allowOnlyPureTimeML, 
							JarelTootlus.formatAsCreationTime(referentsAeg)) );				
				} else if ( format.equalsIgnoreCase("t3olp") ){
					ps.println( JarelTootlus.eraldamiseTulemusT3OLPEraldiReal( 
								sisendSone, tulemAjavtSonad,
							    JarelTootlus.formatAsCreationTime(referentsAeg), false, false) );
				} else if ( format.equalsIgnoreCase("json") ){
					ps.println( JarelTootlus.eraldamiseTulemusVabaMorfiJSON(
								sisendSone, tulemAjavtSonad, 
								JarelTootlus.formatAsCreationTime(referentsAeg), allowOnlyPureTimeML, prettyPrintJson));
				}
				ps.flush();
				ps.close();
			} else if (outputType.equalsIgnoreCase("file") && outputFile != null){
				if ( format.equalsIgnoreCase("text") ){
					FileUtils.printIntoFile( 
						JarelTootlus.eraldamiseTulemusPretty(tulemAjavtSonad, sisendSone, 
								allowOnlyPureTimeML, JarelTootlus.formatAsCreationTime(referentsAeg)), 
									"UTF-8", outputFile);
				} else if ( format.equalsIgnoreCase("t3olp") ){
					FileUtils.printIntoFile(
						JarelTootlus.eraldamiseTulemusT3OLPEraldiReal( sisendSone, tulemAjavtSonad, 
								JarelTootlus.formatAsCreationTime(referentsAeg), false, false), "UTF-8", outputFile);
				} else if ( format.equalsIgnoreCase("json") ){
					FileUtils.printIntoFile( JarelTootlus.eraldamiseTulemusVabaMorfiJSON(
							sisendSone, tulemAjavtSonad, 
								JarelTootlus.formatAsCreationTime(referentsAeg), allowOnlyPureTimeML, prettyPrintJson), "UTF-8", outputFile);
				}
			}
			// -------------------------
			//   DEBUG v2ljundid
			// -------------------------
			if (fullAnnotation || fullDebug || partialDebug || t3olpAlignmentOnly){
				if ( format.equalsIgnoreCase("t3olp") ){
			    	debugOutputForT3OLP(sisendSone, tulemAjavtSonad, referentsAeg, outputFile, allowOnlyPureTimeML, fullDebug, fullAnnotation, t3olpAlignmentOnly);
			    }
				if ( format.equalsIgnoreCase("text") && (fullDebug || partialDebug) ){
					debugOutputForText_partialDebug(sisendSone, tulemAjavtSonad, referentsAeg, allowOnlyPureTimeML, fullDebug, logIntoFile, tuvastaja);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(-1);
		}
		//System.out.println( inputType );
		//System.out.println( inputFile );
		//System.out.println( outputType );
		//System.out.println( outputFile );
	}
	
	
	/**
	 *    Loob etteantud sisends6ne p6hjal referentsaja: granulaarsuste <i>aasta, kuu, kuupaev,
	 *    tund, minut</i> vaartuste j2rjendi. Kui sisends6ne ei rahulda regulaaravaldisi 
	 *    <code>/[0-9X]{4}-[0-9X]{2}-[0-9X]{2}T[0-9X]{2}:[0-9X]{2}/</code> või
	 *    <code>/[0-9X]{4}-[0-9X]{2}-[0-9X]{2}/</code> , tagastatakse null.
	 *    Kui sisends6ne on null, luuakse referentsaja j2rjend minuti-t2psusega hetkeaja p6hjal.
	 *    <br><br> 
	 *    Tagastatud j2rjendis on granulaarsuste vaartused j2rjekorras 
	 *    <i>aasta, kuu, kuupaev, tund, minut</i>. 
	 */
	public static String[] looSonePohjalReferentsAeg(String sone){
		if ( sone != null ){
			if ( sone.matches("[0-9X]{4}-[0-9X]{2}-[0-9X]{2}T[0-9X]{2}:[0-9X]{2}") ){
				// 1) Kui sisends6ne vastab etteantud mustrile
				String[] kalendriValjadeVaartused = new String [5];
				int j = 0;
				boolean seenXXXvalues   = false;
				StringTokenizer tokens1 = new StringTokenizer(sone, "-:T");
				while (tokens1.hasMoreTokens()) {
					String s = (String) tokens1.nextToken();
					if (j < kalendriValjadeVaartused.length){
						kalendriValjadeVaartused[j] = s;
					}
					if (s.matches("X+")){
						seenXXXvalues = true;
					} else {
						if (seenXXXvalues){
							// Kui on l2bisegi numbrid ja XX-v22rtused, on tegemist mitte-
							// koosk6lalise  sisendajaga:   parandame   numbrid   XX-ideks
							kalendriValjadeVaartused[j] = "XX";
						}
					}
					if (j < kalendriValjadeVaartused.length){
						j++;
					}
				}
				return kalendriValjadeVaartused;				
			} else if ( sone.matches("^[0-9X]{4}-[0-9X]{2}-[0-9X]{2}$") ){
				// 2) Kui sisends6ne vastab etteantud mustrile
				String[] kalendriValjadeVaartused = new String [5];
				int j = 0;
				boolean seenXXXvalues   = false;
				StringTokenizer tokens2 = new StringTokenizer(sone, "-");
				while (tokens2.hasMoreTokens()) {
					String s = (String) tokens2.nextToken();
					if (j < kalendriValjadeVaartused.length){
						kalendriValjadeVaartused[j] = s;
					}
					if (s.matches("X+")){
						seenXXXvalues = true;
					} else {
						if (seenXXXvalues){
							// Kui on l2bisegi numbrid ja XX-v22rtused, on tegemist mitte-
							// koosk6lalise  sisendajaga:   parandame   numbrid   XX-ideks
							kalendriValjadeVaartused[j] = "XX";
						}
					}
					if (j < kalendriValjadeVaartused.length){
						j++;
					}
				}
				// Viimased (kellaaja osa) ongi selle mustri puhul teadmata
				kalendriValjadeVaartused[3] = "XX";
				kalendriValjadeVaartused[4] = "XX";
				return kalendriValjadeVaartused;	
			}
			return null;
		} else {
			// 2) Kui sisends6ne puudub v6i ei vasta etteantud mustrile, loome uue
			// referentsaja, milleks saab hetkeaeg
			LocalDateTime hetkeAeg = new LocalDateTime();
			String[] kalendriValjadeVaartused = new String [5];
			kalendriValjadeVaartused[0] = String.valueOf( hetkeAeg.getYear() );
			kalendriValjadeVaartused[1] = String.valueOf( hetkeAeg.getMonthOfYear() );
			kalendriValjadeVaartused[2] = String.valueOf( hetkeAeg.getDayOfMonth() );
			kalendriValjadeVaartused[3] = String.valueOf( hetkeAeg.getHourOfDay() );
			kalendriValjadeVaartused[4] = String.valueOf( hetkeAeg.getMinuteOfHour() );
			for (int i = 1; i < kalendriValjadeVaartused.length; i++) {
				if (kalendriValjadeVaartused[i].length() == 1){
					kalendriValjadeVaartused[i] = "0"+kalendriValjadeVaartused[i];
				}
			}
			return kalendriValjadeVaartused;
		}
	}
	
	
	static Pattern jsonDCT = Pattern.compile("(?i)\"(dct)\"\\s*:\\s*\"([^\"]+)\"");
	
	/**
	 *    Otsib etteantud vabamorfi JSON kujul sisends6nest referentsaega: v6tit "dct",
	 *    millega on seotud v22rtus-s6ne;
	 *    <br><br>
	 *    Kui leiab otsitava, tagastab s6nede massiivi, kus esimene element on v6tme string
	 *    ja teine element v22rtuse string; vastasel juhul tagastab <tt>null</tt>;
	 */
	public static String[] leiaJSONSonestReferentsAeg(String sone){
		if ( sone != null  ){
			Matcher dctMatch = jsonDCT.matcher(sone);
			if (dctMatch.find()){
				String key = dctMatch.group(1);
				String val = dctMatch.group(2);
				return new String[] {key, val};
			}
		}
		return null;
	}
	
	/**
	 *    Yritab leida JSON sisendist referentsaja stringi (meetodiga leiaJSONSonestReferentsAeg) ning 
	 *   luua selle p6hjal uue referentsaja (meetodiga looSonePohjalReferentsAeg). Kui k6ik 6nnestub,
	 *   tagastab sama tulemuse, mis meetod looSonePohjalReferentsAeg 6nnestumise korral,
	 *   vastasel juhul tagastab <tt>null</tt>;
	 */
	public static String [] prooviLuuaJSONsisendiP6hjalRefAeg(String jsonSisend){
		String konehetkJSONist [] = Main.leiaJSONSonestReferentsAeg(jsonSisend);
		if (konehetkJSONist != null){
			return Main.looSonePohjalReferentsAeg(konehetkJSONist[1]);
		}
		return null;
	}
	
	/**
	 * Loeb sisendi UTF-8 kodeeringus standardsisendist
	 */
	private static String readInputFromStdIn() throws IOException {
		StringBuffer puhver = new StringBuffer();
		BufferedReader stdInput = new BufferedReader(
						new InputStreamReader(System.in, "UTF-8"));
		int character;
		while ((character = stdInput.read()) != -1) {
			puhver.append( Character.toChars(character) );
		}
		stdInput.close();
		return puhver.toString();
	}

	/**
	 * Loeb sisendi UTF-8 kodeeringus failist
	 */
	private static String readInputFromFile(String inputFile) throws Exception {
		StringBuilder sb = new StringBuilder();			
		try {
			FileInputStream fstream = new FileInputStream( inputFile );
		    DataInputStream in = new DataInputStream(fstream);
		    BufferedReader sisend = new BufferedReader(new InputStreamReader(in, "UTF-8"));
		    String rida = null;
			while ((rida = sisend.readLine()) != null){
				if (rida.length() > 0){
					sb.append(rida + "\n");
				}
			}			    
		} catch (Exception e) {
			throw e;
		}
		return sb.toString();
	}


	/**
	 *   T3-olp formaadi vanad debug v2ljundid.
	 */
	private static void debugOutputForT3OLP(String sisendT3OLP, List<AjavtSona> sonad, 
			                             String[] konehetk, String t3olpFileName, 
			                             boolean allowOnlyPureTimeML, boolean debug, 
			                             boolean full, boolean alignmentOnly) throws Exception {
		if (t3olpFileName == null){
			throw new Exception(" Output file must be specified in order to debug T3OLP output! ");
		}
		if (debug){
			// V2ljund, kus iga s6na on eraldi real ning lisatud on debug informatsioon 
			FileUtils.printIntoFile( JarelTootlus.eraldamiseTulemusIgaSonaEraldiRealDebug(sonad, 
									 JarelTootlus.formatAsCreationTime(konehetk)), "UTF-8",
									 t3olpFileName, "debug" );
		} else if (full){
			// V2ljund, kus lausestusm2rgid ja morf analyys on eemaldatud ning iga lause on eraldi real
			FileUtils.printIntoFile( JarelTootlus.eraldamiseTulemusPrettyT3OLP( sonad, 
					  				 JarelTootlus.formatAsCreationTime(konehetk), allowOnlyPureTimeML), "UTF-8",
					  				 t3olpFileName, "txt-timex" );
			// V2ljund, kus iga s6na on eraldi real ning ignore osa on vahele j2etud
			FileUtils.printIntoFile( JarelTootlus.eraldamiseTulemusT3OLPEraldiReal( sisendT3OLP, sonad,
									 JarelTootlus.formatAsCreationTime(konehetk), allowOnlyPureTimeML, true), "UTF-8",
									 t3olpFileName, "t3-olp-ri-timex" );
		} else if (alignmentOnly){
			// Trykime ainult joonduse
			List<AjavtSona> joonduseSonad = EelTootlus.eeltootlusT3OLP( sisendT3OLP );
			StringBuilder sb = new StringBuilder();
			for (AjavtSona ajavtSona : joonduseSonad) {
				sb.append( ajavtSona.getInnerTokenPosition()+":"+ajavtSona.getTokenPosition()+": "+ajavtSona.getAlgSona() );
				sb.append( "\n" );
			}
			// V2ljund, kus on toodud ainult s6nade positsioonid tekstis
			FileUtils.printIntoFile( sb.toString(), "UTF-8", t3olpFileName, "t3-olp-align" );
		}
	}

	/**
	 *   Text formaadi vana debug v2ljund: full debug v6i partial debug;
	 */
	private static void debugOutputForText_partialDebug(String sisendTekst, List<AjavtSona> sonad, 
			                               String[] konehetk, boolean allowOnlyPureTimeML,
			                               boolean fullDebug, boolean outPutToLogFile,
			                               AjaTuvastaja tuvastaja){
		if (!fullDebug){
			// partial debug
			if (outPutToLogFile){
				LogiPidaja logi = new LogiPidaja(true);
				logi.setKirjutaLogiFaili(true);
				logi.setKirjutaLogiValjundisse(true);
				logi.println( 
						JarelTootlus.eraldamiseTulemusDebug( 
								sonad, sisendTekst, allowOnlyPureTimeML, JarelTootlus.formatAsCreationTime(konehetk)) );
				logi.setKirjutaLogiFaili(false); // sulgeb faili			
			} else {
				System.out.println("*******************************************************"); 
				System.out.println( 
						JarelTootlus.eraldamiseTulemusDebug(
								sonad, sisendTekst, allowOnlyPureTimeML, JarelTootlus.formatAsCreationTime(konehetk)) );
			}
		} else {
			LogiPidaja logi = new LogiPidaja(true);
			logi.setKirjutaLogiValjundisse(false);
			// --------------------------------------------------------------------
			//   *) Debug: kuvame sisseloetud reeglid
			// --------------------------------------------------------------------
			tuvastaja.kuvaSonaKlassidJaReeglid( logi );
			// --------------------------------------------------------------------
			//   *) Debug: kuvame morf analyysi tulemused logisse
			// --------------------------------------------------------------------
		 	logi.println("-------------------------------------");
			logi.println("Sonu oli kokku: "+sonad.size());
			logi.println("-------------------------------------");
			logi.println(" Morf analyysi tulemused mudelisse vormitult:");
			logi.println("");
			for (AjavtSona sona : sonad) {
					logi.print(sona.getAlgSona());
					if (sona.getAlgSonaYmbritsevateMarkideta() != null){
						logi.println("("+sona.getAlgSonaYmbritsevateMarkideta()+")");
					} else {
						logi.println();
					}
				if (sona.kasLeidusAnalyys()){
					List<MorfAnRida> analyysiTulemused = sona.getAnalyysiTulemused();
					for (MorfAnRida morfAnRida : analyysiTulemused) {
						logi.println("\t"+morfAnRida.getLemma()+" + "+morfAnRida.getLopp());
						logi.println("\t\t"+morfAnRida.getSonaLiikPikalt());
						if (morfAnRida.getVormiNimetused().length() > 0){
							logi.print( morfAnRida.getVormiNimetusedPikalt("\t\t") );
						}
					}
				}
			}		
			logi.println();
			logi.println( JarelTootlus.eraldamiseTulemusIgaSonaEraldiRealDebug( sonad, JarelTootlus.formatAsCreationTime(konehetk) ) );
			logi.setKirjutaLogiFaili(false); // sulgeb faili
		}
		
	}


}
