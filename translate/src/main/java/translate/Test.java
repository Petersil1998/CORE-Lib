package translate;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import storage.Storage;
import storage.StorageImpl;

public class Test {

    public static void main(String[] args) throws Exception {
        Credentials credentials = Credentials.loadDefaultCredentials();
        Configuration configuration = Configuration.builder().build();
        TranslateService translateService = new TranslateService(configuration, credentials);
        TranslateRequest request = TranslateRequest.builder()
                .inputFile("translate\\input.txt")
                .language("en")
                .build();
        TranslateResponse response = translateService.translate(request, Provider.AZURE, "germanywestcentral");
        // write result to output bucket
        Storage storage = new StorageImpl(Credentials.loadDefaultCredentials());
        storage.write(response.getText().getBytes(), "translate\\output.txt");
    }
}
