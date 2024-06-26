package recognition;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.ToString;
import org.json.JSONArray;
import org.json.JSONObject;
import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import shared.Runtime;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribe.TranscribeClient;
import software.amazon.awssdk.services.transcribe.model.*;
import storage.BucketInfo;
import storage.FileInfo;
import storage.Storage;

@ToString
public class SpeechRecognitionAmazon implements SpeechRecognition {

  private final Credentials credentials;
  private final Storage storage;
  private final Runtime runtime;
  private final Configuration configuration;
  private BucketInfo tmpInputBucket;
  @Getter private String serviceRegion;

  public SpeechRecognitionAmazon(
      Credentials credentials, Runtime runtime, Storage storage, Configuration configuration) {
    this.credentials = credentials;
    this.storage = storage;
    this.runtime = runtime;
    this.configuration = configuration;
  }

  public SpeechRecognitionAmazon(
      Credentials credentials,
      Runtime runtime,
      Storage storage,
      Configuration configuration,
      String serviceRegion) {
    this(credentials, runtime, storage, configuration);
    this.serviceRegion = serviceRegion;
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
    try {
      // parse input file url
      FileInfo inputFileInfo = FileInfo.parse(inputFile);
      // select region where to run the service
      if (serviceRegion == null) {
        serviceRegion = selectRegion(inputFileInfo);
      }
      // copy the input file to temporary S3 bucket if necessary
      if (inputFileInfo.isLocal()
          || !inputFileInfo.getBucketInfo().getProvider().equals(Provider.AWS)) {
        inputFileInfo = uploadInputToTmpS3Bucket(inputFileInfo);
      }
      // invoke the service
      String jobName = UUID.randomUUID().toString();
      StartTranscriptionJobRequest startTranscriptionJobRequest =
          startTranscriptionJobRequest(
              inputFileInfo,
              jobName,
              sampleRate,
              languageCode,
              getSubtitleFormats(srtSubtitles, vttSubtitles));
      long start = System.currentTimeMillis();
      TranscribeClient transcribeClient = getTranscribeClient();
      transcribeClient.startTranscriptionJob(startTranscriptionJobRequest);
      // wait for the service to finish
      TranscriptionJob transcriptionJob = awaitTermination(jobName, transcribeClient);
      // download the result
      Transcript transcript = transcriptionJob.transcript();
      String transcriptFileUri = transcript.transcriptFileUri();
      String transcriptJsonString = downloadFile(transcriptFileUri);
      long end = System.currentTimeMillis();
      String srtSubtitlesString = null;
      if (srtSubtitles) {
        srtSubtitlesString = downloadSubtitles(SubtitleFormat.SRT, transcriptionJob);
      }
      String vttSubtitlesString = null;
      if (vttSubtitles) {
        vttSubtitlesString = downloadSubtitles(SubtitleFormat.VTT, transcriptionJob);
      }
      // parse response
      SpeechRecognitionResponse response =
          parseResponse(transcriptJsonString, srtSubtitlesString, vttSubtitlesString);
      response.setProvider(Provider.AWS);
      response.setRecognitionTime(end - start);
      transcribeClient.close();
      return response;
    } finally {
      if (tmpInputBucket != null) {
        storage.deleteBucket(Provider.AWS, tmpInputBucket.getBucketName(), serviceRegion);
        serviceRegion = null;
        tmpInputBucket = null;
      }
    }
  }

  private StartTranscriptionJobRequest startTranscriptionJobRequest(
      FileInfo input,
      String jobName,
      int sampleRate,
      String languageCode,
      Collection<SubtitleFormat> subtitleFormats) {
    String bucketName = input.getBucketInfo().getBucketName();
    String fileName = input.getFileName();
    String s3Uri = "s3://" + bucketName + "/" + fileName;
    StartTranscriptionJobRequest.Builder builder =
        StartTranscriptionJobRequest.builder()
            .transcriptionJobName(jobName)
            .media(Media.builder().mediaFileUri(s3Uri).build())
            .languageCode(languageCode) // en-US
            .mediaSampleRateHertz(sampleRate);
    if (subtitleFormats != null && !subtitleFormats.isEmpty()) {
      builder.subtitles(Subtitles.builder().formats(subtitleFormats).outputStartIndex(1).build());
    }
    return builder.build();
  }

  private TranscriptionJob awaitTermination(String jobName, TranscribeClient transcribeClient) {
    while (true) {
      GetTranscriptionJobRequest getTranscriptionJobRequest =
          GetTranscriptionJobRequest.builder().transcriptionJobName(jobName).build();
      GetTranscriptionJobResponse transcriptionJobResponse =
          transcribeClient.getTranscriptionJob(getTranscriptionJobRequest);
      TranscriptionJobStatus status =
          transcriptionJobResponse.transcriptionJob().transcriptionJobStatus();
      if (status.equals(TranscriptionJobStatus.COMPLETED)) {
        return transcriptionJobResponse.transcriptionJob();
      } else if (status.equals(TranscriptionJobStatus.FAILED)) {
        throw new RuntimeException("Transcription failed.");
      }
    }
  }

