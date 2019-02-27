package ro.top.service;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import ro.top.proxy.NotificationProxy;
import static ro.top.service.NotificationController.getInstance;

/**
 * The implementation for the NotificationProxy interface
 * @author Alexandru Topala
 */
public class NotificationService extends UnicastRemoteObject implements NotificationProxy {
    private static final long serialVersionUID = 1L;
    
    public NotificationService() throws RemoteException {        
    }
   
    @Override
    public void subscribe(String name, String clientId) throws Exception {
        // TODO : delete that
        Logger.getGlobal().log(Level.INFO, "Client {0} subscribed to topic {1} name", new Object[]{clientId, name});
        
        getInstance().subscribe(name, clientId);
    }

    @Override
    public void unsubscribe(String name, String clientId) {
        // TODO : delete that
        Logger.getGlobal().log(Level.INFO, "Client {0} unsubscribed from topic {1} name", new Object[]{clientId, name});
        
        getInstance().unsubscribe(name, clientId);
    }

    @Override
    public void deleteTopic(String name) {
        // TODO : delete that
        Logger.getGlobal().log(Level.INFO, "Topic {0} was silently deleted ", name);
        
        getInstance().deleteTopic(name, false, null);
    }
    
    @Override
    public void deleteTopic(String name, boolean notifySubscribers) {
        // TODO : delete that
        Logger.getGlobal().log(Level.INFO, "Topic {0} was {1} silentlty deleted", new Object[]{name, notifySubscribers ? "not" : ""});
        
        getInstance().deleteTopic(name, notifySubscribers, null);
    }
    
    @Override
    public void deleteTopic(String name, Object data) {
        // TODO : delete that
        Logger.getGlobal().log(Level.INFO, "Topic {0} was delete with {1}", new Object[]{name, data});
        
        getInstance().deleteTopic(name, true, data);
    }

    @Override
    public void notifyTopic(String name) {
        // TODO : deleta that
        Logger.getGlobal().log(Level.INFO, "Topic {0} was notified", name);
        
        getInstance().notifyTopic(name);
    }

    @Override
    public void dataNotifyTopic(Object data, String name) {
        // TODO : delete that
        Logger.getGlobal().log(Level.INFO, "Topic {0} was notified with {1}", new Object[]{name, data});
        
        getInstance().dataNotifyTopic(data, name);
    }

    @Override
    public boolean exists(String topicName) {
        return getInstance().exists(topicName);
    }

    @Override
    public int getSubscribersCount(String name) {
        return getInstance().getSubscribersCountForTopic(name);
    }

    @Override
    public String registerClient(String clientSocket) {  
        // TODO : delete that
        Logger.getGlobal().log(Level.INFO, "Client with socket {0} just registred", new Object[]{clientSocket});
                
        return getInstance().registerClient(clientSocket);
    }

    @Override
    public void unregisterClient(String clientId) {
        // TODO : delete that
        Logger.getGlobal().log(Level.INFO, "Client with id {0} just unregistred", new Object[]{clientId});
        
        getInstance().unregisterClient(clientId);
    }
    
}
