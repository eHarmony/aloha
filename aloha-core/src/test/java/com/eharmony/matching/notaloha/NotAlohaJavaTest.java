package com.eharmony.matching.notaloha;

import org.junit.Test;
import com.eharmony.matching.aloha.models.Model;
import com.eharmony.matching.aloha.models.ensemble.maxima.ArgMaxModelTest;

public class NotAlohaJavaTest {

    @Test
    public void test() {
        final Model<Object, String> argMaxModel = new ArgMaxModelTest().getArgMaxModelInteger();
        argMaxModel.getScore("", false);
    }
}
