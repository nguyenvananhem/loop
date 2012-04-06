package loop.confidence.modules;

import loop.AnnotatedError;
import loop.Loop;
import loop.LoopCompileException;
import loop.LoopTest;
import loop.ast.script.ModuleLoader;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

/**
 * Confidence tests run a bunch of semi-realistic programs and assert that their results are
 * as expected. This is meant to be our functional regression test suite.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public class ModulesConfidenceTest extends LoopTest {
  @Test(expected = LoopCompileException.class)
  public final void requireFaultyLoopModule() {
    assertEquals(new Date(10), Loop.run("test/loop/confidence/modules/require_loop_error_1.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void requireLoopModuleTwiceCausesVerifyError() {
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals(10, Loop.run("test/loop/confidence/modules/require_loop_error_2.loop"));
  }

  @Test
  public final void requireLoopModule() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals(30, Loop.run("test/loop/confidence/modules/require_loop_mod_1.loop"));
  }

  @Test
  public final void requireLoopModuleTransitivelyErrorsIfMissingModuleDecl() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    List<AnnotatedError> errors = null;
    try {
      Loop.run("test/loop/confidence/modules/require_loop_mod_3.loop");
      fail();
    } catch (LoopCompileException e) {
      errors = e.getErrors();
    }

    assertNotNull(errors);
    assertEquals(1, errors.size());
  }

  @Test(expected = LoopCompileException.class)
  public final void requireLoopModuleHidesTransitiveDeps() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals(4, Loop.run("test/loop/confidence/modules/require_loop_mod_4.loop"));
  }

  @Test
  public final void requireLoopModuleRaiseAndCatchException() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals("now is the winter of our discontent!",
        Loop.run("test/loop/confidence/modules/require_loop_mod_2.loop"));
  }

  @Test
  public final void preludeConfidence1() {
    // Set the search path for prelude, first.
    ModuleLoader.searchPaths = new String[] { "test/loop/confidence/modules" };

    assertEquals(true,
        Loop.run("test/loop/confidence/modules/prelude_conf_1.loop"));
  }

  @Test
  public final void requireJavaClass() {
    assertEquals(new Date(10), Loop.run("test/loop/confidence/modules/require_java.loop"));
  }

  @Test(expected = LoopCompileException.class)
  public final void requireJavaClassFails() {
    assertEquals(new Date(10), Loop.run("test/loop/confidence/modules/require_java_error.loop"));
  }
}
