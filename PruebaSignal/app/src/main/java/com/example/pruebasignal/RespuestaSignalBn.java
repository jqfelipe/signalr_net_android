package com.example.pruebasignal;

import microsoft.aspnet.signalr.client.hubs.SubscriptionHandler3;

public class RespuestaSignalBn {
    private String idProceso;
    private int codigo;
    private String mensaje;

    public String getIdProceso() {
        return idProceso;
    }

    public void setIdProceso(String idProceso) {
        this.idProceso = idProceso;
    }

    public int getCodigo() {
        return codigo;
    }

    public void setCodigo(int codigo) {
        this.codigo = codigo;
    }

    public String getMensaje() {
        return mensaje;
    }

    public void setMensaje(String mensaje) {
        this.mensaje = mensaje;
    }
}
