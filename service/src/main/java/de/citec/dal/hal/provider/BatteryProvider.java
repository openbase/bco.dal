/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.provider;

import de.citec.jul.exception.CouldNotPerformException;

/**
 *
 * @author thuxohl
 */
public interface BatteryProvider extends Provider {

    public double getBattaryLevel() throws CouldNotPerformException;
}