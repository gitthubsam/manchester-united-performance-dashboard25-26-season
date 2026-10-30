import java.awt.Color;
import java.awt.Font;

/** Shared colour palette and typography so every page uses the same design. */
final class DashboardTheme {
    private DashboardTheme() {}
    static final Color BG = new Color(11, 16, 25);
    static final Color SIDEBAR = new Color(17, 23, 34);
    static final Color CARD = new Color(23, 31, 44);
    static final Color INPUT = new Color(30, 40, 54);
    static final Color BORDER = new Color(46, 58, 75);
    static final Color WHITE = new Color(243, 247, 252);
    static final Color MUTED = new Color(150, 162, 179);
    static final Color RED = new Color(232, 45, 59);
    static final Color GREEN = new Color(47, 191, 140);
    static final Color AMBER = new Color(239, 179, 82);
    static final Color BLUE = new Color(83, 147, 232);

    static Font uiFont(int style, int size) {
        return DashboardAssets.uiFont(style, size);
    }
}
