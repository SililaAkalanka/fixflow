# FIXFLOW

## Smart Repair Workflow & Warranty Tracking System

### Finalized Project Proposal and Requirements Baseline

**Project type:** Java-based desktop application\
**Document version:** 1.0\
**Document status:** Finalized project direction; detailed
implementation decisions remain subject to validation during
development\
**Target platform:** Windows 11\
**Primary technologies:** Java 21, Java Swing, FlatLaf, SQLite, JDBC and
Maven

------------------------------------------------------------------------

## 1. Executive Summary

FixFlow is a desktop-based repair management application intended for a
medium-sized electronics repair shop. It will provide a centralized way
to manage customer and device records, repair jobs, technician
assignments, spare-part inventory, payments, invoices and warranties.

The initial release will be a standalone Windows 11 application using a
local SQLite database. The application will provide simple shared
role-based login for five predefined roles. It will not require a web
server, cloud infrastructure or a separate database server.

The project is intended both to address a practical repair-shop
operational problem and to demonstrate the Java object-oriented
programming, database and software development concepts required by the
university group assignment.

The implementation will deliberately remain manageable for a first-year
Java project. It will use a straightforward application structure rather
than enterprise frameworks or unnecessary architectural layers.

## 2. Background and Problem Statement

Repair shops may maintain customer information, device details, repair
progress, technician assignments, spare-part stock, payments and
warranty records using paper documents or disconnected spreadsheets.

This can result in duplicated information, difficulty locating previous
repair records, delays in tracking repair progress, inaccurate stock
information and difficulty preparing operational summaries.

FixFlow proposes a desktop application that stores the shop's
operational information in a structured local database and supports a
consistent repair workflow from device intake to collection.

## 3. Project Aim

To design and develop a user-friendly Java desktop application that
improves the organization, accuracy and visibility of electronic device
repair-shop operations.

## 4. Project Objectives

-   Register and maintain customer and electronic device information.
-   Create repair jobs and track their progress.
-   Assign repair jobs to technicians.
-   Record diagnosis findings, estimated costs and customer approval.
-   Manage spare-part information, stock quantities and part usage.
-   Calculate repair costs and record full cash payments.
-   Generate and print PDF invoices.
-   Record and check repair warranties.
-   Provide dashboards and basic operational reports.
-   Apply Java OOP principles, JDBC and relational database concepts.
-   Demonstrate exception handling, collections, generics, file handling
    and Git-based collaboration.

## 5. Target Business and Deployment Context

### 5.1 Business Type

FixFlow is designed for a **medium-sized electronics repair shop** with
multiple technicians and administrative staff.

### 5.2 Supported Devices

The system will support multiple electronic device categories,
including:

-   Smartphones
-   Tablets
-   Laptops
-   Desktop computers
-   Other electronic devices

Device categories should be maintainable through application
configuration or predefined values.

### 5.3 Initial Deployment

The first release will be used on **one Windows 11 computer**.

-   The database will be stored locally using SQLite.
-   Different staff roles will use the same computer and sign in using
    their role's shared credentials.
-   No network server, cloud synchronization or multi-computer database
    access is included in the initial release.
-   The application structure should avoid unnecessary assumptions that
    would prevent a future database migration, but multi-computer
    support is not an MVP requirement.

## 6. User Roles and Access

FixFlow will have five predefined roles.

  -----------------------------------------------------------------------
  Role                                Main responsibilities
  ----------------------------------- -----------------------------------
  Administrator                       Initial setup, shared role
                                      credentials, application settings
                                      and record deletion

  Manager                             Monitor operations, view reports
                                      and oversee repair jobs and
                                      assignments

  Receptionist                        Register customers and devices,
                                      create repair jobs and record
                                      customer-facing information

  Technician                          View all repair jobs, diagnose
                                      devices and update jobs assigned to
                                      them

  Inventory Staff                     Maintain spare-part records, update
                                      stock and manage inventory
  -----------------------------------------------------------------------

