package org.example.CameraClasses;

import org.example.Main;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.CountDownLatch;

public class VideoStreamSecondProcessing implements Runnable {

    private final String url_params_prop = "params_system.properties";
    private final String url_img_prop = "images.properties";
    private static Properties params_properties;
    private static Properties img_properties;
//    private final String url_params_prop = "images.properties";
//    private static Properties params_properties;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private ImageProcessing imageProcessing;
    Thread threadImage;
    private Mat frame;
//    private Mat rectFrame;
//    private Mat prevFrame;
    private Mat diffFrame;
    private Mat grayFrame;
    private Mat grayPrevFrame;
    private Mat frag;
    private Mat frameCut;
    private Mat grayFrameCut;
    private Mat grayPrevFrameCut;
//    private final double thresholdFirst = 35.0;
    private final double constThresholdSecond = 120.0;
    private final int constMinHeight = 500, constMinWight = 1100;
    private final int constYCutTop = 140, constYCutDown = 200;
    private double thresholdSecond;
    //private final long minSquare = 1300 * 600;

    private int minHeight, minWight;
    private int yCutTop, yCutDown;
    //private int frameCount = 0;
    //private final int numOfEveryFrame = 5;

    private volatile boolean isActive = true;

//    private File dirImg;

    private volatile Queue<Container2Mat> images = new LinkedList<>();

    //private final int sleepCount = 2400;
    private CountDownLatch countDownLatch = new CountDownLatch(1);

    private final String constDirBlurName = "ImgBlur";
    private final String constDirDiffName = "ImgDiff";
    private String dirBlurName;
    private String dirDiffName;
//    private String pathBlur;
//    private String pathDiff;
//    private String dirProgName = "ImgProc";
    private boolean imgFlagBlur = false;

    private boolean imgFlagDiff = false;

//    private boolean imgFlagProg = false;

    public VideoStreamSecondProcessing(){
        //URL url_params = Main.class.getClassLoader().getResource(url_params_prop);
        params_properties = new Properties();
        try(InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(url_params_prop)) {
            params_properties.load(inputStream);
        } catch (IOException e) {
            logger.error("Reading error params_system.properties for videoSecond", e);
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
            minHeight = Integer.parseInt(params_properties.getProperty("video_second.min_height"));
            minWight = Integer.parseInt(params_properties.getProperty("video_second.min_wight"));
            thresholdSecond = Double.parseDouble(params_properties.getProperty("video_second.threshold"));
            yCutDown = Integer.parseInt(params_properties.getProperty("video_second.y_cut_down"));
            yCutTop = Integer.parseInt(params_properties.getProperty("video_second.y_cut_top"));
            if(minHeight <= 0 || minWight <= 0 || Double.compare(thresholdSecond, 0) <= 0 || yCutTop < 0 || yCutDown < 0){
                minHeight = constMinHeight;
                minWight = constMinWight;
                thresholdSecond = constThresholdSecond;
                yCutDown = constYCutDown;
                yCutTop = constYCutTop;
                logger.info("Invalid params params_system.properties for videoSecond, the default values are set");
            }
        }catch (Exception e){
            logger.error("Reading error params_system.properties for videoSecond, the default values are set", e);
            minHeight = constMinHeight;
            minWight = constMinWight;
            thresholdSecond = constThresholdSecond;
            yCutDown = constYCutDown;
            yCutTop = constYCutTop;
        }

        //URL url_img = Main.class.getClassLoader().getResource(url_img_prop);
        img_properties = new Properties();
        try(InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(url_img_prop)) {
            img_properties.load(inputStream);
        } catch (IOException e) {
            logger.error("Reading error images.properties for videoSecond", e);
            throw new RuntimeException(e);
        }
//        try{
//            fis_img = new FileInputStream(url_img.getFile());
//            img_properties.load(fis_img);
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        try{
            imgFlagBlur = Boolean.parseBoolean(img_properties.getProperty("flag.blur"));
            imgFlagDiff = Boolean.parseBoolean(img_properties.getProperty("flag.diff"));
        }
        catch (Exception e){
            logger.error("Reading flags error images.properties for videoSecond, the default values are set", e);
        }
        toInitPathsAndDirs();
//        File dirImg = new File("ImgProg");
//        if(!dirImg.exists()){
//            dirImg.mkdir();
//            logger.info("Создание папки " + dirImg.getName());
//        }
        frame = new Mat();
//        rectFrame = new Mat();
//        prevFrame = new Mat();
        diffFrame = new Mat();
        grayFrame = new Mat();
        grayPrevFrame = new Mat();
        frag = new Mat();
        grayFrameCut = new Mat();
        grayPrevFrameCut = new Mat();
        frameCut = new Mat();

        imageProcessing = new ImageProcessing();
        threadImage = new Thread(imageProcessing);

        threadImage.start();

        logger.info("Params of videoSecond: min_height = " + minHeight +", min_wight = " + minWight + ", threshold_second = " + thresholdSecond + ", y_cut_top = " + yCutTop + ", y_cut_down = " + yCutDown);
    }