  private String downloadSubtitles(SubtitleFormat subtitleFormat, TranscriptionJob transcriptionJob)
      throws IOException {
    if (transcriptionJob.subtitles().hasSubtitleFileUris()) {
      List<String> subtitleFileUris = transcriptionJob.subtitles().subtitleFileUris();
      for (String subtitleFileUri : subtitleFileUris) {
        if (subtitleFormat.equals(SubtitleFormat.SRT)
            && subtitleFileUri.contains("/srtSubtitles.srt?")) {
          return downloadFile(subtitleFileUri);
        } else if (subtitleFormat.equals(SubtitleFormat.VTT)
            && subtitleFileUri.contains("/vttSubtitles.vtt?")) {
          return downloadFile(subtitleFileUri);
        }
      }
    }
    return null;
  }

  private String downloadFile(String uri) throws IOException {
    URL url = new URL(uri);
    BufferedInputStream bis = new BufferedInputStream(url.openStream());
    String result = new String(bis.readAllBytes());
    bis.close();
    return result;
  }

  private SpeechRecognitionResponse parseResponse(
      String jsonString, String srtSubtitles, String vttSubtitles) throws IOException {
    JSONObject jsonRoot = new JSONObject(jsonString);
    ArrayList<Word> words = new ArrayList<>();
    JSONObject results = jsonRoot.getJSONObject("results");
    String fullText = results.getJSONArray("transcripts").getJSONObject(0).getString("transcript");
    JSONArray jsonItems = results.getJSONArray("items");
    for (int i = 0; i < jsonItems.length(); i++) {
      JSONObject jsonItem = jsonItems.getJSONObject(i);
      JSONObject firstAlternative = jsonItem.getJSONArray("alternatives").getJSONObject(0);
      String content = firstAlternative.getString("content");
      Word.WordBuilder wordBuilder =
          Word.builder().confidence(firstAlternative.getDouble("confidence")).content(content);
      if (jsonItem.getString("type").equals("pronunciation")) {
        wordBuilder.startTime(jsonItem.optDouble("start_time"));
        wordBuilder.endTime(jsonItem.optDouble("end_time"));
      }
      words.add(wordBuilder.build());
    }
    // create response object
    return SpeechRecognitionResponse.builder()
        .fullTranscript(fullText)
        .words(words)
        .srtSubtitles(srtSubtitles)
        .vttSubtitles(vttSubtitles)
        .build();
  }

  private FileInfo uploadInputToTmpS3Bucket(FileInfo inputFileInfo) throws Exception {
    tmpInputBucket = createTmpS3Bucket();
    String tmpFileName = UUID.randomUUID().toString();
    String tmpFileUrl = tmpInputBucket.getBucketUrl() + tmpFileName;
    byte[] data = storage.read(inputFileInfo.getFileUrl());
    storage.write(data, tmpFileUrl);
    return FileInfo.parse(tmpFileUrl);
  }

  private BucketInfo createTmpS3Bucket() throws Exception {
    String tmpOutputBucketName = UUID.randomUUID().toString();
    storage.createBucket(Provider.AWS, tmpOutputBucketName, serviceRegion);
    String tmpOutputBucketUrl =
        "https://" + tmpOutputBucketName + ".s3." + serviceRegion + ".amazonaws.com/";
    return BucketInfo.parse(tmpOutputBucketUrl);
  }

  private Collection<SubtitleFormat> getSubtitleFormats(
      boolean srtSubtitles, boolean vttSubtitles) {
    Collection<SubtitleFormat> subtitleFormats = new ArrayList<>();
    if (srtSubtitles) {
      subtitleFormats.add(SubtitleFormat.SRT);
    }
    if (vttSubtitles) {
      subtitleFormats.add(SubtitleFormat.VTT);
    }
    return subtitleFormats;
  }

  private String selectRegion(FileInfo inputFileInfo) throws IOException {
    if (!inputFileInfo.isLocal()
        && inputFileInfo.getBucketInfo().getProvider().equals(Provider.AWS)) {
      // run in bucket region
      return storage.getRegion(inputFileInfo.getBucketInfo().getBucketUrl());
    } else if (Provider.AWS.equals(runtime.getFunctionProvider())) {
      // run in function region
      return runtime.getFunctionRegion();
    }
    // run in default region
    return configuration.getDefaultRegionAws();
  }

  private TranscribeClient getTranscribeClient() {
    return TranscribeClient.builder()
        .region(Region.of(serviceRegion))
        .endpointOverride(URI.create("https://transcribe." + serviceRegion + ".amazonaws.com/"))
        .credentialsProvider(credentials.getAwsCredentials())
        .build();
  }
}
