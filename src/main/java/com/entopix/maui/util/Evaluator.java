package com.entopix.maui.util;

import java.util.List;

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

}
