package org.example.CameraClasses;

import org.opencv.core.Mat;

public class Container2Mat {
    private final Mat first, second, full;
    private final String name;

    public Container2Mat(Mat first, Mat second, String name){
        this.first = first.clone();
        this.second = second.clone();
        this.full = null;
        this.name = name;
    }

    public Container2Mat(Mat first, Mat second, Mat full, String name){
        this.first = first.clone();
        this.second = second.clone();
        this.full = full.clone();
        this.name = name;
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
    public String getName(){
        return name;
    }
}
