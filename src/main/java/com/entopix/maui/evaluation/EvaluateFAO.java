package com.entopix.maui.evaluation;

import java.util.ArrayList;
import java.util.List;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.filters.MauiFilter.MauiFilterException;
import com.entopix.maui.main.MauiModelBuilder;
import com.entopix.maui.main.MauiTopicExtractor;
import com.entopix.maui.stemmers.SremovalStemmer;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsFactory;
import com.entopix.maui.util.DataLoader;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.util.MauiTopics;
import com.entopix.maui.util.Evaluator;
import com.entopix.maui.vocab.Vocabulary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Utils;


public class EvaluateFAO {

	private static final Logger log = LoggerFactory.getLogger(EvaluateFAO.class);

    private final Stemmer stemmer = new SremovalStemmer();
    private final String language = "en";
    private String encoding = "UTF-8";
	private final Stopwords stopwords = StopwordsFactory.makeStopwords(language);
    
    private Vocabulary vocabulary;
    private MauiTopicExtractor topicExtractor;
	private MauiModelBuilder modelBuilder;
	
    
    public EvaluateFAO(String vocabularyPath, String vocabularyName) {
		this.vocabulary = new Vocabulary();
		vocabulary.setStemmer(this.stemmer);
		vocabulary.setLanguage(this.language);
    	vocabulary.setStopwords(this.stopwords);
    	try {
			DataLoader.loadVocabulary(vocabulary, vocabularyPath, vocabularyName);
		} catch (Exception e) {
			log.error("Error while loading vocabulary from " + vocabularyPath);
			log.error(e.getMessage());
			throw new RuntimeException();
		}
    	
    	topicExtractor = new MauiTopicExtractor();
		modelBuilder = new MauiModelBuilder();
		
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
		modelBuilder.setNodeDegreeFeature(true);
		modelBuilder.setVocabulary(vocabulary);
		modelBuilder.setVocabularyName(vocabularyName);
		modelBuilder.modelName = "test";
    }
    

	private void runEvaluation(List<MauiDocument> testDocuments) throws MauiFilterException {
		int numDocs = testDocuments.size();
		int fold = 10;
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
			MauiFilter mauiFilter = this.modelBuilder.buildModel(train);
			long stopTime = System.currentTimeMillis();
			long elapsedTime = stopTime - startTime;
			log.info("Built the model in " + elapsedTime + "ms.");
			
			topicExtractor.setModel(mauiFilter);
			List<MauiTopics> topics = topicExtractor.extractTopics(test);
			topicExtractor.printTopics(topics);
			double[] PRF = Evaluator.evaluateTopics(topics);
			precision[run - 1] = PRF[0];
			recall[run - 1] = PRF[1];
			fmeasure[run - 1] = PRF[2];
			
		}
		
		double avgRecall = Utils.mean(recall);
		double avgPrecision = Utils.mean(precision);
		double avgFmeasure = Utils.mean(fmeasure);
		
		log.info("Precision " + avgPrecision + "; Recall " + avgRecall + "; F-Measure " + avgFmeasure);
		
	}
	
	/**
	 * @param args
	 * @throws MauiFilterException 
	 */
	public static void main(String[] args) throws MauiFilterException {
		String datasetPath = "/Users/zelandiya/Documents/Data/Entopix/fao_test";
		
		long startTime = System.currentTimeMillis();
		List<MauiDocument> testDocuments = DataLoader.loadTestDocuments(datasetPath);
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		log.info("Loaded " + testDocuments.size()  + " documents in " + elapsedTime + "ms.");
		
		String vocabularyPath = "/Users/zelandiya/Documents/Data/Entopix/AGROVOC/";
		String vocabularyName = "agrovoc_en";
		
		EvaluateFAO evaluation = new EvaluateFAO(vocabularyPath, vocabularyName);
	
		evaluation.runEvaluation(testDocuments);
	}

}
