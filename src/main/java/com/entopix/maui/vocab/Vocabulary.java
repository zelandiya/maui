package com.entopix.maui.vocab;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

import com.entopix.maui.stemmers.Stemmer;
import com.entopix.maui.stopwords.Stopwords;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Builds an index with the content of the controlled vocabulary.
 * Accepts vocabularies as rdf files (SKOS format) and in plain text format:
 * vocabulary_name.en (with "ID TERM" per line) - descriptors & non-descriptors
 * vocabulary_name.use (with "ID_NON-DESCR \t ID_DESCRIPTOR" per line)
 * vocabulary_name.rel (with "ID \t RELATED_ID1 RELATED_ID2 ... " per line)
 *
 * @author Alyona Medelyan (medelyan@gmail.com)
 */
public class Vocabulary {

	private static final Logger log = LoggerFactory.getLogger(Vocabulary.class);

	public enum Relation {

		kRelationPrefLabel,
		kRelationAltLabel,
		kRelationHiddenLabel,
		kRelationBroader,
		kRelationNarrower,
		kRelationComposite,
		kRelationCompositeOf,
		kRelationHasTopConcept,
		kRelationRelated,
		kRelationNumRelations
	};

	
	private VocabularyStore vocabStore;
	private String vocabularyName;
	private String vocabularyDirectory;
	/** Document language */
	private String language = "en";
	/** Document encoding */
	private String encoding = "UTF-8";
	/** Default stemmer to be used */
	private transient Stemmer stemmer;
	/** List of stopwords to be used */
	private transient Stopwords stopwords;
	/** Normalization to lower case - default true */
	private boolean toLowerCase = true;
	/** Normalization via alphabetic reordering - default true*/
	private boolean reorder = true;
	private boolean debugMode = false;
	private boolean serialize = false;


	/** Initializes vocabulary from a directory
	 *
	 * Given the name of the vocabulary and the format, it first checks whether
	 * the data/vocabularies directory contains the specified files:<br>
	 * - vocabularyName.rdf if skos format is selected<br>
	 * - or a set of 3 flat txt files starting with vocabularyName and with extensions<br>
	 * <li>.en (id term)
	 * <li>.use (non-descriptor \t descriptor)
	 * <li>.rel (id \t related_id1 related_id2 ...)
	 * If the required files exist, the vocabulary index is built.
	 *
	 * @param vocabularyName The name of the vocabulary file (before extension).
	 * @param vocabularyFormat The format of the vocabulary (skos or text).
	 * @param vocabularyDirectory The path to directory with the required vocabulary files
	 * @throws IOException
	 * @throws VocabularyException 
	 * */
	public void initializeVocabulary(String vocabularyName, String vocabularyFormat, String vocabularyDirectory, boolean serialize)
			throws IOException, VocabularyException {

		this.serialize = serialize;
		this.vocabularyDirectory = vocabularyDirectory;
		this.vocabularyName = vocabularyName;

		if (vocabularyFormat.equals("skos")) {
			/** Location of the rdf version of the controlled vocabulary
			 * it needs to be in the SKOS format! */
			File skosFile = new File(vocabularyDirectory + "/" + vocabularyName + ".rdf.gz");

			if (!skosFile.exists()) {
				throw new IOException("File " + skosFile.getAbsolutePath() + " not found!");
			}
			initializeFromSKOSFile(skosFile);
			
		} else if (vocabularyFormat.equals("text")) {
			
			/** Location of the vocabulary's *.en file
			 * containing all terms of the vocabularies and their ids.*/
			File enFile = new File(vocabularyDirectory + "/" + vocabularyName + ".en");
			if (!enFile.exists()) {
				throw new IOException("File " + enFile.getAbsolutePath()
						+ " does not exist.");
			}

			/** Location of the vocabulary's *.use file
			 * containing ids of non-descriptor with the corresponding ids of descriptors.*/
			File useFile = new File(vocabularyDirectory + "/" + vocabularyName + ".use");
			if (!useFile.exists()) {
				throw new IOException("File " + useFile.getAbsolutePath()
						+ " does not exist.");
			}
			
			/** Location of the vocabulary's *.rel file
			 * containing semantically related terms for each descriptor in the vocabulary.*/
			File relFile = new File(vocabularyDirectory + "/" + vocabularyName + ".rel");
			if (!relFile.exists()) {
				throw new IOException("File " + relFile.getAbsolutePath()
						+ " does not exist.");
			}
			initializeFromTXTFiles(enFile, useFile, relFile);

		} else {
			throw new VocabularyException(vocabularyFormat
					+ "is an unsupported vocabulary format! Use skos or text");
		}
		
	}
	
	
	/** Initializes vocabulary from an RDF Model object
	 *  
	 * @param vocabularyName The name of the vocabulary file (before extension).
	 * @param model RDF Model of the SKOS contents of the vocabulary.
	 * @throws Exception
	 * */
	public void initializeVocabulary(String vocabularyName, Model model) throws VocabularyException {
		this.vocabularyName = vocabularyName;
		if (model != null) {
			initializeFromModel(model);
		} else {
			throw new VocabularyException("Model can't be null!");
		}
	}
	
	

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public void setLowerCase(boolean toLowerCase) {
		this.toLowerCase = toLowerCase;
	}

