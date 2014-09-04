package com.entopix.maui.wikifeatures;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class WikiFeaturesTest {
	
	private static final Logger log = LoggerFactory.getLogger(WikiFeaturesTest.class);
	
	@Test
	public void wikiFeaturesTest()
	{
		WikiFeatures features = new WikiFeatures();

		long startTime, endTime;
		startTime = System.currentTimeMillis();
		features.load_csv("src/main/resources/data/labels.csv.gzip", true );
		endTime = System.currentTimeMillis();
		log.info("Reading in Took "+(endTime-startTime)+"ms");
		
		log.info("Auckland:");
		log.info("\tgenerality: " + features.getWikipediaGenerality("auckland"));
		log.info("\tkeyphraseness: " + features.getWikipediaKeyphraseness("auckland"));
		log.info("\tnumber of inlinks: " + features.getInversedWikipediaFrequency("auckland"));
		log.info("New Zealand");
		log.info("\tgenerality: " + features.features.get("New Zealand").generality);
		log.info("\tkeyphraseness: " + features.features.get("New Zealand").keyphrasness);
		log.info("\tnumber of inlinks: " + features.features.get("New Zealand").number_in_links);
		
		log.info("Num features "+features.features.size());
		

	}

}
