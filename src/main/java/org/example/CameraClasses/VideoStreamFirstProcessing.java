package org.example.CameraClasses;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.example.Main;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

public class VideoStreamFirstProcessing implements Runnable {


//    ImageProcessing imageProcessing;
//    Thread threadImage;
    private final String url_params_prop = "params_system.properties";
    private static Properties params_properties;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static VideoStreamSecondProcessing videoStreamSecondProcessing;
    Thread threadVideo2;
    private FFmpegFrameGrabber camera;
    private OpenCVFrameConverter.ToMat converter;
    private org.bytedeco.opencv.opencv_core.Mat javaCVMat;
    private Frame grabbedFrame;
    private Mat centerCol;
    private Mat centerPrevCol;
    private Mat frame;
//    private Mat rectFrame;
//    private Mat prevFrame;
    private Mat diffFrame;
    private Mat grayFrame;
    private Mat grayPrevFrame;
    private final double constThresholdFirst = 35.0;
    private final int constTimeCheck = 200;
    private final double constFirstCheckPosition = 0.70;
    private double thresholdFirst;
    private int timeCheck;
    private double firstCheckPosition;
//    private final double thresholdSecond = 120.0;
//    private final long minSquare = 1400 * 700;

    //private int frameCount = 0;
    //private final int numOfEveryFrame = 5;

    private volatile boolean isActive = true;

    //private boolean imgFlagBlur = false;


//    private final int sleepCount = 2400;
    private CountDownLatch countDownLatch = new CountDownLatch(1);


