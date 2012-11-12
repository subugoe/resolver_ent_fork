/*
 * Created on 13.06.2005
 *
 */
package de.unigoettingen.sub.commons.resolver;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import java.util.LinkedList;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Servlet receives a URL with an identifier and asks different local identifier
 * resolving systems connected to indiviual document management systems if they
 * have a document with this identifier. Communication is done using simple HTTP
 * requests with an XML-based replay.<p/>
 * If only one answer is received, the origin request is redirected; otherwise a
 * HTML output is generated for the user to chose the appropriate target.
 * <p/>
 * example URLs could be:<br/>
 * http://resolver.sub.uni-goettingen.de/purl/?PPN235181684_0006
 *
 * @author enders
 */
public class Resolver extends HttpServlet {

    static Logger logger = Logger.getLogger(Resolver.class.getName());
    private static final long serialVersionUID = 0022001;
    private Preferences myPrefs = null;
    public static final String VERSION = "version 0.3";
    private static final String CONTENT_TYPE = "text/html";
    private static final String HTML_START = "<html><head>";
    private static final String TITLE = "<title>SUB resolver</title>";
    private static final String HEAD_BODY = "</head><body>";
    private static final String HTML_END = "</head><body>";

    /**
     * Handles the servlet get-request.
     *
     * @param request contains the parameters; there must only be a single
     * containing the identifier; e.g. http://....../purl?PPN12345678
     * @param response used for getting an output stream
     */
    @Override
    public void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        LinkedList<LocalResolverConnectorThread> allThreads = new LinkedList<LocalResolverConnectorThread>();

        //	 get parameters
        logger.info("SUBResolver: received a request");

        ArrayList<String> params = Collections.list(request.getParameterNames());

        for (String p : params) {
            logger.debug("SUBResolver: parameter=" + p);
        }

        if (params.isEmpty()) {
            // error handling; no parameter/identifier given
            logger.warn("SUBResolver: didn't receive a parameter");
            return;
        } else if (params.size() > 1) {
            // invalid request
            logger.warn("SUBResolver: wrong number of parameters");
            return;
        }
        String parameter = params.get(0);

        // just ask all LocalResolver 
        // every connection in done in a seperate thread
        for (LocalResolver lr : myPrefs.getResolvers()) {
            String url = lr.getURL();
            logger.info("SUBResolver: url:" + url + parameter);
            LocalResolverConnectorThread rt = new LocalResolverConnectorThread(url + parameter, myPrefs.getMaxThreadRuntime());
            // create a new thread
            rt.start();   // start thread
            allThreads.add(rt); // add thread to groups of threads

        }

        // checking, if threads are still running
        for (LocalResolverConnectorThread t : allThreads) {
            try {
                t.join(myPrefs.getMaxThreadRuntime());   // just wait max. 20 seconds until thread must be finished
            } catch (InterruptedException e) {
                // thread was interrupted
                response.setContentType("text/html");
                showHTML_Error(response);
                return;
            }
        }

        LinkedList<ResolvedURL> answeredRequest = new LinkedList<ResolvedURL>();
        for (LocalResolverConnectorThread t : allThreads) {
            if (t.isAlive()) {
                // thread is finished
                logger.info("SUBResolver: " + t.getUrl() + " thread is still running");
            }
            {
                // thread is not finsihed, so we won't have any answer
                if (t.getResponses() != null && (t.getResponses().size() > 0)) {
                    for (ResolvedURL ru : t.getResponses()) {
                        logger.debug("XML Response:\n" + dumpResolvedUrl(ru));
                        answeredRequest.add(ru);
                    }
                } // endif
            }

        }

        // just one hit; do a redirect
        if (answeredRequest.size() == 1) {
            response.setStatus(307); // temporary redirect; avoid caching
            response.setHeader("Location", answeredRequest.get(0).getUrl());
            return;
        }


        // set HTTP header - since HTML is comming after this point
        response.setContentType(CONTENT_TYPE);
        // output the result
        // if only one result is available

        PrintWriter webout = response.getWriter(); // get stream;

        // No hit is available
        if (answeredRequest.size() == 0) {
            // no result
            logger.info("SUBResolver: sorry, no result");
            showHTML_NoHits(webout);
            return;
        }


