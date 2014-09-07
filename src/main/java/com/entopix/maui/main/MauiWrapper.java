package com.entopix.maui.main;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
//import org.apache.log4j.BasicConfigurator;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.filters.MauiFilter.MauiFilterException;
import com.entopix.maui.stemmers.SremovalStemmer;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsFactory;
import com.entopix.maui.util.DataLoader;
import com.entopix.maui.util.Topic;
import com.entopix.maui.vocab.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * This class shows how to use Maui on a single document or just a string of
 * text.
 *
 * @author a_medelyan
 *
 */
public final class MauiWrapper {

    private static final Logger log = LoggerFactory.getLogger(MauiWrapper.class);

    // default values of the configuration
    private final Stemmer stemmer = new SremovalStemmer();
    private final String language = "en";
    private final Stopwords stopwords = StopwordsFactory.makeStopwords(language);
    
    // these need to be loaded either from a file, or from a pre-loaded object
    private MauiFilter extractionModel = null;
    private Vocabulary vocabulary = null;
    
    /**
     * Constructor to initialize MauiWrapper with default
     * stemmer, stopwords and language
     */
    public MauiWrapper(String modelName, String vocabularyName, String vocabularyFormat) { 
    	this(modelName, vocabularyName, vocabularyFormat, null, null, null);
    }
    
    /**
     * Constructor to initialize MauiWrapper with pre-set
     * stemmer, stopwords and language.
     * Use null for the parameters that should be left default.
     * @param stopwords
     * @param stemmer
     * @param language
     */
    public MauiWrapper(String modelName, String vocabularyName, String vocabularyFormat, Stopwords stopwords, Stemmer stemmer, String language) {
    	if (!vocabularyName.equals("none")) {
	    	this.vocabulary = new Vocabulary();
	    	if (stemmer != null)
	    		vocabulary.setStemmer(stemmer);
	    	else
	    		vocabulary.setStemmer(this.stemmer);
	    	
	    	if (language != null)
	    		vocabulary.setLanguage(language);
	    	else
	    		vocabulary.setLanguage(this.language);
	    	
	    	if (stopwords != null)
	            vocabulary.setStopwords(stopwords);
	        else
	        	vocabulary.setStopwords(this.stopwords);
	    	
	    	vocabulary.initializeVocabulary(vocabularyName, vocabularyFormat);
    	}
    	this.extractionModel = DataLoader.loadModel(modelName);
    }
    
    /**
     * Constructor, which loads model and vocabulary using pre-loaded objects.
     * Make sure that both objects have the same values for:
     * Stemmer, Stopwords and language
     * 
     * @param vocab Pre-loaded vocabulary
     * @param model Pre-loaded model
     */
    public MauiWrapper(Vocabulary vocab, MauiFilter model) {
        this.vocabulary = vocab;                
        this.extractionModel = model;
    }
    
    

    

    /**
     * Assigns the vocabulary to model
     * @param vocabularyName
     * @param stemmer
     * @param stopwords
     * @param language
     */
    public void setModelParameters(String vocabularyName, Stemmer stemmer, Stopwords stopwords, String language) {
    	
    	if (stemmer != null)
    		extractionModel.setStemmer(stemmer);
    	else
    		extractionModel.setStemmer(this.stemmer);
    	
    	if (language != null)
    		extractionModel.setDocumentLanguage(language);
    	else
    		extractionModel.setDocumentLanguage(this.language);
    
    	if (stopwords != null)
            extractionModel.setStopwords(stopwords);
        else
        	extractionModel.setStopwords(this.stopwords);
       
        extractionModel.setVocabularyName(vocabularyName);
        extractionModel.setVocabularyFormat("skos");
        extractionModel.setVocabulary(vocabulary);
        
        if (vocabularyName.equals("none"))
        	extractionModel.setMinNumOccur(2);
    }

