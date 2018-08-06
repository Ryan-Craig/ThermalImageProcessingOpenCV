package com.kraydel.opencv;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class ambientTemperatureTest {

    double[] testTemps;

    @Before
    public void setup(){
         testTemps = new double[]{24,24,24,23,25,30};
    }

    @Test
    public void basicAverage() {
        assertEquals(25.0,ambientTemperature.basicAverage(testTemps),0.1);
    }

    @Test
    public void standardDeviation() {
        assertEquals(2.3,ambientTemperature.standardDeviation(testTemps),0.1);
    }

    @Test
    public void extremesRemoved() {
        assertEquals(24,ambientTemperature.extremesRemoved(testTemps),0.1);
    }

    @Test
    public void txtToArray() {
        //Setup
        double[] expectedArray = new double[]{12.5,12.5,10.0,11.3,13.7,9.8};
        String initialString = "+12.5\t+12.5\t+10.0\t+11.3\t+13.7\t+9.8";
        InputStream testStream = new ByteArrayInputStream(initialString.getBytes());

        //Execute
        double[] actualArray = ambientTemperature.txtToArray(testStream,1,6);

        //Assert
        assertArrayEquals(expectedArray,actualArray,0.1);
    }

    @Test
    public void incorrectInput(){
        InputStream nullStream = null;

        double[] actualArray = ambientTemperature.txtToArray(nullStream,0,0);

        double[] nullArray = null;
        assertArrayEquals(nullArray,nullArray,0.1);
    }
}