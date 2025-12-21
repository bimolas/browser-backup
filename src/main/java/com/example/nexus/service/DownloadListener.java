package com.example.nexus.service;

import com.example.nexus.model.Download;


public interface DownloadListener {
    default void downloadAdded(Download download) {}
    default void downloadUpdated(Download download) {}
    default void downloadRemoved(int downloadId) {}
}

