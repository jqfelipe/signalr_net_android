package com.example.pruebasignal;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonElement;

import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;

import microsoft.aspnet.signalr.client.Action;
import microsoft.aspnet.signalr.client.Credentials;
import microsoft.aspnet.signalr.client.ErrorCallback;
import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.http.Request;
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.Subscription;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler1;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler3;
import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler5;
import microsoft.aspnet.signalr.client.transport.ClientTransport;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

public class SignalRService extends Service {
    private HubConnection mHubConnection;
    private HubProxy mHubProxy;
    private Handler mHandler; // to display Toast message
    private final IBinder mBinder = new LocalBinder(); // Binder given to clients

    //Se debe colocar en el constructor para obtenerlo de los settings
    private final String SERVER_URL = "https://app-websocket-v1-0-1-bncr-as.bndesarrollo.com/";
    private final String USR_ANDROID = "AppAndroid";
    private final String INICIACONEXION = "IniciaConexion";
    private final String SERVER_HUB_CHAT = "bncrWebCon";
    private final String FINALIZACONEXION = "";
    private final String TAG = "ServicioSignal";

    public SignalRService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        //mHandler = new Handler(Looper.getMainLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int result = super.onStartCommand(intent, flags, startId);
        startSignalR();
        return result;
    }

    @Override
    public void onDestroy() {
        mHubConnection.stop();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Return the communication channel to the service.
        startSignalR();
        return mBinder;
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        public SignalRService getService() {
            // Return this instance of SignalRService so clients can call public methods
            return SignalRService.this;
        }
    }

    /**
     * Inicia la conexion en el WebSocket del BNCR
     * @param idProceso Identificador del proceso a iniciar
     */
    public void iniciarConexion(String idProceso) {
        mHubProxy.invoke(INICIACONEXION, idProceso);
    }

    public void startSignalR() {
        Platform.loadPlatformComponent(new AndroidPlatformComponent());

        Credentials credentials = request -> request.addHeader("User-Name", USR_ANDROID);

        mHubConnection = new HubConnection(SERVER_URL);
        mHubConnection.setCredentials(credentials);
        mHubProxy = mHubConnection.createHubProxy(SERVER_HUB_CHAT);
        ClientTransport clientTransport = new ServerSentEventsTransport(mHubConnection.getLogger());
        SignalRFuture<Void> signalRFuture = mHubConnection.start(clientTransport);

        try {
            signalRFuture.onError(error -> {
               String msg = error.getMessage();
                Log.d(TAG, msg);
            });
           signalRFuture.get();
            Log.d(TAG, "Inició la conexión");
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return;
        }

        //Aqui se coloca el id del proceso a iniciar
        iniciarConexion(" MTIzNDU2Nzg=");
        Log.d(TAG, "Registró el proceso");


        Subscription subscription = mHubProxy.subscribe("finalizaconexion");
        subscription.addReceivedHandler(new Action<JsonElement[]>() {
            @Override
            public void run(JsonElement[] obj) throws Exception {
                String idProceso = obj[0].toString();
                String resultado = obj[1].toString();
                String mensaje = obj[2].toString();
                Log.d(TAG, mensaje);
            }
        });
    }
}
