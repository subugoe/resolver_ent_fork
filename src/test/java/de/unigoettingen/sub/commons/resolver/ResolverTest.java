/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package de.unigoettingen.sub.commons.resolver;

import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.meterware.servletunit.ServletRunner;
import com.meterware.servletunit.ServletUnitClient;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.log4j.Logger;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 *
 * @author cmahnke
 */
public class ResolverTest {

    static ServletRunner sr = null;
    static Logger logger = Logger.getLogger(ResolverTest.class.getName());
    static String REQUEST_URI = "http://localhost:8080/resolver/purl";
    static Integer TEST_RUNS = 100;

    public ResolverTest() {
    }

    @BeforeClass
    public static void initServlet() throws IOException, SAXException {
        File webXml = new File("./src/main/webapp/WEB-INF/web.xml");
        sr = new ServletRunner(webXml, "/resolver");
        logger.info("Set up ServletRunner");
    }

    //?PPN726109029
    @Test
    public void testResolverRedirect() throws IOException, SAXException {
        WebResponse response = resolveIdentifier("PPN726109029");

        assertNotNull("No response received", response);
        assertEquals(HttpStatus.SC_TEMPORARY_REDIRECT, response.getResponseCode());
        logger.info("Response code is " + String.valueOf(response.getResponseCode()));
    }

    /**
     * This test checks if the redirect works if the URL ends with a slash, this
     * sould be assured by the rewrite filter. TODO: This doesn't work yet, need
     * to find out how the ServletRunner can be used with the filter.
     *
     * @throws IOException
     * @throws SAXException
     */
    @Test
    @Ignore
    public void testResolverRedirectSlash() throws IOException, SAXException {
        assertNotNull(sr);
        ServletUnitClient sc = sr.newClient();
        logger.info("Creating Request to " + REQUEST_URI + "/");
        WebRequest request = new GetMethodWebRequest(REQUEST_URI + "/");
        request.setParameter("PPN726109029", "");
        WebResponse response = sc.getResponse(request);

        assertNotNull("No response received", response);
        assertEquals(HttpStatus.SC_TEMPORARY_REDIRECT, response.getResponseCode());
        logger.info("Response code is " + String.valueOf(response.getResponseCode()));

    }

    //GDZPPN002383659
    @Test
    public void testResolverSelect() throws IOException, SAXException {
        WebResponse response = resolveIdentifier("GDZPPN002383659");

        assertNotNull("No response received", response);
        assertEquals(HttpStatus.SC_OK, response.getResponseCode());
        logger.info("Response code is " + String.valueOf(response.getResponseCode()));
    }

    @Ignore
    @Test
    public void testConnectionLeak() throws IOException, SAXException {
        List<String> PPNs = new ArrayList<String>();
        //GDZ PPNs
        PPNs.add("PPN727485059");
        PPNs.add("PPN732398088");
        PPNs.add("PPN66102976X");
        PPNs.add("PPN734617216");
        PPNs.add("PPN734685599");
        PPNs.add("PPN730424642");
        PPNs.add("PPN73009667X");
        PPNs.add("PPN719207002");
        PPNs.add("PPN731520696");
        PPNs.add("PPN730068110");
        PPNs.add("PPN730092534");
        PPNs.add("PPN729605310");
        PPNs.add("PPN72704916X");
        PPNs.add("PPN731636279");
        //Gretil
        PPNs.add("gr_elib-218");
        PPNs.add("gr_elib-219");
        PPNs.add("gr_elib-220");
        PPNs.add("gr_elib-222");
        PPNs.add("gr_elib-212");
        PPNs.add("gr_elib-223");
        PPNs.add("gr_elib-224");
        //GoeScholar
        PPNs.add("gs-1/4461");
        PPNs.add("goescholar/2206");
        PPNs.add("gs-1/8419");
        PPNs.add("gs-1/8407");
        PPNs.add("gs-1/8517");
        PPNs.add("gs-1/8384");
        List<String> resolvedURLs;
        
        for (int i = 1; i < TEST_RUNS + 1; i++) {
            resolvedURLs = new ArrayList<String>();
            for (String PPN : PPNs) {
                logger.info("Resolving identifier " + PPN);
                WebResponse response = resolveIdentifier(PPN);

                assertNotNull("No response received", response);
                assertTrue("Response not 200 or 307", (response.getResponseCode() == HttpStatus.SC_OK || response.getResponseCode() == HttpStatus.SC_TEMPORARY_REDIRECT));
                String u = getRedirectURL(response);
                //This is a test if a resolved URL gets reported twice (race conditions etc)
                assertFalse("Duplicate URL", resolvedURLs.contains(u));
                logger.info("Response code is " + String.valueOf(response.getResponseCode()) + " Redirect URL is " + u);
            }
        }

    }

    @Ignore
    @Test
    public void testBrokenIdentifier() throws IOException, SAXException {
        List<String> PPNs = new ArrayList<String>();
        //these shouldn't work
        PPNs.add("PPN7274834569");
        PPNs.add("This");
        PPNs.add("is");
        PPNs.add("a");
        PPNs.add("test");
        PPNs.add("of");
        PPNs.add("the");
        PPNs.add("resolver");
        for (int i = 1; i < TEST_RUNS + 1; i++) {
            for (String PPN : PPNs) {
                logger.info("Resolving identifier " + PPN);
                WebResponse response = resolveIdentifier(PPN);

                assertNotNull("No response received", response);
                //assertEquals(200, response.getResponseCode());
                logger.info("Response code is " + String.valueOf(response.getResponseCode()));

            }
        }
    }

    private WebResponse resolveIdentifier(String identifier) throws IOException, SAXException {
        assertNotNull(sr);
        ServletUnitClient sc = sr.newClient();
        logger.info("Creating Request to " + REQUEST_URI);
        WebRequest request = new GetMethodWebRequest(REQUEST_URI);
        request.setParameter(identifier, "");
        return sc.getResponse(request);
    }

    private String getRedirectURL(WebResponse wr) {
        if (wr.getResponseCode() == HttpStatus.SC_TEMPORARY_REDIRECT) {
            return wr.getHeaderField("Location");
        } else {
            return "HTML Redirect";
        }
    }
}
