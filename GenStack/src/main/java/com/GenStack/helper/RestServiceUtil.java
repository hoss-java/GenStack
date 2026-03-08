package com.GenStack.helper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class RestServiceUtil {
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    // Method to check the availability of a REST service
    public static boolean isServiceAvailable(String serviceUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl))
                    .method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(java.time.Duration.ofSeconds(5))
                    .build();

            HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
            return (response.statusCode() == 200);
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    // Method to call a REST service with a payload and return the response
    public static String callRestService(String serviceUrl, String payload) {
        String responseBody = "";
        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(serviceUrl))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .header("Content-Type", "application/json");

            if (payload != null) {
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(payload));
            } else {
                requestBuilder.method("GET", HttpRequest.BodyPublishers.noBody()); // Or another method if needed
            }

            HttpRequest request = requestBuilder.build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                responseBody = response.body();
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
        return responseBody;
    }
}

