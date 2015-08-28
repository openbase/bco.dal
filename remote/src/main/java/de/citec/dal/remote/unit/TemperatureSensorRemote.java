/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.TemperatureSensorInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.AlarmStateType.AlarmState;
import rst.homeautomation.unit.TemperatureSensorType.TemperatureSensor;

/**
 *
 * @author thuxohl
 */
public class TemperatureSensorRemote extends DALRemoteService<TemperatureSensor> implements TemperatureSensorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
    }

    public TemperatureSensorRemote() {
    }

    @Override
    public void notifyUpdated(TemperatureSensor data) {
    }

    @Override
    public Double getTemperature() throws CouldNotPerformException {
        return getData().getTemperature();
    }

    @Override
    public AlarmState getTemperatureAlarmState() throws CouldNotPerformException {
        return getData().getTemperatureAlarmState();
    }

}
