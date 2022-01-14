import org.apache.commons.lang3.ArrayUtils;
import org.apache.log4j.Logger;
import org.jenetics.*;
import org.jenetics.engine.Engine;
import org.jenetics.engine.EvolutionResult;
import org.jenetics.engine.EvolutionStatistics;
import org.neuroph.core.Connection;
import org.neuroph.core.Neuron;
import org.neuroph.core.Weight;
import org.neuroph.nnet.MultiLayerPerceptron;
import org.neuroph.nnet.comp.neuron.BiasNeuron;

import java.io.*;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * Created by martin on 29/04/17.
 */
public class Main {
    final static Logger logger = Logger.getLogger(Main.class.getName());

    static List<List<String>> rawData;


    public static void main(String[] args) throws IOException {
        rawData = readDataFromFile("files/EURUSD_2017.csv", ",");
        rawData.remove(0);

        final int NO_OF_THREADS = 3;

        final Genotype genotype = Genotype.of(
                DoubleChromosome.of(-1.0, 1.0, 224)
        );

        final ExecutorService executorService = Executors.newFixedThreadPool(NO_OF_THREADS);

        final Engine<DoubleGene, Double> engine = Engine.
                builder(Main::fitness, genotype).
                executor(executorService).
                populationSize(100).
                optimize(Optimize.MAXIMUM).
                selector(new TournamentSelector(3)).
                alterers(
                        new Mutator(0.05),
                        new MultiPointCrossover(0.1, 3)
                ).
                build();

        final EvolutionStatistics<Double, ?> statistics = EvolutionStatistics.ofNumber();

        final Phenotype best = engine.stream().
                limit(500).
                peek(statistics).
                collect(EvolutionResult.toBestPhenotype());

        logger.info(statistics);
        logger.info(best);
        System.out.println(statistics);
        System.out.println(best);
    }

