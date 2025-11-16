package co.edu.uptc.notification.service;

import co.edu.uptc.notification.dto.ReportViewedEventDTO;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class EmailNotificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailNotificationService.class);

    @Autowired
    private JavaMailSender mailSender;

    @Value("${notification.email.enabled:true}")
    private boolean emailEnabled;

    @Value("${notification.email.admin-emails}")
    private String[] adminEmails;

    @Value("${notification.email.from}")
    private String fromEmail;

    @Value("${notification.email.from-name:Sistema de Quejas Boyacá}")
    private String fromName;

    @Async("eventProcessorExecutor")
    public CompletableFuture<Void> sendReportViewedNotification(ReportViewedEventDTO event) {
        return CompletableFuture.runAsync(() -> {

            if (!emailEnabled) {
                log.debug("Email notifications disabled, skipping");
                return;
            }

            if (adminEmails == null || adminEmails.length == 0) {
                log.warn("No admin emails configured");
                return;
            }

            try {
                log.info("=== Processing notification ===");
                log.info("Event ID: {}", event.getEventId());
                log.info("Event Type: {}", event.getEventType());
                log.info("Report Type (raw): {}", event.getReportType());
                log.info("Timestamp: {}", event.getTimestamp());
                log.info("Total Complaints: {}", event.getTotalComplaints());
                log.info("IP Address: {}", event.getIpAddress());
                log.info("==============================");

                String subject = String.format(
                        "🔍 Reporte Visualizado - %d quejas (%s)",
                        event.getTotalComplaints(),
                        event.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                );

                String htmlContent = generateEmailContent(event);

                sendEmailsInParallel(adminEmails, subject, htmlContent);

                log.info("Report viewed notification processed successfully for event: {}", event.getEventId());

            } catch (Exception e) {
                log.error("Failed to send report viewed notification for event {}: {}",
                        event.getEventId(), e.getMessage(), e);
                throw new RuntimeException("Error sending email notification", e);
            }
        });
    }

    private String generateEmailContent(ReportViewedEventDTO event) {
        LocalDateTime timestamp = event.getTimestamp() != null ? event.getTimestamp() : LocalDateTime.now();
        String formattedDate = timestamp.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));

        String reportType = "REPORTE GENERAL";
        try {
            if (event.getReportType() != null) {
                String temp = event.getReportType().trim();
                if (!temp.isEmpty()) {
                    reportType = temp.replace("_", " ");
                }
            }
        } catch (Exception e) {
            log.warn("Error processing reportType, using default: {}", e.getMessage());
        }

        // HTML SIMPLIFICADO
        return String.format("""
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="UTF-8">
                            <style>
                                body { 
                                    font-family: Arial, sans-serif; 
                                    margin: 0; 
                                    padding: 20px; 
                                    background-color: #f5f5f5; 
                                }
                                .container { 
                                    max-width: 500px; 
                                    margin: 0 auto; 
                                    background: white; 
                                    border-radius: 8px; 
                                    padding: 30px; 
                                    box-shadow: 0 2px 8px rgba(0,0,0,0.1); 
                                }
                                .title { 
                                    color: #333; 
                                    font-size: 20px; 
                                    font-weight: bold; 
                                    margin-bottom: 20px; 
                                    border-bottom: 2px solid #007bff; 
                                    padding-bottom: 10px; 
                                }
                                .info { 
                                    margin: 15px 0; 
                                    line-height: 1.6; 
                                    color: #555; 
                                }
                                .info strong { 
                                    color: #333; 
                                }
                                .footer { 
                                    margin-top: 30px; 
                                    padding-top: 20px; 
                                    border-top: 1px solid #eee; 
                                    text-align: center; 
                                    font-size: 12px; 
                                    color: #999; 
                                }
                            </style>
                        </head>
                        <body>
                            <div class="container">
                                <div class="title">🔍 Visualización de Reportes - Sistema de Quejas Boyacá</div>
                        
                                <div class="info">
                                    <p>Se ha registrado una visualización del sistema de quejas.</p>
                        
                                    <p><strong>Tipo de reporte:</strong> %s</p>
                        
                                    <p><strong>Fecha y hora:</strong> %s</p>
                                </div>
                        
                                <div class="footer">
                                    Sistema de Alertas Automático - Boyacá
                                </div>
                            </div>
                        </body>
                        </html>
                        """,
                reportType,
                formattedDate
        );
    }

    private void sendEmailsInParallel(String[] recipients, String subject, String htmlContent) {
        List<CompletableFuture<Void>> emailTasks = Arrays.stream(recipients)
                .map(recipient -> sendSingleEmailAsync(recipient, subject, htmlContent))
                .collect(Collectors.toList());

        CompletableFuture.allOf(emailTasks.toArray(new CompletableFuture[0])).join();

        log.info("All {} emails sent successfully", recipients.length);
    }

    @Async("emailSenderExecutor")
    public CompletableFuture<Void> sendSingleEmailAsync(String recipient, String subject, String htmlContent) {
        return CompletableFuture.runAsync(() -> {
            try {
                log.debug("Sending email to: {}", recipient);
                sendEmail(recipient, subject, htmlContent);
                log.info("Email sent successfully to: {}", recipient);
            } catch (Exception e) {
                log.error("Failed to send email to {}: {}", recipient, e.getMessage(), e);
            }
        });
    }

    private void sendEmail(String to, String subject, String htmlContent) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(htmlContent, true);
        helper.setFrom(fromEmail, fromName);

        mailSender.send(message);
        log.debug("Email sent to: {}", to);
    }

    private String getBrowserInfo(String userAgent) {
        if (userAgent == null) return "Desconocido";

        if (userAgent.contains("Chrome")) return "Chrome";
        if (userAgent.contains("Firefox")) return "Firefox";
        if (userAgent.contains("Safari")) return "Safari";
        if (userAgent.contains("Edge")) return "Edge";
        return "Otro navegador";
    }

    public boolean isEmailEnabled() {
        return emailEnabled;
    }
}