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


import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.AlarmStateType.AlarmState;
import rst.homeautomation.state.SmokeStateType.SmokeState;
import rst.homeautomation.unit.SmokeDetectorType.SmokeDetector;

/**
 *
 * @author thuxohl
 */
public class SmokeDetectorController extends AbstractUnitController<SmokeDetector, SmokeDetector.Builder> implements SmokeDetectorInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeDetector.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(AlarmState.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(SmokeState.getDefaultInstance()));
    }

    public SmokeDetectorController(final UnitHost unitHost, SmokeDetector.Builder builder) throws org.openbase.jul.exception.InstantiationException, CouldNotPerformException {
        super(SmokeDetectorController.class, unitHost, builder);
    }

    public void updateSmokeAlarmStateProvider(final AlarmState value) throws CouldNotPerformException {
        logger.debug("Apply alarm state Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<SmokeDetector.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setSmokeAlarmState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply alarm state Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public AlarmState getSmokeAlarmState() throws NotAvailableException {
        try {
            return getData().getSmokeAlarmState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("smokealarmstate", ex);
        }
    }

    public void updateSmokeStateProvider(final SmokeState value) throws CouldNotPerformException {
        logger.debug("Apply smoke state Update[" + value + "] for " + this + ".");

        try (ClosableDataBuilder<SmokeDetector.Builder> dataBuilder = getDataBuilder(this)) {
            dataBuilder.getInternalBuilder().setSmokeState(value);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply smoke state Update[" + value + "] for " + this + "!", ex);
        }
    }

    @Override
    public SmokeState getSmokeState() throws NotAvailableException {
        try {
            return getData().getSmokeState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("smokestate", ex);
        }
    }
}