-- Users table
CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     name TEXT NOT NULL,
                                     avatar_path TEXT,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Settings table
CREATE TABLE IF NOT EXISTS settings (
                                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                                        user_id INTEGER NOT NULL,
                                        theme TEXT DEFAULT 'light',
                                        accent_color TEXT DEFAULT '#2196f3',
                                        search_engine TEXT DEFAULT 'google',
                                        home_page TEXT DEFAULT 'https://www.google.com',
                                        startup_behavior TEXT DEFAULT 'show_home',
                                        restore_session BOOLEAN DEFAULT 1,
                                        clear_history_on_exit BOOLEAN DEFAULT 0,
                                        FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
    );

-- Bookmarks table
CREATE TABLE IF NOT EXISTS bookmarks (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                                         user_id INTEGER NOT NULL,
                                         title TEXT NOT NULL,
                                         url TEXT NOT NULL,
                                         favicon_url TEXT,
                                         folder_id INTEGER,
                                         position INTEGER DEFAULT 0,
                                         created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (folder_id) REFERENCES bookmarks (id) ON DELETE SET NULL
    );

-- History table
CREATE TABLE IF NOT EXISTS history (
                                       id INTEGER PRIMARY KEY AUTOINCREMENT,
                                       user_id INTEGER NOT NULL,
                                       title TEXT NOT NULL,
                                       url TEXT NOT NULL,
                                       favicon_url TEXT,
                                       visit_count INTEGER DEFAULT 1,
                                       last_visit TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                       FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
    );

-- Downloads table
CREATE TABLE IF NOT EXISTS downloads (
                                         id INTEGER PRIMARY KEY AUTOINCREMENT,
                                         user_id INTEGER NOT NULL,
                                         url TEXT NOT NULL,
                                         file_name TEXT NOT NULL,
                                         file_path TEXT NOT NULL,
                                         file_size INTEGER DEFAULT 0,
                                         downloaded_size INTEGER DEFAULT 0,
                                         status TEXT DEFAULT 'pending',
                                         start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                         end_time TIMESTAMP,
                                         FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
    );

-- Tabs table
CREATE TABLE IF NOT EXISTS tabs (
                                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                                    user_id INTEGER NOT NULL,
                                    title TEXT NOT NULL,
                                    url TEXT NOT NULL,
                                    favicon_url TEXT,
                                    is_pinned BOOLEAN DEFAULT 0,
                                    is_active BOOLEAN DEFAULT 0,
                                    position INTEGER DEFAULT 0,
                                    session_id TEXT,
                                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
    );

-- Insert default user if not exists
INSERT OR IGNORE INTO users (id, name) VALUES (1, 'Default User');

-- Insert default settings for default user
INSERT OR IGNORE INTO settings (user_id) VALUES (1);