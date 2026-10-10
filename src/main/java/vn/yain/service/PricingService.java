package vn.yain.service;

import org.springframework.stereotype.Service;
import vn.yain.entity.Court;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class PricingService {

    public static final BigDecimal PEAK_RATE = new BigDecimal("140000");
    public static final BigDecimal MORNING_RATE = new BigDecimal("80000");
    public static final BigDecimal STANDARD_RATE = new BigDecimal("100000");
    public static final BigDecimal DEPOSIT_PERCENTAGE = new BigDecimal("0.30");

    /**
     * Determines hourly rate based on court entity configuration and time of day
     */
    public BigDecimal calculateHourlyRate(Court court, int startHour) {
        if (court != null && court.getHourlyRate() != null && court.getHourlyRate().compareTo(BigDecimal.ZERO) > 0) {
            return court.getHourlyRate();
        }
        if (startHour >= 17 && startHour < 21) {
            return PEAK_RATE;
        } else if (startHour < 14) {
            return MORNING_RATE;
        } else {
            return STANDARD_RATE;
        }
    }

    /**
     * Calculates total price based on hourly rate and duration in hours
     */
    public BigDecimal calculateTotalPrice(BigDecimal hourlyRate, int durationHours) {
        if (hourlyRate == null) hourlyRate = STANDARD_RATE;
        int duration = Math.max(1, durationHours);
        return hourlyRate.multiply(BigDecimal.valueOf(duration));
    }

    /**
     * Calculates 30% required deposit
     */
    public BigDecimal calculateDepositAmount(BigDecimal totalPrice) {
        if (totalPrice == null) return BigDecimal.ZERO;
        return totalPrice.multiply(DEPOSIT_PERCENTAGE).setScale(0, RoundingMode.HALF_UP);
    }
}