    private static Double fitness(final Genotype dgt) {
        List<List<Double>> doubleData = convertListToDouble(rawData);
        doubleData = convertTo5MinData(doubleData);
        doubleData.remove(0);

        //---------------------------------------------------
        IndicatorCalculator indicatorCalculator = new IndicatorCalculator();
        doubleData = indicatorCalculator.SMA(doubleData, 14, 5);
        doubleData = indicatorCalculator.stohasticOscilatorK(doubleData, 21, 3, 4, 5);
        doubleData = indicatorCalculator.stohasticOscilatorSlowK(doubleData, 7, 7);
        doubleData = indicatorCalculator.stohasticOscilatorSlowD(doubleData, 7, 8);

        doubleData = indicatorCalculator.bollingerBandUpper(doubleData, 20, 5, 2);
        doubleData = indicatorCalculator.bollingerBandLower(doubleData, 20, 5, 2);
        // bollinger bands diff
        doubleData = indicatorCalculator.difference(doubleData, 10, 11);

        doubleData = indicatorCalculator.EMA(doubleData, 14, 13, 6, 5);
        //---------------------------------------------------

        // set up neural net
        MultiLayerPerceptron multiLayerPerceptron = new MultiLayerPerceptron(8, 16, 6);
        List<Neuron> inputNeurons = Arrays.asList(multiLayerPerceptron.getInputNeurons());
        List<Neuron> hiddenNeurons = Arrays.asList(multiLayerPerceptron.getLayerAt(1).getNeurons());

        List<Weight> weights = new ArrayList<>();
        for (Neuron n : inputNeurons) {
            for (Connection c : n.getOutConnections()) {
                weights.add(c.getWeight());
            }
        }
        for (Neuron n : hiddenNeurons) {
            if (n instanceof BiasNeuron) {
                continue;
            }
            for (Connection c : n.getOutConnections()) {
                weights.add(c.getWeight());
            }
        }

        int counter = 0;
        for (Weight w : weights) {
            w.setValue((Double) dgt.getChromosome(0).getGene(counter).getAllele());
            counter++;
        }

        // simulation part starts here

        double realBalance = 2000;
        int maxLeverage = 10;
        double spread = 0.0002;
        boolean isLong = false;
        boolean isShort = false;
        double stopLoss = 10;
        double positionSize = 0; // micro lots

        double lotSize = 1000;

        double balance = realBalance * maxLeverage;

        double profitLossLong = 0;
        double profitLossShort = 0;

        double ask = 0;
        double bid = 0;

        double position = 0;

        double longStopLossPrice = 0;
        double shortStopLossPrice = 0;

        int actionCounter0 = 0;
        int actionCounter1 = 0;
        int actionCounter2 = 0;
        int actionCounter3 = 0;
        int actionCounter4 = 0;
        int actionCounter5 = 0;
        int longStopCounter = 0;
        int shortStopCounter = 0;


        int[] actions = new int[6];

        for (List<Double> entry : doubleData) {
            multiLayerPerceptron.setInput(entry.get(6), entry.get(7), entry.get(8), entry.get(9),
                    entry.get(10), entry.get(11), entry.get(12), entry.get(13));

            multiLayerPerceptron.calculate();

            Double[] outputArray = ArrayUtils.toObject(multiLayerPerceptron.getOutput());
            List<Double> output = Arrays.asList(outputArray);
            int action = getMaxIdx(output);

            actions[action]++;

            int dataSize = doubleData.size();

            ask = entry.get(5);
            bid = ask - spread;

            if (isLong) {
                profitLossLong = (positionSize * lotSize * bid) - position;
            }

            if (isShort) {
                profitLossShort = position - (positionSize * lotSize * ask);
            }

            if (!isLong && !isShort && action == 0) { // go long
                actionCounter0++;
                positionSize = 10;
                position = positionSize * lotSize * ask;
                longStopLossPrice = ask - 0.0010;
                isLong = true;
            } else if (isLong && !isShort && action == 1) { // sell (long)
                actionCounter1++;
                positionSize = 0;
                position = positionSize * lotSize * ask;
                realBalance += profitLossLong;
                isLong = false;
                profitLossLong = 0;
            } else if (!isLong && !isShort && action == 2) { // go short
                actionCounter2++;
                positionSize = 10;
                position = positionSize * lotSize * bid;
                shortStopLossPrice = bid + 0.0010;
                isShort = true;
            } else if (!isLong && isShort && action == 3) { // buy (short)
                actionCounter3++;
                positionSize = 0;
                position = positionSize * lotSize * bid;
                realBalance += profitLossShort;
                isShort = false;
                profitLossShort = 0;
            } else if ((isLong || isShort) && action == 4) { // hold postion
                actionCounter4++;
            } else if (!isLong && !isShort && action == 5) { // pass
                actionCounter5++;
            }

            if ((longStopLossPrice >= entry.get(4) && isLong) || (profitLossLong < -20 && isLong)) {
                longStopCounter++;
                realBalance += profitLossLong;
                isLong = false;
                profitLossLong = 0;
            }

            if ((shortStopLossPrice <= entry.get(3) && isShort) || (profitLossShort < -20 && isShort)) {
                shortStopCounter++;
                realBalance += profitLossShort;
                isShort = false;
                profitLossShort = 0;
            }

        }


        if (realBalance > 2000) {
            NumberFormat formatter = new DecimalFormat("#0.00");

//            logger.info(formatter.format(realBalance) + "\t" + actionCounter0 + "\t" + actionCounter1 + "\t" + actionCounter2 +
//                    "\t" + actionCounter3 + "\t" + actionCounter4 + "\t" + actionCounter5 + "\t" +
//                    longStopCounter + "\t" + shortStopCounter + "\t" + dgt);

            System.out.println(formatter.format(realBalance) + "\t|" + actionCounter0 + "\t" + actionCounter1 + "\t" + actionCounter2 +
                    "\t" + actionCounter3 + "\t" + actionCounter4 + "\t" + actionCounter5 + "\t|" +
                    +actions[0] + "\t" + actions[1] + "\t" + actions[2] +
                    "\t" + actions[3] + "\t" + actions[4] + "\t" + actions[5] + "\t|" +
                    longStopCounter + "\t" + shortStopCounter + "\t" + dgt);
        }

//        return (double)(actionCounter0 + actionCounter2 + actionCounter1*2 + actionCounter3*2);

        if (actionCounter1 == 0 || actionCounter3 == 0) {
            return Double.MIN_VALUE;
        }

        return realBalance + actionCounter1 * 20 + actionCounter3 * 20 - longStopCounter * 10 - shortStopCounter * 10;
//        return realBalance;
    }


