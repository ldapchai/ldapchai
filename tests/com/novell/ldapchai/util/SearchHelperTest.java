/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.novell.ldapchai.util;

import com.novell.ldapchai.provider.ChaiProvider.SEARCH_SCOPE;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

/**
 *
 * @author mpieters
 */
public class SearchHelperTest {
    
    public SearchHelperTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }
    
    @Before
    public void setUp() {
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of setFilter method, of class SearchHelper.
     */
    @Test
    public void testSetFilter_String() {
        System.out.println("setFilter");
        String filter = "";
        SearchHelper instance = new SearchHelper();
        instance.setFilter(filter);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setAttributes method, of class SearchHelper.
     */
    @Test
    public void testSetAttributes_Collection() {
        System.out.println("setAttributes");
        Collection<String> attributes = null;
        SearchHelper instance = new SearchHelper();
        instance.setAttributes(attributes);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setAttributes method, of class SearchHelper.
     */
    @Test
    public void testSetAttributes_String() {
        System.out.println("setAttributes");
        String attributes = "";
        SearchHelper instance = new SearchHelper();
        instance.setAttributes(attributes);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getAttributes method, of class SearchHelper.
     */
    @Test
    public void testGetAttributes() {
        System.out.println("getAttributes");
        SearchHelper instance = new SearchHelper();
        Set expResult = null;
        Set result = instance.getAttributes();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setAttributes method, of class SearchHelper.
     */
    @Test
    public void testSetAttributes_StringArr() {
        System.out.println("setAttributes");
        String[] attributes = null;
        SearchHelper instance = new SearchHelper();
        instance.setAttributes(attributes);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getFilter method, of class SearchHelper.
     */
    @Test
    public void testGetFilter() {
        System.out.println("getFilter");
        SearchHelper instance = new SearchHelper();
        String expResult = "";
        String result = instance.getFilter();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getMaxResults method, of class SearchHelper.
     */
    @Test
    public void testGetMaxResults() {
        System.out.println("getMaxResults");
        SearchHelper instance = new SearchHelper();
        int expResult = 0;
        int result = instance.getMaxResults();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setMaxResults method, of class SearchHelper.
     */
    @Test
    public void testSetMaxResults() {
        System.out.println("setMaxResults");
        int maxResults = 0;
        SearchHelper instance = new SearchHelper();
        instance.setMaxResults(maxResults);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getSearchScope method, of class SearchHelper.
     */
    @Test
    public void testGetSearchScope() {
        System.out.println("getSearchScope");
        SearchHelper instance = new SearchHelper();
        SEARCH_SCOPE expResult = null;
        SEARCH_SCOPE result = instance.getSearchScope();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setSearchScope method, of class SearchHelper.
     */
    @Test
    public void testSetSearchScope() {
        System.out.println("setSearchScope");
        SEARCH_SCOPE searchScope = null;
        SearchHelper instance = new SearchHelper();
        instance.setSearchScope(searchScope);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getTimeLimit method, of class SearchHelper.
     */
    @Test
    public void testGetTimeLimit() {
        System.out.println("getTimeLimit");
        SearchHelper instance = new SearchHelper();
        int expResult = 0;
        int result = instance.getTimeLimit();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setTimeLimit method, of class SearchHelper.
     */
    @Test
    public void testSetTimeLimit() {
        System.out.println("setTimeLimit");
        int timeLimit = 0;
        SearchHelper instance = new SearchHelper();
        instance.setTimeLimit(timeLimit);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clone method, of class SearchHelper.
     */
    @Test
    public void testClone() throws Exception {
        System.out.println("clone");
        SearchHelper instance = new SearchHelper();
        Object expResult = null;
        Object result = instance.clone();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of equals method, of class SearchHelper.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        Object o = null;
        SearchHelper instance = new SearchHelper();
        boolean expResult = false;
        boolean result = instance.equals(o);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of hashCode method, of class SearchHelper.
     */
    @Test
    public void testHashCode() {
        System.out.println("hashCode");
        SearchHelper instance = new SearchHelper();
        int expResult = 0;
        int result = instance.hashCode();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of toString method, of class SearchHelper.
     */
    @Test
    public void testToString() {
        System.out.println("toString");
        SearchHelper instance = new SearchHelper();
        String expResult = "";
        String result = instance.toString();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of returnNoAttributes method, of class SearchHelper.
     */
    @Test
    public void testReturnNoAttributes() {
        System.out.println("returnNoAttributes");
        SearchHelper instance = new SearchHelper();
        instance.returnNoAttributes();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of returnAllAttributes method, of class SearchHelper.
     */
    @Test
    public void testReturnAllAttributes() {
        System.out.println("returnAllAttributes");
        SearchHelper instance = new SearchHelper();
        instance.returnAllAttributes();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of clearFilter method, of class SearchHelper.
     */
    @Test
    public void testClearFilter() {
        System.out.println("clearFilter");
        SearchHelper instance = new SearchHelper();
        instance.clearFilter();
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFilterAnd method, of class SearchHelper.
     */
    @Test
    public void testSetFilterAnd_Map() {
        System.out.println("setFilterAnd");
        HashMap<String, String> nameValuePairs = new HashMap<String, String>();
        nameValuePairs.put("test1", "testValue1");
        SearchHelper instance = new SearchHelper();
        instance.setFilterAnd(nameValuePairs);
        System.out.println(instance.getFilter());
        String result1 = instance.getFilter();
        assert(!result1.startsWith("(("));
        nameValuePairs.put("test2", "testValue2");
        instance.setFilterAnd(nameValuePairs);
        System.out.println(instance.getFilter());
        String result2 = instance.getFilter();
        assert(result2.startsWith("(&("));
    }

    /**
     * Test of setFilterAnd method, of class SearchHelper.
     */
    @Test
    public void testSetFilterAnd_Properties() {
        System.out.println("setFilterAnd");
        Properties nameValuePairs = null;
        SearchHelper instance = new SearchHelper();
        instance.setFilterAnd(nameValuePairs);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFilterExists method, of class SearchHelper.
     */
    @Test
    public void testSetFilterExists_String() {
        System.out.println("setFilterExists");
        String attributeName = "";
        SearchHelper instance = new SearchHelper();
        instance.setFilterExists(attributeName);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFilterExists method, of class SearchHelper.
     */
    @Test
    public void testSetFilterExists_Set() {
        System.out.println("setFilterExists");
        Set<String> attributeNames = null;
        SearchHelper instance = new SearchHelper();
        instance.setFilterExists(attributeNames);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFilterNot method, of class SearchHelper.
     */
    @Test
    public void testSetFilterNot() {
        System.out.println("setFilterNot");
        String attributeName = "";
        String value = "";
        SearchHelper instance = new SearchHelper();
        instance.setFilterNot(attributeName, value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFilter method, of class SearchHelper.
     */
    @Test
    public void testSetFilter_String_String() {
        System.out.println("setFilter");
        String attributeName = "";
        String value = "";
        SearchHelper instance = new SearchHelper();
        instance.setFilter(attributeName, value);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFilterOr method, of class SearchHelper.
     */
    @Test
    public void testSetFilterOr_Map() {
        System.out.println("setFilterOr");
        Map<String, String> nameValuePairs = null;
        SearchHelper instance = new SearchHelper();
        instance.setFilterOr(nameValuePairs);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFilterOr method, of class SearchHelper.
     */
    @Test
    public void testSetFilterOr_Properties() {
        System.out.println("setFilterOr");
        Properties nameValuePairs = null;
        SearchHelper instance = new SearchHelper();
        instance.setFilterOr(nameValuePairs);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
}
