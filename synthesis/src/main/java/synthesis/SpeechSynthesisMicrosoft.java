package synthesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.SpeechSynthesizer;
import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;
import software.amazon.awssdk.utils.CollectionUtils;
import storage.Storage;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

public class SpeechSynthesisMicrosoft implements SpeechSynthesis {

    private final Credentials credentials;
    private final Storage storage;
    private final Runtime runtime;
    private final Configuration configuration;
    private String serviceRegion;

    public SpeechSynthesisMicrosoft(
            Credentials credentials, Storage storage, Configuration configuration, Runtime runtime) {
        this.credentials = credentials;
        this.storage = storage;
        this.configuration = configuration;
        this.runtime = runtime;
    }

    public SpeechSynthesisMicrosoft(
            Credentials credentials,
            Storage storage,
            Configuration configuration,
            Runtime runtime,
            String serviceRegion) {
        this(credentials, storage, configuration, runtime);
        this.serviceRegion = serviceRegion;
    }

    @Override
    public SpeechSynthesisResponse synthesizeSpeech(
            String inputFile, String language, TextType textType, Gender gender, AudioFormat audioFormat)
            throws Exception {
        try {
            // select region where to run the service
            selectRegion();
            // read the input text
            String text = new String(storage.read(inputFile));
            // get voice for language and gender
            VoiceAzure voice = getVoice(language, gender.name().toLowerCase());
            // create request

            try(SpeechConfig config = SpeechConfig.fromSubscription(credentials.getAzureCredentials().getKey(), serviceRegion)) {
                config.setSpeechSynthesisOutputFormat(getOutputFormat(audioFormat));
                config.setSpeechSynthesisVoiceName(voice.getShortName());

                SpeechSynthesizer speechSynthesizer = new SpeechSynthesizer(config, null);

                SpeechSynthesisResult result = null;
                long startSynthesis = 0, endSynthesis = 0;
                switch(textType) {
                    case SSML:
                        startSynthesis = System.currentTimeMillis();
                        result = speechSynthesizer.SpeakSsml(text);
                        endSynthesis = System.currentTimeMillis();
                        break;
                    case PLAIN_TEXT:
                        startSynthesis = System.currentTimeMillis();
                        result = speechSynthesizer.SpeakText(text);
                        endSynthesis = System.currentTimeMillis();
                        break;
                }

                if (result.getReason() == ResultReason.SynthesizingAudioCompleted) {
                    return SpeechSynthesisResponse.builder()
                            .provider(Provider.AZURE)
                            .audio(result.getAudioData())
                            .synthesisTime(endSynthesis - startSynthesis)
                            .build();
                } else if (result.getReason() == ResultReason.Canceled) {
                    SpeechSynthesisCancellationDetails cancellation = SpeechSynthesisCancellationDetails.fromResult(result);
                    throw new RuntimeException(String.format("Speech Recognition Request was canceled. Reason: %s. Error Code: %d. Details: %s",
                            cancellation.getReason().getValue(), cancellation.getErrorCode().getValue(), cancellation.getErrorDetails()));
                }
                return null;
            }
        } finally {
            serviceRegion = null;
        }
    }

    private VoiceAzure getVoice(String languageCode, String gender)
            throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        VoiceAzure[] voices =
                mapper.readValue(
                        this.getClass().getResourceAsStream("azure_voices.json"), VoiceAzure[].class);

        for (VoiceAzure voice : voices) {
            if (getLanguageCodeFromLocale(voice.getLocale()).equalsIgnoreCase(languageCode)
                    && voice.getGender().equalsIgnoreCase(gender)) {
                return voice;
            }
        }
        // Check for secondary Locale
        return Arrays.stream(voices).filter(voice -> gender.equalsIgnoreCase(voice.getGender()))
                .filter(voice -> !CollectionUtils.isNullOrEmpty(voice.getSecondaryLocaleList()))
                .filter(voice -> voice.getSecondaryLocaleList().stream().anyMatch(locale ->
                        languageCode.equalsIgnoreCase(getLanguageCodeFromLocale(locale))))
                .findAny()
                .orElse(null);
    }

    private SpeechSynthesisOutputFormat getOutputFormat(AudioFormat audioFormat) {
        switch (audioFormat) {
            case MP3:
                return SpeechSynthesisOutputFormat.Audio24Khz48KBitRateMonoMp3;
            case WEBM:
                return SpeechSynthesisOutputFormat.Webm24Khz16BitMonoOpus;
            case PCM:
                return SpeechSynthesisOutputFormat.Riff48Khz16BitMonoPcm;
            default:
                break;
        }
        return null;
    }

    private String getLanguageCodeFromLocale(String locale) {
        return Locale.forLanguageTag(locale).getLanguage();
    }

    private void selectRegion() {
        if (serviceRegion != null && !serviceRegion.isEmpty()) {
            return;
        }
        Provider functionProvider = runtime.getFunctionProvider();
        String functionRegion = runtime.getFunctionRegion();
        if (Provider.AZURE.equals(functionProvider) && functionRegion != null) {
            // run in function region
            serviceRegion = functionRegion;
        } else {
            // run in default region
            serviceRegion = configuration.getDefaultRegionAzure();
        }
    }
}
