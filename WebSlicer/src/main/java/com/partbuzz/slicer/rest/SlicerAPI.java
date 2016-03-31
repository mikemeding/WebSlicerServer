package com.partbuzz.slicer.rest;

import com.partbuzz.slicer.cura.CuraEngine;
import com.partbuzz.slicer.util.CuraEngineException;
import com.partbuzz.slicer.util.FileTracker;
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
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.*;
import java.util.HashMap;
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
     * Add a new client to our database and file structure.
     * This will set aside all of the files needed for a new client and link them correctly
     *
     * @param req
     * @return The new uuid for that client so that we may track files
     */
    @POST
    @Path("setupClient")
    public Response setupClient(@Context HttpServletRequest req) {
        try {
            String uuid = FileTracker.setupNewClient();
            JSONObject payload = new JSONObject();
            payload.put("clientId", uuid);

            return Response.ok().entity(payload).build();
        } catch (CuraEngineException e) {
            e.printStackTrace();
            return Response.serverError().build();
        }
    }


    /**
     * Import an STL model file from the client
     *
     * @param req is the http request
     * @return the names of the uploaded files or an error message
     */
    @POST
    @Path("importStl/{clientId}")
    public Response importStl(@Context HttpServletRequest req,
                              @PathParam("clientId") String clientId) {
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
//                String md5Hex = StringUtils.encodeHexString(StringUtils.md5(buffer));
//                filename = StringUtils.encode(md5Hex + "-" + mbp.getFileName());
                filename = StringUtils.encode(mbp.getFileName());

                // Save the file to disk
                File file = new File(FileTracker.getModelPathById(clientId) + FileTracker.delimiter + filename);
                FileOutputStream fp = new FileOutputStream(file);
                fp.write(buffer);
                fp.close();

            }

            // register our new model file
            String fileId = FileTracker.registerModelFile(clientId, filename);

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
    @Path("importSettings/{clientId}")
    public Response importSettings(@Context HttpServletRequest req,
                                   @PathParam("clientId") String clientId) {
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

            // Save to file
            String settingsPath = FileTracker.getSettingsFullPath(clientId);

            // write/overwrite file
            try (PrintWriter out = new PrintWriter(settingsPath)) {
                out.print(data);
            }

            return Response.ok().build();
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
    @Path("slice/{clientId}/{modelId}")
    public Response slice(@Context HttpServletRequest req,
                          @PathParam("clientId") String clientId, @PathParam("modelId") String modelId) {
        try {
            // SAMPLE COMMAND RESULT
            // ../../CuraEngine-master/build/CuraEngine slice -v -j ultimaker2.json -g -e -o "output/test.gcode" -l ../../models/ControlPanel.stl

            // create new CuraEngine platform executable
            CuraEngine ce = new CuraEngine();
            ce.options() // add options. order is irrelevant.
                    .verbose()
                    .currentGroupOnly()
                    .extruderTrainOption()
                    .logProgress()
                    .settingsFilename(FileTracker.getSettingsFullPath(clientId))
                    .outputFilename(FileTracker.getOutputFilePath(clientId))
                    .modelFilename(FileTracker.getModelFullPath(clientId, modelId));
            ce.execute();

            // build response from output file and return it.
            JSONObject response = new JSONObject();
            byte[] raw = FileHelper.readContent(new File(FileTracker.getOutputFilePath(clientId)));
            String gcode = new String(raw);
            response.put("gcode", gcode);

            return Response.ok().entity(response.toString()).build();
        } catch (IOException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    /**
     * This function is to run a full test slice with a sample model and return its gcode.
     * Eliminates the need for a UI to run backend tests.
     * @param req is the http request
     * @return the names of the uploaded files or an error message
     */
    @POST
    @Path("testSlice")
    public Response testSlice(@Context HttpServletRequest req) {
        try {
            // create new CuraEngine platform executable
            CuraEngine ce = new CuraEngine();
            ce.options() // add options. order is irrelevant.
                    .verbose()
                    .currentGroupOnly()
                    .extruderTrainOption()
                    .logProgress()
                    .settingsFilename("/tmp/webslicer/test/prusa_i3.json")
                    .outputFilename("/tmp/webslicer/test/output.gcode")
                    .modelFilename("/tmp/webslicer/test/testModel.stl");
            ce.execute();

            // build response from output file and return it.
            JSONObject response = new JSONObject();
            byte[] raw = FileHelper.readContent(new File("/tmp/webslicer/test/output.gcode"));
            String gcode = new String(raw);
            response.put("gcode", gcode);

            return Response.ok().entity(response.toString()).build();
        } catch (IOException e) {
            return Response.serverError().entity(e.getMessage()).build();
        }
    }

    /**
     * Get all of the model file names and tracking ids associated with a client uuid.
     *
     * @param req
     * @param clientId
     * @return
     */
    @GET
    @Path("getFiles/{clientId}")
    @Produces("application/json")
    public Response getFiles(@Context HttpServletRequest req,
                             @PathParam("clientId") String clientId) {
        HashMap filesMap = FileTracker.getAllModelFiles(clientId);
        return Response.ok().entity(filesMap).build();
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
