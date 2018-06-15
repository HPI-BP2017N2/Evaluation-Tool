package de.hpi.evaluationtool.service;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@EqualsAndHashCode
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "nodeType")
@JsonSubTypes({
        @JsonSubTypes.Type(value = AttributeNodeSelector.class, name = "ATTRIBUTE_NODE"),
        @JsonSubTypes.Type(value = DataNodeSelector.class, name = "DATA_NODE"),
        @JsonSubTypes.Type(value = TextNodeSelector.class, name = "TEXT_NODE")
})
abstract class Selector {

    public enum NodeType {
        ATTRIBUTE_NODE,
        DATA_NODE,
        TEXT_NODE,
    }

    private double normalizedScore;

    private int score;

    private int leftCutIndex;

    private int rightCutIndex;

    private NodeType nodeType;

    private String cssSelector;

    @JsonCreator
    Selector(@JsonProperty(value = "nodeType") NodeType nodeType, @JsonProperty(value = "cssSelector") String
            cssSelector) {
        setNodeType(nodeType);
        setCssSelector(cssSelector);
    }
}
