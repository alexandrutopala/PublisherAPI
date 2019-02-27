/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ro.top.exception;

/**
 *
 * @author Marius
 */
public class UninitializedNotificationServerException extends RuntimeException {
    public UninitializedNotificationServerException() {
        super("Notification server couldn't start with current configurations");
    }
    
}
