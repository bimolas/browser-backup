package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Profile;
import com.example.nexus.repository.ProfileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ProfileService {
    private static final Logger logger = LoggerFactory.getLogger(ProfileService.class);

    private final ProfileRepository profileRepository;

    public ProfileService(DIContainer container) {
        this.profileRepository = container.getOrCreate(ProfileRepository.class);
    }

    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public Profile getProfile(int id) {
        return profileRepository.findById(id);
    }

    public void saveProfile(Profile profile) {
        profileRepository.save(profile);
    }

    public void updateProfile(Profile profile) {
        profileRepository.update(profile);
    }

    public void deleteProfile(int id) {
        profileRepository.delete(id);
    }
}