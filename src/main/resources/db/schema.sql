-- FixFlow Simplified Database Schema
-- SQLite 3 Relational Schema with Foreign Keys & Constraints

-- 1. User Credentials (Admin & Technician)
CREATE TABLE IF NOT EXISTS users (
    user_id INTEGER PRIMARY KEY AUTOINCREMENT,
    role TEXT UNIQUE NOT NULL,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    created_at TEXT NOT NULL,
    updated_at TEXT
);

-- 2. Customers
CREATE TABLE IF NOT EXISTS customers (
    customer_id INTEGER PRIMARY KEY AUTOINCREMENT,
    full_name TEXT NOT NULL,
    phone TEXT NOT NULL,
    email TEXT,
    address TEXT,
    created_at TEXT NOT NULL
);

-- 3. Devices (with embedded warranty information)
CREATE TABLE IF NOT EXISTS devices (
    device_id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    category TEXT NOT NULL,
    brand TEXT NOT NULL,
    model TEXT NOT NULL,
    serial_number TEXT,
    colour TEXT,
    accessories TEXT,
    warranty_months INTEGER DEFAULT 0,
    warranty_start_date TEXT,
    warranty_expiry_date TEXT,
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

-- 5. Repair Jobs (6-status lifecycle)
CREATE TABLE IF NOT EXISTS repair_jobs (
    job_id INTEGER PRIMARY KEY AUTOINCREMENT,
    customer_id INTEGER NOT NULL,
    device_id INTEGER NOT NULL,
    technician_id INTEGER,
    date_received TEXT NOT NULL,
    initial_problem TEXT NOT NULL,
    current_status TEXT NOT NULL CHECK (current_status IN ('RECEIVED', 'DIAGNOSING', 'IN_PROGRESS', 'COMPLETED', 'COLLECTED', 'CANCELLED')),
    diagnosis_findings TEXT,
    estimated_cost REAL,
    labour_charges REAL DEFAULT 0.0,
    estimated_completion_date TEXT,
    actual_completion_date TEXT,
    notes TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id) ON DELETE RESTRICT,
    FOREIGN KEY (device_id) REFERENCES devices(device_id) ON DELETE RESTRICT,
    FOREIGN KEY (technician_id) REFERENCES technicians(technician_id) ON DELETE RESTRICT
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

-- 8. Payments (supports multiple partial payments per repair job, no UNIQUE constraint on job_id)
CREATE TABLE IF NOT EXISTS payments (
    payment_id INTEGER PRIMARY KEY AUTOINCREMENT,
    job_id INTEGER NOT NULL,
    payment_date TEXT NOT NULL,
    amount_paid REAL NOT NULL CHECK (amount_paid >= 0),
    payment_method TEXT NOT NULL DEFAULT 'CASH',
    notes TEXT,
    created_at TEXT NOT NULL,
    FOREIGN KEY (job_id) REFERENCES repair_jobs(job_id) ON DELETE RESTRICT
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers(phone);
CREATE INDEX IF NOT EXISTS idx_devices_customer ON devices(customer_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_customer ON repair_jobs(customer_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_device ON repair_jobs(device_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_technician ON repair_jobs(technician_id);
CREATE INDEX IF NOT EXISTS idx_repair_jobs_status ON repair_jobs(current_status);
CREATE INDEX IF NOT EXISTS idx_part_usage_job ON repair_part_usage(job_id);
CREATE INDEX IF NOT EXISTS idx_payments_job ON payments(job_id);
