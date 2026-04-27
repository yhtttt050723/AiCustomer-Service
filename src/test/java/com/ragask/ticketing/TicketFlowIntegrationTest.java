package com.ragask.ticketing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ragask.ticketing.common.api.Result;
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
            ResponseEntity<String> response = restTemplate.postForEntity(
                    url("/api/tickets/ask"),
                    new AskTicketRequest(testCase.getQuestion(), null),
                    String.class
            );
            Assertions.assertTrue(response.getStatusCode().is2xxSuccessful());
            Result<TicketResponse> wrapped = objectMapper.readValue(
                    response.getBody(),
                    new TypeReference<>() {}
            );
            TicketResponse body = wrapped.getData();
            Assertions.assertNotNull(body);
            Assertions.assertNotNull(body.getId());
            Assertions.assertFalse(body.getCitations().isEmpty());
            if (testCase.isExpectAutoResolved()) {
                Assertions.assertEquals(TicketStatus.RESOLVED, body.getStatus(), testCase.getQuestion());
            } else {
                Assertions.assertEquals(TicketStatus.L1_ASSIGNED, body.getStatus(), testCase.getQuestion());
            }
        }
    }

    @Test
    void shouldIngestHighValueTicketToKnowledge() {
        String askedRaw = restTemplate.postForObject(
                url("/api/tickets/ask"),
                new AskTicketRequest("账号连续输错密码后怎么解锁", null),
                String.class
        );
        Assertions.assertNotNull(askedRaw);
        TicketResponse asked = unwrap(askedRaw, new TypeReference<Result<TicketResponse>>() {});

        ResponseEntity<String> resolved = restTemplate.exchange(
                url("/api/tickets/" + asked.getId() + "/resolve"),
                HttpMethod.POST,
                new HttpEntity<>(new ResolveTicketRequest("已按后台解锁流程处理", true)),
                String.class
        );
        Assertions.assertTrue(resolved.getStatusCode().is2xxSuccessful());
        TicketResponse resolvedBody = unwrap(resolved.getBody(), new TypeReference<Result<TicketResponse>>() {});
        Assertions.assertEquals(TicketStatus.RESOLVED, resolvedBody.getStatus());

        ResponseEntity<String> kbTitles = restTemplate.getForEntity(url("/api/knowledge/titles"), String.class);
        Assertions.assertTrue(kbTitles.getStatusCode().is2xxSuccessful());
        String[] titles = unwrap(kbTitles.getBody(), new TypeReference<Result<String[]>>() {});
        Assertions.assertNotNull(titles);
        Assertions.assertTrue(
                java.util.Arrays.stream(titles).anyMatch(title -> title.contains("ticket-")),
                "expected ticket summary ingested to knowledge base"
        );
    }

    @Test
    void shouldExposeHitRateMetrics() {
        restTemplate.postForEntity(url("/api/tickets/ask"), new AskTicketRequest("PO号在哪里", null), String.class);
        ResponseEntity<String> metrics = restTemplate.getForEntity(
                url("/api/tickets/metrics/hitrate"),
                String.class
        );
        Assertions.assertTrue(metrics.getStatusCode().is2xxSuccessful());
        HitRateSnapshot snapshot = unwrap(metrics.getBody(), new TypeReference<Result<HitRateSnapshot>>() {});
        Assertions.assertNotNull(snapshot);
        Assertions.assertTrue(snapshot.getTotalQueries() >= 1);
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

    private <T> T unwrap(String raw, TypeReference<Result<T>> type) {
        try {
            Result<T> wrapped = objectMapper.readValue(raw, type);
            Assertions.assertNotNull(wrapped);
            Assertions.assertEquals(200, wrapped.getCode(), "expected success result");
            return wrapped.getData();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static class TestCase {
        private String question;
        private boolean expectAutoResolved;

        public String getQuestion() {
            return question;
        }

        public boolean isExpectAutoResolved() {
            return expectAutoResolved;
        }
    }
}
