package com.hieu.analytics_service.service;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight aggregations against analytics-events-* indices for sanity-check endpoints.
 * Kibana is the main analytics UI — this exists for scripts / health probes only.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnalyticsQueryService {

    private final ElasticsearchTemplate esTemplate;

    @Value("${analytics.index-prefix:analytics-events}")
    private String indexPrefix;

    public Map<String, Object> summary(Instant from, Instant to) {
        IndexCoordinates indices = IndexCoordinates.of(indexPrefix + "-*");
        Map<String, Object> result = new HashMap<>();
        result.put("from", from);
        result.put("to", to);

        try {
            result.put("totalEvents", countByEventType(indices, from, to, null));
            result.put("orderPlaced", countByEventType(indices, from, to, "ORDER_PLACED"));
            result.put("orderCancelled", countByEventType(indices, from, to, "ORDER_CANCELLED"));
            result.put("paymentCompleted", countByEventType(indices, from, to, "PAYMENT_COMPLETED"));
            result.put("paymentFailed", countByEventType(indices, from, to, "PAYMENT_FAILED"));
            result.put("usersRegistered", countByEventType(indices, from, to, "USER_REGISTERED"));
        } catch (Exception e) {
            log.warn("Summary query failed: {}", e.getMessage());
            result.put("error", e.getMessage());
        }
        return result;
    }

    private long countByEventType(IndexCoordinates indices, Instant from, Instant to, String eventType) {
        var q = NativeQuery.builder()
                .withQuery(buildRangeAndType(from, to, eventType))
                .withMaxResults(0)
                .build();
        SearchHits<Map> hits = esTemplate.search(q, Map.class, indices);
        return hits.getTotalHits();
    }

    private Query buildRangeAndType(Instant from, Instant to, String eventType) {
        return Query.of(qb -> qb.bool(b -> {
            b.must(m -> m.range(r -> r.date(d -> d
                    .field("timestamp")
                    .gte(from.toString())
                    .lt(to.toString()))));
            if (eventType != null) {
                b.must(m -> m.term(t -> t.field("eventType").value(eventType)));
            }
            return b;
        }));
    }
}
