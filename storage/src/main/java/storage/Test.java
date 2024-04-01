package storage;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.specialized.BlockBlobClient;
import shared.Configuration;
import shared.Credentials;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Test {

    public static void main(String[] args) throws Exception {
        Credentials credentials = Credentials.loadDefaultCredentials();
        Configuration configuration = Configuration.builder().build();

        BlobServiceClient storageClient = new BlobServiceClientBuilder()
                .endpoint("https://uibkcore.blob.core.windows.net")
                .credential(credentials.getAzureSasCredential())
                .buildClient();

        /*
         * This example shows several common operations just to get you started.
         */

        /*
         * Create a client that references a to-be-created container in your Azure Storage account. This returns a
         * ContainerClient object that wraps the container's endpoint, credential and a request pipeline (inherited from storageClient).
         * Note that container names require lowercase.
         */
        BlobContainerClient blobContainerClient = storageClient.getBlobContainerClient("myjavacontainerbasic" + System.currentTimeMillis());

        /*
         * Create a container in Storage blob account.
         */
        blobContainerClient.create();

        /*
         * Create a client that references a to-be-created blob in your Azure Storage account's container.
         * This returns a BlockBlobClient object that wraps the blob's endpoint, credential and a request pipeline
         * (inherited from containerClient). Note that blob names can be mixed case.
         */
        BlockBlobClient blobClient = blobContainerClient.getBlobClient("HelloWorld.txt").getBlockBlobClient();

        String data = "Hello world!";
        InputStream dataStream = new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8));

        /*
         * Create the blob with string (plain text) content.
         */
        blobClient.upload(dataStream, data.length());

        dataStream.close();
    }
}
