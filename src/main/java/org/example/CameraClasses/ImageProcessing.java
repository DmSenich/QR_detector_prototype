package org.example.CameraClasses;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.example.DBHelper;
import org.example.Main;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    private final String constDirProgName = "ImgProc";
    private final String constDirQRName = "ImgQR";
    private final String constDirFullName = "ImgFullFrame";
    private final String constDirFilterName = "ImgFiltered";
    private final String constDirBinaryName = "ImgBinary";
    private String dirProgName;
    private String dirQRName;
    private String dirFullName;
    private String dirFilterName;
    private String dirBinaryName;
//    private File dirImg;
    //private File tempDirQRImg = new File("tempQR");

    private volatile Queue<Container2Mat> images = new LinkedList<>();
//    private volatile Queue<Mat> imagesQ = new LinkedList<>();
//    private volatile Queue<Mat> framesQ = new LinkedList<>();

    private String currentCode = "";
    private final int constValueThresh = 190;
    private final int constValueMedianBlur = 29;
    private final int constTimeCheck = 3000;
    private int valueThresh;
    private int valueMedianBlur;
    private int timeCheck;

//    private final int sleepCount = 500;
    private volatile boolean isActive = true;
//    private Mat originalImage;
    private boolean imgFlagFull = false;
    private boolean imgFlagQR = false;
    private boolean imgFlagBinary = false;
    private boolean imgFlagFiltered = false;
    private boolean imgFlagProg = false;

    private Mat fullImage;
    private Mat image;
    private Mat matFiltered;
    private Mat matBinary;
    private Mat fragmentQR;

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
            if(valueThresh <= 0 || valueMedianBlur <= 0 || timeCheck <= 0){
                valueThresh = constValueThresh;
                valueMedianBlur = constValueMedianBlur;
                timeCheck = constTimeCheck;
                logger.info("Invalid params params_system.properties for imageProcessing, the default values are set");
            }
        }catch (Exception e){
            logger.error("Reading error params_system.properties for imageProcessing, the default values are set", e);
            valueThresh = constValueThresh;
            valueMedianBlur = constValueMedianBlur;
            timeCheck = constTimeCheck;
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
        }
        catch (Exception e){
            logger.error("Reading error images.properties for imageProcessing, the default values are set", e);
        }

        toInitPathsAndDirs();

        fullImage = new Mat();
        image = new Mat();
        matFiltered = new Mat();
        matBinary = new Mat();
        fragmentQR = new Mat();