    //----------------------------
    public static void main1(String[] args) {
        //   0     1     2  3  4  5   6   7   8   9   10  11    12     13
        // Hour, Minute, O, H, L, C, SMA, fK, sK, sD, uB, lB, BBdiff,  EMA
        List<List<String>> rawData = readDataFromFile("files/EURUSD_2017.csv", ",");
        System.out.println("LOG: raw data populated");

        // remove header
        rawData.remove(0);

        // read data as double
        List<List<Double>> doubleData = convertListToDouble(rawData);

        // calculate 5min data
//        doubleData = convertTo5MinData(doubleData);
//        doubleData.remove(0);

        // print original
//        System.out.println("Print original");
//        for (int i = 0; i < 100; i++) {
//            List<Double> doubleList = doubleData.get(i);
//            for (Double d : doubleList) {
//                System.out.print(d + "\t");
//            }
//            System.out.println();
//        }


        IndicatorCalculator indicatorCalculator = new IndicatorCalculator();
        doubleData = indicatorCalculator.SMA(doubleData, 14, 5);
        doubleData = indicatorCalculator.stohasticOscilatorK(doubleData, 14, 3, 4, 5);
        doubleData = indicatorCalculator.stohasticOscilatorSlowK(doubleData, 3, 7);
        doubleData = indicatorCalculator.stohasticOscilatorSlowD(doubleData, 3, 8);

        doubleData = indicatorCalculator.bollingerBandUpper(doubleData, 14, 5, 2);
        doubleData = indicatorCalculator.bollingerBandLower(doubleData, 14, 5, 2);
        // bollinger bands diff
        doubleData = indicatorCalculator.difference(doubleData, 10, 11);

        doubleData = indicatorCalculator.EMA(doubleData, 14, 13, 6, 5);

//        System.out.println();
//        System.out.println("Print new");
//        for (int i = 0; i < 100; i++) {
//            List<Double> doubleList = doubleData.get(i);
//            for (Double d : doubleList) {
//                System.out.print(d + "\t");
//            }
//            System.out.println();
//        }

        // outputs: long, sell, short, buy, pass, hold
        // 8 inputs, 16 hidden, 6 outputs
        MultiLayerPerceptron multiLayerPerceptron = new MultiLayerPerceptron(8, 16, 6);
//        NeuralNetworkType
        List<Neuron> inputNeurons = Arrays.asList(multiLayerPerceptron.getInputNeurons());
        List<Neuron> hiddenNeurons = Arrays.asList(multiLayerPerceptron.getLayerAt(1).getNeurons());
        List<Neuron> outputNeurons = Arrays.asList(multiLayerPerceptron.getOutputNeurons());

        List<Weight> weights = new ArrayList<>();
        for (Neuron n : inputNeurons) {
            for (Connection c : n.getOutConnections()) {
                weights.add(c.getWeight());
            }
        }
        for (Neuron n : hiddenNeurons) {
            if (n instanceof BiasNeuron) {
                continue;
            }
            for (Connection c : n.getOutConnections()) {
                weights.add(c.getWeight());
            }
        }

//        inputNeurons.get(0).getOutConnections()[0].setWeight(new Weight(1.0)); // works

        double realBalance = 2000;
        int maxLeverage = 10;
        double spread = 0.0002;
        boolean isLong = false;
        boolean isShort = false;
        double stopLoss = 10;
        double positionSize = 0; // micro lots

        double lotSize = 1000;

        double balance = realBalance * maxLeverage;

        double profitLossLong = 0;
        double profitLossShort = 0;

        double ask = 0;
        double bid = 0;

        double position = 0;


        int posCounter = 0;

        int[] actions = new int[6];

        for (List<Double> entry : doubleData) {
            multiLayerPerceptron.setInput(entry.get(6), entry.get(7), entry.get(8), entry.get(9),
                    entry.get(10), entry.get(11), entry.get(12), entry.get(13));

            multiLayerPerceptron.calculate();

            Double[] outputArray = ArrayUtils.toObject(multiLayerPerceptron.getOutput());
            List<Double> output = Arrays.asList(outputArray);
            int action = getMaxIdx(output);

            actions[action]++;

            ask = entry.get(5);
            bid = ask - spread;

            if (isLong) {
                profitLossLong = (positionSize * lotSize * bid) - position;
            }

            if (isShort) {
                profitLossShort = position - (positionSize * lotSize * ask);
            }

            if (!isLong && !isShort && action == 0) { // go long
                posCounter++;
                System.out.println("Entering long at: ask=" + ask);
                positionSize = 10;
                position = positionSize * lotSize * ask;
                isLong = true;
            } else if (isLong && !isShort && action == 1) { // sell (long)
                positionSize = 0;
                position = positionSize * lotSize * ask;
                realBalance += profitLossLong;
                isLong = false;
                System.out.println("Exiting long at: bid=" + bid + ", profit=" + profitLossLong + ", new balance=" + realBalance);
                profitLossLong = 0;
            } else if (!isLong && !isShort && action == 2) { // go short
                posCounter++;
                System.out.println("Entering short at: bid=" + bid);
                positionSize = 10;
                position = positionSize * lotSize * bid;
                isShort = true;
            } else if (!isLong && isShort && action == 3) { // buy (short)
                positionSize = 0;
                position = positionSize * lotSize * bid;
                realBalance += profitLossShort;
                isShort = false;
                System.out.println("Exiting short at: ask=" + ask + ", profit=" + profitLossShort + ", new balance=" + realBalance);
                profitLossShort = 0;
            } else if ((isLong || isShort) && action == 4) { // hold postion

            } else if (!isLong && !isShort && action == 5) { // pass

            }

            if (profitLossShort < -20 && isShort) {
                realBalance += profitLossShort;
                isShort = false;
                System.out.println("Stoploss short at: ask=" + ask + ", profit=" + profitLossShort + ", new balance=" + realBalance);
                profitLossShort = 0;
            }
            if (profitLossLong < -20 && isLong) {
                realBalance += profitLossLong;
                isLong = false;
                System.out.println("Stoploss long at: bid=" + bid + ", profit=" + profitLossLong + ", new balance=" + realBalance);
                profitLossLong = 0;
            }

        }

        System.out.println();
        System.out.println("Enter longs: " + actions[0]);
        System.out.println("Exit longs: " + actions[1]);
        System.out.println("Enter shorts: " + actions[2]);
        System.out.println("Exit shorts: " + actions[3]);
        System.out.println("Holds: " + actions[4]);
        System.out.println("Passes: " + actions[5]);


    }

