import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Skeleton for an evaluator
 */
public class Evaluator {

    /**
     * Takes as arguments (1) the gold standard and (2) the output of a program.
     * Prints to the screen one line with the precision
     * and one line with the recall.
     */

    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.err.println("usage: Disambiguation <Gloadstandard-sample> <Result>");
            return;
        }
        File goldFile;
        File resultFile;
        try{
            goldFile = new File(args[0]);
            resultFile = new File(args[1]);
        }catch (Exception e){
            System.err.println("Something goes wrong.");
            return;
        }
        Map<String, Set<String>> sample = new HashMap<>();
        Map<String, Set<String>> results = new HashMap<>();
        SimpleDatabase.load(goldFile, sample, false);
        SimpleDatabase.load(resultFile, results, false);

        double pre = 0.0, rec = 0.0;
        int preNum = 0, numCor = 0;
        int recNum = sample.size();

        for (Map.Entry<String, Set<String>> entry : results.entrySet()) {
            if (sample.containsKey(entry.getKey())) {
                String valueResult = entry.getValue().toArray()[0].toString();
                String valueGold = (sample.get(entry.getKey()).toArray()[0]).toString();
                preNum++;
                if (valueResult.equals(valueGold)) numCor++;
            }
        }
        pre = (double) numCor / preNum;
        rec = (double) numCor / recNum;
        System.out.println("Precision: " + pre + "\n" + "Recall: " + rec + "\n" + "f1:" + (2 * pre * rec) / (pre + rec));
    }
}
