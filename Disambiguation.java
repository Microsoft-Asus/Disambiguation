import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.*;


/**
 * Skeleton class to perform disambiguation
 *
 * @author Jonathan Lajus
 */
public class Disambiguation {
    private static String[] stopWordsofwordnet = {"without", "see", "unless", "due", "also", "must", "might", "like", "]", "[", "}", "{", "<", ">", "?", "\"", "\\", "/", ")", "(", "will", "may", "can", "much", "every", "the", "in", "other", "this", "the", "many", "any", "an", "or", "for", "in", "an", "an ", "is", "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren’t", "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by", "can’t", "cannot", "could", "couldn’t", "did", "didn’t", "do", "does", "doesn’t", "doing", "don’t", "down", "during", "each", "few", "for", "from", "further", "had", "hadn’t", "has", "hasn’t", "have", "haven’t", "having", "he", "he’d", "he’ll", "he’s", "her", "here", "here’s", "hers", "herself", "him", "himself", "his", "how", "how’s", "i ", " i", "i’d", "i’ll", "i’m", "i’ve", "if", "in", "into", "is", "isn’t", "it", "it’s", "its", "itself", "let’s", "me", "more", "most", "mustn’t", "my", "myself", "no", "nor", "not", "of", "off", "on", "once", "only", "ought", "our", "ours", "ourselves", "out", "over", "own", "same", "shan’t", "she", "she’d", "she’ll", "she’s", "should", "shouldn’t", "so", "some", "such", "than", "that", "that’s", "their", "theirs", "them", "themselves", "then", "there", "there’s", "these", "they", "they’d", "they’ll", "they’re", "they’ve", "this", "those", "through", "to", "too", "under", "until", "up", "very", "was", "wasn’t", "we", "we’d", "we’ll", "we’re", "we’ve", "were", "weren’t", "what", "what’s", "when", "when’s", "where", "where’s", "which", "while", "who", "who’s", "whom", "why", "why’s", "with", "won’t", "would", "wouldn’t", "you", "you’d", "you’ll", "you’re", "you’ve", "your", "yours", "yourself", "yourselves", "Without", "See", "Unless", "Due", "Also", "Must", "Might", "Like", "Will", "May", "Can", "Much", "Every", "The", "In", "Other", "This", "The", "Many", "Any", "An", "Or", "For", "In", "An", "An ", "Is", "A", "About", "Above", "After", "Again", "Against", "All", "Am", "An", "And", "Any", "Are", "Aren’t", "As", "At", "Be", "Because", "Been", "Before", "Being", "Below", "Between", "Both", "But", "By", "Can’t", "Cannot", "Could", "Couldn’t", "Did", "Didn’t", "Do", "Does", "Doesn’t", "Doing", "Don’t", "Down", "During", "Each", "Few", "For", "From", "Further", "Had", "Hadn’t", "Has", "Hasn’t", "Have", "Haven’t", "Having", "He", "He’d", "He’ll", "He’s", "Her", "Here", "Here’s", "Hers", "Herself", "Him", "Himself", "His", "How", "How’s", "I ", " I", "I’d", "I’ll", "I’m", "I’ve", "If", "In", "Into", "Is", "Isn’t", "It", "It’s", "Its", "Itself", "Let’s", "Me", "More", "Most", "Mustn’t", "My", "Myself", "No", "Nor", "Not", "Of", "Off", "On", "Once", "Only", "Ought", "Our", "Ours", "Ourselves", "Out", "Over", "Own", "Same", "Shan’t", "She", "She’d", "She’ll", "She’s", "Should", "Shouldn’t", "So", "Some", "Such", "Than", "That", "That’s", "Their", "Theirs", "Them", "Themselves", "Then", "There", "There’s", "These", "They", "They’d", "They’ll", "They’re", "They’ve", "This", "Those", "Through", "To", "Too", "Under", "Until", "Up", "Very", "Was", "Wasn’t", "We", "We’d", "We’ll", "We’re", "We’ve", "Were", "Weren’t", "What", "What’s", "When", "When’s", "Where", "Where’s", "Which", "While", "Who", "Who’s", "Whom", "Why", "Why’s", "With", "Won’t", "Would", "Wouldn’t", "You", "You’d", "You’ll", "You’re", "You’ve", "Your", "Yours", "Yourself", "Yourselves"};

