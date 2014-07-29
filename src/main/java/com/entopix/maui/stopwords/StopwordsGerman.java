package com.entopix.maui.stopwords;

/**
 * Class that can test whether a given string is a stop word. Lowercases all
 * words before the test.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class StopwordsGerman extends Stopwords {

    private static final long serialVersionUID = 1L;
    
    /**
     * Default constructor uses a static stopword list
     */
    public StopwordsGerman() {
        // http://snowball.tartarus.org/german/stop.txt
        super(StopwordsStatic.GERMAN);
    }

    public StopwordsGerman(String filePath) {
        super(filePath);
    }
    
    @Override
    public boolean isStopword(String word) {
        // make sure word is in lowercase
        return super.isStopword(word.toLowerCase());
    }
}
