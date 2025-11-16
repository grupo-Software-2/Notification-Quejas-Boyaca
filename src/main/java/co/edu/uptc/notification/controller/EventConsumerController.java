package co.edu.uptc.notification.controller;

import co.edu.uptc.notification.dto.ReportViewedEventDTO;
import co.edu.uptc.notification.service.EmailNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class EventConsumerController {

    private static final Logger log = LoggerFactory.getLogger(EventConsumerController.class);
    private final EmailNotificationService emailService;

    public EventConsumerController(EmailNotificationService emailService) {
        this.emailService = emailService;
    }

    @PostMapping("/events/report-viewed")
    public ResponseEntity<?> handleReportViewedEvent(@RequestBody ReportViewedEventDTO event) {
        try {
            if (event == null) {
                log.error("Received null REPORT_VIEWED event");
                return ResponseEntity.badRequest().body(Map.of(
                        "error", "Invalid event payload"
                ));
            }

            log.info("Received REPORT_VIEWED event: {}", event.getEventId());
            log.info("Event metadata → type={}, timestamp={}, ip={}, totalComplaints={}, reportType={}",
                    event.getEventType(),
                    event.getTimestamp(),
                    event.getIpAddress(),
                    event.getTotalComplaints(),
                    event.getReportType()
            );

            // Ahora se envía DIRECTAMENTE el evento sin wrapper
            emailService.sendReportViewedNotification(event);

            return ResponseEntity.ok(Map.of(
                    "message", "Event processed successfully",
                    "eventId", event.getEventId()
            ));

        } catch (Exception e) {
            log.error("Error processing REPORT_VIEWED event", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", "Failed to process event: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "notification-service",
                "timestamp", System.currentTimeMillis()
        ));
    }

    @GetMapping("/status")
    public ResponseEntity<?> getStatus() {
        return ResponseEntity.ok(Map.of(
                "emailEnabled", emailService.isEmailEnabled(), "service", "notification-service"
        ));
    }
}
