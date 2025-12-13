package com.example.nexus.exception;

/**
 * Base exception for browser-related errors.
 */
public class BrowserException extends RuntimeException {
    private final ErrorCode errorCode;

    public enum ErrorCode {
        DATABASE_ERROR("DB001", "Database operation failed"),
        HISTORY_NOT_FOUND("HIS001", "History entry not found"),
        HISTORY_SAVE_ERROR("HIS002", "Failed to save history entry"),
        HISTORY_DELETE_ERROR("HIS003", "Failed to delete history entry"),
        BOOKMARK_NOT_FOUND("BKM001", "Bookmark not found"),
        BOOKMARK_SAVE_ERROR("BKM002", "Failed to save bookmark"),
        BOOKMARK_DELETE_ERROR("BKM003", "Failed to delete bookmark"),
        FOLDER_NOT_FOUND("FLD001", "Bookmark folder not found"),
        FOLDER_SAVE_ERROR("FLD002", "Failed to save bookmark folder"),
        FOLDER_DELETE_ERROR("FLD003", "Failed to delete bookmark folder"),
        INVALID_INPUT("INP001", "Invalid input provided"),
        NETWORK_ERROR("NET001", "Network operation failed"),
        UNKNOWN_ERROR("UNK001", "An unknown error occurred");

        private final String code;
        private final String defaultMessage;

        ErrorCode(String code, String defaultMessage) {
            this.code = code;
            this.defaultMessage = defaultMessage;
        }

        public String getCode() { return code; }
        public String getDefaultMessage() { return defaultMessage; }
    }

    public BrowserException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
    }

    public BrowserException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public BrowserException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public BrowserException(ErrorCode errorCode, Throwable cause) {
        super(errorCode.getDefaultMessage(), cause);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorCodeString() {
        return errorCode.getCode();
    }

    @Override
    public String toString() {
        return String.format("[%s] %s: %s", errorCode.getCode(), errorCode.name(), getMessage());
    }
}
