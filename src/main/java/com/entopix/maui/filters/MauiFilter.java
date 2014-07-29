package com.entopix.maui.filters;

/*
 *    MauiTopicExtractor.java
 *    Copyright (C) 2001-2009 Eibe Frank, Olena Medelyan
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
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

import com.entopix.maui.stemmers.PorterStemmer;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsEnglish;
import com.entopix.maui.util.Candidate;
import com.entopix.maui.util.Counter;
import com.entopix.maui.vocab.Vocabulary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Attribute;
import weka.core.Capabilities;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Utils;
import weka.core.Capabilities.Capability;
import weka.filters.Filter;
import weka.classifiers.Classifier;
import weka.classifiers.meta.Bagging;

/**
 * This filter converts the incoming data into data appropriate for keyphrase
 * classification. It assumes that the dataset contains three string attributes.
 * The first attribute should contain the name of the file. The second attribute
 * should contain the text of a document from that file. The second attribute
 * should contain the topics associated with that document (if present).
 * <br>
 * The filter converts every instance (i.e. document) into a set of instances,
 * one for each candidate topic identified in the document. The string attribute
 * representing the document is replaced by some numeric features, the estimated
 * probability of each candidate being a topic, and the rank of this candidate
 * in the document according to the probability. Each new instance also has a
 * class value associated with it. The class is "true" if the topic has been
 * assigned manually to this document, and "false" otherwise. It is also
 * possible to use numeric attributes, if more then one manually selected topic
 * sets per document are available. If the input document doesn't come with
 * author-assigned topics, the class values for that document will be missing.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz), Olena Medelyan (olena@cs.waikato.ac.nz)
 * @version 2.0
 */
public class MauiFilter extends Filter {

    private static final Logger log = LoggerFactory.getLogger(MauiFilter.class);

    private static final long serialVersionUID = 1L;

    /**
     * Index of attribute containing the name of the file
     */
    private int fileNameAtt = 0;

    /**
     * Index of attribute containing the documents
     */
    private int documentAtt = 1;

    /**
     * Index of attribute containing the keyphrases
     */
    private int keyphrasesAtt = 2;

    /**
     * Maximum length of phrases
     */
    private int maxPhraseLength = 5;

    /**
     * Minimum length of phrases
     */
    private int minPhraseLength = 1;

   /**
     * Number of human indexers (times a keyphrase appears in the keyphrase set)
     */
    private int numIndexers = 1;

    /**
     * Is class value nominal or numeric? *
     */
    private boolean nominalClassValue = true;

    /**
     * Flag for debugging mode
     */
    private boolean debugMode = false;

    /**
     * The minimum number of occurences of a phrase
     */
    private int minOccurFrequency = 1;

    /**
     * The number of features describing a phrase
     */
    private int numFeatures = 10;

    /**
     * Number of manually specified keyphrases
     */
    private int totalCorrect = 0;

    /**
     * Indices of attributes in classifierData
     */
    // General features
    private int tfIndex = 0; // term frequency
    private int idfIndex = 1; // inverse document frequency
    private int tfidfIndex = 2; // TFxIDF
    private int firstOccurIndex = 3; // position of the first occurrence
    private int lastOccurIndex = 4; // position of the last occurrence
    private int spreadOccurIndex = 5; // spread of occurrences
    private int domainKeyphIndex = 6; // domain keyphraseness
    private int lengthIndex = 7; // term length
    private int generalityIndex = 8; // generality

    // Thesaurus features
    private int nodeDegreeIndex = 9; // node degree

    /**
     * Use basic features TFxIDF & First Occurrence
     */
    boolean useBasicFeatures = true;

    /**
     * Use keyphraseness feature
     */
    boolean useKeyphrasenessFeature = true;

    /**
     * Use frequency features TF & IDF additionally
     */
    boolean useFrequencyFeatures = true;

    /**
     * Use occurrence position features LastOccurrence & Spread
     */
    boolean usePositionsFeatures = true;

    /**
     * Use thesaurus features Node degree
     */
    boolean useNodeDegreeFeature = true;

    /**
     * Use length feature
     */
    boolean useLengthFeature = true;

    /**
     * The punctuation filter used by this filter
     */
    private MauiPhraseFilter phraseFilter = null;

    /**
     * The numbers filter used by this filter
     */
    private NumbersFilter numbersFilter = null;

    /**
     * The actual classifier used to compute probabilities
     */
    private Classifier classifier = null;

    /**
     * The dictionary containing the document frequencies
     */
    public HashMap<String, Counter> globalDictionary = null;

    /**
     * The dictionary containing the keyphrases
     */
    private HashMap<String, Counter> keyphraseDictionary = null;

    transient HashMap<Instance, HashMap<String, Candidate>> allCandidates = null;

    /**
     * The number of documents in the global frequencies corpus
     */
    private int numDocs = 0;

    /**
     * Template for the classifier data
     */
    private Instances classifierData = null;

    /**
     * Default stemmer to be used
     */
    private Stemmer stemmer = new PorterStemmer();

    /**
     * List of stop words to be used
     */
    private Stopwords stopwords = new StopwordsEnglish();

    /**
     * Default language to be used
     */
    public String documentLanguage = "en";

    /**
     * Vocabulary object
     */
    transient Vocabulary vocabulary;

