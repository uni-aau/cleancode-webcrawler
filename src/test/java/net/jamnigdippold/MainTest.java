package net.jamnigdippold;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.io.*;
import java.security.Permission;
import java.util.Scanner;

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
        System.setOut(defaultOut);
        System.setErr(defaultErr);
        System.setSecurityManager(null);
        closeMocks();
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
    void testGetUserInput() {
        mockJFileChooser(0, "TestFile");
        mockSetupScanner("https://example.com\n3\nen\n");
        mockIsBrokenLink();

        Main.getUserInput();

        Assertions.assertEquals("https://example.com", Main.websiteUrl);
        Assertions.assertEquals(3, Main.depthOfRecursiveSearch);
        Assertions.assertEquals("en", Main.languageCode);
        Assertions.assertEquals("TestFile.md", Main.outputPath);

        Assertions.assertThrows(IllegalStateException.class, () -> Main.inputScanner.next());
    }

    @Test
    void testCreateFileChooserParent() {
        if (mockedMain == null)
            mockedMain = Mockito.mockStatic(Main.class, Mockito.CALLS_REAL_METHODS);
        JFrame mockedFrame = Mockito.mock(JFrame.class);
        mockedMain.when(Main::createJFrame).thenReturn(mockedFrame);

        JFrame test = Main.createFileChooserParent();

        Assertions.assertEquals(test, mockedFrame);
        Mockito.verify(mockedFrame).setLocationRelativeTo(null);
        Mockito.verify(mockedFrame).setVisible(true);
        Mockito.verify(mockedFrame).setExtendedState(JFrame.ICONIFIED);
        Mockito.verify(mockedFrame).setExtendedState(JFrame.NORMAL);
    }

    @Test
    void testCreateJFrame() {
        Assertions.assertNotNull(Main.createJFrame());
    }

    @Test
    void testFileChooserFilter() {
        String expectedFilterDescription = "Markdown File (.md)";

        Main.createFileChooser();
        FileFilter filter = Main.fileChooser.getFileFilter();

        Assertions.assertNotNull(Main.fileChooser);
        Assertions.assertEquals(expectedFilterDescription, filter.getDescription());
    }

    @Test
    void testRunFileChooser() {
        mockJFileChooser(0, "Test.md");
        Main.createFileChooser();

        Main.runFileChooser();

        Assertions.assertEquals(0, Main.fileChooserStatus);
        Mockito.verify(mockedFrame).setVisible(false);
    }

    @Test
    void testGetOutputFileInput() {
        mockJFileChooser(0, "Test.txt");

        Main.getOutputFileInput();

        Assertions.assertEquals("Test.txt.md", Main.outputPath);
        Assertions.assertEquals("Choose a location for the output File" + System.getProperty("line.separator"), outContent.toString());
    }

    @Test
    void testGetWrongInputFromFileChooser() {
        mockJFileChooser(JFileChooser.ERROR_OPTION, "Test");
        mockSystemExit();

        Assertions.assertThrows(RuntimeException.class, Main::getOutputFileInput, "SecurityException: Tried to exit with status -1");
        Assertions.assertEquals("ERROR: Unexpected error. Stopping program." + System.getProperty("line.separator"), errContent.toString());
    }

    @Test
    void testAbortFileChooser() {
        mockJFileChooser(JFileChooser.CANCEL_OPTION, "Test");
        mockSystemExit();

        Assertions.assertThrows(RuntimeException.class, Main::getOutputFileInput, "SecurityException: Tried to exit with status 0");
        Assertions.assertEquals("ERROR: File choosing aborted. Stopping program." + System.getProperty("line.separator"), errContent.toString());
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
        if (mockedMain == null)
            mockedMain = Mockito.mockStatic(Main.class, Mockito.CALLS_REAL_METHODS);
        mockedFrame = Mockito.mock(JFrame.class);
        mockedMain.when(Main::createJFrame).thenReturn(mockedFrame);
        mockedMain.when(Main::createFileChooser).then(invocationOnMock -> {
            Main.fileChooser = Mockito.mock(JFileChooser.class);
            Mockito.when(Main.fileChooser.showSaveDialog(Mockito.any())).thenReturn(returnCode);
            Mockito.when(Main.fileChooser.getSelectedFile()).thenReturn(new File(chosenPath));
            return null;
        });
    }

    @Test
    void testAddFileExtension() {
        Main.outputPath = "E:\\RealFolder\\output";

        Main.addFileExtension();

        Assertions.assertEquals("E:\\RealFolder\\output.md", Main.outputPath);

    }

    @Test
    void testUnnecessaryAddFileExtension() {
        Main.outputPath = "E:\\RealFolder\\output.md";

        Main.addFileExtension();

        Assertions.assertEquals("E:\\RealFolder\\output.md", Main.outputPath);
    }

    @Test
    void testGetLanguageInput() {
        mockInputScanner("en");

        Main.getLanguageInput();

        assertTestGetLanguage();
    }

    private void assertTestGetLanguage() {
        Assertions.assertEquals("Enter your language code [zB de]" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("", errContent.toString());
        Assertions.assertEquals("en", Main.languageCode);
    }

    @Test
    void testGetLanguageInputError() {
        mockInputScanner("Not a language code\nef\nen");

        Main.getLanguageInput();

        assertTestGetLanguageError();
    }

    private void assertTestGetLanguageError() {
        Assertions.assertEquals("Enter your language code [zB de]" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("ERROR: Please enter a valid language code." + System.getProperty("line.separator") + "ERROR: Please enter a valid language code." + System.getProperty("line.separator"), errContent.toString());
        Assertions.assertEquals("en", Main.languageCode);
    }

    @Test
    void testGetDepthInput() {
        mockInputScanner("3\n");

        Main.getDepthInput();

        assertTestGetDepthInput();
    }

    private void assertTestGetDepthInput() {
        Assertions.assertEquals("Enter the depth of search (how many additional Links should be analyzed)" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("", errContent.toString());
        Assertions.assertEquals(3, Main.depthOfRecursiveSearch);
    }

    @Test
    void testGetDepthInputError() {
        mockInputScanner("Number\n-1\n-2\n-10\n0\n");

        Main.getDepthInput();

        assertTestGetDepthInputError();
    }

    private void assertTestGetDepthInputError() {
        Assertions.assertEquals("Enter the depth of search (how many additional Links should be analyzed)" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("ERROR: Please enter a valid number." + System.getProperty("line.separator")
                + "ERROR: Please enter a positive number." + System.getProperty("line.separator")
                + "ERROR: Please enter a positive number." + System.getProperty("line.separator")
                + "ERROR: Please enter a positive number." + System.getProperty("line.separator"), errContent.toString());
        Assertions.assertEquals(0, Main.depthOfRecursiveSearch);
    }

    @Test
    void testGetWebsiteInput() {
        setUpTestGetWebsiteInput("https://example.com\n");

        Main.getWebsiteInput();

        assertTestGetWebsiteInput();
    }

    private void assertTestGetWebsiteInput() {
        Assertions.assertEquals("Enter the website url that should be crawled" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("", errContent.toString());
        Assertions.assertEquals("https://example.com", Main.websiteUrl);
        mockedCrawler.verify(() -> WebsiteCrawler.isBrokenLink("https://example.com"));
    }

    @Test
    void testGetWebsiteInputError() {
        setUpTestGetWebsiteInput("wrong URL format\nhttps://www.notARealWebsite.com\nhttps://example.com\n");

        Main.getWebsiteInput();

        assertTestGetWebsiteInputError();
    }

    private void assertTestGetWebsiteInputError() {
        Assertions.assertEquals("Enter the website url that should be crawled" + System.getProperty("line.separator"), outContent.toString());
        Assertions.assertEquals("ERROR: Cannot connect to url, please enter a valid url" + System.getProperty("line.separator") + "ERROR: Cannot connect to url, please enter a valid url" + System.getProperty("line.separator"), errContent.toString());
        Assertions.assertEquals("https://example.com", Main.websiteUrl);
    }

    private void setUpTestGetWebsiteInput(String testInput) {
        mockIsBrokenLink();
        mockInputScanner(testInput);
    }

    private void mockInputScanner(String testInput) {
        Main.inputScanner = new Scanner(new ByteArrayInputStream(testInput.getBytes()));
    }

    private void mockSetupScanner(String testInput) {
        if (mockedMain == null)
            mockedMain = Mockito.mockStatic(Main.class, Mockito.CALLS_REAL_METHODS);
        mockedMain.when(Main::setupScanner).then(invocationOnMock -> {
            mockInputScanner(testInput);
            return null;
        });
    }

    private void mockIsBrokenLink() {
        mockedCrawler = Mockito.mockStatic(WebsiteCrawler.class);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink(Mockito.anyString())).thenReturn(true);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink("https://example.com")).thenReturn(false);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink("wrong URL format")).thenReturn(true);
        mockedCrawler.when(() -> WebsiteCrawler.isBrokenLink("https://www.notARealWebsite.com")).thenReturn(true);
    }
}
