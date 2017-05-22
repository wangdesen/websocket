package org.example.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Named;
import javax.websocket.Session;

import org.example.model.Device;
import org.json.*; 

@ApplicationScoped
@Named
public class DeviceSessionHandler {
	
	private DeviceSessionHandler() {
		System.out.println("LazySingleton is create");
	}
	
	//对于静态变量instance赋值为null，确保在系统启动时没有额外的加载
	private static DeviceSessionHandler instance = null;

	//getInstance必须是同步的，因为在多线程环境下可能存在多个实例被创建
	public static synchronized DeviceSessionHandler getInstance() {
		if (instance == null) {
			instance = new DeviceSessionHandler();
		}
		return instance;
	}
	

	private int deviceId = 0;

	private final Set<Session> sessions = new HashSet<>();

	private final Set<Device> devices = new HashSet<>();

	public void addSession(Session session) {
		sessions.add(session);
        for (Device device : devices) {
            //JsonObject addMessage = createAddMessage(device);
            JSONObject addMessage = new JSONObject(device);
            sendToSession(session, addMessage);
        }
	}

	public void removeSession(Session session) {
		sessions.remove(session);
	}

	public List<Device> getDevices() {
		return new ArrayList<>(devices);
	}

	public void addDevice(Device device) {
		device.setId(deviceId);
        devices.add(device);
        deviceId++;
        JSONObject addMessage = new JSONObject(device);
        sendToAllConnectedSessions(addMessage);
	}

	public void removeDevice(int id) {
		Device device = getDeviceById(id);
        if (device != null) {
            devices.remove(device);
            JSONObject removeMessage = new JSONObject(device);
            sendToAllConnectedSessions(removeMessage);
        }
	}

	public void toggleDevice(int id) {
		 //JsonProvider provider = JsonProvider.provider();
	        Device device = getDeviceById(id);
	        if (device != null) {
	            if ("On".equals(device.getStatus())) {
	                device.setStatus("Off");
	            } else {
	                device.setStatus("On");
	            }
	            JSONObject updateDevMessage = new JSONObject(device);
	            /*JsonObject updateDevMessage = provider.createObjectBuilder()
	                    .add("action", "toggle")
	                    .add("id", device.getId())
	                    .add("status", device.getStatus())
	                    .build();*/
	            sendToAllConnectedSessions(updateDevMessage);
	        }
	}

	private Device getDeviceById(int id) {
		for (Device device : devices) {
            if (device.getId() == id) {
                return device;
            }
        }
        return null;
	}

	/*private JsonObject createAddMessage(Device device) {
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
	}*/

	private void sendToAllConnectedSessions(JSONObject message) {
		for (Session session : sessions) {
            sendToSession(session, message);
        }
	}

	private void sendToSession(Session session, JSONObject message) {
		try {
            session.getBasicRemote().sendText(message.toString());
        } catch (IOException ex) {
            sessions.remove(session);
            Logger.getLogger(DeviceSessionHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
	}

}
