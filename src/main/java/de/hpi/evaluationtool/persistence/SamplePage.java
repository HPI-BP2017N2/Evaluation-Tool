package de.hpi.evaluationtool.persistence;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.PersistenceConstructor;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "samplePages")
@Getter
@RequiredArgsConstructor(onConstructor = @__({@PersistenceConstructor}))
public class SamplePage {

    @Id
    private final String id;

    private final String html;

}
