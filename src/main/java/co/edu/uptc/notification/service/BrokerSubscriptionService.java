package co.edu.uptc.notification.service;

import co.edu.uptc.notification.dto.SubscriptionRequestDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class BrokerSubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(BrokerSubscriptionService.class);
    private final RestTemplate restTemplate;

    @Value("${event.broker.url}")
    private String brokerUrl;

    @Value("${notification.service.callback-url}")
    private String callbackUrl;

    @Value("${notification.service.name:notification-service}")
    private String serviceName;

    public BrokerSubscriptionService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void subscribeToReportViewedEvents() {
        try {
            log.info("Subscribing to REPORT_VIEWED events from broker: {}", brokerUrl);

            String subscribeUrl = brokerUrl + "/api/events/subscribe";

            SubscriptionRequestDTO subscription = new SubscriptionRequestDTO();
            subscription.setEventType("REPORT_VIEWED");
            subscription.setCallbackUrl(callbackUrl + "/api/notifications/events/report-viewed");
            subscription.setSubscriberName(serviceName);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<SubscriptionRequestDTO> request = new HttpEntity<>(subscription, headers);

            ResponseEntity<Map> response = restTemplate.postForEntity(subscribeUrl, request, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                Map<String, Object> body = response.getBody();
                String subscriptionId = body != null ? (String) body.get("subscriptionId") : "unknown";
                log.info("Successfully subscribed to broker with subscriptionId: {}", subscriptionId);
            } else {
                log.warn("Failed to subscribe to broker, status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error subscribing to broker: {}", e.getMessage(), e);
        }
    }

    public boolean checkBrokerHealth() {
        try {
            String healthUrl = brokerUrl + "/api/events/health";
            ResponseEntity<Map> response = restTemplate.getForEntity(healthUrl, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.error("Broker health check failed: {}", e.getMessage());
            return false;
        }
    }
}
