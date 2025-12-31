package com.kantic.storeParcelsWS.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Baseline Performance Test - Simple test to verify setup works.
 *
 * Run with: mvn gatling:test -Dgatling.simulationClass=com.kantic.storeParcelsWS.simulation.BaselineSimulation
 */
public class BaselineSimulation extends Simulation {

    // HTTP Configuration - using 127.0.0.1 explicitly
    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://127.0.0.1:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json");

    // Single scenario: Search by store (single-line JSON)
    ScenarioBuilder searchScenario = scenario("Baseline Store Search")
        .repeat(10).on(
            exec(
                http("Search Store ST001")
                    .post("/store_parcels/search")
                    .body(StringBody("{\"criterias\":[{\"field\":\"deliveryStoreId\",\"operator\":\"EQ\",\"value\":\"ST001\"}],\"pagination\":{\"page\":1,\"pageSize\":20}}"))
                    .check(status().is(200))
            ).pause(100)
        );

    {
        setUp(
            searchScenario.injectOpen(
                atOnceUsers(10)
            )
        ).protocols(httpProtocol);
    }
}
