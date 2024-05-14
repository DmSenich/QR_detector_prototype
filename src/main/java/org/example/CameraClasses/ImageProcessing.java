package org.example.CameraClasses;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Queue;

public class ImageProcessing implements Runnable{

    // 4 основных метода - фильтрация (избавляемся от шума), бинаризация (трансформируем в черно-белое изображение),
    // выделение контура (находим координаты прямоугольника с этикеткой), считываем qr-код + отправляем считанные данные.



    private final File dirImg = new File("ImgProc");
    private final File dirQRImg = new File("ImgQR");

    private volatile Queue<Container2Mat> images = new LinkedList<>();
//    private volatile Queue<Mat> imagesQ = new LinkedList<>();
//    private volatile Queue<Mat> framesQ = new LinkedList<>();

    private String cash = "";
    private final int valueThresh = 190;
    private final int valueMedianBlur = 29;

    private final int sleepCount = 500;
    private volatile boolean isActive = true;
//    private Mat originalImage;

    public ImageProcessing(){
        if(!dirImg.exists()){
            dirImg.mkdir();
        }
        if(!dirQRImg.exists()){
            dirQRImg.mkdir();
        }
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
                    //break;
                    continue;
                }

                ///
                if(!dirImg.exists()){
                    dirImg.mkdir();
                }
                try {
                    Imgcodecs.imwrite(dirImg.getCanonicalPath() + File.separator + fullAndFragImages.getName(), image);
                } catch (IOException e) {
                    System.out.println("Not dirImg");
                    continue;
                }
                ///

                Mat matFiltered = toMedianFilter(image);

                ///
                File dirFiltered = new File("ImgFiltered");
                if(!dirFiltered.exists()){
                    dirFiltered.mkdir();
                }
                try {
                    if(matFiltered.empty()){
                        //break;
                        continue;
                    }
                    Imgcodecs.imwrite(dirFiltered.getCanonicalPath() + File.separator + fullAndFragImages.getName(), matFiltered);
                } catch (IOException e) {
                    System.out.println("Not dirFiltered");
                    continue;
                }
                ///

                Mat matBynary = toBynary(matFiltered);

                ///
                File dirBynary = new File("ImgBynary");
                if(!dirBynary.exists()){
                    dirBynary.mkdir();
                }
                try {
                    if(matBynary.empty()){
                        //break;
                        continue;
                    }
                    Imgcodecs.imwrite(dirBynary.getCanonicalPath() + File.separator + fullAndFragImages.getName(), matBynary);
                } catch (IOException e) {
                    System.out.println("Not dirBynary");
                    continue;
                }
                ///

                Mat fragmentQR = toFindWhiteContour(image, matBynary);
                String qrPath = dirQRImg + File.separator + "qr-" + fullAndFragImages.getName();
                File qrFile = new File(qrPath);
                try {
                    if(fragmentQR.empty()){
                        //break;
                        continue;
                    }
                    Imgcodecs.imwrite(qrFile.getCanonicalPath(),fragmentQR);
                } catch (IOException e) {
                    System.out.println(e.getMessage());
                    continue;
                }
                if(toReadQR(qrPath)){
                    File dirFullFrame = new File("ImgFullFrame");
                    if(!dirFullFrame.exists()){
                        dirFullFrame.mkdir();
                    }
                    try {
                        Imgcodecs.imwrite(dirFullFrame.getCanonicalPath() + File.separator + "full-" + fullAndFragImages.getName(), fullImage);
                    } catch (IOException e) {
                        System.out.println("Not dirFull");
                        continue;
                    }
                }

                //fileImg.delete();
            }
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                System.out.println("Not sleep");
            }


            //sleeping = sleepCount;
        }
        System.out.println("ImageThr is finished");

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

        images.add(imgs);
    }

    public void toDisable() {
        isActive = false;
    }

    private Mat toMedianFilter(Mat matFragment){
        Mat matFiltered = new Mat();
        Imgproc.cvtColor(matFragment, matFiltered, Imgproc.COLOR_BGR2GRAY);
        Imgproc.medianBlur(matFiltered, matFiltered, valueMedianBlur);
        return matFiltered;
    }

    private Mat toBynary(Mat matFiltered){
        Mat matBynary = new Mat();
        Imgproc.threshold(matFiltered, matBynary, valueThresh, 255, Imgproc.THRESH_BINARY);

        ///
        File dirBynary = new File("ImgBynary");
        if(!dirBynary.exists()){
            dirBynary.mkdir();
        }
        try {
            Imgcodecs.imwrite(dirBynary.getCanonicalPath() + File.separator + "Bynary0.png", matBynary);
        } catch (IOException e) {
            System.out.println("toBynary\n" + e.getMessage());
            throw new RuntimeException(e);
        }
        ///

        Imgproc.medianBlur(matBynary, matBynary, valueMedianBlur);
        return matBynary;
    }

    private Mat toFindWhiteContour(Mat original,Mat matBynary){
        Rect boundingRect = Imgproc.boundingRect(matBynary);
        Mat matQRFragment = new Mat(original, boundingRect);
        return matQRFragment;
    }

    private boolean toReadQR(String path){
        boolean res = true;
        try{
            BufferedImage readerImage = ImageIO.read(new FileInputStream(path));
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(readerImage)));
            Result resultObj = new MultiFormatReader().decode(binaryBitmap);
            if(resultObj != null && resultObj.getText() != null && !resultObj.getText().equals(cash)){
                toSaveQR(resultObj.getText());
                cash = resultObj.getText();
            }
            else{
                res = false;
            }

        }catch (Exception ex){
            System.out.println(ex.getMessage());
            res = false;
        }
        return res;
    }

    private void toSaveQR(String data){
        String path = "SavingQR.txt";
        try(FileWriter writer = new FileWriter(path, true)) {
            writer.write(data);
            writer.write("\n");
        }catch (IOException ex){
            System.out.println("toSaveQR\n" + ex.getMessage());
        }
    }


}
