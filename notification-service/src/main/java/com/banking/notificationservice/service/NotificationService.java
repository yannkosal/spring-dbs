package com.banking.notificationservice.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationService {

    @KafkaListener(topics = "transaction.otp.generated")
    public void consumeOtpGenerated(@Payload Map<String, Object> payload) {
        try {
            String accountNumber = value(payload, "accountNumber");
            String otp = value(payload, "otp");
            String amount = value(payload, "amount");
            String reason = value(payload, "reason");

            sendAlert(accountNumber,
                    "TRANSACTION VERIFICATION REQUIRED",
                    String.format("Suspicious activity detected on your account. "
                                    + "Reason: %s "
                                    + "A transaction of %s is pending verification. "
                                    + "Your OTP is: %s. Valid for 5 minutes. "
                                    + "If this wasn't you - ignore this message. ",
                            reason, amount, otp
                    ));
        } catch (Exception e) {
            log.error("Error sending OTP notification", e);
        }
    }

    @KafkaListener(topics = "transaction.completed")
    public void consumeTransactionCompleted(@Payload Map<String, Object> payload) {
        try {
            String senderAccount = value(payload, "senderAccountNumber");
            String receiverAccount = value(payload, "receiverAccountNumber");
            String amount = value(payload, "amount");

            // DEBIT ALERT
            sendAlert(senderAccount, "DEBIT ALERT", String.format(
                    "%s debited from account %s", amount, senderAccount));

            // CREDIT ALERT
            sendAlert(receiverAccount, "CREDIT ALERT", String.format(
                    "%s credit from account %s", amount, receiverAccount));
        } catch (Exception e) {
            log.error("Error sending transaction notification", e);
        }
    }

    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(@Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String reason = (String) payload.get("reason");

            sendAlert(accountNumber, "SUSPICIOUS ACTIVITY DETECTED", String.format(
                    "Your account %s has been blocked." + "Reason: %s, " + "Please contact your bank immediately", accountNumber, reason));

        } catch (Exception e) {
            log.error("Error sending fraud alert: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "transaction.refund")
    public void consumeTransactionRefunded(@Payload Map<String, Object> payload) {
        try {
            String senderAccount = value(payload, "senderAccountNumber");
            String amount = value(payload, "amount");
            String reason = value(payload, "reason");

            sendAlert(senderAccount, "REFUND PROCESSED", String.format(
                    "Your transaction of %s was cancelled." + "Reason: %s" + "%s has been refunded to account %s.",
                    amount, reason, amount, senderAccount));
        } catch (Exception e) {
            log.error("Error sending refund notification", e);
        }
    }

    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(@Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = (String) payload.get("amount");

            sendAlert(accountNumber, "PAYMENT SUCCESSFUL", String.format(
                    "Payment of %s completed. " + "Razorpay ID: %s", amount, payload.get("razorpayPaymentId")
            ));
        } catch (Exception e) {
            log.error("Error send payment notification: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "payment.failed")
    public void consumePaymentFailed(@Payload Map<String, Object> payload) {
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = (String) payload.get("amount");
            sendAlert(accountNumber, "PAYMENT FAILED", String.format(
                    "Your payment of %s could not be processed. " + "Please try again or contact support", amount
            ));
        } catch (Exception e) {
            log.error("Error send payment failure notification: {}", e.getMessage());
        }
    }

    private void sendAlert(String accountNumber, String subject, String message) {
        log.info("--------------------------------------");
        log.info("Account: {}", accountNumber);
        log.info("Subject: {}", subject);
        log.info("Message: {}", message);
        log.info("--------------------------------------");
    }

    private String value(Map<String, Object> payload, String key) {
        Object value = payload.get(key);
        if (value == null) {
            throw new IllegalArgumentException("Missing notification field: " + key);
        }
        return value.toString();
    }
}
