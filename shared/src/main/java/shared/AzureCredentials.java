package shared;

import com.azure.core.credential.AzureKeyCredential;
import com.azure.identity.ClientSecretCredential;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.ToString;

import java.io.IOException;

@Getter
@ToString
public class AzureCredentials {

    private final AzureKeyCredential translationCredentials;
    private final String speechSynthesisApiKey;
    private final ClientSecretCredential storageClientSecretCredentials;
    private final String storageSubscriptionKey;
    private final String storageAccountName;
    private final String storageApiKey;
    private final AzureKeyCredential sentimentAnalysisCredentials;
    private final String sentimentAnalysisEndpoint;
    private final String speechRecognitionApiKey;
    private final String ocrApiKey;
    private final String ocrEndpoint;

    AzureCredentials(String credentialString) throws IOException {
        JsonNode azureRoot = getCredentialsJSONRoot(credentialString);
        this.translationCredentials = getTranslationCredentials(azureRoot);
        this.speechSynthesisApiKey = getSpeechSynthesisApiKey(azureRoot);
        this.storageClientSecretCredentials = getStorageClientSecretCredentials(azureRoot);
        this.storageSubscriptionKey = getStorageSubscriptionKey(azureRoot);
        this.storageAccountName = getStorageAccountName(azureRoot);
        this.storageApiKey = getStorageApiKey(azureRoot);
        this.sentimentAnalysisCredentials = getSentimentAnalysisApiKey(azureRoot);
        this.sentimentAnalysisEndpoint = getSentimentAnalysisEndpoint(azureRoot);
        this.speechRecognitionApiKey = getSpeechRecognitionApiKey(azureRoot);
        this.ocrApiKey = getOcrApiKey(azureRoot);
        this.ocrEndpoint = getOcrEndpoint(azureRoot);
    }

    private AzureKeyCredential getTranslationCredentials(JsonNode jsonObject) {
        return new AzureKeyCredential(jsonObject.get("translation").get("api_key").asText());
    }

    private String getSpeechSynthesisApiKey(JsonNode jsonObject) {
        return jsonObject.get("speech_synthesis").get("api_key").asText();
    }

    private ClientSecretCredential getStorageClientSecretCredentials(JsonNode jsonObject) {
        JsonNode storage = jsonObject.get("storage");
        return new ClientSecretCredentialBuilder()
                .tenantId(storage.get("tenant_id").asText())
                .clientId(storage.get("client_id").asText())
                .clientSecret(storage.get("client_secret").asText())
                .build();
    }

    private String getStorageSubscriptionKey(JsonNode jsonObject) {
        return jsonObject.get("storage").get("subscription_key").asText();
    }

    private String getStorageAccountName(JsonNode jsonObject) {
        return jsonObject.get("storage").get("account_name").asText();
    }

    private String getStorageApiKey(JsonNode jsonObject) {
        return jsonObject.get("storage").get("api_key").asText();
    }

    private AzureKeyCredential getSentimentAnalysisApiKey(JsonNode jsonObject) {
        return new AzureKeyCredential(jsonObject.get("sentiment_analysis").get("api_key").asText());
    }

    private String getSentimentAnalysisEndpoint(JsonNode jsonObject) {
        return jsonObject.get("sentiment_analysis").get("endpoint").asText();
    }

    private String getSpeechRecognitionApiKey(JsonNode jsonObject) {
        return jsonObject.get("speech_recognition").get("api_key").asText();
    }

    private String getOcrApiKey(JsonNode jsonObject) {
        return jsonObject.get("optical_character_recognition").get("api_key").asText();
    }

    private String getOcrEndpoint(JsonNode jsonObject) {
        return jsonObject.get("optical_character_recognition").get("endpoint").asText();
    }

    private JsonNode getCredentialsJSONRoot(String credentialsString)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(credentialsString).get("azure_credentials");
    }
}
