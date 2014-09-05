package com.entopix.maui.evaluation;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import com.entopix.maui.util.IndexerTopics;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.util.MauiTopics;
import com.entopix.maui.vocab.Vocabulary;

public class IndexingConsistencyTest {

	private static final Logger log = LoggerFactory.getLogger(IndexingConsistencyTest.class);

	public void testIndexingConsistency() throws MauiFilterException {

		// Input data
		String vocabularyPath = "/Users/zelandiya/Documents/Data/Entopix/AGROVOC"; //"src/test/resources/data/vocabularies";
		String vocabularyName = "agrovoc_en"; // "agrovoc_sample";
		String datasetPath = "src/test/resources/data/term_assignment/test_fao30/documents";
		String indexers = "src/test/resources/data/term_assignment/test_fao30/indexers";

		Stemmer stemmer = new PorterStemmer();
		String language = "en";
		String encoding = "UTF-8";
		Stopwords stopwords = StopwordsFactory.makeStopwords(language);

		Vocabulary vocabulary  = new Vocabulary();
		vocabulary.setStemmer(stemmer);
		vocabulary.setLanguage(language);
		vocabulary.setStopwords(stopwords);
		vocabulary.setSerialize(true);
		try {
			DataLoader.loadVocabulary(vocabulary, vocabularyPath, vocabularyName);
		} catch (Exception e) {
			log.error("Error while loading vocabulary from " + vocabularyPath);
			log.error(e.getMessage());
			throw new RuntimeException();
		}

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
		List<IndexerTopics> indexersTopics = DataLoader.readIndexersTopics(indexers);
		DataLoader.addTopicsFromIndexers(testDocuments, indexersTopics);

		int numDocs = testDocuments.size();

		int fold = numDocs - 1;
		int part = numDocs/fold;
		int startTest, endTest;

		List<MauiTopics> allTopics = new ArrayList<MauiTopics>();

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
			allTopics.addAll(topics);

		}

		Evaluator.evaluateConsistency(indexersTopics, allTopics);
	}



	@Test
	public void indexingConsistencyTest() throws Exception {
		IndexingConsistencyTest validationTest = new IndexingConsistencyTest();

		long startTime = System.currentTimeMillis();
		validationTest.testIndexingConsistency();
		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		log.info("Completed in " + elapsedTime + "ms.");
	}
}
