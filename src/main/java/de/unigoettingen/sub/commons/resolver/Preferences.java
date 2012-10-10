/*
 * Created on 13.06.2005
 *
 */
package de.unigoettingen.sub.commons.resolver;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class reads the preference file and parses its XML-structure. The
 * preference file contains all the LPIRs (LocalPersisitentIdentifierResolver)
 * with their name and URL.
 *
 * The file must have the following syntax:
 * <pre>
 *
 * &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 * &lt;!-- -configuration file for global SUB resolver --&gt;
 * &lt;config&gt;
 *     &lt;maxThreadRuntime&gt;20000&lt;/maxThreadRuntime&gt;
 *     &lt;logoImage&gt;SUBLogo.gif&lt;/logoImage&gt;
 *     &lt;contact&gt;user@host.com&lt;/contact&gt;
 *     &lt;localresolver&gt;
 *         &lt;!-- define a single resolver; --&gt;
 *         &lt;resolver&gt;
 *             &lt;name&gt;Göttingen Digitalisierungszentrum&lt;/name&gt;
 *             &lt;url&gt;http://dz-srv1.sub.uni-goettingen.de/cgi-bin/digbib.cgi?xml&amp;&lt;/url&gt;
 *        &lt;/resolver&gt;
 *     &lt;/localresolver&gt;
 * &lt;/config&gt;
 *
 *  </pre>
 *
 * @author Enders
 *
 */
public class Preferences {

    static Logger logger = Logger.getLogger(Resolver.class.getName());
    //int debug = 0;
    //String logfile = null;
    String logoImage = "SUBLogo.gif";
    String contact = "";
    LinkedList<LocalResolver> resolvers = null;
    int max_threadruntime = 30000;
    String DIRSEP;

    /**
     * The constructor need the filename; the preferences are read from this
     * file using the private read method.
     *
     * @param filename
     */
    public Preferences(String filename) {

        DIRSEP = System.getProperty("file.separator");

        if (!read(filename)) {
            logger.error("ERROR: Can't read Preference file in " + filename);
            /*
            System.err.println("ERROR: Can't read Preference file in " + filename);
            */
        }
    }

    /**
     * Reads the preferences from file
     *
     * @param filename
     * @return true, if reading was successful, otherwise false
     */
    private boolean read(String filename) {
        //String DIRSEP = System.getProperty("file.separator");  // get seperator for directories

        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document xmldoc = docBuilder.parse(new File(filename));

            Node topmostelement = xmldoc.getDocumentElement();

            if (!topmostelement.getNodeName().equals("config")) {
                logger.error("ERROR reading configuration file " + filename + " - doesn't seem to be a valid configuration file");
                /*
                System.err.println("ERROR reading configuration file " + filename + " - doesn't seem to be a valid configuration file");
                */
            }

            // get all child-elements and parse them
            NodeList allchildnodes = topmostelement.getChildNodes();

            for (int x = 0; x < allchildnodes.getLength(); x++) {
                Node singlenode = allchildnodes.item(x);

                if (singlenode.getNodeType() == Node.ELEMENT_NODE) {

                    if (singlenode.getNodeName().equals("localresolver")) {		// read list of all types, which are serials 
                        resolvers = readAllLocalResolvers(singlenode);
                    }
                    /*
                    if (singlenode.getNodeName().equals("debug")) {
                        String debug_str = getValueOfElement(singlenode);
                        debug = Integer.parseInt(debug_str);
                    }
                    */
                    if (singlenode.getNodeName().equals("maxThreadRuntime")) {
                        String debug_str = getValueOfElement(singlenode);
                        max_threadruntime = Integer.parseInt(debug_str);
                    }
                    /*
                    if (singlenode.getNodeName().equals("logfile")) {
                        logfile = getValueOfElement(singlenode);
                    }
                    */
                    if (singlenode.getNodeName().equals("contact")) {
                        contact = getValueOfElement(singlenode);
                    }
                    if (singlenode.getNodeName().equals("logoImage")) {
                        logoImage = getValueOfElement(singlenode);
                    }
                } else {
                    continue; // next iteration in loop
                }
            } // end of for loop
        } catch (ParserConfigurationException pce) {
            logger.error("ERROR: couldn't parse XML file ", pce);
            /*
            System.err.println("ERROR: couldn't parse XML file " + pce);
            */
            return false;
        } catch (IOException ioe) {
            logger.error("ERROR: Can't open xml-file " + filename, ioe);
            /*
            System.err.println("ERROR: Can't open xml-file " + filename);
            System.err.println(ioe);
            */
            return false;
        } catch (SAXException se) {
            logger.error("ERROR: SAX exception ", se);
            /*
            System.err.println("ERROR: SAX exception " + se);
            */
            return false;
        }

