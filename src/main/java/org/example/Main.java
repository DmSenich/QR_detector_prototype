package org.example;

import nu.pattern.OpenCV;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.example.CameraClasses.VideoStreamFirstProcessing;
import org.opencv.videoio.VideoCapture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_QUIET;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;
//import org.bytedeco.javacpp.avutil.AVUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.Properties;
import java.util.Scanner;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
//    static VideoCapture camera;
    static VideoStreamFirstProcessing videoStreamProcessing;
    static Thread threadVideo;
    private static String url_system_prop = "params_system.properties";
    private static String pathVideo;// = "rtsp://admin:Itkzktcjv02@10.10.20.205/Streaming/Channels/101";
    static {
        OpenCV.loadLocally();
        logger.info("OpenCV загружен");
    }
    public static void main(String[] args) throws InterruptedException {
        logger.info("Приложение запущено");
        DBHelper.toInit();


        URL url = Main.class.getClassLoader().getResource(url_system_prop);
        Properties sys_properties = new Properties();
        FileInputStream fis;
        try{
            fis = new FileInputStream(url.getFile());
            sys_properties.load(fis);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try{
            pathVideo = sys_properties.getProperty("system.url_camera");
        }
        catch (Exception e){
            logger.error("Ошибка чтения params_system.properties", e);
        }


        Scanner scanner = new java.util.Scanner(System.in);

        // Отключаем вывод предупреждений в консоль
//        AVUtil.av_log_set_level(AVUtil.AV_LOG_ERROR);
        av_log_set_level(AV_LOG_QUIET);
        FFmpegFrameGrabber camera = new FFmpegFrameGrabber(pathVideo);
        camera.setOption("rtsp_transport", "tcp");
        camera.setOption("stimeout", "50000");
        camera.setFrameRate(10);

//        camera = new VideoCapture(pathVideo); //для исползования файла видео


        videoStreamProcessing = new VideoStreamFirstProcessing(camera);

        //imageProcessing = new ImageProcessing();

        logger.info("Попытка запуска потока threadVideo");
        threadVideo = new Thread(videoStreamProcessing);
        threadVideo.start();

        while (true) {
            String input = scanner.nextLine();
            if ("exit".equals(input)) {
                System.out.println("Завершение работы...");
                logger.info("Начало завершения приложения");
                break;
            }
        }
        //Нужен timeOut для завершения в случае отсутствия потоков

        videoStreamProcessing.toDisable();
        threadVideo.join();

        logger.info("Приложение завершено");



    }
}