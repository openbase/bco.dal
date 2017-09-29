package org.openbase.bco.dal.lib.layer.unit;

import java.util.concurrent.Future;
import org.openbase.bco.dal.lib.layer.service.operation.BrightnessStateOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.PowerStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.exception.VerificationFailedException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import org.openbase.jul.schedule.FutureProcessor;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.BrightnessStateType.BrightnessState;
import rst.domotic.state.PowerStateType.PowerState;
import rst.domotic.unit.dal.DimmerDataType.DimmerData;
import rst.domotic.unit.UnitConfigType;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2017 openbase.org
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
/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public class DimmerController extends AbstractDALUnitController<DimmerData, DimmerData.Builder> implements Dimmer {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(DimmerData.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(PowerState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(BrightnessState.getDefaultInstance()));
    }

    private PowerStateOperationService powerStateService;
    private BrightnessStateOperationService brightnessStateService;

    public DimmerController(final UnitHost unitHost, DimmerData.Builder builder) throws org.openbase.jul.exception.InstantiationException, CouldNotPerformException {
        super(DimmerController.class, unitHost, builder);
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            this.powerStateService = getServiceFactory().newPowerService(this);
            this.brightnessStateService = getServiceFactory().newBrightnessService(this);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    @Override
    public Future<ActionFuture> setPowerState(PowerState state) throws CouldNotPerformException {
        try {
            verifyOperationServiceStateValue(state.getValue());
        } catch (VerificationFailedException ex) {
            return FutureProcessor.canceledFuture(ActionFuture.class, ex);
        }
        return powerStateService.setPowerState(state);
    }

    @Override
    public PowerState getPowerState() throws NotAvailableException {
        try {
            return getData().getPowerState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("powerState", ex);
        }
    }

    public void updatePowerStateProvider(final PowerState value) throws CouldNotPerformException {
        logger.debug("Apply powerState Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<DimmerData.Builder> dataBuilder = getDataBuilder(this)) {
            long transactionId = dataBuilder.getInternalBuilder().getPowerState().getTransactionId() + 1;
            dataBuilder.getInternalBuilder().setPowerState(value.toBuilder().setTransactionId(transactionId));
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply powerState Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<ActionFuture> setBrightnessState(BrightnessState brightnessState) throws CouldNotPerformException {
        return brightnessStateService.setBrightnessState(brightnessState);
    }

    @Override
    public BrightnessState getBrightnessState() throws NotAvailableException {
        try {
            return getData().getBrightnessState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("brightnessState", ex);
        }
    }

    public void updateBrightnessStateProvider(final BrightnessState brightnessState) throws CouldNotPerformException {
        logger.debug("Apply brightnessState Update[" + brightnessState + "] for " + this + ".");

        try (ClosableDataBuilder<DimmerData.Builder> dataBuilder = getDataBuilder(this)) {
            long transactionId = dataBuilder.getInternalBuilder().getBrightnessState().getTransactionId() + 1;
            dataBuilder.getInternalBuilder().setBrightnessState(brightnessState.toBuilder().setTransactionId(transactionId));
            if (brightnessState.getBrightness() == 0) {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.OFF);
            } else {
                dataBuilder.getInternalBuilder().getPowerStateBuilder().setValue(PowerState.State.ON);
            }
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply brightnessState Update[" + brightnessState + "] for " + this + "!", ex);
        }
    }
}