### 6.1 Shared Login Model

Authentication and authorization will be deliberately simple.

-   There will be one shared username and password for each role.
-   Employees will not have individual accounts.
-   The application will determine the active role after login.
-   The role determines which screens and actions are available.
-   Complex configurable RBAC, individual activity attribution and
    employee self-registration are out of scope.

### 6.2 First-Launch Setup

On the first launch:

1.  The application detects that initial setup has not been completed.
2.  The user creates the administrator username and password.
3.  The administrator configures shared credentials for the other four
    roles.
4.  Normal login becomes available.

The administrator can subsequently change shared role credentials.
Passwords must not be stored in plain text; use an established
password-hashing library or an appropriate secure Java implementation.

## 7. Functional Requirements

### 7.1 Customer Management

Each customer record will contain:

-   Automatically generated customer ID
-   Full name
-   Phone number
-   Email address (optional)
-   Address (optional)

The application shall allow users with the appropriate role to:

-   Register customers.
-   View customer details.
-   Update customer information.
-   Search by customer name, phone number or customer ID.
-   View repair history associated with the customer record.

**Customer/device relationship decision:** Each customer record is
associated with one registered device. If the same person brings another
device, a separate customer record will be created. This is the selected
initial business rule, even though it may create duplicate customer
records for the same person.

### 7.2 Device Management

Each device record will contain:

-   Automatically generated device ID
-   Associated customer ID
-   Device category
-   Brand
-   Model
-   Serial number or IMEI (optional)
-   Colour
-   Accessories received

The application shall support device registration, viewing, updating and
searching. Users shall be able to view the repair history associated
with a device.

### 7.3 Repair Management

Repair management is the core module of FixFlow.

#### Repair job creation

Only the receptionist role can create repair jobs.

Each repair job shall include:

-   Automatically generated repair job ID
-   Customer ID
-   Device ID
-   Date received
-   Initial problem description
-   Assigned technician
-   Current repair status
-   Estimated completion date
-   Actual completion date, when completed

#### Technician assignment

-   The receptionist or manager can manually assign a technician.
-   Technicians can view all repair jobs.
-   Technicians can update repair information and status only for jobs
    assigned to them.
-   Inactive technicians cannot receive new assignments.

#### Diagnosis

The technician shall be able to record:

-   Diagnosis findings
-   Estimated repair cost
-   Estimated completion date

#### Customer approval

Customer approval is required before repair work begins.

-   The approval decision must be recorded.
-   A repair must not enter the repairing stage before approval.
-   If the customer rejects the estimated cost, the job is cancelled.
-   The cancellation reason must be retained.
-   Cancelled repair records must remain available as historical
    records.

#### Repair statuses

  -----------------------------------------------------------------------
  Status                              Meaning
  ----------------------------------- -----------------------------------
  `RECEIVED`                          Device has been accepted for repair

  `DIAGNOSING`                        Technician is examining the device

  `WAITING_FOR_APPROVAL`              Waiting for customer approval of
                                      the estimate

  `WAITING_FOR_PARTS`                 Required spare parts are
                                      unavailable

  `REPAIRING`                         Repair work is in progress

  `READY_FOR_COLLECTION`              Repair is complete and the device
                                      is ready for handover

  `COLLECTED`                         Device has been handed back to the
                                      customer

  `CANCELLED`                         Repair job has been cancelled
  -----------------------------------------------------------------------

Expected normal flow:

`RECEIVED → DIAGNOSING → WAITING_FOR_APPROVAL → REPAIRING → READY_FOR_COLLECTION → COLLECTED`

When parts are unavailable:

`REPAIRING → WAITING_FOR_PARTS → REPAIRING`

A job can be cancelled where appropriate. The application must validate
status transitions and prevent invalid transitions, such as moving
directly from `RECEIVED` to `COLLECTED`.

There will be no separate quality-check stage.

#### Repair history

-   A device can have multiple repair jobs over its lifetime.
-   Previous repair jobs must remain accessible.
-   Customer and device views should provide access to their associated
    repair history.

