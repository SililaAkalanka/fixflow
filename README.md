# FixFlow – Smart Repair Management System (CLI)

FixFlow is a Java Command-Line Interface (CLI) application designed for electronic device repair shops to streamline customer registrations, device tracking, repair lifecycle workflows, technician assignments, spare-part inventory, cash payments, and business analytics.

This project is built for a first-year Object-Oriented Programming (OOP) module. It demonstrates fundamental OOP principles with clear, beginner-friendly code while utilizing **JSON-file-based persistence** instead of an external database engine.

---

## 1. Project Overview & Problem Statement

Small and medium electronic repair centers frequently face challenges with:
- Misplaced paper tickets and unorganized customer contact details.
- Inaccurate repair status tracking (e.g., losing track of diagnosing, in-progress, or ready devices).
- Stock discrepancies from unrecorded spare parts usage during repairs.
- Inconsistent billing and confusion over partial deposit payments vs. full settlements.
- Unintentional release of repaired devices before outstanding repair balances are settled.

**FixFlow CLI** resolves these issues by providing a structured, terminal-driven console system backed by human-readable JSON storage.

---

## 2. Key Features

- **Role-Based Authentication**: Shared account login supporting **Administrator** and **Technician** roles with role-based permission enforcement.
- **Customer Management**: Register, list, search, view full history, update, and delete customers (with referential integrity validation).
- **Device Management**: Associate electronic devices (Smartphones, Laptops, Tablets, Audio, etc.) with registered owners and track active warranty duration.
- **Technician Staff Management**: Manage technician profiles, specializations, and active/inactive duty status.
- **Repair Lifecycle Engine**:
  - Enforces strict status transitions:
    $$\text{RECEIVED} \rightarrow \text{DIAGNOSING} \rightarrow \text{IN\_PROGRESS} \rightarrow \text{COMPLETED} \rightarrow \text{COLLECTED}$$
    $$\text{Or cancelled from any active state with a mandatory cancellation reason} \rightarrow \text{CANCELLED}$$
  - Guard rule: Devices **cannot be marked COLLECTED** if an outstanding balance exists.
- **Inventory & Spare Parts**:
  - Track available quantities and reorder thresholds.
  - Issue spare parts directly to repair jobs with snapshot selling price recording.
  - Enforces stock availability via `InsufficientStockException` to prevent negative inventory.
- **Payments & Billing**:
  - Automatically calculates total repair cost: $\text{Labour Charges} + \text{Spare Parts Used}$.
  - Supports multiple partial cash payments and automatic balance recalculation.
  - Rejects overpayments and payments on already settled jobs.
  - Generates formatted PDF invoice receipts directly to `invoices/`.
- **Operational Reports**:
  - Live summary of active, completed, collected, and cancelled jobs.
  - Real-time revenue figures (cash collected vs. outstanding receivables).
  - Low-stock inventory alert report and total inventory valuation.

---

## 3. Technology Stack & Dependencies

- **Programming Language**: Java 21 (LTS)
- **Build Tool**: Apache Maven (Wrapper included: `./mvnw.cmd` / `./mvnw`)
- **JSON Serialization**: Jackson Databind `2.18.1` (indented, human-readable JSON)
- **Password Security**: jBCrypt `0.4` (salted hashing)
- **PDF Generation**: Apache PDFBox `3.0.3`
- **Testing Framework**: JUnit 5 (Jupiter) `5.11.3`

> [!NOTE]
> All graphical Swing/FlatLaf libraries and SQLite JDBC dependencies have been removed. The application operates solely via terminal console and JSON files.

---

## 4. Build and Run Instructions

### Prerequisites
- Java Development Kit (JDK) 21 or higher installed and on your system `PATH`.
- Terminal / Command Prompt / PowerShell.

### Compile the Project
```bash
./mvnw clean compile
```
*(On Windows Command Prompt or PowerShell, use `.\mvnw.cmd clean compile`)*

### Run the Test Suite
```bash
./mvnw test
```

### Run the Application
```bash
./mvnw compile exec:java -Dexec.mainClass="com.fixflow.Main"
```
Or run the compiled classes directly:
```bash
java -cp "target/classes;target/dependency/*" com.fixflow.Main
```

---

## 5. First-Run & Demo Credentials

When the application starts for the first time, if no data files exist in the `data/` directory, `DataSeeder` automatically populates the system with realistic demo records so you can test all features immediately.

### Pre-Configured Demo Accounts

| Role | Username | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Administrator** | `admin` | `admin123` | Full access: Customers, Devices, Repairs, Technicians, Inventory, Payments, Reports, Password Updates |
| **Technician** | `tech1` | `tech123` | Operational access: Repairs, Diagnosing, Parts Usage, Customers, Devices, Payments (Technician Management restricted) |

---

## 6. How JSON Persistence Works

All application data is persisted as indented JSON files inside the relative `data/` directory:

