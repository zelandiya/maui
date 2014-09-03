package com.entopix.maui.wikifeatures;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.Input;


public class WikiFeatures {

	public HashMap<String,WikiStringFeatures> features;
	
	public void load_csv(String filename, boolean gzipped ) {
		BufferedReader br = null;
		String line = "";
		features = new HashMap<String,WikiStringFeatures>();
		try
		{
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
		}
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		} catch (IOException e) 
		{
			e.printStackTrace();
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
					e.printStackTrace();
				}
			}
		}
	}
	
	public void load_serialized(String filename) {
		try {
			Kryo kryo = new Kryo();
		    kryo.register(HashMap.class);
		    kryo.register(WikiStringFeatures.class);
		    File file = new File(filename);
			Input input = new Input(
					new GZIPInputStream(
					new BufferedInputStream(
					new FileInputStream(file))));
		    kryo.readObject(input, HashMap.class);
		    input.close();
		} catch(IOException i) {
			System.err.println("Error writing index into " + filename);
			i.printStackTrace();
		}
	}

	public void save_serialized(String filename) {
		try {
			Kryo kryo = new Kryo();
		    kryo.register(HashMap.class);
		    kryo.register(WikiStringFeatures.class);
		    File file = new File(filename);
			Output output = new Output(
					new GZIPOutputStream(
					new BufferedOutputStream(
					new FileOutputStream(file))));
		    kryo.writeObject(output, features);
		    output.close();
		} catch(IOException i) {
			System.err.println("Error writing index into " + filename);
			i.printStackTrace();
		}
	}

}
