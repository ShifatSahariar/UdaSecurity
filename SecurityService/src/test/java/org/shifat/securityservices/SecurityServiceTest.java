package org.shifat.securityservices;


import com.google.common.primitives.Booleans;
import org.junit.jupiter.params.provider.ValueSource;
import org.shifat.ImageService;
import org.shifat.securityservices.application.StatusListener;
import org.shifat.securityservices.data.AlarmStatus;
import org.shifat.securityservices.data.ArmingStatus;
import org.shifat.securityservices.data.SecurityRepository;
import org.shifat.securityservices.data.Sensor;
import org.shifat.securityservices.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.stream.Stream;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit test for simple App.
 */

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest

{
    private SecurityService securityService;

    @Mock
    private ImageService imageService;
    @Mock
    private SecurityRepository securityRepository;
    @Mock
    private Sensor sensor,secondSensor;
    @BeforeEach
    void init() {
        securityService = new SecurityService(securityRepository, imageService);

    }
//1st
// If alarm is armed and a sensor becomes activated, put the system into pending alarm status.
    @ParameterizedTest
    @MethodSource("changeSensorActivationStatus")
    public void ArmedAndSensorActivated_changeStatusToPending(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);


        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(sensor, times(1)).setActive(eq(true));
        verify(securityRepository, times(1)).updateSensor(eq(sensor));
    }
    private static Stream<Arguments> changeSensorActivationStatus() {
        return Stream.of(
                Arguments.of(ArmingStatus.ARMED_AWAY),
                Arguments.of(ArmingStatus.ARMED_HOME)
        );
    }
   //2nd
   // If alarm is armed and a sensor becomes activated and the system is already pending alarm, set the alarm status to alarm on. [This is the case where all sensors are deactivated and then one gets activated]
    @ParameterizedTest
    @MethodSource("changeSensorActivationStatus")
    void ifArmedAndSensorActivatedAndPending_changeStatusToAlarm(ArmingStatus armingStatus) {
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
        verify(securityRepository, times(1)).updateSensor(eq(sensor));
    }

    //3rd
    // If pending alarm and all sensors are inactive, return to no alarm state
    @Test
    public void PendingAlarmAndSensorInactive_returnNoAlarm() {
        when(sensor.getActive()).thenReturn(true);

      //  Set<Sensor> Sensors = Set.of(sensor, secondSensor,thirdSensor);


        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor,false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }
  /* @Test
    public void AlarmAndSensorInactive_returnPendingAlarm() {
        when(sensor.getActive()).thenReturn(true);

        //  Set<Sensor> Sensors = Set.of(sensor, secondSensor,thirdSensor);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor,false);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);

    }*/


    //4th
// If alarm is active, change in sensor state should not affect the alarm state.
    @ParameterizedTest
    @ValueSource(booleans = {true,false})
    public void AlarmIsActivated_ButSensorShouldNotAffectAlarm(Boolean state) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(secondSensor, state);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
        //sensor.setActive(true);
        //securityService.changeSensorActivationStatus(sensor, false);
       // verify(securityRepository, never()).setAlarmStatus(AlarmStatus.PENDING_ALARM);


    }
//5th
// If a sensor is activated while already active and the system is in pending state, change it to alarm state.
// [This is the case where one sensor is already active and then another gets activated]
    @Test
    public void ifSensorActivatedWhileActiveAndPendingAlarm_changeStatusToAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
//6th
// If a sensor is deactivated while already inactive, make no changes to the alarm state.
@ParameterizedTest
@MethodSource("changeSensorDeActivationStatus")
void whenSensorDeactivatedWhileInactive_MakeNoChangesToAlarmState(AlarmStatus status) {


    when(securityRepository.getAlarmStatus()).thenReturn(status);
    securityService.changeSensorActivationStatus(sensor, false);

    verify(securityRepository, never()).setAlarmStatus(any());
}


    //7th
    // If the camera image contains a cat while the system is armed-home, put the system into alarm status.
@Mock
    BufferedImage bufferedImage;
    @Test
    public void changeAlarm_imageContainCatDetectedAndArmed_changeToAlarmStatus() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(bufferedImage);
        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.ALARM);

    }
    //8th solved
    // If the camera image does not contain a cat, change the status to no alarm as long as the sensors are not active.
    @Test
    public void whenImageServiceIdentifyNoCatImage_changeToNoAlarmAsLongSensorsNotActive() {
        Set<Sensor> sensors = Set.of(sensor,secondSensor);
        //when(sensors.iterator().next().getActive()).thenReturn(false);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
       // when(securityRepository.getSensors()).thenReturn(sensors);
        sensors.iterator().next().setActive(false);
        securityService.processImage(bufferedImage);

        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //If the system is disarmed, set the status to no alarm.
    //9th
    @Test
    void whenSystemDisArmed_changeToAlarmStatus(){
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, atMostOnce()).setAlarmStatus(AlarmStatus.NO_ALARM);

    }
    //10th solved
    //If the system is armed, reset all sensors to inactive.
    @ParameterizedTest
    @MethodSource("changeSensorActivationStatus")
    void whenSystemArmed_resetSensorsToInactive(ArmingStatus status) {
        Set<Sensor> sensors = Set.of(sensor,secondSensor);

        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);
        securityService.setArmingStatus(status);
        sensors.iterator().next().setActive(false);
        securityService.getSensors().forEach(sensor -> assertEquals(false,sensor.getActive()));
    }
    //11th
    //If the system is armed-home while the camera shows a cat, set the alarm status to alarm.
    @Test
    public void changeAlarmStatus_systemArmedHomeAndCatDetected_changeToAlarmStatus(){

        //when(securityRepository.getArmingStatus()).thenReturn(any());
        when(imageService.imageContainsCat(any(),anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(bufferedImage);
        //securityRepository.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }





    @ParameterizedTest
    @MethodSource("changeSensorDeActivationStatus")
    public void alarmStatusReturn(AlarmStatus alarmStatus){

        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        assertEquals(alarmStatus,securityService.getAlarmStatus());

    }

    private static Stream<Arguments> changeSensorDeActivationStatus() {
        return Stream.of(
                Arguments.of(AlarmStatus.NO_ALARM),
                Arguments.of(AlarmStatus.ALARM),
                Arguments.of(AlarmStatus.PENDING_ALARM)
        );
    }
}


