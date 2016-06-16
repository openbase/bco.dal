package org.openbase.bco.dal.lib.layer.unit;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 openbase.org
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-3.0.html>.
 * #L%
 */
import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.PowerStateType.PowerState;
import rst.homeautomation.unit.DimmerType.Dimmer;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class DimmerController extends AbstractUnitController<Dimmer, Dimmer.Builder> implements DimmerInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Dimmer.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
    }

    private PowerOperationService powerService;
    private BrightnessOperationService brightnessService;

    public DimmerController(final UnitHost unitHost, Dimmer.Builder builder) throws org.openbase.jul.exception.InstantiationException, CouldNotPerformException {
        super(DimmerController.class, unitHost, builder);
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            this.powerService = getServiceFactory().newPowerService(this);
            this.brightnessService = getServiceFactory().newBrightnessService(this);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void updatePowerProvider(final PowerState value) throws CouldNotPerformException {
        logger.debug("Apply power Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Dimmer.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setPowerState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply power Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setPower(final PowerState state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to Power [" + state + "]");
        return powerService.setPower(state);
    }

    @Override
    public PowerState getPower() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("power", ex);
        }
    }

    public void updateBrightnessProvider(final Double value) throws CouldNotPerformException {
        logger.debug("Apply dim Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Dimmer.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setValue(value);
            if (value == 0) {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.OFF);
            } else {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply dim Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setBrightness(Double brightness) throws CouldNotPerformException {
        return brightnessService.setBrightness(brightness);
    }

    @Override
    public Double getBrightness() throws NotAvailableException {
        try {
            return getData().getValue();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("brightness", ex);
        }
    }
}