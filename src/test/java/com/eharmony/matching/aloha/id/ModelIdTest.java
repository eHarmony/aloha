package com.eharmony.matching.aloha.id;

import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;
import org.junit.runners.BlockJUnit4ClassRunner;
import spray.json.PimpedString;
import spray.json.RootJsonFormat;

@RunWith(BlockJUnit4ClassRunner.class)
public class ModelIdTest {

    private static final long MODEL_NUMERIC_ID = 7;
    private static final String MODEL_NAME = "asdf";

    @Test
    public void testStaticFactoryMethod() {
        final ModelId empty = ModelId.empty();
        assertEquals(new ModelId(0, ""), empty);
    }

    @Test
    public void test() {
        final String json = "{\"id\": " + MODEL_NUMERIC_ID + ", \"name\": \"" + MODEL_NAME + "\"}";
        final ModelId modelId =
                new PimpedString(json)
                    .parseJson()
                    .convertTo(ModelIdentityJson.modelIdJsonFormat());

        assertEquals(ModelId.class.getCanonicalName(), modelId.getClass().getCanonicalName());
        assertEquals(MODEL_NUMERIC_ID, modelId.id());
        assertEquals(MODEL_NUMERIC_ID, modelId.getId());
        assertEquals(MODEL_NAME, modelId.name());
        assertEquals(MODEL_NAME, modelId.getName());
    }
}