        if (answeredRequest.size() > 0) {
            // just output debug information
            for (ResolvedURL ru : answeredRequest) {
                logger.debug("checking answer" + dumpResolvedUrl(ru));
            }

            // output HTML
            webout.println(HTML_START + printCSSLink() + TITLE + HEAD_BODY + showHeader()
                    + "<center><table width=\"600\"><tr><td>"
                    + "The document you requested " + request.getRequestURL() + "?" + parameter
                    + " is available at:<br/>");

            for (ResolvedURL ru : answeredRequest) {
                webout.println("<h4><a href=\"" + ru.getServicehome() + "\">"
                        + ru.getService() + "</a></h4>" + "go to document:&nbsp;"
                        + "<a href=\"" + ru.getUrl() + "\">" + ru.getUrl() + "</a>"
                        + "<br/>");
            }
            webout.println("</td></tr></table>" + showFooter() + HTML_END); // end of html-document
        }
    }

    /**
     * Handles the post request; from here onle the method
     * <pre>doGet</pre> is called.
     */
    @Override
    public void doPost(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {
        doGet(request, response);
    }

    /**
     * Shows the HTML page, if no hit was returned from any LPIR.
     *
     * @param webout
     * @throws IOException
     */
    private void showHTML_NoHits(PrintWriter webout) {
        webout.println(HTML_START + printCSSLink()
                + "<title>error - document not found</title>"
                + HEAD_BODY + showHeader() + "<center><table width=\"600\"><tr><td>"
                + "Unfortunately the URL could not be resolved. None of the"
                + " underlying local document resolver were able to find a document with the"
                + " given identifier. Maybe one of the services is down or a document "
                + "with the number doesn't exist. As your URL should contain a persistent"
                + " identifier, please check again later."
                + "</td></tr></table>"
                + showFooter() + HTML_END); // end of html-document
    }

    /**
     * Shows an HTML page, if an internal error occured.
     *
     * @param response
     * @throws IOException
     */
    private void showHTML_Error(HttpServletResponse response)
            throws IOException {
        PrintWriter webout = response.getWriter(); // get stream;
        String errorMailAdress = myPrefs.getContact();
        webout.println(HTML_START + printCSSLink() + "<title>error - internal error</title>" + HEAD_BODY
                + showHeader()
                + "An internal error occured. Please report the URL and the error-message to"
                + " <a href=\"mailto:" + errorMailAdress + "\">" + errorMailAdress + "</a>"
                + showFooter()
                + HTML_END); // end of html-document
    }
    
    /**
     * Retuns the HTML Element pointing to the CSS as String
     * @returns the HTML fragment pointing to the CSS file
     */
    private String printCSSLink () {
        return "<link media=\"all\" href=\"" + myPrefs.getCssFile() + "\" type=\"text/css\" rel=\"stylesheet\">";
    }
    
    /**
     * Returns the header (with logo) of every HTML-page
     *
     */
    private String showHeader() {
        return "<center><table width=\"600\"><tr><td>"
                + "<br/><center><img width=\"95%\" src=\"" + myPrefs.getLogoImage()
                + "\"/></center><br/><br/><h1>Document Resolver</h1></center>"
                + "</td></tr></table></center>";
    }

    /**
     * Returns the footer of any HTML page
     *
     */
    private String showFooter() {
        return "<center><table width=\"600\"><tr><td>"
                + "<hr><font size=\"-1\">"
                + "&copy;  Nieders&auml;chsische Staats- und Universit&auml;tsbibliothek G&ouml;ttingen, 2012"
                + "</font></td></tr></table></center>";
    }

    /**
     * Returns the contenst of a ResolvedUrl object
     *
     */
    private String dumpResolvedUrl(ResolvedURL ru) {
        return "SUBResolver: ru.URL: " + ru.getUrl() + "\n"
                + "SUBResolver: ru.PURL:" + ru.getPurl() + "\n"
                + "SUBResolver: ru.Service:" + ru.getService() + "\n"
                + "SUBResolver: ru.Servicehome:" + ru.getServicehome() + "\n"
                + "SUBResolver: ru.Version:" + ru.getVersion();
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
    //TODO: Try to get rid of this, avid circular dependencies
    protected static String getValueOfElement(Node inNode) {
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
     * Method initializes the servlet: loads preferences (from
     * resolver_config.xml within the application's webapp-folder).
     *
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        String dirsep = System.getProperty("file.separator");

        String configFile;
        if (config.getInitParameter("config") == null || "".equals(config.getInitParameter("config"))) {
            configFile = Preferences.CONFIGFILE;
        } else {
            configFile = config.getInitParameter("config");
        }

        //String prefix = getServletContext().getRealPath(DIRSEP + "WEB-INF");
        String configPath = getServletContext().getRealPath(".") + dirsep + "WEB-INF" + dirsep + configFile;
        logger.info("Config Path is " + configPath);

        logger.info("Starting  == GLOBAL SUB RESOLVER == " + VERSION);

        try {
            myPrefs = new Preferences(configPath);
        } catch (FileNotFoundException fe) {
            throw new ServletException("Configuration File not found!", fe);
        }

        logger.info("Loglevel: " + logger.getEffectiveLevel().toString() + " using Log4J");

    }
}
