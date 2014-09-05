package com.entopix.maui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;

public class IndexerTopics {
	
	private String name;
	private HashMap<String, List<Topic>> topics;
	
	public IndexerTopics(String name) {
		this.name = name;
		this.topics = new HashMap<String, List<Topic>>();
	}
	
	public String getName() {
		return name;
	}
	
	public HashMap<String, List<Topic>> getTopics() {
		return topics;
	}

	public void addTopic(String keyFile, String mTopic) {
		if (!topics.containsKey(keyFile)) {
			return;
		}
		topics.get(keyFile).add(new Topic(mTopic));
	}

	public void addKeyFile(String keyFile) {
		topics.put(keyFile, new ArrayList<Topic>());
	}
	
}
