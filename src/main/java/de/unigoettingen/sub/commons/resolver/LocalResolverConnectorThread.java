/*
 * Created on 13.06.2005
 *
 */
package de.unigoettingen.sub.commons.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class is a thread which connects to a single LPIR (local resolver) and
 * is created by the
 * <pre>Resolver</pre> class. For every single LPIR an appropriate
 * <pre>LocalResolverConnectorThread</pre> instance is created. In its
 * run-method the Http-request is build.<p> The result from the HTTP-request is
 * parsed (getResponse-method). As a result of the thread the retrieved URLs
 * should be retrievable by using the
 * <pre>getResponses</pre> method.
 *
 * @author enders
 */
public class LocalResolverConnectorThread extends Thread {

    static Logger logger = Logger.getLogger(Resolver.class.getName());
    //boolean running = false;
    Preferences myPrefs;
    String url;
    int timeout;
    //boolean finished = false;
    LinkedList allURLs;
    String localresolverurl = null; // URL or the local resolver to connect for resolution

    public LocalResolverConnectorThread(Preferences inPrefs, String inUrl, int inTimeout) {
        //running = true;
        myPrefs = inPrefs;
        url = inUrl;
        timeout = inTimeout;
        localresolverurl = inUrl;
    }

    /**
     * The
     * <pre>run</pre> method is started as soon as the thread starts. It will
     * can only end, if <ul> <li>the request has been carried out successfully
     * and the answer is parsed</li> <li>an error occured - no matter, if a
     * connection error or an XML-parsing error of the response</li> <li>The
     * calling class (
     * <pre>Resolver</pre> interrupts the execution; usually if a timeout is
     * assumed. This timeout can be set using the
     * <pre>&lt;maxThreadRuntime&gt;</pre> element in the preferences file</li>
     * </ul>
     */
    @Override
    public synchronized void run() {

        // create client
        HttpClient client = new HttpClient();
        HttpMethod method = new GetMethod(url);

        client.setTimeout(timeout); // sets timeout in milliseconds

        try {
            int statusCode = client.executeMethod(method);

            // error checking
            if (statusCode != HttpStatus.SC_OK) {
                logger.warn("SUBResolver: Method failed: " + method.getStatusLine() +  " for URL:" + url);
                /*
                writeLog("SUBResolver: Method failed: " + method.getStatusLine() +  " for URL:" + url);
            
                */ 
            }

            InputStream responseStream = method.getResponseBodyAsStream();
            allURLs = getResponse(responseStream);

        } catch (HttpException e) {
            //Exception ex = (Exception) e.getCause();
            logger.error("HTTP Method failed: ", e);
            /*
            System.out.println(e.getMessage());
            if (ex != null) {
                System.out.println(" : " + ex);
                ex.printStackTrace();
            }
            */
        } catch (IOException e) {
            logger.error("SUBResolver: Fatal transport error: " + e.getMessage() + "\n             url:" + url, e);
            /*
            writeLog("SUBResolver: Fatal transport error: " + e.getMessage() + "\n             url:" + url);
            if (myPrefs.debug > 1) {
                e.printStackTrace();
            }
            */ 
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
        //finished = true;
    }
    /*
    public boolean isFinished() {
        return finished;
    }
    */

    /**
     * Retrieves the result of the connection request; if no URLs are available
     * null is returned.
     *
     * @return a LinkedList containing <pre>ResolvedURL</pre> objects.
     */
    public LinkedList getResponses() {
        return allURLs;
    }

    /**
     * Parses the response and calls
     * <pre>readPURL</pre> method, for every
     * <pre>resolvedLPIs</pre> block in the response
     *
     * @param responseStream
     * @return LinkedList containing <pre>ResolvedURL</pre> objects
     */
    private LinkedList getResponse(InputStream responseStream) {

        LinkedList allURL = new LinkedList();

        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

            Document xmldoc = docBuilder.parse(responseStream);
            //Document xmldoc=docBuilder.parse(originalContent.toString());
            Node topmostelement = xmldoc.getDocumentElement();

            if (!topmostelement.getNodeName().equals("response")) {
                logger.error("SUBResolver: ERROR: xml answer doesn't match DTD for URL:" + url);
                /*
                writeLog("SUBResolver: ERROR: xml answer doesn't match DTD for URL:" + url);
                
                */ 
                return null;
            }
            // get all child-elements and parse them
            NodeList allchildnodes = topmostelement.getChildNodes();

            for (int x = 0; x < allchildnodes.getLength(); x++) {
                Node singlenode = allchildnodes.item(x);

                if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equals("header"))) {
                }

                if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && ((singlenode.getNodeName().equalsIgnoreCase("resolvedPURLs")) || (singlenode.getNodeName().equalsIgnoreCase("resolvedLPIs")))) {
                    ResolvedURL ru = readPURL(singlenode);
                    if (ru != null) {
                        allURL.add(ru);
                    }
                }

            }

        } catch (ParserConfigurationException pce) {
            logger.error("SUBResolver: ERROR: couldn't parse XML file occured while receiving response for url: " + url, pce);
            /*
            writeLog("SUBResolver: ERROR: couldn't parse XML file " + pce);
            writeLog("             occured while receiving response for url: " + url);
            */
            return null;
        } catch (IOException ioe) {
            logger.error("SUBResolver: ERROR: no content delivered occured while receiving response for url: " + url, ioe);
            /*
            writeLog("SUBResolver: ERROR: no content delivered");
            writeLog("             occured while receiving response for url: " + url);
            */
            return null;
        } catch (SAXException se) {
            logger.error("SUBResolver: ERROR: SAX exception occured while receiving response for url: " + url, se);
            /*
            writeLog("SUBResolver: ERROR: SAX exception " + se);
            writeLog("             occured while receiving response for url: " + url);
            */
            return null;
        }
        return allURL;
    }

    /**
     * Parses the DOM-tree of the response for every
     * <pre>resolvedLPIs</pre> block. This block is interpreted and a new
     * <pre>ResolvedURL</pre> object is created for a valid block.
     *
     * @param inNode DOM-Node of the  <pre>resolvedLPIs</pre> element
     * @return ResolvedURL instance; or null if the response was invalid
     */
    private ResolvedURL readPURL(Node inNode) {

        String purl = null;
        String access = null;
        String service = null;
        String servicehome = null;
        String version = null;
        String resolverurl = null; // URL in the repository; local URL

        Node purlnode = null;

        NodeList allchildnodes = inNode.getChildNodes();
        for (int x = 0; x < allchildnodes.getLength(); x++) {
            Node singlenode = allchildnodes.item(x);
            // this is for version 0.1
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equalsIgnoreCase("PURL"))) {
                purlnode = singlenode;
                break; // get out of loop
            }
            // this is for version 0.2
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equalsIgnoreCase("LPI"))) {
                purlnode = singlenode;
                break; // get out of loop
            }
        }

        if (purlnode == null) {
            // no node with name PURL found
            logger.warn("PURL/LPI node NOT found for URL:" + localresolverurl);
            /*
            if (myPrefs.debug > 0) {
                writeLog("PURL/LPI node NOT found for URL:" + localresolverurl);
            }
            */
            return null;
        }

        allchildnodes = purlnode.getChildNodes();

        // iterate over all children
        for (int x = 0; x < allchildnodes.getLength(); x++) {
            Node singlenode = allchildnodes.item(x);
            // this is vor version 0.1
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equalsIgnoreCase("requestedPURL"))) {
                purl = getValueOfElement(singlenode);
            }
            // this is for version 0.2
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equalsIgnoreCase("requestedLPI"))) {
                purl = getValueOfElement(singlenode);
            }
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equalsIgnoreCase("service"))) {
                service = getValueOfElement(singlenode);
            }
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equalsIgnoreCase("servicehome"))) {
                servicehome = getValueOfElement(singlenode);
            }
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equalsIgnoreCase("url"))) {
                resolverurl = getValueOfElement(singlenode);
            }
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equalsIgnoreCase("version"))) {
                version = getValueOfElement(singlenode);
            }
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equalsIgnoreCase("access"))) {
                access = getValueOfElement(singlenode);
            }
        }

        if ((purl != null) && (resolverurl != null) && (service != null)) {
            // valid answer, all required fields are available
            ResolvedURL ru = new ResolvedURL();
            if (purl != null) {
                ru.setPurl(purl);
            }
            if (resolverurl != null) {
                ru.setUrl(resolverurl);
            }
            if (service != null) {
                ru.setService(service);
            }
            if (servicehome != null) {
                ru.setServicehome(servicehome);
            }
            if (access != null) {
                ru.setAccess(access);
            }
            if (version != null) {
                ru.setVersion(version);
            }
            logger.info("SUBResolver: response from " + localresolverurl + " is: local URL:" + resolverurl);
            /*
            if (myPrefs.debug > 5) {
                writeLog("SUBResolver: response from " + localresolverurl + " is: local URL:" + resolverurl);
            }
            */
            return ru;

        } else {
            // not an appropriate answer
            logger.warn("SUBResolver: response from " + localresolverurl + " is empty or invalid");
            /*
            if (myPrefs.debug > 0) {
                writeLog("SUBResolver: response from " + localresolverurl + " is empty or invalid");
            }
            */
            return null;
        }
    }

    private String getValueOfElement(Node inNode) {
        NodeList childnodes = inNode.getChildNodes();

        for (int i = 0; i < childnodes.getLength(); i++) {
            Node singlenode = childnodes.item(i);
            if (singlenode.getNodeType() == Node.TEXT_NODE) {
                String value = singlenode.getNodeValue();
                return value;
            }
        }
        return null;
    }

    //TODO: Use a Logger for this
    /*
    private void writeLog(String inMessage) {
        if (myPrefs.getLogfile() != null) {
            // open logfile and write
            try {
                BufferedWriter out = new BufferedWriter(new FileWriter(myPrefs.getLogfile(), true));
                out.write(inMessage + "\n");
                out.close();
            } catch (IOException e) {
                System.err.println("Resolver: ERROR occured while writing logfile to " + myPrefs.getLogfile());
                System.err.println("Information which should be written to logfile was\n" + inMessage);
            }
        } else {
            System.out.println("Resolver-Log:" + inMessage);
        }
    }
    */
}
