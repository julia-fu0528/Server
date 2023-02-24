package Person;

import Person.Person;
import RowCreator.CreatorFromRow;
import RowCreator.FactoryFailureException;

import java.util.List;


/**
 * Creates usable class that implements CreatorFromRow interface
 * This way CSv can parse into different data types
 *
 */
public class PersonCreator implements CreatorFromRow {

    /**
     * Creates the correct data type to be output given the input (in this cas the same List of Strings input)
     */
    @Override
    public Object create(List row) throws FactoryFailureException {
        String name =  (String) row.get(0);
        int year = Integer.parseInt( (String) row.get(1));
        String hometown =  (String) row.get(2);
        String major =  (String) row.get(3);
        return new Person(name, year, hometown, major);
    }

}
