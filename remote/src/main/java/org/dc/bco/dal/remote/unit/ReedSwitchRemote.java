/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.remote.unit;

import org.dc.bco.dal.lib.layer.unit.ReedSwitchInterface;
import org.dc.jul.exception.CouldNotPerformException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ReedSwitchStateType.ReedSwitchState;
import rst.homeautomation.unit.ReedSwitchType.ReedSwitch;

/**
 *
 * @author thuxohl
 */
public class ReedSwitchRemote extends AbstractUnitRemote<ReedSwitch> implements ReedSwitchInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ReedSwitch.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ReedSwitchState.getDefaultInstance()));
    }

    public ReedSwitchRemote() {
    }

    @Override
    public void notifyUpdated(ReedSwitch data) {
    }

    @Override
    public ReedSwitchState getReedSwitch() throws CouldNotPerformException {
        return getData().getReedSwitchState();
    }

}