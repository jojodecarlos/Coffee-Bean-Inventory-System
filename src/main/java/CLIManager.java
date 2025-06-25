package com.example.coffeedms;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;

/**
 * Joses De Carlos, CEN3024C 31774, 06-18-2025
 * Handles all command-line interactions with the user.
 * Displays menus, prompts for input, and calls repository methods.
 */
public class CLIManager {
    private final BeanRepository repo;
    private final Scanner scanner = new Scanner(System.in);

    /**
     * Constructs a CLIManager tied to a given repository.
     *
     * @param repo the BeanRepository for CRUD operations
     */
    public CLIManager(BeanRepository repo) {
        this.repo = repo;
    }

    /**
     * showMenu
     *
     * Displays the main menu repeatedly until the user chooses to exit.
     */
    public void showMenu() {
        while (true) {
            System.out.println("\n=== Coffee Bean DMS ===");
            System.out.println("1) Add bean lot");
            System.out.println("2) Remove bean lot");
            System.out.println("3) Update bean lot");
            System.out.println("4) View all bean lots");
            System.out.println("5) Calculate inventory value");
            System.out.println("6) Exit");
            System.out.print("Select an option: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1": handleAdd();    break;
                case "2": handleRemove(); break;
                case "3": handleUpdate(); break;
                case "4": handleView();   break;
                case "5": handleCalculate(); break;
                case "6":
                    System.out.println("Goodbye!");
                    return;
                default:
                    System.out.println("Invalid option, please try again.");
            }
        }
    }

    /**
     * handleAdd
     *
     * Prompts user to batch-import or manually add a bean,
     * then performs the add operation.
     */
    private void handleAdd() {
        System.out.print("Batch import (1) or manual entry (2)? ");
        String opt = scanner.nextLine().trim();
        if (opt.equals("1")) {
            System.out.print("Enter file path: ");
            String path = scanner.nextLine().trim();
            try {
                List<CoffeeBean> loaded = repo.loadFromFile(path);
                System.out.printf("%d bean(s) imported.%n", loaded.size());
            } catch (IOException ex) {
                System.out.println("Error loading file: " + ex.getMessage());
            }
        } else if (opt.equals("2")) {
            CoffeeBean bean = promptForBean();
            boolean ok = repo.add(bean);
            System.out.println(ok ? "Bean added." : "Failed to add bean.");
        } else {
            System.out.println("Invalid choice.");
        }
    }

    /**
     * handleRemove
     *
     * Prompts for a beanID and removes that bean from the repository.
     */
    private void handleRemove() {
        String id = promptForID();
        boolean ok = repo.removeByID(id);
        System.out.println(ok ? "Bean removed." : "No bean with that ID.");
    }

    /**
     * handleUpdate
     *
     * Prompts for a beanID, loads it, then prompts for new values
     * and updates the repository.
     */
    private void handleUpdate() {
        String id = promptForID();
        CoffeeBean existing = repo.findByID(id);
        if (existing == null) {
            System.out.println("No bean with that ID.");
            return;
        }
        System.out.println("Enter new values:");
        CoffeeBean updated = promptForBeanWithID(id);
        boolean ok = repo.update(updated);
        System.out.println(ok ? "Bean updated." : "Update failed.");
    }

    /**
     * handleView
     *
     * Retrieves all beans and prints them to the console.
     */
    private void handleView() {
        List<CoffeeBean> all = repo.findAll();
        if (all.isEmpty()) {
            System.out.println("No beans to display.");
        } else {
            all.forEach(b -> System.out.println(b));
        }
    }

    /**
     * handleCalculate
     *
     * Calls the custom action to compute total inventory value
     * and displays the result.
     */
    private void handleCalculate() {
        BigDecimal total = repo.calculateTotalInventoryValue();
        System.out.println("Total inventory value: $" + total);
    }

    /**
     * promptForBean
     *
     * Prompts the user for all bean fields and returns
     * a new CoffeeBean instance.
     *
     * @return populated CoffeeBean
     */
    private CoffeeBean promptForBean() {
        return promptForBeanWithID(null);
    }

