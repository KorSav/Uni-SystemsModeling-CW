package com;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ua.stetsenkoinna.PetriObj.PetriObjModel;
import ua.stetsenkoinna.PetriObj.PetriP;

import com.LinearRegrAsis.ExperimentResult;

public class Experiment {

    private static final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public record ExperimentData(int n, double delay, double epsilon) {

        @Override
        public String toString() {
            return "n=%-2d del=%.2f".formatted(n, delay);
        }
    }

    private static int[] ns = new int[]{4, 10};
    private static double[] delays = new double[]{0.2, 0.3};
    private static ExperimentData[] expData = new ExperimentData[]{
        new ExperimentData(ns[0], delays[0], 1e-1),
        new ExperimentData(ns[0], delays[1], 1e-1),
        new ExperimentData(ns[1], delays[0], 1e-1),
        new ExperimentData(ns[1], delays[1], 1e-1)
    };

    private static final int nexp = expData.length;

    public static void main(String[] args) throws Throwable {
        System.out.print("\u001B[?25l");
        final boolean isVerbose = true;
        final int passesTransPrd = 5;
        final int passesExp = 20; // ɛ=σ, β=0.95

        System.out.println("Start parallel transition periods search...");
        var transPrds = parallelFindTransitionPeriods(expData, passesTransPrd);
        System.out.println("Start parallel measure of output values...");
        var expResPasses = parallelTakeExperiments(expData, transPrds, passesExp);
        var means = new double[nexp];
        var vars = new double[nexp];
        System.out.println("Statistical data for exps with beta=0.95:");
        System.out.println("  %-12s | %-7s | %-7s | %s".formatted("experiment", "std dev", "mean", "var"));
        for (int i = 0; i < nexp; i++) {
            means[i] = Helper.calcMean(expResPasses[i]);
            vars[i] = Helper.calcVar(expResPasses[i], means[i]);
            var stdDev = Math.sqrt(vars[i]);
            System.out.println("  %12s | %-7.3f | %-7.3f | %f".formatted(expData[i].toString(), stdDev, means[i], vars[i]));
        }
        // Cochren test
        double maxD = 0;
        double sumD = 0;
        for (int i = 0; i < nexp; i++) {
            var s = vars[i] * passesExp / (passesExp - 1);
            sumD += s;
            maxD = Double.max(maxD, s);
        }
        var G = maxD / sumD;
        System.out.println("  maxD=%.5f  sumD=%.5f".formatted(maxD, sumD));
        var G_cr = 0.41;
        if (G <= G_cr) {
            System.out.println("%.2f < %.2f => exp is reproducible".formatted(G, G_cr));
        } else {
            System.out.println("%.2f > %.2f => exp is not reproducible".formatted(G, G_cr));
            executor.shutdown();
            return;
        }
        var expRes = new ExperimentResult[nexp];
        for (int i = 0; i < nexp; i++) {
            expRes[i] = new ExperimentResult(expData[i], means[i]);
        }
        var lra = new LinearRegrAsis(expRes);
        var b = lra.calcPolynomial();
        var D = sumD / nexp;
        var isBSignificant = new ArrayList<Boolean>(b.length);
        for (int i = 0; i < b.length; i++) {
            var ti = Math.abs(b[i]) * Math.sqrt(nexp * passesExp / D);
            var msg = "  b%d: %.3f %%c %.2f - %%s%s".formatted(i, ti, 2.0, "significant");
            isBSignificant.add(ti > 2);
            if (ti > 2) {
                System.out.println(msg.formatted('>', ""));
            } else {
                System.out.println(msg.formatted('<', "not "));
            }
        }
        if (isBSignificant.stream().allMatch(s -> s)) {
            System.out.println("All coeficients are significant. Research succeeded");
        } else if (isBSignificant.stream().skip(1).allMatch(s -> !s)) {
            System.out.println("All coeficients are not significant. Research failed");
        } else {
            System.out.println("There are some non significant coeficients");
            var signCnt = isBSignificant.stream().filter(s -> s).count();
            var sumActualRegrDiff = 0d;
            for (var exp : expRes) {
                var d = exp.qmax() - lra.estimate(exp);
                sumActualRegrDiff += d * d;
            }
            var D_ad = sumActualRegrDiff / (nexp - signCnt);
            var F = D_ad / D;
            if (F < 1) {
                System.out.println("%.3f < 1, regr model is adequate".formatted(F));
            } else if (F < 8.58) {
                System.out.println("%.3f < F_cr, regr model is adequate".formatted(F));
            } else {
                System.out.println("%.3f >= F_cr, regr model is not adequate".formatted(F));
            }
        }
        executor.shutdown();
    }

