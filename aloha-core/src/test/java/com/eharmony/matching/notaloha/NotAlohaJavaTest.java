package com.eharmony.matching.notaloha;

import org.junit.Test;
import com.eharmony.aloha.models.Model;
import com.eharmony.aloha.models.ensemble.maxima.ArgMaxModelTest;

public class NotAlohaJavaTest {

    @Test
    public void test() {
        final Model<Object, String> argMaxModel = new ArgMaxModelTest().getArgMaxModelInteger();
        argMaxModel.getScore("", false);
    }
}
