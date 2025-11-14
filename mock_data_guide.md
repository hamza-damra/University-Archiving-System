# Mock Data Guide

## Enhanced DataInitializer

The `DataInitializer` class has been enhanced to automatically populate your database with comprehensive mock data when the application starts.

### What Gets Created:

#### 1. Departments
- Computer Science
- Mathematics  
- Physics

#### 2. Users (All with password: "password123")

**HOD Users:**
- hod.cs@alquds.edu (Ahmad Al-Rashid) - Computer Science
- hod.math@alquds.edu (Fatima Al-Zahra) - Mathematics

**Professor Users:**
- prof.omar@alquds.edu (Omar Al-Khouri) - Computer Science
- prof.layla@alquds.edu (Layla Al-Mansouri) - Computer Science  
- prof.hassan@alquds.edu (Hassan Al-Tamimi) - Mathematics
- prof.nour@alquds.edu (Nour Al-Qasemi) - Physics

#### 3. Document Requests
- Advanced Algorithms (Research Paper) - Due in 7 days
- Database Systems (Project Documentation) - Due in 14 days
- Linear Algebra (Assignment Solutions) - Due in 5 days
- Quantum Mechanics (Lab Report) - Due in 10 days

#### 4. Submitted Documents
- Sample on-time submission for Advanced Algorithms
- Sample late submission for Database Systems

#### 5. Notifications
- Various notifications for professors about new requests, deadlines, and submissions

## How to Use:

1. **Automatic:** Simply start the application - the DataInitializer runs automatically
2. **Manual Reset:** Delete all data from tables and restart the app to reinitialize

## Login Credentials:
- **Email:** Use any of the emails listed above
- **Password:** `password123`

## Test Scenarios:
- HOD users can create document requests and view reports
- Professor users can view assigned requests and submit documents
- Sample submissions demonstrate both on-time and late submission workflows
