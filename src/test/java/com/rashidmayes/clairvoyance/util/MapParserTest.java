package com.rashidmayes.clairvoyance.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapParserTest {

    @DisplayName("map")
    @Test
    void map() {
        var string = "cluster_size=1;cluster_key=D70B83295E71;cluster_generation=1";
        var map = MapParser.parseMap(string, ";");

        assertEquals("1", map.get("cluster_size"));
        assertEquals("D70B83295E71", map.get("cluster_key"));
        assertEquals("1", map.get("cluster_generation"));
    }

    @DisplayName("getString - no match")
    @Test
    void getStringNoMatch() {
        var map = Map.<String, String>of();
        var string = MapParser.getString(map, "ns_name", "ns");

        assertEquals("", string);
    }

    @DisplayName("getString - match")
    @Test
    void getStringMatch() {
        var map = Map.of("ns", "users");
        var string = MapParser.getString(map, "ns_name", "ns");

        assertEquals("users", string);
    }

    @DisplayName("getLong - no match")
    @Test
    void getLongNoMatch() {
        var map = Map.<String, String>of();
        var result = MapParser.getLong(map, "used-bytes-memory", "memory_used_bytes");

        assertEquals(0L, result);
    }

    @DisplayName("getLong - match")
    @Test
    void getLongMatch() {
        var map = Map.of("used-bytes-memory", "12");
        var result = MapParser.getLong(map, "used-bytes-memory", "memory_used_bytes");

        assertEquals(12L, result);
    }

}