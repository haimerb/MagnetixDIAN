package com.magnetixdian.domain.dian;

public enum OperacionDian {
    SEND_BILL_SYNC("SendBillSync", "http://wcf.dian.colombia/IWcfDianCustomerServices/SendBillSync"),
    SEND_TEST_SET_ASYNC("SendTestSetAsync", "http://wcf.dian.colombia/IWcfDianCustomerServices/SendTestSetAsync"),
    GET_STATUS("GetStatus", "http://wcf.dian.colombia/IWcfDianCustomerServices/GetStatus"),
    GET_STATUS_BY_TRACK_ID("GetStatusByTrackId", "http://wcf.dian.colombia/IWcfDianCustomerServices/GetStatusByTrackId");

    private final String nombre;
    private final String soapAction;

    OperacionDian(String nombre, String soapAction) {
        this.nombre = nombre;
        this.soapAction = soapAction;
    }

    public String nombre() {
        return nombre;
    }

    public String soapAction() {
        return soapAction;
    }
}
