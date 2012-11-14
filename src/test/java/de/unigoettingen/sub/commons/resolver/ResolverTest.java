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
        assertNotNull(sr);
        ServletUnitClient sc = sr.newClient();
        logger.info("Creating Request to " + REQUEST_URI);
        WebRequest request = new GetMethodWebRequest(REQUEST_URI);
        request.setParameter("PPN726109029", "");
        WebResponse response = sc.getResponse(request);

        assertNotNull("No response received", response);
        assertEquals(307, response.getResponseCode());
        logger.info("Response code is " + String.valueOf(response.getResponseCode()));
    }

    /**
     * This test checks if the redirect works if the URL ends with a slash, this
     * sould be assured by the rewrite filter.
     * TODO: This doesn't work yet, need to find out how the ServletRunner can be used with the filter.
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
        assertEquals(307, response.getResponseCode());
        logger.info("Response code is " + String.valueOf(response.getResponseCode()));

    }

    //GDZPPN002383659
    @Test
    public void testResolverSelect() throws IOException, SAXException {
        assertNotNull(sr);
        ServletUnitClient sc = sr.newClient();
        logger.info("Creating Request to " + REQUEST_URI);
        WebRequest request = new GetMethodWebRequest(REQUEST_URI);
        request.setParameter("GDZPPN002383659", "");
        WebResponse response = sc.getResponse(request);

        assertNotNull("No response received", response);
        assertEquals(200, response.getResponseCode());
        logger.info("Response code is " + String.valueOf(response.getResponseCode()));
    }
}
