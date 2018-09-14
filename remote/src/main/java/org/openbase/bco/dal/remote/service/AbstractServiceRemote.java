package org.openbase.bco.dal.remote.service;

/*
 * #%L
 * BCO DAL Remote
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

import com.google.protobuf.Descriptors.EnumValueDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import com.google.protobuf.ProtocolMessageEnum;
import org.openbase.bco.dal.lib.action.ActionDescriptionProcessor;
import org.openbase.bco.dal.lib.jp.JPResourceAllocation;
import org.openbase.bco.dal.lib.layer.service.*;
import org.openbase.bco.dal.lib.layer.unit.*;
import org.openbase.bco.dal.remote.unit.Units;
import org.openbase.bco.registry.lib.util.UnitConfigProcessor;
import org.openbase.bco.registry.remote.Registries;
import org.openbase.jps.core.JPService;
import org.openbase.jps.exception.JPNotAvailableException;
import org.openbase.jul.exception.*;
import org.openbase.jul.exception.printer.ExceptionPrinter;
import org.openbase.jul.exception.printer.LogLevel;
import org.openbase.jul.extension.rsb.scope.ScopeGenerator;
import org.openbase.jul.extension.rst.processing.TimestampProcessor;
import org.openbase.jul.pattern.Observable;
import org.openbase.jul.pattern.ObservableImpl;
import org.openbase.jul.pattern.Observer;
import org.openbase.jul.pattern.Remote;
import org.openbase.jul.pattern.provider.DataProvider;
import org.openbase.jul.schedule.GlobalCachedExecutorService;
import org.openbase.jul.schedule.SyncObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.communicationpatterns.ResourceAllocationType.ResourceAllocation;
import rst.domotic.action.ActionDescriptionType.ActionDescription;
import rst.domotic.action.ActionFutureType.ActionFuture;
import rst.domotic.action.ActionReferenceType.ActionReference;
import rst.domotic.service.ServiceStateDescriptionType.ServiceStateDescription;
import rst.domotic.service.ServiceTemplateType.ServiceTemplate.ServiceType;
import rst.domotic.state.ActionStateType.ActionState;
import rst.domotic.state.EnablingStateType.EnablingState.State;
import rst.domotic.unit.UnitConfigType.UnitConfig;
import rst.domotic.unit.UnitTemplateType.UnitTemplate.UnitType;
import rst.timing.TimestampType.Timestamp;

import javax.xml.crypto.Data;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.openbase.bco.dal.lib.layer.service.ServiceStateProcessor.*;

/**
 * @param <S>  generic definition of the overall service type for this remote.
 * @param <ST> the corresponding state for the service type of this remote.
 * @author <a href="mailto:divine@openbase.org">Divine Threepwood</a>
 */
public abstract class AbstractServiceRemote<S extends Service, ST extends GeneratedMessage> implements ServiceRemote<S, ST> {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    private boolean active;
    private boolean filterInfrastructureUnits;
    private final ServiceType serviceType;
    private long connectionPing;
    private final Class<ST> serviceDataClass;
    private final Map<String, UnitRemote<?>> unitRemoteMap;
    private final Map<String, UnitRemote<?>> disabledUnitRemoteMap;
    private final Map<String, UnitRemote<?>> infrastructureUnitMap;
    private final Map<UnitType, List<S>> unitRemoteTypeMap;
    private final Map<String, S> serviceMap;
    private final Observer dataObserver;
    private final Observer unitConfigObserver;
    private final Observer connectionStateObserver;
    protected final ObservableImpl<DataProvider<ST>, ST> serviceStateObservable = new ObservableImpl<>();
    private final SyncObject syncObject = new SyncObject("ServiceStateComputationLock");
    private final SyncObject maintainerLock = new SyncObject("MaintainerLock");
    private final SyncObject connectionStateLock = new SyncObject("ConnectionStateLock");
    protected Object maintainer;

    /**
     * AbstractServiceRemote constructor.
     * Activates filtering infrastructure units per default.
     *
     * @param serviceType      The remote service type.
     * @param serviceDataClass The service data class.
     */
    public AbstractServiceRemote(final ServiceType serviceType, final Class<ST> serviceDataClass) {
        this(serviceType, serviceDataClass, true);
    }

    /**
     * AbstractServiceRemote constructor.
     *
     * @param serviceType               The remote service type.
     * @param serviceDataClass          The service data class.
     * @param filterInfrastructureUnits Flag determining if units marked as infrastructure will be used by this service remote.
     */
    public AbstractServiceRemote(final ServiceType serviceType, final Class<ST> serviceDataClass, final boolean filterInfrastructureUnits) {
        this.serviceType = serviceType;
        this.serviceDataClass = serviceDataClass;
        this.filterInfrastructureUnits = filterInfrastructureUnits;
        this.unitRemoteMap = new HashMap<>();
        this.unitRemoteTypeMap = new HashMap<>();
        this.disabledUnitRemoteMap = new HashMap<>();
        this.infrastructureUnitMap = new HashMap<>();
        this.serviceMap = new HashMap<>();
        this.dataObserver = (source, data) -> {
            updateServiceState();
        };
        this.unitConfigObserver = (source, data) -> {
            final UnitConfig unitConfig = (UnitConfig) data;
            updateIgnoredUnitMaps(unitConfig.getId(), disabledUnitRemoteMap, unitConfig.getEnablingState().getValue() == State.DISABLED);
            if (filterInfrastructureUnits) {
                updateIgnoredUnitMaps(unitConfig.getId(), infrastructureUnitMap, Units.getUnit(unitConfig, false).isInfrastructure());
            }
        };
        this.connectionStateObserver = (source, data) -> {
            synchronized (connectionStateLock) {
                connectionStateLock.notifyAll();
            }
        };
        this.serviceStateObservable.setExecutorService(GlobalCachedExecutorService.getInstance().getExecutorService());
    }


