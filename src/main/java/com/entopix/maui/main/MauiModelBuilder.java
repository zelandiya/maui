package com.entopix.maui.main;

/*
 *    MauiModelBuilder.java
 *    Copyright (C) 2001-2014 Eibe Frank, Alyona Medelyan
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
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import com.entopix.maui.filters.MauiFilter;
import com.entopix.maui.filters.MauiFilter.MauiFilterException;
import com.entopix.maui.stemmers.PorterStemmer;
import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;
import com.entopix.maui.stopwords.StopwordsEnglish;
import com.entopix.maui.util.DataLoader;
import com.entopix.maui.util.MauiDocument;
import com.entopix.maui.vocab.Vocabulary;
import com.entopix.maui.vocab.VocabularyStoreFactory;
import com.entopix.maui.vocab.VocabularyStore_HT;
import com.entopix.maui.wikifeatures.WikiFeatures;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * Builds a topic indexing model from the documents in a given directory.
 * Assumes that the file names for the documents end with ".txt". Assumes that
 * files containing corresponding author-assigned keyphrases end with ".key".
 * Optionally an encoding for the documents/keyphrases can be defined (e.g. for
 * Chinese text).
 *
 * Valid options are:
 * <p>
 *
 * -l "documents directory"<br>
 * Specifies name of directory with documents to analyze.<p>
 *
 * -m "model path"<br>
 * Specifies path to the model file.<p>
 *
 * -v "vocabulary path"<br>
 * Specifies path to the vocabulary file.<p>
 *
 * -e "encoding"<br>
 * Specifies encoding.<p>
 * .<p>
 *
 * -f "vocabulary format" <br>
 * Specifies vocabulary format (txt or skos)
 * .<p>
 *
 * -i "document language" <br>
 * Specifies document language (en, es, de, fr)
 * .<p>
 *
 * -d<br>
 * Turns debugging mode on.<p>
 *
 * -x "length"<br>
 * Sets maximum phrase length (default: 3)
 * .<p>
 *
 * -y "length"<br>
 * Sets minimum phrase length (default: 1)
 * .<p>
 *
 * -o "number"<br>
 * Sets the minimum number of times a phrase needs to occur (default: 2).
 * <p>
 *
 * -s "stopwords class"<br>
 * Sets the name of the class implementing the stop words (default: StopwordsEnglish)
 * .<p>
 *
 * -t "stemmer class "<br>
 * Sets stemmer to use (default: PorterStemmer).
 * <p>
 * 
 * -z "use serialization"<br>
 * If this option is used, the vocabulary is serialized for faster usage
 * <p>
 * 
 * @author Eibe Frank (eibe@cs.waikato.ac.nz), Alyona Medelyan
 * (medelyan@gmail.com)
 * @version 1.0
 */
public class MauiModelBuilder implements OptionHandler {

	private static final Logger log = LoggerFactory.getLogger(MauiModelBuilder.class);

	/**
	 * Path to the directory
	 */
	public String inputDirectoryName = null;

	/**
	 * Path to the model
	 */
	public String modelName = null;

	/**
	 * Path to the vocabulary
	 */
	public String vocabularyName = "none";

	/**
	 * Format of the vocabulary {skos,text}
	 */
	public String vocabularyFormat = null;

	/**
	 * Document language {en,es,de,fr,...}
	 */
	public String documentLanguage = "en";

	/**
	 * Document encoding
	 */
	public String documentEncoding = "default";
	
	/**
	 * Serialize vocabulary?
	 */
	public boolean serialize = false;

	/**
	 * Maximum length of phrases
	 */
	public int maxPhraseLength = 5;

	/**
	 * Minimum length of phrases
	 */
	public int minPhraseLength = 1;

	/**
	 * Minimum number of occurences of a phrase
	 */
	public int minNumOccur = 1;

	/**
	 * Classifier
	 */
	private Classifier classifier = null;

	/**
	 * Use basic features TFxIDF & First Occurrence
	 */
	boolean useBasicFeatures = true;

	/**
	 * Use domain keyphraseness feature
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
	 * Use thesaurus features Node degree & Generality
	 */
	boolean useThesaurusFeatures = true;

	/**
	 * Use Wikipedia features
	 */
	boolean useWikipediaFeatures = false;

	/**
	 * Use length feature
	 */
	boolean useLengthFeature = true;
	
	WikiFeatures wikiFeatures = null;

	/**
	 * Maui filter object
	 */
	private MauiFilter mauiFilter = null;

	/**
	 * Stemmer to be used
	 */
	public Stemmer stemmer = new PorterStemmer();

	/**
	 * Llist of stopwords to be used
	 */
	public Stopwords stopwords = new StopwordsEnglish();

	private Vocabulary vocabulary = null;

	private void loadVocabulary() {
		if (vocabulary != null) {
			return;
		}

		try {

			log.info("--- Loading the vocabulary...");
			vocabulary = new Vocabulary();
			vocabulary.setStemmer(stemmer);
			if (!vocabularyName.equals("lcsh")) {
				vocabulary.setStopwords(stopwords);
			}

			vocabulary.setLanguage(documentLanguage);
			// make serialize global var
			vocabulary.setSerialize(serialize);
			vocabulary.initializeVocabulary(vocabularyName, vocabularyFormat);

		} catch (Exception e) {
			log.error("Failed to load thesaurus!", e);
		}

	}

