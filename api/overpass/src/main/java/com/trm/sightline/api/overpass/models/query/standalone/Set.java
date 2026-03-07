package com.trm.sightline.api.overpass.models.query.standalone;

import com.trm.sightline.api.overpass.models.query.statements.base.Statement;

/**
 * A Set is a special case Statement {@link Statement}. It can be assigned to other Statements and
 * later on recalled.
 */
public class Set implements Statement {

  public static final String DEFAULT_SET = "_";
  private final String name;
  private boolean assigned = false;

  /**
   * Construct a Set by giving it a name it can be recalled later on.
   *
   * @param name String
   */
  public Set(String name) {
    this.name = name;
    if (name.equals(DEFAULT_SET)) assigned = true;
  }

  /**
   * Assign Set to a temporary result. All Sets except the default must be assigned to be created.
   *
   * @param expression String
   * @return String
   */
  public String assign(String expression) {
    assigned = true;
    return String.format("%s->.%s", expression, name);
  }

  @Override
  public boolean isSuccessor() {
    return true;
  }

  /**
   * Recall already assigned Set.
   *
   * @return String
   */
  @Override
  public String create() {
    return String.format(".%s", name);
  }

  public boolean isAssigned() {
    return assigned;
  }
}
