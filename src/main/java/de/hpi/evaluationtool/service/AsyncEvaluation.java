package de.hpi.evaluationtool.service;

import de.hpi.evaluationtool.persistence.ShopRules;
import de.hpi.evaluationtool.persistence.repository.ShopRulesRepository;
import info.debatty.java.stringsimilarity.Levenshtein;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Getter(AccessLevel.PRIVATE)
@Setter(AccessLevel.PRIVATE)
@Component
@RequiredArgsConstructor
public class AsyncEvaluation {

    private final EvaluationBridge evaluationBridge;

    private final RestTemplate restTemplate;

    private final ShopRulesRepository shopRulesRepository;

    private final Levenshtein levenshtein = new Levenshtein();

    @Async("threadpool")
    public void saveEvaluationForCollection(String collection) {
        List<String[]> metricsTable = new LinkedList<>();
        metricsTable.add(createMetricsTableHeader());
        List<String[]> mismatchTable = new LinkedList<>();
        mismatchTable.add(createMismatchTableHeader());
        for (ShopRules rules : getShopRulesRepository().getAllRulesOfCollection(collection)) {
            System.out.println("Processing shop " + rules.getShopID() + " of " + collection);
            try {
                addMetricsFor(rules, metricsTable, mismatchTable);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Completed collection " + collection);
        try {
            save("./" + collection + "-metrics.csv", metricsTable);
            save("./" + collection + "-mismatches.csv", mismatchTable);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void save(String fileName, List<String[]> table) throws IOException {
        try (
                BufferedWriter writer = Files.newBufferedWriter(Paths.get(fileName));
                CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(table.get(0)));
        ) {
            for (int iRow = 1; iRow < table.size(); iRow++)
                csvPrinter.printRecord(Arrays.asList(table.get(iRow)));
            csvPrinter.flush();
        }
    }

    private String[] createMismatchTableHeader() {
        return new String[] {"Shop", "Url", "Attribute", "Found", "Wanted", "Distance"};
    }

    private String[] createMetricsTableHeader() {
        String[] header = new String[1 + 2 * OfferAttribute.values().length];
        header[0] = "Shop";
        for (int iOfferAttribute = 0; iOfferAttribute < OfferAttribute.values().length; iOfferAttribute++) {
            header[1 + iOfferAttribute] = OfferAttribute.values()[iOfferAttribute].toString();
            header[1 + iOfferAttribute + OfferAttribute.values().length] = "MM_" + OfferAttribute.values()[iOfferAttribute].toString();
        }
        return header;
    }

    private void addMetricsFor(ShopRules rules, List<String[]> metricsTable, List<String[]> mismatchTable) throws Exception {
        EnumMap<OfferAttribute, Integer> matchCount = createEmptyMap();
        EnumMap<OfferAttribute, Integer> mismatchCount = createEmptyMap();
        List<Mismatch> mismatches = new LinkedList<>();
        IdealoOffers idealoOffers = getEvaluationBridge().getSampleOffers(rules.getShopID());
        for (int iOffer = 50; iOffer < idealoOffers.size(); iOffer++) {
            updateMetricsFor(rules, idealoOffers.get(iOffer), matchCount, mismatchCount, mismatches);
        }
        updateTables(metricsTable, mismatchTable, matchCount, mismatchCount, mismatches, rules.getShopID());
    }

    private EnumMap<OfferAttribute, Integer> createEmptyMap(){
        EnumMap<OfferAttribute, Integer> map = new EnumMap<>(OfferAttribute.class);
        Arrays.stream(OfferAttribute.values()).forEach(offerAttribute -> map.put(offerAttribute, 0));
        return map;
    }

    private void updateTables(List<String[]> metricsTable, List<String[]> mismatchTable, EnumMap<OfferAttribute, Integer> matchCount, EnumMap<OfferAttribute, Integer> mismatchCount, List<Mismatch> mismatches, long shopID) {
        updateMetricsTable(metricsTable, matchCount, mismatchCount, shopID);
        updateMismatchTable(mismatchTable, mismatches);
    }

    private void updateMismatchTable(List<String[]> mismatchTable, List<Mismatch> mismatches) {
        for (Mismatch mismatch : mismatches) {
            mismatchTable.add(new String[] {
                    Long.toString(mismatch.getShopID()),
                    mismatch.getUrl(),
                    mismatch.getOfferAttribute().toString(),
                    mismatch.getActual(),
                    mismatch.getExpected().toString(),
                    Integer.toString(mismatch.getDistance())});
        }
    }

    private void updateMetricsTable(List<String[]> metricsTable, EnumMap<OfferAttribute, Integer> matchCount, EnumMap<OfferAttribute, Integer> mismatchCount, long shopID) {
        String[] row = new String[1 + 2 * OfferAttribute.values().length];
        row[0] = Long.toString(shopID);
        for (int iOfferAttribute = 0; iOfferAttribute < OfferAttribute.values().length; iOfferAttribute++) {
            row[1 + iOfferAttribute] = Integer.toString(matchCount.get(OfferAttribute.values()[iOfferAttribute]));
        }
        for (int iOfferAttribute = 0; iOfferAttribute < OfferAttribute.values().length; iOfferAttribute++) {
            row[1 + iOfferAttribute + OfferAttribute.values().length] = Integer.toString(mismatchCount.get(OfferAttribute.values()[iOfferAttribute]));
        }
        metricsTable.add(row);
    }

    private void updateMetricsFor(ShopRules rules, IdealoOffer offer, EnumMap<OfferAttribute, Integer>
            matchCount, EnumMap<OfferAttribute, Integer> mismatchCount, List<Mismatch> mismatches) {
        String html = getEvaluationBridge().getSamplePage(offer.get(OfferAttribute.URL).get(0).replace
                ("http://localhost:5221/fetchPage/", "")
        );
        Document document = Jsoup.parse(html);
        Map<OfferAttribute, String> extractedData = Parser.extractData(rules.getSelectorMap(), document);

        for (OfferAttribute attribute : OfferAttribute.values()) {
            if (doesMatch(attribute, extractedData.get(attribute), offer.get(attribute))) {
                matchCount.put(attribute, matchCount.getOrDefault(attribute, 0) + 1);
            } else if (extractedData.get(attribute).replace(" ", "").length() > 0) {
                mismatchCount.put(attribute, mismatchCount.getOrDefault(attribute, 0) + 1);
                mismatches.add(new Mismatch(
                        calculateDistance(extractedData.get(attribute), offer.get(attribute)),
                        offer.get(attribute),
                        rules.getShopID(),
                        attribute,
                        extractedData.get(attribute),
                        offer.get(OfferAttribute.URL).get(0)));
            }
        }
    }

    private int calculateDistance(String extractedData, List<String> offerAttributes) {
        return offerAttributes.stream().map(offerAttribute -> (int) getLevenshtein().distance(extractedData, offerAttribute)).min
                (Integer::compare).orElse(-1);
    }

    private String removeLeadingZeroes(String s) {
        int startIndex = 0;
        for (int iChar = 0; iChar < s.length(); iChar++)
            if (s.charAt(iChar) == '0') startIndex++;
            else break;
        return s.substring(startIndex);
    }

    private boolean doesMatch(OfferAttribute attribute, String extractedData, List<String> offerAttributes) {
        extractedData = extractedData.replace("  ", " ");
        if (OfferAttribute.EAN.equals(attribute)) {
            extractedData = removeLeadingZeroes(extractedData);
            for (int iEAN = 0; iEAN < offerAttributes.size(); iEAN++)
                offerAttributes.set(iEAN, removeLeadingZeroes(offerAttributes.get(iEAN)));
        }
        if (OfferAttribute.IMAGE_URLS.equals(attribute)) {
            final String data = extractedData;
            return offerAttributes.stream().anyMatch(offerAttribute -> offerAttribute.endsWith(data) ||
                    data.endsWith(offerAttribute));
        }
        return offerAttributes.stream().anyMatch(extractedData::equalsIgnoreCase);
    }
}