	public void setReorder(boolean reorder) {
		this.reorder = reorder;
	}

	public void setStemmer(Stemmer stemmer) {
		this.stemmer = stemmer;
	}

	public void setDebug(boolean debugMode) {
		this.debugMode = debugMode;
	}

	public void setVocabularyStore(VocabularyStore store) {
		vocabStore = store;
	}
	
	public void setSerialize(boolean serialize) {
		this.serialize = serialize;
	}
	
	

	/**
	 * Loading RDF Model into a VocabularyStore structure for fast access.
	 */
	public void initializeFromModel(Model model) {
		
		vocabStore = VocabularyStoreFactory.CreateVocabStore(vocabularyDirectory, vocabularyName, serialize);
		
		// we already have a de-serialized vocabStore
		if (vocabStore.isInitialized()) {
			return;
		}

		log.info("--- Building the Vocabulary index from the RDF model...");

		StmtIterator iter;
		Statement stmt;
		Resource concept;
		Property property;
		RDFNode value;
		Relation rel;
		
		HashMap<String, ArrayList<String>> children = new HashMap<String, ArrayList<String>>();

		// to create IDs for non-descriptors!
		int count = 0;
		// Iterating over all statements in the SKOS file
		iter = model.listStatements();

		while (iter.hasNext()) {
			stmt = iter.nextStatement();

			// id of the concept (Resource), e.g. "c_4828"
			concept = stmt.getSubject();
			String id_string = concept.getURI();
			
			
			// relation or Property of the concept, e.g. "narrower"
			property = stmt.getPredicate();
			String relation = property.getLocalName();

			// value of the property, e.g. c_4828 has narrower term "c_4829"
			value = stmt.getObject();
			String name = value.toString();

			rel = getRelationForString(relation);

			if (rel == Relation.kRelationPrefLabel) {

				String descriptor, language;
				int atPosition = name.indexOf('@');
				if (atPosition != -1) {
					language = name.substring(atPosition + 1);
					name = name.substring(0, atPosition);
					if (language.equals(this.language)) {
						descriptor = name;
					} else {
						continue;
					}

				} else {
					descriptor = name;
				}

				String descriptorNormalized = normalizePhrase(descriptor);

				if (descriptorNormalized.length() >= 1) {
					vocabStore.addSense(descriptorNormalized, id_string);
					vocabStore.addDescriptor(id_string, descriptor);
				}

			} else if (rel == Relation.kRelationAltLabel
					|| rel == Relation.kRelationHiddenLabel) {

				String non_descriptor, language;

				int atPosition = name.indexOf('@');
				if (atPosition != -1) {
					language = name.substring(atPosition + 1);
					name = name.substring(0, atPosition);
					if (language.equals(this.language)) {
						non_descriptor = name;
					} else {
						continue;
					}

				} else {
					non_descriptor = name;
				}


				String non_descriptorNormalized = normalizePhrase(non_descriptor);
				if (non_descriptorNormalized.length() >= 1) {
					vocabStore.addSense(non_descriptorNormalized, id_string);
				}
				addNonDescriptor(count, id_string, non_descriptor, non_descriptorNormalized);
				count++;

			} else if (rel == Relation.kRelationBroader
					|| rel == Relation.kRelationNarrower
					|| rel == Relation.kRelationComposite
					|| rel == Relation.kRelationCompositeOf
					|| rel == Relation.kRelationHasTopConcept
					|| rel == Relation.kRelationRelated) {

				// adds directly related term
				vocabStore.addRelatedTerm(id_string, name);
				
//				if (rel == Relation.kRelationNarrower) {
//					if (!children.containsKey(id_string)) {
//						children.put(id_string, new ArrayList<String>());
//					}
//					if (!children.get(id_string).contains(name))
//						children.get(id_string).add(name);
//				} else if (rel == Relation.kRelationBroader) {
//					if (!children.containsKey(name)) {
//						children.put(name, new ArrayList<String>());
//					}
//					if (!children.get(name).contains(id_string))
//						children.get(name).add(id_string);
//				}

				vocabStore.addRelationship(id_string, name, rel);
				if (rel == Relation.kRelationRelated) {
					vocabStore.addRelationship(name, id_string, rel);
				}
			}
		}
		
//		// adds indirectly related terms
//		for (String id_string : children.keySet()) {
//			for (String child1 : children.get(id_string)) {
//				for (String child2 : children.get(id_string)) {
//					if (!child1.equals(child2)) {
//						// adds sibling as related term
//						vocabStore.addRelatedTerm(child2, child1);
//						vocabStore.addRelatedTerm(child1, child2);
//					}
//				}
//			}	
//		}

		if (debugMode) {
			log.info("--- Statistics about the vocabulary: ");
			log.info("\t" + vocabStore.getNumTerms() + " terms in total");
			log.info("\t" + vocabStore.getNumNonDescriptors() + " non-descriptive terms");
			log.info("\t" + vocabStore.getNumRelatedTerms() + " terms have related terms");
		}

		vocabStore.finishedInitialized();

		if (serialize) {
			VocabularyStoreFactory.SerializeNewVocabStore( vocabularyDirectory, vocabularyName, vocabStore);
		}
	}

	

