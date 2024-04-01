package synthesis;

import lombok.*;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class VoiceAzure {
    private String name;
    private String displayName;
    private String localName;
    private String shortName;
    private String gender;
    private String locale;
    private String localeName;
    private List<String> secondaryLocaleList;
    private List<String> styleList;
    private String sampleRateHertz;
    private String voiceType;
    private String status;
    private List<String> rolePlayList;
    private String wordsPerMinute;
}
