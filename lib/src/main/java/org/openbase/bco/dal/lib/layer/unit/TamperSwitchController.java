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
import org.openbase.jul.exception.InstantiationException;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.extension.protobuf.ClosableDataBuilder;
import rsb.converter.DefaultConverterRepository;
import rsb.converter.ProtocolBufferConverter;
import rst.homeautomation.state.TamperStateType.TamperState;
import rst.homeautomation.unit.TamperSwitchType.TamperSwitch;
import rst.timing.TimestampType;

/**
 *
 * @author thuxohl
 */
public class TamperSwitchController extends AbstractUnitController<TamperSwitch, TamperSwitch.Builder> implements TamperSwitchInterface {

    static {
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperSwitch.getDefaultInstance()));
        DefaultConverterRepository.getDefaultConverterRepository().addConverter(new ProtocolBufferConverter<>(TamperState.getDefaultInstance()));
    }

    public TamperSwitchController(final UnitHost unitHost, final TamperSwitch.Builder builder) throws InstantiationException, CouldNotPerformException {
        super(TamperSwitchController.class, unitHost, builder);
    }

    public void updateTamperProvider(final TamperState state) throws CouldNotPerformException {
        
        logger.debug("Apply tamper Update[" + state + "] for " + this + ".");
        
        try (ClosableDataBuilder<TamperSwitch.Builder> dataBuilder = getDataBuilder(this)) {
            
            TamperState.Builder tamperStateBuilder = dataBuilder.getInternalBuilder().getTamperStateBuilder();
            
            // Update value
            tamperStateBuilder.setValue(state.getValue());
            
            // Update timestemp if necessary
            if (state.getValue()== TamperState.State.TAMPER) {
                //TODO tamino: need to be tested! Please write an unit test.
                tamperStateBuilder.setLastDetection(TimestampType.Timestamp.newBuilder().setTime(System.currentTimeMillis()));
            }

            dataBuilder.getInternalBuilder().setTamperState(tamperStateBuilder);
        } catch (Exception ex) {
            throw new CouldNotPerformException("Could not apply tamper Update[" + state + "] for " + this + "!", ex);
        }
    }

    @Override
    public TamperState getTamper() throws NotAvailableException {
        try {
            return getData().getTamperState();
        } catch (CouldNotPerformException ex) {
            throw new NotAvailableException("tamper", ex);
        }
    }
}