package de.hpi.evaluationtool.service;

import de.hpi.evaluationtool.persistence.SampleOffer;
import de.hpi.evaluationtool.persistence.ShopRules;
import de.hpi.evaluationtool.persistence.repository.ISampleOfferRepository;
import de.hpi.evaluationtool.persistence.repository.ISamplePageRepository;
import de.hpi.evaluationtool.persistence.repository.ShopRulesRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Getter(AccessLevel.PRIVATE)
@RequiredArgsConstructor
public class Evaluation {

    private final ISampleOfferRepository sampleOfferRepository;

    private final ISamplePageRepository samplePageRepository;

    private final ShopRulesRepository shopRulesRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void evaluate() {
        Set<String> collectionNames = getShopRulesRepository().getCollectionNames();
        for (String collection : collectionNames) {
            List<String[]> metricsTable = new LinkedList<>();
            metricsTable.add(createMetricsTableHeader());
            List<String[]> mismatchTable = new LinkedList<>();
            mismatchTable.add(createMismatchTableHeader());
            for (ShopRules rules : getShopRulesRepository().getAllRulesOfCollection(collection)) {
                try {
                    addMetricsFor(rules, metricsTable, mismatchTable);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            try {
                save("./" + collection + "-metrics.csv", metricsTable);
                save("./" + collection + "-mismatches.csv", mismatchTable);
            } catch (IOException e) {
                e.printStackTrace();
            }
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
        return new String[] {"Shop", "Url", "Attribute", "Found", "Wanted"};
    }

    private String[] createMetricsTableHeader() {
        String[] header = new String[1 + 2 * OfferAttribute.values().length];
        header[0] = "Shop";
        for (int iOfferAttribute = 0; iOfferAttribute < OfferAttribute.values().length; iOfferAttribute++) {
            header[1 + iOfferAttribute] = OfferAttribute.values()[iOfferAttribute].toString();
            header[1 + iOfferAttribute + OfferAttribute.values().length] = OfferAttribute.values()[iOfferAttribute].toString();
        }
        return header;
    }

    private void addMetricsFor(ShopRules rules, List<String[]> metricsTable, List<String[]> mismatchTable) throws Exception {
        EnumMap<OfferAttribute, Integer> matchCount = new EnumMap<>(OfferAttribute.class);
        EnumMap<OfferAttribute, Integer> mismatchCount = new EnumMap<>(OfferAttribute.class);
        List<Mismatch> mismatches = new LinkedList<>();
        SampleOffer sampleOffer = getSampleOfferRepository().findByShopID(rules.getShopID())
                .orElseThrow(() -> new Exception("Fatal error occurred. Could not find sampleOffer for shop."));
        for (IdealoOffer offer : sampleOffer.getIdealoOffers()) {
            updateMetricsFor(rules, offer, matchCount, mismatchCount, mismatches);
        }
        updateTables(metricsTable, mismatchTable, matchCount, mismatchCount, mismatches, rules.getShopID());
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
                    mismatch.getExpected().toString()});
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
        String html = getSamplePageRepository().findById(offer.get(OfferAttribute.URL).get(0)).get().getHtml();
        Document document = Jsoup.parse(html);
        Map<OfferAttribute, String> extractedData = Parser.extractData(rules.getSelectorMap(), document);

        for (OfferAttribute attribute : OfferAttribute.values()) {
            if (doesMatch(extractedData.get(attribute), offer.get(attribute))) {
                matchCount.put(attribute, matchCount.getOrDefault(attribute, 0) + 1);
            } else {
                mismatchCount.put(attribute, mismatchCount.getOrDefault(attribute, 0) + 1);
                mismatches.add(new Mismatch(offer.get(attribute), rules.getShopID(), attribute, extractedData.get(attribute), offer.get(OfferAttribute.URL).get(0)));
            }
        }
    }

    private boolean doesMatch(String extractedData, List<String> offerAttributes) {
        return offerAttributes.stream().anyMatch(extractedData::equalsIgnoreCase);
    }

}