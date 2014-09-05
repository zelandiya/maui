package com.entopix.maui.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.entopix.maui.filters.MauiFilter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataLoader {

	private static final Logger log = LoggerFactory.getLogger(DataLoader.class);

	
    /**
     * Loads model from a file path
     * @param modelPath
     * @return
     * @throws ClassNotFoundException 
     */
    public static MauiFilter loadModel(String modelPath) {
		BufferedInputStream inStream = null;
		MauiFilter model = null;
		try {
			inStream = new BufferedInputStream(
					new FileInputStream(modelPath));
			ObjectInputStream in = new ObjectInputStream(inStream);
			model = (MauiFilter) in.readObject();
			in.close();
			inStream.close();
			
		} catch (IOException e) {
			log.error("Error while loading extraction model from " + modelPath + "!\n", e);
			throw new RuntimeException();
		} catch (ClassNotFoundException e) {
			log.error("Mismatch of the class in " + modelPath + "!\n", e);
			throw new RuntimeException();
		} finally {
			try {
				inStream.close();
			} catch (IOException e1) {
				log.error("Error while loading extraction model from " + modelPath + "!\n", e1);
				throw new RuntimeException();
			}
		}
		return model;
	
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
			log.error("Directory " + datasetDir.getAbsolutePath() + " not found!");
			throw new RuntimeException();
		}

		for (File file : datasetDir.listFiles()) {
			if (!file.getName().endsWith(".txt")) {
				continue;
			}
			try {
				String textContent = FileUtils.readFileToString(file);
				File keyFile = new File(file.getAbsoluteFile().toString().replace(".txt", ".key"));
				String manualTopics = "";
				if (keyFile.exists()) {
					manualTopics = FileUtils.readFileToString(keyFile);
				}
				MauiDocument testDocument = new MauiDocument(file.getName(), file.getAbsolutePath(), textContent, manualTopics);
				testDocuments.add(testDocument);

			} catch (IOException e) {
				log.error("Error while loading documents: " + e.getMessage());
				throw new RuntimeException();
			}
		}

		return testDocuments;
	}

	public static List<IndexerTopics> readIndexersTopics(String pathToIndexersDirs) {

		List<IndexerTopics> indexersTopics = new ArrayList<IndexerTopics>();

		File indexersDir = new File(pathToIndexersDirs);
		if (!indexersDir.exists()) {
			log.error("Directory " + indexersDir.getAbsolutePath() + " not found!");
			throw new RuntimeException();
		}

		String manualTopics;
		// read the list of indexers
		for (File indexer : indexersDir.listFiles()) {

			IndexerTopics indexerTopics = new IndexerTopics(indexer.getName());
			// read the files they have topics for
			for (File keyFile : indexer.listFiles()) {
				if (!keyFile.getName().endsWith(".key")) {
					continue;
				}

				// initialize topics for that file and that indexer
				indexerTopics.addKeyFile(keyFile.getName());
				try {
					// read the topics
					manualTopics = FileUtils.readFileToString(keyFile);
					for (String mTopic : manualTopics.split("\n")) {
						if (mTopic.length() > 1) {
							indexerTopics.addTopic(keyFile.getName(), mTopic);
						}
					}
				} catch (IOException e) {
					log.error("Error while loading key file: " + keyFile + "\n" + e.getMessage());
					throw new RuntimeException();
				}
			}
			indexersTopics.add(indexerTopics);
		}
		return indexersTopics;
	}


	public static void addTopicsFromIndexers(
			List<MauiDocument> testDocuments, List<IndexerTopics> indexersTopics) {

		for (MauiDocument document : testDocuments) {
			String keyFile = document.getFileName().replace(".txt", ".key");
			HashMap<String, Counter> countTopics = new HashMap<String, Counter>();

			for (IndexerTopics indexerTopics : indexersTopics) {
				List<Topic> keyFileTopics = indexerTopics.getTopics().get(keyFile);
				for (Topic topic : keyFileTopics) {
					String topicTitle = topic.getTitle();
					if (!countTopics.containsKey(topicTitle)) {
						countTopics.put(topicTitle, new Counter());
					}
					countTopics.get(topicTitle).increment();
				}
			}
			
			StringBuilder allManualTopics = new StringBuilder();
			for (String topic : countTopics.keySet()) {
				allManualTopics.append(topic);
				allManualTopics.append("\t");
				allManualTopics.append(countTopics.get(topic));
				allManualTopics.append("\n");
			}
			document.setTopicsString(allManualTopics.toString());
		}
	}

}
