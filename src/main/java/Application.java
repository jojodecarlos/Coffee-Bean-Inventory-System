package com.example.coffeedms;

/**
 * Entry point for the Coffee Bean DMS CLI application.
 * Manages the overall program flow by instantiating the repository
 * and launching the menu interface.
 */
public class Application {
    /**
     * main
     *
     * Starts the Coffee Bean DMS by creating necessary objects
     * and invoking the command-line interface loop.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        BeanRepository repo = new BeanRepository();
        new CLIManager(repo).showMenu();
    }
}
