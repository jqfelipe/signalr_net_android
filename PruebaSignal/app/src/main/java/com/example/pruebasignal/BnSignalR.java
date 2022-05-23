package com.example.pruebasignal;

import android.util.Log;
import java.util.concurrent.ExecutionException;
import microsoft.aspnet.signalr.client.Credentials;
import microsoft.aspnet.signalr.client.Platform;
import microsoft.aspnet.signalr.client.SignalRFuture;
import microsoft.aspnet.signalr.client.http.android.AndroidPlatformComponent;
import microsoft.aspnet.signalr.client.hubs.HubConnection;
import microsoft.aspnet.signalr.client.hubs.HubProxy;
import microsoft.aspnet.signalr.client.hubs.Subscription;
import microsoft.aspnet.signalr.client.transport.ClientTransport;
import microsoft.aspnet.signalr.client.transport.ServerSentEventsTransport;

public class BnSignalR {
    private HubProxy mHubProxy;
    private final String urlSocket;
    private boolean procesoRegistrado;
    private final String idProceso;
    private final String USR_ANDROID = "AppAndroid";
    private final String TAG = "ServicioSignal";

    public BnSignalR(String url, String idProceso){
        urlSocket = url;
        this.idProceso = idProceso;
    }

    /**
     * Inicia la conexion en el WebSocket
     * @param idProceso Identificador del proceso a iniciar
     */
    private void iniciarConexion(String idProceso) {
        String INICIACONEXION = "IniciaConexion";
        mHubProxy.invoke(INICIACONEXION, idProceso);
    }

    public void startSignalR() {
        Platform.loadPlatformComponent(new AndroidPlatformComponent());

        Credentials credentials = request -> request.addHeader("User-Name", USR_ANDROID);

        HubConnection mHubConnection = new HubConnection(urlSocket);
        mHubConnection.setCredentials(credentials);
        String SERVER_HUB_CHAT = "SETPROXYHERE";  //Set the hub name here
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

        iniciarConexion(idProceso);
        procesoRegistrado = true;
        Log.d(TAG, "Registró el proceso");

    }

    public void iniciarListener(final SignalCallBack callBack){
        String FINALIZACONEXION = "finalizaconexion";
        Subscription subscription = mHubProxy.subscribe(FINALIZACONEXION);
        subscription.addReceivedHandler(obj -> {
            String idProceso = obj[0].toString();
            int resultado = Integer.parseInt(obj[1].toString());
            String mensaje = obj[2].toString();
            Log.d(TAG, mensaje);
            callBack.onFinal(resultado, mensaje, idProceso);
        });

    }

    public boolean isProcesoRegistrado() {
        return procesoRegistrado;
    }
}