    public static double[][] parallelTakeExperiments(ExperimentData[] expData, double[] transPrds, int passesCnt, double timeModel) throws Exception {
        final int colsCnt = 5;
        var stringWriters = new CRStringWriter[expData.length * passesCnt];
        for (int i = 0; i < stringWriters.length; i++) {
            stringWriters[i] = new CRStringWriter();
            stringWriters[i].write("-----\r");
        }
        final Object modelsCreationLock = new Object();
        var tasks = new ArrayList<Callable<Double>>(stringWriters.length);
        for (int iexp = 0; iexp < expData.length; iexp++) {
            for (int ipass = 0; ipass < passesCnt; ipass++) {
                final var sw = stringWriters[iexp * passesCnt + ipass];
                final var exp = expData[iexp];
                final var iexpLocal = iexp;
                tasks.add(() -> {
                    var pw = new PrintWriter(sw, true);
                    double ov = findOutVal(exp, transPrds[iexpLocal], timeModel, pw, modelsCreationLock);
                    sw.close();
                    return ov;
                });
            }
        }
        var tpResults = tasks.stream().map(executor::submit).toList();
        final int linesCnt = expData.length * (passesCnt / colsCnt + 1);
        for (int i = 0; i < linesCnt; i++) {
            System.out.println();
        }
        boolean run = true;
        while (run) { // to print the results once again when all finished
            if (tpResults.stream().allMatch(res -> res.isDone())) {
                run = false;
            }
            System.out.print("\u001B[%dA".formatted(linesCnt));
            for (int iexp = 0; iexp < expData.length; iexp++) {
                System.out.print("Out vals for %s:".formatted(expData[iexp]));
                for (int ipass = 0; ipass < passesCnt; ipass++) {
                    final int iexpLog = iexp * passesCnt + ipass;
                    final int icol = ipass % colsCnt;
                    if (icol == 0) {
                        System.out.println();
                    }
                    System.out.print("   %-12s".formatted(stringWriters[iexpLog].toString()));
                }
                System.out.println();
            }
        }
        //
        var expResults = new double[expData.length][passesCnt];
        for (int iexp = 0; iexp < expData.length; iexp++) {
            for (int ipass = 0; ipass < passesCnt; ipass++) {
                expResults[iexp][ipass] = tpResults.get(iexp * passesCnt + ipass).get();
            }
        }
        return expResults;
    }

    public static double[][] parallelTakeExperiments(ExperimentData[] expData, double[] transPrds, int passesCnt) throws Exception {
        return parallelTakeExperiments(expData, transPrds, passesCnt, 10_000);
    }

    private static double findOutVal(ExperimentData exp, double transPrd, double measureTime, PrintWriter pw, Object modelCreationLock) throws Exception {
        PetriObjModel model;
        synchronized (modelCreationLock) {
            model = LiftObject.CreateLiftModel(exp.n, exp.delay);
        }
        var queues = new PetriP[5];
        var totals = new PetriP[5];
        for (int i = 0; i < 5; i++) {
            var floor = model.findObj("Поверх %d".formatted(i + 1));
            queues[i] = floor.findPlace("Очік на п%d".formatted(i + 1));
            totals[i] = floor.findPlace("Всього було на п%d".formatted(i + 1));
        }
        var ravg = new RunningAverage();
        while (model.moveNext(transPrd + measureTime)) {
            if (model.getCurrentTime() > transPrd) {
                var tFromTPEnd = model.getCurrentTime() - transPrd;
                ravg.updateAverage(Helper.calcOutputValue(queues, totals, model.getCurrentTime()), tFromTPEnd);
                pw.print("%.3f %2d%%\r".formatted(ravg.average, (int) (tFromTPEnd / measureTime * 100)));
            } else {
                pw.print("tp %2d%%\r".formatted((int) (model.getCurrentTime() / transPrd * 100)));
            }
        }
        pw.print("%.3f".formatted(ravg.average));
        return ravg.average;
    }

