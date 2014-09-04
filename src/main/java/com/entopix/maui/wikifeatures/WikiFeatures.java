package com.entopix.maui.wikifeatures;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WikiFeatures implements Serializable {

	private static final long serialVersionUID = 1L;
	
	private static final Logger log = LoggerFactory.getLogger(WikiFeatures.class);
	
	public HashMap<String,WikiStringFeatures> features;
	
	public int getWikipediaGenerality(String inputString) {
		if (features.containsKey(inputString)) {
			return features.get(inputString).generality;
		} else {
			String capitalizedFirst = capitalizeFirst(inputString);
			if (features.containsKey(capitalizedFirst)) {
				return features.get(capitalizedFirst).generality;
			}
		}
		return -1;
	}
	
	public int getInversedWikipediaFrequency(String inputString) {
		if (features.containsKey(inputString)) {
			return features.get(inputString).number_in_links;
		} else {
			String capitalizedFirst = capitalizeFirst(inputString);
			if (features.containsKey(capitalizedFirst)) {
				return features.get(capitalizedFirst).number_in_links;
			}
		}
		return -1;
	}
	
	public double getWikipediaKeyphraseness(String inputString) {
		// TODO: Should work on lower case also
		if (features.containsKey(inputString)) {
			return features.get(inputString).keyphrasness;
		} else {
			String capitalizedFirst = capitalizeFirst(inputString);
			if (features.containsKey(capitalizedFirst)) {
				return features.get(capitalizedFirst).keyphrasness;
			}
		}
		return 0.0;
	}
 	
	private String capitalizeFirst(String inputString) {
		char first = inputString.charAt(0);
		return Character.toUpperCase(first) + inputString.substring(1);
	}

	public void load_csv(String filename, boolean gzipped ) {
		BufferedReader br = null;
		String line = "";
		features = new HashMap<String,WikiStringFeatures>();
		try
		{
			log.info("Loading the data required for Wikipedia Features...");
			InputStream r = new FileInputStream(filename);
			if( gzipped )
				r = new GZIPInputStream( r );
			br = new BufferedReader(new InputStreamReader(r) );
			while ((line = br.readLine()) != null) {
				// use comma as separator
				String[] values = line.split(",");
				WikiStringFeatures features = new WikiStringFeatures();
				features.number_in_links = Integer.parseInt(values[values.length-1]);
				features.generality = Integer.parseInt(values[values.length-2]);
				features.keyphrasness = Float.parseFloat(values[values.length-3]);
				String link_name = values[0];
				if( values.length > 4 )
				{
					StringBuilder builder = new StringBuilder();
					builder.append( link_name );
					for(int i=1;i<values.length-3;i++) {
					    builder.append(values[i]);
					}
					link_name =  builder.toString();
				}
				this.features.put(link_name,features);
			}
			
			log.info("...completed!");
		}
		catch (FileNotFoundException e) 
		{
			log.error("File " + filename + " could not be found!");
		} catch (IOException e) 
		{
			log.error("Error reading " + filename + "!");
		} 
		catch (NumberFormatException e) 
		{
			e.printStackTrace();
		} 
		finally {
			if (br != null) {
				try {
					br.close();
				} 
				catch (IOException e) {
					log.error("Error closing " + filename + "!");
				}
			}
		}
	}
}
