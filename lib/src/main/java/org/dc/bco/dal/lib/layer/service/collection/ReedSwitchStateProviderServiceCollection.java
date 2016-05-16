/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.collection;

/*
 * #%L
 * DAL Library
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
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

import java.util.Collection;
import org.dc.bco.dal.lib.layer.service.provider.ReedSwitchProvider;
import org.dc.jul.exception.CouldNotPerformException;
import rst.homeautomation.state.ReedSwitchStateType.ReedSwitchState;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public interface ReedSwitchStateProviderServiceCollection extends ReedSwitchProvider {

    /**
     * Returns open if at least one of the reed switch providers returns open
     * and else no closed.
     *
     * @return
     * @throws CouldNotPerformException
     */
    @Override
    default public ReedSwitchState getReedSwitch() throws CouldNotPerformException {
        for (ReedSwitchProvider provider : getReedSwitchStateProviderServices()) {
            if (provider.getReedSwitch().getValue() == ReedSwitchState.State.OPEN) {
                return ReedSwitchState.newBuilder().setValue(ReedSwitchState.State.OPEN).build();
            }
        }
        return ReedSwitchState.newBuilder().setValue(ReedSwitchState.State.CLOSED).build();
    }

    public Collection<ReedSwitchProvider> getReedSwitchStateProviderServices();
}