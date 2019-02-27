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
public class TopicAlreadyExistsException extends RuntimeException {
    public TopicAlreadyExistsException() {
        super("A topic with the same name already exists");
    }
}
