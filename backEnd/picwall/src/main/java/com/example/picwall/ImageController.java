package com.example.picwall;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
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

        if ("livp".equalsIgnoreCase(extension)) {
            return unzipAndReturnJpg(imagePath);
        } else {
            return returnImageFile(imagePath);
        }
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
