/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dc.bco.dal.visual.service;

/*
 * #%L
 * DAL Visualisation
 * %%
 * Copyright (C) 2014 - 2016 DivineCooperation
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
import org.dc.bco.dal.lib.layer.service.StandbyService;
import org.dc.jul.exception.CouldNotPerformException;
import org.dc.jul.exception.printer.ExceptionPrinter;
import org.dc.jul.exception.InvalidStateException;
import org.dc.jul.processing.StringProcessor;
import java.awt.Color;
import java.util.concurrent.Callable;
import rst.homeautomation.state.StandbyStateType.StandbyState;

/**
 *
 * @author mpohling
 */
public class StandbyServicePanel extends AbstractServicePanel<StandbyService> {

    private static final StandbyState RUNNING = StandbyState.newBuilder().setValue(StandbyState.State.RUNNING).build();
    private static final StandbyState STANDBY = StandbyState.newBuilder().setValue(StandbyState.State.STANDBY).build();

    /**
     * Creates new form BrightnessService
     *
     * @throws org.dc.jul.exception.InstantiationException
     */
    public StandbyServicePanel() throws org.dc.jul.exception.InstantiationException {
        initComponents();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        standbyButton = new javax.swing.JButton();
        standbyStatePanel = new javax.swing.JPanel();
        standbyStatusLabel = new javax.swing.JLabel();

        standbyButton.setText("PowerButton");
        standbyButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                standbyButtonActionPerformed(evt);
            }
        });

        standbyStatePanel.setBackground(new java.awt.Color(204, 204, 204));
        standbyStatePanel.setBorder(new javax.swing.border.LineBorder(new java.awt.Color(0, 0, 0), 5, true));
        standbyStatePanel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N

        standbyStatusLabel.setFont(new java.awt.Font("Dialog", 1, 14)); // NOI18N
        standbyStatusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        standbyStatusLabel.setText("PowerState");
        standbyStatusLabel.setFocusable(false);
        standbyStatusLabel.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        javax.swing.GroupLayout standbyStatePanelLayout = new javax.swing.GroupLayout(standbyStatePanel);
        standbyStatePanel.setLayout(standbyStatePanelLayout);
        standbyStatePanelLayout.setHorizontalGroup(
            standbyStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(standbyStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
        );
        standbyStatePanelLayout.setVerticalGroup(
            standbyStatePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(standbyStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 23, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(standbyStatePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(standbyButton, javax.swing.GroupLayout.DEFAULT_SIZE, 287, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(standbyStatePanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(standbyButton)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void standbyButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_standbyButtonActionPerformed
        execute(new Callable<Void>() {

            @Override
            public Void call() throws Exception {
                try {
                    switch (getService().getStandby().getValue()) {
                        case STANDBY:
                            getService().setStandby(RUNNING);
                            break;
                        case RUNNING:
                        case UNKNOWN:
                            getService().setStandby(STANDBY);
                            break;
                        default:
                            throw new InvalidStateException("State[" + getService().getStandby().getValue().name() + "] is unknown.");
                    }
                } catch (CouldNotPerformException ex) {
                    ExceptionPrinter.printHistory(new CouldNotPerformException("Could not set standby state!", ex), logger);
                }
                return null;
            }
        });
    }//GEN-LAST:event_standbyButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton standbyButton;
    private javax.swing.JPanel standbyStatePanel;
    private javax.swing.JLabel standbyStatusLabel;
    // End of variables declaration//GEN-END:variables

    @Override
    protected void updateDynamicComponents() {
        System.out.println("updateDynamicComponents");

        try {
            switch (getService().getStandby().getValue()) {
                case STANDBY:
                    standbyStatusLabel.setForeground(Color.LIGHT_GRAY);
                    standbyStatePanel.setBackground(Color.BLUE.lightGray);
                    standbyButton.setText("Wakeup");
                    break;
                case RUNNING:
                    standbyStatusLabel.setForeground(Color.BLACK);
                    standbyStatePanel.setBackground(Color.GREEN.darker());
                    standbyButton.setText("Activate Standby");
                    break;

                case UNKNOWN:
                    standbyStatusLabel.setForeground(Color.BLACK);
                    standbyStatePanel.setBackground(Color.ORANGE.darker());
                    standbyButton.setText("Activate Standby");
                    break;
                default:
                    throw new InvalidStateException("State[" + getService().getStandby().getValue() + "] is unknown.");
            }
            standbyStatusLabel.setText("Current StandbyState = " + StringProcessor.transformUpperCaseToCamelCase(getService().getStandby().getValue().name()));
        } catch (CouldNotPerformException ex) {
            ExceptionPrinter.printHistory(ex, logger);
        }
    }
}
