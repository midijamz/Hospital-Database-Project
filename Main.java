import java.io.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class Main {

    private static java.sql.Connection dbConnection = null;
    private static Main instance = null;
    final String dbURL = "jdbc:mysql://localhost:3306/hospital_system";
    final String dbUsername = "root";
    final String dbPassword = "admin";
    static final String filePerson = "C:\Hospital System\\person.txt";
    static final String fileDoctor = "C:\Hospital System\\doctor.txt";
    static final String fileTreatments = "C:\Hospital System\\treatments.txt";

    public static void main(String[] args) throws SQLException, ClassNotFoundException, IOException {

        //connect to database
        Main db = Main.getInstance();
        List<String> sqlQueries = new ArrayList<>();
        Formatter formatter = new Formatter();
        Scanner input = new Scanner(System.in);
        int userMenuChoice = -1;
        String userContinueChoice = "yes";
        //read content from file
        db.readPersonFile(filePerson);
        db.readDoctorFile(fileDoctor);
        db.readTreatmentsFile(fileTreatments);

        //add queries
        sqlQueries = addQueries();
        //get user input for queries
        while(userContinueChoice.equalsIgnoreCase("yes")) {
            System.out.println("\n\n\n\n\n\nEnter the number of the SQL query category: ");
            System.out.println("1. Room Utilization\n2. Patient Information\n3. Diagnosis and Treatment Information\n4. Employee Information\n");

            try {
                userMenuChoice = Integer.parseInt(input.next());
                if (userMenuChoice < 1 || userMenuChoice > 4) {
                    System.out.println("Invalid input");
                    continue;
                }
            } catch (Exception e) {
                System.out.println("Invalid input");
                continue;
            }

            System.out.println("Enter the number of the SQL query to print its output: ");
            switch (userMenuChoice) {
                case 1 -> printRoomUtilizationPrompt();
                case 2 -> printPatientInfoPrompt();
                case 3 -> printDiagnosisTreatmentInfoPrompt();
                case 4 -> printEmployeeInfoPrompt();
            }

            try {
                userMenuChoice = Integer.parseInt(input.next());
                if (userMenuChoice < 1 || userMenuChoice > 21) {
                    System.out.println("Invalid input");
                    continue;
                }
            } catch (Exception e) {
                System.out.println("Invalid input");
                continue;
            }

            //print results to console
            try {
                Statement statement = dbConnection.createStatement();
                ResultSet resultSet = statement.executeQuery(sqlQueries.get(userMenuChoice - 1));
                ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
                int columnCount = resultSetMetaData.getColumnCount();
                // first loop to get all tuples
                while(resultSet.next()) {
                    // second loop to get all columns in each tuple
                    for(int i=1; i<=columnCount; i++) {
                        String column_title = resultSetMetaData.getColumnName(i);
                        System.out.print(column_title + ": ");
                        // Data type object since the columns can be int, string, or datetime
                        Object column_value = resultSet.getObject(column_title);
                        System.out.println(column_value + " ");
                    }
                    System.out.println("");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            //prompt user to continue or quit
            System.out.println("\nContinue? (Yes/No)");
            userContinueChoice = input.next();
            if (!userContinueChoice.equalsIgnoreCase("yes")) {
                break;
            }
        }
    }



    public Main() {

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            dbConnection = DriverManager.getConnection(dbURL, dbUsername, dbPassword);
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
        }
    }

    private static Main getInstance() {

        if (instance == null) {
            instance = new Main();
        }
        return instance;
    }

    //utility methods

    private void readPersonFile(String fileURL) throws IOException, SQLException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileURL));
        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
            String[] personData = line.trim().split(",");
            if (personData[0].equals("P")) {
                insertPatient(line);
            } else if (personData[0].equals("D")) {
                insertEmployee(line);
            }
        }
    }

    private void insertEmployee(String line) throws SQLException {

        String[] employeeData = line.trim().split(",");
        String firstName = employeeData[1].trim();
        String lastName = employeeData[2].trim();

        //check if employee already exist in db
        if (checkIfEmployeeExistInDB(lastName)) {
            System.out.println("Insert error. Employee " + firstName + " " + lastName + " already exist in database");
        } else {
            String category = switch(employeeData[0]) {
                case "D" -> "doctor";
                case "A" -> "administrator";
                case "N" -> "nurse";
                case "T" -> "technician";
                default -> "";
            };

            String employeeInsertQuery = "INSERT INTO employees VALUES(default, ?, ?, ?)";
            java.sql.PreparedStatement preparedStatement = dbConnection.prepareStatement(employeeInsertQuery);
            preparedStatement.setString(1,firstName);
            preparedStatement.setString(2,lastName);
            preparedStatement.setString(3,category);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
    }

    private boolean checkIfEmployeeExistInDB(String lastName) throws SQLException {

        boolean flag = false;
        String checkEmployee = "SELECT * FROM employees WHERE lastname = ?";
        java.sql.PreparedStatement preparedStatement = dbConnection.prepareStatement(checkEmployee);
        preparedStatement.setString(1,lastName);
        java.sql.ResultSet resultSet = preparedStatement.executeQuery();
        if(resultSet.next()) {
            flag = true;
        }
        return flag;
    }

    private void insertPatient(String line) throws SQLException {

        String[] patientData = line.trim().split(",");
        String firstName = patientData[1].trim();
        String lastName = patientData[2].trim();
        int patientID = -1;

        //check if patient already exist in db
        if (checkIfPatientExistInDB(lastName)) {
            System.out.println("Insert error. Patient " + firstName + " " + lastName + " already exist in database");
        } else {
            int roomNumber = Integer.parseInt(patientData[3].trim());
            int emergencyContactID = insertEmergencyContact(patientData[4], patientData[5].trim());
            int insuranceID = getInsuranceDetails(patientData[6].trim(), patientData[7].trim());
            int primaryDoctor = getDoctorFromLastName(patientData[8].trim());
            String initialDiagnosis = patientData[9].trim();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm", Locale.ENGLISH);
            LocalDateTime arrivalDate = LocalDateTime.parse(patientData[10].trim(), formatter);
            LocalDateTime dischargeDate = LocalDateTime.parse(patientData[11].trim(), formatter);

            String patientInsertQuery = "INSERT INTO patients VALUES(default, ?, ?, ?, ?, ?, ?)";
            PreparedStatement preparedStatement = dbConnection.prepareStatement(patientInsertQuery, Statement.RETURN_GENERATED_KEYS);
            preparedStatement.setString(1, firstName);
            preparedStatement.setString(2, lastName);
            preparedStatement.setInt(3, roomNumber);
            preparedStatement.setInt(4, insuranceID);
            preparedStatement.setInt(5, emergencyContactID);
            preparedStatement.setInt(6, primaryDoctor);
            preparedStatement.executeUpdate();
            ResultSet resultSet = preparedStatement.getGeneratedKeys();
            if(resultSet.next()) {
                patientID = resultSet.getInt(1);
            }
            preparedStatement.close();
            resultSet.close();

            String insertAdmittedPatients = "INSERT INTO admitted_patients VALUES(default, ?, ?, ?, ?)";
            PreparedStatement ps = dbConnection.prepareStatement(insertAdmittedPatients);
            ps.setInt(1, patientID);
            ps.setString(2, initialDiagnosis);
            ps.setObject(3, arrivalDate);
            ps.setObject(4, dischargeDate);
            ps.executeUpdate();
            ps.close();
        }
    }

    private boolean checkIfPatientExistInDB(String lastName) throws SQLException {

        boolean flag = false;
        String checkPatient = "SELECT * FROM patients WHERE lastname = ?";
        java.sql.PreparedStatement preparedStatement = dbConnection.prepareStatement(checkPatient);
        preparedStatement.setString(1,lastName);
        java.sql.ResultSet resultSet = preparedStatement.executeQuery();
        if(resultSet.next()) {
            flag = true;
        }
        return flag;
    }

    private int getPatientFromLastName(String lastname) throws SQLException {

        int patientID = -1;
        String doctorQuery = "SELECT patient_id from patients WHERE lastname = ?";
        PreparedStatement preparedStatement = dbConnection.prepareStatement(doctorQuery);
        preparedStatement.setString(1, lastname);
        ResultSet resultSet = preparedStatement.executeQuery();
        if(resultSet.next()) {
            patientID = resultSet.getInt("patient_id");
        }
        preparedStatement.close();
        resultSet.close();
        return patientID;
    }

    private int getInsuranceDetails(String policy_number, String company_name) throws SQLException {

        int insurance_id = -1;
        String insertInsuranceQuery = "INSERT INTO insurance VALUES(default, ?, ?)";
        PreparedStatement preparedStatement = dbConnection.prepareStatement(insertInsuranceQuery, Statement.RETURN_GENERATED_KEYS);
        preparedStatement.setString(1, policy_number);
        preparedStatement.setString(2, company_name);
        preparedStatement.executeUpdate();
        ResultSet resultSet = preparedStatement.getGeneratedKeys();
        if(resultSet.next()) {
            insurance_id = resultSet.getInt(1);
        }
        preparedStatement.close();
        resultSet.close();
        return insurance_id;
    }

    private int insertEmergencyContact(String name, String phone) throws SQLException {

        int contact_id = -1;
        String insertEmergencyContactQuery = "INSERT INTO emergency_contacts VALUES(default, ?, ?)";
        PreparedStatement preparedStatement = dbConnection.prepareStatement(insertEmergencyContactQuery, Statement.RETURN_GENERATED_KEYS);
        preparedStatement.setString(1, name);
        preparedStatement.setString(2, phone);
        preparedStatement.executeUpdate();
        ResultSet resultSet = preparedStatement.getGeneratedKeys();
        if(resultSet.next()) {
            contact_id = resultSet.getInt(1);
        }
        preparedStatement.close();
        resultSet.close();
        return contact_id;
    }

    private void readDoctorFile(String fileURL) throws SQLException, IOException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileURL));
        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
            String[] doctorData = line.trim().split(",");
            insertDoctor(line);
        }
    }

    private void insertDoctor(String line) throws SQLException {

        String[] doctorData = line.trim().split(",");
        String patientLastName = doctorData[0].trim();
        String doctorLastName = doctorData[1].trim();
        int patientID = getPatientFromLastName(patientLastName);
        int doctorID = getDoctorFromLastName(doctorLastName);

        //check if doctor already exist in db
        if (doctorID == -1 || patientID == -1) {
            System.out.println("Insert error. Employee " + doctorLastName + " already exist in database");
        } else {
            String doctorInsertQuery = "INSERT INTO assigned_doctors VALUES(?, ?)";
            java.sql.PreparedStatement preparedStatement = dbConnection.prepareStatement(doctorInsertQuery);
            preparedStatement.setInt(1,patientID);
            preparedStatement.setInt(2,doctorID);
            preparedStatement.executeUpdate();
            preparedStatement.close();
        }
    }

    private int getDoctorFromLastName(String lastname) throws SQLException {

        int employee_id = -1;
        String doctorQuery = "SELECT employee_id from employees WHERE lastname = ?";
        PreparedStatement preparedStatement = dbConnection.prepareStatement(doctorQuery);
        preparedStatement.setString(1, lastname);
        ResultSet resultSet = preparedStatement.executeQuery();
        if(resultSet.next()) {
            employee_id = resultSet.getInt("employee_id");
        }
        preparedStatement.close();
        resultSet.close();
        return employee_id;
    }

    private void readTreatmentsFile(String fileURL) throws IOException, SQLException {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(fileURL));
        String line = "";
        while ((line = bufferedReader.readLine()) != null) {
            String[] treatmentData = line.trim().split(",");
            insertTreatments(line);
        }
    }

    private void insertTreatments(String line) throws SQLException {

        String[] treatmentData = line.trim().split(",");
        String patientLastName = treatmentData[0].trim();
        String employeeLastName = treatmentData[1].trim();
        int patientID = getPatientFromLastName(patientLastName);
        int doctorID = getDoctorFromLastName(employeeLastName);
        String treatmentType = treatmentData[2];
        String treatmentName = treatmentData[3];

        //DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-M-d HH:mm", Locale.ENGLISH);
        //LocalDateTime timeStamp = LocalDateTime.parse(treatmentData[4], formatter);

        //check if doctor does not exist
        if (doctorID == -1 || patientID == -1) {
            System.out.println("Insert error. Employee " + employeeLastName + " does not exist in database");
        } else {
            String treatmentInsertQuery = "INSERT INTO treatments VALUES(default, ?, ?, ?, ?, ?)";
            java.sql.PreparedStatement preparedStatement = dbConnection.prepareStatement(treatmentInsertQuery);
            preparedStatement.setInt(1,patientID);
            preparedStatement.setInt(2,doctorID);
            preparedStatement.setString(3,treatmentName);
            preparedStatement.setString(4,treatmentType);
            preparedStatement.setObject(5,treatmentData[4]);
            preparedStatement.executeUpdate();
            preparedStatement.close();
       }
    }

    private static List<String> addQueries() {
        List<String> sqlQueries = new ArrayList<>();
        sqlQueries.add(0, "SELECT patients.firstname, patients.lastname, patients.room_number, admitted_patients.arrival_date FROM admitted_patients, patients WHERE patients.patient_id = admitted_patients.patient_id AND discharge_date IS NULL");
        sqlQueries.add(1, "SELECT * FROM (VALUES ROW (1) , ROW (2) , ROW (3), ROW (4), ROW (5), ROW (6), ROW (7), ROW (8), ROW (9), ROW (10), ROW (11), ROW (12), ROW (13), ROW (14), ROW (15), ROW (16), ROW (17), ROW (18), ROW(19), ROW (20)) R(room_number) WHERE NOT EXISTS (SELECT room_number FROM patients, admitted_patients WHERE patients.room_number = R.room_number AND patients.patient_id = admitted_patients.patient_id AND discharge_date IS NULL);");
        sqlQueries.add(2, "SELECT patients.firstname, patients.lastname, patients.room_number, admitted_patients.arrival_date " +
                "FROM admitted_patients, patients " +
                "WHERE patients.patient_id = admitted_patients.patient_id " +
                "AND discharge_date IS NULL " +
                "UNION " +
                "SELECT *, null, null, null " +
                "FROM (VALUES ROW (1) , ROW (2) , ROW (3), ROW (4), ROW (5), ROW (6), ROW (7), ROW (8), " +
                "ROW (9), ROW (10), ROW (11), ROW (12), ROW (13), ROW (14), ROW (15), ROW (16), ROW (17), " +
                "ROW (18), ROW(19), ROW (20)) R(room_number) " +
                "WHERE NOT EXISTS ( " +
                "SELECT room_number  " +
                "FROM patients, admitted_patients  " +
                "WHERE patients.room_number = R.room_number " +
                "AND patients.patient_id = admitted_patients.patient_id " +
                "AND discharge_date IS NULL);");
        sqlQueries.add(3, "SELECT firstname, lastname, insurance_id, emergency_contact_id, room_number FROM patients;");
        sqlQueries.add(4, "SELECT patients.patient_id, patients.firstname, patients.lastname " +
                "FROM admitted_patients, patients " +
                "WHERE patients.patient_id = admitted_patients.patient_id " +
                "AND discharge_date IS NULL;");
        sqlQueries.add(5, "SELECT DISTINCT patients.patient_id, patients.firstname, patients.lastname " +
                "FROM admitted_patients, patients " +
                "WHERE patients.patient_id = admitted_patients.patient_id " +
                "AND discharge_date BETWEEN 'insert date here' and 'insert date here';");
        sqlQueries.add(6, "SELECT DISTINCT patients.patient_id, patients.firstname, patients.lastname " +
                "FROM admitted_patients, patients " +
                "WHERE patients.patient_id = admitted_patients.patient_id " +
                "AND arrival_date BETWEEN 'insert date here' and 'insert date here';");
        sqlQueries.add(7, "SELECT admitted_patients.admit_id, admitted_patients.initial_diagnosis " +
                "FROM admitted_patients, patients " +
                "WHERE patients.lastname = 'Jackson' " +
                "AND patients.patient_id = admitted_patients.patient_id;");
        sqlQueries.add(8, "SELECT admitted_patients.admit_id, treatments.treatment_name " +
                "FROM patients, admitted_patients NATURAL JOIN treatments " +
                "WHERE patients.lastname = 'Jackson' " +
                "AND patients.patient_id = treatments.patient_id " +
                "GROUP BY treatment_name " +
                "ORDER BY treatments.timestamp ASC;");
        sqlQueries.add(9, "SELECT patients.patient_id, patients.firstname, patients.lastname, admitted_patients.initial_diagnosis, employees.firstname, employees.lastname " +
                "FROM employees, patients, admitted_patients " +
                "WHERE (arrival_date - 30) <= discharge_date " +
                "AND admitted_patients.patient_id = patients.patient_id " +
                "AND patients.employee_id = employees.employee_id;");
        sqlQueries.add(10, "SELECT patients.patient_id, COUNT(*) AS admissions " +
                "FROM patients, admitted_patients " +
                "WHERE admitted_patients.patient_id = patients.patient_id " +
                "GROUP BY patient_id;");
        sqlQueries.add(11, "SELECT admitted_patients.initial_diagnosis, COUNT(*) " +
                "FROM admitted_patients " +
                "GROUP BY initial_diagnosis " +
                "ORDER BY arrival_date DESC;");
        sqlQueries.add(12, "SELECT admitted_patients.initial_diagnosis, COUNT(*) " +
                "FROM admitted_patients " +
                "WHERE discharge_date IS NULL " +
                "GROUP BY initial_diagnosis " +
                "ORDER BY arrival_date DESC;");
        sqlQueries.add(13, "SELECT treatments.treatment_name, COUNT(*) " +
                "FROM treatments, admitted_patients " +
                "WHERE treatments.patient_id = admitted_patients.patient_id " +
                "AND discharge_date IS NULL " +
                "GROUP BY treatment_name " +
                "ORDER BY treatments.timestamp DESC;");
        sqlQueries.add(14, "SELECT admitted_patients.initial_diagnosis " +
                "FROM admitted_patients, patients " +
                "WHERE patients.patient_id = admitted_patients.patient_id " +
                "HAVING MAX(admit_id);");
        sqlQueries.add(15, "SELECT patients.firstname, patients.lastname, employees.firstname, employees.lastname " +
                "FROM patients, employees, treatments " +
                "WHERE patients.patient_id = treatments.patient_id " +
                "AND employees.employee_id = treatments.employee_id " +
                "AND treatments.treatment_id = 8;");
        sqlQueries.add(16, "SELECT employees.lastname, employees.firstname, employees.category " +
                "FROM employees " +
                "ORDER BY lastname ASC;");
        sqlQueries.add(17, "SELECT DISTINCT employees.firstname, employees.lastname " +
                "FROM employees, patients, admitted_patients " +
                "WHERE patients.employee_id = employees.employee_id " +
                "AND patients.patient_id = admitted_patients.patient_id " +
                "HAVING COUNT(admit_id) >= 4;");
        sqlQueries.add(18, "SELECT admitted_patients.initial_diagnosis, COUNT(*) " +
                "FROM admitted_patients, employees, patients " +
                "WHERE employees.lastname = 'Jones' " +
                "AND patients.employee_id = employees.employee_id " +
                "AND patients.patient_id = admitted_patients.patient_id " +
                "GROUP BY initial_diagnosis " +
                "ORDER BY arrival_date DESC;");
        sqlQueries.add(19, "SELECT treatments.treatment_name, COUNT(*) " +
                "FROM treatments, employees " +
                "WHERE employees.lastname = 'Jones' " +
                "AND treatments.employee_id = employees.employee_id " +
                "GROUP BY treatment_name " +
                "ORDER BY timestamp DESC;");
        sqlQueries.add(20, "SELECT DISTINCT employees.firstname,  employees.lastname " +
                "FROM treatments, employees " +
                "WHERE treatments.employee_id = employees.employee_id;");

        return sqlQueries;
    }

    private static void printRoomUtilizationPrompt() {
        System.out.println("""

                Room Utilization
                1. List the rooms that are occupied, along with the associated patient names and the date the patient was admitted.
                2. List the rooms that are currently unoccupied.
                3. List all rooms in the hospital along with patient names and admission dates for those that are occupied.""");
    }
    private static void printPatientInfoPrompt() {
        System.out.println("""

                Patient Information
                4. List all patients in the database, with full personal information.
                5. List all patients currently admitted to the hospital. List only patient identification number and name.
                6. List all patients who were discharged in a given date range. List only patient identification number and name.\s
                7. List all patients who were admitted within a given date range. List only patient identification number and name.\s
                8. For a given patient (either patient identification number or name), list all admissions to the hospital along with the diagnosis for each admission.\s
                9. For a given patient (either patient identification number or name), list all treatments that were administered.\s
                Group treatments by admissions. List admissions in descending chronological order, and list treatments in ascending chronological order within each admission.\s
                10. List patients who were admitted to the hospital within 30 days of their last discharge date. For each patient list their patient identification number, name, diagnosis, and admitting doctor.\s
                11. For each patient that has ever been admitted to the hospital, list their total number of admissions,\s
                average duration of each admission, longest span between admissions, shortest span between admissions, and average span between admissions.\s""");
    }
    private static void printDiagnosisTreatmentInfoPrompt() {
        System.out.println("""

                Diagnosis and Treatment Information\s
                12. List the diagnoses given to patients, in descending order of occurrences. List diagnosis identification number, name, and total occurrences of each diagnosis.\s
                13. List the diagnoses given to hospital patients, in descending order of occurrences. List diagnosis identification number, name, and total occurrences of each diagnosis.\s
                14. List the treatments performed on admitted patients, in descending order of occurrences. List treatment identification number, name, and total number of occurrences of each treatment.\s
                15. List the diagnoses associated with patients who have the highest occurrences of admissions to the hospital, in ascending order or correlation.\s
                16. For a given treatment occurrence, list the patient name and the doctor who ordered the treatment.\s""");
    }
    private static void printEmployeeInfoPrompt() {
        System.out.println("""

                Employee Information\s
                17. List all workers at the hospital, in ascending last name, first name order. For each worker, list their, name, and job category.\s
                18. List the primary doctors of patients with a high admission rate (at least 4 admissions within a one-year time frame).\s
                19. For a given doctor, list all associated diagnoses in descending order of occurrence. For each diagnosis, list the total number of occurrences for the given doctor.\s
                20. For a given doctor, list all treatments that they ordered in descending order of occurrence. For each treatment, list the total number of occurrences for the given doctor.\s
                21. List employees who have been involved in the treatment of every admitted patient.\s""");
    }
}
