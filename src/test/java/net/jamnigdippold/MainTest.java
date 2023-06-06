package net.jamnigdippold;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.security.Permission;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MainTest {
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream defaultOut = System.out;
    private final PrintStream defaultErr = System.err;

    private MockedStatic<WebsiteCrawler> mockedCrawler;
    private MockedStatic<Main> mockedMain;
    @Mock
    private JFrame mockedFrame;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
    }

    @AfterEach
    public void tearDown() {
        resetSystemValues();
        closeMocks();
        resetMainField();
    }

    private void resetSystemValues() {
        System.setOut(defaultOut);
        System.setErr(defaultErr);
        System.setSecurityManager(null);
    }

    private void resetMainField() {
        Main.websiteUrls = null;
        Main.depthOfRecursiveSearch = -1;
        Main.depthsOfRecursiveSearch = null;
        Main.languageCodes = null;
        Main.languageCode = null;
        Main.outputPath = null;
        Main.urlInputAmount = -1;
    }

    private void closeMocks() {
        if (mockedCrawler != null) {
            mockedCrawler.close();
            mockedCrawler = null;
        }
        if (mockedMain != null) {
            mockedMain.close();
            mockedMain = null;
        }
        if (mockedFrame != null) {
            mockedFrame = null;
        }
    }

    @Test
    void testMain() {
        mockedMain = Mockito.mockStatic(Main.class);
        mockedMain.when(() -> Main.main(new String[0])).thenCallRealMethod();
        mockedMain.when(Main::createThreadOrganizer).thenCallRealMethod();
        MockedConstruction<ThreadOrganizer> mockedConstruction = mockConstruction(ThreadOrganizer.class);
        mockSystemExit();

        assertThrows(RuntimeException.class, () -> Main.main(new String[0]), "SecurityException: Tried to exit with status 0");

        verify(mockedConstruction.constructed().get(0)).startConcurrentCrawling();
        mockedMain.verify(Main::getUserInput);

        mockedConstruction.close();
    }

    @Test
    void testGetAmountOfCrawlingWebsites() {
        mockInputScanner("1\n");

        Main.getAmountOfCrawlingWebsites();

        assertEquals("Enter the amount of website urls that should be crawled" + System.getProperty("line.separator"), outContent.toString());
        assertEquals("", errContent.toString());
        assertEquals(1, Main.urlInputAmount);
    }

    @Test
    void testGetAmountOfCrawlingWebsitesInputMismatch() {
        mockInputScanner("Number\n-1\n1\n");

        Main.getAmountOfCrawlingWebsites();

        assertEquals("Enter the amount of website urls that should be crawled" + System.getProperty("line.separator"), outContent.toString());
        assertEquals("ERROR: Please enter a valid number." + System.getProperty("line.separator") + "ERROR: Please enter a valid url amount greater than zero!" + System.getProperty("line.separator"), errContent.toString());
        assertEquals(1, Main.urlInputAmount);
    }

    @Test
    void testGetUserInput() {
        mockJFileChooser(0, "TestFile");
        mockSetupScanner("1\nhttps://example.com\n3\nen\n");
        mockIsBrokenLink();

        Main.getUserInput();

        assertEquals(1, Main.urlInputAmount);
        assertArrayEquals(new String[]{"https://example.com"}, Main.websiteUrls);
        assertArrayEquals(new int[]{3}, Main.depthsOfRecursiveSearch);
        assertArrayEquals(new String[]{"en"}, Main.languageCodes);
        assertEquals("TestFile.md", Main.outputPath);

        assertThrows(IllegalStateException.class, () -> Main.inputScanner.next());
    }

    @Test
    void testGetUserMultipleUrlInputs() {
        mockJFileChooser(0, "TestFile");
        mockSetupScanner("2\nhttps://example.com\nhttps://example.com\n2\n3\nde\nen\n");
        mockIsBrokenLink();

        Main.getUserInput();

        assertEquals(2, Main.urlInputAmount);
        assertArrayEquals(new String[]{"https://example.com", "https://example.com"}, Main.websiteUrls);
        assertArrayEquals(new int[]{2, 3}, Main.depthsOfRecursiveSearch);
        assertArrayEquals(new String[]{"de", "en"}, Main.languageCodes);
        assertEquals("TestFile.md", Main.outputPath);

        assertThrows(IllegalStateException.class, () -> Main.inputScanner.next());
    }

    @Test
    void testCreateFileChooserParent() {
        setupMockedMain();
        mockedFrame = mock(JFrame.class);
        mockedMain.when(Main::createJFrame).thenReturn(mockedFrame);

        JFrame test = Main.createFileChooserParent();

        assertEquals(test, mockedFrame);

        verify(mockedFrame).setLocationRelativeTo(null);
        verify(mockedFrame).setVisible(true);
        verify(mockedFrame).setExtendedState(JFrame.ICONIFIED);
        verify(mockedFrame).setExtendedState(JFrame.NORMAL);
    }

    @Test
    void testCreateJFrame() {
        assertNotNull(Main.createJFrame());
    }

    @Test
    void testFileChooserFilter() {
        String expectedFilterDescription = "Markdown File (.md)";

        Main.createFileChooser();
        FileFilter filter = Main.fileChooser.getFileFilter();

        assertNotNull(Main.fileChooser);
        assertEquals(expectedFilterDescription, filter.getDescription());
    }

    @Test
    void testRunFileChooser() {
        mockJFileChooser(0, "Test.md");

        Main.createFileChooser();
        Main.runFileChooser();

        assertEquals(0, Main.fileChooserStatus);
        verify(mockedFrame).setVisible(false);
    }

    @Test
    void testGetOutputFileInput() {
        mockJFileChooser(0, "Test.txt");

        Main.getOutputFileInput();

        assertEquals("Test.txt.md", Main.outputPath);
        assertEquals("Choose a location for the output File" + System.getProperty("line.separator"), outContent.toString());
    }

    @Test
    void testGetWrongInputFromFileChooser() {
        mockJFileChooser(JFileChooser.ERROR_OPTION, "Test");
        mockSystemExit();

        assertThrows(RuntimeException.class, Main::getOutputFileInput, "SecurityException: Tried to exit with status -1");
        assertEquals("ERROR: Unexpected error. Stopping program." + System.getProperty("line.separator"), errContent.toString());
    }

    @Test
    void testAbortFileChooser() {
        mockJFileChooser(JFileChooser.CANCEL_OPTION, "Test");
        mockSystemExit();

        assertThrows(RuntimeException.class, Main::getOutputFileInput, "SecurityException: Tried to exit with status 0");
        assertEquals("ERROR: File choosing aborted. Stopping program." + System.getProperty("line.separator"), errContent.toString());
    }

    private void mockSystemExit() {
        System.setSecurityManager(new SecurityManager() {
            @Override
            public void checkPermission(Permission perm) {
            }

            @Override
            public void checkExit(int status) {
                super.checkExit(status);
                throw new RuntimeException("SecurityException: Tried to exit with status " + status);
            }
        });
    }

    private void mockJFileChooser(int returnCode, String chosenPath) {
        setupMockedMain();
        mockedFrame = mock(JFrame.class);
        mockedMain.when(Main::createJFrame).thenReturn(mockedFrame);
        mockedMain.when(Main::createFileChooser).then(invocationOnMock -> {
            Main.fileChooser = mock(JFileChooser.class);
            when(Main.fileChooser.showSaveDialog(any())).thenReturn(returnCode);
            when(Main.fileChooser.getSelectedFile()).thenReturn(new File(chosenPath));
            return null;
        });
    }

    @Test
    void testAddFileExtension() {
        Main.outputPath = "E:\\RealFolder\\output";

        Main.addFileExtension();

        assertEquals("E:\\RealFolder\\output.md", Main.outputPath);

    }

    @Test
    void testUnnecessaryAddFileExtension() {
        Main.outputPath = "E:\\RealFolder\\output.md";

        Main.addFileExtension();

        assertEquals("E:\\RealFolder\\output.md", Main.outputPath);
    }

    @Test
    void testGetLanguageInput() {
        Main.urlInputAmount = 1;
        mockInputScanner("en");

        Main.getMultipleLanguageInputs();

        assertTestGetLanguage();
    }

    private void assertTestGetLanguage() {
        assertEquals("Please enter the language code for the language into which the headers should be translated [e.g. de] 1/1" + System.getProperty("line.separator"), outContent.toString());
        assertEquals("", errContent.toString());
        assertEquals("en", Main.languageCode);
    }

    @Test
    void testGetLanguageInputError() {
        Main.urlInputAmount = 1;
        mockInputScanner("Not a language code\nef\nen");

        Main.getMultipleLanguageInputs();

        assertTestGetLanguageError();
    }

    private void assertTestGetLanguageError() {
        assertEquals("Please enter the language code for the language into which the headers should be translated [e.g. de] 1/1" + System.getProperty("line.separator"), outContent.toString());
        assertEquals("ERROR: Please enter a valid language code." + System.getProperty("line.separator") + "ERROR: Please enter a valid language code." + System.getProperty("line.separator"), errContent.toString());
        assertEquals("en", Main.languageCode);
    }

    @Test
    void testGetDepthInput() {
        Main.urlInputAmount = 1;
        mockInputScanner("3\n");

        Main.getMultipleCrawlingDepthInputs();

        assertTestGetDepthInput();
    }

    private void assertTestGetDepthInput() {
        assertEquals("Enter the depth of search (how many additional Links should be analyzed) 1/1" + System.getProperty("line.separator"), outContent.toString());
        assertEquals("", errContent.toString());
        assertEquals(3, Main.depthOfRecursiveSearch);
    }

    @Test
    void testGetDepthInputError() {
        Main.urlInputAmount = 1;
        mockInputScanner("Number\n-1\n-2\n-10\n0\n");

        Main.getMultipleCrawlingDepthInputs();

        assertTestGetDepthInputError();
    }

    private void assertTestGetDepthInputError() {
        assertEquals("Enter the depth of search (how many additional Links should be analyzed) 1/1" + System.getProperty("line.separator"), outContent.toString());
        assertEquals("ERROR: Please enter a valid number." + System.getProperty("line.separator")
                + "ERROR: Please enter a positive number." + System.getProperty("line.separator")
                + "ERROR: Please enter a positive number." + System.getProperty("line.separator")
                + "ERROR: Please enter a positive number." + System.getProperty("line.separator"), errContent.toString());
        assertEquals(0, Main.depthOfRecursiveSearch);
    }

    @Test
    void testGetWebsiteInput() {
        setUpTestGetWebsiteInput("https://example.com\n");

        Main.urlInputAmount = 1;
        Main.getMultipleWebsiteUrlInputs();

        assertTestGetWebsiteInput();
    }

    private void assertTestGetWebsiteInput() {
        assertEquals("Enter the website URL that should be crawled 1/1" + System.getProperty("line.separator"), outContent.toString());
        assertEquals("", errContent.toString());
        assertArrayEquals(new String[]{"https://example.com"}, Main.websiteUrls);
        mockedCrawler.verify(() -> WebsiteCrawler.isBrokenLink("https://example.com"), times(2));
    }

    @Test
    void testGetWebsiteInputError() {
        setUpTestGetWebsiteInput("wrong URL format\nhttps://www.notARealWebsite.com\nhttps://example.com\n");

        Main.urlInputAmount = 1;
        Main.getMultipleWebsiteUrlInputs();

        assertTestGetWebsiteInputError();
    }

    private void assertTestGetWebsiteInputError() {
        assertEquals("Enter the website URL that should be crawled 1/1" + System.getProperty("line.separator"), outContent.toString());
        assertEquals("ERROR: Cannot connect to URL, please enter a valid URL" + System.getProperty("line.separator") + "ERROR: Cannot connect to URL, please enter a valid URL" + System.getProperty("line.separator"), errContent.toString());
        assertArrayEquals(new String[]{"https://example.com"}, Main.websiteUrls);
    }

    private void setUpTestGetWebsiteInput(String testInput) {
        mockIsBrokenLink();
        mockInputScanner(testInput);
    }

    private void mockInputScanner(String testInput) {
        Main.inputScanner = new Scanner(new ByteArrayInputStream(testInput.getBytes()));
    }

    private void setupMockedMain() {
        if (mockedMain == null)
            mockedMain = mockStatic(Main.class, CALLS_REAL_METHODS);
    }

    private void mockSetupScanner(String testInput) {
        setupMockedMain();
        mockedMain.when(Main::setupScanner).then(invocationOnMock -> {
            mockInputScanner(testInput);
            return null;
        });
    }

    private void mockIsBrokenLink() {
        mockedCrawler = mockStatic(WebsiteCrawler.class);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink(anyString())).thenReturn(true);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink("https://example.com")).thenReturn(false);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink("wrong URL format")).thenReturn(true);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink("https://www.notARealWebsite.com")).thenReturn(true);
    }
}