package net.jamnigdippold;

import java.util.ArrayList;

public class ErrorLogger implements Logger {
    private static ErrorLogger logInstance;
    private ArrayList<String> loggedErrors = new ArrayList<>();

    private ErrorLogger() {
    }

    public static ErrorLogger getInstance() {
        if (logInstance == null) {
            synchronized (ErrorLogger.class) {
                logInstance = new ErrorLogger();
            }
        }
        return logInstance;
    }


    @Override
    public void logError(String errorMessage) {
        loggedErrors.add(errorMessage);
    }

    @Override
    public String getErrorLogAsString() {
        String initialErrorHeadline = "# <br> ------- ERRORS ------- <br>\n";

        synchronized (loggedErrors) {
            StringBuilder errorLog = new StringBuilder();
            errorLog.append(initialErrorHeadline);

            if (loggedErrors.isEmpty()) {
                errorLog.append("- No errors thrown while executing program <br>\n");
            } else {
                for (String logEntry : loggedErrors) {
                    errorLog.append("- ").append(logEntry).append("<br>\n");
                }
            }
            return errorLog.toString();
        }
    }

    @Override
    public ArrayList<String> getErrorLog() {
        return loggedErrors;
    }
}
