package de.lmu.ifi.dbs.elki.index.preprocessed.subspaceproj;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.distance.DistanceDBIDList;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Matrix;
import de.lmu.ifi.dbs.elki.math.linearalgebra.SubspaceProjectionResult;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.documentation.Title;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Preprocessor for PreDeCon local dimensionality and locally weighted matrix
 * assignment to objects of a certain database.
 * 
 * @author Peer Kröger
 * 
 * @apiviz.has SubspaceProjectionResult
 * 
 * @param <D> Distance type
 * @param <V> Vector type
 */
@Title("PreDeCon Preprocessor")
@Description("Computes the projected dimension of objects of a certain database according to the PreDeCon algorithm.\n" + "The variance analysis is based on epsilon range queries.")
public class PreDeConSubspaceIndex<V extends NumberVector<?>, D extends Distance<D>> extends AbstractSubspaceProjectionIndex<V, D, SubspaceProjectionResult> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PreDeConSubspaceIndex.class);

  /**
   * The threshold for small eigenvalues.
   */
  protected double delta;

  /**
   * The kappa value for generating the variance vector.
   */
  private final int kappa = 50;

  /**
   * Constructor.
   * 
   * @param relation Relation
   * @param epsilon Epsilon value
   * @param rangeQueryDistanceFunction range query distance
   * @param minpts Minpts parameter
   * @param delta Delta value
   */
  public PreDeConSubspaceIndex(Relation<V> relation, D epsilon, DistanceFunction<V, D> rangeQueryDistanceFunction, int minpts, double delta) {
    super(relation, epsilon, rangeQueryDistanceFunction, minpts);
    this.delta = delta;
  }

  @Override
  protected SubspaceProjectionResult computeProjection(DBIDRef id, DistanceDBIDList<D> neighbors, Relation<V> database) {
    StringBuilder msg = null;

    int referenceSetSize = neighbors.size();
    V obj = database.get(id);

    if(getLogger().isDebugging()) {
      msg = new StringBuilder();
      msg.append("referenceSetSize = ").append(referenceSetSize);
      msg.append("\ndelta = ").append(delta);
    }

    if(referenceSetSize == 0) {
      throw new RuntimeException("Reference Set Size = 0. This should never happen!");
    }

    // prepare similarity matrix
    int dim = obj.getDimensionality();
    Matrix simMatrix = new Matrix(dim, dim, 0);
    for(int i = 0; i < dim; i++) {
      simMatrix.set(i, i, 1);
    }

    // prepare projected dimensionality
    int projDim = 0;

    // start variance analysis
    double[] sum = new double[dim];
    for(DBIDIter neighbor = neighbors.iter(); neighbor.valid(); neighbor.advance()) {
      V o = database.get(neighbor);
      for(int d = 0; d < dim; d++) {
        final double diff = obj.doubleValue(d) - o.doubleValue(d);
        sum[d] += diff * diff;
      }
    }

    for(int d = 0; d < dim; d++) {
      if(Math.sqrt(sum[d]) / referenceSetSize <= delta) {
        if(msg != null) {
          msg.append("\nsum[").append(d).append("]= ").append(sum[d]);
          msg.append("\n  Math.sqrt(sum[d]) / referenceSetSize)= ").append(Math.sqrt(sum[d]) / referenceSetSize);
        }
        // projDim++;
        simMatrix.set(d, d, kappa);
      }
      else {
        // bug in paper?
        projDim++;
      }
    }

    if(projDim == 0) {
      if(msg != null) {
        // msg.append("\nprojDim == 0!");
      }
      projDim = dim;
    }

    if(msg != null) {
      msg.append("\nprojDim ");
      // .append(database.getObjectLabelQuery().get(id));
      msg.append(": ").append(projDim);
      msg.append("\nsimMatrix ");
      // .append(database.getObjectLabelQuery().get(id));
      msg.append(": ").append(FormatUtil.format(simMatrix, FormatUtil.NF4));
      getLogger().debugFine(msg.toString());
    }

    return new SubspaceProjectionResult(projDim, simMatrix);
  }

  @Override
  public String getLongName() {
    return "PreDeCon Subspaces";
  }

  @Override
  public String getShortName() {
    return "PreDeCon-subsp";
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  @Override
  public void logStatistics() {
    // No statistics to log.
  }

  /**
   * Factory.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.uses PreDeConSubspaceIndex oneway - - «creates»
   * 
   * @param <V> Vector type
   * @param <D> Distance type
   */
  public static class Factory<V extends NumberVector<?>, D extends Distance<D>> extends AbstractSubspaceProjectionIndex.Factory<V, D, PreDeConSubspaceIndex<V, D>> {
    /**
     * The default value for delta.
     */
    public static final double DEFAULT_DELTA = 0.01;

    /**
     * Parameter for Delta.
     */
    public static final OptionID DELTA_ID = new OptionID("predecon.delta", "a double between 0 and 1 specifying the threshold for small Eigenvalues (default is delta = " + DEFAULT_DELTA + ").");

    /**
     * The threshold for small eigenvalues.
     */
    protected double delta;

    /**
     * Constructor.
     * 
     * @param epsilon
     * @param rangeQueryDistanceFunction
     * @param minpts
     * @param delta
     */
    public Factory(D epsilon, DistanceFunction<V, D> rangeQueryDistanceFunction, int minpts, double delta) {
      super(epsilon, rangeQueryDistanceFunction, minpts);
      this.delta = delta;
    }

    @Override
    public PreDeConSubspaceIndex<V, D> instantiate(Relation<V> relation) {
      return new PreDeConSubspaceIndex<>(relation, epsilon, rangeQueryDistanceFunction, minpts, delta);
    }

    /**
     * Parameterization class.
     * 
     * @author Erich Schubert
     * 
     * @apiviz.exclude
     */
    public static class Parameterizer<V extends NumberVector<?>, D extends Distance<D>> extends AbstractSubspaceProjectionIndex.Factory.Parameterizer<V, D, Factory<V, D>> {
      /**
       * The threshold for small eigenvalues.
       */
      protected double delta;

      @Override
      protected void makeOptions(Parameterization config) {
        super.makeOptions(config);
        DoubleParameter deltaP = new DoubleParameter(DELTA_ID, DEFAULT_DELTA);
        deltaP.addConstraint(CommonConstraints.GREATER_THAN_ZERO_DOUBLE);
        deltaP.addConstraint(CommonConstraints.LESS_THAN_ONE_DOUBLE);
        if(config.grab(deltaP)) {
          delta = deltaP.doubleValue();
        }
      }

      @Override
      protected Factory<V, D> makeInstance() {
        return new Factory<>(epsilon, rangeQueryDistanceFunction, minpts, delta);
      }
    }
  }
}
