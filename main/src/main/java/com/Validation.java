package com;

import java.io.File;
import java.io.FileWriter;

public class Validation {

    public static void main(String[] args) throws Exception {
        final String fname = "validation_10k.csv";
        final String delim = ";";
        final String lf = "\n";
        final int passesCnt = 5;
        final int modelMin = 10_000;

        var csvFile = new File(fname);
        csvFile.createNewFile();
        try (FileWriter fw = new FileWriter(csvFile)) {
            fw.write(RunArgs.getOneLineHeader(delim));
            fw.write(delim);
            fw.write(delim);
            fw.write(RunResults.getOneLineHeader(delim));
            fw.write(lf);
            fw.flush();

            var ns = new int[]{6, 4, 2, 8, 10, 12};
            var delayMins = new double[]{0.25, 0.1, 0.05, 0.5, 0.75, 1};
            var totalRuns = passesCnt * (1 + ns.length - 1 + delayMins.length - 1);

            var pastRuns = 0;
            System.out.print("Validating %3d%%\r".formatted((int) (pastRuns / (float) totalRuns * 100)));
            var runArgs = new RunArgs(ns[0], delayMins[0]);
            RunResults total = Run(runArgs, modelMin);
            System.out.print("Validating %3d%%\r".formatted((int) (++pastRuns / (float) totalRuns * 100)));
            for (int ipass = 1; ipass < passesCnt; ipass++) {
                total = total.sum(Run(runArgs, modelMin));
                System.out.print("Validating %3d%%\r".formatted((int) (++pastRuns / (float) totalRuns * 100)));
            }
            var avg = total.div(passesCnt);
            fw.write(runArgs.toString(delim));
            fw.write(delim);
            fw.write(delim);
            fw.write(avg.toString(delim));
            fw.write(lf);
            fw.flush();
            // vary freePlaces
            for (int i = 1; i < ns.length; i++) {
                runArgs = new RunArgs(ns[i], delayMins[0]);
                total = Run(runArgs, modelMin);
                System.out.print("Validating %3d%%\r".formatted((int) (++pastRuns / (float) totalRuns * 100)));
                for (int ipass = 1; ipass < passesCnt; ipass++) {
                    total = total.sum(Run(runArgs, modelMin));
                    System.out.print("Validating %3d%%\r".formatted((int) (++pastRuns / (float) totalRuns * 100)));
                }
                avg = total.div(passesCnt);
                fw.write(runArgs.toString(delim));
                fw.write(delim);
                fw.write(delim);
                fw.write(avg.toString(delim));
                fw.write(lf);
                fw.flush();
            }
            // vary delayMins
            for (int i = 1; i < delayMins.length; i++) {
                runArgs = new RunArgs(ns[0], delayMins[i]);
                total = Run(runArgs, modelMin);
                System.out.print("Validating %3d%%\r".formatted((int) (++pastRuns / (float) totalRuns * 100)));
                for (int ipass = 1; ipass < passesCnt; ipass++) {
                    total = total.sum(Run(runArgs, modelMin));
                    System.out.print("Validating %3d%%\r".formatted((int) (++pastRuns / (float) totalRuns * 100)));
                }
                avg = total.div(passesCnt);
                fw.write(runArgs.toString(delim));
                fw.write(delim);
                fw.write(delim);
                fw.write(avg.toString(delim));
                fw.write(lf);
                fw.flush();
            }
        }
        System.out.println("\nValidation finished! Results are in: ./%s".formatted(fname));
    }

    private static RunResults Run(RunArgs args, double modelMin) throws Exception {
        var waitingInQueues = new double[5];
        var model = LiftObject.CreateLiftModel(args.n, args.delayMin);
        model.setProtocolPrint(false);
        // model.go(modelMin);
        while (model.moveNext(modelMin)) {
        }

        for (int i = 0; i < 5; i++) {
            var floor = model.findObj("Поверх %d".formatted(i + 1));
            var queue = floor.findPlace("Очік на п%d".formatted(i + 1));
            var meanLength = queue.getMean();
            var wereInTotal = floor.findPlace("Всього було на п%d".formatted(i + 1)).getMark();
            waitingInQueues[i] = meanLength * model.getCurrentTime() / wereInTotal;
        }

        var waitsTimeRatio = model.findObj("Логіка очікування").findPlace("Чекає").getMean();
        var movesEmptyTimeRatio = model.findObj("Рух ліфту").findPlace("Рухається порожній").getMean();
        var movesWithPassengersTimeRatio = 1 - waitsTimeRatio - movesEmptyTimeRatio;
        var ltr = new LiftTimeRatios(waitsTimeRatio, movesEmptyTimeRatio, movesWithPassengersTimeRatio);

        var pFreePlaces = model.findObj("Рух ліфту").findPlace("Вільних місць");
        var maxAmountInLift = args.n - pFreePlaces.getObservedMin();
        var meanAmountInLift = args.n - pFreePlaces.getMean();

        var failuresRate = new double[5];
        for (int ifloor = 0; ifloor < 5; ifloor++) {
            var floor = model.findObj("Поверх %d".formatted(ifloor + 1));
            var satCnt = floor.findPlace("Сіли в ліфт на п%d".formatted(ifloor + 1)).getMark();
            var totalCnt = floor.findPlace("Всього було на п%d".formatted(ifloor + 1)).getMark();
            failuresRate[ifloor] = 1 - satCnt / (double) totalCnt;
        }
        return new RunResults(waitingInQueues, ltr, maxAmountInLift, meanAmountInLift, failuresRate);
    }