    public static double[] parallelFindTransitionPeriods(ExperimentData[] expData, int passesPerExp) throws Throwable {
        final boolean isVerbose = true;

        var stringWriters = new CRStringWriter[expData.length * passesPerExp];
        for (int i = 0; i < stringWriters.length; i++) {
            stringWriters[i] = new CRStringWriter();
            stringWriters[i].write("<not run yet>\r");
        }
        final Object modelsCreationLock = new Object();
        var tasks = new ArrayList<Callable<Double>>(stringWriters.length);
        for (int iexp = 0; iexp < expData.length; iexp++) {
            for (int ipass = 0; ipass < passesPerExp; ipass++) {
                final var sw = stringWriters[iexp * passesPerExp + ipass];
                final var exp = expData[iexp];
                tasks.add(() -> {
                    var pw = new PrintWriter(sw, true);
                    double transPrd = findTransitionPeriod(exp.n, exp.delay, exp.epsilon, isVerbose, pw, modelsCreationLock);
                    pw.print(", tp = %.3f min".formatted(transPrd));
                    sw.close();
                    return transPrd;
                });
            }
        }
        var tpResults = tasks.stream().map(executor::submit).toList();
        final int linesCnt = expData.length * (passesPerExp + 1);
        for (int i = 0; i < linesCnt; i++) {
            System.out.println();
        }
        boolean run = true;
        while (run) { // to print the results once again when all finished
            if (tpResults.stream().allMatch(res -> res.isDone())) {
                run = false;
            }
            System.out.print("\u001B[%dA".formatted(linesCnt));
            for (int iexp = 0; iexp < expData.length; iexp++) {
                System.out.println("Finding transition period for %s".formatted(expData[iexp]));
                for (int ipass = 0; ipass < passesPerExp; ipass++) {
                    final int iexpLog = iexp * passesPerExp + ipass;
                    System.out.println("  " + stringWriters[iexpLog].toString());
                }
            }
        }
        double[] transPrds = new double[expData.length];
        for (int i = 0; i < tpResults.size(); i++) {
            var iexp = i / passesPerExp;
            var ipass = i % passesPerExp;
            if (ipass == 0) {
                transPrds[iexp] = tpResults.get(i).get();
            } else {
                transPrds[iexp] = Double.max(transPrds[iexp], tpResults.get(i).get());
            }
        }
        System.out.println("Taking maximum from each pass, so:");
        for (int i = 0; i < expData.length; i++) {
            System.out.println("  For %s tp = %.3f min".formatted(expData[i], transPrds[i]));
        }
        return transPrds;
    }