	/**
	 * Loads the Model from the SKOS file first, then initializes it.
	 * @throws IOException 
	 *
	 */
	public void initializeFromSKOSFile(File skosFile) throws IOException {
		
		if (serialize) {
			vocabStore = VocabularyStoreFactory.CreateVocabStore(vocabularyDirectory, vocabularyName, serialize);
			
			// we already have a de-serialized vocabStore
			if (vocabStore.isInitialized()) {
				return;
			} else {
				Model model = readModelFromFile(skosFile);
				initializeFromModel(model);
			}
		} else {
			Model model = readModelFromFile(skosFile);
			initializeFromModel(model);
		}
	}
	
	private Model readModelFromFile(File skosFile) throws IOException {
		log.info("--- Loading RDF model from the SKOS file...");
		Model model = ModelFactory.createDefaultModel();
		InputStream gzipStream = new GZIPInputStream(new FileInputStream(skosFile));
		model.read(new InputStreamReader(gzipStream, encoding), "");
		return model;
	}

	

	/**
	 * Loading data from text files into a Vocabulary Store object for fast access.
	 * 
	 * @throws IOException 
	 *
	 */
	public void initializeFromTXTFiles(File enFile, File useFile, File relFile) throws IOException {

		vocabStore = VocabularyStoreFactory.CreateVocabStore(vocabularyDirectory, vocabularyName, serialize);
		
		// we already have a de-serialized vocabStore
		if (vocabStore.isInitialized()) {
			return;
		}
		
		log.info("--- Loading Vocabulary from text files...");
		buildTEXT(enFile);
		buildUSE(useFile);
		buildREL(relFile);
	
		vocabStore.finishedInitialized();

		if (serialize) {
			VocabularyStoreFactory.SerializeNewVocabStore( vocabularyDirectory, vocabularyName, vocabStore );
		}
	}

	/**
	 * Set the stopwords class.
	 * @param stopwords
	 */
	public void setStopwords(Stopwords stopwords) {
		this.stopwords = stopwords;
	}

