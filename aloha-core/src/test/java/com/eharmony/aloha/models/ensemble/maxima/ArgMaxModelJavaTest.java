//package com.eharmony.aloha.models.ensemble.maxima;
//
//import com.eharmony.aloha.score.proto.conversions.rich.RichScoreLike;
//import com.eharmony.aloha.score.proto.conversions.rich.RichScoreOps;
//import com.eharmony.aloha.score.proto.order.ScoreComparator;
//import org.junit.Test;
//import static org.junit.Assert.*;
//
//import com.eharmony.aloha.score.Scores.Score;
//import com.eharmony.aloha.score.proto.conversions.StrictConversions;
//import scala.collection.JavaConversions;
//
//import java.util.Arrays;
//import java.util.Collections;
//import java.util.List;
//
//import static com.eharmony.aloha.score.Scores.Score.BaseScore.ScoreType.STRING;
//
//public class ArgMaxModelJavaTest {
//
//    /**
//     * An model argmax model whose submodels are "Model[Any, Int]" Notice that the Int types resolve to Object.  This
//     * is a known issue and by design.  https://groups.google.com/forum/?fromgroups=#!topic/scala-user/mUH5GQxVzgs
//     */
//    private static final ArgMax<Object, Object, String> INT_MODEL = new ArgMaxModelTest().getArgMaxModelInt();
//
//    /**
//     * This is a model with submodel output type java.lang.Integer.  See second type parameter.
//     */
//    private static final ArgMax<Object, Integer, String> INTEGER_MODEL_2 = new ArgMaxModelTest().getArgMaxModelInteger();
//
//    /**
//     * The score computed from the Int (primitive) based model
//     */
//    private static final Score SCORE = INT_MODEL.score(null);
//
//    /**
//     * The score computed from the Integer (object) based model
//     */
//    private static final Score SCORE2 = INTEGER_MODEL_2.score(null);
//
//    /**
//     * The two models should output the same score even though the internal submodels have different types, because the
//     * natural ordering of Int and java.lang.Integer is the same, the result should still be the same.
//     */
//    @Test
//    public void testScoresAreSame() {
//        assertEquals(SCORE, SCORE2);
//    }
//
//    /**
//     * Should be string based because this is the right-most (output) type in the model.
//     */
//    @Test
//    public void testScoresIsStringBased() {
//        assertEquals(STRING, SCORE.getScore().getType());
//    }
//
//    /**
//     * Check all scores.
//     */
//    @Test
//    public void testSubScoresCorrect() {
//        final ArgMax<Object, Object, String> intModel = INT_MODEL;
//        final String topLevelScore = StrictConversions.asJavaString(SCORE);
//        assertEquals("three", topLevelScore);
//        final List<Score> subScoresList = SCORE.getSubScoresList();
//        assertEquals(4, subScoresList.size());
//        int[] subscores = new int[subScoresList.size()];
//        for (int i = 0; i < subscores.length; ++i) subscores[i] = StrictConversions.asJavaInteger(subScoresList.get(i));
//        Arrays.sort(subscores);
//        assertArrayEquals(new int[]{1,2,3,4}, subscores);
//    }
//
//    @Test
//    public void testTree() {
//
//        final ArgMax<Object, Integer, Double> argMaxTree = new ArgMaxModelTest().getArgMaxTree();//TODO: Expand this
//        final Score score = argMaxTree.score(null);
//        final Double top = StrictConversions.asJavaDouble(score);
//        assertEquals(2.0, top, 0);
//        final RichScoreLike rs = RichScoreOps.toRichScore(score);
//        final List<Score> scores = new java.util.ArrayList<Score>(JavaConversions.seqAsJavaList(rs.allScores()));
//        Collections.sort(scores, new ScoreComparator());
//
//        // This is the structure of the model tree.
//        assertEquals(2.0, StrictConversions.asJavaDouble(scores.get(0)), 0);
//          assertEquals(7, StrictConversions.asJavaInteger(scores.get(1)).intValue());
//            assertEquals("a", StrictConversions.asJavaString(scores.get(2)));
//            assertEquals("b", StrictConversions.asJavaString(scores.get(6)));
//          assertEquals(6, StrictConversions.asJavaInteger(scores.get(4)).intValue());
//            assertEquals(2, StrictConversions.asJavaInteger(scores.get(3)).intValue());
//            assertEquals(3, StrictConversions.asJavaInteger(scores.get(5)).intValue());
//    }
//}
