package com.kraydel.opencv;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ambientTemperature {

    public static double basicAverage(double[] temps){
        double avg,total=0;
        for(double val : temps){
                total+=val;
        }

        avg = total/(temps.length);

        return avg;
    }

    public static double basicAverage(double[][] temps2D){
        double[] temps = array2Dto1D(temps2D);
        return basicAverage(temps);
    }

    public static double standardDeviation(double[] temps){
        double sd,total = 0, avg = basicAverage(temps);
        for(double val : temps){
            total += (val-avg)*(val-avg);
        }

        sd = Math.sqrt(total/temps.length);
        return sd;
    }

    public static double standardDeviation(double[][] temps2D){
        double[] temps = array2Dto1D(temps2D);
        return standardDeviation(temps);
    }

    public static double extremesRemoved(double[] temps){
        double avg, total = 0, bAvg = basicAverage(temps), sd = standardDeviation(temps);
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

    public static double extremesRemoved(double[][] temps2D){
        double[] temps = array2Dto1D(temps2D);
        return extremesRemoved(temps);
    }

    public static double[] txtToArray(InputStream stream, int lines, int length){

        double[] vals = new double[(lines*length)];
        int k =0;

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

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

    private static double[] array2Dto1D(double[][] temps2D){
        ArrayList<Double> temps1D = new ArrayList<>();

        for(int i = 0; i < temps2D.length; i++)
            for (int j = 0; j < temps2D[i].length; j++)
                temps1D.add(temps2D[i][j]);

        double[] temps = new double[temps1D.size()];

        for(int i = 0; i < temps1D.size(); i++)
            temps[i] = temps1D.get(i);

        return temps;
    }
}
