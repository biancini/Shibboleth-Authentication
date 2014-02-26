package it.infn.mib.shibboleth.jaas.impl;

/**
 * Exception class.
 * 
 * @version 1.0, 06/06/12
 */
public class HTTPException extends Exception {
	
	private String message = null;
	private Exception e = null;
	private String exceptionClass = null;
	
	/**
	 * @serial
	 */
	private static final long serialVersionUID = 458120052035549838L;
	
	/**
	 * Constructor with the message for the exception.
	 * 
	 * @param message the message describing the error.
	 */
	public HTTPException(String message) {
		this.message = message;
	}
	
	/**
	 * Constructor with the message, the original exception and its class for the exception.
	 * 
	 * @param message the message describing the error
	 * @param e the original exception
	 */
	public HTTPException(String message, Exception e) {
		this.message = message;
		this.e = e;
	}
	
	/**
	 * Constructor with the message, the original exception and its class for the exception.
	 * 
	 * @param message the message describing the error
	 * @param e the original exception
	 * @param exceptionClass the class of the original exception
	 */
	public HTTPException(String message, Exception e, String exceptionClass) {
		this.message = message;
		this.e = e;
		this.exceptionClass = exceptionClass;
	}
	
	/**
	 * toString method describing the error class to be printed to user or in logs.
	 * 
	 * @return a <code>String</code> describing the error for the user
	 */
	public String toString() {
		String description = "HTTPException while authenticating the user.";
		description += "Error message: " + this.message;
		if (this.e != null && this.exceptionClass != null) description += "Original exception (" + this.exceptionClass + "): " + this.e.toString();
		else if (this.e != null) description += "Original exception: " + this.e.toString();
		return description;
	}
}