    /**
     * Vocabulary name
     */
    private String vocabularyName = "agrovoc";

    /**
     * Vocabulary format
     */
    public String vocabularyFormat = "skos";
  
    /**
     * Returns the total number of manually assigned topics in a given document
     *
     * @return number of manually assigned topics (int)
     */
    public int getTotalCorrect() {
        return totalCorrect;
    }

    public void setBasicFeatures(boolean useBasicFeatures) {
        this.useBasicFeatures = useBasicFeatures;
    }

    public void setClassifier(Classifier classifier) {
        this.classifier = classifier;
    }

    public void setKeyphrasenessFeature(boolean useKeyphrasenessFeature) {
        this.useKeyphrasenessFeature = useKeyphrasenessFeature;
    }

    public void setFrequencyFeatures(boolean useFrequencyFeatures) {
        this.useFrequencyFeatures = useFrequencyFeatures;
    }

    public void setPositionsFeatures(boolean usePositionsFeatures) {
        this.usePositionsFeatures = usePositionsFeatures;
    }

    public void setThesaurusFeatures(boolean useThesaurusFeatures) {
        this.useNodeDegreeFeature = useThesaurusFeatures;
    }

    public void setLengthFeature(boolean useLengthFeature) {
        this.useLengthFeature = useLengthFeature;
    }

    public void setStopwords(Stopwords stopwords) {
        this.stopwords = stopwords;
    }

    public void setStemmer(Stemmer stemmer) {
        this.stemmer = stemmer;
    }

    public void setNumIndexers(int numIndexers) {
        this.numIndexers = numIndexers;
    }

    public void setMinNumOccur(int minNumOccur) {
        this.minOccurFrequency = minNumOccur;
    }

    public void setMaxPhraseLength(int maxPhraseLength) {
        this.maxPhraseLength = maxPhraseLength;
    }

    public void setMinPhraseLength(int minPhraseLength) {
        this.minPhraseLength = minPhraseLength;
    }

    public void setDocumentLanguage(String documentLanguage) {
        this.documentLanguage = documentLanguage;
    }

    public void setDebug(boolean debugMode) {
        this.debugMode = debugMode;
    }

    public void setVocabularyName(String vocabularyName) {
        if (vocabularyName.equals("none")) {
            setThesaurusFeatures(false);
        }
        this.vocabularyName = vocabularyName;
    }

    public void setVocabularyFormat(String vocabularyFormat) {
        this.vocabularyFormat = vocabularyFormat;
    }

    /**
     * Returns the index of the normalized candidate form in the output ARFF
     * file.
     */
    public int getNormalizedFormIndex() {
        return documentAtt;
    }

    /**
     * Returns the index of the most frequent form for the candidate topic or
     * the original form of it in the vocabulary in the output ARFF file.
     */
    public int getOutputFormIndex() {
        return documentAtt;
    }

    /**
     * Returns the index of the candidates' probabilities in the output ARFF
     * file.
     */
    public int getProbabilityIndex() {
        // 2 indexes for phrase forms
        return documentAtt + numFeatures + 1;
    }

    /**
     * Returns the index of the candidates' ranks in the output ARFF file.
     */
    public int getRankIndex() {
    	// TODO: Fix this!
//    	return getProbabilityIndex() + 1;
    	return 13;
    }

    public int getDocumentAtt() {
        return documentAtt;
    }

    public void setDocumentAtt(int documentAtt) {
        this.documentAtt = documentAtt;
    }

    public int getKeyphrasesAtt() {
        return keyphrasesAtt;
    }

    public void setKeyphrasesAtt(int keyphrasesAtt) {
        this.keyphrasesAtt = keyphrasesAtt;
    }

    public void setVocabulary(Vocabulary vocabulary) {
        this.vocabulary = vocabulary;
    }

    /**
     * Returns a string describing this filter
     *
     * @return a description of the filter suitable for displaying in the
     * explorer/experimenter gui
     */
    public String globalInfo() {
        return "Converts incoming data into data appropriate for "
                + "keyphrase classification.";
    }

    /**
     * Sets the format of the input instances.
     *
     * @param instanceInfo an Instances object containing the input instance
     * structure (any instances contained in the object are ignored - only the
     * structure is required).
     * @return true if the outputFormat may be collected immediately
     */
    public boolean setInputFormat(Instances instanceInfo) throws MauiFilterException {

        if (instanceInfo.classIndex() >= 0) {
            throw new MauiFilterException("Don't know what do to if class index set!");
        }

        if (!instanceInfo.attribute(keyphrasesAtt).isString()
                || !instanceInfo.attribute(documentAtt).isString()) {
            throw new MauiFilterException("Keyphrase attribute and document attribute "
                    + "need to be string attributes.");
        }

        try {
	        phraseFilter = new MauiPhraseFilter();
	        int[] arr = new int[1];
	        arr[0] = documentAtt;
	        phraseFilter.setAttributeIndicesArray(arr);
	        phraseFilter.setInputFormat(instanceInfo);
        } catch (Exception e) {
        	throw new MauiFilterException("Exception loading MauiPhraseFilter");
        }

        try {
	        if (vocabularyName.equals("none")) {
	            numbersFilter = new NumbersFilter();
	            numbersFilter.setInputFormat(phraseFilter.getOutputFormat());
	            super.setInputFormat(numbersFilter.getOutputFormat());
	        } else {
	            super.setInputFormat(phraseFilter.getOutputFormat());
	        }
        } catch (Exception e) {
        	throw new MauiFilterException("Exception loading NumbersFilter");
        }

        return false;

    }

