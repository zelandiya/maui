package com.entopix.maui.util;


import java.io.*;

/**
 * Class that implements a simple counter.
 *
 * @author Eibe Frank (eibe@cs.waikato.ac.nz)
 * @version 1.0
 */
public class Counter implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/** Integer value stored */
	private int value = 1;

	/**
	 * Initializes the counter to 1
	 */
	public Counter() {
		value = 1;
	}

	/**
	 * Initializes the counter to the given value
	 */
	public Counter(int val) {
		value = val;
	}

	/**
	 * Increments the counter.
	 */
	public void increment() {
		value++;
	}
	
	/**
	 * Increments the counter by a given value.
	 */
	public void increment(int number) {
		value += number;
	}

	/**
	 * Returns the value.
	 * @return the value
	 */
	public int value() {
		return value;
	}

	/**
	 * Returns the value as a string
	 */
	public String toString() {
		return String.valueOf(value);
	}
}
