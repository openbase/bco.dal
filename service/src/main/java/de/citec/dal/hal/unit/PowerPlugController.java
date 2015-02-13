/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.unit;

import de.citec.dal.hal.device.DeviceInterface;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import rsb.Event;
import rsb.RSBException;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rsb.patterns.EventCallback;
import rsb.patterns.LocalServer;
import rst.homeautomation.PowerPlugType;
import rst.homeautomation.PowerPlugType.PowerPlug;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author mpohling
 */
public class PowerPlugController extends AbstractUnitController<PowerPlug, PowerPlug.Builder> implements PowerPlugInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerPlugType.PowerPlug.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(
                new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    public PowerPlugController(final String label, DeviceInterface hardwareUnit, PowerPlug.Builder builder) throws InstantiationException {
        super(PowerPlugController.class, label, hardwareUnit, builder);
    }

    @Override
    public void registerMethods(final LocalServer server) throws RSBException {
        server.addMethod("setPower", new SetPowerStateCallback());
    }

    public void updatePowerState(final PowerType.Power.PowerState state) {
        data.getPowerStateBuilder().setState(state);
        notifyChange();
    }

    @Override
    public void setPower(final PowerType.Power.PowerState state) throws CouldNotPerformException {
        logger.debug("Setting [" + label + "] to Power [" + state.name() + "]");
        throw new UnsupportedOperationException("Not supported yet.");
//        OpenhabCommand.Builder newBuilder = OpenhabCommand.newBuilder();
//        newBuilder.setOnOff(PowerStateTransformer.transform(state)).setType(OpenhabCommand.CommandType.ONOFF);
//        executeCommand(newBuilder);
    }

    @Override
    public PowerType.Power.PowerState getPowerState() throws CouldNotPerformException {
        return data.getPowerState().getState();
    }

    public class SetPowerStateCallback extends EventCallback {

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                PowerPlugController.this.setPower(((PowerType.Power) request.getData()).getState());
                return new Event(String.class, "Ok");
            } catch (Exception ex) {
                logger.warn("Could not invoke method for [" + PowerPlugController.this.getName()+ "}", ex);
                return new Event(String.class, "Failed");
            }
        }
    }
}