    private record RunArgs(int n, double delayMin) {

        public static String getOneLineHeader(String delim) {
            var res = new StringBuilder();
            res.append("N%s".formatted(delim));
            res.append("Lift delay, min".formatted(delim));
            return res.toString();
        }

        public String toString(String delim) {
            var res = new StringBuilder();
            res.append("%d%s".formatted(n, delim));
            res.append("%.3f".formatted(delayMin, delim));
            return res.toString();
        }
    }

    private record LiftTimeRatios(double waiting, double movesEmpty, double movesNempty) {

        public LiftTimeRatios sum(LiftTimeRatios rhs) {
            return new LiftTimeRatios(waiting + rhs.waiting, movesEmpty + rhs.movesEmpty, movesNempty + rhs.movesNempty);
        }

        public LiftTimeRatios div(int divisor) {
            return new LiftTimeRatios(waiting / divisor, movesEmpty / divisor, movesNempty / divisor);
        }
    }

    private record RunResults(double[] waitingInQueues, LiftTimeRatios liftTimeRatio, int maxInLift, double avgInLift, double[] failuresRate) {

        final static int floorsCnt = 5;

        public static String getOneLineHeader(String delim) {
            var res = new StringBuilder();
            for (int i = 0; i < floorsCnt; i++) {
                res.append("Q%d, min%s".formatted(i + 1, delim));
            }
            res.append("Lift waits%s".formatted(delim));
            res.append("Lift mvEmpty%s".formatted(delim));
            res.append("Lift mvNempty%s".formatted(delim));
            res.append("Max in lift%s".formatted(delim));
            res.append("Avg in lift%s".formatted(delim));
            for (int i = 0; i < floorsCnt; i++) {
                res.append("Fail %d%s".formatted(i + 1, i == floorsCnt - 1 ? "" : delim));
            }
            return res.toString();
        }

        public String toString(String delim) {
            var res = new StringBuilder();
            for (int i = 0; i < floorsCnt; i++) {
                res.append("%f%s".formatted(waitingInQueues[i], delim));
            }
            res.append("%f%s".formatted(liftTimeRatio.waiting, delim));
            res.append("%f%s".formatted(liftTimeRatio.movesEmpty, delim));
            res.append("%f%s".formatted(liftTimeRatio.movesNempty, delim));
            res.append("%d%s".formatted(maxInLift, delim));
            res.append("%f%s".formatted(avgInLift, delim));
            for (int i = 0; i < floorsCnt; i++) {
                res.append("%f%s".formatted(failuresRate[i], i == floorsCnt - 1 ? "" : delim));
            }
            return res.toString();
        }

        public RunResults sum(RunResults rhs) {
            var resWaiting = new double[floorsCnt];
            for (int i = 0; i < floorsCnt; i++) {
                resWaiting[i] = waitingInQueues[i] + rhs.waitingInQueues[i];
            }
            var resFailuresRate = new double[floorsCnt];
            for (int i = 0; i < floorsCnt; i++) {
                resFailuresRate[i] = failuresRate[i] + rhs.failuresRate[i];
            }
            return new RunResults(resWaiting, liftTimeRatio.sum(rhs.liftTimeRatio), maxInLift + rhs.maxInLift, avgInLift + rhs.avgInLift, resFailuresRate);
        }

        public RunResults div(int divisor) {
            var resWaiting = new double[floorsCnt];
            for (int i = 0; i < floorsCnt; i++) {
                resWaiting[i] = waitingInQueues[i] / divisor;
            }
            var resFailuresRate = new double[floorsCnt];
            for (int i = 0; i < floorsCnt; i++) {
                resFailuresRate[i] = failuresRate[i] / divisor;
            }
            return new RunResults(resWaiting, liftTimeRatio.div(divisor), maxInLift / divisor, avgInLift / divisor, resFailuresRate);
        }
    }
}
