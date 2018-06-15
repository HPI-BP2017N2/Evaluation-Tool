package de.hpi.evaluationtool.service;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public class Mismatch {

    private final int distance;

    private final List<String> expected;

    private final long shopID;

    private final OfferAttribute offerAttribute;

    private final String actual;

    private final String url;

}
