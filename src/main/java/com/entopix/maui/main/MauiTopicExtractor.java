package com.entopix.maui.main;

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
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.filters.MauiFilter.MauiFilterException;
import com.entopix.maui.stemmers.PorterStemmer;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsEnglish;
import com.entopix.maui.util.DataLoader;
import com.entopix.maui.util.Evaluator;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.util.MauiTopics;
import com.entopix.maui.util.Topic;
import com.entopix.maui.vocab.Vocabulary;
import com.entopix.maui.vocab.VocabularyStoreFactory;
import com.entopix.maui.vocab.VocabularyStore_HT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Extracts topics from the documents in a given directory. Assumes that the
 * file names for the documents end with ".txt". Puts extracted topics into
 * corresponding files ending with ".maui".
 * Optionally an encoding for the documents/keyphrases can be defined (e.g. for
 * Chinese text). Corresponding ".key" files (if such exists) are used for evaluation.
 *
 * Valid options are:
 * <p>
 *
 * -l "directory name"<br>
 * Specifies name of directory.<p>
 *
 * -m "model name"<br>
 * Specifies name of model.<p>
 *
 * -v "vocabulary name"<br>
 * Specifies name of vocabulary.<p>
 *
 * -f "vocabulary format"<br>
 * Specifies format of vocabulary (text or skos)
 * .<p>
 *
 * -i "document language" <br>
 * Specifies document language (en, es, de, fr)
 * .<p>
 *
 * -e "encoding"<br>
 * Specifies encoding.<p>
 *
 * -n <br>
 * Specifies number of phrases to be output (default: 5)
 * .<p>
 *
 * -t "name of class implementing stemmer"<br>
 * Sets stemmer to use (default: SremovalStemmer).
 * <p>
 *
 * -s "name of class implementing stopwords"<br>
 * Sets stemmer to use (default: StopwordsEnglish).
 * <p>
 *
 * -d<br>
 * Turns debugging mode on.<p>
 *
 * -g<br>
 * Build global dictionaries from the test set.<p>
 *
 * -p<br>
 * Prints plain-text graph description of the topics for visual representation
 * of the results.<p>
 *
 * -a<br>
 * Also write stemmed phrase and score into ".maui" file.<p>
 *
 * -c<br>
 * Cut off threshold for the topic probability.<p>
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz), zelandiya (medelyan@gmail.com)
 * 
 * @version 1.3
 */
public class MauiTopicExtractor implements OptionHandler {

	private static final Logger log = LoggerFactory.getLogger(MauiTopicExtractor.class);

	/**
	 * Name of directory
	 */
	public String inputDirectoryName = null;

	/**
	 * Name of model
	 */
	public String modelName = null;

	/**
	 * Vocabulary name
	 */
	public String vocabularyName = "none";

	/**
	 * Format of the vocabulary
	 */
	public String vocabularyFormat = null;

	/**
	 * Document language
	 */
	public String documentLanguage = "en";

	/**
	 * Document encoding
	 */
	public String documentEncoding = "default";

	/**
	 * Debugging mode?
	 */
	public boolean debugMode = false;
	
	/**
	 * Cut off threshold for the topic probability.
	 * Minimum probability of a topic as returned by the classifier
	 * 
	 */
	public double cutOffTopicProbability = 0.0;

	/**
	 * Maui filter object
	 */
	private MauiFilter mauiFilter = null;


	/**
	 * The number of phrases to extract.
	 */
	int topicsPerDocument = 10;

	/**
	 * Directory where vocabularies are stored *
	 */
	public String vocabularyDirectory = "src/test/resources/data/vocabularies";

	/**
	 * Stemmer to be used
	 */
	public Stemmer stemmer = new PorterStemmer();

	/**
	 * List of stopwords to be used
	 */
	public Stopwords stopwords = new StopwordsEnglish();

	/**
	 * Vocabulary object (if required)
	 */
	private Vocabulary vocabulary = null;

	/**
	 * Also write stemmed phrase and score into .maui file.
	 */
	boolean additionalInfo = false;

	/**
	 * Build global dictionaries from the test set.
	 */
	boolean buildGlobalDictionary = false;

	public boolean getDebug() {
		return debugMode;
	}

