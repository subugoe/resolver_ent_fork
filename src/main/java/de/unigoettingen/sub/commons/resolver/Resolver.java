/*
 * Created on 13.06.2005
 *
 */
package de.unigoettingen.sub.commons.resolver;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;

import java.util.Enumeration;
import java.util.LinkedList;

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
    Preferences myPrefs = null;
    String version = "version 0.3";
    String DIRSEP = null;
    String imagepath = null;

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

        LinkedList<LocalResolverConnectorThread> allThreads = new LinkedList();

        //	 get parameters
        logger.info("SUBResolver: received a request");
        /*
        if (myPrefs.getDebug() > 0) {
            writeLog("SUBResolver: received a request");
        }
        */
        Enumeration<String> enumm = request.getParameterNames();

        ArrayList<String> params = Collections.list(request.getParameterNames());
        
        //int i = 0;
        int i = params.size();
        //for (String parameter: params) {
 
        String parameter = null;
        while (enumm.hasMoreElements()) {
            parameter = enumm.nextElement();
            logger.debug("SUBResolver: parameter=" + parameter);

            /*
            if (myPrefs.getDebug() > 0) {
                writeLog("SUBResolver: parameter=" + parameter);
            }
            */
            //i++;
        }
        
        if (i == 0) {
            // error handling; no parameter/identifier given
            logger.warn("SUBResolver: didn't receive a parameter");
            /*
            if (myPrefs.debug > 0) {
                writeLog("SUBResolver: didn't receive a parameter");
            }
            */
            return;
        } else if (i > 1) {
            // invalid request
            logger.warn("SUBResolver: wrong number of parameters");
            /*
            if (myPrefs.debug > 0) {
                writeLog("SUBResolver: wrong number of parameters");
            }
            */
            return;
        }
        

        // deliver an image in image path
        //
        if (parameter.substring(0, 4).equals("name")) {
            String name = request.getParameter("name");
            String filename = imagepath + DIRSEP + name;
            /*
            if (myPrefs.debug > 0) {
                writeLog("SUBResolver: received request for image " + filename);
            }
            */
            logger.info("SUBResolver: received request for image " + filename);
            ShowImage si = new ShowImage(filename, response);
            return;
        }

        // just ask all LocalResolver 
        // every connection in done in a seperate thread
        for (LocalResolver lr: myPrefs.getResolvers()) {
        /*
        Iterator<LocalResolver> it = myPrefs.getResolvers().iterator();
        //i = 0;
        while (it.hasNext()) {
            LocalResolver lr = it.next();
        */
            String url = lr.getURL();
            logger.info("SUBResolver: url:" + url + parameter);
            /*
            if (myPrefs.getDebug() > 0) {
                writeLog("SUBResolver: url:" + url + parameter);
            }
            */
            LocalResolverConnectorThread rt = new LocalResolverConnectorThread(myPrefs, url + parameter, myPrefs.getMax_threadruntime());
            // create a new thread
            rt.start();   // start thread

            allThreads.add(rt); // add thread to groups of threads
            //i++;
        }

        // checking, if threads are still running
        for (LocalResolverConnectorThread t: allThreads) {
        /*
        Iterator<LocalResolverConnectorThread> tit = allThreads.iterator();
        while (tit.hasNext()) {
            LocalResolverConnectorThread t = tit.next();
        */
            try {
                t.join(myPrefs.getMax_threadruntime());   // just wait max. 20 seconds until thread must be finished
            } catch (InterruptedException e) {
                // thread was interrupted
                response.setContentType("text/html");
                showHTML_Error(response);
                return;
            }
        }

        LinkedList<ResolvedURL> answeredRequest = new LinkedList();
        for (LocalResolverConnectorThread t: allThreads) {
        /*
        Iterator<LocalResolverConnectorThread> tit = allThreads.iterator();
        while (tit.hasNext()) {
            LocalResolverConnectorThread t = tit.next();
        */    
            if (t.isAlive()) {
                // thread is finished
                logger.info("SUBResolver: " + t.getUrl() + " thread is still running");
                /*
                if (myPrefs.debug > 2) {
                    writeLog("SUBResolver: " + t.url + " thread is still running");
                }
                */
            }
            {
                // thread is not finsihed, so we won't have any answer
                //System.out.println("SUBResolver: "+t.url+" thread is dead");
                //System.out.println("SUBResolver: "+t.getResponses().size()+" responses");
                if (t.getResponses() != null && (t.getResponses().size() > 0)) {
                    for (ResolvedURL ru: t.getResponses()) {
                    /*
                    Iterator<ResolvedURL> it_test = t.getResponses().iterator();
                    while (it_test.hasNext()) {
                        Object obj = it_test.next();
                        ResolvedURL ru = (ResolvedURL) obj;
                    */
                        /*
                        if (myPrefs.debug > 1) {
                            writeLog("XML Response:\n"
                                    + "SUBResolver1: ru.URL: " + ru.getUrl() + "\n"
                                    + "SUBResolver1: ru.PURL:" + ru.getPurl() + "\n"
                                    + "SUBResolver1: ru.Service:" + ru.getService() + "\n"
                                    + "SUBResolver1: ru.Servicehome:" + ru.getServicehome() + "\n"
                                    + "SUBResolver1: ru.Version:" + ru.getVersion());
                        }
                        */
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
        if ((answeredRequest == null) || (answeredRequest.size() == 0)) {
            response.setContentType("text/html");
            showHTML_NoHits(response, request);
            return;
        }

        // just one hit; do a redirect

        if ((answeredRequest != null) && (answeredRequest.size() == 1)) {
            response.setContentType("text/html");
            for (ResolvedURL ru: answeredRequest) {
            /*
            Iterator<ResolvedURL> it2 = answeredRequest.iterator();
            while (it2.hasNext()) {
                ResolvedURL ru = it2.next();
            */
                response.setStatus(307); // temporary redirect; avoid caching
                response.setHeader("Location", ru.url);
            }
            return;
        }

        // output the result
        // as HTML or do a simple forward
        // if only one result is available


        // just output debug information
        if ((answeredRequest != null) || (answeredRequest.size() > 0)) {
            for (ResolvedURL ru: answeredRequest) {
            /*
            Iterator<ResolvedURL> it2 = answeredRequest.iterator();
            while (it2.hasNext()) {
                ResolvedURL ru = it2.next();
            */
                /*
                if (myPrefs.getDebug() > 1) {
                    writeLog("SUBResolver: ru.URL: " + ru.getUrl() + "\n"
                            + "SUBResolver: ru.PURL:" + ru.getPurl() + "\n"
                            + "SUBResolver: ru.Service:" + ru.getService() + "\n"
                            + "SUBResolver: ru.Servicehome:" + ru.getServicehome() + "\n"
                            + "SUBResolver: ru.Version:" + ru.getVersion());
                }
                */
                logger.debug("SUBResolver: ru.URL: " + ru.getUrl() + "\n"
                            + "SUBResolver: ru.PURL:" + ru.getPurl() + "\n"
                            + "SUBResolver: ru.Service:" + ru.getService() + "\n"
                            + "SUBResolver: ru.Servicehome:" + ru.getServicehome() + "\n"
                            + "SUBResolver: ru.Version:" + ru.getVersion());
            }

            //
            // output HTML

            // set http header
            response.setContentType("text/html");
            PrintWriter webout = response.getWriter(); // get stream;

            webout.println("<html><head>");
            webout.println("<title>SUB resolver</title>");
            webout.println("</head><body>");

            showHeader(webout, request);

            webout.println("<center><table width=\"600\"><tr><td>");

            webout.println("The document you requested ");
            webout.println("" + request.getRequestURL() + "?" + parameter);
            webout.println(" is available at:<br>");

            for (ResolvedURL ru: answeredRequest) {
            /*
            Iterator<ResolvedURL> it3 = answeredRequest.iterator();
            while (it3.hasNext()) {
                ResolvedURL ru = it3.next();
            */
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
            /*
            writeLog("SUBResolver: sorry, no result");
            */
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
    private void showHTML_NoHits(HttpServletResponse response, HttpServletRequest request)
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
        showHeader(webout, request);
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
                + " <a href=\"mailto:" + errorMailAdress +"\"" + errorMailAdress + "</a>");
        webout.println("</body></html>"); // end of html-document

    }

    /**
     * Shows the header (with logo) of every HTML-page
     *
     * @param out
     * @param request
     */
    private void showHeader(PrintWriter out, HttpServletRequest request) {
        out.println("<center><table width=\"600\"><tr><td>");
        out.println("<center><img width=\"20%\" src=\"" + request.getRequestURL() + "?name=" + myPrefs.getLogoImage() + "\">");
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
     * Writes a log-message to the logfile; the location of th elogfile is set
     * in the preference file.
     *
     * @param inMessage
     */
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
    /**
     * Method initializes the servlet: loads preferences (from
     * resolver_config.xml within the application's webapp-folder).
     *
     */
    @Override
    public void init() {

        DIRSEP = System.getProperty("file.separator");
        String prefix = getServletContext().getRealPath(DIRSEP + "WEB-INF");
        String configpath = prefix + DIRSEP;
        
        logger.fatal("\nstarting  == GLOBAL SUB RESOLVER == " + version + "\n");
        /*
        System.out.println("\nstarting  == GLOBAL SUB RESOLVER ==");
        System.out.println("          " + version + "\n");
        */
 
        myPrefs = new Preferences(configpath + DIRSEP + Preferences.CONFIGFILE);
        logger.fatal("Loglevel: " + logger.getLevel() + " using Log4J");
        /*
        System.out.println("           debug level set to " + myPrefs.getDebug());
        if (myPrefs.getLogfile() != null) {
            System.out.println("           logfile set to " + myPrefs.getLogfile());
        } else {
            System.out.println("           no logfile defined; logging will go to System.out");
        }
        */
        imagepath = getServletContext().getRealPath(DIRSEP + "images" + DIRSEP);

        /*
        if ((myPrefs.getDebug() > 0) && (myPrefs.getLogfile() != null)) {
            writeLog("\nstarting  == GLOBAL SUB RESOLVER ==\n"
                    + "          " + version + "\n\n"
                    + "debug level: " + myPrefs.getDebug() + "\n"
                    + "logfile:     " + myPrefs.getLogfile() + "\n");
        }
        */
    }
}
