package shared;

import com.azure.core.management.AzureEnvironment;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Configuration {
  @Builder.Default private Provider defaultProvider = Provider.AWS;
  @Builder.Default private String defaultRegionAws = "us-east-1";
  @Builder.Default private String defaultRegionGcp = "us";
  @Builder.Default private String defaultRegionAzure = "germanywestcentral";
  @Builder.Default private AzureEnvironment defaultAzureEnvironment = AzureEnvironment.AZURE;
}
