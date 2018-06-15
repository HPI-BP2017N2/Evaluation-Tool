package de.hpi.evaluationtool.service;

import org.jsoup.nodes.Document;

import java.util.*;

import static de.hpi.evaluationtool.service.Normalizer.normalizeData;

public class Parser {

    public static Map<OfferAttribute, String> extractData(SelectorMap selectorMap, Document
            page) {
        Map<OfferAttribute, String> extractedData = new EnumMap<>(OfferAttribute.class);
        selectorMap.forEach((offerAttribute, selectors) -> {
            if (OfferAttribute.EAN.equals(offerAttribute)) {
                Optional<String> ean = EANExtractor.extract(page);
                if (ean.isPresent()) {
                    extractedData.put(OfferAttribute.EAN, ean.get());
                    return;
                }
            }
            extractedData.put(offerAttribute, getBestMatchFor(selectors, page, offerAttribute));
        });
        return extractedData;
    }

    private static String getBestMatchFor(Set<Selector> selectors, Document page, OfferAttribute offerAttribute) {
        HashMap<String, Double> scores = new HashMap<>();
        selectors.forEach(selector ->
                updateScoreMap(scores,
                        normalizeData(DataExtractor.extract(page, selector), offerAttribute),
                        selector.getNormalizedScore()));
        Optional<Map.Entry<String, Double>> optional = scores.entrySet().stream()
                .filter(entry -> entry.getKey().replace(" ", "").length() > 0)
                .max(Map.Entry.comparingByValue());
        return optional.isPresent() ? optional.get().getKey() : "";
    }

    private static void updateScoreMap(HashMap<String, Double> scores, String content, double normalizedScore) {
        double prevScore = scores.getOrDefault(content, 0.0);
        scores.put(content, prevScore + normalizedScore);
    }
}
