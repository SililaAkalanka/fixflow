# FixFlow – Installation & System Setup Guide

**FixFlow: Smart Repair Workflow & Warranty Tracking System**  
*Java Desktop Application for University Object-Oriented Programming (OOP) Group Assignment*

---

## 1. System Requirements & Prerequisites

| Component | Minimum Requirement | Recommended |
|---|---|---|
| **Operating System** | Windows 10 (64-bit) | Windows 11 (64-bit) |
| **Java Development Kit** | JDK 21 (LTS) | Eclipse Temurin JDK 21 / Oracle JDK 21 |
| **Build Tool** | Apache Maven 3.9+ *(or included Maven Wrapper `mvnw.cmd`)* | Maven Wrapper (Included) |
| **Memory (RAM)** | 2 GB Available RAM | 4 GB+ RAM |
| **Storage** | 200 MB Free Disk Space | 500 MB+ Free Disk Space |
| **Display Resolution** | 1280 × 720 | 1920 × 1080 |

> [!NOTE]
> FixFlow utilizes an embedded SQLite relational database with the official `sqlite-jdbc` native driver. **No external database server installation (such as MySQL or PostgreSQL) is required.**

---

## 2. Technology Stack & Key Dependencies

- **Programming Language:** Java 21 (LTS) bytecode standard
- **Desktop Graphical Interface:** Java Swing with FlatLaf (Light & Dark modern themes)
- **Database:** Local SQLite relational database accessed via standard JDBC
- **Password Security:** `jBCrypt` (Salted BCrypt hashing for shared role credentials)
- **Invoice Generation:** Apache PDFBox 3.x (Lightweight, standards-compliant PDF export)
- **Testing Framework:** JUnit 5 (Jupiter Engine)
- **Packaging:** Maven Shade Plugin (Standalone Runnable Fat JAR) & `jpackage` (Windows desktop distribution)

---

## 3. Quick Start (Running FixFlow)

### Option A: Running the Pre-Packaged Standalone Executable JAR
If `target/fixflow-1.0.0-executable.jar` is already compiled, simply run:
```cmd
java -jar target/fixflow-1.0.0-executable.jar
```
*(Or double-click `fixflow-1.0.0-executable.jar` in Windows File Explorer).*

---

### Option B: Building and Launching from Source

1. **Open PowerShell or Command Prompt** in the project directory:
   ```cmd
   cd C:\Users\<YourUsername>\Documents\FixFlow
   ```

2. **Verify Java 21 Installation**:
   ```cmd
   java -version
   javac -version
   ```
   Ensure the output indicates Java version 21 or higher.

3. **Compile and Run Tests with Maven Wrapper**:
   ```cmd
   .\mvnw clean compile
   .\mvnw test
   ```

4. **Package the Executable Fat JAR**:
   ```cmd
   .\mvnw package
   ```
   This generates the runnable bundle at:
   `target/fixflow-1.0.0-executable.jar`

5. **Launch the Application**:
   ```cmd
   java -jar target/fixflow-1.0.0-executable.jar
   ```

---

## 4. First-Launch Setup Walkthrough

When FixFlow is launched for the first time, it automatically detects that no user accounts exist and launches the **First-Launch System Configuration Wizard** (`SetupFrame`).

### The Five Shared System Roles

In accordance with Section 7.2 of the project specification, FixFlow implements a **shared-role login model** where one role is active per workstation session:

| System Role | Default Suggested Username | Functional Access / Responsibilities |
|---|---|---|
| **Administrator** | `admin` | Full system access, password management, database backups, records deletion |
| **Manager** | `manager` | Operational oversight, financial summaries, technician assignments, approvals |
| **Receptionist** | `receptionist` | Customer/device intake, job creation, payments, invoices, warranty claims |
| **Technician** | `technician` | Assigned job diagnostics, part requests, status progression to completion |
| **Inventory Staff** | `inventory` | Spare parts cataloging, stock replenishment, inventory valuation |

### Setup Steps:
1. On the setup screen, enter desired credentials for all 5 roles (passwords must be at least 6 characters).
2. Click **"Save Configuration & Launch FixFlow"**.
3. All 5 role credentials are salted, hashed with jBCrypt, and committed into the database within an atomic transaction.
4. The application transitions directly to the **Login Screen**.

---

## 5. Database Storage & Maintenance

### Database Location on Windows
FixFlow stores all operational data in a persistent local SQLite database file outside the application installation folder:
```text
%APPDATA%\FixFlow\fixflow.db
(Typical full path: C:\Users\<Username>\AppData\Roaming\FixFlow\fixflow.db)
```

- **Relational Integrity:** Foreign key enforcement (`PRAGMA foreign_keys = ON;`) is enabled on every connection.
- **Concurrency Mode:** Write-Ahead Logging (`PRAGMA journal_mode = WAL;`) is enabled for concurrent reads without UI locking.

---

### Safe Backup & Recovery Procedures (§7.10)

#### Method 1: Cold / Offline Backup (Official Recommended Procedure)
To guarantee 100% data consistency for archival or system migrations:
1. **Close FixFlow completely** on all workstations.
2. Open Windows Run (`Win + R`), type `%APPDATA%\FixFlow`, and press **Enter**.
3. Copy `fixflow.db` (and `fixflow.db-wal` / `fixflow.db-shm` if present) to an external drive or backup directory.

#### Method 2: Live In-App Backup (Administrator Feature)
1. Log in with the **Administrator** role.
2. Navigate to **System Settings** in the sidebar.
3. In the *Database Maintenance* card, click **"Create Backup Copy Now..."**.
4. Choose the destination directory and file name (e.g., `fixflow_backup_20261110.db`).
5. FixFlow executes an atomic checkpoint and snapshots the database safely.

#### Database Restoration:
1. Close FixFlow.
2. Replace `%APPDATA%\FixFlow\fixflow.db` with your verified backup `.db` file.
3. Relaunch FixFlow.

---

## 6. Packaging as a Native Windows Executable (`jpackage`)

To produce an installer or standalone Windows `.exe` that bundles the Java runtime (no separate Java installation needed on client PCs):

```cmd
jpackage ^
  --type app-image ^
  --name "FixFlow" ^
  --app-version "1.0.0" ^
  --input "target" ^
  --main-jar "fixflow-1.0.0-executable.jar" ^
  --main-class "com.fixflow.Main" ^
  --dest "dist" ^
  --win-shortcut ^
  --win-menu
```

This creates a standalone folder in `dist/FixFlow/` containing `FixFlow.exe` ready for Windows 11 desktop deployment.

---

## 7. Troubleshooting & FAQs

### Q1: "Database is locked" (`SQLITE_BUSY`)
- **Cause:** Multiple write transactions overlapping, or an uncommitted SQLite lock.
- **Resolution:** FixFlow utilizes atomic transactions and WAL mode. If a lock persists due to an abrupt shutdown, delete the temporary `-wal` and `-shm` files in `%APPDATA%\FixFlow\` after closing the app.

### Q2: How do I reset a forgotten Administrator password?
- **Resolution:** An administrator can update any role's password inside **System Settings $\rightarrow$ Update Password**. If locked out of the Admin role, delete `%APPDATA%\FixFlow\fixflow.db` to re-trigger the first-launch setup wizard.

### Q3: PDF Invoices fail to open automatically
- **Resolution:** Generated invoices are saved to your chosen location via the file chooser. Ensure a default PDF viewer (e.g., Adobe Acrobat, Microsoft Edge, Google Chrome) is associated with `.pdf` files in Windows Settings.

