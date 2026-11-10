# FixFlow – Group Work Division & Git Contribution Mapping

**Project Title:** FixFlow – Smart Repair Workflow & Warranty Tracking System  
**Architecture:** 3-Tier Desktop Architecture (Presentation UI, Domain Models & Business Logic, SQLite DAO)  
**Target Group Size:** 6 Members  

This document defines the strict, conflict-free division of the FixFlow codebase across all 6 team members. **Every file in the repository is allocated to exactly one member (zero overlapping files)**, enabling clean, conflict-free commits when pushing to a new repository and providing clear individual ownership for presentation and viva evaluations.

---

## High-Level Subsystem Ownership Summary

| Member | Subsystem / Role | Key Domain Responsibilities | Total Files |
|---|---|---|:---:|
| **Member 1** | **Project Foundation, Core Architecture & Security** | Maven build setup, entry point, SQLite engine, multi-role RBAC, BCrypt hashing, session manager, settings & backups | **31** |
| **Member 2** | **Client & Master Entity Management** | Customer registration, device inventory, technician roster, regex validation, referential integrity | **14** |
| **Member 3** | **Repair Workflow & Finite State Machine** | Repair lifecycle state transitions, diagnostics recording, customer approval decisions, transition audit trail | **9** |
| **Member 4** | **Spare-Part Inventory & Stock Logistics** | 27 hardware categories, stock replenishment, atomic part deductions, low-stock alerts, frozen pricing | **6** |
| **Member 5** | **Financial Billing & PDF Invoice Engine** | Labour & parts cost calculations (LKR), cash settlement, change calculator, pre-collection gate, PDFBox generator | **7** |
| **Member 6** | **Warranty Lifecycle & Business Intelligence** | 30-day auto-warranty, warranty claims/re-repairs, real-time KPI metrics, tabular operational reporting | **11** |
| **Total** | | **Entire Repository (Zero Overlap)** | **78 Files** |

---

## Detailed File Allocation per Member

```
src/
├── main/
│   ├── java/com/fixflow/
│   │   ├── database/       <- DB access classes (DAOs)
│   │   ├── enums/          <- State and role enumerations
│   │   ├── exception/      <- Domain-specific business exceptions
│   │   ├── model/          <- Encapsulated OOP entity models
│   │   ├── ui/             <- Swing presentation views & dialogs
│   │   └── util/           <- Utility & helper services
│   └── resources/          <- SQLite schema and icon resources
└── test/                   <- JUnit 5 automated test suites
```

---

### Member 1: Project Foundation, Core Architecture & Security Subsystem

Member 1 initializes the repository and establishes the project structure, build configuration, database connectivity, and the shared RBAC authentication framework.

#### 1. Root & Build Configuration (9 Files)
- `pom.xml` *(Maven build configuration, Java 21, dependencies: FlatLaf, SQLite, jBCrypt, PDFBox 3.x, JUnit 5)*
- `.gitignore` *(Build artifacts, IDE files, local database exclusion rules)*
- `mvnw` *(Unix Maven wrapper script)*
- `mvnw.cmd` *(Windows Maven wrapper script)*
- `.mvn/wrapper/maven-wrapper.properties` *(Maven wrapper distribution configuration)*
- `.mvn/wrapper/mvnw.ps1` *(PowerShell Maven wrapper script)*
- `INSTALLATION_GUIDE.md` *(System installation, setup, and troubleshooting documentation)*
- `PROJECT_SPECIFICATION.md` *(Authoritative project specification)*
- `src/main/resources/icons/.gitkeep` *(Icon assets placeholder directory)*

