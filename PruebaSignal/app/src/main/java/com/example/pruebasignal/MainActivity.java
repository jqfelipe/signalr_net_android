package com.example.pruebasignal;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    private final Context mContext = this;
    private SignalRService mService;
    private boolean mBound = false;
    private BnSignalR bnSignalR;
    TextView myAwesomeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /* Esto es una prueba con un servicio pero para el ejemplod e android seria mejor con una clase
        Intent intent = new Intent();
        intent.setClass(mContext, SignalRService.class);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);

         */

        bnSignalR = new BnSignalR("setsignalrurlhere", "123456");
        bnSignalR.startSignalR();
        if(bnSignalR.isProcesoRegistrado()){
            myAwesomeTextView = (TextView)findViewById(R.id.tvMensaje);
            myAwesomeTextView.setText("Proceso registrado...");
        }

        bnSignalR.iniciarListener((codigo, mensaje, idProceso)
                -> {
            String detalle = idProceso +": "+ codigo + ": "+ mensaje;
            toastNotify(detalle);
        });


    }



    public void toastNotify(final String notificationMessage) {
        runOnUiThread(() -> Toast.makeText(MainActivity.this, notificationMessage, Toast.LENGTH_LONG).show());
    }
}