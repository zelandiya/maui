package com.entopix.maui.stemmers;

/**
 * Wrapper for the Snowball stemmer for Spanish // use stemSB Or translation of
 * the Stemmer implemented in C i found here:
 * http://members.unine.ch/jacques.savoy/clef/index.html
 *
 * @author Olena Medelyan
 */
public class SpanishStemmer extends Stemmer {

    private static final long serialVersionUID = 1L;

    private SpanishStemmerSB stemmer = new SpanishStemmerSB();

    public String stemSB(String str) {
        stemmer.setCurrent(str);
        stemmer.stem();
        return stemmer.getCurrent();
    }

    /*  Spanish stemmer tring to remove inflectional suffixes */
    public String stem(String word) {

        int len = word.length() - 1;

        if (len > 3) {

            word = removeSpanishAccent(word);

            if (word.endsWith("eses")) {
                //  corteses -> cortÈs
                word = word.substring(0, len - 1);
                return word;
            }

            if (word.endsWith("ces")) {
                //  dos veces -> una vez
                word = word.substring(0, len - 2);
                word = word + 'z';
                return word;
            }

            if (word.endsWith("os") || word.endsWith("as") || word.endsWith("es")) {
                //  ending with -os, -as  or -es
                word = word.substring(0, len - 1);
                return word;

            }
            if (word.endsWith("o") || word.endsWith("a") || word.endsWith("e")) {
                //  ending with  -o,  -a, or -e
                word = word.substring(0, len - 1);
                return word;
            }

        }
        return word;
    }

    private String removeSpanishAccent(String word) {
        word = word.replaceAll("‡|·|‚|‰", "a");
        word = word.replaceAll("Ú|Û|Ù|ˆ", "o");
        word = word.replaceAll("Ë|È|Í|Î", "e");
        word = word.replaceAll("˘|˙|˚|¸", "a");
        word = word.replaceAll("Ï|Ì|Ó|Ô", "a");

        return word;
    }

    /**
     * The main method. // for testing
     */
    public static void main(String[] ops) {

        SpanishStemmer s = new SpanishStemmer();
        System.out.println(s.stem("veces"));
    }

}
