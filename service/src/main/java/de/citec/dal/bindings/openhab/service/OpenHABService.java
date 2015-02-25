/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.bindings.openhab.service;

import de.citec.dal.bindings.openhab.AbstractOpenHABDeviceController;
import de.citec.dal.bindings.openhab.OpenhabBinding;
import de.citec.dal.bindings.openhab.OpenhabBindingInterface;
import de.citec.dal.bindings.openhab.transform.ItemTransformer;
import de.citec.dal.data.Location;
import de.citec.dal.hal.service.Service;
import de.citec.dal.hal.device.Device;
import de.citec.dal.hal.service.ServiceType;
import de.citec.dal.hal.unit.Unit;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InstantiationException;
import de.citec.jul.exception.NotAvailableException;
import de.citec.jul.exception.NotSupportedException;
import java.util.concurrent.Future;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rst.homeautomation.openhab.OpenhabCommandType;

/**
 *
 * @author mpohling
 * @param <ST> related service type
 */
public abstract class OpenHABService<ST extends Service & Unit> implements Service {

	private OpenhabBindingInterface openhabBinding;

	private final Logger logger = LoggerFactory.getLogger(getClass());

	protected final Device device;
	protected final ST unit;
	private final String itemName;
	private final ServiceType serviceType;

	public OpenHABService(final Device device, final ST unit) throws InstantiationException {
		try {
			this.device = device;
			this.unit = unit;
			this.serviceType = detectServiceType();
			this.itemName = ItemTransformer.generateItemName(device, unit, this);
			this.openhabBinding = OpenhabBinding.getInstance();
		} catch (CouldNotPerformException ex) {
			throw new InstantiationException(this, ex);
		}
	}

	public final ServiceType detectServiceType() throws NotSupportedException {
		return ServiceType.valueOfByServiceName(getClass().getSimpleName().replaceFirst("Service", "").replaceFirst("Provider", "").replaceFirst("Impl", ""));
	}

	public Device getDevice() {
		return device;
	}

	public ST getUnit() {
		return unit;
	}

	public String getItemID() {
		return itemName;
	}

	@Override
	public ServiceType getServiceType() {
		return serviceType;
	}

	public Future executeCommand(final OpenhabCommandType.OpenhabCommand.Builder command) throws CouldNotPerformException {
		if (itemName == null) {
			throw new NotAvailableException("itemID");
		}
		return executeCommand(itemName, command, OpenhabCommandType.OpenhabCommand.ExecutionType.SYNCHRONOUS);
	}

	public Future executeCommand(final String itemName, final OpenhabCommandType.OpenhabCommand.Builder command, final OpenhabCommandType.OpenhabCommand.ExecutionType type) throws CouldNotPerformException {
		if (command == null) {
			throw new CouldNotPerformException("Skip sending empty command!", new NullPointerException("Argument command is null!"));
		}

		if (openhabBinding == null) {
			throw new CouldNotPerformException("Skip sending command, binding not ready!", new NullPointerException("Argument rsbBinding is null!"));
		}

		logger.debug("Execute command: Setting item [" + this.itemName + "] to [" + command.getType().toString() + "]");
		return openhabBinding.executeCommand(command.setItem(itemName).setExecutionType(type).build());
	}
}