    private void toInitPathsAndDirs() {
        String pathDiff;
        String pathBlur;
        if(imgFlagDiff){
            try{
                dirDiffName = img_properties.getProperty("dir_img.diff");
                pathDiff = img_properties.getProperty("path_img.diff");
                Path pDiff = Paths.get(pathDiff);
                Path fullPathDiff = pDiff.resolve(dirDiffName);
                File fileDiff = new File(pathDiff);
                if(fileDiff.exists()){
                    File fullFileDiff = new File(fullPathDiff.toString());
                    if(!fullFileDiff.exists()){
                        fullFileDiff.mkdir();
                    }
                    dirDiffName = fullFileDiff.getCanonicalPath();
                }else {
                    logger.info("Invalid path for Diff images.properties for videoSecond, values are set '' ");
                }
            }
            catch (Exception e){
                logger.error("Reading dir Diff error images.properties for videoSecond, the default values are set", e);
                dirDiffName = constDirDiffName;
            }
        }
        if(imgFlagBlur){
            try{
                dirBlurName = img_properties.getProperty("dir_img.blur");
                pathBlur = img_properties.getProperty("path_img.blur");
                Path pBlur = Paths.get(pathBlur);
                Path fullPathBlur = pBlur.resolve(dirBlurName);

                File fileBlur = new File(pathBlur);
                if(fileBlur.exists()){
                    File fullFileBlur = new File(fullPathBlur.toString());
                    if(!fullFileBlur.exists()){
                        fullFileBlur.mkdir();
                    }
                    dirBlurName = fullFileBlur.getCanonicalPath();
                }else {
                    logger.info("Invalid path Blur images.properties for videoSecond, values are set '' ");
                }
            }
            catch (Exception e){
                logger.error("Reading dir Blur error images.properties for videoSecond, the default values are set", e);
                dirBlurName = constDirBlurName;
            }
        }
    }
//    private Mat getOneCol(Mat frame){
//        Mat oneCol = new Mat(frame.rows(), 1, CvType.CV_8UC1);
//        for(int i = 0; i < frame.rows();i++){
//            oneCol.put(i,0, frame.get(i,frame.cols() * 3 / 4 ));
//        }
//        return oneCol;
//    }

