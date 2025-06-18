package com.example.coffeedms;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a single lot of coffee beans with all required attributes.
 * Used by the DMS to store, display, and calculate values for each lot.
 */
public class CoffeeBean {
    private final String beanID;
    private final String originCountry;
    private final String farmName;
    private final RoastLevel roastLevel;
    private final LocalDate roastDate;
    private final double quantityKg;
    private final BigDecimal costPerKg;
    private final String flavorNotes;
    private final double caffeineContentMgPerGram;

    /**
     * Constructs a CoffeeBean with all fields initialized.
     *
     * @param beanID                 unique identifier for the bean lot
     * @param originCountry          country where the beans were grown
     * @param farmName               name of the farm or cooperative
     * @param roastLevel             roast level (LIGHT, MEDIUM, DARK)
     * @param roastDate              date when the beans were roasted
     * @param quantityKg             weight of the lot in kilograms
     * @param costPerKg              purchase cost per kilogram
     * @param flavorNotes            tasting notes for the beans
     * @param caffeineContentMgPerGram caffeine content in milligrams per gram
     */
    public CoffeeBean(String beanID,
                      String originCountry,
                      String farmName,
                      RoastLevel roastLevel,
                      LocalDate roastDate,
                      double quantityKg,
                      BigDecimal costPerKg,
                      String flavorNotes,
                      double caffeineContentMgPerGram) {
        this.beanID = beanID;
        this.originCountry = originCountry;
        this.farmName = farmName;
        this.roastLevel = roastLevel;
        this.roastDate = roastDate;
        this.quantityKg = quantityKg;
        this.costPerKg = costPerKg;
        this.flavorNotes = flavorNotes;
        this.caffeineContentMgPerGram = caffeineContentMgPerGram;
    }

    // Getters

    public String getBeanID() {
        return beanID;
    }

    public String getOriginCountry() {
        return originCountry;
    }

    public String getFarmName() {
        return farmName;
    }

    public RoastLevel getRoastLevel() {
        return roastLevel;
    }

    public LocalDate getRoastDate() {
        return roastDate;
    }

    public double getQuantityKg() {
        return quantityKg;
    }

    public BigDecimal getCostPerKg() {
        return costPerKg;
    }

    public String getFlavorNotes() {
        return flavorNotes;
    }

    public double getCaffeineContentMgPerGram() {
        return caffeineContentMgPerGram;
    }

    /**
     * calculateValue
     *
     * Computes the total monetary value of this bean lot
     * by multiplying cost per kilogram by the quantity.
     *
     * @return BigDecimal representing quantityKg × costPerKg
     */
    public BigDecimal calculateValue() {
        return costPerKg.multiply(BigDecimal.valueOf(quantityKg));
    }

    /**
     * toString
     *
     * Formats all CoffeeBean attributes into a single line
     * suitable for display in the CLI.
     *
     * @return a string containing all fields of this bean lot
     */
    @Override
    public String toString() {
        return String.format(
                "%s | %s | %s | %s | %s | %.2f kg | $%s/kg | %s | %.2f mg/g",
                beanID,
                originCountry,
                farmName,
                roastLevel,
                roastDate,
                quantityKg,
                costPerKg,
                flavorNotes,
                caffeineContentMgPerGram
        );
    }
}
