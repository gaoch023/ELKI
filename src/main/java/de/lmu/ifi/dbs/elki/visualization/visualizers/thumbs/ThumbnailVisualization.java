package de.lmu.ifi.dbs.elki.visualization.visualizers.thumbs;

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

import java.awt.image.BufferedImage;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.SelectionResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.batikutil.ThumbnailRegistryEntry;
import de.lmu.ifi.dbs.elki.visualization.style.StyleResult;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;

/**
 * Thumbnail visualization.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses ThumbnailThread
 */
public class ThumbnailVisualization extends AbstractVisualization implements ThumbnailThread.Listener, DataStoreListener {
  /**
   * Constant to listen for data changes
   */
  public static final int ON_DATA = 1;

  /**
   * Constant to listen for selection changes
   */
  public static final int ON_SELECTION = 2;

  /**
   * Constant to listen for style result changes
   */
  public static final int ON_STYLE = 4;

  /**
   * Constant to <em>not</em> listen for projection changes
   */
  public static final int NO_PROJECTION = 8;

  /**
   * Visualizer factory
   */
  protected final VisFactory visFactory;

  /**
   * The thumbnail id.
   */
  protected int thumbid = -1;

  /**
   * Pending redraw
   */
  protected ThumbnailThread.Task pendingThumbnail = null;

  /**
   * Thumbnail resolution
   */
  protected int tresolution;

  /**
   * The event mask. See {@link #ON_DATA}, {@link #ON_SELECTION},
   * {@link #ON_STYLE}, {@link #NO_PROJECTION}
   */
  private int mask;

  /**
   * Our thumbnail (keep a reference to prevent garbage collection!)
   */
  private BufferedImage thumb;

  /**
   * Constructor.
   * 
   * @param visFactory Visualizer Factory to use
   * @param task Task to use
   * @param mask Event mask (for auto-updating)
   */
  public ThumbnailVisualization(VisFactory visFactory, VisualizationTask task, int mask) {
    super(task);
    this.visFactory = visFactory;
    this.tresolution = task.thumbsize;
    this.layer = task.getPlot().svgElement(SVGConstants.SVG_G_TAG);
    this.thumbid = -1;
    this.thumb = null;
    this.mask = mask;
    // Listen for database events only when needed.
    if ((mask & ON_DATA) == ON_DATA) {
      context.addDataStoreListener(this);
    }
    // Listen for result changes, including the one we monitor
    context.addResultListener(this);
  }

  @Override
  public void destroy() {
    if (pendingThumbnail != null) {
      ThumbnailThread.UNQUEUE(pendingThumbnail);
    }
    // TODO: remove image from registry?
    context.removeResultListener(this);
    context.removeDataStoreListener(this);
  }

  @Override
  public Element getLayer() {
    if (thumbid < 0) {
      synchronizedRedraw();
    }
    return layer;
  }

  /**
   * Redraw the visualization (maybe incremental).
   * 
   * Optional - by default, it will do a full redraw, which often is faster!
   */
  @Override
  protected void incrementalRedraw() {
    final Element oldcontainer;
    if (layer.hasChildNodes()) {
      oldcontainer = layer;
      layer = (Element) layer.cloneNode(false);
    } else {
      oldcontainer = null;
    }
    redraw();
    if (oldcontainer != null && oldcontainer.getParentNode() != null) {
      oldcontainer.getParentNode().replaceChild(layer, oldcontainer);
    }
  }

  /**
   * Perform a full redraw.
   */
  @Override
  protected void redraw() {
    if (thumbid < 0) {
      // LoggingUtil.warning("Generating new thumbnail " + this);
      layer.appendChild(SVGUtil.svgWaitIcon(task.getPlot().getDocument(), 0, 0, task.getWidth(), task.getHeight()));
      if (pendingThumbnail == null) {
        pendingThumbnail = ThumbnailThread.QUEUE(this);
      }
    } else {
      // LoggingUtil.warning("Injecting Thumbnail " + this);
      Element i = task.getPlot().svgElement(SVGConstants.SVG_IMAGE_TAG);
      SVGUtil.setAtt(i, SVGConstants.SVG_X_ATTRIBUTE, 0);
      SVGUtil.setAtt(i, SVGConstants.SVG_Y_ATTRIBUTE, 0);
      SVGUtil.setAtt(i, SVGConstants.SVG_WIDTH_ATTRIBUTE, task.getWidth());
      SVGUtil.setAtt(i, SVGConstants.SVG_HEIGHT_ATTRIBUTE, task.getHeight());
      i.setAttributeNS(SVGConstants.XLINK_NAMESPACE_URI, SVGConstants.XLINK_HREF_QNAME, ThumbnailRegistryEntry.INTERNAL_PROTOCOL + ":" + thumbid);
      layer.appendChild(i);
    }
  }

  @Override
  public synchronized void doThumbnail() {
    pendingThumbnail = null;
    try {
      SVGPlot plot = new SVGPlot();
      plot.getRoot().setAttribute(SVGConstants.SVG_VIEW_BOX_ATTRIBUTE, "0 0 " + task.getWidth() + " " + task.getHeight());

      // Work on a clone
      VisualizationTask clone = task.clone(plot, context);
      clone.thumbnail = false;
      Visualization vis = visFactory.makeVisualization(clone);

      plot.getRoot().appendChild(vis.getLayer());
      plot.updateStyleElement();
      final int tw = (int) (task.getWidth() * tresolution);
      final int th = (int) (task.getHeight() * tresolution);
      thumb = plot.makeAWTImage(tw, th);
      thumbid = ThumbnailRegistryEntry.registerImage(thumb);
      // The visualization will not be used anymore.
      vis.destroy();
      synchronizedRedraw();
    } catch (Exception e) {
      final Logging logger = Logging.getLogger(task.getFactory().getClass());
      if (logger != null && logger.isDebugging()) {
        logger.exception("Thumbnail failed.", e);
      } else {
        LoggingUtil.warning("Thumbnail for " + task.getFactory().getClass().getName() + " failed - enable debugging to see details.");
      }
      // TODO: hide the failed image?
    }
  }

  protected void refreshThumbnail() {
    // Discard an existing thumbnail
    thumbid = -1;
    thumb = null;
    // TODO: also purge from ThumbnailRegistryEntry?
    synchronizedRedraw();
  }

  @Override
  public void resultChanged(Result current) {
    if ((mask & ON_SELECTION) == ON_SELECTION && current instanceof SelectionResult) {
      refreshThumbnail();
      return;
    }
    if ((mask & ON_STYLE) == ON_STYLE && current instanceof StyleResult) {
      refreshThumbnail();
      return;
    }
    if (task.getProj() != null && (mask & NO_PROJECTION) != NO_PROJECTION && current == task.getProj()) {
      refreshThumbnail();
      return;
    }
    super.resultChanged(current);
  }
}
