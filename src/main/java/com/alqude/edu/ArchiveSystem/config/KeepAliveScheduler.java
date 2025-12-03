package com.alqude.edu.ArchiveSystem.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Scheduled task to keep the application active on Render.com free tier.
 * Render spins down free tier services after 15 minutes of inactivity.
 * This scheduler pings the health endpoint every 14 minutes to prevent cold starts.
 */
@Component
@EnableScheduling
public class KeepAliveScheduler {

    private static final Logger logger = LoggerFactory.getLogger(KeepAliveScheduler.class);

    @Value("${app.keep-alive.enabled:true}")
    private boolean enabled;

    @Value("${app.keep-alive.url:}")
    private String keepAliveUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Ping the application every 14 minutes to keep it alive.
     * Runs at minute 0, 14, 28, and 42 of every hour.
     */
    @Scheduled(cron = "0 */14 * * * *")
    public void keepAlive() {
        if (!enabled || keepAliveUrl == null || keepAliveUrl.isEmpty()) {
            logger.debug("Keep-alive is disabled or URL not configured");
            return;
        }

        try {
            String response = restTemplate.getForObject(keepAliveUrl + "/actuator/health", String.class);
            logger.info("Keep-alive ping successful: {}", response);
        } catch (Exception e) {
            logger.warn("Keep-alive ping failed: {}", e.getMessage());
        }
    }

    /**
     * Log application status every hour for monitoring.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void logStatus() {
        logger.info("Application is running - hourly status check");
    }
}
