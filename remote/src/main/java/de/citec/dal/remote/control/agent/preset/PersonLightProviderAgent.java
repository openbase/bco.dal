/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.control.agent.preset;

import de.citec.dal.remote.service.PowerServiceRemote;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.pattern.Observable;
import de.citec.lm.remote.LocationRegistryRemote;
import rst.homeautomation.control.agent.AgentConfigType.AgentConfig;
import rst.homeautomation.service.ServiceTemplateType.ServiceTemplate;
import rst.homeautomation.state.MotionStateType.MotionState;
import rst.homeautomation.state.MotionStateType.MotionStateOrBuilder;
import rst.homeautomation.state.PowerStateType;
import rst.homeautomation.unit.UnitTemplateType;

/**
 *
 * @author <a href="mailto:thuxohl@techfak.uni-bielefeld.com">Tamino Huxohl</a>
 */
public class PersonLightProviderAgent extends AbstractAgent {

    public static final double MINIMUM_LIGHT_THRESHOLD = 100;
    private MotionStateFutionProvider motionStateProvider;
    private PowerServiceRemote powerServiceRemote;

    public PersonLightProviderAgent(AgentConfig agentConfig) throws InstantiationException, CouldNotPerformException, InterruptedException {
        super(agentConfig);
    }

    @Override
    public void activate() throws CouldNotPerformException, InterruptedException {
        logger.info("Activating [" + getClass().getSimpleName() + "]");
        super.activate();

        LocationRegistryRemote locationRegistryRemote = new LocationRegistryRemote();
        locationRegistryRemote.init();
        locationRegistryRemote.activate();
        
        this.motionStateProvider = new MotionStateFutionProvider(locationRegistryRemote.getUnitConfigs(UnitTemplateType.UnitTemplate.UnitType.MOTION_SENSOR, agentConfig.getLocationId()));
        this.motionStateProvider.addObserver((Observable<MotionState> source, MotionState data) -> {
            notifyMotionStateChanged(data);
        });
        
        String homeId = locationRegistryRemote.getLocationConfigsByLabel("Home").stream().findFirst().get().getId();
        powerServiceRemote = new PowerServiceRemote();
        powerServiceRemote.init(locationRegistryRemote.getUnitConfigs(ServiceTemplate.ServiceType.POWER_SERVICE, homeId));

        motionStateProvider.activate();
        powerServiceRemote.activate();
        
        locationRegistryRemote.deactivate();
    }

    @Override
    public void deactivate() throws CouldNotPerformException, InterruptedException {
        logger.info("Deactivating [" + getClass().getSimpleName() + "]");
        powerServiceRemote.deactivate();
        motionStateProvider.shutdown();

        super.deactivate();
    }

    private void notifyMotionStateChanged(final MotionStateOrBuilder motionState) throws CouldNotPerformException {
        if (motionState.getValue() == MotionState.State.MOVEMENT) {
            powerServiceRemote.setPower(PowerStateType.PowerState.State.ON);
        } else {
            powerServiceRemote.setPower(PowerStateType.PowerState.State.OFF);
        }

        logger.info("detect: " + motionState.getValue());

    }
}
