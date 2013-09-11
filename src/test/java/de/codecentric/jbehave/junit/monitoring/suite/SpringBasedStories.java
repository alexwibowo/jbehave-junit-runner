package de.codecentric.jbehave.junit.monitoring.suite;

import de.codecentric.jbehave.junit.monitoring.JBehaveTest;
import de.codecentric.jbehave.junit.monitoring.SpringJUnitReportingRunner;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.embedder.Embedder;
import org.jbehave.core.io.StoryFinder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import java.util.List;

import static java.util.Arrays.asList;
import static org.jbehave.core.io.CodeLocations.codeLocationFromClass;

@RunWith(SpringJUnitReportingRunner.class)
@ContextConfiguration(
        loader = AnnotationConfigContextLoader.class,
        classes = SpringBasedStoryConfig.class
)
public class SpringBasedStories implements JBehaveTest{

    @Autowired
    protected Embedder embedder;

    @Autowired
    protected Configuration configuration;

    @Autowired
    protected InjectableStepsFactory injectableStepsFactory;

    @Override
    public Embedder configuredEmbedder() {
        return embedder;
    }

    @Override
    public InjectableStepsFactory getInjectableStepsFactory() {
        return injectableStepsFactory;
    }

    @Override
    public List<String> storyPaths() {
        return new StoryFinder().findPaths(
                codeLocationFromClass(this.getClass()).getFile(),
                asList("de/codecentric/jbehave/junit/monitoring/simple_multiplication.story"),
                asList("")
        );
    }

    @Test
    public void run() throws Throwable {
        Embedder embedder = configuredEmbedder();
        try {
            embedder.runStoriesAsPaths(storyPaths());
        } finally {
            embedder.generateCrossReference();
        }
    }
}
