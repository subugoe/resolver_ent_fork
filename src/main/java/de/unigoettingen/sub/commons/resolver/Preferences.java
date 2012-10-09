/*
 * Created on 13.06.2005
 *
 */
package de.unigoettingen.sub.commons.resolver;

import java.io.File;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
 *     &lt;debug&gt;0&lt;/debug&gt;
 *     &lt;maxThreadRuntime&gt;20000&lt;/maxThreadRuntime&gt;
 *     &lt;logoImage&gt;SUBLogo.gif&lt;/logoImage&gt;
 *     &lt;logfile&gt;/usr/tomcat_srv3/resolver/logs/resolver.log&lt;/logfile&gt;
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

    int debug = 0;
    String logfile = null;
    String logoImage = "SUBLogo.gif";
    LinkedList resolvers = null;
    int max_threadruntime = 30000;

    /**
     * The constructor need the filename; the preferences are read from this
     * file using the private read method.
     *
     * @param filename
     */
    public Preferences(String filename) {

        String DIRSEP = System.getProperty("file.separator");

        if (!read(filename)) {
            System.err.println("ERROR: Can't read Preference file in " + filename);
        }
    }

    /**
     * Reads the preferences from file
     *
     * @param filename
     * @return true, if reading was successful, otherwise false
     */
    private boolean read(String filename) {
        String DIRSEP = System.getProperty("file.separator");  // get seperator for directories

        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document xmldoc = docBuilder.parse(new File(filename));

            Node topmostelement = xmldoc.getDocumentElement();

            if (!topmostelement.getNodeName().equals("config")) {
                System.err.println("ERROR reading configuration file " + filename + " - doesn't seem to be a valid configuration file");
            }

            // get all child-elements and parse them
            NodeList allchildnodes = topmostelement.getChildNodes();

            for (int x = 0; x < allchildnodes.getLength(); x++) {
                Node singlenode = allchildnodes.item(x);

                if (singlenode.getNodeType() == Node.ELEMENT_NODE) {

                    if (singlenode.getNodeName().equals("localresolver")) {		// read list of all types, which are serials 
                        resolvers = readAllLocalResolvers(singlenode);
                    }

                    if (singlenode.getNodeName().equals("debug")) {
                        String debug_str = getValueOfElement(singlenode);
                        debug = Integer.parseInt(debug_str);
                    }
                    if (singlenode.getNodeName().equals("maxThreadRuntime")) {
                        String debug_str = getValueOfElement(singlenode);
                        max_threadruntime = Integer.parseInt(debug_str);
                    }
                    if (singlenode.getNodeName().equals("logfile")) {
                        logfile = getValueOfElement(singlenode);
                    }
                } else {
                    continue; // next iteration in loop
                }
            } // end of for loop
        } catch (javax.xml.parsers.ParserConfigurationException pce) {
            System.err.println("ERROR: couldn't parse XML file " + pce);
            return false;
        } catch (java.io.IOException ioe) {
            System.err.println("ERROR: Can't open xml-file " + filename);
            System.err.println(ioe);
            return false;
        } catch (org.xml.sax.SAXException se) {
            System.err.println("ERROR: SAX exception " + se);
            return false;
        }

        // check fields, which must have a value (e.h. database fields)

        if ((resolvers == null) || (resolvers.size() == 0)) {
            System.err.println("Preferences: - error - No indexes found");
            return false;
        }
        return true;
    }

    private LinkedList readAllLocalResolvers(Node inNode) {
        LinkedList localResolvers = new LinkedList();
        NodeList allnodes = inNode.getChildNodes();
        for (int i = 0; i < allnodes.getLength(); i++) {
            Node singlenode = allnodes.item(i);
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equals("resolver"))) {
                LocalResolver singleresolver = readSingleResolver(singlenode);
                if (singleresolver != null) {
                    localResolvers.add(singleresolver);
                } else {
                    System.err.println("Error occured while reading users from preferences");
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
    public int getDebug() {
        return debug;
    }

    /**
     * @param debug The debug to set.
     */
    public void setDebug(int debug) {
        this.debug = debug;
    }

    /**
     * @return Returns the logfile.
     */
    public String getLogfile() {
        return logfile;
    }

    /**
     * @param logfile The logfile to set.
     */
    public void setLogfile(String logfile) {
        this.logfile = logfile;
    }

    /**
     * @return the max_threadruntime
     */
    public int getMax_threadruntime() {
        return max_threadruntime;
    }

    /**
     * @param max_threadruntime the max_threadruntime to set
     */
    public void setMax_threadruntime(int max_threadruntime) {
        this.max_threadruntime = max_threadruntime;
    }

    /**
     * LogoImage
     *
     * @return the logoImage
     */
    public String getLogoImage() {
        return logoImage;
    }

    /**
     * @param logoImage the logoImage to set
     */
    public void setLogoImage(String logoImage) {
        this.logoImage = logoImage;
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
