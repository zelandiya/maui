package com.entopix.maui.stopwords;

/**
 *
 * @author Ignas Kukenys <ignas.kukenys@gmail.com>
 */
public class StopwordsFactory {
    
    public static Stopwords makeStopwords(String languageCode) {
        
        languageCode = languageCode.toLowerCase();
        
        if (languageCode.equals("de")) {
            return new StopwordsGerman();
            
        } else if (languageCode.equals("en")) {
            return new StopwordsEnglish();
            
        } else if (languageCode.equals("es")) {
            return new StopwordsSpanish();
            
        } else if (languageCode.equals("fr")) {
            return new StopwordsFrench();
            
        } else {
            throw new RuntimeException("Unsupported language code: use one of: de, en, es, fr..");
        }
    }
    
}
