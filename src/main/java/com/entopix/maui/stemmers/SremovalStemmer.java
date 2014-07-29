package com.entopix.maui.stemmers;

/**
 * A basic stemmer that only performs the first step of the 
 * PorterStemmer algorithm: removing of the plural endings.
 * @author olena
 *
 */

public class SremovalStemmer extends Stemmer {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public String stem(String str)  {
        // check for zero length
	if (str.length() > 3) {
	    // all characters must be letters
	    char[] c = str.toCharArray();
	    for (int i = 0; i < c.length; i++) {
		if (!Character.isLetter(c[i])) {
		    return str;
		}
	    }
	} else {            
	    return str;
	}
	str = step1a(str);
	return str;
    } // end stem

    protected String step1a (String str) {
        // SSES -> SS
        if (str.endsWith("sses")) {
            return str.substring(0, str.length() - 2);
        // IES -> I
        } else if (str.endsWith("ies")) {
            return str.substring(0, str.length() - 2);
        // SS -> S
        } else if (str.endsWith("ss")) {
            return str;
        // S ->
        } else if (str.endsWith("s")) {
            return str.substring(0, str.length() - 1);
        } else {
            return str;
        }
    } // end step1a

  } // end class
