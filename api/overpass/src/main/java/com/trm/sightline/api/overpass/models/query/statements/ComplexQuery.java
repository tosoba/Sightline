package com.trm.sightline.api.overpass.models.query.statements;

import com.trm.sightline.api.overpass.models.query.standalone.Recursion;
import com.trm.sightline.api.overpass.models.query.standalone.Set;
import com.trm.sightline.api.overpass.models.query.statements.base.Query;
import com.trm.sightline.api.overpass.models.query.statements.base.Statement;
import com.trm.sightline.api.overpass.models.query.statements.base.Type;
import com.trm.sightline.api.overpass.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

public final class ComplexQuery extends Query {

  static final String NO_CONTENT =
      "You didn't define any GeoQuery content, please provide at "
          + "least a NodeQuery, WayQuery or RelationQuery.";
  static final String NO_PREDECESSOR_RECURSION =
      "You didn't define any Query content, please provide at "
          + "least a NodeQuery, WayQuery or RelationQuery before you define a Recursion.";
  static final String EX_NO_PREDECESSOR =
      "The first Set or Recursion in the list needs a "
          + "predecessor because it uses a Selection that refers to a input set!";

  private final List<String> sets;

  /** OverpassApi' basic structure */
  public enum Geometry {
    NODE,
    WAY,
    RELATION
  }

  private ComplexQuery(Builder builder) {
    super(builder);
    this.sets = builder.sets;
  }

  @Override
  public String toQuery() {
    return getHead() + SPACE + getContent() + SPACE + getTail() + SPACE;
  }

  @Override
  protected String getContent() {
    if (sets.isEmpty()) throw new IllegalArgumentException(NO_CONTENT);
    return StringUtils.join(SPACE, sets);
  }

  public static class Builder extends Query.Builder<Builder> {

    private static final char[] CHARS = "abcdefghijklmnopqrstuvwxyz".toCharArray();
    private final List<String> sets = new ArrayList<>();

    @Override
    public ComplexQuery build() {
      return new ComplexQuery(this);
    }

    @Override
    protected Builder self() {
      return this;
    }

    /**
     * Add a {@link NodeQuery} as a {@link Statement} to the Request body. <br>
     * <b>Attention</b>: input order matters!
     *
     * @param nodeQuery {@link NodeQuery}
     * @return {@link Builder}
     */
    public Builder node(NodeQuery nodeQuery) {
      return add(nodeQuery);
    }

    /**
     * Add a {@link NodeQuery} as a {@link Statement} to the Request body. <br>
     * <b>Attention</b>: input order matters!
     *
     * @param nodeQuery {@link NodeQuery}
     * @param resultSet {@link Set}
     * @return {@link Builder}
     */
    public Builder node(WayQuery nodeQuery, Set resultSet) {
      return add(nodeQuery, resultSet);
    }

    /**
     * Add a {@link WayQuery} as a {@link Statement} to the Request body. <br>
     * <b>Attention</b>: input order matters!
     *
     * @param wayQuery {@link WayQuery}
     * @return {@link Builder}
     */
    public Builder way(WayQuery wayQuery) {
      return add(wayQuery);
    }

    /**
     * Add a {@link WayQuery} as a {@link Statement} to the Request body. <br>
     * <b>Attention</b>: input order matters!
     *
     * @param wayQuery {@link WayQuery}
     * @param resultSet Set {@link Set}
     * @return {@link Builder}
     */
    public Builder way(WayQuery wayQuery, Set resultSet) {
      return add(wayQuery, resultSet);
    }

    /**
     * Add a relationQuery {@link RelationQuery} as a Statement {@link Statement} to the Request
     * body. <br>
     * <b>Attention</b>: input order matters!
     *
     * @param relationQuery {@link RelationQuery}
     * @return {@link Builder}
     */
    public Builder relation(RelationQuery relationQuery) {
      return add(relationQuery);
    }

    /**
     * Add a {@link RelationQuery} as a {@link Statement} to the Request body. <br>
     * <b>Attention</b>: input order matters!
     *
     * @param relationQuery {@link RelationQuery}
     * @param resultSet {@link Set}
     * @return {@link Builder}
     */
    public Builder relation(RelationQuery relationQuery, Set resultSet) {
      return add(relationQuery, resultSet);
    }

    /**
     * Add a recurse up relations query {@link Recursion} to the Request body. The recurse up
     * relations standalone query has a similar syntax to the recurse up query and differs only in
     * two aspects: <br>
     * - It is written as a double less than. <br>
     * - It also recursively returns all relations that have a member relation appearing in the
     * input set. <br>
     * <b>Attention</b>: input order matters! <br>
     * e.g. {@code <<}
     *
     * @return {@link Builder}
     */
    public Builder recurseUpRelation() {
      return recursion(Recursion.DOUBLE_UP);
    }

