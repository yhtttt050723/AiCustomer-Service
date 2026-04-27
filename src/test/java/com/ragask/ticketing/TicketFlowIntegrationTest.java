package com.ragask.ticketing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragask.ticketing.model.dto.AskTicketRequest;
import com.ragask.ticketing.model.dto.HitRateSnapshot;
import com.ragask.ticketing.model.dto.ResolveTicketRequest;
import com.ragask.ticketing.model.dto.TicketResponse;
import com.ragask.ticketing.model.enums.TicketStatus;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TicketFlowIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldRunRagTestSet() throws Exception {
        List<TestCase> cases = loadCases();
        for (TestCase testCase : cases) {
            ResponseEntity<TicketResponse> response = restTemplate.postForEntity(
                    url("/api/tickets/ask"),
                    new AskTicketRequest(testCase.question(), null),
                    TicketResponse.class
            );
            Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
            TicketResponse body = response.getBody();
            Assertions.assertNotNull(body);
            Assertions.assertNotNull(body.id());
            Assertions.assertFalse(body.citations().isEmpty());
            if (testCase.expectAutoResolved()) {
                Assertions.assertEquals(TicketStatus.RESOLVED, body.status(), testCase.question());
            } else {
                Assertions.assertEquals(TicketStatus.L1_ASSIGNED, body.status(), testCase.question());
            }
        }
    }

    @Test
    void shouldIngestHighValueTicketToKnowledge() {
        TicketResponse asked = restTemplate.postForObject(
                url("/api/tickets/ask"),
                new AskTicketRequest("账号连续输错密码后怎么解锁", null),
                TicketResponse.class
        );
        Assertions.assertNotNull(asked);

        ResponseEntity<TicketResponse> resolved = restTemplate.exchange(
                url("/api/tickets/" + asked.id() + "/resolve"),
                HttpMethod.POST,
                new HttpEntity<>(new ResolveTicketRequest("已按后台解锁流程处理", true)),
                TicketResponse.class
        );
        Assertions.assertTrue(resolved.getStatusCode().is2xxSuccessful());
        Assertions.assertEquals(TicketStatus.RESOLVED, resolved.getBody().status());

        ResponseEntity<String[]> kbTitles = restTemplate.getForEntity(url("/api/knowledge/titles"), String[].class);
        Assertions.assertTrue(kbTitles.getStatusCode().is2xxSuccessful());
        Assertions.assertNotNull(kbTitles.getBody());
        Assertions.assertTrue(
                java.util.Arrays.stream(kbTitles.getBody()).anyMatch(title -> title.contains("ticket-")),
                "expected ticket summary ingested to knowledge base"
        );
    }

    @Test
    void shouldExposeHitRateMetrics() {
        restTemplate.postForEntity(url("/api/tickets/ask"), new AskTicketRequest("PO号在哪里", null), TicketResponse.class);
        ResponseEntity<HitRateSnapshot> metrics = restTemplate.getForEntity(
                url("/api/tickets/metrics/hitrate"),
                HitRateSnapshot.class
        );
        Assertions.assertTrue(metrics.getStatusCode().is2xxSuccessful());
        Assertions.assertNotNull(metrics.getBody());
        Assertions.assertTrue(metrics.getBody().totalQueries() >= 1);
    }

    private List<TestCase> loadCases() throws Exception {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("rag-testset.json")) {
            Assertions.assertNotNull(inputStream, "rag-testset.json not found");
            return objectMapper.readValue(inputStream, new TypeReference<>() {});
        }
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }

    private record TestCase(String question, boolean expectAutoResolved) {
    }
}
