/*
 * Created on 13.06.2005
 *
 */
package de.unigoettingen.sub.commons.resolver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
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
    public static final String CONFIGFILE = "resolver_config.xml";
    private String logoImage = "./images/SUBLogo.gif";
    private String contact = "";
    private List<LocalResolver> resolvers = null;
    private int maxThreadRuntime = 30000;

    /**
     * The constructor need the filename; the preferences are read from this
     * file using the private read method.
     *
     * @param filename
     */
    public Preferences(String filename) throws FileNotFoundException {
        if (!read(filename)) {
            logger.error("ERROR: Can't read Preference file in " + filename);
        }
    }

    /**
     * Reads the preferences from file
     *
     * @param filename
     * @return true, if reading was successful, otherwise false
     */
    private boolean read(String filename) throws FileNotFoundException {
        try {
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document xmldoc = docBuilder.parse(new File(filename));

            Node topmostelement = xmldoc.getDocumentElement();

            if (!topmostelement.getNodeName().equals("config")) {
                logger.error("ERROR reading configuration file " + filename + " - doesn't seem to be a valid configuration file");
                return false;
            }

            // get all child-elements and parse them
            NodeList allchildnodes = topmostelement.getChildNodes();

            for (int x = 0; x < allchildnodes.getLength(); x++) {
                Node singlenode = allchildnodes.item(x);

                if (singlenode.getNodeType() != Node.ELEMENT_NODE) {
                    continue; // next iteration in loop
                }
                String nodeName = singlenode.getNodeName().toLowerCase();
                if (nodeName.equals("localresolver")) {		// read list of all types, which are serials 
                    resolvers = readAllLocalResolvers(singlenode);
                } else if (nodeName.equals("maxthreadruntime")) {
                    maxThreadRuntime = Integer.parseInt(Resolver.getValueOfElement(singlenode));
                } else if (nodeName.equals("contact")) {
                    contact = Resolver.getValueOfElement(singlenode);
                } else if (nodeName.equals("logoimage")) {
                    logoImage = Resolver.getValueOfElement(singlenode);
                }

            } // end of for loop
        } catch (ParserConfigurationException pce) {
            logger.error("ERROR: couldn't parse XML file ", pce);
            return false;
        } catch (IOException ioe) {
            logger.error("ERROR: Can't open xml-file " + filename, ioe);
            return false;
        } catch (SAXException se) {
            logger.error("ERROR: SAX exception ", se);
            return false;
        }

        // check fields, which must have a value)
        if ((resolvers == null) || (resolvers.isEmpty())) {
            logger.error("Preferences: - error - No resolvers found");
            return false;
        }
        return true;
    }

    private List<LocalResolver> readAllLocalResolvers(Node inNode) {
        List<LocalResolver> localResolvers = new LinkedList<LocalResolver>();
        NodeList allnodes = inNode.getChildNodes();
        for (int i = 0; i < allnodes.getLength(); i++) {
            Node singlenode = allnodes.item(i);
            if ((singlenode.getNodeType() == Node.ELEMENT_NODE) && (singlenode.getNodeName().equals("resolver"))) {
                LocalResolver singleresolver = readSingleResolver(singlenode);
                if (singleresolver != null) {
                    localResolvers.add(singleresolver);
                } else {
                    logger.error("Error occured while reading users from preferences");
                }
            }
        }
        return localResolvers;
    }

    /**
     * @return the maxThreadRuntime
     */
    protected int getMaxThreadRuntime() {
        return maxThreadRuntime;
    }

    /**
     * @param maxThreadRuntime the maxThreadRuntime to set
     */
    protected void setMaxThreadRuntime(int maxThreadRuntime) {
        this.maxThreadRuntime = maxThreadRuntime;
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
     * Reolvers
     *
     * @return the resolvers
     */
    protected List<LocalResolver> getResolvers() {
        return resolvers;
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
            if (singlenode.getNodeType() != Node.ELEMENT_NODE) {
                //Not a Element Node, next please!
                continue; 
            }
            String nodeName = singlenode.getNodeName().toLowerCase();
            if (nodeName.equals("name")) {
                internalName = Resolver.getValueOfElement(singlenode);
            } else if (nodeName.equals("url")) {
                url = Resolver.getValueOfElement(singlenode);
            }
        }
        if ((internalName == null) || (url == null)) {
            return null;
        }
        newresolver.setName(internalName);
        newresolver.setURL(url);
        return newresolver;
    }

}
