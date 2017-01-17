package com.eharmony.aloha.score.audit.take5.scoreproto;

import com.eharmony.aloha.id.ModelId;
import com.eharmony.aloha.score.Scores.Score;
import com.eharmony.aloha.score.audit.take5.ConstantModel;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;
import scala.Option;

/**
 * Created by ryan on 1/17/17.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class JavaScoreAuditorTest {
    @Rule
    public ExpectedException thrown= ExpectedException.none();

    @Test
    public void testBooleanConstantModel() {
        final Object anyInput = "";
        final ScoreAuditor<Object> auditor = ScoreAuditor.booleanAuditor();
        final ModelId id = new ModelId(1, "one");
        final Option<Boolean> constant = Option.apply(true);
        final ConstantModel<Score, Object, Score> model =
                ConstantModel.createFromJava(id, auditor, constant);

        final Score score = model.apply(anyInput);
        System.out.println(score);
    }

    @Test
    public void testIntConstantModelWithBooleanAuditorThrowsAtScoreTime() {
        final Object anyInput = "";
        final ScoreAuditor<Object> auditor = ScoreAuditor.booleanAuditor();
        final ModelId id = new ModelId(1, "one");
        final Option<Integer> constant = Option.apply(1);

        // NOTICE: This doesn't throw but maybe should.
        // TODO: Make type incompatibilities like this throw an exception.
        final ConstantModel<Score, Object, Score> model =
                ConstantModel.createFromJava(id, auditor, constant);

        // Because the value 'constant' is an Option<Integer> and 'auditor' audits a Boolean,
        // We get a problem at runtime.  This isn't caught by the type checker because subtypes
        // of scala.AnyVal resolve to Object in Java and by the API, constant must be of type:
        //
        //   Option<? extends N>,  (since N = scala.Boolean it becomes N = Object)
        //
        // This is true.  Unfortunately, both scala.Boolean and java.lang.Integer are both
        // subtypes of Object so while this compiles, there's still a problem at score time.
        thrown.expect(ClassCastException.class);
        thrown.expectMessage("java.lang.Integer cannot be cast to java.lang.Boolean");
        model.apply(anyInput);
    }
}
