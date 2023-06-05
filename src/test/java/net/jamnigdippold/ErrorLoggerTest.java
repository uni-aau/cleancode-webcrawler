package net.jamnigdippold;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ErrorLoggerTest {
    private final Logger logger = ErrorLogger.getInstance();
    String errorMessageMock = "Error message mock";

    @BeforeEach
    public void setUp() {

    }

    @AfterEach
    public void teardown() {
        logger.clearLog();
    }

    @Test
    void testLogError() {
        logger.logError(errorMessageMock);

        assertEquals(errorMessageMock, logger.getErrorLog().get(0));
    }

    @Test
    void testGetErrorLogAsString() {
        String expectedErrorLogMessage = "# <br> ------- ERRORS ------- <br>\n- Error message mock<br>\n";
        logger.logError(errorMessageMock);

        assertEquals(expectedErrorLogMessage, logger.getErrorLogAsString());
    }

    @Test
    void testGetErrorLog() {
        ArrayList<String> expectedArrayList = new ArrayList<>();
        expectedArrayList.add(errorMessageMock);

        logger.logError(errorMessageMock);

        assertEquals(expectedArrayList, logger.getErrorLog());
    }

    @Test
    void testClearErrorLog() {
        ArrayList<String> expectedArrayList = new ArrayList<>();

        logger.logError(errorMessageMock);
        logger.clearLog();

        assertEquals(expectedArrayList, logger.getErrorLog());
    }
}
