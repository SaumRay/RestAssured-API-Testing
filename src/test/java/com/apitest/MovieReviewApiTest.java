package com.apitest;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static io.restassured.RestAssured.*;
import static org.hamcrest.Matchers.*;

public class MovieReviewApiTest {

    private static final String API_KEY = "reqres_c1522612d64c40bf968914ef85391df9"; // 🔑 Your ReqRes key

    @BeforeClass
    public void setup() {
        RestAssured.baseURI = "https://reqres.in";
    }

    // =============================================
    // PHASE 1 ✅ - GET Tests
    // =============================================

    @Test
    public void testGetAllMovieReviews() {
        System.out.println("\n===== GET ALL REVIEWS =====");

        given()
                .header("x-api-key", API_KEY)
                .queryParam("page", 1)
                .when()
                .get("/api/users")
                .then()
                .statusCode(200)
                .body("page", equalTo(1))
                .body("data", not(empty()))
                .log().body();
    }

    @Test
    public void testGetSingleMovieReview() {
        System.out.println("\n===== GET SINGLE REVIEW (ID=2) =====");

        given()
                .header("x-api-key", API_KEY)
                .when()
                .get("/api/users/2")
                .then()
                .statusCode(200)
                .body("data.id", equalTo(2))
                .body("data.first_name", notNullValue())
                .body("data.email", notNullValue())
                .log().body();
    }

    @Test
    public void testGetMovieReview_NotFound() {
        System.out.println("\n===== GET REVIEW - NOT FOUND (Negative Test) =====");

        given()
                .header("x-api-key", API_KEY)
                .when()
                .get("/api/users/9999")
                .then()
                .statusCode(404)
                .log().status();

        System.out.println("✅ 404 returned as expected for invalid ID");
    }

    // =============================================
    // PHASE 2 - POST Test
    // =============================================

    @Test
    public void testPostNewMovieReview() {
        System.out.println("\n===== POST NEW REVIEW =====");

        String reviewPayload = "{\n" +
                "  \"movie\": \"Inception\",\n" +
                "  \"director\": \"Christopher Nolan\",\n" +
                "  \"rating\": 9.5,\n" +
                "  \"review\": \"Mind-bending masterpiece!\",\n" +
                "  \"reviewer\": \"John Doe\"\n" +
                "}";

        given()
                .header("x-api-key", API_KEY)
                .header("Content-Type", "application/json")
                .body(reviewPayload)
                .when()
                .post("/api/users")
                .then()
                .statusCode(201)                          // 201 = Created
                .body("movie", equalTo("Inception"))
                .body("director", equalTo("Christopher Nolan"))
                .body("id", notNullValue())               // Auto-generated ID
                .body("createdAt", notNullValue())        // Timestamp added
                .log().body();
    }

    // =============================================
    // PHASE 3 - PUT Test (Full Update)
    // =============================================

    @Test
    public void testPutUpdateMovieReview() {
        System.out.println("\n===== PUT - FULL UPDATE OF REVIEW =====");

        String updatedReview = "{\n" +
                "  \"movie\": \"Inception\",\n" +
                "  \"director\": \"Christopher Nolan\",\n" +
                "  \"rating\": 10,\n" +
                "  \"review\": \"Perfect film — updated full review!\",\n" +
                "  \"reviewer\": \"Jane Smith\"\n" +
                "}";

        given()
                .header("x-api-key", API_KEY)
                .header("Content-Type", "application/json")
                .body(updatedReview)
                .when()
                .put("/api/users/2")
                .then()
                .statusCode(200)
                .body("movie", equalTo("Inception"))
                .body("rating", equalTo(10))
                .body("reviewer", equalTo("Jane Smith"))
                .body("updatedAt", notNullValue())        // Timestamp updated
                .log().body();
    }

    // =============================================
    // PHASE 4 - PATCH Test (Partial Update)
    // =============================================

    @Test
    public void testPatchMovieReviewRating() {
        System.out.println("\n===== PATCH - PARTIAL UPDATE (Rating only) =====");

        // Only updating rating and review comment — NOT entire object
        String partialUpdate = "{\n" +
                "  \"rating\": 8.5,\n" +
                "  \"review\": \"Great but slightly overrated on rewatch.\"\n" +
                "}";

        given()
                .header("x-api-key", API_KEY)
                .header("Content-Type", "application/json")
                .body(partialUpdate)
                .when()
                .patch("/api/users/2")
                .then()
                .statusCode(200)
                .body("rating", equalTo(8.5F))
                .body("review", equalTo("Great but slightly overrated on rewatch."))
                .body("updatedAt", notNullValue())
                .log().body();
    }

    // =============================================
    // PHASE 5 - DELETE Test
    // =============================================

    @Test
    public void testDeleteMovieReview() {
        System.out.println("\n===== DELETE REVIEW (ID=2) =====");

        given()
                .header("x-api-key", API_KEY)
                .when()
                .delete("/api/users/2")
                .then()
                .statusCode(204);   // 204 = No Content (successful delete)

        System.out.println("✅ Review deleted successfully — 204 No Content received");
    }

    // =============================================
    // BONUS - Extract Response Value
    // =============================================

    @Test
    public void testExtractAndPrintReviewerName() {
        System.out.println("\n===== EXTRACT VALUE FROM RESPONSE =====");

        Response response =
                given()
                        .header("x-api-key", API_KEY)
                        .when()
                        .get("/api/users/2")
                        .then()
                        .statusCode(200)
                        .extract().response();

        // Extract specific fields from response
        String firstName = response.jsonPath().getString("data.first_name");
        String lastName  = response.jsonPath().getString("data.last_name");
        String email     = response.jsonPath().getString("data.email");

        System.out.println("Reviewer Name : " + firstName + " " + lastName);
        System.out.println("Reviewer Email: " + email);

        // Assert extracted values
        assert firstName != null : "First name should not be null";
        assert email.contains("@") : "Email should contain @";
    }
}