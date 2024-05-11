package sentiment;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;

public class SentimentAnalyzerService {

    private final Configuration configuration;
    private final Credentials credentials;

    public SentimentAnalyzerService(Configuration configuration, Credentials credentials) {
        this.credentials = credentials;
        this.configuration = configuration;
    }

    private SentimentAnalyzerResponse detectSentiment(SentimentAnalyzerRequest request, SentimentAnalyzer provider) throws Exception {
        return provider.detectSentiment(request.getInputFile(), request.getLanguageCode());
    }

    public SentimentAnalyzerResponse detectSentiment(SentimentAnalyzerRequest request, Provider provider) throws Exception {
        SentimentAnalyzerFactory factory = new SentimentAnalyzerFactoryImpl(configuration, credentials, new Runtime());
        return detectSentiment(request, factory.getProvider(provider));
    }

    public SentimentAnalyzerResponse detectSentiment(SentimentAnalyzerRequest request, Provider provider, String serviceRegion) throws Exception {
        SentimentAnalyzerFactory factory = new SentimentAnalyzerFactoryImpl(configuration, credentials, new Runtime());
        return detectSentiment(request, factory.getProvider(provider, serviceRegion));
    }
}
