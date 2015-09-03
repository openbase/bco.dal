/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.hal.unit.SmokeDetectorInterface;
import de.citec.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.AlarmStateType.AlarmState;
import rst.homeautomation.state.SmokeStateType.SmokeState;
import rst.homeautomation.unit.SmokeDetectorType.SmokeDetector;
import rst.homeautomation.unit.TemperatureSensorType;

/**
 *
 * @author thuxohl
 */
public class SmokeDetectorRemote extends DALRemoteService<SmokeDetector> implements SmokeDetectorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TemperatureSensorType.TemperatureSensor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeState.getDefaultInstance()));
    }

    @Override
    public void notifyUpdated(SmokeDetector data) throws CouldNotPerformException {
    }

    @Override
    public AlarmState getSmokeAlarmState() throws CouldNotPerformException {
        return getData().getSmokeAlarmState();
    }

    @Override
    public SmokeState getSmokeState() throws CouldNotPerformException {
        return getData().getSmokeState();
    }
}