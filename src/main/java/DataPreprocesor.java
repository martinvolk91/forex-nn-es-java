import java.util.ArrayList;
import java.util.List;

/**
 * Created by martin on 06/04/17.
 */
public class DataPreprocesor {

    public DataPreprocesor() {

    }

    /**
     * This function cleans the data from missing data. It takes an integer parameter nback that determines the
     * minimal number of non-interrupted sequential data to form a new bag. It return List of "bags". Each bag is
     * itself a matrix of non-interrupted sequential data as String
     *
     * @param rawData - raw data as String matrix
     * @param nback   - int number of steps back
     * @return List<List < List < String>>> - bags of matrices of data as String
     */
    public List<List<List<String>>> getCleanedData(List<List<String>> rawData, int nback) {
        List<List<String>> cleanData = new ArrayList<List<String>>();
        List<List<List<String>>> baggedData = new ArrayList<>();
        int nBackCounter = 0;

        for (int i = 1; i < rawData.size(); i++) {
            List<String> prevEntry = rawData.get(i - 1);
            List<String> entry = rawData.get(i);

            if (i > nback) {
                if (nBackCounter > nback) { // add new entry to cleanedData
                    cleanData.add(prevEntry);
                } else if (nBackCounter == nback) { // add past 50 entries to celanedData
                    for (int j = i - nback - 1; j < i; j++) {
                        cleanData.add(rawData.get(j));
                    }
                }
            }
            if (Integer.parseInt(entry.get(0)) - Integer.parseInt(prevEntry.get(0)) <= 60
                    || Integer.parseInt(entry.get(0)) - Integer.parseInt(prevEntry.get(0)) > 2500) {
                nBackCounter++;
            } else {
                nBackCounter = 0;
                if (cleanData.size() > 0) {
                    baggedData.add(cleanData);
                    cleanData = new ArrayList<>();
                }
            }
        }
        return baggedData;
    }

    /**
     * Transforms raw data to numerical data as Double.
     *
     * @param data - bags of matrices of data as String
     * @return - bags of matrices of data as Double
     */
    public List<List<List<Double>>> getNumericData(List<List<List<String>>> data) {
        List<List<List<Double>>> numericData = new ArrayList<>();

        for (List<List<String>> bag : data) {
            List<List<Double>> numericBag = new ArrayList<>();
            for (List<String> entry : bag) {
                List<Double> numericEntry = new ArrayList<>();
                for (String value : entry) {
                    numericEntry.add(Double.parseDouble(value));
                }
                numericBag.add(numericEntry);
            }
            numericData.add(numericBag);
        }
        return numericData;
    }
}
