/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.citec.dal.service;

import de.citec.dal.hal.AbstractUnitController;
import de.citec.dal.util.MultiException;
import de.citec.dal.util.NotAvailableException;
import de.citec.dal.util.Observable;
import de.citec.dal.util.Observer;
import java.util.ArrayList;
import org.slf4j.LoggerFactory;
import rsb.Scope;

/**
 *
 * @author thuxohl
 */
public class UnitPanel extends javax.swing.JPanel {

	protected final org.slf4j.Logger logger = LoggerFactory.getLogger(getClass());

	private final DALRegistry registry = DALRegistry.getInstance();

	private final Observable<Scope> observable;

	/**
	 * Creates new form UnitPanel
	 */
	public UnitPanel() {
		initComponents();
		observable = new Observable<>();
	}

	public void addObserver(Observer<Scope> observer) {
		observable.addObserver(observer);
	}

	public void removeObserver(Observer<Scope> observer) {
		observable.removeObserver(observer);
	}

	public void fillComboBox(Class<? extends AbstractUnitController> unitClass) {
		unitComboBox.removeAllItems();
		try {
			ArrayList<UnitContainer> unitNames = new ArrayList<>();
			for (AbstractUnitController unit : registry.getUnits(unitClass)) {
				unitNames.add(new UnitContainer(unit, unit.getLable()));
			}
			unitComboBox.setModel(new javax.swing.DefaultComboBoxModel(unitNames.toArray()));
			unitComboBox.setSelectedItem(0);
		} catch (NotAvailableException ex) {
			logger.error("Could not fill the UnitComboBox!", ex);
		}
	}

	public Scope getScope() {
		return ((UnitContainer) unitComboBox.getSelectedItem()).getUnit().getScope();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	@SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        unitComboBox = new javax.swing.JComboBox();

        setBorder(javax.swing.BorderFactory.createTitledBorder("Unit"));

        unitComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                unitComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(unitComboBox, 0, 447, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(unitComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void unitComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_unitComboBoxActionPerformed

		new Thread("ScopeUpdaterThread"){

			@Override
			public void run() {
				try {
					if (unitComboBox.getSelectedItem() != null) {
						observable.notifyObservers(((UnitContainer) unitComboBox.getSelectedItem()).getUnit().getScope());
					}
				} catch (MultiException ex) {
					logger.error("Could not set the scope for this unit!", ex);
				}
				logger.debug(getName()+ " finished successful.");
			}

		}.start();
    }//GEN-LAST:event_unitComboBoxActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox unitComboBox;
    // End of variables declaration//GEN-END:variables

	class UnitContainer {

		private final AbstractUnitController unit;
		private final String label;

		public UnitContainer(AbstractUnitController unit, String label) {
			this.unit = unit;
			this.label = label;
		}

		public AbstractUnitController getUnit() {
			return unit;
		}

		public String getLabel() {
			return label;
		}

		@Override
		public String toString() {
			return unit.getRelatedHardwareUnit().getLocation().getScope()+" -- "+ label;
		}
	}
}