    public VideoStreamFirstProcessing(FFmpegFrameGrabber camera){
        this.camera = camera;

        //URL url_params = Main.class.getClassLoader().getResource(url_params_prop);
        params_properties = new Properties();
        try(InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(url_params_prop)) {
            params_properties.load(inputStream);
        } catch (IOException e) {
            logger.error("Reading error params_system.properties for videoFirst stream", e);
            throw new RuntimeException(e);
        }
//        try{
//            fis_prm = new FileInputStream(url_params.getFile());
//            params_properties.load(fis_prm);
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        try{
            timeCheck = Integer.parseInt(params_properties.getProperty("video_first.time_check"));
            thresholdFirst = Double.parseDouble(params_properties.getProperty("video_first.threshold"));
            firstCheckPosition = Double.parseDouble(params_properties.getProperty("video_first.first_check_position"));
            if(timeCheck <= 0 || Double.compare(thresholdFirst, 0) <= 0  || Double.compare(firstCheckPosition, 0) <= 0 || Double.compare(firstCheckPosition, 1) > 1){
                timeCheck = constTimeCheck;
                thresholdFirst = constThresholdFirst;
                firstCheckPosition = constFirstCheckPosition;
                logger.info("Invalid params params_system.properties for videoFirst stream, the default values are set");
            }
        }catch (Exception e){
            logger.error("Reading error params_system.properties for videoFirst stream, the default values are set", e);
            timeCheck = constTimeCheck;
            thresholdFirst = constThresholdFirst;
            firstCheckPosition = constFirstCheckPosition;
        }

//        try {
//            camera.start();
//        } catch (FFmpegFrameGrabber.Exception e) {
//            System.out.println("Camera not found");
//            throw new RuntimeException(e);
//        }
//        File dirImg = new File("ImgProc");
//        if(!dirImg.exists()){
//            dirImg.mkdir();
//            logger.info("Создание папки " + dirImg.getName());
//        }
        converter = new OpenCVFrameConverter.ToMat();

        centerCol = new Mat();
        centerPrevCol = new Mat();
        grabbedFrame = null;
        frame = new Mat();
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
        logger.info("Params of videoFirst: time_check = " + timeCheck +", threshold = " + thresholdFirst + ", first_check_position = " + firstCheckPosition);
    }
    private Mat getOneCol(Mat frame){
        Mat oneCol = new Mat(frame.rows(), 1, CvType.CV_8UC1);
        for(int i = 0; i < frame.rows();i++){
            oneCol.put(i,0, frame.get(i, (int) (frame.cols() * firstCheckPosition)));
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
        int timestamp = 0;
        logger.info("Launch attempt threadFirst stream");
        try {
            camera.start();
            logger.debug("Launch attempt VideoStream");

        } catch (FFmpegFrameGrabber.Exception e) {
            logger.error("Reading error VideoStream", e);
            throw new RuntimeException(e);
        }

        logger.info("Reading VideoStream");
        ///
//        int i = 0;
        //int sleeping = 0;
//        grayPrevFrame = new Mat();
        while (isActive) {
            grabbedFrame = null;
            try {
                camera.setFrameNumber(timestamp);
                grabbedFrame = camera.grabImage();
            } catch (FFmpegFrameGrabber.Exception e) {
                logger.error("Error receiving a frame from the VideoStream", e);
                throw new RuntimeException(e);
            }
            if (grabbedFrame != null) {
//                i++;
//                if((i = i % numOfEveryFrame) != 0){
//                    continue;
//                }

                Date dateImg = new Date();
                int height = grabbedFrame.imageHeight;
                int width = grabbedFrame.imageWidth;

                javaCVMat = converter.convert(grabbedFrame);
                BytePointer bytePointer = javaCVMat.data();
                int dataSize = (int) (javaCVMat.total() * javaCVMat.elemSize());
//                grayFrame.release();
                javaCVMat.release();
//            ByteBuffer buffer = (ByteBuffer) grabbedFrame.image[0];

                // Преобразуем кадр из формата Frame в формат Mat
//            Java2DFrameConverter converter = new Java2DFrameConverter();
//            BufferedImage bufferedImage = converter.convert(grabbedFrame);
                byte[] buffer = new byte[dataSize];
                bytePointer.get(buffer);

                frame = new Mat(height, width, CvType.CV_8UC3);
                frame.put(0, 0, buffer);
                //SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd__HH-mm-ss");
                //String nameImg = formater.format(dateImg) +".png";
//                Mat grayFrame = new Mat();
                Mat fGrayFrame = new Mat();
                Imgproc.cvtColor(frame, fGrayFrame, Imgproc.COLOR_BGR2GRAY);
                Imgproc.GaussianBlur(fGrayFrame, grayFrame, new Size(9, 9), 0);
                fGrayFrame.release();
                if (grayPrevFrame.empty()) {
                    grayPrevFrame.release();
                    frame.release();
                    grayPrevFrame = grayFrame.clone();
                    grayFrame.release();
//                    grayPrevFrame = new Mat();

                    //frameCount++;
                    timestamp += timeCheck;
                    continue;
                } else{
//                    System.out.println("No sleep");
//                    diffFrame = new Mat();
                    centerCol = getOneCol(grayFrame);
                    centerPrevCol = getOneCol(grayPrevFrame);
                    Mat fDiff = new Mat();
                    Core.absdiff(centerCol, centerPrevCol, fDiff);
                    centerCol.release();
                    centerPrevCol.release();
//                    Core.absdiff(grayFrame, grayPrevFrame, diffFrame);
                    Imgproc.threshold(fDiff, diffFrame, thresholdFirst, 255, Imgproc.THRESH_BINARY);
                    Scalar mean = Core.mean(diffFrame);
                    diffFrame.release();
                    fDiff.release();
                    double diff = mean.val[0];
//                    double diff = compFrames(grayFrame, grayPrevFrame);
                    if (diff > thresholdFirst) {
//                           System.out.println("Sleep");

//                        if(imgFlagBlur){
//                            ///
//                            File dirImgBlur = new File("ImgBlur");
//                            if(!dirImgBlur.exists()){
//                                dirImgBlur.mkdir();
//                            }
//                            try {
//                                Imgcodecs.imwrite(dirImgBlur.getCanonicalPath() + File.separator + nameImg, grayFrame);
//                            } catch (IOException e) {
//                                throw new RuntimeException(e);
//                            }
//                            ///
//                        }
                        //Новый класс
                        Container2Mat greyAndPrevGrey = new Container2Mat(grayFrame, grayPrevFrame, frame, dateImg);
                        videoStreamSecondProcessing.toAddImages(greyAndPrevGrey);

                        //*
                        logger.info("Movement detected");
                        //System.out.println("Frame " + frameCount + " is significantly different");
                    }
                }
                frame.release();
                grayPrevFrame.release();
                grayPrevFrame = grayFrame.clone();
                grayFrame.release();
                //frameCount++;
//                synchronized (this) {
//                    try {
////                        Thread.sleep(200);
//                        wait(timeCheck);
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
//                }
                timestamp += timeCheck;
            } else {
                //System.out.println("Frame not captured");
//                logger.info("VideoStream is lost");
                try {
                    camera.stop();
                    camera.start();
//                    logger.info("Restoring the VideoStream");
                } catch (FFmpegFrameGrabber.Exception e) {
                    logger.error("Restoring error the VideoStream", e);
                    throw new RuntimeException(e);
                }
            }

        }
        try {
            stop();
        } catch (InterruptedException e) {
            //System.out.println("Stop Except");
            logger.error("Completion error videoFirst stream", e);
            throw new RuntimeException(e);
        } catch (FFmpegFrameGrabber.Exception e) {
            logger.error("Completion error videoFirst stream", e);
            throw new RuntimeException(e);
        }
        logger.info("The videoFirst stream is closed");
        //System.out.println("Video1Thr is finished");
    }

    public void stop() throws InterruptedException, FFmpegFrameGrabber.Exception {
//        HighGui.destroyAllWindows();
        videoStreamSecondProcessing.toDisable();
        threadVideo2.join();
        camera.stop();
        logger.info("Camera is closed");
    }

    public void toDisable() {
        isActive = false;
    }
}
