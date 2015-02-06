/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.transform.HSVColorToRGBColorTransformer;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.hal.unit.AmbientLightInterface;
import de.citec.jul.rsb.RSBRemoteService;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.NotAvailableException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.slf4j.LoggerFactory;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.AmbientLightType;
import rst.homeautomation.states.PowerType;
import rst.vision.HSVColorType;
import rst.vision.HSVColorType.HSVColor;

/**
 *
 * @author mpohling
 */
public class AmbientLightRemote extends RSBRemoteService<AmbientLightType.AmbientLight> implements AmbientLightInterface {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(DALService.class);

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AmbientLightType.AmbientLight.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(HSVColorType.HSVColor.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerType.Power.getDefaultInstance()));
    }

    public AmbientLightRemote() {
    }

    public void setColor(final java.awt.Color color) throws CouldNotPerformException {
        try {
            setColor(HSVColorToRGBColorTransformer.transform(color));
        } catch (RSBBindingException ex) {
            logger.warn("Could not set color!", ex);
        }
    }

    @Override
    public void setColor(final HSVColor color) throws CouldNotPerformException {
        callMethodAsync("setColor", color);
    }

    @Override
    public void notifyUpdated(AmbientLightType.AmbientLight data) {
    }

    @Override
    public HSVColor getColor() {
        return this.getData().getColor();
    }

    @Override
    public void setBrightness(double brightness) throws Exception {
        callMethodAsync("setBrightness", new Double(brightness));
    }

    @Override
    public double getBrightness() throws Exception {
        return this.getData().getColor().getValue();
    }

    @Override
    public PowerType.Power.PowerState getPowerState() throws CouldNotPerformException {
        return this.getData().getPowerState().getState();
    }

    @Override
    public void setPowerState(PowerType.Power.PowerState state) throws CouldNotPerformException {
        callMethodAsync("setPowerState", PowerType.Power.newBuilder().setState(state).build());
    }
}
