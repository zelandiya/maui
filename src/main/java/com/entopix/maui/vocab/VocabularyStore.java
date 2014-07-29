package com.entopix.maui.vocab;


import java.util.ArrayList;

/**
 *
 * @author nathanholmberg
 */
public abstract class VocabularyStore {

    protected boolean initialized = false;

    protected boolean wants_serialization = true;

    protected String vocabularyName = "";

    public boolean isInitialized(){ return initialized; }

    public void finishedInitialized(){ initialized = true; }

    public String getFormatedName( String in )
    {
        return in;
    }

    public void setVocabularyName( String name )
    {
        vocabularyName = name;
    }

    public boolean getWantsSerialization()
    {
        return wants_serialization;
    }

    public abstract void addSense( String descriptor, String id );

    public abstract void addDescriptor( String id_string, String descriptor );

    public abstract void addNonDescriptor(String id, String nonDescriptor);

    public abstract void addRelatedTerm( String term, String relatedTerm );

    public abstract void addRelationship( String id_string, String name, Vocabulary.Relation rel);

    public abstract int getNumTerms();

    public abstract int getNumNonDescriptors();

    public abstract int getNumRelatedTerms();

    public abstract ArrayList<String> getRelatedTerms(String id);

    public abstract int getNumSenses( String sense );

    public abstract String getTerm(String id);

    public abstract ArrayList<String> getSensesForPhrase( String phrase );
}
