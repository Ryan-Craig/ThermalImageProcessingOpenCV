package com.kraydel.opencv;


import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

import org.opencv.android.*;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.Double.parseDouble;
import static org.opencv.core.Core.*;
import static org.opencv.core.CvType.CV_16UC1;
import static org.opencv.core.CvType.CV_64FC1;
import static org.opencv.core.CvType.CV_8UC1;

public class OpenCVActivity extends Activity
        implements CvCameraViewListener {

    private CameraBridgeViewBase openCvCameraView;
    private CascadeClassifier cascadeClassifier;
    private Mat grayscaleImage;
    private int absoluteFaceSize;
    private Boolean firstFrame = true;
    private Mat previousFrame;

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

        try {
            String fileName = "lbpcascade_frontalface.xml";
            // Copy the resource into a temp file so OpenCV can load it
            Log.d("OpenCVActivity", "Initialising dependencies");
            InputStream is = getResources().getAssets().open(fileName);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            Log.d("OpenCVActivity", "cascadeDir " + cascadeDir);
            File mCascadeFile = new File(cascadeDir, fileName);
            FileOutputStream os = new FileOutputStream(mCascadeFile);


            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();


            // Load the cascade classifier
            cascadeClassifier = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            Log.d("OpenCVActivity", "cascadeClassifier " + cascadeClassifier.toString());
        } catch (Exception e) {
            Log.e("OpenCVActivity", "Error loading cascade", e);
        }

        // And we are ready to go
        openCvCameraView.enableView();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        openCvCameraView = new JavaCameraView(this, -1);
        setContentView(openCvCameraView);
        openCvCameraView.setCvCameraViewListener(this);
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        grayscaleImage = new Mat(height, width, CvType.CV_8UC4);
        // The faces will be a 20% of the height of the screen
        absoluteFaceSize = (int) (height * 0.2);
    }

    @Override
    public void onCameraViewStopped() {
    }

    @Override
    public Mat onCameraFrame(Mat aInputFrame) {
        /*
        String csvFile = "C:\\Users\\David\\Documents\\Temperatures2.txt";
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";
        Mat image = new Mat(32, 32, CV_16UC1);
        int j = 0;
        try
        {

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null)
            {
                // use comma as separator
                String[] data = line.split(cvsSplitBy);
                for(int i = 0; i < data.length; i++)
                {
                    image.put(i, j, (int)(10 * Double.parseDouble(data[i])));
                }
                j++;
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        */
        // Create a grayscale image
        Imgproc.cvtColor(aInputFrame, grayscaleImage, Imgproc.COLOR_RGB2GRAY);

        /*
        if(firstFrame)
        {
            firstFrame = false;
        }
        else // We have a previous frame to work with
        {
            Mat differenceFrame = new Mat();
            Mat binarisedFrame = new Mat();
            Mat kernel = new Mat(3, 3, CV_8UC1);
            absdiff(grayscaleImage, previousFrame, differenceFrame);
            Imgproc.threshold(differenceFrame, binarisedFrame, 50, 255, Imgproc.THRESH_BINARY);
            Imgproc.erode(binarisedFrame, binarisedFrame, kernel);
            Imgproc.dilate(binarisedFrame, binarisedFrame, kernel);
            List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
            contours.clear();
            Imgproc.findContours(binarisedFrame, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);
            Imgproc.drawContours(aInputFrame, contours, -1, new Scalar(255, 0, 0), 5);
        }
        previousFrame = grayscaleImage.clone();
        */

        Core.flip(aInputFrame, grayscaleImage.t(), 1);
        
        MatOfRect objects = new MatOfRect();

        // Use the classifier to detect objects
        if (cascadeClassifier != null) {
            cascadeClassifier.detectMultiScale(grayscaleImage, objects, 1.1, 4, 2,
                    new Size(absoluteFaceSize, absoluteFaceSize), new Size());
        }

        // If there are any objects found, draw a rectangle around it
        Rect[] objectArray = objects.toArray();
        for (Rect anObjectArray : objectArray) {
            Imgproc.rectangle(aInputFrame, anObjectArray.tl(), anObjectArray.br(), new Scalar(0, 255, 0, 255), 3);
        }

        /*
        Mat histogram = new Mat();
        MatOfInt histSize = new MatOfInt(401);
        final MatOfFloat histRange = new MatOfFloat(0f, 400f);
        List<Mat> images = new ArrayList<Mat> ();
        images.add(image);
        Imgproc.calcHist(images, 1, new Mat(), histogram, histSize, histRange);
        Imgproc.putText(aInputFrame, "Hello World!", new Point(400, 400), Core.FONT_HERSHEY_SIMPLEX, 1, new Scalar(0, 0, 0), 5);
        */
        return aInputFrame;
    }

    @Override
    public void onResume() {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_6, this, mLoaderCallback);
    }
}