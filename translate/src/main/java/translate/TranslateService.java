package translate;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;

public class TranslateService {

    private final Configuration configuration;
    private final Credentials credentials;

    public TranslateService(Configuration configuration, Credentials credentials){
        this.credentials = credentials;
        this.configuration = configuration;
    }

    public TranslateResponse translate(TranslateRequest translateRequest, Provider provider, String region) throws Exception {
        TranslateProviderFactoryImpl factory = new TranslateProviderFactoryImpl(configuration, credentials, new Runtime());
        TranslateProvider translateProvider = factory.getProvider(provider, region);
        // invoke the service
        return translateProvider.translate(
                translateRequest.getInputFile(),
                translateRequest.getLanguage()
        );
    }
}
