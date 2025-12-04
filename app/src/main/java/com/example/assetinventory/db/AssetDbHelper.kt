
db.execSQL(
    """
    CREATE TABLE assets (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        code TEXT NOT NULL,
        name TEXT NOT NULL,
        user TEXT,
        department TEXT,
        location TEXT,
        start_date TEXT,
        category TEXT,           -- 新增字段
        status INTEGER NOT NULL,
        task_id INTEGER NOT NULL
    )
    """
)
