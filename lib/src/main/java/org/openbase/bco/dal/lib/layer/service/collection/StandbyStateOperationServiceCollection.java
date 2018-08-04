package org.openbase.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2018 openbase.org
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

import org.openbase.bco.dal.lib.layer.service.operation.StandbyStateOperationService;
import org.openbase.jul.exception.CouldNotPerformException;
import org.openbase.jul.exception.NotAvailableException;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.state.StandbyStateType.StandbyState;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;

import java.util.concurrent.Future;

/**
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface StandbyStateOperationServiceCollection extends StandbyStateOperationService {

    public Future<ActionFuture> setStandbyState(final StandbyState state, final UnitType unitType) throws CouldNotPerformException;

    /**
     * Returns running if at least one of the standbyServices is running and
     * else standby.
     *
     * @return
     *
     * @throws NotAvailableException
     */
    @Override
    default StandbyState getStandbyState() throws NotAvailableException {
        return StandbyStateOperationService.super.getStandbyState();
    }

    /**
     * Returns running if at least one of the standbyServices with given unitType is running and
     * else standby.
     *
     * @param unitType
     *
     * @return
     *
     * @throws NotAvailableException
     */
    StandbyState getStandbyState(final UnitType unitType) throws NotAvailableException;
}
