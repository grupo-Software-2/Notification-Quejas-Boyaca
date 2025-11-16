package co.edu.uptc.notification;

import co.edu.uptc.notification.service.BrokerSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class NotificationQuejasBoyacaApplication {

    @Autowired
    private BrokerSubscriptionService subscriptionService;

    public static void main(String[] args) {
        SpringApplication.run(NotificationQuejasBoyacaApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        subscriptionService.subscribeToReportViewedEvents();
    }
}
