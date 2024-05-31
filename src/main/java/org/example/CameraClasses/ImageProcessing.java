package org.example.CameraClasses;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.example.DBHelper;
import org.example.Main;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Queue;

public class ImageProcessing implements Runnable{

    // 4 основных метода - фильтрация (избавляемся от шума), бинаризация (трансформируем в черно-белое изображение),
    // выделение контура (находим координаты прямоугольника с этикеткой), считываем qr-код + отправляем считанные данные.
    private final String url_img_prop = "images.properties";
    private static Properties img_properties;
    private final String url_params_prop = "params_system.properties";
    private static Properties params_properties;
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    private String dirProgName = "ImgProc";
    private String dirQRName = "ImgQR";
    private String dirFullName = "ImgFullFrame";
    private String dirFilterName = "ImgFiltered";
    private String dirBinaryName = "ImgBinary";
    private File dirImg;
    private File dirQRImg;

    private volatile Queue<Container2Mat> images = new LinkedList<>();
//    private volatile Queue<Mat> imagesQ = new LinkedList<>();
//    private volatile Queue<Mat> framesQ = new LinkedList<>();

    private String currentCode = "";
    private int valueThresh = 190;
    private int valueMedianBlur = 29;
    private int timeCheck = 3000;

//    private final int sleepCount = 500;
    private volatile boolean isActive = true;
//    private Mat originalImage;
    private boolean imgFlagFull = false;
    private boolean imgFlagQR = false;
    private boolean imgFlagBinary = false;
    private boolean imgFlagFiltered = false;
    private boolean imgFlagProg = false;

    public ImageProcessing(){
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
            valueThresh = Integer.parseInt(params_properties.getProperty("image_process.threshold"));
            valueMedianBlur = Integer.parseInt(params_properties.getProperty("image_process.median_blur"));
            timeCheck = Integer.parseInt(params_properties.getProperty("image_process.time_check"));
        }catch (Exception e){
            logger.error("Reading error params_system.properties for imageProcessing, the default values are set", e);
        }

        //URL url_img = Main.class.getClassLoader().getResource(url_img_prop);
        img_properties = new Properties();
        try(InputStream inputStream = Main.class.getClassLoader().getResourceAsStream(url_img_prop)) {
            img_properties.load(inputStream);
        } catch (IOException e) {
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
            imgFlagFull = Boolean.parseBoolean(img_properties.getProperty("flag.full"));
            imgFlagProg = Boolean.parseBoolean(img_properties.getProperty("flag.prog"));
            imgFlagQR = Boolean.parseBoolean(img_properties.getProperty("flag.QR"));
            imgFlagFiltered = Boolean.parseBoolean(img_properties.getProperty("flag.filter"));
            imgFlagBinary = Boolean.parseBoolean(img_properties.getProperty("flag.binary"));
            dirFullName = img_properties.getProperty("dir_img.full");
            dirProgName = img_properties.getProperty("dir_img.prog");
            dirQRName = img_properties.getProperty("dir_img.QR");
            dirFilterName = img_properties.getProperty("dir_img.filter");
            dirBinaryName = img_properties.getProperty("dir_img.binary");
        }
        catch (Exception e){
            logger.error("Reading error images.properties for imageProcessing, the default values are set", e);
        }

