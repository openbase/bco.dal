/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal;

import com.google.protobuf.GeneratedMessage;
import de.citec.dal.RSBBindingConnection;
import de.citec.dal.RSBBindingInterface;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import de.citec.dal.data.Location;
import de.citec.dal.exception.RSBBindingException;
import de.citec.dal.exception.VerificatioinFailedException;
import de.citec.dal.hal.al.HardwareUnit;
import de.citec.dal.service.RSBCommunicationService;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import rsb.RSBException;
import rsb.patterns.LocalServer;

/**
 *
 * @author Divine <DivineThreepwood@gmail.com>
 * @param <B>
 */
public abstract class AbstractDeviceController<M extends GeneratedMessage, MB extends GeneratedMessage.Builder> extends RSBCommunicationService<M, MB> implements HardwareUnit {

    public final static String ID_SEPERATOR = "_";
    
    protected final String id;
    protected final String label;
    protected final String hardware_id;
    protected final String instance_id;
    protected final Location location;
    protected final Map<String, Method> halFunctionMapping;
    protected final Map<String, AbstractUnitController> unitMap;

    protected RSBBindingInterface rsbBinding = RSBBindingConnection.getInstance();

    public AbstractDeviceController(final String id, final String label, final Location location, final MB builder) throws RSBBindingException {
        super(generateScope(id, location), builder);
        this.id = id;
        this.label = label;
        this.hardware_id = parseHardwareId(id, getClass());
        this.instance_id = parseInstanceId(id);
        this.location = location;
        this.unitMap = new HashMap<>();
        this.halFunctionMapping = new HashMap<>();
         setField("id", id);
//        super.builder.setField(builder.getDescriptorForType().findFieldByName("label"), label); //TODO: Activate after rst integration
        
        try {
            init();
        } catch (RSBException ex) {
            throw new RSBBindingException("Could not init RSBCommunicationService!", ex);
        }

        try {
            initHardwareMapping();
        } catch (Exception ex) {
            throw new RSBBindingException("Could not apply hardware mapping for " + getClass().getSimpleName() + "!", ex);
        }
    }
    
    public final static String parseHardwareId(String id, Class<? extends AbstractDeviceController> hardware) throws VerificatioinFailedException {
        assert id != null;
        assert hardware != null;
        String hardwareId = hardware.getSimpleName().replace("Controller", "");
        
        /* verify given id */
        if(!id.startsWith(hardwareId)) {
            throw new VerificatioinFailedException("Given id ["+id+"] does not start with prefix ["+hardwareId+"]!");
        }
        
        return hardwareId;
    }
    
    public final static String parseInstanceId(String id) throws VerificatioinFailedException {
        String[] split = id.split(ID_SEPERATOR);
        String instanceId;
                
        try {
            instanceId = split[split.length-1];
        } catch (IndexOutOfBoundsException ex) {
            throw new VerificatioinFailedException("Given id ["+id+"] does not contain saperator ["+ID_SEPERATOR+"]");
        }
        
        /* verify instance id */
        try {
            Integer.parseInt(instanceId);
        } catch (NumberFormatException ex) {
            throw new VerificatioinFailedException("Given id ["+id+"] does not end with a instance nubmer!");
        }        
        return instanceId;
    }

    protected void register(final AbstractUnitController hardware) {
        unitMap.put(hardware.getId(), hardware);
    }

    @Override
    public Location getLocation() {
        return location;
    }

    public void internalReceiveUpdate(String itemName, State newState) {
        logger.debug("internalReceiveUpdate ["+itemName+"="+newState+"]");
        
        String id_suffix = itemName.replaceFirst(id + "_", "");
        Method relatedMethod = halFunctionMapping.get(id_suffix);

        if (relatedMethod == null) {
            logger.warn("Could not apply update: Related Method unknown!");
            return;
        }
        try {
            relatedMethod.invoke(this, newState);
        } catch (IllegalAccessException ex) {
            logger.error("Cannot acces related Method [" + relatedMethod.getName() + "]", ex);
        } catch (IllegalArgumentException ex) {
            logger.error("The given argument is [" +newState.getClass().getName() + "]!");
            logger.error("Does not match [" +relatedMethod.getParameterTypes()[0].getName()+ "] which is needed by [" + relatedMethod.getName() + "]!", ex);
        } catch (InvocationTargetException ex) {
            logger.error("The related method [" +relatedMethod.getName()+ "] throws an exceptioin!", ex);
        } catch (Exception ex) {
            logger.error("Fatal invokation error!", ex);
        }
    }

    @Override
    public String getId() {
        return id;
    }

    public String getLable() {
        return label;
    }
    
    @Override
    public void activate() throws RSBBindingException {
        try {
            super.activate();

            for (AbstractUnitController controller : unitMap.values()) {
                controller.activate();
            }
        } catch (Exception ex) {
            throw new RSBBindingException(ex);
        }
    }

    @Override
    public void deactivate() throws RSBBindingException {
        try {
            super.deactivate();

            for (AbstractUnitController controller : unitMap.values()) {
                controller.deactivate();
            }
        } catch (Exception ex) {
            throw new RSBBindingException(ex);
        }
    }

    @Override
    public void registerMethods(LocalServer server) {
        // dummy construct: For registering methods overwrite this method.
    }

    public void postCommand(String itemName, Command command) throws RSBBindingException {
        rsbBinding.postCommand(itemName, command);
    }

    public void sendCommand(String itemName, Command command) throws RSBBindingException {
        rsbBinding.sendCommand(itemName, command);
    }
    
    @Override
    public String getHardware_id() {
        return hardware_id;
    }

    @Override
    public String getInstance_id() {
        return instance_id;
    }

    protected abstract void initHardwareMapping() throws NoSuchMethodException, SecurityException;
    
    @Override
    public String toString() {
        return getClass().getSimpleName()+"[id:"+id+"|scope:"+getLocation().getScope()+"]";
    }
}