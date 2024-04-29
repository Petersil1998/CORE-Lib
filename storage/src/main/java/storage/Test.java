package storage;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Test {

    public static void main(String[] args) throws Exception {
        Credentials credentials = Credentials.loadDefaultCredentials();
        //com.azure.resourcemanager.storage.fluent.models.StorageAccountInner.fromJson()

        StorageProvider azure = new StorageProviderFactoryImpl(credentials).getStorageProvider(Provider.AZURE);

        String data = "Hello world!";
        InputStream dataStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        azure.createBucket("peter-test-container", null);
        // TODO: FIX
        String region = azure.getRegion("https://uibkcore.blob.core.windows.net/peter-test-container");

        /*
         * Create the blob with string (plain text) content.
         */
        // blobClient.upload(dataStream, data.length());

        dataStream.close();
    }
}