| JSON Data File | Purpose | Corresponding Class |
| :--- | :--- | :--- |
| `data/customers.json` | Registered customer records | `Customer.java` |
| `data/devices.json` | Registered customer electronic devices | `Device.java` |
| `data/technicians.json` | Staff technicians and active status | `Technician.java` |
| `data/spare_parts.json` | Inventory catalog and stock levels | `SparePart.java` |
| `data/repair_jobs.json` | Repair tickets and lifecycle state | `RepairJob.java` |
| `data/part_usage.json` | Line-item spare parts issued to jobs | `RepairPartUsage.java` |
| `data/payments.json` | Cash payment transactions | `Payment.java` |
| `data/users.json` | Hashed credentials and polymorphic roles | `User.java` (`AdminUser`, `TechnicianUser`) |

### Persistence Mechanism:
1. On application startup, repositories load data into `ArrayList<T>` collections via `JsonFileHandler.loadList()`.
2. When any entity is created, modified, or removed, the repository updates memory and immediately flushes the changes to its respective `.json` file via `JsonFileHandler.saveList()`.
3. If files are missing, parent directories are created automatically and demo data is seeded.
4. All data persists between application restarts.

---

## 7. Package Structure

```text
src/main/java/com/fixflow/
├── Main.java                          # CLI entry point
├── cli/                               # Command-line presentation layer
│   ├── InputHandler.java              # Robust Scanner wrapper with validation
│   ├── MainMenu.java                  # Main menu loop and authentication
│   ├── CustomerMenu.java              # Customer operations
│   ├── DeviceMenu.java                # Device registration & warranty
│   ├── TechnicianMenu.java            # Technician management
│   ├── RepairMenu.java                # Repair job lifecycle & assignment
│   ├── InventoryMenu.java             # Stock management & parts issuance
│   ├── PaymentMenu.java               # Payment recording & receipt generation
│   └── ReportMenu.java                # Operational summaries & analytics
├── enums/                             # Domain enumerations
│   ├── PaymentStatus.java             # UNPAID, PARTIALLY_PAID, PAID
│   ├── RepairStatus.java              # RECEIVED, DIAGNOSING, IN_PROGRESS, COMPLETED, COLLECTED, CANCELLED
│   └── UserRole.java                  # ADMINISTRATOR, TECHNICIAN
├── exception/                         # Custom checked exceptions
│   ├── InsufficientStockException.java # Thrown when part stock < requested
│   └── InvalidStatusTransitionException.java # Thrown on illegal repair state jump
├── model/                             # Domain entities
│   ├── User.java                      # Abstract user base class
│   ├── AdminUser.java                 # Administrator user subclass
│   ├── TechnicianUser.java            # Technician user subclass
│   ├── Customer.java                  # Customer model
│   ├── Device.java                    # Electronic device model
│   ├── Technician.java                # Staff technician model
│   ├── SparePart.java                 # Inventory part model
│   ├── RepairJob.java                 # Repair job model with transition rules
│   ├── RepairPartUsage.java           # Issued part line-item snapshot
│   ├── Payment.java                   # Payment transaction model
│   └── DashboardMetrics.java          # Aggregated dashboard metrics
├── repository/                        # JSON Data Access & CRUD layer
│   ├── Repository.java                # Generic repository interface
│   ├── CustomerRepository.java
│   ├── DeviceRepository.java
│   ├── TechnicianRepository.java
│   ├── InventoryRepository.java
│   ├── PartUsageRepository.java
│   ├── RepairRepository.java
│   ├── PaymentRepository.java
│   └── UserRepository.java
├── service/                           # Business logic layer
│   ├── CustomerService.java
│   ├── DeviceService.java
│   ├── TechnicianService.java
│   ├── InventoryService.java
│   ├── RepairService.java
│   ├── PaymentService.java
│   ├── UserService.java
│   └── ReportService.java
└── util/                              # Shared utility helpers
    ├── DataSeeder.java                # Initial demo data seeding
    ├── JsonFileHandler.java           # Jackson JSON read/write handler
    ├── PasswordUtil.java              # BCrypt hashing & verification
    ├── PdfInvoiceGenerator.java       # PDF invoice generation via PDFBox
    ├── SessionManager.java            # Active user session manager
    └── ValidationUtil.java            # Phone, email, and input regex checks
```

---

## 8. OOP Concepts Mapping (Viva Reference)