    /**
     * Returns the Capabilities of this filter.
     *
     * @return the capabilities of this object
     * @see Capabilities
     */
    public Capabilities getCapabilities() {
        Capabilities result = super.getCapabilities();

        // attributes
        result.enableAllAttributes();
        result.enable(Capability.MISSING_VALUES);

        // class
        result.enable(Capability.NOMINAL_CLASS);
        result.enable(Capability.NO_CLASS);
        result.enableAllClasses();

		// result.or(new LinearRegression().getCapabilities());
        return result;
    }

    /**
     * Input an instance for filtering. Ordinarily the instance is processed and
     * made available for output immediately. Some filters require all instances
     * be read before producing output.
     *
     * @param instance the input instance
     * @return true if the filtered instance may now be collected with output().
     * @exception Exception if the input instance was not of the correct format
     * or if there was a problem with the filtering.
     */
    @SuppressWarnings("unchecked")
    public boolean input(Instance instance) throws MauiFilterException {

        if (getInputFormat() == null) {
            throw new MauiFilterException("No input instance format defined");
        }
        if (m_NewBatch) {
            resetQueue();
            m_NewBatch = false;
        }

        if (debugMode) {
            log.info("-- Reading instance");
        }

        try {
	        phraseFilter.input(instance);
	        phraseFilter.batchFinished();
	        instance = phraseFilter.output();
        } catch (Exception e) {
        	throw new MauiFilterException("Error applying PhraseFilter ");
        }
        
        if (vocabularyName.equals("none")) {
        	try {
		        numbersFilter.input(instance);
		        numbersFilter.batchFinished();
		        instance = numbersFilter.output();
	        } catch (Exception e) {
	        	throw new MauiFilterException("Error applying NumbersFilter ");
	        }
	    }

        if (globalDictionary == null) {

            bufferInput(instance);
            return false;

        } else {

            FastVector vector = convertInstance(instance, false);
            Enumeration<Instance> en = vector.elements();
            while (en.hasMoreElements()) {
                Instance inst = en.nextElement();
                push(inst);
            }
            return true;
        }

    }

    /**
     * Signify that this batch of input to the filter is finished. If the filter
     * requires all instances prior to filtering, output() may now be called to
     * retrieve the filtered instances.
     *
     * @return true if there are instances pending output
     * @exception Exception if no input structure has been defined
     */
    public boolean batchFinished() throws MauiFilterException {

        if (getInputFormat() == null) {
            throw new MauiFilterException("No input instance format defined");
        }

        if (globalDictionary == null) {
            selectCandidates();
            buildGlobalDictionaries();
            buildClassifier();
            convertPendingInstances();
        }
        flushInput();
        m_NewBatch = true;
        return (numPendingOutput() != 0);
    }

    private void selectCandidates() {

        if (debugMode) {
            log.info("--- Computing candidates...");
        }

        allCandidates = new HashMap<Instance, HashMap<String, Candidate>>();

        // Convert pending input instances into data for classifier
        int totalDocuments = getInputFormat().numInstances();
        for (int i = 0; i < totalDocuments; i++) {

            Instance current = getInputFormat().instance(i);

            String fileName = current.stringValue(fileNameAtt);
            int j = i + 1;
            if (debugMode) {
                log.info("---- Processing document " + fileName
                        + ", " + j + " out of " + totalDocuments + "...");
            }

            // Get the phrases for the document
            String documentText = current.stringValue(documentAtt);

            HashMap<String, Candidate> candidateList = getCandidates(documentText);

            if (debugMode) {
                log.info("---- " + candidateList.size() + " candidates");
            }
            allCandidates.put(current, candidateList);

        }

    }

    /**
     * Builds the global dictionaries.
     */
    public void buildGlobalDictionaries() {
        if (debugMode) {
            log.info("--- Building global frequency dictionary");
        }

		// Build a dictionary of candidates with associated
        // document frequencies
        globalDictionary = new HashMap<String, Counter>();
        for (HashMap<String, Candidate> candidates : allCandidates.values()) {
            for (String candidateName : candidates.keySet()) {
                Counter counter = globalDictionary.get(candidateName);
                if (counter == null) {
                    globalDictionary.put(candidateName, new Counter());
                } else {
                    counter.increment();
                }
            }
        }

        if (debugMode) {
            log.info("--- Building keyphraseness dictionary");
        }
		// Build a dictionary of candidates that occur as keyphrases
        // with associated keyphrase frequencies
        keyphraseDictionary = new HashMap<String, Counter>();
        for (int i = 0; i < getInputFormat().numInstances(); i++) {
            String str = getInputFormat().instance(i)
                    .stringValue(keyphrasesAtt);
            HashMap<String, Counter> hash = getGivenKeyphrases(str);
            if (hash != null) {
                for (String term : hash.keySet()) {

                    Counter documentCount = hash.get(term);
                    Counter counter = keyphraseDictionary.get(term);
                    if (counter == null) {
                        keyphraseDictionary.put(term, new Counter(documentCount
                                .value()));
                    } else {
                        counter.increment(documentCount.value());
                    }
                }
            }
        }

        if (debugMode) {
            log.info("--- Statistics about global dictionaries: ");
            log.info("\t" + globalDictionary.size()
                    + " terms in the global dictionary");
            log.info("\t" + keyphraseDictionary.size()
                    + " terms in the keyphrase dictionary");
        }

        // Set the number of documents in the global corpus
        numDocs = getInputFormat().numInstances();
    }

