package com.example.nexus.service;

import com.example.nexus.core.DIContainer;
import com.example.nexus.model.Tab;
import com.example.nexus.repository.TabRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class TabService {
    private static final Logger logger = LoggerFactory.getLogger(TabService.class);

    private final TabRepository tabRepository;

    public TabService(DIContainer container) {
        this.tabRepository = container.getOrCreate(TabRepository.class);
    }

    public List<Tab> getAllTabs() {
        return tabRepository.findAll();
    }

    public Tab getTab(int id) {
        return tabRepository.findById(id);
    }

    public void saveTab(Tab tab) {
        tabRepository.save(tab);
    }

    public void updateTab(Tab tab) {
        tabRepository.update(tab);
    }

    public void deleteTab(int id) {
        tabRepository.delete(id);
    }

    public List<Tab> getTabsBySessionId(String sessionId) {
        return tabRepository.findBySessionId(sessionId);
    }

    public void deleteTabsBySessionId(String sessionId) {
        List<Tab> tabs = tabRepository.findBySessionId(sessionId);
        for (Tab tab : tabs) {
            tabRepository.delete(tab.getId());
        }
    }

    public void saveSessionTabs(List<Tab> tabs, String sessionId) {

        deleteTabsBySessionId(sessionId);

        for (Tab tab : tabs) {
            tab.setSessionId(sessionId);
            tabRepository.save(tab);
        }
    }

    public List<Tab> getTabsByProfileId(int profileId) {
        return tabRepository.findByProfileId(profileId);
    }

    public void saveProfileTabs(List<Tab> tabs, int profileId) {
        // Always delete existing tabs for this profile first
        // This ensures old sessions are cleared even if tabs list is empty
        tabRepository.deleteByProfileId(profileId);
        logger.info("Cleared old tabs for profile {}", profileId);

        // Save new tabs (if any)
        if (tabs != null && !tabs.isEmpty()) {
            for (Tab tab : tabs) {
                tab.setProfileId(profileId);
                tabRepository.save(tab);
            }
            logger.info("Saved {} tabs for profile {}", tabs.size(), profileId);
        } else {
            logger.info("No tabs to save for profile {}", profileId);
        }
    }

    public void clearProfileTabs(int profileId) {
        tabRepository.deleteByProfileId(profileId);
        logger.info("Cleared all tabs for profile {}", profileId);
    }
}
