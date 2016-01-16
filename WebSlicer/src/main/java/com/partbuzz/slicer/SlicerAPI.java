package com.partbuzz.slicer;

import os.io.FileHelper;
import os.util.ExceptionHelper;
import os.util.StringUtils;
import os.util.json.DefaultJSONFactory;
import os.util.json.JSONException;
import os.util.json.JSONObject;

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
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by mike on 1/9/16.
 */
@Path("slicer")
public class SlicerAPI {
    private static final Logger log = Logger.getLogger(SlicerAPI.class.getName());
    private final DefaultJSONFactory JSONFactory = DefaultJSONFactory.getInstance();

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
     * Import an STL model file from the client
     *
     * @param req is the http request
     * @return the names of the uploaded files or an error message
     */
    @POST
    @Path("importStl")
    public Response importStl(@Context HttpServletRequest req) {
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

                //TODO: check for file format before storing file

                // Save the file
//                String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
                String tmpDir = "/tmp";
                File file = new File(tmpDir, filename);
                FileOutputStream fp = new FileOutputStream(file);
                fp.write(buffer);
                fp.close();

            }

            // register our new model file
            String fileId = FileTracker.registerModelfile(filename);

            // build response message
            JSONObject response = new JSONObject();
            response.put("fileId", fileId);

            return Response.ok().entity(response.toString()).build();
        } catch (Exception e) {
            return Response.ok().entity(ExceptionHelper.getStackTrace(e)).build();
        }
    }

    /**
     * Import a settings file from the client
     *
     * @param req is the http request
     * @return the names of the uploaded files or an error message
     */
    @POST
    @Path("importSettings")
    public Response importSettings(@Context HttpServletRequest req) {
        log.log(Level.INFO, "importing settings ");
        StringBuilder sb = new StringBuilder();
        try {
            String line;

            // parse input stream into a string
            try (BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()))) {
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            String data = sb.toString(); // finish parse
//            data = JSONFactory.jsonObject(data).toString(); // run data through extra parser

            // Save to file
//            String tmpDir = System.getProperty("java.io.tmpdir", "/tmp");
            String tmpDir = "/tmp";
            String fileName = FileTracker.generateSettingsFileName();
            String fullPath = tmpDir + "/" + fileName;

            try (PrintWriter out = new PrintWriter(fullPath)) {
                out.print(data); // print our entire json string to file
            }


            // register our new file with our registry
            String fileId = FileTracker.registerSettingsFile(fileName);

            // build response message
            JSONObject response = new JSONObject();
            response.put("fileId", fileId);

            return Response.ok().entity(response.toString()).build();
        } catch (IOException e) {
            e.printStackTrace();
            return Response.serverError().entity("File write error").build();
        }
    }

    /**
     * Slice is the main function of this API. Given the parameters below it will return a formatted .gcode file
     * to the user.
     * <p>
     * The parameters required are a .stl file ID and a .json settings file ID.
     * These parameters are given when the above API methods are called. It is up to the client to track these ID's
     * <p>
     * SAMPLE JSON FORMAT
     * {"modelId":"UUID","settingsId":"UUID"}
     *
     * @param req is the http request
     * @return the names of the uploaded files or an error message
     */
    @POST
    @Path("slice")
    public Response slice(@Context HttpServletRequest req) {
        // parse input JSON stream
        JSONObject jo = parseJSONStream(req);

        // pull out our needed fields
        String modelId = jo.getString("modelId");
        String settingsId = jo.getString("settingsId");

        if (modelId == null || settingsId == null) { // inproperly formatted data
            return Response.serverError().entity("Model or settings file id not found").build();
        }

        try {
            // SAMPLE COMMAND
            // ../../CuraEngine-master/build/CuraEngine slice -v -j ultimaker2.json -g -e -o "output/test.gcode" -l ../../models/ControlPanel.stl

            // create new CuraEngine platform executable
            CuraEngine ce = new CuraEngine();
            ce.options() // add options. order is irrelevant.
                    .verbose()
                    .currentGroupOnly()
                    .extruderTrainOption()
                    .logProgress()
                    .settingsFilename("/tmp/" + FileTracker.getSettingFileNameById(settingsId))
                    .outputFilename("/tmp/output.gcode")
                    .modelFilename("/tmp/" + FileTracker.getModelFileNameById(modelId));

            ce.execute();

            // build response from output file and return it.
            JSONObject response = new JSONObject();
            byte[] raw = FileHelper.readContent(new File("/tmp/output.gcode"));
            String gcode = new String(raw);
            response.put("gcode", gcode);

            return Response.ok().entity(response.toString()).build();
        } catch (IOException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    /**
     * A util function to parse an input JSON stream and parses to a JSONObject
     *
     * @param req
     * @return
     */
    private JSONObject parseJSONStream(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        try {
            String line;

            // parse input stream into a string
            try (BufferedReader br = new BufferedReader(new InputStreamReader(req.getInputStream()))) {
                while ((line = br.readLine()) != null) {
                    sb.append(line);
                }
            }
            String data = sb.toString();

            log.info(data);

            return JSONFactory.jsonObject(data);

        } catch (IOException | JSONException e) {
            log.severe(e.getMessage());
            throw new JSONException("Stream parse error.");
        }

    }

}
