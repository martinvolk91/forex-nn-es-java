import java.lang.reflect.Array;
import java.util.*;

/**
 * Created by martin on 06/04/17.
 */
public class IndicatorCalculator {

    public IndicatorCalculator() {

    }

    /**
     * Calculates Simple Moving Average of a closing price with a certain period.
     *
     * @param data
     * @param period
     */
    public List<List<Double>> SMA(List<List<Double>> data, int period, int parameter) {
        List<List<Double>> newData = new ArrayList<>();

        for (int i = 0; i < period; i++) {
            List<Double> doubleList = new ArrayList<>(data.get(i));
            doubleList.add(null);
            newData.add(doubleList);
        }

        for (int i = period - 1; i < data.size(); i++) {
            List<Double> entry = data.get(i);
            List<Double> outputEntry = new ArrayList<>(entry);

            Double sum = new Double(0);

            for (int j = i - period + 1; j <= i; j++) {
                sum += data.get(j).get(parameter);
            }
            Double average = sum / period;
            outputEntry.add(average);
            newData.add(outputEntry);
        }
        for (int i = 0; i < period + 1; i++) {
            newData.remove(0);
        }
        return newData;
    }

    public List<List<Double>> stohasticOscilatorK(List<List<Double>> data, int period, int high, int low, int closeP) {
        List<List<Double>> newData = new ArrayList<>();

        Queue<Double> minLowBuffer = new ArrayDeque<>();
        Queue<Double> maxHighBuffer = new ArrayDeque<>();
        for (int i = 0; i < data.size(); i++) {
            List<Double> entry = new ArrayList<>(data.get(i));

            maxHighBuffer.add(entry.get(high));
            minLowBuffer.add(entry.get(low));

            if (minLowBuffer.size() > period) {
                Double minLow = Collections.min(minLowBuffer);
                Double maxHigh = Collections.max(maxHighBuffer);
                Double close = entry.get(closeP);
                Double k = ((close - minLow) / (maxHigh - minLow)) * 100;
                entry.add(k);
                newData.add(entry);
            } else {
                entry.add(null);
                newData.add(entry);
            }

            // remove first
            if (minLowBuffer.size() > period) {
                minLowBuffer.poll();
            }
            if (maxHighBuffer.size() > period) {
                maxHighBuffer.poll();
            }
        }

        for (int i = 0; i < period + 1; i++) {
            newData.remove(0);
        }
        return newData;
    }


    /**
     * Calculates the Slow K of stohastics oscilator.
     *
     * @param data
     * @param period
     */
    public List<List<Double>> stohasticOscilatorSlowK(List<List<Double>> data, int period, int fastKParameterPosition) {
        List<List<Double>> newData = SMA(data, period, fastKParameterPosition);
        for (int i = 0; i < period + 1; i++) {
            newData.remove(0);
        }
        return newData;
    }

    /**
     * Calculates the Slow D of stohastics oscilator.
     *
     * @param data
     * @param period
     */
    public List<List<Double>> stohasticOscilatorSlowD(List<List<Double>> data, int period, int slowKParameterPosition) {
        List<List<Double>> newData = SMA(data, period, slowKParameterPosition);
        for (int i = 0; i < period + 1; i++) {
            newData.remove(0);
        }
        return newData;
    }

    /**
     * Calculates stdDev and return a List of stdDevs
     *
     * @param data
     * @param period
     * @param parameter
     * @return
     */
    private List<List<Double>> stdDev(List<List<Double>> data, int period, int parameter) {
        List<List<Double>> result = new ArrayList<>();

        for (int i = period - 1; i < data.size(); i++) {
            List<Double> entry = new ArrayList<>(data.get(i));
            Double sum = new Double(0);

            for (int j = i - period + 1; j <= i; j++) {
                sum += data.get(j).get(parameter);
            }
            Double average = sum / period;

            Double squareDiff = new Double(0);
            for (int j = i - period + 1; j <= i; j++) {
                squareDiff += Math.abs(data.get(j).get(parameter) - average);
            }
            squareDiff = Math.sqrt(squareDiff);
            Double stdDev = squareDiff / period;

            entry.add(stdDev);
            result.add(entry);
        }
        for (int i = 0; i < period + 1; i++) {
            result.remove(0);
        }
        return result;
    }

    public List<List<Double>> bollingerBandUpper(List<List<Double>> data, int period, int closeP, double stdDevM) {
        List<List<Double>> upperBand = new ArrayList<>();
        List<List<Double>> smaList = SMA(data, period, closeP);
        List<List<Double>> stdDevList = stdDev(smaList, period, closeP);

        for (int i = 0; i < stdDevList.size(); i++) {
            List<Double> outputEntry = new ArrayList<>(data.get(i));
            List<Double> smaEntry = smaList.get(i);
            Double sma = smaEntry.get(smaEntry.size() - 1);
            List<Double> stdEntry = stdDevList.get(i);
            Double stdDev = stdEntry.get(stdEntry.size() - 1);

            if (stdDev.isNaN()) {
                stdDev = 0.0;
            }
            Double uBand = sma + stdDev * stdDevM;
            outputEntry.add(uBand);
            upperBand.add(outputEntry);
        }

        return upperBand;
    }


    /**
     * Calculates lower Bollinger band
     *
     * @param data
     * @param period
     * @param closeP
     * @return
     */
    public List<List<Double>> bollingerBandLower(List<List<Double>> data, int period, int closeP, double stdDevM) {
        List<List<Double>> lowerBand = new ArrayList<>();
        List<List<Double>> smaList = SMA(data, period, closeP);
        List<List<Double>> stdDevList = stdDev(smaList, period, closeP);

        for (int i = 0; i < stdDevList.size(); i++) {
            List<Double> outputEntry = new ArrayList<>(data.get(i));
            List<Double> smaEntry = smaList.get(i);
            Double sma = smaEntry.get(smaEntry.size() - 1);
            List<Double> stdEntry = stdDevList.get(i);
            Double stdDev = stdEntry.get(stdEntry.size() - 1);

            if (stdDev.isNaN()) {
                stdDev = 0.0;
            }
            Double lBand = sma - stdDev * stdDevM;
            outputEntry.add(lBand);
            lowerBand.add(outputEntry);
        }

        return lowerBand;
    }

    /**
     * Calculates |parameter1 - parameter2| and adds the result to the dataset
     *
     * @param data
     * @param parameter1
     * @param parameter2
     * @return
     */
    public List<List<Double>> difference(List<List<Double>> data, int parameter1, int parameter2) {
        List<List<Double>> newData = new ArrayList<>();

        for (List<Double> entry : data) {
            List<Double> newEntry = new ArrayList<>(entry);
            newEntry.add(Math.abs(newEntry.get(parameter1) - newEntry.get(parameter2)));
            newData.add(newEntry);
        }
        return newData;
    }

    public List<List<Double>> EMA(List<List<Double>> data, int period, int EMAparameter, int SMAparameter, int closeP) {
        List<List<Double>> newData = new ArrayList<>();

        double multiplier = 2 / ((double) (period) + 1);

        for (int i = 0; i < data.size(); i++) {
            List<Double> entry = data.get(i);
            List<Double> newEntry = new ArrayList<>(entry);
            if (i == 0) {
                newEntry.add(data.get(i).get(SMAparameter));
            } else {
                newEntry.add(((data.get(i).get(closeP)) - newData.get(i - 1).get(EMAparameter)) *
                        multiplier + newData.get(i - 1).get(EMAparameter));
            }
            newData.add(newEntry);
        }

        return newData;
    }

}