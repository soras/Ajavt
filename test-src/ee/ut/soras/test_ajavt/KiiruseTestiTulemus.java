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

import java.util.ArrayList;
import java.util.List;

import org.joda.time.DurationFieldType;
import org.joda.time.Period;
import org.joda.time.PeriodType;

import ee.ut.soras.ajavtV2.util.LogiPidaja;

/**
 *   Klass AjaVT protsessimiskiiruste salvestamiseks. Voimaldab iga faili kohta salvestada eraldi 
 *  t3mesta'le kulunud aega, ajavt-le kulunud aega ning l6puks leida protsessimise kiirust. 
 * 
 *   @author Siim Orasmaa
 */
public class KiiruseTestiTulemus {

	List<Long> preprocessExecTimes  = new ArrayList<Long>();
	List<Long> ajavtProcessTimes    = new ArrayList<Long>();
	List<Long> t3mestaTimes         = new ArrayList<Long>();	
	List<Long> fileSizesInBytes  = new ArrayList<Long>();
	List<Long> fileSizesInCPs    = new ArrayList<Long>();
	
	//==============================================================================
	//   	G e t t e r s     a n d     S e t t e r s
	//==============================================================================
	
	public List<Long> getPreprocessExecTimes() {
		return preprocessExecTimes;
	}
	
	public void addPreprocessExecTimes(long execTime) {
		(this.preprocessExecTimes).add(execTime);
	}
	
	public void addPreprocessExecTimes(String execTime) {
		try {
			long time = Long.parseLong(execTime);
			(this.preprocessExecTimes).add(time);
		} catch (Exception e) {
		}
	}
	
	public List<Long> getAjavtProcessTimes() {
		return ajavtProcessTimes;
	}
	
	public void addAjavtProcessTimes(long ajavtProcessTime) {
		(this.ajavtProcessTimes).add(ajavtProcessTime);
	}

	public void addAjavtProcessTimes(String ajavtProcessTime) {
		try {
			long time = Long.parseLong(ajavtProcessTime);
			(this.ajavtProcessTimes).add(time);
		} catch (Exception e) {
		}
	}

	public List<Long> getFileSizesInBytes(){
		return fileSizesInBytes;
	}

	public void addFileSizeInBytes(Long fileSize) {
		(this.fileSizesInBytes).add(fileSize);
	}

	/**
	 *   Add file size in codepoints.
	 */
	public void addFileSizeInCPs(Long fileSize) {
		(this.fileSizesInCPs).add(fileSize);
	}

	public void addT3MestaTimes(Long time) {
		(this.t3mestaTimes).add(time);
	}
	
	//==============================================================================
	//   	C a l c u l a t i n g    r e s u l t s
	//==============================================================================
	
	private double getAverage(List<Long> results){
		double sum = 0.0;
		for (Long resultTimeMS : results) {
			sum += resultTimeMS;
		}
		return (sum / results.size());
	}
	
	private String convertToOtherFields(double timeInMills){
		Period p = new Period((long)timeInMills, PeriodType.millis());
		p = p.normalizedStandard();
		DurationFieldType[] fieldTypes = p.getFieldTypes();
		int[] values = p.getValues();
		if (values.length == fieldTypes.length){
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < values.length; i++) {
				if (values[i] != 0){
					sb.append(values[i] + " "+ fieldTypes[i].getName()+ " ");
				}
			}
			return sb.toString();
		}
		return p.toString();
	}
	
	private double getSpeed(List<Long> preprocess, List<Long> ajavt, List<Long> fileSizes){
		double avgSpeed = 0.0;
		if (preprocess.size() == ajavt.size() && ajavt.size() == fileSizes.size()){
			for (int i = 0; i < preprocess.size(); i++) {
				double time = preprocess.get(i) + ajavt.get(i);
				double size = fileSizes.get(i);
				avgSpeed += size / time;
			}
			return (avgSpeed / preprocess.size());
		} else if (ajavt.size() == fileSizes.size()){
			for (int i = 0; i < fileSizes.size(); i++) {
				double time = ajavt.get(i);
				double size = fileSizes.get(i);
				avgSpeed += size / time;
			}
			return (avgSpeed / fileSizes.size());
		}
		return avgSpeed;
	}
	
	//==============================================================================
	//   	P r i n t i n g   r e s u l t s
	//==============================================================================
	
	public void printResultsOfLastTagging(LogiPidaja logi){
	    logi.println();
	    logi.println("----------------------------------------------------");
 	    logi.println(" ");
 	    List<Long> preprocess  = preprocessExecTimes.subList(preprocessExecTimes.size()-1, preprocessExecTimes.size());
 	    List<Long> t3mesta     = t3mestaTimes.subList(t3mestaTimes.size()-1, t3mestaTimes.size());
 	    List<Long> ajavt       = ajavtProcessTimes.subList(ajavtProcessTimes.size()-1, ajavtProcessTimes.size());
 	    List<Long> fileSizeBytes = fileSizesInBytes.subList(fileSizesInBytes.size()-1, fileSizesInBytes.size());
 	    List<Long> fileSizeCPs   = fileSizesInCPs.subList(fileSizesInCPs.size()-1, fileSizesInCPs.size());
 	    logi.println(" Last file size:    "+fileSizeBytes.get(0)+" bytes");
 	    logi.println("                    "+fileSizeCPs.get(0)+" codepoints");
 	    logi.println(" Last preprocess time: "+convertToOtherFields(preprocess.get(0)));
 	    logi.println(" Last t3mesta time:    "+convertToOtherFields(t3mesta.get(0)));
 	    logi.println(" Last ajavt time:      "+convertToOtherFields(ajavt.get(0)));
 	    double speedBytes = getSpeed(preprocess, ajavt, fileSizeBytes);
 	    double speedCodePoint = getSpeed(preprocess, ajavt, fileSizeCPs);
 	    logi.println(" Last speed: "+speedBytes+" bytes/millisec");
 	    logi.println("             "+speedCodePoint+" codepoints/millisec");
	}

	public void printOverallResults(LogiPidaja logi){
	    logi.println();
	    logi.println("----------------------------------------------------");
 	    logi.println(" Avg file size:    "+getAverage(fileSizesInBytes)+" bytes");
 	    logi.println("                   "+getAverage(fileSizesInCPs)+" codepoints");
 	    logi.println(" Avg preprocess time: "+convertToOtherFields(getAverage(preprocessExecTimes)));
 	    logi.println(" Avg t3mesta time:    "+convertToOtherFields(getAverage(t3mestaTimes))); 	    
 	    logi.println(" Avg ajavt time:      "+convertToOtherFields(getAverage(ajavtProcessTimes)));
 	    logi.println();
 	    double speedBytes     = getSpeed(preprocessExecTimes, ajavtProcessTimes, fileSizesInBytes);
 	    double speedCodePoint = getSpeed(preprocessExecTimes, ajavtProcessTimes, fileSizesInCPs);
 	    logi.println(" Avg speed: "+speedBytes+" bytes/millisec");
 	    logi.println("            "+speedCodePoint+" codepoints/millisec");
 	    
	}
	
}
