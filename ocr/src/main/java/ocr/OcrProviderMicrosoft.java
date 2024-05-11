package ocr;

import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVision;
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionClient;
import com.microsoft.azure.cognitiveservices.vision.computervision.ComputerVisionManager;
import com.microsoft.azure.cognitiveservices.vision.computervision.implementation.ComputerVisionImpl;
import com.microsoft.azure.cognitiveservices.vision.computervision.models.*;
import shared.Configuration;
import shared.Credentials;
import shared.Runtime;
import storage.Storage;

import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

public class OcrProviderMicrosoft implements OcrProvider {

    private final Credentials credentials;
    private final Storage storage;
    private final Runtime runtime;
    private final Configuration configuration;
    private String serviceRegion;

    public OcrProviderMicrosoft(
            Credentials credentials, Runtime runtime, Storage storage, Configuration configuration) {
        this.credentials = credentials;
        this.storage = storage;
        this.runtime = runtime;
        this.configuration = configuration;
    }

    public OcrProviderMicrosoft(
            Credentials credentials,
            Runtime runtime,
            Storage storage,
            Configuration configuration,
            String serviceRegion) {
        this(credentials, runtime, storage, configuration);
        this.serviceRegion = serviceRegion;
    }
    @Override
    public OcrResponse extract(String inputFile) throws Exception {
        byte[] data = storage.read(inputFile);

        ComputerVisionClient client = ComputerVisionManager.authenticate(credentials.getAzureCredentials().getOcrApiKey());
        ComputerVisionImpl vision = (ComputerVisionImpl) client
                .withEndpoint(credentials.getAzureCredentials().getOcrEndpoint())
                .computerVision();

        ReadInStreamHeaders responseHeader = vision
                .readInStreamWithServiceResponseAsync(data, null)
                .toBlocking()
                .single()
                .headers();

        String text = getReadResult(vision, responseHeader.operationLocation());

        return OcrResponse.builder()
                .text(text)
                .build();
    }

    private String extractOperationIdFromOpLocation(String operationLocation) {
        if (operationLocation != null && !operationLocation.isEmpty()) {
            String[] splits = operationLocation.split("/");

            if (splits.length > 0) {
                return splits[splits.length - 1];
            }
        }
        throw new IllegalStateException("Something went wrong: Couldn't extract the operation id from the operation location");
    }

    private String getReadResult(ComputerVision vision, String operationLocation) throws InterruptedException {
        // Extract OperationId from Operation Location
        String operationId = extractOperationIdFromOpLocation(operationLocation);

        boolean pollForResult = true;
        ReadOperationResult readResults = null;

        while (pollForResult) {
            Thread.sleep(500);
            readResults = vision.getReadResult(UUID.fromString(operationId));

            // The results will no longer be null when the service has finished processing the request.
            if (readResults != null) {
                // Get request status
                OperationStatusCodes status = readResults.status();

                if (status == OperationStatusCodes.FAILED || status == OperationStatusCodes.SUCCEEDED) {
                    pollForResult = false;
                }
            }
        }

        // Print read results, page per page
        return readResults.analyzeResult()
                .readResults()
                .stream()
                .map(ReadResult::lines)
                .flatMap(Collection::stream)
                .map(Line::text)
                .collect(Collectors.joining("\n"));
    }
}
