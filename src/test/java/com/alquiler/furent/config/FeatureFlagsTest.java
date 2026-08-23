package com.alquiler.furent.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FeatureFlagsTest {

    @Test
    void defaults_allFlagsEnabled() {
        FeatureFlags flags = new FeatureFlags();
        assertTrue(flags.isCacheEnabled());
        assertTrue(flags.isNotificationsEnabled());
        assertTrue(flags.isReviewsEnabled());
        assertTrue(flags.isExportPdfEnabled());
        assertTrue(flags.isAnalyticsEnabled());
    }

    @Test
    void setCacheEnabled_changesValue() {
        FeatureFlags flags = new FeatureFlags();
        flags.setCacheEnabled(false);
        assertFalse(flags.isCacheEnabled());
    }

    @Test
    void setNotificationsEnabled_changesValue() {
        FeatureFlags flags = new FeatureFlags();
        flags.setNotificationsEnabled(false);
        assertFalse(flags.isNotificationsEnabled());
    }

    @Test
    void setReviewsEnabled_changesValue() {
        FeatureFlags flags = new FeatureFlags();
        flags.setReviewsEnabled(false);
        assertFalse(flags.isReviewsEnabled());
    }

    @Test
    void setExportPdfEnabled_changesValue() {
        FeatureFlags flags = new FeatureFlags();
        flags.setExportPdfEnabled(false);
        assertFalse(flags.isExportPdfEnabled());
    }

    @Test
    void setAnalyticsEnabled_changesValue() {
        FeatureFlags flags = new FeatureFlags();
        flags.setAnalyticsEnabled(false);
        assertFalse(flags.isAnalyticsEnabled());
    }
}
