package sentiment;

public interface SentimentAnalyzer {

    SentimentAnalyzerResponse detectSentiment(String inputFile, String languageCode) throws Exception;
}
