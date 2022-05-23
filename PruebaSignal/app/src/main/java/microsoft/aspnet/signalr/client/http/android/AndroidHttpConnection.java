/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
See License.txt in the project root for license information.
*/

package microsoft.aspnet.signalr.client.http.android;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import microsoft.aspnet.signalr.client.LogLevel;
import microsoft.aspnet.signalr.client.Logger;
import microsoft.aspnet.signalr.client.http.HttpConnection;
import microsoft.aspnet.signalr.client.http.HttpConnectionFuture;
import microsoft.aspnet.signalr.client.http.HttpConnectionFuture.ResponseCallback;
import microsoft.aspnet.signalr.client.http.Request;
import microsoft.aspnet.signalr.client.http.StreamResponse;

/**
 * Android HttpConnection implementation, based on AndroidHttpClient and
 * AsyncTask for async operations
 * Se ajusta para que utilice las librerias mas actualizadas para la conexion
 */
public class AndroidHttpConnection implements HttpConnection {
    private final Logger mLogger;

    /**
     * Initializes the AndroidHttpConnection
     * 
     * @param logger
     *            logger to log activity
     */
    public AndroidHttpConnection(Logger logger) {
        if (logger == null) {
            throw new IllegalArgumentException("logger");
        }

        mLogger = logger;
    }

    @Override
    public HttpConnectionFuture execute(final Request request, final ResponseCallback responseCallback) {

        mLogger.log("Create new AsyncTask for HTTP Connection", LogLevel.Verbose);

        final HttpConnectionFuture future = new HttpConnectionFuture();

        final RequestTask requestTask = new RequestTask() {

            HttpURLConnection mClient;

            InputStream mResponseStream;

            @Override
            protected Void doInBackground(Void... voids) {
                if (request == null) {
                    future.triggerError(new IllegalArgumentException("request"));
                }

                mResponseStream = null;

                try {
                    mLogger.log("Create an Android-specific request", LogLevel.Verbose);
                    request.log(mLogger);

                    URL url = new URL(request.getUrl());
                    mClient = (HttpURLConnection)url.openConnection();
                    mClient.setRequestMethod(request.getVerb());


                    // Se agrega validacion en post para obtener contenido
                    if(request.getVerb().equalsIgnoreCase("POST")){
                        String jsonInputString = request.getContent();
                        try(OutputStream os = mClient.getOutputStream()) {
                        byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                        }
                    }

                    mLogger.log("Execute the HTTP Request", LogLevel.Verbose);

                    try {
                        mResponseStream = mClient.getInputStream();
                    } catch (SocketTimeoutException timeoutException) {
                        closeStreamAndClient();
                        mLogger.log("Timeout executing request: " + timeoutException.getMessage(), LogLevel.Information);

                        future.triggerTimeout(timeoutException);

                        return null;
                    }

                    mLogger.log("Request executed", LogLevel.Verbose);

                    Map<String, List<String>> headersMap = new HashMap<String, List<String>>();
                    int status = mClient.getResponseCode();
                    if(status == HttpURLConnection.HTTP_OK){
                        headersMap = mClient.getHeaderFields();
                    }

                    responseCallback.onResponse(new StreamResponse(mResponseStream, status, headersMap));
                    future.setResult(null);
                    closeStreamAndClient();
                } catch (Exception e) {
                    closeStreamAndClient();
                    mLogger.log("Error executing request: " + e.getMessage(), LogLevel.Critical);

                    future.triggerError(e);
                }

                return null;
            }

            protected void closeStreamAndClient() {
                if (mResponseStream != null) {
                    try {
                        mResponseStream.close();
                    } catch (IOException e) {
                    }
                }

                if (mClient != null) {
                    mClient.disconnect();
                }
            }
        };

        future.onCancelled(new Runnable() {

            @Override
            public void run() {
                AsyncTask<Void, Void, Void> cancelTask = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        requestTask.closeStreamAndClient();
                        return null;
                    }
                };

                executeTask(cancelTask);
            }
        });

        executeTask(requestTask);

        return future;
    }

    @SuppressLint("NewApi")
    private void executeTask(AsyncTask<Void, Void, Void> task) {
        // If it's running with Honeycomb or greater, it must execute each
        // request in a different thread
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            task.execute();
        }
    }

    /**
     * Internal class to represent an async operation that can close a stream
     */
    private abstract class RequestTask extends AsyncTask<Void, Void, Void> {

        /**
         * Closes the internal stream and http client, if they exist
         */
        abstract protected void closeStreamAndClient();
    }

}
