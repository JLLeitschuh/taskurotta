package ru.taskurotta.recipes;

import net.sourceforge.argparse4j.inf.ArgumentParserException;
import org.junit.Test;
import ru.taskurotta.bootstrap.Bootstrap;
import ru.taskurotta.test.flow.BasicFlowArbiter;
import ru.taskurotta.test.flow.FlowArbiterFactory;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static ru.taskurotta.recipes.RecipesRunner.run;

/**
 * Created by void 04.04.13 20:18
 */
public class SummatorTest {

	//@org.junit.Ignore
	@Test
	public void testWorkflow() throws ArgumentParserException, IOException, ClassNotFoundException {
		run("ru/taskurotta/recipes/summator/");

		BasicFlowArbiter arbiter = (BasicFlowArbiter) new FlowArbiterFactory().getInstance(); // created in spring context
		assertTrue(arbiter.waitForFinish(30000));
	}
}
