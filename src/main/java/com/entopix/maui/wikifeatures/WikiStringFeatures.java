package com.entopix.maui.wikifeatures;

public class WikiStringFeatures {
	public float keyphrasness;
	public int generality;
	public int number_in_links;
	
	public boolean equals(Object obj){
		 if ( this == obj ) return true;
		  if ( !(obj instanceof WikiStringFeatures) ) return false;
		  WikiStringFeatures wsf_obj = (WikiStringFeatures)obj;
		  return this.keyphrasness==wsf_obj.keyphrasness &&
				  this.generality==wsf_obj.generality &&
						  this.number_in_links==wsf_obj.number_in_links ;
		  }

}
