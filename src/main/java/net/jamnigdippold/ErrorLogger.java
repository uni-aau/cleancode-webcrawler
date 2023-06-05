package net.jamnigdippold;

import java.util.ArrayList;
import java.util.List;

public class ErrorLogger implements Logger {
    private List<String> loggedErrors = new ArrayList<>();

    @Override
    public void logError(String errorMessage) {
        loggedErrors.add(errorMessage);
    }

    @Override
    public ArrayList<String> getLoggedErrors() {
        return null;
    }
}
