package de.lmu.ifi.dbs.elki.database.datastore;

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

import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;

/**
 * Double-valued data store (avoids boxing/unboxing).
 * 
 * @author Erich Schubert
 */
public interface DoubleDistanceDataStore extends DataStore<DoubleDistance> {
  /**
   * Getter, but using objects.
   * 
   * @deprecated Use {@link #doubleValue} instead, to avoid boxing/unboxing cost.
   */
  @Override
  @Deprecated
  public DoubleDistance get(DBIDRef id);

  /**
   * Retrieves an object from the storage.
   * 
   * @param id Database ID.
   * @return Double value
   */
  public double doubleValue(DBIDRef id);
}