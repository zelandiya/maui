package com.entopix.maui.filters;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import weka.core.*;
import weka.filters.*;
import weka.core.Capabilities.Capability;

/**
 * This filter splits the text in selected string attributes into phrases. The
 * resulting string attributes contain these phrases separated by '\n'
 * characters.
 *
 * Phrases are identified according to the following definitions:
 *
 * A phrase is a sequence of words interrupted only by sequences of whitespace
 * characters, where each sequence of whitespace characters contains at most one
 * '\n'.
 *
 * A word is a sequence of letters or digits that contains at least one letter,
 * with the following exceptions:
 *
 * a) '.', '@', '_', '&', '/' are allowed if surrounded by letters or digits,
 *
 * b) '\'' is allowed if preceeded by a letter or digit,
 *
 * c) '-', '/' are also allowed if succeeded by whitespace characters followed
 * by another word. In that case the whitespace characters will be deleted.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class MauiPhraseFilter extends Filter implements OptionHandler {

    private static final Logger log = LoggerFactory.getLogger(MauiPhraseFilter.class);
    /**
     *
     */
    private static final long serialVersionUID = 1L;

    /**
     * Stores which columns to select as a funky range
     */
    protected Range m_SelectCols = new Range();

    /**
     * Determines whether internal periods are allowed
     */
    protected boolean m_DisallowInternalPeriods = false;

    /**
     * Returns a string describing this filter
     *
     * @return a description of the filter suitable for displaying in the
     * explorer/experimenter gui
     */
    public String globalInfo() {
        return "This filter splits the text contained "
                + "by the selected string attributes into phrases.";
    }

    /**
     * Returns an enumeration describing the available options
     *
     * @return an enumeration of all the available options
     */
    @Override
    public Enumeration<Option> listOptions() {

        ArrayList<Option> newVector = new ArrayList<Option>(3);

        newVector.add(new Option(
                "\tSpecify list of attributes to process. First and last are valid\n"
                + "\tindexes. (default none)", "R", 1,
                "-R <index1,index2-index4,...>"));
        newVector.add(new Option("\tInvert matching sense", "V", 0, "-V"));
        newVector.add(new Option("\tDisallow internal periods", "P", 0,
                "-P"));

        return Collections.enumeration(newVector);
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

        return result;
    }

    /**
     * Parses a given list of options controlling the behaviour of this object.
     * Valid options are:
     * <p>
     *
     * -R index1,index2-index4,...<br>
     * Specify list of attributes to process. First and last are valid indexes.
     * (default none)
     * <p>
     *
     * -V<br>
     * Invert matching sense
     * <p>
     *
     * -P<br>
     * Disallow internal periods
     * <p>
     *
     * @param options the list of options as an array of strings
     * @exception Exception if an option is not supported
     */
    public void setOptions(String[] options) throws Exception {

        String list = Utils.getOption('R', options);
        if (list.length() != 0) {
            setAttributeIndices(list);
        }
        setInvertSelection(Utils.getFlag('V', options));

        setDisallowInternalPeriods(Utils.getFlag('P', options));

        if (getInputFormat() != null) {
            setInputFormat(getInputFormat());
        }
    }

    /**
     * Gets the current settings of the filter.
     *
     * @return an array of strings suitable for passing to setOptions
     */
    public String[] getOptions() {

        String[] options = new String[4];
        int current = 0;

        if (getInvertSelection()) {
            options[current++] = "-V";
        }
        if (getDisallowInternalPeriods()) {
            options[current++] = "-P";
        }
        if (!getAttributeIndices().equals("")) {
            options[current++] = "-R";
            options[current++] = getAttributeIndices();
        }

        while (current < options.length) {
            options[current++] = "";
        }
        return options;
    }

    /**
     * Sets the format of the input instances.
     *
     * @param instanceInfo an Instances object containing the input instance
     * structure (any instances contained in the object are ignored - only the
     * structure is required).
     * @return true if the outputFormat may be collected immediately
     */
    public boolean setInputFormat(Instances instanceInfo) throws Exception {

        super.setInputFormat(instanceInfo);
        setOutputFormat(instanceInfo);
        m_SelectCols.setUpper(instanceInfo.numAttributes() - 1);

        return true;
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
    public boolean input(Instance instance) throws Exception {

        if (getInputFormat() == null) {
            throw new Exception("No input instance format defined");
        }
        if (m_NewBatch) {
            resetQueue();
            m_NewBatch = false;
        }
        convertInstance(instance);
        return true;
    }

    /**
     * Signify that this batch of input to the filter is finished. If the filter
     * requires all instances prior to filtering, output() may now be called to
     * retrieve the filtered instances. Any subsequent instances filtered should
     * be filtered based on setting obtained from the first batch (unless the
     * inputFormat has been re-assigned or new options have been set). This
     * default implementation assumes all instance processing occurs during
     * inputFormat() and input().
     *
     * @return true if there are instances pending output
     * @exception NullPointerException if no input structure has been defined,
     * @exception Exception if there was a problem finishing the batch.
     */
    public boolean batchFinished() throws Exception {

        if (getInputFormat() == null) {
            throw new NullPointerException("No input instance format defined");
        }
        m_NewBatch = true;
        return (numPendingOutput() != 0);
    }

    /**
     * Main method for testing this class.
     *
     * @param argv should contain arguments to the filter: use -h for help
     */
    public static void main(String[] argv) {

        try {
            if (Utils.getFlag('b', argv)) {
                Filter.batchFilterFile(new MauiPhraseFilter(), argv);
            } else {
                Filter.filterFile(new MauiPhraseFilter(), argv);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    /**
     * Converts an instance by removing all non-alphanumeric characters from its
     * string attribute values.
     */
    private void convertInstance(Instance instance) throws Exception {

        double[] instVals = new double[instance.numAttributes()];

        for (int i = 0; i < instance.numAttributes(); i++) {

            if (!instance.attribute(i).isString() || instance.isMissing(i)) {
                instVals[i] = instance.value(i);
            } else {
                if (!m_SelectCols.isInRange(i)) {
                    int index = getOutputFormat().attribute(i).addStringValue(
                            instance.stringValue(i));
                    instVals[i] = (double) index;
                    continue;
                }
                String text = instance.stringValue(i);

                String tokenizedText = tokenize(text);

                int index = getOutputFormat().attribute(i).addStringValue(
                        tokenizedText);
                instVals[i] = (double) index;
            }
        }
        Instance inst = new Instance(instance.weight(), instVals);
        inst.setDataset(getOutputFormat());
        push(inst);
    }

    /**
     * This filter splits the text in selected string attributes into phrases.
     * The resulting string attributes contain these phrases separated by '\n'
     * characters.
     *
     * @param text
     * @return the same text with large tokens separated by \n
     */
    public String tokenize(String text) {
        StringBuffer result = new StringBuffer();
        int j = 0;
        boolean phraseStart = true;
        boolean seenNewLine = false;
        boolean haveSeenHyphen = false;
        boolean haveSeenSlash = false;
        while (j < text.length()) {
            boolean isWord = false;
            boolean potNumber = false;
            int startj = j;
            while (j < text.length()) {
                char ch = text.charAt(j);
                if (Character.isLetterOrDigit(ch)) {
                    potNumber = true;
                    if (Character.isLetter(ch)) {
                        isWord = true;
                    }
                    j++;
                } else if ((!m_DisallowInternalPeriods && (ch == '.'))
                        || (ch == '@') || (ch == '_') || (ch == '&')
                        || (ch == '/') || (ch == '\'')) {
                    if ((j > 0) && (j + 1 < text.length())
                            && Character.isLetterOrDigit(text.charAt(j - 1))
                            && Character.isLetterOrDigit(text.charAt(j + 1))) {
                        j++;
                    } else {
                        break;
                    }
                } else if (ch == '\'') {
                    if ((j > 0)
                            && Character.isLetterOrDigit(text.charAt(j - 1))) {
                        j++;
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
            if (isWord == true) {
                if (!phraseStart) {
                    if (haveSeenHyphen) {
                        result.append(' ');
                    } else if (haveSeenSlash) {
                        result.append('/');
                    } else {
                        result.append(' ');
                    }
                }
                result.append(text.substring(startj, j));
                if (j == text.length()) {
                    break;
                }
                phraseStart = false;
                seenNewLine = false;
                haveSeenHyphen = false;
                haveSeenSlash = false;
                if (Character.isWhitespace(text.charAt(j))) {
                    if (text.charAt(j) == '\n') {
                        seenNewLine = true;
                    }
                } else if (text.charAt(j) == '-') {
                    haveSeenHyphen = true;
                } else if (text.charAt(j) == '/') {
                    haveSeenSlash = true;
                } else {
                    phraseStart = true;
                    result.append('\n');
                }
                j++;
            } else if (j == text.length()) {
                break;
            } else if (text.charAt(j) == '\n') {
                if (seenNewLine) {
                    if (phraseStart == false) {
                        result.append('\n');
                        phraseStart = true;
                    }
                } else if (potNumber) {
                    if (phraseStart == false) {
                        phraseStart = true;
                        result.append('\n');
                    }
                }
                seenNewLine = true;
                j++;
            } else if (Character.isWhitespace(text.charAt(j))) {
                if (potNumber) {
                    if (phraseStart == false) {
                        phraseStart = true;
                        result.append('\n');
                    }
                }
                j++;
            } else {
                if (phraseStart == false) {
                    result.append('\n');
                    phraseStart = true;
                }
                j++;
            }
        }

        return result.toString();
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     * explorer/experimenter gui
     */
    public String invertSelectionTipText() {

        return "If set to false, the specified attributes will be processed;"
                + " If set to true, specified attributes won't be processed.";
    }

    /**
     * Get whether the supplied columns are to be processed
     *
     * @return true if the supplied columns won't be processed
     */
    public boolean getInvertSelection() {

        return m_SelectCols.getInvert();
    }

    /**
     * Set whether selected columns should be processed. If true the selected
     * columns won't be processed.
     *
     * @param invert the new invert setting
     */
    public void setInvertSelection(boolean invert) {

        m_SelectCols.setInvert(invert);
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     * explorer/experimenter gui
     */
    public String disallowInternalPeriodsTipText() {

        return "If set to false, internal periods are allowed.";
    }

    /**
     * Get whether the supplied columns are to be processed
     *
     * @return true if the supplied columns won't be processed
     */
    public boolean getDisallowInternalPeriods() {

        return m_DisallowInternalPeriods;
    }

    /**
     * Set whether selected columns should be processed. If true the selected
     * columns won't be processed.
     *
     * @param disallow the new invert setting
     */
    public void setDisallowInternalPeriods(boolean disallow) {

        m_DisallowInternalPeriods = disallow;
    }

    /**
     * Returns the tip text for this property
     *
     * @return tip text for this property suitable for displaying in the
     * explorer/experimenter gui
     */
    public String attributeIndicesTipText() {

        return "Specify range of attributes to act on."
                + " This is a comma separated list of attribute indices, with"
                + " \"first\" and \"last\" valid values. Specify an inclusive"
                + " range with \"-\". E.g: \"first-3,5,6-10,last\".";
    }

    /**
     * Get the current range selection.
     *
     * @return a string containing a comma separated list of ranges
     */
    public String getAttributeIndices() {

        return m_SelectCols.getRanges();
    }

    /**
     * Set which attributes are to be processed
     *
     * @param rangeList a string representing the list of attributes. Since the
     * string will typically come from a user, attributes are indexed from 1.
     * <br>
     * eg: first-3,5,6-last
     */
    public void setAttributeIndices(String rangeList) {

        m_SelectCols.setRanges(rangeList);
    }

    /**
     * Set which attributes are to be processed
     *
     * @param attributes an array containing indexes of attributes to select.
     * Since the array will typically come from a program, attributes are
     * indexed from 0.
     */
    public void setAttributeIndicesArray(int[] attributes) {

        setAttributeIndices(Range.indicesToRangeList(attributes));
    }
}
