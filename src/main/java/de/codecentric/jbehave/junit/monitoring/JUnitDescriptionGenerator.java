package de.codecentric.jbehave.junit.monitoring;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.Keywords;
import org.jbehave.core.configuration.Keywords.StartingWordNotFound;
import org.jbehave.core.embedder.MetaFilter;
import org.jbehave.core.embedder.StoryControls;
import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.model.Meta;
import org.jbehave.core.model.Scenario;
import org.jbehave.core.model.Story;
import org.jbehave.core.steps.CandidateSteps;
import org.jbehave.core.steps.StepCandidate;
import org.jbehave.core.steps.StepType;
import org.junit.runner.Description;


public class JUnitDescriptionGenerator {
    public static final String EXAMPLE_DESCRIPTION_PREFIX = "Example: ";
    public static final String SCENARIO_DESCRIPTION_PREFIX = "Scenario: ";

    DescriptionTextUniquefier uniq = new DescriptionTextUniquefier();

    private int testCases;

    private List<StepCandidate> allCandidates = new ArrayList<StepCandidate>();

    private final Configuration configuration;

    private final StoryControls storyControls;

    private String previousNonAndStep;

    private MetaFilter metaFilter;

    public JUnitDescriptionGenerator(List<CandidateSteps> candidateSteps,
                                     Configuration configuration,
                                     MetaFilter metaFilter) {
        this.configuration = configuration;
        this.metaFilter = metaFilter;
        this.storyControls = configuration.storyControls();
        for (CandidateSteps candidateStep : candidateSteps) {
            allCandidates.addAll(candidateStep.listCandidates());
        }
    }

    public Description createDescriptionFrom(Story story) {
        Description storyDescription = createDescriptionForStory(story);


        addAllScenariosToDescription(story, storyDescription);
        return storyDescription;
    }

    public Description createDescriptionFrom(Scenario scenario) {
        Description scenarioDescription = createDescriptionForScenario(scenario);
        if (hasGivenStories(scenario)) {
            insertGivenStories(scenario, scenarioDescription);
        }

        if (hasExamples(scenario)) {
            insertDescriptionForExamples(scenario, scenarioDescription);
        } else {
            addStepsToExample(scenario, scenarioDescription);
        }
        return scenarioDescription;
    }

    public String getJunitSafeString(String string) {
        return uniq.getUniqueDescription(replaceLinebreaks(string)
                .replaceAll("[\\(\\)]", "|"));
    }

    public int getTestCases() {
        return testCases;
    }

    private boolean hasGivenStories(Scenario scenario) {
        return !scenario.getGivenStories().getPaths().isEmpty();
    }

    private boolean hasExamples(Scenario scenario) {
        return isParameterized(scenario)
                && !parameterNeededForGivenStories(scenario);
    }

    private boolean isParameterized(Scenario scenario) {
        ExamplesTable examplesTable = scenario.getExamplesTable();
        boolean isParameterized1 = examplesTable != null
                && examplesTable.getRowCount() > 0;
        return isParameterized1;
    }

    private boolean parameterNeededForGivenStories(Scenario scenario) {
        boolean parametersNeededForGivenStories = scenario.getGivenStories()
                .requireParameters();
        return parametersNeededForGivenStories;
    }

    private void insertGivenStories(Scenario scenario,
                                    Description scenarioDescription) {
        for (String path : scenario.getGivenStories().getPaths()) {
            addGivenStoryToScenario(scenarioDescription, path);
        }
    }

    private void addGivenStoryToScenario(Description scenarioDescription,
                                         String path) {
        scenarioDescription.addChild(Description
                .createSuiteDescription(getJunitSafeString(getFilename(path))));
        testCases++;
    }

    private String getFilename(String path) {
        return path.substring(path.lastIndexOf("/") + 1, path.length()).split(
                "#")[0];
    }

    private void insertDescriptionForExamples(Scenario scenario,
                                              Description scenarioDescription) {
        ExamplesTable examplesTable = scenario.getExamplesTable();
        List<Map<String, String>> rows = examplesTable.getRows();
        for (Map<String, String> row : rows) {
            Description exampleRowDescription = Description
                    .createSuiteDescription(EXAMPLE_DESCRIPTION_PREFIX + row,
                            (Annotation[]) null);
            scenarioDescription.addChild(exampleRowDescription);
            addStepsToExample(scenario, exampleRowDescription);
        }
    }

    private void addStepsToExample(Scenario scenario, Description description) {
        List<String> steps = scenario.getSteps();
        addSteps(description, steps);
    }