	public void setVocabulary(Vocabulary vocabulary) {
		this.vocabulary = vocabulary;
	}

	public void setBasicFeatures(boolean useBasicFeatures) {
		this.useBasicFeatures = useBasicFeatures;
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
		this.useThesaurusFeatures = useThesaurusFeatures;
	}

	public void setWikipediaFeatures(boolean useWikipediaFeatures) {
		this.useWikipediaFeatures = useWikipediaFeatures;
		if (this.useWikipediaFeatures) {
			wikiFeatures = new WikiFeatures();
			this.wikiFeatures.load_csv("src/main/resources/data/labels.csv.gzip", true);
		}
	}

	public void setLengthFeature(boolean useLengthFeature) {
		this.useLengthFeature = useLengthFeature;
	}

	public void setVocabularyName(String vocabularyName) {
		this.vocabularyName = vocabularyName;
	}

	/**
	 * Parses a given list of options controlling the behaviour of this object.
	 * Valid options are:
	 * <p>
	 *
	 * -l "directory name" <br>
	 * Specifies name of directory.<p>
	 *
	 * -m "model name" <br>
	 * Specifies name of model.<p>
	 *
	 * -v "vocabulary name" <br>
	 * Specifies vocabulary name.<p>
	 *
	 * -f "vocabulary format" <br>
	 * Specifies vocabulary format.<p>
	 *
	 * -i "document language" <br>
	 * Specifies document language.<p>
	 *
	 * -e "encoding" <br>
	 * Specifies encoding.<p>
	 *
	 * -d<br>
	 * Turns debugging mode on.<p>
	 *
	 * -x "length"<br>
	 * Sets maximum phrase length (default: 3)
	 * .<p>
	 *
	 * -y "length"<br>
	 * Sets minimum phrase length (default: 3)
	 * .<p>
	 *
	 * -o "number"<br>
	 * The minimum number of times a phrase needs to occur (default: 2).
	 * <p>
	 *
	 * -s "name of class implementing list of stop words"<br>
	 * Sets list of stop words to used (default: StopwordsEnglish)
	 * .<p>
	 *
	 * -t "name of class implementing stemmer"<br>
	 * Sets stemmer to use (default: IteratedLovinsStemmer).
	 * <p>
	 *
	 * @param options the list of options as an array of strings
	 * @exception Exception if an option is not supported
	 */
	@Override
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

