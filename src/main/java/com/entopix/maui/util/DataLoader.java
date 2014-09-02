package com.entopix.maui.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.entopix.maui.vocab.Vocabulary;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataLoader {

	private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

	 /**
     * Loads a vocabulary from a given directory
     *
     * @param vocabularyDirectory
     * @param vocabularyName
     * @return Vocabulary
     * @throws Exception 
     */
    public static void loadVocabulary(Vocabulary vocabulary, String vocabularyDirectory, String vocabularyName) throws Exception {
       vocabulary.initializeVocabulary(vocabularyName, "skos", vocabularyDirectory, true);
    }
    
    
    /**
     * Loads MauiDocument objects from data in a given directory.
     * Assumes that each document to extract keywords from is stored as a .txt file,
     * and that it has a list of keywords in .key file with the same name.
     * @param datasetPath
     * @return
     */
	public static List<MauiDocument> loadTestDocuments(String datasetPath) {
		List<MauiDocument> testDocuments = new ArrayList<MauiDocument>();
		
		File datasetDir = new File(datasetPath);
		if (!datasetDir.exists()) {
			log.error("Directory " + datasetPath + " not found!");
			throw new RuntimeException();
		}

		for (File file : datasetDir.listFiles()) {
			if (!file.getName().endsWith(".txt")) {
				continue;
			}
			try {
				String textContent = FileUtils.readFileToString(file);
				File keywordsFile = new File(file.getAbsoluteFile().toString().replace(".txt", ".key"));
				String manualKeywords = FileUtils.readFileToString(keywordsFile);
				MauiDocument testDocument = new MauiDocument(file.getName(), file.getAbsolutePath(), textContent, manualKeywords);
				testDocuments.add(testDocument);
				
			} catch (IOException e) {
				log.error("Error while loading documents: " + e.getMessage());
				throw new RuntimeException();
			}
		}
		
		return testDocuments;
	}

}
