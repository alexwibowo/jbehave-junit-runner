package de.codecentric.jbehave.junit.monitoring.step;

import org.jbehave.core.annotations.*;
import org.jbehave.core.model.ExamplesTable;
import org.jbehave.core.steps.Parameters;
import org.junit.Assert;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

@Component
public class SpringExampleSteps {
	int x;
	private Map<String, Integer> variables;
	private int result;

	@Given("a variable x with value $value")
	public void givenXValue(@Named("value") int value) {
		x = value;
	}

	@When("I multiply x by $value")
	public void whenImultiplyXBy(@Named("value") int value) {
		x = x * value;
	}

	@Then("x should equal $value")
	public void thenXshouldBe(@Named("value") int value) {
		Assert.assertEquals(value, x);
	}


}
