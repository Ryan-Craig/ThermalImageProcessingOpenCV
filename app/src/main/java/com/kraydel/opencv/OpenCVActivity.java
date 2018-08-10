package com.kraydel.opencv;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.WindowManager;

import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static com.kraydel.opencv.ambientTemperature.basicAverage;
import static com.kraydel.opencv.ambientTemperature.standardDeviation;
import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;

public class OpenCVActivity extends Activity
        implements CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;
    private int imageSelect = 0;
    private RemovalState removalState = RemovalState.NONE;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    initializeOpenCVDependencies();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    private void initializeOpenCVDependencies() {
        // And we are ready to go
        openCvCameraView.enableView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        openCvCameraView = new JavaCameraView(this, 98); //98 Front camera, 99 Back camera
        setContentView(openCvCameraView);
        openCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        //Saves the width of the screen as an int
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int width = displayMetrics.widthPixels;

        //Checks if the user has tapped the screen
        if(event.getAction() == MotionEvent.ACTION_DOWN ) {
            //Checks which side of the screen has been tapped
            if (event.getX() < width / 2) {
                //Shifts back one removal state, jumping back to previous image when at the first state
                switch (removalState) {
                    case NONE:
                        removalState = RemovalState.CANNY_REMOVAL;
                        previousImage();
                        break;
                    case SD_REMOVAL:
                        removalState = RemovalState.NONE;
                        break;
                    case CANNY_REMOVAL:
                        removalState = RemovalState.SD_REMOVAL;
                        break;
                    default:
                        removalState = RemovalState.NONE;
                        break;
                }
            } else {
                //Shifts forward one removal state, jumping the the next image when at the last state
                switch (removalState) {
                    case NONE:
                        removalState = RemovalState.SD_REMOVAL;
                        break;
                    case SD_REMOVAL:
                        removalState = RemovalState.CANNY_REMOVAL;
                        break;
                    case CANNY_REMOVAL:
                        removalState = RemovalState.NONE;
                        nextImage();
                        break;
                    default:
                        removalState = RemovalState.NONE;
                        break;
                }
            }
        }
        return super.onTouchEvent(event);
    }

    private void nextImage(){
        if (imageSelect < 10) {
            imageSelect++;
        } else {
            //once you reach the end of the images, jump to the first one
            imageSelect = 0;
        }
    }

    private void previousImage(){
        if (imageSelect > 0) {
            imageSelect--;
        } else {
            //once you reach the start of the images, jump to the last one
            imageSelect = 10;
        }
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {

        //Switches the image being shown when screen is tapped by incrementing imageSelect in the onTouchEvent method
        String file;
        //TODO Get better low temp files
        switch (imageSelect){
            case 0: file = "EmptyRoom(26).txt"; break;
            case 1: file = "StandingFar(26).txt"; break;
            case 2: file = "StandingFar2(26).txt"; break;
            case 3: file = "StandingClose(26).txt"; break;
            case 4: file = "StandingClose2(26).txt"; break;
            case 5: file = "EmptyRoom(22).txt"; break;
            case 6: file = "RoomWithPeople(22).txt"; break;
            case 7: file = "StandingFar(22).txt"; break;
            case 8: file = "StandingFar2(22).txt"; break;
            case 9: file = "StandingClose(22).txt"; break;
            case 10: file = "StandingClose2(22).txt"; break;
            default: file = "thermal.txt"; break;
        }

        //Takes in a txt file of temperatures, converts them to an array and then converts that to
        //a grayscale and RGB Mat
        double[][] tempArray = readThermalImage(file);
        Mat rgbThermalImage = convertToRGBThermalMat(tempArray);
        Mat grayscaleThermalImage = convertToGrayscaleMat(tempArray);

        //based on which removal state the application is in a different removal algorithm is applied to the the temperature array
        switch(removalState){
            case NONE: break;
            case SD_REMOVAL: extremePixelsRemoved(tempArray, rgbThermalImage); break;
            case CANNY_REMOVAL: drawContours(grayscaleThermalImage,rgbThermalImage); break;
        }

        //Scales the thermal Mats up to the full size of the screen
        //hijacks the aInputFrame Mat and replaces it with a stretched version of the thermal Mat
        Size fullScreen = new Size (aInputFrame.size().width,aInputFrame.size().height);
        Imgproc.resize(rgbThermalImage,aInputFrame,fullScreen);
        //calculates the ambient temperature of the current frame and displays it in the top left
        calculateAmbientTemp(aInputFrame,file);

        return aInputFrame;
    }

    //Reads a thermal image from a txt file of temperatures and converts it to a grayscale Mat with exaggerated colours
    //TODO: read files directly from thermal camera?
    private double[][] readThermalImage(String fileName){
        double[][] vals = new double[32][32];

        try {
            InputStream in = getResources().getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            double val;
            //runs through the 32 lines in the txt file, separating each temp value based on the tab breaks
            for(int i = 0; i < 32; i++){
                String[] temps = reader.readLine().split("\\t");
                for(int j = 0; j < temps.length; j++) {
                    //removes additional formatting on the values to allow them to be parsed at doubles
                    val = Double.parseDouble(temps[j] = temps[j].replace("+", ""));
                    //saves each temp value into an array
                    vals[i][j] = val;
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading Temperature File", e);
        }
        return vals;
    }

    private Mat convertToGrayscaleMat(double[][] temps){
        Mat thermalImage = new Mat(32,32,CV_8UC1);

        //adjusts the temps to allow easier conversion to grayscale values
        for(int i =0; i<32; i++){
            for(int j = 0; j < 32; j++){
                double adjustedTemp = temps[i][j]+17;
                //any temps below -17 (before being adjusted) are treated as -17
                if(adjustedTemp<0){
                    temps[i][j] = 0;
                    //any temps above 55(before adjustment) are treated as 55
                }else if(adjustedTemp > 72){
                    temps[i][j] = 255;
                }else {

                    //ratio applied to stretch the (-17)-55 range across 0-255 to allow for conversion to grayscale
                    temps[i][j] = (adjustedTemp) * (255 / 72);
                }
            }
        }

        //adds the adjusted and stretched temps to the mat
        for(int i = 0; i < 32; i++)
            for(int j = 0; j < 32; j++)
                thermalImage.put(i,j,temps[i][j]);

        return thermalImage;
    }

    private Mat convertToRGBThermalMat(double[][] temps){
        double[][][] rgbArray = new double[32][32][3];
        Mat rgbThermalImage = new Mat(32,32,CV_8UC3);

        //Similar process to the grayscale mat but with more complex ratios needed to create rgb values
        for(int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                double temp = temps[i][j];
                double red = 0, green = 0, blue =0;
                //temperatures below 256K (-17*C) are always black
                if ( temp < -17) {
                    //keep all rgb values at 0
                } else if (temp >= 55) {
                    //return white color for values above 328K (55*C)
                    red = 0xFF;
                    green = 0xFF;
                    blue = 0xFF;
                } else {
                    if (temp >= -17 && temp < 15) {
                        blue = ((temp + 17) / 2) * 0x11;
                    } else if (temp >= 15 && temp < 23) {
                        blue = 0xFF;
                        green = (temp - 15) * 0x22;
                    } else if (temp >= 23 && temp < 31) {
                        blue = 0;
                        green = 0xFF;
                        red = (temp - 23) * 0x22;
                    } else if (temp >= 31 && temp < 39) {
                        blue = 0;
                        green =0xFF - ((temp - 31) * 0x22);
                        red = 0xFF;
                    } else if (temp >= 39 && temp < 55) {
                        blue = ((temp - 39) * 0x11);
                        green = ((temp - 39) * 0x11);
                        red = 0xFF;
                    }
                }
                rgbArray[i][j][0] = red;
                rgbArray[i][j][1] = green;
                rgbArray[i][j][2] = blue;
                rgbThermalImage.put(i,j,rgbArray[i][j]);
            }
        }
        return rgbThermalImage;
    }

    public void extremePixelsRemoved(double[][] temps2D, Mat thermal){
        double[] empty = new double[]{0,0,0};

        double avg = basicAverage(temps2D), sd = standardDeviation(temps2D);

        //any values that lie outside the standard deviation are set to 0, essentially removing them from the image
        //Note: In theory this would not work with images containing a lot of extreme cold areas but in practise should be fine
        //for almost all use cases
        for(int i = 0; i < 32; i++) {
            for (int j = 0; j < 32; j++) {
                if ((temps2D[i][j] - avg) > sd) {
                    thermal.put(i,j,empty);
                }
            }
        }
    }

    public ArrayList<MatOfPoint> findContours(Mat grayscaleImage){
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        Mat cannyFrame = new Mat();
        //Canny method used to draw contours to a new frame
        Imgproc.Canny(grayscaleImage, cannyFrame,65,15);

        //Contours are then saved into a ArrayList of MatOfPoints
        Imgproc.findContours(cannyFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
        return contours;
    }

    public void drawContours(Mat grayscaleThermalImage, Mat rgbThermalImage){
        ArrayList<MatOfPoint> contours = new ArrayList<>();
        contours.clear();
        contours = findContours(grayscaleThermalImage);

        //TODO Tighter Bounding Rects?
        for(MatOfPoint c: contours)
        {
            //Draws black boxes binding the contour paths
            c.size();
            Rect boundingRect = Imgproc.boundingRect(c);
            Mat nothing = new Mat(rgbThermalImage, boundingRect);
            nothing.setTo(new Scalar(0, 0, 0));
        }

        //Contours drawn on the image with red lines
        Imgproc.drawContours(rgbThermalImage, contours, -1, new Scalar(255, 0, 0), 1);
    }

    private void calculateAmbientTemp(Mat image, String file){
        //If the ambient temp cannot be calculated them absolute zero is displayed
        double ambientT = -273.15;
        try {
            //Method from ambientTemperature is run to convert the txt file into a 1D array of values
            double[] tempVals = ambientTemperature.txtToArray(getResources().getAssets().open(file),32,32);
            //TODO Calculate avg with canny removal
            //Based on the current removal state of the application a corresponding ambient temperature algorithm is applied
            switch (removalState){
                case NONE: ambientT = basicAverage(tempVals); break;
                case SD_REMOVAL: ambientT = ambientTemperature.extremesRemoved(tempVals); break;
                case CANNY_REMOVAL: ambientT = -273.15; break;
                default: ambientT = -273.15; break;
            }
        }catch (IOException e){
            Log.e("OpenCVActivity", "Error loading Temperature File", e);
        }
        //Ambient temperature value is drawn in the top left of the image in white text
        Imgproc.putText(image, Double.toString(ambientT), new Point(50, 50), Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(255, 255, 255), 5);
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }
}