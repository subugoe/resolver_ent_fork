/*
 * Created on 20.06.2005
 *
 */
package de.unigoettingen.sub.commons.resolver;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

import javax.servlet.http.HttpServletResponse;

/**
 * Helper class to load a GIF image and output it to the output stream.
 *
 * @author enders
 */
public class ShowImage {
    static String contentType = "image/gif";
    Preferences myPrefs = null;
    String DIRSEP;

    /**
     * Loads a GIF-image and sends it to an OutputStream derived from the
     * HttpServletResponse.
     *
     * @param filename
     * @param response
     */
    public ShowImage(String filename, HttpServletResponse response) {
        

        if (filename == null) {
            return;
        } // no filename given
        // get output stream
        //

        try {
            response.setContentType(contentType);
            OutputStream out = response.getOutputStream();

            // read file and output it
            FileInputStream fstream = new FileInputStream(filename);
            int c;
            while ((c = fstream.read()) != -1) {
                out.write(c);
            }
            // finsihed reading; we are reading as long as there is something left to read
        } catch (IOException ioe) {
            //TODO: Add a logger
            System.out.println("IOException while reading and writing image data:");
            System.out.println(ioe);
        }
    }
}
