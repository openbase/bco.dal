package org.openbase.bco.dal.lib.simulation.service;

/*-
 * #%L
 * BCO DAL Library
 * %%
 * Copyright (C) 2014 - 2021 openbase.org
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

import org.openbase.bco.dal.lib.layer.unit.UnitController;
import org.openbase.jul.exception.NotAvailableException;
import org.openbase.type.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import org.openbase.type.domotic.state.ColorStateType.ColorState;
import org.openbase.type.vision.ColorType.Color;
import org.openbase.type.vision.HSBColorType;

/**
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 *
 * Custom unit simulator.
 */
public class ColorStateServiceSimulator extends AbstractScheduledServiceSimulator<ColorState> {

    /**
     * Creates a new custom unit simulator.
     * @param unitController the unit to simulate.
     */
    public ColorStateServiceSimulator(UnitController unitController) {
        super(unitController, ServiceType.COLOR_STATE_SERVICE);
    }

    /**
     * {@inheritDoc }
     *
     * @return {@inheritDoc }
     * @throws NotAvailableException {@inheritDoc }
     */
    @Override
    protected ColorState getNextServiceState() throws NotAvailableException {
        return ColorState.newBuilder().setColor(Color.newBuilder().setHsbColor(HSBColorType.HSBColor.newBuilder()
                .setBrightness(RANDOM.nextDouble())
                .setSaturation(RANDOM.nextDouble())
                .setHue(RANDOM.nextInt(360)).build()).build()).build();
    }
}
