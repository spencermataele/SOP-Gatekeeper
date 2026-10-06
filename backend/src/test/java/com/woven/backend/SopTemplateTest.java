package com.woven.backend;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woven.app.service.governance.SopTemplate;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static com.woven.support.StructuredFixtures.details;
import static org.junit.jupiter.api.Assertions.*;

class SopTemplateTest {
    private final SopTemplate template = new SopTemplate(new ObjectMapper());
    @Test void legacyCanBeSavedButMustBeConvertedForSubmission() {
        assertNull(template.validate("Original imported procedure", false));
        assertThrows(IllegalArgumentException.class, () -> template.validate("Original imported procedure", true));
    }
    @Test void incompleteDraftCannotBeSubmitted() {
        var content = template.validate(details("Do the work"), false);
        ((com.fasterxml.jackson.databind.node.ObjectNode) content.path("steps").get(0)).put("where", " ");
        assertNotNull(template.validate(content.toString(), false));
        assertThrows(IllegalArgumentException.class, () -> template.validate(content.toString(), true));
    }
    @Test void duplicatedStepIdentifiersAreRejected() {
        var content = template.validate(details("Do the work"), false);
        ((com.fasterxml.jackson.databind.node.ArrayNode) content.path("steps")).add(content.path("steps").get(0).deepCopy());
        assertThrows(IllegalArgumentException.class, () -> template.validate(content.toString(), true));
    }
    @Test void authoritativeContextReplacesUserSuppliedMetadata() throws Exception {
        var content = template.validate(details("Do the work"), true);
        content.putObject("context").put("processOwner", "Impersonated owner");
        var saved = new ObjectMapper().readTree(template.withContext(content, Map.of("processOwner", "Actual owner")));
        assertEquals("Actual owner", saved.path("context").path("processOwner").asText());
        assertEquals(content.path("steps"), saved.path("steps"));
    }
}
