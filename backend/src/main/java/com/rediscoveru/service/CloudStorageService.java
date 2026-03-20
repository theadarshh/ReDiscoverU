package com.rediscoveru.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.util.UUID;

/**
 * CloudStorageService — Cloudflare R2 (S3-compatible) file storage.
 *
 * ── How it works ──────────────────────────────────────────────────
 * Cloudflare R2 is fully S3-compatible. We use AWS SDK v2 pointing at
 * the R2 endpoint instead of AWS.
 *
 * ── Enabled vs Disabled ──────────────────────────────────────────
 * If R2 credentials are NOT set (env vars missing), cloud storage is
 * DISABLED and all uploads fall back to local disk automatically.
 * The app starts and works normally in both modes.
 *
 * ── How to get R2 credentials ─────────────────────────────────────
 * 1. Login to Cloudflare → R2 → Create bucket (e.g. "rediscoveru")
 * 2. R2 → Manage R2 API Tokens → Create API Token
 *    Permissions: Object Read & Write
 * 3. Copy Access Key ID and Secret Access Key
 * 4. In bucket settings → enable Public Access → copy public URL
 *    (looks like: https://pub-xxxxxxxxx.r2.dev)
 * 5. Set env vars on server:
 *    R2_ACCESS_KEY_ID=your-access-key
 *    R2_SECRET_ACCESS_KEY=your-secret-key
 *    R2_BUCKET_NAME=rediscoveru
 *    R2_ACCOUNT_ID=your-cloudflare-account-id
 *    R2_PUBLIC_URL=https://pub-xxxxxxxxx.r2.dev
 *
 * ── URL format ────────────────────────────────────────────────────
 * Uploaded files are public at:
 *   https://pub-xxxxxxxxx.r2.dev/motivation/uuid_filename.jpg
 *   https://pub-xxxxxxxxx.r2.dev/mentors/uuid_photo.jpg
 *   https://pub-xxxxxxxxx.r2.dev/programs/1/uuid_file.pdf
 */
@Service
public class CloudStorageService {

    @Value("${r2.access.key.id:}")           private String accessKeyId;
    @Value("${r2.secret.access.key:}")       private String secretAccessKey;
    @Value("${r2.bucket.name:rediscoveru}")  private String bucketName;
    @Value("${r2.account.id:}")              private String accountId;
    @Value("${r2.public.url:}")              private String publicUrl;

    private S3Client s3;
    private boolean  enabled = false;

    @PostConstruct
    public void init() {
        if (accessKeyId.isBlank() || secretAccessKey.isBlank() || accountId.isBlank()) {
            System.out.println("[CloudStorage] R2 credentials not set — using local disk storage");
            enabled = false;
            return;
        }
        try {
            String endpoint = "https://" + accountId + ".r2.cloudflarestorage.com";
            s3 = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                    .region(Region.of("auto"))
                    .build();
            enabled = true;
            System.out.println("[CloudStorage] Cloudflare R2 enabled — bucket: " + bucketName);
        } catch (Exception e) {
            System.err.println("[CloudStorage] R2 init failed — falling back to local disk: " + e.getMessage());
            enabled = false;
        }
    }

    /**
     * Returns true if R2 is configured and ready.
     * Use this to decide whether to upload to cloud or local disk.
     */
    public boolean isEnabled() { return enabled; }

    /**
     * Upload a file to R2 and return its public URL.
     *
     * @param file   the uploaded file from the HTTP request
     * @param folder the R2 folder/prefix e.g. "motivation", "mentors", "programs/1"
     * @return full public URL like https://pub-xxx.r2.dev/motivation/uuid_name.jpg
     */
    public String upload(MultipartFile file, String folder) throws Exception {
        if (!enabled) throw new IllegalStateException("Cloud storage is not enabled");

        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._-]", "_")
                : "upload.bin";
        String key = folder + "/" + UUID.randomUUID() + "_" + originalName;

        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .build();

        s3.putObject(req, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));

        // Return the public URL
        String base = publicUrl.isBlank()
                ? "https://" + accountId + ".r2.cloudflarestorage.com/" + bucketName
                : publicUrl.replaceAll("/$", "");
        return base + "/" + key;
    }

    /**
     * Delete a file from R2 by its public URL.
     * Silently ignores failures (file may already be gone).
     */
    public void delete(String publicFileUrl) {
        if (!enabled || publicFileUrl == null || publicFileUrl.isBlank()) return;
        try {
            // Extract key from URL: everything after the bucket public URL prefix
            String base = publicUrl.isBlank() ? "" : publicUrl.replaceAll("/$", "");
            String key  = publicFileUrl.replace(base + "/", "");
            if (key.isBlank() || key.equals(publicFileUrl)) return; // couldn't parse
            s3.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
        } catch (Exception e) {
            System.err.println("[CloudStorage] Delete failed for " + publicFileUrl + ": " + e.getMessage());
        }
    }
}
