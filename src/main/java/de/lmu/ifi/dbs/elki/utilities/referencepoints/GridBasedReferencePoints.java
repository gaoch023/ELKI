package de.lmu.ifi.dbs.elki.utilities.referencepoints;

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

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.CommonConstraints;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Grid-based strategy to pick reference points.
 * 
 * @author Erich Schubert
 * 
 * @param <V> Object type
 */
public class GridBasedReferencePoints<V extends NumberVector<?>> implements ReferencePointsHeuristic<V> {
  // TODO: add "grid sampling" option.

  /**
   * Parameter to specify the grid resolution.
   * <p>
   * Key: {@code -grid.size}
   * </p>
   */
  public static final OptionID GRID_ID = new OptionID("grid.size", "The number of partitions in each dimension. Points will be placed on the edges of the grid, except for a grid size of 0, where only the mean is generated as reference point.");

  /**
   * Parameter to specify the extra scaling of the space, to allow
   * out-of-data-space reference points.
   * <p>
   * Key: {@code -grid.oversize}
   * </p>
   */
  public static final OptionID GRID_SCALE_ID = new OptionID("grid.scale", "Scale the grid by the given factor. This can be used to obtain reference points outside the used data space.");

  /**
   * Holds the value of {@link #GRID_ID}.
   */
  protected int gridres;

  /**
   * Holds the value of {@link #GRID_SCALE_ID}.
   */
  protected double gridscale;

  /**
   * Constructor.
   * 
   * @param gridres Grid resolution
   * @param gridscale Grid scaling
   */
  public GridBasedReferencePoints(int gridres, double gridscale) {
    super();
    this.gridres = gridres;
    this.gridscale = gridscale;
  }

  @Override
  public <T extends V> Collection<V> getReferencePoints(Relation<T> db) {
    Relation<V> database = DatabaseUtil.relationUglyVectorCast(db);
    Pair<V, V> minmax = DatabaseUtil.computeMinMax(database);
    NumberVector.Factory<V, ?> factory = RelationUtil.getNumberVectorFactory(database);

    int dim = RelationUtil.dimensionality(db);

    // Compute mean from minmax.
    double[] mean = new double[dim];
    for(int d = 0; d < dim; d++) {
      mean[d] = (minmax.first.doubleValue(d) + minmax.second.doubleValue(d)) * .5;
    }

    int gridpoints = Math.max(1, MathUtil.ipowi(gridres + 1, dim));
    ArrayList<V> result = new ArrayList<>(gridpoints);
    double[] delta = new double[dim];
    if(gridres > 0) {
      double halfgrid = gridres / 2.0;
      for(int d = 0; d < dim; d++) {
        delta[d] = (minmax.second.doubleValue(d) - minmax.first.doubleValue(d)) / gridres;
      }

      double[] vec = new double[dim];
      for(int i = 0; i < gridpoints; i++) {
        int acc = i;
        for(int d = 0; d < dim; d++) {
          int coord = acc % (gridres + 1);
          acc = acc / (gridres + 1);
          vec[d] = mean[d] + (coord - halfgrid) * delta[d] * gridscale;
        }
        V newp = factory.newNumberVector(vec);
        // logger.debug("New reference point: " + FormatUtil.format(vec));
        result.add(newp);
      }
    }
    else {
      result.add(factory.newNumberVector(mean));
      // logger.debug("New reference point: " + FormatUtil.format(mean));
    }

    return result;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<V extends NumberVector<?>> extends AbstractParameterizer {
    /**
     * Holds the value of {@link #GRID_ID}.
     */
    protected int gridres;

    /**
     * Holds the value of {@link #GRID_SCALE_ID}.
     */
    protected double gridscale;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      IntParameter gridP = new IntParameter(GRID_ID, 1);
      gridP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_INT);
      if(config.grab(gridP)) {
        gridres = gridP.getValue();
      }

      DoubleParameter gridscaleP = new DoubleParameter(GRID_SCALE_ID, 1.0);
      gridscaleP.addConstraint(CommonConstraints.GREATER_EQUAL_ZERO_DOUBLE);
      if(config.grab(gridscaleP)) {
        gridscale = gridscaleP.getValue();
      }
    }

    @Override
    protected GridBasedReferencePoints<V> makeInstance() {
      return new GridBasedReferencePoints<>(gridres, gridscale);
    }
  }
}
