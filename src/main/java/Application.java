/**
 * Joses De Carlos, CEN3024C 31774, 06-18-2025
 * Entry point for the Coffee Bean DMS CLI application.
 * Manages the overall program flow by instantiating the repository
 * and launching the menu interface.
 */
package com.example.coffeedms;

import javax.swing.SwingUtilities;

/**
 * Entry point for the Coffee Bean DMS GUI application.
 * Launches the CoffeeDmsGUI on the Event Dispatch Thread.
 */
public class Application {
    /**
     * main
     *
     * @param args unused
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(CoffeeDmsGUI::new);
    }
}

