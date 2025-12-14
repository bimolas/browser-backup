package com.example.nexus.controller;

import com.example.nexus.model.Profile;
import com.example.nexus.service.ProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Controller for user profile management.
 * Handles profile switching, creation, and customization.
 */
public class ProfileController {
    private static final Logger logger = LoggerFactory.getLogger(ProfileController.class);

    private final ProfileService profileService;
    private Consumer<Profile> onProfileChanged;
    private Profile currentProfile;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
        // Load default profile
        List<Profile> profiles = profileService.getAllProfiles();
        if (!profiles.isEmpty()) {
            this.currentProfile = profiles.get(0);
        }
    }

    /**
     * Set callback for profile changes
     */
    public void setOnProfileChanged(Consumer<Profile> callback) {
        this.onProfileChanged = callback;
    }

    /**
     * Get the current active profile
     */
    public Profile getCurrentProfile() {
        return currentProfile;
    }

    /**
     * Get all profiles
     */
    public List<Profile> getAllProfiles() {
        return profileService.getAllProfiles();
    }

    /**
     * Create a new profile
     */
    public Profile createProfile(String name) {
        try {
            Profile profile = new Profile();
            profile.setName(name);
            profileService.saveProfile(profile);
            logger.info("Created profile: {}", name);
            return profile;
        } catch (Exception e) {
            logger.error("Error creating profile: {}", name, e);
            return null;
        }
    }

    /**
     * Switch to a different profile
     */
    public void switchProfile(Profile profile) {
        try {
            this.currentProfile = profile;
            logger.info("Switched to profile: {}", profile.getName());

            if (onProfileChanged != null) {
                onProfileChanged.accept(profile);
            }
        } catch (Exception e) {
            logger.error("Error switching profile", e);
        }
    }

    /**
     * Delete a profile
     */
    public void deleteProfile(int profileId) {
        try {
            profileService.deleteProfile(profileId);
            logger.info("Deleted profile: {}", profileId);
        } catch (Exception e) {
            logger.error("Error deleting profile: {}", profileId, e);
        }
    }

    /**
     * Update profile name
     */
    public void updateProfileName(Profile profile, String newName) {
        try {
            profile.setName(newName);
            profileService.updateProfile(profile);
            logger.info("Updated profile name to: {}", newName);
        } catch (Exception e) {
            logger.error("Error updating profile name", e);
        }
    }
}
