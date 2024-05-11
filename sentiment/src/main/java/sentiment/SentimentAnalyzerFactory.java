package sentiment;

import shared.Provider;

public interface SentimentAnalyzerFactory {

    SentimentAnalyzer getProvider(Provider provider);

    SentimentAnalyzer getProvider(Provider provider, String region);
}
