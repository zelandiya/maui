package com.entopix.maui.stemmers;

/**
 * If selected in the stemmer option in KEA no stemming is applied.
 *
 * @author Alyona Medelyan (medelyan@gmail.com)
 *
 */
public class NoStemmer extends Stemmer {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public String stem(String str) {
        return str;
    }
}
