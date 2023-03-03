package CSV.RowCreators.Person;

public class Person {

    private String name;
    private int year;
    private String hometown;
    private String major;

    public Person(String name, int year, String hometown, String major){
        this.name = name;
        this.year = year;
        this.hometown = hometown;
        this.major = major;
    }

    @Override
    public boolean equals (Object p1){
        Person p2 = (Person) p1;
        if (this.name.equals(p2.getName()) && this.year == p2.getYear() && this.major.equals(p2.getMajor())
        && this.hometown.equals(p2.getHometown())){
            return true;
        }
        else{
            return false;
        }
    }

    public String getName(){
        return this.name;
    }

    public int getYear(){
        return this.year;
    }

    public String getHometown(){
        return this.hometown;
    }

    public String getMajor(){
        return this.major;
    }
}
