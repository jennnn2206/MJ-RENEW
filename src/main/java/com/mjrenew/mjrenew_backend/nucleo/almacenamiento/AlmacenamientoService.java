package com.mjrenew.mjrenew_backend.nucleo.almacenamiento;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Sube archivos a un bucket S3-compatible (MinIO en desarrollo local) y
 * devuelve la URL pública resultante. Las fotos existentes (URLs externas de
 * Unsplash/Google cargadas antes de este servicio) no se tocan: siguen
 * siendo URLs normales y se siguen leyendo igual en toda la app.
 *
 * Si MinIO no está corriendo, la app debe arrancar igual: el intento de
 * conexión queda aislado a @PostConstruct (no detiene el contexto de Spring)
 * y a subirArchivo() (falla solo esa petición, no el arranque).
 */
@Service
public class AlmacenamientoService {

    private static final Logger log = LoggerFactory.getLogger(AlmacenamientoService.class);

    private final S3Client s3Client;
    private final String endpoint;
    private final String bucket;

    public AlmacenamientoService(
            @Value("${minio.endpoint}") String endpoint,
            @Value("${minio.access-key}") String accessKey,
            @Value("${minio.secret-key}") String secretKey,
            @Value("${minio.bucket}") String bucket
    ) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .forcePathStyle(true)
                .build();
    }

    @PostConstruct
    public void inicializarBucket() {
        try {
            try {
                s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            } catch (Exception noExiste) {
                s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            }
            s3Client.putBucketPolicy(PutBucketPolicyRequest.builder()
                    .bucket(bucket)
                    .policy(politicaLecturaPublica())
                    .build());
        } catch (Exception e) {
            log.warn("No se pudo inicializar el bucket de MinIO '{}'. ¿Está corriendo docker-compose? "
                    + "La app sigue arrancando; solo fallarán las subidas de archivos hasta que MinIO esté disponible.", bucket);
        }
    }

    public String subirArchivo(MultipartFile archivo, String carpeta) {

        String nombreArchivo = carpeta + "/" + UUID.randomUUID() + "-" + limpiarNombre(archivo.getOriginalFilename());

        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(nombreArchivo)
                            .contentType(archivo.getContentType())
                            .build(),
                    RequestBody.fromInputStream(archivo.getInputStream(), archivo.getSize())
            );
        } catch (IOException e) {
            throw new RuntimeException("No se pudo subir el archivo a almacenamiento", e);
        }

        return endpoint + "/" + bucket + "/" + nombreArchivo;
    }

    private String limpiarNombre(String nombreOriginal) {
        if (nombreOriginal == null || nombreOriginal.isBlank()) {
            return "archivo";
        }
        return nombreOriginal.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String politicaLecturaPublica() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
    }
}
