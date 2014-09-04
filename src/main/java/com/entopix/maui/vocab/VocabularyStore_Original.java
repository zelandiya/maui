/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.entopix.maui.vocab;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


import java.io.Externalizable;
import java.io.ObjectOutput;
import java.io.ObjectInput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nathanholmberg
 */
public class VocabularyStore_Original extends VocabularyStore implements Externalizable {

    //private static final Logger log = LoggerFactory.getLogger(VocabularyStore_Original.class);
    
    /** reverse index : id --> descriptor */
    private HashMap<String, String> idTermIndex;
    /** normalized descriptor --> list of all possible meanings */
    private HashMap<String, ArrayList<String>> listsOfSenses;
    /** non-descriptor id  --> descriptors id */
    private HashMap<String, String> nonDescriptorIndex = null;
    /** id -->  list of related ids */
    private HashMap<String, ArrayList<String>> listsOfRelatedTerms = null;
    /** id-relatedId --> relation */
    private HashMap<String, Vocabulary.Relation> relationIndex = null;


  

    public VocabularyStore_Original() {
        idTermIndex = new HashMap<String, String>();
        listsOfSenses = new HashMap<String, ArrayList<String>>();

        nonDescriptorIndex = new HashMap<String, String>();
        listsOfRelatedTerms = new HashMap<String, ArrayList<String>>();
        relationIndex = new HashMap<String, Vocabulary.Relation>();
    }

    public void addSense(String descriptor, String id) {
        ArrayList<String> ids = listsOfSenses.get(descriptor);
        if (ids == null) {
            ids = new ArrayList<String>();
        }
        ids.add(id);
        listsOfSenses.put(descriptor, ids);
    }

    public void addDescriptor(String id_string, String descriptor){
        idTermIndex.put(id_string, descriptor);
    }

    public void addNonDescriptor(String id, String nonDescriptor){
        nonDescriptorIndex.put(id, nonDescriptor);
    }

    public void addRelatedTerm(String term, String relatedTerm)
    {

        ArrayList<String> related_terms = listsOfRelatedTerms.get(term);
        if (related_terms == null) {
            related_terms = new ArrayList<String>();
        }
        related_terms.add(relatedTerm);
        listsOfRelatedTerms.put(term, related_terms);
    }

    public void addRelationship(String id_string, String name, Vocabulary.Relation rel){
        // relationIndex.put(id_string+"-"+ name, rel);
    }

    public int getNumTerms() {
        return idTermIndex.size();
    }

    public int getNumNonDescriptors() {
        return nonDescriptorIndex.size();
    }

    public int getNumRelatedTerms() {
        return listsOfRelatedTerms.size();
    }

    public ArrayList<String> getRelatedTerms(String id) {
        return listsOfRelatedTerms.get(id);
    }

    public int getNumSenses(String sense)
    {
        ArrayList<String> meanings = listsOfSenses.get(sense);
        if (meanings != null) {
            return meanings.size();
        }
        return 0;
    }

    public String getTerm(String id){
        return idTermIndex.get(id);
    }
 public ArrayList<String> getSensesForPhrase(String phrase) {

        ArrayList<String> senses = new ArrayList<String>();
        if (listsOfSenses.containsKey(phrase)) {
            ArrayList<String> list = listsOfSenses.get(phrase);
            for (String senseId : list) {
                // 1. retrieve a descriptor if this sense is a non-descriptor
                if (nonDescriptorIndex.containsKey(senseId)) {
                    senseId = nonDescriptorIndex.get(senseId);
                }
                if (!idTermIndex.containsKey(senseId)) {
                    continue;
                }
                if (!senses.contains(senseId)) {
                    // if ambiguous sense, check if there's a nonambiguous one.
                    // helps with LCSHs!
                    String nonambig = idTermIndex.get(senseId);
                    if (nonambig.indexOf('(') == -1) {
                        senses.add(senseId);
                    }
                }
            }
        }

        return senses;
    }

  public void writeExternal(ObjectOutput out) throws java.io.IOException {
        /** reverse index : id --> descriptor */
        out.writeInt(idTermIndex.size());

        for (Map.Entry<String, String> e : idTermIndex.entrySet()) {
            out.writeUTF(e.getKey());
            out.writeUTF(e.getValue());
        }


        /** normalized descriptor --> list of all possible meanings */
        out.writeInt(listsOfSenses.size());

        for (Map.Entry<String, ArrayList<String>> e : listsOfSenses.entrySet()) {
            out.writeUTF(e.getKey());
            ArrayList<String> senses = e.getValue();
            out.writeInt(senses.size());
            for (int i = 0; i < senses.size(); i++) {
                out.writeUTF(senses.get(i));
            }
        }

        /** non-descriptor id  --> descriptors id */
        out.writeInt(nonDescriptorIndex.size());

        for (Map.Entry<String, String> e : nonDescriptorIndex.entrySet()) {
            out.writeUTF(e.getKey());
            out.writeUTF(e.getValue());
        }

        /** id -->  list of related ids */
        out.writeInt(listsOfRelatedTerms.size());

        for (Map.Entry<String, ArrayList<String>> e : listsOfRelatedTerms.entrySet()) {
            out.writeUTF(e.getKey());
            ArrayList<String> relatedTerms = e.getValue();
            out.writeInt(relatedTerms.size());
            for (int i = 0; i < relatedTerms.size(); i++) {
                out.writeUTF(relatedTerms.get(i));
            }
        }

        /** id-relatedId --> relation */
        out.writeInt(relationIndex.size());

        for (Map.Entry<String, Vocabulary.Relation> e : relationIndex.entrySet()) {
            out.writeUTF(e.getKey());
            out.writeObject(e.getValue());
        }

    }

    public void readExternal(ObjectInput in) throws java.io.IOException, ClassNotFoundException {
        int size = 0;
        /** reverse index : id --> descriptor */
        size = in.readInt();
        idTermIndex = new HashMap<String, String>(size);
        for (int i = 0; i < size; i++) {
            idTermIndex.put(in.readUTF(), in.readUTF());
        }


        /** normalized descriptor --> list of all possible meanings */
        size = in.readInt();
        listsOfSenses = new HashMap<String, ArrayList<String>>(size);

        for (int i = 0; i < size; i++) {
            String sense = in.readUTF();
            int size_2 = in.readInt();
            ArrayList<String> senses = new ArrayList<String>(size_2);
            for (int j = 0; j < size_2; j++) {
                senses.add(in.readUTF());
            }
            listsOfSenses.put(sense, senses);
        }

        /** non-descriptor id  --> descriptors id */
        size = in.readInt();

        nonDescriptorIndex = new HashMap<String, String>(size);
        for (int i = 0; i < size; i++) {
            nonDescriptorIndex.put(in.readUTF(), in.readUTF());
        }

        /** id -->  list of related ids */
        size = in.readInt();

        listsOfRelatedTerms = new HashMap<String, ArrayList<String>>(size);
        for (int i = 0; i < size; i++) {
            String term = in.readUTF();
            int size_2 = in.readInt();
            ArrayList<String> relations = new ArrayList<String>(size_2);
            for (int j = 0; j < size_2; j++) {
                relations.add(in.readUTF());
            }
            listsOfRelatedTerms.put(term, relations);
        }

        /** id-relatedId --> relation */
        size = in.readInt();

        relationIndex = new HashMap<String, Vocabulary.Relation>(size);
        for (int i = 0; i < size; i++) {
            String id = in.readUTF();
            Vocabulary.Relation rel = (Vocabulary.Relation) in.readObject();
            relationIndex.put(id, rel);
        }

        finishedInitialized();
    }
}
