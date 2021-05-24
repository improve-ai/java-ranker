package ai.improve.android;

import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.improve.BaseIMPDecisionModel;
import ai.improve.IMPDecision;
import ai.improve.XXHashProvider;

import static org.junit.Assert.*;

public class IMPDecisionTest {
    public class IMPDecisionModel extends BaseIMPDecisionModel {
        public IMPDecisionModel(String modelName, XXHashProvider xxHashProvider) {
            super(modelName, xxHashProvider);
        }
    }

    public class XXHashProviderImp implements XXHashProvider {

        @Override
        public long xxhash(byte[] data, long seed) {
            return 0;
        }
    }

    @Test
    public void testGetWithoutVariants() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");
        variants.add("hello, world!");
        variants.add("hello");
        variants.add("hi");
        variants.add("Hello World!");

        int loop = 10000;
        IMPDecisionModel decisionModel = new IMPDecisionModel("", new XXHashProviderImp());
        for(int i = 0; i < loop; i++) {
            IMPDecision decision = new IMPDecision(decisionModel);
            String greeting = (String) decision.chooseFrom(variants).get();
            assertEquals(greeting, variants.get(0));
        }
    }

    @Test
    public void testChooseFrom() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        IMPDecisionModel decisionModel = new IMPDecisionModel("", new XXHashProviderImp());
        IMPDecision decision = new IMPDecision(decisionModel);
        decision.chooseFrom(variants).get();

        List<Object> newVariants = new ArrayList<>();
        newVariants.add("HELLO WORLD!");
        Object variant = decision.chooseFrom(newVariants).get();
        assertEquals(variants.get(0), variant);
    }

    @Test
    public void testChooseFromNullVariants() {
        List<Object> variants = new ArrayList<>();
        IMPDecisionModel decisionModel = new IMPDecisionModel("", new XXHashProviderImp());
        IMPDecision decision = new IMPDecision(decisionModel);
        assertNull(decision.chooseFrom(variants).get());
    }

    @Test
    public void testChooseFromEmptyVariants() {
        IMPDecisionModel decisionModel = new IMPDecisionModel("", new XXHashProviderImp());
        IMPDecision decision = new IMPDecision(decisionModel);
        assertNull(decision.chooseFrom(null).get());
    }

    @Test
    public void testGiven() {
        List<Object> variants = new ArrayList<>();
        variants.add("Hello, World!");

        Map<String, Object> given = new HashMap<>();
        given.put("size", 0);

        IMPDecisionModel decisionModel = new IMPDecisionModel("", new XXHashProviderImp());
        IMPDecision decision = new IMPDecision(decisionModel);
        decision.chooseFrom(variants).given(given);
        assertEquals(given, getFieldValue(decision, "givens"));
    }

    private Object getFieldValue(Object object, String fieldName){
        Field field = getDeclaredField(object, fieldName) ;
        field.setAccessible(true) ;
        try {
            return field.get(object) ;
        } catch(Exception e) {
            e.printStackTrace() ;
        }
        return null;
    }

    private Field getDeclaredField(Object object, String fieldName){
        Field field ;
        Class<?> clazz = object.getClass() ;
        for(; clazz != Object.class ; clazz = clazz.getSuperclass()) {
            try {
                field = clazz.getDeclaredField(fieldName) ;
                return field ;
            } catch (Exception e) {
            }
        }
        return null;
    }
}