	/**
	 * Parses a given list of options controlling the behaviour of this object.
	 * Valid options are:
	 * <p>
	 *
	 * -l "directory name"<br>
	 * Specifies name of directory.<p>
	 *
	 * -m "model name"<br>
	 * Specifies name of model.<p>
	 *
	 * -v "vocabulary name"<br>
	 * Specifies vocabulary name.<p>
	 *
	 * -f "vocabulary format"<br>
	 * Specifies vocabulary format.<p>
	 *
	 * -i "document language" <br>
	 * Specifies document language.<p>
	 *
	 * -e "encoding"<br>
	 * Specifies encoding.<p>
	 *
	 * -n<br>
	 * Specifies number of phrases to be output (default: 5)
	 * .<p>
	 *
	 * -d<br>
	 * Turns debugging mode on.<p>
	 *
	 * -b<br>
	 * Builds global dictionaries for computing TFxIDF from the test
	 * collection.<p>
	 *
	 * -a<br>
	 * Also write stemmed phrase and score into ".maui" file.<p>
	 *
	 * -c<br>
	 * Cut off threshold for the topic probability.<p>
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	public void setOptions(String[] options) throws Exception {

		String dirName = Utils.getOption('l', options);
		if (dirName.length() > 0) {
			inputDirectoryName = dirName;
		} else {
			inputDirectoryName = null;
			throw new Exception("Name of directory required argument.");
		}

		String modelName = Utils.getOption('m', options);
		if (modelName.length() > 0) {
			this.modelName = modelName;
		} else {
			this.modelName = null;
			throw new Exception("Name of model required argument.");
		}

		String vocabularyName = Utils.getOption('v', options);
		if (vocabularyName.length() > 0) {
			this.vocabularyName = vocabularyName;
		}

		String vocabularyFormat = Utils.getOption('f', options);

		if (!vocabularyName.equals("none")) {
			if (vocabularyFormat.length() > 0) {
				if (vocabularyFormat.equals("skos")
						|| vocabularyFormat.equals("text")) {
					this.vocabularyFormat = vocabularyFormat;
				} else {
					throw new Exception(
							"Unsupported format of vocabulary. It should be either \"skos\" or \"text\".");
				}
			} else {
				throw new Exception(
						"If a controlled vocabulary is used, format of vocabulary required argument (skos or text).");
			}
		}

		String encoding = Utils.getOption('e', options);
		if (encoding.length() > 0) {
			this.documentEncoding = encoding;
		}

		String documentLanguage = Utils.getOption('i', options);
		if (documentLanguage.length() > 0) {
			this.documentLanguage = documentLanguage;
		}

		String numPhrases = Utils.getOption('n', options);
		if (numPhrases.length() > 0) {
			this.topicsPerDocument = Integer.parseInt(numPhrases);
		}

		String stopwordsString = Utils.getOption('s', options);
		if (stopwordsString.length() > 0) {
			stopwordsString = "maui.stopwords.".concat(stopwordsString);
			this.stopwords = (Stopwords) Class.forName(stopwordsString)
					.newInstance();
		}

		String stemmerString = Utils.getOption('t', options);
		if (stemmerString.length() > 0) {
			stemmerString = "maui.stemmers.".concat(stemmerString);
			this.stemmer = (Stemmer) Class.forName(stemmerString).newInstance();
		}

		debugMode = Utils.getFlag('d', options);
		this.buildGlobalDictionary = Utils.getFlag('b', options);
		this.additionalInfo = Utils.getFlag('a', options);
		

		String cutOffProbability = Utils.getOption('c', options);
		if (cutOffProbability.length() > 0) {
			this.cutOffTopicProbability = Double.parseDouble(cutOffProbability);
		}
		
		Utils.checkForRemainingOptions(options);
	}

	/**
	 * Gets the current option settings.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	public String[] getOptions() {

		String[] options = new String[22];
		int current = 0;

		options[current++] = "-l";
		options[current++] = "" + (this.inputDirectoryName);
		options[current++] = "-m";
		options[current++] = "" + (this.modelName);
		options[current++] = "-v";
		options[current++] = "" + (this.vocabularyName);
		options[current++] = "-f";
		options[current++] = "" + (this.vocabularyFormat);
		options[current++] = "-e";
		options[current++] = "" + (this.documentEncoding);
		options[current++] = "-i";
		options[current++] = "" + (this.documentLanguage);
		options[current++] = "-n";
		options[current++] = "" + (this.topicsPerDocument);
		options[current++] = "-c";
		options[current++] = "" + (this.cutOffTopicProbability);
		options[current++] = "-t";
		options[current++] = "" + (stemmer.getClass().getName());
		options[current++] = "-s";
		options[current++] = "" + (stopwords.getClass().getName());

		if (getDebug()) {
			options[current++] = "-d";
		}


		if (this.buildGlobalDictionary) {
			options[current++] = "-b";
		}

		if (additionalInfo) {
			options[current++] = "-a";
		}

		while (current < options.length) {
			options[current++] = "";
		}
		return options;
	}
	
	 public void setVocabulary(Vocabulary vocabulary) {
	    	this.vocabulary = vocabulary;
	    }
	 
	 public void setTopicProbability(double prob) {
		 this.cutOffTopicProbability = prob;
	 }

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options
	 */
	public Enumeration<Option> listOptions() {

		Vector<Option> newVector = new Vector<Option>(15);

		newVector.addElement(new Option(
				"\tSpecifies name of directory.",
				"l", 1, "-l <directory name>"));
		newVector.addElement(new Option(
				"\tSpecifies name of model.",
				"m", 1, "-m <model name>"));
		newVector.addElement(new Option(
				"\tSpecifies vocabulary name.",
				"v", 1, "-v <vocabulary name>"));
		newVector.addElement(new Option(
				"\tSpecifies vocabulary format.",
				"f", 1, "-f <vocabulary format>"));
		newVector.addElement(new Option(
				"\tSpecifies encoding.",
				"e", 1, "-e <encoding>"));
		newVector.addElement(new Option(
				"\tSpecifies document language (en (default), es, de, fr).",
				"i", 1, "-i <document language>"));
		newVector.addElement(new Option(
				"\tSpecifies number of phrases to be output (default: 5).",
				"n", 1, "-n"));
		newVector.addElement(new Option(
				"\tSpecifies cut off probability for each topic (default: 0.0).",
				"c", 1, "-c"));
		newVector.addElement(new Option(
				"\tSet the stemmer to use (default: SremovalStemmer).",
				"t", 1, "-t <name of stemmer class>"));
		newVector.addElement(new Option(
				"\tSet the stopwords class to use (default: EnglishStopwords).",
				"s", 1, "-s <name of stopwords class>"));
		newVector.addElement(new Option(
				"\tTurns debugging mode on.",
				"d", 0, "-d"));
		newVector.addElement(new Option(
				"\tBuilds global dictionaries for computing TFIDF from the test collection.",
				"b", 0, "-b"));
		newVector.addElement(new Option(
				"\tPrints graph description into a \".gv\" file, in GraphViz format.",
				"p", 0, "-p"));
		newVector.addElement(new Option(
				"\tAlso write stemmed phrase and score into \".key\" file.",
				"a", 0, "-a"));

		return newVector.elements();
	}

