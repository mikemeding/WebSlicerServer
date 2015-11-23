package websocket;

import model.Device;

import javax.enterprise.context.ApplicationScoped;
import javax.json.JsonObject;
import javax.json.spi.JsonProvider;
import javax.websocket.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by mike on 10/30/15.
 */

@ApplicationScoped
public class DeviceSessionHandler {

    private Logger log = Logger.getLogger(DeviceSessionHandler.class.getName());
    private int deviceId = 0;
    private final Set<Session> sessions = new HashSet<>();
    private final Set<Device> devices = new HashSet<>();

    /**
     * Each client has its own session
     *
     * @param session
     */
    public void addSession(Session session) {
        sessions.add(session);
        for (Device device : devices) {
            JsonObject addMessage = createAddMessage(device);
            sendToSession(session, addMessage);
        }
    }

    public void removeSession(Session session) {
        sessions.remove(session);
    }


    /**
     * Retrieve the list of devices and their attributes.
     *
     * @return
     */
    public List getDevices() {
        return new ArrayList<>(devices);
    }

    /**
     * Add a device to the application.
     *
     * @param device
     */
    public void addDevice(Device device) {
        device.setId(deviceId);
        devices.add(device);
        deviceId++;
        JsonObject addMessage = createAddMessage(device);
        sendToAllConnectedSessions(addMessage);
    }

    /**
     * Remove a device from the application.
     *
     * @param id
     */
    public void removeDevice(int id) {
        Device device = getDeviceById(id);
        if (device != null) {
            devices.remove(device);
            JsonProvider provider = JsonProvider.provider();
            JsonObject removeMessage = provider.createObjectBuilder()
                    .add("action", "remove")
                    .add("id", id)
                    .build();
            sendToAllConnectedSessions(removeMessage);
        }
    }

    /**
     * Toggle the device status.
     *
     * @param id
     */
    public void toggleDevice(int id) {
        JsonProvider provider = JsonProvider.provider();
        Device device = getDeviceById(id);
        if (device != null) {
            if ("On".equals(device.getStatus())) {
                device.setStatus("Off");
            } else {
                device.setStatus("On");
            }
            JsonObject updateDevMessage = provider.createObjectBuilder()
                    .add("action", "toggle")
                    .add("id", device.getId())
                    .add("status", device.getStatus())
                    .build();
            sendToAllConnectedSessions(updateDevMessage);
        }
    }

    /**
     * Retrieve a device with a specific identifier.
     *
     * @param id
     * @return
     */
    private Device getDeviceById(int id) {
        for (Device device : devices) {
            if (device.getId() == id) {
                return device;
            }
        }
        return null;
    }

    /**
     * Build a JSON message for adding a device to the application.
     *
     * @param device
     * @return
     */
    private JsonObject createAddMessage(Device device) {
        JsonProvider provider = JsonProvider.provider();
        JsonObject addMessage = provider.createObjectBuilder()
                .add("action", "add")
                .add("id", device.getId())
                .add("name", device.getName())
                .add("type", device.getType())
                .add("status", device.getStatus())
                .add("description", device.getDescription())
                .build();
        return addMessage;
    }

    /**
     * Send an event message to all connected clients.
     *
     * @param message
     */
    private void sendToAllConnectedSessions(JsonObject message) {
        for (Session session : sessions) {
            sendToSession(session, message);
        }
    }

    /**
     * Send an event message to a client.
     *
     * @param session
     * @param message
     */
    private void sendToSession(Session session, JsonObject message) {
        try {
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException ex) {
            sessions.remove(session);
            log.log(Level.SEVERE, null, ex);
        }
    }

}