        dirImg = new File(dirProgName);
        dirQRImg = new File(dirQRName);
        if(!dirImg.exists()){
            dirImg.mkdir();
            logger.info("Создание папки " + dirImg.getName());
        }
        if(!dirQRImg.exists()){
            dirQRImg.mkdir();
            logger.info("Создание папки " + dirQRImg.getName());
        }
        logger.info("Params of imageProcessing stream: time_check = " + timeCheck +", valueThresh = " + valueThresh + ", valueMedianBlur = " + valueMedianBlur);
    }
    @Override
    public void run() {
        //int sleeping = 0;
        while (isActive){
//            if(sleeping > 0){
//                sleeping--;
//                continue;
//            }

//            List<File> imgFiles = Arrays.stream(dirImg.listFiles((File file) -> file.isFile() && file.getName().endsWith(".png"))).toList();
//            if(imgFiles.isEmpty()){
//                sleeping = sleepCount;
//                continue;
//            }

            Container2Mat fullAndFragImages;
            while ((fullAndFragImages = images.poll()) != null) {
//                (image = imagesQ.poll()) != null
                Mat fullImage = fullAndFragImages.getFirst();
                Mat image = fullAndFragImages.getSecond();
                Date dateImg = fullAndFragImages.getDate();
                SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd__HH-mm-ss");
                String nameImg = formater.format(dateImg) +".png";

                //Mat image;
                //String path;
//                try {
//                    path = fileImg.getCanonicalPath();
//                    image = Imgcodecs.imread(path);
//                } catch (IOException e) {
////                    throw new RuntimeException(e);
//                    System.out.println(e.getMessage());
//                    continue;
//                }
                if(image.empty()){
                    logger.info("ImageProcessing queue is empty");
                    //break;
                    continue;
                }

                if(imgFlagProg) {
                    ///
                    if (!dirImg.exists()) {
                        dirImg.mkdir();
                    }
                    try {
                        Imgcodecs.imwrite(dirImg.getCanonicalPath() + File.separator + nameImg, image);
                    } catch (IOException e) {
                        //System.out.println("Not dirImg");
                        continue;
                    }
                    ///
                }
                Mat matFiltered = toMedianFilter(image);

                if(imgFlagFiltered) {
                    ///
                    File dirFiltered = new File(dirFilterName);
                    if (!dirFiltered.exists()) {
                        dirFiltered.mkdir();
                    }
                    try {
                        if (matFiltered.empty()) {
                            //break;
                            continue;
                        }
                        Imgcodecs.imwrite(dirFiltered.getCanonicalPath() + File.separator + nameImg, matFiltered);
                    } catch (IOException e) {
                        //System.out.println("Not dirFiltered");
                        continue;
                    }
                    ///
                }
                Mat matBynary = toBynary(matFiltered);

                if(imgFlagBinary) {
                    ///
                    File dirBinary = new File(dirBinaryName);
                    if (!dirBinary.exists()) {
                        dirBinary.mkdir();
                    }
                    try {
                        if (matBynary.empty()) {
                            //break;
                            continue;
                        }
                        Imgcodecs.imwrite(dirBinary.getCanonicalPath() + File.separator + nameImg, matBynary);
                    } catch (IOException e) {
                        //System.out.println("Not dirBinary");
                        continue;
                    }
                    ///
                }
                Mat fragmentQR = toFindWhiteContour(image, matBynary);
                String qrPath = dirQRImg + File.separator + "qr-" + nameImg;
                File qrFile = new File(qrPath);
                try {
                    if(fragmentQR.empty()){
                        //break;
                        logger.info("Fragment is empty");
                        continue;
                    }
                    Imgcodecs.imwrite(qrFile.getCanonicalPath(),fragmentQR);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    continue;
                }
                if(toReadQR(qrPath)){
                    logger.info("The QR-code is successfully read: " + currentCode);
                    toSaveQR(currentCode, dateImg);
                    if(imgFlagFull){
                        File dirFullFrame = new File(dirFullName);
                        if(!dirFullFrame.exists()){
                            dirFullFrame.mkdir();
                        }
                        try {
                            logger.info("Save full frame");
                            Imgcodecs.imwrite(dirFullFrame.getCanonicalPath() + File.separator + "full-" + nameImg, fullImage);
                        } catch (IOException e) {
                            logger.error("Saving error full frame", e);
                            System.out.println("Not dirFull");
                            continue;
                        }
                    }
                }
                if(!imgFlagQR){
                    qrFile.delete();
                }

                //fileImg.delete();
            }
            try {
                Thread.sleep(timeCheck);
            } catch (InterruptedException e) {
                System.out.println("Not sleep");
            }


            //sleeping = sleepCount;
        }
        logger.info("ImageProcessing stream is closed");
        //System.out.println("ImageThr is finished");

    }

    public void toAddImg(Container2Mat imgs){
        ///
//        if(!dirImg.exists()){
//            dirImg.mkdir();
//        }
//        try {
//            Imgcodecs.imwrite(dirImg.getCanonicalPath() + File.separator + "img0.png", img);
//        } catch (IOException e) {
//            System.out.println("Not dirFiltered");
//        }
        ///
//        imagesQ.add(img);
//        framesQ.add(frame);
        logger.info("Adding data in queue of imageProcessing");
        images.add(imgs);
    }

    public void toDisable() {
        isActive = false;
    }

    private Mat toMedianFilter(Mat matFragment){
        Mat matFiltered = new Mat();
        Imgproc.cvtColor(matFragment, matFiltered, Imgproc.COLOR_BGR2GRAY);
        Imgproc.medianBlur(matFiltered, matFiltered, valueMedianBlur);
        logger.info("Median filter in imageProcessing");
        return matFiltered;
    }

    private Mat toBynary(Mat matFiltered){
        Mat matBynary = new Mat();
        Imgproc.threshold(matFiltered, matBynary, valueThresh, 255, Imgproc.THRESH_BINARY);

//        if(imgFlagBinary) {
//            ///
//            File dirBynary = new File(dirBinaryName);
//            if (!dirBynary.exists()) {
//                dirBynary.mkdir();
//            }
//            try {
//                Imgcodecs.imwrite(dirBynary.getCanonicalPath() + File.separator + "Bynary0.png", matBynary);
//            } catch (IOException e) {
//                System.out.println("toBynary\n" + e.getMessage());
//                throw new RuntimeException(e);
//            }
//            ///
//        }
        Imgproc.medianBlur(matBynary, matBynary, valueMedianBlur);
        logger.info("Binarization in imageProcessing");
        return matBynary;
    }

    private Mat toFindWhiteContour(Mat original,Mat matBynary){
        Rect boundingRect = Imgproc.boundingRect(matBynary);
        Mat matQRFragment = new Mat(original, boundingRect);
        logger.info("Search white fragment");
        return matQRFragment;
    }

    private boolean toReadQR(String path){
        boolean res = true;
        try{
            BufferedImage readerImage = ImageIO.read(new FileInputStream(path));
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(readerImage)));
            Result resultObj = new MultiFormatReader().decode(binaryBitmap);
            if(resultObj != null && resultObj.getText() != null && !resultObj.getText().equals(currentCode)){
                //toSaveQR(resultObj.getText());
                currentCode = resultObj.getText();
            }
            else{
                res = false;
            }

        }catch (Exception e){
            logger.error("Ошибка чтения qr-кода", e);
            System.out.println(e.getMessage());
            res = false;
        }
        return res;
    }

    private void toSaveQR(String code, Date date){
        logger.info("Сохранение QR-кода");
//        String path = "SavingQR.txt";
//        try(FileWriter writer = new FileWriter(path, true)) {
//            writer.write(data);
//            writer.write("\n");
//        }catch (IOException e){
//            logger.error("Ошибка сохранения данных", e);
//            System.out.println("toSaveQR\n" + e.getMessage());
//        }
        DBHelper.insert(date, code);
    }


}
