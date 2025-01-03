package assign.tom.system;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Stream;

public class FileSystem {
    private static String ABSOLUTE_PATH_TXT;
    private static String ABSOLUTE_PATH_JSON;
    private final String NAME_FILE_TXT;
    private final String NAME_FILE_JSON;
    private static final int LIMIT_WORDS = 10;

    public FileSystem(String nameFile, String jsonFile) {
        this.NAME_FILE_TXT = nameFile;
        this.NAME_FILE_JSON = jsonFile;
        ABSOLUTE_PATH_TXT = String.valueOf(directory(nameFile));
        ABSOLUTE_PATH_JSON = String.valueOf(directory(jsonFile));

        if (ABSOLUTE_PATH_TXT == null) {
            throw new NullPointerException("[ERROR] Could not find file: " + nameFile);
        }

        if (ABSOLUTE_PATH_JSON == null) {
            throw new NullPointerException("[ERROR] Could not find file: " + nameFile);
        }

    }

    private Path directory(String namePath) {
        Path pathCurrent = Paths.get(System.getProperty("user.dir"));
        System.out.println(pathCurrent.toAbsolutePath());
        try (Stream<Path> stream = Files.walk(pathCurrent)) {
                return stream.filter(path -> path.getFileName().toString().equals(namePath))
                        .findFirst()
                        .orElse(null);

        }catch (IOException e){
            e.printStackTrace();
        }

        return null;
    }


    private static List<String> readLineTxt() throws IOException {
        return Files.readAllLines(Paths.get(ABSOLUTE_PATH_TXT));
    }


    /*
     Reads all the words in it in txt format
     @ return 10 random words found in the text file
    */
    public static List<String> read() throws IOException {
        List<String> lines = readLineTxt();
        List<String> allWords = new ArrayList<String>();

        if (lines.isEmpty()) {
            System.out.println("[ERROR] Could not find file or words invalid: " + ABSOLUTE_PATH_TXT);
            return null;
        }

        Random random = new Random();
        int start = random.nextInt(1,lines.size() - LIMIT_WORDS - 1);

        for (int i = start; i < lines.size(); i++) {
            String[] words = lines.get(i).split("\\s+");
            for (String word : words) {
                if (!word.isEmpty()) {
                    allWords.add(word);
                    if (allWords.size() == LIMIT_WORDS){
                        return allWords;
                    }
                }
            }
        }
        return allWords;
    }

    public static List<String> readJson() throws IOException {
        List<String> peers = new ArrayList<>();
        String jsonObjectContent = Files.readString(Paths.get(ABSOLUTE_PATH_JSON), StandardCharsets.UTF_8);
        jsonObjectContent = jsonObjectContent.trim();

        if (jsonObjectContent.startsWith("{") && jsonObjectContent.endsWith("}")) {
            int peersIndex = jsonObjectContent.indexOf("\"peers\":");
            if (peersIndex != -1) {
                int arrayStart = jsonObjectContent.indexOf("[", peersIndex);
                int arrayEnd = jsonObjectContent.indexOf("]", arrayStart);
                if (arrayStart != -1 && arrayEnd != -1) {
                    String peersArray = jsonObjectContent.substring(arrayStart + 1, arrayEnd).trim();
                    String[] peerObjects = peersArray.split("},");
                    for (String peerObject : peerObjects) {
                        peerObject = peerObject.replace("{", "").replace("}", "").trim();
                        String[] fields = peerObject.split(",");
                        StringBuilder peerDetails = new StringBuilder();
                        for (String field : fields) {
                            String[] keyValue = field.split(":");
                            if (keyValue.length > 1) {
                                String value = keyValue[1].trim().replaceAll("\"", "");
                                peerDetails.append(value).append(" ");
                            }
                        }
                        peers.add(peerDetails.toString().trim());
                    }
                }
            }
        } else {
            System.out.println("[ERROR] JSON file is not properly formatted: " + ABSOLUTE_PATH_JSON);
        }

        return peers;
    }
}
