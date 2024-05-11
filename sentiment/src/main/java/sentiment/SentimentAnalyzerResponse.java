package sentiment;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class SentimentAnalyzerResponse {
    private SentimentType sentimentType;
}
