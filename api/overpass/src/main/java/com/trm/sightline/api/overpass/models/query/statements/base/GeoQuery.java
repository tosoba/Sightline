package com.trm.sightline.api.overpass.models.query.statements.base;

import com.trm.sightline.api.overpass.models.query.filters.SpatialSearch;
import com.trm.sightline.api.overpass.models.query.filters.Tags;
import com.trm.sightline.api.overpass.models.query.settings.Filter;
import com.trm.sightline.api.overpass.models.query.settings.Selection;
import com.trm.sightline.api.overpass.models.query.settings.SpatialSearchType;
import com.trm.sightline.api.overpass.models.query.standalone.Set;
import com.trm.sightline.api.overpass.utils.StringUtils;
import java.util.ArrayList;
import java.util.List;

public abstract class GeoQuery extends Query implements Statement {

  public static final String EX_WRONG_SCHEMA = "Selection doesn't fit scheme";
  public static final String EX_SELECTION_AREA = "Selection doesn't fit to SpatialSearchType AREA";
  public static final String EX_NOT_ASSIGNED = "You can't use the Set before assignment!";
  private static final String AREA = "(area)";
  private final List<String> queries;
  protected final Selection selection;
  protected final Tags.Builder tags;
  protected final SpatialSearch region;
  protected final Set set;
  protected final boolean isSuccessor;
  private final boolean mapToArea;
  protected String type;

  protected GeoQuery(Builder<?> builder) {
    super(builder);
    type = builder.type;
    selection = builder.selection;
    tags = builder.tags;
    region = builder.region;
    set = builder.set;
    isSuccessor = builder.isSuccessor;
    mapToArea = builder.mapToArea;
    queries = new ArrayList<>();
  }

  @Override
  public String toQuery() {
    return getHead() + SPACE + getContent() + SPACE + getTail() + SPACE;
  }

  @Override
  protected String getContent() {
    return create();
  }

  @Override
  public String create() {
    handleSet();
    queries.add(hasSelection() ? String.format("(%s)", selection.sign) : "");
    queries.add(tags.build().toQuery());
    return hasRegion() && isArea() ? typeArea() : standard();
  }

  @Override
  public boolean isSuccessor() {
    return isSuccessor;
  }

  private void handleSet() {
    if (set != null && !set.isAssigned()) {
      throw new IllegalArgumentException(EX_NOT_ASSIGNED);
    } else if (set != null && set.isAssigned()) {
      this.type = String.format("%s%s", type, set.create());
    }
  }

  private boolean isArea() {
    return region.getType() == SpatialSearchType.AREA;
  }

  private String typeArea() {
    queries.add(mapToArea ? ";map_to_area" : "");
    return region.create() + ";" + type + AREA + StringUtils.join("", queries);
  }

  private String standard() {
    queries.add(hasRegion() ? region.create() : "");
    queries.add(mapToArea ? ";map_to_area" : "");
    return type + StringUtils.join("", queries);
  }

  private boolean hasRegion() {
    return region != null;
  }

  private boolean hasSelection() {
    return selection != null;
  }

  public abstract static class Builder<T extends Builder<T>> extends Query.Builder<T> {
    private final String type;
    private final Tags.Builder tags;
    private SpatialSearch region;
    private Set set;
    protected Selection selection;
    private boolean isSuccessor;
    private boolean mapToArea = false;

    public Builder(String type) {
      this.type = type;
      this.tags = new Tags.Builder();
    }

    @Override
    protected abstract T self();

    public T is(String key) {
      tag(key, null, Filter.IS);
      return self();
    }

    public T isNot(String key) {
      tag(key, null, Filter.IS_NOT);
      return self();
    }

    public T like(String key, String value) {
      tag(key, value, Filter.LIKE);
      return self();
    }

    public T notLike(String key, String value) {
      tag(key, value, Filter.NOT_LIKE);
      return self();
    }

    public T ilike(String key, String value) {
      tag(key, value, Filter.ILIKE);
      return self();
    }

    public T notILike(String key, String value) {
      tag(key, value, Filter.NOT_ILIKE);
      return self();
    }

    public T equal(String key, String value) {
      tag(key, value, Filter.EQUAL);
      return self();
    }

    public T notEqual(String key, String value) {
      tag(key, value, Filter.NOT_EQUAL);
      return self();
    }

    public T tag(String key, String value) {
      tag(key, value, null);
      return self();
    }

    public T tag(String key, String value, Filter filter) {
      this.tags.add(key, value, filter);
      return self();
    }

    public T tags(Tags tags) {
      this.tags.add(tags);
      return self();
    }

    public T area(String area) {
      this.region = new SpatialSearch(SpatialSearchType.AREA, area);
      return self();
    }

    public T around(double lat, double lon, float radius) {
      this.region = new SpatialSearch(SpatialSearchType.AROUND, lat, lon, radius);
      return self();
    }

    public T boundingBox(double[] coordinates) {
      this.region = new SpatialSearch(SpatialSearchType.BOUNDING_BOX, coordinates);
      return self();
    }

    public T polygon(double[][] coordinates) {
      this.region = new SpatialSearch(SpatialSearchType.POLYGON, coordinates);
      return self();
    }

    public T region(SpatialSearch region) {
      this.region = region;
      return self();
    }

    public T set(Set set) {
      this.set = set;
      return self();
    }

    public T select(Selection selection) {
      this.isSuccessor = true;
      this.selection = selection;
      validate();
      return self();
    }

    public T mapToArea() {
      this.mapToArea = true;
      return self();
    }

    protected void validate() {
      if (hasSelection() && hasRegion() && isArea()) {
        throw new IllegalArgumentException(EX_SELECTION_AREA);
      }
    }

    private boolean hasSelection() {
      return selection != null;
    }

    private boolean hasRegion() {
      return region != null;
    }

    private boolean isArea() {
      return region.getType() == SpatialSearchType.AREA;
    }
  }
}