### 7.4 Technician Management

Each technician record shall contain:

-   Automatically generated technician ID
-   Name
-   Phone number
-   Specialization
-   Employment status (active/inactive)

The application shall support technician registration, viewing,
updating, searching, activation and deactivation.

Deactivating a technician must prevent new assignments while preserving
historical repair records and existing assignment history.

### 7.5 Spare-Part Inventory

Each spare-part record shall contain:

-   Automatically generated part ID
-   Part name
-   Category
-   Compatible device/model
-   Purchase cost
-   Selling price
-   Available quantity
-   Minimum stock level

Inventory staff shall be able to register parts, update details, add
stock, search and view stock quantities.

#### Stock movements and part usage

-   Inventory staff manually record stock additions.
-   Technicians record the spare parts and quantities used for their
    assigned repair jobs.
-   Stock is automatically deducted when parts are issued/recorded as
    used.
-   The system must prevent issuing a quantity greater than the
    available stock.
-   Each repair job must retain its spare-part usage details.
-   Low-stock alerts are displayed when the available quantity falls
    below the configured minimum stock level.

Stock deductions and repair-part usage records should be saved in a
database transaction so that partial updates do not leave stock and
repair records inconsistent.

### 7.6 Payment and Invoice Management

#### Repair cost calculation

The final repair cost is calculated as:

**Total repair cost = Labour charges + Spare-part selling costs**

Spare-part selling cost is calculated using the selling price and
quantity of each part used.

The price applied to a repair should be retained with the repair-part
usage record when the part is issued. This prevents later changes to the
inventory selling price from changing the cost of an existing repair.

Labour charges are entered as part of the repair cost information.

#### Payment rules

-   Payment is recorded after repair completion.
-   Cash is the only supported payment method.
-   Partial payments are not supported.
-   Full payment is required before the device is collected.
-   No payment gateway or online transaction integration is included.

#### Payment information

Each payment record shall contain:

-   Automatically generated payment ID
-   Repair job ID
-   Payment date
-   Total repair cost
-   Amount paid
-   Payment method
-   Payment status

#### PDF invoices

The application shall generate PDF invoices that can be saved and
printed.

An invoice should contain:

-   Invoice number
-   Repair-shop information
-   Customer information
-   Device information
-   Repair job details
-   Labour charges
-   Spare-part descriptions, quantities and charges
-   Total amount
-   Payment information
-   Invoice date

### 7.7 Warranty Management

Every completed repair receives a fixed **30-day warranty**.

Each warranty record shall contain:

-   Automatically generated warranty ID
-   Original repair job ID
-   Warranty start date
-   Warranty expiry date
-   Warranty status

When a customer returns with a warranty claim:

1.  The receptionist searches for the original repair.
2.  The application checks warranty validity.
3.  A new repair job is created.
4.  The new job is linked to the original repair job.
5.  Both repair records remain available for historical reference.

Warranty date calculations must use a consistent rule. The
implementation should document whether the start date is the repair
completion date or collection date and apply the same rule throughout
the application.

### 7.8 Dashboard

The dashboard shall display operational summary information:

-   Total repair jobs
-   Repair jobs grouped by status
-   Today's repair jobs
-   Revenue summary
-   Low-stock alerts

Dashboard information should be relevant to the active role. The manager
should have access to the overall operational summary.

### 7.9 Reports

The application shall provide in-application reports for:

1.  Repair jobs
2.  Revenue and payment summaries
3.  Inventory and stock

Reports will be displayed within the application. CSV and Excel report
export are not included in the initial release.

### 7.10 Application Settings and General Operations

-   The application shall automatically generate unique identifiers for
    customers, devices, repair jobs, technicians, spare parts, payments
    and warranties.
-   Only administrators can delete records.
-   Deletion must respect database relationships and protect dependent
    financial and repair-history records.
