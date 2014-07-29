package com.entopix.maui.filters;

import weka.core.*;
import weka.core.Capabilities.Capability;
import weka.filters.*;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Removes all numbers from all the string attributes in the given dataset.
 * Assumes that words are separated by whitespace.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class NumbersFilter extends Filter {
    
    private static final Logger log = LoggerFactory.getLogger(NumbersFilter.class);

    private static final long serialVersionUID = 1L;

    /**
     * Returns a string describing this filter
     *
     * @return a description of the filter suitable for displaying in the
     * explorer/experimenter gui
     */
    public String globalInfo() {
        return "Removes all numbers from all the string attributes in "
                + "the given dataset. Assumes that words are separated by whitespace.";
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
        return true;
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
     * Main method for testing this class.
     *
     * @param argv should contain arguments to the filter: use -h for help
     */
    public static void main(String[] argv) {

        try {
            if (Utils.getFlag('b', argv)) {
                Filter.batchFilterFile(new NumbersFilter(), argv);
            } else {
                Filter.filterFile(new NumbersFilter(), argv);
            }
        } catch (Exception ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    private String filterNumbers(String inputString) {
        StringBuffer resultString = new StringBuffer();
        StringTokenizer tok = new StringTokenizer(inputString, " \t\n", true);
        while (tok.hasMoreTokens()) {
            String token = tok.nextToken();

			// Everything that doesn't contain at least
            // one letter is considered to be a number
            boolean isNumber = true;
            for (int j = 0; j < token.length(); j++) {
                if (Character.isLetter(token.charAt(j))) {
                    isNumber = false;
                    break;
                }
            }
            if (!isNumber) {
                resultString.append(token);
            } else {
                if (token.equals(" ") || token.equals("\t")
                        || token.equals("\n")) {
                    resultString.append(token);
                } else {
                    resultString.append(" \n ");
                }
            }
        }
        return resultString.toString();
    }

    /**
     * Converts an instance. A phrase boundary is inserted where a number is
     * found.
     */
    private void convertInstance(Instance instance) throws Exception {

        double[] instVals = new double[instance.numAttributes()];

        for (int i = 0; i < instance.numAttributes(); i++) {
            if ((!instance.attribute(i).isString()) || instance.isMissing(i)) {
                instVals[i] = instance.value(i);
            } else {

                String str = instance.stringValue(i);
                // if it is the document string only!
                if (i == 1) {
                    str = filterNumbers(str);
                }
                int index = getOutputFormat().attribute(i).addStringValue(str);
                instVals[i] = (double) index;
            }
        }
        Instance inst = new Instance(instance.weight(), instVals);
        inst.setDataset(getOutputFormat());
        push(inst);
    }

}
