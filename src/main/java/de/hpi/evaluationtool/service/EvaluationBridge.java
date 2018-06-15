package de.hpi.evaluationtool.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Getter(AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Component
public class EvaluationBridge {

    private final RestTemplate restTemplate;

    @Retryable(
            value = { HttpClientErrorException.class },
            backoff = @Backoff(delay = 3000, multiplier = 5))
    IdealoOffers getSampleOffers(long shopID) {
        return getRestTemplate().getForObject(getSampleOffersURI(shopID), IdealoOffers.class);
    }

    private URI getSampleOffersURI(long shopID) {
        return UriComponentsBuilder.fromUriString("http://localhost:5221")
                .path("/sampleOffers/" + shopID)
                .queryParam("maxCount", 150)
                .build()
                .encode()
                .toUri();
    }

    @Retryable(
            value = { HttpClientErrorException.class },
            backoff = @Backoff(delay = 3000, multiplier = 5))
    String getSamplePage(String pageID) {
        return getRestTemplate().getForObject(getSamplePageURI(pageID), String.class);
    }

    private URI getSamplePageURI(String pageID) {
        return UriComponentsBuilder.fromUriString("http://localhost:5221")
                .path("/fetchPage/" + pageID)
                .build()
                .encode()
                .toUri();
    }
}