    /**
     * Main method to extract the main topics from a given text
     *
     * @param text
     * @param topicsPerDocument
     * @return
     * @throws Exception
     */
    public ArrayList<Topic> extractTopicsFromText(String text, int topicsPerDocument) throws MauiFilterException {

        if (text.length() < 5) {
            log.warn("Text is too short: " + text.length() + " characters.");
        }

        FastVector atts = new FastVector(3);
        atts.addElement(new Attribute("filename", (FastVector) null));
        atts.addElement(new Attribute("doc", (FastVector) null));
        atts.addElement(new Attribute("keyphrases", (FastVector) null));
        Instances data = new Instances("keyphrase_training_data", atts, 0);

        double[] newInst = new double[3];

        newInst[0] = data.attribute(0).addStringValue("inputFile");
        newInst[1] = data.attribute(1).addStringValue(text);
        newInst[2] = Instance.missingValue();
        data.add(new Instance(1.0, newInst));

        extractionModel.input(data.instance(0));

        data = data.stringFreeStructure();
        Instance[] topRankedInstances = new Instance[topicsPerDocument];
        Instance inst;
        int index = 0;
        // Iterating over all extracted keyphrases (inst)
        while ((inst = extractionModel.output()) != null) {
        	
        	double probability = inst.value(extractionModel.getProbabilityIndex());
            if (index < topicsPerDocument) {
             	if (probability > 0) {
             		topRankedInstances[index] = inst;
             		index++;
             	}
            }
         }

        ArrayList<Topic> topics = new ArrayList<Topic>();

        for (int i = 0; i < topicsPerDocument; i++) {
            if (topRankedInstances[i] != null) {
            	double probability = topRankedInstances[i].value(extractionModel.getProbabilityIndex());
                String topic = topRankedInstances[i].stringValue(extractionModel.getOutputFormIndex());
                String id = "";
                if (vocabulary != null) {
                	id = vocabulary.getFormatedName(topRankedInstances[i].stringValue(0));
                }
                topics.add(new Topic(topic, id, probability));

                /**
                 * Indices of attributes in classifierData
        // General features
                // 2 term frequency
                // 3 inverse document frequency
                // 4 TFxIDF
                // 5 position of the first occurrence
                // 6 position of the last occurrence
                // 7 spread of occurrences
                // 8 domain keyphraseness
                // 9 term length
                // 10 generality
                // 11 node degree
                String features = "";
                for (int j = 2; j < 11; j++) {
                    double value = topRankedInstances[i].value(j);
                    if (value < 1) {
                        value = Math.round(value * 100.0) / 100.0;
                    } else {
                        value = Math.round(value);
                    }
                    features = features + value + ' ';
                }
                topics.add(topic + " #" + vocabulary.getFormatedName(topRankedInstances[i].stringValue(0)) + " >> " + features);
                         */
		
            }
        }
        extractionModel.batchFinished();
        return topics;
    }

    /* Main method to extract the main topics from a given text
     * @param text
     * @param topicsPerDocument
     * @return
     * @throws Exception
     */
    public List<Topic> extractTopicsFromTextAsResults(String text, int topicsPerDocument) throws Exception {

        if (text.length() < 5) {
            throw new Exception("Text is too short!");
        }

        FastVector atts = new FastVector(3);
        atts.addElement(new Attribute("filename", (FastVector) null));
        atts.addElement(new Attribute("doc", (FastVector) null));
        atts.addElement(new Attribute("keyphrases", (FastVector) null));
        Instances data = new Instances("keyphrase_training_data", atts, 0);

        double[] newInst = new double[3];

        newInst[0] = data.attribute(0).addStringValue("inputFile");
        newInst[1] = data.attribute(1).addStringValue(text);
        newInst[2] = Instance.missingValue();
        data.add(new Instance(1.0, newInst));

        extractionModel.input(data.instance(0));

        data = data.stringFreeStructure();
        Instance[] topRankedInstances = new Instance[topicsPerDocument];
        Instance inst;

        // Iterating over all extracted keyphrases (inst)
        while ((inst = extractionModel.output()) != null) {

            int index = (int) inst.value(extractionModel.getRankIndex()) - 1;

            if (index < topicsPerDocument) {
                topRankedInstances[index] = inst;
            }
        }

        List<Topic> topics = new ArrayList<Topic>();

        for (int i = 0; i < topicsPerDocument; i++) {
            if (topRankedInstances[i] != null) {
                String id = vocabulary.getFormatedName(topRankedInstances[i].stringValue(0));
                String title = topRankedInstances[i].stringValue(extractionModel.getOutputFormIndex());
                double probability = topRankedInstances[i].value(extractionModel.getProbabilityIndex());
                topics.add(new Topic(title, id, probability));
            }
        }
        extractionModel.batchFinished();
        return topics;
    }

    /**
     * Triggers topic extraction from a text file
     *
     * @param filePath
     * @param numberOfTopics
     * @return
     * @throws MauiFilterException 
     * @throws Exception
     */
    public ArrayList<Topic> extractTopicsFromFile(String filePath, int numberOfTopics) throws IOException, MauiFilterException {
        File documentTextFile = new File(filePath);
        String documentText = FileUtils.readFileToString(documentTextFile);
        return extractTopicsFromText(documentText, numberOfTopics);
    }

}
