package org.example.CameraClasses;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

public class VideoStreamSecondProcessing implements Runnable {

    ImageProcessing imageProcessing;
    Thread threadImage;
    private Mat frame;
    private Mat rectFrame;
    private Mat prevFrame;
    private Mat diffFrame;
    private Mat grayFrame;
    private Mat grayPrevFrame;
    private final double thresholdFirst = 35.0;
    private final double thresholdSecond = 125.0;
    private final long minSquare = 1300 * 600;
    private int frameCount = 0;
    private final int numOfEveryFrame = 5;

    private volatile boolean isActive = true;

    private final File dirImg = new File("ImgProc");

    private volatile Queue<Container2Mat> images = new LinkedList<>();

    private final int sleepCount = 2400;
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public VideoStreamSecondProcessing(){

        if(!dirImg.exists()){
            dirImg.mkdir();
        }
        frame = new Mat();
        rectFrame = new Mat();
        prevFrame = new Mat();
        diffFrame = new Mat();
        grayFrame = new Mat();
        grayPrevFrame = new Mat();

        imageProcessing = new ImageProcessing();
        threadImage = new Thread(imageProcessing);
        threadImage.start();
    }
    private Mat getOneCol(Mat frame){
        Mat oneCol = new Mat(frame.rows(), 1, CvType.CV_8UC1);
        for(int i = 0; i < frame.rows();i++){
            oneCol.put(i,0, frame.get(i,frame.cols() * 3 / 4 ));
        }
        return oneCol;
    }

    private Mat getCutedFrame(Mat frame){
        Mat cuted = new Mat(frame, new Rect(0, 140, frame.width(), frame.height() - 140 - 200));
        return cuted;
    }
// 1400 * 700
    private boolean isTrueSize(Mat frag){
        if(frag.width() * frag.height() < minSquare){
            return false;
        }
        else return true;
    }

//    private Mat getVerticalProjection(Mat frame){
//        Mat proj = new Mat(frame.rows(), 1, CvType.CV_64F);
//        Core.reduce(frame, proj, 1, Core.REDUCE_SUM, CvType.CV_64F);
//        return proj;
//    }

//    private double compFrames(Mat fr1, Mat fr2){
//        Mat proj1 = getVerticalProjection(fr1);
//        Mat proj2 = getVerticalProjection(fr2);
//        double diff = Core.norm(proj1, proj2, Core.NORM_L1);
//        return diff;
//    }


    @Override
    public void run(){
        int i = 0;
        //int sleeping = 0;
        while (isActive) {
            Container2Mat grayAndPrevGrayImages;
            while ((grayAndPrevGrayImages = images.poll()) != null) {
                String nameImg = grayAndPrevGrayImages.getName();
                grayFrame = grayAndPrevGrayImages.getFirst();
                grayPrevFrame = grayAndPrevGrayImages.getSecond();
                frame = grayAndPrevGrayImages.getFull();
//                    double diff = compFrames(grayFrame, grayPrevFrame);

//                           System.out.println("Sleep");

                ///
                File dirImgBlur = new File("ImgBlur");
                if (!dirImgBlur.exists()) {
                    dirImgBlur.mkdir();
                }
                try {
                    Imgcodecs.imwrite(dirImgBlur.getCanonicalPath() + File.separator + nameImg, grayFrame);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                ///

                //sleeping = sleepCount;
                Core.absdiff(getCutedFrame(grayFrame), getCutedFrame(grayPrevFrame), diffFrame);
                Imgproc.threshold(diffFrame, diffFrame, thresholdSecond, 255, Imgproc.THRESH_BINARY);
                ///
                File dirDiff = new File("ImgDiff");
                if (!dirDiff.exists()) {
                    dirDiff.mkdir();
                }
                try {
                    Imgcodecs.imwrite(dirDiff.getCanonicalPath() + File.separator + nameImg, diffFrame);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                ///

//                System.out.println("Frame " + frameCount + " is significantly different");
                Rect boundingRect = Imgproc.boundingRect(diffFrame);
                Mat frag = new Mat(getCutedFrame(frame), boundingRect);
                Imgproc.rectangle(getCutedFrame(frame), new Point(boundingRect.x, boundingRect.y),
                        new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                        new Scalar(0, 0, 255), 2);
                //Imgproc.resize(frame, frame, new Size(640, 480));
                if (isTrueSize(frag)) {
                    String path = "";
                    try {
                        path = dirImg + File.separator + "1-" + nameImg;
                        File file1 = new File(path);
                        path = dirImg + File.separator + "2-" + nameImg;
                        File file2 = new File(path);
                        //Imgcodecs.imwrite(file1.getCanonicalPath(), frame);
                        Container2Mat fullAndFragImages = new Container2Mat(frame, frag, nameImg);
                        imageProcessing.toAddImg(fullAndFragImages);
                        Imgcodecs.imwrite(file2.getCanonicalPath(), frag);
                    } catch (Exception ex) {
                        System.out.println(path + "\n" + ex.getMessage());
                    }
                }
            }
        }

        try {
            stop();
        } catch (InterruptedException e) {
            System.out.println("Stop Except");
            throw new RuntimeException(e);
        }
        System.out.println("Video2Thr is finished");

    }

    public void stop() throws InterruptedException {
//        HighGui.destroyAllWindows();
        imageProcessing.toDisable();
        threadImage.join();
//        camera.release();
    }

    public void toDisable() {
        isActive = false;
    }

    public void toAddImages(Container2Mat grayAndPrevGray) {
        images.add(grayAndPrevGray);
    }
}
