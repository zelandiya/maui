package com.entopix.maui.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Utils;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.filters.MauiFilter.MauiFilterException;
import com.entopix.maui.main.MauiModelBuilder;
import com.entopix.maui.main.MauiTopicExtractor;
import com.entopix.maui.stemmers.PorterStemmer;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsFactory;
import com.entopix.maui.util.DataLoader;
import com.entopix.maui.util.Evaluator;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.util.MauiTopics;
import com.entopix.maui.vocab.Vocabulary;

public class CrossValidationTest {

	private static final Logger log = LoggerFactory.getLogger(CrossValidationTest.class);
	
	public void testCrossValidation() throws MauiFilterException {
		
		// Input data
		String vocabularyPath = "/Users/zelandiya/Documents/Data/Entopix/AGROVOC"; //"src/test/resources/data/vocabularies";
		String vocabularyName = "agrovoc_en"; // "agrovoc_sample";
//		String datasetPath = "/Users/zelandiya/Documents/Data/Entopix/fao780"; //"src/test/resources/data/term_assignment/train";
//		
//		String vocabularyPath = "src/test/resources/data/vocabularies";
//		String vocabularyName = "agrovoc_sample";
		String datasetPath = "/Users/zelandiya/Documents/Data/Entopix/fao_test"; //"src/test/resources/data/term_assignment/train";
		
		// Number of validation folds
		// If fold equals the number of documents in the dataset,
		// then it's a Leave-One-Out
		int fold = 10;
		
		Stemmer stemmer = new PorterStemmer();
	    String language = "en";
	    String encoding = "UTF-8";
		Stopwords stopwords = StopwordsFactory.makeStopwords(language);
	    
	    Vocabulary vocabulary  = new Vocabulary();
	    vocabulary.setStemmer(stemmer);
		vocabulary.setLanguage(language);
    	vocabulary.setStopwords(stopwords);
    	vocabulary.setSerialize(false);
    	vocabulary.initializeVocabulary(vocabularyPath, vocabularyName);
    	
	    MauiTopicExtractor topicExtractor = new MauiTopicExtractor();
		MauiModelBuilder modelBuilder = new MauiModelBuilder();
		
    	modelBuilder.stemmer = stemmer;
		modelBuilder.stopwords = stopwords;
		modelBuilder.documentLanguage = language;
		modelBuilder.documentEncoding = encoding;
		topicExtractor.stemmer = stemmer;
		topicExtractor.stopwords = stopwords;
		topicExtractor.documentEncoding = encoding;
		topicExtractor.documentLanguage = language;
		
		modelBuilder.setBasicFeatures(true);
		modelBuilder.setKeyphrasenessFeature(true);
		modelBuilder.setFrequencyFeatures(true);
		modelBuilder.setPositionsFeatures(true);
		modelBuilder.setLengthFeature(true);
		modelBuilder.setThesaurusFeatures(true);
		modelBuilder.setWikipediaFeatures(false);
		
		modelBuilder.setVocabulary(vocabulary);
		modelBuilder.setVocabularyName(vocabularyName);
		modelBuilder.modelName = "test";
		
		List<MauiDocument> testDocuments = DataLoader.loadTestDocuments(datasetPath);

		int numDocs = testDocuments.size();
		int part = numDocs/fold;
		int startTest, endTest;
		
		double[] precision = new double[fold];
		double[] recall = new double[fold];
		double[] fmeasure = new double[fold];
		
		for (int run = 1; run <= fold; run++) {
			startTest = (run - 1) * part;
			endTest = startTest + part;
			log.info("Run " + run + ": Start index = " + startTest + ", end index = " + endTest);
			List<MauiDocument> test = new ArrayList<MauiDocument>();
			List<MauiDocument> train = new ArrayList<MauiDocument>();
			for (int i = 0; i < testDocuments.size(); i++) {
				if (i >= startTest && i < endTest) {
					test.add(testDocuments.get(i));
				} else {
					train.add(testDocuments.get(i));
				}
			}
			
			long startTime = System.currentTimeMillis();
			MauiFilter mauiFilter = modelBuilder.buildModel(train);
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			log.info("Built the model in " + elapsedTime + "ms.");
			
			topicExtractor.setModel(mauiFilter);
			List<MauiTopics> topics = topicExtractor.extractTopics(test);
			double[] PRF = Evaluator.evaluateTopics(topics);
			precision[run - 1] = PRF[0];
			recall[run - 1] = PRF[1];
			fmeasure[run - 1] = PRF[2];
			
		}
		
		double avgRecall = Utils.roundDouble(Utils.mean(recall)*100, 2);
		double avgPrecision = Utils.roundDouble(Utils.mean(precision)*100, 2);
		double avgFmeasure = Utils.roundDouble(Utils.mean(fmeasure)*100, 2);
		
		log.info("Precision " + avgPrecision + "; Recall " + avgRecall + "; F-Measure " + avgFmeasure);
		
		
	}
	
	@Test
	public void crossValidationTest() throws Exception {
		CrossValidationTest validationTest = new CrossValidationTest();

		long startTime = System.currentTimeMillis();
		validationTest.testCrossValidation();
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		log.info("Completed in " + elapsedTime + "ms.");
	}
}
