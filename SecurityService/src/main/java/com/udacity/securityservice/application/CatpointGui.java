package com.udacity.securityservice.application;


import com.udacity.imageservice.*;
import com.udacity.securityservice.data.PretendDatabaseSecurityRepositoryImpl;
import com.udacity.securityservice.data.SecurityRepository;
import com.udacity.securityservice.service.SecurityService;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

/**
 * This is the primary JFrame for the application that contains all the top-level JPanels.
 *
 * We're not using any dependency injection framework, so this class also handles constructing
 * all our dependencies and providing them to other classes as necessary.
 */
public class CatpointGui extends JFrame {



    public CatpointGui() {
        setLocation(100, 100);
        setSize(600, 850);
        setTitle("Very Secure App");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        IService imageService = new FakeImageService();

        SecurityRepository securityRepository = new PretendDatabaseSecurityRepositoryImpl();
        SecurityService securityService = new SecurityService(securityRepository, imageService);
        DisplayPanel displayPanel = new DisplayPanel(securityService);
        ControlPanel controlPanel = new ControlPanel(securityService);
        SensorPanel sensorPanel = new SensorPanel(securityService);
        ImagePanel imagePanel = new ImagePanel(securityService);

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new MigLayout());
        mainPanel.add(displayPanel, "wrap");
        mainPanel.add(imagePanel, "wrap");
        mainPanel.add(controlPanel, "wrap");
        mainPanel.add(sensorPanel);

        getContentPane().add(mainPanel);

    }
}
