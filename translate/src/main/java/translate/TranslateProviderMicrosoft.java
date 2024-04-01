package translate;

import com.azure.ai.translation.text.TextTranslationClient;
import com.azure.ai.translation.text.TextTranslationClientBuilder;
import com.azure.ai.translation.text.models.InputTextItem;
import com.azure.ai.translation.text.models.TranslatedTextItem;
import com.google.cloud.translate.v3.TranslateTextResponse;
import com.google.cloud.translate.v3.TranslationServiceClient;
import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;
import storage.Storage;

import java.io.IOException;
import java.util.List;

public class TranslateProviderMicrosoft implements TranslateProvider {
    private static final String ENDPOINT = "https://api.cognitive.microsofttranslator.com";
    private Credentials credentials;
    private Storage storage;
    private Runtime runtime;
    private Configuration configuration;
    private String serviceRegion;

    public TranslateProviderMicrosoft(
            Credentials credentials, Runtime runtime, Storage storage, Configuration configuration) {
        this.credentials = credentials;
        this.storage = storage;
        this.runtime = runtime;
        this.configuration = configuration;
    }

    public TranslateProviderMicrosoft(
            Credentials credentials,
            Runtime runtime,
            Storage storage,
            Configuration configuration,
            String serviceRegion) {
        this(credentials, runtime, storage, configuration);
        this.serviceRegion = serviceRegion;
    }

    @Override
    public TranslateResponse translate(String inputFile, String language) throws Exception {
        // read the input text
        String text = new String(storage.read(inputFile));
        // select region
        selectRegion();
        // translate text
        TextTranslationClient translateClient = getTranslateClient();
        List<TranslatedTextItem> translations = translateClient.translate(List.of(language), List.of(new InputTextItem(text)));
        String translatedText = translations.get(0).getTranslations().get(0).getText();
        // return response
        return TranslateResponse.builder().text(translatedText).build();
    }

    public TextTranslationClient getTranslateClient() throws IOException {
        TextTranslationClientBuilder builder = new TextTranslationClientBuilder()
                .region(serviceRegion)
                .endpoint(ENDPOINT)
                .credential(credentials.getAzureCredentials());
        return builder.buildClient();
    }

    private void selectRegion() {
        if (serviceRegion != null && !serviceRegion.isEmpty()) {
            return;
        }
        Provider functionProvider = runtime.getFunctionProvider();
        String functionRegion = runtime.getFunctionRegion();
        if (Provider.AZURE.equals(functionProvider) && functionRegion != null) {
            // run in function region
            serviceRegion = functionRegion;
        } else {
            // run in default region
            serviceRegion = configuration.getDefaultRegionAzure();
        }
    }
}
