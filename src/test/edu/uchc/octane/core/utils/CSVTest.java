package edu.uchc.octane.core.utils;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class CSVTest {

    @Test
    public void test_custom_separator() {

        String line = "10|AU|Australia";
        List<String> result = CSVUtils.parseLine(line, '|');

        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(result.get(0), "10");
        assertEquals(result.get(1), "AU");
        assertEquals(result.get(2), "Australia");

    }

    @Test
    public void test_custom_separator_and_quote() {

        String line = "'10'|'AU'|'Australia'";
        List<String> result = CSVUtils.parseLine(line, '|', '\'');

        assertNotNull(result);
        assertEquals(result.size(), 3);
        assertEquals(result.get(0), "10");
        assertEquals(result.get(1), "AU");
        assertEquals(result.get(2), "Australia");

    }

}
