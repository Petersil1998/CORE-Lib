package recognition;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;
import storage.FileInfo;
import storage.Storage;
import storage.StorageImpl;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

public class SpeechRecognitionMicrosoft implements SpeechRecognition {

    public static final String ENV_FFMPEG_LOCATION = "CORE_FFMPEG_LOCATION";
    public static final String ENV_TEMP_FILE_DIR = "CORE_TEMP_FILE_DIR";

    private final Credentials credentials;
    private final Storage storage;
    private final Runtime runtime;
    private final Configuration configuration;
    private String serviceRegion;

    public SpeechRecognitionMicrosoft(
            Credentials credentials, Runtime runtime, Storage storage, Configuration configuration) {
        this.credentials = credentials;
        this.runtime = runtime;
        this.storage = storage;
        this.configuration = configuration;
    }

    public SpeechRecognitionMicrosoft(
            Credentials credentials,
            Runtime runtime,
            Storage storage,
            Configuration configuration,
            String serviceRegion) {
        this(credentials, runtime, storage, configuration);
        this.serviceRegion = serviceRegion;
    }

    @Override
    public SpeechRecognitionResponse recognizeSpeech(String inputFile, int sampleRate, String languageCode, int channelCount, boolean srtSubtitles, boolean vttSubtitles, boolean profanityFilter, boolean spokenEmoji, boolean spokenPunctuation, boolean includeSNR) throws Exception {
        FileInfo inputFileInfo = FileInfo.parse(inputFile);
        String localFileName;
        if(inputFileInfo.isLocal()) {
            localFileName = inputFileInfo.getFileUrl();
        } else {
            localFileName = createLocalTmpFile(inputFileInfo);
        }

        String convertedFileName;
        if(!localFileName.endsWith(".wav")) {
            convertedFileName = convertAudioToWav(localFileName, sampleRate, channelCount);
        } else {
            convertedFileName = localFileName;
        }

        try(AudioConfig audioConfig = AudioConfig.fromWavFileInput(convertedFileName);
            SpeechConfig speechConfig = SpeechConfig.fromSubscription(credentials.getAzureCredentials().getSpeechRecognitionApiKey(), configuration.getDefaultRegionAzure())) {
            speechConfig.setSpeechRecognitionLanguage(languageCode);
            if(spokenPunctuation) {
                speechConfig.enableDictation();
            }
            if(profanityFilter) {
                speechConfig.setProfanity(ProfanityOption.Masked);
            }
            if(includeSNR) {
                speechConfig.setProperty(PropertyId.SpeechServiceResponse_RequestSnr, "true");
            }
            speechConfig.requestWordLevelTimestamps();
            speechConfig.setOutputFormat(OutputFormat.Detailed);

            SpeechRecognizer speechRecognizer = new SpeechRecognizer(speechConfig, audioConfig);

            long startTime = System.currentTimeMillis();
            Future<SpeechRecognitionResult> future = speechRecognizer.recognizeOnceAsync();
            SpeechRecognitionResult result = future.get();
            long endTime = System.currentTimeMillis();

            if (result.getReason() == ResultReason.RecognizedSpeech) {
                JSONObject object = new JSONObject(result.getProperties().getProperty(PropertyId.SpeechServiceResponse_JsonResult));

                return SpeechRecognitionResponse.builder()
                        .provider(Provider.AZURE)
                        .fullTranscript(result.getText())
                        .recognitionTime(endTime - startTime)
                        .words(getWordsFromResponse(object))
                        .signalToNoiseRatio(getSNRFromResponse(object))
                        .build();
            }
            else if (result.getReason() == ResultReason.Canceled) {
                CancellationDetails cancellation = CancellationDetails.fromResult(result);
                throw new RuntimeException(String.format("Speech Recognition Request was canceled. Reason: %s. Error Code: %d. Details: %s",
                        cancellation.getReason().getValue(), cancellation.getErrorCode().getValue(), cancellation.getErrorDetails()));
            }
        }
        return null;
    }

    private String createLocalTmpFile(FileInfo fileInfo) throws Exception {
        byte[] data = new StorageImpl(credentials, configuration).read(fileInfo.getFileUrl());
        File tempDir = new File(System.getenv(ENV_TEMP_FILE_DIR));
        if(!tempDir.exists() || !tempDir.isDirectory()) {
            throw new IllegalArgumentException(String.format("'%s' Environment Variable is not set to an existing directory", ENV_TEMP_FILE_DIR));
        }

        File audioInputFile = File.createTempFile("core-", "-input-" + fileInfo.getFileName(), tempDir);
        Files.write(audioInputFile.toPath(), data);
        return audioInputFile.getAbsolutePath();
    }

    private String convertAudioToWav(String inputFile, int sampleRate, int channelCount) throws Exception {
        FFmpeg ffmpeg = new FFmpeg(System.getenv(ENV_FFMPEG_LOCATION) + File.separator + "ffmpeg.exe");
        FFprobe ffprobe = new FFprobe(System.getenv(ENV_FFMPEG_LOCATION) + File.separator + "ffprobe.exe");

        File tempDir = new File(System.getenv(ENV_TEMP_FILE_DIR));
        if(!tempDir.exists() || !tempDir.isDirectory()) {
            throw new IllegalArgumentException(String.format("'%s' Environment Variable is not set to an existing directory", ENV_TEMP_FILE_DIR));
        }

        File audioOutputFile = File.createTempFile("core-", "-output.wav", tempDir);

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(new File(inputFile).getAbsolutePath())
                .overrideOutputFiles(true)
                .addOutput(audioOutputFile.getAbsolutePath())
                .setFormat("wav")

                .setAudioChannels(channelCount)
                .setAudioSampleRate(sampleRate)
                .setAudioBitRate(32768)

                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);
        executor.createJob(builder).run();
        return audioOutputFile.getAbsolutePath();
    }

    private List<Word> getWordsFromResponse(JSONObject object) {
        List<Word> words = new ArrayList<>();

        JSONArray jsonArray = object.getJSONArray("NBest")
                .getJSONObject(0)
                .getJSONArray("Words");

        for(int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonWord = jsonArray.getJSONObject(i);

            double startTime = jsonWord.getLong("Offset") / 10_000.0;
            double duration = jsonWord.getLong("Duration") / 10_000.0;

            words.add(Word.builder()
                    .content(jsonWord.getString("Word"))
                    .confidence(jsonWord.getDouble("Confidence"))
                    .startTime(startTime)
                    .endTime(startTime + duration)
                    .build());
        }
        return words;
    }

    private Float getSNRFromResponse(JSONObject object) {
        return object.has("SNR") ? object.getFloat("SNR") : null;
    }
}
