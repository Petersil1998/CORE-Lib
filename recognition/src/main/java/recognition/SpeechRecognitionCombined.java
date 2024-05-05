package recognition;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;

public class SpeechRecognitionCombined implements SpeechRecognition {

  private final Credentials credentials;
  private final Runtime runtime;
  private final Configuration configuration;

  public SpeechRecognitionCombined(
      Credentials credentials, Runtime runtime, Configuration configuration) {
    this.credentials = credentials;
    this.runtime = runtime;
    this.configuration = configuration;
  }

  @Override
  public SpeechRecognitionResponse recognizeSpeech(
      String inputFile,
      int sampleRate,
      String languageCode,
      int channelCount,
      boolean srtSubtitles,
      boolean vttSubtitles,
      boolean profanityFilter,
      boolean spokenEmoji,
      boolean spokenPunctuation,
      boolean includeSNR)
      throws Exception {
    SpeechRecognitionFactoryImpl factory =
        new SpeechRecognitionFactoryImpl(configuration, credentials, runtime);
    SpeechRecognition amazonSpeechRecognition = factory.getS2TProvider(Provider.AWS);
    SpeechRecognition googleSpeechRecognition = factory.getS2TProvider(Provider.GCP);
    SpeechRecognition microsoftSpeechRecognition = factory.getS2TProvider(Provider.AZURE);
    SpeechRecognitionResponse amazonResult =
        amazonSpeechRecognition.recognizeSpeech(
            inputFile,
            sampleRate,
            languageCode,
            channelCount,
            srtSubtitles,
            vttSubtitles,
            profanityFilter,
            spokenEmoji,
            spokenPunctuation,
            includeSNR);
    SpeechRecognitionResponse googleResult =
        googleSpeechRecognition.recognizeSpeech(
            inputFile,
            sampleRate,
            languageCode,
            channelCount,
            srtSubtitles,
            vttSubtitles,
            profanityFilter,
            spokenEmoji,
            spokenPunctuation,
            includeSNR);
    SpeechRecognitionResponse microsoftResult =
        microsoftSpeechRecognition.recognizeSpeech(
            inputFile,
            sampleRate,
            languageCode,
            channelCount,
            srtSubtitles,
            vttSubtitles,
            profanityFilter,
            spokenEmoji,
            spokenPunctuation,
            includeSNR);

    googleResult.setSrtSubtitles(amazonResult.getSrtSubtitles());
    googleResult.setVttSubtitles(amazonResult.getVttSubtitles());
    googleResult.setSignalToNoiseRatio(microsoftResult.getSignalToNoiseRatio());
    return googleResult;
  }
}
