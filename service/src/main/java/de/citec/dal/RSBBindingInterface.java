/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.exception.RSBBindingException;
import java.util.concurrent.Future;
import org.openhab.core.types.State;
import rst.homeautomation.openhab.OpenhabCommandType.OpenhabCommand;

/**
 *
 * @author thuxohl
 */
public interface RSBBindingInterface {
            
    void internalReceiveUpdate(String itemName, State newState);
    
    Future executeCommand(OpenhabCommand command) throws RSBBindingException;
}