        // check fields, which must have a value (e.h. database fields)

        if ((resolvers == null) || (resolvers.size() == 0)) {
            logger.error("Preferences: - error - No indexes found");
            /*
            System.err.println("Preferences: - error - No indexes found");
            */
            return false;
        }
        return true;
    }

    private LinkedList<LocalResolver> readAllLocalResolvers(Node inNode) {
        LinkedList<LocalResolver> localResolvers = new LinkedList<LocalResolver>();
        NodeList allnodes = inNode.getChildNodes();
        for (int i = 0; i < allnodes.getLength(); i++) {
            Node singlenode = allnodes.item(i);
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equals("resolver"))) {
                LocalResolver singleresolver = readSingleResolver(singlenode);
                if (singleresolver != null) {
                    localResolvers.add(singleresolver);
                } else {
                    logger.error("Error occured while reading users from preferences");
                    /*
                    System.err.println("Error occured while reading users from preferences");
                    */
                }
            }
        }
        return localResolvers;
    }

    /**
     * get value of <debug> element; this value set's the log level for
     * debugging purposes.
     *
     * @return Returns the debug.
     */
    /*
    private int getDebug() {
        return debug;
    }
    */

    /**
     * @param debug The debug to set.
     */
    /*
    private void setDebug(int debug) {
        this.debug = debug;
    }
    */

    /**
     * @return Returns the logfile.
     */
    /*
    protected String getLogfile() {
        return logfile;
    }
    */

    /**
     * @param logfile The logfile to set.
     */
    /*
    protected void setLogfile(String logfile) {
        this.logfile = logfile;
    }
    */

    /**
     * @return the max_threadruntime
     */
    protected int getMax_threadruntime() {
        return max_threadruntime;
    }

    /**
     * @param max_threadruntime the max_threadruntime to set
     */
    protected void setMax_threadruntime(int max_threadruntime) {
        this.max_threadruntime = max_threadruntime;
    }

    /**
     * LogoImage
     *
     * @return the logoImage
     */
    protected String getLogoImage() {
        return logoImage;
    }

    /**
     * @param logoImage the logoImage to set
     */
    protected void setLogoImage(String logoImage) {
        this.logoImage = logoImage;
    }
    
    /**
     * Contact
     *
     * @return the contact
     */
    protected String getContact() {
        return contact;
    }

    /**
     * @param contact the contsct to set
     */
    protected void setContact(String contact) {
        this.contact = contact;
    }

    /**
     * Reads the data for a single LPIR (local resolver)
     *
     * @param inNode
     * @return a LocalResolver instanciated from the information in the
     * preferences file.
     */
    private LocalResolver readSingleResolver(Node inNode) {
        NodeList allnodes = inNode.getChildNodes();
        LocalResolver newresolver = new LocalResolver();
        String internalName = null;
        String url = null;
        for (int i = 0; i < allnodes.getLength(); i++) {
            Node singlenode = allnodes.item(i);
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equals("name"))) {
                internalName = getValueOfElement(singlenode);
            }
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equals("url"))) {
                url = getValueOfElement(singlenode);
            }
        }
        if ((internalName == null) || (url == null)) {
            return null;
        }
        newresolver.setName(internalName);
        newresolver.setURL(url);
        return newresolver;
    }

    /**
     * Searches for the value of an XML-element. The value of an element is
     * stored in a text node, which is the child of an element node. So we are
     * looking for any direct childnodes and check, if these nodes a textnodes.
     *
     * @param inNode
     * @return String or null, if node has no text-node with contents as a child
     * node.
     *
     */
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
}
