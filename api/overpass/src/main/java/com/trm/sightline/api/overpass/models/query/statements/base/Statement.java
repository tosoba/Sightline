package com.trm.sightline.api.overpass.models.query.statements.base;

public interface Statement {

  /**
   * Method returns weather the {@link Statement} needs a Predecessor or not.
   *
   * @return boolean is true if {@link Statement} needs a Predecessor.
   */
  boolean isSuccessor();

  /**
   * Method returns String containing partial query.
   *
   * @return String
   */
  String create();
}
