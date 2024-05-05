package storage;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;

public class StorageProviderFactoryImpl implements StorageProviderFactory {

  private final Credentials credentials;
  private final Configuration configuration;

  public StorageProviderFactoryImpl(Credentials credentials, Configuration configuration) {
    this.credentials = credentials;
    this.configuration = configuration;
  }

  @Override
  public StorageProvider getStorageProvider(FileInfo fileInfo) {
    if (!fileInfo.isLocal()) {
      return getStorageProvider(fileInfo.getBucketInfo());
    }
    return null;
  }

  @Override
  public StorageProvider getStorageProvider(BucketInfo bucketInfo) {
    if (bucketInfo.getProvider() != null) {
      return getStorageProvider(bucketInfo.getProvider());
    }
    return null;
  }

  @Override
  public StorageProvider getStorageProvider(Provider provider) {
    if (provider.equals(Provider.AWS)) {
      return new StorageProviderAmazon(credentials, configuration);
    } else if (provider.equals(Provider.GCP)) {
      return new StorageProviderGoogle(credentials, configuration);
    } else if(provider.equals(Provider.AZURE)) {
      return new StorageProviderMicrosoft(credentials, configuration);
    }
    return null;
  }
}