    /**
     * Builds the classifier.
     * @throws MauiFilterException 
     */
    private void buildClassifier() throws MauiFilterException {

        // Generate input format for classifier
        FastVector atts = new FastVector();
        for (int i = 0; i < getInputFormat().numAttributes(); i++) {
            if (i == documentAtt) {
                atts.addElement(new Attribute("Term_frequency")); // 2
                atts.addElement(new Attribute("IDF")); //
                atts.addElement(new Attribute("TFxIDF")); //
                atts.addElement(new Attribute("First_occurrence")); //
                atts.addElement(new Attribute("Last_occurrence")); //
                atts.addElement(new Attribute("Spread")); //
                atts.addElement(new Attribute("Domain_keyphraseness")); //
                atts.addElement(new Attribute("Length")); //
                atts.addElement(new Attribute("Generality")); //
                atts.addElement(new Attribute("Node_degree")); //
              

            } else if (i == keyphrasesAtt) {
                if (nominalClassValue) {
                    FastVector vals = new FastVector(2);
                    vals.addElement("False");
                    vals.addElement("True");
                    atts.addElement(new Attribute("Keyphrase?", vals));
                } else {
                    atts.addElement(new Attribute("Keyphrase?"));
                }
            }
        }

        classifierData = new Instances("ClassifierData", atts, 0);

        classifierData.setClassIndex(numFeatures);

        if (debugMode) {
            log.info("--- Converting instances for classifier");
        }
        int totalDocuments = getInputFormat().numInstances();
        // Convert pending input instances into data for classifier
        for (int i = 0; i < totalDocuments; i++) {
            Instance current = getInputFormat().instance(i);

            // Get the key phrases for the document
            String keyphrases = current.stringValue(keyphrasesAtt);
            HashMap<String, Counter> hashKeyphrases = getGivenKeyphrases(keyphrases);

            // Get the phrases for the document
            HashMap<String, Candidate> candidateList = allCandidates
                    .get(current);

			// Compute the feature values for each phrase and
            // add the instance to the data for the classifier
            int countPos = 0;
            int countNeg = 0;

            if (debugMode) {
                log.info("--- Computing features for document " + i + " out of " + totalDocuments + "...");
            }

            for (Candidate candidate : candidateList.values()) {

                // ignore all candidates that appear less than a threshold
                if (candidate.getFrequency() < minOccurFrequency) {
                    continue;
                }
                
                // compute feature values
                double[] vals = computeFeatureValues(candidate, true,
                        hashKeyphrases, candidateList);

                if (vals[vals.length - 1] == 0) {
                    countNeg++;
                } else {
                    countPos++;
                }
                Instance inst = new Instance(current.weight(), vals);
                // log.info(candidate + "\t" + inst);
                classifierData.add(inst);

            }
            log.debug(countPos + " positive; " + countNeg + " negative instances");
        }

        log.debug("--- Building classifier");
     
        if (classifier == null) {
            // Build classifier
            if (nominalClassValue) {

//			FilteredClassifier fclass = new FilteredClassifier();
//			fclass.setClassifier(new NaiveBayesSimple());
//			fclass.setFilter(new Discretize());
//			classifier = fclass;
                classifier = new Bagging(); // try also //
                try {
                	classifier.setOptions(Utils.splitOptions("-P 10 -S 1 -I 10 -W weka.classifiers.trees.J48 -- -U -M 2"));
                } catch (Exception e) {
                	log.warn("Exception while loading classifier's options " + e.getMessage());
                }

            } else {

                classifier = new Bagging();
			// try also
                // classifier.setOptions(Utils.splitOptions("-P 10 -S 1 -I 10 -W
                // weka.classifiers.trees.J48 -- -U -M 2")) ;
                try {
                	String optionsString = "-P 100 -S 1 -I 10 -W weka.classifiers.trees.M5P -- -U -M 7.0";
                    String[] options = Utils.splitOptions(optionsString);
                    classifier.setOptions(options);
                } catch (Exception e) {
                	log.warn("Exception while loading classifier's options " + e.getMessage());
                }
                
            }
        }
        try {
        	classifier.buildClassifier(classifierData);
        } catch (Exception e) {
        	throw new MauiFilterException("Exception while building classifier " + e.getMessage());
        }
        
        if (debugMode) {
            log.info(classifier.toString());
        }

        // Save space
        classifierData = new Instances(classifierData, 0);
    }

