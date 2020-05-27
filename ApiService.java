import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class ApiService {

    private static String getDataFromServer(String endpoint) throws FileNotFoundException {
        try {
            URL url = new URL(Config.apiRoot + endpoint);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            String response = "";
            while((line = bufferedReader.readLine()) != null) {
                response += line;
            }

            return response;
        }
        catch (FileNotFoundException e) {
            throw new FileNotFoundException();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String getPractitionerIdentifier(String practitionerId) throws PractitionerNotFoundException {
        try {
            JSONObject practitionerObject = new JSONObject(getDataFromServer("/Practitioner/" + practitionerId + "?_format=json"));

            return practitionerObject.getJSONArray("identifier").getJSONObject(0).getString("system") +
                    "%7C" + //UTF-8 Encoding for | symbol
                    practitionerObject.getJSONArray("identifier").getJSONObject(0).getString("value");
        }
        catch (FileNotFoundException e) {
            throw new PractitionerNotFoundException("Practitioner not found");
        }
    }

    public static ArrayList<Patient> getPatients(String practitionerId) throws PractitionerNotFoundException {
        ArrayList<Patient> patients = new ArrayList<>();
        Set<String> uniquePatientIDs = new HashSet<>();

        String identifier = getPractitionerIdentifier(practitionerId);

        try {
            String encounters = getDataFromServer("/Encounter/?participant.identifier=" + identifier + "&_include=Encounter.participant.individual&_include=Encounter.patient&_format=json");

            JSONObject encountersObject = new JSONObject(encounters);

            for(int i = 0; i < encountersObject.getJSONArray("entry").length(); i++) {
                String patientResourceRef = encountersObject.getJSONArray("entry").getJSONObject(i).getJSONObject("resource").getJSONObject("subject").getString("reference");
                String patientId = patientResourceRef.split("/")[1];

                String patientName = encountersObject.getJSONArray("entry").getJSONObject(i).getJSONObject("resource").getJSONObject("subject").getString("display");

                if(uniquePatientIDs.add(patientId)) {
                    Patient patient = new Patient(patientId, patientName);
                    patients.add(patient);
                }
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return patients;
    }

    public static Map<String, String> getCholesterol(String patientId) throws ObservationNotFoundException {
        Map<String, String> cholesterolData = new HashMap<>();
        try {
            String observations = getDataFromServer("/Observation/?patient=" + patientId + "&code=2093-3&_sort=-date&_count=1&_format=json");
            JSONObject observationsObject = new JSONObject(observations);

            if (observationsObject.isNull("entry")) {
                throw new ObservationNotFoundException("Patient has no observations");
            }

            double totalCholesterol = observationsObject.
                    getJSONArray("entry").getJSONObject(0).
                    getJSONObject("resource").getJSONObject("valueQuantity").
                    getDouble("value");
            String cholesterolUnit = observationsObject.
                    getJSONArray("entry").getJSONObject(0).
                    getJSONObject("resource").getJSONObject("valueQuantity").
                    getString("unit");
            String effectiveDateTime = observationsObject.
                    getJSONArray("entry").getJSONObject(0).
                    getJSONObject("resource").getString("effectiveDateTime");

            cholesterolData.put("totalCholesterol", Double.toString(totalCholesterol));
            cholesterolData.put("cholesterolUnit", cholesterolUnit);
            cholesterolData.put("effectiveDateTime", effectiveDateTime);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
            LocalDateTime now = LocalDateTime.now();

            cholesterolData.put("updated", formatter.format(now));
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return cholesterolData;
    }

    public static Patient getPatientDetails(String patientId) {
        try {
            String patientData = getDataFromServer("/Patient/" + patientId + "/?_format=json");
            JSONObject patientObject = new JSONObject(patientData);

            String lastName = patientObject.getJSONArray("name").getJSONObject(0).getString("family");
            String firstName = patientObject.getJSONArray("name").getJSONObject(0).getJSONArray("given").getString(0);
            String gender = patientObject.getString("gender");
            String address = patientObject.getJSONArray("address").getJSONObject(0).getJSONArray("line").getString(0);
            String city = patientObject.getJSONArray("address").getJSONObject(0).getString("city");
            String state = patientObject.getJSONArray("address").getJSONObject(0).getString("state");
            String country = patientObject.getJSONArray("address").getJSONObject(0).getString("country");
            Patient patient = new Patient(patientId, firstName + " " + lastName);
            patient.setGender(gender);
            patient.setAddress(address);
            patient.setCity(city);
            patient.setState(state);
            patient.setCountry(country);

            return patient;

        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        return null;
    }
}