#### 2. Source Code & Architecture (15 Files)
- `src/main/java/com/fixflow/Main.java` *(Application bootstrap & FlatLaf initialization on EDT)*
- `src/main/resources/db/schema.sql` *(Relational database schema with 10 tables, foreign keys & constraints)*
- `src/main/java/com/fixflow/database/DatabaseConnection.java` *(SQLite JDBC connection provider & PRAGMA configuration)*
- `src/main/java/com/fixflow/database/DatabaseManager.java` *(Database initializer, live WAL backup & schema verification)*
- `src/main/java/com/fixflow/database/UserDB.java` *(User credentials DAO, transactional 5-role initialization)*
- `src/main/java/com/fixflow/model/User.java` *(User entity model)*
- `src/main/java/com/fixflow/enums/UserRole.java` *(5-role enumeration: Admin, Manager, Receptionist, Technician, Inventory)*
- `src/main/java/com/fixflow/enums/NavigationItem.java` *(Sidebar navigation items and role permission mapping)*
- `src/main/java/com/fixflow/util/PasswordUtil.java` *(jBCrypt salted password hashing and verification)*
- `src/main/java/com/fixflow/util/SessionManager.java` *(Thread-safe active session and role-checking singleton)*
- `src/main/java/com/fixflow/ui/BasePanel.java` *(Abstract template base class for all module panels)*
- `src/main/java/com/fixflow/ui/MainFrame.java` *(Main desktop application window with dynamic role-based sidebar)*
- `src/main/java/com/fixflow/ui/LoginFrame.java` *(Shared-role authentication interface)*
- `src/main/java/com/fixflow/ui/SetupFrame.java` *(First-launch wizard to configure initial passwords)*
- `src/main/java/com/fixflow/ui/SettingsPanel.java` *(System administration, credentials update & database backup panel)*

#### 3. Test Suites (7 Files)
- `src/test/java/com/fixflow/database/DatabaseManagerTest.java`
- `src/test/java/com/fixflow/database/DatabaseMaintenanceTest.java`
- `src/test/java/com/fixflow/database/UserDBTest.java`
- `src/test/java/com/fixflow/util/PasswordUtilTest.java`
- `src/test/java/com/fixflow/util/SessionManagerTest.java`
- `src/test/java/com/fixflow/ui/BasePanelTest.java`
- `src/test/java/com/fixflow/ui/RoleAccessPolicyTest.java`

* **Viva Focus Areas**: Singleton pattern (`SessionManager`), jBCrypt work factors, SQLite WAL mode, database backup consistency (`VACUUM INTO`), and role-based access filtering.

---

### Member 2: Client & Master Entity Management Subsystem

Member 2 owns the master data entities (Customers, Devices, Technicians) and the input validation framework.

#### 1. Source Code (10 Files)
- `src/main/java/com/fixflow/model/Customer.java` *(Customer domain entity)*
- `src/main/java/com/fixflow/model/Device.java` *(Electronic device domain entity)*
- `src/main/java/com/fixflow/model/Technician.java` *(Technician specialist entity)*
- `src/main/java/com/fixflow/database/CustomerDB.java` *(Customer CRUD DAO with active job deletion protection)*
- `src/main/java/com/fixflow/database/DeviceDB.java` *(Device CRUD DAO with active repair foreign-key protection)*
- `src/main/java/com/fixflow/database/TechnicianDB.java` *(Technician roster DAO with assignment safeguards)*
- `src/main/java/com/fixflow/util/ValidationUtil.java` *(RegEx patterns for telephone, email, and non-empty validation)*
- `src/main/java/com/fixflow/ui/CustomerPanel.java` *(Customer intake, search, and editing interface)*
- `src/main/java/com/fixflow/ui/DevicePanel.java` *(Device registration and category tracking interface)*
- `src/main/java/com/fixflow/ui/TechnicianPanel.java` *(Technician roster and specialization management interface)*

#### 2. Test Suites (4 Files)
- `src/test/java/com/fixflow/database/CustomerDBTest.java`
- `src/test/java/com/fixflow/database/DeviceDBTest.java`
- `src/test/java/com/fixflow/database/TechnicianDBTest.java`
- `src/test/java/com/fixflow/util/ValidationUtilTest.java`

* **Viva Focus Areas**: Referential integrity, 1-to-many relationships (Customer $\rightarrow$ Devices $\rightarrow$ Repair Jobs), RegEx validation for Sri Lankan phone numbers, preventing accidental data loss through deletion guards.

---

### Member 3: Repair Workflow & Finite State Machine Subsystem

