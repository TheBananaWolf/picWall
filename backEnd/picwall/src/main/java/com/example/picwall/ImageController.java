package com.example.picwall;


import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Stream2BufferedImage;
import org.im4java.process.Pipe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class ImageController {
    @Value("${imagemagick.localtion.windows}")
    private String ImagemagickLocaltionWindows;

    @Value("${app.image.location}")
    private Path imageLocation;
    @Value("${livp.image.location}")
    private Path livpimageLocation;
    List<Path> imageFiles;

    @GetMapping("/showmedia")
    public ResponseEntity<byte[]> getRandomImage() throws IOException, InterruptedException {
        if (imageFiles == null || imageFiles.isEmpty()) {
            imageFiles = getImagesFiles();
        }
        Path imagePath = imageFiles.get(new SecureRandom().nextInt(imageFiles.size()));
        String filename = imagePath.getFileName().toString();
        String extension = filename.substring(filename.lastIndexOf('.') + 1);
        if ("heic".equalsIgnoreCase(extension)) {
            return returnJpg(imagePath);
        }
        if ("livp".equalsIgnoreCase(extension)) {
            return unzipAndReturnJpg(imagePath);
        } else {
            return returnImageFile(imagePath);
        }
    }

    private ResponseEntity<byte[]> returnJpg(Path imagePath) {
        try {
            byte[] heicBytes = Files.readAllBytes(imagePath);
            byte[] jpegBytes = convertHEICtoJPEG(heicBytes);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(jpegBytes);
        } catch (IOException | InterruptedException | IM4JavaException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    public byte[] convertHEICtoJPEG(byte[] heicBytes) throws IOException, InterruptedException, IM4JavaException, IM4JavaException {
        IMOperation op = new IMOperation();
        op.addImage("-");
        op.addImage("jpeg:-");
        File tempFile = File.createTempFile("temp", ".heic");
        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(heicBytes);
        }
        FileInputStream fis = new FileInputStream(tempFile);
        Pipe pipeIn  = new Pipe(fis,null);
        ConvertCmd convert = new ConvertCmd();
//        convert.setSearchPath(ImagemagickLocaltionLinux);
        convert.setInputProvider(pipeIn);
        Stream2BufferedImage s2b = new Stream2BufferedImage();
        convert.setOutputConsumer(s2b);
        convert.run(op);
        BufferedImage image = s2b.getImage();
        byte[] imageBytes = imageToBytes(image);
        fis.close();
        return imageBytes;

    }


    private static byte[] imageToBytes(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpeg", baos);
        baos.flush();
        byte[] imageBytes = baos.toByteArray();
        baos.close();
        return imageBytes;
    }

    private ResponseEntity<byte[]> unzipAndReturnJpg(Path zipPath) throws IOException, InterruptedException {
        Path outputDir = livpimageLocation;
        ProcessBuilder builder = new ProcessBuilder("7z", "x", zipPath.toString(), "-o" + outputDir.toString(), "*.jpeg", "-y");

        Process process = builder.start();
        process.waitFor();

        try (Stream<Path> paths = Files.walk(outputDir)) {
            Path jpgPath = paths.filter(p -> p.toString().endsWith(".jpeg")).findFirst().orElse(null);
            if (jpgPath != null && Files.isReadable(jpgPath)) {
                byte[] imageBytes = Files.readAllBytes(jpgPath);
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_JPEG)
                        .body(imageBytes);
            }
        }
        return ResponseEntity.notFound().build();
    }

    private ResponseEntity<byte[]> returnImageFile(Path imagePath) throws IOException {
        Resource image = new UrlResource(imagePath.toUri());
        if (image.exists() && image.isReadable()) {
            System.out.println(imagePath);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(Files.probeContentType(imagePath)))
                    .body(Files.readAllBytes(imagePath));
        }
        return ResponseEntity.notFound().build();
    }

    private List<Path> getImagesFiles() {
        try (Stream<Path> paths = Files.walk(imageLocation)) {
            return paths.filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
