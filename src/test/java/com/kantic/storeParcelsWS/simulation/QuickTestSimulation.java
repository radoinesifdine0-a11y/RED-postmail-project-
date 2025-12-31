package com.kantic.storeParcelsWS.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Quick Performance Test - Runs fast to generate a report.
 */
public class QuickTestSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://127.0.0.1:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json");

    ScenarioBuilder searchScenario = scenario("Quick Search Test")
        .exec(
            http("Search Store ST001")
                .post("/store_parcels/search")
                .body(StringBody("{\"criterias\":[{\"field\":\"deliveryStoreId\",\"operator\":\"EQ\",\"value\":\"ST001\"}],\"pagination\":{\"page\":1,\"pageSize\":20}}"))
                .check(status().is(200))
        );

    {
        setUp(
            searchScenario.injectOpen(
                rampUsers(50).during(5)  // 50 users over 5 seconds
            )
        ).protocols(httpProtocol);
    }
}
