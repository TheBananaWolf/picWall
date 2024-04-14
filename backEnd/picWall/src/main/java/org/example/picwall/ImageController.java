package org.example.picwall;


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
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
public class ImageController {

    private final Path imageLocation = Paths.get("E:\\picWall\\imgs");
    @GetMapping("/showimage")
    public ResponseEntity<Resource> getRandomImage() throws MalformedURLException {
        List<Path> imageFiles;
        try (Stream<Path> paths = Files.walk(imageLocation)) {
            imageFiles = paths
                    .filter(Files::isRegularFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }

        if (imageFiles.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Path imagePath = imageFiles.get(new Random().nextInt(imageFiles.size()));
        Resource image = new UrlResource(imagePath.toUri());

        if (image.exists() && image.isReadable()) {
            String contentType = determineContentType(imagePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(image);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private String determineContentType(Path imagePath) {
        try {
            String contentType = Files.probeContentType(imagePath);
            return contentType != null ? contentType : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}
