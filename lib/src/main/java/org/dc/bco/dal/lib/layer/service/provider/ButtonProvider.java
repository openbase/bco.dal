/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.lib.layer.service.provider;

import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.InvocationFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rsb.Event;
import rsb.patterns.EventCallback;
import rst.homeautomation.state.ButtonStateType.ButtonState;

/**
 *
 * @author thuxohl
 */
public interface ButtonProvider extends Provider {

    public ButtonState getButton() throws CouldNotPerformException;

    public class GetButtonCallback extends EventCallback {

        private static final Logger logger = LoggerFactory.getLogger(GetButtonCallback.class);

        private final ButtonProvider provider;

        public GetButtonCallback(final ButtonProvider provider) {
            this.provider = provider;
        }

        @Override
        public Event invoke(final Event request) throws Throwable {
            try {
                return new Event(ButtonState.class, provider.getButton());
            } catch (Exception ex) {
                throw ExceptionPrinter.printHistoryAndReturnThrowable(new InvocationFailedException(this, provider, ex), logger);
            }
        }
    }
}