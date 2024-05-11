package sentiment;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class SentimentAnalyzerRequest {
    private String inputFile;
    private String languageCode;
}
