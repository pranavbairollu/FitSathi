package com.example.fitsathi.managers;

import androidx.annotation.NonNull;

import com.example.fitsathi.models.Squad;
import com.example.fitsathi.models.SquadMemberStat;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public class SquadManager {

    private static final String SQUADS_KEY = "squads";
    private static final String USER_SQUADS_KEY = "user_squads";
    private static final String SQUAD_STATS_KEY = "squad_stats";
    private static final String INVITE_CODES_KEY = "invite_codes";

    public interface SquadCallback<T> {
        void onComplete(T result, String error);
    }

    private static DatabaseReference getBaseRef() {
        return FirebaseDatabase.getInstance().getReference();
    }

    private static String getCurrentUserId() {
        return FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
    }

    /**
     * Creates a new squad with unique invite code verification.
     */
    public static void createSquad(String name, SquadCallback<Squad> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onComplete(null, "User not authenticated");
            return;
        }

        if (name == null || name.trim().length() < 3) {
            callback.onComplete(null, "Squad name must be at least 3 characters");
            return;
        }

        final String inviteCode = generateInviteCode();

        // CHECK FOR UNIQUENESS
        getBaseRef().child(INVITE_CODES_KEY).child(inviteCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Collision! Try again recursively
                    createSquad(name, callback);
                } else {
                    // Unique code found, proceed with creation
                    finalizeSquadCreation(name, inviteCode, uid, callback);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(null, error.getMessage());
            }
        });
    }

    private static void finalizeSquadCreation(String name, String inviteCode, String uid,
            SquadCallback<Squad> callback) {
        String squadId = getBaseRef().child(SQUADS_KEY).push().getKey();
        if (squadId == null) {
            callback.onComplete(null, "Database error");
            return;
        }

        Squad squad = new Squad(squadId, name, inviteCode, uid);
        squad.getMembers().put(uid, true);

        Map<String, Object> updates = new HashMap<>();
        updates.put("/" + SQUADS_KEY + "/" + squadId, squad);
        updates.put("/" + USER_SQUADS_KEY + "/" + uid + "/" + squadId, true);
        updates.put("/" + INVITE_CODES_KEY + "/" + inviteCode, squadId);

        getBaseRef().updateChildren(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                callback.onComplete(squad, null);
            } else {
                callback.onComplete(null, task.getException().getMessage());
            }
        });
    }

    /**
     * Joins a squad using an invite code (Case-Insensitive).
     */
    public static void joinSquad(String inviteCode, SquadCallback<Squad> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onComplete(null, "User not authenticated");
            return;
        }

        if (inviteCode == null || inviteCode.trim().length() != 6) {
            callback.onComplete(null, "Invite code must be 6 characters");
            return;
        }

        String normalizedCode = inviteCode.trim().toUpperCase();

        getBaseRef().child(INVITE_CODES_KEY).child(normalizedCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String squadId = snapshot.getValue(String.class);
                        if (squadId == null) {
                            callback.onComplete(null, "Invalid invite code");
                            return;
                        }

                        // Check if already in squad to prevent redundant writes
                        getBaseRef().child(USER_SQUADS_KEY).child(uid).child(squadId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                                        if (snapshot.exists()) {
                                            callback.onComplete(null, "You are already a member of this squad");
                                            return;
                                        }

                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("/" + SQUADS_KEY + "/" + squadId + "/members/" + uid, true);
                                        updates.put("/" + USER_SQUADS_KEY + "/" + uid + "/" + squadId, true);

                                        getBaseRef().updateChildren(updates).addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                getSquadDetails(squadId, callback);
                                            } else {
                                                callback.onComplete(null, task.getException().getMessage());
                                            }
                                        });
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        callback.onComplete(null, error.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(null, error.getMessage());
                    }
                });
    }

    /**
     * Removes the current user from a squad.
     */
    public static void leaveSquad(String squadId, String inviteCode, SquadCallback<Boolean> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onComplete(false, "User not authenticated");
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("/" + SQUADS_KEY + "/" + squadId + "/members/" + uid, null);
        updates.put("/" + USER_SQUADS_KEY + "/" + uid + "/" + squadId, null);
        // We don't delete the stats log for the user to maintain historical leaderboard
        // integrity for others,
        // but the user will no longer be part of the squad.

        getBaseRef().updateChildren(updates).addOnCompleteListener(task -> {
            callback.onComplete(task.isSuccessful(), task.isSuccessful() ? null : "Failed to leave squad");
        });
    }

    /**
     * Fetches details of a specific squad.
     */
    public static void getSquadDetails(String squadId, SquadCallback<Squad> callback) {
        if (squadId == null) {
            callback.onComplete(null, "Invalid Squad ID");
            return;
        }
        getBaseRef().child(SQUADS_KEY).child(squadId).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onComplete(null, "Squad no longer exists");
                    return;
                }
                Squad squad = snapshot.getValue(Squad.class);
                callback.onComplete(squad, squad == null ? "Error parsing squad data" : null);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onComplete(null, error.getMessage());
            }
        });
    }

    /**
     * Fetches all squads the current user is a member of.
     */
    public static void getUserSquads(SquadCallback<List<Squad>> callback) {
        String uid = getCurrentUserId();
        if (uid == null) {
            callback.onComplete(null, "User not authenticated");
            return;
        }

        final java.util.concurrent.atomic.AtomicBoolean isFinished = new java.util.concurrent.atomic.AtomicBoolean(false);

        // Timeout fallback
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinished.get()) {
                isFinished.set(true);
                android.util.Log.w("SquadManager", "Squad loading timed out.");
                callback.onComplete(new ArrayList<>(), "Loading timed out. Check your connection.");
            }
        }, 12000);

        getBaseRef().child(USER_SQUADS_KEY).child(uid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (isFinished.get()) return;

                final List<String> squadIds = new ArrayList<>();
                for (DataSnapshot ds : snapshot.getChildren()) {
                    if (ds.getKey() != null) {
                        squadIds.add(ds.getKey());
                    }
                }

                if (squadIds.isEmpty()) {
                    if (!isFinished.get()) {
                        isFinished.set(true);
                        callback.onComplete(new ArrayList<>(), null);
                    }
                    return;
                }

                final List<Squad> squads = new ArrayList<>();
                final java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
                final int total = squadIds.size();

                for (String id : squadIds) {
                    getSquadDetails(id, (squad, error) -> {
                        if (squad != null) {
                            squads.add(squad);
                        }
                        if (count.incrementAndGet() >= total) {
                            if (!isFinished.get()) {
                                isFinished.set(true);
                                callback.onComplete(squads, null);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (!isFinished.get()) {
                    isFinished.set(true);
                    callback.onComplete(null, error.getMessage());
                }
            }
        });
    }

    /**
     * Fetches leaderboard stats for a squad for the current week.
     */
    public static void getSquadLeaderboard(String squadId, SquadCallback<List<SquadMemberStat>> callback) {
        String weekId = getCurrentWeekId();
        getBaseRef().child(SQUAD_STATS_KEY).child(squadId).child(weekId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<SquadMemberStat> stats = new ArrayList<>();
                        for (DataSnapshot ds : snapshot.getChildren()) {
                            SquadMemberStat stat = ds.getValue(SquadMemberStat.class);
                            if (stat != null) {
                                stat.setUid(ds.getKey());
                                stats.add(stat);
                            }
                        }
                        callback.onComplete(stats, null);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onComplete(null, error.getMessage());
                    }
                });
    }

    /**
     * Generates a unique 6-character invite code (Uppercase).
     */
    private static String generateInviteCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random rnd = new Random();
        while (code.length() < 6) {
            int index = (int) (rnd.nextFloat() * chars.length());
            code.append(chars.charAt(index));
        }
        return code.toString();
    }

    /**
     * Returns a week identifier (e.g., "2024-W18") based on UTC Time.
     * This ensures all users globally are synced to the same competition week.
     */
    public static String getCurrentWeekId() {
        java.util.TimeZone utc = java.util.TimeZone.getTimeZone("UTC");
        Calendar cal = Calendar.getInstance(utc);
        cal.setMinimalDaysInFirstWeek(4); // ISO-8601 standard
        int year = cal.get(Calendar.YEAR);
        int week = cal.get(Calendar.WEEK_OF_YEAR);

        // Handle edge case where first week of year belongs to previous year in ISO
        if (week >= 52 && cal.get(Calendar.MONTH) == Calendar.JANUARY) {
            year--;
        } else if (week == 1 && cal.get(Calendar.MONTH) == Calendar.DECEMBER) {
            year++;
        }

        return String.format(Locale.US, "%d-W%02d", year, week);
    }
}
