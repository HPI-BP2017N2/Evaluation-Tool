package de.hpi.evaluationtool.service;

import de.hpi.evaluationtool.persistence.repository.ShopRulesRepository;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Set;

@Getter(AccessLevel.PRIVATE)
@RequiredArgsConstructor
@Component
public class Evaluation {

    private final AsyncEvaluation asyncEvaluation;

    private final ShopRulesRepository shopRulesRepository;

    @EventListener(ApplicationReadyEvent.class)
    public void evaluate() {
        Set<String> collectionNames = getShopRulesRepository().getCollectionNames();
        for (String collection : collectionNames) {
            getAsyncEvaluation().saveEvaluationForCollection(collection);
        }
    }

}