    private Mat getCutedFrame(Mat frame){
        if(yCutTop + yCutDown >= frame.height()){
            yCutTop = constYCutTop;
            yCutDown = constYCutDown;
        }
        Mat cuted = new Mat(frame, new Rect(0, yCutTop, frame.width(), frame.height() - yCutTop - yCutDown));
        return cuted;
    }
// 1400 * 700
    private boolean isTrueSize(Mat frag){
//        if(frag.width() * frag.height() < minSquare){
//            return false;
//        }
        if(frag.width() < minWight || frag.height() < minHeight){
            return  false;
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
//        int i = 0;
        //int sleeping = 0;
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            logger.error("InterruptedException in second", e);
        }
        logger.info("Launch attempt threadSecond stream");
        while (isActive) {
            Container2Mat grayAndPrevGrayImages;
            while ((grayAndPrevGrayImages = images.poll()) != null) {
                SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
                Date dateImg = grayAndPrevGrayImages.getDate();
                String nameImg = formater.format(dateImg) +".png";
                //String nameImg = grayAndPrevGrayImages.getName();
                grayFrame = grayAndPrevGrayImages.getFirst();
                grayPrevFrame = grayAndPrevGrayImages.getSecond();
                frame = grayAndPrevGrayImages.getFull();
//                    double diff = compFrames(grayFrame, grayPrevFrame);

//                           System.out.println("Sleep");

                if(imgFlagBlur){
                    ///
                    File dirImgBlur = new File(dirBlurName);
                    if(!dirImgBlur.exists()){
                        dirImgBlur.mkdir();
                    }
                    try {
                        Imgcodecs.imwrite(dirImgBlur.getCanonicalPath() + File.separator + nameImg, grayFrame);
                    } catch (IOException e) {
                        logger.error("Saving error blur img", e);
                    }
                    ///
                }

                //sleeping = sleepCount;
//                Mat diffFrame = new Mat();
                grayFrameCut = getCutedFrame(grayFrame);
                grayPrevFrameCut = getCutedFrame(grayPrevFrame);
                grayFrame.release();
                grayPrevFrame.release();
                Mat fDiff = new Mat();
                Core.absdiff(grayFrameCut, grayPrevFrameCut, fDiff);
                grayFrameCut.release();
                grayPrevFrameCut.release();
                Imgproc.threshold(fDiff, diffFrame, thresholdSecond, 255, Imgproc.THRESH_BINARY);
                fDiff.release();
                if(imgFlagDiff) {
                    ///
                    File dirDiff = new File(dirDiffName);
                    if (!dirDiff.exists()) {
                        dirDiff.mkdir();
                    }
                    try {
                        Imgcodecs.imwrite(dirDiff.getCanonicalPath() + File.separator + nameImg, diffFrame);
                    } catch (IOException e) {
                        logger.error("Saving error diff img", e);;
                    }
                    ///
                }
//                System.out.println("Frame " + frameCount + " is significantly different");
                Rect boundingRect = Imgproc.boundingRect(diffFrame);
                diffFrame.release();

                frameCut = getCutedFrame(frame);
                frag = new Mat(frameCut, boundingRect);
                Imgproc.rectangle(frameCut, new Point(boundingRect.x, boundingRect.y),
                        new Point(boundingRect.x + boundingRect.width, boundingRect.y + boundingRect.height),
                        new Scalar(0, 0, 255), 2);
                frameCut.release();
                //Imgproc.resize(frame, frame, new Size(640, 480));
                if (isTrueSize(frag)) {
                    logger.info("A fragment of the required size in threadSecond stream");
                    //String path = "";
//                    try {
//                        path = dirImg + File.separator + "1-" + nameImg;
//                        File file1 = new File(path);
//                        path = dirImg + File.separator + "2-" + nameImg;
//                        File file2 = new File(path);
                        //Imgcodecs.imwrite(file1.getCanonicalPath(), frame);
                    Container2Mat fullAndFragImages = new Container2Mat(frame, frag, dateImg);
                    imageProcessing.toAddImg(fullAndFragImages);
//                        Imgcodecs.imwrite(file2.getCanonicalPath(), frag);
//                    } catch (Exception e) {
//                        logger.error("Error", e);
//                        //System.out.println(path + "\n" + e.getMessage());
//                    }
                }
                frameCut.release();
                frame.release();
                frag.release();
//                diffFrame.release();
//                grayFrame.release();
//                grayPrevFrame.release();
            }
        }

        try {
            stop();
        } catch (InterruptedException e) {
            logger.error("Completion error videoSecond stream", e);
            //System.out.println("Stop Except");
            throw new RuntimeException(e);
        }
        logger.info("The videoSecond stream is closed");
        //System.out.println("Video2Thr is finished");

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
