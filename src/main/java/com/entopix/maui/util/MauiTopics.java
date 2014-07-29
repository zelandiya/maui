package com.entopix.maui.util;

import java.util.ArrayList;
import java.util.List;


/**
 * Object for storing topics extracted from a single document.
 * @author zelandiya
 *
 */
public class MauiTopics {

	private List<Topic> topics;
	private String filePath;
	private int possibleCorrect;
	
	public MauiTopics(String filePath) {
		this.topics = new ArrayList<Topic>();
		this.filePath = filePath;
	}

	public List<Topic> getTopics() {
		return this.topics;
	}
	
	public String getFilePath() {
		return this.filePath;
	}
	
	public void addTopic(Topic topic) {
		this.topics.add(topic);
	}
	
	public void setPossibleCorrect(int possibleCorrect) {
		this.possibleCorrect = possibleCorrect;
	}
	
	public int getPossibleCorrect() {
		return this.possibleCorrect;
	}
}
