package synthesis;

import java.io.IOException;
import java.util.List;
import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;
import storage.Storage;
import storage.StorageImpl;

public class SpeechSynthesisFactoryImpl implements SpeechSynthesisFactory {

  private final List<AudioFormat> audioFormatsAmazon =
      List.of(AudioFormat.MP3, AudioFormat.PCM, AudioFormat.OGG_VORBIS);
  private final List<AudioFormat> audioFormatsGoogle =
      List.of(
          AudioFormat.MP3,
          AudioFormat.PCM,
          AudioFormat.OGG_OPUS,
          AudioFormat.MULAW,
          AudioFormat.ALAW);
  private final List<AudioFormat> audioFormatsAzure =
      List.of(
          AudioFormat.MP3,
          AudioFormat.PCM,
          AudioFormat.WEBM);

  private final Configuration configuration;
  private final Credentials credentials;
  private final Runtime runtime;
  private final Storage storage;

  public SpeechSynthesisFactoryImpl(
      Configuration configuration, Credentials credentials, Runtime runtime, Storage storage) {
    this.configuration = configuration;
    this.credentials = credentials;
    this.runtime = runtime;
    this.storage = storage;
  }

  /** Select provider explicitly. */
  @Override
  public SpeechSynthesis getT2SProvider(Provider provider) {
    Storage storage = new StorageImpl(credentials);
    Runtime runtime = new Runtime();
    if (provider.equals(Provider.AWS)) {
      return new SpeechSynthesisAmazon(credentials, storage, configuration, runtime);
    }
    if (provider.equals(Provider.GCP)) {
      return new SpeechSynthesisGoogle(credentials, storage, configuration, runtime);
    }
    if (provider.equals(Provider.AZURE)) {
      return new SpeechSynthesisMicrosoft(credentials, storage, configuration, runtime);
    }
    throw new RuntimeException("Provider must not be null!");
  }

  @Override
  public SpeechSynthesis getT2SProvider(Provider provider, String region) throws IOException {
    Storage storage = new StorageImpl(credentials);
    Runtime runtime = new Runtime();
    if (provider.equals(Provider.AWS)) {
      return new SpeechSynthesisAmazon(credentials, storage, configuration, runtime, region);
    }
    if (provider.equals(Provider.GCP)) {
      return new SpeechSynthesisGoogle(credentials, storage, configuration, runtime, region);
    }
    if (provider.equals(Provider.AZURE)) {
      return new SpeechSynthesisMicrosoft(credentials, storage, configuration, runtime, region);
    }
    throw new RuntimeException("Provider must not be null!");
  }

  /** Select provider based on the audio format. */
  @Override
  public SpeechSynthesis getT2SProvider(AudioFormat audioFormat) throws Exception {
    // supported by both providers
    if (audioFormatsAmazon.contains(audioFormat) && audioFormatsGoogle.contains(audioFormat) && audioFormatsAzure.contains(audioFormat)) {
      return getT2SProvider(configuration.getDefaultProvider());
    }
    // only supported by amazon
    if (audioFormatsAmazon.contains(audioFormat)) {
      return getT2SProvider(Provider.AWS);
    }
    // only supported by google
    if (audioFormatsGoogle.contains(audioFormat)) {
      return getT2SProvider(Provider.GCP);
    }
    // only supported by Azure
    if(audioFormatsAzure.contains(audioFormat)) {
      return getT2SProvider(Provider.AZURE);
    }
    throw new RuntimeException("Invalid audio format!");
  }

  /** Select provider based on the function location. */
  @Override
  public SpeechSynthesis getT2SProvider() throws Exception {
    // run on function provider
    Provider functionProvider = runtime.getFunctionProvider();
    if (functionProvider != null) {
      // run on function provider
      return getT2SProvider(functionProvider);
    }
    // run on default provider
    return getT2SProvider(configuration.getDefaultProvider());
  }
}
