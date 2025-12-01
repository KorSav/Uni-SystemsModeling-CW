package com;

import com.Experiment.ExperimentData;
import com.Experiment.Helper;

public class FinalTestRun {

    private static ExperimentData[] expData = new ExperimentData[]{
        new ExperimentData(6, 0.25, 1e-1),
        new ExperimentData(10, 0.2, 1e-1)
    };

    private static final int nexp = expData.length;

    public static void main(String[] args) throws Exception, Throwable {
        var transPrds = new double[2];
        System.out.println("Taking experiments to compare improved system with initial...");
        var expResPasses = Experiment.parallelTakeExperiments(expData, transPrds, 20, 20_000);
        var means = new double[nexp];
        var vars = new double[nexp];
        System.out.println("Statistical data to estimate improvements:");
        System.out.println("  %-13s | %-7s | %-7s | %s".formatted("experiment", "std dev", "mean", "var"));
        for (int i = 0; i < nexp; i++) {
            means[i] = Helper.calcMean(expResPasses[i]);
            vars[i] = Helper.calcVar(expResPasses[i], means[i]);
            var stdDev = Math.sqrt(vars[i]);
            System.out.println("  %13s | %-7.3f | %-7.3f | %f".formatted(expData[i].toString(), stdDev, means[i], vars[i]));
        }
    }
}
