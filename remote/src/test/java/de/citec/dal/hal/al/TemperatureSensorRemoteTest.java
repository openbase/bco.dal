/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.hal.al;

import de.citec.dal.DALService;
import de.citec.dal.data.Location;
import de.citec.dal.hal.device.fibaro.F_MotionSensorController;
import de.citec.dal.hal.unit.TemperatureSensorController;
import de.citec.dal.util.DALRegistry;
import de.citec.jps.core.JPService;
import de.citec.jps.properties.JPHardwareSimulationMode;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Ignore;
import org.slf4j.LoggerFactory;

/**
 *
 * @author thuxohl
 */
public class TemperatureSensorRemoteTest {

    private static final Location LOCATION = new Location("paradise");
    private static final String LABEL = "Temperature_Sensor_Unit_Test";

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(TemperatureSensorRemoteTest.class);

    private TemperatureSensorRemote temperatureSensorRemote;
    private DALService dalService;

    public TemperatureSensorRemoteTest() {
    }

    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
        JPService.registerProperty(JPHardwareSimulationMode.class, true);
        dalService = new DALService(new TemperatureSensorRemoteTest.DeviceInitializerImpl());
        dalService.activate();

        temperatureSensorRemote = new TemperatureSensorRemote();
        temperatureSensorRemote.init(LABEL, LOCATION);
        temperatureSensorRemote.activate();
    }

    @After
    public void tearDown() {
        dalService.deactivate();
        try {
            temperatureSensorRemote.deactivate();
        } catch (InterruptedException ex) {
            logger.warn("Could not deactivate temperature sensor remote: ", ex);
        }
    }

    /**
     * Test of notifyUpdated method, of class TemperatureSensorRemote.
     */
    @Ignore
    public void testNotifyUpdated() {
    }

    /**
     * Test of getTemperature method, of class TemperatureSensorRemote.
     *
     * @throws java.lang.Exception
     */
    @Test(timeout = 3000)
    public void testGetTemperature() throws Exception {
        System.out.println("getTemperature");
        float temperature = 37.0F;
        ((TemperatureSensorController) dalService.getRegistry().getUnits(TemperatureSensorController.class).iterator().next()).updateTemperature(temperature);
        while (!(temperatureSensorRemote.getTemperature() == temperature)) {
            Thread.yield();
        }
        assertTrue("The getter for the tamper switch state returns the wrong value!", temperatureSensorRemote.getTemperature() == temperature);
    }

    public static class DeviceInitializerImpl implements de.citec.dal.util.DeviceInitializer {

        @Override
        public void initDevices(final DALRegistry registry) {

            try {
                registry.register(new F_MotionSensorController("F_MotionSensor_000", LABEL, LOCATION));
            } catch (de.citec.jul.exception.InstantiationException ex) {
                logger.warn("Could not initialize unit test device!", ex);
            }
        }
    }
}