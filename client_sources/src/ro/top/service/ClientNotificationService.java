package ro.top.service;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import ro.top.proxy.ClientNotificationProxy;

/**
 *
 * @author Alexandru Topala
 */
public class ClientNotificationService extends UnicastRemoteObject implements ClientNotificationProxy {
    private static final long serialVersionUID = 1L;
    
    public ClientNotificationService() throws RemoteException {        
    }

    @Override
    public void receiveNotification(String name) {
        ClientNotificationController.getInstance().receiveNotification(name);
    }

    @Override
    public void receiveDataNotification(Object data, String name) {
        ClientNotificationController.getInstance().receiveDataNotification(data, name);
    }

    
}
