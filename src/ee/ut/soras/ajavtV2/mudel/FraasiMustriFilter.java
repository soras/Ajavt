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

package ee.ut.soras.ajavtV2.mudel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;

import ee.ut.soras.ajavtV2.mudel.ajavaljend.SemantikaDefinitsioon;
import ee.ut.soras.wrappers.mudel.MorfAnRida;

/**
 *   Filter, mis kirjeldab fraasimustri mingit alamosa (rahuldatud sonaklassid, morfoogilised tunnused) 
 *  ning kannab endas yhtlasi selle alamosa t&auml;iendavaid semantikadefinitsioone; 
 *  <p>
 *   Filtreid rakendatakse p&auml;rast seda, kui esmane fraasi sobitamine fraasimustris on edukalt l&auml;bi
 *  viidud - eesm&auml;rgiga viia kokku fraasi t&auml;psem kirjeldus (fraasimuster v&otilde;ib olla v&auml;ga 
 *  yldine) ning sellele vastav(ad) spetsiifilised semantikadefinitsioon(id);
 *   
 *  @author Siim Orasmaa
 */
public class FraasiMustriFilter {
	
	/**
	 *   Filter morfoloogiliste tunnuste p&otilde;hjal filtreerimiseks. 
	 */
	private List<String []> morfFilter = null;

	/**
	 *   Filter leitud alammustriosa j&auml;rgi filtreerimiseks. 
	 */
	private List<String> seotudMustriOsaFilter = null;
	
	/**
	 *   Semantikadefinitsioonid, mida kasutatakse ajavaljendi arvutamisel, eeldusel et eraldatud fraasiosa
	 *   l&auml;heb edukalt sellest filtrist l&auml;bi. 
	 */
	private List<SemantikaDefinitsioon> semDefinitsioonid = null;
	
	/**
	 *   Mustritahised, mis kleebitakse ajavaljendikandidaadi kylge, kui eraldatud fraasiosa l&auml;heb 
	 *   edukalt sellest filtrist l&auml;bi. 
	 */
	private List<MustriTahis> mustriTahised = null;

	/**
	 *   Initsialiseerib fraasimustri filtri. Sisendiks filtreerimismustrite 
	 *  kirjeldused. 
	 *  <p>
	 *  Kui mustrite kirjeldused puuduvad (st on tyhis6ned), v6ib eeldada, et
	 *  <tt>rakendaFiltrit</tt> tagastab iga sisendi korral 
	 *  <tt>semDefinitsioonid</tt>.
	 */
	public FraasiMustriFilter(String morfFilter, String seotudMustriOsaFilter) {
		if (morfFilter != null){
			StringTokenizer stk = new StringTokenizer(morfFilter);
			this.morfFilter = new ArrayList<String[]>(stk.countTokens());
			while (stk.hasMoreElements()) {
				String string = (String) stk.nextElement();
				string = string.replaceAll("(\\{|\\})","");
				StringTokenizer stk2 = new StringTokenizer( string, "," );
				String [] morphPart = new String[stk2.countTokens()];
				int i = 0;
				while (stk2.hasMoreElements()) {
					morphPart[i++] = (String) stk2.nextElement();
				}
				(this.morfFilter).add(morphPart);
			}
		}
		if (seotudMustriOsaFilter != null){
			StringTokenizer stk = new StringTokenizer(seotudMustriOsaFilter);
			this.seotudMustriOsaFilter = new ArrayList<String>(stk.countTokens());
			while (stk.hasMoreElements()) {
				String s = (String) stk.nextElement();
				(this.seotudMustriOsaFilter).add(s);
			}
		}
	}

	//==============================================================================
	//   	F i l t r e e r i m i n e
	//==============================================================================

