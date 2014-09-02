package com.entopix.maui.main;


import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.stemmers.FrenchStemmer;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsFrench;
import com.entopix.maui.util.DataLoader;
import org.junit.Test;


public class FrenchExampleTest {

	/**
	 * @throws Exception
	 */
	@Test
	public void testFrench() throws Exception {
		
		// location of the data
		String trainDir = "src/test/resources/data/term_assignment/train_fr";
		String testDir = "src/test/resources/data/term_assignment/test_fr";
		
		// name of the file for storing the model
		String modelName = "french_model";
		
		// language specific settings
		Stemmer stemmer = new FrenchStemmer();
		Stopwords stopwords = new StopwordsFrench();
		String language = "fr";
		String encoding = "UTF-8";
		
		// vocabulary to use for term assignment
		String vocabulary = "agrovoc_fr";
		String format = "skos";
		
		// how many topics per document to extract
		int numTopicsToExtract = 8;
		
		// maui objects
		MauiModelBuilder modelBuilder = new MauiModelBuilder();
		MauiTopicExtractor topicExtractor = new MauiTopicExtractor();
		
		// Settings for the model builder
		modelBuilder.inputDirectoryName = trainDir;
		modelBuilder.modelName = modelName;
		modelBuilder.vocabularyFormat = format;
		modelBuilder.vocabularyName = vocabulary;
		modelBuilder.stemmer = stemmer;
		modelBuilder.stopwords = stopwords;
		modelBuilder.documentLanguage = language;
		modelBuilder.documentEncoding = encoding;
		modelBuilder.debugMode = true;
		
		// Which features to use?
		modelBuilder.setBasicFeatures(true);
		modelBuilder.setKeyphrasenessFeature(true);
		modelBuilder.setFrequencyFeatures(false);
		modelBuilder.setPositionsFeatures(true);
		modelBuilder.setLengthFeature(true);
		modelBuilder.setNodeDegreeFeature(true);
		
		// Run model builder
		MauiFilter filter = modelBuilder.buildModel(DataLoader.loadTestDocuments(trainDir));
		modelBuilder.saveModel(filter);
		
		// Settings for the topic extractor
		topicExtractor.inputDirectoryName = testDir;
		topicExtractor.modelName = modelName;
		topicExtractor.vocabularyName = vocabulary;
		topicExtractor.vocabularyFormat = format;
		topicExtractor.stemmer = stemmer;
		topicExtractor.stopwords = stopwords;
		topicExtractor.documentLanguage = language;
		topicExtractor.debugMode = true;
		topicExtractor.topicsPerDocument = numTopicsToExtract; 
		topicExtractor.cutOffTopicProbability = 0.0;
		
		// Run topic extractor
		topicExtractor.loadModel();
		topicExtractor.extractTopics(DataLoader.loadTestDocuments(testDir));
	}

}
