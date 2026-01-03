-- Users table
CREATE TABLE IF NOT EXISTS users (
                                     id INTEGER PRIMARY KEY AUTOINCREMENT,
                                     name TEXT NOT NULL,
                                     avatar_path TEXT,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Settings table (comprehensive browser settings)
CREATE TABLE IF NOT EXISTS settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    -- Appearance
    theme TEXT DEFAULT 'light',
    accent_color TEXT DEFAULT '#3b82f6',
    font_size INTEGER DEFAULT 14,
    page_zoom REAL DEFAULT 1.0,
    show_bookmarks_bar BOOLEAN DEFAULT 1,
    show_status_bar BOOLEAN DEFAULT 1,
    compact_mode BOOLEAN DEFAULT 0,
    -- Startup & Home
    home_page TEXT DEFAULT 'https://www.google.com',
    startup_behavior TEXT DEFAULT 'show_home',
    restore_session BOOLEAN DEFAULT 1,
    new_tab_page TEXT DEFAULT 'new_tab',
    custom_new_tab_url TEXT DEFAULT '',
    -- Search
    search_engine TEXT DEFAULT 'google',
    custom_search_url TEXT DEFAULT '',
    show_search_suggestions BOOLEAN DEFAULT 1,
    search_in_address_bar BOOLEAN DEFAULT 1,
    -- Privacy & Security
    clear_history_on_exit BOOLEAN DEFAULT 0,
    clear_cookies_on_exit BOOLEAN DEFAULT 0,
    clear_cache_on_exit BOOLEAN DEFAULT 0,
    block_popups BOOLEAN DEFAULT 1,
    do_not_track BOOLEAN DEFAULT 1,
    block_third_party_cookies BOOLEAN DEFAULT 0,
    https_only_mode BOOLEAN DEFAULT 0,
    save_browsing_history BOOLEAN DEFAULT 1,
    save_form_data BOOLEAN DEFAULT 1,
    save_passwords BOOLEAN DEFAULT 1,
    -- Downloads
    download_path TEXT DEFAULT '',
    ask_download_location BOOLEAN DEFAULT 0,
    open_pdf_in_browser BOOLEAN DEFAULT 1,
    show_download_notification BOOLEAN DEFAULT 1,
    -- Performance
    hardware_acceleration BOOLEAN DEFAULT 1,
    smooth_scrolling BOOLEAN DEFAULT 1,
    preload_pages BOOLEAN DEFAULT 1,
    lazy_load_images BOOLEAN DEFAULT 1,
    max_tabs_in_memory INTEGER DEFAULT 0,
    -- Accessibility
    high_contrast BOOLEAN DEFAULT 0,
    reduce_motion BOOLEAN DEFAULT 0,
    force_zoom BOOLEAN DEFAULT 0,
    default_encoding TEXT DEFAULT 'UTF-8',
    -- Advanced
    enable_javascript BOOLEAN DEFAULT 1,
    enable_images BOOLEAN DEFAULT 1,
    enable_webgl BOOLEAN DEFAULT 1,
    developer_mode BOOLEAN DEFAULT 0,
    proxy_mode TEXT DEFAULT 'system',
    proxy_host TEXT DEFAULT '',
    proxy_port INTEGER DEFAULT 8080,
    user_agent TEXT DEFAULT '',
    -- Notifications
    enable_notifications BOOLEAN DEFAULT 1,
    sound_enabled BOOLEAN DEFAULT 1,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

-- Bookmarks table - now linked to profile
CREATE TABLE IF NOT EXISTS bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    profile_id INTEGER NOT NULL DEFAULT 1,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    favicon_url TEXT,
    folder_id INTEGER,
    position INTEGER DEFAULT 0,
    is_favorite INTEGER DEFAULT 0,
    description TEXT,
    tags TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE,
    FOREIGN KEY (folder_id) REFERENCES bookmark_folders (id) ON DELETE SET NULL
);

-- Bookmark Folders table - now linked to profile
CREATE TABLE IF NOT EXISTS bookmark_folders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    profile_id INTEGER NOT NULL DEFAULT 1,
    name TEXT NOT NULL,
    parent_folder_id INTEGER,
    position INTEGER DEFAULT 0,
    is_favorite INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE,
    FOREIGN KEY (parent_folder_id) REFERENCES bookmark_folders(id) ON DELETE CASCADE
);

-- History table - now linked to profile
CREATE TABLE IF NOT EXISTS history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    profile_id INTEGER NOT NULL DEFAULT 1,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    favicon_url TEXT,
    visit_count INTEGER DEFAULT 1,
    last_visit TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE
);

-- Downloads table - now linked to profile
CREATE TABLE IF NOT EXISTS downloads (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    profile_id INTEGER NOT NULL DEFAULT 1,
    url TEXT NOT NULL,
    file_name TEXT NOT NULL,
    file_path TEXT NOT NULL,
    file_size INTEGER DEFAULT 0,
    downloaded_size INTEGER DEFAULT 0,
    status TEXT DEFAULT 'pending',
    start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    end_time TIMESTAMP,
    FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE
);

-- Tabs table - now linked to profile
CREATE TABLE IF NOT EXISTS tabs (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    profile_id INTEGER NOT NULL DEFAULT 1,
    title TEXT NOT NULL,
    url TEXT NOT NULL,
    favicon_url TEXT,
    is_pinned BOOLEAN DEFAULT 0,
    is_active BOOLEAN DEFAULT 0,
    position INTEGER DEFAULT 0,
    session_id TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (profile_id) REFERENCES profile (id) ON DELETE CASCADE
);

-- Profile table
CREATE TABLE IF NOT EXISTS profile (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    email TEXT,
    profile_image_path TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert default user if not exists
INSERT OR IGNORE INTO users (id, name) VALUES (1, 'Default User');

-- Insert default settings for default user
INSERT OR IGNORE INTO settings (user_id) VALUES (1);

-- Insert default profile if not exists (id=1)
INSERT OR IGNORE INTO profile (id, username, email, profile_image_path)
VALUES (1, 'Default User', '', '');
