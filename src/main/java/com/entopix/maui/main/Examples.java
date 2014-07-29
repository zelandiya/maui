package com.entopix.maui.main;

/*
 *    MauiModelBuilder.java
 *    Copyright (C) 2009 Olena Medelyan
 *
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
import java.util.List;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.util.DataLoader;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.util.MauiTopics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Demonstrates how to use Maui:  <br>
 * 1. Keyphrase extraction - extracting significant phrases from 
 *  the document, also suitable for automatic tagging. <br>
 * 2. Term assignment - indexing documents with terms
 * from a controlled vocabulary in SKOS or text format. <br> 
 * 
 * @author zelandiya (medelyan@gmail.com)
 * 
 */
public class Examples {
    
    private static final Logger log = LoggerFactory.getLogger(Examples.class);

	private MauiTopicExtractor topicExtractor;
	private MauiModelBuilder modelBuilder;
	
	/**
	 * Sets general parameters: debugging printout, language specific options
	 * like stemmer, stopwords.
	 * @throws Exception 
	 */
	private void setGeneralOptions()  {
	
		
		modelBuilder.debugMode = true;
		
		/* language specific options
		Stemmer stemmer = new FrenchStemmer();
		Stopwords stopwords = new StopwordsFrench();
		String language = "fr";
		String encoding = "UTF-8";
		modelBuilder.stemmer = stemmer;
		modelBuilder.stopwords = stopwords;
		modelBuilder.documentLanguage = language;
		modelBuilder.documentEncoding = encoding;
		topicExtractor.stemmer = stemmer;
		topicExtractor.stopwords = stopwords;
		topicExtractor.documentLanguage = language;
		*/
		
		/* specificity options
		modelBuilder.minPhraseLength = 1;
		modelBuilder.maxPhraseLength = 5;
		*/
		
		topicExtractor.debugMode = true;
		topicExtractor.topicsPerDocument = 10; 
	}

	/**
	 * Set which features to use
	 */
	private void setFeatures() {
		modelBuilder.setBasicFeatures(true);
		modelBuilder.setKeyphrasenessFeature(true);
		modelBuilder.setFrequencyFeatures(true);
		modelBuilder.setPositionsFeatures(true);
		modelBuilder.setLengthFeature(true);
		modelBuilder.setNodeDegreeFeature(true);
	}

	/**
	 * Demonstrates how to perform automatic tagging / keyphrase extraction.
	 * 
	 * @throws Exception
	 */
	public void testAutomaticTagging() throws Exception {
		topicExtractor = new MauiTopicExtractor();
		modelBuilder = new MauiModelBuilder();
		setGeneralOptions();
		setFeatures();
		
		// Directories with train & test data
		String trainDir = "data/automatic_tagging/train";
		String testDir = "data/automatic_tagging/test";

		// name of the file to save the model
		String modelName = "test";

		// Settings for the model builder
		modelBuilder.inputDirectoryName = trainDir;
		modelBuilder.modelName = modelName;
		
		// change to 1 for short documents
		modelBuilder.minNumOccur = 2;

		// Run model builder
		List<MauiDocument> trainingDocs = DataLoader.loadTestDocuments(trainDir);
		MauiFilter mauiFilter = modelBuilder.buildModel(trainingDocs);
		modelBuilder.saveModel(mauiFilter);

		// Settings for topic extractor
		topicExtractor.inputDirectoryName = testDir;
		topicExtractor.modelName = modelName;
		topicExtractor.setTopicProbability(0.0);
		
		// Run topic extractor
		topicExtractor.loadModel();
		List<MauiDocument> testDocs = DataLoader.loadTestDocuments(testDir);
		topicExtractor.extractTopics(testDocs);
	}

	/**
	 * Demonstrates how to perform term assignment. Applicable to any vocabulary
	 * in SKOS or text format.
	 * 
	 * @throws Exception
	 */
	public void testTermAssignment() throws Exception {
		topicExtractor = new MauiTopicExtractor();
		modelBuilder = new MauiModelBuilder();
		setGeneralOptions();
		setFeatures();
		
		// Directories with train & test data
		String trainDir = "data/term_assignment/train";
		String testDir = "data/term_assignment/test";

		// Vocabulary
		String vocabulary = "agrovoc_sample";
		String format = "skos";

		// name of the file to save the model
		String modelName = "test";

		// Settings for the model builder
		modelBuilder.inputDirectoryName = trainDir;
		modelBuilder.modelName = modelName;
		modelBuilder.vocabularyFormat = format;
		modelBuilder.vocabularyName = vocabulary;
		
		// Run model builder
		List<MauiDocument> trainingDocs = DataLoader.loadTestDocuments(trainDir);
		MauiFilter mauiFilter = modelBuilder.buildModel(trainingDocs);
		modelBuilder.saveModel(mauiFilter);

		// Settings for topic extractor
		topicExtractor.inputDirectoryName = testDir;
		topicExtractor.modelName = modelName;
		topicExtractor.vocabularyName = vocabulary;
		topicExtractor.vocabularyFormat = format;
		topicExtractor.setTopicProbability(0.0);
		
		// Run topic extractor
		topicExtractor.loadModel();
		List<MauiDocument> testDocs = DataLoader.loadTestDocuments(testDir);
		List<MauiTopics> allDocumentsTopics = topicExtractor.extractTopics(testDocs);
		topicExtractor.printTopics(allDocumentsTopics);
	}

	/**
	 * Main method for running the three types of topic indexing. Comment out
	 * the required one.
	 * 
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		
		String mode = "term_assignment";
		// TODO: change to args[0];
		if (!mode.equals("tagging") && !mode.equals("term_assignment")) {
			throw new Exception("Choose one of the two modes: tagging or term_assignment");
		}
		
		long startTime = System.currentTimeMillis();
		Examples exampler;
		if (mode.equals("tagging")) {
			exampler = new Examples();
			exampler.testAutomaticTagging();
		} else if (mode.equals("term_assignment")) {
			exampler = new Examples();
			exampler.testTermAssignment();
		} 
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		log.info("Completed in " + elapsedTime + "ms.");
		
	}

}
