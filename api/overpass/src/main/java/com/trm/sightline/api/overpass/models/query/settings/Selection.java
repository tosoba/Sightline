package com.trm.sightline.api.overpass.models.query.settings;

/**
 * Recurse clauses ("recursive filters") to combine with BaseStatements. A predecessor is needed.
 *
 * <p>e.g.
 *
 * <p>node(w); // select child nodes from all ways of the input set node(r); // select node members
 * of relations of the input set way(bn); // select parent ways for all nodes from the input set
 * way(r); // select way members of relations from the input set rel(bn); // select relations that
 * have node members from the input set rel(bw); // select relations that have way members from the
 * input set rel(r); // select all members of type relation from all relations of the input set
 * rel(br); // select all parent relations of all relations from the input set
 */
public enum Selection {
  FORWARD_NODE("n"),
  FORWARD_WAY("w"),
  FORWARD_RELATION("r"),
  BACKWARD_NODE("bn"),
  BACKWARD_WAY("bw"),
  BACKWARD_RELATION("br");

  public final String sign;

  Selection(String sign) {
    this.sign = sign;
  }
}
