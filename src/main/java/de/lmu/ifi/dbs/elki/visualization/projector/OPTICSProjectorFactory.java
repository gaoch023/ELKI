package de.lmu.ifi.dbs.elki.visualization.projector;

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

import java.util.Collection;

import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.optics.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.opticsplot.OPTICSPlot;

/**
 * Produce OPTICS plot projections
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has OPTICSProjector
 */
public class OPTICSProjectorFactory implements ProjectorFactory {
  /**
   * Constructor.
   */
  public OPTICSProjectorFactory() {
    super();
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result newResult) {
    Collection<ClusterOrderResult<?>> cos = ResultUtil.filterResults(newResult, ClusterOrderResult.class);
    for(ClusterOrderResult<?> co : cos) {
      if(OPTICSPlot.canPlot(co)) {
        @SuppressWarnings("unchecked")
        OPTICSProjector<?> proj = new OPTICSProjector<>((ClusterOrderResult<DoubleDistance>) co);
        baseResult.getHierarchy().add(co, proj);
      }
    }
  }
}