    /**
     * Conmputes the feature values for a given phrase.
     */
    private double[] computeFeatureValues(Candidate candidate,
            boolean training, HashMap<String, Counter> hashKeyphrases,
            HashMap<String, Candidate> candidates) {

           // Compute feature values
        double[] newInst = new double[numFeatures + 1];

        String id = candidate.getName();
        String name = candidate.getName();
        String original = candidate.getBestFullForm();
        String title = candidate.getTitle();

        // Compute TFxIDF
        Counter counterGlobal = (Counter) globalDictionary.get(name);
        double globalVal = 0;
        if (counterGlobal != null) {
            globalVal = counterGlobal.value();
            if (training) {
                globalVal = globalVal - 1;
            }
        }
        double tf = candidate.getTermFrequency();
        double idf = -Math.log((globalVal + 1) / ((double) numDocs + 1));

        if (useBasicFeatures) {
            newInst[tfidfIndex] = tf * idf;
            newInst[firstOccurIndex] = candidate.getFirstOccurrence();
        }

        if (useFrequencyFeatures) {
            newInst[tfIndex] = tf;
            newInst[idfIndex] = idf;
        }

        if (usePositionsFeatures) {
            newInst[lastOccurIndex] = candidate.getLastOccurrence();
            newInst[spreadOccurIndex] = candidate.getSpread();
        }

        if (useKeyphrasenessFeature) {
            if (!vocabularyName.equals("none")) {
                name = title;
            }
            Counter domainKeyphr = keyphraseDictionary.get(name);

            if ((training) && (hashKeyphrases != null)
                    && (hashKeyphrases.containsKey(name))) {
                newInst[domainKeyphIndex] = domainKeyphr.value() - 1;
            } else {
                if (domainKeyphr != null) {
                    newInst[domainKeyphIndex] = domainKeyphr.value();
                } else {
                    newInst[domainKeyphIndex] = 0;
                }
            }
        }

        if (useLengthFeature) {

            if (original == null) {
                log.warn("Warning! Problem with candidate " + name);
                newInst[lengthIndex] = 1.0;
            } else {
                // String[] words = candidate.getTitle().split(" ");
                String[] words = original.split(" ");
                newInst[lengthIndex] = (double) words.length;
            }
        }

        if (useNodeDegreeFeature) {
            int nodeDegree = 0;
            if (vocabulary != null) {
                ArrayList<String> relatedTerms = vocabulary.getRelated(id);
                if (relatedTerms != null) {
                    for (String relatedTerm : relatedTerms) {
                    	if (candidates.get(relatedTerm) != null) {
                            nodeDegree++;
                        }
                    }
                }
            }
            if (nodeDegree != 0) {
                //	log.info(candidate + " has node degree " + nodeDegree);
            }
            newInst[nodeDegreeIndex] = (double) nodeDegree;
        }
        
        // how many candidates are from the same area hub?
//        int sameArea = 0;
//        String area = id.substring(id.lastIndexOf("/") + 1).split("\\.")[0];
//        for (String cId : candidates.keySet()) {
//        	String cArea = cId.substring(id.lastIndexOf("/") + 1).split("\\.")[0];
//        	if (cArea.equals(area)) {
//        		sameArea += 1;
//        	}
//        }
////        int generality = idValue.length();
//        newInst[generalityIndex] = sameArea;
//        

        // Compute class value
        String checkManual = name;
        if (!vocabularyName.equals("none")) {
            checkManual = candidate.getTitle();
        }

        if (hashKeyphrases == null) { // No author-assigned keyphrases
            // newInst[numFeatures] = Instance.missingValue();
            newInst[numFeatures] = 0;
        } else if (!hashKeyphrases.containsKey(checkManual)) {
            newInst[numFeatures] = 0; // Not a keyphrase
        } else {
            if (nominalClassValue) {
                newInst[numFeatures] = 1; // Keyphrase
            } else {
                double c = (double) ((Counter) hashKeyphrases.get(checkManual))
                        .value()
                        / numIndexers;
                newInst[numFeatures] = c; // Keyphrase
            }
        }
        /*
         log.info(candidate.toString());
         log.info("\tTFxIDF " + newInst[tfidfIndex]);
         log.info("\ttotalWikipKeyphrIndex " + newInst[totalWikipKeyphrIndex]);
         log.info("\tfirstOccurIndex " + newInst[firstOccurIndex]);
         log.info("\tsemRelIndex " + newInst[semRelIndex]);
         */
        return newInst;
    }

    /**
     * Sets output format and converts pending input instances.
     */
    @SuppressWarnings("unchecked")
	private void convertPendingInstances() {

        if (debugMode) {
            log.info("--- Converting pending instances");
        }

        // Create output format for filter
        FastVector atts = new FastVector();
        for (int i = 1; i < getInputFormat().numAttributes(); i++) {
            if (i == documentAtt) {
                atts.addElement(new Attribute("Candidate_name",
                        (FastVector) null)); // 0
                atts.addElement(new Attribute("Candidate_original",
                        (FastVector) null)); // 1
                atts.addElement(new Attribute("Term_frequency")); // 2
                atts.addElement(new Attribute("IDF")); // 3
                atts.addElement(new Attribute("TFxIDF")); // 4
                atts.addElement(new Attribute("First_occurrence")); // 5
                atts.addElement(new Attribute("Last_occurrence")); // 6
                atts.addElement(new Attribute("Spread")); // 7
                atts.addElement(new Attribute("Domain_keyphraseness")); // 8
                atts.addElement(new Attribute("Length")); // 9
                atts.addElement(new Attribute("Generality")); // 10
                atts.addElement(new Attribute("Node_degree")); // 11

                atts.addElement(new Attribute("Probability")); // 16
                atts.addElement(new Attribute("Rank")); // 17

            } else if (i == keyphrasesAtt) {
                if (nominalClassValue) {
                    FastVector vals = new FastVector(2);
                    vals.addElement("False");
                    vals.addElement("True");
                    atts.addElement(new Attribute("Keyphrase?", vals));
                } else {
                    atts.addElement(new Attribute("Keyphrase?"));
                }
            } else {
                atts.addElement(getInputFormat().attribute(i));
            }
        }

        Instances outFormat = new Instances("mauidata", atts, 0);
        setOutputFormat(outFormat);

        // Convert pending input instances into output data
        for (int i = 0; i < getInputFormat().numInstances(); i++) {
            Instance current = getInputFormat().instance(i);
            FastVector vector = convertInstance(current, true);
            Enumeration<Instance> en = vector.elements();
            while (en.hasMoreElements()) {
                Instance inst = (Instance) en.nextElement();
                push(inst);
            }
        }
    }

