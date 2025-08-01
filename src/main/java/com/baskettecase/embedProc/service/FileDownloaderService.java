package com.baskettecase.embedProc.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.nio.file.Files;
import java.net.URI;

@Service
public class FileDownloaderService {

    private static final Logger logger = LoggerFactory.getLogger(FileDownloaderService.class);
    private final RestTemplate restTemplate;

    public FileDownloaderService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Downloads a file from a URL to a temporary local file.
     * Handles both regular HTTP and WebHDFS URLs.
     * @param fileUrl The URL of the file to download.
     * @return A {@link File} object pointing to the temporary file, or {@code null} on failure.
     */
    public File downloadFileToTemp(String fileUrl) {
        try {
            File tempFile = File.createTempFile("embedproc_", ".txt");
            tempFile.deleteOnExit();
            logger.info("Downloading file to temp: {} -> {}", fileUrl, tempFile.getAbsolutePath());

            String fixedUrl = fixWebHdfsUrl(fileUrl);

            HttpHeaders headers = new HttpHeaders();
            if (isWebHdfsUrl(fixedUrl)) {
                headers.set("User-Agent", "embedProc/1.0");
            }
            HttpEntity<String> entity = new HttpEntity<>(headers);
            URI uri = new URI(fixedUrl);

            ResponseEntity<byte[]> response = restTemplate.exchange(uri, HttpMethod.GET, entity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] content = response.getBody();
                Files.write(tempFile.toPath(), content);
                logger.info("Downloaded {} bytes to temp file {}", content.length, tempFile.getAbsolutePath());
                return tempFile;
            } else {
                logger.error("Failed to download file from {}. Status: {}", fileUrl, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error downloading file {} to temp: {}", fileUrl, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Fetches the content of a file from a URL as a String.
     * Handles both regular HTTP and WebHDFS URLs.
     * @param fileUrl The URL of the file to fetch.
     * @return The file content as a String, or {@code null} on failure.
     */
    public String fetchFileContent(String fileUrl) {
        try {
            String fixedUrl = fixWebHdfsUrl(fileUrl);
            URI uri = new URI(fixedUrl);
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return response.getBody();
            } else {
                logger.error("Failed to fetch file content from {}. Status: {}", fileUrl, response.getStatusCode());
                return null;
            }
        } catch (Exception e) {
            logger.error("Error fetching file content from URL {}: {}", fileUrl, e.getMessage(), e);
            return null;
        }
    }

    public String fixWebHdfsUrl(String url) {
        if (!isWebHdfsUrl(url)) return url;
        String baseUrl = url.split("\\?")[0];
        return baseUrl + "?op=OPEN";
    }

    private boolean isWebHdfsUrl(String url) {
        return url != null && url.contains("/webhdfs/");
    }
}
