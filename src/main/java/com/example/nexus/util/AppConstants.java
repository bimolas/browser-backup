package com.example.nexus.util;

public final class AppConstants {

    private AppConstants() {

    }

    public static final String APP_NAME = "Nexus Browser";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_DESCRIPTION = "A modern web browser built with JavaFX";

    public static final class LightTheme {
        public static final String PRIMARY = "#6366f1";
        public static final String PRIMARY_HOVER = "#4f46e5";
        public static final String SUCCESS = "#22c55e";
        public static final String DANGER = "#ef4444";
        public static final String WARNING = "#f59e0b";

        public static final String TEXT_PRIMARY = "#212529";
        public static final String TEXT_SECONDARY = "#495057";
        public static final String TEXT_MUTED = "#6c757d";

        public static final String BG_PRIMARY = "#ffffff";
        public static final String BG_SECONDARY = "#f8f9fa";
        public static final String BG_TERTIARY = "#e9ecef";

        public static final String BORDER = "#dee2e6";
        public static final String NAV_BG = "#f8f9fa";

        private LightTheme() {}
    }

    public static final class DarkTheme {
        public static final String PRIMARY = "#818cf8";
        public static final String PRIMARY_HOVER = "#6366f1";
        public static final String SUCCESS = "#34d399";
        public static final String DANGER = "#f87171";
        public static final String WARNING = "#fbbf24";

        public static final String TEXT_PRIMARY = "#e0e0e0";
        public static final String TEXT_SECONDARY = "#a0a0a0";
        public static final String TEXT_MUTED = "#808080";

        public static final String BG_PRIMARY = "#1e1e1e";
        public static final String BG_SECONDARY = "#252525";
        public static final String BG_TERTIARY = "#2d2d2d";

        public static final String BORDER = "#333333";
        public static final String NAV_BG = "#252525";

        private DarkTheme() {}
    }

    public static final class Sizes {
        public static final int TAB_MIN_WIDTH = 100;
        public static final int TAB_MAX_WIDTH = 240;
        public static final int TAB_HEIGHT = 36;

        public static final int SIDEBAR_WIDTH = 220;
        public static final int TOOLBAR_HEIGHT = 40;
        public static final int STATUS_BAR_HEIGHT = 24;

        public static final int FONT_SIZE_MIN = 10;
        public static final int FONT_SIZE_MAX = 24;
        public static final int FONT_SIZE_DEFAULT = 14;

        public static final int ICON_SIZE_SMALL = 14;
        public static final int ICON_SIZE_MEDIUM = 18;
        public static final int ICON_SIZE_LARGE = 24;

        public static final int BORDER_RADIUS_SMALL = 4;
        public static final int BORDER_RADIUS_MEDIUM = 8;
        public static final int BORDER_RADIUS_LARGE = 12;

        private Sizes() {}
    }

    public static final class Animation {
        public static final int FAST = 100;
        public static final int NORMAL = 200;
        public static final int SLOW = 300;
        public static final int VERY_SLOW = 500;

        private Animation() {}
    }

    public static final class URLs {
        public static final String DEFAULT_HOME = "https://www.google.com";
        public static final String NEW_TAB_PAGE = "about:blank";
        public static final String SEARCH_GOOGLE = "https://www.google.com/search?q=";
        public static final String SEARCH_BING = "https://www.bing.com/search?q=";
        public static final String SEARCH_DUCKDUCKGO = "https://www.duckduckgo.com/?q=";
        public static final String FAVICON_SERVICE = "https://www.google.com/s2/favicons?domain=";

        private URLs() {}
    }

    public static final class Paths {
        public static final String CSS_MAIN = "/com/example/nexus/css/main.css";
        public static final String CSS_LIGHT = "/com/example/nexus/css/light.css";
        public static final String CSS_DARK = "/com/example/nexus/css/dark.css";
        public static final String FXML_MAIN = "/com/example/nexus/fxml/main.fxml";
        public static final String DB_INIT_SQL = "/com/example/nexus/db/init.sql";

        private Paths() {}
    }

    public static final class Database {
        public static final String FILE_NAME = "browser.db";
        public static final int DEFAULT_USER_ID = 1;

        private Database() {}
    }

    public static final class Zoom {
        public static final double MIN = 0.25;
        public static final double MAX = 5.0;
        public static final double DEFAULT = 1.0;
        public static final double STEP = 0.1;

        public static final double VIEWPORT_MIN = 1.0;
        public static final double VIEWPORT_MAX = 3.0;
        public static final double VIEWPORT_STEP = 0.25;

        private Zoom() {}
    }
}
