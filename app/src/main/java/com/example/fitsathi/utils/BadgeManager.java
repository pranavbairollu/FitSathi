package com.example.fitsathi.utils;

import com.example.fitsathi.R;

public class BadgeManager {

    /**
     * Returns the badge drawable resource ID based on steps and dynamic goal.
     */
    public static int getBadge(int steps, int goal) {
        if (steps >= goal) {
            return R.drawable.ic_badge_goal; // Special badge for goal achieved
        } else if (steps >= goal * 0.75) {
            return R.drawable.ic_badge_10k; // High progress achievement
        } else if (steps >= goal * 0.5) {
            return R.drawable.ic_badge_5k;  // Mid progress achievement
        } else if (steps >= goal * 0.25) {
            return R.drawable.ic_badge_5k;  // Low progress achievement
        } else if (steps >= 1) {
            return R.drawable.ic_badge_goal; // First steps achievement
        } else {
            return R.drawable.ic_badge_default; // No steps yet
        }
    }

    /**
     * Returns a dynamic label/description for the badge based on progress toward the goal.
     */
    public static String getBadgeLabel(int steps, int goal) {
        if (steps >= goal) {
            return "🎉 Goal Achieved! " + goal + " steps!";
        } else if (steps >= goal * 0.75) {
            return "Almost there! Keep pushing 🚀";
        } else if (steps >= goal * 0.5) {
            return "Halfway to your goal! 👍";
        } else if (steps >= goal * 0.25) {
            return "Good start, keep going! 💪";
        } else if (steps >= 1) {
            return "⭐ First steps taken – On your way!";
        } else {
            return "No Steps Yet – Start Walking!";
        }
    }
}
