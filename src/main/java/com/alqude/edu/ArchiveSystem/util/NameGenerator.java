package com.alqude.edu.ArchiveSystem.util;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * Utility class for generating realistic Arabic and English names for mock data.
 * Provides methods to generate full names and email addresses.
 */
public class NameGenerator {
    
    private static final Random random = new Random();
    
    private static final List<String> FIRST_NAMES_MALE = Arrays.asList(
        "Ahmad", "Omar", "Hassan", "Khalid", "Tariq", "Yusuf", "Ibrahim", "Mahmoud",
        "Ali", "Mohammed", "Samir", "Karim", "Nabil", "Rashid", "Faisal", "Jamal"
    );
    
    private static final List<String> FIRST_NAMES_FEMALE = Arrays.asList(
        "Fatima", "Layla", "Nour", "Amira", "Zainab", "Huda", "Rania", "Maryam",
        "Aisha", "Salma", "Dina", "Hana", "Lina", "Sara", "Yasmin", "Rana"
    );
    
    private static final List<String> LAST_NAMES = Arrays.asList(
        "Al-Rashid", "Al-Khouri", "Al-Mansouri", "Al-Tamimi", "Al-Qasemi",
        "Al-Najjar", "Al-Masri", "Al-Shami", "Al-Halabi", "Al-Baghdadi",
        "Al-Zahra", "Al-Hassan", "Al-Hussein", "Al-Kareem", "Al-Amin",
        "Al-Sayed", "Al-Mahmoud", "Al-Farouk", "Al-Nasser", "Al-Aziz"
    );
    
    /**
     * Generates a random full name (first name + last name).
     * 
     * @param isMale true for male names, false for female names
     * @return a full name string
     */
    public static String generateFullName(boolean isMale) {
        String firstName = isMale ? getRandomFirstNameMale() : getRandomFirstNameFemale();
        String lastName = getRandomLastName();
        return firstName + " " + lastName;
    }
    
    /**
     * Generates a random full name with random gender.
     * 
     * @return a full name string
     */
    public static String generateFullName() {
        return generateFullName(random.nextBoolean());
    }
    
    /**
     * Generates an email address based on first name, last name, and department.
     * Format: firstname.lastname@alquds.edu
     * 
     * @param firstName the first name
     * @param lastName the last name
     * @return an email address string
     */
    public static String generateEmail(String firstName, String lastName) {
        String cleanFirstName = firstName.toLowerCase().replace(" ", "");
        String cleanLastName = lastName.toLowerCase().replace("al-", "").replace(" ", "");
        return cleanFirstName + "." + cleanLastName + "@alquds.edu";
    }
    
    /**
     * Generates an email address with a prefix (e.g., "prof", "hod").
     * Format: prefix.firstname.lastname@alquds.edu
     * 
     * @param prefix the prefix to add before the name
     * @param firstName the first name
     * @param lastName the last name
     * @return an email address string
     */
    public static String generateEmailWithPrefix(String prefix, String firstName, String lastName) {
        String cleanFirstName = firstName.toLowerCase().replace(" ", "");
        String cleanLastName = lastName.toLowerCase().replace("al-", "").replace(" ", "");
        return prefix + "." + cleanFirstName + "." + cleanLastName + "@alquds.edu";
    }
    
    /**
     * Generates a department-based email for HODs.
     * Format: hod.{deptCode}@alquds.edu
     * 
     * @param departmentCode the department code (e.g., "cs", "math")
     * @return an email address string
     */
    public static String generateDepartmentEmail(String departmentCode) {
        return "hod." + departmentCode.toLowerCase() + "@alquds.edu";
    }
    
    /**
     * Gets a random male first name.
     * 
     * @return a random male first name
     */
    public static String getRandomFirstNameMale() {
        return FIRST_NAMES_MALE.get(random.nextInt(FIRST_NAMES_MALE.size()));
    }
    
    /**
     * Gets a random female first name.
     * 
     * @return a random female first name
     */
    public static String getRandomFirstNameFemale() {
        return FIRST_NAMES_FEMALE.get(random.nextInt(FIRST_NAMES_FEMALE.size()));
    }
    
    /**
     * Gets a random last name.
     * 
     * @return a random last name
     */
    public static String getRandomLastName() {
        return LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
    }
    
    /**
     * Generates a professor ID based on department code and sequence number.
     * Format: P{deptCode}{number} (e.g., "PCS001", "PMATH002")
     * 
     * @param departmentCode the department code (e.g., "CS", "MATH")
     * @param sequenceNumber the sequence number
     * @return a professor ID string
     */
    public static String generateProfessorId(String departmentCode, int sequenceNumber) {
        return String.format("P%s%03d", departmentCode.toUpperCase(), sequenceNumber);
    }
}
