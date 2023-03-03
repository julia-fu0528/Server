package CSV.RowCreators.RowCreator;

import java.util.List;

public interface CreatorFromRow<T> {
  T create(List<String> row) throws FactoryFailureException;
}
