package de.hpi.evaluationtool.persistence;

import com.fasterxml.jackson.annotation.JsonProperty;
import de.hpi.evaluationtool.service.SelectorMap;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.annotation.Id;

@Getter
@ToString
@Setter
public class ShopRules {

    private SelectorMap selectorMap;

    @Id
    @JsonProperty(value = "_id")
    private long shopID;
}