Member 3 implements the core business workflow of FixFlow: repair job tracking, state transitions, technician assignment, and customer quote approval.

#### 1. Source Code (6 Files)
- `src/main/java/com/fixflow/model/RepairJob.java` *(Repair job entity with encapsulation & valid next-status rules)*
- `src/main/java/com/fixflow/model/StatusHistory.java` *(Audit trail log entry model)*
- `src/main/java/com/fixflow/enums/RepairStatus.java` *(Lifecycle state enum: Received, Diagnosing, Waiting for Approval, etc.)*
- `src/main/java/com/fixflow/exception/InvalidStatusTransitionException.java` *(Custom checked exception for invalid state jumps)*
- `src/main/java/com/fixflow/database/RepairDB.java` *(Core repair job DAO, transition validator & audit trail logger)*
- `src/main/java/com/fixflow/ui/RepairPanel.java` *(Repair job dashboard, status updater, diagnosis recorder & approval modal)*

#### 2. Test Suites (3 Files)
- `src/test/java/com/fixflow/database/RepairDBTest.java`
- `src/test/java/com/fixflow/model/RepairStatusTransitionTest.java`
- `src/test/java/com/fixflow/enums/EnumsTest.java`

* **Viva Focus Areas**: Finite State Machine (FSM) implementation, custom domain exceptions, audit logging (`repair_status_history`), role scoping (technicians editing only assigned jobs), approval decision branches (`Approved` $\rightarrow$ `Repairing`, `Rejected` $\rightarrow$ `Cancelled`).

---

### Member 4: Spare-Part Inventory & Supply Chain Subsystem

Member 4 manages the workshop inventory, hardware categories, stock deductions, and historical price preservation.

#### 1. Source Code (5 Files)
- `src/main/java/com/fixflow/model/SparePart.java` *(Spare part catalog entity with stock alert thresholds)*
- `src/main/java/com/fixflow/model/RepairPartUsage.java` *(Itemized part-to-job usage link with frozen price snapshot)*
- `src/main/java/com/fixflow/exception/InsufficientStockException.java` *(Custom exception thrown on inventory depletion)*
- `src/main/java/com/fixflow/database/InventoryDB.java` *(Inventory DAO, atomic stock deduction & replenishment)*
- `src/main/java/com/fixflow/ui/InventoryPanel.java` *(Catalog view with 27 hardware categories, stock intake & alert badges)*

#### 2. Test Suites (1 File)
- `src/test/java/com/fixflow/database/InventoryDBTest.java`

