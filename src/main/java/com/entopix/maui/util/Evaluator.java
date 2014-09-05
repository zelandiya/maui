package com.entopix.maui.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.entopix.maui.main.MauiTopicExtractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.Utils;

public class Evaluator {


	private static final Logger log = LoggerFactory.getLogger(MauiTopicExtractor.class);


	public static double[] evaluateTopics(List<MauiTopics> allDocumentsTopics) {

		double[] PRF = null;

		double[] correctStatistics = new double[allDocumentsTopics.size()];
		double[] precisionStatistics = new double[allDocumentsTopics.size()];
		double[] recallStatistics = new double[allDocumentsTopics.size()];

		int i = 0;
		for (MauiTopics documentTopics : allDocumentsTopics) {

			log.debug("Document " + documentTopics.getFilePath());

			double numExtracted = documentTopics.getTopics().size(), numCorrect = 0;

			for (Topic topic : documentTopics.getTopics()) {
				if (topic.isCorrect()) {
					numCorrect += 1.0;
				}
			}

			if (numExtracted > 0 && documentTopics.getPossibleCorrect() > 0) {
				log.debug("-- " + numCorrect + " correct");
				correctStatistics[i] = numCorrect;
				precisionStatistics[i] = numCorrect / numExtracted;				
				recallStatistics[i] = numCorrect / documentTopics.getPossibleCorrect();

			} else {
				correctStatistics[i] = 0.0;
				precisionStatistics[i] = 0.0;	
				recallStatistics[i] = 0.0;
			}
			i++;
		}

		if (correctStatistics.length != 0) {


			double avg = Utils.mean(correctStatistics);
			double stdDev = Math.sqrt(Utils.variance(correctStatistics));

			if (correctStatistics.length == 1) {
				log.info("\n-- Evaluation results based on 1 document:");

			} else {
				log.info("\n-- Evaluation results based on " + correctStatistics.length + " documents:");
			}
			log.info("Avg. number of correct keyphrases per document: "
					+ Utils.doubleToString(avg, 2) + " +/- "
					+ Utils.doubleToString(stdDev, 2));


			double avgPrecision = Utils.mean(precisionStatistics);
			double stdDevPrecision = Math.sqrt(Utils.variance(precisionStatistics));

			log.info("Precision: "
					+ Utils.doubleToString(avgPrecision * 100, 2) + " +/- "
					+ Utils.doubleToString(stdDevPrecision * 100, 2));


			double avgRecall = Utils.mean(recallStatistics);
			double stdDevRecall = Math.sqrt(Utils.variance(recallStatistics));

			log.info("Recall: "
					+ Utils.doubleToString(avgRecall * 100, 2) + " +/- "
					+ Utils.doubleToString(stdDevRecall * 100, 2));

			double fMeasure = 0.0;
			if (avgPrecision > 0 && avgRecall > 0) {
				fMeasure = 2 * avgRecall * avgPrecision / (avgRecall + avgPrecision);
			}

			PRF = new double[] {avgPrecision, avgRecall, fMeasure};

			log.info("F-Measure: " + Utils.doubleToString(fMeasure * 100, 2));

			log.info("");
		}
		return PRF;
	}


	public static void evaluateConsistency(List<IndexerTopics> indexersTopics, List<MauiTopics> allTopics) {

		// compute average consistency across indexers
		double[] consistencyPeople = new double[indexersTopics.size()];
		int person = 0;

		for (IndexerTopics indexer : indexersTopics) {

			// compute average consistency across documents
			double[] consistencyDocs = new double[allTopics.size()];
			int doc = 0;
			for (MauiTopics topics : allTopics) {

				String file = topics.getFilePath().substring(topics.getFilePath().lastIndexOf("/") + 1).replace(".txt", ".key");
				List<Topic> mauiTopics = topics.getTopics();


				Set<String> indexerTopics = new HashSet<String>();
				if (!indexer.getTopics().containsKey(file)) {
					log.warn("Indexer " + indexer.getName() + " doesn't have a file named " + file);
					continue;
				}
				
				for (Topic iTopic : indexer.getTopics().get(file)) {
					indexerTopics.add(iTopic.getTitle());
				}
				int correct = 0;
				for (Topic mTopic : mauiTopics) {
					if (indexerTopics.contains(mTopic.getTitle().toLowerCase())) {
						correct++;
					}
				}
				double consistency = 2*correct / (double) (mauiTopics.size() + indexerTopics.size());
				log.info("Consistency with indexer " + indexer.getName() + " on " + file + " is " + consistency);
				consistencyDocs[doc] = consistency;
				doc++;
			}
			// average across all docs
			double avgConsistencyDocs = Utils.mean(consistencyDocs);
			log.info("Average consistency with indexer " + indexer.getName() + ": " + avgConsistencyDocs);

			consistencyPeople[person] = avgConsistencyDocs;
			person++;
		}
		// average across all people (indexers)
		double avgConsistencyPeople = Utils.mean(consistencyPeople);
		log.info("Average consistency overall: " + avgConsistencyPeople);

	}
}
