package io.github.ludovicianul.words.game;

import io.github.ludovicianul.words.game.util.ConsoleUtil;
import org.fusesource.jansi.Ansi;

import javax.inject.Singleton;
import java.security.SecureRandom;
import java.util.*;

import static java.lang.System.in;
import static java.lang.System.out;

@Singleton
public class ThreeWords implements Game {
  public static final char WILD_CHAR = '•';
  private static final int NO_OF_WORDS = 3;
  private static final int CHARS_TO_REMOVE = 2;
  private final List<String> randomWords = new ArrayList<>();
  private final List<String> obfuscatedWords = new ArrayList<>();
  private final List<Character> removedChars = new ArrayList<>();
  private final List<List<Character>> allPermutations = new ArrayList<>();
  private final List<Integer> generatedIndexes = new ArrayList<>();
  private final List<List<String>> allWordsCombinations = new ArrayList<>();
  private List<Character> shuffledRemovedChars = new ArrayList<>();
  private GameContext gameContext;

  private void generateWords() {
    SecureRandom random = new SecureRandom();
    for (int i = 0; i < NO_OF_WORDS; i++) {
      randomWords.add(gameContext.getWords().get(random.nextInt(gameContext.getWords().size())));
    }
  }

  private void removeChars() {
    SecureRandom random = new SecureRandom();
    for (String word : randomWords) {
      for (int i = 0; i < CHARS_TO_REMOVE; i++) {
        int currentIndex = random.nextInt(gameContext.getSelectedWord().length());
        while (word.charAt(currentIndex) == WILD_CHAR) {
          currentIndex = random.nextInt(gameContext.getSelectedWord().length());
        }
        generatedIndexes.add(currentIndex);
        removedChars.add(word.charAt(currentIndex));
        word = this.replaceChar(word, currentIndex, WILD_CHAR);
      }
      obfuscatedWords.add(word);
    }
  }

  private String replaceChar(String inputString, int atIndex, char withChar) {
    StringBuilder builder = new StringBuilder(inputString);
    builder.setCharAt(atIndex, withChar);
    return builder.toString();
  }

  private void printObfuscatedWords() {
    obfuscatedWords.forEach(
        word -> System.out.println(ConsoleUtil.formatString(word, Ansi.Color.WHITE)));
    System.out.println();
  }

  private void createWordCombinations() {
    this.getAllRec(removedChars.size(), new ArrayList<>(removedChars), allPermutations);
    for (List<Character> permutation : allPermutations) {
      List<String> candidate = new ArrayList<>();
      for (int i = 0; i < NO_OF_WORDS; i++) {
        String word = obfuscatedWords.get(i);
        for (int j = 0; j < CHARS_TO_REMOVE; j++) {
          word =
              this.replaceChar(
                  word,
                  generatedIndexes.get(i * CHARS_TO_REMOVE + j),
                  permutation.get(i * CHARS_TO_REMOVE + j));
        }
        if (gameContext.getWords().contains(word)) {
          candidate.add(word);
        }
      }
      if (candidate.size() == NO_OF_WORDS) {
        allWordsCombinations.add(candidate);
      }
    }
  }

  public void getAllRec(int n, List<Character> elements, List<List<Character>> results) {
    if (n == 1) {
      results.add(new ArrayList<>(elements));
    } else {
      for (int i = 0; i < n - 1; i++) {
        this.getAllRec(n - 1, elements, results);
        if (n % 2 == 0) {
          Collections.swap(elements, i, n - 1);
        } else {
          Collections.swap(elements, 0, n - 1);
        }
      }
      this.getAllRec(n - 1, elements, results);
    }
  }

  private void startGame() {
    long startTime = System.currentTimeMillis();
    List<String> guessedAndObfuscated = new ArrayList<>(obfuscatedWords);
    List<String> guessedWords = new ArrayList<>();
    List<Character> guessedChars = new ArrayList<>();
    List<String> candidate = Collections.emptyList();
    Scanner scanner = new Scanner(in);

    while (guessedWords.size() < NO_OF_WORDS) {
      out.println("Enter your guess: ");
      String word = scanner.nextLine().toUpperCase(Locale.ROOT);
      Optional<List<String>> possibleCandidate =
          allWordsCombinations.stream()
              .filter(
                  permutation ->
                      permutation.contains(word) && permutation.containsAll(guessedWords))
              .findFirst();
      if (possibleCandidate.isPresent()) {
        guessedWords.add(word);
        int indexOfWord = possibleCandidate.get().indexOf(word);
        guessedChars.addAll(this.getGuessedChars(indexOfWord));
        guessedAndObfuscated.set(indexOfWord, word);
        candidate = possibleCandidate.get();
      } else {
        out.println("Not a valid combination!");
      }
      printState(guessedAndObfuscated);
      printRemainingLetters(guessedChars);
      out.println();
    }

    finishGame(startTime, candidate);
  }

  private void finishGame(long startTime, List<String> candidate) {
    out.println(
        Ansi.ansi()
            .bold()
            .fgGreen()
            .a("Congrats! You finished in: ")
            .a(((System.currentTimeMillis() - startTime) / 1000))
            .a(" seconds")
            .reset());
    candidate.forEach(
        word -> ConsoleUtil.printSelectedWorDefinition(word, gameContext.getLanguage()));
  }

  public void printRemainingLetters(List<Character> guessedChars) {
    List<Character> remaining = new ArrayList<>(shuffledRemovedChars);
    for (Character character : guessedChars) {
      remaining.remove(character);
    }
    out.println("Remaining letters: " + remaining);
  }

  private void printState(List<String> guessedAndObfuscated) {
    for (String word : guessedAndObfuscated) {
      if (word.indexOf(WILD_CHAR) != -1) {
        out.println(ConsoleUtil.formatString(word, Ansi.Color.WHITE));
      } else {
        out.println(ConsoleUtil.formatString(word, Ansi.Color.GREEN));
      }
    }
  }

  private List<Character> getGuessedChars(int index) {
    List<Character> characters = new ArrayList<>();
    for (int i = 0; i < CHARS_TO_REMOVE; i++) {
      characters.add(removedChars.get(index * CHARS_TO_REMOVE + i));
    }
    return characters;
  }

  private void printRemovedChars() {
    shuffledRemovedChars = new ArrayList<>(removedChars);
    Collections.shuffle(shuffledRemovedChars);
    System.out.println("Available letters: " + shuffledRemovedChars);
  }

  @Override
  public void play(GameContext gameContext) {
    this.gameContext = gameContext;
    generateWords();
    removeChars();
    createWordCombinations();
    printObfuscatedWords();
    printRemovedChars();
    startGame();
  }

  @Override
  public GameType gameType() {
    return GameType.THREE_WORDS;
  }
}
