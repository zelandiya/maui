package com.entopix.maui.util;

/**
 * Object to store the individual topic extracted from a document.
 * A topic is a generic word for keyword, tag, index term, category etc.
 * 
 * @author zelandiya
 *
 */
public class Topic {

	private String title;
	private String id;
	private double probability;
	private boolean correct;
	
	public Topic(String title) {
		this.title = title;
	}
	
	public Topic(String title, String id, double probability) {
		this.title = title;
		this.probability = probability;
		this.id = id;
	}
	
	public boolean isCorrect() {
		return this.correct;
	}
	
	public void setCorrectness(boolean correct) {
		this.correct = correct;
	}
	
	public String getTitle() {
		return this.title;
	}
	
	public double getProbability() {
		return this.probability;
	}
	
	public String getId() {
		return this.id;
	}
}
