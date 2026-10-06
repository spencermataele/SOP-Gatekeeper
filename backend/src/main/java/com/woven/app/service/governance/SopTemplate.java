package com.woven.app.service.governance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Version 2 is the first structured Who/What/Where template; version 1 remains readable history. */
@Component
public class SopTemplate {
    private final ObjectMapper json;
    public SopTemplate(ObjectMapper json) { this.json = json; }

    public ObjectNode validate(String details, boolean submitting) {
        JsonNode root;
        try { root = json.readTree(details); }
        catch (Exception ignored) {
            if (!submitting) return null;
            throw new IllegalArgumentException("Convert this procedure to the standard Who/What/Where template before submitting.");
        }
        if (root == null || !root.isObject() || !"gatekeeper-sop".equals(root.path("template").asText())) {
            if (!submitting) return null;
            throw new IllegalArgumentException("The standard SOP template is required for submission.");
        }
        if (!root.path("schemaVersion").isIntegralNumber() || root.path("schemaVersion").asInt() != 2)
            throw new IllegalArgumentException("Unsupported SOP template version.");
        text(root, "scope", false); text(root, "references", false);
        JsonNode steps = root.path("steps");
        if (!steps.isArray() || steps.size() > 200 || (submitting && steps.isEmpty()))
            throw new IllegalArgumentException("Provide between 1 and 200 procedure steps before submission.");
        Set<String> ids = new HashSet<>();
        for (JsonNode step : steps) {
            if (!step.isObject()) throw new IllegalArgumentException("Each procedure step must be an object.");
            String id = step.path("id").asText();
            try { if (!UUID.fromString(id).toString().equals(id)) throw new IllegalArgumentException(); }
            catch (IllegalArgumentException invalid) { throw new IllegalArgumentException("Each step needs a valid stable identifier."); }
            if (!ids.add(id)) throw new IllegalArgumentException("Procedure step identifiers must be unique.");
            for (String field : new String[]{"who", "what", "where"}) text(step, field, submitting);
            text(step, "notes", false);
        }
        JsonNode subgroup = root.get("subgroupId");
        if (subgroup != null && !subgroup.isNull() && (!subgroup.isIntegralNumber() || subgroup.asInt() <= 0))
            throw new IllegalArgumentException("Choose a valid department subgroup.");
        return (ObjectNode) root;
    }

    private void text(JsonNode node, String field, boolean required) {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().length() > 20000 || (required && value.asText().isBlank()))
            throw new IllegalArgumentException("Step/template field '" + field + "' must be text" + (required ? " and cannot be blank." : "."));
    }

    public String withContext(ObjectNode content, Map<String, Object> context) {
        content.set("context", json.valueToTree(context));
        return content.toString();
    }
}
