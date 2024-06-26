package org.example;

import nu.pattern.OpenCV;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.example.CameraClasses.VideoStreamFirstProcessing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_QUIET;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;
//import org.bytedeco.javacpp.avutil.AVUtil;

import java.io.*;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;


public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
//    static VideoCapture camera;
    private static VideoStreamFirstProcessing videoStreamProcessing;
    private static Thread threadVideo;
    private static String url_system_prop = "params_system.properties";
    private static String pathVideo;
    static {
        OpenCV.loadLocally();
        logger.info("OpenCV is loaded");

//        // Загрузка файла конфигурации logback.xml из папки properties
//        try {
//            org.slf4j.bridge.SLF4JBridgeHandler.install();
//            ch.qos.logback.classic.util.ContextInitializer.CONFIG_FILE = "properties.logback.xml";
//            ch.qos.logback.classic.LoggerContext loggerContext = (ch.qos.logback.classic.LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
//            loggerContext.reset();
//        } catch (Exception e) {
//            logger.error("Failed to load logback configuration file: {}", e.getMessage());
//        }
    }
    public static void main(String[] args) throws InterruptedException {
        logger.info("Application is started");
        DBHelper.toInit();


        //URL url = Main.class.getClassLoader().getResource(url_system_prop);
        Properties sys_properties = new Properties();
//        FileInputStream fis;
//        try{
//            fis = new FileInputStream(url.getFile());
//            sys_properties.load(fis);
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
        try(InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(url_system_prop)) {
            sys_properties.load(inputStream);
        } catch (IOException e) {
            logger.error("Reading error params_system.properties", e);
            throw new RuntimeException(e);
        }
        try{
            pathVideo = sys_properties.getProperty("system.camera_url");
        }
        catch (Exception e){
            logger.error("Reading error params_system.properties", e);
        }


        Scanner scanner = new java.util.Scanner(System.in);

        // Отключаем вывод предупреждений в консоль
//        AVUtil.av_log_set_level(AVUtil.AV_LOG_ERROR);
        av_log_set_level(AV_LOG_QUIET);
        FFmpegFrameGrabber camera = new FFmpegFrameGrabber(pathVideo);
        camera.setOption("rtsp_transport", "tcp");
        camera.setOption("stimeout", "5000");
        camera.setFrameRate(10);

//        camera = new VideoCapture(pathVideo); //для исползования файла видео


        videoStreamProcessing = new VideoStreamFirstProcessing(camera);

        //imageProcessing = new ImageProcessing();

        logger.info("Launch attempt threadVideo stream");
        threadVideo = new Thread(videoStreamProcessing);
        threadVideo.start();

        while (true) {
            String input = scanner.nextLine();
            if ("exit".equals(input)) {
                //System.out.println("Completion ...");
                logger.info("Start of the completion of application");
                break;
            }
        }
        //Нужен timeOut для завершения в случае отсутствия потоков

//        Thread.State state = threadVideo.getState();
//        System.out.println(state.name());
        videoStreamProcessing.toDisable();
        threadVideo.join();

        logger.info("Application is finished");



    }
}