    /**
     * Converts an instance.
     */
    private FastVector convertInstance(Instance instance, boolean training) {

        FastVector vector = new FastVector();

        String fileName = instance.stringValue(fileNameAtt);

        if (debugMode) {
            log.info("-- Converting instance for document " + fileName);
        }

        // Get the key phrases for the document
        HashMap<String, Counter> hashKeyphrases = null;

        if (!instance.isMissing(keyphrasesAtt)) {
            String keyphrases = instance.stringValue(keyphrasesAtt);
            hashKeyphrases = getGivenKeyphrases(keyphrases);
        }

        // Get the document text
        String documentText = instance.stringValue(documentAtt);

        // Compute the candidate topics
        HashMap<String, Candidate> candidateList;
        if (allCandidates != null && allCandidates.containsKey(instance)) {
            candidateList = allCandidates.get(instance);
        } else {
            candidateList = getCandidates(documentText);
        }
        if (debugMode) {
            log.info(candidateList.size() + " candidates ");
        }

        // Set indices for key attributes
        int tfidfAttIndex = documentAtt + 2;
        int distAttIndex = documentAtt + 3;
        int probsAttIndex = documentAtt + numFeatures;

        int countPos = 0;
        int countNeg = 0;

        // Go through the phrases and convert them into instances
        for (Candidate candidate : candidateList.values()) {

            if (candidate.getFrequency() < minOccurFrequency) {
                continue;
            }

            String name = candidate.getName();
            String orig = candidate.getBestFullForm();
            if (!vocabularyName.equals("none")) {
                orig = candidate.getTitle();
            }
            
            double[] vals = computeFeatureValues(candidate, training,
                    hashKeyphrases, candidateList);

            Instance inst = new Instance(instance.weight(), vals);

            inst.setDataset(classifierData);
            
            double[] probs = null;
            try {
            	// Get probability of a phrase being key phrase
            	probs = classifier.distributionForInstance(inst);
            } catch (Exception e) {
            	log.error("Exception while getting probability for candidate " + candidate.getName());
            	continue;
            }
            
            double prob = probs[0];
            if (nominalClassValue) {
                prob = probs[1];
            }

            // Compute attribute values for final instance
            double[] newInst = new double[instance.numAttributes() + numFeatures + 2];

            int pos = 0;
            for (int i = 1; i < instance.numAttributes(); i++) {

                if (i == documentAtt) {

                    // output of values for a given phrase:
                    // Add phrase
                    int index = outputFormatPeek().attribute(pos)
                            .addStringValue(name);
                    newInst[pos++] = index;

                    // Add original version
                    if (orig != null) {
                        index = outputFormatPeek().attribute(pos).addStringValue(orig);
                    } else {
                        index = outputFormatPeek().attribute(pos).addStringValue(name);
                    }

                    newInst[pos++] = index;

                    // Add features
                    newInst[pos++] = inst.value(tfIndex);
                    newInst[pos++] = inst.value(idfIndex);
                    newInst[pos++] = inst.value(tfidfIndex);
                    newInst[pos++] = inst.value(firstOccurIndex);
                    newInst[pos++] = inst.value(lastOccurIndex);
                    newInst[pos++] = inst.value(spreadOccurIndex);
                    newInst[pos++] = inst.value(domainKeyphIndex);
                    newInst[pos++] = inst.value(lengthIndex);
                    newInst[pos++] = inst.value(generalityIndex);
                    newInst[pos++] = inst.value(nodeDegreeIndex);
//                    newInst[pos++] = inst.value(semRelIndex);
//                    newInst[pos++] = inst.value(wikipKeyphrIndex);
//                    newInst[pos++] = inst.value(invWikipFreqIndex);
//                    newInst[pos++] = inst.value(totalWikipKeyphrIndex);

                    // Add probability
                    probsAttIndex = pos;
                    newInst[pos++] = prob;

                    // Set rank to missing (computed below)
                    newInst[pos++] = Instance.missingValue();

                } else if (i == keyphrasesAtt) {
                    newInst[pos++] = inst.classValue();
                } else {
                    newInst[pos++] = instance.value(i);
                }
            }

            Instance ins = new Instance(instance.weight(), newInst);
            ins.setDataset(outputFormatPeek());
            vector.addElement(ins);

            if (inst.classValue() == 0) {
                countNeg++;
            } else {
                countPos++;
            }
           
        }
        if (debugMode) {
            log.info(countPos + " positive; " + countNeg + " negative instances");
        }

        // Sort phrases according to their distance (stable sort)
        double[] vals = new double[vector.size()];
        for (int i = 0; i < vals.length; i++) {
            vals[i] = ((Instance) vector.elementAt(i)).value(distAttIndex);
        }
        FastVector newVector = new FastVector(vector.size());
        int[] sortedIndices = Utils.stableSort(vals);
        for (int i = 0; i < vals.length; i++) {
            newVector.addElement(vector.elementAt(sortedIndices[i]));
        }
        vector = newVector;

        // Sort phrases according to their tfxidf value (stable sort)
        for (int i = 0; i < vals.length; i++) {
            vals[i] = -((Instance) vector.elementAt(i)).value(tfidfAttIndex);
        }
        newVector = new FastVector(vector.size());
        sortedIndices = Utils.stableSort(vals);
        for (int i = 0; i < vals.length; i++) {
            newVector.addElement(vector.elementAt(sortedIndices[i]));
        }
        vector = newVector;

        // Sort phrases according to their probability (stable sort)
        for (int i = 0; i < vals.length; i++) {
            vals[i] = 1 - ((Instance) vector.elementAt(i)).value(probsAttIndex);
        }
        newVector = new FastVector(vector.size());
        sortedIndices = Utils.stableSort(vals);
        for (int i = 0; i < vals.length; i++) {
            newVector.addElement(vector.elementAt(sortedIndices[i]));
        }
        vector = newVector;

		// Compute rank of phrases. Check for subphrases that are ranked
        // lower than superphrases and assign probability -1 and set the
        // rank to Integer.MAX_VALUE
        int rank = 1;
        for (int i = 0; i < vals.length; i++) {
            Instance currentInstance = (Instance) vector.elementAt(i);

            // log.info(vals[i] + "\t" + currentInstance);
            
            // Short cut: if phrase very unlikely make rank very low and
            // continue
            if (Utils.grOrEq(vals[i], 1.0)) {
                currentInstance.setValue(probsAttIndex + 1, Integer.MAX_VALUE);
                continue;
            }

            // Otherwise look for super phrase starting with first phrase
            // in list that has same probability, TFxIDF value, and distance as
            // current phrase. We do this to catch all superphrases
            // that have same probability, TFxIDF value and distance as current
            // phrase.
            int startInd = i;
            while (startInd < vals.length) {
                Instance inst = (Instance) vector.elementAt(startInd);
                if ((inst.value(tfidfAttIndex) != currentInstance
                        .value(tfidfAttIndex))
                        || (inst.value(probsAttIndex) != currentInstance
                        .value(probsAttIndex))
                        || (inst.value(distAttIndex) != currentInstance
                        .value(distAttIndex))) {
                    break;
                }
                startInd++;
            }
            currentInstance.setValue(probsAttIndex + 1, rank++);

        }

        return vector;
    }

