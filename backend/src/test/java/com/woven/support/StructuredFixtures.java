package com.woven.support;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
public final class StructuredFixtures {
    private StructuredFixtures() {}
    public static String details(String what) {
        try { return new ObjectMapper().writeValueAsString(Map.of("template","gatekeeper-sop","schemaVersion",2,
                "scope","","references","","steps",List.of(Map.of("id","00000000-0000-0000-0000-000000000001",
                        "who","Operator","what",what,"where","Workstation","notes","")))); }
        catch(Exception e) {throw new IllegalStateException(e);}
    }
}