    /**
     * Compute the service state of this service collection if an underlying
     * service changes.
     *
     * @return the computed server state is returned.
     * @throws CouldNotPerformException if an underlying service throws an
     *                                  exception
     */
    protected abstract ST computeServiceState() throws CouldNotPerformException;

    /**
     * Compute the current service state and notify observer.
     *
     * @throws CouldNotPerformException if the computation fails
     */
    private void updateServiceState() throws CouldNotPerformException {
        final ST serviceState;
        synchronized (syncObject) {
            serviceState = computeServiceState();
        }
        serviceStateObservable.notifyObservers(serviceState);
        assert serviceStateObservable.isValueAvailable();
    }

    /**
     * @return the current service state
     * @throws NotAvailableException if the service state data has not been set at
     *                               least once.
     */
    @Override
    public ST getData() throws NotAvailableException {
        if (!serviceStateObservable.isValueAvailable()) {
            throw new NotAvailableException("Data");
        }
        return serviceStateObservable.getValue();
    }

    /**
     * Add an observer to get notifications when the service state changes.
     *
     * @param observer the observer which is notified
     */
    @Override
    public void addDataObserver(final Observer<DataProvider<ST>, ST> observer) {
        serviceStateObservable.addObserver(observer);
    }

    /**
     * Remove an observer for the service state.
     *
     * @param observer the observer which has been registered
     */
    @Override
    public void removeDataObserver(final Observer<DataProvider<ST>, ST> observer) {
        serviceStateObservable.removeObserver(observer);
    }

    @Override
    public void addServiceStateObserver(final ServiceType serviceType, final Observer<DataProvider<ST>, ST> observer) {
        try {
            if (serviceType != getServiceType()) {
                throw new VerificationFailedException("ServiceType[" + serviceType.name() + "] is not compatible with " + this);
            }
            addDataObserver(observer);
        } catch (final CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not add service state observer!", ex), logger);
        }
    }

    @Override
    public void removeServiceStateObserver(final ServiceType serviceType, final Observer<DataProvider<ST>, ST> observer) {
        try {
            if (serviceType != getServiceType()) {
                throw new VerificationFailedException("ServiceType[" + serviceType.name() + "] is not compatible with " + this);
            }
            addDataObserver(observer);
        } catch (final CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(new CouldNotPerformException("Could not remove service state observer!", ex), logger, LogLevel.WARN);
        }
    }

    @Override
    public Class<ST> getDataClass() {
        return serviceDataClass;
    }

    /**
     * Method request the data of all internal unit remotes.
     *
     * @param failOnError flag decides if an exception should be thrown in case one data request fails.
     * @return the recalculated server state data based on the newly requested data.
     * @throws CouldNotPerformException is thrown if non of the request was successful. In case the failOnError is set to true any request error throws an CouldNotPerformException.
     */
    @Override
    public CompletableFuture<ST> requestData(final boolean failOnError) throws CouldNotPerformException {
        final CompletableFuture<ST> requestDataFuture = new CompletableFuture<>();
        GlobalCachedExecutorService.submit(() -> {
            try {
                final List<Future> taskList = new ArrayList<>();
                MultiException.ExceptionStack exceptionStack = null;
                for (final Remote remote : getInternalUnits()) {
                    try {
                        taskList.add(remote.requestData());
                    } catch (CouldNotPerformException ex) {
                        MultiException.push(remote, ex, exceptionStack);
                    }
                }
                boolean noResponse = true;
                for (final Future task : taskList) {
                    try {
                        task.get();
                        noResponse = false;
                    } catch (ExecutionException ex) {
                        MultiException.push(task, ex, exceptionStack);
                    }
                }

                try {
                    MultiException.checkAndThrow("Could not request status of all internal remotes!", exceptionStack);
                } catch (MultiException ex) {
                    if (failOnError || noResponse) {
                        throw ex;
                    }
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not request data of all internal unit remotes!", ex), logger, LogLevel.WARN);
                }
                requestDataFuture.complete(getData());
            } catch (InterruptedException | CouldNotPerformException ex) {
                requestDataFuture.completeExceptionally(ex);
            }
        });

        return requestDataFuture;
    }

