package storage;

import com.azure.core.management.profile.AzureProfile;
import com.azure.core.util.BinaryData;
import com.azure.resourcemanager.AzureResourceManager;
import com.azure.resourcemanager.storage.fluent.models.StorageAccountInner;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.specialized.BlockBlobClient;
import com.azure.storage.common.StorageSharedKeyCredential;
import shared.Configuration;
import shared.Credentials;

import java.util.List;
import java.util.stream.Collectors;

public class StorageProviderMicrosoft implements StorageProvider {

    private final Credentials credentials;
    private final Configuration configuration;

    public StorageProviderMicrosoft(Credentials credentials) {
        this(credentials, Configuration.builder().build());
    }

    public StorageProviderMicrosoft(Credentials credentials, Configuration configuration) {
        this.credentials = credentials;
        this.configuration = configuration;
    }

    @Override
    public byte[] read(String fileUrl) {
        FileInfo fileInfo = FileInfo.parse(fileUrl);
        BlobContainerClient client = getBlobStorageClient(credentials, fileInfo.getBucketInfo().getBucketName());
        BlockBlobClient blobClient = client.getBlobClient(fileInfo.getFileName()).getBlockBlobClient();
        return blobClient.downloadContent().toBytes();
    }

    @Override
    public void write(byte[] data, String fileUrl) {
        FileInfo fileInfo = FileInfo.parse(fileUrl);
        BlobContainerClient client = getBlobStorageClient(credentials, fileInfo.getBucketInfo().getBucketName());
        BlockBlobClient blobClient = client.getBlobClient(fileInfo.getFileName()).getBlockBlobClient();
        blobClient.upload(BinaryData.fromBytes(data), true);
    }

    @Override
    public boolean delete(String fileUrl) {
        FileInfo fileInfo = FileInfo.parse(fileUrl);
        BlobContainerClient client = getBlobStorageClient(credentials, fileInfo.getBucketInfo().getBucketName());
        BlockBlobClient blobClient = client.getBlobClient(fileInfo.getFileName()).getBlockBlobClient();
        if (blobClient != null && blobClient.exists()) {
            blobClient.delete();
            return true;
        }
        return false;
    }

    @Override
    public String createBucket(String bucketName, String region) {
        BlobContainerClient client = getBlobStorageClient(credentials, bucketName);
        client.create();
        return bucketName;
    }

    @Override
    public String deleteBucket(String bucketName, String region) {
        BlobContainerClient client = getBlobStorageClient(credentials, bucketName);
        // delete all objects in the bucket
        for(BlobItem blob: client.listBlobs()) {
            client.getBlobClient(blob.getName()).delete();
        }
        // delete the bucket itself
        client.delete();
        return bucketName;
    }

    @Override
    public String getRegion(String bucketUrl) {
        AzureResourceManager manager = AzureResourceManager.authenticate(
                credentials.getAzureClientSecretCredentials(),
                new AzureProfile(configuration.getDefaultAzureEnvironment())
        ).withSubscription(credentials.getAzureSubscriptionKey());
        StorageAccountInner storageAccount = manager.storageAccounts().manager().serviceClient().getStorageAccounts().list().stream()
                .filter(storage -> credentials.getAzureStorageAccountName().equals(storage.name()))
                .findAny()
                .orElse(null);

        if(storageAccount != null)
            return storageAccount.location();
        return null;
    }

    @Override
    public List<String> listFiles(String bucketUrl) {
        BucketInfo bucketInfo = BucketInfo.parse(bucketUrl);
        BlobContainerClient client = getBlobStorageClient(credentials, bucketInfo.getBucketName());
        return client.listBlobs().stream().map(BlobItem::getName).collect(Collectors.toList());
    }

    /** Create Google Cloud Storage client */
    private BlobContainerClient getBlobStorageClient(Credentials credentials, String bucketName) {
        StorageSharedKeyCredential credential = new StorageSharedKeyCredential(credentials.getAzureStorageAccountName(), credentials.getAzureCredentials().getKey());
        return new BlobContainerClientBuilder()
                .endpoint(String.format("https://%s.blob.core.windows.net", credentials.getAzureStorageAccountName()))
                .credential(credential)
                .containerName(bucketName)
                .buildClient();
    }
}