    /**
     * promptForBeanWithID
     *
     * Prompts user for bean data, using a fixed ID if provided.
     *
     * @param fixedID existing ID or null to prompt for one
     * @return populated CoffeeBean
     */
    private CoffeeBean promptForBeanWithID(String fixedID) {
        String beanID = fixedID != null ? fixedID : promptNonEmpty("Bean ID");
        String origin = promptNonEmpty("Origin country");
        String farm = promptNonEmpty("Farm name");
        RoastLevel roast = promptEnum("Roast level (LIGHT/MEDIUM/DARK)", RoastLevel.class);
        LocalDate date = promptDate("Roast date (YYYY-MM-DD)");
        double qty = promptDouble("Quantity (kg)", 0, Double.MAX_VALUE);
        BigDecimal cost = promptBigDecimal("Cost per kg", BigDecimal.ZERO, null);
        String notes = promptNonEmpty("Flavor notes");
        double caffeine = promptDouble("Caffeine mg per gram", 0, Double.MAX_VALUE);

        return new CoffeeBean(beanID, origin, farm, roast, date, qty, cost, notes, caffeine);
    }

    /**
     * promptForID
     *
     * Prompts until the user enters a non-empty bean ID.
     *
     * @return user-entered bean ID
     */
    private String promptForID() {
        return promptNonEmpty("Bean ID");
    }

    /**
     * promptNonEmpty
     *
     * Ensures the user provides a non-blank string.
     *
     * @param prompt text to show
     * @return non-empty user input
     */
    private String promptNonEmpty(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            String input = scanner.nextLine().trim();
            if (!input.isEmpty()) return input;
            System.out.println("Input cannot be empty.");
        }
    }

    /**
     * promptEnum
     *
     * Prompts the user to enter one of an enum’s values.
     *
     * @param prompt text to show
     * @param type   enum class
     * @param <E>    enum type
     * @return chosen enum constant
     */
    private <E extends Enum<E>> E promptEnum(String prompt, Class<E> type) {
        while (true) {
            System.out.print(prompt + ": ");
            try {
                return Enum.valueOf(type, scanner.nextLine().trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                System.out.println("Invalid value. Try again.");
            }
        }
    }

    /**
     * promptDate
     *
     * Prompts until the user enters a valid ISO date.
     *
     * @param prompt text to show
     * @return parsed LocalDate
     */
    private LocalDate promptDate(String prompt) {
        while (true) {
            System.out.print(prompt + ": ");
            try {
                return LocalDate.parse(scanner.nextLine().trim());
            } catch (Exception ex) {
                System.out.println("Invalid date format.");
            }
        }
    }

    /**
     * promptDouble
     *
     * Prompts until the user enters a number within the given range.
     *
     * @param prompt text to show
     * @param min    inclusive minimum
     * @param max    inclusive maximum
     * @return parsed double
     */
    private double promptDouble(String prompt, double min, double max) {
        while (true) {
            System.out.print(prompt + ": ");
            try {
                double val = Double.parseDouble(scanner.nextLine().trim());
                if (val >= min && val <= max) return val;
                System.out.printf("Value must be between %.2f and %.2f.%n", min, max);
            } catch (NumberFormatException ex) {
                System.out.println("Invalid number.");
            }
        }
    }

    /**
     * promptBigDecimal
     *
     * Prompts until the user enters a decimal within the given range.
     *
     * @param prompt text to show
     * @param min    inclusive minimum or null
     * @param max    inclusive maximum or null
     * @return parsed BigDecimal
     */
    private BigDecimal promptBigDecimal(String prompt, BigDecimal min, BigDecimal max) {
        while (true) {
            System.out.print(prompt + ": ");
            try {
                BigDecimal val = new BigDecimal(scanner.nextLine().trim());
                if ((min == null || val.compareTo(min) >= 0)
                        && (max == null || val.compareTo(max) <= 0)) {
                    return val;
                }
                System.out.println("Value out of range.");
            } catch (NumberFormatException ex) {
                System.out.println("Invalid decimal number.");
            }
        }
    }
}
