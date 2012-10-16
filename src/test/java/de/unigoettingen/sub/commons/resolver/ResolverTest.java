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
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 *
 * @author cmahnke
 */
public class ResolverTest {
    static ServletRunner sr = null;
    
    
    public ResolverTest() {
    }
    
    //?PPN726109029
    @BeforeClass
    public static void initServlet () throws IOException, SAXException {
        File webXml = new File("./src/main/webapp/WEB-INF/web.xml");
        sr = new ServletRunner(webXml, "/resolver");
    }
    
    @Test
    public void testResolver () throws IOException, SAXException {
        assertNotNull(sr);
        ServletUnitClient sc = sr.newClient();
        WebRequest request = new GetMethodWebRequest("http://localhost:8080/resolver/purl");
        request.setParameter("PPN726109029", "");
        WebResponse response = sc.getResponse(request);

        assertNotNull("No response received", response);
        assertEquals(307, response.getResponseCode());
    
    }
    
}
