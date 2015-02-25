/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.device.Device;
import de.citec.jul.exception.InstantiationException;

/**
 *
 * @author mpohling
 */
public interface ServiceFactory {

    public abstract BrightnessService newBrightnessService(Device device, BrightnessService unit) throws InstantiationException;

    public abstract ColorService newColorService(Device device, ColorService unit) throws InstantiationException;

    public abstract PowerService newPowerService(Device device, PowerService unit) throws InstantiationException;
    
    public abstract OpeningRatioService newOpeningRatioService(Device device, OpeningRatioService unit) throws InstantiationException;
    
    public abstract ShutterService newShutterService(Device device, ShutterService unit) throws InstantiationException;
}
