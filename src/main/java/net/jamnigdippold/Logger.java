package net.jamnigdippold;

import java.util.ArrayList;

public interface Logger {
    void logError(String errorMessage);

    String getErrorLogAsString();

    ArrayList<String> getErrorLog();
}