| Required Concept | Implementation in FixFlow | Exact Class(es) & Method(s) |
| :--- | :--- | :--- |
| **1. Encapsulation** | Private fields with public getters/setters and domain validation methods. Internal state cannot be modified arbitrarily. | `Customer.java`, `Device.java`, `SparePart.java`, `RepairJob.java`<br>• `Device.isUnderWarranty()`<br>• `SparePart.isLowStock()`<br>• `RepairJob.getValidNextStatuses()` |
| **2. Abstraction** | Abstract base class declaring contract methods implemented differently by specific roles. Also repository abstraction. | `User.java` (abstract class)<br>• `public abstract UserRole getRole();`<br>• `public abstract boolean canManageTechnicians();`<br>• `public abstract String getRoleDescription();`<br>`Repository.java` (abstracts data storage) |
| **3. Inheritance** | Subclasses inherit common identity, authentication fields, and timestamp management from the base class. | `AdminUser extends User`<br>`TechnicianUser extends User` |
| **4. Polymorphism** | Subclasses override abstract methods with role-specific behavior. The CLI menu invokes them polymorphically without casting. | `AdminUser.java` & `TechnicianUser.java`<br>• Overriding `canManageTechnicians()`<br>• Overriding `getRoleDescription()`<br>• Called in `MainMenu.java` (`user.getRoleDescription()`) |
| **5. Exception Handling** | Custom checked exceptions representing domain-specific business constraint violations, plus try-catch input validation. | `InsufficientStockException.java`<br>• Thrown in `InventoryService.issuePartToJob()`<br>• Caught in `InventoryMenu.issuePartToJob()`<br>`InvalidStatusTransitionException.java`<br>• Thrown in `RepairJob.validateTransition()`<br>• Caught in `RepairMenu.updateRepairStatus()` |
| **6. Collections & Generics** | Generic interface and standard `ArrayList<T>` collections manipulated using beginner-friendly loops. | `Repository<T>.java`<br>• `void add(T item)`<br>• `T findById(int id)`<br>• `List<T> findAll()`<br>Implemented in `CustomerRepository`, `DeviceRepository`, `RepairRepository`, etc. |
| **7. File Handling** | Reading and writing JSON files on disk. Survives application restart. | `JsonFileHandler.java`<br>• `<T> List<T> loadList(String filePath, Class<T> clazz)`<br>• `<T> void saveList(String filePath, List<T> list)` |
| **8. Interfaces & Packages** | Clean separation of concerns across layered packages (`cli`, `model`, `repository`, `service`, `util`, `enums`, `exception`). | `public interface Repository<T>` in `com.fixflow.repository`<br>Decouples services and CLI from storage specifics. |

---

## 9. Example End-to-End Repair Workflow

To test the complete workflow in the CLI:

1. **Login**: Login with `admin` / `admin123`.
2. **Register Customer**: Open `1. Customer Management` $\rightarrow$ `1. Register New Customer` (e.g. Kasun, 0771122334). Note the assigned ID (e.g., `#1`).
3. **Register Device**: Open `2. Device Management` $\rightarrow$ `1. Register Device for Customer` (select Customer `#1`, Apple iPhone 13, 12 months warranty). Note the Device ID (e.g., `#1`).
4. **Create Repair Job**: Open `3. Repair Management` $\rightarrow$ `1. Create New Repair Job` (select Customer `#1`, Device `#1`, issue: "Cracked Screen"). Status starts at `RECEIVED`.
5. **Assign Technician**: Open `3. Repair Management` $\rightarrow$ `5. Assign Technician to Job` (assign active technician Kamal Perera).
6. **Diagnose**: Open `3. Repair Management` $\rightarrow$ `6. Record Diagnosis & Labour Charges` (Findings: "OLED shattered", Labour: `3500.00`).
7. **Advance to In Progress**: Open `3. Repair Management` $\rightarrow$ `7. Advance / Update Repair Status` $\rightarrow$ Advance `RECEIVED` $\rightarrow$ `DIAGNOSING` $\rightarrow$ `IN_PROGRESS`.
8. **Issue Spare Part**: Open `5. Inventory Management` $\rightarrow$ `5. Issue Spare Part to Repair Job` (Issue 1x Screen Part to Job. Part stock is automatically deducted and line cost added).
   *(Tip: Try issuing 999 units to trigger `InsufficientStockException`!)*
9. **Complete Repair**: Open `3. Repair Management` $\rightarrow$ `7. Advance / Update Repair Status` $\rightarrow$ Advance to `COMPLETED`.
10. **Test Collection Guard**: Try updating status to `COLLECTED`. The system blocks it:
    > `[!] COLLECTION RULE BLOCKED: Cannot collect device: Repair Job #... has an outstanding balance of LKR ...`
11. **Settle Payment**: Open `6. Payment Management` $\rightarrow$ `1. Record Payment for Repair Job` (Pay the exact remaining balance). Status changes to `PAID`.
12. **Collect Device**: Open `3. Repair Management` $\rightarrow$ `7. Advance / Update Repair Status` $\rightarrow$ Select `COLLECTED`. Transition now succeeds!
13. **Generate PDF Receipt**: Open `6. Payment Management` $\rightarrow$ `5. Generate PDF Invoice Receipt` (Creates a PDF invoice in `invoices/`).

---

## 10. Known Limitations

- **Single Terminal Session**: Designed for one active terminal user at a time (suitable for single-workstation service desks).
- **Concurrency**: File writes are synchronous per operation. Distributed multi-user database locking is out of scope for a first-year assignment.
- **Payment Methods**: Currently focused on Cash payment tracking.
