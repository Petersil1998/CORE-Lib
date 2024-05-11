package sentiment;

import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.language.v1.*;
import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;
import storage.FileInfo;
import storage.Storage;

import java.util.Optional;

public class SentimentAnalyzerGoogle implements SentimentAnalyzer {

    private final Credentials credentials;
    private final Storage storage;
    private final Runtime runtime;
    private final Configuration configuration;
    private String serviceRegion;

    public SentimentAnalyzerGoogle(
            Credentials credentials, Runtime runtime, Storage storage, Configuration configuration) {
        this.credentials = credentials;
        this.storage = storage;
        this.runtime = runtime;
        this.configuration = configuration;
    }

    public SentimentAnalyzerGoogle(
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
        FileInfo fileInfo = FileInfo.parse(inputFile);
        Document.Builder builder = Document.newBuilder()
                .setType(Document.Type.PLAIN_TEXT);

        if(!fileInfo.isLocal() && fileInfo.getBucketInfo().getProvider().equals(Provider.GCP)) {
            String bucket = fileInfo.getBucketInfo().getBucketName();
            String key = fileInfo.getFileName();
            String gcsUrl = "gs://" + bucket + "/" + key;
            builder.setGcsContentUri(gcsUrl);
        } else {
            String text = new String(storage.read(inputFile));
            builder.setContent(text);
        }

        LanguageServiceSettings settings = LanguageServiceSettings.newBuilder()
                .setCredentialsProvider(FixedCredentialsProvider.create(
                        Optional.ofNullable(credentials.getGcpClientCredentials())
                                .orElse(credentials.getGcpCredentials()))).build();

        try (LanguageServiceClient language = LanguageServiceClient.create(settings)) {
            // Detects the sentiment of the text
            AnalyzeSentimentResponse response = language.analyzeSentiment(builder.build());
            Sentiment sentiment = response.getDocumentSentiment();

            System.out.printf("Sentiment: %s, %s%n", sentiment.getScore(), sentiment.getMagnitude());
            return SentimentAnalyzerResponse.builder()
                    .sentimentType(SentimentType.of(sentiment.getScore()))
                    .build();
        }
    }
}
