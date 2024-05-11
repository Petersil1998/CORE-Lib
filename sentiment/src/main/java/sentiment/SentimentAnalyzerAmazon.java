package sentiment;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.comprehend.ComprehendClient;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentRequest;
import software.amazon.awssdk.services.comprehend.model.DetectSentimentResponse;
import software.amazon.awssdk.services.comprehend.model.LanguageCode;
import storage.Storage;

public class SentimentAnalyzerAmazon implements SentimentAnalyzer {

    private final Credentials credentials;
    private final Storage storage;
    private final Runtime runtime;
    private final Configuration configuration;
    private String serviceRegion;

    public SentimentAnalyzerAmazon(
            Credentials credentials, Runtime runtime, Storage storage, Configuration configuration) {
        this.credentials = credentials;
        this.storage = storage;
        this.runtime = runtime;
        this.configuration = configuration;
    }

    public SentimentAnalyzerAmazon(
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

        String serviceRegion = selectRegion();

        String text = new String(storage.read(inputFile));
        try(ComprehendClient client = ComprehendClient.builder()
                .region(Region.of(serviceRegion))
                .credentialsProvider(credentials.getAwsCredentials())
                .build()) {

            DetectSentimentRequest request = DetectSentimentRequest.builder()
                    .text(text)
                    .languageCode(LanguageCode.EN)
                    .build();

            DetectSentimentResponse response = client.detectSentiment(request);
            System.out.println(response);
            return SentimentAnalyzerResponse.builder()
                    .sentimentType(SentimentType.of(response.sentiment()))
                    .build();
        }
    }

    private String selectRegion() {
        if (serviceRegion != null && !serviceRegion.isEmpty()) {
            return serviceRegion;
        }
        Provider functionProvider = runtime.getFunctionProvider();
        String functionRegion = runtime.getFunctionRegion();
        if (Provider.AWS.equals(functionProvider) && functionRegion != null) {
            // run in function region
            return functionRegion;
        }
        // run in default region
        return configuration.getDefaultRegionAws();
    }
}
