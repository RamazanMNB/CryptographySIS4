package quantum.quantum.ui;

import quantum.quantum.BB84Result;
import quantum.quantum.E91Protocol;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Renders live statistics as bar charts and key metrics.
 */
public class StatsChart extends JPanel {

    private static final Color BG      = new Color(15, 17, 26);
    private static final Color GRID    = new Color(40, 50, 70);
    private static final Color BAR1    = new Color(86, 182, 254);
    private static final Color BAR2    = new Color(80, 200, 120);
    private static final Color BAR3    = new Color(255, 180,  50);
    private static final Color BAR_ERR = new Color(255, 100, 100);
    private static final Color TEXT    = new Color(200, 210, 230);
    private static final Color MUTED   = new Color(100, 110, 140);

    // BB84 stats
    private BB84Result bb84Result = null;
    // E91 stats
    private E91Protocol.E91Result e91Result = null;
    // History for multiple runs
    private final List<Double> errorRateHistory = new ArrayList<>();
    private final List<Double> efficiencyHistory = new ArrayList<>();
    private static final int MAX_HISTORY = 20;

    public StatsChart() {
        setBackground(BG);
        setPreferredSize(new Dimension(300, 500));
    }

    public void updateBB84(BB84Result r) {
        this.bb84Result = r;
        errorRateHistory.add(r.errorRate * 100);
        efficiencyHistory.add(r.keyEfficiency);
        if (errorRateHistory.size() > MAX_HISTORY) errorRateHistory.remove(0);
        if (efficiencyHistory.size() > MAX_HISTORY) efficiencyHistory.remove(0);
        repaint();
    }

    public void updateE91(E91Protocol.E91Result r) {
        this.e91Result = r;
        repaint();
    }

    public void reset() {
        bb84Result = null;
        e91Result  = null;
        repaint();
    }

    // =========================================================================

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        g2.setColor(BG);
        g2.fillRect(0, 0, w, h);

        if (bb84Result == null && e91Result == null) {
            drawPlaceholder(g2, w, h);
            return;
        }

        int y = 12;