    private static int getMaxIdx(List<Double> list) {
        int idx = 0;
        Double max = Double.MIN_VALUE;
        for (int i = 0; i < list.size(); i++) {
            Double current = list.get(i);
            if (current > max) {
                max = current;
                idx = i;
            }
        }
        return idx;
    }

    private static List<List<Double>> convertListToDouble(List<List<String>> rawData) {
        List<List<Double>> doubleData = new ArrayList<>();

        for (List<String> list : rawData) {
            List<Double> doubleList = new ArrayList<>();
//            list.remove(0);
            for (String s : list) {
                doubleList.add(Double.parseDouble(s));
            }
            doubleData.add(doubleList);
        }
        return doubleData;
    }

    /**
     * Reads data from file and save it to List<List<String>>. Inner list holds single row (OHLC) and outer list holds
     * all rows. Data is saved as String.
     *
     * @param filename - path to the file
     * @return returns List<List<String>> holding all the data
     */
    private static List<List<String>> readDataFromFile(String filename, String delimiter) {
        File file = new File(filename);
        List<List<String>> data = new ArrayList<List<String>>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                List<String> list = new ArrayList<>(Arrays.asList(line.split(delimiter)));
                data.add(list);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return data;
    }

    public static void writeDataToFile(String filename, List<String> header, List<List<String>> data) {
        File file = new File(filename);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            String headerLine = "";

            for (String headerElement : header) {
                headerLine += headerElement + ",";
            }
            headerLine = headerLine.substring(0, headerLine.length() - 1);
            headerLine += "\n";
            bw.write(headerLine);
            for (List<String> entry : data) { // take entry
                String line = "";
                for (String element : entry) { // take element and prepare string to write
                    line += element + ",";
                }
                line = line.substring(0, line.length() - 1);
                line += "\n";

                bw.write(line);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * oneMinData must be in this format:
     * h, m, OHLC, .....
     * Minute should be second position
     *
     * @param oneMinData
     */
    private static List<List<Double>> convertTo5MinData(List<List<Double>> oneMinData) {
        List<List<Double>> fiveMinData = new ArrayList<>();
        double hour = 0;
        double minute = 0;
        double open = 0;
        double high = 0;
        double low = 0;
        double close = 0;

        for (List<Double> entry : oneMinData) {
            if ((int) (entry.get(1).doubleValue()) % 5 == 0) {
                hour = entry.get(0);
                minute = entry.get(1);

                open = entry.get(2);
                high = Double.MIN_VALUE;
                low = Double.MAX_VALUE;
            }

            // set high
            if (high < entry.get(3)) {
                high = entry.get(3);
            }

            //set low
            if (low > entry.get(4)) {
                low = entry.get(4);
            }

            // set close on last minute
            if ((int) (entry.get(1).doubleValue()) % 5 == 4) {
                close = entry.get(5);
                List<Double> newEntry = new ArrayList<>();
                newEntry.add(hour);
                newEntry.add(minute);
                newEntry.add(open);
                newEntry.add(high);
                newEntry.add(low);
                newEntry.add(close);
                fiveMinData.add(newEntry);
            }
        }
        return fiveMinData;
    }
}
