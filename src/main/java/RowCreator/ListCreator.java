package RowCreator;

import java.util.List;


/**
 * Creates usable class that implements CreatorFromRow interface
 * This way CSv can parse into different data types
 *
 */
public class ListCreator implements CreatorFromRow {

  /**
   * Creates the correct data type to be output given the input (in this cas the same List of Strings input)
   */
  @Override
  public Object create(List row) throws FactoryFailureException {
    return row;
  }
}
