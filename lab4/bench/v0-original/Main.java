/*
  Original program (v0), wrapped in a class and given imports so it compiles on JDK 17+.
  The logic is unchanged. It always reads src/txt/harry.txt.
*/
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class Main {
public static void main(String[] args) throws IOException {

        LocalDateTime start = LocalDateTime.now();

        String content = new String(Files.readAllBytes(Paths.get("src/txt/harry.txt")));
        int size = content.length();
        content = content.replaceAll("[^A-Za-z ]"," ").toLowerCase(Locale.ROOT);

        String[] words = content.split(" +"); // 400 000


        Arrays.sort(words);

        String distinctString = " ";

        for (int i = 0; i < words.length ; i++) {
        if (!distinctString.contains(words[i])){
        distinctString+= words[i] + " ";
        }
        }

        String[] distincts = distinctString.split(" ");
        int[] freq = new int[distincts.length];

        for (int i = 0; i < distincts.length ; i++) {
        int count = 0;
        for (int j = 0; j < words.length ; j++) {
        if (distincts[i].equals(words[j])) {
        count++;
        }
        }
        freq[i] = count;
        }

        for (int i = 0; i < distincts.length ; i++) { // 5 000
        distincts[i] += " " + freq[i];
        }

        Arrays.sort(distincts, Comparator.comparing(str
        -> Integer.valueOf(str.replaceAll("[^0-9]", ""))));

        for (int i = 0; i < 30; i++) {
        System.out.println(distincts[distincts.length - 1 - i]);
        }
        LocalDateTime finish = LocalDateTime.now();

        System.out.println("------");
        System.out.println(ChronoUnit.MILLIS.between(start, finish));


        }
}
