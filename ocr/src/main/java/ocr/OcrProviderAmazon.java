package ocr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;
import storage.FileInfo;
import storage.Storage;

public class OcrProviderAmazon implements OcrProvider {

    private final Credentials credentials;
    private final Storage storage;
    private final Runtime runtime;
    private final Configuration configuration;
    private String serviceRegion;

    public OcrProviderAmazon(
            Credentials credentials, Runtime runtime, Storage storage, Configuration configuration) {
        this.credentials = credentials;
        this.storage = storage;
        this.runtime = runtime;
        this.configuration = configuration;
    }

  public OcrProviderAmazon(
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
        FileInfo inputFileInfo = FileInfo.parse(inputFile);
    String automatedServiceRegion;
        Document doc;
        if (!inputFileInfo.isLocal() && Provider.AWS.equals(inputFileInfo.getBucketInfo().getProvider())) {
      automatedServiceRegion = storage.getRegion(inputFileInfo.getBucketInfo().getBucketUrl());
            S3Object s3Object = S3Object.builder().bucket(inputFileInfo.getBucketInfo().getBucketName()).name(inputFileInfo.getFileName()).build();
            doc = Document.builder().s3Object(s3Object).build();
        }else{
      automatedServiceRegion = selectRegion();
            byte[] data = storage.read(inputFile);
            ByteArrayInputStream sourceStream = new ByteArrayInputStream(data);
            SdkBytes sourceBytes = SdkBytes.fromInputStream(sourceStream);
            doc = Document.builder().bytes(sourceBytes).build();
        }
    // invoke service
    serviceRegion =
        (serviceRegion != null && !serviceRegion.isEmpty())
            ? serviceRegion
            : automatedServiceRegion;
        TextractClient textractClient = getTextractClient(serviceRegion);
        DetectDocumentTextRequest detectDocumentTextRequest =
                DetectDocumentTextRequest.builder().document(doc).build();
        DetectDocumentTextResponse response = textractClient.detectDocumentText(detectDocumentTextRequest);
        // parse response
        StringBuilder resultBuilder = new StringBuilder();
        for (Block block : response.blocks()) {
            if (block.blockType() == BlockType.LINE) {
                resultBuilder.append(block.text())
                        .append("\n");
            }
        }
        String text = resultBuilder.toString();
        return OcrResponse.builder().text(text).build();
    }

    private String selectRegion() {
        Provider functionProvider = runtime.getFunctionProvider();
        String functionRegion = runtime.getFunctionRegion();
        if (Provider.AWS.equals(functionProvider) && functionRegion != null) {
            // run in function region
            return functionRegion;
        }
        // run in default region
        return configuration.getDefaultRegionAws();
    }

    /**
     * Create amazon textract client Java SDK V2
     */
    private TextractClient getTextractClient(String region) throws IOException {
        return TextractClient.builder()
                .region(Region.of(region))
                .endpointOverride(URI.create("https://textract." + region + ".amazonaws.com/"))
                .credentialsProvider(credentials.getAwsCredentials())
                .build();
    }

}
