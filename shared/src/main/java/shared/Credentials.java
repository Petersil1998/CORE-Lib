package shared;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.core.credential.AzureSasCredential;
import com.azure.core.credential.TokenCredential;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.GoogleCredentials;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.json.JSONObject;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentials;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

@Getter
@Setter
@ToString
public class Credentials {

  private StaticCredentialsProvider awsCredentials;
  private GoogleCredentials gcpCredentials;
  private String googleProjectId;
  private GoogleCredentials gcpClientCredentials;
  private AzureKeyCredential azureCredentials;
  private String azureStorageAccountName;
  private String azureSubscriptionKey;
  private ClientSecretCredential azureClientSecretCredentials;
  private String azureVisionEndpoint;

  private Credentials(String credentialsString) throws IOException {
    this.awsCredentials = getAwsCredentialsV2(credentialsString);
    this.gcpCredentials = getGoogleServiceCredentials(credentialsString);
    this.googleProjectId = getGoogleProjectId(credentialsString);
    this.gcpClientCredentials = getGoogleClientCredentials(credentialsString);
    this.azureCredentials = getAzureKeyCredentials(credentialsString);
    this.azureStorageAccountName = getAzureStorageAccountName(credentialsString);
    this.azureSubscriptionKey = getAzureSubscriptionKey(credentialsString);
    this.azureClientSecretCredentials = getAzureClientSecretCredentials(credentialsString);
    this.azureVisionEndpoint = getAzureVisionEndpoint(credentialsString);
  }

  public static Credentials loadDefaultCredentials() throws IOException {
    return loadFromResourceFolder("credentials.json");
  }

  public static Credentials loadFromFile(String path) throws IOException {
    String credentialsString = loadCredentialsFromFile(path);
    return new Credentials(credentialsString);
  }

  public static Credentials loadFromResourceFolder(String path) throws IOException {
    try(InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(path)) {
      String credentialsString = new String(in.readAllBytes());
      return new Credentials(credentialsString);
    }
  }

  private static String loadCredentialsFromFile(String credentialsFilePath) throws IOException {
    try(InputStream in = new FileInputStream(credentialsFilePath)) {
      return new String(in.readAllBytes());
    }
  }

  /** Load credentials for AWS Java SDK V2 */
  private StaticCredentialsProvider getAwsCredentialsV2(String credentialsString)
      throws IOException {
    Map<String, String> credentialsMap = getCredentialsMap(credentialsString, "aws_credentials");
    String key = credentialsMap.get("access_key");
    String secret = credentialsMap.get("secret_key");
    String token = credentialsMap.get("token");
    AwsCredentials awsCreds;
    if (token != null && !token.isBlank()) {
      awsCreds = AwsSessionCredentials.create(key, secret, token);
    } else {
      awsCreds = AwsBasicCredentials.create(key, secret);
    }
    return StaticCredentialsProvider.create(awsCreds);
  }

  /**
   * Load google service credentials, which are needed to use default endpoints of the google API.
   */
  private GoogleCredentials getGoogleServiceCredentials(String credentialsString)
      throws IOException {
    InputStream in = getCredentialsStream(credentialsString, "gcp_credentials");
    return GoogleCredentials.fromStream(in);
  }

  private GoogleCredentials getGoogleClientCredentials(String credentialString) throws IOException {
    InputStream in = getCredentialsStream(credentialString, "gcp_client_credentials");
    return GoogleCredentials.fromStream(in);
  }

  private Map<String, String> getCredentialsMap(String credentialsString, String key)
      throws IOException {
    JSONObject jsonRoot = new JSONObject(credentialsString);
    String awsCredentials = jsonRoot.getJSONObject(key).toString();
    ObjectMapper mapper = new ObjectMapper();
    return mapper.readValue(awsCredentials, new TypeReference<>() {});
  }

  private InputStream getCredentialsStream(String credentialsString, String key) {
    JSONObject jsonRoot = new JSONObject(credentialsString);
    String gcpClientCredentials = jsonRoot.getJSONObject(key).toString();
    return new ByteArrayInputStream(gcpClientCredentials.getBytes(StandardCharsets.UTF_8));
  }

  /** Retrieve the google client project id from the google client credentials file. */
  private String getGoogleProjectId(String credentialsString) throws IOException {
    InputStream in = getCredentialsStream(credentialsString, "gcp_credentials");
    ObjectMapper mapper = new ObjectMapper();
    Map<String, String> credentialsMap = mapper.readValue(in, new TypeReference<>() {});
    return credentialsMap.get("project_id");
  }

  private AzureKeyCredential getAzureKeyCredentials(String credentialsString) throws IOException {
    Map<String, String> credentialsMap = getCredentialsMap(credentialsString, "azure_key_credentials");
    return new AzureKeyCredential(credentialsMap.get("api_key"));
  }

  private String getAzureStorageAccountName(String credentialsString) throws IOException {
    Map<String, String> credentialsMap = getCredentialsMap(credentialsString, "azure_key_credentials");
    return credentialsMap.get("storage_account_name");
  }

  private String getAzureSubscriptionKey(String credentialsString) throws IOException {
    Map<String, String> credentialsMap = getCredentialsMap(credentialsString, "azure_key_credentials");
    return credentialsMap.get("subscription_key");
  }

  private ClientSecretCredential getAzureClientSecretCredentials(String credentialsString) throws IOException {
    Map<String, String> credentialsMap = getCredentialsMap(credentialsString, "azure_key_credentials");
    return new ClientSecretCredentialBuilder()
            .tenantId(credentialsMap.get("tenant_id"))
            .clientId(credentialsMap.get("client_id"))
            .clientSecret(credentialsMap.get("client_secret"))
            .build();
  }

  private String getAzureVisionEndpoint(String credentialsString) throws IOException {
    Map<String, String> credentialsMap = getCredentialsMap(credentialsString, "azure_key_credentials");
    return credentialsMap.get("vision_endpoint");
  }
}
