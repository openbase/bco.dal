package org.openbase.bco.dal.remote.unit;

/*
 * #%L
 * DAL Remote
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
import org.openbase.jul.extension.rsb.com.AbstractIdentifiableRemote;
import org.openbase.jul.pattern.Factory;
import rst.homeautomation.unit.UnitConfigType.UnitConfig;

/**
 *
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public interface UnitRemoteFactory extends Factory<UnitRemote, UnitConfig> {

    /**
     * Creates and initializes an unit remote out of the given unit configuration.
     * @param config the unit configuration which defines the remote type and is used for the remote initialization.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     * * @deprecated use newInitializedInstance instead!
     */
    @Deprecated
    public AbstractIdentifiableRemote createAndInitUnitRemote(final UnitConfig config) throws CouldNotPerformException;

    /**
     * Creates an unit remote out of the given unit configuration.
     * @param config the unit configuration which defines the remote type.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     * @deprecated use newInstance instead!
     */
    @Deprecated
    public AbstractIdentifiableRemote createUnitRemote(final UnitConfig config) throws CouldNotPerformException;


     /**
     * Creates and initializes an unit remote out of the given unit configuration.
     * @param config the unit configuration which defines the remote type and is used for the remote initialization.
     * @return the new created unit remote.
     * @throws CouldNotPerformException
     * @throws java.lang.InterruptedException
     */
    public UnitRemote newInitializedInstance(final UnitConfig config) throws CouldNotPerformException, InterruptedException;

    /**
     * Creates an unit remote out of the given unit configuration.
     * @param config the unit configuration which defines the remote type.
     * @return the new created unit remote.
     * @throws InstantiationException
     */
    @Override
    public UnitRemote newInstance(final UnitConfig config) throws InstantiationException;
}
