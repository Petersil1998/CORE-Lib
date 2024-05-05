package storage;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;

import java.io.File;
import java.nio.file.Files;

public class Test {

    public static void main(String[] args) throws Exception {
        Credentials credentials = Credentials.loadDefaultCredentials();
        //com.azure.resourcemanager.storage.fluent.models.StorageAccountInner.fromJson()

        StorageProvider azure = new StorageProviderFactoryImpl(credentials, Configuration.builder().build()).getStorageProvider(Provider.AZURE);

        byte[] data = Files.readAllBytes(new File("D:\\Projects\\IdeaProjects\\CORE-Lib\\synthesis\\output.mp3").toPath());

        String region = azure.getRegion("https://uibkcore.blob.core.windows.net/peter-test-container");
        azure.write(data, "https://uibkcore.blob.core.windows.net/peter-test-container/output.mp3");
    }
}
