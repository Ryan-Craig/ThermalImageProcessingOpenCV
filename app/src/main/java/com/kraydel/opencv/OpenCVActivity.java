package com.kraydel.opencv;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
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

import static org.opencv.core.CvType.CV_8UC1;
import static org.opencv.core.CvType.CV_8UC3;

public class OpenCVActivity extends Activity
        implements CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;
    private int imageSelect = 0;

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
        if(event.getAction() == MotionEvent.ACTION_DOWN ){
            if(imageSelect < 5) {
                imageSelect++;
            }else {
                imageSelect = 0;
            }
        }
        return super.onTouchEvent(event);
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {

        //Switches the image being shown when screen is tapped by incrementing imageSelect in the onTouchEvent method
        String file;
        switch (imageSelect){
            case 0: file = "FaceOn(23p5).txt"; break;
            case 1: file = "Sitting(23p5).txt"; break;
            case 2: file = "Room(23p5).txt"; break;
            case 3: file = "Window(23p5).txt"; break;
            case 4: file = "Celling(23p5).txt"; break;
            case 5: file = "PC(23p5).txt"; break;
            default: file = "thermal.txt"; break;
        }

        //hijacks the aInputFrame Mat and replaces it with a stretched version of the thermal Mat
        Size fullScreen = new Size (aInputFrame.size().width,aInputFrame.size().height);
        Imgproc.resize(readThermalImage(file),aInputFrame,fullScreen);

        calculateAmbientTemp(aInputFrame,file);
        return aInputFrame;
    }

    //Reads a thermal image from a txt file of temperatures and converts it to a grayscale Mat with exaggerated colours
    private Mat readThermalImage(String fileName){
        double[][] vals = new double[32][32];

        try {
            InputStream in = getResources().getAssets().open(fileName);
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            double val;
            for(int i = 0; i < 32; i++){
                String[] temps = reader.readLine().split("\\t");
                for(int j = 0; j < temps.length; j++) {
                    val = Double.parseDouble(temps[j] = temps[j].replace("+", ""));
                    vals[i][j] = val;
                }
            }
            reader.close();
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading Temperature File", e);
        }

        //Mat thermalImage = convertToGrayscaleMat(vals);
        Mat thermalImage = convertToRGBThermalMat(vals);

        return thermalImage;
    }

    private Mat convertToGrayscaleMat(double[][] temps){
        Mat thermalImage = new Mat(32,32,CV_8UC1);


        for(int i =0; i<32; i++){
            for(int j = 0; j < 32; j++){
                double adjustedTemp = temps[i][j]+17;
                if(adjustedTemp<0){
                    temps[i][j] = 0;
                }else if(adjustedTemp > 72){
                    temps[i][j] = 255;
                }else {
                    temps[i][j] = (adjustedTemp) * (255 / 72);
                }
            }
        }

        //adds the values from the int array of doubled temps to the mat
        for(int i = 0; i < 32; i++)
            for(int j = 0; j < 32; j++)
                thermalImage.put(i,j,temps[i][j]);

        return thermalImage;
    }

    private Mat convertToRGBThermalMat(double[][] temps){
        double[][][] rgbArray = new double[32][32][3];
        Mat rgbThermalImage = new Mat(32,32,CV_8UC3);

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

    private void calculateAmbientTemp(Mat image, String file){
        double ambientT = -273.15, correctedAmbientT = -273.15;
        try {
            double[] tempVals = ambientTemperature.txtToArray(getResources().getAssets().open(file),32,32);
            ambientT = ambientTemperature.basicAverage(tempVals);
            correctedAmbientT = ambientTemperature.extremesRemoved(tempVals);
        }catch (IOException e){
            Log.e("OpenCVActivity", "Error loading Temperature File", e);
        }

        Imgproc.putText(image, Double.toString(ambientT), new Point(50, 50), Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(0, 0, 0), 5);
        Imgproc.putText(image, Double.toString(correctedAmbientT), new Point(50, 100), Core.FONT_HERSHEY_SIMPLEX, 2, new Scalar(0, 0, 0), 5);

    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }
}