	private Relation getRelationForString(String rel) {
		if (rel.equals("prefLabel")) {
			return Relation.kRelationPrefLabel;
		} else if (rel.equals("altLabel")) {
			return Relation.kRelationAltLabel;
		} else if (rel.equals("hiddenLabel")) {
			return Relation.kRelationHiddenLabel;
		} else if (rel.equals("broader")) {
			return Relation.kRelationBroader;
		} else if (rel.equals("narrower")) {
			return Relation.kRelationNarrower;
		} else if (rel.equals("composite")) {
			return Relation.kRelationComposite;
		} else if (rel.equals("compositeOf")) {
			return Relation.kRelationCompositeOf;
		} else if (rel.equals("hasTopConcept")) {
			return Relation.kRelationHasTopConcept;
		} else if (rel.equals("related")) {
			return Relation.kRelationRelated;
		}

		return Relation.kRelationNumRelations;
	}

	public VocabularyStore getVocabularyStore() {
		return vocabStore;
	}


	private void addNonDescriptor(int count, String idDescriptor,
			String nonDescriptor, String normalizedNonDescriptor) {

		if (vocabularyName.equals("lcsh") && nonDescriptor.indexOf('(') != -1) {
			return;
		}

		String idNonDescriptor = "d_" + count;

		if (normalizedNonDescriptor.length() >= 1) {
			vocabStore.addSense(normalizedNonDescriptor, idNonDescriptor);
		}

		vocabStore.addDescriptor(idNonDescriptor, nonDescriptor);
		vocabStore.addNonDescriptor(idNonDescriptor, idDescriptor);
	}

	public String getFormatedName( String in )
	{
		return vocabStore.getFormatedName( in );
	}

	/**
	 * Builds the vocabulary index from the text files.
	 * @throws IOException 
	 */
	public void buildTEXT(File enFile) throws IOException {

		log.info("-- Building the Vocabulary index");

		String readline;
		String term;
		String avterm;
		String id_string;
		InputStreamReader is = new InputStreamReader(new FileInputStream(enFile));
		BufferedReader br = new BufferedReader(is);
		while ((readline = br.readLine()) != null) {
			int i = readline.indexOf(' ');
			term = readline.substring(i + 1);

			avterm = normalizePhrase(term);

			if (avterm.length() >= 1) {
				id_string = readline.substring(0, i);
				vocabStore.addDescriptor(id_string, term);
			}
		}
		br.close();
		is.close();
	}

	/**
	 * Builds the vocabulary index with descriptors/non-descriptors relations.
	 */
	public void buildUSE(File useFile) throws IOException {
		String readline;
		String[] entry;

		InputStreamReader is = new InputStreamReader(new FileInputStream(useFile));
		BufferedReader br = new BufferedReader(is);
		while ((readline = br.readLine()) != null) {
			entry = readline.split("\t");
			//	if more than one descriptors for
			//	one non-descriptors are used, ignore it!
			//	probably just related terms (cf. latest edition of Agrovoc)
			if ((entry[1].indexOf(" ")) == -1) {
				vocabStore.addNonDescriptor(entry[0], entry[1]);
			}
		}
		br.close();
		is.close();
	}

	/**
	 * Builds the vocabulary index with semantically related terms.
	 */
	public void buildREL(File relFile) throws IOException {
		log.info("-- Building the Vocabulary index with related pairs");
		String readline;
		String[] entry;
		InputStreamReader is = new InputStreamReader(new FileInputStream(relFile));
		BufferedReader br = new BufferedReader(is);
		while ((readline = br.readLine()) != null) {
			entry = readline.split("\t");
			String[] temp = entry[1].split(" ");
			for (int i = 0; i < temp.length; i++) {
				vocabStore.addRelatedTerm(entry[0], temp[i]);
			}
		}
		br.close();
		is.close();
	}

	/**
	 * Returns the term for the given id (as a string)
	 * @param id - id of some phrase in the vocabulary
	 * @return phrase, i.e. the full form listed in the vocabulary
	 */
	public String getTerm(String id) {
		return vocabStore.getTerm(id);
	}

	/**
	 * Checks whether a normalized phrase
	 * is a valid vocabulary term.
	 * @param phrase
	 * @return true if phrase is in the vocabulary
	 */
	public boolean containsNormalizedEntry(String phrase) {
		return vocabStore.getNumSenses(normalizePhrase(phrase)) > 0;
	}

	/**
	 * Returns true if a phrase has more than one senses
	 * @param phrase
	 * @return false if a phrase has only one sense
	 */
	public boolean isAmbiguous(String phrase) {
		return vocabStore.getNumSenses(normalizePhrase(phrase)) > 1;
	}

