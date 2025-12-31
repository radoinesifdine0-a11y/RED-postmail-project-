package com.kantic.storeParcelsWS.simulation;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

/**
 * Full Performance Test Simulation for StoreParcelAPI.
 *
 * Run with: mvn gatling:test -Dgatling.simulationClass=com.kantic.storeParcelsWS.simulation.ParcelSearchSimulation
 */
public class ParcelSearchSimulation extends Simulation {

    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://127.0.0.1:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json");

    // Scenario 1: Simple search (no filters)
    ScenarioBuilder simpleSearch = scenario("Simple Search")
        .exec(
            http("Search All Parcels")
                .post("/store_parcels/search")
                .body(StringBody("{\"criterias\":[],\"pagination\":{\"page\":1,\"pageSize\":20}}"))
                .check(status().is(200))
        );

    // Scenario 2: Search by store
    ScenarioBuilder storeSearch = scenario("Store Search")
        .exec(
            http("Search by Store ST001")
                .post("/store_parcels/search")
                .body(StringBody("{\"criterias\":[{\"field\":\"deliveryStoreId\",\"operator\":\"EQ\",\"value\":\"ST001\"}],\"pagination\":{\"page\":1,\"pageSize\":20}}"))
                .check(status().is(200))
        );

    // Scenario 3: Search by store + status
    ScenarioBuilder storeStatusSearch = scenario("Store + Status Search")
        .exec(
            http("Search ST001 READY_FOR_PICKUP")
                .post("/store_parcels/search")
                .body(StringBody("{\"criterias\":[{\"field\":\"deliveryStoreId\",\"operator\":\"EQ\",\"value\":\"ST001\"},{\"field\":\"parcelStatus\",\"operator\":\"EQ\",\"value\":\"READY_FOR_PICKUP\"}],\"pagination\":{\"page\":1,\"pageSize\":20}}"))
                .check(status().is(200))
        );

    // Scenario 4: Search by customer name (LIKE query)
    ScenarioBuilder customerSearch = scenario("Customer Search")
        .exec(
            http("Search Customer John")
                .post("/store_parcels/search")
                .body(StringBody("{\"criterias\":[{\"field\":\"customer.firstName\",\"operator\":\"LIKE\",\"value\":\"John\"}],\"pagination\":{\"page\":1,\"pageSize\":20}}"))
                .check(status().is(200))
        );

    // Scenario 5: Random store search
    ScenarioBuilder randomStoreSearch = scenario("Random Store Search")
        .exec(session -> {
            int storeNum = 1 + (int)(Math.random() * 10);
            String storeId = String.format("ST%03d", storeNum);
            return session.set("storeId", storeId);
        })
        .exec(
            http("Search Random Store")
                .post("/store_parcels/search")
                .body(StringBody("{\"criterias\":[{\"field\":\"deliveryStoreId\",\"operator\":\"EQ\",\"value\":\"#{storeId}\"}],\"pagination\":{\"page\":1,\"pageSize\":20}}"))
                .check(status().is(200))
        );

    {
        // 20 users/sec for 30 seconds = 600 total requests
        setUp(
            simpleSearch.injectOpen(constantUsersPerSec(2).during(30)),
            storeSearch.injectOpen(constantUsersPerSec(4).during(30)),
            storeStatusSearch.injectOpen(constantUsersPerSec(4).during(30)),
            customerSearch.injectOpen(constantUsersPerSec(2).during(30)),
            randomStoreSearch.injectOpen(constantUsersPerSec(8).during(30))
        ).protocols(httpProtocol)
         .assertions(
             global().responseTime().percentile(95.0).lt(60),   // P95 < 60ms (TARGET!)
             global().successfulRequests().percent().gt(99.0)   // >99% success rate
         );
    }
}
