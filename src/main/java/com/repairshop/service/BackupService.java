package com.repairshop.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class BackupService {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${jwt.secret}")
    private String secretKey;

    private static final String ALGORITHM = "AES";

    public byte[] generateBackup() throws Exception {
        String dbName = extractDbName(datasourceUrl);
        String mysqldumpPath = findMysqldumpPath();
        java.io.File tempFile = java.io.File.createTempFile("backup_", ".sql");
        
        try {
            List<String> command = new ArrayList<>();
            command.add(mysqldumpPath);
            command.add("-u" + username);
            command.add("-p" + password);
            command.add("--column-statistics=0");
            command.add(dbName);
            command.add("--result-file=" + tempFile.getAbsolutePath());

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Backup failed with exit code " + exitCode + ". Output: " + output.toString());
            }
            
            byte[] backupData = java.nio.file.Files.readAllBytes(tempFile.toPath());
            return encrypt(backupData);
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    public void restoreBackup(byte[] encryptedData) throws Exception {
        byte[] data = decrypt(encryptedData);
        String dbName = extractDbName(datasourceUrl);
        String mysqlPath = findMysqlPath();
        java.io.File tempFile = java.io.File.createTempFile("restore_", ".sql");
        
        try {
            java.nio.file.Files.write(tempFile.toPath(), data);
            
            List<String> command = new ArrayList<>();
            command.add(mysqlPath);
            command.add("-u" + username);
            command.add("-p" + password);
            command.add(dbName);
            command.add("-e");
            command.add("source " + tempFile.getAbsolutePath().replace("\\", "/"));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Restore failed with exit code " + exitCode + ". Output: " + output.toString());
            }
        } finally {
            if (tempFile.exists()) {
                tempFile.delete();
            }
        }
    }

    private byte[] encrypt(byte[] data) throws Exception {
        SecretKeySpec key = generateKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    private byte[] decrypt(byte[] data) throws Exception {
        SecretKeySpec key = generateKey();
        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, key);
        return cipher.doFinal(data);
    }

    private SecretKeySpec generateKey() throws Exception {
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        byte[] key = secretKey.getBytes(StandardCharsets.UTF_8);
        key = sha.digest(key);
        key = Arrays.copyOf(key, 16); // Use 128 bits for broad compatibility
        return new SecretKeySpec(key, ALGORITHM);
    }

    private String findMysqlPath() {
        if (canExecute("mysql")) return "mysql";
        String[] commonPaths = {
            "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe",
            "C:\\Program Files\\MySQL\\MySQL Server 8.1\\bin\\mysql.exe",
            "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysql.exe",
            "C:\\xampp\\mysql\\bin\\mysql.exe",
            "C:\\wamp\\bin\\mysql\\mysql8.0.21\\bin\\mysql.exe"
        };
        for (String path : commonPaths) {
            if (new java.io.File(path).exists()) return path;
        }
        return "mysql";
    }

    private String findMysqldumpPath() {
        if (canExecute("mysqldump")) return "mysqldump";
        String[] commonPaths = {
            "C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysqldump.exe",
            "C:\\Program Files\\MySQL\\MySQL Server 8.1\\bin\\mysqldump.exe",
            "C:\\Program Files\\MySQL\\MySQL Server 5.7\\bin\\mysqldump.exe",
            "C:\\xampp\\mysql\\bin\\mysqldump.exe",
            "C:\\wamp\\bin\\mysql\\mysql8.0.21\\bin\\mysqldump.exe"
        };
        for (String path : commonPaths) {
            if (new java.io.File(path).exists()) return path;
        }
        return "mysqldump";
    }

    private boolean canExecute(String command) {
        try {
            Process p = new ProcessBuilder(command, "--version").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private String extractDbName(String url) {
        String cleanUrl = url;
        int questionMarkIndex = url.indexOf("?");
        if (questionMarkIndex != -1) {
            cleanUrl = url.substring(0, questionMarkIndex);
        }
        int slashIndex = cleanUrl.lastIndexOf("/");
        return cleanUrl.substring(slashIndex + 1);
    }
}
