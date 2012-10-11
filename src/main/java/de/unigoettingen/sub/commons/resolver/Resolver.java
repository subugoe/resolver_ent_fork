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
    private String DIRSEP = null;

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
                        logger.debug("XML Response:\n"
                                + "SUBResolver1: ru.URL: " + ru.getUrl() + "\n"
                                + "SUBResolver1: ru.PURL:" + ru.getPurl() + "\n"
                                + "SUBResolver1: ru.Service:" + ru.getService() + "\n"
                                + "SUBResolver1: ru.Servicehome:" + ru.getServicehome() + "\n"
                                + "SUBResolver1: ru.Version:" + ru.getVersion());
                        answeredRequest.add(ru);
                    }
                } // endif
            }

        }

        // No hit is available
        if (answeredRequest.size() == 0) {
            response.setContentType("text/html");
            showHTML_NoHits(response);
            return;
        }

        // just one hit; do a redirect
        if (answeredRequest.size() == 1) {
            response.setContentType("text/html");
            for (ResolvedURL ru : answeredRequest) {
                response.setStatus(307); // temporary redirect; avoid caching
                response.setHeader("Location", ru.getUrl());
            }
            return;
        }

        // output the result
        // as HTML or do a simple forward
        // if only one result is available


        // just output debug information
        if ((answeredRequest != null) || (answeredRequest.size() > 0)) {
            for (ResolvedURL ru : answeredRequest) {
                logger.debug("SUBResolver: ru.URL: " + ru.getUrl() + "\n"
                        + "SUBResolver: ru.PURL:" + ru.getPurl() + "\n"
                        + "SUBResolver: ru.Service:" + ru.getService() + "\n"
                        + "SUBResolver: ru.Servicehome:" + ru.getServicehome() + "\n"
                        + "SUBResolver: ru.Version:" + ru.getVersion());
            }

            // output HTML

            // set http header
            response.setContentType("text/html");
            PrintWriter webout = response.getWriter(); // get stream;

            webout.println("<html><head>");
            webout.println("<title>SUB resolver</title>");
            webout.println("</head><body>");

            showHeader(webout);

            webout.println("<center><table width=\"600\"><tr><td>");

            webout.println("The document you requested ");
            webout.println("" + request.getRequestURL() + "?" + parameter);
            webout.println(" is available at:<br>");

            for (ResolvedURL ru : answeredRequest) {
                webout.println("<h4> <a href=\"" + ru.getServicehome() + "\">" + ru.getService() + "</a></h4>");
                webout.println("go to document:&nbsp;<a href=\"" + ru.getUrl() + "\">" + ru.getUrl() + "</a>");
                webout.println("<br>");
            }
            webout.println("</td></tr></table>");

            showFooter(webout);

            webout.println("</body></html>"); // end of html-document
        } else {
            // no result
            logger.info("SUBResolver: sorry, no result");
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
     * @param response
     * @param request
     * @throws IOException
     */
    private void showHTML_NoHits(HttpServletResponse response)
            throws IOException {
        PrintWriter webout;
        try {
            webout = response.getWriter(); // get stream;
        } catch (IOException ioe) {
            logger.error("IO Exception while getting response stream", ioe);
            return;
        }
        webout.println("<html><head>");
        webout.println("<title>error - document not found</title>");
        webout.println("</head><body>");
        showHeader(webout);
        webout.println("<center><table width=\"600\"><tr><td>");
        webout.println("Unfortunately the URL could not be resolved. None of the underlying local document resolver were able to find a document with the"
                + " given identifier. Maybe one of the services is down or a document with the number doesn't exist. As your URL should contain a persistent"
                + " identifier, please check again later.");
        webout.println("</td></tr></table>");
        showFooter(webout);
        webout.println("</body></html>"); // end of html-document
    }

    /**
     * Shows an HTML page, if an internal error occured.
     *
     * @param response
     * @throws IOException
     */
    private void showHTML_Error(HttpServletResponse response)
            throws IOException {
        PrintWriter webout;
        String errorMailAdress = myPrefs.getContact();
        try {
            webout = response.getWriter(); // get stream;
        } catch (IOException ioe) {
            logger.error("IO Exception while getting response stream", ioe);
            return;
        }
        webout.println("<html><head>");
        webout.println("<title>error - internal error</title>");
        webout.println("</head><body>");
        webout.println("An internal error occured. Please report the URL and the error-message to"
                + " <a href=\"mailto:" + errorMailAdress + "\"" + errorMailAdress + "</a>");
        webout.println("</body></html>"); // end of html-document

    }

    /**
     * Shows the header (with logo) of every HTML-page
     *
     * @param out
     * @param request
     */
    private void showHeader(PrintWriter out) {
        out.println("<center><table width=\"600\"><tr><td>");
        out.println("<center><img width=\"20%\" src=\"" + myPrefs.getLogoImage() + "\"/></center>");
        out.println("<br><b>Document Resolver</b></center>");

        out.println("</td></tr></table></center>");
    }

    /**
     * Shows the footer of any HTML page
     *
     * @param out
     */
    private void showFooter(PrintWriter out) {
        out.println("<center><table width=\"600\"><tr><td>");
        out.println("<hr><font size=\"-1\">");
        out.println("(C) Nieders&auml;chsische Staats- und Universit&auml;tsbibliothek G&ouml;ttingen, 2005");
        out.println("</font></td></tr></table></center>");
    }

    /**
     * Method initializes the servlet: loads preferences (from
     * resolver_config.xml within the application's webapp-folder).
     *
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        DIRSEP = System.getProperty("file.separator");

        String configFile;
        if (config.getInitParameter("config") == null || "".equals(config.getInitParameter("config"))) {
            configFile = Preferences.CONFIGFILE;
        } else {
            configFile = config.getInitParameter("config");
        }

        //String prefix = getServletContext().getRealPath(DIRSEP + "WEB-INF");
        String configPath = getServletContext().getRealPath(".") + DIRSEP + "WEB-INF"+ DIRSEP + configFile;
        logger.info("Config Path is " + configPath);

        logger.info("Starting  == GLOBAL SUB RESOLVER == " + VERSION);

        try {
            myPrefs = new Preferences(configPath);
        } catch (FileNotFoundException fe) {
            throw new ServletException("Configuration File not found!", fe);
        }

        logger.fatal("Loglevel: " + logger.getEffectiveLevel().toString() + " using Log4J");

    }
}
