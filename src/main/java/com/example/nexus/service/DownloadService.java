package com.example.nexus.service;


import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Download;
import com.example.nexus.repository.DownloadRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class DownloadService {
    private static final Logger logger = LoggerFactory.getLogger(DownloadService.class);

    private final DownloadRepository downloadRepository;

    // Executor for download tasks
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setDaemon(true);
        return t;
    });

    // Track active tasks by download id
    private final ConcurrentMap<Integer, DownloadTask> activeTasks = new ConcurrentHashMap<>();

    // Map external download tokens (from JCEF or other engines) to internal download IDs
    private final ConcurrentMap<String, Integer> externalMap = new ConcurrentHashMap<>();

    public DownloadService(DIContainer container) {
        this.downloadRepository = container.getOrCreate(DownloadRepository.class);
    }

    // Download listeners
    private final CopyOnWriteArrayList<com.example.nexus.service.DownloadListener> listeners = new CopyOnWriteArrayList<>();

    public void addListener(com.example.nexus.service.DownloadListener l) {
        if (l != null) listeners.add(l);
    }

    public void removeListener(com.example.nexus.service.DownloadListener l) {
        if (l != null) listeners.remove(l);
    }

    public List<Download> getAllDownloads() {
        return downloadRepository.findAll();
    }

    public Download getDownload(int id) {
        return downloadRepository.findById(id);
    }

    /**
     * Start (or queue) a download. If file exists and partial size is present, resume will be used.
     */
    public void startDownload(String url, String fileName, String filePath) {
        try {
            // Ensure parent directories exist and avoid colliding with existing files
            File f = new File(filePath);
            File parent = f.getParentFile();
            if (parent != null && !parent.exists()) {
                boolean ok = parent.mkdirs();
                if (!ok) logger.warn("DownloadService: failed to create parent directories for {}", filePath);
                else logger.info("DownloadService: created parent directories for {}", filePath);
            }

            // If a file already exists at the target path, make a unique filename
            if (f.exists()) {
                String dir = f.getParent();
                String name = f.getName();
                String base;
                String ext = "";
                int dot = name.lastIndexOf('.');
                if (dot > 0) {
                    base = name.substring(0, dot);
                    ext = name.substring(dot);
                } else {
                    base = name;
                }
                int index = 1;
                File nf;
                do {
                    String candidate = base + " (" + index + ")" + ext;
                    nf = new File(dir, candidate);
                    index++;
                } while (nf.exists());
                filePath = nf.getAbsolutePath();
                fileName = nf.getName();
                f = nf;
            }

            Download download = new Download(url, fileName, filePath);
            download.setStatus("pending");
            download.setStartTime(LocalDateTime.now());
            downloadRepository.save(download);
            // notify listeners
            for (com.example.nexus.service.DownloadListener l : listeners) {
                try { l.downloadAdded(download); } catch (Exception ignored) {}
            }

            DownloadTask task = new DownloadTask(download);
            activeTasks.put(download.getId(), task);
            download.setStatus("downloading");
            downloadRepository.update(download);
            executor.submit(task);

            logger.info("Started download: {} to {} (id={})", url, filePath, download.getId());
        } catch (Exception e) {
            logger.error("Failed to initialize download for {} -> {}", url, filePath, e);
        }
    }

    public void pauseDownload(int id) {
        DownloadTask task = activeTasks.get(id);
        if (task != null) task.requestPause();
        else {
            // if no active task but DB exists, mark paused
            Download d = downloadRepository.findById(id);
            if (d != null) {
                d.setStatus("paused");
                downloadRepository.update(d);
            }
        }
    }

    public void resumeDownload(int id) {
        Download d = downloadRepository.findById(id);
        if (d == null) return;
        // If already running, ignore
        if (activeTasks.containsKey(id)) return;
        // Create a new task that resumes from downloadedSize
        DownloadTask task = new DownloadTask(d);
        activeTasks.put(id, task);
        d.setStatus("downloading");
        downloadRepository.update(d);
        executor.submit(task);
    }

    public void retryDownload(int id) {
        Download d = downloadRepository.findById(id);
        if (d == null) return;
        // reset status and downloadedSize
        d.setDownloadedSize(0);
        d.setStatus("pending");
        d.setStartTime(LocalDateTime.now());
        d.setEndTime(null);
        downloadRepository.update(d);
        resumeDownload(id);
    }

    public void updateDownloadProgress(int id, long downloadedSize) {
        Download download = downloadRepository.findById(id);
        if (download != null) {
            download.setDownloadedSize(downloadedSize);
            downloadRepository.update(download);
            for (com.example.nexus.service.DownloadListener l : listeners) {
                try { l.downloadUpdated(download); } catch (Exception ignored) {}
            }
        }
    }

    public void completeDownload(int id) {
        Download download = downloadRepository.findById(id);
        if (download != null) {
            download.setStatus("completed");
            download.setDownloadedSize(download.getFileSize());
            download.setEndTime(LocalDateTime.now());
            downloadRepository.update(download);
            for (com.example.nexus.service.DownloadListener l : listeners) {
                try { l.downloadUpdated(download); } catch (Exception ignored) {}
            }
        }
        activeTasks.remove(id);
    }

    public void failDownload(int id, String error) {
        Download download = downloadRepository.findById(id);
        if (download != null) {
            download.setStatus("failed");
            download.setEndTime(LocalDateTime.now());
            downloadRepository.update(download);
            for (com.example.nexus.service.DownloadListener l : listeners) {
                try { l.downloadUpdated(download); } catch (Exception ignored) {}
            }
        }
        activeTasks.remove(id);
    }

    public void cancelDownload(int id) {
        DownloadTask task = activeTasks.get(id);
        if (task != null) task.requestCancel();
        Download download = downloadRepository.findById(id);
        if (download != null) {
            download.setStatus("cancelled");
            download.setEndTime(LocalDateTime.now());
            downloadRepository.update(download);
        }
        activeTasks.remove(id);
    }

    public void deleteDownload(int id) {
        // stop task if active
        DownloadTask task = activeTasks.remove(id);
        if (task != null) task.requestCancel();
        // delete file
        Download d = downloadRepository.findById(id);
        if (d != null) {
            try {
                File f = new File(d.getFilePath());
                if (f.exists()) {
                    boolean ok = f.delete();
                    if (!ok) logger.warn("Failed to delete file: {}", f.getAbsolutePath());
                }
            } catch (Exception e) {
                logger.warn("Failed to delete download file", e);
            }
        }
        downloadRepository.delete(id);
        for (com.example.nexus.service.DownloadListener l : listeners) {
            try { l.downloadRemoved(id); } catch (Exception ignored) {}
        }
    }

    public void clearDownloads() {
        // cancel all active
        for (Map.Entry<Integer, DownloadTask> en : activeTasks.entrySet()) {
            en.getValue().requestCancel();
        }
        activeTasks.clear();
        downloadRepository.clearAll();
    }

    /**
     * Register an externally-managed download (for example from JCEF's download handler).
     * externalToken should be unique for the external download lifecycle (JCEF download id or generated UUID).
     * Returns the internal download id saved in our DB, or -1 on failure.
     */
    public int registerExternalDownload(String externalToken, String url, String fileName, String filePath) {
        try {
            if (externalToken == null || externalToken.isEmpty() || url == null || url.isEmpty()) {
                logger.warn("registerExternalDownload called with invalid args");
                return -1;
            }

            // Choose a filename if not provided
            String fn = fileName != null && !fileName.isEmpty() ? fileName : "download";
            String fp = filePath;
            if (fp == null || fp.isEmpty()) {
                // fallback to user's Downloads directory
                String userHome = System.getProperty("user.home", "");
                fp = userHome + java.io.File.separator + "Downloads" + java.io.File.separator + fn;
            }

            Download download = new Download(url, fn, fp);
            download.setStatus("pending");
            download.setStartTime(java.time.LocalDateTime.now());
            downloadRepository.save(download);
            externalMap.put(externalToken, download.getId());
            logger.info("Registered external download token={} -> id={}", externalToken, download.getId());
            return download.getId();
        } catch (Exception e) {
            logger.error("Failed to register external download", e);
            return -1;
        }
    }

    /**
     * Update progress reported by an external download engine.
     */
    public void attachExternalProgress(String externalToken, long receivedBytes, long totalBytes) {
        try {
            Integer id = externalMap.get(externalToken);
            if (id == null) return;
            Download d = downloadRepository.findById(id);
            if (d == null) return;
            if (totalBytes > 0) d.setFileSize(totalBytes);
            d.setDownloadedSize(receivedBytes);
            if (d.getStatus() == null || (!d.getStatus().equals("downloading") && !d.getStatus().equals("pending"))) {
                d.setStatus("downloading");
            }
            downloadRepository.update(d);
        } catch (Exception e) {
            logger.debug("attachExternalProgress failed", e);
        }
    }

    public void externalCompleted(String externalToken) {
        Integer id = externalMap.remove(externalToken);
        if (id == null) return;
        completeDownload(id);
    }

    public void externalFailed(String externalToken, String reason) {
        Integer id = externalMap.remove(externalToken);
        if (id == null) return;
        failDownload(id, reason != null ? reason : "external failed");
    }

    public void externalCancelled(String externalToken) {
        Integer id = externalMap.remove(externalToken);
        if (id == null) return;
        cancelDownload(id);
    }

    private class DownloadTask implements Runnable {
        private final Download download;
        private volatile boolean pauseRequested = false;
        private volatile boolean cancelRequested = false;

        public DownloadTask(Download download) {
            this.download = download;
        }

        public void requestPause() {
            pauseRequested = true;
        }

        public void requestCancel() {
            cancelRequested = true;
        }

        @Override
        public void run() {
            int id = download.getId();
            File outFile = new File(download.getFilePath());
            long existing = outFile.exists() ? outFile.length() : 0L;
            download.setDownloadedSize(existing);
            downloadRepository.update(download);

            InputStream in = null;
            RandomAccessFile raf = null;
            HttpURLConnection conn = null;

            boolean success = false;
            try {
                boolean retriedAfter416 = false;
                // Before attempting GET, do a lightweight HEAD probe to learn server capabilities
                boolean serverAcceptsRanges = false;
                long serverFileSize = -1L;
                try {
                    URL probeUrl = new URL(download.getUrl());
                    HttpURLConnection probeConn = (HttpURLConnection) probeUrl.openConnection();
                    probeConn.setConnectTimeout(8000);
                    probeConn.setReadTimeout(8000);
                    probeConn.setRequestMethod("HEAD");
                    probeConn.connect();
                    int probeCode = probeConn.getResponseCode();
                    if (probeCode / 100 == 2) {
                        String ar = probeConn.getHeaderField("Accept-Ranges");
                        serverAcceptsRanges = ar != null && ar.toLowerCase().contains("bytes");
                        String clProbe = probeConn.getHeaderField("Content-Length");
                        if (clProbe != null) {
                            try { serverFileSize = Long.parseLong(clProbe); } catch (NumberFormatException ignored) {}
                        }
                    }
                    try { probeConn.disconnect(); } catch (Exception ignored) {}
                } catch (Exception ignored) {}

                for (int attempt = 0; attempt < 2 && !success; attempt++) {
                    try {
                        URL url = new URL(download.getUrl());
                        conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(15000);
                        conn.setReadTimeout(15000);

                        // Only set Range header if we have an existing partial file and server supports ranges.
                        if (existing > 0 && !retriedAfter416 && serverAcceptsRanges) {
                            conn.setRequestProperty("Range", "bytes=" + existing + "-");
                        }

                        conn.connect();

                        int responseCode = conn.getResponseCode();

                        if (responseCode == 416) { // HTTP 416 Range Not Satisfiable
                            // If we get 416 it usually means our 'existing' offset is invalid (maybe file changed on server)
                            if (existing > 0 && !retriedAfter416) {
                                // delete the partial file and retry without Range once
                                try { outFile.delete(); } catch (Exception ignored) {}
                                existing = 0L;
                                retriedAfter416 = true;
                                try { if (conn != null) conn.disconnect(); } catch (Exception ignored) {}
                                continue; // retry without Range
                            } else {
                                logger.error("Server returned HTTP 416 for download id {}", id);
                                download.setStatus("failed");
                                downloadRepository.update(download);
                                return;
                            }
                        }

                        if (responseCode / 100 == 2 || responseCode == HttpURLConnection.HTTP_PARTIAL) {
                            long total = -1;
                            String cl = conn.getHeaderField("Content-Length");
                            if (cl != null) {
                                try { total = Long.parseLong(cl); } catch (NumberFormatException ignored) {}
                            }
                            // If server sent Content-Range (for partial) we can parse the total size from it
                            long fullSize = -1;
                            String cr = conn.getHeaderField("Content-Range");
                            if (cr != null) {
                                // format: bytes start-end/total
                                int slash = cr.indexOf('/');
                                if (slash > 0) {
                                    String tot = cr.substring(slash + 1).trim();
                                    try { fullSize = Long.parseLong(tot); } catch (NumberFormatException ignored) {}
                                }
                            }
                            long fileSize = -1;
                            if (responseCode == HttpURLConnection.HTTP_PARTIAL) {
                                if (fullSize > 0) fileSize = fullSize; else if (total > 0) fileSize = existing + total; else fileSize = -1;
                            } else {
                                // 200 OK - content-length is the full file size
                                fileSize = total > 0 ? total : serverFileSize;
                            }
                            if (fileSize > 0) download.setFileSize(fileSize);
                            download.setStatus("downloading");
                            downloadRepository.update(download);

                            try {
                                raf = new RandomAccessFile(outFile, "rw");
                                raf.seek(existing);
                            } catch (FileNotFoundException fnf) {
                                logger.error("Could not open file for writing: {}", outFile.getAbsolutePath(), fnf);
                                download.setStatus("failed");
                                downloadRepository.update(download);
                                return;
                            }

                            in = new BufferedInputStream(conn.getInputStream());
                            byte[] buffer = new byte[8192];
                            int read;
                            long lastPersist = System.currentTimeMillis();
                            while ((read = in.read(buffer, 0, buffer.length)) != -1) {
                                if (cancelRequested) {
                                    download.setStatus("cancelled");
                                    downloadRepository.update(download);
                                    // notify listeners about change
                                    for (com.example.nexus.service.DownloadListener l : listeners) {
                                        try { l.downloadUpdated(download); } catch (Exception ignored) {}
                                    }
                                    activeTasks.remove(id);
                                    return;
                                }
                                if (pauseRequested) {
                                    download.setStatus("paused");
                                    try { download.setDownloadedSize(raf.length()); } catch (IOException ignore) {}
                                    downloadRepository.update(download);
                                    // notify listeners about change
                                    for (com.example.nexus.service.DownloadListener l : listeners) {
                                        try { l.downloadUpdated(download); } catch (Exception ignored) {}
                                    }
                                    // remove active task entry so resume can create a fresh task
                                    activeTasks.remove(id);
                                    return;
                                }
                                raf.write(buffer, 0, read);
                                existing += read;
                                download.setDownloadedSize(existing);
                                long now = System.currentTimeMillis();
                                if (now - lastPersist > 1000) {
                                    downloadRepository.update(download);
                                    // notify listeners the download progressed
                                    for (com.example.nexus.service.DownloadListener l : listeners) {
                                        try { l.downloadUpdated(download); } catch (Exception ignored) {}
                                    }
                                    lastPersist = now;
                                }
                            }

                            // finished successfully
                            download.setDownloadedSize(existing);
                            download.setStatus("completed");
                            download.setEndTime(LocalDateTime.now());
                            downloadRepository.update(download);
                            // notify listeners of completion
                            for (com.example.nexus.service.DownloadListener l : listeners) {
                                try { l.downloadUpdated(download); } catch (Exception ignored) {}
                            }
                            activeTasks.remove(id);
                            success = true;
                            break;
                        } else {
                            logger.error("Server returned HTTP {} for download id {}", responseCode, id);
                            download.setStatus("failed");
                            downloadRepository.update(download);
                            for (com.example.nexus.service.DownloadListener l : listeners) {
                                try { l.downloadUpdated(download); } catch (Exception ignored) {}
                            }
                            activeTasks.remove(id);
                            break;
                        }

                    } catch (MalformedURLException e) {
                        throw new RuntimeException(e);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    } finally {
                        try { if (in != null) in.close(); } catch (IOException ignored) {}
                        try { if (raf != null) raf.close(); } catch (IOException ignored) {}
                        if (conn != null) conn.disconnect();
                        // reset stream handles for next attempt
                        in = null; raf = null; conn = null;
                    }
                }

                // Ensure that if we exit without success, activeTasks is cleared
                if (!success) {
                    activeTasks.remove(id);
                }

            } catch (Throwable t) {
                logger.error("Unexpected error in download task id {}", id, t);
                try {
                    download.setStatus("failed");
                    download.setEndTime(LocalDateTime.now());
                    downloadRepository.update(download);
                    for (com.example.nexus.service.DownloadListener l : listeners) {
                        try { l.downloadUpdated(download); } catch (Exception ignored) {}
                    }
                } catch (Exception ignored) {}
                activeTasks.remove(id);
            }
        }
    }

    /**
     * Shutdown the download service and cancel active tasks. Safe to call on app exit.
     */
    public void shutdown() {
        try {
            for (Map.Entry<Integer, DownloadTask> en : activeTasks.entrySet()) {
                try { en.getValue().requestCancel(); } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        try { executor.shutdownNow(); } catch (Exception ignored) {}
    }

}
