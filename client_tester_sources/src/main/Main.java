/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import gui.ConfigFrame;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import ro.top.service.ClientNotificationController;

/**
 *
 * @author Marius
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            // TODO code application logic here
            new ConfigFrame().setVisible(true);
            System.setProperty("java.rmi.server.hostname", ClientNotificationController.getLocalIp());
        } catch (SocketException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
}