-   Employee deactivation must preserve historical records.
-   The application shall retain important operational history,
    particularly repair status changes and payment information.
-   The application shall support light and dark themes.
-   Administrators will manually back up the SQLite database file.
-   Backup instructions must explain that the application should be
    closed before copying the database file.

## 8. Business Rules and Important Constraints

1.  A customer record is associated with one device in the initial
    release.
2.  A device can have multiple repair jobs over time.
3.  Only receptionists create repair jobs.
4.  Receptionists and managers assign technicians.
5.  Technicians can view all jobs but update only their assigned jobs.
6.  Customer approval is required before repair work begins.
7.  A rejected estimate results in a cancelled job with a retained
    reason.
8.  Repair status transitions must be validated.
9.  A job waiting for parts returns to repairing when parts become
    available.
10. Repair completion does not require a separate quality-check stage.
11. Repair cost includes labour and spare-part selling costs.
12. Payment is full cash payment only, after repair completion.
13. A device cannot be collected until full payment is recorded.
14. Every completed repair receives a fixed 30-day warranty.
15. Warranty claims create a new repair job linked to the original
    repair.
16. Inactive technicians cannot receive new assignments.
17. Stock cannot become negative through part issuance.
18. Only administrators can delete records.
19. Historical financial and repair records must be protected from
    destructive deletion.

## 9. Proposed Technology Stack

  Technology           Purpose
  -------------------- --------------------------------------------------
  Java 21              Main programming language and OOP implementation
  Java Swing           Desktop graphical user interface
  FlatLaf              Modern Swing appearance and light/dark themes
  SQLite               Local relational database
  JDBC                 Java database connectivity
  SQLite JDBC driver   SQLite integration
  Maven                Dependency and build management
  JUnit 5              Unit testing
  Git and GitHub       Version control and collaboration
  jpackage             Windows application packaging

### 9.1 Additional Libraries

Keep external dependencies limited.

-   **FlatLaf:** Swing look and feel.
-   **SQLite JDBC:** Database connectivity.
-   **JUnit 5:** Testing.
-   **PDF library:** Select one lightweight Java PDF-generation library
    for invoices.
-   **Password hashing library:** Use an established implementation for
    shared role passwords.

Use Java's standard libraries for collections, file operations,
date/time handling and basic logging wherever practical.

Avoid introducing an ORM, dependency-injection framework, backend
framework or unnecessary reporting engine.

## 10. Application Architecture

The application will use a simple three-part structure.

### 10.1 UI

Contains Swing windows, panels, forms, tables, buttons and event
listeners. It handles user interaction and displays information.

### 10.2 Model

Contains the main application classes and their data, such as Customer,
Device, RepairJob, Technician, SparePart, Payment and Warranty.

### 10.3 Database

Contains the SQLite connection and JDBC operations, including SQL
queries and CRUD methods.

A UI event can call an appropriate database class method directly.
Business validation should be implemented in clear, reusable methods
without introducing unnecessary layers.

### 10.4 Supporting Packages

Use small supporting packages where useful:

-   `enums` -- user roles, repair statuses and payment statuses.
-   `util` -- validation, file and common utility methods.
-   `resources` -- icons and other application resources.

### 10.5 Proposed Project Structure

``` text
com.fixflow
│
├── Main.java
│
├── model/
│   ├── User.java
│   ├── Customer.java
│   ├── Device.java
│   ├── RepairJob.java
│   ├── Technician.java
│   ├── SparePart.java
│   ├── Payment.java
│   └── Warranty.java
│
├── ui/
│   ├── LoginFrame.java
│   ├── DashboardFrame.java
│   ├── CustomerPanel.java
│   ├── DevicePanel.java
│   ├── RepairPanel.java
│   ├── InventoryPanel.java
│   └── PaymentPanel.java
│
├── database/
│   ├── DatabaseConnection.java
│   ├── CustomerDB.java
│   ├── RepairDB.java
│   ├── InventoryDB.java
│   └── PaymentDB.java
│
├── enums/
│   ├── UserRole.java
│   ├── RepairStatus.java
│   └── PaymentStatus.java
│
├── util/
│   ├── ValidationUtil.java
│   └── FileUtil.java
│
└── resources/
    └── icons/
```