    /**
     * Add a recurse up standalone query {@link Recursion} to the Request body. The recurse up
     * standalone query is written as a single less than. It takes an input set. It produces a
     * result set. Its result set is composed of: <br>
     * - all ways that have a node which appears in the input set; plus <br>
     * - all relations that have a node or way which appears in the input set; plus <br>
     * - all relations that have a way which appears in the result set <br>
     * <b>Attention</b>: input order matters! <br>
     * e.g. {@code <}
     *
     * @return {@link Builder}
     */
    public Builder recurseUp() {
      return recursion(Recursion.UP);
    }

    /**
     * Add a recurse down relations query {@link Recursion} to the Request body. The recurse down
     * relations standalone query has a similar syntax to the recurse down query and differs only in
     * two aspects: <br>
     * - It is written as a double greater than. <br>
     * -It also recursively returns all relations that are members of a relation appearing in the
     * input set. <br>
     * <b>Attention</b>: input order matters! <br>
     * e.g. {@code >>}
     *
     * @return {@link Builder}
     */
    public Builder recurseDownRelation() {
      return recursion(Recursion.DOUBLE_DOWN);
    }

    /**
     * Add a recurse down standalone query {@link Recursion} to the Request body. The recurse down
     * standalone query is written as a single greater than. It takes an input set. It produces a
     * result set. Its result set is composed of: <br>
     * - all nodes that are part of a way which appears in the input set; plus <br>
     * - all nodes and ways that are members of a relation which appears in the input set; plus <br>
     * - all nodes that are part of a way which appears in the result set <br>
     * <b>Attention</b>: input order matters! <br>
     * e.g. {@code >}
     *
     * @return {@link Builder}
     */
    public Builder recurseDown() {
      return recursion(Recursion.DOWN);
    }

    /**
     * Add a {@link Recursion} to the Request body. <br>
     * <b>Attention</b>: input order matters!
     *
     * @param recursion {@link Recursion}
     * @return {@link Builder}
     */
    public Builder recursion(Recursion recursion) {
      if (sets.isEmpty()) throw new IllegalArgumentException(NO_PREDECESSOR_RECURSION);
      sets.add(recursion.create());
      return self();
    }

    /**
     * Add a {@link Statement} to the Request body. A {@link Statement} can be either a {@link
     * NodeQuery}, a {@link WayQuery}, a {@link RelationQuery}, a {@link Recursion} or a {@link
     * Set}. <br>
     * <b>Attention</b>: input order matters!
     *
     * @param statement {@link Statement}
     * @return {@link Builder}
     */
    public Builder add(Statement statement) {
      return add(statement, null);
    }

    /**
     * Add a {@link Statement} to the Request body. A {@link Statement} can be either a {@link
     * NodeQuery}, a {@link WayQuery}, a {@link RelationQuery}, a {@link Recursion} or a {@link
     * Set}. <br>
     * <b>Attention</b>: input order matters!
     *
     * @param statement {@link Statement}
     * @param resultSet {@link Set}
     * @return {@link Builder}
     */
    public Builder add(Statement statement, Set resultSet) {
      validateFirstStatement(statement);
      String expression = statement.create();
      this.sets.add(resultSet != null ? resultSet.assign(expression) : expression);
      return self();
    }

    /**
     * Add a List of {@link Statement} to the Request body. A {@link Statement} can be either a
     * {@link NodeQuery}, a {@link WayQuery}, a {@link RelationQuery}, a {@link Recursion} or a
     * {@link Set}. <br>
     * <b>Attention</b>: list input order matters!
     *
     * @param statements {@link List}
     * @return {@link Builder}
     */
    public Builder addAll(List<Statement> statements) {
      validateFirstStatement(statements.get(0));
      for (Statement statement : statements) {
        sets.add(statement.create());
      }
      return self();
    }

    /**
     * Difference between two {@link Statement} sets as one {@link Statement} in the Request body. A
     * {@link Statement} can be either a {@link NodeQuery}, a {@link WayQuery}, a {@link
     * RelationQuery}, a {@link Recursion} or a {@link Set}. <br>
     * <b>Attention</b>: list input order matters! <br>
     * OverpassApi equivalent: {@code (node[name="Foo"]; - node(50.0,7.0,51.0,8.0););}
     *
     * @param statement1 {@link Statement}
     * @param statement2 {@link Statement}
     * @return {@link Builder}
     */
    public Builder difference(Statement statement1, Statement statement2) {
      return difference(statement1, statement2, null);
    }

    /**
     * Difference between two {@link Statement} sets as one {@link Statement} in the Request body as
     * a {@link Set}. A {@link Statement} can be either a {@link NodeQuery}, a {@link WayQuery}, a
     * {@link RelationQuery}, a {@link Recursion} or a {@link Set}. <br>
     * <b>Attention</b>: list input order matters! <br>
     * OverpassApi equivalent: {@code (node[name="Foo"]; - node(50.0,7.0,51.0,8.0);)->.c;}
     *
     * @param statement1 {@link Statement}
     * @param statement2 {@link Statement}
     * @param resultSet {@link Set}
     * @return {@link Builder}
     */
    public Builder difference(Statement statement1, Statement statement2, Set resultSet) {
      validateFirstStatement(statement1);
      validateFirstStatement(statement2);
      String expression = String.format("(%s; - %s;)", statement1.create(), statement2.create());
      expression = resultSet != null ? resultSet.assign(expression) : expression;
      sets.add(expression);
      return self();
    }

