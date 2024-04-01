package synthesis;

import shared.Configuration;
import shared.Credentials;
import shared.Provider;
import storage.Storage;
import storage.StorageImpl;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

public class Test {

    public static void main(String[] args) throws Exception {
        Credentials credentials = Credentials.loadDefaultCredentials();
        Configuration configuration = Configuration.builder().build();
        SpeechSynthesizer speechSynthesizer = new SpeechSynthesizer(configuration, credentials);

        SpeechSynthesisRequest request = SpeechSynthesisRequest.builder()
                .gender(Gender.MALE)
                .language(Locale.ENGLISH.toLanguageTag())
                .inputFile("synthesis\\input.txt")
                .textType(TextType.PLAIN_TEXT)
                .build();

        SpeechSynthesisResponse response = speechSynthesizer.synthesizeSpeech(request, Provider.AZURE, "germanywestcentral");

        byte[] wav = pcmToWav(response.getAudio(), 48000, 16, 1);

        Storage storage = new StorageImpl(credentials);
        storage.write(wav, "synthesis\\output.wav");
    }

    private static byte[] pcmToWav(byte[] data, int sampleRate, int sampleSizeInBits, int channelCount)
            throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        javax.sound.sampled.AudioFormat format =
                new AudioFormat(sampleRate, sampleSizeInBits, channelCount, true, false);
        AudioSystem.write(
                new AudioInputStream(new ByteArrayInputStream(data), format, data.length),
                AudioFileFormat.Type.WAVE,
                out);
        return out.toByteArray();
    }
}