    public static void main(String[] args) throws IOException {
        if (args.length < 3) {
            System.err.println("usage: Disambiguation <yagoLinks> <yagoLabels> <wikiText>");
            return;
        }
        File dblinks = new File(args[0]);
        File dblabels = new File(args[1]);
        File wiki = new File(args[2]);
        SimpleDatabase db = new SimpleDatabase(dblinks, dblabels);
        try (Parser parser = new Parser(wiki)) {
            try (Writer out = new OutputStreamWriter(new FileOutputStream("results.tsv"), "UTF-8")) {
                while (parser.hasNext()) {
                    Page nextPage = parser.next();
                    String pageTitle = nextPage.title; // "<Clinton_1>"
                    String pageContent = nextPage.content; // "Hillary Clinton was..."
                    String pageLabel = nextPage.label(); // "Clinton"
                    String correspondingYagoEntity = "";
                    try {
                        correspondingYagoEntity = findEntityByPageLabel(db, pageLabel, pageContent);
                        out.write(pageTitle + "\t" + correspondingYagoEntity + "\n");
                    } catch (Exception e) {
                        correspondingYagoEntity = "<" + pageLabel + ">";
                        out.write(pageTitle + "\t" + correspondingYagoEntity + "\n");
                    }
                }
            }
        }
    }

    private static String findEntityByPageLabel(SimpleDatabase db, String pageLabel, String pageContent) {

        Map<String, Integer> entities = new HashMap<String, Integer>();
        initFrameWork(entities, db.reverseLabels.get(pageLabel), pageLabel);
        findRelevantEntityByLinks(db, entities, pageContent);
        int sum = 0;
        for (int i : entities.values())
            sum += i;
        if (sum == 0)
            entities.put("<" + pageLabel + ">", 999);
        return entities.entrySet().stream().max((entry1, entry2) -> entry1.getValue() - entry2.getValue()).get().getKey();
    }

    private static void findRelevantEntityByLinks(SimpleDatabase db, Map<String, Integer> entities, String pageContent) {
        for (String key : entities.keySet()) {
            searchIng(db, key, entities, pageContent);
        }
    }

    private static void searchIng(SimpleDatabase db, String key, Map<String, Integer> entities, String pageContent) {
        pageContent = removeStopWord(pageContent);
        for (String str : db.links.get(key)) {
            int cpt = 0;
            str = removeStopWord(str.replaceAll("_", " "));
            String[] ss = str.replaceAll("[<>]", "").split(" ");
            for (String s : ss) {
                if (s.toLowerCase().replaceAll("s$", "").length() > 2)
                    if (pageContent.toLowerCase().contains(s.toLowerCase()) || (pageContent.toLowerCase().contains(s.toLowerCase().replaceAll("s$", ""))))
                        cpt++;
                    else if (pageContent.toLowerCase().contains(s.toLowerCase()))
                        cpt++;
            }
            if (cpt > entities.get(key))
                entities.put(key, cpt);
        }
    }

    private static String removeStopWord(String str) {
        ArrayList<String> wordsList = new ArrayList<String>();
        str = str.trim().replaceAll("\\s+", " ");
        String[] words = str.split(" ");
        Collections.addAll(wordsList, words);

        for (int i = 0; i < wordsList.size(); i++)
            for (String aStopWordsofwordnet : stopWordsofwordnet)
                if (aStopWordsofwordnet.contains(wordsList.get(i)))
                    wordsList.remove(i);
        str = "";
        for (String s : wordsList)
            str = String.format("%s %s", str, s);
        return str.trim();
    }

    private static void initFrameWork(Map<String, Integer> map, Set<String> strs, String pageLabel) {
        for (String str : strs)
            if (!str.replaceAll("[<>]", "").equals(pageLabel))
                map.putIfAbsent(str, 0);
    }
}
