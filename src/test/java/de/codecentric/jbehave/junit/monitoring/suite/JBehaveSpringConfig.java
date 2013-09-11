package de.codecentric.jbehave.junit.monitoring.suite;

import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.configuration.spring.SpringStoryControls;
import org.jbehave.core.configuration.spring.SpringStoryReporterBuilder;
import org.jbehave.core.embedder.*;
import org.jbehave.core.failures.FailingUponPendingStep;
import org.jbehave.core.failures.PendingStepStrategy;
import org.jbehave.core.failures.RethrowingFailure;
import org.jbehave.core.i18n.LocalizedKeywords;
import org.jbehave.core.io.AbsolutePathCalculator;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.model.ExamplesTableFactory;
import org.jbehave.core.parsers.RegexStoryParser;
import org.jbehave.core.parsers.StoryParser;
import org.jbehave.core.reporters.CrossReference;
import org.jbehave.core.reporters.Format;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.StepMonitor;
import org.jbehave.core.steps.spring.SpringStepsFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Locale;

@Configuration
public class JBehaveSpringConfig {

    @Autowired
    protected ApplicationContext context;

    @Value("${jbehave.storytimeout.sec:300}")
    private int storyTimeoutInSecs;

    @Value("${jbehave.verboseFailures:true}")
    private boolean verboseFailures;

    @Value("${jbehave.ignoreFailureInStories:false}")
    private boolean ignoreFailureInStories;

    @Value("${jbehave.ignoreFailureInView:false}")
    private boolean ignoreFailureInView;

    @Value("${jbehave.skipScenariosAfterFailure:false}")
    private boolean skipScenariosAfterFailure;

    @Bean
    public org.jbehave.core.configuration.Configuration configuration() {
        return new MostUsefulConfiguration()
                .useFailureStrategy(new RethrowingFailure())
                .useKeywords(new LocalizedKeywords(Locale.ENGLISH))
                .usePathCalculator(new AbsolutePathCalculator())
                .useStoryControls(storyControls())
                .useStoryParser(storyParser())
                .usePendingStepStrategy(getPendingStepStrategy())
                .useStepMonitor(stepMonitor())
                .useStoryReporterBuilder(storyReporterBuilder());
    }

    protected PendingStepStrategy getPendingStepStrategy() {
        return new FailingUponPendingStep();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        propertySourcesPlaceholderConfigurer.setIgnoreResourceNotFound(true);
        propertySourcesPlaceholderConfigurer.setIgnoreUnresolvablePlaceholders(false);
        return propertySourcesPlaceholderConfigurer;
    }

    @Bean
    public StoryParser storyParser() {
        StoryParser parser = new RegexStoryParser(
                new ExamplesTableFactory(
                        new LoadFromClasspath(this.getClass())
                )
        );
        return parser;
    }

    @Bean
    public StepMonitor stepMonitor() {
        return crossReference().getStepMonitor();
    }

    @Bean
    public StoryReporterBuilder storyReporterBuilder() {
        Format[] formats = {
            org.jbehave.core.reporters.Format.CONSOLE,
                org.jbehave.core.reporters.Format.HTML
        };
        return new SpringStoryReporterBuilder()
                .withFailureTrace(true)
                .withFailureTraceCompression(true)
                .withDefaultFormats()
                .withCrossReference(crossReference())
                .withFormats(formats);

    }

    @Bean
    public StoryControls storyControls() {
        return new SpringStoryControls()
                .doDryRun(false)
                .doSkipScenariosAfterFailure(skipScenariosAfterFailure)
                .doResetStateBeforeScenario(false);
    }


    @Bean
    public CrossReference crossReference() {
        return new CrossReference()
                .withJsonOnly()
                .withPendingStepStrategy(getPendingStepStrategy())
                .withOutputAfterEachStory(false)
                .excludingStoriesWithNoExecutedScenarios(false);
    }

    @Bean
    public Embedder jbehaveEmbedder() {
        Embedder embedder = new Embedder(new StoryMapper(), new StoryRunner(), new SilentEmbedderMonitor(System.out));
        embedder.useConfiguration(configuration());
        embedder.useStepsFactory(stepsFactory());
        embedder.embedderControls()
                .doGenerateViewAfterStories(true)
                .doIgnoreFailureInView(ignoreFailureInView)
                .doIgnoreFailureInStories(ignoreFailureInStories)
                .doVerboseFailures(verboseFailures)
                .useThreads(1)
                .useStoryTimeoutInSecs(storyTimeoutInSecs);
        return embedder;
    }

    @Bean
    public InjectableStepsFactory stepsFactory() {
        return new SpringStepsFactory(configuration(), context);
    }

}