	/**
	 * Retrieves all possible descriptors for a given phrase
	 * @param phrase
	 * @return a vector list of all senses of a given term
	 */
	public ArrayList<String> getSenses(String phrase) {
		String normalized = normalizePhrase(phrase);
		ArrayList<String> senses = vocabStore.getSensesForPhrase(normalized);
		return senses;
	}

	/**
	 * Given id of a term returns the list with ids of terms related to this term.
	 * @param id
	 * @return a vector with ids related to the input id
	 */
	public ArrayList<String> getRelated(String id) {
		return vocabStore.getRelatedTerms(id);
	}

	/***
	 * Returns falls if the phrase only contains upper case characters
	 * @param phrase
	 * @return
	 */
	private boolean isOkToLower(String phrase) {
		int lower = 0;
		int upper = 0;
		for (char p : phrase.toCharArray()) {
			if (Character.isLowerCase(p)) {
				lower++;
			}
			if (Character.isUpperCase(p)) {
				upper++;
			}
		}

		// don't lower case words containing 5 or less characters
		// that are all capitalized (most likely it's an abbreviation)
		if (upper > lower && upper < 5) {
			return false;
		}
		return true;
	}

	/**
	 * Generates the pseudo phrase from a string.
	 * A pseudo phrase is a version of a phrase
	 * that only contains non-stopwords,
	 * which are stemmed and sorted into alphabetical order.
	 */
	public String normalizePhrase(String phrase) {
	
		String orig = phrase;
		if (orig.endsWith("-") || orig.endsWith(".")) {
			return orig;
		}
		StringBuilder result = new StringBuilder();
		char prev = ' ';
		int i = 0;
		while (i < phrase.length()) {
			char c = phrase.charAt(i);

			// we ignore everything after the "/" symbol and everything in brackets
			// e.g. Monocytes/*immunology/microbiology -> monocytes
			if (vocabularyName.equals("mesh") && c == '/') {
				break;
			}

			if (c == '&' || c == '.' || c == '.') {
				c = ' ';
			}

			if (c == '*' || c == ':') {
				prev = c;
				i++;
				continue;
			}

			if (c != ' ' || prev != ' ') {
				result.append(c);
			}

			prev = c;
			i++;
		}


		phrase = result.toString().trim();
		//        int indexOfOpen = phrase.indexOf('(');
		//        if (indexOfOpen != -1) {
		//            int indexOfClose = phrase.indexOf(')');
		//            if (indexOfOpen < indexOfClose && indexOfOpen != 0) {
		//                if (indexOfClose == phrase.length()) {
		//                    phrase = phrase.substring(0, indexOfOpen - 1);
		//                } else {
		//                    phrase = phrase.substring(0, indexOfOpen - 1) + phrase.substring(indexOfClose + 1);
		//                }
		//            }
		//        }


		if (isOkToLower(phrase) && toLowerCase) {
			phrase = phrase.toLowerCase();
		}

		if (reorder || stopwords != null || stemmer != null) {
			phrase = pseudoPhrase(phrase);
		}
		if (phrase.equals("")) {
			// to prevent cases where the term is a stop word (e.g. Back).
			return result.toString();
		} else {
			// log.info(orig + " >> " + phrase);
			return phrase;
		}
	}

	/**
	 * Generates the preudo phrase from a string.
	 * A pseudo phrase is a version of a phrase
	 * that only contains non-stopwords,
	 * which are stemmed and sorted into alphabetical order.
	 */
	public String pseudoPhrase(String str) {
		String result = "";
		String[] words = str.split(" ");
		if (reorder) {
			Arrays.sort(words);
		}
		for (String word : words) {

			if (stopwords != null) {
				if (stopwords.isStopword(word)) {
					continue;
				}
			}
			int apostr = word.indexOf('\'');
			if (apostr != -1 && apostr == word.length() - 2) {
				word = word.substring(0, apostr);
			}
			if (stemmer != null) {
				word = stemmer.stem(word);
			}
			result += word + " ";
		}
		return result.trim();
	}


	public void setVocabularyName(String vocabularyName) {
		this.vocabularyName = vocabularyName;	
	}
	
	public class VocabularyException extends Exception {
        /**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public VocabularyException(String message) {
            super(message);
        }
    }

	public double getGenerality(String id) {
		// TODO: insert generality method using SPARQL query
		return 0.0;
	}
}
