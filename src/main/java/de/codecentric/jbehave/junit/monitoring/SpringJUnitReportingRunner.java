package de.codecentric.jbehave.junit.monitoring;

/**
 * User: alexwibowo
 * Date: 31/07/13
 * Time: 10:46 PM
 */

import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.embedder.*;
import org.jbehave.core.model.Meta;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.steps.CandidateSteps;
import org.jbehave.core.steps.InjectableStepsFactory;
import org.jbehave.core.steps.NullStepMonitor;
import org.jbehave.core.steps.StepMonitor;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.Statement;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class SpringJUnitReportingRunner extends SpringJUnit4ClassRunner {
    private List<Description> storyDescriptions;
    private Embedder configuredEmbedder;
    private List<String> storyPaths;
    private Configuration configuration;
    private int numberOfTestCases;
    private Description rootDescription;
    List<CandidateSteps> candidateSteps;
    private JBehaveTest testInstance;

    @SuppressWarnings("unchecked")
    public SpringJUnitReportingRunner(Class<? extends JBehaveTest> testClass)
            throws Throwable {
        super(testClass);
        testInstance = testClass.newInstance();
        getTestContextManager().prepareTestInstance(testInstance);

        getStoryPaths();

        configuration = configuredEmbedder.configuration();

        StepMonitor originalStepMonitor = createCandidateStepsWithNoMonitor();
        storyDescriptions = buildDescriptionFromStories();
        createCandidateStepsWith(originalStepMonitor);

        initRootDescription();
    }

    @Override
    public Description getDescription() {
        return rootDescription;
    }

    @Override
    public int testCount() {
        return numberOfTestCases;
    }

    @Override
    public void run(RunNotifier notifier) {

        JUnitScenarioReporter junitReporter = new JUnitScenarioReporter(
                notifier, numberOfTestCases, rootDescription);
        // tell the reporter how to handle pending steps
        junitReporter.usePendingStepStrategy(configuration.pendingStepStrategy());

        addToStoryReporterFormats(junitReporter);

        try {
            Statement statement = new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    configuredEmbedder.runStoriesAsPaths(storyPaths);
                }
            };
            statement = withBeforeClasses(statement);
            statement = withAfterClasses(statement);
            statement.evaluate();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        } finally {
            configuredEmbedder.generateCrossReference();
        }
    }

    private void createCandidateStepsWith(StepMonitor stepMonitor) {
        // reset step monitor and recreate candidate steps
        configuration.useStepMonitor(stepMonitor);
        createCandidateSteps();
        for (CandidateSteps step : candidateSteps) {
            step.configuration().useStepMonitor(stepMonitor);
        }
    }

    private StepMonitor createCandidateStepsWithNoMonitor() {
        StepMonitor usedStepMonitor = configuration.stepMonitor();
        createCandidateStepsWith(new NullStepMonitor());
        return usedStepMonitor;
    }

    @SuppressWarnings("unchecked")
    private void getStoryPaths()
            throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
        configuredEmbedder = testInstance.configuredEmbedder();
        storyPaths = testInstance.storyPaths();
    }

    private void createCandidateSteps() {
        InjectableStepsFactory stepsFactory = testInstance.getInjectableStepsFactory();
        if (stepsFactory != null) {
            candidateSteps = stepsFactory.createCandidateSteps();
        } else {
            Embedder embedder = testInstance.configuredEmbedder();
            candidateSteps = embedder.candidateSteps();
            if (candidateSteps == null || candidateSteps.isEmpty()) {
                candidateSteps = embedder.stepsFactory().createCandidateSteps();
            }
        }
    }

    private void initRootDescription() {
        rootDescription = Description.createSuiteDescription(testInstance.getClass());
        rootDescription.getChildren().addAll(storyDescriptions);
    }

    private void addToStoryReporterFormats(JUnitScenarioReporter junitReporter) {
        StoryReporterBuilder storyReporterBuilder = configuration
                .storyReporterBuilder();
        StoryReporterBuilder.ProvidedFormat junitReportFormat = new StoryReporterBuilder.ProvidedFormat(
                junitReporter);
        storyReporterBuilder.withFormats(junitReportFormat);
    }

    private List<Description> buildDescriptionFromStories() {
        JUnitDescriptionGenerator descriptionGenerator = new JUnitDescriptionGenerator(
                candidateSteps, configuration, configuredEmbedder.metaFilter());
        StoryRunner storyRunner = new StoryRunner();
        List<Description> storyDescriptions = new ArrayList<Description>();

        addSuite(storyDescriptions, "BeforeStories");
        addStories(storyDescriptions, storyRunner, descriptionGenerator);
        addSuite(storyDescriptions, "AfterStories");

        return storyDescriptions;
    }

    private void addStories(List<Description> storyDescriptions,
                            StoryRunner storyRunner, JUnitDescriptionGenerator gen) {
        for (String storyPath : storyPaths) {
            MetaFilter metaFilter = configuredEmbedder.metaFilter();
            Story parseStory = storyRunner.storyOfPath(configuration, storyPath);
            FilteredStory filteredStory = new FilteredStory(metaFilter, parseStory, configuration.storyControls());
            if (filteredStory.allowed() || storyHasAllowedScenario(parseStory, metaFilter)) {
                Description descr = gen.createDescriptionFrom(parseStory);
                storyDescriptions.add(descr);
                numberOfTestCases += parseStory.getScenarios().size();
            }
        }
    }

    private boolean storyHasAllowedScenario(Story story, MetaFilter metaFilter) {
        StoryControls storyControls = configuration.storyControls();
        String storyMetaPrefix = storyControls.storyMetaPrefix();
        String scenarioMetaPrefix = storyControls.scenarioMetaPrefix();
        Meta storyMeta = story.getMeta().inheritFrom(story.asMeta(storyMetaPrefix));
        for (Scenario scenario : story.getScenarios()) {
            Meta scenarioMeta = scenario.getMeta().inheritFrom(
                    scenario.asMeta(scenarioMetaPrefix).inheritFrom(storyMeta));
            boolean scenarioAllowed = metaFilter.allow(scenarioMeta);
            if (scenarioAllowed) {
                return true;
            }
        }
        return false;
    }

    private void addSuite(List<Description> storyDescriptions, String name) {
        storyDescriptions.add(Description.createTestDescription(Object.class,
                name));
        numberOfTestCases++;
    }
}