package com.partbuzz.slicer;

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
import java.util.UUID;
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

    /**
     * Import a settings file from the client
     *
     * @param req is the http request
     * @return the names of the uploaded files or an error message
     */
    @POST
    @Path("importSettings")
    public Response importSettings(@Context HttpServletRequest req) {
        // parse input JSON stream
        JSONObject jo = parseJSONStream(req);

        //TODO: parse correct settings from json object.
        /**
         * SAMPLE JSON SETTINGS FILE
         *
         { "id": "prusa_i3", "version": 1,
         "name": "Prusa i3",
         "manufacturer": "Other",
         "author": "Other",
         "icon": "icon_ultimaker2.png",
         "platform": "prusai3_platform.stl",

         "inherits": "fdmprinter.json",

         "overrides": {
         "machine_heated_bed": { "default": true },
         "machine_width": { "default": 200 },
         "machine_height": { "default": 200 },
         "machine_depth": { "default": 200 },
         "machine_center_is_zero": { "default": false },
         "machine_nozzle_size": { "default": 0.4 },
         "machine_nozzle_heat_up_speed": { "default": 2.0 },
         "machine_nozzle_cool_down_speed": { "default": 2.0 },
         "machine_head_shape_min_x": { "default": 75 },
         "machine_head_shape_min_y": { "default": 18 },
         "machine_head_shape_max_x": { "default": 18 },
         "machine_head_shape_max_y": { "default": 35 },
         "machine_nozzle_gantry_distance": { "default": 55 },
         "machine_gcode_flavor": { "default": "RepRap (Marlin/Sprinter)" },

         "machine_start_gcode": {
         "default": "G21 ;metric values\nG90 ;absolute positioning\nM82 ;set extruder to absolute mode\nM107 ;start with the fan off\nG28 X0 Y0 ;move X/Y to min endstops\nG28 Z0 ;move Z to min endstops\nG1 Z15.0 F9000 ;move the platform down 15mm\nG92 E0 ;zero the extruded length\nG1 F200 E3 ;extrude 3mm of feed stock\nG92 E0 ;zero the extruded length again\nG1 F9000\n;Put printing message on LCD screen\nM117 Printing..."
         },
         "machine_end_gcode": {
         "default": "M104 S0 ;extruder heater off\nM140 S0 ;heated bed heater off (if you have it)\nG91 ;relative positioning\nG1 E-1 F300  ;retract the filament a bit before lifting the nozzle, to release some of the pressure\nG1 Z+0.5 E-5 X-20 Y-20 F9000 ;move Z up a bit and retract filament even more\nG28 X0 Y0 ;move X/Y to min endstops, so the head is out of the way\nM84 ;steppers off\nG90 ;absolute positioning"
         }
         }
         }
         */

        log.log(Level.INFO, "importSettings: " + jo.toString());
        //TODO: UUID needs to be tracked for each file.
        return Response.ok().entity(UUID.randomUUID()).build(); // return a random uuid
    }

    /**
     * Slice is the main function of this API. Given the parameters below it will return a formatted .gcode file
     * to the user.
     * <p>
     * The parameters required are a .stl file ID and a .json settings file ID.
     * These parameters are given when the above API methods are called. It is up to the client to track these ID's
     *
     * @param req is the http request
     * @return the names of the uploaded files or an error message
     */
    @POST
    @Path("slice")
    public Response slice(@Context HttpServletRequest req) {
        // parse input JSON stream
        JSONObject jo = parseJSONStream(req);

        //TODO: call PlatformExecutable to preform actual slice after verifying file ID's
        String settings = jo.getString("settings"); // or something...

        return Response.ok().build();
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

            // parse string to json object
            JSONObject jo = JSONFactory.jsonObject(data);

            return jo;

        } catch (IOException | JSONException e) {
            log.severe(e.getMessage());
            throw new JSONException("Stream parse error.");
        }

    }


}
