package com.example.nexus.service;

import com.example.nexus.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

public class AuthenticationService {
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationService.class);

    private User currentUser;
    private Consumer<User> onUserChanged;
    private Consumer<Boolean> onAuthStatusChanged;

    public void signup(String email, String password, String username) {
        try {

            if (!isValidEmail(email) || password.length() < 8) {
                notifyAuthStatusChanged(false);
                logger.warn("Signup validation failed for: {}", email);
                return;
            }

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setUsername(username);
            newUser.setPasswordHash(hashPassword(password));

            this.currentUser = newUser;
            notifyUserChanged(newUser);
            notifyAuthStatusChanged(true);
            logger.info("User signed up: {}", email);
        } catch (Exception e) {
            logger.error("Signup error", e);
            notifyAuthStatusChanged(false);
        }
    }

    public void login(String email, String password) {
        try {
            Optional<User> user = findUserByEmail(email);
            if (user.isPresent() && verifyPassword(password, user.get().getPasswordHash())) {
                this.currentUser = user.get();
                notifyUserChanged(currentUser);
                notifyAuthStatusChanged(true);
                logger.info("User logged in: {}", email);
            } else {
                notifyAuthStatusChanged(false);
                logger.warn("Login failed for: {}", email);
            }
        } catch (Exception e) {
            logger.error("Login error", e);
            notifyAuthStatusChanged(false);
        }
    }

    public void logout() {
        try {
            this.currentUser = null;
            notifyUserChanged(null);
            notifyAuthStatusChanged(false);
            logger.info("User logged out");
        } catch (Exception e) {
            logger.error("Logout error", e);
        }
    }

    public boolean isAuthenticated() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public void setOnUserChanged(Consumer<User> callback) {
        this.onUserChanged = callback;
    }

    public void setOnAuthStatusChanged(Consumer<Boolean> callback) {
        this.onAuthStatusChanged = callback;
    }

    private void notifyUserChanged(User user) {
        if (onUserChanged != null) {
            onUserChanged.accept(user);
        }
    }

    private void notifyAuthStatusChanged(boolean isAuthenticated) {
        if (onAuthStatusChanged != null) {
            onAuthStatusChanged.accept(isAuthenticated);
        }
    }

    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    private String hashPassword(String password) {

        return password;
    }

    private boolean verifyPassword(String password, String hash) {

        return hashPassword(password).equals(hash);
    }

    private Optional<User> findUserByEmail(String email) {

        return Optional.empty();
    }
}
