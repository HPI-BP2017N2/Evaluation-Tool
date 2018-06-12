package de.hpi.evaluationtool.service;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jsoup.nodes.Document;
import org.springframework.data.annotation.AccessType;

import java.util.*;
import java.util.stream.Collectors;

@Setter(AccessLevel.PRIVATE)
@Getter
public class IdealoOffer {

    private EnumMap<OfferAttribute, List<String>> offerAttributes;

    @Setter private Document fetchedPage;

    public IdealoOffer() {
        setOfferAttributes(new EnumMap<>(OfferAttribute.class));
        Arrays.stream(OfferAttribute.values()).forEach(offerAttribute -> getOfferAttributes().put(offerAttribute, new
                LinkedList<>()));
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setEan(Property<String> ean) {
        getOfferAttributes().put(OfferAttribute.EAN, toList(ean.getValue()));
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setSku(Property<String> sku) {
        getOfferAttributes().put(OfferAttribute.SKU, toList(sku.getValue()));
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setHan(Property<String> han) {
        getOfferAttributes().put(OfferAttribute.HAN, toList(han.getValue()));
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setTitles(Property<Map<String, String>> titles) {
        getOfferAttributes().put(OfferAttribute.TITLE, toList(titles.getValue()));
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setCategoryPaths(Property<String[]> categoryPaths) {
        getOfferAttributes().put(OfferAttribute.CATEGORY, toList(categoryPaths.getValue()));
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setBrandName(Property<String> brandName) {
        getOfferAttributes().put(OfferAttribute.BRAND, toList(brandName.getValue()));
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setPrices(Property<Map<String, Integer>> prices) {
        getOfferAttributes().put(OfferAttribute.PRICE, toList(prices.getValue()));
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setDescriptions(Property<Map<String, String>> descriptions) {
        getOfferAttributes().put(OfferAttribute.DESCRIPTION, toList(descriptions.getValue()));
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setUrls(Property<Map<String, String>> urls) {
        getOfferAttributes().put(OfferAttribute.URL, toList(urls.getValue()));
    }

    @AccessType(AccessType.Type.PROPERTY)
    public void setImageUrls(Property<Map<String, List<String>>> imageUrls) {
        getOfferAttributes().put(OfferAttribute.IMAGE_URLS, mapWithArrayToList(imageUrls.getValue()));
    }

    List<String> get(OfferAttribute attribute) {
        return getOfferAttributes().get(attribute);
    }

    boolean has(OfferAttribute attribute) { return getOfferAttributes().containsKey(attribute); }


    //convert
    private List<String> toList(Object object) {
        return new LinkedList<>(Collections.singletonList(String.valueOf(object)));
    }

    private List<String> toList(Map<String, ?> map) {
        return map.values().stream().map(String::valueOf).collect(Collectors.toList());
    }

    private List<String> mapWithArrayToList(Map<String, List<String>> map) {
        return map.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    private List<String> toList(String[] array) {
        return Arrays.asList(array);
    }
}
