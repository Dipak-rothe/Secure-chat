package com.dipak.desktop;

/**
 * Entry point that does not extend javafx.application.Application.
 * This prevents the JavaFX runtime from checking for JavaFX modules on the module path,
 * allowing the application to be launched normally or packaged with jpackage.
 */
public class Launcher {
    public static void main(String[] args) {
        App.main(args);
    }
}
