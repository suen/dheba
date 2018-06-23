package com.daubajee.dheba;

import io.vertx.core.Vertx;

public class TestLauncher {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle(), handler -> {
            System.out.println("Deployed");
        });

    }

}