		if (!"".equals(vocabularyName) && !vocabularyName.equals("none")) {
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

		String maxPhraseLengthString = Utils.getOption('x', options);
		if (maxPhraseLengthString.length() > 0) {
			this.maxPhraseLength = Integer.parseInt(maxPhraseLengthString);
		}

		String minPhraseLengthString = Utils.getOption('y', options);
		if (minPhraseLengthString.length() > 0) {
			this.minPhraseLength = Integer.parseInt(minPhraseLengthString);
		}

		String minNumOccurString = Utils.getOption('o', options);
		if (minNumOccurString.length() > 0) {
			this.minNumOccur = Integer.parseInt(minNumOccurString);
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
		this.serialize = Utils.getFlag('z', options);
		Utils.checkForRemainingOptions(options);
	}

	/**
	 * Gets the current option settings.
	 *
	 * @return an array of strings suitable for passing to setOptions
	 */
	@Override
	public String[] getOptions() {

		String[] options = new String[23];
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
		options[current++] = "-z";
		options[current++] = "-x";
		options[current++] = "" + (this.maxPhraseLength);
		options[current++] = "-y";
		options[current++] = "" + (this.minPhraseLength);
		options[current++] = "-o";
		options[current++] = "" + (this.minNumOccur);
		options[current++] = "-s";
		options[current++] = "" + (stopwords.getClass().getName());
		options[current++] = "-t";
		options[current++] = "" + (stemmer.getClass().getName());

		while (current < options.length) {
			options[current++] = "";
		}
		return options;
	}

	/**
	 * Returns an enumeration describing the available options.
	 *
	 * @return an enumeration of all the available options
	 */
	@Override
	public Enumeration<Option> listOptions() {

		ArrayList<Option> newVector = new ArrayList<Option>(12);

		newVector.add(new Option("\tSpecifies name of directory.", "l",
				1, "-l <directory name>"));
		newVector.add(new Option("\tSpecifies name of model.", "m", 1,
				"-m <model name>"));
		newVector.add(new Option("\tSpecifies vocabulary name.", "v", 1,
				"-v <vocabulary name>"));
		newVector.add(new Option(
				"\tSpecifies vocabulary format (text or skos or none).", "f",
				1, "-f <vocabulary format>"));
		newVector.add(new Option(
				"\tSpecifies document language (en (default), es, de, fr).",
				"i", 1, "-i <document language>"));
		newVector.add(new Option("\tSpecifies encoding.", "e", 1,
				"-e <encoding>"));
		newVector.add(new Option("\tTurns serialization on.", "z", 0,
				"-z"));
		newVector.add(new Option(
				"\tSets the maximum phrase length (default: 5).", "x", 1,
				"-x <length>"));
		newVector.add(new Option(
				"\tSets the minimum phrase length (default: 1).", "y", 1,
				"-y <length>"));
		newVector.add(new Option(
				"\tSet the minimum number of occurences (default: 2).", "o", 1,
				"-o"));
		newVector
		.add(new Option(
				"\tSets the list of stopwords to use (default: StopwordsEnglish).",
				"s", 1, "-s <name of stopwords class>"));
		newVector.add(new Option(
				"\tSet the stemmer to use (default: SremovalStemmer).", "t", 1,
				"-t <name of stemmer class>"));

		return Collections.enumeration(newVector);
	}

	public MauiFilter buildModel() throws MauiFilterException {
		List<MauiDocument> testDocuments = DataLoader.loadTestDocuments(inputDirectoryName);
		return buildModel(testDocuments);
	}

	/**
	 * Builds the model from the training data
	 * @throws MauiFilterException 
	 */
	public MauiFilter buildModel(List<MauiDocument> documents) throws MauiFilterException {

		log.info("-- Building the model... ");

		FastVector atts = new FastVector(3);
		atts.addElement(new Attribute("filename", (FastVector) null));
		atts.addElement(new Attribute("document", (FastVector) null));
		atts.addElement(new Attribute("keyphrases", (FastVector) null));
		Instances data = new Instances("keyphrase_training_data", atts, 0);

		mauiFilter = new MauiFilter();
		mauiFilter.setMaxPhraseLength(maxPhraseLength);
		mauiFilter.setMinPhraseLength(minPhraseLength);
		mauiFilter.setMinNumOccur(minNumOccur);
		mauiFilter.setStemmer(stemmer);
		mauiFilter.setDocumentLanguage(documentLanguage);
		mauiFilter.setVocabularyName(vocabularyName);
		mauiFilter.setVocabularyFormat(vocabularyFormat);
		mauiFilter.setStopwords(stopwords);
		mauiFilter.setVocabulary(vocabulary);

		if (classifier != null) {
			mauiFilter.setClassifier(classifier);
		}

		mauiFilter.setInputFormat(data);

		// set features configurations
		mauiFilter.setBasicFeatures(useBasicFeatures);
		mauiFilter.setKeyphrasenessFeature(useKeyphrasenessFeature);
		mauiFilter.setFrequencyFeatures(useFrequencyFeatures);
		mauiFilter.setPositionsFeatures(usePositionsFeatures);
		mauiFilter.setLengthFeature(useLengthFeature);
		mauiFilter.setThesaurusFeatures(useThesaurusFeatures);
		mauiFilter.setWikipediaFeatures(useWikipediaFeatures, wikiFeatures);

		mauiFilter.setClassifier(classifier);

		if (!vocabularyName.equals("none")) {
			loadVocabulary();
			mauiFilter.setVocabulary(vocabulary);
		}

		log.info("-- Adding documents as instances... ");

		for (MauiDocument document : documents) {

			double[] newInst = new double[3];
			newInst[0] = data.attribute(0).addStringValue(document.getFileName());

			// Adding the text and the topics for the document to the instance
			if (document.getTextContent().length() > 0) {
				newInst[1] = data.attribute(1).addStringValue(document.getTextContent());
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
		}
		log.info("-- Building the model... ");

		mauiFilter.batchFinished();

		while ((mauiFilter.output()) != null) {
		}

		return mauiFilter;

	}

	/**
	 * Saves the extraction model to the file.
	 * @param mauiFilter 
	 */
	public void saveModel(MauiFilter mauiFilter) throws Exception {

		BufferedOutputStream bufferedOut = new BufferedOutputStream(
				new FileOutputStream(modelName));
		ObjectOutputStream out = new ObjectOutputStream(bufferedOut);
		out.writeObject(mauiFilter);
		out.flush();
		out.close();
	}

	/**
	 * The main method.
	 */
	public static void main(String[] ops) {

		MauiModelBuilder modelBuilder = new MauiModelBuilder();
		VocabularyStoreFactory.setPrefferedVocabStoreType(VocabularyStore_HT.class);

		try {

			modelBuilder.setOptions(ops);

			// Output what options are used
			log.info("Building model with options: ");
			String[] optionSettings = modelBuilder.getOptions();
			String options = "";
			for (String optionSetting : optionSettings) {
				options += optionSetting + " ";
			}
			log.info(options);
			
			MauiFilter mauiFilter = modelBuilder.buildModel();

			log.info("Model built. Saving the model...");
			
			modelBuilder.saveModel(mauiFilter);

			log.info("Done!");

		} catch (Exception e) {

			// Output information on how to use this class
			log.error("Error running MauiModelBuilder..", e);
			log.error(e.getMessage());
			log.error("\nOptions:\n");
			Enumeration<Option> en = modelBuilder.listOptions();
			while (en.hasMoreElements()) {
				Option option = en.nextElement();
				log.error(option.synopsis());
				log.error(option.description());
			}
		}
	}


}
