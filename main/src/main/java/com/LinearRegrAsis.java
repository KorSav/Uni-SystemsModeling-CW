package com;

import java.util.ArrayList;

import com.Experiment.ExperimentData;

public class LinearRegrAsis {

    final double X1;
    final double X2;
    final double d1;
    final double d2;
    final ExperimentResult[] exps;
    final int N;
    final double[] b = new double[4];

    public LinearRegrAsis(ExperimentResult[] exps) {
        int maxN = 0;
        int minN = Integer.MAX_VALUE;
        double maxDel = 0;
        double minDel = Double.MAX_VALUE;
        for (var e : exps) {
            maxN = Integer.max(maxN, e.n());
            minN = Integer.min(minN, e.n());
            maxDel = Double.max(maxDel, e.delay());
            minDel = Double.min(minDel, e.delay());
        }
        X1 = (minN + maxN) / 2;
        X2 = (minDel + maxDel) / 2;
        d1 = (maxN - minN) / 2;
        d2 = (maxDel - minDel) / 2;
        this.exps = exps;
        N = exps.length;
    }

    public double[] calcPolynomial() {
        var normalized = new ArrayList<NormExpRes>(N);
        for (var e : exps) {
            normalized.add(normalize(e));
        }
        for (var ne : normalized) {
            b[0] += ne.qmax;
            b[1] += ne.n * ne.qmax;
            b[2] += ne.delay * ne.qmax;
            b[3] += (ne.n * ne.delay) * ne.qmax;
        }
        for (int i = 0; i < b.length; i++) {
            b[i] /= N;
        }
        System.out.println("Regression equation: ");
        System.out.print("y = %.3f".formatted(b[0], b[1], b[2], b[3]));
        for (int i = 1; i < b.length; i++) {
            var abi = Math.abs(b[i]);
            final var xi = new String[]{"x1", "x2", "x1*x2"};
            System.out.print(" %c %.3f*%s".formatted(b[i] < 0 ? '-' : '+', abi, xi[i - 1]));
        }
        System.out.println();
        return b;
    }

    public double estimate(ExperimentResult expRes) {
        var norm = normalize(expRes);
        return b[0] + b[1] * norm.n + b[2] * norm.delay + b[3] * norm.n * norm.delay;
    }

    private NormExpRes normalize(ExperimentResult exp) {
        return new NormExpRes((exp.n - X1) / d1, (exp.delay - X2) / d2, exp.qmax);
    }

    public record NormExpRes(double n, double delay, double qmax) {

    }

    public record ExperimentResult(int n, double delay, double qmax) {

        public ExperimentResult(ExperimentData exp, double qmax) {
            this(exp.n(), exp.delay(), qmax);
        }
    }

}
