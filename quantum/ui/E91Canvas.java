package quantum.quantum.ui;

import quantum.quantum.E91Protocol;

import javax.swing.*;
import java.awt.*;

/**
 * Visualizes the E91 entanglement-based protocol.
 * Shows: source, Alice, Bob, entangled pair animation, CHSH meter.
 */
public class E91Canvas extends JPanel {

    private static final Color BG       = new Color(15, 17, 26);
    private static final Color ALICE    = new Color(86, 182, 254);
    private static final Color BOB      = new Color(80, 200, 120);
    private static final Color SOURCE   = new Color(180, 130, 255);
    private static final Color ENTANGLE = new Color(255, 180, 50);
    private static final Color TEXT     = new Color(200, 210, 230);
    private static final Color MUTED    = new Color(100, 120, 160);
    private static final Color SUCCESS  = new Color(80, 200, 120);
    private static final Color DANGER   = new Color(255, 100, 100);

    private E91Protocol.E91Result result = null;
    private double animT = 0;
    private Timer  animTimer;

    public E91Canvas() {
        setBackground(BG);
        setPreferredSize(new Dimension(600, 400));
    }

    public void showResult(E91Protocol.E91Result r) {
        this.result = r;
        this.animT  = 0;
        if (animTimer != null) animTimer.stop();
        animTimer = new Timer(20, e -> {
            animT += 0.02;
            if (animT > 1.0) { animT = 1.0; animTimer.stop(); }
            repaint();
        });
        animTimer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth(), h = getHeight();
        int cy = h / 2 - 20;
        int srcX = w / 2, aliceX = 80, bobX = w - 80;

        drawBackground(g2, w, h);

        if (result == null) {
            drawPlaceholder(g2, w, h);
            return;
        }

        // ── Entanglement lines ────────────────────────────────────────────────
        drawEntangleLine(g2, srcX, cy, aliceX, cy, animT);
        drawEntangleLine(g2, srcX, cy, bobX,   cy, animT);

        // ── Nodes ─────────────────────────────────────────────────────────────
        drawParticle(g2, srcX,   cy, "Source", SOURCE, "⊗");
        drawParticle(g2, aliceX, cy, "Alice",  ALICE,  "◉");
        drawParticle(g2, bobX,   cy, "Bob",    BOB,    "◉");

        // ── Measurement angle labels ──────────────────────────────────────────
        drawAngleLabel(g2, aliceX, cy + 40, "Angles: 0°  45°  90°",  ALICE);
        drawAngleLabel(g2, bobX,   cy + 40, "Angles: 45°  90°  135°", BOB);

        // ── CHSH meter ────────────────────────────────────────────────────────
        drawCHSHMeter(g2, w, cy + 80);

        // ── Result summary ────────────────────────────────────────────────────
        drawResultSummary(g2, w, h);
    }

    // ─────────────────────────────────────────────────────────────────────────

    private void drawBackground(Graphics2D g2, int w, int h) {
        g2.setColor(BG);
        g2.fillRect(0, 0, w, h);
    }

    private void drawPlaceholder(Graphics2D g2, int w, int h) {
        g2.setColor(MUTED);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        String s = "Click 'Run E91' to simulate";
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(s, (w - fm.stringWidth(s)) / 2, h / 2);
    }

    private void drawEntangleLine(Graphics2D g2, int x1, int y1, int x2, int y2, double t) {
        // Animated wave line
        int steps  = 80;
        int prevX = x1, prevY = y1;
        g2.setStroke(new BasicStroke(1.5f));
        for (int i = 1; i <= (int)(steps * t); i++) {
            double frac = (double) i / steps;
            int    nx   = (int)(x1 + frac * (x2 - x1));
            double wave = 10 * Math.sin(frac * Math.PI * 6 + animT * Math.PI * 4);
            int    ny   = (int)(y1 + wave);
            float  alpha = (float)(0.3 + 0.7 * frac);
            g2.setColor(new Color(ENTANGLE.getRed(), ENTANGLE.getGreen(), ENTANGLE.getBlue(),
                    (int)(alpha * 180)));
            g2.drawLine(prevX, prevY, nx, ny);
            prevX = nx; prevY = ny;
        }
        g2.setStroke(new BasicStroke(1.5f));
    }