    /**
     * {@inheritDoc}
     *
     * @param unitConfig {@inheritDoc}
     * @throws InitializationException {@inheritDoc}
     * @throws InterruptedException    {@inheritDoc}
     */
    @Override
    public void init(final UnitConfig unitConfig) throws InitializationException, InterruptedException {
        try {
            if (unitRemoteMap.containsKey(unitConfig.getId()) || disabledUnitRemoteMap.containsKey(unitConfig.getId()) || infrastructureUnitMap.containsKey(unitConfig.getId())) {
                // skip duplicated units
                return;
            }

            verifyMaintainability();

            if (!verifyServiceCompatibility(unitConfig, serviceType)) {
                throw new NotSupportedException("UnitTemplate[" + serviceType.name() + "]", unitConfig.getLabel());
            }

            final UnitRemote<?> unitRemote = Units.getUnit(unitConfig, false);

            if (filterInfrastructureUnits && unitRemote.isInfrastructure()) {
                // filter is on so put on map and ignore for now
                infrastructureUnitMap.put(unitConfig.getId(), unitRemote);
                return;
            }

            if (!unitRemoteTypeMap.containsKey(unitRemote.getUnitType())) {
                unitRemoteTypeMap.put(unitRemote.getUnitType(), new ArrayList());
                for (UnitType superType : Registries.getTemplateRegistry().getSuperUnitTypes(unitRemote.getUnitType())) {
                    if (!unitRemoteTypeMap.containsKey(superType)) {
                        unitRemoteTypeMap.put(superType, new ArrayList<>());
                    }
                }
            }

            unitRemote.addConfigObserver(unitConfigObserver);

            if (unitConfig.getEnablingState().getValue() == State.DISABLED) {
                disabledUnitRemoteMap.put(unitConfig.getId(), unitRemote);
                return;
            }

            addUnitRemoteToActiveMaps(unitRemote);

            if (active) {
                unitRemote.addDataObserver(dataObserver);
                unitRemote.addConnectionStateObserver(connectionStateObserver);
            }
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * Add a unit remote to all according internal maps for active units. Active means that the remote will be
     * considered by this service remote.
     * This includes the unitRemoteMap, the serviceMap, to the unitRemoteTypeMap for each unit type and super type.
     *
     * @param unitRemote the unit remote added to the internal maps
     * @throws CouldNotPerformException thrown if the unit does not implement the service specified by this service remote
     */
    private void addUnitRemoteToActiveMaps(final UnitRemote<?> unitRemote) throws CouldNotPerformException {
        try {
            serviceMap.put(unitRemote.getId(), (S) unitRemote);
            unitRemoteTypeMap.get(unitRemote.getUnitType()).add((S) unitRemote);
            for (UnitType superType : Registries.getTemplateRegistry().getSuperUnitTypes(unitRemote.getUnitType())) {
                unitRemoteTypeMap.get(superType).add((S) unitRemote);
            }
        } catch (ClassCastException ex) {
            throw new NotSupportedException("ServiceInterface[" + serviceType.name() + "]", unitRemote, "Remote does not implement the service interface!", ex);
        }
        unitRemoteMap.put(unitRemote.getId(), unitRemote);
    }

    /**
     * Remove a unit remote from all according internal maps for active units. Active means that the remote will be
     * considered by this service remote.
     * This includes the unitRemoteMap, the serviceMap, to the unitRemoteTypeMap for each unit type and super type.
     *
     * @param unitId the id of the unit remote to be removed
     * @return the removed unit remote
     * @throws CouldNotPerformException thrown if the super unit types of the remote could not be resolved
     */
    private UnitRemote<?> removeUnitRemoteFromActiveMaps(final String unitId) throws CouldNotPerformException {
        // unit is now ignored
        final UnitRemote<?> unitRemote = unitRemoteMap.remove(unitId);

        // remove unit from maps
        serviceMap.remove(unitRemote.getId());
        unitRemoteTypeMap.get(unitRemote.getUnitType()).remove(unitRemote);
        for (UnitType superType : Registries.getTemplateRegistry().getSuperUnitTypes(unitRemote.getUnitType())) {
            unitRemoteTypeMap.get(superType).remove(unitRemote);
        }

        return unitRemote;
    }

    /**
     * Update a map containing ignored units. If the unit remote specified by the unit id is inside
     * the ignoredUnitMap and it is now longer ignored it will be removed from the map and added to the internal active maps.
     * If the unit remote is not contained in the ignoredUnitMap but should now be ignored it will be removed from
     * the active unit maps and added to the ignored map.
     *
     * @param unitId               the id of the unit tested
     * @param ignoredUnitRemoteMap the map containing ignored units
     * @param ignored              flag determining if the unit should now be ignored
     * @throws CouldNotPerformException
     */
    private void updateIgnoredUnitMaps(final String unitId, final Map<String, UnitRemote<?>> ignoredUnitRemoteMap, final boolean ignored) throws CouldNotPerformException {
        if (ignoredUnitRemoteMap.containsKey(unitId) && !ignored) {
            // unit is now longer ignored
            final UnitRemote<?> unitRemote = ignoredUnitRemoteMap.remove(unitId);

            // add unit to maps
            addUnitRemoteToActiveMaps(unitRemote);
        } else if (!ignoredUnitRemoteMap.containsKey(unitId) && ignored) {
            // unit is now ignored
            final UnitRemote<?> unitRemote = removeUnitRemoteFromActiveMaps(unitId);

            // put on filtered unit map
            ignoredUnitRemoteMap.put(unitRemote.getId(), unitRemote);
        }
    }

    /**
     * Initializes this service remote with a set of unit configurations. Each
     * of the units referred by the given configurations should provide the
     * service type of this service remote.
     *
     * @param configs a set of unit configurations.
     * @throws InitializationException is thrown if the service remote could not
     *                                 be initialized.
     * @throws InterruptedException    is thrown if the current thread is
     *                                 externally interrupted.
     */
    @Override
    public void init(final Collection<UnitConfig> configs) throws InitializationException, InterruptedException {
        try {
            verifyMaintainability();
            MultiException.ExceptionStack exceptionStack = null;
            for (UnitConfig config : configs) {
                try {
                    init(config);
                } catch (CouldNotPerformException ex) {
                    exceptionStack = MultiException.push(this, ex, exceptionStack);
                }
            }
            MultiException.checkAndThrow("Could not activate all service units!", exceptionStack);
        } catch (CouldNotPerformException ex) {
            throw new InitializationException(this, ex);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        verifyMaintainability();
        active = true;
        unitRemoteMap.values().stream().forEach((remote) -> {
            remote.addDataObserver(dataObserver);
            remote.addConnectionStateObserver(connectionStateObserver);
        });
        updateServiceState();
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public void activate(Object maintainer) throws InterruptedException, CouldNotPerformException {
        if (!isLocked() || this.maintainer.equals(maintainer)) {
            synchronized (maintainerLock) {
                unlock(maintainer);
                activate();
                lock(maintainer);
            }
        } else {
            throw new VerificationFailedException("[" + maintainer + "] is not the current maintainer of this remote");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     * @throws InterruptedException     {@inheritDoc}
     */
    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        verifyMaintainability();
        active = false;
        unitRemoteMap.values().stream().forEach(remote -> {
            remote.removeDataObserver(dataObserver);
            remote.removeConnectionStateObserver(connectionStateObserver);
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown() {
        try {
            verifyMaintainability();
            deactivate();
            unitRemoteMap.values().stream().forEach(remote -> remote.removeConfigObserver(unitConfigObserver));
            disabledUnitRemoteMap.values().stream().forEach(remote -> remote.removeConfigObserver(unitConfigObserver));
            infrastructureUnitMap.values().stream().forEach(remote -> remote.removeConfigObserver(unitConfigObserver));
        } catch (CouldNotPerformException | InterruptedException ex) {
            ExceptionPrinter.printHistory(new ShutdownException(this, ex), logger);
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return active;
    }

    @Override
    public void removeUnit(UnitConfig unitConfig) throws CouldNotPerformException, InterruptedException {
        UnitRemote unitRemote;
        if (unitRemoteMap.containsKey(unitConfig.getId())) {
            unitRemote = unitRemoteMap.remove(unitConfig.getId());

            serviceMap.remove(unitConfig.getId());
            unitRemoteTypeMap.get(unitConfig.getUnitType()).remove(unitRemote);
            for (UnitType superType : Registries.getTemplateRegistry().getSuperUnitTypes(unitRemote.getUnitType())) {
                unitRemoteTypeMap.get(superType).remove(unitRemote);
            }
        } else if (disabledUnitRemoteMap.containsKey(unitConfig.getId())) {
            unitRemote = disabledUnitRemoteMap.remove(unitConfig.getId());
            if (infrastructureUnitMap.containsKey(unitConfig.getId())) {
                infrastructureUnitMap.remove(unitConfig.getId());
            }
        } else if (infrastructureUnitMap.containsKey(unitConfig.getId())) {
            unitRemote = infrastructureUnitMap.remove(unitConfig.getId());
        } else {
            throw new NotAvailableException("UnitConfig[" + ScopeGenerator.generateStringRep(unitConfig.getScope()) + "]");
        }

        unitRemote.removeDataObserver(dataObserver);
        unitRemote.removeConnectionStateObserver(connectionStateObserver);
        unitRemote.removeConfigObserver(unitConfigObserver);
    }

    /**
     * Returns a collection of all internally used unit remotes.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    @Override
    public Collection<org.openbase.bco.dal.lib.layer.unit.UnitRemote> getInternalUnits() {
        return Collections.unmodifiableCollection(unitRemoteMap.values());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Collection<org.openbase.bco.dal.lib.layer.unit.UnitRemote> getInternalUnits(UnitType unitType) throws CouldNotPerformException {
        List<UnitRemote> unitRemotes = new ArrayList<>();
        for (UnitRemote unitRemote : unitRemoteMap.values()) {
            if (unitType == UnitType.UNKNOWN || unitType == unitRemote.getUnitType() || UnitConfigProcessor.isBaseUnit(unitRemote.getUnitType()) || Registries.getTemplateRegistry().getSubUnitTypes(unitType).contains(unitRemote.getUnitType())) {
                unitRemotes.add(unitRemote);
            }
        }
        return Collections.unmodifiableCollection(unitRemotes);
    }

    @Override
    public boolean hasInternalRemotes() {
        return !unitRemoteMap.isEmpty();
    }

    /**
     * Returns a collection of all internally used unit remotes.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    @Override
    public Collection<S> getServices() {
        return Collections.unmodifiableCollection(serviceMap.values());
    }

    /**
     * Returns a collection of all internally used unit remotes filtered by the
     * given unit type.
     *
     * @return a collection of unit remotes limited to the service interface.
     */
    @Override
    public Collection<S> getServices(final UnitType unitType) {
        if (unitType == UnitType.UNKNOWN) {
            return Collections.unmodifiableCollection(serviceMap.values());
        }

        if (!unitRemoteTypeMap.containsKey(unitType)) {
            return new ArrayList<>();
        }

        return Collections.unmodifiableCollection(unitRemoteTypeMap.get(unitType));
    }

    /**
     * Returns the service type of this remote.
     *
     * @return the remote service type.
     */
    @Override
    public ServiceType getServiceType() {
        return serviceType;
    }


    public Future<ActionFuture> applyAction(final ActionDescription actionDescription) throws CouldNotPerformException {
        return applyAction(actionDescription.toBuilder());
    }

    @Override
    public Future<ActionFuture> applyAction(final ActionDescription.Builder actionDescriptionBuilder) throws CouldNotPerformException {
        try {
            if (!actionDescriptionBuilder.getServiceStateDescription().getServiceType().equals(getServiceType())) {
                throw new VerificationFailedException("Service type is not compatible to given action config!");
            }

            if (!JPService.getProperty(JPResourceAllocation.class).getValue()) {
                final List<Future> actionFutureList = new ArrayList<>();
                final Message serviceAttribute = new ServiceJSonProcessor().deserialize(actionDescriptionBuilder.getServiceStateDescription().getServiceAttribute(), actionDescriptionBuilder.getServiceStateDescription().getServiceAttributeType());
                for (final UnitRemote unitRemote : getInternalUnits(actionDescriptionBuilder.getServiceStateDescription().getUnitType())) {


                    ActionDescriptionProcessor.updateActionDescription(actionDescriptionBuilder, serviceAttribute, actionDescriptionBuilder.getServiceStateDescription().getServiceType(), unitRemote);

                    ActionReference.Builder actionReference = ActionReference.newBuilder();
                    actionReference.setActionId(actionDescriptionBuilder.getId());
                    actionReference.setAuthority(actionDescriptionBuilder.getActionAuthority());
                    actionReference.setServiceStateDescription(actionDescriptionBuilder.getServiceStateDescription());
                    actionDescriptionBuilder.addActionChain(actionReference);

                    actionFutureList.add(unitRemote.applyAction(actionDescriptionBuilder.build()));
                }
                return GlobalCachedExecutorService.allOf(ActionFuture.getDefaultInstance(), actionFutureList);
            } else {
                Map<String, UnitRemote> scopeUnitMap = new HashMap();
                for (final UnitRemote unitRemote : getInternalUnits(actionDescriptionBuilder.getServiceStateDescription().getUnitType())) {
                    if (unitRemote instanceof MultiUnitServiceFusion) {
                        /*
                         * For units which control other units themselves, e.g. locations, do not list the unit itself
                         * but all units it controls. Because the resource allocation has to allocate all these units at the
                         * same time and all units would themselves again try to allocate themselves a token is used in the
                         * ResourceAllocation. But when an allocation has a token all following requests will just return the
                         * state of this allocation so that this token cannot be used to allocate more resources. That is why
                         * they all have to be allocated at the same time. Furthermore the allocation is done hierarchically. So
                         * an allocation for a location blocks everything else going on in that location and maybe not only the
                         * homeautomation.
                         */
                        MultiUnitServiceFusion multiUnitServiceFusion = (MultiUnitServiceFusion) unitRemote;
                        Collection<UnitRemote> units = (Collection<UnitRemote>) multiUnitServiceFusion.getServiceRemote(serviceType).getInternalUnits(actionDescriptionBuilder.getServiceStateDescription().getUnitType());
                        for (UnitRemote unit : units) {
                            scopeUnitMap.put(ScopeGenerator.generateStringRep(unit.getScope()), unit);
                        }
                    } else {
                        scopeUnitMap.put(ScopeGenerator.generateStringRep(unitRemote.getScope()), unitRemote);
                    }
                }

                // Setup ActionDescription with resource ids, token and slot
                ResourceAllocation.Builder resourceAllocation = actionDescriptionBuilder.getResourceAllocationBuilder();
                resourceAllocation.clearResourceIds();
                resourceAllocation.addAllResourceIds(scopeUnitMap.keySet());
                ActionDescriptionProcessor.updateResourceAllocationSlot(actionDescriptionBuilder);
                actionDescriptionBuilder.setActionState(ActionState.newBuilder().setValue(ActionState.State.INITIALIZED).build());

                UnitAllocation unitAllocation;
                final List<Future> actionFutureList = new ArrayList<>();
                final ActionFuture.Builder actionFuture = ActionFuture.newBuilder();
                switch (actionDescriptionBuilder.getMultiResourceAllocationStrategy().getStrategy()) {
                    case AT_LEAST_ONE:
                        logger.info("AT_LEAST_ONE!");

                        for (UnitRemote unitRemote : scopeUnitMap.values()) {
                            ActionDescription unitActionDescription = updateActionDescriptionForUnit(actionDescriptionBuilder.build(), unitRemote);
                            actionFuture.addActionDescription(unitActionDescription);
                            actionFutureList.add(unitRemote.applyAction(unitActionDescription));
                        }

                        logger.info("Waiting [" + actionDescriptionBuilder.getExecutionTimePeriod() / actionFutureList.size() + "]ms per future");
                        return GlobalCachedExecutorService.atLeastOne(actionFuture.build(), actionFutureList, actionDescriptionBuilder.getExecutionTimePeriod() / actionFutureList.size(), TimeUnit.MILLISECONDS);
                    case ALL_OR_NOTHING:
                        logger.info("ALL_OR_NOTHING!");
                        if (scopeUnitMap.isEmpty()) {
                            CompletableFuture<ActionFuture> completableFuture = new CompletableFuture<>();
                            completableFuture.complete(actionFuture.build());
                            return completableFuture;
                        }
                        // generate token for all or nothing allocation
                        ActionDescriptionProcessor.generateToken(actionDescriptionBuilder);
                        actionFuture.addActionDescription(actionDescriptionBuilder);

                        // Resource Allocation
                        unitAllocation = UnitAllocator.allocate(actionDescriptionBuilder, () -> {
                            for (UnitRemote unitRemote : scopeUnitMap.values()) {
                                ActionDescription unitActionDescription = updateActionDescriptionForUnit(actionDescriptionBuilder.build(), unitRemote);
                                actionFuture.addActionDescription(unitActionDescription);
                                actionFutureList.add(unitRemote.applyAction(unitActionDescription));
                            }

                            return GlobalCachedExecutorService.allOf(actionFuture.build(), actionFutureList).get();
                        });
                        return unitAllocation.getTaskExecutor().getFuture();

                    default:
                        throw new FatalImplementationErrorException("Resource allocation strategy[" + actionDescriptionBuilder.getMultiResourceAllocationStrategy().getStrategy().name() + "] not handled", this);
                }
            }
        } catch (CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not apply action!", ex);
        } catch (JPNotAvailableException ex) {
            throw new CouldNotPerformException("Could not access resource allocation property", ex);
        }
    }

    private ActionDescription updateActionDescriptionForUnit(ActionDescription actionDescription, UnitRemote unitRemote) throws CouldNotPerformException {
        // create new builder and copy fields
        ActionDescription.Builder unitActionDescription = ActionDescription.newBuilder(actionDescription);
        // get a new resource allocation id
        ActionDescriptionProcessor.updateResourceAllocationId(unitActionDescription);
        // update the action chain
        ActionDescriptionProcessor.updateActionChain(unitActionDescription, actionDescription);
        // resource ids should only contain that unit
        ResourceAllocation.Builder unitResourceAllocation = unitActionDescription.getResourceAllocationBuilder();
        unitResourceAllocation.clearResourceIds();
        unitResourceAllocation.addResourceIds(ScopeGenerator.generateStringRep(unitRemote.getScope()));
        // update the id in the serviceStateDescription to that of the unit
        ServiceStateDescription.Builder serviceStateDescription = unitActionDescription.getServiceStateDescriptionBuilder();
        serviceStateDescription.setUnitId((String) unitRemote.getId());

        return unitActionDescription.build();
    }

    /**
     * Method generates a new service state out of the compatible service providers provided the units referred by the {@code unitType}.
     *
     * @param unitType       the unit type to filter the service provider collection. Use UNKNOWN as wildcard.
     * @param neutralState   the neutral state which is only used if all other instances are in the neutral state.
     * @param effectiveState the effective state which is set if at least one provider is referring this state.
     * @return a new generated service state builder containing all fused states.
     * @throws CouldNotPerformException is thrown in case the fusion fails.
     */
    protected Message.Builder generateFusedState(final UnitType unitType, final ProtocolMessageEnum neutralState, final ProtocolMessageEnum effectiveState) throws CouldNotPerformException {

        try {
            // generate builder
            final Message.Builder stateBuilder = Services.generateServiceStateBuilder(getServiceType(), neutralState);

            // lookup field descriptors
            final FieldDescriptor valueDescriptor = stateBuilder.getDescriptorForType().findFieldByName(FIELD_NAME_VALUE);
            final FieldDescriptor mapFieldDescriptor = stateBuilder.getDescriptorForType().findFieldByName(FIELD_NAME_LAST_VALUE_OCCURRENCE);
            final FieldDescriptor timestampDescriptor = stateBuilder.getDescriptorForType().findFieldByName(FIELD_NAME_TIMESTAMP);

            // verify field descriptors
            if (valueDescriptor == null) {
                throw new NotAvailableException("Field[" + FIELD_NAME_VALUE + "] does not exist for type " + stateBuilder.getClass().getName());
            } else if (mapFieldDescriptor == null) {
                throw new NotAvailableException("Field[" + FIELD_NAME_LAST_VALUE_OCCURRENCE + "] does not exist for type " + stateBuilder.getClass().getName());
            } else if (timestampDescriptor == null) {
                throw new NotAvailableException("Field[" + FIELD_NAME_TIMESTAMP + "] does not exist for type " + stateBuilder.getClass().getName());
            }

            // pre init timestamp
            long timestamp = 0;

            FieldDescriptor mapEntryKeyDescriptor = null;
            FieldDescriptor mapEntryValueDescriptor = null;
            for (S service : getServices(unitType)) {

                // do not handle if data is not synced yet.
                if (!((UnitRemote) service).isDataAvailable()) {
                    continue;
                }

                // handle state
                final Message state = (Message) Services.invokeProviderServiceMethod(getServiceType(), service);
                if (state.getField(valueDescriptor).equals(effectiveState.getValueDescriptor())) {
                    stateBuilder.setField(valueDescriptor, state.getField(valueDescriptor));
                }

                // handle latest occurrence timestamps
                for (int i = 0; i < state.getRepeatedFieldCount(mapFieldDescriptor); i++) {
                    final Message entry = (Message) state.getRepeatedField(mapFieldDescriptor, i);

                    if (mapEntryKeyDescriptor == null) {
                        mapEntryKeyDescriptor = entry.getDescriptorForType().findFieldByName(FIELD_NAME_KEY);
                        mapEntryValueDescriptor = entry.getDescriptorForType().findFieldByName(FIELD_NAME_VALUE);

                        if (mapEntryKeyDescriptor == null) {
                            throw new NotAvailableException("Field[" + FIELD_NAME_KEY + "] does not exist for type " + entry.getClass().getName());
                        } else if (mapEntryValueDescriptor == null) {
                            throw new NotAvailableException("Field[" + FIELD_NAME_VALUE + "] does not exist for type " + entry.getClass().getName());
                        }
                    }

                    try {
                        ServiceStateProcessor.updateLatestValueOccurrence((EnumValueDescriptor) entry.getField(mapEntryKeyDescriptor), (Timestamp) entry.getField(mapEntryValueDescriptor), stateBuilder);
                    } catch (CouldNotPerformException ex) {
                        ExceptionPrinter.printHistory("Could not update latest occurrence timestamp of Entry[" + entry + "]", ex, logger);
                    }
                }

                // handle timestamp
                timestamp = Math.max(timestamp, ((Timestamp) state.getField(timestampDescriptor)).getTime());
            }

            // update final timestamp
            TimestampProcessor.updateTimestamp(timestamp, stateBuilder, TimeUnit.MICROSECONDS, logger);

            // return merged state
            return stateBuilder;
        } catch (final CouldNotPerformException ex) {
            throw new CouldNotPerformException("Could not fuse service state!", ex);
        }
    }

    /**
     * Method blocks until an initial data message was dataObserverreceived from
     * every remote controller.
     *
     * @throws CouldNotPerformException is thrown if any error occurs.
     * @throws InterruptedException     is thrown in case the thread is externally
     *                                  interrupted.
     */
    @Override
    public void waitForData() throws CouldNotPerformException, InterruptedException {
        if (unitRemoteMap.isEmpty()) {
            return;
        }

        for (UnitRemote remote : unitRemoteMap.values()) {
            remote.waitForData();
        }
        serviceStateObservable.waitForValue();
    }

    /**
     * Method blocks until an initial data message was received from every
     * remote controller or the given timeout is reached.
     *
     * @param timeout  maximal time to wait for the main controller data. After
     *                 the timeout is reached a TimeoutException is thrown.
     * @param timeUnit the time unit of the timeout.
     * @throws CouldNotPerformException is thrown in case the any error occurs,
     *                                  or if the given timeout is reached. In this case a TimeoutException is
     *                                  thrown.
     * @throws InterruptedException     is thrown in case the thread is externally
     *                                  interrupted.
     */
    @Override
    public void waitForData(final long timeout, final TimeUnit timeUnit) throws CouldNotPerformException, InterruptedException {
        if (unitRemoteMap.isEmpty()) {
            return;
        }
        //todo reimplement with respect to the given timeout.
        for (UnitRemote remote : unitRemoteMap.values()) {
            remote.waitForData(timeout, timeUnit);
        }
        serviceStateObservable.waitForValue(timeout, timeUnit);
    }

    /**
     * Checks if a server connection is established for every underlying remote.
     *
     * @return is true in case that the connection for every underlying remote
     * it established.
     */
    @Override
    public boolean isConnected() {
        return getInternalUnits().stream().noneMatch((unitRemote) -> (!unitRemote.isConnected()));
    }

    /**
     * Check if the data object is already available for every underlying
     * remote.
     *
     * @return is true in case that for every underlying remote data is
     * available.
     */
    @Override
    public boolean isDataAvailable() {
        if (!hasInternalRemotes()) {
            return false;
        }
        return serviceStateObservable.isValueAvailable();
    }

    public static boolean verifyServiceCompatibility(final UnitConfig unitConfig, final ServiceType serviceType) {
        return unitConfig.getServiceConfigList().stream().anyMatch((serviceConfig) -> (serviceConfig.getServiceDescription().getServiceType() == serviceType));
    }

    /**
     * {@inheritDoc}
     *
     * @throws VerificationFailedException {@inheritDoc}
     */
    @Override
    public void verifyMaintainability() throws VerificationFailedException {
        if (isLocked()) {
            throw new VerificationFailedException("Manipulation of " + this + "is currently not valid because the maintains is protected by another instance! "
                    + "Did you try to modify an instance which is locked by a managed instance pool?");
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public boolean isLocked() {
        synchronized (maintainerLock) {
            return maintainer != null;
        }
    }

    /**
     * {@inheritDoc}
     *
     * @throws CouldNotPerformException {@inheritDoc}
     */
    @Override
    public void lock(final Object maintainer) throws CouldNotPerformException {
        synchronized (maintainerLock) {
            if (this.maintainer != null) {
                throw new CouldNotPerformException("Could not lock remote for because remote is already locked by another instance!");
            }
            this.maintainer = maintainer;
        }
    }

    /**
     * Method unlocks this instance.
     *
     * @param maintainer the instance which currently holds the lock.
     * @throws CouldNotPerformException is thrown if the instance could not be
     *                                  unlocked.
     */
    @Override
    public void unlock(final Object maintainer) throws CouldNotPerformException {
        synchronized (maintainerLock) {
            if (this.maintainer != null && this.maintainer != maintainer) {
                throw new CouldNotPerformException("Could not unlock remote for because remote is locked by another instance!");
            }
            this.maintainer = null;
        }
    }

    public void setInfrastructureFilter(final boolean filterInfrastructureUnits) throws CouldNotPerformException {
        if (this.filterInfrastructureUnits != filterInfrastructureUnits) {
            this.filterInfrastructureUnits = filterInfrastructureUnits;
            if (filterInfrastructureUnits) {
                for (UnitRemote<?> unitRemote : new ArrayList<>(unitRemoteMap.values())) {
                    updateIgnoredUnitMaps(unitRemote.getId(), infrastructureUnitMap, unitRemote.isInfrastructure());
                }
            } else {
                for (UnitRemote<?> unitRemote : new ArrayList<>(infrastructureUnitMap.values())) {
                    updateIgnoredUnitMaps(unitRemote.getId(), infrastructureUnitMap, unitRemote.isInfrastructure());
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Future<Long> ping() {
        if (unitRemoteMap.isEmpty()) {
            return CompletableFuture.completedFuture(0l);
        }

        final List<Future<Long>> futurePings = new ArrayList<>();

        for (final UnitRemote remote : unitRemoteMap.values()) {
            if (remote.isConnected()) {
                futurePings.add(remote.ping());
            }
        }

        return GlobalCachedExecutorService.allOf(input -> {
            try {
                long sum = 0;
                for (final Future<Long> future : input) {
                    sum += future.get();
                }

                long ping;
                if (!input.isEmpty()) {
                    ping = sum / input.size();
                } else {
                    ping = 0;
                }
                connectionPing = ping;
                return ping;
            } catch (ExecutionException ex) {
                throw new CouldNotPerformException("Could not compute ping!", ex);
            }
        }, futurePings);
    }

    /**
     * {@inheritDoc}
     *
     * @return {@inheritDoc}
     */
    @Override
    public Long getPing() {
        return connectionPing;
    }

    /**
     * Returns a short instance description.
     *
     * @return a description as string.
     */
    @Override
    public String toString() {
        if (serviceType == null) {
            return getClass().getSimpleName() + "[serviceType: ? ]";
        }
        return getClass().getSimpleName() + "[serviceType:" + serviceType.name() + "]";
    }

    @Override
    public void waitForConnectionState(ConnectionState connectionState, long timeout) throws InterruptedException, TimeoutException {
        synchronized (connectionStateLock) {
            if (connectionState == getConnectionState()) {
                return;
            }

            final long startingTime = System.currentTimeMillis();
            long timeWaited = 0;
            while (connectionState != getConnectionState()) {
                if (timeout > 0) {
                    connectionStateLock.wait(timeout - timeWaited);

                    timeWaited = System.currentTimeMillis() - startingTime;
                    if (timeout - timeWaited <= 0) {
                        throw new TimeoutException();
                    }
                } else {
                    connectionState.wait();
                }
            }
        }
    }

    @Override
    public Message getServiceState(ServiceType serviceType) throws NotAvailableException {
        if (serviceType != this.serviceType) {
            throw new NotAvailableException("ServiceState", new InvalidStateException("ServiceType["+serviceType.name()+"] not compatible with "+ this));
        }
        return getData();
    }

    @Override
    public ServiceProvider getServiceProvider() {
        return this;
    }
}
