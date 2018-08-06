package com.kraydel.opencv;

import android.util.Log;


import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ambientTemperature {

    public static double basicAverage(double[] temps){
        double avg,total=0;
        for(int i = 0; i < temps.length; i++){
                total+=temps[i];
        }

        avg = total/(temps.length);

        return avg;
    }

    public static double standardDeviation(double[] temps){
        double sd = 0,total = 0, avg = basicAverage(temps);
        for(int i = 0; i<temps.length; i++){
            total += (temps[i]-avg)*(temps[i]-avg);
        }

        sd = Math.sqrt(total/temps.length);
        return sd;
    }

    public static double extremesRemoved(double[] temps){
        double avg = 0, total = 0, bAvg = basicAverage(temps), sd = standardDeviation(temps);
        int count = 0;
        for(int i = 0; i < temps.length; i++) {
            if((temps[i]-bAvg) < sd){
            total+=temps[i];
            count++;
            }
        }

        avg = total/count;
        return avg;
    }

    public static double[] txtToArray(InputStream stream, int lines, int length){

        double[] vals = new double[(lines*length)];
        int k =0;

        try {
            InputStream in = stream;
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));

            double val,max=0,min=0,range;
            for(int i = 0; i < lines; i++){
                String[] temps = reader.readLine().split("\\t");
                for(int j = 0; j < length; j++) {
                    val = Double.parseDouble(temps[j] = temps[j].replace("+", ""));
                    vals[k] = val;
                    k++;
                }
            }
            reader.close();
        } catch (Exception e) {
        }
        return vals;
    }
}
