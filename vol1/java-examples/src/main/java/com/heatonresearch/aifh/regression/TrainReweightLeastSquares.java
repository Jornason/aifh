package com.heatonresearch.aifh.regression;

import Jama.LUDecomposition;
import Jama.Matrix;
import com.heatonresearch.aifh.AIFHError;
import com.heatonresearch.aifh.general.data.BasicData;

import java.util.List;

/**
 * Train a GLM using iteratively reweighted least squares.
 * <p/>
 * http://en.wikipedia.org/wiki/Iteratively_reweighted_least_squares
 */
public class TrainReweightLeastSquares {

    /**
     * The GLM to train.
     */
    private final MultipleLinearRegression algorithm;

    /**
     * The training data.
     */
    private final List<BasicData> trainingData;

    /**
     * The last error.
     */
    private double error;

    /**
     * The Hessian matrix.
     */
    private final double[][] hessian;

    /**
     * The gradient matrix.
     */
    private final Matrix gradient;

    /**
     * Construct the trainer.
     *
     * @param theAlgorithm    The GLM to train.
     * @param theTrainingData The training data.
     */
    public TrainReweightLeastSquares(MultipleLinearRegression theAlgorithm, List<BasicData> theTrainingData) {
        this.algorithm = theAlgorithm;
        this.trainingData = theTrainingData;
        this.gradient = new Matrix(theAlgorithm.getLongTermMemory().length, 1);
        this.hessian = new double[theAlgorithm.getLongTermMemory().length][theAlgorithm.getLongTermMemory().length];
    }

    /**
     * Perform one iteration of training.
     */
    public void iteration() {
        int rowCount = this.trainingData.size();
        int coeffCount = this.algorithm.getLongTermMemory().length;

        double[][] working = new double[rowCount][coeffCount];
        double[] errors = new double[rowCount];
        double[] weights = new double[rowCount];
        Matrix deltas;

        for (int i = 0; i < rowCount; i++) {
            BasicData element = this.trainingData.get(i);

            working[i][0] = 1;
            for (int j = 0; j < element.getInput().length; j++)
                working[i][j + 1] = element.getInput()[j];
        }

        for (int i = 0; i < rowCount; i++) {
            BasicData element = this.trainingData.get(i);
            double y = this.algorithm.computeRegression(element.getInput())[0];
            errors[i] = y - element.getIdeal()[0];
            weights[i] = y * (1.0 - y);
        }

        for (int i = 0; i < gradient.getColumnDimension(); i++) {
            gradient.set(0, i, 0);
            for (int j = 0; j < gradient.getColumnDimension(); j++)
                hessian[i][j] = 0;
        }

        for (int j = 0; j < working.length; j++) {
            for (int i = 0; i < gradient.getColumnDimension(); i++) {
                gradient.set(i, 0, gradient.get(i, 0) + working[j][i] * errors[j]);
            }
        }

        for (int k = 0; k < weights.length; k++) {
            double[] r = working[k];

            for (int j = 0; j < r.length; j++) {
                for (int i = 0; i < r.length; i++) {
                    hessian[j][i] += r[i] * r[j] * weights[k];
                }
            }
        }

        LUDecomposition lu = new LUDecomposition(new Matrix(hessian));

        if (lu.isNonsingular()) {
            deltas = lu.solve(gradient);
        } else {
            throw new AIFHError("Matrix Non singular");
        }

        double[] prev = this.algorithm.getLongTermMemory().clone();

        for (int i = 0; i < this.algorithm.getLongTermMemory().length; i++)
            this.algorithm.getLongTermMemory()[i] -= deltas.get(i, 0);

        double max = 0;
        for (int i = 0; i < deltas.getColumnDimension(); i++)
            max = Math.max(Math.abs(deltas.get(i, 0)) / Math.abs(prev[i]), max);

        this.error = max;
    }

    /**
     * @return The last error.
     */
    public double getError() {
        return this.error;
    }
}
