package com.udacity.securityservice.service;
import com.udacity.imageservice.*;
import com.udacity.securityservice.data.*;

import static com.udacity.securityservice.data.AlarmStatus.NO_ALARM;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    private final String  testSensorName = "T";


    @Mock
    SecurityRepository securityRepository;
    @Mock IService imageService;
    private Sensor sensor;
    private SecurityService securityService;
    private int i = 0;

    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = new Sensor(testSensorName + i,SensorType.DOOR);
        i++;
    }

    /**
     * Parameterized test that ensure that SecurityService.changeSensorActivationStatus
     *method can change the AlarmStatus from NO_ALARM to Pending_ALARM
     * ArgumentCaptor is used to ensure that AlarmStatus.PENDING_ALARM is passed
     * into the SecurityRepository.setAlarmStatus method, and that the method is called once.
     *
     */
    @ParameterizedTest //covers 1
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void checkThatArmedSystemCanChangeAlarmStatusFromNoAlarmToPendingAlarm(ArmingStatus armingStatus){
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        when(securityService.getAlarmStatus()).thenReturn(NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, times(1)).setAlarmStatus(argumentCaptor.capture());

        assertEquals(argumentCaptor.getValue(), AlarmStatus.PENDING_ALARM );
    }

    /**
     * Covers 2
     * Test ensures that SecurityService.changeSensorActivationStatus
     * can change the AlarmStatus from PENDING_ALARM to ALARM
     *
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME","ARMED_AWAY"})
    void checkThatArmedSystemCanChangeAlarmStatusFromPendingToAlarm(ArmingStatus armingStatus) {
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,true);
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository,times(1)).setAlarmStatus(argumentCaptor.capture());
        assertEquals(argumentCaptor.getValue(),AlarmStatus.ALARM);
    }

    /**
     * Covers 3
     * This test ensures that the changeSensorActivationStatus method can set the AlarmStatus to NO_ALARM
     * from ALARM
     */
    @Test
    void checkThatPendingAlarmWithInactiveSensorCanChangeToNoAlarm()  {
        Set<Sensor> sensorSet = new HashSet<>();
        sensorSet.add(new Sensor("A",SensorType.DOOR));
        sensorSet.add(new Sensor("B",SensorType.WINDOW));
        sensorSet.add(new Sensor("C",SensorType.DOOR));
        Sensor tester = sensorSet.iterator().next();
        tester.setActive(true);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(tester,false);
        ArgumentCaptor<AlarmStatus> argumentCaptor = ArgumentCaptor.forClass((AlarmStatus.class));
        verify(securityRepository,times(1)).setAlarmStatus(argumentCaptor.capture());
        assertEquals(argumentCaptor.getValue(), AlarmStatus.NO_ALARM);
    }


    /**
     * Covers 4
     * Ensure that the setAlarmStatus method is not called when the Alarm is active
     * and sensor is activated or deactivated
     *
     */
    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    void checkThatSensorStateDoesNotAffectActiveAlarm(boolean state) {
        Sensor testSen = new Sensor("x",SensorType.DOOR);

        if (!state) {
            testSen.setActive(true);
        }


        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(testSen,state);
        verify(securityRepository,times(0)).setAlarmStatus(any());
        assertEquals(securityService.getAlarmStatus(), AlarmStatus.ALARM);

    }

    /**
     * Covers 5
     * Checks that if Sensor is active and AlarmStatus is Pending and
     * changeSensorActivation method is called, then the AlarmStatus will go from Pending to Alarm
     */
    @Test
    void checkThatActiveSensorWhenActivatedWillChangePendingAlarmToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        ArgumentCaptor<AlarmStatus> alarmStatusArgumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, times(1)).setAlarmStatus(alarmStatusArgumentCaptor.capture());
        assertEquals(alarmStatusArgumentCaptor.getValue(),AlarmStatus.ALARM);
    }

    /**
     * Covers 6
     * Testing that deactivating an already inactive alarm doesn't affect AlarmStatus
     *
     */
    @Test
    void checkThatInactiveSensorMakesNoChangesToAlarmState() {
        securityService.changeSensorActivationStatus(new Sensor("x",SensorType.DOOR), false);
        verify(securityRepository,times(0)).setAlarmStatus(any(AlarmStatus.class));

    }

    /**
     * Covers 7
     * Test to ensure than when a cat is detected
     * the security system goes into Alarm Mode
     */
    @Test
    void checkThatSystemPutIntoAlarmModeIfCatDetectedInArmedAtHomeMode() {
        BufferedImage catImage = new BufferedImage(1,1,BufferedImage.TYPE_3BYTE_BGR);
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(catImage);
        ArgumentCaptor<AlarmStatus> alarmStatusArgumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository,times(1)).setAlarmStatus(alarmStatusArgumentCaptor.capture());
        assertEquals(alarmStatusArgumentCaptor.getValue(),AlarmStatus.ALARM);
    }

    /**
     * Covers 8
     * Creating stub for getSensors/imageContainsCat methods to ensure
     * that if there is no cat detected, then all sensors are set to inactive
     */
    @Test
    void checkThatSystemChangesToNoAlarmIfNoCatDetectedAndAllSensorsAreInactive() {
        Set<Sensor> sensorSet = Set.of(new Sensor("A",SensorType.DOOR),new Sensor("B",SensorType.MOTION));
        BufferedImage notCatImage = new BufferedImage(1,1,BufferedImage.TYPE_3BYTE_BGR);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(false);
        securityService.processImage(notCatImage);
        ArgumentCaptor<AlarmStatus> alarmStatusArgumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository,times(1)).setAlarmStatus(alarmStatusArgumentCaptor.capture());
        assertEquals(alarmStatusArgumentCaptor.getValue(), AlarmStatus.NO_ALARM);
    }

    /**
    Covers 9
     */
    @Test
    void checkThatDisarmedSystemHasStatusSetToNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        ArgumentCaptor<AlarmStatus> alarmStatusArgumentCaptor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository,times(1)).setAlarmStatus(alarmStatusArgumentCaptor.capture());
        assertEquals(alarmStatusArgumentCaptor.getValue(),AlarmStatus.NO_ALARM);

    }

    /**
     * Covers 10
     *

     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void checkThatIfTheSystemIsArmedAllSensorsAreSetToInactive(ArmingStatus status) {
        Sensor a = new Sensor("A",SensorType.DOOR);
        a.setActive(true);
        Sensor b = new Sensor("B",SensorType.MOTION);
        b.setActive(true);
        Set<Sensor> sensorSet = new HashSet<>();
        sensorSet.add(a);
        sensorSet.add(b);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        securityService.setArmingStatus(status);

        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }

    /**
     * Test to make sure all are set to inactive if armed and set of sensors has
     * both active an inactive sensors
     *
     */
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void checkThatIfTheSystemIsArmedAllSensorsAreSetToInactiveEvenIfAlreadyInactive(ArmingStatus status) {

        Sensor a = new Sensor("A",SensorType.DOOR);
        a.setActive(true);
        Sensor b = new Sensor("B",SensorType.MOTION);
        b.setActive(true);
        Sensor c = new Sensor("C", SensorType.DOOR);
        Set<Sensor> sensorSet = new HashSet<>();
        sensorSet.add(a);
        sensorSet.add(b);
        sensorSet.add(c);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        securityService.setArmingStatus(status);

        securityService.getSensors().forEach(sensor -> assertFalse(sensor.getActive()));
    }



    /**
     * Covers 11
     */
    @Test
    void checkThatSystemGoesIntoAlarmStateIfCatDetectedAndSystemGoesToArmedHome(){
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, atMostOnce()).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);
    }


    /**
     * Covers the situation where System is Disarmed, the ImagePanel scans an Image of a Cat,
     * and then the system becomes armed.
     * SetAlarmStatus is called twice, from setArmingStatus
     */
    @Test
    void checkThatSystemGoesIntoAlarmStateIfCatDetectedAndSystemGoesToArmedHome2(){
        Sensor d = new Sensor("D",SensorType.DOOR);
        d.setActive(true);
        Sensor e = new Sensor("E",SensorType.MOTION);
        e.setActive(true);
        Sensor f = new Sensor("F", SensorType.DOOR);
        Set<Sensor> sensorSet = new HashSet<>();
        sensorSet.add(d);
        sensorSet.add(e);
        sensorSet.add(f);
        when(securityRepository.getSensors()).thenReturn(sensorSet);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        ArgumentCaptor<AlarmStatus> captor = ArgumentCaptor.forClass(AlarmStatus.class);
        verify(securityRepository, times(2)).setAlarmStatus(captor.capture());
        assertEquals(captor.getValue(), AlarmStatus.ALARM);

    }













}
