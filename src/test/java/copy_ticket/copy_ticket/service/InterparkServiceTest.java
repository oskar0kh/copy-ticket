package copy_ticket.copy_ticket.service;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * 렌더링된 HTML을 파일로 직접 저장하는 테스트
 * (Playwright를 직접 사용해서 page.content()의 결과를 보는 방식)
 */
public class InterparkServiceTest {

    @Test
    public void testSaveRenderedHtmlFile() {
        String url = "https://tickets.interpark.com/goods/26003042";
        String projectRoot = System.getProperty("user.dir");
        String htmlFilePath = projectRoot + "/example_interpark_html.html";

        System.out.println("\n" + "=".repeat(80));
        System.out.println("🌐 RENDERING AND SAVING HTML");
        System.out.println("=".repeat(80));
        System.out.println("📍 URL: " + url);
        System.out.println("💾 Save to: " + htmlFilePath);

        try (Playwright playwright = Playwright.create()) {
            Browser browser = playwright.chromium().launch();
            BrowserContext context = browser.newContext();
            Page page = context.newPage();

            System.out.println("\n⏳ Navigating to page...");
            page.navigate(url);

            System.out.println("⏳ Waiting for page load (NETWORKIDLE)...");
            page.waitForLoadState(LoadState.NETWORKIDLE);
            System.out.println("✅ Page loaded!");

            // 렌더링된 HTML 가져오기
            String htmlContent = page.content();
            System.out.println("\n📊 HTML Statistics:");
            System.out.println("   - Total size: " + htmlContent.length() + " bytes");
            System.out.println("   - Lines: " + htmlContent.split("\n").length);

            // 파일로 저장
            Files.write(Paths.get(htmlFilePath), htmlContent.getBytes());
            System.out.println("\n✅ HTML saved to: " + htmlFilePath);

            context.close();
            browser.close();
            System.out.println("\n✅ Rendering completed!");

        } catch (Exception e) {
            System.err.println("❌ Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
