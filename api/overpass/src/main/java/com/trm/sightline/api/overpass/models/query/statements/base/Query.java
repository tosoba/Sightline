package com.trm.sightline.api.overpass.models.query.statements.base;

import com.trm.sightline.api.overpass.models.query.settings.Modifier;
import com.trm.sightline.api.overpass.models.query.settings.Settings;
import com.trm.sightline.api.overpass.models.query.settings.Verbosity;
import com.trm.sightline.api.overpass.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public abstract class Query {

  private static final String OUT = "out";
  protected static final String SPACE = ";";

  private boolean isSorted;
  protected Settings.Builder settings;
  private Verbosity verbosity;
  private Modifier modifier;
  private int limit;

  protected Query(Builder<?> builder) {
    settings = builder.settings;
    verbosity = builder.verbosity;
    modifier = builder.modifier;
    isSorted = builder.isSorted;
    limit = builder.limit;
  }

  /**
   * Method creates query string.
   *
   * @return {@link String}
   */
  public abstract String toQuery();

  /**
   * Method creates content body for the query.
   *
   * @return {@link String}
   */
  protected abstract String getContent();

  protected String getHead() {
    final Settings settings = this.settings.build();
    return settings.toQuery();
  }

  protected String getTail() {
    List<String> tail =
        new ArrayList<String>() {
          {
            add(OUT);
          }
        };

    if (verbosity != null) tail.add(verbosity.sign);
    if (modifier != null) tail.add(modifier.sign);
    if (isSorted) tail.add("qt");
    if (limit != 0) tail.add(String.valueOf(limit));

    return StringUtils.join(" ", tail);
  }

  protected abstract static class Builder<Q extends Builder<Q>> {

    Settings.Builder settings = new Settings.Builder();
    Verbosity verbosity;
    Modifier modifier;
    boolean isSorted = false;
    int limit;

    public abstract Query build();

    protected abstract Q self();

    /**
     * Set header settings for query.
     *
     * @param settings Settings {@link Settings.Builder}
     * @return {@link Q}
     */
    public Q settings(Settings.Builder settings) {
      this.settings = settings;
      return self();
    }

    /**
     * Set global BoundingBox for query.
     *
     * <p>e.g. [bbox:south,west,north,east]
     *
     * @param coordinates in the format of [south, west, north, east]
     * @return {@link Q}
     */
    public Q globalBoundingBox(double[] coordinates) {
      settings.globalBoundingBox(coordinates);
      return self();
    }

    /**
     * Set OverpassApi timeout in seconds for Request.
     *
     * @param timeout integer
     * @return {@link Q}
     */
    public Q timeout(int timeout) {
      settings.timeout(timeout);
      return self();
    }

    /**
     * Set OverpassApi maximum query size in bytes for Request.
     *
     * @param size long
     * @return {@link Q}
     */
    public Q size(long size) {
      settings.size(size);
      return self();
    }

    /**
     * Set Verbosity {@link Verbosity} of Response body.
     *
     * @param verbosity Verbosity {@link Verbosity}
     * @return {@link Q}
     */
    public Q verbosity(Verbosity verbosity) {
      this.verbosity = verbosity;
      return self();
    }

    /**
     * Set Modifier {@link Modifier} for Response body.
     *
     * @param modifier {@link Modifier}
     * @return {@link Q}
     */
    public Q modifier(Modifier modifier) {
      this.modifier = modifier;
      return self();
    }

    /**
     * Use if Response body should be sorted by distance.
     *
     * @return {@link Q}
     */
    public Q sort() {
      this.isSorted = true;
      return self();
    }

    /**
     * Set a limit for the entries in the Response body.
     *
     * @param limit integer
     * @return {@link Q}
     */
    public Q limit(int limit) {
      this.limit = limit;
      return self();
    }
  }
}
