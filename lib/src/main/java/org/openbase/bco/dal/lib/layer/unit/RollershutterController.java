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
import org.openbase.bco.dal.lib.layer.service.operation.OpeningRatioOperationService;
import org.openbase.bco.dal.lib.layer.service.operation.ShutterOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.InitializationException;
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.ShutterStateType.ShutterState;
import rst.homeautomation.unit.RollershutterType.Rollershutter;
import rst.homeautomation.unit.UnitConfigType;

/**
 *
 * @author thuxohl
 */
public class RollershutterController extends AbstractUnitController<Rollershutter, Rollershutter.Builder> implements RollershutterInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(Rollershutter.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(ShutterState.getDefaultInstance()));
    }

    private ShutterOperationService shutterService;
    private OpeningRatioOperationService openingRatioService;

    public RollershutterController(final UnitHost unitHost, final Rollershutter.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(RollershutterController.class, unitHost, builder);
    }

    @Override
    public void init(UnitConfigType.UnitConfig config) throws InitializationException, InterruptedException {
        super.init(config);
        try {
            shutterService = getServiceFactory().newShutterService(this);
            openingRatioService = getServiceFactory().newOpeningRatioService(this);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    public void updateShutterProvider(final ShutterState value) throws CouldNotPerformException {
        logger.debug("Apply shutter Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Rollershutter.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setShutterState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply shutter Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setShutter(final ShutterState state) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to ShutterState [" + state + "]");
        return shutterService.setShutter(state);
    }

    @Override
    public ShutterState getShutter() throws NotAvailableException {
        try {
            return getData().getShutterState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("shutter", ex);
        }
    }

    public void updateOpeningRatioProvider(final Double value) throws CouldNotPerformException {
        logger.debug("Apply opening ratio Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<Rollershutter.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setOpeningRatio(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply opening ratio Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public Future<Void> setOpeningRatio(Double openingRatio) throws CouldNotPerformException {
        logger.debug("Setting [" + getLabel() + "] to OpeningRatio [" + openingRatio + "]");
        return openingRatioService.setOpeningRatio(openingRatio);
    }

    @Override
    public Double getOpeningRatio() throws NotAvailableException {
        try {
            return getData().getOpeningRatio();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("opening ratio", ex);
        }
    }
}