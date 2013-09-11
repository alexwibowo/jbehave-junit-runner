package de.codecentric.jbehave.junit.monitoring;

import org.jbehave.core.embedder.Embedder;
import org.jbehave.core.steps.InjectableStepsFactory;

import java.util.List;

public interface JBehaveTest {

    Embedder configuredEmbedder();


    InjectableStepsFactory getInjectableStepsFactory();


    List<String> storyPaths();

}
