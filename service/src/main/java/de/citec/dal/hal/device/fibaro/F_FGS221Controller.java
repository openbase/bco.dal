/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.device.fibaro;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.CouldNotTransformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.device.fibaro.F_FGS_221Type;
import de.citec.jul.exception.InstantiationException;
import rst.homeautomation.device.DeviceConfigType.DeviceConfig;

/**
 *
 * @author mpohling
 */
public class F_FGS221Controller extends AbstractOpenHABDeviceController<F_FGS_221Type.F_FGS_221, F_FGS_221Type.F_FGS_221.Builder> {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(F_FGS_221Type.F_FGS_221.getDefaultInstance()));
    }

    public F_FGS221Controller(final DeviceConfig config) throws InstantiationException, CouldNotTransformException {
        super(config, F_FGS_221Type.F_FGS_221.newBuilder());
        try {
            registerUnits(config.getUnitConfigList());
        } catch (CouldNotPerformException ex) {
            throw new InstantiationException(this, ex);
        }
    }

    
}