	public void loadVocabulary() {
		if (vocabulary != null) {
			return;
		}

		try {

			if (debugMode) {
				log.info("--- Loading the vocabulary...");
			}
			vocabulary = new Vocabulary();
			vocabulary.setStemmer(stemmer);
			if (!vocabularyName.equals("lcsh")) {
				vocabulary.setStopwords(stopwords);
			}

			vocabulary.setDebug(debugMode);
			vocabulary.setLanguage(documentLanguage);
			vocabulary.initializeVocabulary(vocabularyName, vocabularyFormat, vocabularyDirectory, true);

		} catch (Exception e) {
			log.error("Failed to load thesaurus!", e);
		}

	}

	/**
	 * Loads the documents
	 * @return
	 * @throws Exception
	 */
	public List<MauiDocument> loadDocuments() throws MauiFilterException {
		return DataLoader.loadTestDocuments(inputDirectoryName);
	}

	/**
	 * Extracts topics from all documents
	 * @throws MauiFilterException 
	 */
	public List<MauiTopics> extractTopics(List<MauiDocument> documents) throws MauiFilterException {

		List<MauiTopics> allDocumentTopics = new ArrayList<MauiTopics>();
		
		// Weka data structures
		FastVector atts = new FastVector(3);
		atts.addElement(new Attribute("filename", (FastVector) null));
		atts.addElement(new Attribute("doc", (FastVector) null));
		atts.addElement(new Attribute("keyphrases", (FastVector) null));
		Instances data = new Instances("keyphrase_training_data", atts, 0);

		log.info("-- Extracting keyphrases... ");

		for (MauiDocument document : documents) {

			double[] newInst = new double[3];

			newInst[0] = (double) data.attribute(0).addStringValue(document.getFileName());

			// Adding the text of the document to the instance
			if (document.getTextContent().length() > 0) {
				newInst[1] = (double) data.attribute(1).addStringValue(document.getTextContent());
			} else {
				newInst[1] = Instance.missingValue();
			}

			if (document.getTopicsString().length() > 0) {
				newInst[2] = data.attribute(2).addStringValue(document.getTopicsString());
			} else {
				newInst[2] = Instance.missingValue();
			}

			data.add(new Instance(1.0, newInst));

			mauiFilter.input(data.instance(0));

			data = data.stringFreeStructure();
			if (debugMode) {
				log.info("-- Processing document: " + document.getFileName());
			}
			
			Instance[] topRankedInstances = new Instance[topicsPerDocument];
			Instance inst;

			MauiTopics documentTopics = new MauiTopics(document.getFilePath());
			
			documentTopics.setPossibleCorrect(document.getTopicsString().split("\n").length);
			
			int index = 0;
			double probability;
			Topic topic;
			String title, id;

			if (debugMode) {
				log.debug("-- Keyphrases and feature values:");
			}
			
			// Iterating over all extracted topic instances
			while ((inst = mauiFilter.output()) != null) {
				probability = inst.value(mauiFilter.getProbabilityIndex());
				if (index < topicsPerDocument) {
					if (probability > cutOffTopicProbability) {
						topRankedInstances[index] = inst;
						title = topRankedInstances[index].
								stringValue(mauiFilter.getOutputFormIndex());
						id = "1"; // topRankedInstances[index].
								//stringValue(mauiFilter.getOutputFormIndex() + 1); // TODO: Check
						topic = new Topic(title,  id,  probability);
						log.info(title + "\t" + probability);
						
						if ((int) topRankedInstances[index].
								value(topRankedInstances[index].numAttributes() - 1) == 1) {
							topic.setCorrectness(true);
						} else {
							topic.setCorrectness(false);
						}
						
						documentTopics.addTopic(topic);
						if (debugMode) {
							log.debug("Topic " + title + " " + id + " " + probability);
						}
						
						index++;
					}
				}
			}
			allDocumentTopics.add(documentTopics);
		}
		
		mauiFilter.batchFinished();
		return allDocumentTopics;
	}