    /**
     * Intersection two or more {@link Statement} sets to one {@link Statement} in the Request body.
     * A {@link Statement} can be either a {@link NodeQuery}, a {@link WayQuery}, a {@link
     * RelationQuery}, a {@link Recursion} or a {@link Set}. <br>
     * Attention: list input order matters! <br>
     * OverpassApi equivalent: {@code node[name="Foo"]->.a; node(50.0,7.0,51.0,8.0)->.b;node.a.b;}
     *
     * @param statements {@link List}
     * @return {@link Builder}
     */
    public Builder intersection(Geometry geometry, List<Statement> statements) {
      return intersection(geometry, statements, null);
    }

    /**
     * Intersection two or more {@link Statement} sets to one {@link Statement} in the Request body
     * as a {@link Set}. A {@link Statement} can be either a {@link NodeQuery}, a {@link WayQuery},
     * a {@link RelationQuery}, a {@link Recursion} or a {@link Set}. <br>
     * Attention: list input order matters! <br>
     * OverpassApi equivalent: {@code node[name="Foo"]->.a;
     * node(50.0,7.0,51.0,8.0)->.b;node.a.b->.c;}
     *
     * @param statements {@link List}
     * @param resultSet {@link Set}
     * @return {@link Builder}
     */
    public Builder intersection(Geometry geometry, List<Statement> statements, Set resultSet) {
      validateFirstStatement(statements.get(0));
      StringBuilder sb = new StringBuilder();
      List<String> setResults = new ArrayList<>();
      int i = 0;
      for (Statement statement : statements) {
        String random = getSetName(i);
        sb.append(String.format("%s->.%s;", statement.create(), random));
        setResults.add(random);
        i++;
      }
      String type;
      switch (geometry) {
        case NODE:
          type = Type.NODE;
          break;
        case WAY:
          type = Type.WAY;
          break;
        case RELATION:
          type = Type.RELATION;
          break;
        default:
          type = Type.NODE;
      }
      sb.append(String.format("%s.%s", type, StringUtils.join(".", setResults)));
      String expression = sb.toString();
      expression = resultSet != null ? resultSet.assign(expression) : expression;
      sets.add(expression);
      return self();
    }

    /**
     * Unite two or more {@link Statement} sets to one {@link Statement} in the Request body. A
     * {@link Statement} can be either a {@link NodeQuery}, a {@link WayQuery}, a {@link
     * RelationQuery}, a {@link Recursion} or a {@link Set}. <br>
     * Attention: list input order matters! <br>
     * OverpassApi equivalent: {@code (node[name="Foo"];way[name="Foo"];);}
     *
     * @param statement1 {@link Statement}
     * @param statement2 {@link Statement}
     * @return {@link Builder}
     */
    public Builder union(Statement statement1, Statement statement2) {
      List<Statement> statements = new ArrayList<>();
      statements.add(statement1);
      statements.add(statement2);
      return union(statements, null);
    }

    /**
     * Unite two or more {@link Statement} sets to one {@link Statement} in the Request body. A
     * {@link Statement} can be either a {@link NodeQuery}, a {@link WayQuery}, a {@link
     * RelationQuery}, a {@link Recursion} or a {@link Set}. <br>
     * Attention: list input order matters! <br>
     * OverpassApi equivalent: {@code (node[name="Foo"];way[name="Foo"];);}
     *
     * @param statements {@link List}
     * @return {@link Builder}
     */
    public Builder union(List<Statement> statements) {
      return union(statements, null);
    }

    /**
     * Unite two or more {@link Statement} sets to one {@link Statement} in the Request body as a
     * {@link Set}. A {@link Statement} can be either a {@link NodeQuery}, a {@link WayQuery}, a
     * {@link RelationQuery}, a {@link Recursion} or a {@link Set}. <br>
     * Attention: list input order matters! <br>
     * OverpassApi equivalent: {@code (node[name="Foo"];way[name="Foo"];)->.c;}
     *
     * @param statements {@link List}
     * @param resultSet {@link Set}
     * @return {@link Builder}
     */
    public Builder union(List<Statement> statements, Set resultSet) {
      validateFirstStatement(statements.get(0));
      StringBuilder sb = new StringBuilder();
      sb.append("(");
      for (Statement statement : statements) {
        sb.append(String.format("%s;", statement.create()));
      }
      sb.append(")");
      String expression = sb.toString();
      expression = resultSet != null ? resultSet.assign(expression) : expression;
      sets.add(expression);
      return self();
    }

    private void validateFirstStatement(Statement statement) {
      if (this.sets.isEmpty() && statement.isSuccessor())
        throw new IllegalArgumentException(EX_NO_PREDECESSOR);
    }

    private String getSetName(int start) {
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 2; i++) {
        char c = CHARS[start + i];
        sb.append(c);
      }
      return sb.toString();
    }
  }
}