	/**
	 *    Rakendab filtrit eraldamise tulemuste (rahuldatud mustriosad ja neile vastavad
	 *   alamfraasid) peal. Kui filter 6nnestub edukalt l&auml;bida, tagastab <tt>true</tt>.
	 */
	private boolean rakendaFiltrit(HashMap<String, List<AjavtSona>> malliRahuldavadAlamFraasid, 
								  HashMap<String, String> rahuldatudMustriosad){
		if (this.seotudMustriOsaFilter != null){
			if (!kasSeotudMustriOsaOnPositiivseltRahuldatud(
													malliRahuldavadAlamFraasid, 
													rahuldatudMustriosad)){
				return false;
			}
		}
		if (this.morfFilter != null){
			if (!kasMorfOsaOnPositiivseltRahuldatud(malliRahuldavadAlamFraasid,rahuldatudMustriosad)){
				return false;
			}
		}
		return true;
	}
	
	/**
	 *    Rakendab filtrit eraldamise tulemuste (rahuldatud mustriosad ja neile vastavad
	 *   alamfraasid) peal. Kui filter 6nnestub edukalt l&auml;bida, tagastab see meetod 
	 *   rahuldatud mustriosa t&auml;iendavalt kirjeldavad semantikadefinitsioonid. 
	 *   Kui filtrit labida ei 6nnestu, tagastatakse <tt>null</tt>.
	 */
	public List<SemantikaDefinitsioon> rakendaFiltritJaTagastaSonaMallid(
					HashMap<String, List<AjavtSona>> malliRahuldavadAlamFraasid, 
					HashMap<String, String> rahuldatudMustriosad){
		return (this.rakendaFiltrit(malliRahuldavadAlamFraasid, rahuldatudMustriosad))?(this.semDefinitsioonid):(null);
	}

	/**
	 *    Rakendab filtrit eraldamise tulemuste (rahuldatud mustriosad ja neile vastavad
	 *   alamfraasid) peal. Kui filter 6nnestub edukalt l&auml;bida, tagastab see meetod 
	 *   rahuldatud mustriosa t&auml;hise (<tt>MustriTahis</tt>). 
	 *   Kui filtrit labida ei 6nnestu, tagastatakse <tt>null</tt>.
	 */
	public List<MustriTahis> rakendaFiltritJaTagastaMustriTahised(
					HashMap<String, List<AjavtSona>> malliRahuldavadAlamFraasid, 
					HashMap<String, String> rahuldatudMustriosad){
		if (this.rakendaFiltrit(malliRahuldavadAlamFraasid, rahuldatudMustriosad)){
			// 1) Koopia selle filtri alla kuuluvatest tahistest
			List<MustriTahis> mustriTahisedTagasi = new ArrayList<MustriTahis>(	this.mustriTahised );
			// 2) Eemaldame tahised, mille seotudMustriOsa ei klapi			
			Iterator<MustriTahis> iterator = mustriTahisedTagasi.iterator();
			while(iterator.hasNext()){
				MustriTahis mustriTahis = iterator.next();
				List<String> posRahMustriosad = 
					mustriTahis.leiaPositiivseltRahuldatudMustriosad(malliRahuldavadAlamFraasid, rahuldatudMustriosad);
				if (posRahMustriosad == null){
					iterator.remove();
				}
			}
			// 3) Tagastame, kui on mida tagastada
			if (!mustriTahisedTagasi.isEmpty()){
				return mustriTahisedTagasi;
			}
		}
		return null;
	}
	
	/**
	 *   Testib, kas rahuldatud mustriosad l&auml;hevad l&auml;bi filtri <tt>seotudMustriOsaFilter</tt>.
	 *   Tagastab <tt>true</tt> l&auml;bimineku korral;
	 */
	private boolean kasSeotudMustriOsaOnPositiivseltRahuldatud(
					HashMap<String, List<AjavtSona>> malliRahuldavadAlamFraasid, 
					HashMap<String, String> rahuldatudMustriosad){
		for (int i = 0; i < (this.seotudMustriOsaFilter).size(); i++) {
			String mustriElement = (this.seotudMustriOsaFilter).get(i);
			boolean eitus        = mustriElement.startsWith("^");
			if (eitus){
				mustriElement = mustriElement.replace("^", "");
				// Kontrollime, et antud mustrit EI esinenud - kui esines, on mittesobivus (mismatch)
				if (rahuldatudMustriosad.containsKey(mustriElement) || 
						malliRahuldavadAlamFraasid.containsKey(mustriElement)){
					return false;
				}
			} else {
				// Kontrollime, et antud mustrit esines - kui ei esinenud, on mittesobivus (mismatch)
				if (!rahuldatudMustriosad.containsKey(mustriElement) && 
						!malliRahuldavadAlamFraasid.containsKey(mustriElement)){
					return false;
				}
			}
		}
		return true;
	}

