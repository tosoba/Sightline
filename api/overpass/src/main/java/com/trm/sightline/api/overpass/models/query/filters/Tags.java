package com.trm.sightline.api.overpass.models.query.filters;

import com.trm.sightline.api.overpass.models.query.Convertible;
import com.trm.sightline.api.overpass.models.query.settings.Filter;
import com.trm.sightline.api.overpass.models.query.statements.NodeQuery;
import com.trm.sightline.api.overpass.models.query.statements.RelationQuery;
import com.trm.sightline.api.overpass.models.query.statements.WayQuery;
import com.trm.sightline.api.overpass.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Tag filters for a {@link NodeQuery}, {@link WayQuery} or {@link RelationQuery}. */
public class Tags implements Convertible<Tags.Builder> {

  public static final String REGEX_ALL = ".";
  public static final String REGEX_EMPTY = "^$";
  private final List<String> tags;

  private Tags(final Builder builder) {
    tags = builder.tags;
  }

  @Override
  public Builder toBuilder() {
    return new Tags.Builder().add(this);
  }

  @Override
  public String toQuery() {
    return StringUtils.join("", tags);
  }

  /**
   * Get {@link List} of tags.
   *
   * @return {@link List}
   */
  public List<String> getList() {
    return tags;
  }

  public static final class Builder {

    private final List<String> tags;

    public Builder() {
      tags = new ArrayList<>();
    }

    /**
     * Filter using existence of key word.
     *
     * @param key String
     * @return {@link Builder}
     */
    public Builder is(String key) {
      return add(key, null, Filter.IS);
    }

    /**
     * Filter using non existence of key word.
     *
     * @param key String
     * @return {@link Builder}
     */
    public Builder isNot(String key) {
      return add(key, null, Filter.IS_NOT);
    }

    /**
     * Filter using regular expression value case sensitive.
     *
     * @param key String
     * @param value a regular expression
     * @return {@link Builder}
     */
    public Builder like(String key, String value) {
      return add(key, value, Filter.LIKE);
    }

    /**
     * Filter using negated regular expression value case sensitive.
     *
     * @param key String
     * @param value a regular expression
     * @return {@link Builder}
     */
    public Builder notLike(String key, String value) {
      return add(key, value, Filter.NOT_LIKE);
    }

    /**
     * Filter using regular expression value case insensitive.
     *
     * @param key String
     * @param value a regular expression
     * @return {@link Builder}
     */
    public Builder ilike(String key, String value) {
      return add(key, value, Filter.ILIKE);
    }

    /**
     * Filter using negated regular expression value case insensitive.
     *
     * @param key String
     * @param value a regular expression
     * @return {@link Builder}
     */
    public Builder notILike(String key, String value) {
      return add(key, value, Filter.NOT_ILIKE);
    }

    /**
     * Filter using exact equality.
     *
     * @param key String
     * @param value String
     * @return {@link Builder}
     */
    public Builder equal(String key, String value) {
      return add(key, value, Filter.EQUAL);
    }

    /**
     * Filter using negated equality.
     *
     * @param key String
     * @param value String
     * @return {@link Builder}
     */
    public Builder notEqual(String key, String value) {
      return add(key, value, Filter.NOT_EQUAL);
    }

    /**
     * Filter using exact equality.
     *
     * @param key String
     * @param value String
     * @return {@link Builder}
     */
    public Builder add(String key, String value) {
      return add(key, value, Filter.EQUAL);
    }

    /**
     * Add collection of tags {@link Tags}.
     *
     * @param tags {@link Tags}
     * @return {@link Builder}
     */
    public Builder add(Tags tags) {
      this.tags.addAll(tags.getList());
      return this;
    }

    /**
     * Filter using a self specified Filter {@link Filter} option.
     *
     * @param key String
     * @param value String
     * @param filter {@link Filter}
     * @return {@link Tags}
     */
    public Builder add(String key, String value, Filter filter) {
      filter = filter == null ? Filter.EQUAL : filter;
      tags.add(format(key, value, filter));
      return this;
    }

    private String format(String key, String value, Filter filter) {
      validate(key, value, filter);
      String sign = hasFilter(filter) ? filter.sign : Filter.EQUAL.sign;

      switch (filter) {
        case IS:
          return String.format("[\"%s\"]", key);
        case IS_NOT:
          return String.format("[%s\"%s\"]", sign, key);
        case EQUAL:
          return String.format("[\"%s%s%s\"]", key, sign, value);
        case NOT_EQUAL:
          return String.format("[\"%s%s%s\"]", key, sign, value);
        case LIKE:
          return String.format("[\"%s%s%s\"]", key, sign, value);
        case NOT_LIKE:
          return String.format("[\"%s%s%s\"]", key, sign, value);
        case ILIKE:
          return String.format("[\"%s%s%s\", i]", key, sign, value);
        case NOT_ILIKE:
          return String.format("[\"%s%s%s\", i]", key, sign, value);
        default:
          throw new IllegalArgumentException("Filter doesn't exist!");
      }
    }

    /**
     * Get {@link Tags}.
     *
     * @return {@link Tags}
     */
    public Tags build() {
      return new Tags(this);
    }

    private void validate(String key, String value, Filter filter) {
      if (key == null) {
        throw new IllegalArgumentException("the key should never be null");
      }
      if (value == null && !isExist(filter) && !isNotExist(filter)) {
        throw new IllegalArgumentException(
            "Value can only be null when used with existence check, e.g.\n"
                + "is(<String>)\n"
                + "is(<String>, <Filter>)\n"
                + "isNot(<String>)\n"
                + "isNot(<String>, <Filter>)\n"
                + "add(<String>, null, Filter.IS)\n"
                + "add(<String>, null, Filter.IS_NOT)\n");
      }
    }

    private boolean isNotExist(Filter filter) {
      return hasFilter(filter) && Objects.equals(filter.sign, Filter.IS_NOT.sign);
    }

    private boolean isExist(Filter filter) {
      return hasFilter(filter) && Objects.equals(filter.sign, Filter.IS.sign);
    }

    private boolean hasFilter(Filter filter) {
      return filter != null;
    }
  }
}
