package com.partbuzz.slicer;

import os.util.ExceptionHelper;
import os.util.StringUtils;

import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileOutputStream;
import java.util.logging.Logger;

/**
 * Created by mike on 1/9/16.
 */
@Path("slicer")
public class Slicer {
    private static final Logger log = Logger.getLogger(Slicer.class.getName());

    /**
     * Simply returns 200 OK if called.
     * <p>
     *
     * @param req
     * @return
     */
    @GET
    @Path("ping")
    @Produces({MediaType.TEXT_PLAIN})
    public Response ping(@Context HttpServletRequest req
    ) {
        log.info("ping called");
        return Response.ok("PONG", MediaType.TEXT_PLAIN).build();
    }

    /**
     * Import the panel spreadsheet from the client
     *
     * @param req is the http request
     * @return the names of the uploaded files or an error message
     */
    @POST
    @Path("importFile")
    public Response importFiles(@Context HttpServletRequest req) {
        try {

            ByteArrayDataSource bads = new ByteArrayDataSource(req.getInputStream(), req.getContentType());
            MimeMultipart mmp = new MimeMultipart(bads);

            // Read the raw file
            String filename = "";
            for (int i = 0; i < mmp.getCount(); i++) {
                MimeBodyPart mbp = (MimeBodyPart) mmp.getBodyPart(i);

                byte[] buffer = new byte[mbp.getSize()];
                mbp.getInputStream().read(buffer);

                // calculate a filename
                String md5Hex = StringUtils.encodeHexString(StringUtils.md5(buffer));
                filename = StringUtils.encode(md5Hex + "-" + mbp.getFileName());

                // Save the file
                String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
                File file = new File(tmpDir, filename);
                FileOutputStream fp = new FileOutputStream(file);
                fp.write(buffer);
                fp.close();

            }
            return Response.ok().entity(filename).build();
        } catch (Exception e) {
            return Response.ok().entity(ExceptionHelper.getStackTrace(e)).build();
        }
    }

}
