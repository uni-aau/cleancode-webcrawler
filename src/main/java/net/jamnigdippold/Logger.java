package net.jamnigdippold;

import java.util.ArrayList;

public interface Logger {
    void logError(String errorMessage);
    ArrayList<String> getLoggedErrors();
}
