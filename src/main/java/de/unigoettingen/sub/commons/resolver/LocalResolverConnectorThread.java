/*
 * Created on 13.06.2005
 *
 */
package de.unigoettingen.sub.commons.resolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
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
    private String url;
    private int timeout;
    private List<ResolvedURL> allURLs;
    private String localresolverurl = null; // URL or the local resolver to connect for resolution

    public LocalResolverConnectorThread(String inUrl, int inTimeout) {
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
                logger.warn("SUBResolver: Method failed: " + method.getStatusLine() + " for URL:" + url);
            }

            InputStream responseStream = method.getResponseBodyAsStream();
            allURLs = getResponse(responseStream);

        } catch (HttpException e) {
            logger.error("HTTP Method failed: ", e);
        } catch (IOException e) {
            logger.error("SUBResolver: Fatal transport error: " + e.getMessage() + "\n             url:" + url, e);
        } finally {
            // Release the connection.
            method.releaseConnection();
        }
        //finished = true;
    }

    /**
     * Retrieves the result of the connection request; if no URLs are available
     * null is returned.
     *
     * @return a LinkedList containing <pre>ResolvedURL</pre> objects.
     */
    public List<ResolvedURL> getResponses() {
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
    private List<ResolvedURL> getResponse(InputStream responseStream) {

        List<ResolvedURL> allURL = new LinkedList();

        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();

            Document xmldoc = docBuilder.parse(responseStream);
            Node topmostelement = xmldoc.getDocumentElement();

            if (!topmostelement.getNodeName().equals("response")) {
                logger.error("SUBResolver: ERROR: xml answer doesn't match DTD for URL:" + url);
                return null;
            }
            // get all child-elements and parse them
            NodeList allchildnodes = topmostelement.getChildNodes();

            for (int x = 0; x < allchildnodes.getLength(); x++) {
                Node singlenode = allchildnodes.item(x);

                /*
                 if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equals("header"))) {
                 }
                 */

                if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && ((singlenode.getNodeName().equalsIgnoreCase("resolvedPURLs")) || (singlenode.getNodeName().equalsIgnoreCase("resolvedLPIs")))) {
                    ResolvedURL ru = readPURL(singlenode);
                    if (ru != null) {
                        allURL.add(ru);
                    }
                }

            }

        } catch (ParserConfigurationException pce) {
            logger.error("SUBResolver: ERROR: couldn't parse XML file occured while receiving response for url: " + url, pce);
            return null;
        } catch (IOException ioe) {
            logger.error("SUBResolver: ERROR: no content delivered occured while receiving response for url: " + url, ioe);
            return null;
        } catch (SAXException se) {
            logger.error("SUBResolver: ERROR: SAX exception occured while receiving response for url: " + url, se);
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
            if (singlenode.getNodeType() != Node.ELEMENT_NODE) {
                //Not a Element Node
                continue; 
            }
            
            // this is for version 0.1
            if (singlenode.getNodeName().equalsIgnoreCase("PURL")) {
                purlnode = singlenode;
                break; // get out of loop
            }
            // this is for version 0.2
            if (singlenode.getNodeName().equalsIgnoreCase("LPI")) {
                purlnode = singlenode;
                break; // get out of loop
            }
        }

        if (purlnode == null) {
            // no node with name PURL found
            logger.warn("PURL/LPI node NOT found for URL:" + localresolverurl);
            return null;
        }

        allchildnodes = purlnode.getChildNodes();

        // iterate over all children
        for (int x = 0; x < allchildnodes.getLength(); x++) {
            Node singlenode = allchildnodes.item(x);
            if (singlenode.getNodeType() != Node.ELEMENT_NODE) {
                //Not a Element Node
                continue; 
            }
            
            // this is vor version 0.1
            if (singlenode.getNodeName().equalsIgnoreCase("requestedPURL")) {
                purl = getValueOfElement(singlenode);
            }
            // this is for version 0.2
            if (singlenode.getNodeName().equalsIgnoreCase("requestedLPI")) {
                purl = getValueOfElement(singlenode);
            }
            if (singlenode.getNodeName().equalsIgnoreCase("service")) {
                service = getValueOfElement(singlenode);
            }
            if (singlenode.getNodeName().equalsIgnoreCase("servicehome")) {
                servicehome = getValueOfElement(singlenode);
            }
            if (singlenode.getNodeName().equalsIgnoreCase("url")) {
                resolverurl = getValueOfElement(singlenode);
            }
            if (singlenode.getNodeName().equalsIgnoreCase("version")) {
                version = getValueOfElement(singlenode);
            }
            if (singlenode.getNodeName().equalsIgnoreCase("access")) {
                access = getValueOfElement(singlenode);
            }
        }

        if ((purl != null) && (resolverurl != null) && (service != null)) {
            // valid answer, all required fields are available
            ResolvedURL ru = new ResolvedURL();
            ru.setPurl(purl);
            ru.setUrl(resolverurl);
            ru.setService(service);
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
            return ru;

        } else {
            // not an appropriate answer
            logger.warn("SUBResolver: response from " + localresolverurl + " is empty or invalid");
            return null;
        }
    }

    private String getValueOfElement(Node inNode) {
        NodeList childnodes = inNode.getChildNodes();

        for (int i = 0; i < childnodes.getLength(); i++) {
            Node singlenode = childnodes.item(i);
            if (singlenode.getNodeType() == Node.TEXT_NODE) {
                return singlenode.getNodeValue();
            }
        }
        return null;
    }

    /**
     * url
     *
     * @return the url
     */
    public String getUrl() {
        return url;
    }
}
