package storage;

import com.google.api.gax.paging.Page;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.*;
import shared.Configuration;
import shared.Credentials;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class StorageProviderGoogle implements StorageProvider {

  private final Credentials credentials;
  private final Configuration configuration;

  public StorageProviderGoogle(Credentials credentials, Configuration configuration) {
    this.credentials = credentials;
    this.configuration = configuration;
  }

  @Override
  public byte[] read(String fileUrl) throws Exception {
    FileInfo fileInfo = FileInfo.parse(fileUrl);
    Storage gcs = getGoogleCloudStorage(credentials);
    return gcs.readAllBytes(fileInfo.getBucketInfo().getBucketName(), fileInfo.getFileName());
  }

  @Override
  public void write(byte[] data, String fileUrl) throws Exception {
    FileInfo fileInfo = FileInfo.parse(fileUrl);
    Storage gcs = getGoogleCloudStorage(credentials);
    BlobId blobId = BlobId.of(fileInfo.getBucketInfo().getBucketName(), fileInfo.getFileName());
    BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
    gcs.createFrom(blobInfo, new ByteArrayInputStream(data));
  }

  @Override
  public boolean delete(String fileUrl) {
    FileInfo fileInfo = FileInfo.parse(fileUrl);
    Storage gcs = getGoogleCloudStorage(credentials);
    Blob blob = gcs.get(fileInfo.getBucketInfo().getBucketName(), fileInfo.getFileName());
    if (blob != null) {
      Storage.BlobSourceOption precondition =
          Storage.BlobSourceOption.generationMatch(blob.getGeneration());
      gcs.delete(fileInfo.getBucketInfo().getBucketName(), fileInfo.getFileName(), precondition);
      return true;
    }
    return false;
  }

  @Override
  public String createBucket(String bucketName, String region) {
    Storage gcs = getGoogleCloudStorage(credentials);
    com.google.cloud.storage.BucketInfo gcsBucket =
        com.google.cloud.storage.BucketInfo.newBuilder(bucketName).setLocation(region).build();
    gcs.create(gcsBucket);
    return bucketName;
  }

  @Override
  public String deleteBucket(String bucketName, String region) {
    Storage gcs = getGoogleCloudStorage(credentials);
    // delete all objects in the bucket
    Page<Blob> blobs = gcs.list(bucketName);
    for (Blob blob : blobs.iterateAll()) {
      blob.delete(Blob.BlobSourceOption.generationMatch());
    }
    // delete the bucket itself

    Bucket bucket = gcs.get(bucketName);
    bucket.delete();
    return bucketName;
  }

  @Override
  public String getRegion(String bucketUrl) throws IOException {
    BucketInfo bucketInfo = BucketInfo.parse(bucketUrl);
    Storage gcs = getGoogleCloudStorage(credentials);
    Bucket bucket = gcs.get(bucketInfo.getBucketName());
    String singleRegion = bucket.getLocation().toLowerCase();
    // convert single-region code to multi-region code
    if (singleRegion.startsWith("eu") || singleRegion.startsWith("us")) {
      return singleRegion.substring(0, 2);
    }
    return null;
  }

  @Override
  public List<String> listFiles(String bucketUrl) {
    BucketInfo bucketInfo = BucketInfo.parse(bucketUrl);
    Storage gcs = getGoogleCloudStorage(credentials);
    Page<Blob> blobs = gcs.list(bucketInfo.getBucketName());
    List<String> fileKeys = new ArrayList<>();
    for (Blob blob : blobs.iterateAll()) {
      fileKeys.add(blob.getName());
    }
    return fileKeys;
  }

  /** Create Google Cloud Storage client */
  private Storage getGoogleCloudStorage(Credentials credentials) {
    return StorageOptions.newBuilder()
        .setCredentials(credentials.getGcpCredentials())
        .setProjectId(credentials.getGoogleProjectId())
        .build()
        .getService();
  }

}
