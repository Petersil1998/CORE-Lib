package recognition;

import java.io.IOException;
import shared.*;
import shared.Runtime;
import storage.FileInfo;
import storage.Storage;
import storage.StorageImpl;

public class SpeechRecognitionFactoryImpl implements SpeechRecognitionFactory {

  private final Configuration configuration;
  private final Credentials credentials;
  private final Runtime runtime;

  public SpeechRecognitionFactoryImpl(
      Configuration configuration, Credentials credentials, Runtime runtime) {
    this.configuration = configuration;
    this.credentials = credentials;
    this.runtime = runtime;
  }

  @Override
  public SpeechRecognition getS2TProvider(String inputFile)
      throws IOException {
    // parse input file
    FileInfo input = FileInfo.parse(inputFile);
    if (input.isLocal()) {
      // run on the function provider
      Provider functionProvider = runtime.getFunctionProvider();
      if (functionProvider != null) {
        return getS2TProvider(functionProvider);
      }
      // run on the default provider
      return getS2TProvider(configuration.getDefaultProvider());
    }
    // run on the input bucket provider
    Provider bucketProvider = input.getBucketInfo().getProvider();
    return getS2TProvider(bucketProvider);
  }

  @Override
  public SpeechRecognition getS2TProvider(SpeechRecognitionFeatures speechRecognitionFeatures)
      throws IOException {
    // run on amazon
    if (hasAmazonFeatures(speechRecognitionFeatures)
        && !hasGoogleFeatures(speechRecognitionFeatures)
        && !hasMicrosoftFeatures(speechRecognitionFeatures)) {
      return getS2TProvider(Provider.AWS);
    }
    // run on google
    if (!hasAmazonFeatures(speechRecognitionFeatures)
        && hasGoogleFeatures(speechRecognitionFeatures)
        && !hasMicrosoftFeatures(speechRecognitionFeatures)) {
      return getS2TProvider(Provider.GCP);
    }

    // run on Microsoft
    if (!hasAmazonFeatures(speechRecognitionFeatures)
        && !hasGoogleFeatures(speechRecognitionFeatures)
        && hasMicrosoftFeatures(speechRecognitionFeatures)) {
      return getS2TProvider(Provider.AZURE);
    }

    // run on all providers
    if (hasAmazonFeatures(speechRecognitionFeatures)
        && hasGoogleFeatures(speechRecognitionFeatures)
        && hasMicrosoftFeatures(speechRecognitionFeatures)) {
      return new SpeechRecognitionCombined(credentials, new Runtime(), configuration);
    }
    return getS2TProvider(configuration.getDefaultProvider());
  }

  @Override
  public SpeechRecognition getS2TProvider(Provider provider) throws IOException {
    Storage storage = new StorageImpl(credentials);
    Runtime runtime = new Runtime();
    if (provider.equals(Provider.AWS)) {
      return new SpeechRecognitionAmazon(credentials, runtime, storage, configuration);
    }
    if (provider.equals(Provider.GCP)) {
      return new SpeechRecognitionGoogle(credentials, runtime, storage, configuration);
    }
    if (provider.equals(Provider.AZURE)) {
      return new SpeechRecognitionMicrosoft(credentials, runtime, storage, configuration);
    }
    throw new RuntimeException("Failed to initialize S2T provider.");
  }

  @Override
  public SpeechRecognition getS2TProvider(Provider provider, String region) {
    Storage storage = new StorageImpl(credentials);
    Runtime runtime = new Runtime();
    if (provider.equals(Provider.AWS)) {
      return new SpeechRecognitionAmazon(credentials, runtime, storage, configuration, region);
    }
    if (provider.equals(Provider.GCP)) {
      return new SpeechRecognitionGoogle(credentials, runtime, storage, configuration, region);
    }
    if (provider.equals(Provider.AZURE)) {
      return new SpeechRecognitionMicrosoft(credentials, runtime, storage, configuration, region);
    }
    throw new RuntimeException("Failed to initialize S2T provider.");
  }

  private boolean hasAmazonFeatures(SpeechRecognitionFeatures speechRecognitionFeatures) {
    return speechRecognitionFeatures.isSrtSubtitles() || speechRecognitionFeatures.isVttSubtitles();
  }

  private boolean hasGoogleFeatures(SpeechRecognitionFeatures speechRecognitionFeatures) {
    return speechRecognitionFeatures.isSpokenPunctuation()
        || speechRecognitionFeatures.isProfanityFilter()
        || speechRecognitionFeatures.isSpokenEmoji();
  }

  private boolean hasMicrosoftFeatures(SpeechRecognitionFeatures speechRecognitionFeatures) {
    return speechRecognitionFeatures.isSpokenPunctuation()
            || speechRecognitionFeatures.isProfanityFilter()
            || speechRecognitionFeatures.isIncludeSNR();
  }
}
