package ocr;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;
import storage.Storage;
import storage.StorageImpl;

public class OcrProviderFactoryImpl implements OcrProviderFactory {

    private final Configuration configuration;
    private final Credentials credentials;
    private final Runtime runtime;

    public OcrProviderFactoryImpl(
            Configuration configuration, Credentials credentials, Runtime runtime) {
        this.configuration = configuration;
        this.credentials = credentials;
        this.runtime = runtime;
    }

    @Override
    public OcrProvider getProvider(Provider provider) {
        Storage storage = new StorageImpl(credentials);
        if (provider.equals(Provider.AWS)) {
            return new OcrProviderAmazon(credentials, runtime, storage, configuration);
        }
        if (provider.equals(Provider.GCP)) {
            return new OcrProviderGoogle(credentials, runtime, storage, configuration);
        }
        if (provider.equals(Provider.AZURE)) {
            return new OcrProviderMicrosoft(credentials, runtime, storage, configuration);
        }
        throw new RuntimeException("Failed to initialize translate provider.");
    }

    @Override
    public OcrProvider getProvider(Provider provider, String serviceRegion) {
        Storage storage = new StorageImpl(credentials);
        if (provider.equals(Provider.AWS)) {
            return new OcrProviderAmazon(credentials, runtime, storage, configuration, serviceRegion);
        }
        if (provider.equals(Provider.GCP)) {
            return new OcrProviderGoogle(credentials, runtime, storage, configuration, serviceRegion);
        }
        if (provider.equals(Provider.AZURE)) {
            return new OcrProviderMicrosoft(credentials, runtime, storage, configuration, serviceRegion);
        }
        throw new RuntimeException("Failed to initialize translate provider.");
    }
}
