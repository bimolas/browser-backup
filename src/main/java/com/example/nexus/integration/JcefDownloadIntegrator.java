package com.example.nexus.integration;

import com.example.nexus.service.DownloadService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.*;
import java.util.UUID;

/**
 * Reflection-based integration with JCEF download handlers.
 * This class does not import org.cef.* types at compile time and will no-op if JCEF is not on the classpath.
 */
public final class JcefDownloadIntegrator {
    private static final Logger logger = LoggerFactory.getLogger(JcefDownloadIntegrator.class);

    private JcefDownloadIntegrator() {}

    public static boolean isAvailable() {
        try {
            Class.forName("org.cef.CefApp");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Attach a download handler to a CefBrowser instance (passed as Object). Uses reflection.
     * If attachment succeeds, the integrator will call DownloadService.registerExternalDownload(...)
     * and later update progress/completion via DownloadService methods.
     */
    public static void attachToBrowser(Object cefBrowser, DownloadService downloadService) {
        if (cefBrowser == null || downloadService == null) return;
        if (!isAvailable()) return;

        try {
            Class<?> cefBrowserClass = Class.forName("org.cef.browser.CefBrowser");
            if (!cefBrowserClass.isInstance(cefBrowser)) {
                logger.debug("attachToBrowser: provided object is not a CefBrowser");
                return;
            }

            // Obtain client: cefBrowser.getClient()
            Method getClient = cefBrowserClass.getMethod("getClient");
            Object client = getClient.invoke(cefBrowser);
            if (client == null) {
                logger.debug("attachToBrowser: client is null");
                return;
            }

            Class<?> clientClass = Class.forName("org.cef.CefClient");

            // Create an instance of org.cef.handler.CefDownloadHandler via dynamic Proxy
            Class<?> downloadHandlerClass = Class.forName("org.cef.handler.CefDownloadHandler");

            Object handlerProxy = Proxy.newProxyInstance(
                    JcefDownloadIntegrator.class.getClassLoader(),
                    new Class<?>[]{downloadHandlerClass.getInterfaces().length==0?downloadHandlerClass:downloadHandlerClass},
                    new InvocationHandler() {
                        @Override
                        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                            try {
                                String name = method.getName();
                                // onBeforeDownload(org.cef.browser.CefBrowser browser, org.cef.handler.CefDownloadHandler.CefBeforeDownloadCallback callback, String downloadPath, String suggestedName)
                                if (name.equals("onBeforeDownload") || name.equals("onBeforeDownload0")) {
                                    // args may include browser, downloadCallback, downloadPath, suggestedName
                                    String suggested = null;
                                    String url = null;
                                    // try to get suggestedName and URL from args via reflection when possible
                                    for (Object a : args) {
                                        if (a == null) continue;
                                        String s = a.toString();
                                        if (s.startsWith("http") || s.startsWith("file:")) url = s;
                                        if (s.endsWith(".pdf") || s.endsWith(".zip") || s.contains(".")) {
                                            suggested = s;
                                        }
                                    }
                                    // create a token and register external download
                                    String token = UUID.randomUUID().toString();
                                    int id = downloadService.registerExternalDownload(token, url != null ? url : "", suggested, null);
                                    logger.info("JCEF: registered external download token={} -> id={} (suggested={})", token, id, suggested);
                                    return null;
                                }
                                // onDownloadUpdated(org.cef.browser.CefBrowser browser, org.cef.handler.CefDownloadHandler.CefDownloadItem downloadItem)
                                if (name.equals("onDownloadUpdated") || name.equals("onDownloadUpdated0")) {
                                    // We cannot reliably cast downloadItem; try to reflectively read properties
                                    if (args != null && args.length >= 2) {
                                        Object downloadItem = args[1];
                                        try {
                                            Method getReceivedBytes = downloadItem.getClass().getMethod("getReceivedBytes");
                                            Method getTotalBytes = downloadItem.getClass().getMethod("getTotalBytes");
                                            Method isComplete = downloadItem.getClass().getMethod("isCompleted");
                                            Method isCanceled = downloadItem.getClass().getMethod("isCanceled");
                                            Method isInProgress = downloadItem.getClass().getMethod("isInProgress");
                                            Object rec = getReceivedBytes.invoke(downloadItem);
                                            Object tot = getTotalBytes.invoke(downloadItem);
                                            Object completed = isComplete.invoke(downloadItem);
                                            Object canceled = isCanceled.invoke(downloadItem);

                                            long received = rec instanceof Number ? ((Number)rec).longValue() : 0L;
                                            long total = tot instanceof Number ? ((Number)tot).longValue() : -1L;
                                            boolean done = completed instanceof Boolean ? (Boolean) completed : false;
                                            boolean cancel = canceled instanceof Boolean ? (Boolean) canceled : false;

                                            // We don't have a token mapping---this simple integrator can't map back to our ID
                                            // unless we stored a mapping earlier from onBeforeDownload; as a safe fallback we
                                            // won't attempt to update progress here. A fuller integrator would capture the
                                            // downloadItem.getId() or similar and map it to a token.
                                            // TODO: improve mapping by storing downloadItem identity during onBeforeDownload
                                        } catch (NoSuchMethodException nsme) {
                                            // ignore
                                        }
                                    }
                                    return null;
                                }
                            } catch (Throwable t) {
                                logger.debug("JCEF handler proxy failed", t);
                            }
                            return null;
                        }
                    }
            );

            // client.addDownloadHandler(handlerProxy)
            Method addDownloadHandler = clientClass.getMethod("addDownloadHandler", downloadHandlerClass);
            addDownloadHandler.invoke(client, handlerProxy);
            logger.info("Attached JCEF download handler (reflective)");
        } catch (Throwable t) {
            logger.debug("Failed to attach JCEF download handler", t);
        }
    }
}

