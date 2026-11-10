-- FixFlow Database Schema
-- SQLite 3 Relational Schema with Foreign Keys & Constraints

-- 1. Shared Role Credentials
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    role TEXT UNIQUE NOT NULL,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT
);

-- 2. Customers
-- Rule: Each customer record is associated with one registered device.
CREATE TABLE IF NOT EXISTS customers (
    customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT,
    address TEXT,
    created_at TEXT NOT NULL
);

-- 3. Devices
CREATE TABLE IF NOT EXISTS devices (
    device_id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    category TEXT NOT NULL,
    brand TEXT NOT NULL,
    model TEXT NOT NULL,
    serial_number TEXT,
    colour TEXT,
    accessories TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT
);

-- 4. Technicians
CREATE TABLE IF NOT EXISTS technicians (
    technician_id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    phone TEXT NOT NULL,
    specialization TEXT,
    is_active INTEGER NOT NULL DEFAULT 1 CHECK (is_active IN (0, 1)),
    created_at TEXT NOT NULL
);

-- 5. Repair Jobs
CREATE TABLE IF NOT EXISTS repair_jobs (
    job_id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    device_id INTEGER NOT NULL,
    technician_id INTEGER,
    date_received TEXT NOT NULL,
    initial_problem TEXT NOT NULL,
    current_status TEXT NOT NULL,
    diagnosis_findings TEXT,
    estimated_cost REAL,
    labour_charges REAL DEFAULT 0.0,
    estimated_completion_date TEXT,
    actual_completion_date TEXT,
    approval_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (approval_status IN ('PENDING', 'APPROVED', 'REJECTED')),
    approval_date TEXT,
    cancellation_reason TEXT,
    original_job_id INTEGER,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE RESTRICT,
    FOREIGN KEY (technician_id) REFERENCES technicians(technician_id) ON DELETE RESTRICT,
    FOREIGN KEY (original_job_id) REFERENCES repair_jobs(job_id) ON DELETE SET NULL
);

-- 6. Spare Parts
CREATE TABLE IF NOT EXISTS spare_parts (
    part_id INTEGER PRIMARY KEY AUTOINCREMENT,
    part_name TEXT NOT NULL,
    category TEXT NOT NULL,
    compatible_device_model TEXT NOT NULL,
    purchase_cost REAL NOT NULL CHECK (purchase_cost >= 0),
    selling_price REAL NOT NULL CHECK (selling_price >= 0),
    available_quantity INTEGER NOT NULL DEFAULT 0 CHECK (available_quantity >= 0),
    min_stock_level INTEGER NOT NULL DEFAULT 5 CHECK (min_stock_level >= 0),
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

-- 7. Repair Part Usage
CREATE TABLE IF NOT EXISTS repair_part_usage (
    usage_id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    part_id INTEGER NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    selling_price_at_time REAL NOT NULL CHECK (selling_price_at_time >= 0),
    date_used TEXT NOT NULL,
    FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT,
    FOREIGN KEY (part_id) REFERENCES spare_parts(part_id) ON DELETE RESTRICT
);

-- 8. Payments
CREATE TABLE IF NOT EXISTS payments (
    payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER UNIQUE NOT NULL,
    payment_date TEXT NOT NULL,
    total_repair_cost REAL NOT NULL CHECK (total_repair_cost >= 0),
    amount_paid REAL NOT NULL CHECK (amount_paid >= 0),
    payment_method TEXT NOT NULL DEFAULT 'CASH',
    payment_status TEXT NOT NULL DEFAULT 'PAID',
    created_at TEXT NOT NULL,
    FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT
);

-- 9. Warranties
CREATE TABLE IF NOT EXISTS warranties (
    warranty_id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER UNIQUE NOT NULL,
    start_date TEXT NOT NULL,
    expiry_date TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'CLAIMED')),
    created_at TEXT NOT NULL,
    FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT
);

-- 10. Repair Status History
CREATE TABLE IF NOT EXISTS repair_status_history (
    history_id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    from_status TEXT,
    to_status TEXT NOT NULL,
    changed_by_role TEXT,
    change_date TEXT NOT NULL,
    notes TEXT,
    FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_devices_customer ON devices(customer_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_customer ON repair_jobs(customer_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_device ON repair_jobs(device_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_technician ON repair_jobs(technician_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_status ON repair_jobs(current_status);
CREATE INDEX IF NOT EXISTS idx_part_usage_job ON repair_part_usage(job_id);
CREATE INDEX IF NOT EXISTS idx_warranties_job ON warranties(job_id);
CREATE INDEX IF NOT EXISTS idx_status_history_job ON repair_status_history(job_id);