	/**
	 * Loads the extraction model from the file.
	 */
	public void loadModel() throws Exception {

		BufferedInputStream inStream
		= new BufferedInputStream(new FileInputStream(modelName));
		ObjectInputStream in = new ObjectInputStream(inStream);
		mauiFilter = (MauiFilter) in.readObject();

		// If TFxIDF values are to be computed from the test corpus
		if (buildGlobalDictionary == true) {
			if (debugMode) {
				log.info("-- The global dictionaries will be built from this test collection..");
			}
			mauiFilter.globalDictionary = null;
		}
		in.close();

		// initialize vocabulary
		mauiFilter.setVocabularyName(vocabularyName);
		mauiFilter.setVocabularyFormat(vocabularyFormat);
		mauiFilter.setDocumentLanguage(documentLanguage);
		mauiFilter.setStemmer(stemmer);

		if (!vocabularyName.equals("none")) {
			loadVocabulary();
			mauiFilter.setVocabulary(vocabulary);
		}

	}
	
	public void printTopics(List<MauiTopics> allDocumentsTopics) {
		FileOutputStream out = null;
		PrintWriter printer = null;
		
		for (MauiTopics documentTopics : allDocumentsTopics) { 
			log.info("Topics for document " + documentTopics.getFilePath());
			try {
				out = new FileOutputStream(documentTopics.getFilePath().replace(".txt", ".maui"));
				if (!documentEncoding.equals("default")) {
					printer = new PrintWriter(new OutputStreamWriter(out, documentEncoding));
				} else {
					printer = new PrintWriter(out);
				}
				
				for (Topic topic : documentTopics.getTopics()) {
						log.info("Topic " + topic.getTitle() + " " + topic.getProbability());
						printer.print(topic.getTitle());
						if (additionalInfo) {
							printer.print("\t");
							printer.print(topic.getProbability());
						}
						printer.println();
				}
				printer.close();
				out.close();
			} catch (FileNotFoundException e) {
				log.error(e.getMessage());
			} catch (IOException e) {
				log.error(e.getMessage());
			}
		}	
	}
	
	public void setModel(MauiFilter mauiFilter) {
		this.mauiFilter = mauiFilter;
	}

	/**
	 * The main method.
	 */
	public static void main(String[] ops) {

		MauiTopicExtractor topicExtractor = new MauiTopicExtractor();
		VocabularyStoreFactory.setPrefferedVocabStoreType(VocabularyStore_HT.class);

		try {
			// Checking and Setting Options selected by the user:
			topicExtractor.setOptions(ops);
			log.info("Extracting keyphrases with options: ");

			// Reading Options, which were set above and output them:
			String[] optionSettings = topicExtractor.getOptions();
			String options = "";
			for (String optionSetting : optionSettings) {
				options += optionSetting + " "; 
			}
			log.info(options);

			// Loading selected Model:
			log.info("-- Loading the model... ");
			topicExtractor.loadModel();

			// Extracting Keyphrases from all files in the input directory
			List<MauiDocument> documents = topicExtractor.loadDocuments();
			List<MauiTopics> topics = topicExtractor.extractTopics(documents);
			topicExtractor.printTopics(topics);
			Evaluator.evaluateTopics(topics);
			

		} catch (Exception e) {

			// Output information on how to use this class
			log.error("Error running MauiTopicExtractor..", e);
			log.error(e.getMessage());
			log.error("\nOptions:\n");
			Enumeration<Option> en = topicExtractor.listOptions();
			while (en.hasMoreElements()) {
				Option option = (Option) en.nextElement();
				log.error(option.synopsis());
				log.error(option.description());
			}
		}
	}

	
}
