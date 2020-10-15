package ai.improve.android;

import ai.improve.android.hasher.MapFlattener;
import junit.framework.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapFlattenerTest {

    @Test
    public void testSimple() {
        Map m1 = new HashMap();
        m1.put("a", "1");
        m1.put("b", "2");
        m1.put("c", "3");

        Map m2 = new HashMap();
        m2.put("x", m1);
        m2.put("y", m1);
        m2.put("z", m1);

        Map result = MapFlattener.flatten(m2, ".");
        System.out.println(result);
        Assert.assertTrue(result.size() == m2.size() * m1.size());
        Assert.assertTrue(result.containsKey("x.a"));
        Assert.assertTrue(result.containsKey("x.b"));
        Assert.assertTrue(result.containsKey("x.c"));
    }

    @Test
    public void testMultiLevel() {
        Map m1 = new HashMap();
        m1.put("a", "1");
        m1.put("b", "2");
        m1.put("c", "3");

        Map m2 = new HashMap();
        m2.put("x", m1);
        m2.put("y", m1);
        m2.put("z", m1);

        Map m3 = new HashMap();
        m3.put("o", m2);
        m3.put("p", m2);
        m3.put("q", m2);

        Map result = MapFlattener.flatten(m3, ".");
        System.out.println(result);
        Assert.assertTrue(result.size() == m2.size() * m1.size() * m3.size());
        Assert.assertTrue(result.containsKey("p.x.a"));
        Assert.assertTrue(result.containsKey("p.x.b"));
        Assert.assertTrue(result.containsKey("p.x.c"));
    }

    @Test
    public void testList() {
        List m1 = Arrays.asList(0,1,2);

        Map m2 = new HashMap();
        m2.put("x", m1);
        m2.put("y", m1);
        m2.put("z", m1);

        Map m3 = new HashMap();
        m3.put("o", m2);
        m3.put("p", m2);
        m3.put("q", m2);

        Map result = MapFlattener.flatten(m3, ".");
        System.out.println(result);
        Assert.assertTrue(result.size() == m2.size() * m1.size());
        Assert.assertTrue(result.containsKey("p.x.0"));
        Assert.assertTrue(result.containsKey("p.x.1"));
        Assert.assertTrue(result.containsKey("p.x.2"));
    }

    @Test
    public void testArray() {
        int[] m1 = new int[] {1,2,3};

        Map m2 = new HashMap();
        m2.put("x", m1);
        m2.put("y", m1);
        m2.put("z", m1);

        Map m3 = new HashMap();
        m3.put("o", m2);
        m3.put("p", m2);
        m3.put("q", m2);

        Map result = MapFlattener.flatten(m3, ".");
        System.out.println(result);
        Assert.assertTrue(result.size() == m3.size() * m2.size() * m1.length);
        Assert.assertTrue(result.containsKey("p.x.0"));
        Assert.assertTrue(result.containsKey("p.x.1"));
        Assert.assertTrue(result.containsKey("p.x.2"));
    }

    @Test
    public void testCircular() {

        Map m1 = new HashMap();
        m1.put("a", "1");
        m1.put("b", "2");
        m1.put("c", "3");

        Map m2 = new HashMap();
        m2.put("x", m1);
        m2.put("y", m1);
        m2.put("z", m1);

        m1.put("lol", m2);

        //this is not supported, but shouldn't fail grossly either
        Map result = MapFlattener.flatten(m2, ".");
        System.out.println(result);
    }
}
