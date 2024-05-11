package sentiment;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;
import storage.Storage;
import storage.StorageImpl;

public class SentimentAnalyzerFactoryImpl implements SentimentAnalyzerFactory {

    private final Configuration configuration;
    private final Credentials credentials;
    private final Runtime runtime;

    public SentimentAnalyzerFactoryImpl(
            Configuration configuration, Credentials credentials, Runtime runtime) {
        this.configuration = configuration;
        this.credentials = credentials;
        this.runtime = runtime;
    }

    @Override
    public SentimentAnalyzer getProvider(Provider provider) {
        Storage storage = new StorageImpl(credentials);
        if (provider.equals(Provider.AWS)){
            return new SentimentAnalyzerAmazon(credentials, runtime, storage, configuration);
        }
        if (provider.equals(Provider.GCP)){
            return new SentimentAnalyzerGoogle(credentials, runtime, storage, configuration);
        }
        if(provider.equals(Provider.AZURE)) {
            return new SentimentAnalyzerMicrosoft(credentials, runtime, storage, configuration);
        }
        throw new RuntimeException("Failed to initialize translate provider.");
    }

    @Override
    public SentimentAnalyzer getProvider(Provider provider, String region) {
        Storage storage = new StorageImpl(credentials);
        if (provider.equals(Provider.AWS)) {
            return new SentimentAnalyzerAmazon(credentials, runtime, storage, configuration, region);
        }
        if (provider.equals(Provider.GCP)) {
            return new SentimentAnalyzerGoogle(credentials, runtime, storage, configuration, region);
        }
        if(provider.equals(Provider.AZURE)) {
            return new SentimentAnalyzerMicrosoft(credentials, runtime, storage, configuration, region);
        }
        throw new RuntimeException("Failed to initialize translate provider.");
    }
}
