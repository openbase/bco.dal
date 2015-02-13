/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.service;

import de.citec.dal.hal.provider.PowerProvider;
import de.citec.dal.hal.unit.AmbientLightController;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.rsb.RSBCommunicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.states.PowerType;

/**
 *
 * @author mpohling
 */
public interface PowerService extends Service, PowerProvider {

    

    public void setPower(final PowerType.Power.PowerState state) throws CouldNotPerformException;

	public class SetPowerCallback extends EventCallback {

		private static final Logger logger = LoggerFactory.getLogger(SetPowerCallback.class);

		private final PowerService service;

		public SetPowerCallback(final PowerService service) {
			this.service = service;
		}

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                service.setPower(((PowerType.Power) request.getData()).getState());
                return RSBCommunicationService.RPC_FEEDBACK_OK;
            } catch (Exception ex) {
                logger.warn("Could not invoke method [setPowerState] for [" + service+ "]", ex);
                throw ex;
            }
        }
    }

}