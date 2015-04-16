/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal;

import de.citec.dal.util.ConnectionManager;
import de.citec.dal.registry.DeviceRegistry;
import de.citec.jul.exception.InitializationException;
import de.citec.jul.exception.InstantiationException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author mpohling
 */
public class DALServiceTest {
    
    private DALService dALService;
    
    public DALServiceTest() {
        
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() throws InitializationException, InstantiationException {
        dALService = new DALService();
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of activate method, of class DALService.
     */
    @Test
    public void testActivate() throws InitializationException, InstantiationException {
        System.out.println("activate");
        DALService instance = new DALService();
        instance.activate();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of deactivate method, of class DALService.
     */
    @Test
    public void testDeactivate() throws InitializationException, InstantiationException {
        System.out.println("deactivate");
        DALService instance = new DALService();
        instance.deactivate();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getRegistry method, of class DALService.
     * @throws de.citec.jul.exception.InitializationException
     */
    @Test
    public void testGetRegistry() throws InitializationException, InstantiationException {
        System.out.println("getRegistry");
        DALService instance = new DALService();
        DeviceRegistry expResult = null;
        DeviceRegistry result = instance.getDeviceRegistry();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getHardwareManager method, of class DALService.
     */
    @Test
    public void testGetHardwareManager() throws InitializationException, InstantiationException {
        System.out.println("getHardwareManager");
        DALService instance = new DALService();
        ConnectionManager expResult = null;
        ConnectionManager result = instance.getConnectionManager();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of main method, of class DALService.
     */
    @Test
    public void testMain() throws Throwable {
        System.out.println("main");
        String[] args = null;
        DALService.main(args);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
}
