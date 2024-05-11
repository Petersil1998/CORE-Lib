package sentiment;

import com.azure.ai.textanalytics.models.TextSentiment;

import java.util.Arrays;

public enum SentimentType {
    POSITIVE,
    NEUTRAL,
    NEGATIVE,
    MIXED;

    private static final float THRESHOLD = 0.5f;

    public static SentimentType of(String sentiment) {
        return Arrays.stream(SentimentType.values())
                .filter(s -> s.name().equalsIgnoreCase(sentiment))
                .findAny()
                .orElse(null);
    }

    // Google Cloud
    public static SentimentType of(float score) {
        if(score >= THRESHOLD) return POSITIVE;
        if(score <= -THRESHOLD) return NEGATIVE;
        return NEUTRAL;
    }

    // Azure
    public static SentimentType of(TextSentiment sentiment) {
        if(sentiment.equals(TextSentiment.POSITIVE)) return POSITIVE;
        if(sentiment.equals(TextSentiment.NEUTRAL)) return NEUTRAL;
        if(sentiment.equals(TextSentiment.NEGATIVE)) return NEGATIVE;
        if(sentiment.equals(TextSentiment.MIXED)) return MIXED;
        return null;
    }

    // AWS
    public static SentimentType of(software.amazon.awssdk.services.comprehend.model.SentimentType sentiment) {
        switch (sentiment) {
            case POSITIVE: return POSITIVE;
            case NEUTRAL: return NEUTRAL;
            case NEGATIVE: return NEGATIVE;
            case MIXED: return MIXED;
            default: return null;
        }
    }
}
