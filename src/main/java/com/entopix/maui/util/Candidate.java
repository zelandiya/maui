package com.entopix.maui.util;

import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An object for storing intermediate results of topic extraction:
 * the candidate topics extracted from document text.
 * 
 * @author zelandiya
 *
 */
public class Candidate {
    
    private static final Logger log = LoggerFactory.getLogger(Candidate.class);

    /**
     * Normalized string or vocabulary id
     */
    String name;

    /**
     * The original full form as it appears in the document
     */
    String fullForm;

    /**
     * The title of the descriptor in the vocabulary
     */
    String title;

    /**
     * Number of occurrences of the candidate in the document
     */
    int frequency;

    /**
     * Normalized frequenc
     */
    double termFrequency;

    /**
     * Position of the first occurrence
     */
    double firstOccurrence;

    /**
     * Position of the last occurrence
     */
    double lastOccurrence;

   /**
     * HashMap to store occurrence frequencies of all full forms
     */
    HashMap<String, Counter> fullForms;

    /**
     * Constructor for the first occurrence of a candidate
     */
    public Candidate(String name, String fullForm, int firstOccurrence) {
        this.name = name;
        this.frequency = 1;

        this.firstOccurrence = (double) firstOccurrence;
        this.lastOccurrence = (double) firstOccurrence;
        this.fullForm = fullForm;

        fullForms = new HashMap<String, Counter>();
        fullForms.put(fullForm, new Counter());

    }

    public Candidate(String name, String fullForm, int firstOccurrence,
            double probability) {

        this.name = name;
        this.frequency = 1;

        this.firstOccurrence = (double) firstOccurrence;
        this.lastOccurrence = (double) firstOccurrence;
        this.fullForm = fullForm;

        fullForms = new HashMap<String, Counter>();
        fullForms.put(fullForm, new Counter());

    }

    public Candidate getCopy() {
        Candidate newCandidate = new Candidate(this.name, this.fullForm, (int) this.firstOccurrence);

        newCandidate.frequency = this.frequency;
        newCandidate.termFrequency = this.termFrequency;
        newCandidate.firstOccurrence = this.firstOccurrence;
        newCandidate.lastOccurrence = this.lastOccurrence;
        newCandidate.fullForms = this.fullForms;
        return newCandidate;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitle() {
        return title;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns all document phrases that were mapped to this candidate.
     *
     * @return HashMap in which the keys are the full forms and the values are
     * their frequencies
     */
    public HashMap<String, Counter> getFullForms() {
        return fullForms;
    }

    /**
     * Records the occurrence position and the full form of a candidate
     *
     * @param fullForm
     * @param occurrence
     */
    public void recordOccurrence(String fullForm, int occurrence) {
        frequency++;

        lastOccurrence = occurrence;
        if (fullForms.containsKey(fullForm)) {
            fullForms.get(fullForm).increment();
        } else {
            fullForms.put(fullForm, new Counter());
        }

    }

    /**
     * In case of free indexing, e.g. tagging or keyphrase extraction, retrieves
     * the most frequent full form for a given candidate.
     *
     * @return best full form of a candidate
     */
    public String getBestFullForm() {
        int maxFrequency = 0;
        String bestFullForm = "";
        for (String form : fullForms.keySet()) {
            int formFrequency = fullForms.get(form).value();
            if (formFrequency > maxFrequency) {
                bestFullForm = form;
                maxFrequency = formFrequency;
            }
        }
        return bestFullForm;
    }

    public String getName() {
        return name;
    }

    public double getFrequency() {
        return frequency;
    }

    public double getTermFrequency() {
        return termFrequency;
    }

    public double getFirstOccurrence() {
        return firstOccurrence;
    }

    public double getLastOccurrence() {
        return lastOccurrence;
    }

    public double getSpread() {
        return lastOccurrence - firstOccurrence;
    }

    /**
     * Normalizes all occurrence positions and frequencies by the total values
     * in the given document
     */
    public void normalize(int totalFrequency, int documentLength) {
        termFrequency = frequency / (double) totalFrequency;
        firstOccurrence = firstOccurrence / (double) documentLength;
        lastOccurrence = lastOccurrence / (double) documentLength;

    }

    public String toString() {
        return name + " (" + fullForm + "," + title + ")";
    }

    public String getIdAndTitle() {
        return name + ": " + title;
    }

    /**
     * If two candidates were disambiguated to the same topic, their values are
     * merged.
     *
     * @param previousCandidate
     */
    public void mergeWith(Candidate previousCandidate) {

		// name stays the same
        // full form stays the same
        // title stays the same
        // frequency increments
        this.frequency += previousCandidate.frequency;

        // term frequency increments
        this.termFrequency += previousCandidate.termFrequency;

        // update first occurrence to the earliest one
        double previous = previousCandidate.firstOccurrence;
        if (previous < this.firstOccurrence) {
            this.firstOccurrence = previous;
        }

        // and the opposite with the last occurrence
        previous = previousCandidate.lastOccurrence;
        if (previous > this.lastOccurrence) {
            this.lastOccurrence = previous;
        }

        // full forms should be added to the hash of full forms
        if (fullForms == null) {
            log.info("Is it ever empty??? ");
            fullForms = previousCandidate.fullForms;
        }
        HashMap<String, Counter> prevFullForms = previousCandidate.fullForms;
        for (String prevForm : prevFullForms.keySet()) {
            int count = prevFullForms.get(prevForm).value();
            if (fullForms.containsKey(prevForm)) {
                fullForms.get(prevForm).increment(count);
            } else {
                fullForms.put(prevForm, new Counter(count));
            }
        }

    }

    /**
     * Retrieves all recorded info about a candidate
     *
     * @return info about a candidate formatted as a string
     */
    public String getInfo() {

        String result = "";

        String allFullForms = "";
        for (String form : fullForms.keySet()) {
            allFullForms += form + " (" + fullForms.get(form) + "), ";
        }

        result += "\tName: " + this.name + "\n";
        result += "\tFullForm: " + this.fullForm + "\n";
        result += "\tAllFullForms: " + allFullForms + "\n";
        result += "\tTitle: " + this.title + "\n";
        result += "\tFreq " + this.frequency + "\n";
        result += "\tTermFreq: " + this.termFrequency + "\n";
        result += "\tFirstOcc: " + this.firstOccurrence + "\n";
        result += "\tLastOcc: " + this.lastOccurrence + "\n";
        return result;
    }

}