This structure is a starting point, not a requirement to create every
listed class. Additional or fewer classes may be used where the
implementation genuinely needs them.

## 11. Database Requirements

SQLite will be the local relational database.

The implementation is expected to include entities/tables corresponding
to:

-   User credentials
-   Customer
-   Device
-   Technician
-   Repair job
-   Spare part
-   Repair-part usage
-   Payment
-   Warranty
-   Repair status history

The database must use appropriate primary keys, foreign keys, unique
constraints, required-field constraints and indexes.

Use JDBC prepared statements for user-provided values. Enable and verify
SQLite foreign-key enforcement. Use transactions for operations that
update multiple related records, particularly stock deductions, payment
recording and repair completion.

The exact table definitions and relationships will be derived during
implementation from the requirements in this document. A separate
manually prepared ER diagram is not a project deliverable.

## 12. Object-Oriented Programming Requirements

The project must demonstrate the concepts required by the university
assignment.

  -----------------------------------------------------------------------
  OOP requirement                     Planned demonstration
  ----------------------------------- -----------------------------------
  Encapsulation                       Private fields and controlled
                                      access in model classes

  Abstraction                         Abstract classes or interfaces
                                      where appropriate

  Inheritance                         Suitable user-role or other class
                                      hierarchies

  Polymorphism                        Method overriding and interface
                                      implementations

  Exception handling                  Validation, database and
                                      application errors

  Collections                         ArrayList, HashMap and other Java
                                      collections

  Generics                            Standard typed collections and
                                      generic Java types

  File handling                       PDF invoice creation and database
                                      backup operations

  Interfaces                          Useful contracts such as
                                      report-exporting or other shared
                                      behaviour

  Packages                            Organized application packages
  -----------------------------------------------------------------------

The team should be able to explain the OOP concepts used in the actual
implementation. Avoid artificial abstractions introduced only to
increase the number of classes.

## 13. Validation, Error Handling and Security

The application shall:

-   Validate required fields and basic input formats.
-   Prevent invalid repair status transitions.
-   Prevent negative stock quantities.
-   Prevent duplicate identifiers.
-   Handle database connection and SQL errors gracefully.
-   Display understandable messages rather than exposing raw stack
    traces to users.
-   Use prepared statements for database operations.
-   Hash passwords rather than storing plain-text credentials.
-   Restrict screens and actions according to the active role.
-   Protect records that have dependent financial or repair-history
    data.
-   Use database transactions for related multi-step operations.

## 14. Testing Requirements

Testing should be performed throughout development.

### 14.1 Unit Testing

Use JUnit 5 to test selected methods and business rules, including:

-   Repair cost calculation.
-   Stock availability and deduction.
-   Warranty validity.
-   Repair status transition validation.
-   Input validation.

### 14.2 Database Testing

Verify:

-   Customer and device CRUD operations.
-   Repair job creation and updates.
-   Technician assignments.
-   Spare-part stock updates.
-   Payment records.
-   Warranty records.
-   Foreign-key constraints.
-   Transaction rollback on failures.

### 14.3 Functional Testing

Test complete workflows, including:

1.  First-launch administrator setup.
2.  Login for each role.
3.  Customer and device registration.
4.  Repair job creation.
5.  Technician assignment.
6.  Diagnosis and customer approval.
7.  Waiting-for-parts handling.
8.  Repair completion.
9.  Final repair-cost calculation.
10. Full cash payment.
11. PDF invoice generation.
12. Device collection.
13. Warranty creation and claim handling.
14. Dashboard and report accuracy.
15. Manual database backup and restoration.

## 15. Desktop Packaging and Distribution

The first release will target Windows 11.

Use `jpackage` to produce a distributable desktop application or
installer.

The packaged application should:

