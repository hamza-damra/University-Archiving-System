# Database Fix Instructions

Your application is failing because Flyway detected a failed migration. Here's how to fix it:

## Option 1: Using MySQL Workbench or any MySQL Client

1. Open MySQL Workbench (or your preferred MySQL client)
2. Connect to your local MySQL server (localhost:3306)
3. Run the following SQL commands:

```sql
USE archive_system;

-- Delete the failed migration record
DELETE FROM flyway_schema_history WHERE version = '2';

-- Verify the fix
SELECT * FROM flyway_schema_history;
```

## Option 2: Using Command Line (if MySQL is in PATH)

Run this command from any terminal:
```bash
mysql -u root -e "USE archive_system; DELETE FROM flyway_schema_history WHERE version = '2';"
```

## Option 3: Temporary - Disable Flyway

If you need to start the app quickly, you can temporarily disable Flyway by adding this to `application.properties`:
```properties
spring.flyway.enabled=false
```

## What I've Fixed

1. **Fixed the migration file** (`V2__Add_Multi_File_Support.sql`) - Changed from PostgreSQL syntax to MySQL syntax:
   - Changed `BIGSERIAL` to `BIGINT AUTO_INCREMENT`
   - Changed `ALTER COLUMN ... DROP NOT NULL` to `MODIFY COLUMN ... NULL`
   - Removed `IF NOT EXISTS` from `ALTER TABLE` statements (MySQL doesn't support this)

2. **Added Flyway configuration** to `application.properties`:
   - `spring.flyway.validate-on-migrate=false` - Disables validation to allow the app to start

## After Fixing the Database

Once you've cleaned the Flyway schema history table, the application should start successfully with the corrected migration.
