public class Patient {
    private String id;
    private String name;
    private String gender;
    private String address;
    private String city;
    private String state;
    private String country;
    private double totalCholesterol;
    private String cholesterolEffectiveDate;
    private String cholesterolLastUpdated;

    public Patient(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getGender() {
        return gender;
    }

    public String getAddress() {
        return address;
    }

    public String getCountry() {
        return country;
    }

    public double getTotalCholesterol() {
        return totalCholesterol;
    }

    public String getCholesterolEffectiveDate() {
        return cholesterolEffectiveDate;
    }

    public String getCholesterolLastUpdated() {
        return cholesterolLastUpdated;
    }

    public String getCity() {
        return city;
    }

    public String getState() {
        return state;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public void setState(String state) {
        this.state = state;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public void setTotalCholesterol(double totalCholesterol) {
        this.totalCholesterol = totalCholesterol;
    }

    public void setCholesterolEffectiveDate(String cholesterolEffectiveDate) {
        this.cholesterolEffectiveDate = cholesterolEffectiveDate;
    }

    public void setCholesterolLastUpdated(String cholesterolLastUpdated) {
        this.cholesterolLastUpdated = cholesterolLastUpdated;
    }
}
