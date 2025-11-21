# SQL Scripts

This directory contains all SQL scripts organized by category.

## Directory Structure

### `/accounts`
Account creation and management scripts
- `create_dean_account.sql` - Create dean account in the system
- `create_specific_professor.sql` - Create specific professor account
- `copy_dean_password_to_rania.sql` - Copy dean password to Rania account

### `/backups`
Database backup files
- `archive_system_backup.sql` - Full archive system database backup

### `/fixes`
Database fixes and corrections
- `fix_dean_email.sql` - Fix dean email address
- `fix_rania_password.sql` - Fix Rania's password
- `fix_role_column.sql` - Fix role column issues

### `/maintenance`
Database maintenance and cleanup scripts
- `clear_all_data.sql` - Clear all data from the database
- `clear_data_for_dean.sql` - Clear data specific to dean

### `/queries`
Query scripts for data retrieval
- `find-professor-emails.sql` - Find professor email addresses
- `list_professors.sql` - List all professors in the system

## Usage

Execute SQL scripts using your MySQL client:

```bash
# Example: Run from MySQL command line
mysql -u username -p database_name < sql/accounts/create_dean_account.sql

# Example: Run from MySQL Workbench or other GUI tools
# Open the file and execute
```

## Safety Notes

- **Backup scripts**: Always verify backup integrity after creation
- **Maintenance scripts**: Use with caution - scripts like `clear_all_data.sql` are destructive
- **Fix scripts**: Review the script content before execution to ensure it matches your needs
- **Account scripts**: Verify account details before creating new users

## Best Practices

1. Always backup your database before running maintenance or fix scripts
2. Test scripts on a development database first
3. Review script contents before execution
4. Keep backups in a secure location
5. Document any manual changes made to these scripts