	/**
	 *   Testib, kas rahuldatud mustriosad l&auml;hevad l&auml;bi filtri morfoloogilist osa t&auml;psustava
	 *   filtri. Tagastab <tt>true</tt> l&auml;bimineku korral, vastasel juhul <tt>false</tt>.
	 *   <p>
	 *   NB! Praegu rakendub filter k&otilde;igil alamfraasi osadel v&otilde;rdselt (nt kui n&otilde;utakse
	 *   k&auml;&auml;net, kontrollitakse, et k6ik selle alamfraasi s6nad oleksid yhes k&auml;&auml;ndes);
	 *   See v&otilde;ib aga problemaatiline olla n2iteks arvs6nafraasis, kus yhte arvu kirjeldavate s6nade
	 *   kaanded v6ivad erineda;
	 */
	private boolean kasMorfOsaOnPositiivseltRahuldatud(
						HashMap<String, List<AjavtSona>> malliRahuldavadAlamFraasid, 
						HashMap<String, String> rahuldatudMustriosad){
		for (int i = 0; i < (this.morfFilter).size(); i++) {
			String morfKirjeldus [] = (this.morfFilter).get(i);
			String key = String.valueOf(i);
			if (malliRahuldavadAlamFraasid.containsKey(key)){
				for (int j = 0; j < morfKirjeldus.length; j++) {
					String noutudMorfTunnus = morfKirjeldus[j];
					if (!noutudMorfTunnus.equals("_")){
						String morfAlternatiivid [] = null;
						if (noutudMorfTunnus.indexOf("|") > -1){
							morfAlternatiivid = noutudMorfTunnus.split("\\|");
						}
						List<AjavtSona> alamFraas = malliRahuldavadAlamFraasid.get(key);
						for (AjavtSona ajavtSona : alamFraas) {
							if (ajavtSona.kasLeidusAnalyys()){
								List<MorfAnRida> analyysiTulemused = ajavtSona.getAnalyysiTulemused();
								boolean vastavMorfTunnusLeitud = false;
								for (MorfAnRida morfAnRida : analyysiTulemused) {
									if (morfAlternatiivid == null){
										//
										//   Kontrollime ainult yhte morf tunnust
										// 
										if (morfAnRida.leiaKasMorfTunnusEsineb(noutudMorfTunnus)){
											vastavMorfTunnusLeitud = true;
										}
									} else {
										//
										//   Kontrollime mitut morf tunnuse alteratiivset varianti:
										//   vaid yks neist peab vastama ...
										// 
										for (int k = 0; k < morfAlternatiivid.length; k++) {
											if (morfAnRida.leiaKasMorfTunnusEsineb(morfAlternatiivid[k])){
												vastavMorfTunnusLeitud = true;
												break;
											}
										}
									}
								}
								if (!vastavMorfTunnusLeitud){
									return false;
								}
							} else {
								return false;
							}
						}
					}
				}
			}
		}	
		return true;
	}
	
	//==============================================================================
	//   	G e t t e r s   &   S e t t e r s 
	//==============================================================================

	public boolean leidubMustriTahis(){
		return (this.mustriTahised != null && !(this.mustriTahised).isEmpty());
	}
	
	public void setSemDefinitsioonid(List<SemantikaDefinitsioon> semDefinitsioonid) {
		this.semDefinitsioonid = semDefinitsioonid;
	}

	public void addMustriTahis(MustriTahis mustriTahis) {
		if (this.mustriTahised == null){
			this.mustriTahised = new ArrayList<MustriTahis>();
		}
		this.mustriTahised.add(mustriTahis);
	}
	
}
