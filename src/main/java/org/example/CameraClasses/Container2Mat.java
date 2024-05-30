package org.example.CameraClasses;

import org.opencv.core.Mat;

import java.util.Date;

public class Container2Mat {
    private final Mat first, second, full;
    private final Date date;

    public Container2Mat(Mat first, Mat second, Date date){
        this.first = first.clone();
        this.second = second.clone();
        this.full = null;
        this.date = date;
    }

    public Container2Mat(Mat first, Mat second, Mat full, Date date){
        this.first = first.clone();
        this.second = second.clone();
        this.full = full.clone();
        this.date = date;
    }

    public Mat getFirst(){
        return first;
    }

    public Mat getSecond(){
        return second;
    }
    public Mat getFull(){
        return full;
    }
    public Date getDate(){
        return date;
    }
}
