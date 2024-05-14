package org.example.CameraClasses;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CountDownLatch;

public class VideoStreamFirstProcessing implements Runnable {


//    ImageProcessing imageProcessing;
//    Thread threadImage;

    VideoStreamSecondProcessing videoStreamSecondProcessing;
    Thread threadVideo2;
    private FFmpegFrameGrabber camera;
    private OpenCVFrameConverter.ToMat converter;
//    private Mat frame;
//    private Mat rectFrame;
//    private Mat prevFrame;
    private Mat diffFrame;
    private Mat grayFrame;
    private Mat grayPrevFrame;
    private final double thresholdFirst = 40.0;
//    private final double thresholdSecond = 120.0;
//    private final long minSquare = 1400 * 700;
    private int frameCount = 0;
    private final int numOfEveryFrame = 5;

    private volatile boolean isActive = true;

    private final File dirImg = new File("ImgProc");

//    private final int sleepCount = 2400;
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    public VideoStreamFirstProcessing(FFmpegFrameGrabber camera){
        this.camera = camera;
//        try {
//            camera.start();
//        } catch (FFmpegFrameGrabber.Exception e) {
//            System.out.println("Camera not found");
//            throw new RuntimeException(e);
//        }
        if(!dirImg.exists()){
            dirImg.mkdir();
        }
        converter = new OpenCVFrameConverter.ToMat();
//        frame = new Mat();
//        rectFrame = new Mat();
//        prevFrame = new Mat();
        diffFrame = new Mat();
        grayFrame = new Mat();
        grayPrevFrame = new Mat();

//        imageProcessing = new ImageProcessing();
//        threadImage = new Thread(imageProcessing);
//        threadImage.start();

        videoStreamSecondProcessing = new VideoStreamSecondProcessing();
        threadVideo2 = new Thread(videoStreamSecondProcessing);
        threadVideo2.start();
    }
    private Mat getOneCol(Mat frame){
        Mat oneCol = new Mat(frame.rows(), 1, CvType.CV_8UC1);
        for(int i = 0; i < frame.rows();i++){
            oneCol.put(i,0, frame.get(i,frame.cols() * 3 / 4 ));
        }
        return oneCol;
    }

//    private Mat getCutedFrame(Mat frame){
//        Mat cuted = new Mat(frame, new Rect(0, 140, frame.width(), frame.height() - 140 - 200));
//        return cuted;
//    }
// 1400 * 700
//    private boolean isTrueSize(Mat frag){
//        if(frag.width() * frag.height() < minSquare){
//            return false;
//        }
//        else return true;
//    }

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
        try {
            System.out.println("Попытка...");
            camera.start();
            System.out.println("запуска...");

        } catch (FFmpegFrameGrabber.Exception e) {
            System.out.println("Ошибка с камерой...");
            throw new RuntimeException(e);
        }

        System.out.println("Начало...");
        ///
        int i = 0;
        //int sleeping = 0;
        while (isActive) {
            Frame grabbedFrame = null;
            try {
                grabbedFrame = camera.grabImage();
            } catch (FFmpegFrameGrabber.Exception e) {
                throw new RuntimeException(e);
            }
            if (grabbedFrame != null) {
//                i++;
//                if((i = i % numOfEveryFrame) != 0){
//                    continue;
//                }


                int height = grabbedFrame.imageHeight;
                int width = grabbedFrame.imageWidth;

                org.bytedeco.opencv.opencv_core.Mat javaCVMat = converter.convert(grabbedFrame);
                BytePointer bytePointer = javaCVMat.data();
                int dataSize = (int) (javaCVMat.total() * javaCVMat.elemSize());
//            ByteBuffer buffer = (ByteBuffer) grabbedFrame.image[0];

                // Преобразуем кадр из формата Frame в формат Mat
//            Java2DFrameConverter converter = new Java2DFrameConverter();
//            BufferedImage bufferedImage = converter.convert(grabbedFrame);
                byte[] buffer = new byte[dataSize];
                bytePointer.get(buffer);

                Mat frame = new Mat(height, width, CvType.CV_8UC3);

                frame.put(0, 0, buffer);



                String nameImg = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd__HH-mm-ss")) +".png";
                Imgproc.cvtColor(frame, grayFrame, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(grayFrame, grayFrame, new Size(9, 9), 0);

                if (grayPrevFrame.empty()) {
                    grayPrevFrame = grayFrame.clone();
                    frameCount++;
                    continue;
                } else{
//                    System.out.println("No sleep");
                    Mat centerCol = getOneCol(grayFrame);
                    Mat centerPrevCol = getOneCol(grayPrevFrame);
                    Core.absdiff(centerCol, centerPrevCol, diffFrame);
//                    Core.absdiff(grayFrame, grayPrevFrame, diffFrame);
                    Imgproc.threshold(diffFrame, diffFrame, thresholdFirst, 255, Imgproc.THRESH_BINARY);
                    Scalar mean = Core.mean(diffFrame);
                    double diff = mean.val[0];
//                    double diff = compFrames(grayFrame, grayPrevFrame);
                    if (diff > thresholdFirst) {
//                           System.out.println("Sleep");

                        ///
                        File dirImgBlur = new File("ImgBlur");
                        if(!dirImgBlur.exists()){
                            dirImgBlur.mkdir();
                        }
                        try {
                            Imgcodecs.imwrite(dirImgBlur.getCanonicalPath() + File.separator + nameImg, grayFrame);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        ///

                        //Новый класс
                        Container2Mat greyAndPrevGrey = new Container2Mat(grayFrame, grayPrevFrame, frame, nameImg);
                        videoStreamSecondProcessing.toAddImages(greyAndPrevGrey);
                        //*
                        System.out.println("Frame " + frameCount + " is significantly different");
                    }
                }
                grayPrevFrame = grayFrame.clone();
                frameCount++;
                synchronized (this) {
                    try {
//                        Thread.sleep(200);
                        wait(200);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {
                System.out.println("Frame not captured");
                try {
                    camera.stop();
                    camera.start();
                } catch (FFmpegFrameGrabber.Exception e) {
                    throw new RuntimeException(e);
                }
            }

        }
        try {
            stop();
        } catch (InterruptedException e) {
            System.out.println("Stop Except");
            throw new RuntimeException(e);
        } catch (FFmpegFrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Video1Thr is finished");
    }

    public void stop() throws InterruptedException, FFmpegFrameGrabber.Exception {
//        HighGui.destroyAllWindows();
        videoStreamSecondProcessing.toDisable();
        threadVideo2.join();
        camera.stop();
    }

    public void toDisable() {
        isActive = false;
    }
}
