CREATE TABLE IF NOT EXISTS territories (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    owner_clan TEXT NOT NULL,
    world TEXT NOT NULL,
    x INTEGER NOT NULL,
    z INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    last_attacked DATETIME,
    tribute_paid BOOLEAN DEFAULT 0
);

CREATE TABLE IF NOT EXISTS territory_members (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    territory_id INTEGER NOT NULL,
    player_uuid TEXT NOT NULL,
    FOREIGN KEY(territory_id) REFERENCES territories(id)
);

CREATE TABLE IF NOT EXISTS territory_logs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    territory_id INTEGER NOT NULL,
    event_type TEXT NOT NULL,
    event_data TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(territory_id) REFERENCES territories(id)
);