    private void drawParticle(Graphics2D g2, int x, int cy, String name, Color col, String symbol) {
        int r = 24;
        // Glow
        for (int ri = r + 12; ri >= r; ri -= 3) {
            float a = 0.05f * (r + 12 - ri);
            g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(a * 255)));
            g2.fillOval(x - ri, cy - ri, ri * 2, ri * 2);
        }
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 40));
        g2.fillOval(x - r, cy - r, r * 2, r * 2);
        g2.setColor(col);
        g2.setStroke(new BasicStroke(2f));
        g2.drawOval(x - r, cy - r, r * 2, r * 2);

        // Symbol
        g2.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fm = g2.getFontMetrics();
        g2.setColor(col);
        g2.drawString(symbol, x - fm.stringWidth(symbol) / 2, cy + fm.getAscent() / 2 - 2);

        // Name
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        fm = g2.getFontMetrics();
        g2.drawString(name, x - fm.stringWidth(name) / 2, cy - r - 8);
    }

    private void drawAngleLabel(Graphics2D g2, int x, int y, String text, Color col) {
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 160));
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(text, x - fm.stringWidth(text) / 2, y);
    }

    private void drawCHSHMeter(Graphics2D g2, int w, int y) {
        if (result == null) return;
        double s    = Math.abs(result.chshValue);
        double maxS = 2.0 * Math.sqrt(2);

        int mw   = Math.min(w - 60, 500);
        int mx   = (w - mw) / 2;
        int mh   = 20;

        // Background
        g2.setColor(new Color(30, 36, 54));
        g2.fillRoundRect(mx, y, mw, mh, 6, 6);

        // Classical limit marker at S=2
        int classX = mx + (int)(2.0 / maxS * mw);
        g2.setColor(new Color(255, 100, 100, 120));
        g2.fillRect(classX, y, 2, mh);

        // Actual value bar
        int filled = (int)(Math.min(s, maxS) / maxS * mw);
        Color barCol = result.bellTestPassed ? SUCCESS : DANGER;
        g2.setColor(barCol);
        g2.fillRoundRect(mx, y, filled, mh, 6, 6);

        // Labels
        g2.setFont(new Font("Monospaced", Font.BOLD, 11));
        g2.setColor(TEXT);
        String lbl = String.format("CHSH |S| = %.4f  (classical limit: 2.0, quantum max: 2√2 ≈ 2.828)", s);
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString(lbl, (w - fm.stringWidth(lbl)) / 2, y + mh + 16);

        g2.setColor(MUTED);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 10));
        g2.drawString("|S| ≤ 2: classical / eavesdropped", mx, y + mh + 32);
        g2.drawString("|S| > 2: genuine entanglement (secure)", mx + mw / 2, y + mh + 32);
    }

    private void drawResultSummary(Graphics2D g2, int w, int h) {
        if (result == null) return;
        int pad = 16;
        int y   = h - 90;

        // Status
        Color col = result.aborted ? DANGER : SUCCESS;
        String st = result.aborted
                ? "ABORTED — Bell inequality not violated (Eve detected)"
                : "SUCCESS — Entanglement confirmed, channel is secure";

        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int bw = fm.stringWidth(st) + 24, bh = 24;
        g2.setColor(new Color(col.getRed(), col.getGreen(), col.getBlue(), 30));
        g2.fillRoundRect((w - bw) / 2, y, bw, bh, 8, 8);
        g2.setColor(col);
        g2.setStroke(new BasicStroke(1.5f));
        g2.drawRoundRect((w - bw) / 2, y, bw, bh, 8, 8);
        g2.drawString(st, (w - fm.stringWidth(st)) / 2, y + fm.getAscent() + 4);

        y += 32;
        g2.setFont(new Font("Monospaced", Font.PLAIN, 11));
        g2.setColor(MUTED);
        String info = String.format("Pairs: %d  |  Sifted key: %d bits  |  Efficiency: %.1f%%  |  Eve: %s",
                result.totalPairs, result.siftedKeyLength, result.keyEfficiency,
                result.evePresent ? "YES" : "NO");
        fm = g2.getFontMetrics();
        g2.drawString(info, (w - fm.stringWidth(info)) / 2, y + 12);

        // Key preview
        if (!result.aborted && result.aliceFinalKey != null && !result.aliceFinalKey.isEmpty()) {
            y += 20;
            int preview = Math.min(64, result.aliceFinalKey.size());
            StringBuilder sb = new StringBuilder("Key: ");
            for (int i = 0; i < preview; i++) sb.append(result.aliceFinalKey.get(i));
            if (result.aliceFinalKey.size() > 64) sb.append("…");
            g2.setColor(SUCCESS);
            fm = g2.getFontMetrics();
            g2.drawString(sb.toString(), (w - fm.stringWidth(sb.toString())) / 2, y + 12);
        }
    }
}