    private void addSteps(Description description, List<String> steps) {
        previousNonAndStep = null;
        for (String stringStep : steps) {
            String stringStepOneLine = stripLinebreaks(stringStep);
            StepCandidate matchingStep = findMatchingStep(stringStep);
            if (matchingStep == null) {
                addNonExistingStep(description, stringStepOneLine, stringStep);
            } else {
                addExistingStep(description, stringStepOneLine, matchingStep);
            }
        }
    }

    private void addExistingStep(Description description,
                                 String stringStepOneLine, StepCandidate matchingStep) {
        if (matchingStep.isComposite()) {
            addCompositeSteps(description, stringStepOneLine, matchingStep);
        } else {
            addRegularStep(description, stringStepOneLine, matchingStep);
        }
    }

    private void addNonExistingStep(Description description, String stringStepOneLine,
                                    String stringStep) {
        try {
            StepType stepType = configuration.keywords()
                    .stepTypeFor(stringStep);
            if (stepType == StepType.IGNORABLE) {
                addIgnorableStep(description, stringStepOneLine);
            } else {
                addPendingStep(description, stringStepOneLine);
            }
        } catch (Keywords.StartingWordNotFound e) {
            // WHAT NOW?
        }
    }

    private void addIgnorableStep(Description description, String stringStep) {
        testCases++;
        Description ignorableDescription = Description
                .createSuiteDescription(stringStep);
        description.addChild(ignorableDescription);
    }

    private void addPendingStep(Description description, String stringStep) {
        testCases++;
        Description testDescription = Description
                .createSuiteDescription(getJunitSafeString("[PENDING] "
                        + stringStep));
        description.addChild(testDescription);
    }

    private void addRegularStep(Description description, String stringStep,
                                StepCandidate step) {
        testCases++;
        // JUnit and the Eclipse JUnit view needs to be touched/fixed in order
        // to make the JUnit view
        // jump to the corresponding test method accordingly. For now we have to
        // live, that we end up in
        // the correct class.
        Description testDescription = Description.createTestDescription(step
                .getStepsInstance().getClass(), getJunitSafeString(stringStep));
        description.addChild(testDescription);
    }

    private void addCompositeSteps(Description description, String stringStep,
                                   StepCandidate step) {
        Description testDescription;
        testDescription = Description
                .createSuiteDescription(getJunitSafeString(stringStep));
        addSteps(testDescription, Arrays.asList(step.composedSteps()));
        description.addChild(testDescription);
    }

    private void addAllScenariosToDescription(Story story,
                                              Description storyDescription) {
        String storyMetaPrefix = storyControls.storyMetaPrefix();
        Meta storyMeta = story.getMeta().inheritFrom(story.asMeta(storyMetaPrefix));
        List<Scenario> scenarios = story.getScenarios();
        for (Scenario scenario : scenarios) {
            String scenarioMetaPrefix = storyControls.scenarioMetaPrefix();
            Meta scenarioMeta = scenario.getMeta().inheritFrom(scenario.asMeta(scenarioMetaPrefix).inheritFrom(storyMeta));
            boolean scenarioAllowed = metaFilter.allow(scenarioMeta);
            if (scenarioAllowed) {
                storyDescription.addChild(createDescriptionFrom(scenario));
            }
        }
    }

    private StepCandidate findMatchingStep(String stringStep) {
        for (StepCandidate step : allCandidates) {
            if (step.matches(stringStep, previousNonAndStep)) {
                if (step.getStepType() != StepType.AND) {
                    previousNonAndStep = step.getStartingWord() + " ";
                }
                return step;
            }
        }
        return null;
    }

    private String stripLinebreaks(String stringStep) {
        if (stringStep.indexOf('\n') != -1) {
            stringStep = stringStep.substring(0, stringStep.indexOf('\n'));
        }
        return stringStep;
    }

    private String replaceLinebreaks(String string) {
        return string.replaceAll("\r", "\n")
                .replaceAll("\n{2,}", "\n").replaceAll("\n", ", ");
    }

    private Description createDescriptionForStory(Story story) {
        Description storyDescription = Description
                .createSuiteDescription(getJunitSafeString(story.getName()));
        return storyDescription;
    }

    private Description createDescriptionForScenario(Scenario scenario) {
        Description scenarioDescription = Description
                .createSuiteDescription(SCENARIO_DESCRIPTION_PREFIX
                        + getJunitSafeString(scenario.getTitle()));
        return scenarioDescription;
    }

}

