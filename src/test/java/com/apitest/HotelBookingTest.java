package com.apitest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.*;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static io.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchemaInClasspath;
import static org.hamcrest.Matchers.*;

public class HotelBookingTest {

    private static String authToken;      // 🔑 Token from auth call
    private static int createdBookingId;  // 🔗 ID from POST → used in GET/PUT/DELETE

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "https://restful-booker.herokuapp.com";
    }

    // =============================================
    // PHASE 1 — AUTH TOKEN (Login)
    // =============================================

    @Test(priority = 1)
    public void testGenerateAuthToken() {
        System.out.println("\n===== PHASE 1: AUTH - Generate Token =====");

        String credentials = """
        {
            "username": "admin",
            "password": "password123"
        }
        """;

        Response response =
                given()
                        .header("Content-Type", "application/json")
                        .body(credentials)
                        .when()
                        .post("/auth")
                        .then()
                        .statusCode(200)
                        .body("token", notNullValue())
                        .log().body()
                        .extract().response();

        // Extract token for use in later tests
        authToken = response.jsonPath().getString("token");
        System.out.println("Token Generated: " + authToken);
    }

    // =============================================
    // PHASE 2 — CHAINING: POST → capture ID → use in GET
    // =============================================

    @Test(priority = 2)
    public void testCreateBooking() {
        System.out.println("\n===== PHASE 2: POST - Create New Booking =====");

        String newBooking = """
        {
            "firstname": "Saumarghya",
            "lastname": "Ray",
            "totalprice": 750,
            "depositpaid": true,
            "bookingdates": {
                "checkin": "2024-06-02",
                "checkout": "2024-06-09"
            },
            "additionalneeds": "Breakfast"
        }
        """;

        Response response =
                given()
                        .header("Content-Type", "application/json")
                        .body(newBooking)
                        .when()
                        .post("/booking")
                        .then()
                        .statusCode(200)
                        .body("bookingid", notNullValue())
                        .body("booking.firstname", equalTo("Saumarghya"))
                        .body("booking.totalprice", equalTo(750))
                        .body("booking.depositpaid", equalTo(true))
                        //  SCHEMA VALIDATION
                        .body(matchesJsonSchemaInClasspath("booking-schema.json"))
                        .log().body()
                        .extract().response();

        // 🔗 Capture ID for chaining into next tests
        createdBookingId = response.jsonPath().getInt("bookingid");
        System.out.println("Booking Created with ID: " + createdBookingId);
    }

    @Test(priority = 3, dependsOnMethods = "testCreateBooking")
    public void testGetCreatedBooking() {
        System.out.println("\n===== PHASE 2: GET - Fetch Booking Using Chained ID =====");
        System.out.println("Using booking ID from POST: " + createdBookingId);

        given()
                .header("Content-Type", "application/json")
                .when()
                .get("/booking/" + createdBookingId)
                .then()
                .statusCode(200)
                .body("firstname", equalTo("Saumarghya"))
                .body("lastname", equalTo("Ray"))
                .body("totalprice", equalTo(750))
                .log().body();

        System.out.println("Successfully fetched booking using chained ID!");
    }

    // =============================================
    // PHASE 3 — DATA DRIVEN TESTING with TestNG
    // =============================================

    // Different guest data sets
    @DataProvider(name = "guestData")
    public Object[][] provideGuestData() {
        return new Object[][] {
                // { firstname, lastname, price,  deposit, checkin,      checkout }
                { "Tushar", "Paul", 150, true,  "2024-07-02", "2024-07-04" },
                { "Aaditya", "Bhowmik",300, false, "2024-08-10", "2024-08-15" },
                { "Arijit", "Das",  450, true,  "2024-09-01", "2024-09-10" }
        };
    }

    @Test(priority = 4, dataProvider = "guestData")
    public void testCreateMultipleBookings(
            String firstName, String lastName,
            int price, boolean deposit,
            String checkin, String checkout) {

        System.out.println("\n===== PHASE 3: DATA DRIVEN - Booking for " + firstName + " =====");

        String bookingPayload = """
        {
            "firstname": "%s",
            "lastname": "%s",
            "totalprice": %d,
            "depositpaid": %b,
            "bookingdates": {
                "checkin": "%s",
                "checkout": "%s"
            }
        }
        """.formatted(firstName, lastName, price, deposit, checkin, checkout);

        given()
                .header("Content-Type", "application/json")
                .body(bookingPayload)
                .when()
                .post("/booking")
                .then()
                .statusCode(200)
                .body("booking.firstname", equalTo(firstName))
                .body("booking.lastname", equalTo(lastName))
                .body("booking.totalprice", equalTo(price))
                .log().body();

        System.out.println("✅ Booking created for: " + firstName + " " + lastName);
    }

    // =============================================
    // PHASE 4 — PUT (Full Update with Auth Token)
    // =============================================

    @Test(priority = 5, dependsOnMethods = {"testGenerateAuthToken", "testCreateBooking"})
    public void testUpdateBooking() {
        System.out.println("\n===== PHASE 4: PUT - Full Update (With Auth Token) =====");
        System.out.println("Updating booking ID: " + createdBookingId);

        String updatedBooking = """
        {
            "firstname": "Saumarghya",
            "lastname": "Ray",
            "totalprice": 750,
            "depositpaid": true,
            "bookingdates": {
                "checkin": "2024-06-02",
                "checkout": "2024-06-09"
            },
            "additionalneeds": "Breakfast,Dinner and Geyser"
        }
        """;

        given()
                // 🔑 Auth token used here
                .header("Cookie", "token=" + authToken)
                .header("Content-Type", "application/json")
                .body(updatedBooking)
                .when()
                .put("/booking/" + createdBookingId)
                .then()
                .statusCode(200)
                .body("totalprice", equalTo(750))
                .body("additionalneeds", equalTo("Breakfast,Dinner and Geyser"))
                .log().body();

        System.out.println("Booking fully updated with auth token!");
    }

    // =============================================
    // PHASE 5 — PATCH (Partial Update with Auth)
    // =============================================

    @Test(priority = 6, dependsOnMethods = {"testGenerateAuthToken", "testCreateBooking"})
    public void testPartialUpdateBooking() {
        System.out.println("\n===== PHASE 5: PATCH - Partial Update =====");

        String partialUpdate = "{\n" +
                "  \"totalprice\": 999,\n" +
                "  \"additionalneeds\": \"Airport Pickup\"\n" +
                "}";

        given()
                .header("Cookie", "token=" + authToken)
                .header("Content-Type", "application/json")
                .body(partialUpdate)
                .when()
                .patch("/booking/" + createdBookingId)
                .then()
                .statusCode(200)
                .body("totalprice", equalTo(999))
                .body("additionalneeds", equalTo("Airport Pickup"))
                .log().body();

        System.out.println("Booking partially updated!");
    }

    // =============================================
    // PHASE 6 — DELETE (With Auth Token)
    // =============================================

    @Test(priority = 7, dependsOnMethods = {"testGenerateAuthToken", "testCreateBooking"})
    public void testDeleteBooking() {
        System.out.println("\n===== PHASE 6: DELETE - Remove Booking =====");
        System.out.println("Deleting booking ID: " + createdBookingId);

        given()
                .header("Cookie", "token=" + authToken)
                .when()
                .delete("/booking/" + createdBookingId)
                .then()
                .statusCode(201); // Restful Booker returns 201 on delete

        System.out.println("Booking " + createdBookingId + " deleted successfully!");
    }

    // =============================================
    // PHASE 7 — NEGATIVE: Verify Deleted Booking
    // =============================================

    @Test(priority = 8, dependsOnMethods = "testDeleteBooking")
    public void testVerifyBookingDeleted() {
        System.out.println("\n===== PHASE 7: NEGATIVE TEST - Verify Deletion =====");

        given()
                .header("Content-Type", "application/json")
                .when()
                .get("/booking/" + createdBookingId)
                .then()
                .statusCode(404);

        System.out.println("Confirmed — booking no longer exists (404)!");
    }
}