    private static double findTransitionPeriod(int n, double delay, double epsilon, boolean isVerbose, PrintWriter pw, Object modelCreationLock) throws Exception {
        final double confidenceIntervalMin = 1000;
        final int modelsCnt = 4;

        var models = new PetriObjModel[modelsCnt];
        synchronized (modelCreationLock) {
            for (int i = 0; i < modelsCnt; i++) {
                models[i] = LiftObject.CreateLiftModel(n, delay);
            }
        }
        var queues = new PetriP[modelsCnt][5];
        var totals = new PetriP[modelsCnt][5];
        for (int im = 0; im < modelsCnt; im++) {
            for (int i = 0; i < 5; i++) {
                var floor = models[im].findObj("Поверх %d".formatted(i + 1));
                queues[im][i] = floor.findPlace("Очік на п%d".formatted(i + 1));
                totals[im][i] = floor.findPlace("Всього було на п%d".formatted(i + 1));
            }
        }
        double validDiffStart = -1;
        double transPrd = -1;
        double d = 0;
        var ovs = new double[modelsCnt];
        var ovRunningAvgs = new RunningAverage[modelsCnt];
        for (int i = 0; i < modelsCnt; i++) {
            ovRunningAvgs[i] = new RunningAverage();
        }
        var curOvAvgs = new double[modelsCnt];
        while (transPrd == -1 && Helper.moveNext(models, Double.POSITIVE_INFINITY)) {
            ovs = Helper.calcOutputValues(queues, totals, models);
            for (int i = 0; i < modelsCnt; i++) {
                ovRunningAvgs[i].updateAverage(ovs[i], models[i].getCurrentTime());
                curOvAvgs[i] = ovRunningAvgs[i].getAvg();
            }
            d = Helper.calcMaxAbsDiff(curOvAvgs);
            if (d < epsilon) {
                if (validDiffStart < 0) {
                    validDiffStart = Helper.getCurTime(models);
                } else {
                    transPrd = Helper.getCurTime(models);
                }
            } else {
                validDiffStart = -1;
            }
            if (isVerbose) {
                pw.print("qmax=%.3f da=%.3f < e: %c %s                 \r".formatted(ovs[0], d, d < epsilon ? '+' : '-', validDiffStart == -1 ? "" : "for %3d mins".formatted((int) (transPrd - validDiffStart))));
            }
            if (transPrd - validDiffStart < confidenceIntervalMin) {
                transPrd = -1;
            }
        }
        if (transPrd == -1) {
            transPrd = Helper.getCurTime(models);
        }
        if (isVerbose) {
            pw.print("qmax=%.3f da=%.3f < e: %c %s".formatted(ovs[0], d, d < epsilon ? '+' : '-', validDiffStart == -1 ? "" : "for %3d mins".formatted((int) (transPrd - validDiffStart))));
        }
        return transPrd;
    }

    private static class RunningAverage {

        double average = 0;
        double sumT = 0;

        public void updateAverage(double value, double tcur) {
            if (sumT == 0) {
                average = value;
                sumT = tcur;
                return;
            }
            if (tcur - sumT < 0) {
                throw new RuntimeException("Time cannot go backwards");
            }
            double dt = tcur - sumT;
            double sumOfValueProdDt = average * sumT + value * dt;
            sumT = tcur;
            average = sumOfValueProdDt / sumT;
        }

        public double getAvg() {
            return average;
        }
    }

    public static class Helper {

        public static boolean moveNext(PetriObjModel[] models, double timeMod) {
            for (var model : models) {
                if (!model.moveNext(timeMod)) {
                    return false;
                }
            }
            return true;
        }

        public static double[] calcOutputValues(PetriP[][] queues, PetriP[][] totals, PetriObjModel[] models) {
            final int modelsCnt = models.length;
            final double[] res = new double[modelsCnt];
            for (int im = 0; im < modelsCnt; im++) {
                res[im] = calcOutputValue(queues[im], totals[im], models[im].getCurrentTime());
            }
            return res;
        }

        public static double calcOutputValue(PetriP[] queues, PetriP[] totals, double tcur) {
            var outputValue = -1d;
            for (int i = 0; i < 5; i++) {
                var meanLength = queues[i].getMean();
                var wereInTotal = totals[i].getMark();
                var waitingInQueue = 0d;
                if (wereInTotal != 0) {
                    waitingInQueue = meanLength * tcur / wereInTotal;
                }
                outputValue = Double.max(outputValue, waitingInQueue);
            }
            return outputValue;
        }

        public static double calcMaxAbsDiff(double[] outputValues) {
            double min = outputValues[0];
            double max = outputValues[0];
            for (int i = 1; i < outputValues.length; i++) {
                var ov = outputValues[i];
                min = Double.min(min, ov);
                max = Double.max(max, ov);
            }
            return max - min;
        }

        public static double getCurTime(PetriObjModel[] models) {
            double sum = 0;
            for (var m : models) {
                sum += m.getCurrentTime();
            }
            return sum / models.length;
        }

        public static double calcMean(double[] vals) {
            var sum = 0d;
            for (var v : vals) {
                sum += v;
            }
            return sum / vals.length;
        }

        public static double calcVar(double[] vals, double mean) {
            var sum = 0d;
            for (var v : vals) {
                var d = v - mean;
                sum += d * d;
            }
            return sum / vals.length;
        }
    }

}
