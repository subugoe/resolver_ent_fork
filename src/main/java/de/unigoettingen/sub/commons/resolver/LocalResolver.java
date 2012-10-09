/*
 * Created on 13.06.2005
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Style - Code Templates
 */
package de.unigoettingen.sub.commons.resolver;

/**
 *
 * Just a simple data structure to store information about a local resolver (LPIR). The local
 * resolver consists only of a name and it's URL. This URL is used to send an HTTP request 
 * to the local resolver. For this purpose the URL is extended with the persistent identifier.
 * 
 * @author enders
 */
public class LocalResolver {
	
	String url;
	String name;
	
	public LocalResolver(){
		
	}
	
	/**
	 * sets the name of the LocalResolver
	 * @param in
	 */
	public void setName(String in){
		name=in;
	}
	
	/**
	 * Sets the URL of the LocalResolver (LPIR) - this is the URL being used for HTTP
	 * connection to the LPIR. This URL is extended with the requested persistent identifier (PI)
	 * by the LocalResolverConnectorThread.
	 * @param in
	 */
	public void setURL(String in){
		url=in;
	}
	
	/**
	 * Retrieves the name of a LocalResolver instance
	 * @return the name as a String
	 */
	public String getName(){
		return name;
	}
	
	/**
	 * retrieves the URL
	 * @return url
	 */
	public String getURL(){
		return url;
	}
}
