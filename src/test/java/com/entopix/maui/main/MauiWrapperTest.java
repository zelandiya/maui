package com.entopix.maui.main;

import java.io.IOException;
import java.util.ArrayList;

import com.entopix.maui.filters.MauiFilter.MauiFilterException;
import com.entopix.maui.util.Topic;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates how to use MauiWrapper,  <br>
 * a class which has methods
 * to extract keywords from a single file
 * or a text string. <br> 
 * 
 * @author zelandiya (medelyan@gmail.com)
 * 
 */
public class MauiWrapperTest {
    
    private static final Logger log = LoggerFactory.getLogger(MauiWrapperTest.class);

	@Test
	public void testMauiWrapper() throws Exception {
		
		// Extracting topics with vocabulary:
        String vocabularyName = "src/test/resources/data/vocabularies/agrovoc_sample.rdf";
        String modelName = "src/test/resources/data/models/test";
        String filePath = "src/test/resources/data/term_assignment/test/w7540e.txt";
        
        MauiWrapper mauiWrapper = null;
		try {
			// Use default stemmer, stopwords and language
			// MauiWrapper also can be initalized with a pre-loaded vocabulary
			// and a pre-loaded MauiFilter (model) objects
			mauiWrapper = new MauiWrapper(modelName, vocabularyName, "skos");
			
			// the last three items should match what was used in the wrapper constructor
			// i.e. null if the defaults were used
			mauiWrapper.setModelParameters(vocabularyName, null, null, null); 
			
		} catch (Exception e) {
			log.error("Problem while loading and initializing vocabulary and model\n" + e);
			throw new RuntimeException();
		}

        try {
        	ArrayList<Topic> keywords = mauiWrapper.extractTopicsFromFile(filePath, 50);
            for (Topic keyword : keywords) {
                log.info("Keyword: " + keyword.getTitle() + " " + keyword.getProbability());
            }
        } catch (IOException e) {
        	log.error("Problem while reading file:\n" + e);
        	throw new RuntimeException();
		} catch (MauiFilterException e) {
			log.error("MauiFilter exception\n" + e);
			throw new RuntimeException();
		}
	}

}
