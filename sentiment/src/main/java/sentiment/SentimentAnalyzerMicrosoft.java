package sentiment;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.ai.textanalytics.models.AnalyzeSentimentOptions;
import com.azure.ai.textanalytics.models.DocumentSentiment;
import shared.Configuration;
import shared.Credentials;
import shared.Runtime;
import storage.Storage;

public class SentimentAnalyzerMicrosoft implements SentimentAnalyzer {

    private final Credentials credentials;
    private final Storage storage;
    private final Runtime runtime;
    private final Configuration configuration;
    private String serviceRegion;

    public SentimentAnalyzerMicrosoft(
            Credentials credentials, Runtime runtime, Storage storage, Configuration configuration) {
        this.credentials = credentials;
        this.storage = storage;
        this.runtime = runtime;
        this.configuration = configuration;
    }

    public SentimentAnalyzerMicrosoft(
            Credentials credentials,
            Runtime runtime,
            Storage storage,
            Configuration configuration,
            String serviceRegion) {
        this(credentials, runtime, storage, configuration);
        this.serviceRegion = serviceRegion;
    }

    @Override
    public SentimentAnalyzerResponse detectSentiment(String inputFile, String languageCode) throws Exception {
        String text = new String(storage.read(inputFile));

        TextAnalyticsClient client = new TextAnalyticsClientBuilder()
                .credential(credentials.getAzureCredentials().getSentimentAnalysisCredentials())
                .endpoint(credentials.getAzureCredentials().getSentimentAnalysisEndpoint())
                .buildClient();

        AnalyzeSentimentOptions options = new AnalyzeSentimentOptions()
                .setIncludeOpinionMining(true)
                .setIncludeStatistics(true);

        DocumentSentiment documentSentiment = client.analyzeSentiment(text, languageCode, options);
        System.out.println(documentSentiment);
        return SentimentAnalyzerResponse.builder()
                .sentimentType(SentimentType.of(documentSentiment.getSentiment()))
                .build();
    }
}
