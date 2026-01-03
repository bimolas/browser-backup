package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Profile;
import com.example.nexus.repository.ProfileRepository;
import com.example.nexus.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.*;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class ProfileService {
    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);
    private static final String SESSION_FILE = "current_profile_session.dat";

    private final ProfileRepository profileRepository;
    private Profile currentProfile;
    private final List<Consumer<Profile>> changeListeners = new java.util.ArrayList<>();

    public ProfileService(DIContainer container) {
        this.profileRepository = container.getOrCreate(ProfileRepository.class);
        loadDefaultProfile();
    }

    private void loadDefaultProfile() {
        try {

            Profile sessionProfile = loadSessionProfile();
            if (sessionProfile != null) {
                currentProfile = sessionProfile;
                logger.info("Loaded profile from session: {}", currentProfile.getUsername());
            } else {

                List<Profile> profiles = profileRepository.findAll();
                if (profiles.isEmpty()) {
                    currentProfile = new Profile();
                    currentProfile.setUsername("Guest");
                    currentProfile.setEmail("guest@nexus.local");
                    currentProfile.setGuest(true);
                    currentProfile.setLoggedIn(true);
                    profileRepository.save(currentProfile);
                } else {
                    currentProfile = profiles.get(0);
                }
            }
            notifyListeners();
        } catch (Exception e) {
            logger.error("Error loading default profile", e);
            currentProfile = new Profile();
            currentProfile.setUsername("Guest");
        }
    }

    private Profile loadSessionProfile() {
        try {
            Path sessionPath = Paths.get(System.getProperty("user.home"), ".nexus", SESSION_FILE);
            if (Files.exists(sessionPath)) {
                String content = Files.readString(sessionPath);
                int profileId = Integer.parseInt(content.trim());
                Profile profile = profileRepository.findById(profileId);
                if (profile != null) {
                    return profile;
                }
            }
        } catch (Exception e) {
            logger.debug("Could not load session profile", e);
        }
        return null;
    }

    private void saveSessionProfile() {
        try {
            Path nexusDir = Paths.get(System.getProperty("user.home"), ".nexus");
            Files.createDirectories(nexusDir);
            Path sessionPath = nexusDir.resolve(SESSION_FILE);

            if (currentProfile != null && currentProfile.getId() > 0) {
                Files.writeString(sessionPath, String.valueOf(currentProfile.getId()));
                logger.debug("Session profile saved: {}", currentProfile.getId());
            }
        } catch (Exception e) {
            logger.warn("Could not save session profile", e);
        }
    }

    public Profile getCurrentProfile() {
        if (currentProfile == null) {
            loadDefaultProfile();
        }
        return currentProfile;
    }

    public void updateProfile(Profile profile) {
        try {
            currentProfile = profile;
            profileRepository.update(profile);
            saveSessionProfile();
            notifyListeners();
            logger.info("Profile updated: {}", profile.getUsername());
        } catch (Exception e) {
            logger.error("Error updating profile", e);
        }
    }

    public void saveProfile(Profile profile) {
        try {
            currentProfile = profile;
            if (profile.getId() == 0) {
                profileRepository.save(profile);
            } else {
                profileRepository.update(profile);
            }
            saveSessionProfile();
            notifyListeners();
            logger.info("Profile saved: {}", profile.getUsername());
        } catch (Exception e) {
            logger.error("Error saving profile", e);
        }
    }

    public void addProfileChangeListener(Consumer<Profile> listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeProfileChangeListener(Consumer<Profile> listener) {
        changeListeners.remove(listener);
    }

    private void notifyListeners() {
        for (Consumer<Profile> listener : changeListeners) {
            try {
                listener.accept(currentProfile);
            } catch (Exception e) {
                logger.error("Error notifying profile listener", e);
            }
        }
    }

    public List<Profile> getAllProfiles() {
        try {
            return profileRepository.findAll();
        } catch (Exception e) {
            logger.error("Error getting all profiles", e);
            return new java.util.ArrayList<>();
        }
    }

    public List<Profile> getLoggedInProfiles() {
        try {
            return profileRepository.findLoggedInProfiles();
        } catch (Exception e) {
            logger.error("Error getting logged in profiles", e);
            return new java.util.ArrayList<>();
        }
    }

    public List<Profile> getAllNonGuestProfiles() {
        try {
            return getAllProfiles().stream()
                    .filter(p -> !p.isGuest())
                    .collect(java.util.stream.Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting non-guest profiles", e);
            return new java.util.ArrayList<>();
        }
    }

    public void deleteProfile(int id) {
        try {
            profileRepository.delete(id);
            logger.info("Profile deleted: {}", id);

            if (currentProfile != null && currentProfile.getId() == id) {
                loadDefaultProfile();
            }
        } catch (Exception e) {
            logger.error("Error deleting profile", e);
        }
    }

    public void signOutCurrentProfile() {
        try {
            if (currentProfile != null && !currentProfile.isGuest() && currentProfile.getId() > 0) {
                currentProfile.setLoggedIn(false);
                profileRepository.update(currentProfile);
                logger.info("Profile {} logged out", currentProfile.getUsername());
            }

            Profile guest = new Profile();
            guest.setUsername("Guest");
            guest.setEmail("guest@nexus.local");
            guest.setGuest(true);
            guest.setLoggedIn(true);
            this.currentProfile = guest;
            notifyListeners();
            logger.info("Signed out to Guest profile");
        } catch (Exception e) {
            logger.error("Error signing out", e);
        }
    }

    public void switchToGuestProfile() {
        signOutCurrentProfile();
    }

    public Profile addAccount(String username, String email) {
        try {
            Profile p = new Profile();
            p.setUsername(username);
            p.setEmail(email);
            profileRepository.save(p);
            logger.info("Account added: {} with ID: {}", username, p.getId());
            return p;
        } catch (Exception e) {
            logger.error("Error adding account", e);
            return null;
        }
    }

    public boolean switchToAccountByName(String username) {
        try {
            var all = getAllProfiles();
            Optional<Profile> match = all.stream().filter(p -> username != null && username.equals(p.getUsername())).findFirst();
            if (match.isPresent()) {
                this.currentProfile = match.get();
                saveSessionProfile();
                notifyListeners();
                logger.info("Switched to account: {}", username);
                return true;
            }
        } catch (Exception e) {
            logger.error("Error switching account", e);
        }
        return false;
    }

    public boolean removeAccountByName(String username) {
        try {
            var all = getAllProfiles();
            Optional<Profile> match = all.stream().filter(p -> username != null && username.equals(p.getUsername())).findFirst();
            if (match.isPresent()) {
                Profile profile = match.get();
                profile.setLoggedIn(false);
                profileRepository.update(profile);
                logger.info("Logged out account: {}", username);

                if (currentProfile != null && username.equals(currentProfile.getUsername())) {
                    switchToGuestProfile();
                }
                return true;
            }
        } catch (Exception e) {
            logger.error("Error removing account", e);
        }
        return false;
    }

    public Profile registerUser(String username, String email, String password) {
        try {

            if (profileRepository.emailExists(email)) {
                logger.warn("Registration failed: Email already exists: {}", email);
                return null;
            }

            Profile profile = new Profile();
            profile.setUsername(username);
            profile.setEmail(email);
            profile.setPasswordHash(PasswordUtil.hashPassword(password));
            profile.setGuest(false);
            profile.setLoggedIn(true);

            profileRepository.save(profile);

            createDefaultSettingsForProfile(profile.getId());

            logger.info("User registered successfully: {}", username);
            return profile;
        } catch (Exception e) {
            logger.error("Error registering user", e);
            return null;
        }
    }

    public Profile authenticateUser(String email, String password) {
        try {
            Profile profile = profileRepository.findByEmail(email);

            if (profile == null) {
                logger.warn("Authentication failed: Profile not found for email: {}", email);
                return null;
            }

            if (profile.isGuest()) {
                logger.warn("Authentication failed: Cannot authenticate guest profile");
                return null;
            }

            if (profile.getPasswordHash() == null) {
                logger.warn("Authentication failed: No password set for profile: {}", email);
                return null;
            }

            if (PasswordUtil.verifyPassword(password, profile.getPasswordHash())) {
                profile.setLoggedIn(true);
                profileRepository.update(profile);
                logger.info("User authenticated successfully: {}", email);
                return profile;
            } else {
                logger.warn("Authentication failed: Invalid password for: {}", email);
                return null;
            }
        } catch (Exception e) {
            logger.error("Error authenticating user", e);
            return null;
        }
    }

    public void switchProfile(Profile profile) {
        if (profile != null) {
            currentProfile = profile;
            saveSessionProfile();
            notifyListeners();
            logger.info("Switched to profile: {}", profile.getUsername());
        }
    }

    public Profile getGuestProfile() {
        try {

            Profile guest = profileRepository.findById(1);
            if (guest == null || !guest.isGuest()) {

                guest = new Profile();
                guest.setUsername("Guest");
                guest.setGuest(true);
                guest.setEmail(null);
                guest.setPasswordHash(null);
                profileRepository.save(guest);
                createDefaultSettingsForProfile(guest.getId());
            }
            return guest;
        } catch (Exception e) {
            logger.error("Error getting guest profile", e);
            return null;
        }
    }

    private void createDefaultSettingsForProfile(int profileId) {
        try {

            logger.debug("Default settings will be created for profile: {}", profileId);
        } catch (Exception e) {
            logger.warn("Could not create default settings for profile", e);
        }
    }
}
