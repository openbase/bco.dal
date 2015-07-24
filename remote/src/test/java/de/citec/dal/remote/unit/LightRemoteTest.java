/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.remote.unit;

import de.citec.dal.registry.MockRegistry;
import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.unit.LightController;
import de.citec.dal.registry.MockFactory;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import de.citec.jul.exception.CouldNotPerformException;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InvalidStateException;
import de.citec.jul.exception.NotAvailableException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import rst.homeautomation.state.PowerStateType.PowerState;

/**
 *
 * @author thuxohl
 */
public class LightRemoteTest {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(LightRemoteTest.class);

    private static LightRemote lightRemote;
    private static DALService dalService;
    private static MockRegistry registry;
    private static Location location;
    private static String label;

    public LightRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() throws InitializationException, InvalidStateException, de.citec.jul.exception.InstantiationException, CouldNotPerformException, InterruptedException {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        registry = MockFactory.newMockRegistry();

        dalService = new DALService();
        dalService.init();
        

        location = new Location(registry.getLocation());
        label = MockRegistry.LIGHT_LABEL;

        lightRemote = new LightRemote();
        lightRemote.init(label, location);
        lightRemote.activate();
    }

    @AfterClass
    public static void tearDownClass() throws CouldNotPerformException, InterruptedException {
        if (dalService != null) {
            dalService.shutdown();
        }
        if (lightRemote != null) {
            lightRemote.shutdown();
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
     * Test of setPowerState method, of class LightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testSetPowerState() throws Exception {
        System.out.println("setPowerState");
        PowerState.State state = PowerState.State.ON;
        lightRemote.setPower(state);
        lightRemote.requestStatus();
        assertEquals("Power has not been set in time!", state, lightRemote.getData().getPowerState().getValue());
    }

    /**
     * Test of gsetPowerState method, of class LightRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 60000)
    public void testGetPowerState() throws Exception {
        System.out.println("getPowerState");
        PowerState.State state = PowerState.State.OFF;
        ((LightController) dalService.getUnitRegistry().get(lightRemote.getId())).updatePower(state);
        lightRemote.requestStatus();
        assertEquals("Light has not been set in time!", state, lightRemote.getPower().getValue());
    }
}
