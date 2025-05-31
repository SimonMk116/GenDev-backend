package com.SimonMk116.gendev.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents a single internet offer from a provider, detailing various aspects of the plan.
 * This class serves as a data model for transferring and displaying internet service options.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InternetOffer {
    /**
     * A unique identifier for the internet product.
     */
    private String productId;
    /**
     * The name of the internet service provider offering this plan.
     */
    private String providerName;
    /**
     * The advertised download speed of the internet connection, typically in Mbps.
     */
    private int speed;
    /**
     * The monthly cost of the offer during the initial contract period, in cents.
     */
    private int monthlyCostInCent;
    /**
     * The monthly cost of the offer after the initial two-year contract period, in cents.
     */
    private int afterTwoYearsMonthlyCost;
    /**
     * The type of internet connection (e.g., "DSL", "FIBER", "CABLE", "MOBILE" ).
     */
    private String connectionType;
    /**
     * The minimum contract duration for this offer, in months.
     * Null if no specific duration is advertised or applicable.
     */
    private Integer durationInMonths;
    /**
     * Indicates whether an installation service is included or available with this offer.
     * True if included/available, false otherwise.
     */
    private Boolean installationService;
    /**
     * Describes any TV services or packages included with the internet offer.
     * E.g., "Pong TV Premium", "ByteLive Plus".
     */
    private String tv;
    /**
     * The high-speed data limit after which this offer throttles speed.
     * Null if not applicable.
     */
    private Integer limitFrom;
    /**
     * The maximum age limit for specific customer groups eligible for this offer.
     * Null if no age restriction applies.
     */
    private Integer maxAge;
    /**
     * The type of voucher applied to the offer.
     * Can be "PERCENTAGE" for a percentage-based discount or "ABSOLUTE" for a fixed amount discount.
     * Null if no voucher applies.
     */
    private String voucherType;
    /**
     * The value of the voucher.
     * Interpretation depends on {@code voucherType} (e.g., 10 for 10% or 10 EUR).
     */
    private Integer voucherValue;
    /**
     * General discount amount.
     */
    private Integer discount;
    /**
     * The duration for which the {@code discount} is applied, in months.
     */
    private int discountDuration;
    /**
     * The maximum monetary value (in cents) that the discount can reach.
     * Relevant for percentage-based discounts.
     */
    private int discountCap;
    /**
     * The percentage value of the discount for "PERCENTAGE" type vouchers.
     * Only applicable when {@code voucherType} is "PERCENTAGE".
     */
    private Integer percentage;
    /**
     * The maximum discount value in cents for "PERCENTAGE" type vouchers.
     * Only applicable when {@code voucherType} is "PERCENTAGE".
     */
    private Integer maxDiscountInCent;
    /**
     * The absolute discount value in cents for "ABSOLUTE" type vouchers.
     * Only applicable when {@code voucherType} is "ABSOLUTE".
     */
    private Integer discountInCent;
    /**
     * The minimum order value in cents required to apply the "ABSOLUTE" type voucher.
     * Only applicable when {@code voucherType} is "ABSOLUTE".
     */
    private Integer minOrderValueInCent;


}
