package org.openbase.bco.dal.lib.layer.service.provider;

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
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.jul.annotation.RPCMethod;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.PowerConsumptionStateType.PowerConsumptionState;

import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_CONSUMPTION_STATE_SERVICE_VALUE;
import static rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType.POWER_STATE_SERVICE;

/**
 *
 * * @author <a href="mailto:pleminoq@openbase.org">Tamino Huxohl</a>
 */
public interface PowerConsumptionStateProviderService extends ProviderService {

    @RPCMethod
    default PowerConsumptionState getPowerConsumptionState() throws NotAvailableException {
        return (PowerConsumptionState) getServiceProvider().getServiceState(POWER_CONSUMPTION_STATE_SERVICE);
    }
}