    /**
     * Expects an empty hashtable. Fills the hashtable with the candidate
     * keyphrases Stores the position, the number of occurences, and the most
     * commonly occurring orgininal version of each candidate in the Candidate
     * object.
     *
     * Returns the total number of words in the document.
     *
     * @throws Exception
     */
    public HashMap<String, Candidate> getCandidates(String text) {

        if (debugMode) {
            log.info("---- Extracting candidates... ");
        }

        HashMap<String, Candidate> candidatesTable = new HashMap<String, Candidate>();

        String[] buffer = new String[maxPhraseLength];

	// Extracting strings of a predefined length from "str":
        //text
        
        // log.info(text);
        StringTokenizer tok = new StringTokenizer(text, "\n");
        int pos = 0;
        int totalFrequency = 0;
        int firstWord = 0;
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();

            int numSeen = 0;
            StringTokenizer wordTok = new StringTokenizer(token, " ");

            while (wordTok.hasMoreTokens()) {

                pos++;

                String word = wordTok.nextToken();

                // Store word in buffer
                for (int i = 0; i < maxPhraseLength - 1; i++) {
                    buffer[i] = buffer[i + 1];
                }
                buffer[maxPhraseLength - 1] = word;

                // How many are buffered?
                numSeen++;
                if (numSeen > maxPhraseLength) {
                    numSeen = maxPhraseLength;
                }

		// Don't consider phrases that end with a stop word
                if (stopwords.isStopword(buffer[maxPhraseLength - 1])) {
                    continue;
                }
			
                // Loop through buffer and add phrases to hashtable
                StringBuffer phraseBuffer = new StringBuffer();
                for (int i = 1; i <= numSeen; i++) {
                    if (i > 1) {
                        phraseBuffer.insert(0, ' ');
                    }
                    phraseBuffer.insert(0, buffer[maxPhraseLength - i]);

                    // Don't consider phrases that begin with a stop word
                    // In free indexing only
                    if ((i > 1)
                            && (stopwords.isStopword(buffer[maxPhraseLength - i]))) {
                        continue;
                    }
                 
                    // Only consider phrases with minimum length
                    if (i >= minPhraseLength) {

						// each detected candidate phase in its original
                        // spelling form
                        String form = phraseBuffer.toString();

			// list of candidates extracted for a given original
                        // string
                        // in case of term assignment more than one possible!
                        ArrayList<String> candidateNames = new ArrayList<String>();

                        if (vocabularyName.equals("none")) {

							// if it is free keyphrase indexing,
                            // get the pseudo phrase of the original spelling
                            String phrase = pseudoPhrase(form);
                            if (phrase != null) {
                                candidateNames.add(phrase);
                            }
                            totalFrequency++;
                            //	log.info(form + ", ");

                        } else {
                            //	log.info("...retrieving senses for form " + form);
                            // if a controlled vocabulary is used
                            // retrieve its senses
                            for (String sense : vocabulary.getSenses(form)) {
				// mapping
                            	log.debug(form + " => " + vocabulary.getTerm(sense)+" " + this.minOccurFrequency);
                                candidateNames.add(sense);
                            }

                        }
			
                        // log.info("...conflating candidates");

                        // ignore all those phrases
                        // that have empty pseudo phrases or
                        // that map to nothing in the vocabulary
                        if (!candidateNames.isEmpty()) {

                            for (String name : candidateNames) {

                                Candidate candidate = candidatesTable.get(name);

                                if (candidate == null) {
                                    // this is the first occurrence of this
                                    // candidate
                                    // create a candidate object

                                 
                                        firstWord = pos - i;
                                        candidate = new Candidate(name, form,
                                                firstWord);
                                        totalFrequency++;
					// if it's a controlled vocabulary, this
                                        // allows
                                        // retrieve how this topic is refered to
                                        // by a descriptor
                                        if (!vocabularyName.equals("none")) {
                                            candidate.setTitle(vocabulary.getTerm(name));
                                        }

                                  

                                } else {

                                    // candidate has been observed before
                                    // update its values
                                    // log.info(form);
                                    firstWord = pos - i;
                                    candidate.recordOccurrence(form, firstWord);
                                    totalFrequency++;

                                }
                                if (candidate != null) {
                                    candidatesTable.put(name, candidate);
                                }
                            }
                        }
                    }
                }
            }
        }