* **Viva Focus Areas**: Atomic database transactions (avoiding race conditions during stock deductions), frozen historical price snapshots (`selling_price_at_time` ensuring past bills don't change when catalog prices increase), visual low-stock warnings.

---

### Member 5: Financial Billing & PDF Invoice Engine Subsystem

Member 5 handles the financial calculations, cash settlement enforcement, change calculations, and invoice PDF generation.

#### 1. Source Code (5 Files)
- `src/main/java/com/fixflow/model/Payment.java` *(Payment record entity)*
- `src/main/java/com/fixflow/enums/PaymentStatus.java` *(Payment state enum: PENDING, PAID, REFUNDED)*
- `src/main/java/com/fixflow/database/PaymentDB.java` *(Payment settlement DAO, cash settlement & pre-collection check)*
- `src/main/java/com/fixflow/util/PdfInvoiceGenerator.java` *(Apache PDFBox 3.x invoice layout & PDF generation engine)*
- `src/main/java/com/fixflow/ui/PaymentPanel.java` *(Cash payment settlement dialog, change calculator & invoice viewer)*

#### 2. Test Suites (2 Files)
- `src/test/java/com/fixflow/database/PaymentDBTest.java`
- `src/test/java/com/fixflow/util/PdfInvoiceGeneratorTest.java`

* **Viva Focus Areas**: Business Rule 12 & 13 enforcement (cash-only settlement, collection blocked without full payment), real-time change calculation listeners, Apache PDFBox graphics coordinate drawing and font streaming.

---

### Member 6: Warranty Tracking, Business Intelligence & Reporting Subsystem

Member 6 manages customer warranties, warranty claim re-repairs, executive dashboard KPIs, and management reports.

#### 1. Source Code (9 Files)
- `src/main/java/com/fixflow/model/Warranty.java` *(30-day warranty entity with validity calculations)*
- `src/main/java/com/fixflow/enums/WarrantyStatus.java` *(Warranty state enum: ACTIVE, EXPIRED, VOIDED, CLAIMED)*
- `src/main/java/com/fixflow/model/DashboardMetrics.java` *(Aggregated KPI metrics model)*
- `src/main/java/com/fixflow/database/WarrantyDB.java` *(Auto-creation of 30-day warranties & re-repair claim linker)*
- `src/main/java/com/fixflow/database/ReportDB.java` *(SQL aggregation DAO for KPIs and 3 tabular reports)*
- `src/main/java/com/fixflow/ui/WarrantyPanel.java` *(Warranty registry, claim processor & re-repair creation UI)*
- `src/main/java/com/fixflow/ui/DashboardPanel.java` *(Real-time executive KPI cards & technician workload card)*
- `src/main/java/com/fixflow/ui/DashboardFrame.java` *(Standalone executive dashboard window)*
- `src/main/java/com/fixflow/ui/ReportsPanel.java` *(Tabular Reports: Repairs, Cash Revenue, Inventory Valuation with date filtering)*

#### 2. Test Suites (2 Files)
- `src/test/java/com/fixflow/database/WarrantyDBTest.java`
- `src/test/java/com/fixflow/database/ReportDBTest.java`

* **Viva Focus Areas**: Automated warranty generation triggered on collection, warranty claim linking to original job history, SQL aggregate functions (`COUNT`, `SUM`, `CASE WHEN`), dynamic date filtering (`LocalDate.now()`).

---

## Recommended Step-by-Step Git Commit Workflow for the New Repository

To ensure a clean Git history without merge conflicts:

1. **Step 1 (Member 1 Initializes Repo)**:
   - Initializes Git: `git init`
   - Commits root files, build files, core architecture, base UI, and security:
     ```bash
     git add pom.xml .gitignore mvnw mvnw.cmd .mvn/ INSTALLATION_GUIDE.md PROJECT_SPECIFICATION.md
     git add src/main/resources/ src/main/java/com/fixflow/Main.java
     git add src/main/java/com/fixflow/database/DatabaseConnection.java src/main/java/com/fixflow/database/DatabaseManager.java src/main/java/com/fixflow/database/UserDB.java
     git add src/main/java/com/fixflow/model/User.java src/main/java/com/fixflow/enums/UserRole.java src/main/java/com/fixflow/enums/NavigationItem.java
     git add src/main/java/com/fixflow/util/PasswordUtil.java src/main/java/com/fixflow/util/SessionManager.java
     git add src/main/java/com/fixflow/ui/BasePanel.java src/main/java/com/fixflow/ui/MainFrame.java src/main/java/com/fixflow/ui/LoginFrame.java src/main/java/com/fixflow/ui/SetupFrame.java src/main/java/com/fixflow/ui/SettingsPanel.java
     git add src/test/java/com/fixflow/database/Database* src/test/java/com/fixflow/database/UserDBTest.java src/test/java/com/fixflow/util/PasswordUtilTest.java src/test/java/com/fixflow/util/SessionManagerTest.java src/test/java/com/fixflow/ui/BasePanelTest.java src/test/java/com/fixflow/ui/RoleAccessPolicyTest.java
     git commit -m "feat(core): initialize FixFlow foundation, SQLite engine, RBAC auth and admin settings"
     ```
   - Pushes to GitHub/GitLab `main` branch.

2. **Step 2 to 6 (Members 2 through 6 add their subsystems)**:
   - Each member clones the repository or pulls `main`.
   - Creates their feature branch (e.g. `feat/customer-device-mgmt`, `feat/repair-workflow`, etc.).
   - Adds **only their assigned files** from the tables above.
   - Commits and creates a Pull Request / merges to `main`.
   - **Result**: Zero merge conflicts, perfect git commit graph, and transparent contribution metrics!