        if (bb84Result != null) {
            y = drawBB84Stats(g2, w, y);
        }
        if (e91Result != null) {
            y = drawE91Stats(g2, w, y);
        }
        if (errorRateHistory.size() > 1) {
            drawLineChart(g2, w, y);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    private int drawBB84Stats(Graphics2D g2, int w, int y) {
        BB84Result r = bb84Result;
        int pad = 14;

        // Section title
        sectionTitle(g2, "BB84 Statistics", pad, y);
        y += 22;

        // Status badge
        Color statusCol = r.aborted ? BAR_ERR : BAR2;
        String statusTxt = r.aborted ? "ABORTED" : "SUCCESS";
        drawStatusBadge(g2, pad, y, statusTxt, statusCol);
        y += 28;

        // Key metrics bars
        y = drawMetricBar(g2, "Basis match",  r.basisMatchRate * 100, 50.0, "%", pad, y, w - pad * 2, BAR1);
        y = drawMetricBar(g2, "Error rate",   r.errorRate * 100,      11.0, "%", pad, y, w - pad * 2,
                r.errorRate > 0.11 ? BAR_ERR : BAR2);
        y = drawMetricBar(g2, "Key efficiency", r.keyEfficiency,      100.0, "%", pad, y, w - pad * 2, BAR3);

        // Numeric table
        y += 4;
        String[][] rows = {
                {"Photons",      String.valueOf(r.totalPhotons)},
                {"Sifted key",   r.siftedKeyLength + " bits"},
                {"Final key",    r.finalKeyLength + " bits"},
                {"Error rate",   String.format("%.1f%%", r.errorRate * 100)},
                {"Efficiency",   String.format("%.1f%%", r.keyEfficiency)},
                r.evePresent ? new String[]{"Eve match", String.format("%.1f%%", r.eveBasisMatchRate * 100)} : null,
                r.cascadeUsed ? new String[]{"Cascade ↓", r.cascadeErrorsBefore + " → " + r.cascadeErrorsAfter} : null,
        };
        for (String[] row : rows) {
            if (row == null) continue;
            y = drawKV(g2, row[0], row[1], pad, y, w);
        }

        return y + 10;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private int drawE91Stats(Graphics2D g2, int w, int y) {
        E91Protocol.E91Result r = e91Result;
        int pad = 14;

        sectionTitle(g2, "E91 Statistics", pad, y);
        y += 22;

        Color statusCol = r.aborted ? BAR_ERR : BAR2;
        drawStatusBadge(g2, pad, y, r.aborted ? "ABORTED" : "SUCCESS", statusCol);
        y += 28;

        // CHSH bar — scale to 0..2√2
        double maxS = 2.0 * Math.sqrt(2);
        y = drawMetricBar(g2, "|CHSH| S",     Math.abs(r.chshValue), maxS, "", pad, y, w - pad * 2,
                r.bellTestPassed ? BAR2 : BAR_ERR);
        y = drawMetricBar(g2, "Key efficiency", r.keyEfficiency, 100.0, "%", pad, y, w - pad * 2, BAR3);

        String[][] rows = {
                {"Pairs",    String.valueOf(r.totalPairs)},
                {"Sifted",   r.siftedKeyLength + " bits"},
                {"CHSH |S|", String.format("%.4f", Math.abs(r.chshValue))},
                {"Bell test", r.bellTestPassed ? "PASSED" : "FAILED"},
                {"Eve",      r.evePresent ? "YES" : "NO"},
        };
        for (String[] row : rows) y = drawKV(g2, row[0], row[1], pad, y, w);

        return y + 10;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void drawLineChart(Graphics2D g2, int w, int y) {
        int pad = 14, cw = w - pad * 2, ch = 70;
        sectionTitle(g2, "Error Rate History", pad, y);
        y += 20;

        // Background
        g2.setColor(new Color(25, 30, 45));
        g2.fillRoundRect(pad, y, cw, ch, 6, 6);

        // Grid line at 11%
        int threshY = y + ch - (int)(0.11 / 0.30 * ch);
        g2.setColor(new Color(BAR_ERR.getRed(), BAR_ERR.getGreen(), BAR_ERR.getBlue(), 80));
        g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{4,4}, 0));
        g2.drawLine(pad, threshY, pad + cw, threshY);
        g2.setStroke(new BasicStroke(1.5f));

        // Plot
        if (errorRateHistory.size() >= 2) {
            List<Double> hist = errorRateHistory;
            int n = hist.size();
            int[] xs = new int[n], ys = new int[n];
            for (int i = 0; i < n; i++) {
                xs[i] = pad + (int)((double)i / (n - 1) * cw);
                ys[i] = y + ch - (int)(Math.min(hist.get(i), 30.0) / 30.0 * ch);
            }
            g2.setColor(BAR1);
            g2.setStroke(new BasicStroke(1.8f));
            for (int i = 0; i < n - 1; i++) g2.drawLine(xs[i], ys[i], xs[i+1], ys[i+1]);
            for (int i = 0; i < n; i++) {
                g2.setColor(BAR1);
                g2.fillOval(xs[i] - 3, ys[i] - 3, 6, 6);
            }
        }
    }

    // =========================================================================
    // DRAW HELPERS
    // =========================================================================

    private void drawPlaceholder(Graphics2D g2, int w, int h) {
        g2.setColor(MUTED);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        String s = "Run simulation to see statistics";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, (w - fm.stringWidth(s)) / 2, h / 2);
    }

    private void sectionTitle(Graphics2D g2, String title, int x, int y) {
        g2.setColor(new Color(120, 160, 210));
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2.drawString(title, x, y + 12);
        g2.setColor(GRID);
        g2.fillRect(x, y + 16, getWidth() - x * 2, 1);
    }

    private void drawStatusBadge(Graphics2D g2, int x, int y, String text, Color col) {
        g2.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(text) + 20, bh = 20;
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 35));
        g2.fillRoundRect(x, y, bw, bh, 6, 6);
        g2.setColor(col);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect(x, y, bw, bh, 6, 6);
        g2.drawString(text, x + 10, y + fm.getAscent() + 2);
    }

    private int drawMetricBar(Graphics2D g2, String label, double value, double max,
                               String unit, int x, int y, int bw, Color col) {
        int barH = 14;
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(MUTED);
        g2.drawString(label, x, y + 11);

        int labelW = 90, barStart = x + labelW;
        int availW = bw - labelW - 50;
        double ratio = Math.min(value / max, 1.0);
        int filled = (int)(ratio * availW);

        g2.setColor(new Color(40, 50, 70));
        g2.fillRoundRect(barStart, y + 2, availW, barH, 4, 4);
        if (filled > 0) {
            g2.setColor(col);
            g2.fillRoundRect(barStart, y + 2, filled, barH, 4, 4);
        }

        g2.setColor(TEXT);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        String val = String.format("%.1f%s", value, unit);
        g2.drawString(val, barStart + availW + 6, y + 11);
        return y + barH + 8;
    }

    private int drawKV(Graphics2D g2, String key, String val, int x, int y, int w) {
        g2.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2.setColor(MUTED);
        g2.drawString(key, x, y + 11);
        g2.setColor(TEXT);
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(val, w - fm.stringWidth(val) - x, y + 11);
        return y + 15;
    }
}
