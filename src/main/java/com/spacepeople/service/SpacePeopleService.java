package com.spacepeople.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spacepeople.client.SpacePeopleClient;
import com.spacepeople.model.ClientExecutionResult;
import com.spacepeople.model.SpacePeopleResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SpacePeopleService {
    private static final String API_URL = "http://api.open-notify.org/astros.json";

    private final WebClient webClient;
    private final RestTemplate restTemplate;
    private final RestClient restClient;
    private final HttpClient httpClient;
    private final SpacePeopleClient openFeignClient;

    public SpacePeopleService(WebClient.Builder webClientBuilder,
                              RestTemplateBuilder restTemplateBuilder,
                              RestClient.Builder restClientBuilder,
                              SpacePeopleClient openFeignClient) {
        this.webClient = webClientBuilder.baseUrl(API_URL).build();
        this.restTemplate = restTemplateBuilder.build();
        this.restClient = restClientBuilder.baseUrl(API_URL).build();
        this.httpClient = HttpClient.newHttpClient();
        this.openFeignClient = openFeignClient;
    }

    public void executeAllClients() {
        List<CompletableFuture<ClientExecutionResult>> futures = Arrays.asList(
                CompletableFuture.supplyAsync(() -> executeWithRetry(this::executeWebClient, "WebClient")),
                CompletableFuture.supplyAsync(() -> executeWithRetry(this::executeRestTemplate, "RestTemplate")),
                CompletableFuture.supplyAsync(() -> executeWithRetry(this::executeRestClient, "RestClient")),
                CompletableFuture.supplyAsync(() -> executeWithRetry(this::executeHttpClient, "HttpClient")),
                CompletableFuture.supplyAsync(() -> executeWithRetry(this::executeOpenFeign, "OpenFeign"))
        );

        List<ClientExecutionResult> results = futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList());

        printResults(results);
    }

    private ClientExecutionResult executeWithRetry(Supplier<ClientExecutionResult> clientMethod, String clientName) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return clientMethod.get();
            } catch (HttpClientErrorException.TooManyRequests e) {
                log.warn("{} - Attempt {} failed due to rate limiting", clientName, attempt);
                try {
                    Thread.sleep(1000 * attempt); // Экспоненциальная задержка
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            } catch (Exception e) {
                log.error("{} - Unexpected error", clientName, e);
                break;
            }
        }

        // Возвращаем fallback результат в случае неудачи
        return new ClientExecutionResult(clientName, -1,
                new SpacePeopleResponse("Error", 0, Collections.emptyList()));
    }

    // Добавим паузу между запросами
    private void addDelay() {
        try {
            Thread.sleep(500); // Пауза 500 мс между запросами
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private ClientExecutionResult executeWebClient() {
        addDelay();
        long startTime = System.currentTimeMillis();
        SpacePeopleResponse response = webClient.get()
                .retrieve()
                .bodyToMono(SpacePeopleResponse.class)
                .block();
        long executionTime = System.currentTimeMillis() - startTime;
        return new ClientExecutionResult("WebClient", executionTime, response);
    }

    private ClientExecutionResult executeRestTemplate() {
        addDelay();
        long startTime = System.currentTimeMillis();
        SpacePeopleResponse response = restTemplate.getForObject(API_URL, SpacePeopleResponse.class);
        long executionTime = System.currentTimeMillis() - startTime;
        return new ClientExecutionResult("RestTemplate", executionTime, response);
    }

    private ClientExecutionResult executeRestClient() {
        addDelay();
        long startTime = System.currentTimeMillis();
        SpacePeopleResponse response = restClient.get()
                .retrieve()
                .body(SpacePeopleResponse.class);
        long executionTime = System.currentTimeMillis() - startTime;
        return new ClientExecutionResult("RestClient", executionTime, response);
    }

    private ClientExecutionResult executeHttpClient() {
        addDelay();
        long startTime = System.currentTimeMillis();
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            ObjectMapper mapper = new ObjectMapper();
            SpacePeopleResponse spacePeopleResponse = mapper.readValue(response.body(), SpacePeopleResponse.class);

            long executionTime = System.currentTimeMillis() - startTime;
            return new ClientExecutionResult("HttpClient", executionTime, spacePeopleResponse);
        } catch (Exception e) {
            throw new RuntimeException("Error executing HttpClient", e);
        }
    }

    private ClientExecutionResult executeOpenFeign() {
        addDelay();
        long startTime = System.currentTimeMillis();
        SpacePeopleResponse response = openFeignClient.getPeopleInSpace();
        long executionTime = System.currentTimeMillis() - startTime;
        return new ClientExecutionResult("OpenFeign", executionTime, response);
    }

    private void printResults(List<ClientExecutionResult> results) {
        System.out.println("\nРезультаты запросов:");
        results.forEach(result ->
                System.out.printf("- %s: %d ms%n", result.getClientName(), result.getExecutionTime()));

        ClientExecutionResult slowest = results.stream()
                .max(Comparator.comparing(ClientExecutionResult::getExecutionTime))
                .orElseThrow();

        ClientExecutionResult fastest = results.stream()
                .min(Comparator.comparing(ClientExecutionResult::getExecutionTime))
                .orElseThrow();

        System.out.printf("\nСамый долгий запрос: %s (%d ms)%n",
                slowest.getClientName(), slowest.getExecutionTime());
        System.out.printf("Самый быстрый запрос: %s (%d ms)%n",
                fastest.getClientName(), fastest.getExecutionTime());
    }
}

