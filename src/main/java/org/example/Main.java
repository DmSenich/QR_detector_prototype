package org.example;

import nu.pattern.OpenCV;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.example.CameraClasses.VideoStreamFirstProcessing;
import org.opencv.videoio.VideoCapture;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_QUIET;
import static org.bytedeco.ffmpeg.global.avutil.av_log_set_level;
//import org.bytedeco.javacpp.avutil.AVUtil;

import java.util.Scanner;

public class Main {
    static VideoCapture camera;
    static VideoStreamFirstProcessing videoStreamProcessing;
    static Thread threadVideo;
    private static final String pathVideo = "";
    static {
        OpenCV.loadLocally();
    }
    public static void main(String[] args) throws InterruptedException {
        System.out.println("Hello world!");

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

        threadVideo = new Thread(videoStreamProcessing);
        threadVideo.start();

        while (true) {
            String input = scanner.nextLine();
            if ("exit".equals(input)) {
                System.out.println("Завершение работы...");
                break;
            }
        }
        videoStreamProcessing.toDisable();
        threadVideo.join();

        System.out.println("Завершение потока консоли...");



    }
}