//        if(!tempDirQRImg.exists()){
//            tempDirQRImg.mkdir();
//            logger.info("Create directory: " + tempDirQRImg.getName());
//        }
        logger.info("Params of imageProcessing stream: time_check = " + timeCheck +", valueThresh = " + valueThresh + ", valueMedianBlur = " + valueMedianBlur);
    }

    private void toInitPathsAndDirs() {
        // imgFlagFull
        if (imgFlagFull) {
            try {
                dirFullName = img_properties.getProperty("dir_img.full");
                String pathFull = img_properties.getProperty("path_img.full");
                Path pFull = Paths.get(pathFull);
                Path fullPathFull = pFull.resolve(dirFullName);
                File fileFull = new File(pathFull);
                if (fileFull.exists()) {
                    File fullFileFull = new File(fullPathFull.toString());
                    if (!fullFileFull.exists()) {
                        fullFileFull.mkdir();
                    }
                    dirFullName = fullFileFull.getCanonicalPath();
                } else {
                    logger.info("Invalid path for Full images.properties for videoSecond, values are set '' ");
                }
            } catch (Exception e) {
                logger.error("Reading dir Full error images.properties for videoSecond, the default values are set", e);
                dirFullName = constDirFullName;
            }
        }

// imgFlagProg
        if (imgFlagProg) {
            try {
                dirProgName = img_properties.getProperty("dir_img.prog");
                String pathProg = img_properties.getProperty("path_img.prog");
                Path pProg = Paths.get(pathProg);
                Path fullPathProg = pProg.resolve(dirProgName);
                File fileProg = new File(pathProg);
                if (fileProg.exists()) {
                    File fullFileProg = new File(fullPathProg.toString());
                    if (!fullFileProg.exists()) {
                        fullFileProg.mkdir();
                    }
                    dirProgName = fullFileProg.getCanonicalPath();
                } else {
                    logger.info("Invalid path for Prog images.properties for videoSecond, values are set '' ");
                }
            } catch (Exception e) {
                logger.error("Reading dir Prog error images.properties for videoSecond, the default values are set", e);
                dirProgName = constDirProgName;
            }
        }

// imgFlagQR
        if (imgFlagQR) {
            try {
                dirQRName = img_properties.getProperty("dir_img.QR");
                String pathQR = img_properties.getProperty("path_img.QR");
                Path pQR = Paths.get(pathQR);
                Path fullPathQR = pQR.resolve(dirQRName);
                File fileQR = new File(pathQR);
                if (fileQR.exists()) {
                    File fullFileQR = new File(fullPathQR.toString());
                    if (!fullFileQR.exists()) {
                        fullFileQR.mkdir();
                    }
                    dirQRName = fullFileQR.getCanonicalPath();
                } else {
                    logger.info("Invalid path for QR images.properties for videoSecond, values are set '' ");
                }
            } catch (Exception e) {
                logger.error("Reading dir QR error images.properties for videoSecond, the default values are set", e);
                dirQRName = constDirQRName;
            }
        }

// imgFlagFiltered
        if (imgFlagFiltered) {
            try {
                dirFilterName = img_properties.getProperty("dir_img.filter");
                String pathFiltered = img_properties.getProperty("path_img.filter");
                Path pFiltered = Paths.get(pathFiltered);
                Path fullPathFiltered = pFiltered.resolve(dirFilterName);
                File fileFiltered = new File(pathFiltered);
                if (fileFiltered.exists()) {
                    File fullFileFiltered = new File(fullPathFiltered.toString());
                    if (!fullFileFiltered.exists()) {
                        fullFileFiltered.mkdir();
                    }
                    dirFilterName = fullFileFiltered.getCanonicalPath();
                } else {
                    logger.info("Invalid path for Filtered images.properties for videoSecond, values are set '' ");
                }
            } catch (Exception e) {
                logger.error("Reading dir Filtered error images.properties for videoSecond, the default values are set", e);
                dirFilterName = constDirFilterName;
            }
        }

// imgFlagBinary
        if (imgFlagBinary) {
            try {
                dirBinaryName = img_properties.getProperty("dir_img.binary");
                String pathBinary = img_properties.getProperty("path_img.binary");
                Path pBinary = Paths.get(pathBinary);
                Path fullPathBinary = pBinary.resolve(dirBinaryName);
                File fileBinary = new File(pathBinary);
                if (fileBinary.exists()) {
                    File fullFileBinary = new File(fullPathBinary.toString());
                    if (!fullFileBinary.exists()) {
                        fullFileBinary.mkdir();
                    }
                    dirBinaryName = fullFileBinary.getCanonicalPath();
                } else {
                    logger.info("Invalid path for Binary images.properties for videoSecond, values are set '' ");
                }
            } catch (Exception e) {
                logger.error("Reading dir Binary error images.properties for videoSecond, the default values are set", e);
                dirBinaryName = constDirBinaryName;
            }
        }
    }

    @Override
    public void run() {
        //int sleeping = 0;
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            logger.error("InterruptedException in second", e);
        }
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
                fullImage = fullAndFragImages.getFirst();
                image = fullAndFragImages.getSecond();
                Date dateImg = fullAndFragImages.getDate();
                SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss");
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
                    image.release();
                    fullImage.release();
                    //break;
                    continue;
                }

                if(imgFlagProg) {
                    ///
                    File dirImg = new File(dirProgName);
                    if (!dirImg.exists()) {
                        dirImg.mkdir();
                    }
                    try {
                        Imgcodecs.imwrite(dirImg.getCanonicalPath() + File.separator + nameImg, image);
                    } catch (IOException e) {
                        //System.out.println("Not dirImg");
                        logger.error("Saving error prog img", e);
                        continue;
                    }
                    ///
                }
                matFiltered = toMedianFilter(image);

                if(imgFlagFiltered) {
                    ///
                    File dirFiltered = new File(dirFilterName);
                    if (!dirFiltered.exists()) {
                        dirFiltered.mkdir();
                    }
                    try {
                        if (matFiltered.empty()) {
                            matFiltered.release();
                            image.release();
                            fullImage.release();
                            //break;
                            continue;
                        }
                        Imgcodecs.imwrite(dirFiltered.getCanonicalPath() + File.separator + nameImg, matFiltered);
                    } catch (IOException e) {
                        logger.error("Saving error filter img", e);
                        //System.out.println("Not dirFiltered");
                        continue;
                    }
                    ///
                }
                matBinary = toBinary(matFiltered);
                matFiltered.release();

                if(imgFlagBinary) {
                    ///
                    File dirBinary = new File(dirBinaryName);
                    if (!dirBinary.exists()) {
                        dirBinary.mkdir();
                    }
                    try {
                        if (matBinary.empty()) {
                            matFiltered.release();
                            matBinary.release();
                            image.release();
                            fullImage.release();
                            //break;
                            continue;
                        }
                        Imgcodecs.imwrite(dirBinary.getCanonicalPath() + File.separator + nameImg, matBinary);
                    } catch (IOException e) {
                        logger.error("Saving error binary img", e);
                        //System.out.println("Not dirBinary");
                        continue;
                    }
                    ///
                }
                fragmentQR = toFindWhiteContour(image, matBinary);
                matBinary.release();
                image.release();
                if(fragmentQR.empty()){
                    //break;
//                    fragmentQR.release();
//                    matFiltered.release();
//                    matBynary.release();
//                    image.release();
//                    fullImage.release();
                    logger.info("Fragment is empty");
                    continue;
                }