-   Include the required Java runtime or provide a suitable runtime
    package.
-   Include required libraries and application resources.
-   Initialize the SQLite database on first launch.
-   Store the database in a persistent application-data location outside
    the installation directory.
-   Preserve the database when the application is updated or
    reinstalled.
-   Provide a documented backup and restoration procedure.

The application must be tested as a packaged application, not only when
launched from an IDE.

## 16. Project Development Approach

The project will be developed incrementally with AI-assisted coding
tools such as OpenAI Codex or Google Antigravity.

The coding agent should:

-   Follow this requirements document as the main source of project
    decisions.
-   Keep the implementation within the approved technology stack.
-   Avoid unnecessary frameworks and architectural layers.
-   Implement one coherent feature at a time.
-   Run builds and tests after meaningful changes.
-   Verify database operations and end-to-end workflows.
-   Avoid replacing working functionality without a clear reason.
-   Explain significant implementation decisions so the project team can
    understand and defend the code during the viva.

Suggested implementation sequence:

1.  Maven project foundation and application entry point.
2.  SQLite database initialization and connection.
3.  First-launch setup and shared role login.
4.  Main Swing layout, navigation and themes.
5.  Customer and device management.
6.  Technician management.
7.  Repair management and status validation.
8.  Inventory and spare-part usage.
9.  Payment calculation and PDF invoices.
10. Warranty management.
11. Dashboard and reports.
12. Testing, defect fixing and Windows packaging.

This is an implementation sequence, not a team-member allocation plan.

## 17. Project Constraints and Assignment Alignment

The university Object Oriented Programming group assignment requires a
Java-based solution to a practical startup-company problem and expects
the use of OOP principles, exception handling, collections, generics,
file handling, interfaces, packages and Git/GitHub collaboration.

FixFlow is intended to satisfy these requirements through its actual
application functionality.

The assignment document specifies a maximum group size of five members.
The earlier proposal indicated six members; the proposal's team-size
statement must therefore be corrected to comply with the assignment.

The assignment deadline stated in the supplied document is **30 November
2026 at 11:55 PM**. Final code and presentation slides are required.

## 18. Out-of-Scope Future Enhancements

The following may be considered after the initial release, but are not
part of the current implementation commitment:

-   Multi-computer access through a centralized database.
-   Cloud backup and synchronization.
-   Customer-facing web or mobile portal.
-   SMS/email notifications.
-   Online payment integration.
-   Advanced analytics and charting.
-   CSV/Excel report export.
-   Multi-branch support.
-   Individual employee accounts and detailed audit attribution.

## 19. Items to Validate During Implementation

The requirements are sufficiently defined to begin design and
implementation. The following details should be resolved before the
corresponding feature is completed:

1.  Exact repair status transition matrix, including cancellation rules.
2.  Whether the 30-day warranty starts on repair completion or
    collection.
3.  How the system records customer approval (for example, approval
    confirmation and date).
4.  Whether `READY_FOR_COLLECTION` is entered immediately after repair
    completion and remains until payment and handover.
5.  Exact invoice numbering format and shop details.
6.  Whether a warranty claim is free of charge or follows a separate
    business decision.
7.  Exact deletion restrictions for customers, devices, repair jobs and
    payments.
8.  The selected PDF library and its licensing/packaging requirements.

These are implementation clarifications and should not be used to expand
the project unnecessarily.

------------------------------------------------------------------------

# 20. Conclusion

FixFlow will provide a practical desktop-based solution for managing the
daily operations of an electronics repair shop. It will bring customer
and device records, repair tracking, technician assignments, inventory,
payments, invoices and warranty information into one application.

The project will use Java 21, Swing, FlatLaf, SQLite and JDBC, with a
deliberately simple application structure. Its scope is designed to be
achievable within the university project period while demonstrating the
required object-oriented programming and database concepts.

**FixFlow's guiding principle is to deliver a complete, understandable
and usable repair-management application without unnecessary technical
complexity.**