        Set<String> keys = new HashSet<String>();
        keys.addAll(candidatesTable.keySet());
        for (String key : keys) {
            Candidate candidate = candidatesTable.get(key);
            if (candidate.getFrequency() < minOccurFrequency) {
                candidatesTable.remove(key);
            } else {
                candidate.normalize(totalFrequency, pos);
            }
        }

        return candidatesTable;
    }


    /**
     * Collects all the topics assigned manually and puts them into the
     * hashtable. Also stores the counts for each topic, if they are available
     */
    private HashMap<String, Counter> getGivenKeyphrases(String keyphraseListings) {

        HashMap<String, Counter> keyphrases = new HashMap<String, Counter>();

        String keyphrase, listing;
        int tab, frequency;

        StringTokenizer tok = new StringTokenizer(keyphraseListings, "\n");
        while (tok.hasMoreTokens()) {
            listing = tok.nextToken();
            listing = listing.trim();

			// if the keyphrase file contains frequencies associated with each
            // term,
            // parse these separately
            tab = listing.indexOf("\t");
            if (tab != -1) {
                keyphrase = listing.substring(0, tab);
                frequency = Integer.parseInt(listing.substring(tab + 1));
            } else {
                keyphrase = listing;
                frequency = 1;
            }

            if (vocabularyName.equals("none")) {

                keyphrase = pseudoPhrase(keyphrase);
                Counter counter = keyphrases.get(keyphrase);
                if (counter == null) {
                    keyphrases.put(keyphrase, new Counter(frequency));
                } else {
                    counter.increment(frequency);
                }
              } else {
                int colonIndex = keyphrase.indexOf(":");
                if (colonIndex != -1) {
                    keyphrase = keyphrase.substring(colonIndex + 2);
                }
                for (String id : vocabulary.getSenses(keyphrase)) {
                    keyphrase = vocabulary.getTerm(id);
                    Counter counter = keyphrases.get(keyphrase);
                    if (counter == null) {
                        keyphrases.put(keyphrase, new Counter(frequency));
                    } else {
                        counter.increment(frequency);
                    }
                }
            }
        }
        if (keyphrases.isEmpty()) {
            log.warn("Warning! This documents does not contain valid keyphrases");
            log.warn(keyphraseListings);
            // log.warn(keyphraseListings.toString());
            return null;
        } else {
        	log.debug("Found " + keyphrases.size());
            totalCorrect = keyphrases.size();
            return keyphrases;
        }
    }

    /**
     * @return Generates a normalized preudo phrase from a string. A pseudo phrase is a
     * version of a phrase that only contains non-stopwords, which are stemmed
     * and sorted into alphabetical order.
     */
    public String pseudoPhrase(String str) {

        String result = "";

        str = str.toLowerCase();

        // sort words alphabetically
        String[] words = str.split(" ");
        Arrays.sort(words);

        for (String word : words) {

            // remove all stopwords
            if (!stopwords.isStopword(word)) {

                // remove all apostrophes
                int apostr = word.indexOf('\'');
                if (apostr != -1) {
                    word = word.substring(0, apostr);
                }

                // ste	mm the remaining words
                word = stemmer.stem(word);

                result += word + " ";
            }
        }
        result = result.trim();
        if (!result.equals("")) {
            return result;
        }
        return null;
    }
    
    public class MauiFilterException extends Exception {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public MauiFilterException(String message) {
            super(message);
        }
    }

    /**
     * Main method.
     */
    public static void main(String[] argv) {        
        log.error("Use MauiModelBuilder or MauiTopicExtractor!");
    }
}
