package com.magnetixdian.infrastructure.dian;

import java.net.http.HttpClient;

@FunctionalInterface
public interface FabricaHttpClientDian {

    HttpClient crear();
}
