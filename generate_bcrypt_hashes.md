# BCrypt Password Hash Generator Instructions

Before running the V9 migration, you need to generate BCrypt password hashes for all user passwords.

## Passwords to Hash:
- `Admin@123` - for admin@alquds.edu
- `Deanship@123` - for deanship@alquds.edu
- `Hod@123` - for all HOD users
- `Prof@123` - for all Professor users

## Method 1: Using Java (Recommended)

1. Modify `PasswordHashGenerator.java` to generate all hashes:

```java
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        String[] passwords = {"Admin@123", "Deanship@123", "Hod@123", "Prof@123"};
        String[] labels = {"Admin", "Deanship", "HOD", "Professor"};
        
        for (int i = 0; i < passwords.length; i++) {
            String hash = encoder.encode(passwords[i]);
            System.out.println("-- " + labels[i] + " (" + passwords[i] + "): " + hash);
        }
    }
}
```

2. Run it and copy the hashes to replace placeholders in `V9__seed_comprehensive_data.sql`

## Method 2: Using Online Tool

1. Visit https://bcrypt-generator.com/ or https://bcrypt.online/
2. For each password:
   - Enter the password
   - Set rounds to 10 (default)
   - Click "Generate Hash"
   - Copy the hash (starts with `$2a$10$` or `$2b$10$`)

## Method 3: Using Spring Boot Application

You can also generate hashes programmatically in your Spring Boot application:

```java
@Autowired
private PasswordEncoder passwordEncoder;

String hash = passwordEncoder.encode("Admin@123");
System.out.println(hash);
```

## After Generating Hashes

Replace the placeholder hashes in `V9__seed_comprehensive_data.sql`:
- Replace `$2a$10$YourBCryptHashHereForAdmin123` with the actual Admin@123 hash
- Replace `$2a$10$YourBCryptHashHereForDeanship123` with the actual Deanship@123 hash
- Replace `$2a$10$YourBCryptHashHereForHod123` with the actual Hod@123 hash
- Replace `$2a$10$YourBCryptHashHereForProf123` with the actual Prof@123 hash

## Important Notes

- Each BCrypt hash is unique (due to salt), but any hash for the same password will work
- Use the same hash for all users with the same password (e.g., all HODs use the same Hod@123 hash)
- The hash format should be: `$2a$10$...` or `$2b$10$...` (60 characters total)