//                String qrPath = tempDirQRImg + File.separator + "qr-" + nameImg;
//                File qrFile = new File(qrPath);
//                try {
//                    if(fragmentQR.empty()){
//                        //break;
//                        logger.info("Fragment is empty");
//                        continue;
//                    }
//                    Imgcodecs.imwrite(qrFile.getCanonicalPath(),fragmentQR);
//                } catch (IOException e) {
//                    logger.error("Saving error qr img temp", e);
//                    //System.out.println(e.getMessage());
//                    continue;
//                }
                if(imgFlagQR){
                    File dirQR = new File(dirQRName);
                    if(!dirQR.exists()){
                        dirQR.mkdir();
                    }
                    try {

//                        Files.copy(qrFile.getCanonicalFile().toPath(), dirQR.getCanonicalFile().toPath().resolve(nameImg));
                        Imgcodecs.imwrite(dirQR.getCanonicalPath() + File.separator + nameImg, fragmentQR);
                    } catch (IOException e) {
                        logger.error("Saving error qr img", e);
                    }
                }

                if(toReadQR(fragmentQR)){
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
                        }
                    }
                }
                fragmentQR.release();
//                matFiltered.release();
//                matBynary.release();
//                image.release();
                fullImage.release();
                //fileImg.delete();
            }
            try {
                Thread.sleep(timeCheck);
            } catch (InterruptedException e) {
                logger.error("InterruptedException in ImageProcessing", e);
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

    private Mat toBinary(Mat matFiltered){
        Mat binary = new Mat();
        Imgproc.threshold(matFiltered, binary, valueThresh, 255, Imgproc.THRESH_BINARY);

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
        Imgproc.medianBlur(binary, binary, valueMedianBlur);
        logger.info("Binarization in imageProcessing");
        return binary;
    }

    private Mat toFindWhiteContour(Mat original,Mat matBynary){
        Rect boundingRect = Imgproc.boundingRect(matBynary);
        Mat matQRFragment = new Mat(original, boundingRect);
        logger.info("Search white fragment");
        return matQRFragment;
    }

    private boolean toReadQR(Mat matQR){
        boolean res = true;
        try{
            MatOfByte matOfByte = new MatOfByte();
            Imgcodecs.imencode(".png", matQR, matOfByte);
            byte[] bytes = matOfByte.toArray();
//            BufferedImage readerImage = ImageIO.read(new FileInputStream(path));
            BufferedImage readerImage = ImageIO.read(new ByteArrayInputStream(bytes));
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
            logger.error("Reading error qr-code", e);
            res = false;
        }
        return res;
    }

    private void toSaveQR(String code, Date date){
        logger.info("Saving QR-code");
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
