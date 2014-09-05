package com.entopix.maui.util;

/**
 * Object to store input documents that are passed onto
 * MauiModelBuilder and MauiTopicExtractor
 * 
 * @author zelandiya
 *
 */
public class MauiDocument {
	
	private String fileName;
	private String filePath;
	private String textContent;
	private String topicsString;
	

	public MauiDocument(String fileName, String filePath, String textContent, String topicsString) {
		this.fileName = fileName;
		this.filePath = filePath;
		this.textContent = textContent;
		this.topicsString = topicsString;
	}
	
	public String getFileName() {
		return this.fileName;
	}
	
	public String getFilePath() {
		return this.filePath;
	}
	
	public String getTextContent() {
		return this.textContent;
	}
	
	public String getTopicsString() {
		return this.topicsString;
	}
	
	public void setTopicsString(String topicsString) {
		this.topicsString = topicsString;
	}
}
