/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.registry.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.HandleSensorController;
import de.citec.dal.registry.MockFactory;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.HandleStateType.HandleState;

/**
 *
 * @author thuxohl
 */
public class HandleSensorRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(HandleSensorRemoteTest.class);

    private static HandleSensorRemote handleSensorRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;

    public HandleSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException, CouldNotPerformException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockFactory.newMockRegistry();

        dalService = new DALService();
        dalService.init();
        dalService.activate();

        location = new Location(registry.getLocation());

        handleSensorRemote = new HandleSensorRemote();
        handleSensorRemote.init(MockRegistry.HANDLE_SENSOR_LABEL, location);
        handleSensorRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (dalService != null) {
            dalService.shutdown();
        }
        if (handleSensorRemote != null) {
            handleSensorRemote.shutdown();
        }
        if (registry != null) {
            MockFactory.shutdownMockRegistry();
        }
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of notifyUpdated method, of class HandleSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getRotaryHandleState method, of class HandleSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test
    public void testGetRotaryHandleState() throws Exception {
        System.out.println("getRotaryHandleState");
        HandleState.State state = HandleState.State.TILTED;
        ((HandleSensorController) dalService.getUnitRegistry().get(handleSensorRemote.getId())).updateHandle(state);
        handleSensorRemote.requestStatus();
        Assert.assertEquals("The getter for the handle state returns the wrong value!",state, handleSensorRemote.getHandle().getValue());
    }
}
