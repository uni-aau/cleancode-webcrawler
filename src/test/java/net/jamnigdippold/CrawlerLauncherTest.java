package net.jamnigdippold;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class CrawlerLauncherTest {
    private CrawlerLauncher launcher;
    private List<WebsiteCrawler> crawlerList;

    @BeforeEach
    void setUp() {
        launcher = new CrawlerLauncher();
        WebsiteCrawler c1 = mock(WebsiteCrawler.class);
        WebsiteCrawler c2 = mock(WebsiteCrawler.class);
        WebsiteCrawler c3 = mock(WebsiteCrawler.class);
        crawlerList = List.of(c1, c2, c3);
        launcher.setCrawlers(crawlerList);
    }

    @Test
    void testWaitForCrawlerThreadsToFinish() throws InterruptedException {
        launcher.waitForCrawlerThreadsToFinish();

        for (WebsiteCrawler websiteCrawler : crawlerList) {
            verify(websiteCrawler).join();
        }
    }

    @Test
    void testWaitForCrawlerThreadsToFinishException() throws InterruptedException {
        doThrow(new InterruptedException("Error on line 10")).when(crawlerList.get(0)).join();

        launcher.waitForCrawlerThreadsToFinish();

        assertEquals("Error whilst joining crawler threads: java.lang.InterruptedException: Error on line 10", ErrorLogger.getInstance().getErrorLog().get(0));
    }
}
