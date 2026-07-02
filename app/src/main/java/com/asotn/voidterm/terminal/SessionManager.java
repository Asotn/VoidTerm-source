/*
 * VoidTerm - SessionManager
 * Manages the lifecycle of multiple terminal sessions.
 * Supports creating, switching, and destroying sessions.
 *
 * Developer : Asotn | https://github.com/Asotn | s.pi@outlook.sa
 * License   : GPL-3.0
 */

package com.asotn.voidterm.terminal;

import android.content.Context;
import android.util.Log;
import android.util.SparseArray;

import java.util.ArrayList;
import java.util.List;

/**
 * SessionManager is a singleton that tracks all active TerminalSessions.
 * The UI layer calls this to create new sessions and get output callbacks
 * for whichever session is currently active.
 */
public class SessionManager {

    private static final String TAG = "VoidTerm-SessionMgr";
    private static final int    MAX_SESSIONS = 8;

    private final SparseArray<TerminalSession> sessions = new SparseArray<>();
    private int  nextSessionId  = 1;
    private int  activeSession  = -1;
    private final Context ctx;

    // Listener for session lifecycle events
    public interface SessionLifecycleListener {
        void onSessionCreated(int sessionId, String title);
        void onSessionDestroyed(int sessionId);
        void onSessionSwitched(int sessionId);
        void onSessionOutput(int sessionId, String output);
    }

    private final List<SessionLifecycleListener> listeners = new ArrayList<>();

    // Singleton
    private static SessionManager instance;
    public static SessionManager getInstance(Context ctx) {
        if (instance == null) instance = new SessionManager(ctx.getApplicationContext());
        return instance;
    }

    private SessionManager(Context context) {
        this.ctx = context;
    }

    // -------------------------------------------------------------------------
    // createSession
    // -------------------------------------------------------------------------
    public int createSession(String title) {
        if (sessions.size() >= MAX_SESSIONS) {
            Log.w(TAG, "Max sessions reached");
            return -1;
        }

        int id = nextSessionId++;
        TerminalSession session = new TerminalSession(ctx);

        final int capturedId = id;
        session.setOutputCallback(output -> {
            notifyOutput(capturedId, output);
        });

        session.start();
        sessions.put(id, session);

        Log.i(TAG, "Session created: id=" + id + " title=" + title);
        notifyCreated(id, title != null ? title : "Session " + id);

        if (activeSession < 0) {
            setActiveSession(id);
        }

        return id;
    }

    // -------------------------------------------------------------------------
    // destroySession
    // -------------------------------------------------------------------------
    public void destroySession(int sessionId) {
        TerminalSession s = sessions.get(sessionId);
        if (s == null) return;

        s.stop();
        sessions.remove(sessionId);
        notifyDestroyed(sessionId);

        // Switch to another session if destroyed was active
        if (activeSession == sessionId) {
            if (sessions.size() > 0) {
                setActiveSession(sessions.keyAt(0));
            } else {
                activeSession = -1;
            }
        }

        Log.i(TAG, "Session destroyed: id=" + sessionId);
    }

    // -------------------------------------------------------------------------
    // setActiveSession
    // -------------------------------------------------------------------------
    public void setActiveSession(int sessionId) {
        if (sessions.get(sessionId) == null) return;
        activeSession = sessionId;
        notifySwitched(sessionId);
        Log.i(TAG, "Active session: " + sessionId);
    }

    // -------------------------------------------------------------------------
    // getActiveSession
    // -------------------------------------------------------------------------
    public TerminalSession getActiveSession() {
        if (activeSession < 0) return null;
        return sessions.get(activeSession);
    }

    // -------------------------------------------------------------------------
    // getSession
    // -------------------------------------------------------------------------
    public TerminalSession getSession(int sessionId) {
        return sessions.get(sessionId);
    }

    // -------------------------------------------------------------------------
    // getActiveSessionId
    // -------------------------------------------------------------------------
    public int getActiveSessionId() {
        return activeSession;
    }

    // -------------------------------------------------------------------------
    // getAllSessionIds
    // -------------------------------------------------------------------------
    public List<Integer> getAllSessionIds() {
        List<Integer> ids = new ArrayList<>();
        for (int i = 0; i < sessions.size(); i++) {
            ids.add(sessions.keyAt(i));
        }
        return ids;
    }

    // -------------------------------------------------------------------------
    // getSessionCount
    // -------------------------------------------------------------------------
    public int getSessionCount() {
        return sessions.size();
    }

    // -------------------------------------------------------------------------
    // sendInputToActive
    // -------------------------------------------------------------------------
    public void sendInputToActive(String input) {
        TerminalSession s = getActiveSession();
        if (s != null && s.isRunning()) {
            s.sendInput(input);
        }
    }

    // -------------------------------------------------------------------------
    // sendSignalToActive
    // -------------------------------------------------------------------------
    public void sendSignalToActive() {
        TerminalSession s = getActiveSession();
        if (s != null) s.sendSignalInterrupt();
    }

    // -------------------------------------------------------------------------
    // destroyAll
    // Call from Application.onTerminate or TerminalActivity.onDestroy
    // -------------------------------------------------------------------------
    public void destroyAll() {
        for (int i = 0; i < sessions.size(); i++) {
            sessions.valueAt(i).stop();
        }
        sessions.clear();
        activeSession = -1;
        Log.i(TAG, "All sessions destroyed");
    }

    // -------------------------------------------------------------------------
    // Listener management
    // -------------------------------------------------------------------------
    public void addListener(SessionLifecycleListener l) {
        if (!listeners.contains(l)) listeners.add(l);
    }

    public void removeListener(SessionLifecycleListener l) {
        listeners.remove(l);
    }

    private void notifyCreated(int id, String title) {
        for (SessionLifecycleListener l : listeners) l.onSessionCreated(id, title);
    }
    private void notifyDestroyed(int id) {
        for (SessionLifecycleListener l : listeners) l.onSessionDestroyed(id);
    }
    private void notifySwitched(int id) {
        for (SessionLifecycleListener l : listeners) l.onSessionSwitched(id);
    }
    private void notifyOutput(int id, String output) {
        for (SessionLifecycleListener l : listeners) l.onSessionOutput(id, output);
    }
}
