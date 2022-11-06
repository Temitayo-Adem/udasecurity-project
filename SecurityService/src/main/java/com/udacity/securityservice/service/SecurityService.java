package com.udacity.securityservice.service;


import com.udacity.imageservice.*;

import com.udacity.securityservice.application.StatusListener;
import com.udacity.securityservice.data.AlarmStatus;
import com.udacity.securityservice.data.ArmingStatus;
import com.udacity.securityservice.data.SecurityRepository;
import com.udacity.securityservice.data.Sensor;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;


/**
 * Service that receives information about changes to the security system. Responsible for
 * forwarding updates to the repository and making any decisions about changing the system state.
 *
 * This is the class that should contain most of the business logic for our system, and it is the
 * class you will be writing unit tests for.
 */
public class SecurityService {

    private IService imageService;
    private SecurityRepository securityRepository;
    private Set<StatusListener> statusListeners = new HashSet<>();
    private boolean catSeen = false;

    public SecurityService(SecurityRepository securityRepository, IService imageService) {
        this.securityRepository = securityRepository;
        this.imageService = imageService;
    }

    /**
     * Sets the current arming status for the system. Changing the arming status
     * may update both the alarm status.
     * @param armingStatus
     */

    public void setArmingStatus(ArmingStatus armingStatus) {
        if (armingStatus == ArmingStatus.ARMED_HOME && catSeen || armingStatus == ArmingStatus.ARMED_AWAY && catSeen) {
            setAlarmStatus(AlarmStatus.ALARM);
        }
        ConcurrentSkipListSet<Sensor> sensors = new ConcurrentSkipListSet<>(getSensors());

        if(armingStatus == ArmingStatus.DISARMED) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
            securityRepository.setArmingStatus(armingStatus);
            return;

        }

        sensors.forEach(s -> activateArmed(s));
        sensors.forEach(s -> deactivateArmed(s));
        securityRepository.setArmingStatus(armingStatus);
        statusListeners.forEach(sl -> sl.sensorStatusChanged());


    }


    private void activateArmed(Sensor sensor) {
        if (!sensor.getActive()) {
            sensor.setActive(true);
            securityRepository.updateSensor(sensor);
        }
    }

    private void deactivateArmed(Sensor sensor) {
        if (sensor.getActive()) {
            sensor.setActive(false);
            securityRepository.updateSensor(sensor);
        }
    }



    /**
     * Internal method that handles alarm status changes based on whether
     * the camera currently shows a cat.
     * @param cat True if a cat is detected, otherwise false.
     */
    private void catDetected(Boolean cat) {
        catSeen = cat;
        if(cat && getArmingStatus() == ArmingStatus.ARMED_HOME) {
            setAlarmStatus(AlarmStatus.ALARM);
        } else if (!cat && getSensors().stream().allMatch(s->s.getActive().equals(false))) {
            setAlarmStatus(AlarmStatus.NO_ALARM);
        }
        statusListeners.forEach(sl -> sl.catDetected(cat));
    }



    /**
     * Register the StatusListener for alarm system updates from within the SecurityService.
     * @param statusListener
     */
    public void addStatusListener(StatusListener statusListener) {
        statusListeners.add(statusListener);
    }

    public void removeStatusListener(StatusListener statusListener) {
        statusListeners.remove(statusListener);
    }

    /**
     * Change the alarm status of the system and notify all listeners.
     * @param status
     */
    public void setAlarmStatus(AlarmStatus status) {
        securityRepository.setAlarmStatus(status);
        statusListeners.forEach(sl -> sl.notify(status));
    }

    /**
     * Internal method for updating the alarm status when a sensor has been activated.
     */
    private void handleSensorActivated() {
        if(securityRepository.getArmingStatus() == ArmingStatus.DISARMED) {
            return;
        }
        AlarmStatus currStatus = securityRepository.getAlarmStatus();
        switch(currStatus) {
            case NO_ALARM -> setAlarmStatus(AlarmStatus.PENDING_ALARM);
            case PENDING_ALARM -> setAlarmStatus(AlarmStatus.ALARM);
            default -> System.out.println("");
        }
    }

    /**
     * Internal method for updating the alarm status when a sensor has been deactivated
     */
    private void handleSensorDeactivated(Sensor sensor) {


        if (securityRepository.getAlarmStatus().equals(AlarmStatus.PENDING_ALARM)) {
            Set<Sensor> sensors = securityRepository.getSensors();
            sensors.remove(sensor);
            if (securityRepository.getSensors().stream().noneMatch(Sensor::getActive)) {
                setAlarmStatus(AlarmStatus.NO_ALARM);
            }
            sensors.add(sensor);
        }


    }

    /**
     * Change the activation status for the specified sensor and update alarm status if necessary.
     * @param sensor
     * @param active
     */
    public void changeSensorActivationStatus(Sensor sensor, Boolean active) {

        if(!sensor.getActive() && active || sensor.getActive() && active) {
            handleSensorActivated();
        } else if (sensor.getActive() && !active) {
            handleSensorDeactivated(sensor);
        }
        sensor.setActive(active);
        securityRepository.updateSensor(sensor);
    }



    /**
     * Send an image to the SecurityService for processing. The securityService will use it's provided
     * ImageService to analyze the image for cats and update the alarm status accordingly.
     * @param currentCameraImage
     */
    public void processImage(BufferedImage currentCameraImage) {
        catDetected(imageService.imageContainsCat(currentCameraImage, 50.0f));
    }

    public AlarmStatus getAlarmStatus() {
        return securityRepository.getAlarmStatus();
    }

    public Set<Sensor> getSensors() {
        return securityRepository.getSensors();
    }

    public void addSensor(Sensor sensor) {
        securityRepository.addSensor(sensor);
    }

    public void removeSensor(Sensor sensor) {
        securityRepository.removeSensor(sensor);
    }

    public ArmingStatus getArmingStatus() {
        return securityRepository.getArmingStatus();
    }
}
