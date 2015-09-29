package com.eharmony.aloha.score.conversions;

import com.eharmony.aloha.score.Scores.Score;
import com.eharmony.aloha.score.Scores.Score.*;
import com.eharmony.aloha.score.Scores.Score.BaseScore.*;
import com.eharmony.aloha.score.conversions.rich.RichScoreOps;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.runner.RunWith;
import org.junit.runners.BlockJUnit4ClassRunner;

/**
 * Created by deak on 9/29/15.
 */
@RunWith(BlockJUnit4ClassRunner.class)
public class JavaRichScoreLikeTest {
    @Test
    public void testCalling() {
        final Score score = getScore2();
        final String json = RichScoreOps.toRichScore(score).scoreJson().compactPrint();
        assertEquals("{\"1\":1,\"2\":2.0}", json);
    }

    private static Score getScore2() {
        return Score.newBuilder().setScore(
                   BaseScore.newBuilder()
                            .setModel(ModelId.newBuilder().setId(1).setName(""))
                            .setExtension(IntScore.impl, IntScore.newBuilder().setScore(1).build())
                            .setType(ScoreType.INT))
               .addSubScores(
                   Score.newBuilder().setScore(
                       BaseScore.newBuilder()
                                .setModel(Score.ModelId.newBuilder().setId(2).setName(""))
                                .setExtension(DoubleScore.impl, DoubleScore.newBuilder().setScore(2d).build())
                                .setType(ScoreType.DOUBLE))
               ).build();
    }
}
